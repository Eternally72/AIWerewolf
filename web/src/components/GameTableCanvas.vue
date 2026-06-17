<template>
  <section ref="rootRef" class="game-table-canvas glass">
    <canvas ref="canvasRef" />
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { aiAvatarAsset, backgroundAssets, roleAsset } from '../assets'
import type { GameAnimation } from '../game/animationQueue'
import { phaseName, roleName } from '../game/gameLabels'
import { imageCache, preloadImages } from '../game/resourcePreloader'
import type { GameView, PlayerView } from '../types/game'

const props = defineProps<{
  view: GameView | null
  victoryTitle?: string | null
  victoryReason?: string | null
  animations?: GameAnimation[]
}>()

const rootRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
let resizeObserver: ResizeObserver | null = null
let animationFrame = 0
let canvasWidth = 0
let canvasHeight = 0
let previousView: GameView | null = null
let activeAnimations: RenderAnimation[] = []
let sceneLight = 0
let lightFrom = 0
let lightTo = 0
let lightTransitionStartedAt = 0
const lightTransitionDuration = 900

type RenderAnimation =
  | { kind: 'PHASE'; phase: string; startedAt: number; duration: number }
  | { kind: 'SPEECH'; playerId: string; text: string; startedAt: number; duration: number }
  | { kind: 'DEATH'; playerId: string; startedAt: number; duration: number }
  | { kind: 'VOTE'; voterPlayerId: string; targetPlayerId: string; startedAt: number; duration: number }
  | { kind: 'GAME_OVER'; startedAt: number; duration: number }

onMounted(() => {
  resizeObserver = new ResizeObserver(() => scheduleDraw())
  if (rootRef.value) resizeObserver.observe(rootRef.value)
  preloadSceneAssets().finally(scheduleDraw)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  cancelAnimationFrame(animationFrame)
})

watch(() => props.view, (view, oldView) => {
  enqueueViewAnimations(view, oldView ?? previousView)
  updateLightTarget(view, oldView ?? previousView)
  previousView = cloneView(view)
  preloadSceneAssets().finally(scheduleDraw)
}, { deep: true })

watch(() => props.animations, animations => {
  enqueueExternalAnimations(animations ?? [])
  scheduleDraw()
}, { deep: true })

async function preloadSceneAssets() {
  const urls = [
    backgroundAssets.table,
    ...((props.view?.players ?? []).map((player, index) => playerAvatar(player, index)))
  ].filter(Boolean)
  await preloadImages([...new Set(urls)])
}

function scheduleDraw() {
  cancelAnimationFrame(animationFrame)
  animationFrame = requestAnimationFrame(drawFrame)
}

function drawFrame(now = performance.now()) {
  draw(now)
  activeAnimations = activeAnimations.filter(animation => now - animation.startedAt < animation.duration)
  if (activeAnimations.length > 0) {
    animationFrame = requestAnimationFrame(drawFrame)
  }
}

function draw(now = performance.now()) {
  const canvas = canvasRef.value
  const root = rootRef.value
  if (!canvas || !root) return

  const rect = root.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const nextWidth = Math.max(1, Math.floor(rect.width * dpr))
  const nextHeight = Math.max(1, Math.floor(rect.height * dpr))
  if (nextWidth !== canvasWidth || nextHeight !== canvasHeight) {
    canvasWidth = nextWidth
    canvasHeight = nextHeight
    canvas.width = nextWidth
    canvas.height = nextHeight
  }
  canvas.style.width = `${rect.width}px`
  canvas.style.height = `${rect.height}px`

  const context = canvas.getContext('2d')
  if (!context) return
  context.setTransform(dpr, 0, 0, dpr, 0, 0)
  context.clearRect(0, 0, rect.width, rect.height)

  drawBackground(context, rect.width, rect.height)
  drawDayNightOverlay(context, rect.width, rect.height, now)
  drawHud(context, rect.width, now)
  drawPlayers(context, rect.width, rect.height, now)
  drawVoteLines(context, rect.width, rect.height, now)
  if (props.victoryTitle) {
    drawVictory(context, rect.width, now)
  }
  drawSpeechBubbles(context, rect.width, rect.height, now)
}

