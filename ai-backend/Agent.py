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
import sqlite3
import uuid
from datetime import datetime

import jwt as pyjwt
from dotenv import load_dotenv
# ---------- FastAPI ----------
from fastapi import FastAPI, HTTPException, Header, Query, Depends
from fastapi.middleware.cors import CORSMiddleware  # 允许前端跨域访问
from fastapi.responses import StreamingResponse, FileResponse  # 流式响应 + 静态文件
# ---------- LangChain / LangGraph ----------
from langchain.agents import create_agent  # 创建智能体（agent）
from langchain.chat_models import init_chat_model  # 自动识别模型提供商（DeepSeek）
from langchain_tavily import TavilySearch  # Tavily 联网搜索工具
from langgraph.checkpoint.sqlite import SqliteSaver  # SQLite 持久化对话状态
from pydantic import BaseModel, Field  # 请求/响应数据校验

# ---------- LangChain 消息类型 ----------

# ============================================================
# 一、初始化：加载环境变量 & 数据库路径
# ============================================================
load_dotenv()

# ---------- JWT 鉴权 ----------
JWT_SECRET = os.getenv("JWT_SECRET", "xiaoyan")
JWT_ALGORITHM = "HS256"


def _verify_jwt(token: str) -> str:
    """验证 JWT 并返回 user_id。未登录直接拒绝。"""
    if not token:
        raise HTTPException(401, "请先登录")
    try:
        payload = pyjwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
        return str(payload.get("user") or payload.get("admin") or payload.get("sub") or payload.get("userId") or payload.get("id") or "")
    except pyjwt.ExpiredSignatureError:
        raise HTTPException(401, "登录已过期，请重新登录")
    except pyjwt.InvalidTokenError:
        raise HTTPException(401, "身份验证失败")


def get_current_user(authorization: str = Header(default=""),
                     token: str = Query(default="")) -> str:
    """
    FastAPI 依赖：从 Header 或 query string 提取并验证 JWT。
    - REST API 用 Authorization header（axios 自动带）
    - SSE 用 ?token=xxx（EventSource 不支持自定义 header）
    """
    raw = ""
    if authorization:
        raw = authorization.replace("Bearer ", "").replace("bearer ", "")
    elif token:
        raw = token
    return _verify_jwt(raw)


DB_DIR = os.path.join(os.path.dirname(__file__), "db")
os.makedirs(DB_DIR, exist_ok=True)
DB_PATH = os.path.join(DB_DIR, "conversations.db")  # SQLite 数据库文件（LangGraph 用）
APP_DB_PATH = os.path.join(DB_DIR, "app.db")  # 应用自身数据（会话列表、消息记录）
KB_DB_PATH = os.path.join(DB_DIR, "kb.db")  # 知识库（文档+向量）
SYSTEM_PROMPT_PATH = os.path.join(os.path.dirname(__file__), "SystemPrompt.md")


# ---- 知识库向量化（纯 Python，零依赖） ----


def _text_to_vector(text: str) -> list[float]:
    """
    把文本转为固定长度向量（字符 bigram 哈希 + TF 权重）。
    不需要任何 embedding API，纯本地计算。
    """
    DIM = 512
    vec = [0.0] * DIM
    if not text:
        return vec
    # 字符 bigram 特征
    chars = list(text)
    for i in range(len(chars)):
        # unigram
        h = ord(chars[i]) % DIM
        vec[h] += 1.0
        # bigram
        if i + 1 < len(chars):
            h2 = (ord(chars[i]) * 31 + ord(chars[i + 1])) % DIM
            vec[h2] += 0.5
    # L2 归一化
    norm = (sum(v * v for v in vec)) ** 0.5
    if norm > 0:
        vec = [v / norm for v in vec]
    return vec


def _embed(texts: list[str]) -> list[list[float]]:
    """批量文本转向量"""
    return [_text_to_vector(t) for t in texts]


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

# 对话模型（API Key 从环境变量 DEEPSEEK_API_KEY 自动读取）
model = init_chat_model(model="deepseek-v4-flash")


