# Agent Design

每个 AI 玩家是独立 Agent，拥有独立身份、阵营、私有记忆、策略和发言风格。

## LLM

- `LlmClient`：统一完成接口。
- `MockLlmClient`：默认启用，不调用真实模型，CI 可运行。
- `BailianLlmClient`：用于阿里百炼兼容模式接口，通过环境变量启用，未配置 API Key 时回退 Mock。

## Prompt 设计

角色系统提示词采用资源文件方式管理，位置：

```text
server/src/main/resources/prompts/roles/
├── common.md
├── unknown.md
├── werewolf.md
├── wolf_king.md
├── white_wolf_king.md
├── hidden_wolf.md
├── villager.md
├── seer.md
├── witch.md
├── hunter.md
├── guard.md
├── idiot.md
├── knight.md
├── grave_keeper.md
├── magician.md
├── cupid.md
└── elder.md
```

`common.md` 定义所有 Agent 共享的硬约束：信息隔离、禁止上帝视角、JSON 输出格式、fallback 行为。每个角色文件只描述该角色的可见信息、胜利目标、夜晚策略、白天发言、投票和技能使用方式。

`AgentPromptFactory` 会在启动时加载并缓存所有角色 prompt。文件名由 `Role.name().toLowerCase()` 决定，例如 `WHITE_WOLF_KING` 对应 `white_wolf_king.md`。如果新增角色但没有新增对应 prompt 文件，工厂会抛出异常，测试也会失败。

## 输出格式

AI 发言、投票、夜间行动和白天技能均使用 JSON。`AiAgentService` 会调用百炼或兼容模型，提取返回文本中的 JSON 对象，校验目标是否合法，再转换为领域决策对象。

非法 JSON、缺失字段、非法目标、狼人投可见狼队友等情况会触发 fallback。Mock LLM 默认返回 JSON，本地和 CI 不需要真实 API Key。

Agent Prompt 会拼接 Redis 中保存的短期记忆：

```text
agent:{roomId}:{playerId}:stm
```

短期记忆只来自该 Agent 可见的私有/共享记忆，不包含 GodView。

## Fallback

当 AI 决策为空、目标非法或解析失败时，系统选择第一个合法存活目标或 `NONE`，确保自动对局不中断。
