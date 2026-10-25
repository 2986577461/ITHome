"""内部业务请求必须直连，并转发当前用户凭证。"""

import asyncio
import json
from unittest.mock import patch

import httpx
import pytest

from common.business_client import BusinessClient


@pytest.mark.parametrize("method", ["get", "post", "put", "delete"])
def test_business_requests_bypass_proxy_and_forward_token(method):
    original_client = httpx.AsyncClient
    requests = []

    def respond(request):
        requests.append(request)
        return httpx.Response(200, json={"code": 200, "data": {"studentId": "alice"}})

    def client_factory(**kwargs):
        assert kwargs["trust_env"] is False
        assert kwargs["timeout"] == 10
        return original_client(**kwargs, transport=httpx.MockTransport(respond))

    business = BusinessClient("http://localhost:8080/")
    kwargs = {"token": "test-token"}
    if method in ("post", "put"):
        kwargs["json_data"] = {"head": "title"}
    elif method == "get":
        kwargs["params"] = {"page": 1}
    with patch("common.business_client.httpx.AsyncClient", side_effect=client_factory):
        result = asyncio.run(getattr(business, method)("/user/users", **kwargs))
    assert result["data"]["studentId"] == "alice"
    assert len(requests) == 1
    request = requests[0]
    assert request.method == method.upper()
    assert request.url.host == "localhost"
    assert request.url.port == 8080
    assert request.headers["Authorization"] == "test-token"
    if method in ("post", "put"):
        assert json.loads(request.content) == {"head": "title"}
    elif method == "get":
        assert request.url.params["page"] == "1"
