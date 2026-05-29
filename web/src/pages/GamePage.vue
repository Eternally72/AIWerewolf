<template>
  <main class="page">
    <section class="shell layout">
      <header class="bar glass">
        <div>
          <strong>{{ view?.roomName ?? 'AI Werewolf' }}</strong>
          <span>{{ view?.phase ?? 'WAITING' }}</span>
        </div>
        <div class="actions">
          <button class="btn secondary" @click="reload">刷新</button>
          <button class="btn secondary" @click="advance">自动推进</button>
          <RouterLink class="btn" :to="`/rooms/${roomId}/god`">上帝视角</RouterLink>
        </div>
      </header>

      <section class="seat-ring glass">
        <article
          v-for="(player, index) in view?.players ?? []"
          :key="player.id"
          class="seat"
          :class="{ dead: !player.alive }"
          :style="seatStyle(index, view?.players.length ?? 1)"
        >
          <div class="avatar">{{ player.seatNumber }}</div>
          <strong>{{ player.name }}</strong>
          <span>{{ player.type }}</span>
          <em v-if="player.role">{{ player.role }}</em>
        </article>
      </section>

      <aside class="side">
        <section class="glass panel">
          <h2>公开发言</h2>
          <div class="timeline">
            <p v-for="speech in view?.speeches ?? []" :key="speech.createdAt" class="timeline-item">{{ speech.content }}</p>
          </div>
        </section>
        <section class="glass panel">
          <h2>时间线</h2>
          <div class="timeline">
            <p v-for="memory in view?.memories ?? []" :key="memory.id" class="timeline-item">{{ memory.content }}</p>
          </div>
        </section>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, type CSSProperties } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '../stores/game'
import { connectGameSocket } from '../websocket/gameSocket'

const route = useRoute()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const view = computed(() => store.view)
let disconnect: null | (() => void) = null

onMounted(async () => {
  await reload()
  disconnect = connectGameSocket(roomId.value, reload)
})
onUnmounted(() => disconnect?.())

async function reload() {
  await store.loadPublic(roomId.value)
}

async function advance() {
  await store.auto(roomId.value)
}

function seatStyle(index: number, total: number): CSSProperties {
  const angle = (Math.PI * 2 * index) / total - Math.PI / 2
  const x = 50 + Math.cos(angle) * 36
  const y = 50 + Math.sin(angle) * 38
  return { position: 'absolute', left: `${x}%`, top: `${y}%`, transform: 'translate(-50%, -50%)' }
}
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
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
}
.panel {
  padding: 16px;
  border-radius: 8px;
}
h2 {
  margin-top: 0;
  font-size: 18px;
}
@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