# ============================================================
# 三、应用数据库（会话管理 & 消息记录）
# ============================================================
def init_app_db():
    """创建应用自身的表：conversations（会话元数据）、messages（消息记录）"""
    conn = sqlite3.connect(APP_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS conversations
                 (
                     thread_id  TEXT PRIMARY KEY,
                     title      TEXT DEFAULT '新的对话',
                     created_at TEXT NOT NULL,
                     updated_at TEXT NOT NULL
                 )
                 """)
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS messages
                 (
                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                     thread_id   TEXT NOT NULL,
                     role        TEXT NOT NULL CHECK (role IN ('user', 'ai')),
                     content     TEXT NOT NULL,
                     search_info TEXT,
                     created_at  TEXT NOT NULL,
                     FOREIGN KEY (thread_id) REFERENCES conversations (thread_id) ON DELETE CASCADE
                 )
                 """)
    # 兼容旧表：如果新列不存在则添加
    for col, col_type in [("search_info", "TEXT"), ("user_id", "TEXT DEFAULT ''")]:
        try:
            conn.execute(f"ALTER TABLE messages ADD COLUMN {col} {col_type}")
        except sqlite3.OperationalError:
            pass
    try:
        conn.execute("ALTER TABLE conversations ADD COLUMN user_id TEXT DEFAULT ''")
    except sqlite3.OperationalError:
        pass
    conn.commit()
    conn.close()