function drawBackground(context: CanvasRenderingContext2D, width: number, height: number) {
  const background = imageCache().get(backgroundAssets.table)
  if (background?.complete) {
    coverImage(context, background, 0, 0, width, height)
  } else {
    const gradient = context.createLinearGradient(0, 0, width, height)
    gradient.addColorStop(0, '#111827')
    gradient.addColorStop(1, '#211129')
    context.fillStyle = gradient
    context.fillRect(0, 0, width, height)
  }
  context.fillStyle = 'rgba(3, 6, 14, 0.22)'
  context.fillRect(0, 0, width, height)
}

function drawDayNightOverlay(context: CanvasRenderingContext2D, width: number, height: number, now: number) {
  const transitionProgress = lightTransitionStartedAt
    ? easeOutCubic(Math.min(1, (now - lightTransitionStartedAt) / lightTransitionDuration))
    : 1
  sceneLight = lightFrom + (lightTo - lightFrom) * transitionProgress
  const nightAlpha = 0.42 * (1 - sceneLight)
  const dayAlpha = 0.16 * sceneLight
  if (dayAlpha > 0.01) {
    const dayGlow = context.createRadialGradient(width * 0.5, height * 0.42, 20, width * 0.5, height * 0.42, width * 0.54)
    dayGlow.addColorStop(0, `rgba(255, 224, 166, ${dayAlpha})`)
    dayGlow.addColorStop(1, 'rgba(255, 224, 166, 0)')
    context.fillStyle = dayGlow
    context.fillRect(0, 0, width, height)
  }
  if (nightAlpha > 0.01) {
    context.fillStyle = `rgba(2, 6, 23, ${nightAlpha})`
    context.fillRect(0, 0, width, height)
    const moonGlow = context.createRadialGradient(width * 0.72, height * 0.14, 4, width * 0.72, height * 0.14, width * 0.28)
    moonGlow.addColorStop(0, `rgba(191, 219, 254, ${0.16 * (1 - sceneLight)})`)
    moonGlow.addColorStop(1, 'rgba(191, 219, 254, 0)')
    context.fillStyle = moonGlow
    context.fillRect(0, 0, width, height)
  }
}

function drawHud(context: CanvasRenderingContext2D, width: number, now: number) {
  const view = props.view
  if (!view) return
  const phaseAnimation = activeAnimations.find(animation => animation.kind === 'PHASE')
  const pulse = phaseAnimation ? 0.14 * (1 - progress(phaseAnimation, now)) : 0
  roundedRect(context, 22, 18, 132, 48, 8)
  context.fillStyle = `rgba(5, 8, 18, ${0.62 + pulse})`
  context.fill()
  context.fillStyle = '#f8fbff'
  context.font = '700 14px system-ui'
  context.fillText(phaseName(view.phase), 34, 40)
  context.fillStyle = '#b8c4e8'
  context.font = '12px system-ui'
  context.fillText(`${view.players.filter(player => player.alive).length} / ${view.players.length} 存活`, 34, 58)
}

function drawVictory(context: CanvasRenderingContext2D, width: number, now: number) {
  const animation = activeAnimations.find(item => item.kind === 'GAME_OVER')
  const lift = animation ? 12 * (1 - easeOutCubic(progress(animation, now))) : 0
  const panelWidth = Math.min(460, width * 0.58)
  const left = (width - panelWidth) / 2
  roundedRect(context, left, 22 - lift, panelWidth, 92, 8)
  context.fillStyle = 'rgba(6, 9, 18, 0.82)'
  context.fill()
  context.strokeStyle = 'rgba(253, 230, 138, 0.42)'
  context.stroke()
  context.textAlign = 'center'
  context.fillStyle = '#cbd5e1'
  context.font = '12px system-ui'
  context.fillText('游戏结束', width / 2, 47 - lift)
  context.fillStyle = '#fde68a'
  context.font = '700 24px system-ui'
  context.fillText(props.victoryTitle ?? '', width / 2, 76 - lift)
  context.fillStyle = '#e5e7eb'
  context.font = '13px system-ui'
  context.fillText(truncate(props.victoryReason ?? '', 38), width / 2, 100 - lift)
  context.textAlign = 'start'
}

