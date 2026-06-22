"""Agent SSE 流式生成 — _stream_chat + 辅助函数"""

import asyncio
import os
from concurrent.futures import ThreadPoolExecutor

from langchain.agents import create_agent
from langchain_core.messages import AIMessageChunk, ToolMessage
from langgraph.checkpoint.sqlite import SqliteSaver

from state.manager import S, get_state_manager
from common.logger import get_agent_logger

DB_DIR = os.path.join(os.path.dirname(__file__), "db")
DB_PATH = os.path.join(DB_DIR, "conversations.db")
SYSTEM_PROMPT_PATH = os.path.join(os.path.dirname(__file__), "..", "SystemPrompt.md")
os.makedirs(DB_DIR, exist_ok=True)


def load_system_prompt() -> str:
    try:
        with open(SYSTEM_PROMPT_PATH, "r", encoding="utf-8") as f:
            return f.read().strip()
    except FileNotFoundError:
        return "你是一个协会网站的智能客服助手。"


# SSE 信号常量
import json as _json

class SSE:
    DONE = "[DONE]"
    ERROR = "[ERROR]"
    @staticmethod
    def state(**kw) -> str:
        return "[STATE]" + _json.dumps(kw, ensure_ascii=False)


def _sse_escape(text: str) -> str:
    return text.replace("\n", "\\n")


def _format_search_results(tool_content: str) -> str:
    raw = tool_content if isinstance(tool_content, str) else str(tool_content)
    if raw.startswith("[KB]"):
        try:
            return raw[4:].split("\n---\n", 1)[0]
        except Exception:
            return _json.dumps({"results": []})
    try:
        parsed = _json.loads(raw)
    except Exception:
        return _json.dumps({"results": []})
    if isinstance(parsed, dict) and "results" in parsed:
        parsed = parsed["results"]
    if isinstance(parsed, list):
        results = []
        for item in parsed:
            if isinstance(item, dict):
                results.append({
                    "url": item.get("url", ""),
                    "title": item.get("title", ""),
                    "snippet": (item.get("content", "") or "")[:120],
                })
        return _json.dumps({"results": results}, ensure_ascii=False)
    return _json.dumps({"results": []})


def _parse_kb_results(raw: str, info: dict):
    try:
        if raw.startswith("[KB]"):
            json_str = raw[4:].split("\n---\n", 1)[0]
            info["kb"] = __import__('json').loads(json_str).get("results", [])
    except Exception:
        pass


def _parse_web_results(raw: str, info: dict):
    try:
        parsed = __import__('json').loads(raw) if raw.startswith("{") else None
        if parsed and isinstance(parsed, dict) and "results" in parsed:
            info["web"] = [
                {"url": r.get("url", ""), "title": r.get("title", ""), "snippet": (r.get("content", "") or "")[:120]}
                for r in parsed["results"] if isinstance(r, dict)
            ]
    except Exception:
        pass


