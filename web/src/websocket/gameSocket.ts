import { Client } from '@stomp/stompjs'

export function connectGameSocket(roomId: string, onMessage: () => void) {
  const base = import.meta.env.VITE_WS_BASE_URL ?? 'ws://localhost:8080/ws/game'
  const client = new Client({
    brokerURL: base,
    reconnectDelay: 2500,
    onConnect: () => {
      client.subscribe(`/topic/rooms/${roomId}/public`, onMessage)
      client.subscribe(`/topic/rooms/${roomId}/phase`, onMessage)
      client.subscribe(`/topic/rooms/${roomId}/timeline`, onMessage)
      client.subscribe(`/topic/rooms/${roomId}/god-view`, onMessage)
    }
  })
  client.activate()
  return () => client.deactivate()
}
