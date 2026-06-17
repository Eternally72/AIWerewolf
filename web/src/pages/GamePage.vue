<template>
  <main
    class="page game-page"
    :class="{ 'game-over': isGameOver }"
    :style="{ '--table-bg': `url(${backgroundAssets.table})`, '--victory-bg': `url(${backgroundAssets.victory})` }"
  >
    <section class="shell game-shell layout">
      <header class="bar glass">
        <div>
          <strong>{{ view?.roomName ?? 'AI Werewolf' }}</strong>
          <span>
            {{ phaseName(view?.phase) }} · 第 {{ view?.roundNumber ?? 0 }} 轮 · {{ statusText(view?.status) }}
            <template v-if="victorySummary"> · {{ victorySummary.title }}</template>
          </span>
        </div>
        <div class="actions">
          <button class="btn secondary" @click="reload">刷新</button>
          <button class="btn secondary" @click="advance">自动推进</button>
          <RouterLink class="btn" :to="`/rooms/${roomId}/god`">上帝视角</RouterLink>
          <RouterLink v-if="isGameOver" class="btn secondary" :to="`/rooms/${roomId}/over`">结算页</RouterLink>
        </div>
      </header>

      <GameTableCanvas
        :view="view"
        :victory-title="victorySummary?.title"
        :victory-reason="victorySummary?.reason"
        :animations="store.animations"
      />

      <aside class="side">
        <section v-if="!isGameOver" class="glass panel action-panel">
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

          <div class="mini-form">
            <textarea v-model="speechContent" class="input textarea" placeholder="发言内容" />
            <input v-model="claimedRole" class="input" placeholder="声称身份，可空" />
            <button class="btn secondary" :disabled="!actorId || !speechContent" @click="sendSpeech">提交发言</button>
          </div>

          <div class="mini-form">
            <select v-model="voteTargetId" class="input">
              <option value="">投票目标</option>
              <option v-for="player in voteTargets" :key="player.id" :value="player.id">
                {{ player.seatNumber }} 号 {{ player.name }}
              </option>
            </select>
            <input v-model="voteReason" class="input" placeholder="投票理由" />
            <button class="btn secondary" :disabled="!actorId || !voteTargetId" @click="sendVote">提交投票</button>
          </div>

          <div class="mini-form">
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
          </div>
          <p v-if="store.error" class="error">{{ store.error }}</p>
        </section>
        <section v-else class="glass panel result-panel">
          <h2>本局结算</h2>
          <strong>{{ victorySummary?.title ?? '游戏结束' }}</strong>
          <p>{{ victorySummary?.reason ?? '胜负结果已写入时间线。' }}</p>
          <RouterLink class="btn" :to="`/rooms/${roomId}/over`">查看结算页</RouterLink>
        </section>
        <section class="glass panel">
          <h2>公开发言</h2>
          <div class="timeline">
            <article v-for="speech in visibleSpeeches" :key="`${speech.playerId}-${speech.createdAt}`" class="timeline-item speech-item">
              <strong>{{ playerLabel(view?.players ?? [], speech.playerId) }}</strong>
              <span>{{ speech.content }}</span>
            </article>
            <p v-if="!view?.speeches.length" class="empty">还没有公开发言</p>
          </div>
        </section>
        <section class="glass panel">
          <h2>时间线</h2>
          <div class="timeline">
            <p v-for="memory in visibleMemories" :key="memory.id" class="timeline-item">
              <span class="memory-meta">第 {{ memory.roundNumber }} 轮 · {{ phaseName(memory.phase) }}</span>
              {{ replacePlayerIds(memory.content, view?.players ?? []) }}
            </p>
            <p v-if="!view?.memories.length" class="empty">暂无公开时间线</p>
          </div>
        </section>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPrivateView, submitNightAction, submitSpeech, submitVote } from '../api/client'
import GameTableCanvas from '../components/GameTableCanvas.vue'
import { backgroundAssets } from '../assets'
import { campName, phaseName, playerLabel, replacePlayerIds, roleName, statusText } from '../game/gameLabels'
import { useGameStore } from '../stores/game'
import { connectGameSocket } from '../websocket/gameSocket'
import type { GameView } from '../types/game'

