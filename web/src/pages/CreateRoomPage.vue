<template>
  <main class="page">
    <section class="shell grid">
      <header>
        <RouterLink to="/">返回首页</RouterLink>
        <h1>创建狼人杀房间</h1>
        <p>选择模板后可继续微调角色数量和规则。默认 Mock AI，创建后无需任何密钥即可开局。</p>
      </header>

      <div class="glass form">
        <label class="label">房间名称<input v-model="form.roomName" class="input" /></label>
        <label class="label">模板
          <select v-model="selectedTemplate" class="input" @change="applyTemplate">
            <option v-for="tpl in templates" :key="tpl.key" :value="tpl.key">{{ tpl.name }}</option>
          </select>
        </label>
        <label class="label">真人模式
          <select v-model="form.humanMode" class="input">
            <option value="NONE">全 AI 观战</option>
            <option value="SINGLE_HUMAN">1 名真人参与</option>
          </select>
        </label>
        <label v-if="form.humanMode === 'SINGLE_HUMAN'" class="label">真人昵称<input v-model="form.humanPlayerName" class="input" /></label>
        <label class="label">观众视角
          <select v-model="form.observerViewMode" class="input">
            <option value="PUBLIC_VIEW">公共视角</option>
            <option value="GOD_VIEW">上帝视角</option>
          </select>
        </label>

        <section class="roles">
          <label v-for="field in roleFields" :key="field.key" class="label">
            {{ field.label }}
            <input v-model.number="form.roleConfig[field.key]" class="input" type="number" min="0" />
          </label>
        </section>

        <section class="pace-settings">
          <label class="label">Agent 步骤间隔（毫秒）
            <input v-model.number="form.ruleConfig.aiThinkingDelayMillis" class="input" type="number" min="700" max="5000" step="100" />
          </label>
          <label class="toggle-label">
            <input v-model="form.ruleConfig.autoAdvance" type="checkbox" />
            全 AI 对局进入游戏后自动播放
          </label>
        </section>

        <p class="error" v-if="validation">{{ validation }}</p>
        <p class="error" v-if="store.error">{{ store.error }}</p>
        <div class="actions">
          <button class="btn" :disabled="store.loading" @click="create(false)">
            {{ store.loading ? '正在创建...' : '创建房间' }}
          </button>
          <button class="btn secondary" :disabled="store.loading" @click="create(true)">
            {{ store.loading ? '正在开局...' : '创建并开始' }}
          </button>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDefaults } from '../api/client'
import { useGameStore } from '../stores/game'

const router = useRouter()
const store = useGameStore()
const templates = ref<any[]>([])
const selectedTemplate = ref('7-standard')
const form = reactive<any>({
  roomName: 'AI 7人标准局',
  totalSeats: 7,
  humanMode: 'NONE',
  humanPlayerName: '',
  humanRoleAssignMode: 'RANDOM',
  specifiedHumanRole: null,
  observerViewMode: 'GOD_VIEW',
  roleConfig: {},
  ruleConfig: {},
  uiConfig: {}
})

const roleFields = [
  ['werewolfCount', '狼人'], ['wolfKingCount', '狼王'], ['whiteWolfKingCount', '白狼王'], ['hiddenWolfCount', '隐狼'],
  ['villagerCount', '平民'], ['seerCount', '预言家'], ['witchCount', '女巫'], ['hunterCount', '猎人'], ['guardCount', '守卫'],
  ['idiotCount', '白痴'], ['knightCount', '骑士'], ['graveKeeperCount', '守墓人'], ['magicianCount', '魔术师'], ['cupidCount', '丘比特'], ['elderCount', '长老']
].map(([key, label]) => ({ key, label }))

const validation = computed(() => {
  const total = Object.values(form.roleConfig).reduce((sum: number, value: any) => sum + Number(value || 0), 0)
  if (total !== form.totalSeats) return `当前角色总数 ${total}，需要等于座位数 ${form.totalSeats}`
  if ((form.roleConfig.werewolfCount + form.roleConfig.wolfKingCount + form.roleConfig.whiteWolfKingCount + form.roleConfig.hiddenWolfCount) < 1) return '至少需要 1 名狼人阵营角色'
  return ''
})

onMounted(async () => {
  const data = await getDefaults()
  templates.value = data.templates
  applyTemplate()
})

function applyTemplate() {
  const tpl = templates.value.find(item => item.key === selectedTemplate.value) ?? templates.value[0]
  if (!tpl) return
  form.roomName = tpl.name
  form.totalSeats = tpl.seats
  form.roleConfig = { ...tpl.roleConfig }
  form.ruleConfig = { ...tpl.ruleConfig }
  form.uiConfig = { ...tpl.uiConfig }
}

async function create(andStart: boolean) {
  if (validation.value || store.loading) return
  await store.create({ ...form })
  if (store.room && andStart) await store.start(store.room.id)
  if (store.room) router.push(`/rooms/${store.room.id}/${andStart ? 'game' : 'lobby'}`)
}
</script>

<style scoped>
header p {
  color: #bdc8e8;
}
.form {
  display: grid;
  gap: 16px;
  padding: 22px;
  border-radius: 8px;
}
.roles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}
.pace-settings {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(260px, 1fr);
  gap: 16px;
  align-items: end;
}
.toggle-label {
  min-height: 42px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #cbd5e1;
}
.error {
  color: #fecaca;
}
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
@media (max-width: 720px) {
  .pace-settings {
    grid-template-columns: 1fr;
  }
}
</style>
