"""
协会网站智能聊天机器人 —— FastAPI 后端服务

功能：
  1. 接收前端 axios 请求，流式返回 AI 回复（SSE）
  2. 通过 Tavily 工具实时查询网络信息
  3. 对话记忆存储在 SQLite 数据库中
  4. 支持多会话隔离（通过 thread_id）

启动方式：python3 Agent.py
API 文档：启动后访问 http://localhost:8000/docs
"""

import asyncio
import os
from datetime import datetime

from dotenv import load_dotenv
# ---------- FastAPI ----------
from fastapi import FastAPI, Query, Depends
from fastapi.middleware.cors import CORSMiddleware  # 允许前端跨域访问
from fastapi.responses import StreamingResponse, FileResponse  # 流式响应 + 静态文件
# ---------- LangChain / LangGraph ----------
from langchain.agents import create_agent  # 创建智能体（agent）
from langchain.chat_models import init_chat_model  # 自动识别模型提供商（DeepSeek）
from langchain_tavily import TavilySearch  # Tavily 联网搜索工具
from langgraph.checkpoint.sqlite import SqliteSaver  # SQLite 持久化对话状态
from pydantic import Field  # 请求/响应数据校验

load_dotenv()

# JWT 鉴权已提取到 conversation_manager.py
from conversation_manager import get_current_user

DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)
DB_PATH = os.path.join(DB_DIR, "conversations.db")  # SQLite 数据库文件（LangGraph 用）
SYSTEM_PROMPT_PATH = os.path.join(os.path.dirname(__file__), "SystemPrompt.md")

# 知识库（向量化、检索、API）已提取到 knowledge_base_manager
from knowledge_base_manager import search_knowledge_base, kb_router


def load_system_prompt() -> str:
    """从 SystemPrompt.md 加载系统提示词"""
    try:
        with open(SYSTEM_PROMPT_PATH, "r", encoding="utf-8") as f:
            return f.read().strip()
    except FileNotFoundError:
        return "你是一个协会网站的智能客服助手。"  # 文件丢失时的兜底


# ============================================================
# 二、创建模型和工具
# ============================================================

# 联网搜索工具（API Key 从环境变量 TAVILY_API_KEY 自动读取）
web_search = TavilySearch(
    max_results=3,  # 每次搜索最多返回 3 条结果
    description="用于搜索实时信息",  # 告诉 LLM 这个工具何时使用
)

# 获取用户身份工具（调用业务模块 localhost:8080）
from langchain_core.tools import tool
from langchain_core.runnables import RunnableConfig


@tool
def get_user_identity(config: RunnableConfig) -> str:
    """
    从协会业务系统获取当前登录用户的信息（id、姓名、学号、身份、学院、专业、班级、性别、文章数量、资料数量）。
    """

    from business_client import business_client

    runtime = config.get("configurable", {})
    token = runtime.get("token", "")
    if not token:
        return "未登录，无法获取用户身份"

    # agent.stream() 是同步调用，但 business_client 是 async，用 asyncio.run 桥接
    async def _fetch():
        return await business_client.get_user_info(token)

    result = asyncio.run(_fetch())
    # 业务模块返回 {"code":"200", "data":{studentId, name, ...}}
    if result.get("code") == "200":
        return str(result.get("data", {}))
    return f"获取用户信息失败: {result.get('msg', '未知错误')}"


# 发表文章工具（调用业务模块 localhost:8080）
from pydantic import BaseModel as PydanticBaseModel


class ArticleInput(PydanticBaseModel):
    """发表文章的参数结构，供 LLM 填充"""
    type: int = Field(...,
                      description="文章类型编号：1=c/c++, 2=前端, 3=数据结构与算法, 4=mysql数据库, 5=java, 6=python/AI")
    head: str = Field(..., description="文章标题，15字以内")
    content: str = Field(
        ...,
        description=(
            "文章正文 HTML。支持的标签：<h1>~<h7> 标题, <p> 段落, <strong> 加粗, <em> 倾斜, "
            "<ul><li> 无序列表, <ol><li> 有序列表, <pre class=\"code-block\"><code> 代码块, "
            "<a target=\"_blank\" href=\"...\"> 链接。"
            "段落对齐用 style=\"text-align: left/right/center\"。"
            "代码块必须严格按 <pre class=\"code-block\"><code>代码...</code></pre> 格式，代码内换行用 \\n。"
            "必须严格使用这些标签，不得自创任何其他标签或属性。"
        ),
    )