const route = useRoute()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const view = computed(() => store.view)
const alivePlayers = computed(() => view.value?.players.filter(player => player.alive) ?? [])
const voteTargets = computed(() => alivePlayers.value.filter(player => player.id !== actorId.value))
const selectedActor = computed(() => view.value?.players.find(player => player.id === actorId.value) ?? null)
const visibleSpeeches = computed(() => (view.value?.speeches ?? []).slice(-30))
const visibleMemories = computed(() => (view.value?.memories ?? []).slice(-80))
const isGameOver = computed(() => view.value?.status === 'GAME_OVER' || view.value?.phase === 'GAME_OVER')
const victorySummary = computed(() => {
  const gameOverMemory = [...(view.value?.memories ?? [])].reverse().find(memory => memory.eventType === 'GAME_OVER')
  if (!gameOverMemory) return null
  const content = replacePlayerIds(gameOverMemory.content, view.value?.players ?? [])
  const winner = content.match(/胜利阵营：([^，,]+)/)?.[1]?.trim() ?? '未知阵营'
  const reason = content.match(/原因：(.+)$/)?.[1]?.trim() ?? content
  return {
    winner,
    title: `${campName(winner)}胜利`,
    reason
  }
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
let disconnect: null | (() => void) = null

onMounted(async () => {
  await reload()
  actorId.value = alivePlayers.value[0]?.id ?? ''
  disconnect = connectGameSocket(roomId.value, handleSocketMessage)
})
onUnmounted(() => disconnect?.())

watch(actorId, async value => {
  privateView.value = value ? await getPrivateView(roomId.value, value) : null
  voteTargetId.value = voteTargets.value[0]?.id ?? ''
  actionTargetId.value = voteTargets.value[0]?.id ?? ''
  actionType.value = actionOptions.value[0]?.value ?? 'NONE'
})

watch(actionOptions, options => {
  if (!options.some(option => option.value === actionType.value)) {
    actionType.value = options[0]?.value ?? 'NONE'
  }
})

async function reload() {
  await store.loadPublic(roomId.value)
}

function handleSocketMessage(payload?: unknown) {
  if (store.applySocketPayload(payload)) {
    return
  }
  reload()
}

async function advance() {
  await store.auto(roomId.value)
}

async function sendSpeech() {
  if (!actorId.value || !speechContent.value) return
  try {
    await submitSpeech(roomId.value, actorId.value, speechContent.value, claimedRole.value || null)
    speechContent.value = ''
    await reload()
  } catch (error) {
    store.error = error instanceof Error ? error.message : '提交发言失败'
  }
}

async function sendVote() {
  if (!actorId.value || !voteTargetId.value) return
  try {
    await submitVote(roomId.value, actorId.value, voteTargetId.value, voteReason.value)
    await reload()
  } catch (error) {
    store.error = error instanceof Error ? error.message : '提交投票失败'
  }
}

async function sendNightAction() {
  if (!actorId.value) return
  try {
    await submitNightAction(roomId.value, actorId.value, actionType.value, actionTargetId.value || null, null, '玩家手动提交')
    await reload()
  } catch (error) {
    store.error = error instanceof Error ? error.message : '提交夜间行动失败'
  }
}

</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(720px, 1fr) clamp(340px, 22vw, 440px);
  gap: clamp(14px, 1.2vw, 24px);
  align-items: start;
}
.game-shell {
  width: min(1760px, calc(100vw - clamp(24px, 4vw, 72px)));
  max-width: none;
}
.game-page {
  background:
    linear-gradient(rgba(4, 7, 16, 0.5), rgba(4, 7, 16, 0.72)),
    var(--table-bg),
    radial-gradient(circle at 50% 45%, rgba(136, 92, 35, 0.42), transparent 18rem),
    linear-gradient(145deg, #08111f, #1a1024);
  background-size: cover;
  background-position: center;
}
.game-page.game-over {
  background:
    linear-gradient(rgba(4, 7, 16, 0.46), rgba(4, 7, 16, 0.76)),
    var(--victory-bg),
    var(--table-bg),
    linear-gradient(145deg, #08111f, #1a1024);
  background-size: cover;
  background-position: center;
}
.bar {
  grid-column: 1 / -1;
  padding: 14px 16px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}
.bar div:first-child {
  display: grid;
  gap: 4px;
}
.bar span {
  color: #c4b5fd;
}
.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.side {
  display: grid;
  gap: 18px;
  max-height: calc(100vh - 104px);
  overflow: auto;
  padding-right: 2px;
}
.panel {
  padding: 16px;
  border-radius: 8px;
}
.action-panel {
  gap: 12px;
  display: grid;
}
.mini-form {
  display: grid;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
.textarea {
  min-height: 82px;
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
.result-panel {
  display: grid;
  gap: 10px;
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
h2 {
  margin-top: 0;
  font-size: 18px;
}
.speech-item {
  display: grid;
  gap: 6px;
}
.speech-item strong {
  color: #dbeafe;
  font-size: 13px;
}
.speech-item span {
  line-height: 1.55;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
  .game-shell {
    width: min(100%, calc(100vw - 24px));
  }
  .side {
    max-height: none;
    overflow: visible;
  }
}
</style>
