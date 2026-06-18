<template>
  <main class="page arena-page" :class="{ 'game-over': isGameOver }">
    <section class="arena-shell">
      <header class="arena-bar">
        <div>
          <strong>{{ view?.roomName ?? 'AI Werewolf' }}</strong>
          <span>
            {{ phaseName(view?.phase) }} · 第 {{ view?.roundNumber ?? 0 }} 轮 · {{ statusText(view?.status) }}
            <template v-if="victorySummary"> · {{ victorySummary.title }}</template>
          </span>
        </div>
        <div class="actions">
          <button class="btn secondary" :disabled="refreshing || advancing" @click="reload">
            {{ refreshing ? '同步中...' : '刷新' }}
          </button>
          <button class="btn secondary" :disabled="refreshing || advancing" @click="advance">
            {{ advancing ? '推进中...' : '自动推进' }}
          </button>
          <RouterLink class="btn" :to="`/rooms/${roomId}/god`">上帝视角</RouterLink>
          <RouterLink v-if="isGameOver" class="btn secondary" :to="`/rooms/${roomId}/over`">结算页</RouterLink>
        </div>
      </header>

      <aside class="player-rail left-rail">
        <article v-for="player in leftPlayers" :key="player.id" class="player-chip" :class="playerClass(player)">
          <img v-if="roleAsset(player.role)" class="role-token" :src="roleAsset(player.role) ?? ''" :alt="roleName(player.role)" />
          <b v-else>{{ player.seatNumber }}</b>
          <div>
            <strong>{{ player.name }}</strong>
            <span>{{ player.alive ? '存活' : '死亡' }} · {{ publicRoleText(player.role) }}</span>
          </div>
        </article>
      </aside>

      <section class="arena-center">
        <div class="phase-panel">
          <span>{{ phaseName(view?.phase) }}</span>
          <strong>{{ phaseHint }}</strong>
          <em v-if="currentSpeechTurn">{{ currentSpeechTurn }} 正在组织发言...</em>
        </div>

        <VirtualList v-if="arenaEvents.length" class="event-stream" :items="arenaEvents" :item-height="92" key-field="id">
          <template #default="{ item }">
            <article class="event-card" :class="`event-${item.kind}`">
              <small>{{ item.meta }}</small>
              <strong>{{ item.title }}</strong>
              <p>{{ renderedEventContent(item) }}<i v-if="isEventStreaming(item.id)" class="typing-cursor" /></p>
            </article>
          </template>
        </VirtualList>
        <div v-else class="empty-state">
          <strong>对局正在准备</strong>
          <span>点击自动推进后，Agent 的行动、发言和公开结算会在这里流式呈现。</span>
        </div>
      </section>

      <aside class="player-rail right-rail">
        <article v-for="player in rightPlayers" :key="player.id" class="player-chip" :class="playerClass(player)">
          <img v-if="roleAsset(player.role)" class="role-token" :src="roleAsset(player.role) ?? ''" :alt="roleName(player.role)" />
          <b v-else>{{ player.seatNumber }}</b>
          <div>
            <strong>{{ player.name }}</strong>
            <span>{{ player.alive ? '存活' : '死亡' }} · {{ publicRoleText(player.role) }}</span>
          </div>
        </article>
      </aside>

      <aside class="control-panel">
        <section v-if="!isGameOver" class="panel">
          <h2>玩家操作</h2>
          <label class="label">操作者
            <select v-model="actorId" class="input">
              <option value="">选择玩家</option>
              <option v-for="player in alivePlayers" :key="player.id" :value="player.id">
                {{ player.seatNumber }} 号 {{ player.name }}
              </option>
            </select>
          </label>
          <div v-if="privateView" class="identity">
            <span>{{ roleName(privateView.ownRole) }}</span>
            <strong>{{ campName(privateView.ownCamp) }}</strong>
          </div>
          <textarea v-model="speechContent" class="input textarea" placeholder="发言内容" />
          <input v-model="claimedRole" class="input" placeholder="声称身份，可空" />
          <button class="btn secondary" :disabled="!actorId || !speechContent" @click="sendSpeech">提交发言</button>

          <select v-model="voteTargetId" class="input">
            <option value="">投票目标</option>
            <option v-for="player in voteTargets" :key="player.id" :value="player.id">
              {{ player.seatNumber }} 号 {{ player.name }}
            </option>
          </select>
          <input v-model="voteReason" class="input" placeholder="投票理由" />
          <button class="btn secondary" :disabled="!actorId || !voteTargetId" @click="sendVote">提交投票</button>

          <select v-model="actionType" class="input">
            <option v-for="option in actionOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <select v-model="actionTargetId" class="input">
            <option value="">行动目标</option>
            <option v-for="player in voteTargets" :key="player.id" :value="player.id">
              {{ player.seatNumber }} 号 {{ player.name }}
            </option>
          </select>
          <button class="btn secondary" :disabled="!actorId" @click="sendNightAction">提交夜间行动</button>
        </section>
        <section v-else class="panel result-panel">
          <h2>本局结算</h2>
          <strong>{{ victorySummary?.title ?? '游戏结束' }}</strong>
          <p>{{ victorySummary?.reason ?? '胜负结果已写入时间线。' }}</p>
          <RouterLink class="btn" :to="`/rooms/${roomId}/over`">查看结算页</RouterLink>
        </section>
        <p v-if="store.error" class="error">{{ store.error }}</p>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPrivateView, submitNightAction, submitSpeech, submitVote } from '../api/client'
