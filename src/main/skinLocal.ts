import { join } from 'path'
import { app } from 'electron'
import { existsSync, mkdirSync, readFileSync, unlinkSync, writeFileSync } from 'fs'

const PNG_MAGIC = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])

export function normalizeSkinUuid(uuid: string): string {
  return uuid.replace(/-/g, '').toLowerCase()
}

export function localSkinPath(uuid: string): string {
  return join(app.getPath('userData'), 'skins', `${normalizeSkinUuid(uuid)}.png`)
}

export function isLikelyPng(buf: Buffer): boolean {
  if (buf.length < 32) return false
  if (!buf.subarray(0, 8).equals(PNG_MAGIC)) return false
  if (buf.subarray(12, 16).toString('ascii') !== 'IHDR') return false
  return true
}

export function readLocalSkinDataUrl(uuid: string): string | null {
  const p = localSkinPath(uuid)
  if (!existsSync(p)) return null
  try {
    const buf = readFileSync(p)
    if (!isLikelyPng(buf)) return null
    return `data:image/png;base64,${buf.toString('base64')}`
  } catch {
    return null
  }
}

export function writeLocalSkin(uuid: string, buf: Buffer): void {
  if (!isLikelyPng(buf)) throw new Error('Fichier PNG invalide.')
  const dir = join(app.getPath('userData'), 'skins')
  mkdirSync(dir, { recursive: true })
  writeFileSync(localSkinPath(uuid), buf)
}

export function clearLocalSkin(uuid: string): void {
  const p = localSkinPath(uuid)
  if (existsSync(p)) {
    try {
      unlinkSync(p)
    } catch {
      /* ignore */
    }
  }
}
