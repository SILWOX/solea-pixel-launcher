/**
 * Worker dédié au lancement Minecraft (minecraft-java-core).
 * Évite de bloquer le processus principal Electron (UI « Ne répond pas »).
 */
import { parentPort, workerData } from 'node:worker_threads'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const { Launch } = require('minecraft-java-core') as {
  Launch: new () => {
    Launch: (opt: Record<string, unknown>) => Promise<boolean>
    on: (ev: string, fn: (...args: unknown[]) => void) => void
  }
}

const launchOpts = workerData.launchOpts as Record<string, unknown>
const launcher = new Launch()

launcher.on('data', (line: string) => {
  parentPort?.postMessage({ type: 'data', line: String(line) })
})
launcher.on('error', (err: unknown) => {
  const msg = err instanceof Error ? err.message : String(err)
  parentPort?.postMessage({ type: 'error', message: msg })
})
launcher.on('close', () => {
  parentPort?.postMessage({ type: 'close' })
})

void launcher.Launch(launchOpts)