import { roleAsset } from '../assets'
import VirtualList from '../components/VirtualList.vue'
import { campName, phaseName, playerLabel, replacePlayerIds, roleName, statusText } from '../game/gameLabels'
import { useGameStore } from '../stores/game'
import { connectGameSocket } from '../websocket/gameSocket'
import type { GameView, MemoryView, PlayerView } from '../types/game'

type ArenaEvent = {
  id: string
  at: string
  kind: string
  meta: string
  title: string
  content: string
}

const route = useRoute()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const view = computed(() => store.view)
const players = computed(() => view.value?.players ?? [])
const alivePlayers = computed(() => players.value.filter(player => player.alive))
const voteTargets = computed(() => alivePlayers.value.filter(player => player.id !== actorId.value))
const selectedActor = computed(() => players.value.find(player => player.id === actorId.value) ?? null)
const leftPlayers = computed(() => players.value.filter((_, index) => index % 2 === 0))
const rightPlayers = computed(() => players.value.filter((_, index) => index % 2 === 1))
const memories = computed(() => view.value?.memories ?? [])
const speeches = computed(() => view.value?.speeches ?? [])
const isGameOver = computed(() => view.value?.status === 'GAME_OVER' || view.value?.phase === 'GAME_OVER')
const victorySummary = computed(() => {
  const gameOverMemory = [...memories.value].reverse().find(memory => memory.eventType === 'GAME_OVER')
  if (!gameOverMemory) return null
  const content = replacePlayerIds(gameOverMemory.content, players.value)
  const winner = content.match(/胜利阵营：([^，,]+)/)?.[1]?.trim() ?? '未知阵营'
  const reason = content.match(/原因：(.+)$/)?.[1]?.trim() ?? content
  return { winner, title: `${campName(winner)}胜利`, reason }
})
const arenaEvents = computed<ArenaEvent[]>(() => {
  const speechEvents = speeches.value.map(speech => ({
    id: `speech-${speech.playerId}-${speech.createdAt}`,
    at: speech.createdAt,
    kind: 'speech',
    meta: `第 ${speech.roundNumber} 轮公开发言`,
    title: playerLabel(players.value, speech.playerId),
    content: speech.content
  }))
  const memoryEvents = memories.value
    .filter(memory => !['SPEECH', 'PRIVATE_VOTE'].includes(memory.eventType))
    .map(memory => ({
      id: `memory-${memory.id}`,
      at: memory.createdAt,
      kind: memoryKind(memory),
      meta: `第 ${memory.roundNumber} 轮 · ${phaseName(memory.phase)}`,
      title: eventTitle(memory),
      content: replacePlayerIds(memory.content, players.value)
    }))
  return [...speechEvents, ...memoryEvents]
    .sort((left, right) => String(left.at).localeCompare(String(right.at)))
    .slice(-160)
})
const phaseHint = computed(() => {
  const phase = view.value?.phase
  if (!phase) return '等待房间状态同步'
  if (phase === 'DAY_SPEECH') return 'Agent 按座位顺序公开发言，后续发言可参考前序公开摘要。'
  if (phase === 'DAY_VOTE') return '投票为私密过程，最终票型和放逐结果会公开。'
  if (phase.includes('NIGHT') || phase.includes('WEREWOLF') || phase.includes('SEER') || phase.includes('WITCH') || phase.includes('GUARD')) {
    return '夜晚行动只进入对应私有视角或阵营共享视角。'
  }
  if (phase === 'GAME_OVER') return '身份和胜负已经结算，可进入结算页或上帝视角复盘。'
  return '系统正在推进阶段，公开事件会在下方持续更新。'
})
const currentSpeechTurn = computed(() => {
  const speechTurnEvents = memories.value
    .filter(memory => memory.eventType === 'SPEECH_TURN_STARTED' || memory.eventType === 'SPEECH_TURN_COMPLETED')
    .sort((left, right) => String(left.createdAt).localeCompare(String(right.createdAt)))
  const latest = speechTurnEvents.at(-1)
  if (!latest || latest.eventType !== 'SPEECH_TURN_STARTED') return ''
  const metadata = parseMetadata(latest)
  const seat = metadata.seatNumber ? `${metadata.seatNumber} 号 ` : ''
  return `${seat}${metadata.playerName ?? 'Agent'}`
})
const actionOptions = computed(() => {
  const role = privateView.value?.ownRole ?? selectedActor.value?.role
  if (role === 'SEER') return [{ value: 'CHECK', label: '查验' }, { value: 'NONE', label: '无行动' }]
  if (role === 'GUARD') return [{ value: 'GUARD', label: '守护' }, { value: 'NONE', label: '无行动' }]
  if (role === 'WITCH') return [{ value: 'SAVE', label: '解药' }, { value: 'POISON', label: '毒药' }, { value: 'NONE', label: '无行动' }]
  if (role?.includes('WOLF')) return [{ value: 'KILL', label: '击杀' }, { value: 'NONE', label: '无行动' }]
  return [{ value: 'NONE', label: '无行动' }]
})
const actorId = ref('')
const privateView = ref<GameView | null>(null)
const speechContent = ref('')
const claimedRole = ref('')
const voteTargetId = ref('')
const voteReason = ref('根据当前发言和票型判断')
const actionType = ref('KILL')
const actionTargetId = ref('')
const refreshing = ref(false)
const advancing = ref(false)
const streamedContentById = ref<Record<string, string>>({})
const streamingEventId = ref<string | null>(null)
const streamQueue: ArenaEvent[] = []
let streamInitialized = false
let streamTimer: number | null = null
let streamRunning = false
let disconnect: null | (() => void) = null

