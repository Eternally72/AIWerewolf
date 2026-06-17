import type { PlayerView } from '../types/game'

const PHASE_NAMES: Record<string, string> = {
  WAITING: '等待开始',
  ROLE_ASSIGNED: '身份分配',
  FIRST_NIGHT: '首夜',
  NIGHT: '夜晚',
  CUPID_ACTION: '丘比特行动',
  GUARD_ACTION: '守卫行动',
  WEREWOLF_ACTION: '狼人行动',
  SEER_ACTION: '预言家行动',
  WITCH_ACTION: '女巫行动',
  OTHER_NIGHT_ACTION: '其他夜间行动',
  NIGHT_RESOLUTION: '夜晚结算',
  DAY_ANNOUNCEMENT: '白天公告',
  LAST_WORDS: '遗言',
  SHERIFF_ELECTION: '警长竞选',
  DAY_SPEECH: '白天发言',
  DAY_SKILL: '白天技能',
  DAY_VOTE: '投票放逐',
  EXECUTION: '放逐结算',
  HUNTER_SHOOT: '猎人开枪',
  WHITE_WOLF_KING_EXPLODE: '白狼王自爆',
  GAME_OVER: '游戏结束'
}

const ROLE_NAMES: Record<string, string> = {
  WEREWOLF: '狼人',
  WOLF_KING: '狼王',
  WHITE_WOLF_KING: '白狼王',
  HIDDEN_WOLF: '隐狼',
  VILLAGER: '平民',
  SEER: '预言家',
  WITCH: '女巫',
  HUNTER: '猎人',
  GUARD: '守卫',
  IDIOT: '白痴',
  KNIGHT: '骑士',
  GRAVE_KEEPER: '守墓人',
  MAGICIAN: '魔术师',
  CUPID: '丘比特',
  ELDER: '长老'
}

const CAMP_NAMES: Record<string, string> = {
  WEREWOLF: '狼人阵营',
  GOOD: '好人阵营',
  THIRD_PARTY: '第三方',
  LOVERS: '情侣阵营'
}

const STATUS_NAMES: Record<string, string> = {
  WAITING: '等待中',
  RUNNING: '进行中',
  PAUSED: '已暂停',
  GAME_OVER: '已结束'
}

export function phaseName(phase?: string | null) {
  return phase ? PHASE_NAMES[phase] ?? phase : '等待同步'
}

export function roleName(role?: string | null) {
  return role ? ROLE_NAMES[role] ?? role : '未知身份'
}

export function campName(camp?: string | null) {
  return camp ? CAMP_NAMES[camp] ?? camp : '未知阵营'
}

export function statusText(status?: string | null) {
  return status ? STATUS_NAMES[status] ?? status : '同步中'
}

export function playerLabel(players: PlayerView[], playerId: string) {
  const player = players.find(item => item.id === playerId)
  return player ? `${player.seatNumber} 号 ${player.name}` : playerId
}

export function replacePlayerIds(content: string, players: PlayerView[]) {
  return players.reduce(
    (text, player) => text.replaceAll(player.id, `${player.seatNumber} 号 ${player.name}`),
    content
  )
}

function withPrefix(prefix: string, name: string) {
  return prefix ? `${prefix}-${name}` : name
}

export function campClass(camp?: string | null, prefix = 'camp') {
  if (camp === 'WEREWOLF') return withPrefix(prefix, 'wolf')
  if (camp === 'GOOD') return withPrefix(prefix, 'good')
  if (camp === 'THIRD_PARTY' || camp === 'LOVERS') return withPrefix(prefix, 'third')
  return withPrefix(prefix, 'hidden')
}
