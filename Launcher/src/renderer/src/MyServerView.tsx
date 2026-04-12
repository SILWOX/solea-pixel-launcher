/** MY SERVER — hébergement local (UI). */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { isModpackId } from './modpackTheme'
import { useI18n } from './i18n/I18nContext'
import { useToast } from './ui/ToastContext'
import { LauncherSelect } from './ui/LauncherSelect'
import { useFocusTrap } from './a11y/useFocusTrap'
import './my-server.css'

const AUTOSAVE_MS = 850
const MAX_COVER_BYTES = 12 * 1024 * 1024

type SoleServerListRowUi = {
  id: string
  name: string
  description: string
  modpackId: string
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
  const packOptions = useMemo(
    () => modpacksList.filter((m) => isModpackId(m.id)).map((m) => ({ value: m.id, label: m.displayName })),
    [modpacksList]
  )

  const [servers, setServers] = useState<SoleServerListRowUi[]>([])
  const [covers, setCovers] = useState<Record<string, string | null>>({})
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null)
  const [memoryGiB, setMemoryGiB] = useState(16)

  const refreshList = useCallback(async () => {
    const list = await window.solea.soleaServerList()
    setServers(list)
  }, [])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

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
          coverUrl={covers[selected.id] ?? null}
          packOptions={packOptions}
          memoryGiB={memoryGiB}
          chromeGlass={chromeGlass}
          onBack={() => setSelectedId(null)}
          onRefresh={refreshList}
          onDeleteRequest={() => setDeleteConfirmId(selected.id)}
          pushToast={pushToast}
          t={t}
        />
      ) : (
      <div className="my-server-layout" data-my-server-chrome={chromeGlass ? '1' : '0'}>
        <div className="my-server-scroll">
          <section className="my-server-hero my-server-hero--aether" aria-labelledby="my-server-hero-title">
            <div className="my-server-hero-accent" aria-hidden />
            <p className={`my-server-hero-eyebrow${mc}`}>{t('myServer.createEyebrow')}</p>
            <h2 id="my-server-hero-title" className={`my-server-hero-title${mc}`}>
              {t('myServer.heroTitle')}
            </h2>
            <p className="my-server-hero-lead">{t('myServer.heroLead')}</p>
            <ul className="my-server-hero-list">
              <li>{t('myServer.heroBullet1')}</li>
              <li>{t('myServer.heroBullet2')}</li>
              <li>{t('myServer.heroBullet3')}</li>
            </ul>
          </section>
          <div className="my-server-grid">
            {servers.map((s) => (
              <button
                key={s.id}
                type="button"
                className="my-server-card"
                onClick={() => setSelectedId(s.id)}
              >
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
                  <div className="my-server-card-meta">
                    {packOptions.find((p) => p.value === s.modpackId)?.label ?? s.modpackId}
                  </div>
                </div>
              </button>
            ))}
            <button
              type="button"
              className="my-server-card my-server-add-card"
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
      )}
      {createOpen ? (
        <MyServerCreateModal
          packOptions={packOptions}
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

function MyServerCreateModal({
  packOptions,
  chromeGlass,
  onClose,
  onCreated,
  pushToast,
  t
}: {
  packOptions: { value: string; label: string }[]
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
  const [packId, setPackId] = useState(packOptions[0]?.value ?? '')
  const [busy, setBusy] = useState(false)
  const mc = chromeGlass ? ' font-mc' : ''

  const submit = async () => {
    if (!name.trim()) {
      pushToast(t('myServer.errName'), 'error')
      return
    }
    if (!packId) {
      pushToast(t('myServer.errPack'), 'error')
      return
    }
    setBusy(true)
    const r = await window.solea.soleaServerCreate({
      name: name.trim(),
      description: description.trim() || undefined,
      modpackId: packId,
      coverImageDataUrl: coverDataUrl ?? undefined
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

function MyServerDetail({
  server,
  coverUrl,
  packOptions,
  memoryGiB,
  chromeGlass,
  onBack,
  onRefresh,
  onDeleteRequest,
  pushToast,
  t
}: {
  server: SoleServerListRowUi
  coverUrl: string | null
  packOptions: { value: string; label: string }[]
  memoryGiB: number
  chromeGlass: boolean
  onBack: () => void
  onRefresh: () => Promise<void>
  onDeleteRequest: () => void
  pushToast: (msg: string, variant?: 'info' | 'success' | 'error') => void
  t: (k: string, ...args: unknown[]) => string
}) {
  const mc = chromeGlass ? ' font-mc' : ''
  const [name, setName] = useState(server.name)
  const [description, setDescription] = useState(server.description)
  /** Nouvelle image (data URL) ou null si l’utilisateur retire la couverture. */
  const [coverOverride, setCoverOverride] = useState<string | 'remove' | undefined>(undefined)
  const [ramMiB, setRamMiB] = useState(server.ramMiB)
  const [packId, setPackId] = useState(server.modpackId)
  const [lines, setLines] = useState<string[]>([])
  const [cmd, setCmd] = useState('')
  const [ipHidden, setIpHidden] = useState(true)
  const [publicIp, setPublicIp] = useState<string | null>(null)
  const [updateInfo, setUpdateInfo] = useState<{
    hasUpdate: boolean
    latest?: string
    current?: string
  } | null>(null)
  const consoleRef = useRef<HTMLPreElement>(null)
  const persistMetaRef = useRef<(opts?: { silent?: boolean }) => Promise<void>>(async () => {})

  const maxRam = Math.min(64 * 1024, Math.max(1024, Math.floor(memoryGiB * 1024 * 0.92)))

  const displayCoverPreview =
    coverOverride === 'remove' ? null : coverOverride ?? coverUrl

  useEffect(() => {
    setName(server.name)
    setDescription(server.description)
    setRamMiB(server.ramMiB)
    setPackId(server.modpackId)
  }, [server.name, server.description, server.ramMiB, server.modpackId])

  useEffect(() => {
    setCoverOverride(undefined)
  }, [server.id])

  const pullConsole = useCallback(async () => {
    const l = await window.solea.soleaServerConsole(server.id)
    setLines(l)
  }, [server.id])

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

  const persistMeta = useCallback(
    async (opts?: { silent?: boolean }) => {
      if (server.installState === 'installing') return
      const payload: Parameters<typeof window.solea.soleaServerUpdate>[0] = {
        id: server.id,
        name,
        description,
        ramMiB,
        modpackId: packId
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
      ramMiB,
      packId,
      coverOverride,
      onRefresh,
      pushToast,
      t
    ]
  )
  persistMetaRef.current = persistMeta

  /** Texte / RAM / modpack : sauvegarde différée quand ça diffère du serveur. */
  useEffect(() => {
    const dirty =
      name !== server.name ||
      description !== server.description ||
      ramMiB !== server.ramMiB ||
      packId !== server.modpackId
    if (!dirty) return
    const id = window.setTimeout(() => {
      void persistMeta({ silent: true })
    }, AUTOSAVE_MS)
    return () => clearTimeout(id)
  }, [name, description, ramMiB, packId, server.name, server.description, server.ramMiB, server.modpackId, persistMeta])

  /** Image de couverture : sauvegarde dès changement (ajout / suppression). */
  useEffect(() => {
    if (coverOverride === undefined) return
    void persistMetaRef.current({ silent: true })
  }, [coverOverride])

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
        <div className="my-server-detail my-server-detail--dashboard">
          <div className="my-server-detail-head">
            <button type="button" className="btn-muted my-server-back" onClick={onBack}>
              ← {t('myServer.back')}
            </button>
            <div className="my-server-detail-title-block">
              <h2 className={chromeGlass ? 'font-mc' : undefined}>{server.name}</h2>
              <p className="my-server-muted my-server-detail-tagline">{t('myServer.detailLead')}</p>
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

          <div className="my-server-dash-grid">
            <div className="my-server-dash-sidebar">
              <div className="my-server-panel">
                <h3 className={chromeGlass ? 'font-mc' : undefined}>{t('myServer.sectionJoin')}</h3>
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

              <div className="my-server-panel">
                <h3 className={chromeGlass ? 'font-mc' : undefined}>{t('myServer.sectionConfig')}</h3>
                {server.installState === 'installing' ? (
                  <p className="my-server-muted">{t('myServer.installingHint')}</p>
                ) : null}
                {server.installState === 'error' && server.installError ? (
                  <p role="alert" className="my-server-muted" style={{ color: '#f88' }}>
                    {server.installError}
                  </p>
                ) : null}
                <div className="my-server-field">
                  <label htmlFor="msd-name">{t('myServer.fieldName')}</label>
                  <input id="msd-name" type="text" value={name} onChange={(e) => setName(e.target.value)} />
                </div>
                <div className="my-server-field my-server-field--compact">
                  <label htmlFor="msd-desc">{t('myServer.fieldDesc')}</label>
                  <textarea id="msd-desc" value={description} onChange={(e) => setDescription(e.target.value)} />
                </div>
                <div className="my-server-field">
                  <span className="my-server-field-label">{t('myServer.fieldCover')}</span>
                  <CoverImageField
                    inputId="msd-cover"
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
                  <span id="msd-pack" className="my-server-field-label">
                    {t('myServer.fieldPack')}
                  </span>
                  <LauncherSelect
                    aria-labelledby="msd-pack"
                    value={packId}
                    onChange={setPackId}
                    options={packOptions}
                    disabled={server.installState === 'installing'}
                  />
                  <p className="my-server-muted">{t('myServer.changePackHint')}</p>
                </div>
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
                    />
                    <span className="my-server-ram-value">{Math.round(ramMiB / 1024)} Go</span>
                  </div>
                </div>
                <div className="actions-bar my-server-actions-row">
                  <button
                    type="button"
                    className="btn-muted"
                    onClick={() => void checkUpdate()}
                    disabled={server.installState !== 'ready'}
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
                      ? t('myServer.updateLine', { cur: updateInfo.current ?? '?', latest: updateInfo.latest ?? '?' })
                      : t('myServer.updateLineOk', { cur: updateInfo.current ?? '?' })}
                  </p>
                ) : null}
                <p className="my-server-muted my-server-control-hint">{t('myServer.controlHint')}</p>
              </div>
            </div>

            <div className="my-server-dash-console-panel">
              <h3 className={chromeGlass ? 'font-mc' : undefined}>{t('myServer.sectionConsole')}</h3>
              <div className="my-server-console-wrap">
                <pre ref={consoleRef} className="my-server-console" tabIndex={0}>
                  {lines.length ? lines.join('\n') : '—'}
                </pre>
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
          </div>
        </div>
      </div>
    </div>
  )
}
