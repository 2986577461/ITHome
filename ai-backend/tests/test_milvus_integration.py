"""可选的真实 Milvus Lite 集成测试；使用临时库及合成向量，不下载模型。"""

import os

import pytest

from common.vector_store import DIM, VectorStore


@pytest.mark.skipif(os.getenv("RUN_MILVUS_TESTS") != "1", reason="设置 RUN_MILVUS_TESTS=1 运行真实 Milvus Lite 测试")
def test_milvus_shared_reads_and_owner_scoped_deletes(tmp_path):
    store = VectorStore(str(tmp_path / "milvus.db"))
    store.embed = lambda texts: [[1.0] + [0.0] * (DIM - 1) for _ in texts]
    try:
        store.insert_chunks(1, ["alice-private"], "alice")
        store.insert_chunks(2, ["bob-private"], "bob")
        assert store.get_chunks_by_doc(1, "bob", owner_id="alice") == [{"chunk_idx": 0, "content": "alice-private"}]
        assert {hit["doc_id"] for hit in store.search("test", "bob")} == {1, 2}
        store.delete_document(1, "bob")
        assert store.get_chunks_by_doc(1, "alice", owner_id="alice")
        store.delete_document(1, "alice")
        assert store.get_chunks_by_doc(1, "bob", owner_id="alice") == []
        assert store.get_chunks_by_doc(2, "bob", owner_id="bob")
    finally:
        store.close()


@pytest.mark.skipif(os.getenv("RUN_MILVUS_TESTS") != "1", reason="设置 RUN_MILVUS_TESTS=1 运行真实 Milvus Lite 测试")
def test_shared_api_search_and_owner_delete_with_real_milvus(kb, monkeypatch, tmp_path):
    import base64
    store = VectorStore(str(tmp_path / "shared.db"))
    store.embed = lambda texts: [[1.0] + [0.0] * (DIM - 1) for _ in texts]
    monkeypatch.setattr(kb.router, "get_vector_store", lambda: store)
    monkeypatch.setattr(kb.tools, "get_vector_store", lambda: store)
    try:
        response = kb.client.post("/api/kb/upload", json={
            "filename": "shared.txt", "content_b64": base64.b64encode(b"shared evidence").decode(),
        })
        assert response.status_code == 200
        doc_id = response.json()["doc_id"]
        content, artifact = kb.tools.retrieve_knowledge("query", "bob")
        assert artifact["status"] == "success" and "shared evidence" in content
        assert kb.client.get(f"/api/kb/documents/{doc_id}/content", headers={"x-test-user": "bob"}).status_code == 200
        assert kb.client.delete(f"/api/kb/documents/{doc_id}", headers={"x-test-user": "bob"}).status_code == 403
        assert kb.client.delete(f"/api/kb/documents/{doc_id}").status_code == 200
        assert kb.tools.retrieve_knowledge("query", "bob")[1]["status"] == "no_hits"
    finally:
        store.close()
