"""异步 Agent SSE：有界缓冲、取消传播、成功才保存完整回答。"""

import asyncio
from contextlib import suppress
import json
import os

import anyio

from langchain.agents import create_agent
from langchain_core.messages import AIMessageChunk, ToolMessage
from langgraph.checkpoint.sqlite.aio import AsyncSqliteSaver

from agent.harness import HarnessError, HarnessMiddleware, Limits, OutputBudget, RunState
from agent.workers import WorkerBusy, workers

DB_DIR = os.path.join(os.path.dirname(__file__), "db")
DB_PATH = os.path.join(DB_DIR, "conversations.db")
SYSTEM_PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "SystemPrompt.md")
os.makedirs(DB_DIR, exist_ok=True)


def load_system_prompt() -> str:
    try:
        with open(SYSTEM_PROMPT_PATH, encoding="utf-8") as f:
            return f.read().strip()
    except FileNotFoundError:
        return "你是一个协会网站的智能客服助手。"


class SSE:
    DONE = "[DONE]"
    ERROR = "[ERROR]"
    REASONING = "reasoning"
    GENERATING = "generating"
    THINKING = "thinking"

    @staticmethod
    def state(**kw) -> str:
        return "[STATE]" + json.dumps(kw, ensure_ascii=False)


_TOOL_STATES = {"knowledge": "searching_kb", "tavily": "searching_web", "publish": "publishing"}
_active_runs: set[tuple[str, str]] = set()
_title_tasks: set[asyncio.Task] = set()
_producer_tasks: set[asyncio.Task] = set()


def _sse_escape(text: str) -> str:
    return text.replace("\r", "").replace("\n", "\\n")


def _format_search_results(content) -> dict:
    raw = content if isinstance(content, str) else str(content)
    try:
        if raw.startswith("[KB]"):
            return json.loads(raw[4:].split("\n---\n", 1)[0])
        parsed = json.loads(raw)
        if isinstance(parsed, dict):
            parsed = parsed.get("results", [])
        return {"results": [
            {"url": r.get("url", ""), "title": r.get("title", ""),
             "snippet": (r.get("content", "") or "")[:120]}
            for r in parsed if isinstance(r, dict)
        ]} if isinstance(parsed, list) else {"results": []}
    except (ValueError, TypeError):
        return {"results": []}


def _text_content(content) -> str:
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "".join(block.get("text", "") for block in content
                       if isinstance(block, dict) and block.get("type") == "text")
    return ""


def _schedule_title(thread_id, question, reply, model, run):
    # 标题是可选辅助任务，不排队、不阻塞回答完成，不占用主任务预算。
    if len(_title_tasks) >= 2:
        return

    async def generate():
        from conversation.service import generate_and_save_title
        try:
            async with asyncio.timeout(run.limits.title_seconds):
                await generate_and_save_title(thread_id, question, reply, model)
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            run.emit("title_skipped", error_type=type(exc).__name__)

    task = asyncio.create_task(generate(), name=f"title:{run.run_id}")
    _title_tasks.add(task)
    task.add_done_callback(_title_tasks.discard)


async def shutdown_agent_tasks():
    tasks = list(_title_tasks | _producer_tasks)
    for task in tasks:
        task.cancel()
    await asyncio.gather(*tasks, return_exceptions=True)
    workers.shutdown()


