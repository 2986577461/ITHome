# AI 后端开发指南

## 项目目标与当前进度

本项目面向高校 IT 协会，前端为 `../frontend/` 下的 Vue 3 应用，业务后端为 `../backend/` 下的 Spring Boot 应用。本模块提供 AI 对话、知识库、工具调用和离线评估。

业务优化主线已调整为：**保持现有功能 → 最小 Agent Harness → 实际链路追踪与 RAG 评估 → 有依据地优化检索与回答**。用户已取消草稿、人工确认、幂等发布及新业务接口计划，不要恢复该计划。

按阶段实施，每阶段单独验收；不要将待办描述为已完成。

| 阶段 | 状态 | 范围 |
|---|---|---|
| 基础正确性 | 已完成 | 知识库隔离、上传校验、全文恢复、前端 HTML 防护、缓存降级、本地鉴权修复 |
| 最小 Agent Harness | 已实现，待真实服务人工验收 | 异步执行、总/分层超时、调用和上下文预算、有界并发与 SSE 缓冲、断连取消、发布不重试、链路日志 |
| RAG 评估口径 | 已修复，未建立质量基线 | 使用最终模型实际上下文，跳过无证据评分，不再回答后重新检索 |
| 评估驱动优化 | 待办，下一步 | 约 30 条真实标注样本、基线报告，再决定分块/去重/阈值及混合检索/重排 |

**下一步先验收本轮 Harness，再建立真实语料基线，不新增业务功能，不声称已提升检索质量。** 设计与边界详见 [agent/README.md](agent/README.md)。

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

交接时验证：Python 默认 **144 passed, 1 skipped**；启用真实 Milvus Lite 测试为 **145 passed**；前端 **9 项通过**。数量会随测试增加变化，以实际执行结果为准。

测试使用临时 SQLite、模拟身份/Redis/模型；可选 Milvus Lite 测试使用临时库和合成向量，不下载 embedding 模型。不读写开发数据库，不调用真实 Java 或 LLM。尚未进行完整浏览器/真实模型联调，未执行生产构建。

详细验收及已知边界见 [tests/README.md](tests/README.md)。

## 代码结构

```text
main.py                    FastAPI 入口；模型和 Tavily 工具初始化
agent/router.py            GET /chat-stream
agent/service.py           异步 Agent → 有界 asyncio.Queue → SSE，断连回收与后台标题
agent/harness.py           预算/超时 middleware、上下文裁剪、输出预算、单轮追踪
agent/workers.py           共享有界同步工作池，取消后保留实际运行名额
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
eval/execution.py          实际执行样本与最终模型可见 KB 上下文
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

- REST 使用 `Authorization` 请求头；SSE 前端仍使用 EventSource 的 `?token=` 参数；后端聊天工具 Token 与鉴权保持相同的请求头优先规则。
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
| Agent checkpoint | `agent/db/conversations.db` | LangGraph AsyncSqliteSaver；线程标识含用户 ID |
| 文档元数据和原文 | `kb/db/kb.db` | `documents.content` 自动补列，新文档保存原文 |
| 向量片段 | `common/db/milvus.db` | Milvus Lite，512 维 BGE 中文向量 |
| 文档缓存 | Redis | 列表 `kb:docs:{user_id}` 1h；全文 `kb:doc:v2:{user_id}:{doc_id}` 2h |

- 上传只接受 txt/md（无后缀按 txt）、严格 Base64、UTF-8，原文最多 1 MiB。
- 所有普通向量操作必须有非空用户身份；文档读删同时限定用户与文档 ID，不允许空身份退化为全库检索。
- 全文读取先查数据库归属/存在性，再读缓存。Redis 异常降级，不能将已落库的上传因缓存失效失败误报为失败。
- 旧文档无原文时按历史固定 60 字重叠恢复，校验索引、片段数、重叠和原文长度；不能验证则 409 提示重传，不猜测原文。
- 向量删除失败返回 503，保留元数据供重试。SQLite 与 Milvus 无共享事务，上传回滚的向量清理仍是尽力而为，没有持久化补偿任务。
- 前端使用纯文本文档预览和 DOMPurify；Java 发布服务端 HTML 清洗尚未实现，不在本轮范围，不能声称已全面解决 XSS。

## Agent 与 RAG 现状及限制

工具：`get_current_time`、`get_user_identity`、`publish_article`、`search_knowledge_base`、`main.web_search`。

- `publish_article` 仍直接调用 Java `POST /user/articles`，没有草稿、人工确认、数据库幂等或持久化结果核对。单轮最多尝试一次，网络失败报告结果未知；跨任务重复仍可能创建重复文章。用户 Token 从运行时配置注入，作者由 Java 登录上下文确定。
- 工具/Prompt 分类已对齐 Java `ArticleType`；输入限制为标题 2–100、正文 HTML 10–5000 字。
- 原生异步 Agent + AsyncSqliteSaver，应用层有界队列；同步检索使用共享 4 线程池。模型/工具调用数、总时间及上下文受预算约束，详见 `agent/README.md` 和 `.env.example`。
- SSE 兼容 `[STATE]`、文本、`[ERROR]`、`[DONE]`；只有完整成功的回答正常落库，失败不保存半截回复。标题生成异步且有独立超时，不阻塞 DONE。
- 当前模型为 `deepseek-v4-flash`（关闭客户端自动重试）；知识库仍用 `BAAI/bge-small-zh-v1.5`、top_k=8、阈值 0.5，新增 8000 字符检索上下文上限。检索分数不代表答案正确概率。
- RAGAS 使用最终模型实际收到的上下文，不再额外检索；无证据返回 no_context，检索故障单独报告。尚未运行真实质量基线或真实付费评分。
- Prompt 已约束不执行检索内容中的指令，但这不是独立的写操作授权策略，不能宣称提示注入已彻底解决。

## 后续验收与 RAG 优化

- 人工验证真实模型流、KB/联网/身份工具、断连以及本轮环境配置；发布只在测试环境验证。
- 准备约 30 条真实问题及相关来源/答案要点，记录检索、回答、引用、延迟与成本基线。
- 先按错误类型优化结构化分块、去重/相邻片段和阈值；改分块须制定旧向量重建方案，先在测试库验证。
- 仅在基线证明必要时引入混合检索或重排，逐项比较，不新增业务功能。

## 工作约定

- 修改前读代码和 `git status`，保留用户已有改动，不擅自回退。
- 按当前调整后的计划分阶段验收；不恢复已取消的草稿/幂等发布方案，不擅自扩展业务或重写架构。
- 不提交 `.env`、真实密钥、SQLite/Milvus 数据、日志或缓存；备份后才做数据迁移。
- 不主动运行前端生产构建，除非用户明确要求。
