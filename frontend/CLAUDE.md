# 前端开发指南

## 当前工作进度

项目正围绕“资料检索 → 文章草稿 → 用户确认 → 安全发布”分阶段优化。

- **第一阶段已完成**：知识库基础正确性、用户隔离、上传限制、全文展示、HTML 清洗、错误传播；后续发现的本地 Python → Java 鉴权地址与代理问题已修复，用户确认恢复正常。
- **下一步是第二阶段，尚未实现**：草稿预览/编辑、版本绑定的人工确认、Java 幂等发布、超时结果查询及文章跳转。先与用户确认表结构和接口，不只用 Prompt 约束确认行为。
- **第三、四阶段尚未开始**：Agent 执行治理，以及基于实际上下文的 RAG 评估和优化。
- 跨模块交接、后端约束和验收目标见 [../ai-backend/CLAUDE.md](../ai-backend/CLAUDE.md)；基础修复验收见 [../ai-backend/tests/README.md](../ai-backend/tests/README.md)。

## 命令

在 `frontend/` 目录执行：

```bash
npm ci
npm run dev
npm test
npm run preview
# 仅用户明确要求时执行：
npm run build
```

**不要主动执行生产构建。** `npm test` 使用 Node test runner + jsdom，检查 HTML 清洗、Axios Token/错误传播，以及变更页面的 Vue SFC 脚本和模板编译，不生成 `dist/`。

交接时前端 9 项测试通过；不等于完整浏览器端联调已完成。Node 版本需满足 lockfile 内依赖的要求，当前 jsdom 26 要求 Node 18+，其他依赖可能要求更高版本。

## 技术栈与目录

- Vue 3 Composition API、`<script setup>`、Vue Router hash 路由、Pinia。
- Vite 5，路径别名 `@/` 指向 `src/`。
- Element Plus、Axios、TipTap、marked、highlight.js。
- DOMPurify 用于不可信 HTML；jsdom 仅开发/测试依赖。

```text
src/main.js                  应用、Pinia、Router、Element Plus
src/App.vue                  全局布局与登录状态恢复
src/router/index.js          懒加载路由与登录/角色守卫
src/stores/                  user、visible、notification、upload、updateArticle、govern
src/request/axiosInit.js     同域 Axios、Token 注入、响应解包和失败传播
src/request/axiosForAi.js    会话与知识库 REST 请求
src/request/axiosForArticles.js 文章分页、CRUD、图片上传
src/utils/sanitizeHtml.js    AI/文章展示共用 HTML 白名单
src/views/AiDialog.vue       SSE 对话、会话列表、知识库上传/查看/删除
src/views/TechStudy.vue      文章分类、分页、评论、点赞、分享
src/views/UploadArticle.vue  TipTap 富文本编辑与图片上传
src/views/ColonyGovern.vue   成员管理
src/views/ProfilePage.vue    个人资料
src/components/             登录、密码、成员、资源等弹窗与导航
src/style.css               全局样式
vite.config.js              本地代理配置
tests/                      HTML 安全、Axios 异常和 SFC 编译测试
```

## 服务路由与鉴权

Axios `baseURL` 为 `""`，不要直接写死本地 Java/Python 地址：

| 前缀 | 开发环境 Vite proxy | 用途 |
|---|---|---|
| `/user`、`/admin`、`/v3/api-docs` | `http://localhost:8080` | Java 业务及接口文档 |
| `/api`、`/chat-stream`、`/health` | `http://localhost:8000` | Python AI 服务 |

生产环境通过同域反向代理分流。

- Token 存放在 localStorage 的 `authorization`，REST 请求拦截器放入 `Authorization` 请求头，当前约定为原始 Token，不自行加 `Bearer`。
- `App.vue` 恢复会话时调用 `getThis()` 并更新用户 Store。
- SSE 目前使用 EventSource，将 Token 放入查询参数；尚未改为 fetch 流式请求。
- Python 会转发 Token 给 Java `/user/users` 校验，`BUSINESS_BASE_URL` 必须与登录服务一致。
- 401 提示未登录/登录失效；503 优先显示 Python `detail`，不一律解释为限流。
- Axios HTTP 错误必须 `Promise.reject(error)`，让页面 `catch` 执行，不得吞成成功的 `undefined`。当前 401 拦截器只提示，不自动清除 Token。
- 不输出或提交真实 Token。遇到本地 401，核对请求头和响应 `detail`，不要绕过鉴权。

