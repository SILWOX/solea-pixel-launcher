/**
 * Marqueur session : premier lancement après install / réinstall modpack ou install client vanilla.
 * Utilisé pour afficher le toast « premier lancement un peu plus long » (comme sur l’accueil modpack).
 */
const WARMUP_FIRST_PLAY_SESSION = 'solea.pendingWarmupFirstPlay'

function readWarmupPending(): Record<string, boolean> {
  try {
    const raw = sessionStorage.getItem(WARMUP_FIRST_PLAY_SESSION)
    if (!raw) return {}
    const o = JSON.parse(raw) as unknown
    return o && typeof o === 'object' && !Array.isArray(o) ? (o as Record<string, boolean>) : {}
  } catch {
    return {}
  }
}

export function setWarmupPending(id: string): void {
  const p = readWarmupPending()
  p[id] = true
  sessionStorage.setItem(WARMUP_FIRST_PLAY_SESSION, JSON.stringify(p))
}

export function peekWarmupPending(id: string): boolean {
  return !!readWarmupPending()[id]
}

export function consumeWarmupPending(id: string): void {
  const p = readWarmupPending()
  if (!p[id]) return
  delete p[id]
  sessionStorage.setItem(WARMUP_FIRST_PLAY_SESSION, JSON.stringify(p))
}

/** Clé session pour le hub Minecraft vanilla (dossier d’instance type « 1.21.4 Iris Sodium »). */
export function vanillaWarmupSessionKey(profileFolder: string): string {
  return `vanilla:${profileFolder}`
}
