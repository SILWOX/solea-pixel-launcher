/** MY SERVER — hébergement local (UI). */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { isModpackId } from './modpackTheme'
import { useI18n } from './i18n/I18nContext'
import { useToast } from './ui/ToastContext'
import { LauncherSelect, type LauncherSelectEntry } from './ui/LauncherSelect'
import { useFocusTrap } from './a11y/useFocusTrap'
import {
  defaultFormFromMap,
  formToUpdates,
  mergeManagedKeysIntoRaw,
  parseServerProperties,
  validatePropsForm,
  type ServerPropsForm
} from './serverPropertiesFormat'
import './my-server.css'

const PROPS_ERR_I18N: Record<NonNullable<ReturnType<typeof validatePropsForm>>, string> = {
  port: 'myServer.propsErrPort',
  maxPlayers: 'myServer.propsErrMaxPlayers',
  viewDistance: 'myServer.propsErrViewDistance',
  simulationDistance: 'myServer.propsErrSimulationDistance',
  spawnProtection: 'myServer.propsErrSpawnProtection'
}

const AUTOSAVE_MS = 850
const MAX_COVER_BYTES = 12 * 1024 * 1024
const MY_SERVER_VANILLA_PACK_ID = 'vanilla'

const VANILLA_RELEASE_FALLBACK_IDS = ['1.21.4', '1.20.6', '1.16.5', '1.12.2', '1.8.9'] as const

type VanillaReleasesFetch =
  | { kind: 'loading' }
  | { kind: 'ok'; ids: string[] }
  | { kind: 'err'; message: string }

function isSelectOptionEntry(e: LauncherSelectEntry): e is { value: string; label: string } {
  return 'value' in e
}

function flatSelectOptions(entries: LauncherSelectEntry[]): { value: string; label: string }[] {
  return entries.filter(isSelectOptionEntry)
}

function vanillaIdsForSelect(v: VanillaReleasesFetch): string[] {
  if (v.kind === 'ok' && v.ids.length > 0) return v.ids
  return [...VANILLA_RELEASE_FALLBACK_IDS]
}

function buildVanillaVersionSelectOptions(
  v: VanillaReleasesFetch,
  /** Version actuelle du serveur si elle n’apparaît pas encore dans la liste Mojang. */
  pin?: string | undefined
): { value: string; label: string }[] {
  const ids = vanillaIdsForSelect(v)
  const opts = ids.map((id) => ({ value: id, label: id }))
  const p = pin?.trim()
  if (p && vanillaReleaseMeetsMin18(p) && !opts.some((o) => o.value === p)) {
    return [{ value: p, label: p }, ...opts]
  }
  return opts
}

/** Aligné sur `isVanillaServerVersionAtLeast18` (main) : releases 1.7.x refusées, minimum 1.8. */
function vanillaReleaseMeetsMin18(versionId: string): boolean {
  const v = versionId.trim()
  const m = /^(\d+)\.(\d+)(?:\.(\d+))?$/.exec(v)
  if (!m) return true
  const major = parseInt(m[1], 10)
  const minor = parseInt(m[2], 10)
  if (Number.isNaN(major) || Number.isNaN(minor)) return true
  if (major > 1) return true
  if (major < 1) return false
  return minor >= 8
}

const WORLD_LEVEL_TYPES = [
  { value: 'minecraft:normal', labelKey: 'myServer.worldTypeNormal' as const },
  { value: 'minecraft:flat', labelKey: 'myServer.worldTypeFlat' as const },
  { value: 'minecraft:large_biomes', labelKey: 'myServer.worldTypeLargeBiomes' as const },
  { value: 'minecraft:amplified', labelKey: 'myServer.worldTypeAmplified' as const }
]

function serverConsoleLineClass(line: string): string {
  const base = 'my-server-console-line'
  const lower = line.toLowerCase()
  if (lower.startsWith('[stderr]')) return `${base} ${base}--err`
  const inner = line.startsWith('[stderr]') ? line.slice(9) : line
  if (line.includes('[Solea]')) return `${base} ${base}--solea`
  if (
    /\[.*\/error\]|\[.*\/err\]|fatal error|\bfatal\b|\berror:\b|unhandled|exception in thread|java\.lang\.|could not|cannot find|failed to load|failed to bind|address already in use|incompatible|nosuchfile|classnotfound/i.test(
      inner
    )
  ) {
    return `${base} ${base}--err`
  }
  if (/\[.*\/warn\]|\bwarn\b|warning:/i.test(inner)) return `${base} ${base}--warn`
  if (/\/(info|init)\]/i.test(inner)) return `${base} ${base}--info`
  if (/\/debug\]/i.test(inner)) return `${base} ${base}--debug`
  if (/for help, type/i.test(inner)) return `${base} ${base}--success`
  return base
}

type SoleServerListRowUi = {
  id: string
  name: string
  description: string
  modpackId: string
  vanillaGameVersion?: string
  ramMiB: number
  port: number
  coverFile?: string
  createdAt: string
  installState: 'installing' | 'ready' | 'error'
  installError?: string
  modrinthVersionId?: string
  modrinthVersionNumber?: string
  serverState: 'stopped' | 'starting' | 'running' | 'stopping'
}

type MyServerViewProps = {
  modpacksList: { id: string; displayName: string }[]
  /** Police pixel + effets titres : aligné sur Paramètres → barre titre + chrome. */
  chromeGlass: boolean
}

async function readImageFileAsDataUrl(file: File): Promise<string | null> {
  if (!file.type.startsWith('image/')) return null
  return new Promise((resolve) => {
    const r = new FileReader()
    r.onload = () => resolve(typeof r.result === 'string' ? r.result : null)
    r.onerror = () => resolve(null)
    r.readAsDataURL(file)
  })
}

function CoverImageField({
  inputId,
  previewUrl,
  onPick,
  onClear,
  pickLabel,
  clearLabel,
  tooLargeLabel
}: {
  inputId: string
  previewUrl: string | null
  onPick: (dataUrl: string) => void
  onClear: () => void
  pickLabel: string
  clearLabel: string
  tooLargeLabel: string
  onTooLarge?: () => void
}) {
  const ref = useRef<HTMLInputElement>(null)
  return (
    <div className="my-server-cover-field">
      <input
        ref={ref}
        id={inputId}
        type="file"
        accept="image/png,image/jpeg,image/webp"
        className="sr-only"
        onChange={async (e) => {
          const f = e.target.files?.[0]
          e.target.value = ''
          if (!f) return
          if (f.size > MAX_COVER_BYTES) {
            onTooLarge?.()
            return
          }
          const url = await readImageFileAsDataUrl(f)
          if (url) onPick(url)
        }}
      />
      <div className="my-server-cover-actions">
        {previewUrl ? (
          <div className="my-server-cover-preview">
            <img src={previewUrl} alt="" key={previewUrl} decoding="async" />
            <div className="my-server-cover-preview-actions">
              <button type="button" className="btn-muted" onClick={() => ref.current?.click()}>
                {pickLabel}
              </button>
              <button type="button" className="btn-muted" onClick={onClear}>
                {clearLabel}
              </button>
            </div>
          </div>
        ) : (
          <button type="button" className="my-server-cover-placeholder" onClick={() => ref.current?.click()}>
            <span className="my-server-cover-placeholder-icon" aria-hidden>
              +
            </span>
            <span>{pickLabel}</span>
          </button>
        )}
      </div>
    </div>
  )
}

