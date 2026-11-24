/** Miroir de `DebugSnapshot` côté main — garder aligné avec `src/main/debugSnapshot.ts`. */
export type DebugSnapshotUi = {
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
