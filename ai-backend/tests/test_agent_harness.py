"""离线真实 LangGraph 测试：假模型、临时 checkpoint，不访问开发数据库/网络。"""

import asyncio
from dataclasses import replace
import json
import threading
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

import httpx
from langchain_core.language_models.chat_models import BaseChatModel, agenerate_from_stream
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage, ToolMessage
from langchain_core.outputs import ChatGenerationChunk
from langchain_core.tools import tool
import pytest

from agent.harness import HarnessError, Limits, RunState, bounded_messages
from agent.workers import BoundedWorkers, WorkerBusy


class ScriptedModel(BaseChatModel):
    scripts: list
    delay: float = 0
    calls: int = 0
    cancelled: bool = False

    @property
    def _llm_type(self):
        return "offline-script"

    def bind_tools(self, tools, **kwargs):
        return self

    def _generate(self, *args, **kwargs):
        raise AssertionError("不能调用同步模型")

    async def _agenerate(self, messages, stop=None, run_manager=None, **kwargs):
        return await agenerate_from_stream(self._astream(messages, **kwargs))

    async def _astream(self, messages, **kwargs):
        index = self.calls
        self.calls += 1
        try:
            for chunk in self.scripts[min(index, len(self.scripts) - 1)]:
                await asyncio.sleep(self.delay)
                if isinstance(chunk, Exception):
                    raise chunk
                yield ChatGenerationChunk(message=chunk)
        except asyncio.CancelledError:
            self.cancelled = True
            raise


@pytest.fixture
def agent_env(modules, tmp_path, monkeypatch):
    from agent import service
    import conversation.service as conversation
    monkeypatch.setattr(service, "DB_PATH", str(tmp_path / "checkpoint.db"))
    monkeypatch.setattr(service, "load_system_prompt", lambda: "test system")
    ensure = Mock(side_effect=lambda thread, question, user: thread)
    save = Mock()
    monkeypatch.setattr(conversation, "ensure_conversation_and_save_user", ensure)
    monkeypatch.setattr(conversation, "save_ai_message", save)
    original_schedule_title = service._schedule_title
    monkeypatch.setattr(service, "_schedule_title", Mock())
    return SimpleNamespace(service=service, save=save, ensure=ensure, original_schedule_title=original_schedule_title)


def run_stream(env, model, *, tools=None, limits=None):
    limits = limits or Limits()
    run = RunState(limits)

    async def collect():
        stream = env.service.stream_chat("test question", "thread", "alice", "private-token",
                                         model, tools or [], limits=limits, run_state=run)
        return [event async for event in stream]
    return asyncio.run(asyncio.wait_for(collect(), 3)), run


def test_native_async_success_persists_complete_answer(agent_env):
    events, run = run_stream(agent_env, ScriptedModel(scripts=[[AIMessageChunk(
        content="hello\nworld", usage_metadata={"input_tokens": 2, "output_tokens": 3, "total_tokens": 5},
    )]]))
    assert events[-1] == "data: [DONE]\n\n"
    assert any("hello\\nworld" in event for event in events)
    assert run.model_calls == 1
    assert run.usage == {"input_tokens": 2, "output_tokens": 3, "total_tokens": 5}
    agent_env.save.assert_called_once()
    assert agent_env.save.call_args.args[1] == "hello\nworld"


def test_partial_failure_not_saved_or_leaked(agent_env):
    events, run = run_stream(agent_env, ScriptedModel(scripts=[[
        AIMessageChunk(content="partial"), RuntimeError("private-token secret upstream URL"),
    ]]))
    assert events[-1].startswith("data: [ERROR]")
    assert "private-token" not in "".join(events)
    agent_env.save.assert_not_called()


@pytest.mark.parametrize("kind", ["model", "total"])
def test_timeouts_cancel_model_and_do_not_save(agent_env, kind):
    limits = replace(Limits(), **{f"{'model' if kind == 'model' else 'total'}_seconds": 0.05})
    model = ScriptedModel(scripts=[[AIMessageChunk(content="too late")]], delay=1)
    events, run = run_stream(agent_env, model, limits=limits)
    assert events[-1].startswith("data: [ERROR]")
    assert model.cancelled
    agent_env.save.assert_not_called()


def test_model_and_tool_budgets_stop_real_graph(agent_env):
    @tool
    async def repeat() -> str:
        """Repeat."""
        return "ok"

    model = ScriptedModel(scripts=[[AIMessageChunk(content="", tool_calls=[
        {"name": "repeat", "args": {}, "id": "call-1"},
    ])]])
    events, run = run_stream(agent_env, model, tools=[repeat], limits=replace(Limits(), max_model_calls=1))
    assert events[-1].startswith("data: [ERROR]")
    assert run.model_calls == 1 and run.tool_calls == 1
    agent_env.save.assert_not_called()