## API 响应约定

- Axios 成功拦截器返回 `response.data`，调用方拿到响应体。
- Java 返回 `{ code, msg, data }`；成功按 `Number(resp.code) === 200` 兼容数字/字符串。部分现有调用仍用字符串比较，修改时核对，勿假设已经全部统一。
- Java 业务错误可能在 HTTP 200 内以 `code` 表达；当前拦截器会提示，但不自动 reject 这种响应。
- Python 会话/知识库接口通常直接返回列表或对象，不套 Java `Result`；HTTP 异常正文使用 `detail`。
- Swagger：`http://localhost:8080/swagger-ui/index.html`。

### 关键接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/user/articles/page` | `page`、`size`、`type`；文章分页 |
| GET | `/user/articles/count` | 可选 `type` |
| POST / PUT | `/user/articles` | 当前直接创建/修改文章，尚无草稿确认流程 |
| DELETE | `/user/articles/{id}` | 删除文章 |
| POST | `/user/articles/image` | FormData，`files` |
| GET / POST | `/api/conversations` | 会话列表/创建 |
| GET | `/api/conversations/{thread_id}/messages` | 历史消息 |
| DELETE | `/api/conversations/{thread_id}` | 删除会话 |
| GET | `/chat-stream` | `question`、`thread_id`、`token`，SSE |
| POST | `/api/kb/upload` | `{ filename, content_b64 }` |
| GET | `/api/kb/documents` | 本人资料列表 |
| GET | `/api/kb/documents/{doc_id}/content` | 原文对象，`content` 是纯文本 |
| DELETE | `/api/kb/documents/{doc_id}` | 删除本人资料 |

文章对象包含 `id`、`name`、`studentId`、`type`、`head`、`content`、`updatedDateTime` 等字段。后端 `ArticleType` 当前映射为：0=全部，1=C，2=HTML，3=CSS，4=JavaScript，5=Java，6=MySQL。AI 工具/Prompt 的分类描述存在差异，第二阶段必须核对统一，不能直接照搬其中一端。

## HTML 与知识库安全约束

- 上传前检查单文件不超过 1 MiB；后端仍独立校验，前端限制不能替代服务端验证。
- 文档内容用 Vue 文本插值展示，保留换行，不使用 `v-html="docContent"`。
- 聊天内容先 `marked.parse()`，再 `sanitizeHtml()`；文章正文在 `TechStudy.vue` 使用同一清洗函数。
- 白名单保留标题、正文、强调、列表、代码、高亮 class、表格、链接及图片；禁止脚本、事件属性、内联样式、iframe、表单、SVG 和 MathML。不用正则自制 HTML 清洗器。
- 新增不可信 HTML 展示入口时复用清洗函数，并补安全回归；不要给白名单随意增加危险标签或属性。
- 前端展示清洗不等于 Java 发布接口已实现服务端清洗，该部分仍属第二阶段。
- 上传/删除失败不更新成成功 UI；旧资料恢复失败要展示后端重传提示。

## SSE 状态与已知限制

- 协议包含 `[STATE]` JSON、正文、`[ERROR]` 和 `[DONE]`。
- 检索状态包括 `searching_kb`、`searching_web`；发布使用 `publishing`，生成使用 `generating`。
- 会话历史保存搜索信息用于恢复来源卡片。
- 关闭 EventSource 仅关闭客户端连接，当前不保证 Python Agent 停止，也不能撤回已经执行的发布。
- 任务预算、取消传播、结构化错误与 fetch 流式鉴权留待第三阶段；不要声称已有完整执行治理。

## 其他约定

- hash 路由，页面按需懒加载；`/` 跳转 `/home`。
- `meta.requiresAuth` 校验登录，`meta.requiresRole` 按 `userStore.position` 校验，当前成员管理角色为 `admin`，不是旧文档中的会长/副会长字符串。
- `userStore.effectiveAvatar` 直接返回 `state.avatar`；随机头像使用 DiceBear。FormData 上传不要手动设置 Content-Type 边界。
- 复用全局 CSS 自定义属性、`.section`、`.section-inner`、`.section-alt`、公共按钮和 scroll-reveal；注意 `prefers-reduced-motion`。
- 修改前扫描代码并检查 Git 状态，保留用户已有改动；定位不清或设计有歧义先询问。
- 普通明确修复可直接实施；涉及下一阶段表结构/接口设计先确认。不要自行扩展成通用 Agent 平台。
