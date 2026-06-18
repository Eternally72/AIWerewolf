<template>
  <div ref="rootRef" class="virtual-list" @scroll="handleScroll">
    <div :style="{ height: `${topSpacer}px` }" />
    <div
      v-for="entry in visibleEntries"
      :key="entry.key"
      class="virtual-list-item"
      :style="{ minHeight: `${itemHeight}px` }"
    >
      <slot :item="entry.item" :index="entry.index" />
    </div>
    <div :style="{ height: `${bottomSpacer}px` }" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  items: any[]
  itemHeight?: number
  overscan?: number
  keyField?: string
}>(), {
  itemHeight: 72,
  overscan: 6,
  keyField: 'id'
})

const rootRef = ref<HTMLElement | null>(null)
const scrollTop = ref(0)
const viewportHeight = ref(0)

const startIndex = computed(() => Math.max(0, Math.floor(scrollTop.value / props.itemHeight) - props.overscan))
const visibleCount = computed(() => Math.ceil(viewportHeight.value / props.itemHeight) + props.overscan * 2)
const endIndex = computed(() => Math.min(props.items.length, startIndex.value + visibleCount.value))
const topSpacer = computed(() => startIndex.value * props.itemHeight)
const bottomSpacer = computed(() => Math.max(0, (props.items.length - endIndex.value) * props.itemHeight))

const visibleEntries = computed(() => props.items.slice(startIndex.value, endIndex.value).map((item, offset) => {
  const index = startIndex.value + offset
  return {
    item,
    index,
    key: itemKey(item, index)
  }
}))

onMounted(measure)
watch(() => props.items.length, () => {
  measure()
  clampScroll()
})

function handleScroll() {
  scrollTop.value = rootRef.value?.scrollTop ?? 0
}

function measure() {
  viewportHeight.value = rootRef.value?.clientHeight ?? 0
  scrollTop.value = rootRef.value?.scrollTop ?? 0
}

function clampScroll() {
  const root = rootRef.value
  if (!root) return
  const maxScrollTop = Math.max(0, props.items.length * props.itemHeight - root.clientHeight)
  if (root.scrollTop > maxScrollTop) {
    root.scrollTop = maxScrollTop
    scrollTop.value = maxScrollTop
  }
}

function itemKey(item: any, index: number) {
  // 业务列表大多有 id；没有 id 时退回 index，避免调试列表因为 key 缺失而整批重绘。
  if (item && typeof item === 'object' && props.keyField in item) {
    const value = item[props.keyField]
    if (typeof value === 'string' || typeof value === 'number') return value
  }
  return index
}
</script>

<style scoped>
.virtual-list {
  max-height: min(420px, 32vh);
  overflow: auto;
  scrollbar-color: #8b8fa3 rgba(255, 255, 255, 0.08);
  content-visibility: auto;
  contain: layout paint;
}

.virtual-list-item {
  display: grid;
}
</style>
