# FastAPI 路由
# ============================================================
from fastapi import APIRouter, HTTPException
from models.schemas import CreateTaskRequest
from state.manager import get_state_manager

state_router = APIRouter(prefix="/api/tasks", tags=["Agent 任务状态"])


@state_router.post("", summary="创建 Agent 任务")
def api_create_task(body: CreateTaskRequest):
    """创建任务，返回 task_id 供后续 SSE 连接和状态轮询使用"""
    sm = get_state_manager()
    task_id = sm.create_task(
        question=body.question,
        conversation_id=body.thread_id,
    )
    return {"task_id": task_id, "conversation_id": body.thread_id}


@state_router.get("/{task_id}", summary="查询任务状态")
def api_get_task(task_id: str):
    """返回任务当前状态（前端轮询用）"""
    sm = get_state_manager()
    entry = sm.get_state(task_id)
    if not entry:
        raise HTTPException(404, "任务不存在")
    return {
        "task_id": entry["task_id"],
        "state": entry["state"],
        "progress": int(entry.get("progress", 0)),
        "tool_name": entry.get("tool_name", ""),
        "search_results": entry.get("search_results", ""),
        "error": entry.get("error", ""),
        "token_count": int(entry.get("token_count", 0)),
        "created_at": entry.get("created_at", ""),
        "updated_at": entry.get("updated_at", ""),
    }
