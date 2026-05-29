# Agent Design

每个 AI 玩家是独立 Agent，拥有独立身份、阵营、私有记忆、策略和发言风格。

## LLM

- `LlmClient`：统一完成接口。
- `MockLlmClient`：默认启用，不调用真实模型，CI 可运行。
- `BailianLlmClient`：用于阿里百炼兼容模式接口，通过环境变量启用，未配置 API Key 时回退 Mock。

## 输出格式

AI 发言、投票、夜间行动和白天技能均设计为 JSON。当前 MVP 的 Mock Agent 使用启发式策略直接构建决策对象；真实 LLM 接入时应解析 JSON，非法输出自动 fallback。

## Fallback

当 AI 决策为空、目标非法或解析失败时，系统选择第一个合法存活目标或 `NONE`，确保自动对局不中断。
