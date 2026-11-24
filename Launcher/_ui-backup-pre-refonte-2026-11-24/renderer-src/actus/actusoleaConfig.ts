/**
 * URL du flux actu (JSON `ActuSoleaJson` : segments / content, etc.).
 * Défaut : fonction Netlify sur le site (posts publiés via l’admin + Supabase).
 * Secours / dev : `VITE_ACTU_SOLEA_JSON_URL` dans `.env` (ex. ancien GitHub raw `actusolea.md`).
 */
const DEFAULT_ACTU_SOLEA_JSON_URL =
  'https://soleapixel.com/.netlify/functions/news-feed'

export function getActuSoleaJsonUrl(): string {
  const fromEnv = import.meta.env.VITE_ACTU_SOLEA_JSON_URL as string | undefined
  if (typeof fromEnv === 'string' && fromEnv.trim().length > 0) return fromEnv.trim()
  return DEFAULT_ACTU_SOLEA_JSON_URL
}
