import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'
import type { ModpackId } from './modpacks.js'

export type ModpackActivityRecord = {
  lastPlayAt?: string
  lastInstallAt?: string
}

type ActivityFile = Partial<Record<ModpackId, ModpackActivityRecord>>

function activityPath(): string {
  return join(app.getPath('userData'), 'modpack-activity.json')
}

function readAll(): ActivityFile {
  const p = activityPath()
  if (!existsSync(p)) return {}
  try {
    const raw = JSON.parse(readFileSync(p, 'utf8')) as unknown
    return raw && typeof raw === 'object' ? (raw as ActivityFile) : {}
  } catch {
    return {}
  }
}

function writeAll(data: ActivityFile): void {
  const p = activityPath()
  mkdirSync(app.getPath('userData'), { recursive: true })
  writeFileSync(p, JSON.stringify(data, null, 2), 'utf8')
}

export function getModpackActivityMap(): ActivityFile {
  return readAll()
}

export function recordModpackLastPlay(modpackId: ModpackId): void {
  const all = readAll()
  const cur = all[modpackId] ?? {}
  all[modpackId] = { ...cur, lastPlayAt: new Date().toISOString() }
  writeAll(all)
}

export function recordModpackLastInstall(modpackId: ModpackId): void {
  const all = readAll()
  const cur = all[modpackId] ?? {}
  all[modpackId] = { ...cur, lastInstallAt: new Date().toISOString() }
  writeAll(all)
}
