"""最小 Agent Harness：单次运行预算、超时、工具边界和实际上下文追踪。"""

import asyncio
from dataclasses import dataclass, field, fields
import hashlib
import json
import math
import os
import time
import uuid

import httpx
from langchain.agents.middleware import AgentMiddleware
from langchain_core.callbacks import AsyncCallbackHandler
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
from langchain_core.tools import StructuredTool
from pydantic import ValidationError

from agent.workers import WorkerBusy, workers
from common.logger import get_agent_logger


@dataclass(frozen=True)
class Limits:
    total_seconds: float = 120
    model_seconds: float = 45
    tool_seconds: float = 15
    title_seconds: float = 5
    max_model_calls: int = 6
    max_tool_calls: int = 8
    max_input_chars: int = 32000
    max_output_chars: int = 24000
    model_output_tokens: int = 4096
    max_active_runs: int = 4
    stream_buffer: int = 32

    def __post_init__(self):
        for f in fields(self):
            value = getattr(self, f.name)
            if not math.isfinite(value) or value <= 0:
                raise ValueError(f"{f.name} 必须为有限正数")
            if f.type is int and not isinstance(value, int):
                raise ValueError(f"{f.name} 必须为整数")

    @classmethod
    def from_env(cls):
        return cls(**{
            f.name: (int if f.type is int else float)(os.environ[f"AGENT_{f.name.upper()}"])
            for f in fields(cls) if f"AGENT_{f.name.upper()}" in os.environ
        })


class HarnessError(RuntimeError):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code


def tool_error(name: str, call_id: str, code: str, message: str) -> ToolMessage:
    return ToolMessage(
        content=json.dumps({"ok": False, "error": {"code": code, "message": message}}, ensure_ascii=False),
        tool_call_id=call_id, name=name, status="error",
    )


def bounded_messages(messages, max_chars: int):
    """按完整用户轮次截断；不拆分 AI tool_calls 与 ToolMessage 协议。"""
    # 失败/取消的 checkpoint 可能留下未完成的工具请求，不将其带进下一次模型输入。
    cleaned = []
    pending = None
    responses = []
    for message in messages:
        if isinstance(message, AIMessage) and message.tool_calls:
            pending, responses = message, []
        elif isinstance(message, ToolMessage):
            if pending and message.tool_call_id in {c["id"] for c in pending.tool_calls}:
                responses.append(message)
                if {m.tool_call_id for m in responses} == {c["id"] for c in pending.tool_calls}:
                    cleaned.extend([pending, *responses])
                    pending, responses = None, []
        else:
            pending, responses = None, []
            cleaned.append(message)
    groups = []
    for message in cleaned:
        if isinstance(message, HumanMessage) or not groups:
            groups.append([])
        groups[-1].append(message)
    selected, size = [], 0
    for group in reversed(groups):
        cost = sum(len(json.dumps({
            "role": m.type, "content": m.content, "name": m.name,
            "tool_calls": getattr(m, "tool_calls", []),
            "tool_call_id": getattr(m, "tool_call_id", None),
        }, ensure_ascii=False)) for m in group)
        if size + cost > max_chars:
            if not selected:
                raise HarnessError("context_limit", "本轮上下文过长，请缩短问题或开启新会话")
            break
        selected = group + selected
        size += cost
    return selected


@dataclass
class RunState:
    limits: Limits
    run_id: str = field(default_factory=lambda: uuid.uuid4().hex)
    started: float = field(default_factory=time.monotonic)
    model_calls: int = 0
    tool_calls: int = 0
    output_chars: int = 0
    generated_chars: int = 0
    usage: dict = field(default_factory=dict)
    tools: list = field(default_factory=list)
    # 仅在内存用于离线评估；普通日志不输出问题、原文或完整工具结果。
    retrievals: list = field(default_factory=list)
    model_contexts: list = field(default_factory=list)
    publish_status: str = "not_started"
    stopped: bool = False

    def remaining(self):
        seconds = self.limits.total_seconds - (time.monotonic() - self.started)
        if self.stopped:
            raise HarnessError("cancelled", "任务已取消")
        if seconds <= 0:
            raise HarnessError("task_timeout", "任务超过总时限，请稍后重试")
        return seconds

    def emit(self, state: str, **data):
        get_agent_logger().info(self.tool_calls + self.model_calls, state, "Agent 执行", {
            "run_id": self.run_id, "elapsed_ms": round((time.monotonic() - self.started) * 1000), **data,
        })

    def uncertain_publication(self):
        return self.publish_status in {"in_flight", "unknown"}


