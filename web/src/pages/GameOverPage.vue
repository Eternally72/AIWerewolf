<template>
  <main class="page over-page">
    <section class="shell over-layout">
      <header class="over-hero">
        <RouterLink :to="`/rooms/${roomId}/game`">返回游戏桌</RouterLink>
        <h1>{{ victorySummary?.title ?? '游戏结算' }}</h1>
        <p>{{ victorySummary?.reason ?? '正在读取本局结算信息。' }}</p>
        <div class="actions">
          <RouterLink class="btn" to="/create">重新开始</RouterLink>
          <RouterLink class="btn secondary" :to="`/rooms/${roomId}/god`">上帝视角</RouterLink>
        </div>
      </header>

      <section class="glass panel">
        <h2>身份揭示</h2>
        <div class="players">
          <article v-for="player in view?.players ?? []" :key="player.id" class="player" :class="campClass(player.camp, '')">
            <b class="seat-number">{{ player.seatNumber }}</b>
            <strong>{{ player.seatNumber }} 号 {{ player.name }}</strong>
            <span>{{ roleName(player.role) }} · {{ campName(player.camp) }}</span>
            <em>{{ player.alive ? '存活' : '死亡' }}</em>
          </article>
        </div>
      </section>

      <section class="glass panel">
        <h2>关键时间线</h2>
        <div class="timeline">
          <p v-for="memory in keyMemories" :key="memory.id" class="timeline-item">
            <span class="memory-meta">第 {{ memory.roundNumber }} 轮 · {{ phaseName(memory.phase) }}</span>
            {{ replacePlayerIds(memory.content, view?.players ?? []) }}
          </p>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicView } from '../api/client'
import { campClass, campName, phaseName, replacePlayerIds, roleName } from '../game/gameLabels'
import type { GameView } from '../types/game'

const route = useRoute()
const roomId = computed(() => String(route.params.roomId))
const view = ref<GameView | null>(null)
const keyMemories = computed(() => (view.value?.memories ?? []).filter(memory =>
  ['GAME_OVER', 'VOTE_RESULT', 'PLAYER_DEAD', 'PHASE_CHANGED', 'PUBLIC_SPEECH_SUMMARY'].includes(memory.eventType)
).slice(-30))
const victorySummary = computed(() => {
  const gameOverMemory = [...(view.value?.memories ?? [])].reverse().find(memory => memory.eventType === 'GAME_OVER')
  if (!gameOverMemory) return null
  const content = replacePlayerIds(gameOverMemory.content, view.value?.players ?? [])
  const winner = content.match(/胜利阵营：([^，,]+)/)?.[1]?.trim() ?? '未知阵营'
  const reason = content.match(/原因：(.+)$/)?.[1]?.trim() ?? content
  return { title: `${campName(winner)}胜利`, reason }
})

onMounted(async () => {
  view.value = await getPublicView(roomId.value)
})
</script>

<style scoped>
.over-page {
  background: #080b14;
}
.over-layout {
  display: grid;
  gap: 18px;
  width: min(1280px, 100%);
}
.over-hero {
  min-height: 34vh;
  display: grid;
  align-content: end;
  gap: 12px;
  padding-bottom: 18px;
}
.over-hero h1 {
  margin: 0;
  font-size: clamp(42px, 7vw, 92px);
  line-height: 0.95;
}
.over-hero p {
  max-width: 820px;
  color: #dbeafe;
  font-size: 20px;
  line-height: 1.6;
}
.panel {
  padding: 18px;
  border-radius: 8px;
}
.players {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}
.player {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid rgba(232, 238, 255, 0.14);
  background: rgba(6, 9, 18, 0.62);
}
.seat-number {
  width: 64px;
  height: 64px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #1f2937;
  color: #f8fafc;
  font-size: 24px;
}
.player span,
.player em {
  color: #cbd5e1;
  font-style: normal;
}
.player.wolf {
  border-color: rgba(248, 113, 113, 0.34);
}
.player.good {
  border-color: rgba(191, 219, 254, 0.32);
}
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
