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
 * Sons Mixkit (fichiers locaux). Respecte le volume global et les types d’événements.
 */
export function playUiSound(kind: UiSoundKind, prefs: UiSoundPrefs): void {
  if (!prefs.master || prefs.reduceMotion) return
  if (kind === 'install' && !prefs.onInstall) return
  if (kind === 'launch' && !prefs.onLaunch) return

  const src = kind === 'launch' ? launchPopUrl : installPopUrl
  const base = kind === 'launch' ? 0.38 : 0.44
  const vol = base * Math.max(0, Math.min(1, prefs.volume))
  if (vol <= 0) return

  try {
    const a = new Audio(src)
    a.volume = vol
    void a.play().catch(() => {
      /* autoplay / decode */
    })
  } catch {
    /* ignore */
  }
}
