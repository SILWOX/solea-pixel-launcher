export type ModpackGameProfileUI = {
  memoryMin: string
  memoryMax: string
  gameArgs: string
  screenWidth: number | null
  screenHeight: number | null
  fullscreen: boolean
}

export type UiLanguage = 'en' | 'fr'
export type UiTheme = 'light' | 'dark' | 'system'
export type UiFontScale = 's' | 'm' | 'l'
export type SkinViewerAnimation = 'none' | 'idle' | 'walk'
export type UpdateChannel = 'stable' | 'beta'

export type LauncherSettingsUI = {
  memoryMin: string
  memoryMax: string
  jvmArgs: string
  gameArgs: string
  downloadThreads: number
  networkTimeoutMs: number
  javaPath: string
  javaVersion: string
  screenWidth: number | null
  screenHeight: number | null
  fullscreen: boolean
  azureClientId: string
  afterLaunch: 'keep' | 'minimize'
  activeModpackId: string
  modpackProfiles: Record<string, ModpackGameProfileUI>
  uiLanguage: UiLanguage
  uiTheme: UiTheme
  uiAccentHex: string
  uiFontScale: UiFontScale
  uiReduceMotion: boolean
  /** Marges réduites (petits écrans). */
  uiCompact: boolean
  uiSounds: boolean
  /** 0–1 */
  uiSoundVolume: number
  uiSoundInstall: boolean
  uiSoundLaunch: boolean
  discordRichPresence: boolean
  updateChannel: UpdateChannel
  skinViewerAnimation: SkinViewerAnimation
  skinViewerBackground: string
}
