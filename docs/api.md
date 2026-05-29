# API

统一响应：

```json
{"success":true,"code":"OK","message":"success","data":{},"timestamp":"2026-05-29T00:00:00Z"}
```

## 房间

- `POST /api/rooms` 创建房间。Body 使用 `CreateRoomRequest`。
- `GET /api/rooms/{roomId}` 查询房间。
- `POST /api/rooms/{roomId}/start` 开始游戏并分配身份。
- `POST /api/rooms/{roomId}/pause` 暂停。
- `POST /api/rooms/{roomId}/resume` 继续。
- `POST /api/rooms/{roomId}/advance` 推进一个阶段。
- `POST /api/rooms/{roomId}/auto-advance` 推进到下一个真人操作节点。

## 视角

- `GET /api/rooms/{roomId}/public-view`
- `GET /api/rooms/{roomId}/players/{playerId}/private-view`
- `GET /api/rooms/{roomId}/god-view?god=true`

普通请求不允许访问 GodView。

## 玩家操作

- `POST /api/rooms/{roomId}/players/{playerId}/speech`
- `POST /api/rooms/{roomId}/players/{playerId}/vote`
- `POST /api/rooms/{roomId}/players/{playerId}/night-action`
- `POST /api/rooms/{roomId}/players/{playerId}/day-skill`

## 时间线

- `GET /api/rooms/{roomId}/timeline/public`
- `GET /api/rooms/{roomId}/timeline/private/{playerId}`
- `GET /api/rooms/{roomId}/timeline/god?god=true`

## 配置

- `GET /api/roles`
- `GET /api/default-configs`

错误示例：

```json
{"success":false,"code":"INVALID_ROLE_CONFIG","message":"角色数量总和必须等于总座位数","data":null}
```