export function MyServerView({ modpacksList, chromeGlass }: MyServerViewProps) {
  const { t } = useI18n()
  const { pushToast } = useToast()
  const mc = chromeGlass ? ' font-mc' : ''
  const packOptions = useMemo((): LauncherSelectEntry[] => {
    const instanceOpts = modpacksList
      .filter((m) => isModpackId(m.id))
      .map((m) => ({ value: m.id, label: m.displayName }))
    return [
      { type: 'group', label: t('myServer.packGroupVanilla') },
      { value: MY_SERVER_VANILLA_PACK_ID, label: t('myServer.packOptionVanilla') },
      { type: 'group', label: t('myServer.packGroupInstances') },
      ...instanceOpts
    ]
  }, [modpacksList, t])

  const serverPackLabel = useCallback(
    (s: SoleServerListRowUi) => {
      if (s.modpackId === MY_SERVER_VANILLA_PACK_ID) {
        const v = s.vanillaGameVersion?.trim()
        return v ? `${t('myServer.packOptionVanilla')} · ${v}` : t('myServer.packOptionVanilla')
      }
      return flatSelectOptions(packOptions).find((p) => p.value === s.modpackId)?.label ?? s.modpackId
    },
    [packOptions, t]
  )

  const [vanillaReleases, setVanillaReleases] = useState<VanillaReleasesFetch>({ kind: 'loading' })
  useEffect(() => {
    let cancelled = false
    void window.solea.soleaServerListVanillaReleases().then((r) => {
      if (cancelled) return
      if (r.ok) setVanillaReleases({ kind: 'ok', ids: r.ids })
      else setVanillaReleases({ kind: 'err', message: r.error })
    })
    return () => {
      cancelled = true
    }
  }, [])

  const [servers, setServers] = useState<SoleServerListRowUi[]>([])
  const [covers, setCovers] = useState<Record<string, string | null>>({})
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null)
  const [profileEditId, setProfileEditId] = useState<string | null>(null)
  const [memoryGiB, setMemoryGiB] = useState(16)

  const refreshList = useCallback(async () => {
    const list = await window.solea.soleaServerList()
    setServers(list)
  }, [])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    if (profileEditId && !servers.some((s) => s.id === profileEditId)) setProfileEditId(null)
  }, [servers, profileEditId])

  useEffect(() => {
    void window.solea.getMemoryStats().then((m) => setMemoryGiB(m.totalGiB))
  }, [])

  const coverDigest = useMemo(() => servers.map((s) => `${s.id}:${s.coverFile ?? ''}`).join('|'), [servers])

  useEffect(() => {
    let cancelled = false
    void (async () => {
      const entries = await Promise.all(
        servers.map(async (s) => {
          if (!s.coverFile) return [s.id, null] as const
          const u = await window.solea.soleaServerGetCoverDataUrl(s.id)
          return [s.id, u] as const
        })
      )
      if (!cancelled) setCovers(Object.fromEntries(entries))
    })()
    return () => {
      cancelled = true
    }
  }, [coverDigest])

  useEffect(() => {
    const installing = servers.some((s) => s.installState === 'installing')
    if (!installing) return
    const id = window.setInterval(() => void refreshList(), 2000)
    return () => clearInterval(id)
  }, [servers, refreshList])

  const selected = selectedId ? servers.find((s) => s.id === selectedId) : undefined

  return (
    <>
      {selected && selectedId ? (
        <MyServerDetail
          server={selected}
          memoryGiB={memoryGiB}
          chromeGlass={chromeGlass}
          onBack={() => setSelectedId(null)}
          onRefresh={refreshList}
          onDeleteRequest={() => setDeleteConfirmId(selected.id)}
          pushToast={pushToast}
          t={t}
        />
      ) : (
      <div
        className="my-server-layout my-server-layout--hub"
        data-my-server-chrome={chromeGlass ? '1' : '0'}
      >
        <div className="my-server-scroll my-server-scroll--hub">
          <div className="my-server-hub-bg" aria-hidden />
          <section
            className="my-server-beta-banner"
            role="region"
            aria-label={t('myServer.betaBannerAria')}
          >
            <div className="my-server-beta-banner__stripes" aria-hidden />
            <div className="my-server-beta-banner__inner">
              <div className="my-server-beta-banner__stamp-wrap" aria-hidden>
                <span className="my-server-beta-banner__stamp">{t('myServer.betaBannerStamp')}</span>
              </div>
              <div className="my-server-beta-banner__copy">
                <h3 id="my-server-beta-heading" className={`my-server-beta-banner__title${mc}`}>
                  {t('myServer.betaBannerTitle')}
                </h3>
                <p className="my-server-beta-banner__body">{t('myServer.betaBannerBody')}</p>
              </div>
            </div>
          </section>

          <div className="my-server-hub-inner">
            <div className="my-server-landing">
              <div className="my-server-landing-main">
                <section className="my-server-hero my-server-hero--aether" aria-labelledby="my-server-hero-title">
                  <div className="my-server-hero-accent" aria-hidden />
                  <p className={`my-server-hero-eyebrow${mc}`}>{t('myServer.createEyebrow')}</p>
                  <h2 id="my-server-hero-title" className={`my-server-hero-title${mc}`}>
                    {t('myServer.heroTitle')}
                  </h2>
                  <p className="my-server-hero-lead">{t('myServer.heroLead')}</p>
                  <div className="my-server-pill-row" role="list">
                    {(['pill1', 'pill2', 'pill3'] as const).map((k) => (
                      <div key={k} className="my-server-pill" role="listitem">
                        <span className="my-server-pill-check" aria-hidden>
                          ✓
                        </span>
                        <span className="my-server-pill-text">{t(`myServer.${k}`)}</span>
                      </div>
                    ))}
                  </div>
                </section>
              </div>
              <aside className="my-server-aside" aria-label={t('myServer.asideAria')}>
                <div className="my-server-aside-visual" aria-hidden>
                  <span className="my-server-aside-dot" />
                  <span className="my-server-aside-bar" />
                  <span className="my-server-aside-bar my-server-aside-bar--mid" />
                  <span className="my-server-aside-bar my-server-aside-bar--short" />
                </div>
                <p className={`my-server-aside-kicker${mc}`}>{t('myServer.asideKicker')}</p>
                <p className="my-server-aside-text">{t('myServer.asideBlurb')}</p>
              </aside>
            </div>

            <div className="my-server-stage">
              <div className="my-server-stage__head">
                <span className={`my-server-stage__label${mc}`}>{t('myServer.stageLabel')}</span>
                <span className="my-server-stage__rule" aria-hidden />
              </div>
              <div
                className={`my-server-grid${servers.length === 0 ? ' my-server-grid--empty' : ''}`}
              >
            {servers.map((s) => (
              <div key={s.id} className="my-server-card-wrap">
                <button type="button" className="my-server-card" onClick={() => setSelectedId(s.id)}>
                  {s.installState === 'installing' ? (
                    <span className="my-server-card-badge">{t('myServer.badgeInstalling')}</span>
                  ) : null}
                  {s.installState === 'error' ? (
                    <span className="my-server-card-badge my-server-card-badge--err">{t('myServer.badgeError')}</span>
                  ) : null}
                  <div className="my-server-card-cover">
                    {covers[s.id] ? (
                      <img src={covers[s.id]!} alt="" decoding="async" />
                    ) : (
                      <span aria-hidden className="my-server-card-ph">
                        ◆
                      </span>
                    )}
                  </div>
                  <div className="my-server-card-body">
                    <div className={`my-server-card-title${mc}`}>{s.name}</div>
                    <div className="my-server-card-meta">{serverPackLabel(s)}</div>
                  </div>
                </button>
                <button
                  type="button"
                  className="my-server-card-edit"
                  aria-label={t('myServer.editProfile')}
                  onClick={() => setProfileEditId(s.id)}
                >
                  <svg
                    className="my-server-card-edit-svg"
                    viewBox="0 0 24 24"
                    width={18}
                    height={18}
                    fill="none"
                    stroke="currentColor"
                    strokeWidth={2}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden
                  >
                    <path d="M12 20h9" />
                    <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                  </svg>
                </button>
              </div>
            ))}
            <button
              type="button"
              className={`my-server-card my-server-add-card${servers.length === 0 ? ' my-server-add-card--cta' : ''}`}
              onClick={() => setCreateOpen(true)}
            >
              <span className="my-server-add-ico" aria-hidden>
                +
              </span>
              <span className={`my-server-add-label${mc}`}>
                {servers.length ? t('myServer.addAnother') : t('myServer.addFirst')}
              </span>
            </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      )}
      {createOpen ? (
        <MyServerCreateModal
          packOptions={packOptions}
          vanillaReleases={vanillaReleases}
          chromeGlass={chromeGlass}
          onClose={() => setCreateOpen(false)}
          onCreated={(id) => {
            setCreateOpen(false)
            void refreshList().then(() => setSelectedId(id))
          }}
          pushToast={pushToast}
          t={t}
        />
      ) : null}
      {deleteConfirmId ? (
        <DeleteServerModal
          onCancel={() => setDeleteConfirmId(null)}
          onConfirm={async () => {
            const r = await window.solea.soleaServerDelete(deleteConfirmId)
            setDeleteConfirmId(null)
            if (r.ok) {
              pushToast(t('myServer.deleted'), 'success')
              setSelectedId(null)
              void refreshList()
            } else pushToast(r.error, 'error')
          }}
          t={t}
        />
      ) : null}
      {profileEditId && servers.some((s) => s.id === profileEditId) ? (
        <MyServerProfileModal
          server={servers.find((s) => s.id === profileEditId)!}
          coverUrl={covers[profileEditId] ?? null}
          packOptions={packOptions}
          vanillaReleases={vanillaReleases}
          chromeGlass={chromeGlass}
          onClose={() => setProfileEditId(null)}
          onRefresh={refreshList}
          pushToast={pushToast}
          t={t}
        />
      ) : null}
    </>
  )
}

