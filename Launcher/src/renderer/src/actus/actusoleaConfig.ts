/**
 * URL du flux actu (GitHub raw), en pratique `actusolea.md`.
 * Défaut : dépôt public [SILWOX/SOLEA_ACTU](https://github.com/SILWOX/SOLEA_ACTU).
 * Surcharge optionnelle : `VITE_ACTU_SOLEA_JSON_URL` dans `.env`.
 */
const DEFAULT_ACTU_SOLEA_JSON_URL =
  'https://raw.githubusercontent.com/SILWOX/SOLEA_ACTU/main/actusolea.md'

export function getActuSoleaJsonUrl(): string {
  const fromEnv = import.meta.env.VITE_ACTU_SOLEA_JSON_URL as string | undefined
  if (typeof fromEnv === 'string' && fromEnv.trim().length > 0) return fromEnv.trim()
  return DEFAULT_ACTU_SOLEA_JSON_URL
}
