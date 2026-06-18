<template>
  <main class="page">
    <section class="shell god-shell">
      <header class="top">
        <div>
          <RouterLink :to="`/rooms/${roomId}/game`">返回游戏桌</RouterLink>
          <h1>上帝视角</h1>
          <p>完整身份、阵营、私有策略摘要与时间线只在这里展示。</p>
        </div>
        <div class="actions">
          <button class="btn ghost" @click="advance">快进到节点</button>
          <button class="btn" @click="simulate">模拟到结束</button>
        </div>
      </header>

      <nav class="note-tabs" aria-label="上帝视角内容">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="note-tab"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <strong>{{ tab.label }}</strong>
          <span>{{ tab.description }}</span>
        </button>
      </nav>

      <section v-if="activeTab === 'timeline'" class="glass panel note-panel">
        <h2>完整时间线</h2>
        <VirtualList v-if="memories.length" :items="memories" :item-height="92">
          <template #default="{ item: memory }">
            <p class="timeline-item">
              <strong>[{{ memory.scope }}] {{ phaseName(memory.phase) }}</strong>
              <span>{{ formatText(memory.content) }}</span>
            </p>
          </template>
        </VirtualList>
        <p v-else class="empty">暂无时间线</p>
      </section>

      <section v-else-if="activeTab === 'decisions'" class="glass panel note-panel">
        <div class="panel-head">
          <h2>Agent 决策链</h2>
          <button class="btn secondary" @click="loadDiagnostics">刷新决策链</button>
        </div>
        <div class="decision-grid" v-if="decisionChain.length">
          <article v-for="item in decisionChain" :key="item.id" class="decision-card">
            <strong>{{ item.title }}</strong>
            <span>{{ item.summary }}</span>
            <em v-if="item.reason">{{ item.reason }}</em>
            <small>{{ item.meta }}</small>
            <details class="snapshot">
              <summary>查看该 Agent 当时可见的信息</summary>
              <pre>{{ item.inputSnapshot }}</pre>
            </details>
          </article>
        </div>
        <p v-else class="empty">暂无 Agent 决策记录</p>
      </section>

      <section v-else-if="activeTab === 'diagnostics'" class="note-panel">
        <section class="metrics-grid">
          <article class="glass metric-card">
            <span>Agent 平均耗时</span>
            <strong>{{ diagnosticsSummary.averageRunLatency }}ms</strong>
            <em>{{ diagnosticsSummary.runCount }} 次决策</em>
          </article>
          <article class="glass metric-card">
            <span>Fallback 比例</span>
            <strong>{{ diagnosticsSummary.fallbackRate }}%</strong>
            <em>{{ diagnosticsSummary.fallbackCount }} 次 fallback</em>
          </article>
          <article class="glass metric-card">
            <span>任务成功率</span>
            <strong>{{ diagnosticsSummary.taskSuccessRate }}%</strong>
            <em>{{ diagnosticsSummary.taskCount }} 个任务</em>
          </article>
        </section>
        <section class="inspector">
          <article class="glass panel">
          <div class="panel-head">
            <h2>Agent Tasks</h2>
            <button class="btn secondary" @click="loadDiagnostics">加载诊断</button>
          </div>
          <div class="diagnostic-list">
            <VirtualList v-if="tasks.length" :items="tasks" :item-height="88" key-field="taskId">
              <template #default="{ item: task }">
                <p class="timeline-item">
                  <strong>{{ task.status }}</strong>
                  <span>{{ task.purpose }} / {{ phaseName(task.phase) }} / {{ task.latencyMillis }}ms</span>
                  <em>{{ playerLabel(view?.players ?? [], task.playerId) }}</em>
                </p>
              </template>
            </VirtualList>
            <p v-if="!tasks.length" class="empty">点击加载诊断后显示最近任务</p>
          </div>
        </article>
        <article class="glass panel">
          <div class="panel-head">
            <h2>Agent Runs</h2>
            <button class="btn secondary" @click="loadDiagnostics">刷新</button>
          </div>
          <div class="diagnostic-list">
            <VirtualList v-if="runs.length" :items="runs" :item-height="88">
              <template #default="{ item: run }">
                <p class="timeline-item">
                  <strong>{{ run.status }}</strong>
                  <span>{{ run.purpose }} / {{ run.modelProvider }} / {{ run.latencyMillis }}ms</span>
                  <em v-if="run.errorMessage">{{ run.errorMessage }}</em>
                </p>
              </template>
            </VirtualList>
            <p v-if="!runs.length" class="empty">点击加载诊断后显示最近运行记录</p>
          </div>
        </article>
        </section>
      </section>

      <section v-else-if="activeTab === 'replay'" class="glass panel note-panel">
        <div class="panel-head">
          <h2>事件回放</h2>
          <button class="btn secondary" @click="loadReplay">加载回放</button>
        </div>
        <div class="diagnostic-list">
            <VirtualList v-if="replay.length" :items="replay" :item-height="92">
              <template #default="{ item: event }">
                <p class="timeline-item">
                  <strong>[{{ event.scope }}] {{ event.eventType }}</strong>
                  <span>第 {{ event.roundNumber }} 轮 / {{ phaseName(event.phase) }}</span>
                  <em>{{ formatText(payloadText(event.payloadJson)) }}</em>
                </p>
              </template>
            </VirtualList>
            <p v-if="!replay.length" class="empty">点击加载回放后显示事件流</p>
        </div>
      </section>

      <section v-else class="glass table note-panel">
        <article v-for="player in view?.players ?? []" :key="player.id" class="row" :class="{ dead: !player.alive }">
          <img v-if="roleAsset(player.role)" :src="roleAsset(player.role) ?? ''" :alt="roleName(player.role)" />
          <span>{{ player.seatNumber }} 号</span>
          <strong>{{ player.name }}</strong>
          <b>{{ roleName(player.role) }}</b>
          <em>{{ campName(player.camp) }}</em>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAgentRuns, getAgentTasks, getGodReplay } from '../api/client'
