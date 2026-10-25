"""Redis 缓存管理器 — 知识库文档缓存"""

import json
import logging
import os
from typing import Optional

import redis as _redis

logger = logging.getLogger(__name__)


class CacheManager:
    """Redis 缓存（仅 KB 文档列表和全文）"""

    def __init__(self, redis_url: str = ""):
        redis_url = redis_url or os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self._r = _redis.from_url(
            redis_url, decode_responses=True, socket_connect_timeout=1, socket_timeout=1,
        )

    def _read(self, key: str):
        try:
            raw = self._r.get(key)
            return json.loads(raw) if raw else None
        except (_redis.RedisError, ValueError, TypeError):
            logger.warning("知识库缓存读取失败，回源数据库 key=%s", key, exc_info=True)
            return None

    def _write(self, key: str, value, ttl: int) -> None:
        try:
            self._r.set(key, json.dumps(value, ensure_ascii=False), ex=ttl)
        except _redis.RedisError:
            logger.warning("知识库缓存写入失败 key=%s", key, exc_info=True)

    def _invalidate(self, key: str) -> None:
        try:
            self._r.delete(key)
        except _redis.RedisError:
            logger.warning("知识库缓存失效失败 key=%s", key, exc_info=True)

    # ---- KB 文档列表 ----

    def _kbc_key(self, user_id: str) -> str:
        return f"kb:docs:{user_id}"

    def cache_kb_docs(self, user_id: str, docs: list) -> None:
        self._write(self._kbc_key(user_id), docs, 3600)

    def get_cached_kb_docs(self, user_id: str) -> list | None:
        return self._read(self._kbc_key(user_id))

    def invalidate_kb_docs_cache(self, user_id: str) -> None:
        self._invalidate(self._kbc_key(user_id))

    # ---- 文档全文 ----

    def _kbd_key(self, doc_id: int, user_id: str) -> str:
        # 不复用旧版通过重叠片段直接拼接得到的全文缓存。
        return f"kb:doc:v2:{user_id}:{doc_id}"

    def cache_kb_doc_content(self, doc_id: int, user_id: str, data: dict) -> None:
        self._write(self._kbd_key(doc_id, user_id), data, 7200)

    def get_cached_kb_doc_content(self, doc_id: int, user_id: str) -> dict | None:
        return self._read(self._kbd_key(doc_id, user_id))

    def invalidate_kb_doc_content_cache(self, doc_id: int, user_id: str) -> None:
        self._invalidate(self._kbd_key(doc_id, user_id))


# 全局单例
_manager: Optional[CacheManager] = None


def get_cache_manager() -> CacheManager:
    global _manager
    if _manager is None:
        _manager = CacheManager()
    return _manager
