import { getActuSoleaJsonUrl } from './actusoleaConfig'
import type { ActuSoleaJson } from './actusoleaTypes'

export const ACTU_SOLEA_CACHE_KEY = 'solea.actuSolea.cache.v1'
export const ACTU_SOLEA_FETCHED_AT_KEY = 'solea.actuSolea.fetchedAt.v1'

/** Contourne le cache CDN/navigateur sur les URLs GitHub raw. */
function fetchUrlNoCache(base: string): string {
  try {
    const u = new URL(base)
    u.searchParams.set('_', String(Date.now()))
    return u.toString()
  } catch {
    const sep = base.includes('?') ? '&' : '?'
    return `${base}${sep}_=${Date.now()}`
  }
}

export function readActuSoleaFetchedAt(): string | null {
  try {
    const v = localStorage.getItem(ACTU_SOLEA_FETCHED_AT_KEY)
    return v && v.trim().length > 0 ? v.trim() : null
  } catch {
    return null
  }
}

function writeActuSoleaFetchedAt(iso: string): void {
  try {
    localStorage.setItem(ACTU_SOLEA_FETCHED_AT_KEY, iso)
  } catch {
    /* ignore */
  }
}

/** Réponse JSON ou texte (Markdown) — le dépôt peut servir du `.json` qui contient du Markdown. */
function parseActuSoleaPayload(raw: string): ActuSoleaJson {
  const text = raw.replace(/^\uFEFF/, '').trimStart()
  if (text.startsWith('{') || text.startsWith('[')) {
    try {
      const parsed = JSON.parse(raw.replace(/^\uFEFF/, '')) as unknown
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return parsed as ActuSoleaJson
      }
      if (Array.isArray(parsed)) {
        return { segments: parsed.map((x) => String(x)) }
      }
    } catch {
      /* fallback markdown / texte brut */
    }
  }
  return { content: raw.replace(/^\uFEFF/, '') }
}

export const ACTU_SOLEA_UPDATED_EVENT = 'solea-actu-updated'

export function readActuSoleaCache(): ActuSoleaJson | null {
  try {
    const raw = localStorage.getItem(ACTU_SOLEA_CACHE_KEY)
    if (!raw) return null
    return JSON.parse(raw) as ActuSoleaJson
  } catch {
    return null
  }
}

function writeActuSoleaCache(data: ActuSoleaJson): void {
  try {
    localStorage.setItem(ACTU_SOLEA_CACHE_KEY, JSON.stringify(data))
  } catch {
    /* ignore */
  }
}

function shouldUseRendererFetchAfterIpcFailure(err: unknown): boolean {
  const msg = err instanceof Error ? err.message : String(err)
  /* Processus principal pas relancé après ajout du handler, ou build désynchronisé */
  return /no handler registered/i.test(msg)
}

async function fetchRemoteText(finalUrl: string): Promise<string> {
  if (typeof window !== 'undefined' && window.solea?.fetchActuText) {
    try {
      const r = await window.solea.fetchActuText(finalUrl)
      if (r.ok) return r.text
      throw new Error(r.error)
    } catch (e) {
      if (!shouldUseRendererFetchAfterIpcFailure(e)) throw e
    }
  }
  const res = await fetch(finalUrl, {
    cache: 'no-store',
    headers: { 'Cache-Control': 'no-cache', Pragma: 'no-cache' }
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.text()
}

export async function fetchAndCacheActuSolea(): Promise<
  { ok: true; data: ActuSoleaJson } | { ok: false; error: string }
> {
  const url = getActuSoleaJsonUrl()
  if (!url) return { ok: false, error: 'no_url' }
  try {
    const raw = await fetchRemoteText(fetchUrlNoCache(url))
    const data = parseActuSoleaPayload(raw)
    writeActuSoleaCache(data)
    const now = new Date().toISOString()
    writeActuSoleaFetchedAt(now)
    return { ok: true, data }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
}
