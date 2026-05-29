<template>
  <main class="page landing">
    <section class="shell hero">
      <div>
        <p class="eyebrow">Multi-Agent Hidden Role Game</p>
        <h1>AI Werewolf</h1>
        <p class="lead">真人、AI Agent、观众上帝视角都能进入同一张暗夜圆桌。每个 AI 都只看见自己该看见的信息。</p>
        <div class="actions">
          <RouterLink class="btn" to="/create">创建房间</RouterLink>
          <RouterLink class="btn secondary" :to="lastRoomPath">进入上次房间</RouterLink>
        </div>
      </div>
      <div class="feature-grid">
        <article v-for="item in features" :key="item.title" class="glass feature">
          <strong>{{ item.title }}</strong>
          <span>{{ item.text }}</span>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
const lastRoom = localStorage.getItem('lastRoomId')
const lastRoomPath = lastRoom ? `/rooms/${lastRoom}/game` : '/create'
const features = [
  { title: '信息隔离', text: '公共、私有、狼队、情侣、上帝视角分层输出。' },
  { title: '全 AI 对局', text: 'Mock LLM 无需密钥也能自动发言、行动、投票。' },
  { title: '实时观战', text: 'REST 恢复状态，WebSocket 推送阶段和时间线。' }
]
</script>

<style scoped>
.landing {
  display: grid;
  align-items: center;
}
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 38px;
  align-items: end;
}
.eyebrow {
  color: #c4b5fd;
  text-transform: uppercase;
  letter-spacing: 0;
}
h1 {
  margin: 0;
  font-size: clamp(56px, 9vw, 112px);
  line-height: 0.92;
}
.lead {
  max-width: 700px;
  color: #c9d5f5;
  font-size: 20px;
  line-height: 1.7;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.feature-grid {
  display: grid;
  gap: 14px;
}
.feature {
  padding: 18px;
  border-radius: 8px;
  display: grid;
  gap: 8px;
}
.feature span {
  color: #cbd5e1;
}
@media (max-width: 820px) {
  .hero {
    grid-template-columns: 1fr;
  }
}
</style>