onMounted(async () => {
  await reload()
  actorId.value = alivePlayers.value[0]?.id ?? ''
  disconnect = connectGameSocket(roomId.value, handleSocketMessage)
})
onUnmounted(() => {
  disconnect?.()
  if (streamTimer !== null) window.clearTimeout(streamTimer)
})

watch(actorId, async value => {
  if (!value) {
    privateView.value = null
    return
  }
  try {
    privateView.value = await getPrivateView(roomId.value, value)
  } catch {
    privateView.value = null
  }
  voteTargetId.value = voteTargets.value[0]?.id ?? ''
  actionTargetId.value = voteTargets.value[0]?.id ?? ''
  actionType.value = actionOptions.value[0]?.value ?? 'NONE'
})

watch(actionOptions, options => {
  if (!options.some(option => option.value === actionType.value)) {
    actionType.value = options[0]?.value ?? 'NONE'
  }
})

watch(arenaEvents, events => {
  const nextState: Record<string, string> = {}
  const existingIds = new Set(events.map(event => event.id))
  for (const event of events) {
    const previous = streamedContentById.value[event.id]
    if (previous !== undefined) {
      nextState[event.id] = previous
      continue
    }
    if (!streamInitialized) {
      nextState[event.id] = event.content
      continue
    }
    nextState[event.id] = ''
    streamQueue.push(event)
  }
  streamedContentById.value = nextState
  streamInitialized = true
  while (streamQueue.length && !existingIds.has(streamQueue[0].id)) {
    streamQueue.shift()
  }
  drainStreamQueue()
}, { immediate: true })

async function reload() {
  if (refreshing.value) return
  refreshing.value = true
  try {
    await store.loadPublic(roomId.value)
    store.error = ''
  } catch (error) {
    store.error = error instanceof Error ? error.message : '刷新失败'
  } finally {
    refreshing.value = false
  }
}

function handleSocketMessage(payload?: unknown) {
  if (store.applySocketPayload(payload)) return
  reload()
}

