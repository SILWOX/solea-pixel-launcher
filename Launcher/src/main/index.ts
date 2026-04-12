import { app, BrowserWindow, dialog, ipcMain, nativeImage, net, session, shell } from 'electron'
import { join, dirname, basename, resolve } from 'path'
import { fileURLToPath } from 'url'
import { Worker } from 'node:worker_threads'
import os from 'node:os'
import { Buffer } from 'node:buffer'
import {
  existsSync,
  readFileSync,
  writeFileSync,
  rmSync,
  mkdirSync,
  readdirSync,
  statSync,
  renameSync
} from 'fs'
import { readFile } from 'node:fs/promises'
import { createRequire } from 'module'
import type { MicrosoftAuthResponse } from 'minecraft-java-core'

const mainDir = dirname(fileURLToPath(import.meta.url))
const projectRoot = join(mainDir, '..', '..')

if (process.env.SOLEA_TEST_MODE === '1') {
  app.setPath('userData', join(projectRoot, 'test', 'electron-user-data'))
} else {
  try {
    const appData = app.getPath('appData')
    const oldUd = join(appData, 'solea-pixel-launcher')
    const newUd = join(appData, '.soleapixel')
    if (existsSync(oldUd) && !existsSync(newUd)) {
      renameSync(oldUd, newUd)
    }
    app.setPath('userData', newUd)
  } catch {
    /* best-effort : garde le chemin par défaut du package */
  }
}

/** Une seule instance du launcher : les lancements suivants réveillent la fenêtre et affichent un message. */
const soleaSingleInstanceLockOk = app.requestSingleInstanceLock()
if (!soleaSingleInstanceLockOk) {
  app.quit()
  process.exit(0)
}

const requireMjc = createRequire(import.meta.url)
const { Microsoft } = requireMjc('minecraft-java-core') as {
  Microsoft: new (clientId?: string, redirectUri?: string) => {
    getAuth: (type?: string) => Promise<unknown>
    refresh: (acc: MicrosoftAuthResponse) => Promise<unknown>
    exchangeCodeForToken: (code: string) => Promise<unknown>
  }
}

import {
  installMrpackFromModrinth,
  verifyInstanceIntegrity,
  getModpackActionInfo,
  type InstallProgress,
  type IntegrityResult
} from './modrinth.js'
import { setupSoleaServerIpc, shutdownSoleaServerHost } from './soleaServerHost.js'
import { isMinecraftRunning, killMinecraftForInstance } from './gameProcess.js'
import { SKIP_MOD_INTEGRITY_FOR_LAUNCH } from './config.js'
import {
  loadSettings,
  saveSettings,
  mergeLauncherSettingsPatch,
  parseArgsBlock,
  getEffectiveMicrosoftClientId,
  getGameSettingsForModpack,
  DEFAULT_SETTINGS,
  type LauncherSettings
} from './settings.js'
import {
  addOrUpdateAccount,
  addOfflineAccount,
  buildOfflineAuthenticator,
  getActiveAccount,
  hasAnyAccount,
  isOfflineAccount,
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
  recordModpackLastPlay,
  clearAllModpackActivity
} from './modpackActivity.js'
import { getQuickLaunchHint } from './quickLaunchHint.js'
import {
  syncLauncherMetaOnStartup,
  getLauncherMeta,
  clearLauncherUpdatedMemory
} from './launcherMeta.js'

type ModpackAllActionRow = {
  id: ModpackId
  displayName: string
  needsInstall: boolean
  needsUpdate: boolean
  error?: string
  installedVersionNumber?: string
  latestVersionNumber?: string
}

let modpackAllActionCache: { at: number; packs: ModpackAllActionRow[] } | null = null
const MODPACK_ALL_ACTION_TTL_MS = 2600

function invalidateModpackAllActionCache(): void {
  modpackAllActionCache = null
}
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
import {
  buildMicrosoftAuthorizeUrl,
  getMicrosoftAuthCodeEmbedded,
  MICROSOFT_OAUTH_REDIRECT_DESKTOP
} from './microsoftAuthCode.js'
import { isUrlAllowedForExternalOpen } from './safeOpenExternal.js'
import {
  directorySizeAsync,
  directorySizeSync,
  rmDirContentsBestEffort,
  rmTreeRecursiveWithProgress
} from './diskUsage.js'
import { normalizeReinstallPreserve, reinstallModpackPreserveFlow } from './modpackReinstall.js'
import { showNativeNotification } from './notifications.js'
import { logMain } from './logger.js'
import { errWithCode, SPX } from './supportCodes.js'
import {
  resolveInstanceRootForModpack,
  migrateLegacyInstanceLayout,
  migrateExternalSoleapixelInstanceFolderToInstances,
  moveInstanceFolderBestEffort,
  validateModpackInstanceParentPaths
} from './instancePaths.js'

function getActiveModpackSpec() {
  return getModpackSpec(resolveModpackId(loadSettings().activeModpackId))
}

function getInstanceRoot(): string {
  return resolveInstanceRootForModpack(resolveModpackId(loadSettings().activeModpackId), loadSettings())
}

function getInstanceRootForModpack(modpackId: ModpackId): string {
  return resolveInstanceRootForModpack(modpackId, loadSettings())
}

function buildSettingsWithParentForModpack(
  cur: LauncherSettings,
  id: ModpackId,
  parentPath: string | null
): LauncherSettings {
  const merged: Record<string, string> = { ...(cur.modpackInstanceParentPath ?? {}) }
  if (parentPath?.trim()) merged[id] = parentPath.trim()
  else delete merged[id]
  return mergeLauncherSettingsPatch(cur, { modpackInstanceParentPath: merged } as Record<string, unknown>)
}

/** True si au moins une instance Solea a un processus Minecraft lié à son dossier. */
async function isAnySoleaMinecraftRunning(): Promise<boolean> {
  const checks = await Promise.all(
    MODPACKS.map(async (m) => isMinecraftRunning(getInstanceRootForModpack(m.id)))
  )
  return checks.some(Boolean)
}