function DeleteServerModal({
  onCancel,
  onConfirm,
  t
}: {
  onCancel: () => void
  onConfirm: () => void | Promise<void>
  t: (k: string) => string
}) {
  const modalRef = useRef<HTMLDivElement>(null)
  useFocusTrap(true, modalRef, { onEscape: onCancel })
  return (
    <div className="pack-confirm-backdrop my-server-modal-backdrop" role="presentation" onClick={onCancel}>
      <div
        ref={modalRef}
        className="pack-confirm-modal my-server-modal-aether solea-modal-surface"
        role="alertdialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="my-server-modal-heading">{t('myServer.deleteTitle')}</h2>
        <p className="my-server-modal-text">{t('myServer.deleteBody')}</p>
        <div className="pack-confirm-actions">
          <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onCancel}>
            {t('confirm.packCancel')}
          </button>
          <button type="button" className="btn-danger-outline pack-confirm-btn-primary" onClick={() => void onConfirm()}>
            {t('myServer.deleteConfirm')}
          </button>
        </div>
      </div>
    </div>
  )
}

function PackChangeConfirmModal({
  packLabel,
  onCancel,
  onConfirm,
  t,
  chromeGlass,
  stacked
}: {
  packLabel: string
  onCancel: () => void
  onConfirm: () => void
  t: (k: string, ...args: unknown[]) => string
  chromeGlass: boolean
  /** Au-dessus d’une autre modale (ex. profil serveur). */
  stacked?: boolean
}) {
  const modalRef = useRef<HTMLDivElement>(null)
  const mc = chromeGlass ? ' font-mc' : ''
  useFocusTrap(true, modalRef, { onEscape: onCancel })
  return (
    <div
      className={`pack-confirm-backdrop my-server-modal-backdrop${stacked ? ' my-server-modal-backdrop--stack' : ''}`}
      role="presentation"
      onClick={onCancel}
    >
      <div
        ref={modalRef}
        className="pack-confirm-modal my-server-modal-aether solea-modal-surface"
        role="alertdialog"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className={`my-server-modal-heading${mc}`}>{t('myServer.packChangeTitle')}</h2>
        <p className="my-server-modal-text">{t('myServer.packChangeBody', { pack: packLabel })}</p>
        <div className="pack-confirm-actions">
          <button type="button" className="btn-muted pack-confirm-btn-cancel" onClick={onCancel}>
            {t('confirm.packCancel')}
          </button>
          <button type="button" className={`btn-save${mc} pack-confirm-btn-primary`} onClick={onConfirm}>
            {t('myServer.packChangeConfirm')}
          </button>
        </div>
      </div>
    </div>
  )
}

function MyServerCreateModal({
  packOptions,
  vanillaReleases,
  chromeGlass,
  onClose,
  onCreated,
  pushToast,
  t
}: {
  packOptions: LauncherSelectEntry[]
  vanillaReleases: VanillaReleasesFetch
  chromeGlass: boolean
  onClose: () => void
  onCreated: (id: string) => void
  pushToast: (msg: string, variant?: 'info' | 'success' | 'error') => void
  t: (k: string, ...args: unknown[]) => string
}) {
  const modalRef = useRef<HTMLDivElement>(null)
  useFocusTrap(true, modalRef, { onEscape: onClose })
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [coverDataUrl, setCoverDataUrl] = useState<string | null>(null)
  const [packId, setPackId] = useState(MY_SERVER_VANILLA_PACK_ID)
  const vanillaVersionOpts = useMemo(
    () => buildVanillaVersionSelectOptions(vanillaReleases),
    [vanillaReleases]
  )
  const [vanillaGameVersion, setVanillaGameVersion] = useState(
    () => buildVanillaVersionSelectOptions({ kind: 'loading' })[0]?.value ?? ''
  )
  const [busy, setBusy] = useState(false)
  const mc = chromeGlass ? ' font-mc' : ''

  useEffect(() => {
    const first = vanillaVersionOpts[0]?.value ?? ''
    setVanillaGameVersion((prev) => {
      if (prev && vanillaVersionOpts.some((o) => o.value === prev)) return prev
      return first
    })
  }, [vanillaVersionOpts])

  const submit = async () => {
    if (!name.trim()) {
      pushToast(t('myServer.errName'), 'error')
      return
    }
    if (!packId) {
      pushToast(t('myServer.errPack'), 'error')
      return
    }
    if (packId === MY_SERVER_VANILLA_PACK_ID) {
      const v = vanillaGameVersion.trim()
      if (!v) {
        pushToast(t('myServer.errVanillaVersion'), 'error')
        return
      }
      if (!vanillaReleaseMeetsMin18(v)) {
        pushToast(t('myServer.errVanillaMin18'), 'error')
        return
      }
    }
    setBusy(true)
    const r = await window.solea.soleaServerCreate({
      name: name.trim(),
      description: description.trim() || undefined,
      modpackId: packId,
      coverImageDataUrl: coverDataUrl ?? undefined,
      ...(packId === MY_SERVER_VANILLA_PACK_ID ? { vanillaGameVersion: vanillaGameVersion.trim() } : {})
    })
    setBusy(false)
    if (r.ok) {
      pushToast(t('myServer.createStarted'), 'info')
      onCreated(r.id)
    } else pushToast(r.error, 'error')
  }

  return (
    <div className="pack-confirm-backdrop my-server-modal-backdrop" role="presentation" onClick={onClose}>
      <div
        ref={modalRef}
        data-my-server-chrome={chromeGlass ? '1' : '0'}
        className="my-server-modal-create pack-confirm-modal my-server-modal-aether solea-modal-surface"
        role="dialog"
        aria-modal="true"
        aria-labelledby="my-server-create-title"
        onClick={(e) => e.stopPropagation()}
      >
        <p className={`my-server-modal-eyebrow${mc}`}>{t('myServer.createEyebrow')}</p>
        <h2 id="my-server-create-title" className={`my-server-modal-title${mc}`}>
          {t('myServer.createTitle')}
        </h2>
        <div className="my-server-field">
          <label htmlFor="ms-name">{t('myServer.fieldName')}</label>
          <input
            id="ms-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoComplete="off"
          />
        </div>
        <div className="my-server-field">
          <label htmlFor="ms-desc">{t('myServer.fieldDesc')}</label>
          <textarea id="ms-desc" value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="my-server-field">
          <span className="my-server-field-label">{t('myServer.fieldCover')}</span>
          <CoverImageField
            inputId="ms-cover"
            previewUrl={coverDataUrl}
            onPick={setCoverDataUrl}
            onClear={() => setCoverDataUrl(null)}
            pickLabel={t('myServer.coverPick')}
            clearLabel={t('myServer.coverClear')}
            tooLargeLabel={t('myServer.coverTooLarge')}
            onTooLarge={() => pushToast(t('myServer.coverTooLarge'), 'error')}
          />
          <p className="my-server-field-hint">{t('myServer.fieldCoverHint')}</p>
        </div>
        <div className="my-server-field">
          <span id="ms-pack-lbl" className="my-server-field-label">
            {t('myServer.fieldPack')}
          </span>
          <LauncherSelect
            aria-labelledby="ms-pack-lbl"
            value={packId}
            onChange={setPackId}
            options={packOptions}
          />
          {packId === MY_SERVER_VANILLA_PACK_ID ? (
            <div className="my-server-field my-server-field--tight-top">
              <span id="ms-vanilla-ver-lbl" className="my-server-field-label">
                {t('myServer.fieldVanillaVersion')}
              </span>
              <LauncherSelect
                aria-labelledby="ms-vanilla-ver-lbl"
                value={vanillaGameVersion}
                onChange={setVanillaGameVersion}
                options={vanillaVersionOpts}
                disabled={vanillaVersionOpts.length === 0}
              />
              <p className="my-server-field-hint">{t('myServer.fieldVanillaVersionHint')}</p>
              {vanillaReleases.kind === 'err' ? (
                <p className="my-server-field-hint my-server-muted">{t('myServer.vanillaVersionListFallbackHint')}</p>
              ) : null}
            </div>
          ) : null}
        </div>
        <div className="pack-confirm-actions my-server-modal-actions">
          <button type="button" className="btn-muted" onClick={onClose} disabled={busy}>
            {t('confirm.packCancel')}
          </button>
          <button type="button" className={`btn-save${mc}`} onClick={() => void submit()} disabled={busy}>
            {busy ? '…' : t('myServer.createSubmit')}
          </button>
        </div>
      </div>
    </div>
  )
}

