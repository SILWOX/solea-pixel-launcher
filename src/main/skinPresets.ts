import { randomUUID } from 'crypto'
import { join } from 'path'
import { app } from 'electron'
import {
  existsSync,
  mkdirSync,
  readFileSync,
  writeFileSync,
  unlinkSync,
  rmSync
} from 'fs'
import { isLikelyPng, normalizeSkinUuid } from './skinLocal.js'

export type SkinModel = 'slim' | 'default'

export type SkinPresetRecord = {
  id: string
  name: string
  model: SkinModel
}

export type SkinManifest = {
  version: 1
  activePresetId: string | null
  presets: SkinPresetRecord[]
}

function accountDir(uuid: string): string {
  return join(app.getPath('userData'), 'skins', normalizeSkinUuid(uuid))
}

function manifestPath(uuid: string): string {
  return join(accountDir(uuid), 'manifest.json')
}

export function legacySingleSkinPath(uuid: string): string {
  return join(app.getPath('userData'), 'skins', `${normalizeSkinUuid(uuid)}.png`)
}

function presetPngPath(accountUuid: string, presetId: string): string {
  return join(accountDir(accountUuid), 'presets', `${presetId}.png`)
}

function hadLegacyImportedCapes(raw: Record<string, unknown>): boolean {
  if (Array.isArray(raw.capes) && raw.capes.length > 0) return true
  if (raw.mojangCapeId != null && raw.mojangCapeId !== '') return true
  if (!Array.isArray(raw.presets)) return false
  return raw.presets.some(
    (p) =>
      p &&
      typeof p === 'object' &&
      'capeId' in (p as object) &&
      (p as { capeId?: string | null }).capeId != null &&
      (p as { capeId?: string | null }).capeId !== ''
  )
}

function stripLegacyFields(m: SkinManifest): SkinManifest {
  return { version: 1, activePresetId: m.activePresetId, presets: m.presets }
}

function normalizeManifest(raw: unknown): SkinManifest {
  if (!raw || typeof raw !== 'object') {
    return { version: 1, activePresetId: null, presets: [] }
  }
  const o = raw as Record<string, unknown>
  const presets: SkinPresetRecord[] = Array.isArray(o.presets)
    ? o.presets
        .filter((p): p is Record<string, unknown> => !!p && typeof p === 'object')
        .map((p) => ({
          id: typeof p.id === 'string' ? p.id : randomUUID(),
          name: typeof p.name === 'string' ? p.name : 'Skin',
          model: p.model === 'slim' ? 'slim' : 'default'
        }))
    : []
  let activePresetId =
    typeof o.activePresetId === 'string' || o.activePresetId === null ? o.activePresetId : null
  if (activePresetId && !presets.some((pr) => pr.id === activePresetId)) {
    activePresetId = null
  }
  return { version: 1, activePresetId, presets }
}

function removeImportedCapesDir(uuid: string): void {
  const capesDir = join(accountDir(uuid), 'capes')
  if (existsSync(capesDir)) {
    try {
      rmSync(capesDir, { recursive: true, force: true })
    } catch {
      /* ignore */
    }
  }
}

function migrateLegacyIfNeeded(uuid: string): void {
  const legacy = legacySingleSkinPath(uuid)
  if (!existsSync(legacy)) return
  const mp = manifestPath(uuid)
  if (existsSync(mp)) {
    try {
      unlinkSync(legacy)
    } catch {
      /* ignore */
    }
    return
  }
  const id = randomUUID()
  const dir = accountDir(uuid)
  mkdirSync(join(dir, 'presets'), { recursive: true })
  const buf = readFileSync(legacy)
  writeFileSync(presetPngPath(uuid, id), buf)
  const manifest: SkinManifest = {
    version: 1,
    activePresetId: id,
    presets: [{ id, name: 'Skin importé', model: 'default' }]
  }
  writeFileSync(mp, JSON.stringify(manifest, null, 2), 'utf-8')
  try {
    unlinkSync(legacy)
  } catch {
    /* ignore */
  }
}

export function loadManifest(uuid: string): SkinManifest {
  migrateLegacyIfNeeded(uuid)
  const p = manifestPath(uuid)
  if (!existsSync(p)) {
    return { version: 1, activePresetId: null, presets: [] }
  }
  try {
    const raw = JSON.parse(readFileSync(p, 'utf-8')) as Record<string, unknown>
    const m = normalizeManifest(raw)
    if (
      m.activePresetId &&
      !m.presets.some((pr) => pr.id === m.activePresetId)
    ) {
      m.activePresetId = null
      saveManifest(uuid, m)
    }
    const legacyCapes = hadLegacyImportedCapes(raw)
    const hadShowFlag = 'showAccountCape' in raw
    if (legacyCapes) {
      removeImportedCapesDir(uuid)
    }
    if (legacyCapes || hadShowFlag) {
      saveManifest(uuid, stripLegacyFields(m))
    }
    return m
  } catch {
    return { version: 1, activePresetId: null, presets: [] }
  }
}

export function saveManifest(uuid: string, m: SkinManifest): void {
  const dir = accountDir(uuid)
  mkdirSync(join(dir, 'presets'), { recursive: true })
  writeFileSync(manifestPath(uuid), JSON.stringify(m, null, 2), 'utf-8')
}

function bufToDataUrl(buf: Buffer): string {
  return `data:image/png;base64,${buf.toString('base64')}`
}

export type PreviewBundle =
  | {
      kind: 'preset'
      dataUrl: string
      model: SkinModel
      capeDataUrl: string | null
    }
  | {
      kind: 'mojang'
      dataUrl: string
      model: 'auto-detect'
      capeDataUrl: string | null
    }

