/** Récupération des têtes joueur côté main (les <img https://…> sont souvent bloquées ou vides dans Electron). */

const BROWSER_UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'

export function dashedMinecraftUuid(uuid: string): string | null {
  const hex = uuid.replace(/-/g, '').toLowerCase()
  if (!/^[0-9a-f]{32}$/.test(hex)) return null
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20, 32)}`
}

export async function fetchMinecraftHeadDataUrl(uuid: string, size = 64): Promise<string | null> {
  const dashed = dashedMinecraftUuid(uuid)
  if (!dashed) return null
  const s = Math.min(512, Math.max(8, Math.round(size)))
  const compact = dashed.replace(/-/g, '')
  const urls = [
    `https://crafatar.com/avatars/${dashed}?size=${s}&overlay&default=MHF_Steve`,
    `https://mc-heads.net/avatar/${dashed}/${s}`,
    `https://mc-heads.net/avatar/${compact}/${s}`
  ]
  for (const url of urls) {
    try {
      const r = await fetch(url, {
        headers: {
          'User-Agent': BROWSER_UA,
          Accept: 'image/png,image/webp,image/*,*/*'
        }
      })
      if (!r.ok) continue
      const buf = Buffer.from(await r.arrayBuffer())
      if (buf.length < 80) continue
      const ct = (r.headers.get('content-type') || '').split(';')[0].trim()
      const mime = ct.startsWith('image/') ? ct : 'image/png'
      return `data:${mime};base64,${buf.toString('base64')}`
    } catch {
      continue
    }
  }
  return null
}

/** Texture skin complète (64×64) pour aperçu 3D — pas une simple tête. */
export async function fetchMinecraftSkinDataUrl(uuid: string): Promise<string | null> {
  const dashed = dashedMinecraftUuid(uuid)
  if (!dashed) return null
  const compact = dashed.replace(/-/g, '')
  const urls = [
    `https://crafatar.com/skins/${dashed}?default=MHF_Steve&overlay`,
    `https://mc-heads.net/skin/${dashed}`,
    `https://mc-heads.net/skin/${compact}`
  ]
  for (const url of urls) {
    try {
      const r = await fetch(url, {
        headers: {
          'User-Agent': BROWSER_UA,
          Accept: 'image/png,image/webp,image/*,*/*'
        }
      })
      if (!r.ok) continue
      const buf = Buffer.from(await r.arrayBuffer())
      if (buf.length < 80) continue
      const ct = (r.headers.get('content-type') || '').split(';')[0].trim()
      const mime = ct.startsWith('image/') ? ct : 'image/png'
      return `data:${mime};base64,${buf.toString('base64')}`
    } catch {
      continue
    }
  }
  return null
}

export async function fetchMinecraftCapeDataUrl(uuid: string): Promise<string | null> {
  const dashed = dashedMinecraftUuid(uuid)
  if (!dashed) return null
  const compact = dashed.replace(/-/g, '')
  const urls = [
    `https://crafatar.com/capes/${dashed}`,
    `https://mc-heads.net/cape/${dashed}`,
    `https://mc-heads.net/cape/${compact}`
  ]
  for (const url of urls) {
    try {
      const r = await fetch(url, {
        headers: {
          'User-Agent': BROWSER_UA,
          Accept: 'image/png,image/webp,image/*,*/*'
        }
      })
      if (!r.ok || r.status === 404) continue
      const buf = Buffer.from(await r.arrayBuffer())
      if (buf.length < 80) continue
      const ct = (r.headers.get('content-type') || '').split(';')[0].trim()
      const mime = ct.startsWith('image/') ? ct : 'image/png'
      return `data:${mime};base64,${buf.toString('base64')}`
    } catch {
      continue
    }
  }
  return null
}