function MyServerProfileModal({
  server,
  coverUrl,
  packOptions,
  vanillaReleases,
  chromeGlass,
  onClose,
  onRefresh,
  pushToast,
  t
}: {
  server: SoleServerListRowUi
  coverUrl: string | null
  packOptions: LauncherSelectEntry[]
  vanillaReleases: VanillaReleasesFetch
  chromeGlass: boolean
  onClose: () => void
  onRefresh: () => Promise<void>
  pushToast: (msg: string, variant?: 'info' | 'success' | 'error') => void
  t: (k: string, ...args: unknown[]) => string
}) {
  const mc = chromeGlass ? ' font-mc' : ''
  const modalRef = useRef<HTMLDivElement>(null)
  useFocusTrap(true, modalRef, { onEscape: onClose })
  const [name, setName] = useState(server.name)
  const [description, setDescription] = useState(server.description)
  const [coverOverride, setCoverOverride] = useState<string | 'remove' | undefined>(undefined)
  const [packId, setPackId] = useState(server.modpackId)
  const [vanillaGameVersion, setVanillaGameVersion] = useState(() => {
    const opts = buildVanillaVersionSelectOptions(
      { kind: 'loading' },
      server.modpackId === MY_SERVER_VANILLA_PACK_ID ? server.vanillaGameVersion : undefined
    )
    const want = (server.vanillaGameVersion ?? '').trim()
    if (server.modpackId === MY_SERVER_VANILLA_PACK_ID && want && opts.some((o) => o.value === want)) return want
    return opts[0]?.value ?? ''
  })
  const [packChangePending, setPackChangePending] = useState<string | null>(null)
  const persistMetaRef = useRef<(opts?: { silent?: boolean }) => Promise<void>>(async () => {})

  const vanillaVersionOpts = useMemo(
    () =>
      buildVanillaVersionSelectOptions(
        vanillaReleases,
        packId === MY_SERVER_VANILLA_PACK_ID ? server.vanillaGameVersion : undefined
      ),
    [vanillaReleases, packId, server.vanillaGameVersion]
  )

  const displayCoverPreview =
    coverOverride === 'remove' ? null : coverOverride ?? coverUrl

  useEffect(() => {
    setName(server.name)
    setDescription(server.description)
    setPackId(server.modpackId)
  }, [server.id, server.name, server.description, server.modpackId])

  useEffect(() => {
    if (server.modpackId !== MY_SERVER_VANILLA_PACK_ID) return
    const opts = buildVanillaVersionSelectOptions(vanillaReleases, server.vanillaGameVersion)
    const want = (server.vanillaGameVersion ?? '').trim()
    if (want && opts.some((o) => o.value === want)) {
      setVanillaGameVersion(want)
    } else {
      setVanillaGameVersion(opts[0]?.value ?? '')
    }
  }, [server.id, server.modpackId, server.vanillaGameVersion])

  useEffect(() => {
    setCoverOverride(undefined)
  }, [server.id])

  useEffect(() => {
    setPackChangePending(null)
  }, [server.id])

  const persistMeta = useCallback(
    async (opts?: { silent?: boolean }) => {
      if (server.installState === 'installing') return
      if (packId === MY_SERVER_VANILLA_PACK_ID) {
        const v = vanillaGameVersion.trim()
        if (!v || !vanillaReleaseMeetsMin18(v)) return
      }
      const payload: Parameters<typeof window.solea.soleaServerUpdate>[0] = {
        id: server.id,
        name,
        description,
        modpackId: packId,
        ...(packId === MY_SERVER_VANILLA_PACK_ID ? { vanillaGameVersion: vanillaGameVersion.trim() } : {})
      }
      if (coverOverride === 'remove') payload.coverImageDataUrl = null
      else if (coverOverride !== undefined && coverOverride !== 'remove') payload.coverImageDataUrl = coverOverride

      const r = await window.solea.soleaServerUpdate(payload)
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      if (coverOverride !== undefined) setCoverOverride(undefined)
      await onRefresh()
      if (!opts?.silent) pushToast(t('myServer.saved'), 'success')
    },
    [
      server.id,
      server.installState,
      name,
      description,
      packId,
      vanillaGameVersion,
      coverOverride,
      onRefresh,
      pushToast,
      t
    ]
  )
  persistMetaRef.current = persistMeta

  useEffect(() => {
    const dirty =
      name !== server.name ||
      description !== server.description ||
      packId !== server.modpackId ||
      (packId === MY_SERVER_VANILLA_PACK_ID &&
        vanillaGameVersion.trim() !== (server.vanillaGameVersion ?? '').trim())
    if (!dirty) return
    const id = window.setTimeout(() => {
      void persistMeta({ silent: true })
    }, AUTOSAVE_MS)
    return () => clearTimeout(id)
  }, [
    name,
    description,
    packId,
    vanillaGameVersion,
    server.name,
    server.description,
    server.modpackId,
    server.vanillaGameVersion,
    persistMeta
  ])

  useEffect(() => {
    if (coverOverride === undefined) return
    void persistMetaRef.current({ silent: true })
  }, [coverOverride])

  return (
    <div className="pack-confirm-backdrop my-server-modal-backdrop" role="presentation" onClick={onClose}>
      <div
        ref={modalRef}
        data-my-server-chrome={chromeGlass ? '1' : '0'}
        className="my-server-modal-profile pack-confirm-modal my-server-modal-aether solea-modal-surface"
        role="dialog"
        aria-modal="true"
        aria-labelledby="my-server-profile-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="my-server-modal-profile-head">
          <div>
            <p className={`my-server-modal-eyebrow${mc}`}>{t('myServer.createEyebrow')}</p>
            <h2 id="my-server-profile-title" className={`my-server-modal-title${mc}`}>
              {t('myServer.profileModalTitle')}
            </h2>
            <p className="my-server-modal-sub my-server-muted">{t('myServer.profileModalHint')}</p>
          </div>
          <button
            type="button"
            className="btn-muted my-server-modal-profile-close"
            onClick={onClose}
            aria-label={t('myServer.profileModalClose')}
          >
            ×
          </button>
        </div>
        <div className="my-server-modal-profile-scroll">
          {server.installState === 'installing' ? (
            <p className="my-server-muted">{t('myServer.installingHint')}</p>
          ) : null}
          {server.installState === 'error' && server.installError ? (
            <p role="alert" className="my-server-muted" style={{ color: '#f88' }}>
              {server.installError}
            </p>
          ) : null}
          <div className="my-server-field">
            <label htmlFor="msp-name">{t('myServer.fieldName')}</label>
            <input
              id="msp-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={server.installState === 'installing'}
            />
          </div>
          <div className="my-server-field my-server-field--compact">
            <label htmlFor="msp-desc">{t('myServer.fieldDesc')}</label>
            <textarea
              id="msp-desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={server.installState === 'installing'}
            />
          </div>
          <div className="my-server-field">
            <span className="my-server-field-label">{t('myServer.fieldCover')}</span>
            <CoverImageField
              inputId="msp-cover"
              previewUrl={displayCoverPreview}
              onPick={(url) => setCoverOverride(url)}
              onClear={() => setCoverOverride('remove')}
              pickLabel={t('myServer.coverPick')}
              clearLabel={t('myServer.coverClear')}
              tooLargeLabel={t('myServer.coverTooLarge')}
              onTooLarge={() => pushToast(t('myServer.coverTooLarge'), 'error')}
            />
            <p className="my-server-field-hint">{t('myServer.fieldCoverHint')}</p>
          </div>
          <div className="my-server-field">
            <span id="msp-pack" className="my-server-field-label">
              {t('myServer.fieldPack')}
            </span>
            <LauncherSelect
              aria-labelledby="msp-pack"
              value={packId}
              onChange={(next) => {
                if (next === server.modpackId) {
                  setPackId(next)
                  return
                }
                setPackChangePending(next)
              }}
              options={packOptions}
              disabled={server.installState === 'installing'}
            />
            <p className="my-server-muted">{t('myServer.changePackHint')}</p>
            {packId === MY_SERVER_VANILLA_PACK_ID ? (
              <div className="my-server-field my-server-field--tight-top">
                <span id="msp-vanilla-ver-lbl" className="my-server-field-label">
                  {t('myServer.fieldVanillaVersion')}
                </span>
                <LauncherSelect
                  aria-labelledby="msp-vanilla-ver-lbl"
                  value={vanillaGameVersion}
                  onChange={setVanillaGameVersion}
                  options={vanillaVersionOpts}
                  disabled={server.installState === 'installing' || vanillaVersionOpts.length === 0}
                />
                <p className="my-server-field-hint">{t('myServer.fieldVanillaVersionHint')}</p>
                {vanillaReleases.kind === 'err' ? (
                  <p className="my-server-field-hint my-server-muted">{t('myServer.vanillaVersionListFallbackHint')}</p>
                ) : null}
              </div>
            ) : null}
          </div>
        </div>
        <div className="pack-confirm-actions my-server-modal-actions">
          <button type="button" className="btn-muted" onClick={onClose}>
            {t('myServer.profileModalClose')}
          </button>
        </div>
      </div>
      {packChangePending ? (
        <PackChangeConfirmModal
          stacked
          packLabel={
            flatSelectOptions(packOptions).find((p) => p.value === packChangePending)?.label ??
            packChangePending
          }
          onCancel={() => setPackChangePending(null)}
          onConfirm={() => {
            const next = packChangePending
            if (!next) return
            setPackId(next)
            setPackChangePending(null)
            if (next === MY_SERVER_VANILLA_PACK_ID) {
              setVanillaGameVersion(vanillaIdsForSelect(vanillaReleases)[0] ?? '')
            }
          }}
          t={t}
          chromeGlass={chromeGlass}
        />
      ) : null}
    </div>
  )
}

