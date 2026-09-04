"""
会话管理模块 — 会话 CRUD、消息存储、自动标题生成、JWT 鉴权

本模块通过 FastAPI APIRouter 注册路由，在 Agent.py 中用 app.include_router() 挂载。
"""

import os
import sqlite3
import uuid
from datetime import datetime

from dotenv import load_dotenv
from fastapi import APIRouter, HTTPException, Header, Query, Depends
from pydantic import BaseModel, Field

load_dotenv()

# ---- 数据库路径 ----
DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)
APP_DB_PATH = os.path.join(DB_DIR, "app.db")


# ============================================================
# 鉴权：委托 business_client 验证 token
# ============================================================
from common.business_client import business_client


async def get_current_user(authorization: str = Header(default=""),
                           token: str = Query(default="")) -> str:
    """
    拿着 token 请求业务模块 GET /user/users，取 data.studentId 作为 user_id。
    FastAPI 支持 async Depends，同步路由也能用。
    """
    raw = ""
    if authorization:
        raw = authorization
    elif token:
        raw = token
    if not raw:
        raise HTTPException(401, "请先登录")

    try:
        result = await business_client.get_user_info(raw)
    except Exception:
        raise HTTPException(401, "身份验证失败，请重新登录")

    user_data = (result.get("data") or {}) if isinstance(result, dict) else {}
    student_id = user_data.get("studentId", "")
    if not student_id:
        raise HTTPException(401, "身份验证失败：缺少 studentId")

    return str(student_id)


# ============================================================
# 数据库初始化
# ============================================================
def init_app_db():
    """创建 conversations / messages 表，兼容旧表自动加列"""
    conn = sqlite3.connect(APP_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS conversations
                 (
                     thread_id  TEXT PRIMARY KEY,
                     title      TEXT DEFAULT '新的对话',
                     created_at TEXT NOT NULL,
                     updated_at TEXT NOT NULL
                 )
                 """)
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS messages
                 (
                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                     thread_id   TEXT NOT NULL,
                     role        TEXT NOT NULL CHECK (role IN ('user', 'ai')),
                     content     TEXT NOT NULL,
                     search_info TEXT,
                     created_at  TEXT NOT NULL,
                     FOREIGN KEY (thread_id) REFERENCES conversations (thread_id) ON DELETE CASCADE
                 )
                 """)
    for col, col_type in [("search_info", "TEXT"), ("user_id", "TEXT DEFAULT ''")]:
        try:
            conn.execute(f"ALTER TABLE messages ADD COLUMN {col} {col_type}")
        except sqlite3.OperationalError:
            pass
    try:
        conn.execute("ALTER TABLE conversations ADD COLUMN user_id TEXT DEFAULT ''")
    except sqlite3.OperationalError:
        pass
    conn.commit()
    conn.close()


def get_app_db() -> sqlite3.Connection:
    """获取应用数据库连接（自动开启 WAL + 外键，Row 工厂）"""
    conn = sqlite3.connect(APP_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.row_factory = sqlite3.Row
    return conn


# 启动时建表
init_app_db()



class CreateConversationRequest(BaseModel):
    title: str = Field(default="新的对话", description="会话标题")


# ============================================================
# 会话管理辅助函数（供 _stream_chat 使用）
# ============================================================
def ensure_conversation_and_save_user(thread_id: str, question: str, user_id: str) -> str:
    """
    确保会话记录存在 + 保存用户消息，返回最终的 thread_id。
    若 thread_id 存在但归其他用户，自动分配新 id 避免数据泄露。
    """
    conn = get_app_db()
    try:
        now = datetime.now().isoformat()
        existing = conn.execute(
            "SELECT thread_id, user_id FROM conversations WHERE thread_id = ?", (thread_id,)
        ).fetchone()
        if not existing:
            conn.execute(
                "INSERT INTO conversations (thread_id, title, created_at, updated_at, user_id) "
                "VALUES (?, ?, ?, ?, ?)",
                (thread_id, "新的对话", now, now, user_id),
            )
        elif existing["user_id"] != user_id:
            thread_id = str(uuid.uuid4())[:8]
            conn.execute(
                "INSERT INTO conversations (thread_id, title, created_at, updated_at, user_id) "
                "VALUES (?, ?, ?, ?, ?)",
                (thread_id, "新的对话", now, now, user_id),
            )
        else:
            conn.execute(
                "UPDATE conversations SET updated_at = ? WHERE thread_id = ?",
                (now, thread_id),
            )
        conn.execute(
            "INSERT INTO messages (thread_id, role, content, created_at, user_id) "
            "VALUES (?, 'user', ?, ?, ?)",
            (thread_id, question, now, user_id),
        )
        conn.commit()
        return thread_id
    finally:
        conn.close()


def save_ai_message(thread_id: str, content: str, search_info: dict | None, user_id: str):
    """保存 AI 回复到数据库"""
    import json
    conn = get_app_db()
    try:
        conn.execute(
            "INSERT INTO messages (thread_id, role, content, search_info, created_at, user_id) "
            "VALUES (?, 'ai', ?, ?, ?, ?)",
            (thread_id, content,
             json.dumps(search_info, ensure_ascii=False) if search_info else None,
             datetime.now().isoformat(), user_id),
        )
        conn.commit()
    finally:
        conn.close()


def generate_and_save_title(thread_id: str, question: str, first_reply: str, model):
    """
    如果当前标题还是"新的对话"，用 AI 生成一个标题并写入数据库。
    model 参数是 LangChain chat model 实例。
    """
    import traceback

    # 检查是否需要生成标题
    check_conn = get_app_db()
    try:
        current = check_conn.execute(
            "SELECT title FROM conversations WHERE thread_id = ?", (thread_id,)
        ).fetchone()
        need_title = (current and current["title"] == "新的对话")
    finally:
        check_conn.close()

    if not need_title:
        return

    generated_title = ""
    try:
        resp = model.invoke(
            f"用户问题：{question}\n"
            f"助手回答：{first_reply[:300]}\n\n"
            f"为这段对话起一个标题，15个字以内"
        )
        generated_title = resp.content.strip() if hasattr(resp, "content") else str(resp).strip()
        if len(generated_title) > 30:
            generated_title = generated_title[:30]
    except Exception:
        traceback.print_exc()

    if generated_title:
        conn = get_app_db()
        try:
            conn.execute(
                "UPDATE conversations SET title = ?, updated_at = ? WHERE thread_id = ?",
                (generated_title, datetime.now().isoformat(), thread_id),
            )
            conn.commit()
        finally:
            conn.close()