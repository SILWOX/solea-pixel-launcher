import { existsSync, readFileSync, readdirSync, statSync } from 'fs'
import { join } from 'path'

export type QuickLaunchHintKind = 'world' | 'server' | 'instance'

export type QuickLaunchHint = {
  kind: QuickLaunchHintKind
  /** Short display string (world folder name, server address, or pack name). */
  label: string
}

function looksLikePlainServerHint(s: string): boolean {
  const t = s.trim()
  if (t.length < 2 || t.length > 120) return false
  if (/^[A-Za-z0-9+/=_-]{32,}$/.test(t)) return false
  return /[\w.-]+\.[a-z]{2,}/i.test(t) || /:\d+$/.test(t) || /^localhost$/i.test(t) || /^\d{1,3}(\.\d{1,3}){3}(:\d+)?$/.test(t)
}

function readLastServerPlain(instanceRoot: string): string | null {
  const optionsPath = join(instanceRoot, 'options.txt')
  if (!existsSync(optionsPath)) return null
  try {
    const text = readFileSync(optionsPath, 'utf8')
    for (const line of text.split(/\r?\n/)) {
      const m = line.match(/^lastServer\s*:\s*(.*)$/i)
      if (!m) continue
      const v = m[1].trim()
      if (v && looksLikePlainServerHint(v)) return v.length > 56 ? `${v.slice(0, 53)}…` : v
    }
  } catch {
    /* ignore */
  }
  return null
}

function findNewestWorldName(savesDir: string): string | null {
  let best: { name: string; mtime: number } | null = null
  try {
    for (const name of readdirSync(savesDir)) {
      const worldPath = join(savesDir, name)
      let st: ReturnType<typeof statSync>
      try {
        st = statSync(worldPath)
      } catch {
        continue
      }
      if (!st.isDirectory()) continue
      const levelDat = join(worldPath, 'level.dat')
      let mtime = st.mtimeMs
      if (existsSync(levelDat)) {
        try {
          mtime = Math.max(mtime, statSync(levelDat).mtimeMs)
        } catch {
          /* ignore */
        }
      }
      if (!best || mtime > best.mtime) best = { name, mtime }
    }
  } catch {
    return null
  }
  return best?.name ?? null
}

/**
 * Heuristic “last session” label: newest single-player world name, else plain last server from options, else pack name.
 */
export function getQuickLaunchHint(instanceRoot: string, modpackDisplayName: string): QuickLaunchHint {
  const saves = join(instanceRoot, 'saves')
  if (existsSync(saves)) {
    const w = findNewestWorldName(saves)
    if (w) {
      const short = w.length > 36 ? `${w.slice(0, 33)}…` : w
      return { kind: 'world', label: short }
    }
  }
  const srv = readLastServerPlain(instanceRoot)
  if (srv) return { kind: 'server', label: srv }
  const dn = modpackDisplayName.length > 40 ? `${modpackDisplayName.slice(0, 37)}…` : modpackDisplayName
  return { kind: 'instance', label: dn }
}
