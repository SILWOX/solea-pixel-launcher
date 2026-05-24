import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'
import { MODPACKS, resolveModpackId } from './modpacks.js'
import { parseLauncherSettingsFromDisk } from './settingsZod.js'
import { validateModpackInstanceParentPaths } from './instancePaths.js'

/** RAM, résolution et arguments de lancement Minecraft — un jeu par modpack. */
export interface ModpackGameProfile {
  memoryMin: string
  memoryMax: string
  gameArgs: string
  screenWidth: number | null
  screenHeight: number | null
  fullscreen: boolean
}

export type UiLanguage = 'en' | 'fr'
/** Thème visuel complet (clair / sombre / système + palettes AETHER UI et thèmes étendus). */
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
export type SkinViewerAnimation =
  | 'none'
  | 'idle'
  | 'walk'
  | 'run'
  | 'fly'
  | 'wave'
  | 'wave_left'
  | 'crouch'
  | 'hit'
export type UpdateChannel = 'stable' | 'beta'
export type UiHomeCardVariant = 'studio' | 'classic' | 'beta'
export type UiSettingsShellVariant = 'aether2' | 'legacy'

export interface LauncherSettings {
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
  /** Optionnel : ID d’application Azure AD (sinon client par défaut de la lib). */
  azureClientId: string
  /** Comportement du launcher après avoir lancé le jeu */
  afterLaunch: 'keep' | 'minimize'
  /** Ouvrir la fenêtre console de lancement dès qu’une instance démarre. */
  openGameLogOnInstanceLaunch: boolean
  /** Modpack sélectionné dans la barre latérale */
  activeModpackId: string
  /** Réglages de jeu par id modpack (clés = ids internes). */
  modpackProfiles: Partial<Record<string, ModpackGameProfile>>
  /**
   * Dossier parent choisi pour une instance hors AppData : l’instance sera dans
   * `{parent}/.soleapixel/instances/{slug}`. Absent = défaut sous userData (`instances/{slug}`).
   */
  modpackInstanceParentPath: Partial<Record<string, string>>
  /** UI */
  uiLanguage: UiLanguage
  uiTheme: UiTheme
  /** Couleur d’accent (#RRGGBB) — boutons primaires, liens ; indépendante du thème visuel. */
  uiAccentHex: string
  uiFontScale: UiFontScale
  uiReduceMotion: boolean
  uiCompact: boolean
  uiSounds: boolean
  /** 0–1, multiplicateur du volume des sons UI (si uiSounds). */
  uiSoundVolume: number
  /** Son après install / mise à jour du pack. */
  uiSoundInstall: boolean
  /** Son après lancement du jeu. */
  uiSoundLaunch: boolean
  discordRichPresence: boolean
  updateChannel: UpdateChannel
  skinViewerAnimation: SkinViewerAnimation
  /** Raccourcis (format type Electron : CommandOrControl+Comma). */
  uiShortcutOpenSettings: string
  uiShortcutGoNews: string
  uiShortcutGoAccount: string
  /** Notifications système (Windows / macOS) pour install finie, maj dispo. */
  nativeNotifications: boolean
  /** Entrée UI « Mes serveurs » et fonctions associées (désactivable). */
  experimentalServerSystemEnabled: boolean
  /** Lancement avec tas JVM réduit (support / debug). */
  diagnosticLaunch: boolean
  /** Limite les téléchargements parallèles (réseau lent). */
  networkSlowDownloads: boolean
  /** Barre titre + sidebar vitrées (flou + transparence). */
  uiChromeGlass: boolean
  /** Barre latérale + carte accueil : v2.0 (studio) ou v1.0 legacy (classic). */
  uiHomeCardVariant: UiHomeCardVariant
  /** Onglet Paramètres : AETHER 2.0 ou Legacy. */
  uiSettingsShell: UiSettingsShellVariant
}

