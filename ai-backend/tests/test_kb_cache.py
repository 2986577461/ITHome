from concurrent.futures import ThreadPoolExecutor
import importlib
import time
from unittest.mock import Mock

from state.manager import CacheManager


def test_bad_cache_json_is_a_miss():
    cache = CacheManager()
    cache._r = Mock()
    cache._r.get.return_value = "{broken json"
    assert cache.get_cached_kb_docs("alice") is None
    assert cache.get_cached_kb_doc_content(1, "alice") is None


def test_new_content_namespace_does_not_read_old_full_text_cache():
    cache = CacheManager()
    cache._r = Mock()
    cache.get_cached_kb_doc_content(1, "alice")
    cache._r.get.assert_called_once_with("kb:doc:v2:alice:1")


def test_vector_store_singleton_initializes_once_under_concurrency(monkeypatch):
    module = importlib.import_module("common.vector_store")
    sentinel = object()

    def create():
        time.sleep(0.02)
        return sentinel

    factory = Mock(side_effect=create)
    monkeypatch.setattr(module, "_vector_store", None)
    monkeypatch.setattr(module, "VectorStore", factory)
    with ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(lambda _: module.get_vector_store(), range(8)))
    assert all(result is sentinel for result in results)
    factory.assert_called_once()
