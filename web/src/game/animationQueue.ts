export type GameAnimationKind = 'PHASE_CHANGED' | 'PLAYER_SPOKE' | 'PLAYER_DIED' | 'VOTE_CAST' | 'GAME_OVER'

export type GameAnimation = {
  id: string
  kind: GameAnimationKind
  payload?: unknown
  createdAt: number
}

export class GameAnimationQueue {
  private readonly items: GameAnimation[] = []

  enqueue(kind: GameAnimationKind, payload?: unknown) {
    this.items.push({
      id: `${kind}:${Date.now()}:${Math.random().toString(16).slice(2)}`,
      kind,
      payload,
      createdAt: Date.now()
    })
  }

  drain() {
    return this.items.splice(0, this.items.length)
  }

  peek() {
    return [...this.items]
  }
}
