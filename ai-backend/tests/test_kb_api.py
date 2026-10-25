import base64
import sqlite3
from unittest.mock import Mock

import pytest
import redis

from kb.schemas import MAX_BASE64_LENGTH, MAX_DOCUMENT_BYTES


def upload(kb, text="学习资料" * 200, filename="notes.md", user="alice"):
    return kb.client.post("/api/kb/upload", headers={"x-test-user": user}, json={
        "filename": filename,
        "content_b64": base64.b64encode(text.encode()).decode(),
    })


def test_upload_list_read_delete_round_trip(kb):
    text = "# 标题\n" + "带重叠的学习资料\n" * 100
    result = upload(kb, text)
    assert result.status_code == 200
    doc = result.json()
    assert doc["chunks"] > 1
    assert kb.client.get("/api/kb/documents").json()[0]["id"] == doc["doc_id"]
    content = kb.client.get(f"/api/kb/documents/{doc['doc_id']}/content")
    assert content.json()["content"] == text
    assert content.json()["chars"] == len(text)
    kb.vectors.get_chunks_by_doc.assert_not_called()  # 新文档全文不依赖向量库
    assert kb.client.delete(f"/api/kb/documents/{doc['doc_id']}").status_code == 200
    kb.vectors.delete_document.assert_called_once_with(doc["doc_id"], "alice")
    assert kb.client.get("/api/kb/documents").json() == []
    assert kb.client.get(f"/api/kb/documents/{doc['doc_id']}/content").status_code == 404


def test_cross_user_list_read_delete_are_isolated(kb):
    doc_id = upload(kb).json()["doc_id"]
    headers = {"x-test-user": "bob"}
    assert kb.client.get("/api/kb/documents", headers=headers).json() == []
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content", headers=headers).status_code == 404
    assert kb.client.delete(f"/api/kb/documents/{doc_id}", headers=headers).status_code == 404
    kb.vectors.delete_document.assert_not_called()
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content").status_code == 200


@pytest.mark.parametrize("method,path", [
    ("get", "/api/kb/documents"),
    ("get", "/api/kb/documents/1/content"),
    ("delete", "/api/kb/documents/1"),
    ("post", "/api/kb/upload"),
])
def test_authentication_required(kb, method, path):
    response = getattr(kb.client, method)(path, headers={"x-test-user": ""})
    assert response.status_code == 401
    kb.vectors.insert_chunks.assert_not_called()
    kb.vectors.delete_document.assert_not_called()


@pytest.mark.parametrize("payload,status", [
    ({}, 422),
    ({"filename": "a.md", "content_b64": "!!!!"}, 400),
    ({"filename": "a.md", "content_b64": "中文"}, 400),
    ({"filename": "a.md", "content_b64": "YQ==%%%"}, 400),
    ({"filename": "a.md", "content_b64": "/w=="}, 400),
    ({"filename": "a.md", "content_b64": "ICA="}, 400),
    ({"filename": "a.pdf", "content_b64": "YQ=="}, 400),
    ({"filename": "../a.md", "content_b64": "YQ=="}, 422),
    ({"filename": "..\\a.md", "content_b64": "YQ=="}, 422),
    ({"filename": "  ", "content_b64": "YQ=="}, 422),
    ({"filename": "a\x00.md", "content_b64": "YQ=="}, 422),
    ({"filename": "a.md", "content_b64": "YQ==", "user_id": "bob"}, 422),
    ({"filename": 12, "content_b64": "YQ=="}, 422),
    ({"filename": "a.md", "content_b64": "A" * (MAX_BASE64_LENGTH + 1)}, 422),
    ({"filename": "a.md", "content_b64": base64.b64encode(b"a" * (MAX_DOCUMENT_BYTES + 1)).decode()}, 413),
])
def test_invalid_upload_is_rejected_before_vectorization(kb, payload, status):
    response = kb.client.post("/api/kb/upload", json=payload)
    assert response.status_code == status
    kb.vectors.insert_chunks.assert_not_called()
    assert kb.client.get("/api/kb/documents").json() == []


def test_limit_accepts_exactly_one_mib(kb):
    assert upload(kb, "a" * MAX_DOCUMENT_BYTES).status_code == 200


def test_vector_insert_failure_rolls_back_and_cleans_up(kb):
    kb.vectors.insert_chunks.side_effect = RuntimeError("private dependency error")
    response = upload(kb)
    assert response.status_code == 503
    assert "private dependency error" not in response.text
    kb.vectors.delete_document.assert_called_once_with(1, "alice")
    assert kb.client.get("/api/kb/documents").json() == []


def test_vector_delete_failure_keeps_metadata_for_retry(kb):
    doc_id = upload(kb).json()["doc_id"]
    kb.vectors.delete_document.side_effect = RuntimeError("unavailable")
    assert kb.client.delete(f"/api/kb/documents/{doc_id}").status_code == 503
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content").status_code == 200
    kb.vectors.delete_document.side_effect = None
    assert kb.client.delete(f"/api/kb/documents/{doc_id}").status_code == 200


