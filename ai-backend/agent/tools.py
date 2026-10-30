"""Agent 工具：业务 HTTP 使用原生异步，写操作不重试。"""

from datetime import datetime
import json

import httpx
from langchain_core.tools import tool
from langchain_core.runnables import RunnableConfig
from pydantic import BaseModel, Field

from common.business_client import business_client


def _result(ok: bool, *, data=None, code="", message="") -> str:
    payload = {"ok": ok}
    if ok:
        payload["data"] = data
    else:
        payload["error"] = {"code": code, "message": message}
    return json.dumps(payload, ensure_ascii=False)


@tool
async def get_user_identity(config: RunnableConfig) -> str:
    """从协会业务系统获取当前登录用户的信息。"""
    token = config.get("configurable", {}).get("token", "")
    if not token:
        return _result(False, code="unauthorized", message="未登录，无法获取用户身份")
    result = await business_client.get_user_info(token)
    if isinstance(result, dict) and str(result.get("code")) == "200":
        return _result(True, data=result.get("data", {}))
    return _result(False, code="business_rejected", message="获取用户信息失败")


class ArticleInput(BaseModel):
    type: int = Field(..., ge=1, le=6, description="文章分类：1=C语言，2=HTML，3=CSS，4=JavaScript，5=Java，6=MySQL")
    head: str = Field(..., min_length=2, max_length=100, description="文章标题，2–100字")
    content: str = Field(..., min_length=10, max_length=5000, description="文章正文 HTML，10–5000字")


@tool(args_schema=ArticleInput)
async def publish_article(type: int, head: str, content: str, config: RunnableConfig) -> str:
    """按用户要求直接发表技术文章。不可自动重试；结果未知时提示用户检查文章列表。"""
    token = config.get("configurable", {}).get("token", "")
    if not token:
        return _result(False, code="unauthorized", message="发布失败：未登录")
    try:
        result = await business_client.post(
            "/user/articles", json_data={"type": type, "head": head, "content": content}, token=token,
        )
    except httpx.HTTPStatusError as exc:
        if exc.response.status_code in (400, 401, 403, 422):
            return _result(False, code="business_rejected", message="发布被拒绝，请检查登录状态或文章参数")
        return _result(False, code="result_unknown", message="发布结果未知，请检查文章列表，不要重复提交")
    except (httpx.RequestError, ValueError):
        return _result(False, code="result_unknown", message="发布结果未知，请检查文章列表，不要重复提交")
    if not isinstance(result, dict) or "code" not in result:
        return _result(False, code="result_unknown", message="发布结果未知，请检查文章列表，不要重复提交")
    if str(result["code"]) == "200":
        return _result(True, data={"message": f"文章《{head}》发布成功"})
    return _result(False, code="business_rejected", message="发布被业务服务拒绝")


@tool
def get_current_time() -> str:
    """获取当前系统时间，返回 ISO 格式的日期时间字符串。"""
    return datetime.now().isoformat()