function drawPlayers(context: CanvasRenderingContext2D, width: number, height: number, now: number) {
  const players = props.view?.players ?? []
  players.forEach((player, index) => {
    const point = seatPoint(index, players.length, width, height)
    drawSeat(context, player, index, point.x, point.y, now)
  })
}

function drawVoteLines(context: CanvasRenderingContext2D, width: number, height: number, now: number) {
  const players = props.view?.players ?? []
  const voteAnimations = activeAnimations.filter(animation => animation.kind === 'VOTE')
  voteAnimations.forEach(animation => {
    const voterIndex = players.findIndex(player => player.id === animation.voterPlayerId)
    const targetIndex = players.findIndex(player => player.id === animation.targetPlayerId)
    if (voterIndex < 0 || targetIndex < 0) return
    const from = seatPoint(voterIndex, players.length, width, height)
    const to = seatPoint(targetIndex, players.length, width, height)
    const value = easeOutCubic(progress(animation, now))
    const control = {
      x: (from.x + to.x) / 2,
      y: Math.min(from.y, to.y) - Math.max(80, height * 0.12)
    }
    context.save()
    context.globalAlpha = 1 - Math.max(0, progress(animation, now) - 0.78) / 0.22
    context.lineWidth = 2
    context.strokeStyle = 'rgba(248, 113, 113, 0.72)'
    context.shadowColor = 'rgba(248, 113, 113, 0.55)'
    context.shadowBlur = 12
    context.beginPath()
    context.moveTo(from.x, from.y)
    context.quadraticCurveTo(control.x, control.y, to.x, to.y)
    context.stroke()

    const dot = quadraticPoint(from, control, to, value)
    context.fillStyle = '#fee2e2'
    context.beginPath()
    context.arc(dot.x, dot.y, 5 + Math.sin(value * Math.PI) * 2, 0, Math.PI * 2)
    context.fill()

    context.shadowBlur = 18
    context.strokeStyle = 'rgba(254, 226, 226, 0.65)'
    context.lineWidth = 3
    context.beginPath()
    context.arc(to.x, to.y - 32, 34 + Math.sin(value * Math.PI) * 8, 0, Math.PI * 2)
    context.stroke()
    context.restore()
  })
}

function drawSeat(context: CanvasRenderingContext2D, player: PlayerView, index: number, x: number, y: number, now: number) {
  const cardWidth = Math.max(96, Math.min(128, canvasRef.value ? canvasRef.value.clientWidth * 0.072 : 116))
  const cardHeight = 132
  const deathAnimation = activeAnimations.find(animation => animation.kind === 'DEATH' && animation.playerId === player.id)
  const scale = deathAnimation ? 1 + 0.08 * Math.sin(progress(deathAnimation, now) * Math.PI) : 1
  context.save()
  context.translate(x, y)
  context.scale(scale, scale)
  context.translate(-x, -y)
  context.globalAlpha = player.alive ? 1 : 0.46
  roundedRect(context, x - cardWidth / 2, y - cardHeight / 2, cardWidth, cardHeight, 8)
  context.fillStyle = 'rgba(5, 8, 18, 0.78)'
  context.fill()
  context.strokeStyle = player.camp === 'WEREWOLF' ? 'rgba(248, 113, 113, 0.34)' : 'rgba(211, 224, 255, 0.2)'
  context.stroke()

  drawAvatar(context, player, index, x, y - 32)

  context.fillStyle = '#f8fbff'
  context.font = '700 15px system-ui'
  context.textAlign = 'center'
  context.fillText(`${player.name}`, x, y + 16)
  context.fillStyle = '#cbd5e1'
  context.font = '12px system-ui'
  context.fillText(`${player.seatNumber} 号 · ${player.type}`, x, y + 36)
  context.fillStyle = player.role ? '#fde68a' : '#8b93aa'
  context.fillText(player.role ? roleName(player.role) : '身份未公开', x, y + 58)
  context.restore()
}

