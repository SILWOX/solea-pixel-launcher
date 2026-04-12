/** Profil Minecraft Services : capes du compte (authentifié). */

const MC_PROFILE = 'https://api.minecraftservices.com/minecraft/profile'
const MC_CAPE_ACTIVE = 'https://api.minecraftservices.com/minecraft/profile/capes/active'

/** Limite les appels répétés à GET /profile (aperçu compte + modale cape → 429). */
const PROFILE_CACHE_MS = 50_000

const BROWSER_UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'

type ProfileCapeJson = {
  id?: string
  alias?: string
  state?: string
  url?: string
}

type ProfileJson = {
  capes?: ProfileCapeJson[]
  error?: string
  errorMessage?: string
  developerMessage?: string
}

const profileCache = new Map<string, { capes: ProfileCapeJson[]; expiresAt: number }>()
const profileInflight = new Map<string, Promise<{ ok: true; capes: ProfileCapeJson[] } | { ok: false; error: string }>>()

function invalidateProfileCacheForToken(accessToken: string): void {
  profileCache.delete(accessToken)
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function humanProfileError(status: number, j: ProfileJson): string {
  const api = j.errorMessage || j.developerMessage || j.error
  if (api && typeof api === 'string' && api.trim()) return api
  if (status === 429) {
    return 'Mojang limite les requêtes (429). Attends une minute, ferme puis rouvre la fenêtre, ou réessaie.'
  }
  return `Profil Mojang (${status})`
}

async function fetchProfileOnce(
  accessToken: string
): Promise<
  | { ok: true; capes: ProfileCapeJson[] }
  | { ok: false; status: number; body: ProfileJson; retryAfterMs?: number }
> {
  const r = await fetch(MC_PROFILE, {
    headers: { Authorization: `Bearer ${accessToken}` }
  })
  let j: ProfileJson = {}
  try {
    j = (await r.json()) as ProfileJson
  } catch {
    j = {}
  }
  if (r.ok) {
    return { ok: true, capes: Array.isArray(j.capes) ? j.capes : [] }
  }
  let retryAfterMs: number | undefined
  const ra = r.headers.get('Retry-After')
  if (ra && /^\d+$/.test(ra.trim())) {
    retryAfterMs = Math.min(120_000, parseInt(ra.trim(), 10) * 1000)
  }
  return { ok: false, status: r.status, body: j, retryAfterMs }
}

async function fetchProfileJsonWithRetry(
  accessToken: string
): Promise<{ ok: true; capes: ProfileCapeJson[] } | { ok: false; error: string }> {
  const maxAttempts = 4
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const once = await fetchProfileOnce(accessToken)
    if (once.ok) return once

    const is429 = once.status === 429
    const lastAttempt = attempt === maxAttempts - 1
    if (!is429 || lastAttempt) {
      return { ok: false, error: humanProfileError(once.status, once.body) }
    }

    const waitMs = once.retryAfterMs ?? Math.min(30_000, 2000 * (attempt + 1))
    await sleep(waitMs)
  }
  return { ok: false, error: humanProfileError(429, {}) }
}

export type ProfileCapeRow = {
  id: string
  alias: string
  state: string
  url: string | null
  dataUrl: string | null
}

async function fetchTextureDataUrl(httpUrl: string): Promise<string | null> {
  try {
    const r = await fetch(httpUrl, {
      headers: {
        'User-Agent': BROWSER_UA,
        Accept: 'image/png,image/webp,image/*,*/*'
      }
    })
    if (!r.ok) return null
    const buf = Buffer.from(await r.arrayBuffer())
    if (buf.length < 32) return null
    const ct = (r.headers.get('content-type') || '').split(';')[0].trim()
    const mime = ct.startsWith('image/') ? ct : 'image/png'
    return `data:${mime};base64,${buf.toString('base64')}`
  } catch {
    return null
  }
}

export async function fetchProfileJson(accessToken: string): Promise<
  { ok: true; capes: ProfileCapeJson[] } | { ok: false; error: string }
> {
  const now = Date.now()
  const cached = profileCache.get(accessToken)
  if (cached && cached.expiresAt > now) {
    return { ok: true, capes: cached.capes }
  }

  const waitInflight = profileInflight.get(accessToken)
  if (waitInflight) return waitInflight

  const task = (async () => {
    const res = await fetchProfileJsonWithRetry(accessToken)
    if (res.ok) {
      profileCache.set(accessToken, { capes: res.capes, expiresAt: Date.now() + PROFILE_CACHE_MS })
    }
    return res
  })().finally(() => {
    profileInflight.delete(accessToken)
  })

  profileInflight.set(accessToken, task)
  return task
}

export async function listProfileCapesWithTextures(
  accessToken: string
): Promise<{ ok: true; capes: ProfileCapeRow[] } | { ok: false; error: string }> {
  const raw = await fetchProfileJson(accessToken)
  if (!raw.ok) return raw
  const out: ProfileCapeRow[] = []
  for (let i = 0; i < raw.capes.length; i++) {
    const c = raw.capes[i]!
    if (i > 0) await sleep(120)
    const id = typeof c.id === 'string' ? c.id : ''
    if (!id) continue
    const url = typeof c.url === 'string' ? c.url : null
    let dataUrl: string | null = null
    if (url) {
      dataUrl = await fetchTextureDataUrl(url)
    }
    out.push({
      id,
      alias: typeof c.alias === 'string' && c.alias.trim() ? c.alias : id.slice(0, 8),
      state: typeof c.state === 'string' ? c.state : 'INACTIVE',
      url,
      dataUrl
    })
  }
  return { ok: true, capes: out }
}

export function activeCapeIdFromRows(capes: ProfileCapeRow[]): string | null {
  const a = capes.find((c) => c.state === 'ACTIVE')
  return a?.id ?? null
}

export async function activeCapeDataUrlFromToken(accessToken: string): Promise<string | null> {
  const raw = await fetchProfileJson(accessToken)
  if (!raw.ok) return null
  for (const c of raw.capes) {
    if (c.state === 'ACTIVE' && typeof c.url === 'string' && c.url) {
      return fetchTextureDataUrl(c.url)
    }
  }
  return null
}

function readMojangError(j: unknown): string {
  if (!j || typeof j !== 'object') return 'Erreur inconnue'
  const o = j as Record<string, unknown>
  return String(o.errorMessage || o.developerMessage || o.error || 'Requête refusée')
}

export async function setProfileActiveCape(
  accessToken: string,
  capeId: string | null
): Promise<{ ok: true } | { ok: false; error: string }> {
  if (capeId === null) {
    const r = await fetch(MC_CAPE_ACTIVE, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${accessToken}` }
    })
    if (r.status === 204 || r.ok) {
      invalidateProfileCacheForToken(accessToken)
      return { ok: true }
    }
    let j: unknown
    try {
      j = await r.json()
    } catch {
      j = null
    }
    return { ok: false, error: readMojangError(j) || `HTTP ${r.status}` }
  }

  const r = await fetch(MC_CAPE_ACTIVE, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ capeId })
  })
  if (r.ok) {
    invalidateProfileCacheForToken(accessToken)
    return { ok: true }
  }
  let j: unknown
  try {
    j = await r.json()
  } catch {
    j = null
  }
  return { ok: false, error: readMojangError(j) || `HTTP ${r.status}` }
}
