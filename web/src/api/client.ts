import axios from 'axios'
import type { AgentRun, AgentTask, ApiResponse, GameEvent, GameView, Room } from '../types/game'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080',
  timeout: 12000
})

http.interceptors.response.use(
  response => response,
  error => Promise.reject(new Error(error.response?.data?.message ?? '请求失败，请稍后重试'))
)

export async function getDefaults() {
  const { data } = await http.get<ApiResponse<any>>('/api/default-configs')
  return data.data
}

export async function createRoom(payload: unknown) {
  const { data } = await http.post<ApiResponse<Room>>('/api/rooms', payload)
  if (data.data.godViewToken) {
    localStorage.setItem(`godViewToken:${data.data.id}`, data.data.godViewToken)
  }
  return data.data
}

export async function startRoom(roomId: string) {
  const { data } = await http.post<ApiResponse<Room>>(`/api/rooms/${roomId}/start`)
  return data.data
}

export async function advanceRoom(roomId: string) {
  const { data } = await http.post<ApiResponse<Room>>(`/api/rooms/${roomId}/advance`)
  return data.data
}

export async function autoAdvance(roomId: string) {
  const { data } = await http.post<ApiResponse<Room>>(`/api/rooms/${roomId}/auto-advance`)
  return data.data
}

export async function simulateRoom(roomId: string) {
  const { data } = await http.post<ApiResponse<Room>>(`/api/rooms/${roomId}/simulate`)
  return data.data
}

export async function getPublicView(roomId: string) {
  const { data } = await http.get<ApiResponse<GameView>>(`/api/rooms/${roomId}/public-view`)
  return data.data
}

export async function getPrivateView(roomId: string, playerId: string) {
  const { data } = await http.get<ApiResponse<GameView>>(`/api/rooms/${roomId}/players/${playerId}/private-view`)
  return data.data
}

export async function getGodView(roomId: string) {
  const token = localStorage.getItem(`godViewToken:${roomId}`) ?? ''
  const { data } = await http.get<ApiResponse<GameView>>(`/api/rooms/${roomId}/god-view`, {
    headers: { 'X-God-View-Token': token }
  })
  return data.data
}

export async function submitSpeech(roomId: string, playerId: string, content: string, claimedRole: string | null) {
  await http.post<ApiResponse<null>>(`/api/rooms/${roomId}/players/${playerId}/speech`, { content, claimedRole })
}

export async function submitVote(roomId: string, playerId: string, targetPlayerId: string, reason: string) {
  await http.post<ApiResponse<null>>(`/api/rooms/${roomId}/players/${playerId}/vote`, { targetPlayerId, reason })
}

export async function submitNightAction(
  roomId: string,
  playerId: string,
  actionType: string,
  targetPlayerId: string | null,
  secondaryTargetPlayerId: string | null,
  reason: string
) {
  await http.post<ApiResponse<null>>(`/api/rooms/${roomId}/players/${playerId}/night-action`, {
    actionType,
    targetPlayerId,
    secondaryTargetPlayerId,
    reason
  })
}

export async function getAgentRuns(roomId: string) {
  const token = localStorage.getItem(`godViewToken:${roomId}`) ?? ''
  const { data } = await http.get<ApiResponse<AgentRun[]>>(`/api/rooms/${roomId}/agent-runs`, {
    headers: { 'X-God-View-Token': token }
  })
  return data.data
}

export async function getAgentTasks(roomId: string) {
  const token = localStorage.getItem(`godViewToken:${roomId}`) ?? ''
  const { data } = await http.get<ApiResponse<AgentTask[]>>(`/api/rooms/${roomId}/agent-tasks`, {
    headers: { 'X-God-View-Token': token }
  })
  return data.data
}

export async function getPublicReplay(roomId: string) {
  const { data } = await http.get<ApiResponse<GameEvent[]>>(`/api/rooms/${roomId}/replay/public`)
  return data.data
}

export async function getGodReplay(roomId: string) {
  const token = localStorage.getItem(`godViewToken:${roomId}`) ?? ''
  const { data } = await http.get<ApiResponse<GameEvent[]>>(`/api/rooms/${roomId}/replay/god`, {
    headers: { 'X-God-View-Token': token }
  })
  return data.data
}
