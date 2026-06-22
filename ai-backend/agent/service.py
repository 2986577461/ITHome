"""Agent SSE 流式生成 — _stream_chat + 辅助函数"""

import asyncio
import json
import os
from concurrent.futures import ThreadPoolExecutor

from langchain.agents import create_agent
from langchain_core.messages import AIMessageChunk, ToolMessage
from langgraph.checkpoint.sqlite import SqliteSaver

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
class SSE:
    DONE = "[DONE]"
    ERROR = "[ERROR]"
    @staticmethod
    def state(**kw) -> str:
        return "[STATE]" + json.dumps(kw, ensure_ascii=False)


def _sse_escape(text: str) -> str:
    return text.replace("\n", "\\n")


def _format_search_results(tool_content: str) -> str:
    raw = tool_content if isinstance(tool_content, str) else str(tool_content)
    if raw.startswith("[KB]"):
        try:
            return raw[4:].split("\n---\n", 1)[0]
        except Exception:
            return json.dumps({"results": []})
    try:
        parsed = json.loads(raw)
    except Exception:
        return json.dumps({"results": []})
    if isinstance(parsed, dict) and "results" in parsed:
        parsed = parsed["results"]
    if isinstance(parsed, list):
        results = [
            {"url": r.get("url", ""), "title": r.get("title", ""),
             "snippet": (r.get("content", "") or "")[:120]}
            for r in parsed if isinstance(r, dict)
        ]
        return json.dumps({"results": results}, ensure_ascii=False)
    return json.dumps({"results": []})


def _parse_kb_results(raw: str, info: dict):
    try:
        if raw.startswith("[KB]"):
            info["kb"] = json.loads(raw[4:].split("\n---\n", 1)[0]).get("results", [])
    except Exception:
        pass


def _parse_web_results(raw: str, info: dict):
    try:
        parsed = json.loads(raw) if raw.startswith("{") else None
        if parsed and "results" in parsed:
            info["web"] = [
                {"url": r.get("url", ""), "title": r.get("title", ""),
                 "snippet": (r.get("content", "") or "")[:120]}
                for r in parsed["results"] if isinstance(r, dict)
            ]
    except Exception:
        pass


async def stream_chat(question: str, thread_id: str, user_id: str = "",
                      token: str = "", task_id: str = "",
                      model=None, tools: list = None):
    """SSE 流式生成器。model 和 tools 由 main.py 传入。"""
    from conversation.service import ensure_conversation_and_save_user, save_ai_message, generate_and_save_title

    thread_id = ensure_conversation_and_save_user(thread_id, question, user_id)
    alog = get_agent_logger()

    queue: asyncio.Queue = asyncio.Queue()
    loop = asyncio.get_running_loop()
    ai_reply_chunks: list[str] = []
    search_info: dict = {}

    def run_agent():
        nonlocal ai_reply_chunks
        checkpointer_ctx = SqliteSaver.from_conn_string(DB_PATH)
        checkpointer = checkpointer_ctx.__enter__()
        try:
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
                    # 收集模型推理过程日志
                    _rc = chunk.additional_kwargs.get("reasoning_content", "")
                    if _rc:
                        _thinking.append(_rc)
                    elif _thinking:
                        _full = "".join(_thinking)
                        alog.info( step, "model_thinking", "模型推理", {"think": _full[:500]})
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
                            loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='reasoning', step=step))
                        _lower = current_tool_name.lower()
                        if "knowledge" in _lower or "tavily" in _lower or "publish" in _lower:
                            searching_sent = True
                            search_info["split_pos"] = len("".join(ai_reply_chunks))
                            tool_state = "searching_kb" if "knowledge" in _lower else "searching_web" if "tavily" in _lower else "publishing"
                            loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state=tool_state))
                            alog.info( step, tool_state, "调用工具", {"tool": current_tool_name})
                    if chunk.content and not chunk.tool_calls and not chunk.tool_call_chunks:
                        ai_reply_chunks.append(chunk.content)
                        if not gen_sent:
                            gen_sent = True
                            loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='generating'))
                        loop.call_soon_threadsafe(queue.put_nowait, chunk.content)
                elif isinstance(chunk, ToolMessage) or type(chunk).__name__ == "ToolMessage":
                    _is_search_tool = ("knowledge" in current_tool_name.lower() or "tavily" in current_tool_name.lower())
                    current_tool_name = ""
                    if not _is_search_tool:
                        step += 1
                        loop.call_soon_threadsafe(queue.put_nowait, SSE.state(state='thinking', step=step))
                        continue
                    step += 1
                    searching_sent = False
                    raw = str(chunk.content)
                    if raw.startswith("[KB]"):
                        _parse_kb_results(raw, search_info)
                    else:
                        _parse_web_results(raw, search_info)
                    gen_sent = False
                    results_json = _format_search_results(chunk.content)
                    loop.call_soon_threadsafe(queue.put_nowait,
                        SSE.state(state='generating', search_done=True, results=json.loads(results_json)))
                    alog.info( step, "search_done", "检索完成",
                              {"hit_count": search_info.get("kb", []) or search_info.get("web", [])})
        except Exception as e:
            error_msg = str(e)
            alog.error( step, "failed", "任务异常", {"error": error_msg[:200]})
            if "insufficient tool messages" in error_msg or "tool_calls" in error_msg:
                loop.call_soon_threadsafe(queue.put_nowait, "[ERROR] 网络错误，请刷新页面重试")
            else:
                loop.call_soon_threadsafe(queue.put_nowait, f"[ERROR] {e}")
        finally:
            try:
                checkpointer_ctx.__exit__(None, None, None)
            except Exception:
                pass
            # 无论 SSE 是否断开，都在线程内保存消息
            try:
                _full = "".join(ai_reply_chunks)
                if _full:
                    save_ai_message(thread_id, _full, search_info, user_id)
                    generate_and_save_title(thread_id, question, _full, model)
            except Exception:
                pass
            alog.info( step, "completed", "任务完成", {"tokens": len(ai_reply_chunks)})
            loop.call_soon_threadsafe(queue.put_nowait, None)

    executor = ThreadPoolExecutor(max_workers=1)
    executor.submit(run_agent)

    while True:
        data = await queue.get()
        if data is None:
            yield f"data: {SSE.DONE}\n\n"
            break
        if str(data).startswith("[ERROR]"):
            yield f"data: {data}\n\n"
            break
        yield f"data: {_sse_escape(data)}\n\n"