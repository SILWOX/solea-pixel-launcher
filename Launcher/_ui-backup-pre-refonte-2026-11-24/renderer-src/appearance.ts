/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import {
  SOLEA_ACCENT_BEE_GOLD,
  SOLEA_ACCENT_BEE_GOLD_AMBER,
  SOLEA_ACCENT_BEE_GOLD_BRIGHT,
  SOLEA_ACCENT_BEE_GOLD_DEEP,
  SOLEA_ACCENT_LEGACY_ORANGE,
  SOLEA_ACCENT_LEGACY_ORANGE_DEEP,
  isLegacyOrangeAccent
} from '../../shared/soleaBrandColors.js'
import type { LauncherSettingsUI, UiTheme } from './launcherTypes'

/** Luminosité pour data-theme (sélecteurs CSS). */
function resolveDataTheme(choice: UiTheme): 'light' | 'dark' {
  if (choice === 'light') return 'light'
  if (choice === 'paper') return 'light'
  if (choice === 'dark') return 'dark'
  if (choice === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }
  return 'dark'
}

/** Valeur de `data-ui-preset` sur `<html>` (surcharges CSS). */
type UiPreset =
  | 'studio'
  | 'amber'
  | 'solea_pixel'
  | 'midnight'
  | 'high_contrast'
  | 'forest'
  | 'nether'
  | 'end'
  | 'paper'
  | 'ocean'
  | 'monochrome'
  | 'solarized'

function resolveUiPreset(choice: UiTheme): UiPreset {
  switch (choice) {
    case 'amber':
      return 'amber'
    case 'solea_pixel':
      return 'solea_pixel'
    case 'midnight':
      return 'midnight'
    case 'high_contrast':
      return 'high_contrast'
    case 'forest':
      return 'forest'
    case 'nether':
      return 'nether'
    case 'end':
      return 'end'
    case 'paper':
      return 'paper'
    case 'ocean':
      return 'ocean'
    case 'monochrome':
      return 'monochrome'
    case 'solarized':
      return 'solarized'
    default:
      return 'studio'
  }
}

/**
 * Accent principal : jaune abeille / or (thèmes par défaut).
 * Orange legacy uniquement avec le thème « Ancien SOLEA PIXEL ».
 */
export function resolveAccentHex(s: LauncherSettingsUI): string {
  if (s.uiTheme === 'solea_pixel') return SOLEA_ACCENT_LEGACY_ORANGE
  if (s.uiTheme === 'monochrome') {
    const raw = (s.uiAccentHex || '').trim()
    if (/^#[0-9A-Fa-f]{6}$/.test(raw) && !isLegacyOrangeAccent(raw)) return raw
  }
  return SOLEA_ACCENT_BEE_GOLD
}

function parseHexRgb(hex: string): [number, number, number] {
  const h = hex.replace('#', '')
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16)
  ]
}

function toHex(r: number, g: number, b: number): string {
  return `#${[r, g, b].map((v) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0')).join('')}`
}

/** Variantes dégradé bouton dérivées d’une couleur d’accent (thème Minuit, Forêt, etc.). */
function mixAccentTowardWhite(hex: string, whiteMix: number): string {
  const [r, g, b] = parseHexRgb(hex)
  const t = Math.max(0, Math.min(1, whiteMix))
  return toHex(r + (255 - r) * t, g + (255 - g) * t, b + (255 - b) * t)
}

function mixAccentTowardBlack(hex: string, blackMix: number): string {
  const [r, g, b] = parseHexRgb(hex)
  const t = Math.max(0, Math.min(1, blackMix))
  return toHex(r * (1 - t), g * (1 - t), b * (1 - t))
}

function accentIsLight(hex: string): boolean {
  const [r, g, b] = parseHexRgb(hex)
  return (0.299 * r + 0.587 * g + 0.114 * b) / 255 > 0.62
}

function applyPrimaryButtonFg(root: HTMLElement, accent: string): void {
  const light = accentIsLight(accent)
  root.style.setProperty('--v3-primary-btn-fg', light ? '#1a1008' : '#fff')
  root.style.setProperty(
    '--v3-primary-btn-text-shadow',
    light ? '0 1px 0 rgba(255, 255, 255, 0.42)' : '0 1px 2px rgba(0, 0, 0, 0.32)'
  )
}

/** Or abeille fixe (thème studio / défaut AETHER). */
function applyBeeGoldTokens(root: HTMLElement): void {
  const accent = SOLEA_ACCENT_BEE_GOLD
  root.style.setProperty('--accent', accent)
  root.style.setProperty('--accent-orange', accent)
  root.style.setProperty('--accent-bee', SOLEA_ACCENT_BEE_GOLD)
  root.style.setProperty('--accent-bee-bright', SOLEA_ACCENT_BEE_GOLD_BRIGHT)
  root.style.setProperty('--accent-bee-deep', SOLEA_ACCENT_BEE_GOLD_DEEP)
  root.style.setProperty('--accent-bee-amber', SOLEA_ACCENT_BEE_GOLD_AMBER)
  root.style.setProperty(
    '--accent-orange-hover',
    `color-mix(in srgb, ${SOLEA_ACCENT_BEE_GOLD_BRIGHT} 88%, white)`
  )
  root.style.setProperty('--accent-glow', `color-mix(in srgb, ${accent} 52%, transparent)`)
  applyPrimaryButtonFg(root, accent)
}

