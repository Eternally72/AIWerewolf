<template>
  <main class="page">
    <section class="shell glass over">
      <RouterLink to="/">返回首页</RouterLink>
      <h1>游戏结算</h1>
      <p>胜利阵营、身份揭示、关键事件回放和 AI 表现摘要会在完整对局结束后显示在这里。</p>
      <RouterLink class="btn" to="/create">重新开始</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.over {
  padding: 28px;
  border-radius: 8px;
}
.over p {
  color: #cbd5e1;
}
</style>