@tool(args_schema=ArticleInput)
def publish_article(type: int, head: str, content: str, config: RunnableConfig) -> str:
    """
    帮用户在协会网站上发表一篇技术文章。
    当用户说"帮我发一篇文章"、"发表文章"、"写一篇文章发出去"、"发布"等时调用。
    type 根据文章主题判断：1=c/c++, 2=前端, 3=数据结构与算法, 4=mysql数据库, 5=java, 6=python/AI
    content 必须是严格 HTML，只允许 <h1>-<h7>, <p>, <strong>, <em>, <ul><li>, <ol><li>,
    <pre class=\"code-block\"><code>, <a target=\"_blank\" href=\"...\">, style=\"text-align:left/right/center\"。
    发布的内容按照"语气与态度"一栏的要求
    """
    import asyncio as _asyncio
    from business_client import business_client

    runtime = config.get("configurable", {})
    token = runtime.get("token", "")
    if not token:
        return "发布失败：未登录，无法获取身份凭据"

    # agent.stream() 是同步调用，但 business_client 是 async，用 asyncio.run 桥接
    async def _fetch():
        return await business_client.post(
            "/user/articles",
            json_data={"type": type, "head": head, "content": content},
            token=token,
        )

    result = _asyncio.run(_fetch())
    # 业务模块返回 {"code":"200", "data":{...}}
    if result.get("code") == "200":
        return f"文章《{head}》发布成功！"
    return f"发布失败: {result.get('msg', '未知错误')}"


# 对话模型（API Key 从环境变量 DEEPSEEK_API_KEY 自动读取）
model = init_chat_model(model="deepseek-v4-flash")

# ============================================================
# 三、应用数据库（会话管理 & 消息记录）
# ============================================================
# 应用数据库（app.db）已提取到 conversation_manager，自动建表
from conversation_manager import ensure_conversation_and_save_user, save_ai_message, generate_and_save_title



# ============================================================
# 四、FastAPI 应用
# ============================================================
app = FastAPI(
    title="协会网站聊天机器人",
    description="支持联网搜索、SQLite 记忆、流式输出的智能对话接口",
    version="1.0.0",
)

# 允许前端跨域访问（开发时前端可能在不同端口或直接用 file:// 打开）
# 注册 conversation 模块的路由（会话 CRUD）
from conversation_manager import conversation_router

app.include_router(conversation_router)

# 注册 knowledge_base 模块的路由（知识库文档管理）
app.include_router(kb_router)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源（生产环境应限制为具体域名）
    allow_methods=["*"],  # 允许所有 HTTP 方法
    allow_headers=["*"],  # 允许所有请求头
)


# 知识库 API 已提取到 knowledge_base_manager（kb_router）


# ---- 时间工具 ----
@tool
def get_current_time() -> str:
    """获取当前系统时间，返回 ISO 格式的日期时间字符串。当用户询问"昨天、今年，上个月"等关键词时调用"""
    return datetime.now().isoformat()


