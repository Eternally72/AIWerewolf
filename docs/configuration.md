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

阿里百炼接入示例：

```env
LLM_PROVIDER=bailian
BAILIAN_API_KEY=your_key
BAILIAN_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
BAILIAN_MODEL=qwen-plus
```

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
