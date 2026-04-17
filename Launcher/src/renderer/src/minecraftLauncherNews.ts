/** Flux d’actus du launcher officiel (Mojang). `/v2/news.json` est tenu à jour ; `/news.json` sans v2 est historique (~2024). */

export const MOJANG_LAUNCHER_NEWS_URL = 'https://launchercontent.mojang.com/v2/news.json'

/** Ancien fichier figé — secours uniquement si le v2 est indisponible. */
export const MOJANG_LAUNCHER_NEWS_URL_LEGACY = 'https://launchercontent.mojang.com/news.json'

export const MOJANG_LAUNCHER_IMAGE_ORIGIN = 'https://launchercontent.mojang.com'

export type McLauncherNewsEntry = {
  id: string
  title: string
  text: string
  date: string
  tag?: string
  category?: string
  readMoreLink?: string
  newsType?: string[]
  playPageImage?: { url: string; title?: string }
  newsPageImage?: { url: string; title?: string }
}

export type McLauncherNewsPayload = {
  version?: number
  entries?: McLauncherNewsEntry[]
}

export function parseLauncherNewsJson(text: string): McLauncherNewsEntry[] {
  try {
    const data = JSON.parse(text) as McLauncherNewsPayload
    return Array.isArray(data.entries) ? data.entries : []
  } catch {
    return []
  }
}

/** Entrées taguées Java / catégorie Java — utile si on veut un filtre strict (masque les actus Bedrock plus récentes du même fichier). */
export function filterJavaHubNews(entries: McLauncherNewsEntry[]): McLauncherNewsEntry[] {
  return entries.filter((e) => {
    if (e.newsType?.includes('Java')) return true
    if (e.category === 'Minecraft: Java Edition') return true
    return false
  })
}

/** Toutes les entrées valides du JSON launcher Mojang, tri du plus récent au plus ancien (comme le carrousel officiel). */
export function allLauncherNewsSorted(entries: McLauncherNewsEntry[]): McLauncherNewsEntry[] {
  return sortNewsByDateDesc(entries.filter((e) => Boolean(e.id?.trim()) && Boolean(e.title?.trim())))
}

export function sortNewsByDateDesc(entries: McLauncherNewsEntry[]): McLauncherNewsEntry[] {
  return [...entries].sort((a, b) => {
    const ta = Date.parse(a.date)
    const tb = Date.parse(b.date)
    const na = Number.isNaN(ta) ? 0 : ta
    const nb = Number.isNaN(tb) ? 0 : tb
    return nb - na
  })
}

export function newsImageUrl(e: McLauncherNewsEntry): string | null {
  const rel = e.playPageImage?.url ?? e.newsPageImage?.url
  if (!rel) return null
  if (rel.startsWith('http://') || rel.startsWith('https://')) return rel
  return `${MOJANG_LAUNCHER_IMAGE_ORIGIN}${rel.startsWith('/') ? '' : '/'}${rel}`
}

/** Affichage date courte (évite la chaîne ISO brute dans l’UI). */
export function formatNewsDateForUi(isoDate: string): string {
  const t = Date.parse(isoDate)
  if (Number.isNaN(t)) return isoDate
  try {
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(t)
  } catch {
    return isoDate
  }
}
