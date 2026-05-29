# Configuration

## 环境变量

见根目录 `.env.example` 和 `web/.env.example`。

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

## MySQL

```bash
docker compose -f docker-compose.example.yml up -d
```

默认数据库为 `ai_werewolf`，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`，时区 `Asia/Shanghai`。
