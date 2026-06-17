import type { GameView, MemoryView, Room } from '../types/game'

export type ClientGameEvent =
  | { type: 'VIEW_SYNCED'; view: GameView }
  | { type: 'ROOM_PHASE_CHANGED'; room: Room }
  | { type: 'MEMORY_APPENDED'; memory: MemoryView }
  | { type: 'RESET' }

export type ClientGameState = {
  view: GameView | null
  room: Room | null
  version: number
  lastEventType: ClientGameEvent['type'] | null
}

export function initialClientGameState(): ClientGameState {
  return {
    view: null,
    room: null,
    version: 0,
    lastEventType: null
  }
}

export function reduceClientGameState(state: ClientGameState, event: ClientGameEvent): ClientGameState {
  switch (event.type) {
    case 'VIEW_SYNCED':
      return {
        ...state,
        view: event.view,
        version: state.version + 1,
        lastEventType: event.type
      }
    case 'ROOM_PHASE_CHANGED':
      return {
        ...state,
        room: event.room,
        view: state.view
          ? {
              ...state.view,
              status: event.room.status,
              phase: event.room.phase
            }
          : state.view,
        version: state.version + 1,
        lastEventType: event.type
      }
    case 'MEMORY_APPENDED':
      if (!state.view || state.view.memories.some(memory => memory.id === event.memory.id)) {
        return state
      }
      return {
        ...state,
        view: {
          ...state.view,
          memories: [...state.view.memories, event.memory]
        },
        version: state.version + 1,
        lastEventType: event.type
      }
    case 'RESET':
      return initialClientGameState()
  }
}

export function isGameViewPayload(value: unknown): value is GameView {
  if (!value || typeof value !== 'object') return false
  const payload = value as Partial<GameView>
  return typeof payload.roomId === 'string' && Array.isArray(payload.players) && Array.isArray(payload.memories)
}

export function isRoomPayload(value: unknown): value is Room {
  if (!value || typeof value !== 'object') return false
  const payload = value as Partial<Room>
  return typeof payload.id === 'string' && typeof payload.phase === 'string' && typeof payload.status === 'string'
}
