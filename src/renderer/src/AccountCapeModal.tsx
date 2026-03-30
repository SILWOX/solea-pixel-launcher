import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { SyntheticEvent } from 'react'
import { AccountSkinViewer } from './AccountSkinViewer'
import { useI18n } from './i18n/I18nContext'
import type { SkinViewerAnimation } from './launcherTypes'
import { LauncherSelect } from './ui/LauncherSelect'
import { useToast } from './ui/ToastContext'
import { useFocusTrap } from './a11y/useFocusTrap'

function isMojangCapeElytraAtlas(w: number, h: number): boolean {
  return w > 0 && h > 0 && w * 11 === h * 23
}

function CapeTileImage({ src, alt }: { src: string; alt: string }) {
  const [mojangAtlas, setMojangAtlas] = useState(false)
  const onLoad = (e: SyntheticEvent<HTMLImageElement>) => {
    const { naturalWidth, naturalHeight } = e.currentTarget
    setMojangAtlas(isMojangCapeElytraAtlas(naturalWidth, naturalHeight))
  }
  return (
    <img
      src={src}
      alt={alt}
      className={`cape-tile-img${mojangAtlas ? ' cape-tile-img-mojang-atlas' : ''}`}
      onLoad={onLoad}
    />
  )
}

type CapeRow = {
  id: string
  alias: string
  state: string
  url: string | null
  dataUrl: string | null
}

type SortMode = 'active' | 'name'

type AccountCapeModalProps = {
  open: boolean
  onClose: () => void
  playerName: string
  skinDataUrl: string | null
  skinModel: 'slim' | 'default' | 'auto-detect'
  viewerBackground: string
  skinAnim: SkinViewerAnimation
  reduceMotion: boolean
  onApplied: () => void
}

