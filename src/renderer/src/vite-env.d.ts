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
}

type LS = import('./launcherTypes').LauncherSettingsUI

export type SoleaApi = {
  windowMinimize: () => Promise<void>
  windowToggleMaximize: () => Promise<{ maximized: boolean }>
  windowClose: () => Promise<void>
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
  }>
  setActiveModpack: (
    id: string
  ) => Promise<{ ok: true; activeModpackId: string } | { ok: false; error: string }>
  getAuthState: () => Promise<{ requiresMicrosoftLogin: boolean }>
  listAccounts: () => Promise<{ uuid: string; name: string }[]>
  getActiveAccount: () => Promise<{ name: string; uuid: string } | null>
  addAccount: () => Promise<
    { ok: true; name: string; uuid: string } | { ok: false; reason: string; detail?: string }
  >
  setActiveAccount: (uuid: string) => Promise<{ ok: true } | { ok: false; error: string }>
  removeAccount: (uuid: string) => Promise<{ ok: true }>
  refreshActiveAccount: () => Promise<{ ok: true; name: string } | { ok: false; error: string }>
  getSettings: () => Promise<LS>
  saveSettings: (partial: Partial<LS>) => Promise<{ ok: true } | { ok: false; error: string }>
  resetSettings: () => Promise<{ ok: true } | { ok: false; error: string }>
  installModpack: () => Promise<{ ok: true } | { ok: false; error: string }>
  reinstallModpack: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  uninstallModpack: (id: string) => Promise<{ ok: true } | { ok: false; error: string }>
  openExternalUrl: (url: string) => Promise<void>
  verifyModpack: () => Promise<
    { ok: true } | { ok: false; reason: string; detail?: string; paths?: string[] }
  >
  getModpackActionInfo: () => Promise<{
    needsInstall: boolean
    needsUpdate: boolean
    installedVersionNumber?: string
    latestVersionNumber?: string
    error?: string
  }>
  isGameRunning: () => Promise<boolean>
  stopGame: () => Promise<{ ok: true } | { ok: false; error: string }>
  launch: () => Promise<{ ok: true } | { ok: false; error: string }>
  openInstanceFolder: () => Promise<{ ok: true } | { ok: false; error: string }>
  openUserDataFolder: () => Promise<{ ok: true } | { ok: false; error: string }>
  onInstallProgress: (cb: (p: InstallProgressPayload) => void) => () => void
  onGameLog: (cb: (line: string) => void) => () => void
  getGameLogSnapshot: () => Promise<string>
  openGameLogWindow: () => Promise<{ ok: true }>
  onGameExited: (cb: () => void) => () => void
  getAppVersion: () => Promise<string>
  checkForUpdates: () => Promise<{ ok: true; started: boolean }>
  downloadUpdate: () => Promise<{ ok: true } | { ok: false; error: string }>
  quitAndInstall: () => Promise<{ ok: true }>
  onUpdaterAvailable: (
    cb: (payload: { version: string; releaseNotes?: string | string[] | null }) => void
  ) => () => void
  onUpdaterNotAvailable: (cb: () => void) => () => void
  onUpdaterDownloaded: (cb: () => void) => () => void
  onUpdaterError: (cb: (msg: string) => void) => () => void
}

declare global {
  interface Window {
    solea: SoleaApi
  }
}

export {}