# ---------- 流式聊天接口（GET） ----------
@app.get("/chat-stream", summary="流式聊天 SSE（EventSource 用）")
async def chat_stream(question: str, thread_id: str = "default",
                      user_id: str = Depends(get_current_user),
                      token: str = Query(default="")):
    """
    供前端 EventSource 调用的 GET 接口。
    EventSource 不支持自定义 Header，通过 ?token=xxx 传递 JWT。
    """
    return StreamingResponse(
        _stream_chat(question, thread_id, user_id, token),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


# ---------- SSE 流式生成器 ----------
async def _stream_chat(question: str, thread_id: str, user_id: str = "", token: str = ""):
    """
    核心流式逻辑：
    1. 自动创建会话 + 保存用户消息到 app.db
    2. agent.stream() 在独立线程运行，避免阻塞事件循环
    3. 通过 asyncio.Queue 将 token 从工作线程传到 async 生成器
    4. 异步地从队列取 token，逐个通过 SSE 推送到前端
    5. 流结束后保存 AI 完整回复到 app.db
    """
    from concurrent.futures import ThreadPoolExecutor
    from langchain_core.messages import AIMessageChunk, ToolMessage

    # ---- 0. 确保会话记录存在 + 保存用户消息（已提取到 conversation_manager） ----
    thread_id = ensure_conversation_and_save_user(thread_id, question, user_id)

    queue: asyncio.Queue = asyncio.Queue()  # 线程间通信的队列
    loop = asyncio.get_running_loop()  # 在主协程中捕获事件循环，供子线程使用

    # 用列表收集 AI 回复 token + 搜索信息
    ai_reply_chunks: list[str] = []
    search_info: dict = {}  # {"web": [...], "kb": [...], "split_pos": 0}

    def run_agent():
        """
        在独立线程中运行 agent.stream()（同步操作）。
        每拿到一个 token 就 put 到队列，async 主循环再从队列取走推送。
        """
        nonlocal ai_reply_chunks
        checkpointer_ctx = SqliteSaver.from_conn_string(DB_PATH)
        checkpointer = checkpointer_ctx.__enter__()
        try:
            agent = create_agent(
                model=model,
                tools=[get_current_time, search_knowledge_base, web_search, get_user_identity,publish_article],
                checkpointer=checkpointer,
                system_prompt=load_system_prompt(),
            )
            # 用 user_id 前缀隔离不同用户的 LangGraph checkpoint，防止 agent 记忆串号
            # 同时注入 token，供 get_user_identity 工具调用业务模块使用
            config = {"configurable": {"thread_id": f"{user_id}:{thread_id}", "token": token, "user_id": user_id}}
            searching_sent = False  # 避免重复发送 [SEARCHING]
            current_tool_name = ""  # 记录当前正在调用的工具名
            for chunk, metadata in agent.stream(
                    {"messages": [{"role": "user", "content": question}]},
                    config=config,
                    stream_mode="messages"):
                if isinstance(chunk, AIMessageChunk):
                    # 检测到 tool call → 通知前端"正在搜索"
                    if (chunk.tool_calls or chunk.tool_call_chunks) and not searching_sent:
                        # 提取工具名（兼容 dict 和 ToolCallChunk 对象两种格式）
                        if chunk.tool_call_chunks:
                            tc = chunk.tool_call_chunks[0]
                            if isinstance(tc, dict):
                                name = tc.get("name") or ""
                            else:
                                name = getattr(tc, "name", "") or ""
                            # 只在拿到非空名字时才更新，防止后续 chunk 的 None 覆盖掉正确值
                            if name:
                                current_tool_name = name
                        # 只有 KB 和 web 搜索才需要前端展示搜索栏，其他工具跳过
                        _show_searching = ("knowledge" in current_tool_name.lower() or "tavily" in current_tool_name.lower())
                        if _show_searching:
                            searching_sent = True
                            search_info["split_pos"] = len("".join(ai_reply_chunks))
                            if "knowledge" in current_tool_name.lower():
                                marker = "[[KB_MARK]]"
                                search_info["kb_marker_pos"] = search_info["split_pos"]
                            else:
                                marker = "[[WEB_MARK]]"
                                search_info["web_marker_pos"] = search_info["split_pos"]

                            loop.call_soon_threadsafe(queue.put_nowait, marker)
                            loop.call_soon_threadsafe(queue.put_nowait, "[SEARCHING]")
                    # 普通文本 token
                    if chunk.content and not chunk.tool_calls and not chunk.tool_call_chunks:
                        ai_reply_chunks.append(chunk.content)
                        loop.call_soon_threadsafe(queue.put_nowait, chunk.content)
                elif isinstance(chunk, ToolMessage) or type(chunk).__name__ == "ToolMessage":
                    # 只有 KB 和 web 搜索才发送搜索结果标记给前端
                    _is_search_tool = ("knowledge" in current_tool_name.lower() or "tavily" in current_tool_name.lower())
                    current_tool_name = ""

                    if not _is_search_tool:
                        continue
                    # 搜索完成 → 发给前端，同时收集结果用于持久化
                    searching_sent = False
                    results_json = _format_search_results(chunk.content)
                    # 区分 KB 搜索和 web 搜索
                    raw = str(chunk.content)
                    if raw.startswith("[KB]"):
                        _parse_kb_results(raw, search_info)
                        loop.call_soon_threadsafe(queue.put_nowait, f"[KB_RESULT]{results_json}")
                    else:
                        _parse_web_results(raw, search_info)
                        loop.call_soon_threadsafe(queue.put_nowait, f"[SEARCH_RESULT]{results_json}")
        except Exception as e:
            error_msg = str(e)
            if "insufficient tool messages" in error_msg or "tool_calls" in error_msg:
                loop.call_soon_threadsafe(queue.put_nowait, "[ERROR] 网络错误，请刷新页面重试")
            else:
                loop.call_soon_threadsafe(queue.put_nowait, f"[ERROR] {e}")
        finally:
            try:
                checkpointer_ctx.__exit__(None, None, None)
            except Exception:
                pass
            # 发送结束信号
            loop.call_soon_threadsafe(queue.put_nowait, None)

    # 启动独立线程跑 agent
    executor = ThreadPoolExecutor(max_workers=1)
    executor.submit(run_agent)

    # 异步循环：逐 token 从队列取数据，通过 SSE 发送
    while True:
        data = await queue.get()  # 真正异步等待，不阻塞事件循环
        if data is None:
            # ---- agent 结束：保存回复+生成标题（已提取到 conversation_manager） ----
            full_reply = "".join(ai_reply_chunks).replace("[[KB_MARK]]", "").replace("[[WEB_MARK]]", "")
            if full_reply:
                save_ai_message(thread_id, full_reply, search_info, user_id)
                generate_and_save_title(thread_id, question, full_reply, model)
            yield "data: [DONE]\n\n"
            break
        if str(data).startswith("[ERROR]"):
            yield f"data: {data}\n\n"
            break
        yield f"data: {_sse_escape(data)}\n\n"


def _format_search_results(tool_content: str) -> str:
    """
    把工具返回的搜索结果解析为前端可渲染的 JSON。
    支持 KB 格式 [KB]{json} 和 Tavily 格式 {query:..., results:[...]}。
    """
    import json as _json

    raw = tool_content if isinstance(tool_content, str) else str(tool_content)

    # KB 工具专用格式：[KB]{json}\n---\n{context}
    if raw.startswith("[KB]"):
        try:
            kb_json_str = raw[4:].split("\n---\n", 1)[0]
            return kb_json_str
        except Exception:
            return _json.dumps({"results": []})

    # Tavily 格式处理
    try:
        parsed = _json.loads(raw)
    except (_json.JSONDecodeError, TypeError):
        return _json.dumps({"results": []})

    if isinstance(parsed, dict) and "results" in parsed:
        parsed = parsed["results"]

    if isinstance(parsed, list):
        results = []
        for item in parsed:
            if isinstance(item, dict):
                results.append({
                    "url": item.get("url", ""),
                    "title": item.get("title", ""),
                    "snippet": (item.get("content", "") or "")[:120],
                })
        return _json.dumps({"results": results}, ensure_ascii=False)

    return _json.dumps({"results": []})


def _parse_kb_results(raw: str, info: dict):
    """从 KB 工具返回中提取搜索卡片数据"""
    try:
        if raw.startswith("[KB]"):
            json_str = raw[4:].split("\n---\n", 1)[0]
            info["kb"] = __import__('json').loads(json_str).get("results", [])
    except Exception:
        pass


def _parse_web_results(raw: str, info: dict):
    """从 Tavily 工具返回中提取搜索卡片数据"""
    try:
        parsed = __import__('json').loads(raw) if raw.startswith("{") else None
        if parsed and isinstance(parsed, dict) and "results" in parsed:
            info["web"] = [
                {"url": r.get("url", ""), "title": r.get("title", ""), "snippet": (r.get("content", "") or "")[:120]}
                for r in parsed["results"] if isinstance(r, dict)
            ]
    except Exception:
        pass


def _sse_escape(text: str) -> str:
    """
    转义 SSE 消息中的特殊字符：
    - 换行符替换为 \n（SSE 协议要求单行数据）
    """
    return text.replace("\n", "\\n")


# ---------- 健康检查接口 ----------
@app.get("/health", summary="健康检查")
def health():
    """返回服务状态，前端可用于检测后端是否在线"""
    return {"status": "ok", "service": "协会网站聊天机器人"}


# ---------- 前端页面 ----------
@app.get("/", response_class=FileResponse)
def index():
    """访问根路径直接返回前端聊天页面"""
    return FileResponse(os.path.join(os.path.dirname(__file__), "frontend.html"))


# ============================================================
# 五、启动入口
# ============================================================
if __name__ == "__main__":
    import uvicorn

    # host="0.0.0.0" 允许局域网其他设备访问
    # reload=True 文件改动时自动重启（开发用）
    uvicorn.run(
        "Agent:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )