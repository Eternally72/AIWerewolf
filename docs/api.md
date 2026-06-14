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
- `POST /api/rooms/{roomId}/advance` 推进一个阶段。
- `POST /api/rooms/{roomId}/auto-advance` 推进到下一个真人操作节点。
- `POST /api/rooms/{roomId}/simulate` 一键模拟到游戏结束，主要用于全 AI 观战和 GodView 控制台。服务端最多推进 200 步，达到上限仍未结束会返回 `SIMULATION_LIMIT_REACHED`。

## 视角

- `GET /api/rooms/{roomId}/public-view`
- `GET /api/rooms/{roomId}/players/{playerId}/private-view`
- `GET /api/rooms/{roomId}/god-view`

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

## 配置

- `GET /api/roles`
- `GET /api/default-configs`

错误示例：

```json
{"success":false,"code":"INVALID_ROLE_CONFIG","message":"角色数量总和必须等于总座位数","data":null}
```
