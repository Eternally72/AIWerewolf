import { Client } from '@stomp/stompjs'

export function connectGameSocket(
  roomId: string,
  onMessage: (payload?: unknown, destination?: string) => void,
  options: { includeGodView?: boolean } = {}
) {
  const base = import.meta.env.VITE_WS_BASE_URL ?? 'ws://localhost:8080/ws/game'
  let timer: number | null = null
  const scheduleReload = () => {
    if (timer !== null) return
    // 多个阶段/时间线事件常常在同一轮结算中连续到达，这里合并为一次 REST 状态恢复。
    timer = window.setTimeout(() => {
      timer = null
      onMessage()
    }, 180)
  }
  const handleMessage = (destination: string) => (message: { body: string }) => {
    const payload = parsePayload(message.body)
    if (isFullView(payload)) {
      onMessage(payload, destination)
      return
    }
    scheduleReload()
  }
  const client = new Client({
    brokerURL: base,
    reconnectDelay: 2500,
    onConnect: () => {
      client.subscribe(`/topic/rooms/${roomId}/public`, handleMessage('public'))
      client.subscribe(`/topic/rooms/${roomId}/phase`, handleMessage('phase'))
      client.subscribe(`/topic/rooms/${roomId}/timeline`, handleMessage('timeline'))
      if (options.includeGodView) {
        client.subscribe(`/topic/rooms/${roomId}/god-view`, handleMessage('god-view'))
      }
    }
  })
  client.activate()
  return () => {
    if (timer !== null) {
      window.clearTimeout(timer)
    }
    client.deactivate()
  }
}

function parsePayload(body: string) {
  try {
    return JSON.parse(body)
  } catch {
    return null
  }
}

function isFullView(payload: unknown): boolean {
  if (!payload || typeof payload !== 'object') {
    return false
  }
  const maybeView = payload as { roomId?: unknown; players?: unknown; memories?: unknown }
  return typeof maybeView.roomId === 'string' && Array.isArray(maybeView.players) && Array.isArray(maybeView.memories)
}