async function advance() {
  if (advancing.value) return
  advancing.value = true
  try {
    await store.auto(roomId.value)
    store.error = ''
  } catch (error) {
    store.error = error instanceof Error ? error.message : '自动推进失败'
  } finally {
    advancing.value = false
  }
}

async function sendSpeech() {
  if (!actorId.value || !speechContent.value) return
  try {
    await submitSpeech(roomId.value, actorId.value, speechContent.value, claimedRole.value || null)
    speechContent.value = ''
    await reload()
    store.error = ''
  } catch (error) {
    store.error = error instanceof Error ? error.message : '提交发言失败'
  }
}

async function sendVote() {
  if (!actorId.value || !voteTargetId.value) return
  try {
    await submitVote(roomId.value, actorId.value, voteTargetId.value, voteReason.value)
    await reload()
    store.error = ''
  } catch (error) {
    store.error = error instanceof Error ? error.message : '提交投票失败'
  }
}

async function sendNightAction() {
  if (!actorId.value) return
  try {
    await submitNightAction(roomId.value, actorId.value, actionType.value, actionTargetId.value || null, null, '玩家手动提交')
    await store.auto(roomId.value)
    store.error = ''
  } catch (error) {
    store.error = error instanceof Error ? error.message : '提交夜间行动失败'
  }
}

function publicRoleText(role?: string | null) {
  return role ? roleName(role) : '身份隐藏'
}

function playerClass(player: PlayerView) {
  return {
    dead: !player.alive,
    active: player.id === actorId.value,
    wolf: player.camp === 'WEREWOLF',
    good: player.camp === 'GOOD'
  }
}

function memoryKind(memory: MemoryView) {
  if (memory.eventType.includes('VOTE')) return 'vote'
  if (memory.eventType.includes('DEATH') || memory.eventType.includes('GAME_OVER')) return 'danger'
  if (memory.eventType.includes('SUMMARY')) return 'summary'
  return 'system'
}

function eventTitle(memory: MemoryView) {
  if (memory.eventType === 'SPEECH_TURN_STARTED') return '发言轮转'
  if (memory.eventType === 'SPEECH_TURN_COMPLETED') return '发言完成'
  if (memory.eventType === 'PUBLIC_SPEECH_SUMMARY') return '公开发言摘要'
  if (memory.eventType === 'VOTE_RESULT') return '投票结果'
  if (memory.eventType === 'GAME_OVER') return '游戏结束'
  return '系统事件'
}

