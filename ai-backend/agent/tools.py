"""Agent 工具定义"""

import asyncio
from datetime import datetime

from langchain_core.tools import tool
from langchain_core.runnables import RunnableConfig
from pydantic import BaseModel as PydanticBaseModel
from pydantic import Field

from common.business_client import business_client


@tool
def get_user_identity(config: RunnableConfig) -> str:
    """从协会业务系统获取当前登录用户的信息"""
    runtime = config.get("configurable", {})
    token = runtime.get("token", "")
    if not token:
        return "未登录，无法获取用户身份"

    async def _fetch():
        return await business_client.get_user_info(token)

    result = asyncio.run(_fetch())
    if result.get("code") == "200":
        return str(result.get("data", {}))
    return f"获取用户信息失败: {result.get('msg', '未知错误')}"


class ArticleInput(PydanticBaseModel):
    type: int = Field(...,
                      description="文章类型编号：1=c/c++, 2=前端, 3=数据结构与算法, 4=mysql数据库, 5=java, 6=python/AI")
    head: str = Field(..., description="文章标题，15字以内")
    content: str = Field(..., description="文章正文 HTML")


@tool(args_schema=ArticleInput)
def publish_article(type: int, head: str, content: str, config: RunnableConfig) -> str:
    """你以agent的身份在协会网站上发表一篇技术文章。"""

    runtime = config.get("configurable", {})
    token = runtime.get("token", "")
    if not token:
        return "发布失败：未登录"

    async def _fetch():
        return await business_client.post(
            "/user/articles",
            json_data={"type": type, "head": head, "content": content},
            token=token,
        )

    result = asyncio.run(_fetch())
    if result.get("code") == "200":
        return f"文章《{head}》发布成功！"
    return f"发布失败: {result.get('msg', '未知错误')}"


@tool
def get_current_time() -> str:
    """获取当前系统时间，返回 ISO 格式的日期时间字符串。"""
    return datetime.now().isoformat()