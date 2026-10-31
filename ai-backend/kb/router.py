"""知识库 API：鉴权后的用户隔离、受限上传和原文展示。"""

import base64
import binascii
import logging
from datetime import datetime

from fastapi import APIRouter, HTTPException, Depends

from conversation.service import get_current_user
from kb.documents import DocumentError, chunk_text, extract_text
from kb.schemas import CHUNK_OVERLAP, MAX_DOCUMENT_BYTES, UploadDocumentRequest
from kb.tools import get_kb_db
from common.vector_store import get_vector_store
from state.manager import get_cache_manager

logger = logging.getLogger(__name__)
kb_router = APIRouter(prefix="/api/kb", tags=["知识库"])


@kb_router.post("/upload", summary="上传文档到知识库")
def kb_upload(body: UploadDocumentRequest, user_id: str = Depends(get_current_user)):
    # 同步路由由 FastAPI 在线程池中执行，避免 embedding 阻塞事件循环。
    ext = body.filename.rsplit(".", 1)[-1].lower() if "." in body.filename else ""
    if ext not in ("txt", "md", "docx", "pdf"):
        raise HTTPException(400, "仅支持 txt、md、docx 和 pdf")
    try:
        raw = base64.b64decode(body.content_b64, validate=True)
    except (binascii.Error, ValueError):
        raise HTTPException(400, "content_b64 解码失败")
    if len(raw) > MAX_DOCUMENT_BYTES:
        raise HTTPException(413, "单个文档不能超过 1 MiB")
    try:
        text = extract_text(raw, ext)
    except DocumentError as exc:
        raise HTTPException(400, str(exc)) from exc
    chunks = chunk_text(text, ext)
    vs = get_vector_store()
    conn = get_kb_db()
    doc_id = None
    try:
        cur = conn.execute(
            "INSERT INTO documents (filename, file_type, char_count, chunk_count, created_at, user_id, content) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            (body.filename, ext, len(text), len(chunks), datetime.now().isoformat(), user_id, text),
        )
        doc_id = cur.lastrowid
        chunk_count = vs.insert_chunks(doc_id, chunks, user_id)
        conn.commit()
    except Exception:
        # SQLite 与 Milvus 不共享事务，尽力清理可能部分写入的向量。
        # 在释放 SQLite 写锁前清理，防止回滚后的 ID 被其他上传复用。
        if doc_id is not None:
            try:
                vs.delete_document(doc_id, user_id)
            except Exception:
                logger.exception("上传失败后的向量清理失败 doc_id=%s", doc_id)
        conn.rollback()
        logger.exception("知识库上传失败 doc_id=%s", doc_id)
        raise HTTPException(503, "文档存储暂不可用，请稍后重试")
    finally:
        conn.close()

    get_cache_manager().invalidate_kb_docs_cache(user_id)
    return {"ok": True, "doc_id": doc_id, "filename": body.filename,
            "chunks": chunk_count, "char_count": len(text)}


@kb_router.get("/documents", summary="知识库文档列表")
def kb_list_documents(user_id: str = Depends(get_current_user)):
    sm = get_cache_manager()
    cached = sm.get_cached_kb_docs(user_id)
    if cached is not None:
        return cached
    conn = get_kb_db()
    try:
        rows = conn.execute(
            "SELECT id, filename, file_type, char_count, chunk_count, created_at "
            "FROM documents WHERE user_id = ? ORDER BY created_at DESC",
            (user_id,)
        ).fetchall()
        docs = [dict(r) for r in rows]
        sm.cache_kb_docs(user_id, docs)
        return docs
    finally:
        conn.close()


def _restore_legacy_text(chunks: list[dict], char_count: int, chunk_count: int) -> str:
    """旧数据没有原文，只接受能验证固定 60 字重叠与总长度的恢复。"""
    if not chunks or len(chunks) != chunk_count:
        raise HTTPException(409, "旧文档片段不完整，请重新上传")
    parts = []
    previous = ""
    for idx, chunk in enumerate(chunks):
        content = chunk["content"]
        if chunk["chunk_idx"] != idx:
            raise HTTPException(409, "旧文档片段不完整，请重新上传")
        if idx:
            if len(content) < CHUNK_OVERLAP or previous[-CHUNK_OVERLAP:] != content[:CHUNK_OVERLAP]:
                raise HTTPException(409, "旧文档重叠内容不一致，请重新上传")
            parts.append(content[CHUNK_OVERLAP:])
        else:
            parts.append(content)
        previous = content
    text = "".join(parts)
    if len(text) != char_count:
        raise HTTPException(409, "旧文档长度校验失败，请重新上传")
    return text


@kb_router.get("/documents/{doc_id}/content", summary="获取文档全文")
def kb_document_content(doc_id: int, user_id: str = Depends(get_current_user)):
    # 即使有缓存，也以数据库中的归属和存在性为准，防止失效失败后读到已删除文档。
    conn = get_kb_db()
    try:
        row = conn.execute(
            "SELECT filename, content, char_count, chunk_count FROM documents WHERE id = ? AND user_id = ?",
            (doc_id, user_id),
        ).fetchone()
        if not row:
            raise HTTPException(404, "文档不存在或无权访问")
    finally:
        conn.close()

    sm = get_cache_manager()
    cached = sm.get_cached_kb_doc_content(doc_id, user_id)
    if cached is not None:
        return cached
    text = row["content"]
    if text is None:
        chunks = get_vector_store().get_chunks_by_doc(doc_id, user_id)
        text = _restore_legacy_text(chunks, row["char_count"], row["chunk_count"])
    result = {"doc_id": doc_id, "filename": row["filename"], "content": text,
              "chars": len(text), "chunks": row["chunk_count"]}
    sm.cache_kb_doc_content(doc_id, user_id, result)
    return result


@kb_router.delete("/documents/{doc_id}", summary="删除知识库文档")
def kb_delete_document(doc_id: int, user_id: str = Depends(get_current_user)):
    conn = get_kb_db()
    try:
        conn.execute("BEGIN IMMEDIATE")
        row = conn.execute(
            "SELECT id FROM documents WHERE id = ? AND user_id = ?", (doc_id, user_id)
        ).fetchone()
        if not row:
            raise HTTPException(404, "文档不存在或无权访问")
        # 先删向量。失败时保留元数据，允许用户重试，而不是吞异常后报告成功。
        try:
            get_vector_store().delete_document(doc_id, user_id)
        except Exception:
            logger.exception("知识库向量删除失败 doc_id=%s", doc_id)
            raise HTTPException(503, "文档删除暂不可用，请稍后重试")
        conn.execute("DELETE FROM documents WHERE id = ? AND user_id = ?", (doc_id, user_id))
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
    sm = get_cache_manager()
    sm.invalidate_kb_docs_cache(user_id)
    sm.invalidate_kb_doc_content_cache(doc_id, user_id)
    return {"ok": True}
