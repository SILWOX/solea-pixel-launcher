import { app, BrowserWindow, dialog, ipcMain, nativeImage, shell } from 'electron'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import { Worker } from 'node:worker_threads'
import os from 'node:os'
import { existsSync, readFileSync, writeFileSync, rmSync, mkdirSync, readdirSync, statSync } from 'fs'
import { createRequire } from 'module'
import type { MicrosoftAuthResponse } from 'minecraft-java-core'

const mainDir = dirname(fileURLToPath(import.meta.url))
const projectRoot = join(mainDir, '..', '..')

if (process.env.SOLEA_TEST_MODE === '1') {
  app.setPath('userData', join(projectRoot, 'test', 'electron-user-data'))
}

const requireMjc = createRequire(import.meta.url)
const { Microsoft } = requireMjc('minecraft-java-core') as {
  Microsoft: new (clientId?: string, redirectUri?: string) => {
    getAuth: (type?: string) => Promise<unknown>
    refresh: (acc: MicrosoftAuthResponse) => Promise<unknown>
  }
}

import {
  installMrpackFromModrinth,
  verifyInstanceIntegrity,
  getModpackActionInfo,
  type InstallProgress,
  type IntegrityResult
} from './modrinth.js'
import { isMinecraftRunning, killMinecraftForInstance } from './gameProcess.js'
import { SKIP_MOD_INTEGRITY_FOR_LAUNCH } from './config.js'
import {
  loadSettings,
  saveSettings,
  parseArgsBlock,
  getEffectiveMicrosoftClientId,
  getGameSettingsForModpack,
  DEFAULT_SETTINGS,
  type LauncherSettings
} from './settings.js'
import {
  addOrUpdateAccount,
  getActiveAccount,
  hasAnyAccount,
  listAccountSummaries,
  removeAccount,
  setActiveUuid,
  updateAccountTokens,
  findStoredAccountByUuid
} from './accounts.js'
import {
  fetchMinecraftHeadDataUrl,
  fetchMinecraftSkinDataUrl,
  fetchMinecraftCapeDataUrl,
  headDataUrlFromStoredAccount
} from './skinHead.js'
import {
  activeCapeDataUrlFromToken,
  activeCapeIdFromRows,
  listProfileCapesWithTextures,
  setProfileActiveCape
} from './minecraftProfileCapes.js'
import type { SkinModel } from './skinPresets.js'
import { uploadMinecraftProfileSkin, resetMinecraftProfileSkin } from './minecraftProfileSkin.js'
import {
  clearAccountSkinStorage,
  deletePreset,
  getActivePresetSkinForSync,
  getPresetsState,
  getPreviewBundle,
  importPresetFromFile,
  setActivePreset,
  updatePresetModel
} from './skinPresets.js'
import {
  getModpackSpec,
  listModpackSummaries,
  modrinthModpackPageUrl,
  MODPACKS,
  resolveModpackId,
  type ModpackId
} from './modpacks.js'
import {
  getModpackActivityMap,
  recordModpackLastInstall,
  recordModpackLastPlay
} from './modpackActivity.js'
import { getDebugSnapshot } from './debugSnapshot.js'
import { setupAutoUpdater, checkForUpdatesManual, quitAndInstall, downloadUpdate } from './updater.js'
import {
  initDiscordRpcIfNeeded,
  reconnectDiscordRpcIfNeeded,
  setInGamePresence,
  setMenuPresence,
  clearDiscordPresence,
  shutdownDiscordRpc,
  type RichPresencePack
} from './discordRpc.js'
import { isUrlAllowedForExternalOpen } from './safeOpenExternal.js'
import { directorySizeAsync, directorySizeSync, rmDirContentsIfExists } from './diskUsage.js'
import { showNativeNotification } from './notifications.js'
import { logMain } from './logger.js'
import { errWithCode, SPX } from './supportCodes.js'

function getActiveModpackSpec() {
  return getModpackSpec(resolveModpackId(loadSettings().activeModpackId))
}

function getInstanceRoot(): string {
  return join(app.getPath('userData'), 'instances', getActiveModpackSpec().projectSlug)
}

function getInstanceRootForModpack(modpackId: ModpackId): string {
  const spec = getModpackSpec(modpackId)
  return join(app.getPath('userData'), 'instances', spec.projectSlug)
}

/** True si au moins une instance Solea a un processus Minecraft lié à son dossier. */
function isAnySoleaMinecraftRunning(): boolean {
  for (const m of MODPACKS) {
    if (isMinecraftRunning(getInstanceRootForModpack(m.id))) return true
  }
  return false
}

/** Arrête tous les Minecraft détectés pour les dossiers d’instances Solea (un seul jeu à la fois). */
function killAllSoleaMinecraftInstances(): void {
  for (const m of MODPACKS) {
    killMinecraftForInstance(getInstanceRootForModpack(m.id))
  }
}

function isSoleaInstanceInstalled(instanceRoot: string): boolean {
  return existsSync(join(instanceRoot, '.solea-installed.json'))
}

function pushInstallDoneNotification(): void {
  const st = loadSettings()
  if (!st.nativeNotifications) return
  showNativeNotification(
    'Solea Pixel',
    st.uiLanguage === 'fr' ? 'Installation du modpack terminée.' : 'Modpack installation finished.'
  )
}

function discordPresenceForActivePack(): RichPresencePack {
  const st = loadSettings()
  const spec = getActiveModpackSpec()
  return {
    modpackName: spec.displayName,
    largeImageKey: spec.discordLargeImageKey ?? 'logo',
    locale: st.uiLanguage,
    modrinthPackUrl: modrinthModpackPageUrl(spec)
  }
}

