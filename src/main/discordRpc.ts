import { createRequire } from 'module'
import { existsSync, readFileSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'
import { DISCORD_RPC_APPLICATION_ID } from './discordRpcClientId.js'
import { logMain } from './logger.js'

const require = createRequire(import.meta.url)

type RpcClient = {
  login: (o: { clientId: string }) => Promise<void>
  setActivity: (a: Record<string, unknown>) => Promise<void>
  clearActivity: () => Promise<void>
  destroy: () => void
  request: (cmd: string, args: Record<string, unknown>) => Promise<unknown>
}

type ClientCtor = new (opts: { transport: string }) => RpcClient

let ClientClass: ClientCtor | null = null
let registerProtocol: ((id: string) => boolean) | null = null
try {
  const pkg = require('discord-rpc') as { Client: ClientCtor; register?: (id: string) => boolean }
  ClientClass = pkg.Client
  registerProtocol = typeof pkg.register === 'function' ? pkg.register.bind(pkg) : null
} catch {
  ClientClass = null
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let client: any = null
let ready = false
/** Stable “session” start so Discord doesn’t reset the timer on every presence update. */
let presenceSessionStartMs = 0
let warnedMissingClientId = false
let loggedInitFailure = false
let loggedSetActivityHint = false

function readUserDataClientIdFile(): string {
  try {
    if (!app?.isReady?.()) return ''
    const p = join(app.getPath('userData'), 'discord-rpc-client-id.txt')
    if (!existsSync(p)) return ''
    const line = readFileSync(p, 'utf8').split(/\r?\n/)[0]?.trim() ?? ''
    return /^\d{17,20}$/.test(line) ? line : ''
  } catch {
    return ''
  }
}

function resolveRpcClientId(): string {
  const env = process.env.SOLEA_DISCORD_CLIENT_ID?.trim()
  if (env && /^\d{17,20}$/.test(env)) return env
  const fromFile = readUserDataClientIdFile()
  if (fromFile) return fromFile
  const bundled = DISCORD_RPC_APPLICATION_ID.trim()
  if (bundled && /^\d{17,20}$/.test(bundled)) return bundled
  return ''
}

export async function initDiscordRpcIfNeeded(): Promise<void> {
  const id = resolveRpcClientId()
  if (!id) {
    if (!warnedMissingClientId) {
      warnedMissingClientId = true
      logMain(
        'warn',
        'Discord Rich Presence: aucun Client ID. Définissez SOLEA_DISCORD_CLIENT_ID, ou créez discord-rpc-client-id.txt (une ligne, chiffres uniquement) dans le dossier données du launcher, ou renseignez DISCORD_RPC_APPLICATION_ID dans discordRpcClientId.ts.'
      )
    }
    return
  }
  if (!ClientClass || client) return
  try {
    try {
      registerProtocol?.(id)
    } catch {
      /* optionnel : enregistrement du schéma discord-<id>:// */
    }
    const c = new ClientClass({ transport: 'ipc' })
    await c.login({ clientId: id })
    client = c
    ready = true
    loggedInitFailure = false
    logMain('info', 'Discord Rich Presence connectée', { clientId: id })
  } catch (e) {
    client = null
    ready = false
    if (!loggedInitFailure) {
      loggedInitFailure = true
      logMain('warn', 'Discord Rich Presence: échec de connexion (Discord ouvert ? Client ID valide ?)', {
        message: e instanceof Error ? e.message : String(e)
      })
    }
  }
}

/** Réessaie la connexion RPC (ex. Discord lancé après le launcher). */
export async function reconnectDiscordRpcIfNeeded(): Promise<void> {
  const id = resolveRpcClientId()
  if (!id || !ClientClass) return
  if (client) return
  loggedInitFailure = false
  await initDiscordRpcIfNeeded()
}

/** Releases du launcher (binaires) — lien public GitHub. */
export const SOLEA_LAUNCHER_RELEASES_URL =
  'https://github.com/SILWOX/solea-pixel-launcher/releases'

export type RichPresencePack = {
  modpackName: string
  largeImageKey: string
  locale: 'en' | 'fr'
  /** Page Modrinth du modpack actif (bouton « Regarder » / « Watch »). */
  modrinthPackUrl: string
}

function clip(s: string, max: number): string {
  const t = s.trim()
  return t.length <= max ? t : t.slice(0, max - 1) + '…'
}

async function sendSetActivity(activity: Record<string, unknown>): Promise<void> {
  if (!client?.request) return
  await client.request('SET_ACTIVITY', {
    pid: process.pid,
    activity
  })
}

export async function setLauncherPresence(opts: RichPresencePack & { inGame: boolean }): Promise<void> {
  if (!ready || !client) return
  const details = opts.inGame
    ? opts.locale === 'fr'
      ? 'En jeu'
      : 'In game'
    : opts.locale === 'fr'
      ? 'Dans le launcher'
      : 'In launcher'
  const state = clip(opts.modpackName, 120)
  if (!presenceSessionStartMs) presenceSessionStartMs = Date.now()

  const largeKey = (opts.largeImageKey || 'logo').trim() || 'logo'

  const modrinth = String(opts.modrinthPackUrl ?? '').trim()
  const buttons =
    modrinth.startsWith('https://') && modrinth.length <= 512
      ? [
          {
            label: opts.locale === 'fr' ? 'Télécharger' : 'Download',
            url: SOLEA_LAUNCHER_RELEASES_URL
          },
          {
            label: opts.locale === 'fr' ? 'Regarder' : 'Watch',
            url: modrinth
          }
        ]
      : [{ label: opts.locale === 'fr' ? 'Télécharger' : 'Download', url: SOLEA_LAUNCHER_RELEASES_URL }]

  const baseActivity: Record<string, unknown> = {
    type: 0,
    details,
    state,
    timestamps: { start: presenceSessionStartMs },
    instance: false,
    buttons
  }

  const assets = {
    large_image: largeKey,
    large_text: 'Solea Pixel Launcher'
  }

  const activityNoButtons: Record<string, unknown> = { ...baseActivity }
  delete activityNoButtons.buttons

  try {
    await sendSetActivity({ ...baseActivity, assets })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    logMain('warn', 'Discord Rich Presence: essai sans boutons (image conservée)', {
      message: msg,
      largeKey
    })
    try {
      await sendSetActivity({ ...activityNoButtons, assets })
    } catch (e2) {
      const msg2 = e2 instanceof Error ? e2.message : String(e2)
      logMain('warn', 'Discord Rich Presence: setActivity (sans image)', { message: msg2, largeKey })
      try {
        await sendSetActivity(activityNoButtons)
      } catch (e3) {
        const msg3 = e3 instanceof Error ? e3.message : String(e3)
        logMain('warn', 'Discord Rich Presence: setActivity a échoué', { message: msg3 })
        if (!loggedSetActivityHint) {
          loggedSetActivityHint = true
          logMain(
            'info',
            'Vérifiez : Discord ouvert, partage d’activité activé (Paramètres Discord → Confidentialité), images Rich Presence sur le portail développeur (clés logo, solea_pack_palamod, solea_pack_wither).'
          )
        }
      }
    }
  }
}

export async function setInGamePresence(opts: RichPresencePack): Promise<void> {
  await setLauncherPresence({ ...opts, inGame: true })
}

export async function setMenuPresence(opts: RichPresencePack): Promise<void> {
  await setLauncherPresence({ ...opts, inGame: false })
}

export async function clearDiscordPresence(): Promise<void> {
  if (!client || !ready) return
  try {
    await client.clearActivity()
  } catch {
    /* ignore */
  }
}

export function shutdownDiscordRpc(): void {
  try {
    client?.destroy()
  } catch {
    /* ignore */
  }
  client = null
  ready = false
  presenceSessionStartMs = 0
  loggedSetActivityHint = false
}
