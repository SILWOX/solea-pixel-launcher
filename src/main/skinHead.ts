/** Récupération des têtes joueur côté main (les <img https://…> sont souvent bloquées ou vides dans Electron). */

import { nativeImage } from 'electron'
import type { MicrosoftAuthResponse } from 'minecraft-java-core'

const BROWSER_UA =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'

export function dashedMinecraftUuid(uuid: string): string | null {
  const hex = uuid
    .trim()
    .replace(/^\{|\}$/g, '')
    .replace(/-/g, '')
    .toLowerCase()
  if (!/^[0-9a-f]{32}$/.test(hex)) return null
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20, 32)}`
}

/**
 * Extrait la zone visage du PNG skin (64×64 ou HD 64×k). Sans resize : évite le flou
 * d’Electron sur du pixel-art ; le renderer affiche avec image-rendering: pixelated.
 */
export function minecraftFaceDataUrlFromSkinDataUrl(skinDataUrl: string, _targetSize: number): string | null {
  try {
    const image = nativeImage.createFromDataURL(skinDataUrl)
    if (image.isEmpty()) return null
    const { width, height } = image.getSize()
    if (width < 64 || height < 64) return null
    const scale = Math.floor(width / 64)
    if (scale < 1 || width !== 64 * scale || height < 64 * scale) return null
    const faceSize = 8 * scale
    const face = image.crop({
      x: 8 * scale,
      y: 8 * scale,
      width: faceSize,
      height: faceSize
    })
    if (face.isEmpty()) return null
    return face.toDataURL()
  } catch {
    return null
  }
}

/** Tête depuis le skin déjà connu du compte (évite Crafatar / erreurs réseau et affiche le bon joueur). */
export async function headDataUrlFromStoredAccount(
  acc: MicrosoftAuthResponse | null,
  targetSize: number
): Promise<string | null> {
  const s = Math.min(512, Math.max(8, Math.round(targetSize)))
  const skins = acc?.profile?.skins
  if (!Array.isArray(skins) || skins.length === 0) return null
  const active =
    skins.find((sk) => String(sk.state ?? '').toUpperCase() === 'ACTIVE') ?? skins[0]
  const b64 = active?.base64
  if (typeof b64 === 'string' && b64.startsWith('data:image')) {
    const local = minecraftFaceDataUrlFromSkinDataUrl(b64, s)
    if (local) return local
  }
  const url = active?.url
  if (typeof url === 'string' && url.startsWith('http')) {
    try {
      const r = await fetch(url, {
        headers: {
          'User-Agent': BROWSER_UA,
          Accept: 'image/png,image/webp,image/*,*/*'
        }
      })
      if (!r.ok) return null
      const buf = Buffer.from(await r.arrayBuffer())
      if (buf.length < 80) return null
      const ct = (r.headers.get('content-type') || '').split(';')[0].trim()
      const mime = ct.startsWith('image/') ? ct : 'image/png'
      const dataUrl = `data:${mime};base64,${buf.toString('base64')}`
      return minecraftFaceDataUrlFromSkinDataUrl(dataUrl, s)
    } catch {
      return null
    }
  }
  return null
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
