import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'
import { MODPACKS, resolveModpackId } from './modpacks.js'
import { parseLauncherSettingsFromDisk } from './settingsZod.js'

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
export type UiTheme = 'light' | 'dark' | 'system'
export type UiFontScale = 's' | 'm' | 'l'
export type SkinViewerAnimation = 'none' | 'idle' | 'walk'
export type UpdateChannel = 'stable' | 'beta'

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
  /** Modpack sélectionné dans la barre latérale */
  activeModpackId: string
  /** Réglages de jeu par id modpack (clés = ids internes). */
  modpackProfiles: Partial<Record<string, ModpackGameProfile>>
  /** UI */
  uiLanguage: UiLanguage
  uiTheme: UiTheme
  /** Couleur d’accent (#RRGGBB) */
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
  skinViewerBackground: string
}

export const DEFAULT_SETTINGS: LauncherSettings = {
  memoryMin: '2G',
  memoryMax: '6G',
  jvmArgs: '',
  gameArgs: '',
  downloadThreads: 8,
  networkTimeoutMs: 20000,
  javaPath: '',
  javaVersion: '21',
  screenWidth: null,
  screenHeight: null,
  fullscreen: false,
  azureClientId: '',
  afterLaunch: 'keep',
  activeModpackId: 'palamod-recreated',
  modpackProfiles: {},
  uiLanguage: 'en',
  uiTheme: 'dark',
  uiAccentHex: '#ff6a1a',
  uiFontScale: 'm',
  uiReduceMotion: false,
  uiCompact: false,
  uiSounds: true,
  uiSoundVolume: 1,
  uiSoundInstall: true,
  uiSoundLaunch: true,
  discordRichPresence: false,
  updateChannel: 'stable',
  skinViewerAnimation: 'none',
  skinViewerBackground: '#141416'
}

const RAM_RE = /^[0-9]+[mMgG]$/

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
    out[m.id] = { ...legacy, ...(s.modpackProfiles?.[m.id] ?? {}) }
  }
  return { ...s, modpackProfiles: out }
}

export function getGameSettingsForModpack(s: LauncherSettings, modpackId: string): ModpackGameProfile {
  const id = resolveModpackId(modpackId)
  const normalized = normalizeModpackProfiles(s)
  return normalized.modpackProfiles[id] ?? legacyGameSlice(s)
}

export function loadSettings(): LauncherSettings {
  const p = settingsPath()
  if (!existsSync(p)) return normalizeModpackProfiles({ ...DEFAULT_SETTINGS })
  try {
    const raw = JSON.parse(readFileSync(p, 'utf8')) as unknown
    const parsed = parseLauncherSettingsFromDisk(raw)
    return normalizeModpackProfiles({ ...DEFAULT_SETTINGS, ...parsed } as LauncherSettings)
  } catch {
    return normalizeModpackProfiles({ ...DEFAULT_SETTINGS })
  }
}

export function saveSettings(s: LauncherSettings): { ok: true } | { ok: false; error: string } {
  const merged = { ...DEFAULT_SETTINGS, ...s }
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

  if (next.downloadThreads < 1 || next.downloadThreads > 32) {
    return { ok: false, error: 'Téléchargements parallèles : entre 1 et 32.' }
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
  if (next.uiTheme !== 'light' && next.uiTheme !== 'dark' && next.uiTheme !== 'system') {
    return { ok: false, error: 'Thème invalide.' }
  }
  if (!/^#[0-9A-Fa-f]{6}$/.test((next.uiAccentHex || '').trim())) {
    return { ok: false, error: 'Couleur d’accent : format #RRGGBB attendu.' }
  }
  next.uiAccentHex = next.uiAccentHex.trim()
  if (next.uiFontScale !== 's' && next.uiFontScale !== 'm' && next.uiFontScale !== 'l') {
    return { ok: false, error: 'Échelle de police invalide.' }
  }
  if (typeof next.uiReduceMotion !== 'boolean') next.uiReduceMotion = DEFAULT_SETTINGS.uiReduceMotion
  if (typeof next.uiCompact !== 'boolean') next.uiCompact = DEFAULT_SETTINGS.uiCompact
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
  if (
    next.skinViewerAnimation !== 'none' &&
    next.skinViewerAnimation !== 'idle' &&
    next.skinViewerAnimation !== 'walk'
  ) {
    return { ok: false, error: 'Animation skin invalide.' }
  }
  if (typeof next.skinViewerBackground !== 'string' || !next.skinViewerBackground.trim()) {
    next.skinViewerBackground = DEFAULT_SETTINGS.skinViewerBackground
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
