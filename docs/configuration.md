# Configuration

## 环境变量

见根目录 `.env.example` 和 `web/.env.example`。

Redis 可选但推荐启用：

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

项目使用 MySQL 保存长期事实和审计记录，Redis 保存运行态缓存、阶段推进锁、幂等键和 Agent 短期记忆。未启动 Redis 时，服务会降级到 JVM 内存，适合本地快速开发；多人部署或生产环境应启用 Redis。

模型调用统一经过后端 `LlmGateway`。默认 `LLM_PROVIDER=mock`，不调用真实模型；如果配置的 Provider 未注册、未配置 API Key、不可用或调用失败，后端会自动回退到 Mock Provider，保证本地开发和 CI 不依赖真实密钥。

`LLM_PROVIDER` 支持：

- `mock`：默认值，不调用真实模型。
- `bailian`：阿里百炼 OpenAI-compatible 接口。
- `deepseek`：DeepSeek OpenAI-compatible 接口。
- `zhipu`：智谱 OpenAI-compatible 接口。
- `openai-compatible`：任意兼容 `/chat/completions` 的模型平台。

阿里百炼接入示例：

```env
LLM_PROVIDER=bailian
BAILIAN_API_KEY=your_key
BAILIAN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
BAILIAN_MODEL=qwen-plus
```

DeepSeek 接入示例：

```env
LLM_PROVIDER=deepseek
DEEPSEEK_API_KEY=your_key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
```

智谱接入示例：

```env
LLM_PROVIDER=zhipu
ZHIPU_API_KEY=your_key
ZHIPU_BASE_URL=https://open.bigmodel.cn/api/paas/v4
ZHIPU_MODEL=glm-4-flash
```

其他 OpenAI-compatible 平台接入示例：

```env
LLM_PROVIDER=openai-compatible
OPENAI_COMPATIBLE_API_KEY=your_key
OPENAI_COMPATIBLE_BASE_URL=https://api.example.com/v1
OPENAI_COMPATIBLE_MODEL=your-model
```

不要把真实 API Key 写入 `application.yml` 或提交到 GitHub；只放在本地 `.env`、系统环境变量或部署平台 Secret 中。

Agent Worker 配置：

```env
AGENT_WORKER_POOL_SIZE=4
AGENT_WORKER_MAX_POOL_SIZE=8
AGENT_WORKER_QUEUE_CAPACITY=128
AGENT_WORKER_AWAIT_TIMEOUT_SECONDS=60
```

当前 Worker 使用 JVM 内线程池执行 Agent 决策任务，并将任务状态持久化到 MySQL 的 `agent_tasks` 表。Redis 仍用于短期记忆、运行态缓存、阶段锁和幂等键。后续如果要部署多实例，建议将任务投递层升级为 Redis Stream、RocketMQ 或 Kafka。

## 可以提交

- `.env.example`
- `web/.env.example`
- `server/src/main/resources/application.yml`
- `server/src/main/resources/application-dev.example.yml`
- `server/src/main/resources/application-prod.example.yml`
- `docker-compose.example.yml`

## 禁止提交

- `.env`
- `.env.local`
- `application-local.yml`
- `application-secret.yml`
- `application-prod.yml`
- 真实数据库密码、LLM API Key、Token、证书和本地私有配置。

## MySQL 与 Redis

```bash
docker compose -f docker-compose.example.yml up -d
```

默认数据库为 `ai_werewolf`，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`，时区 `Asia/Shanghai`。
默认 Redis 端口为 `6379`。

注意：MySQL 数据库和表的字符集使用 `utf8mb4`，但 JDBC URL 中的 `characterEncoding` 应使用 Java 字符集名 `UTF-8`，不要写成 `characterEncoding=utf8mb4`。

## Actuator 与指标

后端默认暴露以下 Actuator 端点：

```text
/actuator/health
/actuator/info
/actuator/metrics
/actuator/prometheus
```

`/actuator/metrics` 适合本地排查，例如：

```bash
curl http://localhost:8080/actuator/metrics/aiwerewolf.llm.calls
curl http://localhost:8080/actuator/metrics/aiwerewolf.agent.runs
curl http://localhost:8080/actuator/metrics/aiwerewolf.agent.tasks
curl http://localhost:8080/actuator/metrics/aiwerewolf.game.phase.advances
```

`/actuator/prometheus` 用于 Prometheus 抓取。当前项目不会把 API Key、数据库密码、原始 Prompt、模型响应或玩家私有视角写入指标标签。