async def stream_chat(question: str, thread_id: str, user_id: str = "", token: str = "",
                      model=None, tools: list = None, *, limits: Limits | None = None,
                      run_state: RunState | None = None):
    """SSE 协议保持不变；run_state 仅供测试/离线评估注入，不接受客户端配置。"""
    from conversation.service import ensure_conversation_and_save_user, save_ai_message

    limits = limits or (run_state.limits if run_state else Limits.from_env())
    run = run_state or RunState(limits)
    key = (user_id, thread_id)
    if not user_id.strip():
        yield "data: [ERROR] 请先登录\n\n"
        return
    if key in _active_runs or len(_active_runs) >= limits.max_active_runs:
        yield "data: [ERROR] 服务或当前会话繁忙，请等待本轮结束后重试\n\n"
        return
    if not question.strip() or len(question) > limits.max_input_chars:
        yield "data: [ERROR] 问题为空或超过输入长度限制\n\n"
        return
    _active_runs.add(key)
    queue = asyncio.Queue(maxsize=limits.stream_buffer)
    reply, search_info = [], {}

    async def produce():
        nonlocal thread_id
        try:
            async with asyncio.timeout(run.remaining()):
                thread_id = await workers.run(ensure_conversation_and_save_user, thread_id, question, user_id)
                async with AsyncSqliteSaver.from_conn_string(DB_PATH) as checkpointer:
                    agent = create_agent(
                        model=model, tools=tools or [], checkpointer=checkpointer,
                        system_prompt=load_system_prompt(), middleware=[HarnessMiddleware(run)],
                    )
                    config = {"configurable": {"thread_id": f"{user_id}:{thread_id}", "token": token,
                                                "user_id": user_id},
                              "recursion_limit": 4 * (limits.max_model_calls + limits.max_tool_calls) + 10,
                              "callbacks": [OutputBudget(run)],
                              "metadata": {"run_id": run.run_id}}
                    names = {}
                    signalled = set()
                    generating = False
                    stream = agent.astream({"messages": [{"role": "user", "content": question}]},
                                          config=config, stream_mode="messages")
                    try:
                        async for chunk, metadata in stream:
                            run.remaining()
                            if isinstance(chunk, AIMessageChunk):
                                for call in (chunk.tool_call_chunks or chunk.tool_calls):
                                    name = call.get("name") or ""
                                    if call.get("id") and name:
                                        names[call["id"]] = name
                                    marker = (call.get("id") or call.get("index"), name)
                                    if name and marker not in signalled:
                                        signalled.add(marker)
                                        await queue.put(SSE.state(state=SSE.REASONING, run_id=run.run_id))
                                        for word, state in _TOOL_STATES.items():
                                            if word in name.lower():
                                                search_info["split_pos"] = sum(map(len, reply))
                                                await queue.put(SSE.state(state=state))
                                                break
                                text = _text_content(chunk.content)
                                if text and not chunk.tool_calls and not chunk.tool_call_chunks:
                                    run.output_chars += len(text)
                                    if run.output_chars > limits.max_output_chars:
                                        raise HarnessError("output_limit", "回答超过长度预算，请缩小问题范围")
                                    reply.append(text)
                                    if not generating:
                                        generating = True
                                        await queue.put(SSE.state(state=SSE.GENERATING))
                                    await queue.put(text)
                            elif isinstance(chunk, ToolMessage):
                                name = chunk.name or names.get(chunk.tool_call_id, "")
                                if "knowledge" in name or "tavily" in name:
                                    results = _format_search_results(chunk.content)
                                    search_info["kb" if "knowledge" in name else "web"] = results.get("results", [])
                                    generating = False
                                    await queue.put(SSE.state(state=SSE.GENERATING, search_done=True, results=results))
                                else:
                                    await queue.put(SSE.state(state=SSE.THINKING))
                    finally:
                        await stream.aclose()
                run.remaining()
                full_reply = "".join(reply)
                if full_reply:
                    await workers.run(save_ai_message, thread_id, full_reply, search_info, user_id)
                run.emit("completed", model_calls=run.model_calls, tool_calls=run.tool_calls,
                         output_chars=run.output_chars, usage=run.usage)
            # 不在 timeout 的 finally 里保存半截回答，也不等待标题模型。
            if full_reply:
                _schedule_title(thread_id, question, full_reply, model, run)
            await queue.put(SSE.DONE)
        except asyncio.CancelledError:
            run.stopped = True
            run.emit("cancelled", publish_status=run.publish_status)
            raise
        except Exception as exc:
            code = exc.code if isinstance(exc, HarnessError) else (
                "task_timeout" if isinstance(exc, TimeoutError) else "busy" if isinstance(exc, WorkerBusy) else "failed")
            message = str(exc) if isinstance(exc, HarnessError) else {
                "task_timeout": "任务超过总时限，请稍后重试", "busy": "执行资源繁忙，请稍后重试",
            }.get(code, "任务执行失败，请稍后重试")
            if run.uncertain_publication():
                code, message = "publish_result_unknown", "发布结果未知，请检查文章列表，不要重复提交"
            elif run.publish_status == "succeeded":
                code, message = "published_reply_failed", "文章已发布，但后续回复中断；请查看文章列表，不要重复发布"
            run.stopped = True
            run.emit("failed", code=code, error_type=type(exc).__name__, publish_status=run.publish_status)
            await queue.put(SSE.ERROR + " " + message)

    producer = asyncio.create_task(produce(), name=f"agent:{run.run_id}")
    _producer_tasks.add(producer)
    producer.add_done_callback(_producer_tasks.discard)
    run.emit("started")
    try:
        while True:
            data = await queue.get()
            yield f"data: {_sse_escape(data)}\n\n"
            if data == SSE.DONE or data.startswith(SSE.ERROR):
                break
    finally:
        # Starlette 在断连时取消流生成器；取消生产任务并等待 graph/checkpointer 释放。
        run.stopped = True
        # Starlette 使用 AnyIO 的 level cancellation；屏蔽外层取消以完成资源回收。
        with anyio.CancelScope(shield=True):
            producer.cancel()
            with suppress(asyncio.CancelledError):
                await producer
        _active_runs.discard(key)