export const DEFAULT_SETTINGS: LauncherSettings = {
  memoryMin: '2G',
  memoryMax: '6G',
  jvmArgs: '',
  gameArgs: '',
  downloadThreads: 16,
  networkTimeoutMs: 20000,
  javaPath: '',
  javaVersion: '21',
  screenWidth: 800,
  screenHeight: 600,
  fullscreen: false,
  azureClientId: '',
  afterLaunch: 'keep',
  activeModpackId: 'palamod-recreated',
  modpackProfiles: {},
  modpackInstanceParentPath: {},
  uiLanguage: 'en',
  uiTheme: 'dark',
  uiAccentHex: '#ff6a1a',
  uiFontScale: 'm',
  uiReduceMotion: false,
  uiCompact: false,
  uiHomeCardVariant: 'studio',
  uiSounds: true,
  uiSoundVolume: 1,
  uiSoundInstall: true,
  uiSoundLaunch: true,
  discordRichPresence: true,
  updateChannel: 'stable',
  skinViewerAnimation: 'none',
  uiShortcutOpenSettings: 'CommandOrControl+Comma',
  uiShortcutGoNews: 'CommandOrControl+Shift+KeyH',
  uiShortcutGoAccount: 'CommandOrControl+Shift+KeyU',
  nativeNotifications: true,
  experimentalServerSystemEnabled: true,
  diagnosticLaunch: false,
  networkSlowDownloads: false,
  uiChromeGlass: false,
  uiSettingsShell: 'aether2',
  openGameLogOnInstanceLaunch: false
}

/** Thèmes persistés ; alias éventuels → clé canonique. */
const ALL_UI_THEMES: readonly UiTheme[] = [
  'light',
  'dark',
  'system',
  'amber',
  'midnight',
  'high_contrast',
  'forest',
  'nether',
  'end',
  'paper',
  'ocean',
  'monochrome',
  'solarized'
]
const UI_THEME_ALIAS: Record<string, UiTheme> = {
  'high-contrast': 'high_contrast'
}

/** Ramène toute valeur reçue (IPC, JSON) vers un `uiTheme` valide — évite « Thème invalide » si clone / type incorrect. */
export function normalizeUiThemeValue(v: unknown): UiTheme {
  if (typeof v === 'string') {
    const raw = v.trim().normalize('NFKC')
    if (raw === '') return DEFAULT_SETTINGS.uiTheme
    const mapped = UI_THEME_ALIAS[raw] ?? (raw as UiTheme)
    return (ALL_UI_THEMES as readonly string[]).includes(mapped) ? mapped : DEFAULT_SETTINGS.uiTheme
  }
  if (v === undefined || v === null) return DEFAULT_SETTINGS.uiTheme
  return DEFAULT_SETTINGS.uiTheme
}

/**
 * Clés pour lesquelles `null` dans le patch ne doit pas écraser la base (`screenWidth` peut être null volontairement).
 */
const PATCH_NULL_OMIT_KEYS = new Set([
  'uiTheme',
  'uiLanguage',
  'uiFontScale',
  'updateChannel',
  'skinViewerAnimation',
  'afterLaunch',
  'activeModpackId'
])

/**
 * Fusionne un patch sans écraser avec `undefined` : `{ ...base, ...{ k: undefined } }` enlèverait sinon
 * la valeur de base (ex. `uiTheme` → validation « Thème invalide » après IPC / clone).
 */
export function mergeLauncherSettingsPatch(
  base: LauncherSettings,
  patch: Record<string, unknown>
): LauncherSettings {
  if (Object.prototype.hasOwnProperty.call(patch, 'modpackInstanceParentPath')) {
    const raw = patch.modpackInstanceParentPath
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
      const merged: Record<string, string> = { ...(base.modpackInstanceParentPath ?? {}) }
      for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
        if (v === null || v === '') delete merged[k]
        else if (typeof v === 'string') {
          const t = v.trim()
          if (t === '') delete merged[k]
          else merged[k] = t
        }
      }
      patch = { ...patch, modpackInstanceParentPath: merged }
    }
  }
  const clean: Record<string, unknown> = {}
  for (const key of Object.keys(patch)) {
    const v = patch[key]
    if (v === undefined) continue
    if (v === null && PATCH_NULL_OMIT_KEYS.has(key)) continue
    clean[key] = v
  }
  return { ...base, ...clean } as LauncherSettings
}

const RAM_RE = /^[0-9]+[mMgG]$/

