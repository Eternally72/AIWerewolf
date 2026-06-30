# API

统一响应：

```json
{"success":true,"code":"OK","message":"success","data":{},"timestamp":"2026-05-29T00:00:00Z"}
```

## 房间

- `POST /api/rooms` 创建房间。Body 使用 `CreateRoomRequest`。
  - 响应中的 `data.godViewToken` 只在创建房间时返回，前端应本地保存，不要提交到 Git。
- `GET /api/rooms/{roomId}` 查询房间。
- `POST /api/rooms/{roomId}/start` 开始游戏并分配身份。
- `POST /api/rooms/{roomId}/pause` 暂停。
- `POST /api/rooms/{roomId}/resume` 继续。
- `POST /api/rooms/{roomId}/advance` 推进一个游戏步骤。需要模型决策时，每次最多处理一个 Agent；阶段内所有玩家完成后才切换阶段。
- `POST /api/rooms/{roomId}/auto-advance` 推进到下一个真人操作节点。
- `POST /api/rooms/{roomId}/simulate` 一键模拟到游戏结束，主要用于测试和快速评测。服务端最多推进 1000 步，达到上限仍未结束会返回 `SIMULATION_LIMIT_REACHED`。

## 视角

- `GET /api/rooms/{roomId}/public-view`
- `GET /api/rooms/{roomId}/players/{playerId}/private-view`
- `GET /api/rooms/{roomId}/god-view`
- `GET /api/rooms/{roomId}/agent-runs` 查询最近 100 条 AI 决策运行记录，需要 GodView 令牌。
- `GET /api/rooms/{roomId}/agent-tasks` 查询最近 100 条 AI 决策任务状态，需要 GodView 令牌。

GodView 必须携带创建房间时返回的主持人令牌：

```http
X-God-View-Token: <godViewToken>
```

普通请求不允许访问 GodView，缺少或错误令牌会返回 `ACCESS_DENIED`。

## 玩家操作

- `POST /api/rooms/{roomId}/players/{playerId}/speech`
- `POST /api/rooms/{roomId}/players/{playerId}/vote`
- `POST /api/rooms/{roomId}/players/{playerId}/night-action`
- `POST /api/rooms/{roomId}/players/{playerId}/day-skill`

玩家操作会使用房间当前轮次，不需要前端传 `roundNumber`。后端会校验当前阶段、玩家存活状态、目标是否属于同一房间且存活。

## 时间线

- `GET /api/rooms/{roomId}/timeline/public`
- `GET /api/rooms/{roomId}/timeline/private/{playerId}`
- `GET /api/rooms/{roomId}/timeline/god`，同样需要 `X-God-View-Token`。
- `GET /api/rooms/{roomId}/replay/public` 查询公开事件回放。
- `GET /api/rooms/{roomId}/replay/god` 查询完整事件回放，需要 `X-God-View-Token`。

事件回放响应示例：

```json
{
  "id": "event-id",
  "roomId": "room-id",
  "roundNumber": 1,
  "phase": "DAY_SPEECH",
  "eventType": "SPEECH",
  "payloadJson": "{\"content\":\"玩家发言\"}",
  "scope": "PUBLIC",
  "createdAt": "2026-06-14T12:00:00Z"
}
```

## 配置

- `GET /api/roles`
- `GET /api/default-configs`

## AI Infra Evaluation

- `POST /api/evaluations/run`

运行批量自动对局评测。该接口仅在 `dev/test` profile 暴露；当前支持 `7-standard` 模板，`gameCount` 范围为 1 到 20，未传时默认 1。

请求示例：

```json
{"gameCount":1,"templateId":"7-standard"}
```

响应会包含完整结束率、失败局数、AgentRun 数、fallback 次数、fallback 率、非法决策 fallback 次数、公共视角泄露计数、平均延迟和平均轮次。

## Actuator

后端接入 Actuator/Micrometer 后，可以通过以下端点查看运行状态和 AI Infra 指标：

- `GET /actuator/health`
- `GET /actuator/metrics`
- `GET /actuator/metrics/aiwerewolf.llm.calls`
- `GET /actuator/metrics/aiwerewolf.agent.runs`
- `GET /actuator/metrics/aiwerewolf.agent.tasks`
- `GET /actuator/metrics/aiwerewolf.game.phase.advances`
- `GET /actuator/prometheus`

这些端点用于观测模型调用、fallback、AgentRun、阶段推进和评测结果，不返回玩家私有视角、GodView、原始 Prompt 或模型密钥。

错误示例：

```json
{"success":false,"code":"INVALID_ROLE_CONFIG","message":"角色数量总和必须等于总座位数","data":null}
```