export function AccountCapeModal({
  open,
  onClose,
  playerName,
  skinDataUrl,
  skinModel,
  viewerBackground,
  skinAnim,
  reduceMotion,
  onApplied
}: AccountCapeModalProps) {
  const { t, formatInteger } = useI18n()
  const { pushToast } = useToast()
  const dialogRef = useRef<HTMLDivElement>(null)

  useFocusTrap(open, dialogRef, {
    onEscape: () => {
      if (!busy) onClose()
    }
  })
  const [capes, setCapes] = useState<CapeRow[]>([])
  const [pendingId, setPendingId] = useState<string | null>(null)
  const [loadErr, setLoadErr] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [search, setSearch] = useState('')
  const [sortMode, setSortMode] = useState<SortMode>('active')

  const load = useCallback(() => {
    setLoadErr(null)
    setLoading(true)
    void window.solea.listAccountCapes().then((r) => {
      setLoading(false)
      if (!r.ok) {
        setLoadErr(r.error)
        setCapes([])
        setPendingId(null)
        return
      }
      setCapes(r.capes)
      setPendingId(r.activeCapeId)
    })
  }, [])

  useEffect(() => {
    if (open) {
      setSearch('')
      load()
    }
  }, [open, load])

  const filteredSorted = useMemo(() => {
    const q = search.trim().toLowerCase()
    let list = q ? capes.filter((c) => c.alias.toLowerCase().includes(q)) : [...capes]
    if (sortMode === 'name') {
      list.sort((a, b) => a.alias.localeCompare(b.alias, undefined, { sensitivity: 'base' }))
    } else {
      list.sort((a, b) => {
        const aw = a.state === 'ACTIVE' ? 0 : 1
        const bw = b.state === 'ACTIVE' ? 0 : 1
        if (aw !== bw) return aw - bw
        return a.alias.localeCompare(b.alias, undefined, { sensitivity: 'base' })
      })
    }
    return list
  }, [capes, search, sortMode])

  const pendingCapeDataUrl =
    pendingId === null ? null : capes.find((c) => c.id === pendingId)?.dataUrl ?? null

  const apply = () => {
    setBusy(true)
    void window.solea.setAccountActiveCape(pendingId).then((r) => {
      setBusy(false)
      if (!r.ok) {
        pushToast(r.error, 'error')
        return
      }
      onApplied()
      onClose()
    })
  }

  if (!open) return null

  return (
    <div
      className="cape-modal-backdrop"
      role="presentation"
      onClick={() => {
        if (!busy) onClose()
      }}
    >
      <div
        ref={dialogRef}
        className="cape-modal"
        role="dialog"
        aria-labelledby="cape-modal-title"
        aria-modal="true"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="cape-modal-header">
          <div>
            <h2 id="cape-modal-title">{t('cape.title')}</h2>
            <p className="cape-modal-sub">
              {loading ? t('cape.loading') : t('cape.count', { count: formatInteger(capes.length) })}
            </p>
          </div>
          <button
            type="button"
            className="cape-modal-close"
            aria-label={t('cape.close')}
            disabled={busy}
            onClick={() => onClose()}
          >
            ×
          </button>
        </header>
        <div className="cape-modal-body">
          <div className="cape-modal-preview">
            <div className="cape-modal-viewer-wrap">
              {loading ? (
                <div className="cape-modal-viewer-skeleton" aria-hidden />
              ) : (
                <AccountSkinViewer
                  skinDataUrl={skinDataUrl}
                  model={skinModel}
                  capeDataUrl={pendingCapeDataUrl}
                  playerName={playerName}
                  viewerBackground={viewerBackground}
                  animation={skinAnim}
                  reduceMotion={reduceMotion}
                />
              )}
            </div>
            <p className="cape-modal-hint">{t('cape.hint')}</p>
          </div>
          <div className="cape-modal-grid-col">
            <div className="cape-modal-toolbar">
              <label className="cape-modal-search-label">
                <span className="sr-only">{t('cape.search')}</span>
                <input
                  type="search"
                  className="cape-modal-search"
                  placeholder={t('cape.search')}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  disabled={loading || !!loadErr}
                />
              </label>
              <label className="cape-modal-sort-label">
                {t('cape.sort')}
                <LauncherSelect
                  compact
                  value={sortMode}
                  onChange={(v) => setSortMode(v as SortMode)}
                  disabled={loading || !!loadErr}
                  options={[
                    { value: 'active', label: t('cape.sortActive') },
                    { value: 'name', label: t('cape.sortName') }
                  ]}
                />
              </label>
            </div>
            {loadErr ? (
              <div className="cape-modal-err-block">
                <p className="cape-modal-err">{loadErr}</p>
                <button type="button" className="btn-muted cape-modal-retry" onClick={() => load()}>
                  {t('cape.retry')}
                </button>
              </div>
            ) : null}
            <div className="cape-tile-grid">
              <button
                type="button"
                className={`cape-tile cape-tile-none ${pendingId === null ? 'active' : ''}`}
                onClick={() => setPendingId(null)}
              >
                <span className="cape-tile-none-x" aria-hidden>
                  ×
                </span>
                <span>{t('cape.none')}</span>
              </button>
              {filteredSorted.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  className={`cape-tile ${pendingId === c.id ? 'active' : ''}`}
                  title={c.alias}
                  onClick={() => setPendingId(c.id)}
                >
                  {c.dataUrl ? (
                    <CapeTileImage src={c.dataUrl} alt="" />
                  ) : (
                    <span className="cape-tile-fallback">{c.alias.slice(0, 2)}</span>
                  )}
                  <span className="cape-tile-label">{c.alias}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
        <footer className="cape-modal-footer">
          <button type="button" className="btn-muted" disabled={busy} onClick={() => onClose()}>
            {t('cape.cancel')}
          </button>
          <button
            type="button"
            className="btn-save"
            disabled={busy || !!loadErr || loading}
            onClick={() => apply()}
          >
            {busy ? '…' : t('cape.confirm')}
          </button>
        </footer>
      </div>
    </div>
  )
}