/** Installs Modrinth mrpack into a given instance folder (used for active + per-id reinstall). */
async function runModpackInstallForSpec(
  spec: ReturnType<typeof getModpackSpec>,
  instanceRoot: string
): Promise<void> {
  const st = loadSettings()
  const downloadConcurrency = st.networkSlowDownloads
    ? Math.min(2, Math.max(1, st.downloadThreads))
    : Math.max(1, Math.min(32, st.downloadThreads))
  await installMrpackFromModrinth({
    projectSlug: spec.projectSlug,
    gameVersion: spec.gameVersion,
    loader: spec.loader,
    instanceRoot,
    downloadConcurrency,
    onProgress: sendProgress
  })
}

function normalizeForgeLoaderBuild(
  gameVersion: string,
  loaderType: string,
  loaderBuild: string
): string {
  if (loaderType !== 'forge' || !loaderBuild) return loaderBuild
  if (loaderBuild.includes('-')) return loaderBuild
  return `${gameVersion}-${loaderBuild}`
}

function resolveLaunchJavaVersion(
  settings: LauncherSettings,
  spec: ReturnType<typeof getModpackSpec>
): string {
  const raw = settings.javaVersion?.trim() || spec.recommendedJava
  const majorMatch = /^(\d+)/.exec(raw)
  const major = majorMatch ? parseInt(majorMatch[1], 10) : NaN
  if (spec.loader === 'forge' && spec.gameVersion.startsWith('1.20.') && !Number.isNaN(major) && major > 17) {
    return '17'
  }
  return raw
}

/** Icône fenêtre / barre des tâches (logo Solea). Sous Windows, un .ico multi-tailles évite l’ancienne icône PNG mal prise en charge par la barre des tâches. */
function getAppIconPath(): string | undefined {
  if (app.isPackaged) {
    if (process.platform === 'win32') {
      const ico = join(process.resourcesPath, 'app-icon.ico')
      if (existsSync(ico)) return ico
    }
    const extra = join(process.resourcesPath, 'app-icon.png')
    if (existsSync(extra)) return extra
  }
  if (process.platform === 'win32') {
    const devIco = join(projectRoot, 'build', 'icon.ico')
    if (existsSync(devIco)) return devIco
  }
  const candidates = [
    join(projectRoot, 'src', 'renderer', 'src', 'assets', 'branding', 'logo.png'),
    join(projectRoot, 'resources', 'app-icon.png')
  ]
  for (const p of candidates) {
    if (existsSync(p)) return p
  }
  return undefined
}

function readAppVersion(): string {
  try {
    const p = join(projectRoot, 'package.json')
    if (!existsSync(p)) return '1.0.0'
    const pkg = JSON.parse(readFileSync(p, 'utf8')) as { version?: string }
    return pkg.version ?? '1.0.0'
  } catch {
    return '1.0.0'
  }
}

function isAuthError(x: unknown): x is { error: string } {
  return typeof x === 'object' && x !== null && 'error' in x
}

const LAUNCH_LOG_SENTINEL = 'Launching with arguments'

function getGameLaunchWorkerPath(): string {
  return join(dirname(fileURLToPath(import.meta.url)), 'gameLaunchWorker.js')
}

/** Lance Minecraft dans un worker pour garder le processus principal réactif (Windows « Ne répond pas »). */
function runMinecraftLaunchInWorker(launchOpts: Record<string, unknown>): Promise<void> {
  let clone: Record<string, unknown>
  try {
    clone = structuredClone(launchOpts)
  } catch {
    clone = JSON.parse(JSON.stringify(launchOpts)) as Record<string, unknown>
  }

  return new Promise<void>((resolve, reject) => {
    const workerPath = getGameLaunchWorkerPath()
    let settled = false
    const w = new Worker(workerPath, { workerData: { launchOpts: clone } })

    const timer = setTimeout(() => {
      if (settled) return
      settled = true
      void w.terminate().catch(() => {})
      reject(new Error('Délai de lancement dépassé (téléchargements trop longs ou blocage).'))
    }, 45 * 60 * 1000)

    const finishOk = () => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      resolve()
    }
    const finishErr = (e: Error) => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      void w.terminate().catch(() => {})
      reject(e)
    }

    const onGameExitedUi = () => {
      mainWindow?.webContents.send('game-exited')
      const st = loadSettings()
      if (st.discordRichPresence) {
        void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
      } else {
        void clearDiscordPresence()
      }
    }

    w.on('message', (msg: { type: string; line?: string; message?: string }) => {
      if (msg.type === 'data' && typeof msg.line === 'string') {
        appendGameLogLine(msg.line)
        if (msg.line.includes(LAUNCH_LOG_SENTINEL)) finishOk()
      }
      if (msg.type === 'error' && typeof msg.message === 'string') {
        appendGameLogLine(`[erreur] ${msg.message}`)
        finishErr(normalizeLaunchError(new Error(msg.message)))
      }
      if (msg.type === 'close') {
        onGameExitedUi()
      }
    })
    w.on('error', (err) => finishErr(normalizeLaunchError(err)))
    w.on('exit', (code) => {
      if (settled) return
      if (code !== 0) {
        finishErr(new Error(`Processus de lancement arrêté (code ${code}).`))
      }
    })
  })
}

function normalizeLaunchError(err: unknown): Error {
  if (err instanceof Error) return err
  if (typeof err === 'object' && err !== null && 'error' in err) {
    const e = (err as { error: unknown }).error
    return new Error(typeof e === 'string' ? e : JSON.stringify(e))
  }
  return new Error(String(err))
}

function makeMicrosoft(): InstanceType<typeof Microsoft> {
  const settings = loadSettings()
  const id = getEffectiveMicrosoftClientId(settings)
  return id ? new Microsoft(id) : new Microsoft('')
}

function sameMinecraftUuid(a: string, b: string): boolean {
  return a.replace(/-/g, '').toLowerCase() === b.replace(/-/g, '').toLowerCase()
}