import { roleAsset } from '../assets'
import VirtualList from '../components/VirtualList.vue'
import { campName, phaseName, playerLabel, replacePlayerIds, roleName } from '../game/gameLabels'
import { useGameStore } from '../stores/game'
import type { AgentRun, AgentTask, GameEvent } from '../types/game'

type DecisionChainItem = {
  id: string
  title: string
  summary: string
  reason: string
  meta: string
  inputSnapshot: string
}
type TabId = 'timeline' | 'decisions' | 'diagnostics' | 'replay' | 'players'

const route = useRoute()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const view = computed(() => store.view)
const runs = ref<AgentRun[]>([])
const tasks = ref<AgentTask[]>([])
const replay = ref<GameEvent[]>([])
const activeTab = ref<TabId>('timeline')
const memories = computed(() => view.value?.memories ?? [])
const decisionChain = computed<DecisionChainItem[]>(() => [...runs.value]
  .sort((left, right) => String(left.createdAt).localeCompare(String(right.createdAt)))
  .map(toDecisionChainItem))
const tabs = computed<Array<{ id: TabId; label: string; description: string }>>(() => [
  { id: 'timeline', label: '完整时间线', description: `${memories.value.length} 条事件` },
  { id: 'decisions', label: 'Agent 决策链', description: `${decisionChain.value.length} 条决策` },
  { id: 'diagnostics', label: '运行诊断', description: `${tasks.value.length} 个任务` },
  { id: 'replay', label: '事件回放', description: replay.value.length ? `${replay.value.length} 条回放` : '按需加载' },
  { id: 'players', label: '身份表', description: `${view.value?.players.length ?? 0} 名玩家` }
])
const diagnosticsSummary = computed(() => {
  const runCount = runs.value.length
  const fallbackCount = runs.value.filter(run => run.fallbackUsed).length
  const taskCount = tasks.value.length
  const succeededTasks = tasks.value.filter(task => task.status === 'SUCCEEDED').length
  return {
    runCount,
    taskCount,
    fallbackCount,
    averageRunLatency: average(runs.value.map(run => run.latencyMillis)),
    fallbackRate: percent(fallbackCount, runCount),
    taskSuccessRate: percent(succeededTasks, taskCount)
  }
})

onMounted(async () => {
  await store.loadGod(roomId.value)
  await loadDiagnostics()
})

async function advance() {
  await store.auto(roomId.value)
  await store.loadGod(roomId.value)
}

async function simulate() {
  await store.simulate(roomId.value)
  await store.loadGod(roomId.value)
}

