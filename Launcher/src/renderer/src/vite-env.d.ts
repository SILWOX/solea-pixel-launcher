/// <reference types="vite/client" />

declare module '*.mp3' {
  const src: string
  export default src
}
declare module '*.wav' {
  const src: string
  export default src
}

export type InstallProgressPayload = {
  phase: string
  current: number
  total: number
  detail?: string
  task?: 'install' | 'uninstall' | 'launch'
  source?: 'vanilla'
  vanillaDone?: boolean
}

type LS = import('./launcherTypes').LauncherSettingsUI

export type SoleaApi = {
  windowMinimize: () => Promise<void>
  windowToggleMaximize: () => Promise<{ maximized: boolean }>
  windowClose: () => Promise<void>
  appRelaunch: () => Promise<{ ok: true }>
  appQuit: () => Promise<{ ok: true }>
  onCrashPayload: (cb: (p: import('./crashTypes').CrashPayloadUi) => void) => () => void
  debugShowAlreadyRunningOverlay: () => Promise<{ ok: true }>
  debugOpenFakeCrashWindow: () => Promise<{ ok: true }>
  windowIsMaximized: () => Promise<boolean>
  onWindowMaximized: (cb: (maximized: boolean) => void) => () => void
  getSkinHead: (uuid: string, size?: number) => Promise<string | null>,
  getSkinPreview: (uuid: string) => Promise<{
    source: 'local' | 'remote'
    dataUrl: string
    model: 'slim' | 'default' | 'auto-detect'
    capeDataUrl: string | null
  } | null>,
  getSkinPresetsState: (uuid: string) => Promise<{
    activePresetId: string | null
    presets: {
      id: string
      name: string
      model: 'slim' | 'default'
      thumbDataUrl: string
    }[]
  } | null>,
  setActiveSkinPreset: (
    uuid: string,
    presetId: string | null
  ) => Promise<{ ok: true; skinSyncError?: string } | { ok: false; error: string }>,
  deleteSkinPreset: (
    uuid: string,
    presetId: string
  ) => Promise<{ ok: true; skinSyncError?: string } | { ok: false; error: string }>,
  updateSkinPresetModel: (
    uuid: string,
    presetId: string,
    model: 'slim' | 'default'
  ) => Promise<{ ok: true; skinSyncError?: string } | { ok: false; error: string }>,
  importSkinPreset: (
    model: 'slim' | 'default',
    displayName: string
  ) => Promise<
    { ok: true; presetId: string; skinSyncError?: string } | { ok: false; error: string }
  >,
  listAccountCapes: () => Promise<
    | {
        ok: true
        capes: {
          id: string
          alias: string
          state: string
          url: string | null
          dataUrl: string | null
        }[]
        activeCapeId: string | null
      }
    | { ok: false; error: string }
  >
  setAccountActiveCape: (capeId: string | null) => Promise<{ ok: true } | { ok: false; error: string }>,
  getPaths: () => Promise<{
    userData: string
    instanceRoot: string
    testMode: boolean
    projectRoot: string
    version: string
    modpackDisplayName: string
    activeModpackId: string
    modpacks: { id: string; displayName: string }[]
    homeLinks: { modrinthUrl: string; discordUrl?: string }
  }>
  getModpackActivity: () => Promise<
    Record<string, { lastPlayAt?: string; lastInstallAt?: string }>
  >
  getQuickLaunchHint: (modpackId: string) => Promise<{
    kind: 'world' | 'server' | 'instance'
    label: string
  } | null>
  getLauncherMeta: () => Promise<{
    knownVersion?: string
    lastLauncherUpdatedAt?: string | null
  }>
  clearLauncherMemory: () => Promise<{ ok: true } | { ok: false; error: string }>
  setActiveModpack: (
    id: string
  ) => Promise<{ ok: true; activeModpackId: string } | { ok: false; error: string }>
  getAuthState: () => Promise<{ requiresMicrosoftLogin: boolean }>
  listAccounts: () => Promise<{ uuid: string; name: string; offline: boolean }[]>
  getActiveAccount: () => Promise<{ name: string; uuid: string; offline?: boolean } | null>
  addOfflineAccount: (name: string) => Promise<{ ok: true } | { ok: false; error: string }>
  addAccount: () => Promise<
    | { ok: true; name: string; uuid: string }
    | { ok: false; reason: 'cancelled' }
    | { ok: false; reason: 'error'; detail?: string }
  >
  setActiveAccount: (uuid: string) => Promise<{ ok: true } | { ok: false; error: string }>
  removeAccount: (uuid: string) => Promise<{ ok: true }>
  refreshActiveAccount: () => Promise<{ ok: true; name: string } | { ok: false; error: string }>
  getSettings: () => Promise<LS>
  /** Met à jour la 2ᵉ ligne Discord (écran courant) quand la Rich Presence menu est active. */
  setDiscordLauncherScreenLabel: (label: string) => Promise<{ ok: true }>
  saveSettings: (partial: Partial<LS>) => Promise<{ ok: true } | { ok: false; error: string }>
  resetSettings: () => Promise<{ ok: true } | { ok: false; error: string }>
  installModpack: () => Promise<{ ok: true } | { ok: false; error: string }>
  installModpackPack: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  reinstallModpack: (
    id: string,
    preserve?: import('./launcherTypes').ReinstallPreserveOptions
  ) => Promise<{ ok: true } | { ok: false; error: string }>
  uninstallModpack: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  openExternalUrl: (url: string) => Promise<void>
  verifyModpack: () => Promise<
    { ok: true } | { ok: false; reason: string; detail?: string; paths?: string[] }
  >
  verifyModpackFor: (id: string) => Promise<
    { ok: true } | { ok: false; reason: string; detail?: string; paths?: string[] }
  >
  getModpackActionInfo: () => Promise<{
    needsInstall: boolean
    needsUpdate: boolean
    installedVersionNumber?: string
    latestVersionNumber?: string
    error?: string
  }>
  getAllModpacksActionInfo: (forceRefresh?: boolean) => Promise<{
    packs: import('./launcherTypes').ModpackActionInfoRow[]
  }>
  isGameRunning: () => Promise<boolean>
  stopGame: () => Promise<{ ok: true } | { ok: false; error: string }>
  launch: () => Promise<{ ok: true } | { ok: false; error: string }>
  openInstanceFolder: () => Promise<{ ok: true } | { ok: false; error: string }>
  openModpackInstanceFolder: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  getModpackInstanceDetails: (id: string) => Promise<{
    installed: boolean
    folderExists: boolean
    sizeBytes: number | null
    instanceRoot: string
  }>
  openLatestCrashReport: () => Promise<{ ok: true } | { ok: false; error: string }>
  getLatestCrashText: () => Promise<
    | { ok: true; text: string; fileName: string }
    | { ok: false; error: string }
  >
  getLatestScreenshot: (modpackId: string) => Promise<
    | { ok: true; thumbDataUrl: string; fileName: string; folderPath: string }
    | { ok: false; error?: string; reason?: 'no_folder' | 'empty' | 'invalid_image' | 'read_error' }
  >
  openScreenshotsFolder: (modpackId: string) => Promise<{ ok: true } | { ok: false; error: string }>
  listModpackScreenshots: (
    modpackId: string
  ) => Promise<
    | { ok: true; items: { fileName: string; thumbDataUrl: string }[] }
    | { ok: false; error: string }
  >
  getModpackScreenshotFull: (
    modpackId: string,
    fileName: string
  ) => Promise<{ ok: true; dataUrl: string } | { ok: false; error: string }>
  saveDataUrlAsPng: (
    dataUrl: string,
    defaultFileName: string
  ) => Promise<{ ok: true; path: string } | { ok: false; error: string }>
  submitReportDiscordWebhook: (content: string) => Promise<{ ok: true } | { ok: false; error: string }>
  getCacheStats: () => Promise<{ launcherCachesBytes: number; launcherLogsBytes: number }>
  clearCache: (target: 'launcherCaches' | 'launcherLogs') => Promise<
    { ok: true; freedBytes: number; partialDisk?: boolean } | { ok: false; error: string }
  >
  openJavaDownloadPage: () => Promise<{ ok: boolean }>
  getCustomLaunchSoundDataUrl: () => Promise<{ ok: true; dataUrl: string } | { ok: false }>
  openUserDataFolder: () => Promise<{ ok: true } | { ok: false; error: string }>
  pickInstanceParentFolder: () => Promise<
    { ok: true; path: string } | { ok: false; reason: 'cancelled' }
  >
  getInstancePathPreview: (
    modpackId: string,
    parentPath: string | null
  ) => Promise<{
    oldRoot: string
    newRoot: string
    valid: boolean
    error?: string
  }>
  applyInstanceParentPath: (payload: {
    modpackId: string
    parentPath: string | null
    mode: 'move' | 'reinstall'
  }) => Promise<{ ok: true } | { ok: false; error: string }>
  onInstallProgress: (cb: (p: InstallProgressPayload) => void) => () => void
  onGameLog: (cb: (line: string) => void) => () => void
  getGameLogSnapshot: () => Promise<string>
  clearGameLogBuffer: () => Promise<{ ok: true }>
  openGameLogWindow: () => Promise<{ ok: true }>
  onGameExited: (cb: () => void) => () => void
  getAppVersion: () => Promise<string>
  /** Télécharge le fichier actu (HTTPS) depuis le processus principal (contourne CORS du renderer). */
  fetchActuText: (
    url: string
  ) => Promise<{ ok: true; text: string } | { ok: false; error: string }>
  getMemoryStats: () => Promise<{ totalBytes: number; totalGiB: number }>
  checkForUpdates: () => Promise<{ ok: true; started: boolean }>
  downloadUpdate: () => Promise<{ ok: true } | { ok: false; error: string }>
  quitAndInstall: () => Promise<{ ok: true }>
  onUpdaterAvailable: (
    cb: (payload: { version: string; releaseNotes?: string | string[] | null }) => void
  ) => () => void
  onUpdaterNotAvailable: (cb: () => void) => () => void
  onUpdaterDownloaded: (cb: () => void) => () => void
  onUpdaterError: (cb: (msg: string) => void) => () => void
  openDebugWindow: () => Promise<{ ok: true }>
  getDebugSnapshot: () => Promise<import('./debugTypes').DebugSnapshotUi>
  reloadMainLauncher: () => Promise<{ ok: true } | { ok: false; error: string }>
  debugOpenKnownFolder: (
    kind: 'userData' | 'instanceRoot'
  ) => Promise<{ ok: true } | { ok: false; error: string }>
  /** Fenêtre debug uniquement : demande une fausse install/maj sur la fenêtre launcher. */
  debugRequestFakeInstall: (
    kind: 'install' | 'update'
  ) => Promise<{ ok: true } | { ok: false; error: string }>
  /** Fenêtre launcher uniquement : reçoit l’ordre de lancer la simulation (voir debugRequestFakeInstall). */
  onDebugFakeInstall: (cb: (kind: 'install' | 'update') => void) => () => void

  vanillaEnsureProfile: (profileId: string) => Promise<{ ok: true; profileId: string; gameDir: string }>
  vanillaOpenFolder: (
    profileId: string,
    kind: 'game' | 'profile'
  ) => Promise<{ ok: true } | { ok: false; error: string }>
  /** Ouvre le dossier `.minecraft` officiel (AppData…\.minecraft), pas l’ancien arbre `minecraft-version`. */
  vanillaOpenDotMinecraftFolder: () => Promise<{ ok: true } | { ok: false; error: string }>
  vanillaBackupSaves: (
    profileId: string
  ) => Promise<{ ok: true; zipPath: string } | { ok: false; error: string }>
  vanillaMetaGet: (profileId: string) => Promise<{
    ok: true
    meta: {
      javaPath?: string | null
      javaVersion?: string | null
      shaderStack?: 'optifine' | 'iris'
      lastSelectedVersion?: string | null
    }
  }>
  vanillaMetaSet: (
    profileId: string,
    patch: Partial<{
      javaPath: string | null
      javaVersion: string | null
      shaderStack: 'optifine' | 'iris'
      lastSelectedVersion: string | null
    }>
  ) => Promise<
    | {
        ok: true
        meta: {
          javaPath?: string | null
          javaVersion?: string | null
          shaderStack?: 'optifine' | 'iris'
          lastSelectedVersion?: string | null
        }
      }
    | { ok: false; error: string }
  >
  vanillaSyncJavaFromSettings: (profileId: string) => Promise<{
    ok: true
    meta: {
      javaPath?: string | null
      javaVersion?: string | null
      shaderStack?: 'optifine' | 'iris'
      lastSelectedVersion?: string | null
    }
  }>
  vanillaListClientVersions: (profileId: string) => Promise<{ ok: true; versions: string[] }>
  vanillaListAllInstallFolders: () => Promise<{
    ok: true
    entries: { folder: string; versions: string[] }[]
  }>
  vanillaUninstallClientVersion: (
    profileId: string,
    versionId: string
  ) => Promise<{ ok: true } | { ok: false; error: string }>
  vanillaLaunch: (payload: {
    profileId: string
    version: string
    shaderStack?: 'optifine' | 'iris'
    backupSaves?: boolean
  }) => Promise<{ ok: true } | { ok: false; error: string }>
  vanillaDownloadClient: (payload: {
    profileId: string
    version: string
    shaderStack?: 'optifine' | 'iris'
  }) => Promise<{ ok: true } | { ok: false; error: string }>

  soleaServerList: () => Promise<SoleServerListRowUi[]>
  soleaServerListVanillaReleases: () => Promise<
    { ok: true; ids: string[] } | { ok: false; error: string }
  >
  soleaServerGetCoverDataUrl: (id: string) => Promise<string | null>
  soleaServerCreate: (payload: {
    name: string
    description?: string
    /** @deprecated */
    imageUrl?: string
    coverImageDataUrl?: string
    modpackId: string
    vanillaGameVersion?: string
  }) => Promise<{ ok: true; id: string } | { ok: false; error: string }>
  soleaServerUpdate: (payload: {
    id: string
    name?: string
    description?: string
    imageUrl?: string | null
    coverImageDataUrl?: string | null
    ramMiB?: number
    modpackId?: string
    vanillaGameVersion?: string
    port?: number
  }) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerDelete: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerConsole: (id: string) => Promise<string[]>
  soleaServerStart: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerStop: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerKill: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerRestart: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerSendCommand: (id: string, line: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerCheckModpackUpdate: (id: string) => Promise<
    | { ok: true; hasUpdate: boolean; latestVersion?: string; currentVersion?: string }
    | { ok: false; error: string }
  >
  soleaServerReinstallLatest: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerGetPublicIp: () => Promise<{ ok: true; ip: string } | { ok: false; error: string }>
  soleaServerProbeLocalPort: (port: number) => Promise<{ ok: true; free: boolean; inUse: boolean }>
  soleaServerOpenFolder: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerWorldGet: (
    id: string
  ) => Promise<
    { ok: true; levelName: string; levelSeed: string; levelType: string } | { ok: false; error: string }
  >
  soleaServerWorldSet: (payload: {
    id: string
    levelName?: string
    levelSeed?: string
    levelType?: string
  }) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerWorldDelete: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerPropertiesRawGet: (id: string) => Promise<{ ok: true; content: string } | { ok: false; error: string }>
  soleaServerPropertiesRawSet: (payload: {
    id: string
    content: string
  }) => Promise<{ ok: true } | { ok: false; error: string }>
  soleaServerJvmExtraGet: (id: string) => Promise<{ ok: true; content: string } | { ok: false; error: string }>
  soleaServerJvmExtraSet: (payload: {
    id: string
    content: string
  }) => Promise<{ ok: true } | { ok: false; error: string }>
  onSoleaServerEvent: (cb: (ev: SoleServerIpcEventUi) => void) => () => void
}

export type SoleServerListRowUi = {
  id: string
  name: string
  description: string
  modpackId: string
  vanillaGameVersion?: string
  ramMiB: number
  port: number
  coverFile?: string
  createdAt: string
  installState: 'installing' | 'ready' | 'error'
  installError?: string
  modrinthVersionId?: string
  modrinthVersionNumber?: string
  serverState: 'stopped' | 'starting' | 'running' | 'stopping'
}

export type SoleServerIpcEventUi =
  | {
      kind: 'progress'
      serverId: string
      phase: string
      current: number
      total: number
      detail?: string
    }
  | { kind: 'line'; serverId: string; line: string }
  | { kind: 'runtime'; serverId: string; serverState: SoleServerListRowUi['serverState'] }

declare global {
  interface Window {
    solea: SoleaApi
  }
}

interface ImportMetaEnv {
  readonly VITE_ACTU_SOLEA_JSON_URL?: string
}

export {}
