"""Redis 缓存管理器 — 知识库文档缓存"""

import json
import os
from typing import Optional

import redis as _redis


class CacheManager:
    """Redis 缓存（仅 KB 文档列表和全文）"""

    def __init__(self, redis_url: str = ""):
        redis_url = redis_url or os.getenv("REDIS_URL", "redis://localhost:6379/0")
        self._r = _redis.from_url(redis_url, decode_responses=True)

    # ---- KB 文档列表 ----

    def _kbc_key(self, user_id: str) -> str:
        return f"kb:docs:{user_id}"

    def cache_kb_docs(self, user_id: str, docs: list) -> None:
        self._r.set(self._kbc_key(user_id),
                     json.dumps(docs, ensure_ascii=False), ex=3600)

    def get_cached_kb_docs(self, user_id: str) -> list | None:
        raw = self._r.get(self._kbc_key(user_id))
        return json.loads(raw) if raw else None

    def invalidate_kb_docs_cache(self, user_id: str) -> None:
        self._r.delete(self._kbc_key(user_id))

    # ---- 文档全文 ----

    def _kbd_key(self, doc_id: int, user_id: str) -> str:
        return f"kb:doc:{user_id}:{doc_id}"

    def cache_kb_doc_content(self, doc_id: int, user_id: str, data: dict) -> None:
        self._r.set(self._kbd_key(doc_id, user_id),
                     json.dumps(data, ensure_ascii=False), ex=7200)

    def get_cached_kb_doc_content(self, doc_id: int, user_id: str) -> dict | None:
        raw = self._r.get(self._kbd_key(doc_id, user_id))
        return json.loads(raw) if raw else None

    def invalidate_kb_doc_content_cache(self, doc_id: int, user_id: str) -> None:
        self._r.delete(self._kbd_key(doc_id, user_id))


# 全局单例
_manager: Optional[CacheManager] = None


def get_cache_manager() -> CacheManager:
    global _manager
    if _manager is None:
        _manager = CacheManager()
    return _manager
