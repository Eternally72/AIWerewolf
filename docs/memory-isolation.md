# Memory Isolation

信息隔离是项目核心。

## 记忆作用域

- `PUBLIC`：公开事件、发言、投票、死亡公告。
- `PRIVATE`：单个玩家身份、技能结果、AI 策略摘要。
- `WEREWOLF_TEAM`：狼队信息和夜聊。
- `LOVERS`：情侣关系。
- `THIRD_PARTY_TEAM`：第三方阵营内部信息。
- `GOD_VIEW`：完整身份、行动、死亡原因和策略摘要。

## GameViewBuilder

普通前端和 AI Agent 都只能读取 `GameViewBuilder` 输出：

- `buildPublicView(roomId)` 不包含身份真相。
- `buildPrivateView(roomId, playerId)` 只展示本人身份和本人可见记忆。
- 狼人私有视角会展示狼队友身份。
- 情侣视角会展示情侣对象，第三方阵营视角会展示同阵营成员。
- `buildGodView(roomId)` 只供授权观众或主持人使用，REST 调用必须携带 `X-God-View-Token`。

禁止把完整 Entity 或完整 GameState 直接返回给 Controller、前端或 AI。

## 短期记忆

`MemoryEntry` 仍然落 MySQL，作为长期事实和可审计记录。Agent 的短期记忆通过 Redis 保存最近观察，key 形如：

```text
agent:{roomId}:{playerId}:stm
```

短期记忆有 TTL，并限制条数；Redis 不可用时降级为 JVM 内存。AI Prompt 只拼接当前 Agent 可见的短期记忆，不会读取其他玩家私有记忆或 GodView。

## WebSocket

公共频道只推送公开事件；私有频道只推送目标玩家事件；GodView 频道只给授权视角使用。推送失败不会回滚游戏状态。
