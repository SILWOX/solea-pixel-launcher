/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
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

/** Couleur d’accent UI (réglage « couleur d’accent ») — bouton Jouer, pastilles modpack Palamod, etc. */
function resolveAccentHex(s: LauncherSettingsUI): string {
  const raw = (s.uiAccentHex || '').trim()
  if (/^#[0-9A-Fa-f]{6}$/.test(raw)) return raw
  return '#ff6a1a'
}

/** Chrome global : couleur imposée par le préréglage (pas la couleur d’accent utilisateur). */
const PRESET_CHROME_HEX: Record<
  Exclude<UiPreset, 'studio' | 'monochrome'>,
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

function presetUsesUserChrome(preset: UiPreset): boolean {
  return preset === 'studio' || preset === 'monochrome'
}

/** Applique data-* sur document.documentElement (thème, accent, échelle, mouvement). */
export function applyAppearanceSettings(s: LauncherSettingsUI): void {
  const root = document.documentElement
  const choice = s.uiTheme
  root.dataset.theme = resolveDataTheme(choice)
  root.dataset.uiPreset = resolveUiPreset(choice)
  const prefersReduced =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  root.dataset.reduceMotion = s.uiReduceMotion || prefersReduced ? '1' : '0'
  root.dataset.density = s.uiCompact ? 'compact' : 'comfortable'
  root.dataset.chromeGlass = s.uiChromeGlass ? '1' : '0'
  root.dataset.settingsShell = s.uiSettingsShell === 'legacy' ? 'legacy' : 'aether2'
  const userAccent = resolveAccentHex(s)
  root.style.setProperty('--accent', userAccent)

  const preset = resolveUiPreset(choice)
  if (presetUsesUserChrome(preset)) {
    root.style.removeProperty('--accent-orange')
  } else {
    root.style.setProperty('--accent-orange', PRESET_CHROME_HEX[preset])
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
