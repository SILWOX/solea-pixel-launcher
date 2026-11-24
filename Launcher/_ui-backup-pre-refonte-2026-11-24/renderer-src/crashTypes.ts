/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */

export type CrashPayloadUi = {
  source: 'main' | 'renderer' | 'unknown'
  message: string
  stack?: string
  reason?: string
  exitCode?: number
  appVersion: string
  platform: string
  arch: string
  electronVersion: string
  occurredAt: string
  /** Fenêtre de démo depuis le debug : ne pas relancer pour de vrai. */
  isFakeDemo?: boolean
}
