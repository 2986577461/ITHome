"""真实鉴权依赖的状态码与凭证转发回归测试，不调用 Java 服务。"""

import asyncio
import importlib
from unittest.mock import AsyncMock

import httpx
import pytest
from fastapi import HTTPException


@pytest.fixture
def auth(modules, monkeypatch):
    service = importlib.import_module("conversation.service")
    fetch = AsyncMock(return_value={"code": "200", "data": {"studentId": "alice"}})
    monkeypatch.setattr(service.business_client, "get_user_info", fetch)
    return service.get_current_user, fetch


def test_missing_token_is_401_without_business_call(auth):
    dependency, fetch = auth
    with pytest.raises(HTTPException) as error:
        asyncio.run(dependency(authorization="", token=""))
    assert error.value.status_code == 401
    fetch.assert_not_called()


@pytest.mark.parametrize("header,query,expected", [
    ("header-token", "", "header-token"),
    ("", "query-token", "query-token"),
    ("header-token", "query-token", "header-token"),
])
def test_authenticated_identity_and_token_forwarding(auth, header, query, expected):
    dependency, fetch = auth
    assert asyncio.run(dependency(authorization=header, token=query)) == "alice"
    fetch.assert_awaited_once_with(expected)


@pytest.mark.parametrize("upstream,expected", [(401, 401), (403, 401), (404, 503), (500, 503)])
def test_upstream_status_is_not_always_reported_as_invalid_login(auth, upstream, expected):
    dependency, fetch = auth
    response = httpx.Response(upstream, request=httpx.Request("GET", "http://business/user/users"))
    fetch.side_effect = httpx.HTTPStatusError("upstream failed", request=response.request, response=response)
    with pytest.raises(HTTPException) as error:
        asyncio.run(dependency(authorization="token", token=""))
    assert error.value.status_code == expected


@pytest.mark.parametrize("exception", [httpx.ConnectError("offline"), httpx.ReadTimeout("timeout")])
def test_unavailable_business_service_is_503(auth, exception):
    dependency, fetch = auth
    fetch.side_effect = exception
    with pytest.raises(HTTPException) as error:
        asyncio.run(dependency(authorization="token", token=""))
    assert error.value.status_code == 503


@pytest.mark.parametrize("result", [None, [], {}, {"code": "500"}, {"code": "200", "data": []}, {"code": "200", "data": {}}])
def test_invalid_identity_response_is_502(auth, result):
    dependency, fetch = auth
    fetch.return_value = result
    with pytest.raises(HTTPException) as error:
        asyncio.run(dependency(authorization="token", token=""))
    assert error.value.status_code == 502


@pytest.mark.parametrize("code", [401, 403, "401", "403"])
def test_business_error_inside_http_200_is_unauthorized(auth, code):
    dependency, fetch = auth
    fetch.return_value = {"code": code, "msg": "unauthorized"}
    with pytest.raises(HTTPException) as error:
        asyncio.run(dependency(authorization="token", token=""))
    assert error.value.status_code == 401


def test_non_json_response_is_502(auth):
    dependency, fetch = auth
    fetch.side_effect = ValueError("invalid json")
    with pytest.raises(HTTPException) as error:
        asyncio.run(dependency(authorization="token", token=""))
    assert error.value.status_code == 502
