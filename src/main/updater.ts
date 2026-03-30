import { createRequire } from 'module'
import { app, BrowserWindow } from 'electron'

/** electron-updater est CJS : pas d’import ESM nommé fiable dans le bundle main. */
const requireUpdater = createRequire(import.meta.url)
const { autoUpdater } = requireUpdater('electron-updater') as {
  autoUpdater: import('electron-updater').AppUpdater
}

let started = false

/**
 * Mises à jour via electron-updater + GitHub Releases (voir `build.publish` et `repository` dans package.json).
 * Les builds publiées incluent `latest.yml` ; le client vérifie les SHA-512 avant d’appliquer la mise à jour.
 */
export function setupAutoUpdater(mainWindow: BrowserWindow | null, channel: 'stable' | 'beta'): void {
  if (!app.isPackaged || started) return
  started = true

  autoUpdater.autoDownload = false
  autoUpdater.allowPrerelease = channel === 'beta'
  autoUpdater.channel = channel === 'beta' ? 'beta' : 'latest'

  const wc = mainWindow?.webContents
  autoUpdater.on('update-available', (info) => {
    wc?.send('updater:available', { version: info.version, releaseNotes: info.releaseNotes })
  })
  autoUpdater.on('update-not-available', () => {
    wc?.send('updater:not-available')
  })
  autoUpdater.on('update-downloaded', () => {
    wc?.send('updater:downloaded')
  })
  autoUpdater.on('error', (err) => {
    wc?.send('updater:error', err.message)
  })

  setTimeout(() => {
    void autoUpdater.checkForUpdates().catch(() => wc?.send('updater:error', 'checkForUpdates failed'))
  }, 8000)
}

/** @returns false si l’app n’est pas packagée (pas d’auto-update en dev). */
export function checkForUpdatesManual(): boolean {
  if (!app.isPackaged) return false
  void autoUpdater.checkForUpdates()
  return true
}

export function quitAndInstall(): void {
  autoUpdater.quitAndInstall(false, true)
}

export async function downloadUpdate(): Promise<void> {
  await autoUpdater.downloadUpdate()
}
