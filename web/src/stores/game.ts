import { defineStore } from 'pinia'
import { autoAdvance, createRoom, getGodView, getPublicView, simulateRoom, startRoom } from '../api/client'
import type { GameView, Room } from '../types/game'

export const useGameStore = defineStore('game', {
  state: () => ({
    room: null as Room | null,
    view: null as GameView | null,
    error: '',
    loading: false
  }),
  actions: {
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
      this.room = await startRoom(roomId)
    },
    async loadPublic(roomId: string) {
      this.view = await getPublicView(roomId)
    },
    async loadGod(roomId: string) {
      this.view = await getGodView(roomId)
    },
    async auto(roomId: string) {
      this.room = await autoAdvance(roomId)
      await this.loadPublic(roomId)
    },
    async simulate(roomId: string) {
      this.room = await simulateRoom(roomId)
      await this.loadGod(roomId)
    }
  }
})