def test_disconnect_cancels_producer_and_releases_admission(agent_env):
    async def scenario():
        model = ScriptedModel(scripts=[[AIMessageChunk(content="first"), AIMessageChunk(content="last")]], delay=0.1)
        stream = agent_env.service.stream_chat("question", "thread", "alice", model=model)
        await anext(stream)  # generating state; producer 尚未完成。
        await stream.aclose()
        assert not agent_env.service._active_runs
        assert model.cancelled
        agent_env.save.assert_not_called()
    asyncio.run(scenario())


def test_busy_and_same_conversation_do_not_write_user_message(agent_env):
    async def scenario():
        agent_env.service._active_runs.add(("alice", "thread"))
        try:
            events = [event async for event in agent_env.service.stream_chat("q", "thread", "alice")]
            assert "繁忙" in events[0]
            agent_env.ensure.assert_not_called()
        finally:
            agent_env.service._active_runs.clear()
    asyncio.run(scenario())


def test_output_budget_does_not_save_partial_answer(agent_env):
    events, run = run_stream(agent_env, ScriptedModel(scripts=[[AIMessageChunk(content="too long")]]),
                             limits=replace(Limits(), max_output_chars=2))
    assert "长度预算" in events[-1]
    agent_env.save.assert_not_called()


def test_sync_worker_slot_is_retained_after_cancelling_waiter():
    async def scenario():
        pool = BoundedWorkers(1)
        started, release = threading.Event(), threading.Event()
        def blocking():
            started.set()
            release.wait(2)
        task = asyncio.create_task(pool.run(blocking))
        try:
            while not started.is_set():
                await asyncio.sleep(0.001)
            task.cancel()
            with pytest.raises(asyncio.CancelledError):
                await task
            with pytest.raises(WorkerBusy):
                await pool.run(lambda: None)
        finally:
            release.set()
            pool.shutdown()
    asyncio.run(scenario())


def test_context_trimming_keeps_tool_protocol_and_drops_incomplete_calls():
    old = HumanMessage(content="old" * 1000)
    user = HumanMessage(content="new")
    call = AIMessage(content="", tool_calls=[{"name": "x", "args": {}, "id": "one"}])
    response = ToolMessage(content="result", tool_call_id="one")
    assert bounded_messages([old, AIMessage(content="answer"), user, call, response], 2000) == [user, call, response]
    assert bounded_messages([user, call], 2000) == [user]
    with pytest.raises(HarnessError):
        bounded_messages([old], 10)


@pytest.mark.parametrize("value", [0, -1, float("inf"), float("nan")])
def test_invalid_limits_fail_fast(value):
    with pytest.raises(ValueError):
        Limits(total_seconds=value)


def test_actual_kb_context_is_captured_from_tool_and_model_input(agent_env, modules, monkeypatch, caplog):
    vectors = Mock()
    vectors.search.return_value = [
        {"doc_id": 7, "chunk_idx": 3, "score": 0.8, "content": "actual private context"},
        {"doc_id": 7, "chunk_idx": 4, "score": 0.2, "content": "discarded context"},
    ]
    monkeypatch.setattr(modules.tools, "get_vector_store", lambda: vectors)
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{"name": "search_knowledge_base", "args": {"query": "real query"}, "id": "kb-1"}])],
        [AIMessageChunk(content="answer [文档#7 片段#3]")],
    ])
    events, run = run_stream(agent_env, model, tools=[modules.tools.search_knowledge_base])
    assert events[-1] == "data: [DONE]\n\n"
    assert run.retrievals[0]["query"] == "real query"
    assert run.retrievals[0]["selected"] == [{"doc_id": 7, "chunk_idx": 3, "score": 0.8}]
    inputs = run.model_contexts[-1]["messages"]
    assert any(isinstance(m, ToolMessage) and "actual private context" in m.content for m in inputs)
    assert all("discarded context" not in str(m.content) for m in inputs)
    vectors.search.assert_called_once_with("real query", user_id="alice", top_k=8)
    assert "actual private context" not in caplog.text
    assert "real query" not in caplog.text
    assert "private-token" not in caplog.text


def test_publish_transport_failure_is_unknown_and_never_retried(agent_env, monkeypatch):
    from agent.tools import publish_article, business_client
    post = AsyncMock(side_effect=httpx.ReadTimeout("private-token"))
    monkeypatch.setattr(business_client, "post", post)
    model = ScriptedModel(scripts=[[AIMessageChunk(content="", tool_calls=[
        {"name": "publish_article", "args": {"type": 5, "head": "test title", "content": "<p>test article</p>"}, "id": "pub-1"},
    ])]])
    events, run = run_stream(agent_env, model, tools=[publish_article])
    assert "发布结果未知" in events[-1]
    assert "private-token" not in "".join(events)
    assert run.publish_status == "unknown"
    assert post.await_count == 1
    agent_env.save.assert_not_called()