/** Accélérateur type Electron (ex. CommandOrControl+Shift+KeyH). */
export function isValidUiShortcutAccel(s: string): boolean {
  const t = s.trim()
  if (t.length < 2 || t.length > 120) return false
  const parts = t.split('+').map((p) => p.trim())
  if (parts.length < 2) return false
  const mods = parts.slice(0, -1)
  const key = parts[parts.length - 1]!
  if (!key || !/^(Key[A-Z]|Digit[0-9]|F[1-9]|F1[0-2]|Comma|Period|Minus|Equal|Slash|BracketLeft|BracketRight|Backslash|Semicolon|Quote|Backquote|Tab|Space|Enter|Escape|Backspace|Delete|Insert|Home|End|PageUp|PageDown|ArrowUp|ArrowDown|ArrowLeft|ArrowRight|Numpad[0-9]|NumpadAdd|NumpadSubtract|Plus|IntlBackslash)$/.test(key)) {
    return false
  }
  const allowedMod = (m: string) =>
    ['CommandOrControl', 'CmdOrCtrl', 'Control', 'Ctrl', 'Command', 'Cmd', 'Shift', 'Alt', 'Option'].includes(m)
  return mods.length > 0 && mods.every(allowedMod)
}

export function isValidRam(s: string): boolean {
  return RAM_RE.test(s.trim())
}

function settingsPath(): string {
  return join(app.getPath('userData'), 'launcher-settings.json')
}

function legacyGameSlice(s: LauncherSettings): ModpackGameProfile {
  return {
    memoryMin: s.memoryMin,
    memoryMax: s.memoryMax,
    gameArgs: s.gameArgs,
    screenWidth: s.screenWidth,
    screenHeight: s.screenHeight,
    fullscreen: s.fullscreen
  }
}

/** Complète chaque modpack connu ; champs racine servent de repli (fichiers anciens). */
export function normalizeModpackProfiles(s: LauncherSettings): LauncherSettings {
  const legacy = legacyGameSlice(s)
  const out: Record<string, ModpackGameProfile> = { ...(s.modpackProfiles as Record<string, ModpackGameProfile>) }
  for (const m of MODPACKS) {
    const merged = { ...legacy, ...(s.modpackProfiles?.[m.id] ?? {}) }
    out[m.id] = {
      ...merged,
      /** Comportement proche du premier lancement vanilla : 800×600 si non défini. */
      screenWidth: merged.screenWidth ?? 800,
      screenHeight: merged.screenHeight ?? 600
    }
  }
  return { ...s, modpackProfiles: out }
}

export function getGameSettingsForModpack(s: LauncherSettings, modpackId: string): ModpackGameProfile {
  const id = resolveModpackId(modpackId)
  const normalized = normalizeModpackProfiles(s)
  return normalized.modpackProfiles[id] ?? legacyGameSlice(s)
}

/** Anciens fichiers : uiAppearancePreset séparé → un seul uiTheme. */
function migrateLegacyUiTheme(raw: unknown, merged: LauncherSettings): LauncherSettings {
  if (typeof raw !== 'object' || raw === null) return merged
  const o = raw as Record<string, unknown>
  const preset = o.uiAppearancePreset
  if (typeof preset !== 'string') return merged
  if (preset === 'amber') return { ...merged, uiTheme: 'amber' }
  if (preset === 'midnight') return { ...merged, uiTheme: 'midnight' }
  if (preset === 'high_contrast') return { ...merged, uiTheme: 'high_contrast' }
  return merged
}

function normalizeShortcutFields(s: LauncherSettings): LauncherSettings {
  const fix = (v: string, d: string) => (isValidUiShortcutAccel(v) ? v.trim() : d)
  return {
    ...s,
    uiShortcutOpenSettings: fix(s.uiShortcutOpenSettings, DEFAULT_SETTINGS.uiShortcutOpenSettings),
    uiShortcutGoNews: fix(s.uiShortcutGoNews, DEFAULT_SETTINGS.uiShortcutGoNews),
    uiShortcutGoAccount: fix(s.uiShortcutGoAccount, DEFAULT_SETTINGS.uiShortcutGoAccount)
  }
}

