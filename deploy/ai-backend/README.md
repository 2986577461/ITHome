# AI 后端 CI/CD 运维手册

范围仅为 `ai-backend/`。不构建/发布前端或 Java，不重启共享 Nginx、MySQL、Redis，也不影响其他项目。

## 1. 交付边界

- **CI**：`dev`/`main` 的相关 push、PR 和手动触发；真实 Linux amd64 镜像内运行回归测试、临时 Milvus Lite 测试，随后对最终镜像离线做启动、鉴权边界、embedding 和合成向量检索检查。
- **CD**：只有 `workflow_dispatch`；手工指定成功 CI 的 run ID 和完整 Git SHA。发布的是该 CI 已测试的镜像，不在服务器重新构建。
- **真实 LLM 评估**不在默认流水线中，不使用生产用户数据或付费 API；合成向量检索成功不代表真实 RAG 质量提升。
- GitHub 托管 Runner 向服务器推送压缩镜像，不依赖服务器访问 GHCR/Docker Hub。Runner 能否访问生产 SSH 入口仍须由第一次实际运行验证。
- 新代码、真实模型及历史数据的完整人工验收仍是发版条件；工作流不会替代这一判断。

相关文件：

```text
.github/workflows/ai-backend-ci.yml       测试、构建、保存镜像产物（14 天）
.github/workflows/ai-backend-deploy.yml   手动选版本、校验 CI 来源、发布
ai-backend/Dockerfile                    固定基础镜像 digest；test/runtime 两个 target
ai-backend/requirements*.lock            Linux amd64 / Python 3.12 的哈希锁
ai-backend/.dockerignore                 构建上下文白名单
ai-backend/scripts/smoke.py              无外部调用的启动/检索检查
ai-backend/storage-version              数据格式兼容标记
deploy/ai-backend/                       服务器配置、受限入口、备份/回滚与测试
```

## 2. 当前服务器布局

部署根目录：`/opt/apps/ithome-ai`，目录仅 root 可访问。

```text
bin/server.py                  受限部署入口
bin/bootstrap.py               一次性旧容器数据迁移工具，不能重复运行
compose.yaml                   只管理 chatbot-backend；使用外部 ithome_net 网络
legacy.override.yaml           仅旧版本使用的提示词挂载
settings.json                  本机 HTTP 冒烟请求的 Host
state.json / previous.json     当前/上一版本镜像 ID 与兼容标记，无密钥
release.env                    Compose 的不可变 AI_IMAGE ID
pending.json                   发布中断/恢复失败时保留的恢复日志
shared/runtime.env             运行凭证，raw 格式，权限 600；不进入 Git/镜像/产物
shared/legacy-SystemPrompt.md  只供原线上版本使用
shared/data/conversation       → /app/conversation/db
shared/data/agent              → /app/agent/db
shared/data/kb                 → /app/kb/db
shared/data/vector             → /app/common/db（含 Milvus 整个目录）
shared/data/legacy             → /app/db（保留历史旧数据，不能混入当前库）
shared/logs                    → /app/common/logs
backups/                       停止 AI 写入后的完整数据快照及版本/配置
incoming/                      上传暂存；处理完成或失败后删除压缩包
migration-verification.json   首次持久化迁移检查结果
```

`state.json` 中 `legacy-*` 是原容器基线，不代表已经能确定原部署的 Git SHA。首次正常 CD 后，新版本将精确关联提交 SHA，并使用镜像内的 `SystemPrompt.md`；不会再被旧宿主机提示词覆盖。

**不要再使用 `/root/ithome` 下的旧全栈发布方式来更新 AI，也不要运行整个项目的 `compose down`。** 该目录的旧配置不再是 AI 的部署来源。旧服务器 Git 工作区的未提交修改没有被覆盖。

## 3. GitHub 首次启用（需要仓库管理员）

### 3.1 把工作流纳入仓库

审查并提交/推送相关文件。GitHub 的默认分支目前是 `main`，手动工作流必须先出现在默认分支，才方便从 Actions 页面发现和触发。

不需要把整个 `dev` 的业务改动一次性合并到 `main`：可以先将 CI/CD 基础文件及所需依赖文件单独审查引入默认分支，再从 `dev` 跑 CI、选择明确提交发版。避免仅复制工作流 YAML 而漏掉它调用的 `deploy/ai-backend/`、Dockerfile 或锁文件。

默认的路径过滤覆盖 AI 代码、部署脚本、AI 工作流和 `.gitignore`。Java/前端单独改动不触发 AI 流水线。

### 3.2 配置生产 Environment

在 GitHub Settings → Environments 中，**首次运行前**创建 `ithome-ai-production`：

- 限制部署来源分支为 `dev`、`main`。
- 按仓库套餐和团队情况配置 Required reviewers；工作流中的 environment 名称本身不会自动启用人工审批。
- 建议保护这两个分支，并要求审查工作流和部署脚本的改动；路径过滤不是权限隔离。