def get_app_db() -> sqlite3.Connection:
    """获取应用数据库连接（自动开启 WAL + 外键）"""
    conn = sqlite3.connect(APP_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.row_factory = sqlite3.Row
    return conn


# 启动时建表
init_app_db()


# ============================================================
# 三-B、知识库数据库
# ============================================================
def init_kb_db():
    """创建知识库表：documents（文档元数据）+ chunks（文本块+向量JSON）"""
    conn = sqlite3.connect(KB_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS documents
                 (
                     id          INTEGER PRIMARY KEY AUTOINCREMENT,
                     filename    TEXT NOT NULL,
                     file_type   TEXT NOT NULL,
                     char_count  INTEGER DEFAULT 0,
                     chunk_count INTEGER DEFAULT 0,
                     created_at  TEXT NOT NULL,
                     user_id     TEXT DEFAULT ''
                 )
                 """)
    conn.execute("""
                 CREATE TABLE IF NOT EXISTS chunks_vec
                 (
                     id        INTEGER PRIMARY KEY AUTOINCREMENT,
                     doc_id    INTEGER NOT NULL,
                     chunk_idx INTEGER NOT NULL,
                     content   TEXT    NOT NULL,
                     embedding TEXT    NOT NULL,
                     FOREIGN KEY (doc_id) REFERENCES documents (id) ON DELETE CASCADE
                 )
                 """)
    try:
        conn.execute("ALTER TABLE documents ADD COLUMN user_id TEXT DEFAULT ''")
    except sqlite3.OperationalError:
        pass
    conn.commit()
    conn.close()


def get_kb_db() -> sqlite3.Connection:
    """获取知识库数据库连接"""
    conn = sqlite3.connect(KB_DB_PATH)
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA foreign_keys=ON")
    conn.row_factory = sqlite3.Row
    return conn


init_kb_db()


# ---- 知识库核心功能 ----

def _chunk_text(text: str, chunk_size: int = 400, overlap: int = 60) -> list[str]:
    """把长文本切成有重叠的段落"""
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        if end >= len(text):
            break
        start = end - overlap
    return chunks


def _cosine_similarity(a: list[float], b: list[float]) -> float:
    """纯 Python 余弦相似度（无 numpy 依赖）"""
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = (sum(x * x for x in a)) ** 0.5
    norm_b = (sum(x * x for x in b)) ** 0.5
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return dot / (norm_a * norm_b)


# ---- 知识库检索工具工厂（每次请求动态生成，确保用户隔离） ----
from langchain_core.tools import tool


def _make_search_knowledge_base(user_id: str):
    """为指定用户创建知识库检索工具，仅检索该用户上传的文档。"""
    @tool
    def search_knowledge_base(query: str) -> str:
        """检索本地知识库中用户上传的文档内容。用户可能通过文档告诉你个人信息（名字、背景、偏好等），回答问题前必须先调用此工具查文档。参数 query 请使用文档中可能出现的关键词（2-5个词），不要用完整问句。"""
        if not query.strip():
            return "查询内容为空"

        # 1) 向量化查询
        try:
            q_vec = _embed([query])[0]
        except Exception as e:
            return f"查询向量化失败：{e}"

        # 2) 仅加载当前用户的文档 chunks，JOIN documents 表做 user_id 过滤
        conn = get_kb_db()
        try:
            rows = conn.execute(
                "SELECT c.doc_id, c.chunk_idx, c.content, c.embedding "
                "FROM chunks_vec c JOIN documents d ON c.doc_id = d.id "
                "WHERE d.user_id = ?", (user_id,)
            ).fetchall()
        finally:
            conn.close()

        if not rows:
            return "知识库中暂无文档，请先上传文档。"

        import json as _json

        scored = []
        for r in rows:
            try:
                vec = _json.loads(r["embedding"])
            except Exception:
                continue
            sim = _cosine_similarity(q_vec, vec)
            scored.append((sim, r["doc_id"], r["content"]))

        # 3) 取 Top-5 向量相似片段，同时准备全量兜底
        scored.sort(key=lambda x: x[0], reverse=True)
        top = scored[:5]

        cards = []
        context_parts = []
        for sim, doc_id, content in top:
            if sim >= 0.1:
                cards.append({
                    "title": f"文档#{doc_id} 片段（相似度 {sim:.0%}）",
                    "url": "",
                    "snippet": content.strip()[:150],
                })
                context_parts.append(f"[文档#{doc_id}]\n{content.strip()}")

        # 4) 向量没命中时，把该用户所有文档内容都给 Agent，让 LLM 自己判断语义相关性
        if not context_parts:
            seen = set()
            for _, doc_id, content in scored:  # scored 已排序，取全部
                c = content.strip()
                if c not in seen:
                    seen.add(c)
                    context_parts.append(f"[文档#{doc_id}]\n{c}")
            # 前端卡片显示"全文检索"
            cards = [{
                "title": f"全文检索（共 {len(context_parts)} 个片段）",
                "url": "",
                "snippet": "向量未匹配，已将全部文档提供给 AI 分析",
            }]

        ctx = "\n\n".join(context_parts)
        return f"[KB]{_json.dumps({'results': cards}, ensure_ascii=False)}\n---\n{ctx}"

    return search_knowledge_base


# ============================================================
# 四、创建智能体（Agent）
# 注意：checkpointer 在请求处理中动态创建，因为 with 语句限制
# ============================================================
def build_agent():
    """构建一个带联网搜索 + SQLite 记忆的智能体"""
    checkpointer = SqliteSaver.from_conn_string(DB_PATH)  # 不退出 with 块，手动管理
    # 注意：SqliteSaver.from_conn_string 返回的是上下文管理器，
    # 但进入 with 后内部的 connection 需要用 __enter__ 激活
    return checkpointer


# ============================================================
# 四、FastAPI 应用
# ============================================================
app = FastAPI(
    title="协会网站聊天机器人",
    description="支持联网搜索、SQLite 记忆、流式输出的智能对话接口",
    version="1.0.0",
)

# 允许前端跨域访问（开发时前端可能在不同端口或直接用 file:// 打开）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源（生产环境应限制为具体域名）
    allow_methods=["*"],  # 允许所有 HTTP 方法
    allow_headers=["*"],  # 允许所有请求头
)


# ---------- 定义请求体结构 ----------
class ChatRequest(BaseModel):
    question: str = Field(..., description="用户问题", examples=["协会的成立时间是什么时候？"])
    thread_id: str = Field(
        default_factory=lambda: str(uuid.uuid4())[:8],
        description="对话线程 ID，用于区分不同会话。不传则自动生成新会话。",
    )


class CreateConversationRequest(BaseModel):
    title: str = Field(default="新的对话", description="会话标题")


class KbUploadRequest(BaseModel):
    filename: str = Field(..., description="文件名，如 doc.txt")
    content_b64: str = Field(..., description="文件内容的 base64 编码")


# ============================================================
# 知识库 API（3 个接口）
# ============================================================

@app.post("/api/kb/upload", summary="上传文档到知识库")
async def kb_upload(body: KbUploadRequest, user_id: str = Depends(get_current_user)):
    """上传 txt/md 文件（base64 编码），自动分块、向量化、存入数据库"""
    import base64
    # 1) 解码文件内容
    try:
        raw = base64.b64decode(body.content_b64)
    except Exception:
        raise HTTPException(400, "content_b64 解码失败")

    ext = body.filename.rsplit(".", 1)[-1].lower() if "." in body.filename else "txt"

    if ext in ("txt", "md"):
        text = raw.decode("utf-8", errors="replace")
    else:
        raise HTTPException(400, f"不支持的文件类型：.{ext}，仅支持 txt/md")

    if not text.strip():
        raise HTTPException(400, "文件内容为空")

    # 2) 分块
    chunks = _chunk_text(text)
    if not chunks:
        raise HTTPException(400, "文件无法分块")

    # 3) 向量化
    try:
        vectors = _embed(chunks)
    except Exception as e:
        raise HTTPException(500, f"向量化失败：{e}")

    # 4) 存入数据库
    conn = get_kb_db()
    try:
        now = datetime.now().isoformat()
        cur = conn.execute(
            "INSERT INTO documents (filename, file_type, char_count, chunk_count, created_at, user_id) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (body.filename, ext, len(text), len(chunks), now, user_id),
        )
        doc_id = cur.lastrowid
        import json as _json
        for i, (chunk, vec) in enumerate(zip(chunks, vectors)):
            conn.execute(
                "INSERT INTO chunks_vec (doc_id, chunk_idx, content, embedding) "
                "VALUES (?, ?, ?, ?)",
                (doc_id, i, chunk, _json.dumps(vec)),
            )
        conn.commit()
    finally:
        conn.close()

    return {"ok": True, "doc_id": doc_id, "filename": body.filename,
            "chunks": len(chunks), "char_count": len(text)}


@app.get("/api/kb/documents", summary="知识库文档列表")
def kb_list_documents(user_id: str = Depends(get_current_user)):
    """返回已上传的文档列表"""
    conn = get_kb_db()
    try:
        rows = conn.execute(
            "SELECT id, filename, file_type, char_count, chunk_count, created_at "
            "FROM documents WHERE user_id = ? ORDER BY created_at DESC",
            (user_id,)
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


@app.delete("/api/kb/documents/{doc_id}", summary="删除知识库文档")
def kb_delete_document(doc_id: int, user_id: str = Depends(get_current_user)):
    """删除文档及其所有向量块（仅允许删除自己的文档）"""
    conn = get_kb_db()
    try:
        conn.execute("DELETE FROM documents WHERE id = ? AND user_id = ?", (doc_id, user_id))
        conn.execute("DELETE FROM chunks_vec WHERE doc_id = ?", (doc_id,))
        conn.commit()
    finally:
        conn.close()
    return {"ok": True}


# ============================================================
# 会话管理接口（4 个 API）
# ============================================================

# ① 获取会话列表
@app.get("/api/conversations", summary="获取会话列表")
def list_conversations(user_id: str = Depends(get_current_user)):
    """返回当前用户的会话，按更新时间倒序"""
    conn = get_app_db()
    try:
        rows = conn.execute(
            "SELECT thread_id, title, created_at, updated_at "
            "FROM conversations WHERE user_id = ? ORDER BY updated_at DESC",
            (user_id,)
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


# ② 创建新会话
@app.post("/api/conversations", summary="创建新会话")
def create_conversation(body: CreateConversationRequest, user_id: str = Depends(get_current_user)):
    """创建新会话，返回 thread_id"""
    thread_id = str(uuid.uuid4())[:8]
    now = datetime.now().isoformat()
    conn = get_app_db()
    try:
        conn.execute(
            "INSERT INTO conversations (thread_id, title, created_at, updated_at, user_id) "
            "VALUES (?, ?, ?, ?, ?)",
            (thread_id, body.title, now, now, user_id),
        )
        conn.commit()
    finally:
        conn.close()
    return {"thread_id": thread_id, "title": body.title, "created_at": now}


# ③ 删除会话
@app.delete("/api/conversations/{thread_id}", summary="删除会话")
def delete_conversation(thread_id: str, user_id: str = Depends(get_current_user)):
    """删除会话及其所有消息（仅允许删除自己的会话）"""
    conn = get_app_db()
    try:
        cur = conn.execute(
            "DELETE FROM conversations WHERE thread_id = ? AND user_id = ?", (thread_id, user_id)
        )
        conn.commit()
        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="会话不存在")
    finally:
        conn.close()
    return {"ok": True}


# ④ 获取会话消息
@app.get("/api/conversations/{thread_id}/messages", summary="获取会话消息")
def get_messages(thread_id: str, user_id: str = Depends(get_current_user)):
    """返回指定会话的所有消息，按时间正序"""
    conn = get_app_db()
    try:
        conv = conn.execute(
            "SELECT thread_id FROM conversations WHERE thread_id = ? AND user_id = ?",
            (thread_id, user_id)
        ).fetchone()
        if not conv:
            raise HTTPException(status_code=404, detail="会话不存在")

        rows = conn.execute(
            "SELECT role, content, search_info, created_at FROM messages "
            "WHERE thread_id = ? ORDER BY id ASC",
            (thread_id,),
        ).fetchall()
        return [dict(r) for r in rows]
    finally:
        conn.close()


# ---------- 流式聊天接口（GET） ----------
@app.get("/chat-stream", summary="流式聊天 SSE（EventSource 用）")
async def chat_stream(question: str, thread_id: str = "default",
                       user_id: str = Depends(get_current_user)):
    """
    供前端 EventSource 调用的 GET 接口。
    EventSource 不支持自定义 Header，通过 ?token=xxx 传递 JWT。
    """
    return StreamingResponse(
        _stream_chat(question, thread_id, user_id),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


# ---------- SSE 流式生成器 ----------
async def _stream_chat(question: str, thread_id: str, user_id: str = ""):
    """
    核心流式逻辑：
    1. 自动创建会话 + 保存用户消息到 app.db
    2. agent.stream() 在独立线程运行，避免阻塞事件循环
    3. 通过 asyncio.Queue 将 token 从工作线程传到 async 生成器
    4. 异步地从队列取 token，逐个通过 SSE 推送到前端
    5. 流结束后保存 AI 完整回复到 app.db
    """
    import json as json_mod
    from concurrent.futures import ThreadPoolExecutor
    from langchain_core.messages import AIMessageChunk, ToolMessage

    # ---- 0. 确保会话记录存在 + 保存用户消息 ----
    now = datetime.now().isoformat()
    conn = get_app_db()
    try:
        existing = conn.execute(
            "SELECT thread_id, user_id FROM conversations WHERE thread_id = ?", (thread_id,)
        ).fetchone()
        if not existing:
            # 新会话：先用占位标题，流结束后由 AI 生成真正的标题
            conn.execute(
                "INSERT INTO conversations (thread_id, title, created_at, updated_at, user_id) "
                "VALUES (?, ?, ?, ?, ?)",
                (thread_id, "新的对话", now, now, user_id),
            )
        elif existing["user_id"] != user_id:
            # thread_id 被其他用户占用，自动分配新 thread_id 避免数据泄露
            thread_id = str(uuid.uuid4())[:8]
            conn.execute(
                "INSERT INTO conversations (thread_id, title, created_at, updated_at, user_id) "
                "VALUES (?, ?, ?, ?, ?)",
                (thread_id, "新的对话", now, now, user_id),
            )
        else:
            # 自己的会话：只更新最后活跃时间
            conn.execute(
                "UPDATE conversations SET updated_at = ? WHERE thread_id = ?",
                (now, thread_id),
            )
        conn.execute(
            "INSERT INTO messages (thread_id, role, content, created_at, user_id) "
            "VALUES (?, 'user', ?, ?, ?)",
            (thread_id, question, now, user_id),
        )
        conn.commit()
    finally:
        conn.close()

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
                tools=[_make_search_knowledge_base(user_id), web_search],
                checkpointer=checkpointer,
                system_prompt=load_system_prompt(),
            )
            # 用 user_id 前缀隔离不同用户的 LangGraph checkpoint，防止 agent 记忆串号
            config = {"configurable": {"thread_id": f"{user_id}:{thread_id}"}}
            searching_sent = False  # 避免重复发送 [SEARCHING]
            for chunk, metadata in agent.stream(
                    {"messages": [{"role": "user", "content": question}]},
                    config=config,
                    stream_mode="messages",
            ):
                if isinstance(chunk, AIMessageChunk):
                    # 检测到 tool call → 通知前端"正在搜索"
                    if (chunk.tool_calls or chunk.tool_call_chunks) and not searching_sent:
                        searching_sent = True
                        search_info["split_pos"] = len("".join(ai_reply_chunks))
                        # 判断工具类型，发对应标记到文字流（标记定位搜索链接位置）
                        tool_name = ""
                        if chunk.tool_call_chunks:
                            tc = chunk.tool_call_chunks[0]
                            tool_name = (tc.get("name") or "") if isinstance(tc, dict) else ""
                        if "knowledge" in tool_name.lower():
                            marker = "[[KB_MARK]]"
                            search_info["kb_marker_pos"] = search_info["split_pos"]
                        else:
                            marker = "[[WEB_MARK]]"
                            search_info["web_marker_pos"] = search_info["split_pos"]
                        loop.call_soon_threadsafe(queue.put_nowait, marker)
                        loop.call_soon_threadsafe(
                            queue.put_nowait, "[SEARCHING]"
                        )
                    # 普通文本 token
                    if chunk.content and not chunk.tool_calls and not chunk.tool_call_chunks:
                        ai_reply_chunks.append(chunk.content)
                        loop.call_soon_threadsafe(
                            queue.put_nowait, chunk.content
                        )
                elif isinstance(chunk, ToolMessage) or type(chunk).__name__ == "ToolMessage":
                    # 搜索完成 → 发给前端，同时收集结果用于持久化
                    searching_sent = False
                    results_json = _format_search_results(chunk.content)
                    # 区分 KB 搜索和 web 搜索
                    raw = str(chunk.content)
                    if raw.startswith("[KB]"):
                        _parse_kb_results(raw, search_info)
                        loop.call_soon_threadsafe(
                            queue.put_nowait, f"[KB_RESULT]{results_json}"
                        )
                    else:
                        _parse_web_results(raw, search_info)
                        loop.call_soon_threadsafe(
                            queue.put_nowait, f"[SEARCH_RESULT]{results_json}"
                        )
        except Exception as e:
            loop.call_soon_threadsafe(
                queue.put_nowait, f"[ERROR] {e}"
            )
        finally:
            try:
                checkpointer_ctx.__exit__(None, None, None)
            except Exception:
                pass
            # 发送结束信号
            loop.call_soon_threadsafe(
                queue.put_nowait, None
            )

    # 启动独立线程跑 agent
    executor = ThreadPoolExecutor(max_workers=1)
    executor.submit(run_agent)

    # 异步循环：逐 token 从队列取数据，通过 SSE 发送
    while True:
        data = await queue.get()  # 真正异步等待，不阻塞事件循环
        if data is None:
            # ---- agent 结束：先保存回复+生成标题，再发 [DONE] ----
            full_reply = "".join(ai_reply_chunks).replace("[[KB_MARK]]", "").replace("[[WEB_MARK]]", "")
            if full_reply:
                # 1) 保存 AI 回复
                conn = get_app_db()
                try:
                    conn.execute(
                        "INSERT INTO messages (thread_id, role, content, search_info, created_at, user_id) "
                        "VALUES (?, 'ai', ?, ?, ?, ?)",
                        (thread_id, full_reply,
                         __import__('json').dumps(search_info, ensure_ascii=False) if search_info else None,
                         datetime.now().isoformat(), user_id),
                    )
                    conn.commit()
                finally:
                    conn.close()

                # 2) 仅当标题还是占位时，用 AI 生成标题
                generated_title = ""
                # 先查当前标题，避免重复起名
                check_conn = get_app_db()
                try:
                    current = check_conn.execute(
                        "SELECT title FROM conversations WHERE thread_id = ?", (thread_id,)
                    ).fetchone()
                    need_title = (current and current["title"] == "新的对话")
                finally:
                    check_conn.close()

                if need_title:
                    try:
                        import traceback

                        resp = model.invoke(
                            f"用户问题：{question}\n"
                            f"助手回答：{full_reply[:300]}\n\n"
                            f"请为这段对话起一个标题，15个字以内，只输出标题本身不要引号："
                        )
                        generated_title = resp.content.strip() if hasattr(resp, "content") else str(resp).strip()
                        if len(generated_title) > 30:
                            generated_title = generated_title[:30]
                        print(f"[标题生成] thread={thread_id} title={generated_title!r}", flush=True)
                    except Exception:
                        print(f"[标题生成失败] thread={thread_id}", flush=True)
                        traceback.print_exc()

                # 3) 写入标题
                if generated_title:
                    conn = get_app_db()
                    try:
                        conn.execute(
                            "UPDATE conversations SET title = ?, updated_at = ? WHERE thread_id = ?",
                            (generated_title, datetime.now().isoformat(), thread_id),
                        )
                        conn.commit()
                    finally:
                        conn.close()

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