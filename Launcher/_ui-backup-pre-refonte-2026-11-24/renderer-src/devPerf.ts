/** Mesures dev uniquement (ex. ouverture modale) — aucun impact prod. */
export function devPerfMark(name: string): string {
  const id = `solea-${name}`
  if (!import.meta.env.DEV) return id
  try {
    performance.mark(id)
  } catch {
    /* ignore */
  }
  return id
}

export function devPerfMeasure(label: string, startMark: string): void {
  if (!import.meta.env.DEV) return
  try {
    performance.measure(`solea-${label}`, startMark)
    const m = performance.getEntriesByName(`solea-${label}`, 'measure').pop()
    if (m) console.debug(`[solea perf] ${label}: ${m.duration.toFixed(1)}ms`)
  } catch {
    /* ignore */
  }
}