在该 Environment 下添加 Secrets：

| Secret | 内容 |
|---|---|
| `AI_DEPLOY_HOST` | GitHub Runner 可访问的服务器域名/IP，不是本机的 `ithome` SSH 别名 |
| `AI_DEPLOY_PORT` | SSH 端口；空值默认 22 |
| `AI_DEPLOY_KEY` | 本项目专用无口令私钥全文 |
| `AI_DEPLOY_KNOWN_HOSTS` | 经已有可信 SSH 连接核对过的服务器公钥记录全文 |

本轮已在操作电脑的仓库外生成：

```text
~/.ssh/ithome-ai-actions             专用私钥（600），切勿提交、发聊天或截图
~/.ssh/ithome-ai-actions.pub         专用公钥
~/.ssh/ithome-ai-known-hosts         已核对的服务器公钥（可作为 Secret 内容）
~/.ssh/ithome-ai-actions-setup.json  主机、端口和本地文件位置，不含私钥正文
```

可查看 setup JSON 获取主机/端口。私钥只复制到上述 GitHub Secret，不要用日常 root 私钥替代。项目内不保存这些值。服务器 API 密钥仍只保存在服务器，不需要添加为 GitHub Secret。

服务器授权条目采用：

```text
restrict,command="/usr/bin/python3 /opt/apps/ithome-ai/bin/server.py" ssh-ed25519 ...
```

该凭证不能开 shell、PTY、转发、SCP 或执行自定义命令，只允许 `status` 和固定参数的 `deploy`。它仍然拥有 **AI 代码发布权限**，因此能读取 AI 运行环境和修改挂载的 AI 数据；不应视为无风险凭证。泄漏时删除对应 authorized_keys 条目，重建专用密钥并更新 Secret。

不要在运行时用未经核对的 `ssh-keyscan` 替代 known_hosts；服务器主机密钥正常轮换时应通过可信管理通道重新核对。

### 3.3 第一次正常发布

1. 在 `dev` 上运行 **AI Backend CI**，等待完整成功。构建会下载公开的 Python 依赖和 embedding 模型，没有付费调用。
2. 打开成功的 run，记录 URL 中的数字 run ID 和该次完整 40 位 Git SHA。PR run、fork run、其他工作流、失败 run 均不能发布。
3. 确认需要的真实链路/质量验收已完成。
4. 在 **AI Backend Deploy** 中填写这两个值，按 Environment 规则审批。
5. 检查工作流日志和服务器状态；在浏览器用正常账号抽查历史会话、KB 检索和 SSE。不在生产冒烟中自动发布文章。

工作流验证 run 所属仓库、来源事件、分支、工作流路径、SHA 和成功状态，随后验证镜像包哈希；服务器再次验证镜像 ID、SHA 标签、Linux amd64 平台、存储版本及离线预检。

压缩包在 GitHub 保留 14 天；过期后对同一提交重新手动运行 CI，再选择新的 run ID。不要依赖过期/缺失产物，也不要把 PR 提供的任意镜像送进生产。

## 4. 发布实际做了什么

```text
获得服务器独占锁
  → 限制上传大小并校验 SHA256（最大 3 GiB；保留至少 2 GiB 磁盘余量）
  → docker load，按镜像 ID 固定版本
  → 无网络/无生产挂载/假 API Key 的候选镜像预检
  → 检查数据兼容标记与备份空间
  → 写 pending.json
  → 只停止 AI（最多 150 秒优雅退出）
  → 完整备份 shared/data，包含 WAL/SHM、Milvus 目录
  → 只重建 AI，挂载同一套持久化数据
  → 检查实际镜像 ID、容器健康、Nginx /health、未登录请求为 401
  → 成功：更新 state.json 和 previous.json
  → 失败：恢复旧镜像及其旧提示词挂载，检查健康
```

有短暂 AI 不可用窗口，正在执行的请求也可能因超时或进程退出中断。没有做共享 SQLite/Milvus 的双实例蓝绿部署。发布本身不会重试任何业务写入。

新构建关闭 Uvicorn access log，避免 SSE 查询参数中的 Token 写入该层日志；这不是全链路脱敏承诺，共享代理日志仍需另外审计。

## 5. 查询、回滚与中断恢复

在正常管理员 SSH 会话中执行（专用发布密钥不能执行管理员回滚/恢复）：

```bash
python3 /opt/apps/ithome-ai/bin/server.py status
python3 /opt/apps/ithome-ai/bin/server.py rollback
```

`rollback` 会先停写、备份，再切换到 `previous.json` 中的镜像和提示词状态；成功后可反向切换。首次迁移后、尚未正常发布之前没有 `previous.json`，原基线仍在运行，无需回滚新版本。

如果进程/SSH 被打断且存在 `pending.json`，后续发布会拒绝执行。管理员先检查记录、容器状态和数据兼容性，然后：