/** Accent chrome = couleur du préréglage (boutons, toggles, liserés). */
function applyThemeChromeTokens(root: HTMLElement, accent: string): void {
  root.style.setProperty('--accent', accent)
  root.style.setProperty('--accent-orange', accent)
  root.style.setProperty('--accent-bee', accent)
  root.style.setProperty('--accent-bee-bright', mixAccentTowardWhite(accent, 0.22))
  root.style.setProperty('--accent-bee-deep', mixAccentTowardBlack(accent, 0.2))
  root.style.setProperty('--accent-bee-amber', mixAccentTowardBlack(accent, 0.1))
  root.style.setProperty(
    '--accent-orange-hover',
    `color-mix(in srgb, ${mixAccentTowardWhite(accent, 0.18)} 88%, white)`
  )
  root.style.setProperty('--accent-glow', `color-mix(in srgb, ${accent} 52%, transparent)`)
  applyPrimaryButtonFg(root, accent)
}

function applyLegacyOrangeTokens(root: HTMLElement): void {
  root.style.setProperty('--accent', SOLEA_ACCENT_LEGACY_ORANGE)
  root.style.setProperty('--accent-orange', SOLEA_ACCENT_LEGACY_ORANGE)
  root.style.setProperty('--accent-bee', SOLEA_ACCENT_LEGACY_ORANGE)
  root.style.setProperty('--accent-bee-bright', '#ff8a3d')
  root.style.setProperty('--accent-bee-deep', SOLEA_ACCENT_LEGACY_ORANGE_DEEP)
  root.style.setProperty('--accent-bee-amber', SOLEA_ACCENT_LEGACY_ORANGE)
  root.style.setProperty(
    '--accent-orange-hover',
    `color-mix(in srgb, ${SOLEA_ACCENT_LEGACY_ORANGE} 82%, white)`
  )
  root.style.setProperty(
    '--accent-glow',
    `color-mix(in srgb, ${SOLEA_ACCENT_LEGACY_ORANGE} 48%, transparent)`
  )
  applyPrimaryButtonFg(root, SOLEA_ACCENT_LEGACY_ORANGE)
}

/** Chrome global : couleur imposée par le préréglage (palettes non-studio). */
const PRESET_CHROME_HEX: Record<
  Exclude<UiPreset, 'studio' | 'monochrome' | 'solea_pixel'>,
  string
> = {
  amber: '#e8a050',
  midnight: '#6d8cff',
  high_contrast: '#ffe14a',
  forest: '#5cb88a',
  nether: '#ef5f4d',
  end: '#a593f0',
  paper: '#6b4f3d',
  ocean: '#3bb4f7',
  solarized: '#3db39a'
}

/** Applique data-* sur document.documentElement (thème, accent, échelle, mouvement). */
export function applyAppearanceSettings(s: LauncherSettingsUI): void {
  const root = document.documentElement
  const choice = s.uiTheme
  root.dataset.theme = resolveDataTheme(choice)
  const preset = resolveUiPreset(choice)
  root.dataset.uiPreset = preset
  const prefersReduced =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  root.dataset.reduceMotion = s.uiReduceMotion || prefersReduced ? '1' : '0'
  root.dataset.density = s.uiCompact ? 'compact' : 'comfortable'
  const anyGlass = Boolean(s.uiChromeGlass || s.uiLiquidGlass)
  root.dataset.chromeGlass = anyGlass ? '1' : '0'
  root.dataset.liquidGlass = s.uiLiquidGlass ? '1' : '0'
  root.dataset.settingsShell = s.uiSettingsShell === 'legacy' ? 'legacy' : 'aether2'

  const accent = resolveAccentHex(s)

  if (preset === 'solea_pixel') {
    applyLegacyOrangeTokens(root)
  } else if (preset === 'studio') {
    applyBeeGoldTokens(root)
  } else if (preset === 'monochrome') {
    applyThemeChromeTokens(root, accent)
  } else {
    const chrome = PRESET_CHROME_HEX[preset as keyof typeof PRESET_CHROME_HEX]
    if (chrome) applyThemeChromeTokens(root, chrome)
    else applyBeeGoldTokens(root)
  }

  const scale = s.uiFontScale === 's' ? '0.92' : s.uiFontScale === 'l' ? '1.08' : '1'
  root.style.setProperty('--ui-font-scale', scale)
}

export function subscribeSystemTheme(onChange: () => void): () => void {
  const m = window.matchMedia('(prefers-color-scheme: dark)')
  const fn = () => onChange()
  m.addEventListener('change', fn)
  return () => m.removeEventListener('change', fn)
}
