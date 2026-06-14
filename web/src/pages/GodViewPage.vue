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
          <b>{{ player.role }}</b>
          <em>{{ player.camp }}</em>
        </article>
      </section>
      <section class="glass panel">
        <h2>完整时间线</h2>
        <p v-for="memory in view?.memories ?? []" :key="memory.id" class="timeline-item">[{{ memory.scope }}] {{ memory.content }}</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useGameStore } from '../stores/game'

const route = useRoute()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const view = computed(() => store.view)

onMounted(() => store.loadGod(roomId.value))

async function advance() {
  await store.auto(roomId.value)
  await store.loadGod(roomId.value)
}

async function simulate() {
  await store.simulate(roomId.value)
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
@media (max-width: 720px) {
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
