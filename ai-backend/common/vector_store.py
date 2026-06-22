"""
vector_store.py — Milvus Lite 向量存储 + fastembed 语义 embedding

替换 knowledge_base_manager 中的 SQLite chunks_vec 存储和 char-bigram 哈希。
提供纯语义检索（BAAI/bge-small-zh-v1.5）+ ANN 近似搜索。
"""

import os

# 抑制 Milvus Lite 的 gRPC 和 fork 日志
import logging
logging.getLogger("milvus_lite").setLevel(logging.ERROR)
os.environ["GRPC_VERBOSITY"] = "NONE"
os.environ["GLOG_minloglevel"] = "3"
os.environ["MILVUS_LOG_LEVEL"] = "fatal"

from pymilvus import MilvusClient, DataType

DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)

DEFAULT_DB = os.path.join(DB_DIR, "milvus.db")
COLLECTION = "kb_chunks"
DIM = 512  # bge-small-zh-v1.5 输出维度


class VectorStore:
    """Milvus Lite 向量存储封装"""

    def __init__(self, db_path: str = DEFAULT_DB):
        self.client = MilvusClient(db_path)
        self._embed_model = None
        self._init_collection()

    # ------------------------------------------------------------------
    # Embedding
    # ------------------------------------------------------------------

    def _get_model(self):
        if self._embed_model is None:
            from fastembed import TextEmbedding
            self._embed_model = TextEmbedding(model_name="BAAI/bge-small-zh-v1.5")
        return self._embed_model

    def embed(self, texts: list[str], batch_size: int = 64) -> list[list[float]]:
        """将文本列表转为向量列表，分批推理避免内存暴涨"""
        model = self._get_model()
        results = []
        for i in range(0, len(texts), batch_size):
            batch = texts[i:i + batch_size]
            results.extend(v.tolist() for v in model.embed(batch))
        return results

    def embed_one(self, text: str) -> list[float]:
        return self.embed([text])[0]

    # ------------------------------------------------------------------
    # 集合管理
    # ------------------------------------------------------------------

    def _init_collection(self):
        """若集合不存在则创建"""
        if self.client.has_collection(COLLECTION):
            return

        schema = self.client.create_schema(
            auto_id=True,  # Milvus 自动生成 id
            enable_dynamic_field=False,
        )
        schema.add_field("id", DataType.INT64, is_primary=True)
        schema.add_field("vector", DataType.FLOAT_VECTOR, dim=DIM)
        schema.add_field("doc_id", DataType.INT64)
        schema.add_field("chunk_idx", DataType.INT64)
        schema.add_field("content", DataType.VARCHAR, max_length=65535)
        schema.add_field("user_id", DataType.VARCHAR, max_length=128)

        index_params = self.client.prepare_index_params()
        index_params.add_index(
            field_name="vector",
            metric_type="IP",  # Inner Product（等价余弦相似度，因为 BGE 向量已 L2 归一化）
            index_type="HNSW",
            params={"M": 16, "efConstruction": 200},
        )

        self.client.create_collection(
            collection_name=COLLECTION,
            schema=schema,
            index_params=index_params,
        )

    def drop_all(self):
        """清空集合（迁移用）"""
        self.client.drop_collection(COLLECTION)
        self._init_collection()

    # ------------------------------------------------------------------
    # 写入
    # ------------------------------------------------------------------

    def insert_chunks(self, doc_id: int, chunks: list[str],
                      user_id: str) -> int:
        """
        分块 + embedding + 入库。
        返回插入的块数。
        """
        if not chunks:
            return 0

        vectors = self.embed(chunks)
        data = [
            {
                "vector": vectors[i],
                "doc_id": doc_id,
                "chunk_idx": i,
                "content": chunks[i],
                "user_id": user_id,
            }
            for i in range(len(chunks))
        ]
        self.client.insert(COLLECTION, data)
        return len(chunks)

    # ------------------------------------------------------------------
    # 检索
    # ------------------------------------------------------------------

    def search(self, query: str, user_id: str = "",
               top_k: int = 8) -> list[dict]:
        """
        语义检索，返回按相关性降序排列的片段。

        每个元素：{doc_id, chunk_idx, content, score}
        """
        q_vec = self.embed_one(query)
        expr = f'user_id == "{user_id}"' if user_id else ""

        self.client.load_collection(COLLECTION)
        results = self.client.search(
            collection_name=COLLECTION,
            data=[q_vec],
            limit=top_k,
            search_params={"metric_type": "IP"},
            filter=expr or None,
            output_fields=["doc_id", "chunk_idx", "content"],
        )

        hits = []
        for hit in results[0]:
            hits.append({
                "doc_id": hit["entity"]["doc_id"],
                "chunk_idx": hit["entity"]["chunk_idx"],
                "content": hit["entity"]["content"],
                "score": hit["distance"],
            })
        return hits


    def get_chunks_by_doc(self, doc_id: int) -> list[dict]:
        """按 doc_id 查询所有 chunk，按 chunk_idx 排序后返回"""
        self.client.load_collection(COLLECTION)
        results = self.client.query(
            collection_name=COLLECTION,
            filter=f'doc_id == {doc_id}',
            output_fields=["chunk_idx", "content"],
        )
        results.sort(key=lambda r: r["chunk_idx"])
        return [{"chunk_idx": r["chunk_idx"], "content": r["content"]} for r in results]

    # ------------------------------------------------------------------
    # 删除
    # ------------------------------------------------------------------

    def delete_document(self, doc_id: int):
        """删除某个文档的所有向量块"""
        self.client.delete(COLLECTION, filter=f'doc_id == {doc_id}')

    def delete_documents_by_user(self, user_id: str):
        """删除某个用户的所有向量块"""
        self.client.delete(COLLECTION, filter=f'user_id == "{user_id}"')

    def doc_count(self) -> int:
        """统计所有文档数（按 doc_id 去重）"""
        # Milvus 没有 COUNT DISTINCT，这里用所有记录数近似
        stats = self.client.get_collection_stats(COLLECTION)
        return stats.get("row_count", 0)

    def close(self):
        self.client.close()


# 全局单例
_vector_store: VectorStore | None = None


def get_vector_store() -> VectorStore:
    global _vector_store
    if _vector_store is None:
        _vector_store = VectorStore()
    return _vector_store


def reset_vector_store():
    """重置单例（仅测试用）"""
    global _vector_store
    if _vector_store:
        _vector_store.close()
    _vector_store = VectorStore()