type MyServerDetailTab = 'console' | 'world' | 'properties' | 'java' | 'modpack'

function MyServerDetail({
  server,
  memoryGiB,
  chromeGlass,
  onBack,
  onRefresh,
  onDeleteRequest,
  pushToast,
  t
}: {
  server: SoleServerListRowUi
  memoryGiB: number
  chromeGlass: boolean
  onBack: () => void
  onRefresh: () => Promise<void>
  onDeleteRequest: () => void
  pushToast: (msg: string, variant?: 'info' | 'success' | 'error') => void
  t: (k: string, ...args: unknown[]) => string
}) {
  const mc = chromeGlass ? ' font-mc' : ''
  const [ramMiB, setRamMiB] = useState(server.ramMiB)
  const [lines, setLines] = useState<string[]>([])
  const [cmd, setCmd] = useState('')
  const [ipHidden, setIpHidden] = useState(true)
  const [publicIp, setPublicIp] = useState<string | null>(null)
  const [updateInfo, setUpdateInfo] = useState<{
    hasUpdate: boolean
    latest?: string
    current?: string
  } | null>(null)
  const [worldLevelName, setWorldLevelName] = useState('world')
  const [worldSeed, setWorldSeed] = useState('')
  const [worldType, setWorldType] = useState('minecraft:normal')
  const [propsSourceRaw, setPropsSourceRaw] = useState('')
  const [propsForm, setPropsForm] = useState<ServerPropsForm>(() => defaultFormFromMap(new Map()))
  const [detailTab, setDetailTab] = useState<MyServerDetailTab>('console')
  const [jvmExtra, setJvmExtra] = useState('')
  const consoleRef = useRef<HTMLDivElement>(null)

  const maxRam = Math.min(64 * 1024, Math.max(1024, Math.floor(memoryGiB * 1024 * 0.92)))

  useEffect(() => {
    setRamMiB(server.ramMiB)
  }, [server.ramMiB])

  useEffect(() => {
    setDetailTab('console')
  }, [server.id])

  const pullConsole = useCallback(async () => {
    const l = await window.solea.soleaServerConsole(server.id)
    setLines(l)
  }, [server.id])

  const loadWorldFromServer = useCallback(async () => {
    if (server.installState !== 'ready') return
    const r = await window.solea.soleaServerWorldGet(server.id)
    if (!r.ok) return
    setWorldLevelName(r.levelName)
    setWorldSeed(r.levelSeed)
    setWorldType(r.levelType)
  }, [server.id, server.installState])

  useEffect(() => {
    void loadWorldFromServer()
  }, [loadWorldFromServer])

  useEffect(() => {
    void pullConsole()
  }, [pullConsole])

  useEffect(() => {
    const id = window.setInterval(() => void pullConsole(), 1200)
    return () => clearInterval(id)
  }, [pullConsole])

  useEffect(() => {
    const el = consoleRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [lines])

  const loadPropsRaw = useCallback(async () => {
    if (server.installState !== 'ready') {
      setPropsSourceRaw('')
      setPropsForm(defaultFormFromMap(new Map()))
      return
    }
    const r = await window.solea.soleaServerPropertiesRawGet(server.id)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    const raw = r.content
    setPropsSourceRaw(raw)
    setPropsForm(defaultFormFromMap(parseServerProperties(raw)))
  }, [server.id, server.installState, pushToast])

  useEffect(() => {
    void loadPropsRaw()
  }, [loadPropsRaw])

  const loadJvmExtra = useCallback(async () => {
    if (server.installState !== 'ready') {
      setJvmExtra('')
      return
    }
    const r = await window.solea.soleaServerJvmExtraGet(server.id)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    setJvmExtra(r.content)
  }, [server.id, server.installState, pushToast])

  useEffect(() => {
    void loadJvmExtra()
  }, [loadJvmExtra])

  const persistRam = useCallback(
    async (opts?: { silent?: boolean }) => {
      if (server.installState === 'installing') return
      if (ramMiB === server.ramMiB) return
      const r = await window.solea.soleaServerUpdate({ id: server.id, ramMiB })
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      await onRefresh()
      if (!opts?.silent) pushToast(t('myServer.saved'), 'success')
    },
    [server.id, server.installState, server.ramMiB, ramMiB, onRefresh, pushToast, t]
  )

  useEffect(() => {
    if (ramMiB === server.ramMiB) return
    const id = window.setTimeout(() => {
      void persistRam({ silent: true })
    }, AUTOSAVE_MS)
    return () => clearTimeout(id)
  }, [ramMiB, server.ramMiB, persistRam])

  const saveServerProperties = async () => {
    const invalid = validatePropsForm(propsForm)
    if (invalid) {
      pushToast(t(PROPS_ERR_I18N[invalid]), 'error')
      return
    }
    let merged = mergeManagedKeysIntoRaw(propsSourceRaw, formToUpdates(propsForm))
    if (!parseServerProperties(merged).has('level-name')) {
      const ln = worldLevelName.trim()
      const safe = /^[a-zA-Z0-9_-]{1,64}$/.test(ln) ? ln : 'world'
      const boot: Record<string, string> = { 'level-name': safe, 'level-type': worldType }
      if (worldSeed.trim()) boot['level-seed'] = worldSeed.trim()
      merged = mergeManagedKeysIntoRaw(merged, boot)
    }
    const r = await window.solea.soleaServerPropertiesRawSet({ id: server.id, content: merged })
    if (!r.ok) pushToast(r.error, 'error')
    else {
      pushToast(t('myServer.propsSaved'), 'success')
      await onRefresh()
      void loadPropsRaw()
    }
  }

  const saveJvmExtra = async () => {
    const r = await window.solea.soleaServerJvmExtraSet({ id: server.id, content: jvmExtra })
    if (!r.ok) pushToast(r.error, 'error')
    else {
      pushToast(t('myServer.jvmExtraSaved'), 'success')
      void loadJvmExtra()
    }
  }

  const checkUpdate = async () => {
    const r = await window.solea.soleaServerCheckModpackUpdate(server.id)
    if (!r.ok) {
      pushToast(r.error, 'error')
      return
    }
    setUpdateInfo({
      hasUpdate: r.hasUpdate,
      latest: r.latestVersion,
      current: r.currentVersion
    })
    if (r.hasUpdate) pushToast(t('myServer.updateAvailable', { v: r.latestVersion ?? '' }), 'info')
    else pushToast(t('myServer.updateNone'), 'success')
  }

  const reinstall = async () => {
    const r = await window.solea.soleaServerReinstallLatest(server.id)
    if (r.ok) {
      pushToast(t('myServer.reinstallStarted'), 'info')
      void onRefresh()
    } else pushToast(r.error, 'error')
  }

  const showIp = async () => {
    if (!publicIp) {
      const r = await window.solea.soleaServerGetPublicIp()
      if (r.ok) setPublicIp(r.ip)
      else {
        pushToast(r.error, 'error')
        return
      }
    }
    setIpHidden(false)
  }

  const probePort = async () => {
    const r = await window.solea.soleaServerProbeLocalPort(server.port)
    if (r.ok) {
      if (r.free) pushToast(t('myServer.portFree', { port: server.port }), 'info')
      else if (r.inUse) pushToast(t('myServer.portInUse', { port: server.port }), 'info')
      else pushToast(t('myServer.portUnknown'), 'info')
    }
  }

  const start = async () => {
    const r = await window.solea.soleaServerStart(server.id)
    if (r.ok) pushToast(t('myServer.started'), 'success')
    else pushToast(r.error, 'error')
    void onRefresh()
  }
  const stop = async () => {
    const r = await window.solea.soleaServerStop(server.id)
    if (r.ok) pushToast(t('myServer.stopped'), 'info')
    else pushToast(r.error, 'error')
    void onRefresh()
  }
  const kill = async () => {
    const r = await window.solea.soleaServerKill(server.id)
    if (r.ok) pushToast(t('myServer.killed'), 'info')
    else pushToast(r.error, 'error')
    void onRefresh()
  }
  const restart = async () => {
    const r = await window.solea.soleaServerRestart(server.id)
    if (r.ok) pushToast(t('myServer.restarted'), 'info')
    else pushToast(r.error, 'error')
    void onRefresh()
  }

  const sendCmd = async () => {
    const line = cmd.trim()
    if (!line) return
    const r = await window.solea.soleaServerSendCommand(server.id, line)
    setCmd('')
    if (!r.ok) pushToast(r.error, 'error')
    void pullConsole()
  }

  const copyConsoleLogs = async () => {
    if (!lines.length) return
    try {
      await navigator.clipboard.writeText(lines.join('\n'))
      pushToast(t('myServer.consoleCopied'), 'success')
    } catch {
      pushToast(t('myServer.copyFailed'), 'error')
    }
  }

  const worldTypeOptions = useMemo(
    () => WORLD_LEVEL_TYPES.map((o) => ({ value: o.value, label: t(o.labelKey) })),
    [t]
  )

  const propsStopped = server.serverState === 'stopped'
  const propsDifficultyOptions = useMemo(
    () =>
      (['peaceful', 'easy', 'normal', 'hard'] as const).map((v) => ({
        value: v,
        label: t(`myServer.propsDifficulty.${v}`)
      })),
    [t]
  )
  const propsGamemodeOptions = useMemo(
    () =>
      (['survival', 'creative', 'adventure', 'spectator'] as const).map((v) => ({
        value: v,
        label: t(`myServer.propsGamemode.${v}`)
      })),
    [t]
  )

  const applyWorldSettings = async () => {
    const r = await window.solea.soleaServerWorldSet({
      id: server.id,
      levelName: worldLevelName,
      levelSeed: worldSeed,
      levelType: worldType
    })
    if (!r.ok) pushToast(r.error, 'error')
    else {
      pushToast(t('myServer.worldSaved'), 'success')
      void loadWorldFromServer()
      void loadPropsRaw()
    }
  }

  const eraseWorldFolder = async () => {
    if (!window.confirm(t('myServer.worldEraseConfirm'))) return
    const r = await window.solea.soleaServerWorldDelete(server.id)
    if (!r.ok) pushToast(r.error, 'error')
    else {
      pushToast(t('myServer.worldErased'), 'success')
      void pullConsole()
    }
  }

  /** Ligne « Connexion directe » Minecraft : ip:port */
  const minecraftDirect = !ipHidden && publicIp ? `${publicIp}:${server.port}` : null

  const copyMinecraftAddress = async () => {
    if (!minecraftDirect) return
    try {
      await navigator.clipboard.writeText(minecraftDirect)
      pushToast(t('myServer.joinCopied'), 'success')
    } catch {
      pushToast(t('myServer.copyFailed'), 'error')
    }
  }

  const installReady = server.installState === 'ready'
  const isRunning = server.serverState === 'running'
  const isStarting = server.serverState === 'starting'
  const isStopping = server.serverState === 'stopping'
  const canStopOrKill = isRunning || isStarting

  const openServerFolder = async () => {
    const r = await window.solea.soleaServerOpenFolder(server.id)
    if (!r.ok) pushToast(r.error, 'error')
  }

  return (
    <div className="my-server-layout my-server-layout--detail" data-my-server-chrome={chromeGlass ? '1' : '0'}>
      <div className="my-server-scroll my-server-scroll--detail">
        <div className="my-server-detail my-server-detail--dashboard my-server-detail--v2">
          <div className="my-server-detail-head">
            <button type="button" className="btn-muted my-server-back" onClick={onBack}>
              ← {t('myServer.back')}
            </button>
            <div className="my-server-detail-title-block">
              <div className="my-server-detail-title-row">
                <h2 className={chromeGlass ? 'font-mc' : undefined}>{server.name}</h2>
                <span className="my-server-beta-badge" title={t('myServer.betaBadgeTitle')}>
                  {t('myServer.betaBadge')}
                </span>
              </div>
            </div>
          </div>

          <div className="my-server-dash-toolbar">
            <div className="my-server-toolbar-leading">
              <button type="button" className="btn-muted" onClick={() => void openServerFolder()}>
                {t('myServer.openFolder')}
              </button>
              <button type="button" className="btn-danger-outline" onClick={onDeleteRequest}>
                {t('myServer.delete')}
              </button>
            </div>
            <div className="my-server-toolbar-process">
            <span className="my-server-port-badge" title={t('myServer.portFixedHint', { port: server.port })}>
              {t('myServer.portLabel', { port: server.port })}
            </span>
            {server.serverState === 'stopped' ? (
              <button
                type="button"
                className={`btn-save${mc}`}
                disabled={!installReady}
                onClick={() => void start()}
              >
                {t('myServer.btnStart')}
              </button>
            ) : null}
            {isStopping ? (
              <p className="my-server-process-status" role="status">
                {t('myServer.stateStopping')}
              </p>
            ) : null}
            {canStopOrKill ? (
              <div
                className="my-server-segmented my-server-segmented--process"
                role="group"
                aria-label={t('myServer.processGroupAria')}
              >
                <button type="button" className="my-server-segmented__btn" onClick={() => void stop()}>
                  {t('myServer.btnStop')}
                </button>
                <button
                  type="button"
                  className="my-server-segmented__btn my-server-segmented__btn--danger"
                  onClick={() => void kill()}
                >
                  {t('myServer.btnKill')}
                </button>
              </div>
            ) : null}
            {isRunning && installReady ? (
              <button type="button" className="btn-muted my-server-btn-restart" onClick={() => void restart()}>
                {t('myServer.btnRestart')}
              </button>
            ) : null}
            </div>
          </div>

          <div className="my-server-v2-body">
            {server.installState === 'installing' ? (
              <p className="my-server-v2-banner my-server-muted">{t('myServer.installingHint')}</p>
            ) : null}
            {server.installState === 'error' && server.installError ? (
              <p role="alert" className="my-server-v2-banner my-server-muted" style={{ color: '#f88' }}>
                {server.installError}
              </p>
            ) : null}

            <div className="settings-layout my-server-detail-tabs-layout">
              <nav className="settings-nav my-server-detail-nav" aria-label={t('myServer.detailNavAria')}>
                <div className="settings-nav-top">
                  <p className="settings-nav-section-label">{t('myServer.detailNavSection')}</p>
                  <button
                    type="button"
                    className={`nav-item${detailTab === 'console' ? ' on' : ''}`}
                    onClick={() => setDetailTab('console')}
                  >
                    {t('myServer.navConsole')}
                  </button>
                  <button
                    type="button"
                    className={`nav-item${detailTab === 'world' ? ' on' : ''}`}
                    onClick={() => setDetailTab('world')}
                  >
                    {t('myServer.navWorld')}
                  </button>
                  <button
                    type="button"
                    className={`nav-item${detailTab === 'properties' ? ' on' : ''}`}
                    onClick={() => setDetailTab('properties')}
                  >
                    {t('myServer.navProperties')}
                  </button>
                  <button
                    type="button"
                    className={`nav-item${detailTab === 'java' ? ' on' : ''}`}
                    onClick={() => setDetailTab('java')}
                  >
                    {t('myServer.navJava')}
                  </button>
                  <button
                    type="button"
                    className={`nav-item${detailTab === 'modpack' ? ' on' : ''}`}
                    onClick={() => setDetailTab('modpack')}
                  >
                    {t('myServer.navModpack')}
                  </button>
                </div>
              </nav>
              <div className="settings-body my-server-detail-tab-body">
                <div className="settings-body-inner my-server-detail-tab-inner">
                  <div
                    className={`settings-body-scroll my-server-detail-tab-scroll${
                      detailTab === 'console' ? ' my-server-detail-tab-scroll--console' : ''
                    }`}
                  >
                    {detailTab === 'console' ? (
                      <div className="my-server-tab-console">
                        <section className="my-server-detail-section" aria-labelledby="my-server-sec-join">
                          <div className="my-server-detail-section-head">
                            <span className="my-server-detail-section-accent" aria-hidden />
                            <h3 id="my-server-sec-join" className={chromeGlass ? 'font-mc' : undefined}>
                              {t('myServer.sectionJoin')}
                            </h3>
                          </div>
                          <div className="my-server-detail-section-body">
                            <div className="my-server-join-copy-row">
                              {minecraftDirect ? (
                                <>
                                  <div className="my-server-join-code-wrap">
                                    <code className="my-server-join-code">{minecraftDirect}</code>
                                  </div>
                                  <button type="button" className="btn-muted" onClick={() => void copyMinecraftAddress()}>
                                    {t('myServer.copyJoin')}
                                  </button>
                                </>
                              ) : (
                                <code className="my-server-ip-masked my-server-join-code">{t('myServer.ipMasked')}</code>
                              )}
                              {ipHidden ? (
                                <button type="button" className="btn-muted" onClick={() => void showIp()}>
                                  {t('myServer.revealIp')}
                                </button>
                              ) : (
                                <button type="button" className="btn-muted" onClick={() => setIpHidden(true)}>
                                  {t('myServer.hideIp')}
                                </button>
                              )}
                              <button type="button" className="btn-muted" onClick={() => void probePort()}>
                                {t('myServer.checkPort')}
                              </button>
                            </div>
                            <p className="my-server-muted">{t('myServer.joinHint')}</p>
                          </div>
                        </section>
                        <section
                          className="my-server-detail-section my-server-detail-section--console"
                          aria-labelledby="my-server-sec-console"
                        >
                          <div className="my-server-detail-section-head">
                            <span className="my-server-detail-section-accent" aria-hidden />
                            <h3 id="my-server-sec-console" className={chromeGlass ? 'font-mc' : undefined}>
                              {t('myServer.sectionConsole')}
                            </h3>
                          </div>
                          <div className="my-server-detail-section-body my-server-detail-section-body--console">
                            <div className="my-server-console-wrap">
                              <div className="my-server-console-toolbar">
                                <button
                                  type="button"
                                  className="btn-muted my-server-console-toolbar-btn"
                                  disabled={!lines.length}
                                  onClick={() => void copyConsoleLogs()}
                                >
                                  {t('myServer.consoleCopy')}
                                </button>
                              </div>
                              <div ref={consoleRef} className="my-server-console" tabIndex={0} role="log">
                                {lines.length ? (
                                  lines.map((ln, idx) => (
                                    <div key={idx} className={serverConsoleLineClass(ln)}>
                                      {ln}
                                    </div>
                                  ))
                                ) : (
                                  <div className="my-server-console-line my-server-console-line--muted">—</div>
                                )}
                              </div>
                              <div className="my-server-console-input-row">
                                <input
                                  type="text"
                                  value={cmd}
                                  onChange={(e) => setCmd(e.target.value)}
                                  placeholder={t('myServer.consolePlaceholder')}
                                  onKeyDown={(e) => {
                                    if (e.key === 'Enter') void sendCmd()
                                  }}
                                />
                                <button type="button" className="btn-muted" onClick={() => void sendCmd()}>
                                  {t('myServer.consoleSend')}
                                </button>
                              </div>
                            </div>
                          </div>
                        </section>
                      </div>
                    ) : null}

                    {detailTab === 'world' ? (
                      <section className="my-server-detail-section" aria-labelledby="my-server-sec-world">
                        <div className="my-server-detail-section-head">
                          <span className="my-server-detail-section-accent" aria-hidden />
                          <h3 id="my-server-sec-world" className={chromeGlass ? 'font-mc' : undefined}>
                            {t('myServer.worldSection')}
                          </h3>
                        </div>
                        <div className="my-server-detail-section-body">
                          {server.installState === 'ready' ? (
                            <>
                              <div className="my-server-field">
                                <label htmlFor="msd-wname">{t('myServer.worldName')}</label>
                                <input
                                  id="msd-wname"
                                  type="text"
                                  value={worldLevelName}
                                  onChange={(e) => setWorldLevelName(e.target.value)}
                                  disabled={server.serverState !== 'stopped'}
                                  autoComplete="off"
                                />
                              </div>
                              <div className="my-server-field">
                                <label htmlFor="msd-wseed">{t('myServer.worldSeed')}</label>
                                <input
                                  id="msd-wseed"
                                  type="text"
                                  value={worldSeed}
                                  onChange={(e) => setWorldSeed(e.target.value)}
                                  disabled={server.serverState !== 'stopped'}
                                  autoComplete="off"
                                />
                              </div>
                              <div className="my-server-field">
                                <span id="msd-wtype" className="my-server-field-label">
                                  {t('myServer.worldType')}
                                </span>
                                <LauncherSelect
                                  aria-labelledby="msd-wtype"
                                  value={worldType}
                                  onChange={setWorldType}
                                  options={worldTypeOptions}
                                  disabled={server.serverState !== 'stopped'}
                                />
                              </div>
                              <div className="my-server-world-actions">
                                <button
                                  type="button"
                                  className="btn-muted"
                                  disabled={server.serverState !== 'stopped'}
                                  onClick={() => void applyWorldSettings()}
                                >
                                  {t('myServer.worldApply')}
                                </button>
                                <button
                                  type="button"
                                  className="btn-danger-outline"
                                  disabled={server.serverState !== 'stopped'}
                                  onClick={() => void eraseWorldFolder()}
                                >
                                  {t('myServer.worldErase')}
                                </button>
                              </div>
                              <p className="my-server-field-hint">{t('myServer.worldHint')}</p>
                            </>
                          ) : (
                            <p className="my-server-muted">{t('myServer.worldWhenReady')}</p>
                          )}
                        </div>
                      </section>
                    ) : null}

                    {detailTab === 'properties' ? (
                      <section className="my-server-detail-section" aria-labelledby="my-server-sec-props">
                        <div className="my-server-detail-section-head">
                          <span className="my-server-detail-section-accent" aria-hidden />
                          <h3 id="my-server-sec-props" className={chromeGlass ? 'font-mc' : undefined}>
                            {t('myServer.sectionServerProperties')}
                          </h3>
                        </div>
                        <div className="my-server-detail-section-body">
                          <p className="my-server-field-hint">{t('myServer.propsHint')}</p>
                          {installReady ? (
                            <div className="my-server-props-form">
                              <p className="my-server-props-group-label">{t('myServer.propsGroupNetwork')}</p>
                              <div className="my-server-props-grid-nums">
                                <div className="my-server-field">
                                  <label htmlFor="msd-p-port">{t('myServer.propPort')}</label>
                                  <input
                                    id="msd-p-port"
                                    type="number"
                                    min={1024}
                                    max={65535}
                                    value={propsForm.serverPort}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, serverPort: e.target.value }))}
                                    disabled={!propsStopped}
                                  />
                                </div>
                                <div className="my-server-field">
                                  <label htmlFor="msd-p-maxp">{t('myServer.propMaxPlayers')}</label>
                                  <input
                                    id="msd-p-maxp"
                                    type="number"
                                    min={1}
                                    max={999}
                                    value={propsForm.maxPlayers}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, maxPlayers: e.target.value }))}
                                    disabled={!propsStopped}
                                  />
                                </div>
                                <div className="my-server-field">
                                  <label htmlFor="msd-p-vd">{t('myServer.propViewDistance')}</label>
                                  <input
                                    id="msd-p-vd"
                                    type="number"
                                    min={2}
                                    max={32}
                                    value={propsForm.viewDistance}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, viewDistance: e.target.value }))}
                                    disabled={!propsStopped}
                                  />
                                </div>
                                <div className="my-server-field">
                                  <label htmlFor="msd-p-sd">{t('myServer.propSimulationDistance')}</label>
                                  <input
                                    id="msd-p-sd"
                                    type="number"
                                    min={3}
                                    max={32}
                                    value={propsForm.simulationDistance}
                                    onChange={(e) =>
                                      setPropsForm((p) => ({ ...p, simulationDistance: e.target.value }))
                                    }
                                    disabled={!propsStopped}
                                  />
                                </div>
                                <div className="my-server-field">
                                  <label htmlFor="msd-p-sp">{t('myServer.propSpawnProtection')}</label>
                                  <input
                                    id="msd-p-sp"
                                    type="number"
                                    min={0}
                                    max={512}
                                    value={propsForm.spawnProtection}
                                    onChange={(e) =>
                                      setPropsForm((p) => ({ ...p, spawnProtection: e.target.value }))
                                    }
                                    disabled={!propsStopped}
                                  />
                                </div>
                              </div>
                              <div className="my-server-field">
                                <label htmlFor="msd-p-motd">{t('myServer.propMotd')}</label>
                                <input
                                  id="msd-p-motd"
                                  type="text"
                                  value={propsForm.motdPlain}
                                  onChange={(e) => setPropsForm((p) => ({ ...p, motdPlain: e.target.value }))}
                                  disabled={!propsStopped}
                                  autoComplete="off"
                                />
                                <p className="my-server-field-hint">{t('myServer.propMotdHint')}</p>
                              </div>
                              <p className="my-server-props-group-label">{t('myServer.propsGroupWorldRules')}</p>
                              <div className="my-server-props-grid-nums">
                                <div className="my-server-field">
                                  <span id="msd-p-diff" className="my-server-field-label">
                                    {t('myServer.propDifficulty')}
                                  </span>
                                  <LauncherSelect
                                    aria-labelledby="msd-p-diff"
                                    value={propsForm.difficulty}
                                    onChange={(v) =>
                                      setPropsForm((p) => ({
                                        ...p,
                                        difficulty: v as ServerPropsForm['difficulty']
                                      }))
                                    }
                                    options={propsDifficultyOptions}
                                    disabled={!propsStopped}
                                  />
                                </div>
                                <div className="my-server-field">
                                  <span id="msd-p-gm" className="my-server-field-label">
                                    {t('myServer.propGamemode')}
                                  </span>
                                  <LauncherSelect
                                    aria-labelledby="msd-p-gm"
                                    value={propsForm.gamemode}
                                    onChange={(v) =>
                                      setPropsForm((p) => ({ ...p, gamemode: v as ServerPropsForm['gamemode'] }))
                                    }
                                    options={propsGamemodeOptions}
                                    disabled={!propsStopped}
                                  />
                                </div>
                              </div>
                              <p className="my-server-props-group-label">{t('myServer.propsGroupBools')}</p>
                              <div className="my-server-props-check-grid">
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.onlineMode}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, onlineMode: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propOnlineMode')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.pvp}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, pvp: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propPvp')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.allowFlight}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, allowFlight: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propAllowFlight')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.whiteList}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, whiteList: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propWhiteList')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.enforceWhitelist}
                                    onChange={(e) =>
                                      setPropsForm((p) => ({ ...p, enforceWhitelist: e.target.checked }))
                                    }
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propEnforceWhitelist')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.enableCommandBlock}
                                    onChange={(e) =>
                                      setPropsForm((p) => ({ ...p, enableCommandBlock: e.target.checked }))
                                    }
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propCommandBlock')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.spawnMonsters}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, spawnMonsters: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propSpawnMonsters')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.spawnNpcs}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, spawnNpcs: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propSpawnNpcs')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.spawnAnimals}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, spawnAnimals: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propSpawnAnimals')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.forceGamemode}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, forceGamemode: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propForceGamemode')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.hardcore}
                                    onChange={(e) => setPropsForm((p) => ({ ...p, hardcore: e.target.checked }))}
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propHardcore')}</span>
                                </label>
                                <label className="my-server-props-check">
                                  <input
                                    type="checkbox"
                                    checked={propsForm.enforceSecureProfile}
                                    onChange={(e) =>
                                      setPropsForm((p) => ({ ...p, enforceSecureProfile: e.target.checked }))
                                    }
                                    disabled={!propsStopped}
                                  />
                                  <span>{t('myServer.propEnforceSecureProfile')}</span>
                                </label>
                              </div>
                            </div>
                          ) : (
                            <p className="my-server-muted">{t('myServer.worldWhenReady')}</p>
                          )}
                          <div className="my-server-props-actions my-server-props-actions--stacked">
                            <button
                              type="button"
                              className="btn-muted my-server-props-reload"
                              disabled={!installReady}
                              onClick={() => void loadPropsRaw()}
                            >
                              {t('myServer.propsReload')}
                            </button>
                            <button
                              type="button"
                              className={`btn-save${mc} my-server-props-save-btn`}
                              disabled={!installReady || !propsStopped}
                              onClick={() => void saveServerProperties()}
                            >
                              {t('myServer.propsSave')}
                            </button>
                          </div>
                        </div>
                      </section>
                    ) : null}

                    {detailTab === 'java' ? (
                      <section className="my-server-detail-section" aria-labelledby="my-server-sec-java">
                        <div className="my-server-detail-section-head">
                          <span className="my-server-detail-section-accent" aria-hidden />
                          <h3 id="my-server-sec-java" className={chromeGlass ? 'font-mc' : undefined}>
                            {t('myServer.sectionJavaRam')}
                          </h3>
                        </div>
                        <div className="my-server-detail-section-body">
                          <div className="my-server-field">
                            <label htmlFor="msd-ram">{t('myServer.fieldRam', { max: Math.round(maxRam / 1024) })}</label>
                            <div className="my-server-ram-row">
                              <input
                                id="msd-ram"
                                type="range"
                                min={1024}
                                max={maxRam}
                                step={256}
                                value={Math.min(ramMiB, maxRam)}
                                onChange={(e) => setRamMiB(Number(e.target.value))}
                                disabled={server.installState === 'installing'}
                              />
                              <span className="my-server-ram-value">{Math.round(ramMiB / 1024)} Go</span>
                            </div>
                            <p className="my-server-field-hint">{t('myServer.ramAutosaveHint')}</p>
                          </div>
                          <p className="my-server-props-group-label">{t('myServer.jvmExtraGroup')}</p>
                          <p className="my-server-field-hint">{t('myServer.jvmExtraHint')}</p>
                          <textarea
                            id="msd-jvm-extra"
                            className="my-server-jvm-extra-editor"
                            spellCheck={false}
                            value={jvmExtra}
                            onChange={(e) => setJvmExtra(e.target.value)}
                            disabled={!installReady || !propsStopped}
                            rows={8}
                            aria-label={t('myServer.jvmExtraLabel')}
                          />
                          <div className="my-server-props-actions my-server-props-actions--stacked">
                            <button
                              type="button"
                              className="btn-muted my-server-props-reload"
                              disabled={!installReady}
                              onClick={() => void loadJvmExtra()}
                            >
                              {t('myServer.jvmExtraReload')}
                            </button>
                            <button
                              type="button"
                              className={`btn-save${mc} my-server-props-save-btn`}
                              disabled={!installReady || !propsStopped}
                              onClick={() => void saveJvmExtra()}
                            >
                              {t('myServer.jvmExtraSave')}
                            </button>
                          </div>
                        </div>
                      </section>
                    ) : null}

                    {detailTab === 'modpack' ? (
                      <section className="my-server-detail-section" aria-labelledby="my-server-sec-pack">
                        <div className="my-server-detail-section-head">
                          <span className="my-server-detail-section-accent" aria-hidden />
                          <h3 id="my-server-sec-pack" className={chromeGlass ? 'font-mc' : undefined}>
                            {t('myServer.sectionPackTools')}
                          </h3>
                        </div>
                        <div className="my-server-detail-section-body">
                          {server.modpackId === MY_SERVER_VANILLA_PACK_ID ? (
                            <p className="my-server-muted">{t('myServer.vanillaPackToolsHint')}</p>
                          ) : null}
                          <div className="actions-bar my-server-actions-row">
                            <button
                              type="button"
                              className="btn-muted"
                              onClick={() => void checkUpdate()}
                              disabled={
                                server.installState !== 'ready' || server.modpackId === MY_SERVER_VANILLA_PACK_ID
                              }
                            >
                              {t('myServer.checkUpdate')}
                            </button>
                            <button
                              type="button"
                              className="btn-muted"
                              onClick={() => void reinstall()}
                              disabled={server.installState === 'installing'}
                            >
                              {t('myServer.reinstallPack')}
                            </button>
                          </div>
                          {updateInfo ? (
                            <p className="my-server-muted">
                              {updateInfo.hasUpdate
                                ? t('myServer.updateLine', {
                                    cur: updateInfo.current ?? '?',
                                    latest: updateInfo.latest ?? '?'
                                  })
                                : t('myServer.updateLineOk', { cur: updateInfo.current ?? '?' })}
                            </p>
                          ) : null}
                          <p className="my-server-muted my-server-control-hint">{t('myServer.controlHint')}</p>
                        </div>
                      </section>
                    ) : null}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