export async function getPreviewBundle(
  accountUuid: string,
  fetchMojangSkin: (u: string) => Promise<string | null>,
  getCapeDataUrl: (accountUuid: string) => Promise<string | null>
): Promise<PreviewBundle | null> {
  let m = loadManifest(accountUuid)
  const capeDataUrl = await getCapeDataUrl(accountUuid)

  if (m.activePresetId) {
    const pr = m.presets.find((x) => x.id === m.activePresetId)
    const png = pr ? presetPngPath(accountUuid, pr.id) : ''
    if (pr && png && existsSync(png)) {
      try {
        const buf = readFileSync(png)
        if (isLikelyPng(buf)) {
          return {
            kind: 'preset',
            dataUrl: bufToDataUrl(buf),
            model: pr.model,
            capeDataUrl
          }
        }
      } catch {
        /* repair manifest + fallback Mojang */
      }
    }
    m = { ...m, activePresetId: null }
    saveManifest(accountUuid, m)
  }

  const skin = await fetchMojangSkin(accountUuid)
  if (!skin) return null
  return {
    kind: 'mojang',
    dataUrl: skin,
    model: 'auto-detect',
    capeDataUrl
  }
}

export type PresetTile = {
  id: string
  name: string
  model: SkinModel
  thumbDataUrl: string
}

export function getPresetsState(accountUuid: string): {
  activePresetId: string | null
  presets: PresetTile[]
} {
  const m = loadManifest(accountUuid)
  const presets: PresetTile[] = []
  for (const pr of m.presets) {
    const png = presetPngPath(accountUuid, pr.id)
    if (!existsSync(png)) continue
    try {
      const buf = readFileSync(png)
      if (!isLikelyPng(buf)) continue
      presets.push({
        id: pr.id,
        name: pr.name,
        model: pr.model,
        thumbDataUrl: bufToDataUrl(buf)
      })
    } catch {
      /* skip */
    }
  }
  let activePresetId = m.activePresetId
  if (activePresetId && !presets.some((p) => p.id === activePresetId)) {
    activePresetId = null
    saveManifest(accountUuid, { ...m, activePresetId: null })
  }
  return {
    activePresetId,
    presets
  }
}

export function setActivePreset(accountUuid: string, presetId: string | null): void {
  const m = loadManifest(accountUuid)
  if (presetId !== null && !m.presets.some((p) => p.id === presetId)) return
  m.activePresetId = presetId
  saveManifest(accountUuid, m)
}

export function deletePreset(accountUuid: string, presetId: string): void {
  const m = loadManifest(accountUuid)
  m.presets = m.presets.filter((p) => p.id !== presetId)
  if (m.activePresetId === presetId) m.activePresetId = null
  saveManifest(accountUuid, m)
  try {
    unlinkSync(presetPngPath(accountUuid, presetId))
  } catch {
    /* ignore */
  }
}

export function updatePresetModel(accountUuid: string, presetId: string, model: SkinModel): void {
  const m = loadManifest(accountUuid)
  const pr = m.presets.find((p) => p.id === presetId)
  if (pr) {
    pr.model = model
    saveManifest(accountUuid, m)
  }
}

export function importPresetFromFile(
  accountUuid: string,
  filePath: string,
  model: SkinModel,
  displayName: string
): { ok: true; presetId: string } | { ok: false; error: string } {
  const buf = readFileSync(filePath)
  if (buf.length > 2 * 1024 * 1024) {
    return { ok: false, error: 'Fichier trop volumineux (max 2 Mo).' }
  }
  if (!isLikelyPng(buf)) {
    return { ok: false, error: 'Ce fichier n’est pas un PNG valide.' }
  }
  const id = randomUUID()
  const m = loadManifest(accountUuid)
  const dir = accountDir(accountUuid)
  mkdirSync(join(dir, 'presets'), { recursive: true })
  writeFileSync(presetPngPath(accountUuid, id), buf)
  const name = displayName.trim() || `Skin ${m.presets.length + 1}`
  m.presets.push({ id, name, model })
  m.activePresetId = id
  saveManifest(accountUuid, m)
  return { ok: true, presetId: id }
}

/** Données du preset actif pour envoi au profil Mojang (compte Java). */
export function getActivePresetSkinForSync(
  accountUuid: string
):
  | { kind: 'preset'; buffer: Buffer; model: SkinModel }
  | { kind: 'mojang' }
  | { kind: 'missing' } {
  const m = loadManifest(accountUuid)
  if (!m.activePresetId) return { kind: 'mojang' }
  const pr = m.presets.find((p) => p.id === m.activePresetId)
  if (!pr) return { kind: 'missing' }
  const png = presetPngPath(accountUuid, pr.id)
  if (!existsSync(png)) return { kind: 'missing' }
  try {
    const buffer = readFileSync(png)
    if (!isLikelyPng(buffer)) return { kind: 'missing' }
    return { kind: 'preset', buffer, model: pr.model }
  } catch {
    return { kind: 'missing' }
  }
}

export function clearAccountSkinStorage(accountUuid: string): void {
  const dir = accountDir(accountUuid)
  if (existsSync(dir)) {
    try {
      rmSync(dir, { recursive: true, force: true })
    } catch {
      /* ignore */
    }
  }
  const leg = legacySingleSkinPath(accountUuid)
  if (existsSync(leg)) {
    try {
      unlinkSync(leg)
    } catch {
      /* ignore */
    }
  }
}
