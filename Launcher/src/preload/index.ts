import { contextBridge, ipcRenderer } from 'electron'
import type { CrashPayloadUi } from '../renderer/src/crashTypes'

export type InstallProgressPayload = {
  phase: string
  current: number
  total: number
  detail?: string
  task?: 'install' | 'uninstall'
}

const api = {
  windowMinimize: () => ipcRenderer.invoke('window:minimize'),
  windowToggleMaximize: () => ipcRenderer.invoke('window:toggle-maximize'),
  windowClose: () => ipcRenderer.invoke('window:close'),
  appRelaunch: () => ipcRenderer.invoke('app:relaunch') as Promise<{ ok: true }>,
  appQuit: () => ipcRenderer.invoke('app:quit-launcher') as Promise<{ ok: true }>,
  onCrashPayload: (cb: (p: CrashPayloadUi) => void) => {
    const listener = (_e: Electron.IpcRendererEvent, p: CrashPayloadUi) => cb(p)
    ipcRenderer.on('crash-payload', listener)
    return () => ipcRenderer.removeListener('crash-payload', listener)
  },
  debugShowAlreadyRunningOverlay: () =>
    ipcRenderer.invoke('debug:show-already-running-overlay') as Promise<{ ok: true }>,
  debugOpenFakeCrashWindow: () =>
    ipcRenderer.invoke('debug:open-fake-crash-window') as Promise<{ ok: true }>,
  windowIsMaximized: () => ipcRenderer.invoke('window:is-maximized') as Promise<boolean>,
  onWindowMaximized: (cb: (maximized: boolean) => void) => {
    const listener = (_e: Electron.IpcRendererEvent, maximized: boolean) => cb(maximized)
    ipcRenderer.on('window-maximized', listener)
    return () => ipcRenderer.removeListener('window-maximized', listener)
  },
  getPaths: () => ipcRenderer.invoke('app:get-paths'),
  getModpackActivity: () =>
    ipcRenderer.invoke('modpack:activity-get') as Promise<
      Record<string, { lastPlayAt?: string; lastInstallAt?: string }>
    >,
  getQuickLaunchHint: (modpackId: string) =>
    ipcRenderer.invoke('modpack:quick-launch-hint', modpackId) as Promise<{
      kind: 'world' | 'server' | 'instance'
      label: string
    } | null>,
  getLauncherMeta: () =>
    ipcRenderer.invoke('launcher:get-meta') as Promise<{
      knownVersion?: string
      lastLauncherUpdatedAt?: string | null
    }>,
  clearLauncherMemory: () =>
    ipcRenderer.invoke('launcher:clear-memory') as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  setActiveModpack: (id: string) => ipcRenderer.invoke('modpack:set-active', id),
  getSkinHead: (uuid: string, size?: number) =>
    ipcRenderer.invoke('skin:get-head', uuid, size) as Promise<string | null>,
  getSkinPreview: (uuid: string) =>
    ipcRenderer.invoke('skin:get-preview', uuid) as Promise<{
      source: 'local' | 'remote'
      dataUrl: string
      model: 'slim' | 'default' | 'auto-detect'
      capeDataUrl: string | null
    } | null>,
  getSkinPresetsState: (uuid: string) =>
    ipcRenderer.invoke('skin:presets-state', uuid) as Promise<{
      activePresetId: string | null
      presets: {
        id: string
        name: string
        model: 'slim' | 'default'
        thumbDataUrl: string
      }[]
    } | null>,
  setActiveSkinPreset: (uuid: string, presetId: string | null) =>
    ipcRenderer.invoke('skin:set-active-preset', uuid, presetId) as Promise<
      { ok: true; skinSyncError?: string } | { ok: false; error: string }
    >,
  deleteSkinPreset: (uuid: string, presetId: string) =>
    ipcRenderer.invoke('skin:delete-preset', uuid, presetId) as Promise<
      { ok: true; skinSyncError?: string } | { ok: false; error: string }
    >,
  updateSkinPresetModel: (uuid: string, presetId: string, model: 'slim' | 'default') =>
    ipcRenderer.invoke('skin:update-preset-model', uuid, presetId, model) as Promise<
      { ok: true; skinSyncError?: string } | { ok: false; error: string }
    >,
  importSkinPreset: (model: 'slim' | 'default', displayName: string) =>
    ipcRenderer.invoke('skin:import-preset', model, displayName) as Promise<
      | { ok: true; presetId: string; skinSyncError?: string }
      | { ok: false; error: string }
    >,
  listAccountCapes: () =>
    ipcRenderer.invoke('profile:list-capes') as Promise<
      | {
          ok: true
          capes: { id: string; alias: string; state: string; url: string | null; dataUrl: string | null }[]
          activeCapeId: string | null
        }
      | { ok: false; error: string }
    >,
  setAccountActiveCape: (capeId: string | null) =>
    ipcRenderer.invoke('profile:set-active-cape', capeId) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  getAuthState: () => ipcRenderer.invoke('auth:get-state'),
  listAccounts: () =>
    ipcRenderer.invoke('auth:list-accounts') as Promise<
      { uuid: string; name: string; offline: boolean }[]
    >,
  getActiveAccount: () =>
    ipcRenderer.invoke('auth:get-active') as Promise<{
      name: string
      uuid: string
      offline?: boolean
    } | null>,
  addOfflineAccount: (name: string) =>
    ipcRenderer.invoke('auth:add-offline', name) as Promise<{ ok: true } | { ok: false; error: string }>,
  addAccount: () =>
    ipcRenderer.invoke('auth:add-account') as Promise<
      | { ok: true; name: string; uuid: string }
      | { ok: false; reason: 'cancelled' }
      | { ok: false; reason: 'error'; detail: string }
    >,
  setActiveAccount: (uuid: string) => ipcRenderer.invoke('auth:set-active', uuid),
  removeAccount: (uuid: string) => ipcRenderer.invoke('auth:remove-account', uuid),
  refreshActiveAccount: () => ipcRenderer.invoke('auth:refresh-active'),
  getSettings: () => ipcRenderer.invoke('settings:get'),
  setDiscordLauncherScreenLabel: (label: string) =>
    ipcRenderer.invoke('discord:set-launcher-screen-label', label) as Promise<{ ok: true }>,
  saveSettings: (partial: Record<string, unknown>) => ipcRenderer.invoke('settings:save', partial),
  resetSettings: () => ipcRenderer.invoke('settings:reset'),
  installModpack: () => ipcRenderer.invoke('modpack:install'),
  installModpackPack: (id: string) =>
    ipcRenderer.invoke('modpack:install-pack', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  reinstallModpack: (id: string, preserve?: Record<string, unknown>) =>
    ipcRenderer.invoke('modpack:reinstall', id, preserve) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  uninstallModpack: (id: string) =>
    ipcRenderer.invoke('modpack:uninstall', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  openExternalUrl: (url: string) => ipcRenderer.invoke('shell:open-external', url),
  verifyModpack: () => ipcRenderer.invoke('modpack:verify'),
  verifyModpackFor: (id: string) => ipcRenderer.invoke('modpack:verify-for', id),
  getModpackActionInfo: () => ipcRenderer.invoke('modpack:action-info'),
  getAllModpacksActionInfo: (forceRefresh?: boolean) =>
    ipcRenderer.invoke('modpack:all-action-info', Boolean(forceRefresh)),
  isGameRunning: () => ipcRenderer.invoke('game:is-running') as Promise<boolean>,
  stopGame: () =>
    ipcRenderer.invoke('game:stop') as Promise<{ ok: true } | { ok: false; error: string }>,
  launch: () => ipcRenderer.invoke('game:launch'),
  openInstanceFolder: () => ipcRenderer.invoke('shell:open-instance-folder'),
  openModpackInstanceFolder: (id: string) =>
    ipcRenderer.invoke('shell:open-modpack-instance-folder', id) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  getModpackInstanceDetails: (id: string) =>
    ipcRenderer.invoke('modpack:get-instance-details', id) as Promise<{
      installed: boolean
      folderExists: boolean
      sizeBytes: number | null
      instanceRoot: string
    }>,
  openLatestCrashReport: () =>
    ipcRenderer.invoke('shell:open-latest-crash') as Promise<{ ok: true } | { ok: false; error: string }>,
  getLatestCrashText: () =>
    ipcRenderer.invoke('diagnostic:get-latest-crash-text') as Promise<
      | { ok: true; text: string; fileName: string }
      | { ok: false; error: string }
    >,
  getLatestScreenshot: (modpackId: string) =>
    ipcRenderer.invoke('modpack:get-latest-screenshot', modpackId) as Promise<
      | {
          ok: true
          thumbDataUrl: string
          fileName: string
          folderPath: string
        }
      | { ok: false; error?: string; reason?: 'no_folder' | 'empty' | 'invalid_image' | 'read_error' }
    >,
  openScreenshotsFolder: (modpackId: string) =>
    ipcRenderer.invoke('shell:open-screenshots-folder', modpackId) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  listModpackScreenshots: (modpackId: string) =>
    ipcRenderer.invoke('modpack:list-screenshots', modpackId) as Promise<
      | { ok: true; items: { fileName: string; thumbDataUrl: string }[] }
      | { ok: false; error: string }
    >,
  getModpackScreenshotFull: (modpackId: string, fileName: string) =>
    ipcRenderer.invoke('modpack:get-screenshot-full', modpackId, fileName) as Promise<
      | { ok: true; dataUrl: string }
      | { ok: false; error: string }
    >,
  saveDataUrlAsPng: (dataUrl: string, defaultFileName: string) =>
    ipcRenderer.invoke('shell:save-data-url-as', { dataUrl, defaultFileName }) as Promise<
      | { ok: true; path: string }
      | { ok: false; error: string }
    >,
  submitReportDiscordWebhook: (content: string) =>
    ipcRenderer.invoke('report:submit-discord-webhook', { content }) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  getCacheStats: () =>
    ipcRenderer.invoke('system:cache-stats') as Promise<{
      launcherCachesBytes: number
      launcherLogsBytes: number
    }>,
  clearCache: (target: 'launcherCaches' | 'launcherLogs') =>
    ipcRenderer.invoke('system:cache-clear', target) as Promise<
      { ok: true; freedBytes: number; partialDisk?: boolean } | { ok: false; error: string }
    >,
  openJavaDownloadPage: () =>
    ipcRenderer.invoke('app:open-java-download-page') as Promise<{ ok: boolean }>,
  getCustomLaunchSoundDataUrl: () =>
    ipcRenderer.invoke('app:get-custom-launch-sound-data-url') as Promise<
      { ok: true; dataUrl: string } | { ok: false }
    >,
  openUserDataFolder: () => ipcRenderer.invoke('shell:open-userdata-folder'),
  pickInstanceParentFolder: () =>
    ipcRenderer.invoke('instance:pick-parent-folder') as Promise<
      { ok: true; path: string } | { ok: false; reason: 'cancelled' }
    >,
  getInstancePathPreview: (modpackId: string, parentPath: string | null) =>
    ipcRenderer.invoke('instance:get-path-preview', modpackId, parentPath) as Promise<{
      oldRoot: string
      newRoot: string
      valid: boolean
      error?: string
    }>,
  applyInstanceParentPath: (payload: {
    modpackId: string
    parentPath: string | null
    mode: 'move' | 'reinstall'
  }) =>
    ipcRenderer.invoke('instance:apply-parent-path', payload) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  onInstallProgress: (cb: (p: InstallProgressPayload) => void) => {
    const listener = (_e: Electron.IpcRendererEvent, p: InstallProgressPayload) => cb(p)
    ipcRenderer.on('install-progress', listener)
    return () => ipcRenderer.removeListener('install-progress', listener)
  },
  onGameLog: (cb: (line: string) => void) => {
    const listener = (_e: Electron.IpcRendererEvent, line: string) => cb(line)
    ipcRenderer.on('game-log', listener)
    return () => ipcRenderer.removeListener('game-log', listener)
  },
  getGameLogSnapshot: () => ipcRenderer.invoke('game-log:get-snapshot') as Promise<string>,
  clearGameLogBuffer: () => ipcRenderer.invoke('game-log:clear-buffer') as Promise<{ ok: true }>,
  openGameLogWindow: () => ipcRenderer.invoke('game-log-window:open') as Promise<{ ok: true }>,
  onGameExited: (cb: () => void) => {
    const listener = () => cb()
    ipcRenderer.on('game-exited', listener)
    return () => ipcRenderer.removeListener('game-exited', listener)
  },
  getAppVersion: () => ipcRenderer.invoke('app:get-version') as Promise<string>,
  fetchActuText: (url: string) =>
    ipcRenderer.invoke('actu:fetch-text', url) as Promise<
      { ok: true; text: string } | { ok: false; error: string }
    >,
  getMemoryStats: () =>
    ipcRenderer.invoke('app:get-memory-stats') as Promise<{ totalBytes: number; totalGiB: number }>,
  checkForUpdates: () =>
    ipcRenderer.invoke('updater:check') as Promise<{ ok: true; started: boolean }>,
  downloadUpdate: () =>
    ipcRenderer.invoke('updater:download') as Promise<{ ok: true } | { ok: false; error: string }>,
  quitAndInstall: () => ipcRenderer.invoke('updater:quit-and-install') as Promise<{ ok: true }>,
  onUpdaterAvailable: (
    cb: (payload: { version: string; releaseNotes?: string | string[] | null }) => void
  ) => {
    const listener = (_e: Electron.IpcRendererEvent, p: unknown) => cb(p as never)
    ipcRenderer.on('updater:available', listener)
    return () => ipcRenderer.removeListener('updater:available', listener)
  },
  onUpdaterNotAvailable: (cb: () => void) => {
    const listener = () => cb()
    ipcRenderer.on('updater:not-available', listener)
    return () => ipcRenderer.removeListener('updater:not-available', listener)
  },
  onUpdaterDownloaded: (cb: () => void) => {
    const listener = () => cb()
    ipcRenderer.on('updater:downloaded', listener)
    return () => ipcRenderer.removeListener('updater:downloaded', listener)
  },
  onUpdaterError: (cb: (msg: string) => void) => {
    const listener = (_e: Electron.IpcRendererEvent, msg: string) => cb(msg)
    ipcRenderer.on('updater:error', listener)
    return () => ipcRenderer.removeListener('updater:error', listener)
  },
  openDebugWindow: () => ipcRenderer.invoke('debug-window:open') as Promise<{ ok: true }>,
  getDebugSnapshot: () => ipcRenderer.invoke('debug:get-snapshot'),
  reloadMainLauncher: () =>
    ipcRenderer.invoke('debug:reload-main') as Promise<{ ok: true } | { ok: false; error: string }>,
  debugOpenKnownFolder: (kind: 'userData' | 'instanceRoot') =>
    ipcRenderer.invoke('debug:open-known-folder', kind) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  debugRequestFakeInstall: (kind: 'install' | 'update') =>
    ipcRenderer.invoke('debug:fake-install-main', kind) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  onDebugFakeInstall: (cb: (kind: 'install' | 'update') => void) => {
    const listener = (_e: Electron.IpcRendererEvent, k: 'install' | 'update') => cb(k)
    ipcRenderer.on('debug-fake-install', listener)
    return () => ipcRenderer.removeListener('debug-fake-install', listener)
  },
  soleaServerList: () => ipcRenderer.invoke('solea-server:list'),
  soleaServerGetCoverDataUrl: (id: string) =>
    ipcRenderer.invoke('solea-server:get-cover-data-url', id) as Promise<string | null>,
  soleaServerCreate: (payload: {
    name: string
    description?: string
    imageUrl?: string
    coverImageDataUrl?: string
    modpackId: string
  }) =>
    ipcRenderer.invoke('solea-server:create', payload) as Promise<
      { ok: true; id: string } | { ok: false; error: string }
    >,
  soleaServerUpdate: (payload: {
    id: string
    name?: string
    description?: string
    imageUrl?: string | null
    coverImageDataUrl?: string | null
    ramMiB?: number
    modpackId?: string
    port?: number
  }) => ipcRenderer.invoke('solea-server:update', payload) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerDelete: (id: string) =>
    ipcRenderer.invoke('solea-server:delete', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerConsole: (id: string) => ipcRenderer.invoke('solea-server:console', id) as Promise<string[]>,
  soleaServerStart: (id: string) =>
    ipcRenderer.invoke('solea-server:start', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerStop: (id: string) =>
    ipcRenderer.invoke('solea-server:stop', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerKill: (id: string) =>
    ipcRenderer.invoke('solea-server:kill', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerRestart: (id: string) =>
    ipcRenderer.invoke('solea-server:restart', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerSendCommand: (id: string, line: string) =>
    ipcRenderer.invoke('solea-server:send-command', { id, line }) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  soleaServerCheckModpackUpdate: (id: string) =>
    ipcRenderer.invoke('solea-server:check-modpack-update', id) as Promise<
      | {
          ok: true
          hasUpdate: boolean
          latestVersion?: string
          currentVersion?: string
        }
      | { ok: false; error: string }
    >,
  soleaServerReinstallLatest: (id: string) =>
    ipcRenderer.invoke('solea-server:reinstall-latest', id) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  soleaServerGetPublicIp: () =>
    ipcRenderer.invoke('solea-server:get-public-ip') as Promise<
      { ok: true; ip: string } | { ok: false; error: string }
    >,
  soleaServerProbeLocalPort: (port: number) =>
    ipcRenderer.invoke('solea-server:probe-local-port', port) as Promise<{
      ok: true
      free: boolean
      inUse: boolean
    }>,
  soleaServerOpenFolder: (id: string) =>
    ipcRenderer.invoke('solea-server:open-folder', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerWorldGet: (id: string) =>
    ipcRenderer.invoke('solea-server:world-get', id) as Promise<
      | { ok: true; levelName: string; levelSeed: string; levelType: string }
      | { ok: false; error: string }
    >,
  soleaServerWorldSet: (payload: { id: string; levelName?: string; levelSeed?: string; levelType?: string }) =>
    ipcRenderer.invoke('solea-server:world-set', payload) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerWorldDelete: (id: string) =>
    ipcRenderer.invoke('solea-server:world-delete', id) as Promise<{ ok: true } | { ok: false; error: string }>,
  soleaServerPropertiesRawGet: (id: string) =>
    ipcRenderer.invoke('solea-server:properties-raw-get', id) as Promise<
      { ok: true; content: string } | { ok: false; error: string }
    >,
  soleaServerPropertiesRawSet: (payload: { id: string; content: string }) =>
    ipcRenderer.invoke('solea-server:properties-raw-set', payload) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  soleaServerJvmExtraGet: (id: string) =>
    ipcRenderer.invoke('solea-server:jvm-extra-get', id) as Promise<
      { ok: true; content: string } | { ok: false; error: string }
    >,
  soleaServerJvmExtraSet: (payload: { id: string; content: string }) =>
    ipcRenderer.invoke('solea-server:jvm-extra-set', payload) as Promise<
      { ok: true } | { ok: false; error: string }
    >,
  onSoleaServerEvent: (cb: (ev: Record<string, unknown>) => void) => {
    const listener = (_e: Electron.IpcRendererEvent, ev: Record<string, unknown>) => cb(ev)
    ipcRenderer.on('solea-server:event', listener)
    return () => ipcRenderer.removeListener('solea-server:event', listener)
  }
}

contextBridge.exposeInMainWorld('solea', api)