async def stream_chat(question: str, thread_id: str, user_id: str = "",
                      token: str = "", task_id: str = "",
                      model=None, tools: list = None):
    """
    SSE 流式生成器。model 和 tools 由 main.py 传入。
    """
    from conversation.service import ensure_conversation_and_save_user, save_ai_message, generate_and_save_title

    thread_id = ensure_conversation_and_save_user(thread_id, question, user_id)
    sm = get_state_manager()
    alog = get_agent_logger()

    if not task_id:
        task_id = sm.create_task(question=question, user_id=user_id, conversation_id=thread_id)
    else:
        sm.update(task_id, state=S.RUNNING, question=question, progress=5)

    queue: asyncio.Queue = asyncio.Queue()
    loop = asyncio.get_running_loop()
    ai_reply_chunks: list[str] = []
    search_info: dict = {}

    def run_agent():
        nonlocal ai_reply_chunks
        checkpointer_ctx = SqliteSaver.from_conn_string(DB_PATH)
        checkpointer = checkpointer_ctx.__enter__()
        try:
            sm.update(task_id, state=S.RUNNING, progress=5)
            # alog.info(task_id, 0, "running", "任务开始", {"question": question[:80]})

            agent = create_agent(
                model=model,
                tools=tools or [],
                checkpointer=checkpointer,
                system_prompt=load_system_prompt(),
            )
            config = {"configurable": {"thread_id": f"{user_id}:{thread_id}", "token": token, "user_id": user_id}}
            step = 0
            searching_sent = False
            gen_sent = False
            current_tool_name = ""
            has_reasoned = False
            _thinking = []
            for chunk, metadata in agent.stream(
                    {"messages": [{"role": "user", "content": question}]},
                    config=config,
                    stream_mode="messages"):
                if isinstance(chunk, AIMessageChunk):
                    # 收集模型推理过程日志（累积后统一输出）
                    _rc = chunk.additional_kwargs.get("reasoning_content", "")
                    if _rc:
                        _thinking.append(_rc)
                    elif _thinking:
                        _full = "".join(_thinking)
                        alog.info(task_id, step, "model_thinking", "模型推理", {"think": _full[:500]})
                        print(f"  [思考] {_full[:500]}", flush=True)
                        _thinking.clear()
                    if (chunk.tool_calls or chunk.tool_call_chunks) and not searching_sent:
                        if chunk.tool_call_chunks:
                            tc = chunk.tool_call_chunks[0]
                            if isinstance(tc, dict):
                                name = tc.get("name") or ""
                            else:
                                name = getattr(tc, "name", "") or ""
                            if name:
                                current_tool_name = name
                        step += 1
                        if not has_reasoned:
                            has_reasoned = True
                            sm.update(task_id, state=S.REASONING, progress=20)
                            loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='reasoning', step=step))
                            # alog.info(task_id, step, "reasoning", "LLM推理决策")
                        _lower = current_tool_name.lower()
                        if "knowledge" in _lower or "tavily" in _lower or "publish" in _lower:
                            searching_sent = True
                            search_info["split_pos"] = len("".join(ai_reply_chunks))
                            if "knowledge" in _lower:
                                tool_state = S.SEARCHING_KB
                            elif "tavily" in _lower:
                                tool_state = S.SEARCHING_WEB
                            else:
                                tool_state = S.PUBLISHING
                            sm.update(task_id, state=tool_state, tool_name=current_tool_name, progress=30)
                            loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state=tool_state))
                            alog.info(task_id, step, tool_state, "调用工具", {"tool": current_tool_name})
                    if chunk.content and not chunk.tool_calls and not chunk.tool_call_chunks:
                        ai_reply_chunks.append(chunk.content)
                        sm.append_token(task_id, chunk.content)
                        sm.update(task_id, state=S.GENERATING, progress=70)
                        if not gen_sent:
                            gen_sent = True
                            loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='generating'))
                            # alog.info(task_id, step, "generating", "开始生成回答")
                        loop.call_soon_threadsafe(queue.put_nowait, chunk.content)
                elif isinstance(chunk, ToolMessage) or type(chunk).__name__ == "ToolMessage":
                    _is_search_tool = ("knowledge" in current_tool_name.lower() or "tavily" in current_tool_name.lower())
                    current_tool_name = ""
                    if not _is_search_tool:
                        step += 1
                        sm.update(task_id, state=S.THINKING, progress=50)
                        loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='thinking', step=step))
                        continue
                    step += 1
                    searching_sent = False
                    raw = str(chunk.content)
                    if raw.startswith("[KB]"):
                        _parse_kb_results(raw, search_info)
                        sm.set_search_results(task_id, search_info.get("kb", []), "kb")
                    else:
                        _parse_web_results(raw, search_info)
                        sm.set_search_results(task_id, search_info.get("web", []), "web")
                    sm.update(task_id, progress=60)
                    gen_sent = False
                    results_json = _format_search_results(chunk.content)
                    loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='generating', search_done=True, results=_json.loads(results_json)))
                    alog.info(task_id, step, "search_done", "检索完成", {"hit_count": search_info.get("kb", []) or search_info.get("web", [])})
        except Exception as e:
            error_msg = str(e)
            alog.error(task_id, step, "failed", "任务异常", {"error": error_msg[:200]})
            sm.finalize(task_id, state=S.FAILED, error=error_msg)
            if "insufficient tool messages" in error_msg or "tool_calls" in error_msg:
                loop.call_soon_threadsafe(queue.put_nowait, "[ERROR] 网络错误，请刷新页面重试")
            else:
                loop.call_soon_threadsafe(queue.put_nowait, f"[ERROR] {e}")
        finally:
            try:
                checkpointer_ctx.__exit__(None, None, None)
            except Exception:
                pass
            final_state = S.COMPLETED if sm.get_state(task_id).get("state") != S.FAILED else S.FAILED
            sm.finalize(task_id, state=final_state, progress=100)
            # alog.info(task_id, step, final_state, "任务完成", {"tokens": len(ai_reply_chunks)})
            loop.call_soon_threadsafe(queue.put_nowait, None)

    executor = ThreadPoolExecutor(max_workers=1)
    executor.submit(run_agent)

    while True:
        data = await queue.get()
        if data is None:
            full_reply = "".join(ai_reply_chunks)
            if full_reply:
                save_ai_message(thread_id, full_reply, search_info, user_id)
                generate_and_save_title(thread_id, question, full_reply, model)
            yield f"data: {SSE.DONE}\n\n"
            break
        if str(data).startswith("[ERROR]"):
            yield f"data: {data}\n\n"
            break
        yield f"data: {_sse_escape(data)}\n\n"