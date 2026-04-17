/**
 * Worker dédié au lancement Minecraft (minecraft-java-core).
 * Évite de bloquer le processus principal Electron (UI « Ne répond pas »).
 *
 * Modes :
 * - `launch` : `Launch()` complet (téléchargements + jeu).
 * - `download` : uniquement `DownloadGame()` (client + libs + assets), sans démarrer le jeu.
 */
import path from 'node:path'
import { parentPort, workerData } from 'node:worker_threads'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { Launch } = require('minecraft-java-core') as {
  Launch: new () => {
    Launch: (opt: Record<string, unknown>) => Promise<boolean>
    on: (ev: string, fn: (...args: unknown[]) => void) => void
    off: (ev: string, fn: (...args: unknown[]) => void) => void
    DownloadGame?: () => Promise<unknown>
  }
}

type WorkerData = {
  launchOpts: Record<string, unknown>
  mode?: 'launch' | 'download'
}

function applyMergedLaunchOptions(
  launcher: { options?: Record<string, unknown> },
  opt: Record<string, unknown>
): string | null {
  const defaultOptions: Record<string, unknown> = {
    url: null,
    authenticator: null,
    timeout: 10000,
    path: '.Minecraft',
    version: 'latest_release',
    instance: null,
    detached: false,
    intelEnabledMac: false,
    ignore_log4j: false,
    downloadFileMultiple: 5,
    bypassOffline: false,
    loader: {
      path: './loader',
      type: null,
      build: 'latest',
      enable: false
    },
    mcp: null,
    verify: false,
    ignored: [],
    JVM_ARGS: [],
    GAME_ARGS: [],
    java: {
      path: null,
      version: null,
      type: 'jre'
    },
    screen: {
      width: null,
      height: null,
      fullscreen: false
    },
    memory: {
      min: '1G',
      max: '2G'
    },
    ...opt
  }
  launcher.options = defaultOptions
  launcher.options.path = path.resolve(String(launcher.options.path)).replace(/\\/g, '/')
  const mcp = launcher.options.mcp
  if (mcp) {
    const inst = launcher.options.instance
    if (inst)
      launcher.options.mcp = `${launcher.options.path}/instances/${inst}/${mcp}`
    else launcher.options.mcp = path.resolve(`${launcher.options.path}/${mcp}`).replace(/\\/g, '/')
  }
  const ld = launcher.options.loader as { type?: string | null; build?: string; path?: unknown }
  if (ld.type) {
    ld.type = String(ld.type).toLowerCase()
    if (typeof ld.build === 'string') ld.build = ld.build.toLowerCase()
  }
  if (!launcher.options.authenticator) return 'Authenticator not found'
  let dfm = launcher.options.downloadFileMultiple
  if (typeof dfm !== 'number' || dfm < 1) dfm = 1
  if (dfm > 30) dfm = 30
  launcher.options.downloadFileMultiple = dfm
  if (typeof ld.path !== 'string') ld.path = `./loader/${ld.type}`
  const jv = launcher.options.java as { version?: unknown; type?: unknown }
  if (jv.version && typeof jv.type !== 'string') jv.type = 'jre'
  return null
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

function postProgress(current: number, total: number, detail: string): void {
  parentPort?.postMessage({ type: 'progress', current, total, detail })
}

/** Extrait un message lisible (Mojang / minecraft-java-core renvoient souvent `{ error, message }`). */
function formatLaunchCoreError(err: unknown): string {
  if (err instanceof Error) return err.message
  if (!err || typeof err !== 'object') return String(err)
  const o = err as Record<string, unknown>
  if (typeof o.message === 'string' && o.message.trim()) return o.message
  const e = o.error
  if (typeof e === 'string' && e.trim()) return e
  if (e && typeof e === 'object' && typeof (e as { message?: unknown }).message === 'string') {
    return String((e as { message: string }).message)
  }
  try {
    return JSON.stringify(err)
  } catch {
    return 'Unknown error'
  }
}

function isDownloadGameFailure(data: unknown): boolean {
  if (!data || typeof data !== 'object') return true
  return 'error' in data
}

function messageFromDownloadFailure(data: unknown): string {
  if (!data || typeof data !== 'object') return 'DownloadGame failed'
  return formatLaunchCoreError(data)
}

const wd = workerData as WorkerData
const mode = wd.mode ?? 'launch'
const launchOpts = wd.launchOpts as Record<string, unknown>
const javaOpt = launchOpts.java as { path?: string | null } | undefined
if (javaOpt?.path && typeof javaOpt.path === 'string') {
  const p = javaOpt.path
  if (p.toLowerCase().endsWith('javaw.exe')) {
    javaOpt.path = `${p.slice(0, -9)}java.exe`
  }
}

const launcher = new Launch()

let lastProgressAt = 0
const PROGRESS_THROTTLE_MS = 120
let lastCheckAt = 0
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
  if (mode === 'download') postProgress(cur, tot, el)
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
  parentPort?.postMessage({ type: 'error', message: formatLaunchCoreError(err) })
})

