"""
知识库管理模块 — 文档上传、向量化、检索 + API 路由

本模块通过 FastAPI APIRouter 注册路由，在 Agent.py 中用 app.include_router() 挂载。
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
# 一、向量化（纯 Python，零依赖）
# ============================================================
def _text_to_vector(text: str) -> list[float]:
    """
    把文本转为固定长度向量（字符 bigram 哈希 + TF 权重）。
    不需要任何 embedding API，纯本地计算。
    """
    DIM = 512
    vec = [0.0] * DIM
    if not text:
        return vec
    chars = list(text)
    for i in range(len(chars)):
        h = ord(chars[i]) % DIM          # unigram
        vec[h] += 1.0
        if i + 1 < len(chars):
            h2 = (ord(chars[i]) * 31 + ord(chars[i + 1])) % DIM  # bigram
            vec[h2] += 0.5
    norm = (sum(v * v for v in vec)) ** 0.5
    if norm > 0:
        vec = [v / norm for v in vec]
    return vec


def _embed(texts: list[str]) -> list[list[float]]:
    """批量文本转向量"""
    return [_text_to_vector(t) for t in texts]


def _cosine_similarity(a: list[float], b: list[float]) -> float:
    """纯 Python 余弦相似度（无 numpy 依赖）"""
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = (sum(x * x for x in a)) ** 0.5
    norm_b = (sum(x * x for x in b)) ** 0.5
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return dot / (norm_a * norm_b)


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
# 二、数据库
# ============================================================
def init_kb_db():
    """创建知识库表：documents（文档元数据）+ chunks_vec（文本块+向量JSON）"""
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
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS chunks_vec
                 (
                     id        INTEGER PRIMARY KEY AUTOINCREMENT,
                     doc_id    INTEGER NOT NULL,
                     chunk_idx INTEGER NOT NULL,
                     content   TEXT    NOT NULL,
                     embedding TEXT    NOT NULL,
                     FOREIGN KEY (doc_id) REFERENCES documents (id) ON DELETE CASCADE
                 )
                 """)
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


# 启动时建表
init_kb_db()


# ============================================================
# 三、知识库检索工具工厂
# ============================================================
from langchain_core.tools import tool


def _make_search_knowledge_base(user_id: str):
    """为指定用户创建知识库检索工具，仅检索该用户上传的文档。"""
    @tool
    def search_knowledge_base(query: str) -> str:
        """
        检索本地知识库中用户上传的文档内容。
        用户可能通过文档告诉你个人信息（名字、背景、偏好等），
        回答问题前必须先调用此工具查文档。
        参数 query 可以使用关键词（2-5个词），也可以是简短的问句。
        如果用户问"知识库里有啥"，用"全部文档"作为 query 即可。
        """
        if not query.strip():
            return "查询内容为空"

        # 1) 向量化查询
        try:
            q_vec = _embed([query])[0]
        except Exception as e:
            return f"查询向量化失败：{e}"

        # 2) 仅加载当前用户的文档 chunks
        conn = get_kb_db()
        try:
            rows = conn.execute(
                "SELECT c.doc_id, c.chunk_idx, c.content, c.embedding "
                "FROM chunks_vec c JOIN documents d ON c.doc_id = d.id "
                "WHERE d.user_id = ?", (user_id,)
            ).fetchall()
        finally:
            conn.close()

        if not rows:
            return "[KB]{\"results\": []}\n---\n知识库中暂无文档，请先上传文档。"

        scored = []
        for r in rows:
            try:
                vec = json.loads(r["embedding"])
            except Exception:
                continue
            sim = _cosine_similarity(q_vec, vec)
            scored.append((sim, r["doc_id"], r["content"]))

        # 3) 取 Top-5 向量相似片段
        scored.sort(key=lambda x: x[0], reverse=True)
        top = scored[:5]

        cards = []
        context_parts = []
        for sim, doc_id, content in top:
            if sim >= 0.1:
                cards.append({
                    "title": f"文档#{doc_id} 片段（相似度 {sim:.0%}）",
                    "url": "",
                    "snippet": content.strip()[:150],
                })
                context_parts.append(f"[文档#{doc_id}]\n{content.strip()}")

        # 4) 向量没命中时，把所有文档都给 Agent 让 LLM 自己判断
        if not context_parts:
            seen = set()
            for _, doc_id, content in scored:
                c = content.strip()
                if c not in seen:
                    seen.add(c)
                    context_parts.append(f"[文档#{doc_id}]\n{c}")
            cards = [{
                "title": f"全文检索（共 {len(context_parts)} 个片段）",
                "url": "",
                "snippet": "向量未匹配，已将全部文档提供给 AI 分析",
            }]

        ctx = "\n\n".join(context_parts)
        return f"[KB]{json.dumps({'results': cards}, ensure_ascii=False)}\n---\n{ctx}"

    return search_knowledge_base


# ============================================================
# 四、请求模型
# ============================================================
class KbUploadRequest(BaseModel):
    filename: str = Field(..., description="文件名，如 doc.txt")
    content_b64: str = Field(..., description="文件内容的 base64 编码")


# ============================================================
# 五、API 路由
# ============================================================
kb_router = APIRouter(prefix="/api/kb", tags=["知识库"])


@kb_router.post("/upload", summary="上传文档到知识库")
async def kb_upload(body: KbUploadRequest, user_id: str = Depends(get_current_user)):
    """上传 txt/md 文件（base64 编码），自动分块、向量化、存入数据库"""
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

    # 2) 分块
    chunks = _chunk_text(text)
    if not chunks:
        raise HTTPException(400, "文件无法分块")

    # 3) 向量化
    try:
        vectors = _embed(chunks)
    except Exception as e:
        raise HTTPException(500, f"向量化失败：{e}")

    # 4) 存入数据库
    conn = get_kb_db()
    try:
        now = datetime.now().isoformat()
        cur = conn.execute(
            "INSERT INTO documents (filename, file_type, char_count, chunk_count, created_at, user_id) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (body.filename, ext, len(text), len(chunks), now, user_id),
        )
        doc_id = cur.lastrowid
        for i, (chunk, vec) in enumerate(zip(chunks, vectors)):
            conn.execute(
                "INSERT INTO chunks_vec (doc_id, chunk_idx, content, embedding) "
                "VALUES (?, ?, ?, ?)",
                (doc_id, i, chunk, json.dumps(vec)),
            )
        conn.commit()
    finally:
        conn.close()

    return {"ok": True, "doc_id": doc_id, "filename": body.filename,
            "chunks": len(chunks), "char_count": len(text)}


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
    """删除文档及其所有向量块（仅允许删除自己的文档）"""
    conn = get_kb_db()
    try:
        conn.execute("DELETE FROM documents WHERE id = ? AND user_id = ?", (doc_id, user_id))
        conn.execute("DELETE FROM chunks_vec WHERE doc_id = ?", (doc_id,))
        conn.commit()
    finally:
        conn.close()
    return {"ok": True}
