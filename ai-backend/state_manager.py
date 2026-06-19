"""
state_manager.py — Agent 状态管理器

分层：
  Hot Layer: Redis（实时读写，毫秒级）
  Cold Layer: SQLite（持久化，终结态落盘）

状态流转：
  pending → running → {searching_kb | searching_web | calling_tool}
       → generating → completed
       ↓              ↓
    failed        interrupted

断连恢复：前端通过 task_id 查询当前状态，决定重连还是重试。
"""

import json
import os
import sqlite3
import uuid
from datetime import datetime
from typing import Optional

import redis as _redis

DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)
TASKS_DB = os.path.join(DB_DIR, "tasks.db")

# 状态常量
class S:
    PENDING = "pending"
    RUNNING = "running"
    QUEUED = "queued"
    SEARCHING_KB = "searching_kb"
    SEARCHING_WEB = "searching_web"
    PUBLISHING = "publishing"
    CALLING_TOOL = "calling_tool"
    GENERATING = "generating"
    COMPLETED = "completed"
    FAILED = "failed"
    INTERRUPTED = "interrupted"
    ALL = [PENDING, QUEUED, RUNNING, SEARCHING_KB, SEARCHING_WEB, PUBLISHING,
           CALLING_TOOL, GENERATING, COMPLETED, FAILED, INTERRUPTED]

# 还在运行中的状态（非终结态）
ACTIVE_STATES = [S.PENDING, S.QUEUED, S.RUNNING, S.SEARCHING_KB, S.SEARCHING_WEB,
                 S.PUBLISHING, S.CALLING_TOOL, S.GENERATING]