launcher.on('close', () => {
  parentPort?.postMessage({ type: 'close' })
})

/**
 * La lib `minecraft-java-core` appelle `this.start()` sans `await` dans `Launch()` :
 * la promesse de `Launch()` se résout tout de suite alors que téléchargement + spawn continuent.
 * On attend la ligne de log émise juste avant `spawn` (voir `Launch.js` dans la lib).
 */
const SPAWN_LOG_SENTINEL = 'Launching with arguments'
const LAUNCH_WAIT_DONE = '__solea_launch_wait_done__'

async function runDownloadMode(): Promise<void> {
  postDataLine('[launcher] Téléchargement du client Minecraft…\n')
  const authErr = applyMergedLaunchOptions(launcher, launchOpts)
  if (authErr) {
    parentPort?.postMessage({ type: 'error', message: authErr })
    return
  }
  const inst = launcher as unknown as { DownloadGame: () => Promise<unknown> }
  const data = await inst.DownloadGame()
  if (isDownloadGameFailure(data)) {
    parentPort?.postMessage({ type: 'error', message: messageFromDownloadFailure(data) })
    return
  }
  parentPort?.postMessage({ type: 'download-done' })
}

async function runLaunchMode(): Promise<void> {
  postDataLine('[launcher] Starting Minecraft…\n')
  const authErr = applyMergedLaunchOptions(launcher, launchOpts)
  if (authErr) {
    parentPort?.postMessage({ type: 'error', message: authErr })
    return
  }
  try {
    await new Promise<void>((resolve, reject) => {
      let finished = false
      const finalize = () => {
        if (finished) return
        finished = true
        launcher.off('data', onData)
        launcher.off('error', onErr)
        clearTimeout(timer)
      }
      const onData = (line: string) => {
        if (finished) return
        if (String(line).includes(SPAWN_LOG_SENTINEL)) {
          finalize()
          resolve()
        }
      }
      const onErr = (err: unknown) => {
        if (finished) return
        finalize()
        /* Toujours notifier le parent : sinon échec silencieux (emit error sans message + exit 0). */
        parentPort?.postMessage({ type: 'error', message: formatLaunchCoreError(err) })
        reject(new Error(LAUNCH_WAIT_DONE))
      }
      launcher.on('data', onData)
      launcher.on('error', onErr)
      const timer = setTimeout(() => {
        if (finished) return
        finalize()
        parentPort?.postMessage({
          type: 'error',
          message:
            'Launch timed out: Minecraft never reached the spawn step (check Java path, disk space, and the game log).'
        })
        reject(new Error(LAUNCH_WAIT_DONE))
      }, 45 * 60 * 1000)

      void launcher.Launch(launchOpts).catch((e) => {
        if (finished) return
        finalize()
        parentPort?.postMessage({
          type: 'error',
          message: e instanceof Error ? e.message : String(e)
        })
        reject(new Error(LAUNCH_WAIT_DONE))
      })
    })
    parentPort?.postMessage({ type: 'launch-complete' })
  } catch (e) {
    const m = e instanceof Error ? e.message : String(e)
    if (m !== LAUNCH_WAIT_DONE) {
      parentPort?.postMessage({
        type: 'error',
        message: e instanceof Error ? e.message : String(e)
      })
    }
  }
}

try {
  if (mode === 'download') {
    await runDownloadMode()
  } else {
    await runLaunchMode()
  }
} catch (e) {
  parentPort?.postMessage({
    type: 'error',
    message: e instanceof Error ? e.message : String(e)
  })
}