async function refreshActiveMicrosoftAccount(): Promise<MicrosoftAuthResponse | null> {
  const acc = getActiveAccount()
  if (!acc) return null
  const ms = makeMicrosoft()
  const r = await ms.refresh(acc)
  if (isAuthError(r)) return null
  const next = r as MicrosoftAuthResponse
  updateAccountTokens(next)
  return next
}

/**
 * Pousse le preset actif (ou reset Mojang) vers le profil Minecraft du compte connecté.
 * No-op si le compte affiché n’est pas le compte actif du launcher.
 */
async function syncActivePresetToMojangProfile(accountUuid: string): Promise<string | null> {
  const acc = getActiveAccount()
  if (!acc?.refresh_token || !sameMinecraftUuid(acc.uuid, accountUuid)) return null

  const ms = makeMicrosoft()
  const refreshed = await ms.refresh(acc)
  if (isAuthError(refreshed)) {
    return `Session Microsoft : ${refreshed.error}`
  }
  const next = refreshed as MicrosoftAuthResponse
  updateAccountTokens(next)
  const token = next.access_token

  const skin = getActivePresetSkinForSync(accountUuid)
  if (skin.kind === 'missing') {
    return 'Fichier du preset introuvable. Réimporte le skin.'
  }
  if (skin.kind === 'mojang') {
    const r = await resetMinecraftProfileSkin(token)
    return r.ok ? null : r.error
  }
  const r = await uploadMinecraftProfileSkin(token, skin.buffer, skin.model)
  return r.ok ? null : r.error
}

async function getCapeDataUrlForPreview(accountUuid: string): Promise<string | null> {
  const u = accountUuid.trim()
  const acc = getActiveAccount()
  if (!acc || !sameMinecraftUuid(acc.uuid, u)) {
    return fetchMinecraftCapeDataUrl(u)
  }
  const fresh = await refreshActiveMicrosoftAccount()
  if (!fresh) {
    return fetchMinecraftCapeDataUrl(u)
  }
  const fromProfile = await activeCapeDataUrlFromToken(fresh.access_token)
  if (fromProfile) return fromProfile
  return null
}

/** Titre OS (barre des tâches, Alt+Tab) — identique au bandeau custom ; ne pas le faire changer au chargement. */
const MAIN_WINDOW_TITLE = 'SOLEA PIXEL LAUNCHER'

let mainWindow: BrowserWindow | null = null
let discordPresenceRetryInterval: ReturnType<typeof setInterval> | null = null

/** Logs du dernier lancement — fenêtre console + buffer pour rechargement. */
const MAX_GAME_LOG_BYTES = 1_200_000
let gameLogBuffer = ''
let logConsoleWindow: BrowserWindow | null = null
let debugWindow: BrowserWindow | null = null

function clearGameLogBuffer(): void {
  gameLogBuffer = ''
}

function appendGameLogLine(line: string): void {
  const s = String(line)
  gameLogBuffer += s
  if (gameLogBuffer.length > MAX_GAME_LOG_BYTES) {
    gameLogBuffer = gameLogBuffer.slice(-MAX_GAME_LOG_BYTES)
  }
  for (const w of BrowserWindow.getAllWindows()) {
    if (w.isDestroyed()) continue
    try {
      w.webContents.send('game-log', s)
    } catch {
      /* fenêtre fermée */
    }
  }
}

function broadcastMaximizedState(): void {
  if (!mainWindow?.webContents) return
  try {
    mainWindow.webContents.send('window-maximized', mainWindow.isMaximized())
  } catch {
    /* fenêtre fermée */
  }
}

function createWindow(): void {
  const settings = loadSettings()
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined

  mainWindow = new BrowserWindow({
    width: 1280,
    height: 760,
    minWidth: 960,
    minHeight: 600,
    show: false,
    frame: false,
    title: MAIN_WINDOW_TITLE,
    backgroundColor: '#07050c',
    autoHideMenuBar: true,
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })

  // Sous Windows, l’icône barre des tâches ne suit pas toujours le constructeur ; setIcon après création aide.
  if (process.platform === 'win32' && icon && !icon.isEmpty()) {
    mainWindow.setIcon(icon)
  }

  mainWindow.webContents.on('page-title-updated', (event) => {
    event.preventDefault()
  })
  mainWindow.setTitle(MAIN_WINDOW_TITLE)

  const gameWin = getGameSettingsForModpack(settings, settings.activeModpackId)
  if (gameWin.screenWidth && gameWin.screenHeight) {
    mainWindow.setSize(gameWin.screenWidth, gameWin.screenHeight)
  }

  mainWindow.on('maximize', broadcastMaximizedState)
  mainWindow.on('unmaximize', broadcastMaximizedState)
  mainWindow.webContents.on('did-finish-load', broadcastMaximizedState)

  mainWindow.on('ready-to-show', () => {
    if (process.platform === 'win32' && icon && !icon.isEmpty()) {
      mainWindow?.setIcon(icon)
    }
    mainWindow?.show()
  })

  if (process.env.ELECTRON_RENDERER_URL) {
    mainWindow.loadURL(process.env.ELECTRON_RENDERER_URL)
  } else {
    mainWindow.loadFile(join(mainDir, '../renderer/index.html'))
  }

  mainWindow.webContents.once('did-finish-load', () => {
    mainWindow?.setTitle(MAIN_WINDOW_TITLE)
    const st = loadSettings()
    if (st.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
    }
  })

  let discordFocusTimer: ReturnType<typeof setTimeout> | null = null
  mainWindow.on('focus', () => {
    if (!loadSettings().discordRichPresence) return
    if (discordFocusTimer) clearTimeout(discordFocusTimer)
    discordFocusTimer = setTimeout(() => {
      discordFocusTimer = null
      void reconnectDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
    }, 500)
  })
}

function sendProgress(p: InstallProgress): void {
  mainWindow?.webContents.send('install-progress', p)
}

