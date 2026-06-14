export type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
  timestamp: string
}

export type RoleConfig = {
  werewolfCount: number
  wolfKingCount: number
  whiteWolfKingCount: number
  hiddenWolfCount: number
  villagerCount: number
  seerCount: number
  witchCount: number
  hunterCount: number
  guardCount: number
  idiotCount: number
  knightCount: number
  graveKeeperCount: number
  magicianCount: number
  cupidCount: number
  elderCount: number
}

export type RuleConfig = {
  victoryRule: string
  enableSheriff: boolean
  enableLastWords: boolean
  allowWitchSaveSelfFirstNight: boolean
  allowHunterShootWhenPoisoned: boolean
  allowGuardProtectSameTargetConsecutively: boolean
  allowWerewolfNightChat: boolean
  allowWhiteWolfKingExplode: boolean
  enableLovers: boolean
  speechTimeLimitSeconds: number
  voteTimeLimitSeconds: number
  nightActionTimeLimitSeconds: number
  aiThinkingDelayMillis: number
  autoAdvance: boolean
  revealRoleOnDeath: boolean
}

export type UiConfig = {
  theme: string
  animationLevel: string
  enableSoundEffect: boolean
  showRoleAvatar: boolean
  showTimeline: boolean
  showGodViewPanel: boolean
}

export type Room = {
  id: string
  name: string
  status: string
  phase: string
  totalSeats: number
  humanMode: string
  observerViewMode: string
  godViewToken?: string | null
}

export type PlayerView = {
  id: string
  seatNumber: number
  name: string
  type: string
  alive: boolean
  canSpeak: boolean
  canVote: boolean
  role: string | null
  camp: string | null
}

export type MemoryView = {
  id: string
  roundNumber: number
  phase: string
  scope: string
  eventType: string
  content: string
  createdAt: string
}

export type GameView = {
  roomId: string
  roomName: string
  status: string
  phase: string
  roundNumber: number
  viewerPlayerId: string | null
  ownRole: string | null
  ownCamp: string | null
  players: PlayerView[]
  memories: MemoryView[]
  speeches: { playerId: string; roundNumber: number; content: string; claimedRole: string | null; createdAt: string }[]
  votes: { voterPlayerId: string; targetPlayerId: string; reason: string; createdAt: string }[]
  godView: boolean
}
