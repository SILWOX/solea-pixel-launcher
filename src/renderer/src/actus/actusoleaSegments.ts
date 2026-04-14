import type { ActuSoleaJson } from './actusoleaTypes'

/** Une ligne `___` seule (ou entourée de retours ligne) entre deux blocs = bulles séparées. */
export function splitActuSegments(json: ActuSoleaJson): string[] {
  if (json.segments?.length) {
    return json.segments.map((s) => String(s).trim()).filter(Boolean)
  }
  const raw = json.content ?? ''
  if (!raw.trim()) return []
  return raw
    .split(/\r?\n\s*___\s*\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}
