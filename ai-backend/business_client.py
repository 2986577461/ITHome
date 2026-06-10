"""
业务模块通信客户端 — 通过 HTTP 调用隔壁 Spring Boot 服务。

开发环境：localhost:8080（本机）
生产环境：ithome:8080（Docker 容器间通信，通过环境变量 BUSINESS_BASE_URL 配置）

使用方式：
    from business_client import business_client
    data = await business_client.get("/user/common/url")
"""

import os
import httpx

BUSINESS_BASE_URL = os.getenv("BUSINESS_BASE_URL", "http://localhost:8080")


class BusinessClient:
    """
    封装对业务模块的 HTTP 调用。
    支持 GET / POST / PUT / DELETE，自动携带 JWT token 转发。
    """

    def __init__(self, base_url: str = BUSINESS_BASE_URL):
        self.base_url = base_url.rstrip("/")

    # ---------- 通用请求方法 ----------

    async def get(self, path: str, *, params: dict | None = None, token: str | None = None):
        """GET 请求业务模块"""
        async with httpx.AsyncClient(timeout=10) as client:
            headers = {"Authorization": token} if token else {}
            res = await client.get(f"{self.base_url}{path}", params=params, headers=headers)
            return self._parse(res)

    async def post(self, path: str, *, json_data: dict | None = None, token: str | None = None):
        """POST 请求业务模块"""
        async with httpx.AsyncClient(timeout=10) as client:
            headers = {"Authorization": token} if token else {}
            if json_data:
                headers["Content-Type"] = "application/json"
            res = await client.post(f"{self.base_url}{path}", json=json_data, headers=headers)
            return self._parse(res)

    async def put(self, path: str, *, json_data: dict | None = None, token: str | None = None):
        """PUT 请求业务模块"""
        async with httpx.AsyncClient(timeout=10) as client:
            headers = {"Authorization": token} if token else {}
            if json_data:
                headers["Content-Type"] = "application/json"
            res = await client.put(f"{self.base_url}{path}", json=json_data, headers=headers)
            return self._parse(res)

    async def delete(self, path: str, *, token: str | None = None):
        """DELETE 请求业务模块"""
        async with httpx.AsyncClient(timeout=10) as client:
            headers = {"Authorization": token} if token else {}
            res = await client.delete(f"{self.base_url}{path}", headers=headers)
            return self._parse(res)

    # ---------- 业务常用方法 ----------

    async def get_user_info(self, token: str) -> dict:
        """获取当前登录用户信息"""
        return await self.get("/user/users", token=token)

    async def get_common_urls(self) -> dict:
        """获取常用链接"""
        return await self.get("/user/common/url")

    async def get_user_resources(self, token: str) -> dict:
        """获取用户资源"""
        return await self.get("/user/resources/all", token=token)

    # ---------- 内部方法 ----------

    @staticmethod
    def _parse(response: httpx.Response) -> dict:
        """统一解析响应"""
        try:
            return {"ok": response.is_success, "status": response.status_code, "data": response.json()}
        except Exception:
            return {"ok": response.is_success, "status": response.status_code, "data": response.text}


# 单例，全局复用
business_client = BusinessClient()
