# AI 后端开发指南

## 项目目标与当前进度

本项目面向高校 IT 协会，前端为 `../frontend/` 下的 Vue 3 应用，业务后端为 `../backend/` 下的 Spring Boot 应用。本模块提供 AI 对话、知识库、工具调用和离线评估。

业务优化主线：**上传学习资料 → 检索并生成有来源的文章草稿 → 用户预览、修改和确认 → Java 幂等发布 → 超时结果核对**。

按阶段实施，每阶段单独验收；不要将待办描述为已完成。

| 阶段 | 状态 | 范围 |
|---|---|---|
| 1. 基础正确性 | 已完成 | 知识库隔离、上传校验、全文恢复、前端 HTML 防护、缓存降级；随后修复本地鉴权配置与代理问题 |
| 2. 安全发布闭环 | 尚未开始，下一步 | 草稿持久化、版本确认、Java 幂等发布、文章 ID 与发布结果查询、服务端内容校验 |
| 3. 最小 Agent Harness | 尚未开始 | 执行预算、分层超时、取消、并发限制、有界缓冲、错误分类、链路日志 |
| 4. 评估驱动 RAG 优化 | 尚未开始 | 记录实际上下文、测试集、结构化分块、来源定位；按收益决定是否引入混合检索和重排 |

**下一次开发先核对当前代码，再提出第二阶段的数据表、接口和状态流转设计，经用户确认后实现。** 不重复第一阶段，也不一次性重写架构。

## 运行与测试

以下启动命令在 `ai-backend/` 目录执行：

```bash
python3 -m pip install -r requirements.txt
# 本地 .env 由 .env.example 提供配置示例，不提交真实密钥。
BUSINESS_BASE_URL=http://localhost:8080 python3 main.py

# 离线评估 CLI；会调用真实模型，运行前检查依赖、凭证及评估用户。
python3 -m eval.router
```

以下测试命令在仓库根目录执行：

```bash
python3 -m pip install pytest
python3 -m pytest ai-backend/tests -q
RUN_MILVUS_TESTS=1 python3 -m pytest ai-backend/tests -q
npm --prefix frontend test
git diff --check
```

交接时验证：Python 默认 **95 passed, 1 skipped**；启用真实 Milvus Lite 测试为 **96 passed**；前端 **9 项通过**。数量会随测试增加变化，以实际执行结果为准。

测试使用临时 SQLite、模拟身份/Redis/模型；可选 Milvus Lite 测试使用临时库和合成向量，不下载 embedding 模型。不读写开发数据库，不调用真实 Java 或 LLM。尚未进行完整浏览器/真实模型联调，未执行生产构建。

详细验收及已知边界见 [tests/README.md](tests/README.md)。

## 代码结构

```text
main.py                    FastAPI 入口；模型和 Tavily 工具初始化
agent/router.py            GET /chat-stream
agent/service.py           同步 Agent → 线程 → asyncio.Queue → SSE
agent/tools.py             时间、当前身份、直接发布文章工具
conversation/router.py     会话 CRUD 与历史消息接口
conversation/service.py    委托 Java 鉴权、消息保存、自动标题
kb/router.py               文档上传、列表、全文、删除
kb/schemas.py              上传 Pydantic 模型、1 MiB 上限、重叠长度常量
kb/tools.py                SQLite 初始化、分块、知识库检索工具
common/vector_store.py     Milvus Lite + fastembed/BGE
common/business_client.py  内部 Java HTTP 客户端
common/logger.py           结构化日志
state/manager.py           Redis 文档列表和全文缓存
SystemPrompt.md            每次创建 Agent 时读取的系统提示词
eval/router.py             RAGAS 离线 CLI，不是已注册的 HTTP router
tests/                     回归测试和验收文档
```

真实前端在 `../frontend/`，不是本目录下的 CDN 单文件页面。`main.py` 仍保留指向 `frontend.html` 的旧根路由，但仓库未提供该文件，不应依赖它访问前端。

## 请求与鉴权链路

```text
浏览器 :5173
  /user、/admin、/v3/api-docs → Vite proxy → Java :8080
  /api、/chat-stream、/health → Vite proxy → Python :8000
Python get_current_user → GET {BUSINESS_BASE_URL}/user/users → data.studentId
```

- REST 使用 `Authorization` 请求头；SSE 目前使用 EventSource 的 `?token=` 参数。
- Python 不本地解码 JWT，而是委托 Java 校验。请求头优先于查询参数。
- `BUSINESS_BASE_URL` 必须与前端登录所用的 Java 实例一致，默认 `http://localhost:8080`；Docker 部署显式配置业务容器地址。
- 内部业务 HTTP 使用 `timeout=10, trust_env=False`，避免环境/系统代理干扰本地及容器通信；不改变模型或联网搜索的代理行为。
- 缺少或无效凭证返回 401；Java 连接失败/异常 HTTP 状态返回 503；响应格式或身份字段异常返回 502。
- 修改 `.env` 后若旧值仍被继承，完整停止并重启 Python；热重载不保证覆盖父进程已有环境变量。用户已通过重启和重新登录验证本地恢复正常。
- 不输出 Token、密钥或完整敏感环境变量。不能为了修复 401 而绕过鉴权。