def test_stale_content_cache_cannot_bypass_database_ownership(kb):
    doc_id = upload(kb).json()["doc_id"]
    kb.values[kb.cache._kbd_key(doc_id, "bob")] = '{"content":"secret"}'
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content", headers={"x-test-user": "bob"}).status_code == 404
    kb.client.get(f"/api/kb/documents/{doc_id}/content")  # 填充 alice 的缓存
    with kb.tools.get_kb_db() as conn:
        conn.execute("DELETE FROM documents WHERE id = ?", (doc_id,))
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content").status_code == 404


def test_redis_failure_does_not_break_core_document_operations(kb):
    kb.cache._r = Mock()
    for method in (kb.cache._r.get, kb.cache._r.set, kb.cache._r.delete):
        method.side_effect = redis.ConnectionError("offline")
    doc_id = upload(kb).json()["doc_id"]
    assert kb.client.get("/api/kb/documents").status_code == 200
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content").status_code == 200
    assert kb.client.delete(f"/api/kb/documents/{doc_id}").status_code == 200


def legacy_doc(kb, text):
    chunks = kb.tools._chunk_text(text, 300)
    with kb.tools.get_kb_db() as conn:
        cur = conn.execute(
            "INSERT INTO documents (filename, file_type, char_count, chunk_count, created_at, user_id) "
            "VALUES ('legacy.md', 'md', ?, ?, '2026-01-01', 'alice')", (len(text), len(chunks)),
        )
        doc_id = cur.lastrowid
    kb.vectors.get_chunks_by_doc.return_value = [
        {"chunk_idx": idx, "content": value} for idx, value in enumerate(chunks)
    ]
    return doc_id


@pytest.mark.parametrize("text", ["旧资料", "重复字符" * 250, "\n".join(str(i) for i in range(400))])
def test_legacy_document_restored_without_duplicate_overlap(kb, text):
    doc_id = legacy_doc(kb, text)
    response = kb.client.get(f"/api/kb/documents/{doc_id}/content")
    assert response.status_code == 200
    assert response.json()["content"] == text
    kb.vectors.get_chunks_by_doc.assert_called_once_with(doc_id, "alice")


@pytest.mark.parametrize("corruption", ["missing", "index", "overlap", "length"])
def test_corrupt_legacy_document_requires_reupload(kb, corruption):
    doc_id = legacy_doc(kb, "0123456789" * 100)
    chunks = kb.vectors.get_chunks_by_doc.return_value
    if corruption == "missing":
        chunks.pop()
    elif corruption == "index":
        chunks[1]["chunk_idx"] = 20
    elif corruption == "overlap":
        chunks[1]["content"] = "x" + chunks[1]["content"][1:]
    else:
        with kb.tools.get_kb_db() as conn:
            conn.execute("UPDATE documents SET char_count = char_count + 1 WHERE id = ?", (doc_id,))
    assert kb.client.get(f"/api/kb/documents/{doc_id}/content").status_code == 409


def test_old_schema_migration_is_idempotent(kb, tmp_path, monkeypatch):
    path = tmp_path / "legacy-schema.db"
    with sqlite3.connect(path) as conn:
        conn.execute("CREATE TABLE documents (id INTEGER PRIMARY KEY AUTOINCREMENT, filename TEXT NOT NULL, "
                     "file_type TEXT NOT NULL, char_count INTEGER, chunk_count INTEGER, created_at TEXT NOT NULL)")
        conn.execute("INSERT INTO documents VALUES (1, 'old.md', 'md', 3, 1, '2026-01-01')")
    monkeypatch.setattr(kb.tools, "KB_DB_PATH", str(path))
    kb.tools.init_kb_db()
    kb.tools.init_kb_db()
    with kb.tools.get_kb_db() as conn:
        row = conn.execute("SELECT * FROM documents WHERE id = 1").fetchone()
    assert row["filename"] == "old.md"
    assert row["content"] is None
    assert row["user_id"] == ""


def test_empty_tool_identity_never_calls_vector_store(kb, monkeypatch):
    getter = Mock()
    monkeypatch.setattr(kb.tools, "get_vector_store", getter)
    response = kb.tools.search_knowledge_base.invoke({"query": "资料"}, config={"configurable": {"user_id": ""}})
    assert "请先登录" in response
    getter.assert_not_called()


@pytest.mark.parametrize("size,overlap", [(0, 0), (-1, 0), (10, 10), (10, -1)])
def test_invalid_chunk_parameters_do_not_loop(kb, size, overlap):
    with pytest.raises(ValueError):
        kb.tools._chunk_text("abc", size, overlap)
