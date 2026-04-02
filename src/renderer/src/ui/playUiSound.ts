/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import installPopUrl from '../assets/sounds/mixkit-message-pop-alert-2354.mp3?url'
import launchPopUrl from '../assets/sounds/mixkit-long-pop-2358.wav?url'

export type UiSoundKind = 'install' | 'launch'

export type UiSoundPrefs = {
  master: boolean
  reduceMotion: boolean
  /** 0–1, multiplicateur du volume de base */
  volume: number
  onInstall: boolean
  onLaunch: boolean
}

/**
 * Sons Mixkit (fichiers locaux). Lancement : `~/Downloads/clickplay.wav` si présent (lu via IPC main).
 */
export async function playUiSound(kind: UiSoundKind, prefs: UiSoundPrefs): Promise<void> {
  if (prefs.reduceMotion) return
  if (kind === 'install' && !prefs.onInstall) return
  if (kind === 'launch' && !prefs.onLaunch) return

  if (kind === 'launch') {
    try {
      const r = await window.solea.getCustomLaunchSoundDataUrl()
      if (r.ok) {
        const base = 0.38
        const vol = base * Math.max(0, Math.min(1, prefs.volume))
        if (vol <= 0) return
        const a = new Audio(r.dataUrl)
        a.volume = vol
        await a.play().catch(() => {})
        return
      }
    } catch {
      /* fallback default */
    }
  }

  if (!prefs.master) return

  const src = kind === 'launch' ? launchPopUrl : installPopUrl
  const base = kind === 'launch' ? 0.38 : 0.44
  const vol = base * Math.max(0, Math.min(1, prefs.volume))
  if (vol <= 0) return

  try {
    const a = new Audio(src)
    a.volume = vol
    await a.play().catch(() => {})
  } catch {
    /* ignore */
  }
}