function drawSpeechBubbles(context: CanvasRenderingContext2D, width: number, height: number, now: number) {
  const players = props.view?.players ?? []
  const speechAnimations = activeAnimations.filter(animation => animation.kind === 'SPEECH')
  speechAnimations.forEach(animation => {
    const playerIndex = players.findIndex(player => player.id === animation.playerId)
    if (playerIndex < 0) return
    const point = seatPoint(playerIndex, players.length, width, height)
    const value = easeOutCubic(progress(animation, now))
    const bubbleWidth = Math.min(300, Math.max(190, animation.text.length * 8))
    const bubbleHeight = 54
    const x = Math.max(18, Math.min(width - bubbleWidth - 18, point.x - bubbleWidth / 2))
    const y = Math.max(18, point.y - 126 - value * 12)
    context.save()
    context.globalAlpha = 1 - Math.max(0, progress(animation, now) - 0.72) / 0.28
    roundedRect(context, x, y, bubbleWidth, bubbleHeight, 8)
    context.fillStyle = 'rgba(8, 12, 24, 0.88)'
    context.fill()
    context.strokeStyle = 'rgba(219, 234, 254, 0.22)'
    context.stroke()
    context.fillStyle = '#f8fbff'
    context.font = '13px system-ui'
    context.textAlign = 'start'
    context.fillText(truncate(animation.text, 28), x + 12, y + 31)
    context.restore()
  })
}

function drawAvatar(context: CanvasRenderingContext2D, player: PlayerView, index: number, x: number, y: number) {
  const radius = 26
  const src = playerAvatar(player, index)
  const image = imageCache().get(src)
  context.save()
  context.beginPath()
  context.arc(x, y, radius, 0, Math.PI * 2)
  context.clip()
  if (image?.complete) {
    coverImage(context, image, x - radius, y - radius, radius * 2, radius * 2)
  } else {
    context.fillStyle = '#334155'
    context.fillRect(x - radius, y - radius, radius * 2, radius * 2)
    context.fillStyle = '#f8fbff'
    context.font = '700 16px system-ui'
    context.textAlign = 'center'
    context.fillText(String(player.seatNumber), x, y + 5)
  }
  context.restore()
}

function seatPoint(index: number, total: number, width: number, height: number) {
  const angle = (Math.PI * 2 * index) / total - Math.PI / 2
  return {
    x: width * (0.5 + Math.cos(angle) * 0.36),
    y: height * (0.5 + Math.sin(angle) * 0.38)
  }
}

function playerAvatar(player: PlayerView, index: number) {
  return roleAsset(player.role) ?? aiAvatarAsset(index)
}

function coverImage(context: CanvasRenderingContext2D, image: HTMLImageElement, x: number, y: number, width: number, height: number) {
  const scale = Math.max(width / image.naturalWidth, height / image.naturalHeight)
  const drawWidth = image.naturalWidth * scale
  const drawHeight = image.naturalHeight * scale
  context.drawImage(image, x + (width - drawWidth) / 2, y + (height - drawHeight) / 2, drawWidth, drawHeight)
}

function roundedRect(context: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) {
  context.beginPath()
  context.roundRect(x, y, width, height, radius)
}

function truncate(value: string, max: number) {
  return value.length > max ? `${value.slice(0, max)}...` : value
}

function enqueueViewAnimations(view: GameView | null, oldView: GameView | null) {
  if (!view) return
  const now = performance.now()
  if (oldView && oldView.phase !== view.phase) {
    activeAnimations.push({ kind: 'PHASE', phase: view.phase, startedAt: now, duration: 650 })
  }
  const oldSpeechKeys = new Set((oldView?.speeches ?? []).map(speech => `${speech.playerId}:${speech.createdAt}`))
  for (const speech of view.speeches) {
    const key = `${speech.playerId}:${speech.createdAt}`
    if (!oldSpeechKeys.has(key)) {
      activeAnimations.push({ kind: 'SPEECH', playerId: speech.playerId, text: speech.content, startedAt: now, duration: 3200 })
    }
  }
  const oldVoteKeys = new Set((oldView?.votes ?? []).map(vote => `${vote.voterPlayerId}:${vote.targetPlayerId}:${vote.createdAt}`))
  for (const vote of view.votes) {
    const key = `${vote.voterPlayerId}:${vote.targetPlayerId}:${vote.createdAt}`
    if (!oldVoteKeys.has(key)) {
      activeAnimations.push({
        kind: 'VOTE',
        voterPlayerId: vote.voterPlayerId,
        targetPlayerId: vote.targetPlayerId,
        startedAt: now,
        duration: 1400
      })
    }
  }
  const oldPlayers = new Map((oldView?.players ?? []).map(player => [player.id, player]))
  for (const player of view.players) {
    const oldPlayer = oldPlayers.get(player.id)
    if (oldPlayer?.alive && !player.alive) {
      activeAnimations.push({ kind: 'DEATH', playerId: player.id, startedAt: now, duration: 1000 })
    }
  }
  if (oldView?.phase !== 'GAME_OVER' && view.phase === 'GAME_OVER') {
    activeAnimations.push({ kind: 'GAME_OVER', startedAt: now, duration: 1200 })
  }
}

