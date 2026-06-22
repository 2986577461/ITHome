"""
知识库管理模块 — 文档上传、语义向量化、Milvus 检索 + API 路由

向量存储由 vector_store.VectorStore（Milvus Lite + BGE 中文语义模型）提供。
SQLite 仅保留文档元数据（documents 表）。
"""

import json
import os
import sqlite3

from dotenv import load_dotenv


load_dotenv()

# ---- 数据库路径 ----
DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)
KB_DB_PATH = os.path.join(DB_DIR, "kb.db")

# ---- 从 conversation_manager 获取鉴权依赖 ----



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
from common.vector_store import get_vector_store



# ============================================================
# 四、知识库检索工具
# ============================================================
from langchain_core.tools import tool
from langchain_core.runnables import RunnableConfig


@tool
def search_knowledge_base(query: str, config: RunnableConfig) -> str:
    """
    检索本地知识库中用户上传的文档内容。
    参数 query 可以使用关键词（2-5个词），也可以是简短的问句。
    如果用户问"知识库里有什么"、"文档库有哪些记录"等问题，必须回避，并告诉用户"左下角知识库可以查看文档"、"抱歉，我无法回答"等
    """
    print(query)

    if not query.strip():
        return "查询内容为空"

    user_id = config.get("configurable", {}).get("user_id", "")

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
        if h["score"] < 0.5:
            continue
        cards.append({
            "title": f"文档#{h['doc_id']} 片段（相似度 {h['score']:.0%}）",
            "url": "",
            "snippet": h["content"].strip()[:150],
        })
        context_parts.append(f"[文档#{h['doc_id']}]\n{h['content'].strip()}")

    if not context_parts:
        return '[KB]{"results": []}\n---\n知识库中未找到相关内容。'

    ctx = "\n\n".join(context_parts)
    return f"[KB]{json.dumps({'results': cards}, ensure_ascii=False)}\n---\n{ctx}"