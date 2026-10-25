"""仅使用临时 SQLite；鉴权、Redis、向量化由测试替身提供。"""

import importlib
from pathlib import Path
import sqlite3
import sys
from types import SimpleNamespace
from unittest.mock import Mock, patch

import pytest
from fastapi import FastAPI, Header, HTTPException
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


@pytest.fixture(scope="session")
def modules(tmp_path_factory):
    # 历史模块在 import 时建表。重定向这些连接，测试不能打开开发数据库。
    root = tmp_path_factory.mktemp("module-init")
    connect = sqlite3.connect

    def isolated_connect(database, *args, **kwargs):
        if str(database).endswith(("kb.db", "app.db")):
            database = root / Path(database).name
        return connect(database, *args, **kwargs)

    with patch("sqlite3.connect", side_effect=isolated_connect):
        router = importlib.import_module("kb.router")
        tools = importlib.import_module("kb.tools")
    return SimpleNamespace(router=router, tools=tools)


@pytest.fixture
def kb(modules, tmp_path, monkeypatch):
    monkeypatch.setattr(modules.tools, "KB_DB_PATH", str(tmp_path / "kb.db"))
    modules.tools.init_kb_db()
    vectors = Mock()
    vectors.insert_chunks.side_effect = lambda doc_id, chunks, user_id: len(chunks)
    vectors.get_chunks_by_doc.return_value = []
    monkeypatch.setattr(modules.router, "get_vector_store", lambda: vectors)

    from state.manager import CacheManager
    cache = CacheManager()
    values = {}
    cache._r = Mock()
    cache._r.get.side_effect = values.get
    cache._r.set.side_effect = lambda key, value, **kwargs: values.update({key: value})
    cache._r.delete.side_effect = lambda key: values.pop(key, None)
    monkeypatch.setattr(modules.router, "get_cache_manager", lambda: cache)

    def identity(x_test_user: str = Header(default="")):
        if not x_test_user:
            raise HTTPException(401, "请先登录")
        return x_test_user

    app = FastAPI()
    app.include_router(modules.router.kb_router)
    app.dependency_overrides[modules.router.get_current_user] = identity
    with TestClient(app, headers={"x-test-user": "alice"}) as client:
        yield SimpleNamespace(client=client, tools=modules.tools, router=modules.router,
                              vectors=vectors, cache=cache, values=values)