app.whenReady().then(() => {
  logMain('info', 'Application ready', { version: readAppVersion() })
  // Même AUMID que l’app installée en dev → Windows réutilise l’icône du raccourci / cache (souvent l’ancien visuel).
  if (process.platform === 'win32') {
    app.setAppUserModelId(app.isPackaged ? 'fr.solea.pixel.launcher' : 'fr.solea.pixel.launcher.dev')
  }
  createWindow()
  setupAutoUpdater(mainWindow, loadSettings().updateChannel)

  discordPresenceRetryInterval = setInterval(() => {
    if (!loadSettings().discordRichPresence) return
    void reconnectDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
  }, 30_000)

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('before-quit', () => {
  if (discordPresenceRetryInterval) {
    clearInterval(discordPresenceRetryInterval)
    discordPresenceRetryInterval = null
  }
  shutdownDiscordRpc()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

ipcMain.handle('window:minimize', () => {
  mainWindow?.minimize()
})

ipcMain.handle('window:toggle-maximize', () => {
  if (!mainWindow) return { maximized: false }
  if (mainWindow.isMaximized()) mainWindow.unmaximize()
  else mainWindow.maximize()
  return { maximized: mainWindow.isMaximized() }
})

ipcMain.handle('window:close', () => {
  mainWindow?.close()
})

ipcMain.handle('window:is-maximized', () => mainWindow?.isMaximized() ?? false)

ipcMain.handle('app:get-version', () => app.getVersion())

ipcMain.handle('app:get-memory-stats', () => {
  const totalBytes = os.totalmem()
  const totalGiB = totalBytes / (1024 * 1024 * 1024)
  return { totalBytes, totalGiB }
})

ipcMain.handle('updater:check', () => {
  const started = checkForUpdatesManual()
  return { ok: true as const, started }
})

ipcMain.handle('updater:download', async () => {
  try {
    await downloadUpdate()
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

ipcMain.handle('updater:quit-and-install', () => {
  quitAndInstall()
  return { ok: true as const }
})

ipcMain.handle('skin:get-head', async (_e, uuid: string, size?: number) => {
  if (typeof uuid !== 'string' || !uuid.trim()) return null
  const n = typeof size === 'number' && Number.isFinite(size) ? size : 64
  const trimmed = uuid.trim()
  /** Crafatar / mc-heads d’abord : rendu « tête » net comme l’UI d’avant (pas un crop 8×8 étiré). */
  const fromNet = await fetchMinecraftHeadDataUrl(trimmed, n)
  if (fromNet) return fromNet
  const acc = findStoredAccountByUuid(trimmed)
  return headDataUrlFromStoredAccount(acc, n)
})

ipcMain.handle('skin:get-preview', async (_e, uuid: string) => {
  if (typeof uuid !== 'string' || !uuid.trim()) return null
  const u = uuid.trim()
  const bundle = await getPreviewBundle(u, fetchMinecraftSkinDataUrl, getCapeDataUrlForPreview)
  if (!bundle) return null
  const source = bundle.kind === 'preset' ? ('local' as const) : ('remote' as const)
  return {
    source,
    dataUrl: bundle.dataUrl,
    model: bundle.model,
    capeDataUrl: bundle.capeDataUrl
  }
})

ipcMain.handle('skin:presets-state', (_e, uuid: string) => {
  if (typeof uuid !== 'string' || !uuid.trim()) return null
  return getPresetsState(uuid.trim())
})

ipcMain.handle('skin:set-active-preset', async (_e, uuid: string, presetId: string | null) => {
  if (typeof uuid !== 'string' || !uuid.trim()) return { ok: false as const, error: 'UUID invalide.' }
  if (presetId !== null && typeof presetId !== 'string') {
    return { ok: false as const, error: 'Preset invalide.' }
  }
  const sid = uuid.trim()
  const before = getPresetsState(sid).activePresetId
  setActivePreset(sid, presetId)
  if (before === presetId) return { ok: true as const }
  const skinSyncError = (await syncActivePresetToMojangProfile(sid)) ?? undefined
  return skinSyncError
    ? { ok: true as const, skinSyncError }
    : { ok: true as const }
})

ipcMain.handle('skin:delete-preset', async (_e, uuid: string, presetId: string) => {
  if (typeof uuid !== 'string' || !uuid.trim() || typeof presetId !== 'string') {
    return { ok: false as const, error: 'Paramètres invalides.' }
  }
  const sid = uuid.trim()
  const wasActive = getPresetsState(sid).activePresetId === presetId
  deletePreset(sid, presetId)
  if (!wasActive) return { ok: true as const }
  const skinSyncError = (await syncActivePresetToMojangProfile(sid)) ?? undefined
  return skinSyncError
    ? { ok: true as const, skinSyncError }
    : { ok: true as const }
})

ipcMain.handle('skin:update-preset-model', async (_e, uuid: string, presetId: string, model: SkinModel) => {
  if (typeof uuid !== 'string' || !uuid.trim() || typeof presetId !== 'string') {
    return { ok: false as const, error: 'Paramètres invalides.' }
  }
  if (model !== 'slim' && model !== 'default') {
    return { ok: false as const, error: 'Modèle invalide.' }
  }
  const sid = uuid.trim()
  const wasActive = getPresetsState(sid).activePresetId === presetId
  updatePresetModel(sid, presetId, model)
  if (!wasActive) return { ok: true as const }
  const skinSyncError = (await syncActivePresetToMojangProfile(sid)) ?? undefined
  return skinSyncError
    ? { ok: true as const, skinSyncError }
    : { ok: true as const }
})

ipcMain.handle(
  'skin:import-preset',
  async (_e, model: SkinModel, displayName: string) => {
    const acc = getActiveAccount()
    if (!acc) return { ok: false as const, error: 'Aucun compte actif.' }
    if (model !== 'slim' && model !== 'default') {
      return { ok: false as const, error: 'Modèle invalide.' }
    }
    const win = mainWindow ?? BrowserWindow.getFocusedWindow()
    const { canceled, filePaths } = await dialog.showOpenDialog(win ?? undefined, {
      title: 'Importer un skin',
      filters: [{ name: 'PNG', extensions: ['png'] }],
      properties: ['openFile']
    })
    if (canceled || !filePaths[0]) return { ok: false as const, error: 'Annulé.' }
    const r = importPresetFromFile(acc.uuid, filePaths[0]!, model, typeof displayName === 'string' ? displayName : '')
    if (!r.ok) return r
    const skinSyncError = (await syncActivePresetToMojangProfile(acc.uuid)) ?? undefined
    return skinSyncError ? { ...r, skinSyncError } : r
  }
)

ipcMain.handle('profile:list-capes', async () => {
  const na = await refreshActiveMicrosoftAccount()
  if (!na) {
    return {
      ok: false as const,
      error: 'Impossible de joindre le compte Microsoft. Rafraîchis la session ou reconnecte-toi.'
    }
  }
  const r = await listProfileCapesWithTextures(na.access_token)
  if (!r.ok) return { ok: false as const, error: r.error }
  return {
    ok: true as const,
    capes: r.capes,
    activeCapeId: activeCapeIdFromRows(r.capes)
  }
})

ipcMain.handle('profile:set-active-cape', async (_e, capeId: string | null) => {
  if (capeId !== null && typeof capeId !== 'string') {
    return { ok: false as const, error: 'Identifiant de cape invalide.' }
  }
  const na = await refreshActiveMicrosoftAccount()
  if (!na) {
    return { ok: false as const, error: 'Session expirée. Réessaie ou reconnecte-toi.' }
  }
  return setProfileActiveCape(na.access_token, capeId)
})

ipcMain.handle('app:get-paths', () => {
  const spec = getActiveModpackSpec()
  const homeLinks: { modrinthUrl: string; discordUrl?: string } = {
    modrinthUrl: modrinthModpackPageUrl(spec)
  }
  if (spec.discordUrl?.trim()) homeLinks.discordUrl = spec.discordUrl.trim()
  return {
    userData: app.getPath('userData'),
    instanceRoot: getInstanceRoot(),
    testMode: process.env.SOLEA_TEST_MODE === '1',
    projectRoot,
    version: readAppVersion(),
    modpackDisplayName: spec.displayName,
    activeModpackId: spec.id,
    modpacks: listModpackSummaries(),
    homeLinks
  }
})

ipcMain.handle('modpack:activity-get', () => getModpackActivityMap())

ipcMain.handle('auth:get-state', () => ({
  /** true = aucun compte : l’UI doit afficher uniquement la connexion Microsoft */
  requiresMicrosoftLogin: !hasAnyAccount()
}))

ipcMain.handle('settings:get', (): LauncherSettings => loadSettings())

ipcMain.handle('settings:save', (_e, partial: Partial<LauncherSettings>) => {
  const cur = loadSettings()
  const next: LauncherSettings = { ...cur, ...partial }
  const r = saveSettings(next)
  if (r.ok) {
    if (next.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
    } else {
      void clearDiscordPresence()
      shutdownDiscordRpc()
    }
  }
  return r
})

ipcMain.handle('settings:reset', () => {
  const r = saveSettings({ ...DEFAULT_SETTINGS })
  if (r.ok) {
    const st = loadSettings()
    if (st.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
    } else {
      void clearDiscordPresence()
      shutdownDiscordRpc()
    }
  }
  return r
})

ipcMain.handle('auth:list-accounts', () => listAccountSummaries())

ipcMain.handle('auth:get-active', () => {
  const acc = getActiveAccount()
  if (!acc) return null
  return { name: acc.name, uuid: acc.uuid }
})

ipcMain.handle('auth:add-account', async () => {
  const ms = makeMicrosoft()
  const result = await ms.getAuth('electron')
  if (result === false) return { ok: false as const, reason: 'cancelled' }
  if (isAuthError(result)) return { ok: false as const, reason: 'error', detail: result.error }
  addOrUpdateAccount(result as MicrosoftAuthResponse)
  const r = result as MicrosoftAuthResponse
  return { ok: true as const, name: r.name, uuid: r.uuid }
})

ipcMain.handle('auth:set-active', (_e, uuid: string) => {
  if (typeof uuid !== 'string') return { ok: false as const, error: 'UUID invalide.' }
  return setActiveUuid(uuid)
})

ipcMain.handle('auth:remove-account', (_e, uuid: string) => {
  if (typeof uuid !== 'string') return { ok: false as const }
  removeAccount(uuid)
  clearAccountSkinStorage(uuid)
  return { ok: true as const }
})

ipcMain.handle('auth:refresh-active', async () => {
  const acc = getActiveAccount()
  if (!acc?.refresh_token) {
    return { ok: false as const, error: 'Aucun compte actif.' }
  }
  const ms = makeMicrosoft()
  const refreshed = await ms.refresh(acc)
  if (isAuthError(refreshed)) {
    return { ok: false as const, error: refreshed.error }
  }
  updateAccountTokens(refreshed as MicrosoftAuthResponse)
  return { ok: true as const, name: (refreshed as MicrosoftAuthResponse).name }
})

ipcMain.handle('modpack:set-active', (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'Modpack invalide.' }
  const resolved = resolveModpackId(id)
  const r = saveSettings({ ...loadSettings(), activeModpackId: resolved })
  if (!r.ok) return { ok: false as const, error: r.error }
  const st = loadSettings()
  if (st.discordRichPresence) {
    void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
  }
  return { ok: true as const, activeModpackId: resolved }
})

ipcMain.handle('modpack:install', async () => {
  const spec = getActiveModpackSpec()
  const root = getInstanceRoot()
  try {
    await runModpackInstallForSpec(spec, root)
    recordModpackLastInstall(spec.id)
    pushInstallDoneNotification()
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

ipcMain.handle('modpack:reinstall', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'Modpack invalide.' }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  if (isMinecraftRunning(root)) {
    return {
      ok: false as const,
      error: 'Fermez Minecraft pour ce modpack avant de réinstaller.'
    }
  }
  const spec = getModpackSpec(resolved)
  try {
    if (existsSync(root)) rmSync(root, { recursive: true, force: true })
    mkdirSync(root, { recursive: true })
    await runModpackInstallForSpec(spec, root)
    recordModpackLastInstall(resolved)
    pushInstallDoneNotification()
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

ipcMain.handle('modpack:uninstall', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'Modpack invalide.' }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  if (isMinecraftRunning(root)) {
    return {
      ok: false as const,
      error: 'Fermez Minecraft pour ce modpack avant de désinstaller.'
    }
  }
  try {
    if (existsSync(root)) rmSync(root, { recursive: true, force: true })
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

ipcMain.handle('modpack:verify', async () => {
  const r = await verifyInstanceIntegrity(getInstanceRoot())
  return r
})

ipcMain.handle('modpack:action-info', async () => {
  const spec = getActiveModpackSpec()
  try {
    return await getModpackActionInfo({
      instanceRoot: getInstanceRoot(),
      projectSlug: spec.projectSlug,
      gameVersion: spec.gameVersion,
      loader: spec.loader
    })
  } catch (e) {
    return {
      needsInstall: true,
      needsUpdate: false,
      error: e instanceof Error ? e.message : String(e)
    }
  }
})

ipcMain.handle('modpack:all-action-info', async () => {
  const packs: {
    id: ModpackId
    displayName: string
    needsInstall: boolean
    needsUpdate: boolean
    error?: string
    installedVersionNumber?: string
    latestVersionNumber?: string
  }[] = []
  for (const m of MODPACKS) {
    try {
      const root = getInstanceRootForModpack(m.id)
      const info = await getModpackActionInfo({
        instanceRoot: root,
        projectSlug: m.projectSlug,
        gameVersion: m.gameVersion,
        loader: m.loader
      })
      packs.push({
        id: m.id,
        displayName: m.displayName,
        needsInstall: info.needsInstall,
        needsUpdate: info.needsUpdate,
        error: info.error,
        installedVersionNumber: info.installedVersionNumber,
        latestVersionNumber: info.latestVersionNumber
      })
    } catch (e) {
      packs.push({
        id: m.id,
        displayName: m.displayName,
        needsInstall: true,
        needsUpdate: false,
        error: e instanceof Error ? e.message : String(e)
      })
    }
  }
  return { packs }
})

ipcMain.handle('game:is-running', () => isAnySoleaMinecraftRunning())

ipcMain.handle('game:stop', () => {
  killAllSoleaMinecraftInstances()
  return { ok: true as const }
})

ipcMain.handle('game-log:get-snapshot', () => gameLogBuffer)

ipcMain.handle('game-log-window:open', () => {
  if (logConsoleWindow && !logConsoleWindow.isDestroyed()) {
    logConsoleWindow.focus()
    return { ok: true as const }
  }
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined
  logConsoleWindow = new BrowserWindow({
    width: 760,
    height: 520,
    minWidth: 440,
    minHeight: 280,
    title: 'Console',
    backgroundColor: '#0c0c0c',
    autoHideMenuBar: true,
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })
  logConsoleWindow.on('closed', () => {
    logConsoleWindow = null
  })
  if (process.env.ELECTRON_RENDERER_URL) {
    const base = process.env.ELECTRON_RENDERER_URL.replace(/\/$/, '')
    void logConsoleWindow.loadURL(`${base}/index.html?solea=log`)
  } else {
    void logConsoleWindow.loadFile(join(mainDir, '../renderer/index.html'), { query: { solea: 'log' } })
  }
  return { ok: true as const }
})

ipcMain.handle('debug-window:open', () => {
  if (debugWindow && !debugWindow.isDestroyed()) {
    debugWindow.focus()
    return { ok: true as const }
  }
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined
  debugWindow = new BrowserWindow({
    width: 980,
    height: 760,
    minWidth: 560,
    minHeight: 480,
    title: 'Solea — Debug',
    backgroundColor: '#0f0f12',
    autoHideMenuBar: true,
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })
  debugWindow.on('closed', () => {
    debugWindow = null
  })
  if (process.env.ELECTRON_RENDERER_URL) {
    const base = process.env.ELECTRON_RENDERER_URL.replace(/\/$/, '')
    void debugWindow.loadURL(`${base}/index.html?solea=debug`)
  } else {
    void debugWindow.loadFile(join(mainDir, '../renderer/index.html'), { query: { solea: 'debug' } })
  }
  return { ok: true as const }
})

ipcMain.handle('debug:get-snapshot', () =>
  getDebugSnapshot(getInstanceRoot, isAnySoleaMinecraftRunning)
)

ipcMain.handle('debug:reload-main', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.reload()
    return { ok: true as const }
  }
  return { ok: false as const, error: 'Main window missing.' }
})

ipcMain.handle('debug:open-known-folder', async (_e, kind: unknown) => {
  if (kind === 'userData') {
    const p = app.getPath('userData')
    const err = await shell.openPath(p)
    if (err) return { ok: false as const, error: err }
    return { ok: true as const }
  }
  if (kind === 'instanceRoot') {
    const root = getInstanceRoot()
    const err = await shell.openPath(root)
    if (err) return { ok: false as const, error: err }
    return { ok: true as const }
  }
  return { ok: false as const, error: 'Invalid folder kind.' }
})

ipcMain.handle('game:launch', async () => {
  clearGameLogBuffer()
  const settings = loadSettings()
  const spec = getActiveModpackSpec()
  const root = getInstanceRoot()

  for (const m of MODPACKS) {
    const instRoot = getInstanceRootForModpack(m.id)
    if (!isMinecraftRunning(instRoot)) continue
    const fr = settings.uiLanguage === 'fr'
    if (instRoot === root) {
      return {
        ok: false as const,
        error: errWithCode(
          SPX.LAUNCH_ALREADY,
          fr
            ? 'Minecraft est déjà lancé pour ce modpack. Fermez le jeu avant de relancer.'
            : 'Minecraft is already running for this modpack. Close the game before launching again.'
        )
      }
    }
    return {
      ok: false as const,
      error: errWithCode(
        SPX.LAUNCH_BUSY_OTHER,
        fr
          ? `Le modpack « ${m.displayName} » est déjà en cours d’exécution. Fermez Minecraft avant d’en lancer un autre.`
          : `"${m.displayName}" is already running. Close Minecraft before launching another modpack.`
      )
    }
  }

  let acc = getActiveAccount()
  if (!acc?.refresh_token) {
    return {
      ok: false as const,
      error: errWithCode(
        SPX.LAUNCH_AUTH,
        'Sélectionnez un compte Microsoft avec Minecraft Java.'
      )
    }
  }

  const verifyP: Promise<IntegrityResult> = SKIP_MOD_INTEGRITY_FOR_LAUNCH
    ? Promise.resolve({ ok: true })
    : verifyInstanceIntegrity(root)

  const ms = makeMicrosoft()
  const refreshP = ms.refresh(acc)
  const [integ, refreshed] = await Promise.all([verifyP, refreshP])

  if (!isAuthError(refreshed)) {
    acc = refreshed as MicrosoftAuthResponse
    updateAccountTokens(acc)
  }

  if (!SKIP_MOD_INTEGRITY_FOR_LAUNCH && !integ.ok) {
    const base =
      integ.reason === 'extra_mod'
        ? `Mods non autorisés détectés : ${(integ.paths ?? []).join(', ')}. Retirez-les ou réinstallez le pack.`
        : integ.reason === 'hash_mismatch'
          ? `Fichier modifié : ${integ.detail ?? 'inconnu'}. Réinstallez le pack.`
          : integ.reason === 'missing_file'
            ? `Fichier manquant : ${integ.detail ?? ''}. Réinstallez le pack.`
            : integ.detail ?? 'Intégrité : échec de vérification.'
    return { ok: false as const, error: errWithCode(SPX.LAUNCH_INTEGRITY, base) }
  }

  if (isAuthError(refreshed)) {
    return {
      ok: false as const,
      error: errWithCode(
        SPX.LAUNCH_AUTH,
        `Session expirée : ${refreshed.error}. Reconnectez-vous.`
      )
    }
  }

  const installedPath = join(root, '.solea-installed.json')
  if (!existsSync(installedPath)) {
    return {
      ok: false as const,
      error: errWithCode(SPX.LAUNCH_NOT_INSTALLED, 'Modpack non installé.')
    }
  }
  let installed: {
    gameVersion: string
    loaderType?: 'neoforge' | 'forge'
    loaderBuild?: string
    neoForgeVersion?: string
  }
  try {
    installed = JSON.parse(readFileSync(installedPath, 'utf8'))
  } catch {
    return {
      ok: false as const,
      error: errWithCode(
        SPX.LAUNCH_NOT_INSTALLED,
        'Fichier d’installation invalide. Réinstallez le pack.'
      )
    }
  }

  let loaderType: 'neoforge' | 'forge'
  let loaderBuild: string
  if (installed.loaderType && installed.loaderBuild) {
    loaderType = installed.loaderType
    loaderBuild = installed.loaderBuild
  } else if (installed.neoForgeVersion) {
    loaderType = 'neoforge'
    loaderBuild = installed.neoForgeVersion
  } else {
    return {
      ok: false as const,
      error: errWithCode(
        SPX.LAUNCH_LOADER,
        'Installation incomplète (loader). Réinstallez le pack.'
      )
    }
  }

  loaderBuild = normalizeForgeLoaderBuild(installed.gameVersion, loaderType, loaderBuild)

  const game = getGameSettingsForModpack(settings, spec.id)
  const jvmExtra = parseArgsBlock(settings.jvmArgs)
  if (settings.diagnosticLaunch) {
    jvmExtra.push('-XX:+UnlockDiagnosticVMOptions')
  }
  const gameExtra = parseArgsBlock(game.gameArgs)
  const javaVer = resolveLaunchJavaVersion(settings, spec)

  const memMin = settings.diagnosticLaunch ? '512M' : game.memoryMin
  const memMax = settings.diagnosticLaunch ? '1G' : game.memoryMax
  const downloadMult = settings.diagnosticLaunch
    ? Math.min(2, Math.max(1, settings.downloadThreads))
    : settings.downloadThreads

  const launchOpts = {
    path: root,
    authenticator: acc,
    version: installed.gameVersion,
    instance: null,
    detached: true,
    timeout: settings.networkTimeoutMs,
    downloadFileMultiple: downloadMult,
    loader: {
      type: loaderType,
      enable: true,
      build: loaderBuild
    },
    mcp: null,
    verify: false,
    ignored: ['logs', 'crash-reports', 'screenshots', 'texturepacks', 'resourcepacks', 'shaderpacks'],
    JVM_ARGS: jvmExtra,
    GAME_ARGS: gameExtra,
    java: {
      path: settings.javaPath?.trim() || null,
      version: javaVer,
      type: 'jre'
    },
    screen: {
      width: game.screenWidth,
      height: game.screenHeight,
      fullscreen: game.fullscreen
    },
    memory: {
      min: memMin,
      max: memMax
    }
  }

  try {
    await runMinecraftLaunchInWorker(launchOpts as unknown as Record<string, unknown>)
    recordModpackLastPlay(spec.id)

    if (settings.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() =>
        void setInGamePresence({
          modpackName: spec.displayName,
          largeImageKey: spec.discordLargeImageKey ?? 'logo',
          locale: settings.uiLanguage,
          modrinthPackUrl: modrinthModpackPageUrl(spec)
        })
      )
    }

    if (settings.afterLaunch === 'minimize') mainWindow?.minimize()
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: errWithCode(SPX.LAUNCH_GENERIC, msg) }
  }
})

ipcMain.handle('shell:open-external', async (_e, url: string) => {
  if (typeof url !== 'string' || !isUrlAllowedForExternalOpen(url)) {
    logMain('warn', 'open-external blocked', url)
    return
  }
  try {
    await shell.openExternal(url)
  } catch (e) {
    logMain('warn', 'open-external failed', e instanceof Error ? e.message : String(e))
  }
})

ipcMain.handle('shell:open-instance-folder', async () => {
  const root = getInstanceRoot()
  if (!existsSync(root) || !isSoleaInstanceInstalled(root)) {
    return {
      ok: false as const,
      error: 'Instance non installée — installe le modpack depuis l’accueil.'
    }
  }
  const err = await shell.openPath(root)
  if (err) return { ok: false as const, error: err }
  return { ok: true as const }
})

ipcMain.handle('shell:open-modpack-instance-folder', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'Modpack invalide.' }
  const root = getInstanceRootForModpack(resolveModpackId(id))
  if (!existsSync(root) || !isSoleaInstanceInstalled(root)) {
    return {
      ok: false as const,
      error: 'Instance non installée — installe le modpack depuis l’accueil.'
    }
  }
  const err = await shell.openPath(root)
  if (err) return { ok: false as const, error: err }
  return { ok: true as const }
})

ipcMain.handle('modpack:get-instance-details', async (_e, id: string) => {
  if (typeof id !== 'string') {
    return { installed: false, folderExists: false, sizeBytes: null as number | null, instanceRoot: '' }
  }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  const folderExists = existsSync(root)
  const installed = folderExists && isSoleaInstanceInstalled(root)
  const sizeBytes = installed ? await directorySizeAsync(root) : null
  return { installed, folderExists, sizeBytes, instanceRoot: root }
})

ipcMain.handle('shell:open-latest-crash', async () => {
  const root = getInstanceRoot()
  const cr = join(root, 'crash-reports')
  if (!existsSync(cr)) {
    return { ok: false as const, error: 'Dossier crash-reports introuvable (lance le jeu au moins une fois).' }
  }
  const names = readdirSync(cr).filter((f) => f.endsWith('.txt'))
  if (names.length === 0) {
    return { ok: false as const, error: 'Aucun fichier crash-report (.txt).' }
  }
  let best = ''
  let bestT = 0
  for (const f of names) {
    const p = join(cr, f)
    try {
      const t = statSync(p).mtimeMs
      if (t >= bestT) {
        bestT = t
        best = p
      }
    } catch {
      /* skip */
    }
  }
  if (!best) return { ok: false as const, error: 'Impossible de lire les crash-reports.' }
  const err = await shell.openPath(best)
  if (err) return { ok: false as const, error: err }
  return { ok: true as const }
})

ipcMain.handle('system:cache-stats', async () => {
  const gradle = join(os.homedir(), '.gradle', 'caches')
  const logs = join(app.getPath('userData'), 'logs')
  const gradleCachesBytes = existsSync(gradle) ? await directorySizeAsync(gradle) : 0
  const launcherLogsBytes = existsSync(logs) ? await directorySizeAsync(logs) : 0
  return { gradleCachesBytes, launcherLogsBytes }
})

ipcMain.handle('system:cache-clear', (_e, target: string) => {
  if (target === 'gradleCaches') {
    const gradle = join(os.homedir(), '.gradle', 'caches')
    return rmDirContentsIfExists(gradle)
  }
  if (target === 'launcherLogs') {
    const logs = join(app.getPath('userData'), 'logs')
    if (!existsSync(logs)) return { ok: true as const, freedBytes: 0 }
    let freed = 0
    for (const name of readdirSync(logs)) {
      if (!/\.log(\.[12])?$/.test(name)) continue
      const p = join(logs, name)
      try {
        freed += statSync(p).size
        rmSync(p, { force: true })
      } catch {
        /* ignore */
      }
    }
    return { ok: true as const, freedBytes: freed }
  }
  return { ok: false as const, error: 'Cible inconnue.' }
})

ipcMain.handle('app:open-java-download-page', async () => {
  const v = loadSettings().javaVersion?.trim() || '21'
  const url = `https://adoptium.net/temurin/releases/?version=${encodeURIComponent(v)}&os=windows&arch=x64&package=jre`
  if (!isUrlAllowedForExternalOpen(url)) return { ok: false as const }
  await shell.openExternal(url)
  return { ok: true as const }
})

/** Son au clic Play : fichier fourni par l’utilisateur dans Téléchargements. */
function playClickWavPath(): string {
  return join(os.homedir(), 'Downloads', 'clickplay.wav')
}

ipcMain.handle('app:get-custom-launch-sound-data-url', () => {
  const p = playClickWavPath()
  if (!existsSync(p)) return { ok: false as const }
  let st: ReturnType<typeof statSync>
  try {
    st = statSync(p)
  } catch {
    return { ok: false as const }
  }
  if (!st.isFile() || st.size > 900_000) return { ok: false as const }
  const low = p.toLowerCase()
  let mime = 'audio/wav'
  if (low.endsWith('.mp3')) mime = 'audio/mpeg'
  else if (low.endsWith('.ogg')) mime = 'audio/ogg'
  try {
    const buf = readFileSync(p)
    const dataUrl = `data:${mime};base64,${buf.toString('base64')}`
    return { ok: true as const, dataUrl }
  } catch {
    return { ok: false as const }
  }
})

ipcMain.handle('shell:open-userdata-folder', async () => {
  const err = await shell.openPath(app.getPath('userData'))
  if (err) return { ok: false as const, error: err }
  return { ok: true as const }
})