/** Arrête tous les Minecraft détectés pour les dossiers d’instances Solea (un seul jeu à la fois). */
async function killAllSoleaMinecraftInstances(): Promise<void> {
  await Promise.all(MODPACKS.map((m) => killMinecraftForInstance(getInstanceRootForModpack(m.id))))
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

/** Libellé 2ᵉ ligne Discord (écran courant), synchronisé depuis le renderer (`discord:set-launcher-screen-label`). */
let discordLauncherScreenLabelCache = ''

function discordPresenceForActivePack(): RichPresencePack {
  const st = loadSettings()
  const spec = getActiveModpackSpec()
  const label = discordLauncherScreenLabelCache.trim()
  return {
    modpackName: spec.displayName,
    largeImageKey: spec.discordLargeImageKey ?? 'logo',
    locale: st.uiLanguage,
    launcherScreenLabel: label || undefined
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
    : Math.max(1, Math.min(48, st.downloadThreads))
  await installMrpackFromModrinth({
    projectSlug: spec.projectSlug,
    gameVersion: spec.gameVersion,
    loader: spec.loader,
    instanceRoot,
    downloadConcurrency,
    onProgress: sendProgress
  })
}

async function installModpackByResolvedId(
  resolved: ModpackId
): Promise<{ ok: true } | { ok: false; error: string }> {
  const spec = getModpackSpec(resolved)
  const root = getInstanceRootForModpack(resolved)
  try {
    await runModpackInstallForSpec(spec, root)
    recordModpackLastInstall(resolved)
    pushInstallDoneNotification()
    invalidateModpackAllActionCache()
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
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
  if (!acc || isOfflineAccount(acc)) return null
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
  if (!acc || isOfflineAccount(acc) || !sameMinecraftUuid(acc.uuid, u)) {
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

/**
 * Fenêtre principale en 16:9 (type « cinéma » / HUD) : redimensionnement par coin en gardant le ratio.
 * Défaut = taille minimale 1280×720 (HD) — on ne peut pas réduire en dessous.
 */
const LAUNCHER_ASPECT_RATIO = 16 / 9
const LAUNCHER_DEFAULT_WIDTH = 1280
const LAUNCHER_DEFAULT_HEIGHT = 720
const LAUNCHER_MIN_WIDTH = 1280
const LAUNCHER_MIN_HEIGHT = 720
/** Évite une fenêtre géante si la résolution jeu dans les réglages est en 4K. */
const LAUNCHER_MAX_WIDTH = 2560
const LAUNCHER_MAX_HEIGHT = 1440

function fitLauncherWindowSize(width: number, height: number): { width: number; height: number } {
  let w = Math.max(LAUNCHER_MIN_WIDTH, Math.min(LAUNCHER_MAX_WIDTH, Math.round(width)))
  let h = Math.round(w / LAUNCHER_ASPECT_RATIO)
  if (h > LAUNCHER_MAX_HEIGHT) {
    h = LAUNCHER_MAX_HEIGHT
    w = Math.round(h * LAUNCHER_ASPECT_RATIO)
  }
  if (h < LAUNCHER_MIN_HEIGHT) {
    h = LAUNCHER_MIN_HEIGHT
    w = Math.round(h * LAUNCHER_ASPECT_RATIO)
  }
  if (w > LAUNCHER_MAX_WIDTH) {
    w = LAUNCHER_MAX_WIDTH
    h = Math.round(w / LAUNCHER_ASPECT_RATIO)
  }
  return { width: w, height: h }
}

let mainWindow: BrowserWindow | null = null
let discordPresenceRetryInterval: ReturnType<typeof setInterval> | null = null
/** Évite une boucle : le 2ᵉ `before-quit` doit laisser la fermeture se terminer après nettoyage RPC. */
let isQuittingPastDiscordCleanup = false
let soleaProcessCrashHooksRegistered = false
/** Une seule fenêtre crash pour erreurs du processus principal (évite boucles / spam). */
let soleaMainProcessCrashReporterShown = false

/** Logs du dernier lancement — fenêtre console + buffer pour rechargement. */
const MAX_GAME_LOG_BYTES = 1_200_000
let gameLogBuffer = ''
let logConsoleWindow: BrowserWindow | null = null
let debugWindow: BrowserWindow | null = null
let alreadyRunningWindow: BrowserWindow | null = null
let crashReporterWindow: BrowserWindow | null = null

type CrashPayloadMain = {
  source?: 'main' | 'renderer' | 'unknown'
  message: string
  stack?: string
  reason?: string
  exitCode?: number
  isFakeDemo?: boolean
}

function buildCrashPayloadForRenderer(p: CrashPayloadMain) {
  return {
    source: p.source ?? ('unknown' as const),
    message: p.message,
    stack: p.stack,
    reason: p.reason,
    exitCode: p.exitCode,
    appVersion: readAppVersion(),
    platform: process.platform,
    arch: process.arch,
    electronVersion: process.versions.electron ?? '?',
    occurredAt: new Date().toISOString(),
    isFakeDemo: p.isFakeDemo === true
  }
}

function loadRendererWithQuery(win: BrowserWindow, query: Record<string, string>): void {
  if (process.env.ELECTRON_RENDERER_URL) {
    const base = process.env.ELECTRON_RENDERER_URL.replace(/\/$/, '')
    const qs = new URLSearchParams(query).toString()
    void win.loadURL(`${base}/index.html?${qs}`)
  } else {
    void win.loadFile(join(mainDir, '../renderer/index.html'), { query })
  }
}

/** Notifie la fenêtre qui a invoqué les IPC (principal, debug, etc.) de l’état maximisé. */
function sendMaximizedState(win: BrowserWindow | null | undefined): void {
  if (!win || win.isDestroyed()) return
  try {
    const wc = win.webContents
    if (!wc || wc.isDestroyed()) return
    wc.send('window-maximized', win.isMaximized())
  } catch {
    /* fenêtre fermée */
  }
}

/**
 * Windows 11 + Electron : la barre de titre DWM peut réapparaître sur les fenêtres
 * `frame: false` (perte de focus, etc.). Contournement communautaire — electron#46882, #42791.
 */
function applyWin32FramelessCaptionWorkaround(win: BrowserWindow | null | undefined): void {
  if (process.platform !== 'win32' || !win || win.isDestroyed()) return
  const w = win
  w.on('blur', () => {
    if (w.isDestroyed()) return
    try {
      w.setMaximizable(false)
    } catch {
      /* ignore */
    }
  })
  w.on('focus', () => {
    if (w.isDestroyed()) return
    try {
      w.setMaximizable(true)
    } catch {
      /* ignore */
    }
  })
}

function showAlreadyRunningWindow(parent?: BrowserWindow | null): void {
  if (alreadyRunningWindow && !alreadyRunningWindow.isDestroyed()) {
    if (alreadyRunningWindow.isMinimized()) alreadyRunningWindow.restore()
    alreadyRunningWindow.show()
    if (process.platform === 'win32') alreadyRunningWindow.moveTop()
    alreadyRunningWindow.focus()
    return
  }
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined
  const parentWin = parent && !parent.isDestroyed() ? parent : undefined
  alreadyRunningWindow = new BrowserWindow({
    width: 460,
    height: 340,
    minWidth: 460,
    maxWidth: 460,
    minHeight: 340,
    maxHeight: 340,
    resizable: false,
    maximizable: false,
    show: false,
    frame: false,
    title: 'SOLEA PIXEL — DÉJÀ OUVERT',
    backgroundColor: '#07050c',
    autoHideMenuBar: true,
    ...(parentWin ? { parent: parentWin } : {}),
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })
  if (process.platform === 'win32' && icon && !icon.isEmpty()) {
    alreadyRunningWindow.setIcon(icon)
  }
  alreadyRunningWindow.webContents.on('page-title-updated', (ev) => {
    ev.preventDefault()
  })
  alreadyRunningWindow.setTitle('SOLEA PIXEL — DÉJÀ OUVERT')
  alreadyRunningWindow.webContents.on('did-finish-load', () => sendMaximizedState(alreadyRunningWindow))
  alreadyRunningWindow.on('ready-to-show', () => {
    alreadyRunningWindow?.show()
    if (process.platform === 'win32') alreadyRunningWindow?.moveTop()
    alreadyRunningWindow?.focus()
  })
  alreadyRunningWindow.on('closed', () => {
    alreadyRunningWindow = null
  })
  loadRendererWithQuery(alreadyRunningWindow, { solea: 'already-running' })
}

function openCrashReporterWindow(partial: CrashPayloadMain, parent?: BrowserWindow | null): void {
  const payload = buildCrashPayloadForRenderer(partial)
  if (crashReporterWindow && !crashReporterWindow.isDestroyed()) {
    if (crashReporterWindow.isMinimized()) crashReporterWindow.restore()
    crashReporterWindow.show()
    if (process.platform === 'win32') crashReporterWindow.moveTop()
    crashReporterWindow.focus()
    try {
      crashReporterWindow.webContents.send('crash-payload', payload)
    } catch {
      /* ignore */
    }
    return
  }
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined
  const parentWin = parent && !parent.isDestroyed() ? parent : undefined
  crashReporterWindow = new BrowserWindow({
    width: 560,
    height: 640,
    minWidth: 560,
    maxWidth: 560,
    minHeight: 640,
    maxHeight: 640,
    resizable: false,
    maximizable: false,
    show: false,
    frame: false,
    title: 'SOLEA PIXEL — CRASH',
    backgroundColor: '#07050c',
    autoHideMenuBar: true,
    ...(parentWin ? { parent: parentWin } : {}),
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })
  if (process.platform === 'win32' && icon && !icon.isEmpty()) {
    crashReporterWindow.setIcon(icon)
  }
  crashReporterWindow.webContents.on('page-title-updated', (ev) => {
    ev.preventDefault()
  })
  crashReporterWindow.setTitle('SOLEA PIXEL — CRASH')
  crashReporterWindow.webContents.on('did-finish-load', () => {
    sendMaximizedState(crashReporterWindow)
    try {
      crashReporterWindow?.webContents.send('crash-payload', payload)
    } catch {
      /* ignore */
    }
  })
  crashReporterWindow.on('ready-to-show', () => {
    crashReporterWindow?.show()
    if (process.platform === 'win32') crashReporterWindow?.moveTop()
    crashReporterWindow?.focus()
  })
  crashReporterWindow.on('closed', () => {
    crashReporterWindow = null
  })
  loadRendererWithQuery(crashReporterWindow, { solea: 'crash' })
}

function openOrFocusLogConsoleWindow(): void {
  if (logConsoleWindow && !logConsoleWindow.isDestroyed()) {
    if (logConsoleWindow.isMinimized()) logConsoleWindow.restore()
    if (process.platform === 'win32') logConsoleWindow.moveTop()
    logConsoleWindow.show()
    logConsoleWindow.focus()
    return
  }
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined
  logConsoleWindow = new BrowserWindow({
    width: 880,
    height: 600,
    minWidth: 520,
    minHeight: 340,
    show: false,
    frame: false,
    title: 'SOLEA PIXEL — CONSOLE',
    backgroundColor: '#07050c',
    autoHideMenuBar: true,
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })
  if (process.platform === 'win32' && icon && !icon.isEmpty()) {
    logConsoleWindow.setIcon(icon)
  }
  try {
    logConsoleWindow.removeMenu()
  } catch {
    /* ignore */
  }
  applyWin32FramelessCaptionWorkaround(logConsoleWindow)
  logConsoleWindow.webContents.on('page-title-updated', (ev) => {
    ev.preventDefault()
  })
  logConsoleWindow.setTitle('SOLEA PIXEL — CONSOLE')
  logConsoleWindow.on('maximize', () => sendMaximizedState(logConsoleWindow))
  logConsoleWindow.on('unmaximize', () => sendMaximizedState(logConsoleWindow))
  logConsoleWindow.webContents.on('did-finish-load', () => sendMaximizedState(logConsoleWindow))
  logConsoleWindow.on('ready-to-show', () => {
    logConsoleWindow?.show()
    if (process.platform === 'win32') {
      logConsoleWindow?.moveTop()
    }
    logConsoleWindow?.focus()
  })
  logConsoleWindow.on('closed', () => {
    logConsoleWindow = null
  })
  loadRendererWithQuery(logConsoleWindow, { solea: 'log' })
}

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

function createWindow(): void {
  const settings = loadSettings()
  const iconPath = getAppIconPath()
  const icon = iconPath ? nativeImage.createFromPath(iconPath) : undefined

  mainWindow = new BrowserWindow({
    width: LAUNCHER_DEFAULT_WIDTH,
    height: LAUNCHER_DEFAULT_HEIGHT,
    minWidth: LAUNCHER_MIN_WIDTH,
    minHeight: LAUNCHER_MIN_HEIGHT,
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
    const fitted = fitLauncherWindowSize(gameWin.screenWidth, gameWin.screenHeight)
    mainWindow.setSize(fitted.width, fitted.height)
  }

  try {
    mainWindow.setAspectRatio(LAUNCHER_ASPECT_RATIO)
  } catch {
    /* best-effort — certains gestionnaires de fenêtres peuvent ignorer */
  }

  try {
    mainWindow.center()
  } catch {
    /* ignore */
  }

  mainWindow.on('maximize', () => sendMaximizedState(mainWindow))
  mainWindow.on('unmaximize', () => sendMaximizedState(mainWindow))
  mainWindow.webContents.on('did-finish-load', () => sendMaximizedState(mainWindow))

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

  mainWindow.webContents.on('render-process-gone', (_e, details) => {
    if (details.reason !== 'crashed' && details.reason !== 'oom' && details.reason !== 'killed') return
    openCrashReporterWindow({
      source: 'renderer',
      message: `Renderer ${details.reason} (exit ${String(details.exitCode)})`,
      reason: details.reason,
      exitCode: details.exitCode
    })
  })
}

function sendProgress(p: InstallProgress): void {
  mainWindow?.webContents.send('install-progress', p)
}

/** Fichier vide : indique qu’un nettoyage disque du cache Chromium était incomplet (fichiers verrouillés). */
const PENDING_DISK_CACHE_SWEEP_FLAG = '.solea-pending-disk-cache-sweep'

function sweepPendingLauncherDiskCaches(): void {
  const ud = app.getPath('userData')
  const flag = join(ud, PENDING_DISK_CACHE_SWEEP_FLAG)
  if (!existsSync(flag)) return
  try {
    for (const name of LAUNCHER_CLEARABLE_CACHE_SUBDIRS) {
      rmDirContentsBestEffort(join(ud, name))
    }
  } catch (e) {
    logMain('warn', 'pending launcher disk cache sweep failed', {
      message: e instanceof Error ? e.message : String(e)
    })
  } finally {
    try {
      rmSync(flag, { force: true })
    } catch {
      /* ignore */
    }
  }
}

app.on('second-instance', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    if (mainWindow.isMinimized()) mainWindow.restore()
    mainWindow.show()
    mainWindow.focus()
  }
  showAlreadyRunningWindow()
})

if (!soleaProcessCrashHooksRegistered) {
  soleaProcessCrashHooksRegistered = true
  process.on('uncaughtException', (err) => {
    console.error('uncaughtException', err)
    try {
      logMain('error', 'uncaughtException', { message: err.message, stack: err.stack })
    } catch {
      /* ignore */
    }
    if (!soleaMainProcessCrashReporterShown) {
      soleaMainProcessCrashReporterShown = true
      try {
        openCrashReporterWindow({
          source: 'main',
          message: err.message,
          stack: err.stack,
          reason: 'uncaughtException'
        })
      } catch (e) {
        console.error('openCrashReporterWindow failed', e)
      }
    }
  })
  process.on('unhandledRejection', (reason) => {
    const msg = reason instanceof Error ? reason.message : String(reason)
    const stack = reason instanceof Error ? reason.stack : undefined
    console.error('unhandledRejection', reason)
    try {
      logMain('error', 'unhandledRejection', { message: msg, stack })
    } catch {
      /* ignore */
    }
    if (!soleaMainProcessCrashReporterShown) {
      soleaMainProcessCrashReporterShown = true
      try {
        openCrashReporterWindow({
          source: 'main',
          message: `Unhandled rejection: ${msg}`,
          stack,
          reason: 'unhandledRejection'
        })
      } catch (e) {
        console.error('openCrashReporterWindow failed', e)
      }
    }
  })
}

app.whenReady().then(() => {
  logMain('info', 'Application ready', { version: readAppVersion() })
  try {
    migrateExternalSoleapixelInstanceFolderToInstances(loadSettings())
  } catch {
    /* ignore */
  }
  try {
    migrateLegacyInstanceLayout()
  } catch {
    /* ignore */
  }
  syncLauncherMetaOnStartup()
  sweepPendingLauncherDiskCaches()
  // Même AUMID que l’app installée en dev → Windows réutilise l’icône du raccourci / cache (souvent l’ancien visuel).
  if (process.platform === 'win32') {
    app.setAppUserModelId(app.isPackaged ? 'fr.solea.pixel.launcher' : 'fr.solea.pixel.launcher.dev')
  }
  createWindow()
  setupSoleaServerIpc(() => mainWindow)
  setupAutoUpdater(mainWindow, loadSettings().updateChannel)

  discordPresenceRetryInterval = setInterval(() => {
    if (!loadSettings().discordRichPresence) return
    void reconnectDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
  }, 30_000)

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

app.on('before-quit', (event) => {
  if (discordPresenceRetryInterval) {
    clearInterval(discordPresenceRetryInterval)
    discordPresenceRetryInterval = null
  }

  if (isQuittingPastDiscordCleanup) return

  event.preventDefault()
  isQuittingPastDiscordCleanup = true

  void (async () => {
    try {
      shutdownSoleaServerHost()
      await Promise.race([
        shutdownDiscordRpc(),
        new Promise<void>((resolve) => setTimeout(resolve, 3500))
      ])
    } finally {
      app.quit()
    }
  })()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})

ipcMain.handle('window:minimize', (e) => {
  BrowserWindow.fromWebContents(e.sender)?.minimize()
})

ipcMain.handle('window:toggle-maximize', (e) => {
  const w = BrowserWindow.fromWebContents(e.sender)
  if (!w || w.isDestroyed()) return { maximized: false }
  if (!w.isMaximizable()) return { maximized: w.isMaximized() }
  if (w.isMaximized()) w.unmaximize()
  else w.maximize()
  return { maximized: w.isMaximized() }
})

ipcMain.handle('window:close', (e) => {
  BrowserWindow.fromWebContents(e.sender)?.close()
})

ipcMain.handle('window:is-maximized', (e) => {
  const w = BrowserWindow.fromWebContents(e.sender)
  return w && !w.isDestroyed() ? w.isMaximized() : false
})

ipcMain.handle('app:relaunch', () => {
  app.relaunch()
  app.exit(0)
  return { ok: true as const }
})

ipcMain.handle('app:quit-launcher', () => {
  app.quit()
  return { ok: true as const }
})

ipcMain.handle('debug:show-already-running-overlay', (e) => {
  const senderWin = BrowserWindow.fromWebContents(e.sender)
  showAlreadyRunningWindow(senderWin)
  return { ok: true as const }
})

ipcMain.handle('debug:open-fake-crash-window', (e) => {
  const senderWin = BrowserWindow.fromWebContents(e.sender)
  openCrashReporterWindow(
    {
      source: 'renderer',
      message:
        "TypeError: Cannot read properties of undefined (reading 'demo') — simulation (the launcher did not crash).",
      stack:
        'FakeCrashDemo: simulated stack\n    at debug:open-fake-crash-window (main)\n    at CrashReporterDemo (renderer)',
      reason: 'crashed',
      exitCode: -1,
      isFakeDemo: true
    },
    senderWin
  )
  return { ok: true as const }
})

ipcMain.handle('app:get-version', () => app.getVersion())

/** Flux actu (HTTPS) : `net.fetch` évite les blocages CORS du renderer sur certains environnements. */
ipcMain.handle('actu:fetch-text', async (_e, rawUrl: unknown) => {
  if (typeof rawUrl !== 'string' || rawUrl.length === 0 || rawUrl.length > 4096) {
    return { ok: false as const, error: 'URL invalide' }
  }
  let u: URL
  try {
    u = new URL(rawUrl)
  } catch {
    return { ok: false as const, error: 'URL invalide' }
  }
  if (u.protocol !== 'https:' && u.protocol !== 'http:') {
    return { ok: false as const, error: 'Protocole non autorisé' }
  }
  try {
    const res = await net.fetch(rawUrl, {
      headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
    })
    if (!res.ok) return { ok: false as const, error: `HTTP ${String(res.status)}` }
    const text = await res.text()
    return { ok: true as const, text }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

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
  if (acc && isOfflineAccount(acc)) return null
  return headDataUrlFromStoredAccount(acc && !isOfflineAccount(acc) ? acc : null, n)
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
    if (isOfflineAccount(acc)) {
      return { ok: false as const, error: 'Profil hors ligne : import de skin réservé aux comptes Microsoft.' }
    }
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

ipcMain.handle('modpack:quick-launch-hint', async (_e, rawId: unknown) => {
  if (typeof rawId !== 'string') return null
  const id = resolveModpackId(rawId)
  const spec = MODPACKS.find((m) => m.id === id)
  if (!spec) return null
  const root = getInstanceRootForModpack(id)
  if (!existsSync(join(root, '.solea-installed.json'))) return null
  return getQuickLaunchHint(root, spec.displayName)
})

ipcMain.handle('launcher:get-meta', () => getLauncherMeta())

ipcMain.handle('launcher:clear-memory', () => {
  try {
    clearAllModpackActivity()
    clearLauncherUpdatedMemory()
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

ipcMain.handle('auth:get-state', () => ({
  /** true = aucun compte : l’UI doit afficher uniquement la connexion Microsoft */
  requiresMicrosoftLogin: !hasAnyAccount()
}))

ipcMain.handle('settings:get', (): LauncherSettings => loadSettings())

ipcMain.handle('discord:set-launcher-screen-label', async (_e, label: unknown) => {
  discordLauncherScreenLabelCache =
    typeof label === 'string' ? label.trim().slice(0, 120) : ''
  const st = loadSettings()
  if (!st.discordRichPresence) return { ok: true as const }
  await initDiscordRpcIfNeeded()
  /* Ne pas écraser la présence « En jeu » si le renderer envoie encore des mises à jour de navigation. */
  if (await isAnySoleaMinecraftRunning()) return { ok: true as const }
  await setMenuPresence(discordPresenceForActivePack())
  return { ok: true as const }
})

ipcMain.handle('settings:save', async (_e, partial: Partial<LauncherSettings>) => {
  const cur = loadSettings()
  const next = mergeLauncherSettingsPatch(cur, partial as Record<string, unknown>)
  const r = saveSettings(next)
  if (r.ok) {
    if (next.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
    } else {
      await shutdownDiscordRpc()
    }
  }
  return r
})

ipcMain.handle('settings:reset', async () => {
  const r = saveSettings({ ...DEFAULT_SETTINGS })
  if (r.ok) {
    const st = loadSettings()
    if (st.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() => void setMenuPresence(discordPresenceForActivePack()))
    } else {
      await shutdownDiscordRpc()
    }
  }
  return r
})

ipcMain.handle('auth:list-accounts', () => listAccountSummaries())

ipcMain.handle('auth:get-active', () => {
  const acc = getActiveAccount()
  if (!acc) return null
  return { name: acc.name, uuid: acc.uuid, offline: isOfflineAccount(acc) }
})

ipcMain.handle('auth:add-offline', (_e, name: unknown) => {
  try {
    const loc = loadSettings().uiLanguage === 'fr' ? 'fr' : 'en'
    if (typeof name !== 'string' || !name.trim()) {
      return {
        ok: false as const,
        error: loc === 'fr' ? 'Pseudo invalide.' : 'Invalid username.'
      }
    }
    return addOfflineAccount(name.trim(), loc)
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err)
    return { ok: false as const, error: msg }
  }
})

ipcMain.handle('auth:add-account', async () => {
  const settings = loadSettings()
  const clientId = getEffectiveMicrosoftClientId(settings) || '00000000402b5328'
  const redirectUri = MICROSOFT_OAUTH_REDIRECT_DESKTOP
  const authUrl = buildMicrosoftAuthorizeUrl(clientId, redirectUri)
  const ms = makeMicrosoft()
  const msAuthTitle =
    settings.uiLanguage === 'fr'
      ? 'Connexion Microsoft — Solea Pixel'
      : 'Microsoft sign-in — Solea Pixel'
  const code = await getMicrosoftAuthCodeEmbedded(authUrl, redirectUri, mainWindow, {
    windowTitle: msAuthTitle,
    iconPath: getAppIconPath()
  })
  if (code === 'cancel') return { ok: false as const, reason: 'cancelled' }
  const result = await ms.exchangeCodeForToken(code)
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
  const fr = loadSettings().uiLanguage === 'fr'
  if (!acc) {
    return { ok: false as const, error: fr ? 'Aucun compte actif.' : 'No active account.' }
  }
  if (isOfflineAccount(acc)) {
    return {
      ok: false as const,
      error: fr
        ? 'Profil hors ligne : pas de session Microsoft à rafraîchir.'
        : 'Offline profile: no Microsoft session to refresh.'
    }
  }
  if (!acc.refresh_token) {
    return { ok: false as const, error: fr ? 'Aucun compte actif.' : 'No active account.' }
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
  const resolved = resolveModpackId(loadSettings().activeModpackId)
  return installModpackByResolvedId(resolved)
})

ipcMain.handle('modpack:install-pack', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'Modpack invalide.' }
  return installModpackByResolvedId(resolveModpackId(id))
})

ipcMain.handle('modpack:reinstall', async (_e, id: string, preserveRaw?: unknown) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'Modpack invalide.' }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  if (await isMinecraftRunning(root)) {
    return {
      ok: false as const,
      error: 'Fermez Minecraft pour ce modpack avant de réinstaller.'
    }
  }
  const spec = getModpackSpec(resolved)
  const preserve = normalizeReinstallPreserve(preserveRaw)
  try {
    await reinstallModpackPreserveFlow(root, spec.projectSlug, preserve, () =>
      runModpackInstallForSpec(spec, root)
    )
    recordModpackLastInstall(resolved)
    pushInstallDoneNotification()
    invalidateModpackAllActionCache()
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
  if (await isMinecraftRunning(root)) {
    return {
      ok: false as const,
      error: 'Fermez Minecraft pour ce modpack avant de désinstaller.'
    }
  }
  try {
    if (existsSync(root)) {
      sendProgress({
        phase: 'uninstall',
        current: 0,
        total: 0,
        detail: '__scan__',
        task: 'uninstall'
      })
      await rmTreeRecursiveWithProgress(root, (current, total, detail, scanOrDelete) => {
        sendProgress({
          phase: 'uninstall',
          current,
          total,
          detail: scanOrDelete === 'scan' ? '__scan__' : detail,
          task: 'uninstall'
        })
      })
    }
    invalidateModpackAllActionCache()
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

ipcMain.handle('modpack:verify-for', async (_e, id: unknown) => {
  if (typeof id !== 'string') {
    return {
      ok: false as const,
      reason: 'read_error' as const,
      detail: 'Modpack invalide.'
    }
  }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  return verifyInstanceIntegrity(root)
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

ipcMain.handle('modpack:all-action-info', async (_e, forceRefresh?: unknown) => {
  const force = Boolean(forceRefresh)
  const now = Date.now()
  if (
    !force &&
    modpackAllActionCache &&
    now - modpackAllActionCache.at < MODPACK_ALL_ACTION_TTL_MS
  ) {
    return { packs: modpackAllActionCache.packs }
  }
  const packs: ModpackAllActionRow[] = []
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
  modpackAllActionCache = { at: Date.now(), packs }
  return { packs }
})

ipcMain.handle('game:is-running', async () => await isAnySoleaMinecraftRunning())

ipcMain.handle('game:stop', async () => {
  await killAllSoleaMinecraftInstances()
  return { ok: true as const }
})

ipcMain.handle('game-log:get-snapshot', () => gameLogBuffer)

ipcMain.handle('game-log:clear-buffer', () => {
  clearGameLogBuffer()
  return { ok: true as const }
})

ipcMain.handle('game-log-window:open', () => {
  openOrFocusLogConsoleWindow()
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
    width: 1000,
    height: 780,
    minWidth: 560,
    minHeight: 480,
    show: false,
    frame: false,
    title: 'SOLEA PIXEL — DEBUG',
    backgroundColor: '#07050c',
    autoHideMenuBar: true,
    ...(icon && !icon.isEmpty() ? { icon } : {}),
    webPreferences: {
      preload: join(mainDir, '../preload/index.mjs'),
      contextIsolation: true,
      sandbox: false
    }
  })
  if (process.platform === 'win32' && icon && !icon.isEmpty()) {
    debugWindow.setIcon(icon)
  }
  debugWindow.webContents.on('page-title-updated', (event) => {
    event.preventDefault()
  })
  debugWindow.setTitle('SOLEA PIXEL — DEBUG')
  debugWindow.on('maximize', () => sendMaximizedState(debugWindow))
  debugWindow.on('unmaximize', () => sendMaximizedState(debugWindow))
  debugWindow.webContents.on('did-finish-load', () => sendMaximizedState(debugWindow))
  debugWindow.on('ready-to-show', () => {
    if (process.platform === 'win32' && icon && !icon.isEmpty()) {
      debugWindow?.setIcon(icon)
    }
    debugWindow?.show()
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

ipcMain.handle('debug:get-snapshot', async () =>
  getDebugSnapshot(getInstanceRoot, isAnySoleaMinecraftRunning)
)

ipcMain.handle('debug:reload-main', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.reload()
    return { ok: true as const }
  }
  return { ok: false as const, error: 'Main window missing.' }
})

/** Fenêtre debug → lance une fausse barre d’install / maj sur la fenêtre launcher (aucun téléchargement réel). */
ipcMain.handle('debug:fake-install-main', (_e, kind: unknown) => {
  if (kind !== 'install' && kind !== 'update') {
    return { ok: false as const, error: 'Invalid kind.' }
  }
  if (!mainWindow || mainWindow.isDestroyed()) {
    return { ok: false as const, error: 'Main window missing.' }
  }
  if (mainWindow.isMinimized()) mainWindow.restore()
  mainWindow.show()
  mainWindow.focus()
  mainWindow.webContents.send('debug-fake-install', kind)
  return { ok: true as const }
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

  const runningRows = await Promise.all(
    MODPACKS.map(async (m) => {
      const instRoot = getInstanceRootForModpack(m.id)
      return { m, instRoot, running: await isMinecraftRunning(instRoot) }
    })
  )
  const fr = settings.uiLanguage === 'fr'
  for (const { m, instRoot, running } of runningRows) {
    if (!running) continue
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

  const accRaw = getActiveAccount()
  if (!accRaw) {
    return {
      ok: false as const,
      error: errWithCode(
        SPX.LAUNCH_AUTH,
        fr ? 'Aucun compte sélectionné.' : 'No account selected.'
      )
    }
  }

  const verifyP: Promise<IntegrityResult> = SKIP_MOD_INTEGRITY_FOR_LAUNCH
    ? Promise.resolve({ ok: true })
    : verifyInstanceIntegrity(root)

  let authenticator: Record<string, unknown>

  if (isOfflineAccount(accRaw)) {
    const integ = await verifyP
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
    authenticator = buildOfflineAuthenticator(accRaw)
  } else {
    let acc = accRaw as MicrosoftAuthResponse
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

    authenticator = acc as unknown as Record<string, unknown>
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
    installed = JSON.parse(await readFile(installedPath, 'utf8'))
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
    authenticator,
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
      width: game.screenWidth ?? 800,
      height: game.screenHeight ?? 600,
      fullscreen: game.fullscreen
    },
    memory: {
      min: memMin,
      max: memMax
    }
  }

  try {
    /* Laisse le processus principal traiter des événements (curseur Windows, IPC) avant le worker. */
    await new Promise<void>((r) => setImmediate(r))
    await runMinecraftLaunchInWorker(launchOpts as unknown as Record<string, unknown>)
    recordModpackLastPlay(spec.id)

    if (settings.discordRichPresence) {
      void initDiscordRpcIfNeeded().then(() =>
        void setInGamePresence({
          modpackName: spec.displayName,
          largeImageKey: spec.discordLargeImageKey ?? 'logo',
          locale: settings.uiLanguage
        })
      )
    }

    if (settings.afterLaunch === 'minimize') mainWindow?.minimize()
    if (settings.openGameLogOnInstanceLaunch) openOrFocusLogConsoleWindow()
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

ipcMain.handle('diagnostic:get-latest-crash-text', async () => {
  const root = getInstanceRoot()
  const cr = join(root, 'crash-reports')
  if (!existsSync(cr)) {
    return { ok: false as const, error: 'no_crash_folder' }
  }
  const names = readdirSync(cr).filter((f) => f.endsWith('.txt'))
  if (names.length === 0) {
    return { ok: false as const, error: 'no_crash_file' }
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
  if (!best) return { ok: false as const, error: 'no_crash_file' }
  try {
    const text = readFileSync(best, 'utf8')
    return { ok: true as const, text: text.slice(0, 48_000), fileName: basename(best) }
  } catch {
    return { ok: false as const, error: 'read_error' }
  }
})

ipcMain.handle('modpack:get-latest-screenshot', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'invalid_id' }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  const shots = join(root, 'screenshots')
  if (!existsSync(shots)) {
    return { ok: false as const, reason: 'no_folder' as const }
  }
  const names = readdirSync(shots).filter((f) => /\.(png|jpe?g)$/i.test(f))
  if (names.length === 0) {
    return { ok: false as const, reason: 'empty' as const }
  }
  let best = ''
  let bestT = 0
  for (const f of names) {
    const p = join(shots, f)
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
  if (!best) return { ok: false as const, reason: 'empty' as const }
  try {
    const img = nativeImage.createFromPath(best)
    if (img.isEmpty()) return { ok: false as const, reason: 'invalid_image' as const }
    const sz = img.getSize()
    const maxW = 320
    const thumb =
      sz.width > maxW
        ? img.resize({ width: maxW, height: Math.max(1, Math.round((sz.height * maxW) / sz.width)) })
        : img
    return {
      ok: true as const,
      thumbDataUrl: thumb.toDataURL(),
      fileName: basename(best),
      folderPath: shots
    }
  } catch {
    return { ok: false as const, reason: 'read_error' as const }
  }
})

ipcMain.handle('shell:open-screenshots-folder', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'invalid_id' }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  if (!existsSync(root) || !isSoleaInstanceInstalled(root)) {
    return {
      ok: false as const,
      error: 'Instance non installée — installe le modpack depuis l’accueil.'
    }
  }
  const shots = join(root, 'screenshots')
  if (!existsSync(shots)) {
    mkdirSync(shots, { recursive: true })
  }
  const err = await shell.openPath(shots)
  if (err) return { ok: false as const, error: err }
  return { ok: true as const }
})

function safeScreenshotFileName(name: string): boolean {
  if (typeof name !== 'string' || !name.trim()) return false
  const b = basename(name)
  if (b !== name || name.includes('..')) return false
  return /\.(png|jpe?g)$/i.test(name)
}

ipcMain.handle('modpack:list-screenshots', async (_e, id: string) => {
  if (typeof id !== 'string') return { ok: false as const, error: 'invalid_id' }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  const shots = join(root, 'screenshots')
  if (!existsSync(shots)) {
    return { ok: true as const, items: [] as { fileName: string; thumbDataUrl: string }[] }
  }
  const names = readdirSync(shots).filter((f) => /\.(png|jpe?g)$/i.test(f))
  const scored: { f: string; t: number }[] = []
  for (const f of names) {
    const p = join(shots, f)
    try {
      scored.push({ f, t: statSync(p).mtimeMs })
    } catch {
      /* skip */
    }
  }
  scored.sort((a, b) => b.t - a.t)
  const items: { fileName: string; thumbDataUrl: string }[] = []
  const maxFiles = 100
  const maxW = 220
  for (const { f } of scored.slice(0, maxFiles)) {
    const p = join(shots, f)
    try {
      const img = nativeImage.createFromPath(p)
      if (img.isEmpty()) continue
      const sz = img.getSize()
      const thumb =
        sz.width > maxW
          ? img.resize({
              width: maxW,
              height: Math.max(1, Math.round((sz.height * maxW) / sz.width))
            })
          : img
      items.push({ fileName: f, thumbDataUrl: thumb.toDataURL() })
    } catch {
      /* skip */
    }
  }
  return { ok: true as const, items }
})

ipcMain.handle('modpack:get-screenshot-full', async (_e, id: string, fileName: string) => {
  if (typeof id !== 'string' || !safeScreenshotFileName(String(fileName))) {
    return { ok: false as const, error: 'invalid' }
  }
  const resolved = resolveModpackId(id)
  const root = getInstanceRootForModpack(resolved)
  const shots = join(root, 'screenshots')
  const p = join(shots, basename(fileName))
  const shotsAbs = resolve(shots)
  const fileAbs = resolve(p)
  if (!fileAbs.startsWith(shotsAbs) || !existsSync(fileAbs)) {
    return { ok: false as const, error: 'missing' }
  }
  try {
    const st = statSync(fileAbs)
    if (st.size > 14 * 1024 * 1024) {
      return { ok: false as const, error: 'too_large' }
    }
    const img = nativeImage.createFromPath(fileAbs)
    if (img.isEmpty()) return { ok: false as const, error: 'invalid' }
    return { ok: true as const, dataUrl: img.toDataURL() }
  } catch {
    return { ok: false as const, error: 'read_error' }
  }
})

ipcMain.handle(
  'shell:save-data-url-as',
  async (e, payload: { dataUrl: string; defaultFileName: string }) => {
    const dataUrl = typeof payload?.dataUrl === 'string' ? payload.dataUrl : ''
    if (!dataUrl.startsWith('data:image/png;base64,')) {
      return { ok: false as const, error: 'bad_data' }
    }
    const comma = dataUrl.indexOf(',')
    const b64 = comma >= 0 ? dataUrl.slice(comma + 1) : ''
    let buf: Buffer
    try {
      buf = Buffer.from(b64, 'base64')
    } catch {
      return { ok: false as const, error: 'decode' }
    }
    if (buf.length > 25 * 1024 * 1024) {
      return { ok: false as const, error: 'too_large' }
    }
    const rawName =
      typeof payload?.defaultFileName === 'string' && payload.defaultFileName.trim()
        ? basename(payload.defaultFileName.trim())
        : 'screenshot.png'
    const safeName = rawName.replace(/[^\w.\-]+/g, '_') || 'screenshot.png'
    const win = BrowserWindow.fromWebContents(e.sender)
    const { canceled, filePath } = await dialog.showSaveDialog(win ?? undefined, {
      defaultPath: safeName.endsWith('.png') ? safeName : `${safeName}.png`,
      filters: [
        { name: 'PNG', extensions: ['png'] },
        { name: 'JPEG', extensions: ['jpg', 'jpeg'] }
      ]
    })
    if (canceled || !filePath) return { ok: false as const, error: 'cancelled' }
    try {
      writeFileSync(filePath, buf)
      return { ok: true as const, path: filePath }
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err)
      return { ok: false as const, error: msg }
    }
  }
)

/** Webhook signalements : env `SOLEA_REPORT_WEBHOOK_URL` ou fichier `solea-report-webhook.url` (1ère ligne) dans userData. */
function getReportDiscordWebhookUrl(): string | undefined {
  const fromEnv = process.env.SOLEA_REPORT_WEBHOOK_URL?.trim()
  if (fromEnv) return fromEnv
  try {
    const p = join(app.getPath('userData'), 'solea-report-webhook.url')
    if (!existsSync(p)) return undefined
    const line = readFileSync(p, 'utf8').split(/\r?\n/u)[0]?.trim() ?? ''
    if (
      !line ||
      !/^https:\/\/(?:canary\.)?discord\.com\/api\/webhooks\//iu.test(line)
    ) {
      return undefined
    }
    return line
  } catch {
    return undefined
  }
}

ipcMain.handle('report:submit-discord-webhook', async (_e, payload: { content: string }) => {
  const url = getReportDiscordWebhookUrl()
  if (!url) {
    return { ok: false as const, error: 'no_webhook_env' }
  }
  const content =
    typeof payload?.content === 'string' ? payload.content.trim().slice(0, 1900) : ''
  if (!content) {
    return { ok: false as const, error: 'empty_content' }
  }
  try {
    const r = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content })
    })
    if (!r.ok) {
      const t = await r.text()
      return {
        ok: false as const,
        error: `HTTP ${r.status}: ${t.slice(0, 160)}`
      }
    }
    return { ok: true as const }
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return { ok: false as const, error: msg }
  }
})

/** Sous-dossiers Chromium/Electron sous userData — sûrs à vider (pas comptes, pas instances). */
const LAUNCHER_CLEARABLE_CACHE_SUBDIRS = [
  'Cache',
  'Code Cache',
  'GPUCache',
  'DawnGraphiteCache',
  'DawnWebGPUCache',
  'ShaderCache',
  'GrShaderCache'
] as const

ipcMain.handle('system:cache-stats', async () => {
  const ud = app.getPath('userData')
  const logs = join(ud, 'logs')
  let launcherCachesBytes = 0
  for (const name of LAUNCHER_CLEARABLE_CACHE_SUBDIRS) {
    const p = join(ud, name)
    if (existsSync(p)) launcherCachesBytes += await directorySizeAsync(p)
  }
  const launcherLogsBytes = existsSync(logs) ? await directorySizeAsync(logs) : 0
  return { launcherCachesBytes, launcherLogsBytes }
})

ipcMain.handle('system:cache-clear', async (_e, target: string) => {
  if (target === 'launcherCaches') {
    try {
      await session.defaultSession.clearCache()
    } catch {
      /* best-effort */
    }
    try {
      await session.defaultSession.clearCodeCaches({ urls: [] })
    } catch {
      /* best-effort — API selon version Electron */
    }
    const ud = app.getPath('userData')
    let freed = 0
    let diskHadFailures = false
    for (const name of LAUNCHER_CLEARABLE_CACHE_SUBDIRS) {
      const r = rmDirContentsBestEffort(join(ud, name))
      freed += r.freedBytes
      if (r.hadFailures) diskHadFailures = true
    }
    const flagPath = join(ud, PENDING_DISK_CACHE_SWEEP_FLAG)
    if (diskHadFailures) {
      try {
        writeFileSync(flagPath, '1')
      } catch {
        /* ignore */
      }
    } else {
      try {
        rmSync(flagPath, { force: true })
      } catch {
        /* ignore */
      }
    }
    return { ok: true as const, freedBytes: freed, partialDisk: diskHadFailures }
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

ipcMain.handle('instance:pick-parent-folder', async () => {
  const win = mainWindow ?? BrowserWindow.getFocusedWindow()
  const { canceled, filePaths } = await dialog.showOpenDialog(win ?? undefined, {
    title: 'Choisir le dossier parent',
    properties: ['openDirectory', 'createDirectory']
  })
  if (canceled || !filePaths[0]) return { ok: false as const, reason: 'cancelled' as const }
  return { ok: true as const, path: filePaths[0]! }
})

ipcMain.handle(
  'instance:get-path-preview',
  (_e, modpackId: unknown, parentPath: unknown) => {
    const id = resolveModpackId(typeof modpackId === 'string' ? modpackId : '')
    const cur = loadSettings()
    const pp =
      typeof parentPath === 'string' && parentPath.trim() ? parentPath.trim() : null
    const next = buildSettingsWithParentForModpack(cur, id, pp)
    const oldRoot = resolveInstanceRootForModpack(id, cur)
    const newRoot = resolveInstanceRootForModpack(id, next)
    const v = validateModpackInstanceParentPaths(next)
    return {
      oldRoot,
      newRoot,
      valid: v.ok,
      error: v.ok ? undefined : v.error
    }
  }
)

ipcMain.handle(
  'instance:apply-parent-path',
  async (
    _e,
    payload: unknown
  ): Promise<{ ok: true } | { ok: false; error: string }> => {
    const p = payload as { modpackId?: string; parentPath?: string | null; mode?: string }
    const id = resolveModpackId(typeof p.modpackId === 'string' ? p.modpackId : '')
    const parentPath = typeof p.parentPath === 'string' && p.parentPath.trim() ? p.parentPath.trim() : null
    const mode = p.mode === 'reinstall' ? 'reinstall' : 'move'
    const cur = loadSettings()
    const oldRoot = resolveInstanceRootForModpack(id, cur)
    const next = buildSettingsWithParentForModpack(cur, id, parentPath)
    const newRoot = resolveInstanceRootForModpack(id, next)
    const v = validateModpackInstanceParentPaths(next)
    if (!v.ok) return { ok: false as const, error: v.error }

    if (oldRoot === newRoot) {
      const r = saveSettings(next)
      return r.ok ? { ok: true as const } : { ok: false as const, error: r.error }
    }

    if (await isMinecraftRunning(oldRoot)) {
      return {
        ok: false as const,
        error: 'Fermez Minecraft avant de changer le dossier de cette instance.'
      }
    }

    const hasData = (() => {
      try {
        return existsSync(oldRoot) && readdirSync(oldRoot).length > 0
      } catch {
        return false
      }
    })()

    if (!hasData) {
      const r = saveSettings(next)
      if (!r.ok) return { ok: false as const, error: r.error }
      invalidateModpackAllActionCache()
      return { ok: true as const }
    }

    if (mode === 'move') {
      const mv = moveInstanceFolderBestEffort(oldRoot, newRoot)
      if (!mv.ok) return mv
      const r = saveSettings(next)
      if (!r.ok) return { ok: false as const, error: r.error }
      invalidateModpackAllActionCache()
      return { ok: true as const }
    }

    try {
      rmSync(oldRoot, { recursive: true, force: true })
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e)
      return { ok: false as const, error: msg }
    }
    if (existsSync(newRoot)) {
      try {
        rmSync(newRoot, { recursive: true, force: true })
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e)
        return { ok: false as const, error: msg }
      }
    }
    const r = saveSettings(next)
    if (!r.ok) return { ok: false as const, error: r.error }
    const installRes = await installModpackByResolvedId(id)
    invalidateModpackAllActionCache()
    if (!installRes.ok) return { ok: false as const, error: installRes.error }
    return { ok: true as const }
  }
)