export function loadSettings(): LauncherSettings {
  const p = settingsPath()
  if (!existsSync(p)) return normalizeShortcutFields(normalizeModpackProfiles({ ...DEFAULT_SETTINGS }))
  try {
    const raw = JSON.parse(readFileSync(p, 'utf8')) as unknown
    const parsed = parseLauncherSettingsFromDisk(raw)
    let merged = normalizeModpackProfiles({ ...DEFAULT_SETTINGS, ...parsed } as LauncherSettings)
    merged = migrateLegacyUiTheme(raw, merged)
    merged = { ...merged, uiTheme: normalizeUiThemeValue(merged.uiTheme) }
    return normalizeShortcutFields(merged)
  } catch {
    return normalizeShortcutFields(normalizeModpackProfiles({ ...DEFAULT_SETTINGS }))
  }
}

export function saveSettings(s: LauncherSettings): { ok: true } | { ok: false; error: string } {
  const merged = mergeLauncherSettingsPatch(DEFAULT_SETTINGS, s as Record<string, unknown>)
  const next = normalizeModpackProfiles(merged)

  for (const m of MODPACKS) {
    const prof = next.modpackProfiles[m.id]
    if (!prof) return { ok: false, error: `Profil jeu manquant : ${m.id}.` }
    if (!isValidRam(prof.memoryMin)) return { ok: false, error: `RAM min invalide (${m.displayName}).` }
    if (!isValidRam(prof.memoryMax)) return { ok: false, error: `RAM max invalide (${m.displayName}).` }
    const minN = parseRamToMb(prof.memoryMin)
    const maxN = parseRamToMb(prof.memoryMax)
    if (minN > maxN) return { ok: false, error: `RAM min > max (${m.displayName}).` }
    if (prof.screenWidth !== null && (prof.screenWidth < 640 || prof.screenWidth > 7680)) {
      return { ok: false, error: `Largeur fenêtre invalide (${m.displayName}).` }
    }
    if (prof.screenHeight !== null && (prof.screenHeight < 480 || prof.screenHeight > 4320)) {
      return { ok: false, error: `Hauteur fenêtre invalide (${m.displayName}).` }
    }
  }

  if (next.downloadThreads < 1 || next.downloadThreads > 48) {
    return { ok: false, error: 'Téléchargements parallèles : entre 1 et 48.' }
  }
  if (next.networkTimeoutMs < 5000 || next.networkTimeoutMs > 120000) {
    return { ok: false, error: 'Timeout réseau : entre 5000 et 120000 ms.' }
  }
  if (next.afterLaunch !== 'keep' && next.afterLaunch !== 'minimize') {
    return { ok: false, error: 'Option après lancement invalide.' }
  }
  if (next.uiLanguage !== 'en' && next.uiLanguage !== 'fr') {
    return { ok: false, error: 'Langue UI invalide.' }
  }
  next.uiTheme = normalizeUiThemeValue(next.uiTheme)
  if (!/^#[0-9A-Fa-f]{6}$/.test((next.uiAccentHex || '').trim())) {
    return { ok: false, error: 'Couleur d’accent : format #RRGGBB attendu.' }
  }
  next.uiAccentHex = next.uiAccentHex.trim()
  if (next.uiFontScale !== 's' && next.uiFontScale !== 'm' && next.uiFontScale !== 'l') {
    return { ok: false, error: 'Échelle de police invalide.' }
  }
  if (typeof next.uiReduceMotion !== 'boolean') next.uiReduceMotion = DEFAULT_SETTINGS.uiReduceMotion
  if (typeof next.uiCompact !== 'boolean') next.uiCompact = DEFAULT_SETTINGS.uiCompact
  if (typeof next.uiChromeGlass !== 'boolean') next.uiChromeGlass = DEFAULT_SETTINGS.uiChromeGlass
  if (
    next.uiHomeCardVariant !== 'studio' &&
    next.uiHomeCardVariant !== 'classic' &&
    next.uiHomeCardVariant !== 'beta'
  ) {
    next.uiHomeCardVariant = DEFAULT_SETTINGS.uiHomeCardVariant
  }
  if (next.uiSettingsShell !== 'aether2' && next.uiSettingsShell !== 'legacy') {
    next.uiSettingsShell = DEFAULT_SETTINGS.uiSettingsShell
  }
  if (typeof next.uiSounds !== 'boolean') next.uiSounds = DEFAULT_SETTINGS.uiSounds
  if (typeof next.uiSoundVolume !== 'number' || Number.isNaN(next.uiSoundVolume)) {
    next.uiSoundVolume = DEFAULT_SETTINGS.uiSoundVolume
  }
  next.uiSoundVolume = Math.min(1, Math.max(0, next.uiSoundVolume))
  if (typeof next.uiSoundInstall !== 'boolean') next.uiSoundInstall = DEFAULT_SETTINGS.uiSoundInstall
  if (typeof next.uiSoundLaunch !== 'boolean') next.uiSoundLaunch = DEFAULT_SETTINGS.uiSoundLaunch
  if (typeof next.discordRichPresence !== 'boolean') {
    next.discordRichPresence = DEFAULT_SETTINGS.discordRichPresence
  }
  if (next.updateChannel !== 'stable' && next.updateChannel !== 'beta') {
    return { ok: false, error: 'Canal de mise à jour invalide.' }
  }
  const animOk: SkinViewerAnimation[] = [
    'none',
    'idle',
    'walk',
    'run',
    'fly',
    'wave',
    'wave_left',
    'crouch',
    'hit'
  ]
  if (!animOk.includes(next.skinViewerAnimation)) {
    return { ok: false, error: 'Animation skin invalide.' }
  }
  const accelKeys: (keyof LauncherSettings)[] = [
    'uiShortcutOpenSettings',
    'uiShortcutGoNews',
    'uiShortcutGoAccount'
  ]
  for (const k of accelKeys) {
    const v = next[k]
    if (typeof v !== 'string' || !isValidUiShortcutAccel(v)) {
      ;(next as Record<string, unknown>)[k as string] = DEFAULT_SETTINGS[k]
    } else {
      ;(next as Record<string, unknown>)[k as string] = v.trim()
    }
  }
  if (typeof next.nativeNotifications !== 'boolean') {
    next.nativeNotifications = DEFAULT_SETTINGS.nativeNotifications
  }
  if (typeof next.experimentalServerSystemEnabled !== 'boolean') {
    next.experimentalServerSystemEnabled = DEFAULT_SETTINGS.experimentalServerSystemEnabled
  }
  if (typeof next.diagnosticLaunch !== 'boolean') {
    next.diagnosticLaunch = DEFAULT_SETTINGS.diagnosticLaunch
  }
  if (typeof next.openGameLogOnInstanceLaunch !== 'boolean') {
    next.openGameLogOnInstanceLaunch = DEFAULT_SETTINGS.openGameLogOnInstanceLaunch
  }
  if (typeof next.networkSlowDownloads !== 'boolean') {
    next.networkSlowDownloads = DEFAULT_SETTINGS.networkSlowDownloads
  }
  if (!next.modpackInstanceParentPath || typeof next.modpackInstanceParentPath !== 'object') {
    next.modpackInstanceParentPath = {}
  }
  const parentPathsOk = validateModpackInstanceParentPaths(next)
  if (!parentPathsOk.ok) {
    return { ok: false, error: parentPathsOk.error }
  }
  next.activeModpackId = resolveModpackId(
    typeof next.activeModpackId === 'string' ? next.activeModpackId : DEFAULT_SETTINGS.activeModpackId
  )

  const activeId = next.activeModpackId
  const activeProf = next.modpackProfiles[activeId]!
  const serializable: LauncherSettings = {
    ...next,
    memoryMin: activeProf.memoryMin,
    memoryMax: activeProf.memoryMax,
    gameArgs: activeProf.gameArgs,
    screenWidth: activeProf.screenWidth,
    screenHeight: activeProf.screenHeight,
    fullscreen: activeProf.fullscreen
  }

  mkdirSync(app.getPath('userData'), { recursive: true })
  writeFileSync(settingsPath(), JSON.stringify(serializable, null, 2), 'utf8')
  return { ok: true }
}

function parseRamToMb(s: string): number {
  const t = s.trim()
  const n = parseInt(t, 10)
  if (t.toUpperCase().endsWith('G')) return n * 1024
  return n
}

export function parseArgsBlock(text: string): string[] {
  return text
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
}

export function getEffectiveMicrosoftClientId(settings: LauncherSettings): string {
  const fromEnv = process.env.AZURE_CLIENT_ID?.trim()
  if (fromEnv) return fromEnv
  return settings.azureClientId?.trim() ?? ''
}