function parseMetadata(memory: MemoryView): Record<string, any> {
  try {
    const parsed = JSON.parse(memory.metadataJson || '{}')
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function renderedEventContent(item: ArenaEvent) {
  return streamedContentById.value[item.id] ?? item.content
}

function isEventStreaming(eventId: string) {
  return streamingEventId.value === eventId
}

function drainStreamQueue() {
  if (streamRunning || !streamQueue.length) return
  const event = streamQueue.shift()
  if (!event) return
  streamRunning = true
  streamingEventId.value = event.id
  const characters = Array.from(event.content)
  const chunkSize = event.kind === 'speech' ? 2 : 4
  let cursor = 0

  const step = () => {
    cursor = Math.min(characters.length, cursor + chunkSize)
    streamedContentById.value = {
      ...streamedContentById.value,
      [event.id]: characters.slice(0, cursor).join('')
    }
    if (cursor < characters.length) {
      streamTimer = window.setTimeout(step, event.kind === 'speech' ? 18 : 12)
      return
    }
    streamRunning = false
    streamingEventId.value = null
    drainStreamQueue()
  }
  step()
}
</script>

<style scoped>
.arena-page {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 0%, rgba(30, 64, 175, 0.24), transparent 30rem),
    linear-gradient(145deg, #06111f, #100b18 58%, #07121e);
}
.arena-shell {
  width: min(1740px, calc(100vw - clamp(20px, 3vw, 56px)));
  height: calc(100vh - clamp(24px, 3vw, 56px));
  min-height: 0;
  margin: 0 auto;
  padding: 0;
  display: grid;
  grid-template-columns: minmax(180px, 260px) minmax(480px, 1fr) minmax(180px, 260px) minmax(320px, 380px);
  grid-template-rows: auto minmax(0, 1fr);
  gap: 14px;
}
.arena-bar {
  grid-column: 1 / -1;
  padding: 14px 16px;
  border: 1px solid rgba(232, 238, 255, 0.12);
  border-radius: 8px;
  background: rgba(5, 8, 18, 0.7);
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.arena-bar div:first-child {
  display: grid;
  gap: 4px;
}
.arena-bar span {
  color: #c4b5fd;
}
.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.player-rail {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 0;
  overflow: auto;
  scrollbar-gutter: stable;
}
.player-chip {
  min-height: 76px;
  display: grid;
  grid-template-columns: 46px 1fr;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid rgba(211, 224, 255, 0.14);
  background: rgba(5, 8, 18, 0.64);
}
.player-chip b {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #f8fbff;
  background: linear-gradient(135deg, #334155, #4338ca);
}
.role-token {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  object-fit: cover;
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid rgba(232, 238, 255, 0.24);
}
.player-chip div {
  min-width: 0;
  display: grid;
  gap: 3px;
}
.player-chip strong,
.player-chip span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.player-chip span {
  color: #aebce0;
  font-size: 12px;
}
.player-chip.active {
  border-color: rgba(253, 230, 138, 0.48);
}
.player-chip.wolf {
  border-color: rgba(248, 113, 113, 0.42);
}
.player-chip.good {
  border-color: rgba(191, 219, 254, 0.36);
}
.player-chip.dead {
  opacity: 0.46;
  filter: grayscale(1);
}
.arena-center,
.control-panel {
  min-height: 0;
  display: grid;
  gap: 12px;
  align-content: stretch;
}
.arena-center {
  grid-template-rows: auto minmax(0, 1fr);
}
.control-panel {
  overflow: auto;
  scrollbar-gutter: stable;
}
.phase-panel,
.panel,
.event-card,
.empty-state {
  border: 1px solid rgba(232, 238, 255, 0.12);
  border-radius: 8px;
  background: rgba(5, 8, 18, 0.66);
}
.phase-panel {
  padding: 16px;
  display: grid;
  gap: 6px;
}
.phase-panel span {
  color: #c4b5fd;
}
.phase-panel strong {
  line-height: 1.6;
}
.phase-panel em {
  color: #fde68a;
  font-style: normal;
}
.event-stream {
  min-height: 0;
  height: 100%;
  max-height: none;
  scrollbar-gutter: stable;
}
.event-stream :deep(.virtual-list) {
  height: 100%;
  max-height: none;
  overscroll-behavior: contain;
}
.event-card {
  margin: 0 0 10px;
  padding: 12px 14px;
  display: grid;
  gap: 6px;
}
.event-card small {
  color: #9ca8c7;
}
.event-card p {
  margin: 0;
  line-height: 1.62;
  color: #e5e7eb;
  white-space: pre-line;
}
.typing-cursor {
  display: inline-block;
  width: 7px;
  height: 1em;
  margin-left: 3px;
  vertical-align: -0.15em;
  background: #fde68a;
  animation: cursor-blink 0.82s steps(2, start) infinite;
}
.event-speech {
  background: rgba(20, 26, 44, 0.84);
}
.event-summary {
  border-color: rgba(253, 230, 138, 0.24);
}
.event-vote {
  border-color: rgba(129, 140, 248, 0.34);
}
.event-danger {
  border-color: rgba(248, 113, 113, 0.38);
}
.empty-state {
  min-height: 260px;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  color: #aebce0;
}
.panel {
  padding: 14px;
  display: grid;
  gap: 10px;
}
.panel h2 {
  margin: 0;
  font-size: 18px;
}
.textarea {
  min-height: 84px;
  padding: 10px 12px;
  resize: vertical;
}
.identity {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.07);
}
.error {
  margin: 0;
  color: #fecaca;
}
.result-panel strong {
  color: #fde68a;
  font-size: 20px;
}
.result-panel p {
  margin: 0;
  color: #cbd5e1;
  line-height: 1.6;
}
@media (max-width: 1180px) {
  .arena-page {
    height: auto;
    min-height: 100vh;
    overflow: auto;
  }
  .arena-shell {
    grid-template-columns: 1fr;
    height: auto;
  }
  .player-rail {
    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  }
  .event-stream {
    height: 520px;
    max-height: 520px;
  }
  .control-panel,
  .player-rail {
    overflow: visible;
  }
}
@keyframes cursor-blink {
  50% {
    opacity: 0;
  }
}
</style>