class StateManager:
    """Agent 状态管理器"""

    def __init__(self, redis_url: str = "redis://localhost:6379/0"):
        self._r = _redis.from_url(redis_url, decode_responses=True)
        self._init_db()

    # ------------------------------------------------------------------
    # SQLite 持久化
    # ------------------------------------------------------------------

    def _init_db(self):
        conn = sqlite3.connect(TASKS_DB)
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("""
            CREATE TABLE IF NOT EXISTS agent_tasks (
                task_id         TEXT PRIMARY KEY,
                conversation_id TEXT NOT NULL DEFAULT '',
                user_id         TEXT NOT NULL DEFAULT '',
                question        TEXT NOT NULL DEFAULT '',
                state           TEXT NOT NULL DEFAULT 'pending',
                progress        INTEGER DEFAULT 0,
                tool_name       TEXT DEFAULT '',
                tool_args       TEXT DEFAULT '',
                search_results  TEXT DEFAULT '',  -- JSON
                answer          TEXT DEFAULT '',  -- 最终回答全文
                error           TEXT DEFAULT '',
                token_count     INTEGER DEFAULT 0,
                created_at      TEXT NOT NULL,
                updated_at      TEXT NOT NULL
            )
        """)
        conn.commit()
        conn.close()

    def _upsert_db(self, entry: dict):
        conn = sqlite3.connect(TASKS_DB)
        conn.execute("""
            INSERT INTO agent_tasks
                (task_id, conversation_id, user_id, question, state, progress,
                 tool_name, tool_args, search_results, answer, error,
                 token_count, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(task_id) DO UPDATE SET
                state=excluded.state, progress=excluded.progress,
                tool_name=excluded.tool_name, tool_args=excluded.tool_args,
                search_results=excluded.search_results,
                answer=excluded.answer, error=excluded.error,
                token_count=excluded.token_count,
                updated_at=excluded.updated_at
        """, (
            entry["task_id"], entry.get("conversation_id", ""),
            entry.get("user_id", ""), entry.get("question", ""),
            entry["state"], entry.get("progress", 0),
            entry.get("tool_name", ""), entry.get("tool_args", ""),
            entry.get("search_results", ""), entry.get("answer", ""),
            entry.get("error", ""), entry.get("token_count", 0),
            entry.get("created_at", datetime.now().isoformat()),
            entry["updated_at"],
        ))
        conn.commit()
        conn.close()

    # ------------------------------------------------------------------
    # Redis 缓存（热数据）
    # ------------------------------------------------------------------

    def _rk(self, task_id: str) -> str:
        return f"agent:task:{task_id}"

    def _rkh(self, task_id: str, field: str) -> str:
        return f"agent:task:{task_id}:{field}"

    # ------------------------------------------------------------------
    # 公开 API
    # ------------------------------------------------------------------

    def create_task(self, question: str, user_id: str = "",
                    conversation_id: str = "") -> str:
        """创建任务，返回 task_id"""
        task_id = str(uuid.uuid4())
        now = datetime.now().isoformat()
        entry = {
            "task_id": task_id,
            "conversation_id": conversation_id,
            "user_id": user_id,
            "question": question,
            "state": S.PENDING,
            "progress": 0,
            "tool_name": "",
            "tool_args": "",
            "search_results": "",
            "answer": "",
            "error": "",
            "token_count": 0,
            "created_at": now,
            "updated_at": now,
        }
        # 写 Redis
        self._r.hset(self._rk(task_id), mapping={
            k: (json.dumps(v) if isinstance(v, (dict, list)) else str(v))
            for k, v in entry.items()
        })
        self._r.expire(self._rk(task_id), 86400)  # 24h 自动过期
        # 写 SQLite
        self._upsert_db(entry)
        return task_id

    def update(self, task_id: str, **kwargs):
        """更新中间状态（仅写 Redis），终结态请用 finalize()"""
        if not task_id:
            return None

        now = datetime.now().isoformat()
        kwargs["updated_at"] = now

        mapping = {}
        for k, v in kwargs.items():
            if isinstance(v, (dict, list)):
                mapping[k] = json.dumps(v, ensure_ascii=False)
            else:
                mapping[k] = str(v) if v is not None else ""

        if mapping:
            mapping["updated_at"] = now
            self._r.hset(self._rk(task_id), mapping=mapping)
            self._r.expire(self._rk(task_id), 86400)
        return self.get_state(task_id)

    def finalize(self, task_id: str, **kwargs):
        """写入终结态：Redis + SQLite 双写"""
        if not task_id:
            return None

        self.update(task_id, **kwargs)
        entry = self.get_state(task_id)
        if entry:
            self._upsert_db(entry)
        return entry

    def get_state(self, task_id: str) -> Optional[dict]:
        """从 Redis 读（热），miss 则从 SQLite 读"""
        raw = self._r.hgetall(self._rk(task_id))
        if raw:
            return {k: v for k, v in raw.items()}
        # Redis miss → 从 SQLite 读
        conn = sqlite3.connect(TASKS_DB)
        conn.row_factory = sqlite3.Row
        row = conn.execute(
            "SELECT * FROM agent_tasks WHERE task_id = ?", (task_id,)
        ).fetchone()
        conn.close()
        if row:
            entry = dict(row)
            # 写回 Redis（缓存预热）
            self._r.hset(self._rk(task_id), mapping=entry)
            self._r.expire(self._rk(task_id), 86400)
            return entry
        return None

    def append_token(self, task_id: str, token: str):
        """累积 token（Redis 里维护当前已产生的回答）"""
        if not task_id or not token:
            return
        key = self._rkh(task_id, "answer")
        self._r.append(key, token)
        self._r.expire(self._rk(task_id), 86400)
        # 同时更新 token_count
        self._r.hincrby(self._rk(task_id), "token_count", 1)

    def set_search_results(self, task_id: str, results: list, search_type: str):
        """保存搜索结果到 Redis + DB"""
        self.update(task_id,
                    search_results=json.dumps({"type": search_type, "results": results},
                                               ensure_ascii=False))

    def get_active_tasks(self, user_id: str) -> list[dict]:
        """获取用户当前进行中的任务"""
        conn = sqlite3.connect(TASKS_DB)
        conn.row_factory = sqlite3.Row
        rows = conn.execute(
            "SELECT * FROM agent_tasks WHERE user_id = ? AND state IN ('pending','running','searching_kb','searching_web','calling_tool','generating') ORDER BY created_at DESC",
            (user_id,)
        ).fetchall()
        conn.close()
        return [dict(r) for r in rows]

    # ------------------------------------------------------------------
    # 清理
    # ------------------------------------------------------------------

    def clean_expired(self):
        """清理过期任务（Redis 自动过期，这里清理 SQLite 中 7 天前的已完成任务）"""
        conn = sqlite3.connect(TASKS_DB)
        conn.execute(
            "DELETE FROM agent_tasks WHERE state IN ('completed','failed') AND updated_at < datetime('now', '-7 days')"
        )
        conn.commit()
        conn.close()


# 全局单例
_manager: Optional[StateManager] = None


def get_state_manager() -> StateManager:
    global _manager
    if _manager is None:
        _manager = StateManager()
    return _manager


# ============================================================
# FastAPI 路由
# ============================================================
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

state_router = APIRouter(prefix="/api/tasks", tags=["Agent 任务状态"])


class CreateTaskRequest(BaseModel):
    question: str = Field(..., description="用户问题")
    thread_id: str = Field(default="default", description="会话 ID")


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
