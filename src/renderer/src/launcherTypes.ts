export type ModpackGameProfileUI = {
  memoryMin: string
  memoryMax: string
  gameArgs: string
  screenWidth: number | null
  screenHeight: number | null
  fullscreen: boolean
}

export type UiLanguage = 'en' | 'fr'
export type UiTheme =
  | 'light'
  | 'dark'
  | 'system'
  | 'amber'
  | 'midnight'
  | 'high_contrast'
  | 'forest'
  | 'nether'
  | 'end'
  | 'paper'
  | 'ocean'
  | 'monochrome'
  | 'solarized'
export type UiFontScale = 's' | 'm' | 'l'
/** Valeurs persistées ; alignées sur skinview3d. */
export const SKIN_VIEWER_ANIMATION_VALUES = [
  'none',
  'idle',
  'walk',
  'run',
  'fly',
  'wave',
  'wave_left',
  'crouch',
  'hit'
] as const
export type SkinViewerAnimation = (typeof SKIN_VIEWER_ANIMATION_VALUES)[number]
export type UpdateChannel = 'stable' | 'beta'

/** Barre latérale + carte d’accueil : v2.0 Studio (défaut) ou v1.0 Classique (legacy App.css). */
export type UiHomeCardVariant = 'studio' | 'classic'

/** Onglet Paramètres : refonte AETHER 2.0 ou apparence d’origine (Legacy). */
export type UiSettingsShellVariant = 'aether2' | 'legacy'

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
  /** Ouvre la console de logs au démarrage d’une instance Minecraft. */
  openGameLogOnInstanceLaunch: boolean
  activeModpackId: string
  modpackProfiles: Record<string, ModpackGameProfileUI>
  /** Parent absolu pour `{parent}/.soleapixel/instances/…` ; vide = défaut sous le dossier du launcher. */
  modpackInstanceParentPath: Partial<Record<string, string>>
  uiLanguage: UiLanguage
  uiTheme: UiTheme
  uiAccentHex: string
  uiFontScale: UiFontScale
  uiReduceMotion: boolean
  /** Marges réduites (petits écrans). */
  uiCompact: boolean
  /** Barre latérale + carte d’accueil (v2.0 Studio vs v1.0 classique). */
  uiHomeCardVariant: UiHomeCardVariant
  /** Barre titre + sidebar semi-transparentes avec flou ; le fond s’étend derrière. */
  uiChromeGlass: boolean
  /** Écran Paramètres : AETHER UI 2.0 (défaut) ou Legacy (style avant refonte). */
  uiSettingsShell: UiSettingsShellVariant
  uiSounds: boolean
  /** 0–1 */
  uiSoundVolume: number
  uiSoundInstall: boolean
  uiSoundLaunch: boolean
  discordRichPresence: boolean
  updateChannel: UpdateChannel
  skinViewerAnimation: SkinViewerAnimation
  uiShortcutOpenSettings: string
  uiShortcutGoNews: string
  uiShortcutGoAccount: string
  nativeNotifications: boolean
  diagnosticLaunch: boolean
  networkSlowDownloads: boolean
}

/** Ligne renvoyée par `modpack:all-action-info` (un entrée par modpack déclaré). */
export type ModpackActionInfoRow = {
  id: string
  displayName: string
  needsInstall: boolean
  needsUpdate: boolean
  error?: string
  installedVersionNumber?: string
  latestVersionNumber?: string
}

/** Options envoyées au process principal lors d’une réinstallation (conserver des données locales). */
export type ReinstallPreserveOptions = {
  keepSaves: boolean
  keepScreenshots: boolean
  keepOptions: boolean
}
