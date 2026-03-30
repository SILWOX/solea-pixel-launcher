import { existsSync, readFileSync, writeFileSync, mkdirSync, unlinkSync } from 'fs'
import { join } from 'path'
import { app } from 'electron'
import type { MicrosoftAuthResponse } from 'minecraft-java-core'

export interface AccountsStore {
  activeUuid: string | null
  accounts: MicrosoftAuthResponse[]
}

function userData(): string {
  return app.getPath('userData')
}

function storePath(): string {
  return join(userData(), 'accounts.json')
}

function legacyAccountPath(): string {
  return join(userData(), 'account.json')
}

function normalizeUuid(u: string): string {
  return u.replace(/-/g, '').toLowerCase()
}

function migrateLegacyIfNeeded(): void {
  const sp = storePath()
  if (existsSync(sp)) return
  const lp = legacyAccountPath()
  if (!existsSync(lp)) return
  try {
    const acc = JSON.parse(readFileSync(lp, 'utf8')) as MicrosoftAuthResponse
    if (acc?.uuid && acc?.refresh_token) {
      const store: AccountsStore = { activeUuid: acc.uuid, accounts: [acc] }
      mkdirSync(userData(), { recursive: true })
      writeFileSync(sp, JSON.stringify(store, null, 2), 'utf8')
      try {
        unlinkSync(lp)
      } catch {
        /* ignore */
      }
    }
  } catch {
    /* ignore */
  }
}

export function loadStore(): AccountsStore {
  migrateLegacyIfNeeded()
  const sp = storePath()
  if (!existsSync(sp)) return { activeUuid: null, accounts: [] }
  try {
    const raw = JSON.parse(readFileSync(sp, 'utf8')) as AccountsStore
    if (!Array.isArray(raw.accounts)) return { activeUuid: null, accounts: [] }
    return {
      activeUuid: raw.activeUuid ?? null,
      accounts: raw.accounts
    }
  } catch {
    return { activeUuid: null, accounts: [] }
  }
}

function writeStore(store: AccountsStore): void {
  mkdirSync(userData(), { recursive: true })
  writeFileSync(storePath(), JSON.stringify(store, null, 2), 'utf8')
}

export function addOrUpdateAccount(acc: MicrosoftAuthResponse): void {
  const store = loadStore()
  const idx = store.accounts.findIndex((a) => normalizeUuid(a.uuid) === normalizeUuid(acc.uuid))
  if (idx >= 0) store.accounts[idx] = acc
  else store.accounts.push(acc)
  store.activeUuid = acc.uuid
  writeStore(store)
}

export function setActiveUuid(uuid: string): { ok: true } | { ok: false; error: string } {
  const store = loadStore()
  const found = store.accounts.some((a) => normalizeUuid(a.uuid) === normalizeUuid(uuid))
  if (!found) return { ok: false, error: 'Compte introuvable.' }
  store.activeUuid = uuid
  writeStore(store)
  return { ok: true }
}

export function removeAccount(uuid: string): void {
  const store = loadStore()
  store.accounts = store.accounts.filter((a) => normalizeUuid(a.uuid) !== normalizeUuid(uuid))
  if (store.activeUuid && normalizeUuid(store.activeUuid) === normalizeUuid(uuid)) {
    store.activeUuid = store.accounts[0]?.uuid ?? null
  }
  writeStore(store)
}

export function getActiveAccount(): MicrosoftAuthResponse | null {
  const store = loadStore()
  if (!store.activeUuid) return store.accounts[0] ?? null
  return (
    store.accounts.find((a) => normalizeUuid(a.uuid) === normalizeUuid(store.activeUuid!)) ??
    store.accounts[0] ??
    null
  )
}

export function listAccountSummaries(): { uuid: string; name: string }[] {
  return loadStore().accounts.map((a) => ({ uuid: a.uuid, name: a.name }))
}

export function hasAnyAccount(): boolean {
  return loadStore().accounts.length > 0
}

export function updateAccountTokens(acc: MicrosoftAuthResponse): void {
  const store = loadStore()
  const idx = store.accounts.findIndex((a) => normalizeUuid(a.uuid) === normalizeUuid(acc.uuid))
  if (idx >= 0) store.accounts[idx] = acc
  else {
    store.accounts.push(acc)
    if (!store.activeUuid) store.activeUuid = acc.uuid
  }
  writeStore(store)
}
