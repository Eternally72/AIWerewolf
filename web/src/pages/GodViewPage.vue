<template>
  <main class="page">
    <section class="shell grid">
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
      <section class="glass table">
        <article v-for="player in view?.players ?? []" :key="player.id" class="row" :class="{ dead: !player.alive }">
          <span>{{ player.seatNumber }} 号</span>
          <strong>{{ player.name }}</strong>
          <b>{{ roleName(player.role) }}</b>
          <em>{{ campName(player.camp) }}</em>
        </article>
      </section>
      <section class="glass panel">
        <h2>完整时间线</h2>
        <p v-for="memory in view?.memories ?? []" :key="memory.id" class="timeline-item">
          <strong>[{{ memory.scope }}] {{ phaseName(memory.phase) }}</strong>
          <span>{{ formatText(memory.content) }}</span>
        </p>
      </section>
      <section class="inspector">
        <article class="glass panel">
          <div class="panel-head">
            <h2>Agent Tasks</h2>
            <button class="btn secondary" @click="loadDiagnostics">加载诊断</button>
          </div>
          <div class="timeline">
            <p v-for="task in visibleTasks" :key="task.taskId" class="timeline-item">
              <strong>{{ task.status }}</strong>
              <span>{{ task.purpose }} / {{ phaseName(task.phase) }} / {{ task.latencyMillis }}ms</span>
              <em>{{ playerLabel(view?.players ?? [], task.playerId) }}</em>
            </p>
            <p v-if="!tasks.length" class="empty">点击加载诊断后显示最近任务</p>
          </div>
        </article>
        <article class="glass panel">
          <div class="panel-head">
            <h2>Agent Runs</h2>
            <button class="btn secondary" @click="loadDiagnostics">刷新</button>
          </div>
          <div class="timeline">
            <p v-for="run in visibleRuns" :key="run.id" class="timeline-item">
              <strong>{{ run.status }}</strong>
              <span>{{ run.purpose }} / {{ run.modelProvider }} / {{ run.latencyMillis }}ms</span>
              <em v-if="run.errorMessage">{{ run.errorMessage }}</em>
            </p>
            <p v-if="!runs.length" class="empty">点击加载诊断后显示最近运行记录</p>
          </div>
        </article>
      </section>
      <section class="glass panel">
        <div class="panel-head">
          <h2>事件回放</h2>
          <button class="btn secondary" @click="loadReplay">加载回放</button>
        </div>
        <div class="timeline">
            <p v-for="event in visibleReplay" :key="event.id" class="timeline-item">
              <strong>[{{ event.scope }}] {{ event.eventType }}</strong>
              <span>第 {{ event.roundNumber }} 轮 / {{ phaseName(event.phase) }}</span>
              <em>{{ formatText(payloadText(event.payloadJson)) }}</em>
            </p>
            <p v-if="!replay.length" class="empty">点击加载回放后显示事件流</p>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAgentRuns, getAgentTasks, getGodReplay } from '../api/client'
import { campName, phaseName, playerLabel, replacePlayerIds, roleName } from '../game/gameLabels'
import { useGameStore } from '../stores/game'
import type { AgentRun, AgentTask, GameEvent } from '../types/game'

const route = useRoute()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const view = computed(() => store.view)
const runs = ref<AgentRun[]>([])
const tasks = ref<AgentTask[]>([])
const replay = ref<GameEvent[]>([])
const visibleRuns = computed(() => runs.value.slice(0, 30))
const visibleTasks = computed(() => tasks.value.slice(0, 30))
const visibleReplay = computed(() => replay.value.slice(-80))

onMounted(() => store.loadGod(roomId.value))

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
.inspector {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}
.row {
  display: grid;
  grid-template-columns: 70px 1fr 150px 120px;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.row.dead {
  opacity: 0.45;
}
.timeline-item {
  display: grid;
  gap: 6px;
}
.timeline-item span,
.timeline-item em {
  color: #cbd5e1;
  font-style: normal;
}
@media (max-width: 720px) {
  .row {
    grid-template-columns: 1fr;
  }
  .inspector {
    grid-template-columns: 1fr;
  }
}
</style>