function enqueueExternalAnimations(animations: GameAnimation[]) {
  const now = performance.now()
  for (const animation of animations) {
    if (animation.kind === 'PHASE_CHANGED') {
      activeAnimations.push({ kind: 'PHASE', phase: String(animation.payload ?? ''), startedAt: now, duration: 650 })
    }
    if (animation.kind === 'PLAYER_SPOKE') {
      const speech = animation.payload as { playerId?: string; content?: string }
      if (speech.playerId && speech.content) {
        activeAnimations.push({ kind: 'SPEECH', playerId: speech.playerId, text: speech.content, startedAt: now, duration: 3200 })
      }
    }
    if (animation.kind === 'PLAYER_DIED') {
      const player = animation.payload as { id?: string }
      if (player.id) {
        activeAnimations.push({ kind: 'DEATH', playerId: player.id, startedAt: now, duration: 1000 })
      }
    }
    if (animation.kind === 'VOTE_CAST') {
      const vote = animation.payload as { voterPlayerId?: string; targetPlayerId?: string }
      if (vote.voterPlayerId && vote.targetPlayerId) {
        activeAnimations.push({
          kind: 'VOTE',
          voterPlayerId: vote.voterPlayerId,
          targetPlayerId: vote.targetPlayerId,
          startedAt: now,
          duration: 1400
        })
      }
    }
    if (animation.kind === 'GAME_OVER') {
      activeAnimations.push({ kind: 'GAME_OVER', startedAt: now, duration: 1200 })
    }
  }
}

function updateLightTarget(view: GameView | null, oldView: GameView | null) {
  if (!view) return
  const nextLight = phaseLight(view.phase)
  if (!oldView) {
    sceneLight = nextLight
    lightFrom = nextLight
    lightTo = nextLight
    lightTransitionStartedAt = 0
    return
  }
  if (nextLight !== lightTo) {
    lightFrom = sceneLight
    lightTo = nextLight
    lightTransitionStartedAt = performance.now()
  }
}

function phaseLight(phase?: string | null) {
  if (!phase) return 0.72
  if (phase === 'GAME_OVER') return 0.78
  if (phase.includes('NIGHT') || phase.includes('WEREWOLF') || phase.includes('SEER') || phase.includes('WITCH') || phase.includes('GUARD')) {
    return 0.14
  }
  if (phase.includes('DAY') || phase === 'EXECUTION' || phase === 'LAST_WORDS') {
    return 0.82
  }
  return 0.56
}

function quadraticPoint(from: { x: number; y: number }, control: { x: number; y: number }, to: { x: number; y: number }, t: number) {
  const oneMinus = 1 - t
  return {
    x: oneMinus * oneMinus * from.x + 2 * oneMinus * t * control.x + t * t * to.x,
    y: oneMinus * oneMinus * from.y + 2 * oneMinus * t * control.y + t * t * to.y
  }
}

function cloneView(view: GameView | null): GameView | null {
  return view ? structuredClone(view) : null
}

function progress(animation: RenderAnimation, now: number) {
  return Math.min(1, Math.max(0, (now - animation.startedAt) / animation.duration))
}

function easeOutCubic(value: number) {
  return 1 - Math.pow(1 - value, 3)
}

</script>

<style scoped>
.game-table-canvas {
  position: relative;
  height: min(760px, calc(100vh - 116px));
  min-height: 520px;
  border-radius: 12px;
  overflow: hidden;
  contain: layout paint size;
}

canvas {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