async function loadDiagnostics() {
  const [runData, taskData] = await Promise.all([
    getAgentRuns(roomId.value),
    getAgentTasks(roomId.value)
  ])
  runs.value = runData
  tasks.value = taskData
}

async function loadReplay() {
  const replayData = await getGodReplay(roomId.value)
  replay.value = replayData
}

function payloadText(payloadJson: string) {
  try {
    const payload = JSON.parse(payloadJson)
    return payload.content ?? payloadJson
  } catch {
    return payloadJson
  }
}

function formatText(content: string) {
  return replacePlayerIds(content, view.value?.players ?? [])
}

function toDecisionChainItem(run: AgentRun): DecisionChainItem {
  const parsed = parseJsonObject(run.parsedOutputJson)
  const actor = playerLabel(view.value?.players ?? [], run.playerId)
  const purpose = purposeLabel(run.purpose)
  const summary = decisionSummary(run.purpose, parsed)
  const reason = decisionReason(run.purpose, parsed)
  return {
    id: run.id,
    title: `${actor} · ${purpose}`,
    summary: formatText(summary),
    reason: reason ? formatText(reason) : '',
    meta: `${phaseName(run.phase)} · 第 ${run.roundNumber} 轮 · ${run.modelProvider}/${run.modelName ?? 'mock'} · ${run.latencyMillis}ms · ${run.status}${run.fallbackUsed ? ' · fallback' : ''}`,
    inputSnapshot: compactInputSnapshot(run.inputViewSnapshotJson)
  }
}

function parseJsonObject(value: string | null) {
  if (!value) return {}
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed as Record<string, any> : {}
  } catch {
    return {}
  }
}

function decisionSummary(purpose: string, parsed: Record<string, any>) {
  if (purpose === 'SPEECH') {
    const speech = String(parsed.speech ?? '未生成公开发言')
    const claimedRole = parsed.claimedRole ? `；声称身份：${parsed.claimedRole}` : ''
    const strategy = parsed.strategySummary ? `；策略摘要：${parsed.strategySummary}` : ''
    return `公开发言：${speech}${claimedRole}${strategy}`
  }
  if (purpose === 'VOTE') {
    return `投票目标：${playerRef(parsed.targetPlayerId)}；置信度：${parsed.confidence ?? '未提供'}`
  }
  if (purpose === 'NIGHT_ACTION') {
    const secondary = parsed.secondaryTargetPlayerId ? `；第二目标：${playerRef(parsed.secondaryTargetPlayerId)}` : ''
    return `夜间行动：${parsed.actionType ?? 'NONE'}；目标：${playerRef(parsed.targetPlayerId)}${secondary}`
  }
  if (purpose === 'DAY_SKILL') {
    return `白天技能：${parsed.actionType ?? 'NONE'}；目标：${playerRef(parsed.targetPlayerId)}`
  }
  return '未识别的 Agent 决策输出'
}

function decisionReason(purpose: string, parsed: Record<string, any>) {
  if (purpose === 'SPEECH') {
    const suspicions = Array.isArray(parsed.suspicions) ? parsed.suspicions : []
    if (!suspicions.length) return ''
    return `怀疑链：${suspicions.map((item: any) => `${playerRef(item.playerId)}：${item.reason ?? '无理由'}`).join('；')}`
  }
  const reason = parsed.reason
  return typeof reason === 'string' ? `理由：${reason}` : ''
}

function purposeLabel(purpose: string) {
  const labels: Record<string, string> = {
    SPEECH: '公开发言',
    VOTE: '私密投票',
    NIGHT_ACTION: '夜间行动',
    DAY_SKILL: '白天技能'
  }
  return labels[purpose] ?? purpose
}

