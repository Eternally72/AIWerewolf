<template>
  <main class="page">
    <section class="shell grid">
      <header class="top">
        <div>
          <RouterLink to="/create">新房间</RouterLink>
          <h1>游戏大厅</h1>
          <p>{{ roomId }}</p>
        </div>
        <div class="actions">
          <button class="btn secondary" @click="copy">复制房间号</button>
          <button class="btn" @click="start">开始游戏</button>
        </div>
      </header>
      <section class="glass seats">
        <article v-for="seat in seats" :key="seat" class="seat">
          <div class="avatar">{{ seat }}</div>
          <strong>座位 {{ seat }}</strong>
          <span>{{ seat === 1 ? '主持 / AI' : '待分配' }}</span>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/game'

const route = useRoute()
const router = useRouter()
const store = useGameStore()
const roomId = computed(() => String(route.params.roomId))
const seats = computed(() => Array.from({ length: store.room?.totalSeats ?? 7 }, (_, index) => index + 1))

async function start() {
  await store.start(roomId.value)
  router.push(`/rooms/${roomId.value}/game`)
}

function copy() {
  navigator.clipboard?.writeText(roomId.value)
}
</script>

<style scoped>
.top {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: end;
}
.top p {
  color: #94a3b8;
  word-break: break-all;
}
.actions {
  display: flex;
  gap: 10px;
}
.seats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 14px;
  padding: 20px;
  border-radius: 8px;
}
.seat {
  width: auto;
}
.seat span {
  color: #9fb0d0;
}
</style>
