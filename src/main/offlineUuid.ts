/**
 * UUID « offline » identique à Minecraft Java : UUID v3 (MD5) sur
 * `OfflinePlayer:` + nom (UTF-8).
 */
import { createHash } from 'node:crypto'

export function offlinePlayerUuid(name: string): string {
  const input = `OfflinePlayer:${name}`
  const md5 = createHash('md5').update(input, 'utf8').digest()
  const b = Buffer.from(md5)
  b[6] = (b[6]! & 0x0f) | 0x30
  b[8] = (b[8]! & 0x3f) | 0x80
  const hex = b.toString('hex')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}
