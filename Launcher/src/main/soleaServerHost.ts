/**
 * Hébergement local « My Server » — dossier userData/solea-server, un sous-dossier par serveur.
 */
import { app, BrowserWindow, ipcMain, shell } from 'electron'
import { randomUUID } from 'crypto'
import { spawn, type ChildProcess } from 'child_process'
import { createServer } from 'net'
import { join, dirname } from 'path'
import { existsSync, mkdirSync, readFileSync, writeFileSync, rmSync } from 'fs'
import { readFile } from 'fs/promises'
import {
  fetchProjectVersions,
  installMrpackFromModrinth,
  pickLatestVersion,
  type InstallProgress
} from './modrinth.js'
import { getModpackSpec, resolveModpackId, type ModpackId } from './modpacks.js'
import { installForgeNeoForgeServerLoader } from './forgeServerInstaller.js'
import {
  deleteWorldFolderForServer,
  mergeServerPortAndDefaults,
  readServerPropertiesMap,
  sanitizeWorldFolderName,
  writeServerPropertiesMap
} from './serverPropertiesIO.js'
import { loadSettings } from './settings.js'

const MAX_CONSOLE_LINES = 500
const DEFAULT_RAM_MIB = 4096
const DEFAULT_PORT = 25565
/** Lignes JVM supplémentaires (ajoutées après -Xmx dans user_jvm_args.txt au démarrage). */
const SOLEA_JVM_EXTRA_FILE = 'solea-jvm-extra.txt'

function resolveJavaExecutableForServer(): string {
  const s = loadSettings().javaPath?.trim()
  if (!s) return 'java'
  if (process.platform === 'win32' && s.toLowerCase().endsWith('javaw.exe')) {
    return `${s.slice(0, -9)}java.exe`
  }
  return s
}

/** Découpe une ligne de commande (espaces + guillemets simples/doubles). */
function parseQuotedCommandArgs(line: string): string[] {
  const out: string[] = []
  let cur = ''
  let quote: '"' | "'" | null = null
  for (let i = 0; i < line.length; i++) {
    const c = line[i]!
    if (quote) {
      if (c === quote) quote = null
      else cur += c
      continue
    }
    if (c === '"' || c === "'") {
      quote = c
      continue
    }
    if (/\s/.test(c)) {
      if (cur.length) {
        out.push(cur)
        cur = ''
      }
      continue
    }
    cur += c
  }
  if (cur.length) out.push(cur)
  return out
}

/**
 * Extrait les arguments après `java` / `java.exe` dans run.bat ou run.sh (Forge / NeoForge).
 * Permet de lancer le JAR directement : stdin = console Minecraft (les commandes du launcher arrivent au serveur).
 */
