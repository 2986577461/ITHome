"""业务工具保持直接发布，但无隐式重试，参数与 Java DTO 对齐。"""

import asyncio
import json
import sys
from types import SimpleNamespace
from unittest.mock import AsyncMock

import httpx
import pytest
from pydantic import ValidationError

from agent.tools import ArticleInput, business_client, get_user_identity, publish_article


@pytest.mark.parametrize("code", [200, "200"])
def test_identity_accepts_numeric_and_string_business_codes(monkeypatch, code):
    fetch = AsyncMock(return_value={"code": code, "data": {"studentId": "alice"}})
    monkeypatch.setattr(business_client, "get_user_info", fetch)
    value = asyncio.run(get_user_identity.ainvoke({}, config={"configurable": {"token": "current"}}))
    assert json.loads(value) == {"ok": True, "data": {"studentId": "alice"}}
    fetch.assert_awaited_once_with("current")


@pytest.mark.parametrize("field,value", [("type", 0), ("type", 7), ("head", "x"), ("head", "x" * 101),
                                        ("content", "short"), ("content", "x" * 5001)])
def test_article_schema_matches_java_boundaries(field, value):
    args = {"type": 5, "head": "valid title", "content": "<p>valid content</p>"}
    args[field] = value
    with pytest.raises(ValidationError):
        ArticleInput(**args)


@pytest.mark.parametrize("status,code", [(401, "business_rejected"), (500, "result_unknown"), (503, "result_unknown")])
def test_publish_http_failures_do_not_retry(monkeypatch, status, code):
    request = httpx.Request("POST", "http://business/user/articles")
    error = httpx.HTTPStatusError("private upstream details", request=request, response=httpx.Response(status, request=request))
    post = AsyncMock(side_effect=error)
    monkeypatch.setattr(business_client, "post", post)
    result = asyncio.run(publish_article.ainvoke(
        {"type": 5, "head": "title", "content": "<p>valid content</p>"},
        config={"configurable": {"token": "current"}},
    ))
    assert json.loads(result)["error"]["code"] == code
    assert post.await_count == 1
    assert "private upstream details" not in result


def test_chat_tool_token_matches_header_auth_priority(monkeypatch):
    from agent import router
    captured = []
    async def fake_stream(*args, **kwargs):
        captured.append(args)
        yield "data: [DONE]\n\n"
    monkeypatch.setattr(router, "stream_chat", fake_stream)
    monkeypatch.setitem(sys.modules, "main", SimpleNamespace(model=object(), web_search=object()))
    async def scenario():
        response = await router.chat_stream("q", "thread", "alice", "query-token", "header-token")
        events = [event async for event in response.body_iterator]
        assert events == ["data: [DONE]\n\n"]
    asyncio.run(scenario())
    assert captured[0][3] == "header-token"
