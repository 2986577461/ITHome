"""知识库 API 路由"""

import base64
from datetime import datetime

from fastapi import APIRouter, HTTPException, Depends
from models.schemas import KbUploadRequest

from conversation.service import get_current_user
from kb.tools import get_kb_db, _chunk_text
from common.vector_store import get_vector_store
from state.manager import get_state_manager

kb_router = APIRouter(prefix="/api/kb", tags=["知识库"])


@kb_router.post("/upload", summary="上传文档到知识库")
async def kb_upload(body: KbUploadRequest, user_id: str = Depends(get_current_user)):
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

    chunk_size = 800 if len(text) > 500000 else 500 if len(text) > 100000 else 300
    chunks = _chunk_text(text, chunk_size=chunk_size)
    if not chunks:
        raise HTTPException(400, "文件无法分块")

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

    get_state_manager().invalidate_kb_docs_cache(user_id)
    return {"ok": True, "doc_id": doc_id, "filename": body.filename,
            "chunks": chunk_count, "char_count": len(text)}


@kb_router.get("/documents", summary="知识库文档列表")
def kb_list_documents(user_id: str = Depends(get_current_user)):
    sm = get_state_manager()
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


@kb_router.get("/documents/{doc_id}/content", summary="获取文档全文")
def kb_document_content(doc_id: int, user_id: str = Depends(get_current_user)):
    sm = get_state_manager()
    cached = sm.get_cached_kb_doc_content(doc_id, user_id)
    if cached is not None:
        return cached

    conn = get_kb_db()
    try:
        row = conn.execute(
            "SELECT filename FROM documents WHERE id = ? AND user_id = ?",
            (doc_id, user_id),
        ).fetchone()
        if not row:
            raise HTTPException(404, "文档不存在或无权访问")
        filename = row["filename"]
    finally:
        conn.close()

    chunks = get_vector_store().get_chunks_by_doc(doc_id)
    if not chunks:
        raise HTTPException(404, "文档内容为空或已丢失")
    full_text = "".join(c["content"] for c in chunks)
    result = {"doc_id": doc_id, "filename": filename, "content": full_text,
              "chars": len(full_text), "chunks": len(chunks)}
    sm.cache_kb_doc_content(doc_id, user_id, result)
    return result


@kb_router.delete("/documents/{doc_id}", summary="删除知识库文档")
def kb_delete_document(doc_id: int, user_id: str = Depends(get_current_user)):
    conn = get_kb_db()
    try:
        conn.execute("DELETE FROM documents WHERE id = ? AND user_id = ?", (doc_id, user_id))
        conn.commit()
    finally:
        conn.close()
    try:
        get_vector_store().delete_document(doc_id)
    except Exception:
        pass
    sm = get_state_manager()
    sm.invalidate_kb_docs_cache(user_id)
    sm.invalidate_kb_doc_content_cache(doc_id, user_id)
    return {"ok": True}
