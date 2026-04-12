import { randomBytes } from 'crypto'
import type { SkinModel } from './skinPresets.js'

const SKINS_POST = 'https://api.minecraftservices.com/minecraft/profile/skins'
const SKINS_ACTIVE = 'https://api.minecraftservices.com/minecraft/profile/skins/active'

/**
 * Corps multipart RFC 2388 (CRLF). Undici FormData + Blob dans le main Electron
 * envoie parfois un flux incompatible avec l’API Mojang ; ce format est fiable.
 */
function buildSkinUploadMultipart(png: Buffer, variant: 'slim' | 'classic'): {
  body: Buffer
  contentType: string
} {
  const boundary = `----SoleaSkin${randomBytes(16).toString('hex')}`
  const crlf = '\r\n'
  const headVariant =
    `--${boundary}${crlf}` +
    `Content-Disposition: form-data; name="variant"${crlf}${crlf}` +
    `${variant}${crlf}`
  const headFile =
    `--${boundary}${crlf}` +
    `Content-Disposition: form-data; name="file"; filename="skin.png"${crlf}` +
    `Content-Type: image/png${crlf}${crlf}`
  const tail = `${crlf}--${boundary}--${crlf}`
  return {
    body: Buffer.concat([
      Buffer.from(headVariant, 'utf8'),
      Buffer.from(headFile, 'utf8'),
      png,
      Buffer.from(tail, 'utf8')
    ]),
    contentType: `multipart/form-data; boundary=${boundary}`
  }
}

async function readErrorMessage(res: Response): Promise<string> {
  let body = ''
  try {
    body = await res.text()
  } catch {
    return `HTTP ${res.status}`
  }
  try {
    const j = JSON.parse(body) as { errorMessage?: string; error?: string; developerMessage?: string }
    const m = j.errorMessage || j.developerMessage || j.error
    if (m && typeof m === 'string') return m
  } catch {
    /* ignore */
  }
  if (body.length > 0 && body.length < 400) return body
  return `HTTP ${res.status}`
}

export async function uploadMinecraftProfileSkin(
  accessToken: string,
  png: Buffer,
  model: SkinModel
): Promise<{ ok: true } | { ok: false; error: string }> {
  const variant = model === 'slim' ? 'slim' : 'classic'
  const { body, contentType } = buildSkinUploadMultipart(png, variant)

  const res = await fetch(SKINS_POST, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': contentType
    },
    body
  })

  if (res.ok) return { ok: true }
  return { ok: false, error: await readErrorMessage(res) }
}

export async function resetMinecraftProfileSkin(
  accessToken: string
): Promise<{ ok: true } | { ok: false; error: string }> {
  const res = await fetch(SKINS_ACTIVE, {
    method: 'DELETE',
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  })

  if (res.ok) return { ok: true }
  return { ok: false, error: await readErrorMessage(res) }
}
