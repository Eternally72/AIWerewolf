const roleFiles: Record<string, string> = {
  WEREWOLF: 'role-werewolf.png',
  WOLF_KING: 'role-wolf-king.png',
  WHITE_WOLF_KING: 'role-white-wolf-king.png',
  HIDDEN_WOLF: 'role-hidden-wolf.png',
  VILLAGER: 'role-villager.png',
  SEER: 'role-seer.png',
  WITCH: 'role-witch.png',
  HUNTER: 'role-hunter.png',
  GUARD: 'role-guard.png',
  IDIOT: 'role-idiot.png',
  KNIGHT: 'role-knight.png',
  GRAVE_KEEPER: 'role-grave-keeper.png',
  MAGICIAN: 'role-magician.png',
  CUPID: 'role-cupid.png',
  ELDER: 'role-elder.png'
}

const assetVariant = import.meta.env.VITE_ASSET_VARIANT ?? 'png'
const optimized = assetVariant === 'optimized-webp'
const ext = optimized ? 'webp' : 'png'
const root = optimized ? '/assets/optimized' : '/assets'

export const backgroundAssets = {
  landing: `${root}/backgrounds/landing-bg.${ext}`,
  table: `${root}/backgrounds/game-table-bg.${ext}`,
  victory: `${root}/backgrounds/victory-bg.${ext}`
}

export function roleAsset(role?: string | null) {
  if (!role) return null
  const file = roleFiles[role]
  return file ? `${root}/roles/${file.replace('.png', `.${ext}`)}` : null
}

export function aiAvatarAsset(index: number) {
  const normalized = Math.max(1, Math.min(18, index + 1))
  return `${root}/avatars/avatar-ai-${String(normalized).padStart(2, '0')}.${ext}`
}
