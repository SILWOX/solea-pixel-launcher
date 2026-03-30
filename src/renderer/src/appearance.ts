import type { LauncherSettingsUI } from './launcherTypes'

function resolveTheme(theme: LauncherSettingsUI['uiTheme']): 'light' | 'dark' {
  if (theme === 'light') return 'light'
  if (theme === 'dark') return 'dark'
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/** Applique data-* sur document.documentElement (thème, accent, échelle, mouvement). */
export function applyAppearanceSettings(s: LauncherSettingsUI): void {
  const root = document.documentElement
  const resolved = resolveTheme(s.uiTheme)
  root.dataset.theme = resolved
  const prefersReduced =
    typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
  root.dataset.reduceMotion = s.uiReduceMotion || prefersReduced ? '1' : '0'
  root.dataset.density = s.uiCompact ? 'compact' : 'comfortable'
  /* Accent fixe (orange launcher) — plus d’option utilisateur. */
  root.style.setProperty('--accent', '#ff6a1a')
  const scale = s.uiFontScale === 's' ? '0.92' : s.uiFontScale === 'l' ? '1.08' : '1'
  root.style.setProperty('--ui-font-scale', scale)
}

export function subscribeSystemTheme(onChange: () => void): () => void {
  const m = window.matchMedia('(prefers-color-scheme: dark)')
  const fn = () => onChange()
  m.addEventListener('change', fn)
  return () => m.removeEventListener('change', fn)
}
