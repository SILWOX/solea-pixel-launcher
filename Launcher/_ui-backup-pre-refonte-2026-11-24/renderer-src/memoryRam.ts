/** Parse une valeur RAM du type "6G" / "512M" → gigaoctets (approximatif pour M). */
export function ramStringToGb(s: string): number {
  const t = s.trim().match(/^(\d+)\s*([mMgG])$/)
  if (!t) return 6
  const n = parseInt(t[1]!, 10)
  const u = t[2]!.toLowerCase()
  if (u === 'g') return n
  return Math.max(1, Math.round(n / 1024))
}

/** À partir du max choisi sur la barre, remplit min/max pour minecraft-java-core. */
export function allocGbToMinMaxStrings(gb: number): { memoryMin: string; memoryMax: string } {
  const max = Math.max(2, Math.min(128, Math.round(gb)))
  const minGb = Math.max(1, Math.min(max - 1, Math.floor(max * 0.5)))
  return { memoryMin: `${minGb}G`, memoryMax: `${max}G` }
}

export const RAM_BAR_MIN_GB = 2
export const RAM_RECOMMENDED_GB = [6, 8, 12] as const

/** Curseur : min = 2G, max = RAM physique totale du PC (adaptatif), plafond raisonnable pour l’UI. */
export function clampRamAllocRange(totalGiB: number): { minGb: number; maxGb: number } {
  const minGb = RAM_BAR_MIN_GB
  const raw = Math.floor(Number.isFinite(totalGiB) && totalGiB > 0 ? totalGiB : 16)
  const maxGb = Math.min(128, Math.max(minGb, raw))
  return { minGb, maxGb }
}

export function clampGbToRange(gb: number, minGb: number, maxGb: number): number {
  return Math.min(maxGb, Math.max(minGb, Math.round(gb)))
}
