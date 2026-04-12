import { app } from 'electron'
import { existsSync, readFileSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import os from 'node:os'
import { loadSettings } from './settings.js'
import { isMinecraftRunning } from './gameProcess.js'

const projectRoot = join(dirname(fileURLToPath(import.meta.url)), '..', '..')

function readAppVersion(): string {
  try {
    const p = join(projectRoot, 'package.json')
    if (!existsSync(p)) return '1.0.0'
    const pkg = JSON.parse(readFileSync(p, 'utf8')) as { version?: string }
    return pkg.version ?? '1.0.0'
  } catch {
    return '1.0.0'
  }
}

export type DebugSnapshot = {
  appVersion: string
  platform: string
  arch: string
  electronVersion: string
  chromeVersion: string
  nodeVersion: string
  testMode: boolean
  uptimeSec: number
  hostname: string
  totalMemBytes: number
  freeMemBytes: number
  loadAvg: number[]
  processMemory: {
    rss: number
    heapTotal: number
    heapUsed: number
    external: number
    arrayBuffers: number
  }
  /** CPU processus principal (depuis le dernier appel getCPUUsage). */
  mainCpu: { user: number; system: number }
  appMetrics: {
    pid: number
    type: string
    name?: string
    cpu: { percentCPUUsage: number; idleWakeupsPerSecond: number }
    memory: { peakWorkingSetSize: number; workingSetSize: number }
  }[]
  activeModpackId: string
  gameRunning: boolean
  userData: string
  instanceRoot: string
}

export async function getDebugSnapshot(
  getInstanceRoot: () => string,
  isAnyGameRunning?: () => Promise<boolean>
): Promise<DebugSnapshot> {
  const mu = process.memoryUsage()
  const cpu = process.getCPUUsage()
  const metrics = app.getAppMetrics()
  const settings = loadSettings()
  const root = getInstanceRoot()

  return {
    appVersion: readAppVersion(),
    platform: process.platform,
    arch: process.arch,
    electronVersion: process.versions.electron ?? '',
    chromeVersion: process.versions.chrome ?? '',
    nodeVersion: process.versions.node ?? '',
    testMode: process.env.SOLEA_TEST_MODE === '1',
    uptimeSec: Math.floor(process.uptime()),
    hostname: os.hostname(),
    totalMemBytes: os.totalmem(),
    freeMemBytes: os.freemem(),
    loadAvg: os.loadavg(),
    processMemory: {
      rss: mu.rss,
      heapTotal: mu.heapTotal,
      heapUsed: mu.heapUsed,
      external: mu.external,
      arrayBuffers: mu.arrayBuffers ?? 0
    },
    mainCpu: { user: cpu.user, system: cpu.system },
    appMetrics: metrics.map((m) => ({
      pid: m.pid,
      type: String(m.type),
      name: m.name,
      cpu: {
        percentCPUUsage: m.cpu.percentCPUUsage,
        idleWakeupsPerSecond: m.cpu.idleWakeupsPerSecond
      },
      memory: {
        peakWorkingSetSize: m.memory.peakWorkingSetSize,
        workingSetSize: m.memory.workingSetSize
      }
    })),
    activeModpackId: settings.activeModpackId,
    gameRunning: isAnyGameRunning ? await isAnyGameRunning() : await isMinecraftRunning(root),
    userData: app.getPath('userData'),
    instanceRoot: root
  }
}