```bash
python3 /opt/apps/ithome-ai/bin/server.py recover
```

这会恢复日志中的前一镜像，不恢复数据库快照。若恢复也失败，保留日志和快照，先排查镜像是否存在、磁盘/数据库/配置问题，不要删除证据后强行再发。

**恢复代码不等于恢复数据。** 自动回滚不会覆盖用户在新版本启动后产生的数据。需要恢复数据时，必须停止 AI，确认要牺牲的后续写入，再整体恢复一份一致的五目录快照；不得只恢复 SQLite 而保留另一时点的 Milvus。

首次迁移失败或需要人工恢复原容器时，查看 `backups/bootstrap-*/container-inspect.json` 与保留的 `chatbot-backend-legacy-*`。不要在同一套 Milvus 上同时启动两个 AI 容器。

## 6. 数据格式与依赖更新规则

### 数据格式

`ai-backend/storage-version` 当前为 `1`，CI 将它写入镜像标签，服务器只接受与当前状态一致的版本。预检也会核对镜像内的标记文件。

同版本内必须保持前后读写兼容。修改 SQLite 表结构、checkpoint 格式、Milvus/embedding 格式、分块重建方式时，开发者必须评估兼容性：不兼容就提升标记，并另行审查迁移/验收/回滚方案。服务器会拒绝直接部署不同标记。

**标记不会自动发现开发者漏报的数据变更**，不能替代代码审查或备份恢复演练。尤其升级 LangGraph checkpoint、Milvus 和 embedding 模型时，不能只看单元测试。

### 依赖锁

`requirements.txt` 是依赖意图；`requirements.lock` 是 Linux amd64 / Python 3.12 实际构建版本，不应当拿它替换 macOS 上的本地开发环境。

初始直接依赖以当前本地已测试版本为约束生成，传递依赖由解析器在目标 Linux 平台锁定。更新后必须跑 Linux 镜像测试，而不能把本地旧环境通过当成新锁通过。

使用隔离工具环境中的 `uv==0.9.5`，在仓库根目录执行：

```bash
# 保持已有可兼容的锁版本；如有冲突，针对性升级，不要为通过而降低约束。
uv pip compile ai-backend/requirements.txt \
  --constraint ai-backend/requirements.lock \
  --python-version 3.12 --python-platform x86_64-unknown-linux-gnu \
  --generate-hashes --no-emit-index-url -o /tmp/requirements.lock
# 审查 /tmp/requirements.lock 的差异后再替换仓库中的锁文件。

uv pip compile ai-backend/requirements-test.in \
  --constraint ai-backend/requirements.lock \
  --python-version 3.12 --python-platform x86_64-unknown-linux-gnu \
  --generate-hashes --no-emit-index-url -o ai-backend/requirements-test.lock
```

需要升级已有包时，重新解析并审查明确的版本变化；同时维护测试锁、基础镜像 digest 与验证证据，不依赖浮动 `latest`。

## 7. 运维限制与后续验收

- 本机没有运行 Docker daemon；本轮未执行全新 Linux 镜像构建，GitHub 工作流也尚未配置 Secrets/首次运行。**不能把 YAML 检查、Mock 部署测试或本地 Python 测试当成全链路 CD 成功。**
- 首次持久化迁移已在真实服务器执行：沿用旧镜像和全部环境变量；SQLite quick_check/各表记录数与备份一致，向量文件哈希一致，其他服务容器 ID 未变。没有进行真实模型/登录/浏览器人工验收。
- 失败自动回滚的分支目前使用 Fake Docker/临时目录测试；尚未对生产故意注入坏版本。首次正式发布前应在隔离环境演练。
- 备份和旧镜像保留在同一服务器，**不是异地容灾**。没有自动清理旧镜像/备份，避免误删回滚材料；定期人工审查空间并保留有效版本，禁止无范围的 `docker system prune -a`。
- 服务器磁盘有限，共享主机上的其他项目也会增长；部署入口的空间下限不能替代告警。
- 运行凭证更新时，先备份 `shared/runtime.env`，修改后通过受控发布重建 AI 容器。不要输出完整 `compose config`、容器 inspect 或 env 到 CI 日志。
- CI/CD 管理脚本是 root 信任边界，普通镜像发布不能更新它们；修改后先测试，再由管理员通过现有可信 SSH 安装。不要把任意上传的 Compose 文件当作部署输入。

## 8. 本地验证

```bash
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s deploy/ai-backend/tests -v
RUN_MILVUS_TESTS=1 python3 -m pytest ai-backend/tests -q
git diff --check
# 有 Docker daemon 的 Linux amd64 环境：按 CI 工作流构建 test/runtime 并离线运行。
```

部署工具测试只使用临时文件、假 Docker 和合成输入，不连接生产 SSH，不读写真实用户数据库，不调用付费模型。
