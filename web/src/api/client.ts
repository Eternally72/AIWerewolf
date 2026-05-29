import axios from 'axios'
import type { ApiResponse, GameView, Room } from '../types/game'

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

export async function getPublicView(roomId: string) {
  const { data } = await http.get<ApiResponse<GameView>>(`/api/rooms/${roomId}/public-view`)
  return data.data
}

export async function getGodView(roomId: string) {
  const { data } = await http.get<ApiResponse<GameView>>(`/api/rooms/${roomId}/god-view?god=true`)
  return data.data
}
