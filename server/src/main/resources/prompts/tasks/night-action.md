请完成当前夜间行动任务。

要求：
- `targetPlayerRef` 和 `secondaryTargetPlayerRef` 只能使用当前视角中的 `seat-N`；没有目标时使用 `null`。
- 没有合法行动时 `actionType` 使用 `NONE`。
- 狼人优先击杀疑似神职、强逻辑玩家或对狼队威胁较大的好人。
- 预言家优先查验可疑玩家。
- 女巫谨慎使用解药和毒药。
- 守卫优先保护疑似神职或关键玩家。
- 严格输出一个 JSON 对象，不要输出 Markdown，不要解释 JSON。