function extractJavaArgvFromRunScript(root: string, scriptName: 'run.bat' | 'run.sh'): string[] | null {
  const p = join(root, scriptName)
  if (!existsSync(p)) return null
  const lines = readFileSync(p, 'utf8').replace(/\r\n/g, '\n').split('\n')
  for (let raw of lines) {
    raw = raw.trim()
    if (!raw || /^rem\b/i.test(raw) || /^#/i.test(raw) || /^@echo\b/i.test(raw)) continue
    raw = raw.split('&')[0]?.trim() ?? raw
    raw = raw.replace(/^\^/g, '').trim()
    raw = raw.replace(/^\s*(?:call|exec)\s+/i, '').trim()
    const m = raw.match(/\bjava(?:\.exe)?\s+(.+)$/i)
    if (!m) continue
    let rest = m[1].trim()
    rest = rest.replace(/%[*]\s*$/i, '').replace(/"\$@"\s*$/, '').replace(/\$\@\s*$/, '').trim()
    if (!rest) continue
    const args = parseQuotedCommandArgs(rest)
    if (!args.length) continue
    if (!args.some((a) => a.toLowerCase() === 'nogui')) args.push('nogui')
    return args
  }
  return null
}

type RuntimeState = 'stopped' | 'starting' | 'running' | 'stopping'

export type SoleServerRecord = {
  id: string
  name: string
  description: string
  modpackId: ModpackId
  ramMiB: number
  port: number
  coverFile?: string
  createdAt: string
  installState: 'installing' | 'ready' | 'error'
  installError?: string
  modrinthVersionId?: string
  modrinthVersionNumber?: string
}

export type SoleServerListRow = SoleServerRecord & { serverState: RuntimeState }

type RegistryFile = { servers: SoleServerRecord[] }

let getMainWindow: () => BrowserWindow | null = () => null

function soleaServerRoot(): string {
  return join(app.getPath('userData'), 'solea-server')
}

function registryPath(): string {
  return join(soleaServerRoot(), 'registry.json')
}

function serverDir(id: string): string {
  return join(soleaServerRoot(), id)
}

function sendEvent(payload: Record<string, unknown>) {
  const w = getMainWindow()
  if (w && !w.isDestroyed()) w.webContents.send('solea-server:event', payload)
}

function loadRegistry(): SoleServerRecord[] {
  const p = registryPath()
  if (!existsSync(p)) return []
  try {
    const j = JSON.parse(readFileSync(p, 'utf8')) as RegistryFile
    return Array.isArray(j.servers) ? j.servers : []
  } catch {
    return []
  }
}

function saveRegistry(servers: SoleServerRecord[]) {
  mkdirSync(dirname(registryPath()), { recursive: true })
  writeFileSync(registryPath(), JSON.stringify({ servers }, null, 2), 'utf8')
}

const lineBuffers = new Map<string, string[]>()
const processes = new Map<string, ChildProcess>()
const runtimeState = new Map<string, RuntimeState>()
let runningServerId: string | null = null
let installBusy = false

function pushLine(serverId: string, line: string) {
  const prev = lineBuffers.get(serverId) ?? []
  const next = [...prev, line].slice(-MAX_CONSOLE_LINES)
  lineBuffers.set(serverId, next)
  sendEvent({ kind: 'line', serverId, line })
}

function setRuntime(serverId: string, state: RuntimeState) {
  runtimeState.set(serverId, state)
  sendEvent({ kind: 'runtime', serverId, serverState: state })
}

function readInstalledMeta(instanceRoot: string): { versionId?: string; versionNumber?: string } | null {
  const p = join(instanceRoot, '.solea-installed.json')
  if (!existsSync(p)) return null
  try {
    return JSON.parse(readFileSync(p, 'utf8')) as {
      versionId?: string
      versionNumber?: string
    }
  } catch {
    return null
  }
}

async function saveCoverOptional(imageUrl: string | undefined, destDir: string): Promise<string | undefined> {
  if (!imageUrl?.trim()) return undefined
  const u = imageUrl.trim()
  if (!u.startsWith('http://') && !u.startsWith('https://')) return undefined
  try {
    const res = await fetch(u)
    if (!res.ok) return undefined
    const buf = Buffer.from(await res.arrayBuffer())
    if (buf.length > 12 * 1024 * 1024) return undefined
    const dest = join(destDir, 'cover.png')
    writeFileSync(dest, buf)
    return 'cover.png'
  } catch {
    return undefined
  }
}

/** Image depuis le launcher (data URL PNG/JPEG/WebP). */
function saveCoverFromDataUrl(dataUrl: string, destDir: string): string | undefined {
  const match = dataUrl.match(/^data:image\/([\w.+-]+);base64,(.+)$/i)
  if (!match) return undefined
  const mime = match[1].toLowerCase().replace('x-', '')
  const b64 = match[2].replace(/\s/g, '')
  try {
    const buf = Buffer.from(b64, 'base64')
    if (buf.length > 12 * 1024 * 1024) return undefined
    let fname: string
    if (mime === 'png') fname = 'cover.png'
    else if (mime === 'jpeg' || mime === 'jpg') fname = 'cover.jpg'
    else if (mime === 'webp') fname = 'cover.webp'
    else return undefined
    const dest = join(destDir, fname)
    writeFileSync(dest, buf)
    return fname
  } catch {
    return undefined
  }
}

async function runInstallJob(rec: SoleServerRecord) {
  const root = serverDir(rec.id)
  const spec = getModpackSpec(rec.modpackId)
  sendEvent({
    kind: 'progress',
    serverId: rec.id,
    phase: 'prepare',
    current: 0,
    total: 1,
    detail: spec.displayName
  })
  const onProgress = (p: InstallProgress) => {
    sendEvent({
      kind: 'progress',
      serverId: rec.id,
      phase: p.phase,
      current: p.current,
      total: p.total,
      detail: p.detail
    })
  }
  const { version } = await installMrpackFromModrinth({
    projectSlug: spec.projectSlug,
    gameVersion: spec.gameVersion,
    loader: spec.loader,
    instanceRoot: root,
    installProfile: 'server',
    onProgress
  })
  sendEvent({
    kind: 'progress',
    serverId: rec.id,
    phase: 'loader',
    current: 0,
    total: 1,
    detail: 'Forge / NeoForge serveur…'
  })
  await installForgeNeoForgeServerLoader({
    instanceRoot: root,
    onLog: (line) => pushLine(rec.id, line)
  })
  mergeServerPortAndDefaults(root, rec.port)
  const meta = readInstalledMeta(root)
  rec.installState = 'ready'
  rec.installError = undefined
  rec.modrinthVersionId = version.id
  rec.modrinthVersionNumber = version.version_number
  if (meta?.versionId) rec.modrinthVersionId = meta.versionId
  if (meta?.versionNumber) rec.modrinthVersionNumber = meta.versionNumber
  const servers = loadRegistry().map((s) => (s.id === rec.id ? rec : s))
  saveRegistry(servers)
  pushLine(rec.id, `[Solea] Pack installé (${rec.modrinthVersionNumber ?? '?'}).`)
  sendEvent({
    kind: 'progress',
    serverId: rec.id,
    phase: 'done',
    current: 1,
    total: 1,
    detail: 'ok'
  })
}

async function startInstall(rec: SoleServerRecord) {
  if (installBusy) {
    const servers = loadRegistry().map((s) =>
      s.id === rec.id ? { ...s, installState: 'error' as const, installError: 'busy' } : s
    )
    saveRegistry(servers)
    return
  }
  installBusy = true
  try {
    await runInstallJob(rec)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    const servers = loadRegistry().map((s) =>
      s.id === rec.id
        ? { ...s, installState: 'error' as const, installError: msg }
        : s
    )
    saveRegistry(servers)
    pushLine(rec.id, `[Solea] Erreur d’installation: ${msg}`)
    sendEvent({
      kind: 'progress',
      serverId: rec.id,
      phase: 'error',
      current: 0,
      total: 1,
      detail: msg
    })
  } finally {
    installBusy = false
  }
}

function trySpawnServerProcess(rec: SoleServerRecord): boolean {
  const root = serverDir(rec.id)
  const runWin = join(root, 'run.bat')
  const runUnix = join(root, 'run.sh')
  const exe = process.platform === 'win32' ? runWin : runUnix
  if (!existsSync(exe)) {
    pushLine(
      rec.id,
      `[Solea] Aucun run.bat / run.sh trouvé dans le dossier du serveur. Ajoutez le binaire serveur (Forge/NeoForge) ou réinstallez le pack.`
    )
    return false
  }
  try {
    const extraPath = join(root, SOLEA_JVM_EXTRA_FILE)
    let extra = ''
    try {
      if (existsSync(extraPath)) {
        extra = readFileSync(extraPath, 'utf8').replace(/\r\n/g, '\n').trimEnd()
      }
    } catch {
      /* ignore */
    }
    const base = `-Xms256M -Xmx${rec.ramMiB}M`
    const body = extra ? `${base}\n${extra}${extra.endsWith('\n') ? '' : '\n'}` : `${base}\n`
    writeFileSync(join(root, 'user_jvm_args.txt'), body, 'utf8')
  } catch {
    /* ignore */
  }
  setRuntime(rec.id, 'starting')
  const isWin = process.platform === 'win32'
  const scriptName = isWin ? 'run.bat' : 'run.sh'
  const javaArgs = extractJavaArgvFromRunScript(root, scriptName)
  const javaBin = resolveJavaExecutableForServer()
  const spawnOpts = {
    cwd: root,
    windowsHide: false,
    stdio: ['pipe', 'pipe', 'pipe'] as const,
    env: { ...process.env, SOLEA_SERVER: '1' }
  }
  let child: ChildProcess
  if (javaArgs?.length) {
    child = spawn(javaBin, javaArgs, spawnOpts)
    pushLine(
      rec.id,
      `[Solea] Processus Java lancé directement (les commandes de la console du launcher sont envoyées au serveur).`
    )
  } else {
    pushLine(
      rec.id,
      `[Solea] Impossible d’analyser ${scriptName} — lancement via ${isWin ? 'cmd.exe' : 'sh'} (console du launcher : commandes peut‑être sans effet).`
    )
    const cmd = isWin ? 'cmd.exe' : '/bin/sh'
    const args = isWin ? ['/d', '/s', '/c', 'run.bat'] : ['-lc', './run.sh']
    child = spawn(cmd, args, spawnOpts)
  }
  processes.set(rec.id, child)
  runningServerId = rec.id
  setRuntime(rec.id, 'running')
  child.stdout?.on('data', (ch) => {
    String(ch)
      .split(/\r?\n/)
      .filter(Boolean)
      .forEach((ln) => pushLine(rec.id, ln))
  })
  child.stderr?.on('data', (ch) => {
    String(ch)
      .split(/\r?\n/)
      .filter(Boolean)
      .forEach((ln) => pushLine(rec.id, `[stderr] ${ln}`))
  })
  child.on('exit', (code, signal) => {
    processes.delete(rec.id)
    if (runningServerId === rec.id) runningServerId = null
    setRuntime(rec.id, 'stopped')
    pushLine(rec.id, `[Solea] Processus terminé (code ${code ?? '?'}, signal ${signal ?? '—'}).`)
  })
  child.on('error', (err) => {
    pushLine(rec.id, `[Solea] Erreur processus: ${err.message}`)
  })
  return true
}

export function shutdownSoleaServerHost() {
  for (const [id, ch] of processes) {
    try {
      ch.kill('SIGTERM')
    } catch {
      /* ignore */
    }
    processes.delete(id)
  }
  runningServerId = null
}

export function setupSoleaServerIpc(getWindow: () => BrowserWindow | null) {
  getMainWindow = getWindow

  ipcMain.handle('solea-server:get-cover-data-url', async (_e, id: string) => {
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec?.coverFile) return null
    const p = join(serverDir(id), rec.coverFile)
    if (!existsSync(p)) return null
    try {
      const buf = await readFile(p)
      const lower = rec.coverFile.toLowerCase()
      const mime =
        lower.endsWith('.jpg') || lower.endsWith('.jpeg')
          ? 'image/jpeg'
          : lower.endsWith('.webp')
            ? 'image/webp'
            : 'image/png'
      return `data:${mime};base64,${buf.toString('base64')}`
    } catch {
      return null
    }
  })

  ipcMain.handle('solea-server:list', (): SoleServerListRow[] =>
    loadRegistry().map((rec) => ({
      ...rec,
      serverState: runtimeState.get(rec.id) ?? (processes.has(rec.id) ? 'running' : 'stopped')
    }))
  )

  ipcMain.handle(
    'solea-server:create',
    async (
      _e,
      payload: {
        name: string
        description?: string
        /** @deprecated préférer coverImageDataUrl */
        imageUrl?: string
        coverImageDataUrl?: string
        modpackId: string
      }
    ): Promise<{ ok: true; id: string } | { ok: false; error: string }> => {
      if (installBusy) return { ok: false, error: 'Une installation est déjà en cours.' }
      let modpackId: ModpackId
      try {
        modpackId = resolveModpackId(payload.modpackId)
      } catch {
        return { ok: false, error: 'Modpack inconnu.' }
      }
      const name = payload.name.trim()
      if (!name) return { ok: false, error: 'Nom requis.' }
      const id = randomUUID()
      const root = serverDir(id)
      mkdirSync(root, { recursive: true })
      let finalCover: string | undefined
      if (payload.coverImageDataUrl?.trim()) {
        finalCover = saveCoverFromDataUrl(payload.coverImageDataUrl.trim(), root)
      }
      if (!finalCover && payload.imageUrl?.trim()) {
        finalCover = await saveCoverOptional(payload.imageUrl, root)
      }
      const rec: SoleServerRecord = {
        id,
        name,
        description: (payload.description ?? '').trim(),
        modpackId,
        ramMiB: DEFAULT_RAM_MIB,
        port: DEFAULT_PORT,
        coverFile: finalCover,
        createdAt: new Date().toISOString(),
        installState: 'installing'
      }
      const servers = [...loadRegistry(), rec]
      saveRegistry(servers)
      lineBuffers.set(id, [])
      void startInstall(rec)
      return { ok: true, id }
    }
  )

  ipcMain.handle(
    'solea-server:update',
    async (
      _e,
      payload: {
        id: string
        name?: string
        description?: string
        imageUrl?: string | null
        coverImageDataUrl?: string | null
        ramMiB?: number
        modpackId?: string
        port?: number
      }
    ): Promise<{ ok: true } | { ok: false; error: string }> => {
      const servers = loadRegistry()
      const idx = servers.findIndex((s) => s.id === payload.id)
      if (idx < 0) return { ok: false, error: 'Serveur introuvable.' }
      const cur = servers[idx]
      let next: SoleServerRecord = { ...cur }
      if (payload.name !== undefined) next.name = payload.name.trim() || cur.name
      if (payload.description !== undefined) next.description = payload.description.trim()
      if (payload.ramMiB !== undefined)
        next.ramMiB = Math.min(64 * 1024, Math.max(512, Math.floor(payload.ramMiB)))
      if (payload.port !== undefined)
        next.port = Math.min(65535, Math.max(1024, Math.floor(payload.port)))
      if (payload.coverImageDataUrl === null || payload.imageUrl === null) {
        next.coverFile = undefined
        try {
          const dir = serverDir(cur.id)
          for (const f of ['cover.png', 'cover.jpg', 'cover.jpeg', 'cover.webp']) {
            const p = join(dir, f)
            if (existsSync(p)) rmSync(p, { force: true })
          }
        } catch {
          /* ignore */
        }
      } else if (payload.coverImageDataUrl?.trim()) {
        const c = saveCoverFromDataUrl(payload.coverImageDataUrl.trim(), serverDir(cur.id))
        if (c) next.coverFile = c
      } else if (payload.imageUrl?.trim()) {
        const c = await saveCoverOptional(payload.imageUrl, serverDir(cur.id))
        next.coverFile = c
      }
      let reinstall = false
      if (payload.modpackId !== undefined) {
        try {
          const mid = resolveModpackId(payload.modpackId)
          if (mid !== cur.modpackId) {
            next.modpackId = mid
            reinstall = true
          }
        } catch {
          return { ok: false, error: 'Modpack inconnu.' }
        }
      }
      servers[idx] = next
      saveRegistry(servers)
      if (reinstall) {
        next.installState = 'installing'
        next.installError = undefined
        servers[idx] = next
        saveRegistry(servers)
        try {
          rmSync(serverDir(next.id), { recursive: true, force: true })
        } catch {
          /* ignore */
        }
        mkdirSync(serverDir(next.id), { recursive: true })
        void startInstall(next)
      } else if (payload.port !== undefined && next.installState === 'ready') {
        mergeServerPortAndDefaults(serverDir(next.id), next.port)
      }
      return { ok: true }
    }
  )

  ipcMain.handle('solea-server:delete', (_e, id: string) => {
    const all = loadRegistry()
    const servers = all.filter((s) => s.id !== id)
    if (servers.length === all.length) return { ok: false as const, error: 'Introuvable.' }
    const ch = processes.get(id)
    if (ch) {
      try {
        ch.kill('SIGKILL')
      } catch {
        /* ignore */
      }
      processes.delete(id)
    }
    if (runningServerId === id) runningServerId = null
    saveRegistry(servers)
    try {
      rmSync(serverDir(id), { recursive: true, force: true })
    } catch {
      /* ignore */
    }
    lineBuffers.delete(id)
    return { ok: true as const }
  })

  ipcMain.handle('solea-server:console', (_e, id: string) => {
    return lineBuffers.get(id) ?? []
  })

  ipcMain.handle(
    'solea-server:start',
    (_e, id: string): { ok: true } | { ok: false; error: string } => {
      const rec = loadRegistry().find((s) => s.id === id)
      if (!rec) return { ok: false, error: 'Introuvable.' }
      if (rec.installState !== 'ready') return { ok: false, error: "Le pack n'est pas prêt." }
      if (runningServerId && runningServerId !== id) {
        return { ok: false, error: 'Un autre serveur tourne déjà sur cette machine.' }
      }
      if (processes.has(id)) return { ok: false, error: 'Déjà démarré.' }
      if (!trySpawnServerProcess(rec)) {
        return { ok: false, error: 'Script serveur introuvable (run.bat / run.sh).' }
      }
      return { ok: true }
    }
  )

  ipcMain.handle('solea-server:stop', (_e, id: string) => {
    const ch = processes.get(id)
    if (!ch) return { ok: false as const, error: 'Pas en cours.' }
    try {
      ch.kill('SIGTERM')
    } catch {
      /* ignore */
    }
    return { ok: true as const }
  })

  ipcMain.handle('solea-server:kill', (_e, id: string) => {
    const ch = processes.get(id)
    if (!ch) return { ok: false as const, error: 'Pas en cours.' }
    try {
      ch.kill('SIGKILL')
    } catch {
      /* ignore */
    }
    return { ok: true as const }
  })

  ipcMain.handle('solea-server:restart', async (_e, id: string) => {
    const ch = processes.get(id)
    if (ch) {
      try {
        ch.kill('SIGTERM')
      } catch {
        /* ignore */
      }
      await new Promise((r) => setTimeout(r, 400))
    }
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec || rec.installState !== 'ready') return { ok: false as const, error: 'Pas prêt.' }
    if (runningServerId && runningServerId !== id) {
      return { ok: false as const, error: 'Un autre serveur tourne déjà.' }
    }
    if (!trySpawnServerProcess(rec)) {
      return { ok: false as const, error: 'Script serveur introuvable (run.bat / run.sh).' }
    }
    return { ok: true as const }
  })

  ipcMain.handle('solea-server:send-command', (_e, payload: { id: string; line: string }) => {
    const ch = processes.get(payload.id)
    if (!ch?.stdin) return { ok: false as const, error: 'Pas de processus.' }
    try {
      ch.stdin.write(`${payload.line}\n`)
      return { ok: true as const }
    } catch (e) {
      return { ok: false as const, error: e instanceof Error ? e.message : String(e) }
    }
  })

  ipcMain.handle('solea-server:check-modpack-update', async (_e, id: string) => {
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec) return { ok: false as const, error: 'Introuvable.' }
    const spec = getModpackSpec(rec.modpackId)
    const versions = await fetchProjectVersions(spec.projectSlug)
    const latest = pickLatestVersion(versions, spec.gameVersion, spec.loader)
    const installed = readInstalledMeta(serverDir(id))
    const currentId = installed?.versionId ?? rec.modrinthVersionId
    const hasUpdate = Boolean(latest && currentId && latest.id !== currentId)
    return {
      ok: true as const,
      hasUpdate,
      latestVersion: latest?.version_number,
      currentVersion: rec.modrinthVersionNumber ?? installed?.versionNumber
    }
  })

  ipcMain.handle('solea-server:get-public-ip', async () => {
    try {
      const res = await fetch('https://api.ipify.org?format=json', { signal: AbortSignal.timeout(8000) })
      if (!res.ok) return { ok: false as const, error: String(res.status) }
      const j = (await res.json()) as { ip?: string }
      return j.ip ? { ok: true as const, ip: j.ip } : { ok: false as const, error: 'empty' }
    } catch (e) {
      return { ok: false as const, error: e instanceof Error ? e.message : String(e) }
    }
  })

  ipcMain.handle('solea-server:probe-local-port', ( _e, port: number) => {
    return new Promise<{ ok: true; free: boolean; inUse: boolean }>((resolve) => {
      const p = Number(port)
      if (!Number.isFinite(p) || p < 1 || p > 65535) {
        resolve({ ok: true, free: false, inUse: true })
        return
      }
      const srv = createServer()
      srv.once('error', (err: NodeJS.ErrnoException) => {
        if (err.code === 'EADDRINUSE') resolve({ ok: true, free: false, inUse: true })
        else resolve({ ok: true, free: false, inUse: false })
      })
      srv.listen(p, '0.0.0.0', () => {
        srv.close(() => resolve({ ok: true, free: true, inUse: false }))
      })
    })
  })

  ipcMain.handle('solea-server:open-folder', async (event, id: string) => {
    const p = serverDir(id)
    if (!existsSync(p)) return { ok: false as const, error: 'Dossier absent.' }
    await shell.openPath(p)
    const w = BrowserWindow.fromWebContents(event.sender)
    if (w && !w.isDestroyed()) {
      w.focus()
    }
    return { ok: true as const }
  })

  ipcMain.handle('solea-server:world-get', (_e, id: string) => {
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec) return { ok: false as const, error: 'Introuvable.' }
    if (rec.installState !== 'ready') return { ok: false as const, error: "Le pack n'est pas prêt." }
    const root = serverDir(id)
    const m = readServerPropertiesMap(root)
    return {
      ok: true as const,
      levelName: m.get('level-name') ?? 'world',
      levelSeed: m.get('level-seed') ?? '',
      levelType: m.get('level-type') ?? 'minecraft:normal'
    }
  })

  ipcMain.handle(
    'solea-server:world-set',
    (
      _e,
      payload: { id: string; levelName?: string; levelSeed?: string; levelType?: string }
    ): { ok: true } | { ok: false; error: string } => {
      const rec = loadRegistry().find((s) => s.id === payload.id)
      if (!rec) return { ok: false, error: 'Introuvable.' }
      if (processes.has(payload.id)) {
        return { ok: false, error: 'Arrêtez le serveur avant de modifier le monde.' }
      }
      if (rec.installState !== 'ready') return { ok: false, error: "Le pack n'est pas prêt." }
      const root = serverDir(payload.id)
      if (!existsSync(join(root, 'server.properties'))) {
        return { ok: false, error: 'server.properties introuvable.' }
      }
      const m = readServerPropertiesMap(root)
      if (payload.levelName !== undefined) {
        const s = sanitizeWorldFolderName(payload.levelName)
        if (!s) {
          return {
            ok: false,
            error: 'Nom de monde invalide (lettres, chiffres, tirets et underscores, 1–64 caractères).'
          }
        }
        m.set('level-name', s)
      }
      if (payload.levelSeed !== undefined) {
        const seed = payload.levelSeed.trim()
        if (seed.length > 1024) return { ok: false, error: 'Seed trop longue.' }
        if (seed) m.set('level-seed', seed)
        else m.delete('level-seed')
      }
      if (payload.levelType !== undefined) {
        const t = payload.levelType.trim()
        const allowed = new Set([
          'minecraft:normal',
          'minecraft:flat',
          'minecraft:large_biomes',
          'minecraft:amplified'
        ])
        if (!allowed.has(t)) return { ok: false, error: 'Type de monde inconnu.' }
        m.set('level-type', t)
      }
      writeServerPropertiesMap(root, m)
      return { ok: true }
    }
  )

  ipcMain.handle('solea-server:world-delete', (_e, id: string) => {
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec) return { ok: false as const, error: 'Introuvable.' }
    if (processes.has(id)) {
      return { ok: false as const, error: 'Arrêtez le serveur avant de supprimer le monde.' }
    }
    if (rec.installState !== 'ready') return { ok: false as const, error: "Le pack n'est pas prêt." }
    const r = deleteWorldFolderForServer(serverDir(id))
    if (!r.ok) return { ok: false as const, error: r.error }
    pushLine(id, '[Solea] Dossier du monde supprimé. Au prochain démarrage un nouveau monde sera généré.')
    return { ok: true as const }
  })

  ipcMain.handle('solea-server:properties-raw-get', (_e, id: string) => {
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec) return { ok: false as const, error: 'Introuvable.' }
    if (rec.installState !== 'ready') return { ok: false as const, error: "Le pack n'est pas prêt." }
    const p = join(serverDir(id), 'server.properties')
    if (!existsSync(p)) return { ok: true as const, content: '' }
    try {
      return { ok: true as const, content: readFileSync(p, 'utf8') }
    } catch (e) {
      return { ok: false as const, error: e instanceof Error ? e.message : String(e) }
    }
  })

  ipcMain.handle(
    'solea-server:properties-raw-set',
    (_e, payload: { id: string; content: string }): { ok: true } | { ok: false; error: string } => {
      const rec = loadRegistry().find((s) => s.id === payload.id)
      if (!rec) return { ok: false, error: 'Introuvable.' }
      if (processes.has(payload.id)) {
        return { ok: false, error: 'Arrêtez le serveur avant de modifier server.properties.' }
      }
      if (rec.installState !== 'ready') return { ok: false, error: "Le pack n'est pas prêt." }
      const root = serverDir(payload.id)
      const p = join(root, 'server.properties')
      try {
        const normalized = payload.content.replace(/\r\n/g, '\n')
        writeFileSync(p, normalized, 'utf8')
        const portMatch = /^\s*server-port\s*=\s*(\d+)\s*$/im.exec(normalized)
        if (portMatch) {
          const portNum = parseInt(portMatch[1], 10)
          if (Number.isFinite(portNum) && portNum >= 1024 && portNum <= 65535) {
            const servers = loadRegistry()
            const idx = servers.findIndex((s) => s.id === payload.id)
            if (idx >= 0 && servers[idx].port !== portNum) {
              servers[idx] = { ...servers[idx], port: portNum }
              saveRegistry(servers)
            }
          }
        }
        return { ok: true }
      } catch (e) {
        return { ok: false, error: e instanceof Error ? e.message : String(e) }
      }
    }
  )

  ipcMain.handle('solea-server:jvm-extra-get', (_e, id: string) => {
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec) return { ok: false as const, error: 'Introuvable.' }
    if (rec.installState !== 'ready') return { ok: false as const, error: "Le pack n'est pas prêt." }
    const p = join(serverDir(id), SOLEA_JVM_EXTRA_FILE)
    if (!existsSync(p)) return { ok: true as const, content: '' }
    try {
      return { ok: true as const, content: readFileSync(p, 'utf8') }
    } catch (e) {
      return { ok: false as const, error: e instanceof Error ? e.message : String(e) }
    }
  })

  ipcMain.handle(
    'solea-server:jvm-extra-set',
    (_e, payload: { id: string; content: string }): { ok: true } | { ok: false; error: string } => {
      const rec = loadRegistry().find((s) => s.id === payload.id)
      if (!rec) return { ok: false, error: 'Introuvable.' }
      if (processes.has(payload.id)) {
        return { ok: false, error: 'Arrêtez le serveur avant de modifier les arguments JVM.' }
      }
      if (rec.installState !== 'ready') return { ok: false, error: "Le pack n'est pas prêt." }
      const normalized = payload.content.replace(/\r\n/g, '\n')
      if (normalized.length > 16384) {
        return { ok: false, error: 'Fichier JVM trop long (16 Ko max).' }
      }
      const root = serverDir(payload.id)
      const p = join(root, SOLEA_JVM_EXTRA_FILE)
      try {
        writeFileSync(p, normalized, 'utf8')
        return { ok: true }
      } catch (e) {
        return { ok: false, error: e instanceof Error ? e.message : String(e) }
      }
    }
  )

  ipcMain.handle('solea-server:reinstall-latest', async (_e, id: string) => {
    if (installBusy) return { ok: false as const, error: 'Une installation est déjà en cours.' }
    const rec = loadRegistry().find((s) => s.id === id)
    if (!rec) return { ok: false as const, error: 'Introuvable.' }
    if (processes.has(id)) return { ok: false as const, error: 'Arrêtez le serveur avant de réinstaller.' }
    const next: SoleServerRecord = {
      ...rec,
      installState: 'installing',
      installError: undefined
    }
    const servers = loadRegistry().map((s) => (s.id === id ? next : s))
    saveRegistry(servers)
    try {
      rmSync(serverDir(id), { recursive: true, force: true })
    } catch {
      /* ignore */
    }
    mkdirSync(serverDir(id), { recursive: true })
    void startInstall(next)
    return { ok: true as const }
  })
}
