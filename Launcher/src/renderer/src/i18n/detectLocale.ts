import type { Locale } from './dictionary'

/** Langue UI à partir des préférences navigateur / OS (Electron renderer). */
export function detectBrowserUiLanguage(): Locale {
  const langs: string[] = []
  if (typeof navigator !== 'undefined') {
    if (navigator.languages?.length) langs.push(...navigator.languages)
    if (navigator.language) langs.push(navigator.language)
  }
  for (const tag of langs) {
    if (String(tag).toLowerCase().startsWith('fr')) return 'fr'
  }
  return 'en'
}
