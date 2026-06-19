"""
知识库管理模块 — 文档上传、语义向量化、Milvus 检索 + API 路由

向量存储由 vector_store.VectorStore（Milvus Lite + BGE 中文语义模型）提供。
SQLite 仅保留文档元数据（documents 表）。
"""

import base64
import json
import os
import sqlite3
from datetime import datetime

from dotenv import load_dotenv
from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel, Field

load_dotenv()

# ---- 数据库路径 ----
DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)
KB_DB_PATH = os.path.join(DB_DIR, "kb.db")

# ---- 从 conversation_manager 获取鉴权依赖 ----
from conversation_manager import get_current_user


# ============================================================
# 一、分块工具（纯文本分割，无向量化）
# ============================================================
def _chunk_text(text: str, chunk_size: int = 400, overlap: int = 60) -> list[str]:
    """把长文本切成有重叠的段落"""
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        if end >= len(text):
            break
        start = end - overlap
    return chunks


# ============================================================
# 二、SQLite（仅存文档元数据）
# ============================================================
def init_kb_db():
    """创建 documents 表（chunks_vec 已废弃，由 Milvus 替代）"""
    conn = sqlite3.connect(KB_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS documents
                 (
                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                     filename    TEXT NOT NULL,
                     file_type   TEXT NOT NULL,
                     char_count  INTEGER DEFAULT 0,
                     chunk_count INTEGER DEFAULT 0,
                     created_at  TEXT NOT NULL,
                     user_id     TEXT DEFAULT ''
                 )
                 """)
    # 清理旧版 chunks_vec 表（已迁移到 Milvus）
    conn.execute("DROP TABLE IF EXISTS chunks_vec")
    try:
        conn.execute("ALTER TABLE documents ADD COLUMN user_id TEXT DEFAULT ''")
    except sqlite3.OperationalError:
        pass
    conn.commit()
    conn.close()


def get_kb_db() -> sqlite3.Connection:
    """获取知识库数据库连接"""
    conn = sqlite3.connect(KB_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.row_factory = sqlite3.Row
    return conn


# 启动时建表 + 清理旧 chunks_vec
init_kb_db()


# ============================================================
# 三、向量存储（Milvus Lite + BGE 语义 embedding）
# ============================================================
from vector_store import get_vector_store


# ============================================================
# 四、知识库检索工具
# ============================================================
from langchain_core.tools import tool
from langchain_core.runnables import RunnableConfig


@tool
def search_knowledge_base(query: str, config: RunnableConfig) -> str:
    """
    检索本地知识库中用户上传的文档内容。
    用户可能通过文档告诉你个人信息（名字、背景、偏好等），
    回答问题前必须先调用此工具查文档。
    参数 query 可以使用关键词（2-5个词），也可以是简短的问句。
    如果用户问"知识库里有啥"，用"全部文档"作为 query 即可。
    """
    user_id = config.get("configurable", {}).get("user_id", "")
    if not query.strip():
        return "查询内容为空"

    # 语义检索
    try:
        hits = get_vector_store().search(query, user_id=user_id, top_k=8)
    except Exception as e:
        return f"[KB]{json.dumps({'results': []})}\n---\n检索失败：{e}"

    if not hits:
        return "[KB]{\"results\": []}\n---\n知识库中暂无文档，请先上传文档。"

    cards = []
    context_parts = []
    for h in hits:
        cards.append({
            "title": f"文档#{h['doc_id']} 片段（相似度 {h['score']:.0%}）",
            "url": "",
            "snippet": h["content"].strip()[:150],
        })
        context_parts.append(f"[文档#{h['doc_id']}]\n{h['content'].strip()}")

    ctx = "\n\n".join(context_parts)
    return f"[KB]{json.dumps({'results': cards}, ensure_ascii=False)}\n---\n{ctx}"


# ============================================================
# 五、请求模型
# ============================================================
class KbUploadRequest(BaseModel):
    filename: str = Field(..., description="文件名，如 doc.txt")
    content_b64: str = Field(..., description="文件内容的 base64 编码")


# ============================================================
# 六、API 路由
# ============================================================
kb_router = APIRouter(prefix="/api/kb", tags=["知识库"])


@kb_router.post("/upload", summary="上传文档到知识库")
async def kb_upload(body: KbUploadRequest, user_id: str = Depends(get_current_user)):
    """上传 txt/md 文件（base64 编码），自动分块、语义向量化、存入 Milvus"""
    # 1) 解码文件内容
    try:
        raw = base64.b64decode(body.content_b64)
    except Exception:
        raise HTTPException(400, "content_b64 解码失败")

    ext = body.filename.rsplit(".", 1)[-1].lower() if "." in body.filename else "txt"
    if ext in ("txt", "md"):
        text = raw.decode("utf-8", errors="replace")
    else:
        raise HTTPException(400, f"不支持的文件类型：.{ext}，仅支持 txt/md")

    if not text.strip():
        raise HTTPException(400, "文件内容为空")

    # 2) 分块（大文件用更大的块减少数量）
    chunk_size = 800 if len(text) > 500000 else 500 if len(text) > 100000 else 300
    chunks = _chunk_text(text, chunk_size=chunk_size)
    if not chunks:
        raise HTTPException(400, "文件无法分块")

    # 3) 存入 Milvus（内部完成 embedding + 入库）
    vs = get_vector_store()
    conn = get_kb_db()
    try:
        now = datetime.now().isoformat()
        cur = conn.execute(
            "INSERT INTO documents (filename, file_type, char_count, chunk_count, created_at, user_id) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (body.filename, ext, len(text), len(chunks), now, user_id),
        )
        doc_id = cur.lastrowid

        chunk_count = vs.insert_chunks(doc_id, chunks, user_id)
        conn.commit()
    except Exception as e:
        conn.rollback()
        raise HTTPException(500, f"存储失败：{e}")
    finally:
        conn.close()

    return {"ok": True, "doc_id": doc_id, "filename": body.filename,
            "chunks": chunk_count, "char_count": len(text)}


@kb_router.get("/documents", summary="知识库文档列表")
def kb_list_documents(user_id: str = Depends(get_current_user)):
    """返回已上传的文档列表"""
    conn = get_kb_db()
    try:
        rows = conn.execute(
            "SELECT id, filename, file_type, char_count, chunk_count, created_at "
            "FROM documents WHERE user_id = ? ORDER BY created_at DESC",
            (user_id,)
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


@kb_router.delete("/documents/{doc_id}", summary="删除知识库文档")
def kb_delete_document(doc_id: int, user_id: str = Depends(get_current_user)):
    """删除文档及其向量块（SQLite 元数据 + Milvus 向量）"""
    conn = get_kb_db()
    try:
        conn.execute("DELETE FROM documents WHERE id = ? AND user_id = ?", (doc_id, user_id))
        conn.commit()
    finally:
        conn.close()
    # 从 Milvus 删除向量
    try:
        get_vector_store().delete_document(doc_id)
    except Exception:
        pass
    return {"ok": True}