def test_logger_redacts_nested_credentials_but_keeps_usage():
    from common.logger import _redact
    assert _redact({"input_tokens": 12, "calls": [{"authorization": "secret", "api_key": "key", "cookie_header": "cookie"}]}) == {
        "input_tokens": 12, "calls": [{"authorization": "****", "api_key": "****", "cookie_header": "****"}],
    }


def test_read_tool_timeout_is_structured_and_model_can_explain(agent_env):
    @tool
    async def slow_read() -> str:
        """Read something slowly."""
        await asyncio.sleep(1)
        return "late"

    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{"name": "slow_read", "args": {}, "id": "slow"}])],
        [AIMessageChunk(content="读取超时，请稍后重试")],
    ])
    events, run = run_stream(agent_env, model, tools=[slow_read], limits=replace(Limits(), tool_seconds=0.02))
    assert events[-1] == "data: [DONE]\n\n"
    assert run.tools[0]["status"] == "tool_timeout"
    response = next(m for m in run.model_contexts[-1]["messages"] if isinstance(m, ToolMessage))
    assert json.loads(response.content)["error"]["code"] == "tool_timeout"


def test_tool_budget_limits_parallel_calls(agent_env):
    executed = []
    @tool
    async def read_item(item: int) -> str:
        """Read item."""
        executed.append(item)
        return "ok"

    model = ScriptedModel(scripts=[[AIMessageChunk(content="", tool_calls=[
        {"name": "read_item", "args": {"item": i}, "id": f"read-{i}"} for i in range(3)
    ])]])
    events, run = run_stream(agent_env, model, tools=[read_item], limits=replace(Limits(), max_tool_calls=1))
    assert events[-1].startswith("data: [ERROR]")
    assert run.tool_calls == 1 and len(executed) <= 1


def test_duplicate_publish_in_same_run_cannot_submit_twice(agent_env, monkeypatch):
    from agent.tools import business_client, publish_article
    post = AsyncMock(return_value={"code": 200})
    monkeypatch.setattr(business_client, "post", post)
    call = {"name": "publish_article", "args": {"type": 5, "head": "title", "content": "<p>long enough</p>"}, "id": "p"}
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[call])],
        [AIMessageChunk(content="", tool_calls=[{**call, "id": "p-again"}])],
    ])
    events, run = run_stream(agent_env, model, tools=[publish_article])
    assert events[-1].startswith("data: [ERROR]")
    assert "不要重复发布" in events[-1]
    assert post.await_count == 1
    assert run.publish_status == "succeeded"


def test_publish_success_followed_by_model_failure_reports_existing_publication(agent_env, monkeypatch):
    from agent.tools import business_client, publish_article
    post = AsyncMock(return_value={"code": "200"})
    monkeypatch.setattr(business_client, "post", post)
    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{
            "name": "publish_article", "args": {"type": 5, "head": "title", "content": "<p>long enough</p>"}, "id": "p",
        }])],
        [RuntimeError("upstream failed")],
    ])
    events, run = run_stream(agent_env, model, tools=[publish_article])
    assert "文章已发布" in events[-1]
    assert "不要重复发布" in events[-1]
    assert post.await_count == 1


def test_full_stream_buffer_is_bounded_and_disconnect_cleans_producer(agent_env, monkeypatch):
    real_queue = asyncio.Queue
    queues = []
    def capture_queue(*args, **kwargs):
        q = real_queue(*args, **kwargs)
        queues.append(q)
        return q
    monkeypatch.setattr(agent_env.service.asyncio, "Queue", capture_queue)

    async def scenario():
        model = ScriptedModel(scripts=[[AIMessageChunk(content="x") for _ in range(100)]])
        limits = replace(Limits(), stream_buffer=2)
        stream = agent_env.service.stream_chat("question", "thread", "alice", model=model, limits=limits)
        await anext(stream)
        await asyncio.sleep(0.03)
        buffer = next(q for q in queues if q.maxsize == 2)
        assert buffer.qsize() == 2
        await stream.aclose()
        assert not agent_env.service._active_runs
        assert not agent_env.service._producer_tasks
        agent_env.save.assert_not_called()
    asyncio.run(scenario())