function compactInputSnapshot(snapshotJson: string) {
  const snapshot = parseJsonObject(snapshotJson)
  const compact = {
    viewer: playerRef(snapshot.viewerPlayerId),
    ownRole: snapshot.ownRole ?? null,
    ownCamp: snapshot.ownCamp ?? null,
    phase: snapshot.phase ?? null,
    roundNumber: snapshot.roundNumber ?? null,
    players: Array.isArray(snapshot.players)
      ? snapshot.players.map((player: any) => ({
          seatNumber: player.seatNumber,
          name: player.name,
          alive: player.alive,
          role: player.role,
          camp: player.camp
        }))
      : [],
    memories: Array.isArray(snapshot.memories)
      ? snapshot.memories.slice(-12).map((memory: any) => ({
          scope: memory.scope,
          eventType: memory.eventType,
          content: formatText(String(memory.content ?? ''))
        }))
      : [],
    speeches: Array.isArray(snapshot.speeches)
      ? snapshot.speeches.slice(-8).map((speech: any) => ({
          player: playerRef(speech.playerId),
          content: formatText(String(speech.content ?? ''))
        }))
      : []
  }
  return JSON.stringify(compact, null, 2)
}

function playerRef(playerId: unknown) {
  if (typeof playerId !== 'string' || !playerId) {
    return '无'
  }
  return playerLabel(view.value?.players ?? [], playerId)
}

function average(values: number[]) {
  if (!values.length) return 0
  return Math.round(values.reduce((sum, value) => sum + Math.max(0, value), 0) / values.length)
}

function percent(value: number, total: number) {
  if (!total) return 0
  return Math.round((value / total) * 100)
}
</script>

<style scoped>
.top {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 18px;
}
.top p {
  color: #bdc8e8;
}
.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.god-shell {
  display: grid;
  gap: 18px;
}
.note-tabs {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}
.note-tab {
  min-height: 82px;
  padding: 14px;
  border-radius: 8px 8px 3px 3px;
  border: 1px solid rgba(232, 238, 255, 0.14);
  background: rgba(10, 16, 32, 0.7);
  color: #e5e7eb;
  display: grid;
  gap: 6px;
  text-align: left;
  cursor: pointer;
}
.note-tab strong {
  font-size: 16px;
}
.note-tab span {
  color: #9fb0d8;
}
.note-tab.active {
  border-color: rgba(253, 230, 138, 0.52);
  background: linear-gradient(180deg, rgba(67, 56, 202, 0.48), rgba(10, 16, 32, 0.82));
  box-shadow: inset 0 3px 0 #fde68a;
}
.note-panel {
  min-height: min(680px, calc(100vh - 250px));
}
.note-panel :deep(.virtual-list) {
  max-height: min(680px, calc(100vh - 320px));
}
.table,
.panel {
  padding: 16px;
  border-radius: 8px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.panel-head h2 {
  margin: 0;
}
.diagnostic-list {
  display: grid;
  gap: 10px;
}
.decision-grid {
  display: grid;
  gap: 12px;
  max-height: min(680px, calc(100vh - 320px));
  overflow: auto;
  padding-right: 6px;
}
.decision-card {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid rgba(232, 238, 255, 0.1);
  background: rgba(20, 26, 44, 0.82);
}
.decision-card span,
.decision-card em,
.decision-card small {
  color: #cbd5e1;
  font-style: normal;
  white-space: pre-line;
}
.decision-card small {
  color: #96a1bd;
}
.inspector {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.metric-card {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-radius: 8px;
}
.metric-card span,
.metric-card em {
  color: #aebce0;
  font-style: normal;
}
.metric-card strong {
  color: #fde68a;
  font-size: 26px;
}
.row {
  display: grid;
  grid-template-columns: 42px 70px 1fr 150px 120px;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.row img {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  object-fit: cover;
  background: rgba(255, 255, 255, 0.08);
}
.row.dead {
  opacity: 0.45;
}
.timeline-item {
  display: grid;
  gap: 6px;
}
.timeline-item span,
.timeline-item em,
.timeline-item small {
  color: #cbd5e1;
  font-style: normal;
  white-space: pre-line;
}
.timeline-item small {
  color: #96a1bd;
}
.decision-item {
  padding: 10px 0;
}
.snapshot {
  margin-top: 6px;
}
.snapshot summary {
  cursor: pointer;
  color: #fde68a;
}
.snapshot pre {
  max-height: 300px;
  overflow: auto;
  padding: 12px;
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.72);
  color: #dbeafe;
  white-space: pre-wrap;
}
@media (max-width: 720px) {
  .top {
    align-items: start;
    flex-direction: column;
  }
  .note-tabs {
    grid-template-columns: 1fr;
  }
  .row {
    grid-template-columns: 1fr;
  }
  .inspector {
    grid-template-columns: 1fr;
  }
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