## 主要 HTTP 接口

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/health` | 存活检查，不代表依赖全部正常 |
| GET | `/chat-stream` | 流式对话 |
| GET / POST | `/api/conversations` | 列出/新建会话 |
| DELETE | `/api/conversations/{thread_id}` | 删除本人会话 |
| GET | `/api/conversations/{thread_id}/messages` | 本人会话历史 |
| POST | `/api/kb/upload` | JSON：`filename`、`content_b64` |
| GET | `/api/kb/documents` | 本人文档列表 |
| GET | `/api/kb/documents/{doc_id}/content` | 本人文档全文 |
| DELETE | `/api/kb/documents/{doc_id}` | 删除本人文档 |

## 数据与安全约束

| 数据 | 位置 | 说明 |
|---|---|---|
| 会话和消息 | `conversation/db/app.db` | SQLite，按用户隔离 |
| Agent checkpoint | `agent/db/conversations.db` | LangGraph SqliteSaver；线程标识含用户 ID |
| 文档元数据和原文 | `kb/db/kb.db` | `documents.content` 自动补列，新文档保存原文 |
| 向量片段 | `common/db/milvus.db` | Milvus Lite，512 维 BGE 中文向量 |
| 文档缓存 | Redis | 列表 `kb:docs:{user_id}` 1h；全文 `kb:doc:v2:{user_id}:{doc_id}` 2h |

- 上传只接受 txt/md（无后缀按 txt）、严格 Base64、UTF-8，原文最多 1 MiB。
- 所有普通向量操作必须有非空用户身份；文档读删同时限定用户与文档 ID，不允许空身份退化为全库检索。
- 全文读取先查数据库归属/存在性，再读缓存。Redis 异常降级，不能将已落库的上传因缓存失效失败误报为失败。
- 旧文档无原文时按历史固定 60 字重叠恢复，校验索引、片段数、重叠和原文长度；不能验证则 409 提示重传，不猜测原文。
- 向量删除失败返回 503，保留元数据供重试。SQLite 与 Milvus 无共享事务，上传回滚的向量清理仍是尽力而为，没有持久化补偿任务。
- 前端使用纯文本文档预览和 DOMPurify；Java 发布服务端 HTML 清洗尚未实现，不能声称已全面解决 XSS。

## Agent 与 RAG 现状及限制

工具：`get_current_time`、`get_user_identity`、`publish_article`、`search_knowledge_base`、`main.web_search`。

- `publish_article` 仍直接调用 Java `POST /user/articles`，没有草稿、人工确认、幂等或超时核对。用户 Token 从运行时配置注入，作者最终由 Java 登录上下文确定。
- 工具/Prompt 的分类映射与 Java `ArticleType` **存在不一致**，第二阶段先核对并统一；不能只按 Prompt 描述设计数据库分类。
- 每次对话请求创建线程池，队列无界；没有明确任务总预算、取消传播或统一并发控制。前端断连不代表后台已停止。
- SSE 使用 `[STATE]`、文本、`[ERROR]`、`[DONE]`，状态不存 Redis。消息落库及标题生成位于线程收尾逻辑。
- 当前模型为 `deepseek-v4-flash`；知识库使用 `BAAI/bge-small-zh-v1.5`、top_k=8、相似度阈值 0.5。相似度不是答案正确概率。
- 当前 RAGAS 生成答案后另行检索，评分上下文不一定是 Agent 实际使用的上下文。第四阶段需修评估口径，不能据此宣称线上质量提升。

## 第二阶段验收目标

- 未确认不公开；确认绑定具体草稿版本，修改后重新确认。
- 不可发布他人草稿；私有资料转公开文章要提示用户。
- 稳定发布请求 ID + MySQL 唯一约束；文章与发布记录同事务。
- 同一请求重复/并发提交返回同一文章 ID；相同请求 ID 不可复用不同内容。
- 模拟提交成功但响应丢失，按请求 ID 查结果而非盲目重复创建。
- Java 与 Python 参数契约一致，补服务端内容校验；成功可跳转文章。

## 工作约定

- 修改前读代码和 `git status`，保留用户已有改动，不擅自回退。
- 阶段切换先确认范围和验收；第二阶段涉及表结构及接口，先确认方案。
- 不提交 `.env`、真实密钥、SQLite/Milvus 数据、日志或缓存；备份后才做数据迁移。
- 不主动运行前端生产构建，除非用户明确要求。