def test_starlette_disconnect_cancellation_releases_resources(agent_env):
    from starlette.responses import StreamingResponse

    async def scenario():
        disconnected = asyncio.Event()
        initial = True
        async def receive():
            nonlocal initial
            if initial:
                initial = False
                return {"type": "http.request", "body": b"", "more_body": False}
            await disconnected.wait()
            return {"type": "http.disconnect"}
        async def send(message):
            if message["type"] == "http.response.body" and message.get("body"):
                disconnected.set()
        model = ScriptedModel(scripts=[[AIMessageChunk(content="first"), AIMessageChunk(content="late")]], delay=0.1)
        response = StreamingResponse(agent_env.service.stream_chat("q", "thread", "alice", model=model))
        scope = {"type": "http", "asgi": {"version": "3.0", "spec_version": "2.0"}, "method": "GET", "headers": []}
        await asyncio.wait_for(response(scope, receive, send), 2)
        assert not agent_env.service._active_runs
        assert not agent_env.service._producer_tasks
        agent_env.save.assert_not_called()
    asyncio.run(scenario())


def test_title_task_is_independent_and_has_timeout(agent_env, monkeypatch):
    original = agent_env.original_schedule_title
    import conversation.service as conversation
    cancelled = asyncio.Event()
    async def slow_title(*args):
        try:
            await asyncio.sleep(10)
        finally:
            cancelled.set()
    monkeypatch.setattr(conversation, "generate_and_save_title", slow_title)
    async def scenario():
        run = RunState(replace(Limits(), title_seconds=0.02))
        original("thread", "q", "answer", object(), run)
        assert agent_env.service._title_tasks
        await asyncio.wait_for(cancelled.wait(), 1)
        await asyncio.sleep(0)
        assert not agent_env.service._title_tasks
    asyncio.run(scenario())


def test_failed_publish_checkpoint_is_not_replayed_on_next_question(agent_env, monkeypatch):
    from agent.tools import business_client, publish_article
    post = AsyncMock(side_effect=httpx.ReadTimeout("unknown outcome"))
    monkeypatch.setattr(business_client, "post", post)
    publishing_model = ScriptedModel(scripts=[[AIMessageChunk(content="", tool_calls=[{
        "name": "publish_article", "args": {"type": 5, "head": "title", "content": "<p>valid content</p>"}, "id": "pending",
    }])]])
    first, _ = run_stream(agent_env, publishing_model, tools=[publish_article])
    assert "发布结果未知" in first[-1]
    second, run = run_stream(agent_env, ScriptedModel(scripts=[[AIMessageChunk(content="new answer")]]), tools=[publish_article])
    assert second[-1] == "data: [DONE]\n\n"
    assert post.await_count == 1
    assert not any(isinstance(m, AIMessage) and m.tool_calls for m in run.model_contexts[0]["messages"])


def test_swallowed_third_party_error_is_normalized_not_sent_to_model(agent_env):
    @tool
    async def tavily_search(query: str) -> dict:
        """Search the web."""
        return {"error": ValueError("private-token upstream details")}

    model = ScriptedModel(scripts=[
        [AIMessageChunk(content="", tool_calls=[{"name": "tavily_search", "args": {"query": "q"}, "id": "web"}])],
        [AIMessageChunk(content="search unavailable")],
    ])
    events, run = run_stream(agent_env, model, tools=[tavily_search])
    assert events[-1] == "data: [DONE]\n\n"
    result = next(m for m in run.model_contexts[-1]["messages"] if isinstance(m, ToolMessage))
    assert result.status == "error"
    assert "private-token" not in result.content
    assert json.loads(result.content)["error"]["code"] == "dependency_unavailable"


def test_invalid_publish_arguments_fail_before_http(agent_env, monkeypatch):
    from agent.tools import business_client, publish_article
    post = AsyncMock()
    monkeypatch.setattr(business_client, "post", post)
    model = ScriptedModel(scripts=[[AIMessageChunk(content="", tool_calls=[{
        "name": "publish_article", "args": {"type": 99, "head": "x", "content": "short"}, "id": "invalid-pub",
    }])]])
    events, run = run_stream(agent_env, model, tools=[publish_article])
    assert "参数不合法" in events[-1]
    assert run.publish_status == "failed"
    post.assert_not_awaited()


def test_async_agent_reads_existing_sync_sqlite_checkpoint(agent_env):
    from langchain.agents import create_agent
    from langchain_core.outputs import ChatGeneration, ChatResult
    from langgraph.checkpoint.sqlite import SqliteSaver

    class LegacyModel(ScriptedModel):
        def _generate(self, messages, **kwargs):
            return ChatResult(generations=[ChatGeneration(message=AIMessage(content="old reply"))])

    with SqliteSaver.from_conn_string(agent_env.service.DB_PATH) as saver:
        legacy = create_agent(model=LegacyModel(scripts=[]), tools=[], checkpointer=saver)
        legacy.invoke({"messages": [HumanMessage(content="old question")]},
                      config={"configurable": {"thread_id": "alice:thread"}})
    events, run = run_stream(agent_env, ScriptedModel(scripts=[[AIMessageChunk(content="new reply")]]))
    assert events[-1] == "data: [DONE]\n\n"
    assert any(m.content == "old reply" for m in run.model_contexts[0]["messages"])
