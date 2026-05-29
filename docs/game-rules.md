# Game Rules

第一版优先保证 7 人标准局完整链路：创建房间、分配身份、夜间行动、白天发言、投票放逐、死亡结算、胜负判断。

## 流程

`WAITING -> FIRST_NIGHT -> GUARD_ACTION -> WEREWOLF_ACTION -> SEER_ACTION -> WITCH_ACTION -> NIGHT_RESOLUTION -> DAY_ANNOUNCEMENT -> LAST_WORDS -> DAY_SPEECH -> DAY_SKILL -> DAY_VOTE -> EXECUTION -> NIGHT`

## 角色

- 狼人：夜间击杀。
- 平民：白天发言与投票。
- 预言家：夜间查验阵营，隐狼显示为 GOOD。
- 女巫：解药/毒药接口已保留，Mock 第一版偏保守。
- 猎人、守卫等角色能力通过 `RoleAbility` 扩展。

## 胜利规则

- 屠边：狼人全死好人胜；平民全死或神职全死狼人胜。
- 屠城：狼人数量大于等于好人或好人全死狼人胜。
- 情侣第三方：接口预留，后续增强完整结算。
