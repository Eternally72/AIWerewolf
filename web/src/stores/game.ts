import { defineStore } from 'pinia'
import { advanceRoom, autoAdvance, createRoom, getGodView, getPublicView, getRoom, simulateRoom, startRoom } from '../api/client'
import {
  initialClientGameState,
  isGameViewPayload,
  isRoomPayload,
  reduceClientGameState,
  type ClientGameEvent
} from '../game/clientGameState'
import type { GameView, Room } from '../types/game'

const publicViewLoads = new Map<string, Promise<GameView>>()

export const useGameStore = defineStore('game', {
  state: () => ({
    room: null as Room | null,
    view: null as GameView | null,
    clientState: initialClientGameState(),
    error: '',
    loading: false
  }),
  actions: {
    dispatch(event: ClientGameEvent) {
      this.clientState = reduceClientGameState(this.clientState, event)
      this.view = this.clientState.view
      if (this.clientState.room) {
        this.room = this.clientState.room
      }
    },
    syncView(view: GameView) {
      this.dispatch({ type: 'VIEW_SYNCED', view })
    },
    applySocketPayload(payload: unknown) {
      if (isGameViewPayload(payload)) {
        this.syncView(payload)
        return true
      }
      if (isRoomPayload(payload)) {
        this.dispatch({ type: 'ROOM_PHASE_CHANGED', room: payload })
        return false
      }
      return false
    },
    async create(payload: unknown) {
      this.loading = true
      try {
        this.room = await createRoom(payload)
        localStorage.setItem('lastRoomId', this.room.id)
      } catch (error) {
        this.error = error instanceof Error ? error.message : '创建失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async start(roomId: string) {
      this.loading = true
      this.error = ''
      try {
        this.room = await startRoom(roomId)
        this.dispatch({ type: 'ROOM_PHASE_CHANGED', room: this.room })
      } catch (error) {
        this.error = error instanceof Error ? error.message : '开始游戏失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async loadPublic(roomId: string) {
      // 同一房间的 WebSocket 事件可能密集到达，请求合并可以避免重复拉取完整视图。
      if (!this.room || this.room.id !== roomId) {
        this.room = await getRoom(roomId)
      }
      let loading = publicViewLoads.get(roomId)
      if (!loading) {
        loading = getPublicView(roomId).finally(() => publicViewLoads.delete(roomId))
        publicViewLoads.set(roomId, loading)
      }
      let view = await loading
      if (view.players.length === 0 && view.status === 'RUNNING') {
        await startRoom(roomId)
        view = await getPublicView(roomId)
      }
      this.syncView(view)
      this.error = ''
    },
    async loadGod(roomId: string) {
      this.syncView(await getGodView(roomId))
      this.error = ''
    },
    async auto(roomId: string) {
      this.room = await autoAdvance(roomId)
      await this.loadPublic(roomId)
      this.error = ''
    },
    async advanceStep(roomId: string) {
      this.room = await advanceRoom(roomId)
      await this.loadPublic(roomId)
      this.error = ''
    },
    async simulate(roomId: string) {
      this.room = await simulateRoom(roomId)
      await this.loadGod(roomId)
      this.error = ''
    }
  }
})
