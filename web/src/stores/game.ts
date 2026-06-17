import { defineStore } from 'pinia'
import { autoAdvance, createRoom, getGodView, getPublicView, simulateRoom, startRoom } from '../api/client'
import {
  initialClientGameState,
  isGameViewPayload,
  isRoomPayload,
  reduceClientGameState,
  type ClientGameEvent
} from '../game/clientGameState'
import { GameAnimationQueue, type GameAnimation } from '../game/animationQueue'
import type { GameView, Room } from '../types/game'

const publicViewLoads = new Map<string, Promise<GameView>>()
const animationQueue = new GameAnimationQueue()

export const useGameStore = defineStore('game', {
  state: () => ({
    room: null as Room | null,
    view: null as GameView | null,
    clientState: initialClientGameState(),
    animations: [] as GameAnimation[],
    error: '',
    loading: false
  }),
  actions: {
    dispatch(event: ClientGameEvent) {
      this.enqueueAnimations(event)
      this.clientState = reduceClientGameState(this.clientState, event)
      this.view = this.clientState.view
      if (this.clientState.room) {
        this.room = this.clientState.room
      }
    },
    enqueueAnimations(event: ClientGameEvent) {
      if (event.type === 'ROOM_PHASE_CHANGED') {
        animationQueue.enqueue(event.room.phase === 'GAME_OVER' ? 'GAME_OVER' : 'PHASE_CHANGED', event.room)
      }
      if (event.type === 'VIEW_SYNCED') {
        const oldView = this.clientState.view
        if (oldView?.phase !== event.view.phase) {
          animationQueue.enqueue(event.view.phase === 'GAME_OVER' ? 'GAME_OVER' : 'PHASE_CHANGED', event.view.phase)
        }
        const oldSpeechKeys = new Set((oldView?.speeches ?? []).map(speech => `${speech.playerId}:${speech.createdAt}`))
        event.view.speeches
          .filter(speech => !oldSpeechKeys.has(`${speech.playerId}:${speech.createdAt}`))
          .forEach(speech => animationQueue.enqueue('PLAYER_SPOKE', speech))
        const oldVoteKeys = new Set((oldView?.votes ?? []).map(vote => `${vote.voterPlayerId}:${vote.targetPlayerId}:${vote.createdAt}`))
        event.view.votes
          .filter(vote => !oldVoteKeys.has(`${vote.voterPlayerId}:${vote.targetPlayerId}:${vote.createdAt}`))
          .forEach(vote => animationQueue.enqueue('VOTE_CAST', vote))
        const oldPlayers = new Map((oldView?.players ?? []).map(player => [player.id, player]))
        event.view.players
          .filter(player => oldPlayers.get(player.id)?.alive && !player.alive)
          .forEach(player => animationQueue.enqueue('PLAYER_DIED', player))
      }
      this.animations = animationQueue.drain()
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
      this.room = await startRoom(roomId)
      this.dispatch({ type: 'ROOM_PHASE_CHANGED', room: this.room })
    },
    async loadPublic(roomId: string) {
      // 同一房间的 WebSocket 事件可能密集到达，请求合并可以避免重复拉取完整视图。
      let loading = publicViewLoads.get(roomId)
      if (!loading) {
        loading = getPublicView(roomId).finally(() => publicViewLoads.delete(roomId))
        publicViewLoads.set(roomId, loading)
      }
      this.syncView(await loading)
    },
    async loadGod(roomId: string) {
      this.syncView(await getGodView(roomId))
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
