import { existsSync, readFileSync, writeFileSync, rmSync } from 'fs'
import { join } from 'path'

export function parseServerProperties(content: string): Map<string, string> {
  const m = new Map<string, string>()
  for (const line of content.split('\n')) {
    const s = line.replace(/\r$/, '').trim()
    if (!s || s.startsWith('#')) continue
    const i = s.indexOf('=')
    if (i <= 0) continue
    const k = s.slice(0, i).trim()
    let v = s.slice(i + 1).trim()
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1)
    }
    m.set(k, v)
  }
  return m
}

export function serializeServerProperties(m: Map<string, string>): string {
  const lines = [...m.entries()].map(([k, v]) => `${k}=${v}`)
  return `${lines.join('\n')}\n`
}

export function readServerPropertiesMap(instanceRoot: string): Map<string, string> {
  const p = join(instanceRoot, 'server.properties')
  if (!existsSync(p)) return new Map()
  try {
    return parseServerProperties(readFileSync(p, 'utf8'))
  } catch {
    return new Map()
  }
}

export function writeServerPropertiesMap(instanceRoot: string, m: Map<string, string>) {
  const p = join(instanceRoot, 'server.properties')
  writeFileSync(p, serializeServerProperties(m), 'utf8')
}

/** Fusionne port + valeurs par défaut si absentes (première install). */
export function mergeServerPortAndDefaults(instanceRoot: string, port: number) {
  const m = readServerPropertiesMap(instanceRoot)
  m.set('server-port', String(port))
  if (!m.has('online-mode')) m.set('online-mode', 'true')
  if (!m.has('motd')) m.set('motd', JSON.stringify('Solea server'))
  if (!m.has('level-name')) m.set('level-name', 'world')
  if (!m.has('level-type')) m.set('level-type', 'minecraft:normal')
  writeServerPropertiesMap(instanceRoot, m)
}

export function sanitizeWorldFolderName(name: string): string | null {
  const t = name.trim()
  if (!t || t.includes('..') || t.includes('/') || t.includes('\\')) return null
  if (!/^[a-zA-Z0-9_-]{1,64}$/.test(t)) return null
  return t
}

export function deleteWorldFolderForServer(instanceRoot: string): { ok: true } | { ok: false; error: string } {
  const m = readServerPropertiesMap(instanceRoot)
  const raw = m.get('level-name') ?? 'world'
  const safe = sanitizeWorldFolderName(raw)
  if (!safe) return { ok: false, error: 'Invalid level-name in server.properties.' }
  const worldPath = join(instanceRoot, safe)
  if (!existsSync(worldPath)) return { ok: true }
  try {
    rmSync(worldPath, { recursive: true, force: true })
    return { ok: true }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
}