class OutputBudget(AsyncCallbackHandler):
    """在模型生产端计数，不等到慢 SSE 消费端读到 token 才截断。"""
    raise_error = True

    def __init__(self, run: RunState):
        self.run = run

    async def on_llm_new_token(self, token, **kwargs):
        self.run.remaining()
        size = len(token) if isinstance(token, str) else len(json.dumps(token, ensure_ascii=False))
        self.run.generated_chars += size
        if self.run.generated_chars > self.run.limits.max_output_chars:
            raise HarnessError("output_limit", "回答超过长度预算，请缩小问题范围")


class HarnessMiddleware(AgentMiddleware):
    def __init__(self, run: RunState):
        self.run = run

    async def awrap_model_call(self, request, handler):
        run = self.run
        remaining = run.remaining()
        if run.model_calls >= run.limits.max_model_calls:
            raise HarnessError("model_call_limit", "模型调用次数已达上限，请缩小问题范围")
        run.model_calls += 1
        # 包含系统提示词与工具定义的字符开销；不是精确 tokenizer 计数。
        overhead = len(request.system_message.model_dump_json()) if request.system_message else 0
        overhead += sum(len(json.dumps(t if isinstance(t, dict) else {
                            "name": t.name, "description": t.description, "parameters": t.args,
                        }, ensure_ascii=False)) for t in request.tools)
        budget = run.limits.max_input_chars - overhead
        if budget <= 0:
            raise HarnessError("context_limit", "系统提示词和工具定义超过上下文预算")
        messages = bounded_messages(request.messages, budget)
        run.model_contexts.append({"call": run.model_calls, "messages": [m.model_copy(deep=True) for m in messages]})
        started = time.monotonic()
        generated_before = run.generated_chars
        run.emit("model_started", model_call=run.model_calls)
        try:
            async with asyncio.timeout(min(remaining, run.limits.model_seconds)):
                settings = dict(request.model_settings)
                settings["max_tokens"] = min(settings.get("max_tokens") or run.limits.model_output_tokens,
                                             run.limits.model_output_tokens)
                response = await handler(request.override(messages=messages, model_settings=settings))
        except TimeoutError as exc:
            raise HarnessError("model_timeout", "模型响应超时，请稍后重试") from exc
        response_chars = sum(
            len(message.content) if isinstance(message.content, str)
            else len(json.dumps(message.content, ensure_ascii=False))
            for message in getattr(response, "result", [])
        )
        # 非流式模型（如离线评估）不一定触发 token 回调，也必须受累计输出预算约束。
        run.generated_chars = max(run.generated_chars, generated_before + response_chars)
        if run.generated_chars > run.limits.max_output_chars:
            raise HarnessError("output_limit", "回答超过长度预算，请缩小问题范围")
        for message in getattr(response, "result", []):
            for key, value in (getattr(message, "usage_metadata", None) or {}).items():
                if isinstance(value, int):
                    run.usage[key] = run.usage.get(key, 0) + value
        run.emit("model_finished", duration_ms=round((time.monotonic() - started) * 1000), usage=run.usage)
        return response

    async def awrap_tool_call(self, request, handler):
        run = self.run
        remaining = run.remaining()
        if run.tool_calls >= run.limits.max_tool_calls:
            raise HarnessError("tool_call_limit", "工具调用次数已达上限，请缩小问题范围")
        run.tool_calls += 1
        name, call_id = request.tool_call["name"], request.tool_call["id"]
        publishing = name == "publish_article"
        if publishing:
            if run.publish_status != "not_started":
                raise HarnessError("publish_repeat_blocked", "本轮已尝试发布，不会重复提交；请检查文章列表")
            run.publish_status = "in_flight"
        step = run.tool_calls
        started = time.monotonic()
        record = {"step": step, "name": name, "call_id": call_id, "status": "started"}
        run.tools.append(record)
        run.emit("tool_started", tool=name, tool_step=step)
        try:
            if publishing and request.tool is not None:
                # ToolNode 可能把 schema 错误转为文本 ToolMessage；写操作在发请求前显式校验。
                request.tool.args_schema.model_validate(request.tool_call.get("args", {}))
            async with asyncio.timeout(min(remaining, run.limits.tool_seconds)):
                if isinstance(request.tool, StructuredTool) and request.tool.coroutine is None:
                    # 本项目的同步工具只注入 RunnableConfig；调用原工具仍执行 schema 校验。
                    result = await workers.run(request.tool.invoke, request.tool_call, config=request.runtime.config)
                else:
                    result = await handler(request)
            # 有些第三方工具把异常吞成 {error: ...}，不能当成功或把原始异常交给模型。
            if isinstance(result, ToolMessage) and isinstance(result.content, str) and not result.content.startswith("[KB]"):
                try:
                    payload = json.loads(result.content)
                except ValueError:
                    payload = None
                if ((isinstance(payload, dict) and "error" in payload and "ok" not in payload)
                        or result.content.startswith("{'error':")):
                    result = tool_error(name, call_id, "dependency_unavailable", "工具依赖服务暂不可用")
                elif isinstance(payload, dict) and payload.get("ok") is False:
                    result = result.model_copy(update={"status": "error"})
            record["status"] = "error" if getattr(result, "status", None) == "error" else "success"
            artifact = getattr(result, "artifact", None)
            if name == "search_knowledge_base" and isinstance(artifact, dict):
                record["status"] = artifact.get("status", record["status"])
                run.retrievals.append(artifact)
                run.emit("retrieval_finished", tool_step=step, status=artifact.get("status"),
                         query_hash=hashlib.sha256(artifact.get("query", "").encode()).hexdigest()[:16],
                         selected_ids=[f"{h['doc_id']}:{h['chunk_idx']}" for h in artifact.get("selected", [])])
            if publishing:
                # HTTP 工具以结构化结果标记结果未知，不能让模型换参数再次提交。
                payload = json.loads(result.content)
                run.publish_status = "succeeded" if payload.get("ok") else (
                    "unknown" if payload.get("error", {}).get("code") == "result_unknown" else "failed")
                if run.publish_status == "unknown":
                    raise HarnessError("publish_result_unknown", "发布结果未知，请检查文章列表，不要重复提交")
            return result
        except asyncio.CancelledError:
            record["status"] = "cancelled"
            if publishing:
                run.publish_status = "unknown"
            raise
        except HarnessError:
            record["status"] = "error"
            raise
        except Exception as exc:
            code = "tool_failed"
            if isinstance(exc, (TimeoutError, httpx.TimeoutException)):
                code = "tool_timeout"
            elif isinstance(exc, WorkerBusy):
                code = "busy"
            elif isinstance(exc, ValidationError):
                code = "invalid_arguments"
            elif isinstance(exc, httpx.HTTPStatusError):
                code = "unauthorized" if exc.response.status_code in (401, 403) else "dependency_unavailable"
            elif isinstance(exc, httpx.RequestError):
                code = "dependency_unavailable"
            record["status"] = code
            if publishing:
                # 未发请求的参数错误可明确失败；其余保守地视为结果未知并终止。
                run.publish_status = "failed" if code == "invalid_arguments" else "unknown"
                raise HarnessError("publish_result_unknown" if run.uncertain_publication() else code,
                                   "发布结果未知，请检查文章列表，不要重复提交" if run.uncertain_publication()
                                   else "发布参数不合法，本轮未继续提交") from exc
            return tool_error(name, call_id, code, {
                "tool_timeout": "工具执行超时", "busy": "检索资源繁忙，请稍后重试",
                "invalid_arguments": "工具参数不合法", "unauthorized": "登录已失效",
                "dependency_unavailable": "依赖服务暂不可用",
            }.get(code, "工具执行失败"))
        finally:
            record["duration_ms"] = round((time.monotonic() - started) * 1000)
            run.emit("tool_finished", **record)
