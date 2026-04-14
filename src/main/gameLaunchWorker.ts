/**
 * Worker dédié au lancement Minecraft (minecraft-java-core).
 * Évite de bloquer le processus principal Electron (UI « Ne répond pas »).
 */
import { parentPort, workerData } from 'node:worker_threads'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { Launch } = require('minecraft-java-core') as {
  Launch: new () => {
    Launch: (opt: Record<string, unknown>) => Promise<boolean>
    on: (ev: string, fn: (...args: unknown[]) => void) => void
  }
}

function formatBytes(n: number): string {
  if (!Number.isFinite(n) || n < 0) return '?'
  if (n < 1024) return `${Math.round(n)} B`
  const units = ['KB', 'MB', 'GB', 'TB']
  let v = n
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${i === 0 ? Math.round(v) : v.toFixed(1)} ${units[i]}`
}

function postDataLine(line: string): void {
  parentPort?.postMessage({ type: 'data', line })
}

const launchOpts = workerData.launchOpts as Record<string, unknown>
const javaOpt = launchOpts.java as { path?: string | null } | undefined
if (javaOpt?.path && typeof javaOpt.path === 'string') {
  const p = javaOpt.path
  if (p.toLowerCase().endsWith('javaw.exe')) {
    javaOpt.path = `${p.slice(0, -9)}java.exe`
  }
}

postDataLine('[launcher] Starting Minecraft…\n')

const launcher = new Launch()

let lastProgressAt = 0
const PROGRESS_THROTTLE_MS = 120
let lastCheckAt = 0
/** Moins de throttling : la vérif est déjà parallèle dans minecraft-java-core ; le log suit mieux la progression. */
const CHECK_THROTTLE_MS = 200
let lastSpeedEtaAt = 0
const SPEED_ETA_THROTTLE_MS = 900

launcher.on('data', (line: string) => {
  postDataLine(String(line))
})

launcher.on('progress', (a: unknown, b: unknown, c: unknown) => {
  const now = Date.now()
  if (now - lastProgressAt < PROGRESS_THROTTLE_MS) return
  lastProgressAt = now
  const cur = typeof a === 'number' ? a : 0
  const tot = typeof b === 'number' ? b : 0
  const el = c !== undefined && c !== null ? String(c) : ''
  const pct = tot > 0 ? Math.min(100, Math.round((cur / tot) * 100)) : 0
  const suffix = el ? ` · ${el}` : ''
  postDataLine(`[download] ${pct}% · ${formatBytes(cur)} / ${formatBytes(tot)}${suffix}\n`)
})

launcher.on('extract', (msg: unknown) => {
  postDataLine(`[extract] ${String(msg)}\n`)
})

launcher.on('check', (a: unknown, b: unknown, c: unknown) => {
  const now = Date.now()
  if (now - lastCheckAt < CHECK_THROTTLE_MS) return
  lastCheckAt = now
  const el = c !== undefined && c !== null ? String(c) : ''
  postDataLine(`[verify] ${String(a)}/${String(b)}${el ? ` · ${el}` : ''}\n`)
})

launcher.on('patch', (msg: unknown) => {
  postDataLine(`[patch] ${String(msg)}\n`)
})

launcher.on('speed', (speed: unknown) => {
  const now = Date.now()
  if (now - lastSpeedEtaAt < SPEED_ETA_THROTTLE_MS) return
  lastSpeedEtaAt = now
  postDataLine(`[speed] ${String(speed)}\n`)
})

launcher.on('estimated', (time: unknown) => {
  const now = Date.now()
  if (now - lastSpeedEtaAt < SPEED_ETA_THROTTLE_MS) return
  lastSpeedEtaAt = now
  postDataLine(`[eta] ${String(time)}\n`)
})

launcher.on('error', (err: unknown) => {
  const msg =
    err && typeof err === 'object' && 'error' in err && typeof (err as { error: unknown }).error === 'string'
      ? (err as { error: string }).error
      : err instanceof Error
        ? err.message
        : String(err)
  parentPort?.postMessage({ type: 'error', message: msg })
})
launcher.on('close', () => {
  parentPort?.postMessage({ type: 'close' })
})

void launcher.Launch(launchOpts)
