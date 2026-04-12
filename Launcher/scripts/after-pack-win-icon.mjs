/**
 * Applique build/icon.ico au .exe Windows sans activer signAndEditExecutable.
 * Le flux electron-builder (rcedit + signingManager) télécharge winCodeSign et peut
 * échouer à l'extraction si Windows refuse les liens symboliques (sans mode développeur).
 */
import { existsSync } from 'fs'
import { join, dirname } from 'path'
import { fileURLToPath } from 'url'
import { rcedit } from 'rcedit'

const projectRoot = join(dirname(fileURLToPath(import.meta.url)), '..')

export default async function afterPackWinIcon(context) {
  if (context.electronPlatformName !== 'win32') return

  const productFilename = context.packager.appInfo.productFilename
  const exePath = join(context.appOutDir, `${productFilename}.exe`)
  const iconPath = join(projectRoot, 'build', 'icon.ico')

  if (!existsSync(exePath)) {
    console.warn('[after-pack-win-icon] exe introuvable:', exePath)
    return
  }
  if (!existsSync(iconPath)) {
    console.warn('[after-pack-win-icon] build/icon.ico absent — exécuter npm run predist')
    return
  }

  await rcedit(exePath, { icon: iconPath })
}
