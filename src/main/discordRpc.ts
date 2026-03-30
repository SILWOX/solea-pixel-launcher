import { createRequire } from 'module'

const require = createRequire(import.meta.url)

type ClientCtor = new (opts: { transport: string }) => {
  login: (o: { clientId: string }) => Promise<void>
  setActivity: (a: Record<string, unknown>) => Promise<void>
  clearActivity: () => Promise<void>
  destroy: () => void
}

let ClientClass: ClientCtor | null = null
try {
  ClientClass = require('discord-rpc').Client as ClientCtor
} catch {
  ClientClass = null
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let client: any = null
let ready = false

function clientId(): string {
  return process.env.SOLEA_DISCORD_CLIENT_ID?.trim() || ''
}

export async function initDiscordRpcIfNeeded(): Promise<void> {
  const id = clientId()
  if (!id || !ClientClass || client) return
  try {
    const c = new ClientClass({ transport: 'ipc' })
    await c.login({ clientId: id })
    client = c
    ready = true
  } catch {
    client = null
    ready = false
  }
}

export type RichPresencePack = {
  modpackName: string
  largeImageKey: string
  locale: 'en' | 'fr'
}

function clip(s: string, max: number): string {
  const t = s.trim()
  return t.length <= max ? t : t.slice(0, max - 1) + '…'
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
  try {
    await client.setActivity({
      details,
      state,
      startTimestamp: Date.now(),
      largeImageKey: opts.largeImageKey || 'logo',
      largeImageText: 'Solea Pixel Launcher',
      instance: false
    })
  } catch {
    /* ignore */
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
}
