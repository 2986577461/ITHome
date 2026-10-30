"""
协会网站智能聊天机器人 —— 应用入口

启动方式：python3 main.py
"""

import os
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from langchain.chat_models import init_chat_model
from langchain_tavily import TavilySearch

load_dotenv()

# ---- 导入路由 ----
from conversation.router import conversation_router
from kb.router import kb_router
from agent.router import agent_router
from agent.service import shutdown_agent_tasks
from agent.harness import Limits



model = init_chat_model(model="deepseek-v4-flash", max_retries=0)

web_search = TavilySearch(
    max_results=3,
    description="用于搜索实时信息",
)


@asynccontextmanager
async def lifespan(app):
    Limits.from_env()  # 非法预算配置应启动失败，不等到用户发起 SSE 才报错。
    try:
        yield
    finally:
        await shutdown_agent_tasks()


app = FastAPI(
    lifespan=lifespan,
    title="协会网站聊天机器人",
    description="支持联网搜索、SQLite 记忆、流式输出的智能对话接口",
    version="1.0.0",
)

app.include_router(conversation_router)
app.include_router(kb_router)
app.include_router(agent_router)


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)



@app.get("/health", summary="健康检查")
def health():
    return {"status": "ok", "service": "协会网站聊天机器人"}


@app.get("/", response_class=FileResponse)
def index():
    return FileResponse(os.path.join(os.path.dirname(__file__), "frontend.html"))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="warning",
    )