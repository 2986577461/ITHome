"""可选的真实 Milvus Lite 集成测试；使用临时库及合成向量，不下载模型。"""

import os

import pytest

from common.vector_store import DIM, VectorStore


@pytest.mark.skipif(os.getenv("RUN_MILVUS_TESTS") != "1", reason="设置 RUN_MILVUS_TESTS=1 运行真实 Milvus Lite 测试")
def test_milvus_user_isolation(tmp_path):
    store = VectorStore(str(tmp_path / "milvus.db"))
    store.embed = lambda texts: [[1.0] + [0.0] * (DIM - 1) for _ in texts]
    try:
        store.insert_chunks(1, ["alice-private"], "alice")
        store.insert_chunks(2, ["bob-private"], "bob")
        assert store.get_chunks_by_doc(1, "alice") == [{"chunk_idx": 0, "content": "alice-private"}]
        assert store.get_chunks_by_doc(1, "bob") == []
        assert {hit["doc_id"] for hit in store.search("test", "alice")} == {1}
        store.delete_document(1, "bob")
        assert store.get_chunks_by_doc(1, "alice")
        store.delete_document(1, "alice")
        assert store.get_chunks_by_doc(1, "alice") == []
        assert store.get_chunks_by_doc(2, "bob")
    finally:
        store.close()
