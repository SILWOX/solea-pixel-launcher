/**
 * Connexion Microsoft intégrée au launcher : fenêtre alignée sur le chrome Solea (fond, titre, icône),
 * session isolée et User-Agent type Chrome.
 */
import { BrowserWindow, app, nativeImage, session } from 'electron'
import type { BrowserWindow as BrowserWindowType } from 'electron'
import { existsSync } from 'fs'

export const MICROSOFT_OAUTH_REDIRECT_DESKTOP = 'https://login.live.com/oauth20_desktop.srf'

/** Même base que la fenêtre principale du launcher. */
const AUTH_WINDOW_BG = '#07050c'

export type MicrosoftAuthBranding = {
  windowTitle: string
  iconPath?: string
}

function chromeLikeUserAgent(): string {
  if (process.platform === 'darwin') {
    return 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
  }
  if (process.platform === 'linux') {
    return 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
  }
  return 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36'
}

export function buildMicrosoftAuthorizeUrl(clientId: string, redirectUri: string): string {
  const id = clientId.trim() || '00000000402b5328'
  return (
    'https://login.live.com/oauth20_authorize.srf' +
    `?client_id=${id}` +
    '&response_type=code' +
    `&redirect_uri=${redirectUri}` +
    '&scope=XboxLive.signin%20offline_access' +
    '&cobrandid=8058f65d-ce06-4c30-9559-473c9275a65d' +
    '&prompt=select_account'
  )
}

function centerAuthWindowOnParent(win: BrowserWindow, parent: BrowserWindowType | null): void {
  if (!parent || parent.isDestroyed()) return
  try {
    const b = win.getBounds()
    const pb = parent.getBounds()
    const x = Math.round(pb.x + (pb.width - b.width) / 2)
    const y = Math.round(pb.y + (pb.height - b.height) / 2)
    win.setPosition(x, y)
  } catch {
    /* ignore */
  }
}

/**
 * Fenêtre modale alignée sur le launcher : fond sombre au chargement, titre et icône Solea.
 */
export async function getMicrosoftAuthCodeEmbedded(
  authUrl: string,
  redirectUri: string,
  parent: BrowserWindowType | null,
  branding?: MicrosoftAuthBranding
): Promise<string | 'cancel'> {
  await app.whenReady()

  const partition = `temp:solea-ms-${Date.now()}`
  const ses = session.fromPartition(partition, { cache: false })

  const width = 1096
  const height = 768

  let windowIcon: Electron.NativeImage | undefined
  const ip = branding?.iconPath
  if (ip && existsSync(ip)) {
    const img = nativeImage.createFromPath(ip)
    if (!img.isEmpty()) windowIcon = img
  }

  return new Promise((resolve) => {
    let settled = false
    const finish = (v: string | 'cancel') => {
      if (settled) return
      settled = true
      resolve(v)
    }

    const tryParseFromUrl = (loc: string) => {
      if (!loc.startsWith(redirectUri.split('?')[0])) return
      const q = loc.indexOf('?')
      const code =
        q >= 0 ? new URLSearchParams(loc.slice(q + 1)).get('code')?.trim() || null : null
      if (code) {
        finish(code)
        try {
          win.close()
        } catch {
          /* ignore */
        }
      } else if (loc.startsWith(redirectUri.split('?')[0])) {
        finish('cancel')
        try {
          win.close()
        } catch {
          /* ignore */
        }
      }
    }

    const win = new BrowserWindow({
      width,
      height,
      minWidth: 840,
      minHeight: 600,
      resizable: true,
      show: false,
      center: !parent,
      title: branding?.windowTitle ?? 'Solea Pixel',
      backgroundColor: AUTH_WINDOW_BG,
      autoHideMenuBar: true,
      parent: parent ?? undefined,
      modal: Boolean(parent),
      roundedCorners: true,
      ...(windowIcon ? { icon: windowIcon } : {}),
      webPreferences: {
        session: ses,
        contextIsolation: true,
        nodeIntegration: false,
        sandbox: true
      }
    })
    win.setMenu(null)
    if (process.platform === 'win32' && windowIcon) {
      try {
        win.setIcon(windowIcon)
      } catch {
        /* ignore */
      }
    }
    centerAuthWindowOnParent(win, parent)
    win.webContents.setUserAgent(chromeLikeUserAgent())

    win.once('ready-to-show', () => {
      win.show()
    })

    win.webContents.on('will-navigate', (_e, url) => tryParseFromUrl(url))
    win.webContents.on('did-navigate', (_e, url) => tryParseFromUrl(url))
    win.webContents.on('did-navigate-in-page', (_e, url) => tryParseFromUrl(url))

    win.on('close', () => {
      if (!settled) finish('cancel')
    })

    void win.loadURL(authUrl)
  })
}
