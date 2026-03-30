import { appendFileSync, existsSync, mkdirSync, renameSync, statSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'

const MAX_BYTES = 512 * 1024

function logDir(): string {
  return join(app.getPath('userData'), 'logs')
}

function logPath(): string {
  return join(logDir(), 'solea-main.log')
}

function rotateIfNeeded(): void {
  const p = logPath()
  try {
    if (existsSync(p) && statSync(p).size > MAX_BYTES) {
      const bak = `${p}.1`
      if (existsSync(bak)) renameSync(bak, `${p}.2`)
      renameSync(p, bak)
    }
  } catch {
    /* ignore */
  }
}

export function logMain(level: 'info' | 'warn' | 'error', msg: string, extra?: unknown): void {
  const suffix =
    extra !== undefined
      ? ` ${typeof extra === 'string' ? extra : JSON.stringify(extra)}`
      : ''
  const line = `[${new Date().toISOString()}] ${level.toUpperCase()} ${msg}${suffix}\n`
  try {
    mkdirSync(logDir(), { recursive: true })
    rotateIfNeeded()
    appendFileSync(logPath(), line, 'utf8')
  } catch {
    /* ignore */
  }
}
