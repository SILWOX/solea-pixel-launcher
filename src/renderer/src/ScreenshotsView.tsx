/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { isModpackId } from './modpackTheme'
import { useI18n } from './i18n/I18nContext'
import { LauncherSelect } from './ui/LauncherSelect'
import { useToast } from './ui/ToastContext'

type ShotItem = { fileName: string; thumbDataUrl: string }

export function ScreenshotsView({
  modpacksList,
  initialModpackId
}: {
  modpacksList: { id: string; displayName: string }[]
  initialModpackId: string
}) {
  const { t } = useI18n()
  const { pushToast } = useToast()
  const packOptions = modpacksList.filter((m) => isModpackId(m.id))
  const [packId, setPackId] = useState<string>(() =>
    isModpackId(initialModpackId) ? initialModpackId : packOptions[0]?.id ?? ''
  )
  const [items, setItems] = useState<ShotItem[]>([])
  const [loading, setLoading] = useState(false)
  const [preview, setPreview] = useState<null | ShotItem>(null)
  const [fullUrl, setFullUrl] = useState<string | null>(null)
  const [fullLoading, setFullLoading] = useState(false)
  const [retouchOpen, setRetouchOpen] = useState(false)
  const [brightness, setBrightness] = useState(100)
  const [contrast, setContrast] = useState(100)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const loadedImgRef = useRef<HTMLImageElement | null>(null)

  const currentPackLabel = useMemo(
    () => packOptions.find((p) => p.id === packId)?.displayName ?? packId,
    [packOptions, packId]
  )

  const packSelectOptions = useMemo(
    () => packOptions.map((m) => ({ value: m.id, label: m.displayName })),
    [packOptions]
  )

  const refreshList = useCallback(async () => {
    if (!isModpackId(packId)) {
      setItems([])
      return
    }
    setLoading(true)
    try {
      const r = await window.solea.listModpackScreenshots(packId)
      if (r.ok) setItems(r.items)
      else setItems([])
    } finally {
      setLoading(false)
    }
  }, [packId])

  useEffect(() => {
    void refreshList()
  }, [refreshList])

  useEffect(() => {
    if (isModpackId(initialModpackId)) setPackId(initialModpackId)
  }, [initialModpackId])

  const loadFull = async (fileName: string) => {
    if (!isModpackId(packId)) return
    setFullLoading(true)
    setFullUrl(null)
    const r = await window.solea.getModpackScreenshotFull(packId, fileName)
    setFullLoading(false)
    if (!r.ok) {
      pushToast(
        r.error === 'too_large' ? t('screenshots.errors.tooLarge') : t('screenshots.errors.loadFull'),
        'error'
      )
      return
    }
    setFullUrl(r.dataUrl)
  }

  const openPreview = (it: ShotItem) => {
    setPreview(it)
    void loadFull(it.fileName)
  }

  const closePreview = () => {
    setPreview(null)
    setFullUrl(null)
    setRetouchOpen(false)
    setBrightness(100)
    setContrast(100)
    loadedImgRef.current = null
  }

  const exportDataUrl = async (dataUrl: string, defaultName: string) => {
    const r = await window.solea.saveDataUrlAsPng(dataUrl, defaultName)
    if (r.ok) {
      pushToast(t('screenshots.exportOk', { path: r.path }), 'success')
    } else if (r.error !== 'cancelled') {
      pushToast(t('screenshots.exportFail', { detail: r.error }), 'error')
    }
  }

  const drawRetouchCanvas = useCallback(() => {
    const canvas = canvasRef.current
    const img = loadedImgRef.current
    if (!canvas || !img?.complete || !fullUrl) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    const w = img.naturalWidth
    const h = img.naturalHeight
    canvas.width = w
    canvas.height = h
    ctx.filter = `brightness(${brightness}%) contrast(${contrast}%)`
    ctx.drawImage(img, 0, 0)
    ctx.filter = 'none'
  }, [brightness, contrast, fullUrl])

  useEffect(() => {
    if (!retouchOpen || !fullUrl) return
    const img = new Image()
    img.onload = () => {
      loadedImgRef.current = img
      requestAnimationFrame(() => drawRetouchCanvas())
    }
    img.src = fullUrl
  }, [retouchOpen, fullUrl, drawRetouchCanvas])

  useEffect(() => {
    if (retouchOpen) drawRetouchCanvas()
  }, [brightness, contrast, retouchOpen, drawRetouchCanvas])

  const openRetouch = () => {
    if (!fullUrl || !preview) return
    setBrightness(100)
    setContrast(100)
    setRetouchOpen(true)
  }

  const exportRetouched = () => {
    const canvas = canvasRef.current
    if (!canvas || !preview) return
    const dataUrl = canvas.toDataURL('image/png')
    const base = preview.fileName.replace(/\.[^.]+$/, '')
    void exportDataUrl(dataUrl, `${base}-edited.png`)
  }

  const openShotsFolder = () => {
    if (!isModpackId(packId)) return
    void window.solea.openScreenshotsFolder(packId).then((r) => {
      if (!r.ok) pushToast(r.error, 'error')
    })
  }

  if (!packOptions.length) {
    return (
      <div className="shell-content shell-content-news screenshots-view">
        <div className="screenshots-scroll">
          <div className="screenshots-page-inner">
          <section className="screenshots-card screenshots-card--solo">
            <p className="screenshots-eyebrow">{t('screenshots.eyebrow')}</p>
            <h2 className="screenshots-h2">{t('screenshots.title')}</h2>
            <p className="screenshots-muted-block">{t('screenshots.noPacks')}</p>
          </section>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="shell-content shell-content-news screenshots-view">
      <div className="screenshots-scroll">
        <div className="screenshots-page-inner">
        <section className="screenshots-card screenshots-card--intro" aria-labelledby="shots-title">
          <p className="screenshots-eyebrow">{t('screenshots.eyebrow')}</p>
          <h2 id="shots-title" className="screenshots-h2">
            {t('screenshots.title')}
          </h2>
          <p className="screenshots-subtitle">{t('screenshots.subtitle')}</p>
          <div className="screenshots-steps">
            <p className="screenshots-steps-title">{t('screenshots.howTitle')}</p>
            <ol className="screenshots-steps-list">
              <li>{t('screenshots.howStep1')}</li>
              <li>{t('screenshots.howStep2')}</li>
              <li>{t('screenshots.howStep3')}</li>
            </ol>
          </div>
        </section>

        <section className="screenshots-card screenshots-card--toolbar" aria-label={t('screenshots.toolbarAria')}>
          <div className="screenshots-toolbar-grid">
            <div className="screenshots-field">
              <span className="screenshots-field-label">
                {t('screenshots.instanceSection')}
              </span>
              <LauncherSelect
                className="screenshots-instance-select"
                aria-label={t('screenshots.instanceSection')}
                value={packId}
                onChange={setPackId}
                options={packSelectOptions}
                disabled={packSelectOptions.length === 0}
              />
            </div>
            <div className="screenshots-toolbar-actions">
              <button
                type="button"
                className="btn-muted screenshots-toolbar-btn"
                disabled={loading}
                onClick={() => void refreshList()}
              >
                {t('screenshots.refresh')}
              </button>
              <button
                type="button"
                className="btn-save screenshots-toolbar-btn screenshots-toolbar-btn--primary"
                disabled={!isModpackId(packId)}
                onClick={openShotsFolder}
              >
                {t('screenshots.openFolder')}
              </button>
            </div>
          </div>
        </section>

        <section className="screenshots-card screenshots-card--gallery" aria-labelledby="shots-gallery-title">
          <div className="screenshots-gallery-head">
            <div>
              <h3 id="shots-gallery-title" className="screenshots-h3">
                {t('screenshots.galleryTitle')}
              </h3>
              <p className="screenshots-gallery-sub">{currentPackLabel}</p>
            </div>
            {!loading ? (
              <span
                className={`screenshots-badge${items.length === 0 ? ' screenshots-badge--dim' : ''}`}
              >
                {items.length === 0 ? t('screenshots.countEmpty') : t('screenshots.count', { n: items.length })}
              </span>
            ) : (
              <span className="screenshots-badge screenshots-badge--loading">{t('screenshots.loadingHint')}</span>
            )}
          </div>

          {loading ? (
            <div className="screenshots-skeleton" aria-busy="true" aria-label={t('screenshots.loading')}>
              {Array.from({ length: 8 }, (_, i) => (
                <div key={i} className="shots-skel-tile" />
              ))}
            </div>
          ) : items.length === 0 ? (
            <div className="screenshots-empty-state">
              <div className="screenshots-empty-icon" aria-hidden>
                <span className="screenshots-empty-glyph">📷</span>
              </div>
              <h4 className="screenshots-empty-title">{t('screenshots.emptyTitle')}</h4>
              <p className="screenshots-empty-body">{t('screenshots.emptyText')}</p>
              <button type="button" className="btn-save screenshots-empty-cta" onClick={openShotsFolder}>
                {t('screenshots.openFolder')}
              </button>
              <p className="screenshots-empty-tip">{t('screenshots.actionHint')}</p>
            </div>
          ) : (
            <>
              <p className="screenshots-grid-hint">{t('screenshots.gridHint')}</p>
              <div className="screenshots-grid">
                {items.map((it) => (
                  <button
                    key={it.fileName}
                    type="button"
                    className="screenshots-tile"
                    onClick={() => openPreview(it)}
                  >
                    <span className="screenshots-tile-frame">
                      <img src={it.thumbDataUrl} alt="" loading="lazy" />
                    </span>
                    <span className="screenshots-tile-name">{it.fileName}</span>
                  </button>
                ))}
              </div>
            </>
          )}
        </section>
        </div>
      </div>

      {preview ? (
        <div
          className="pack-confirm-backdrop screenshots-preview-backdrop"
          role="presentation"
          onClick={closePreview}
        >
          <div
            className="screenshots-preview-modal"
            role="dialog"
            aria-modal="true"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="screenshots-preview-head">
              <div className="screenshots-preview-head-text">
                <p className="screenshots-preview-eyebrow">{t('screenshots.previewEyebrow')}</p>
                <h3 className="screenshots-preview-title">{preview.fileName}</h3>
              </div>
              <button type="button" className="btn-muted screenshots-preview-close" onClick={closePreview}>
                {t('screenshots.close')}
              </button>
            </div>
            <div className="screenshots-preview-body">
              {fullLoading ? (
                <div className="screenshots-preview-loading">
                  <span className="shots-spinner" aria-hidden />
                  <p>{t('screenshots.loadingFull')}</p>
                </div>
              ) : fullUrl && !retouchOpen ? (
                <div className="screenshots-preview-frame">
                  <img className="screenshots-preview-img" src={fullUrl} alt="" />
                </div>
              ) : !retouchOpen ? (
                <p className="screenshots-preview-err">{t('screenshots.errors.loadFull')}</p>
              ) : null}
            </div>
            {!retouchOpen ? (
              <>
                <p className="screenshots-modal-actions-hint">{t('screenshots.modalActionsHint')}</p>
                <div className="screenshots-preview-actions">
                  <button
                    type="button"
                    className="btn-muted screenshots-preview-action"
                    disabled={!fullUrl}
                    onClick={() => fullUrl && void exportDataUrl(fullUrl, preview.fileName)}
                  >
                    <span className="screenshots-action-label">{t('screenshots.shareExport')}</span>
                    <span className="screenshots-action-desc">{t('screenshots.shareExportDesc')}</span>
                  </button>
                  <button
                    type="button"
                    className="btn-save screenshots-preview-action screenshots-preview-action--primary"
                    disabled={!fullUrl}
                    onClick={openRetouch}
                  >
                    <span className="screenshots-action-label">{t('screenshots.retouch')}</span>
                    <span className="screenshots-action-desc">{t('screenshots.retouchDesc')}</span>
                  </button>
                </div>
              </>
            ) : (
              <div className="screenshots-retouch-panel">
                <p className="screenshots-retouch-hint">{t('screenshots.retouchHint')}</p>
                <div className="screenshots-retouch-canvas-wrap">
                  <canvas ref={canvasRef} className="screenshots-retouch-canvas" />
                </div>
                <label className="screenshots-retouch-slider">
                  <span>{t('screenshots.brightness')}</span>
                  <input
                    type="range"
                    min={60}
                    max={140}
                    value={brightness}
                    onChange={(e) => setBrightness(Number(e.target.value))}
                  />
                  <span>{brightness}%</span>
                </label>
                <label className="screenshots-retouch-slider">
                  <span>{t('screenshots.contrast')}</span>
                  <input
                    type="range"
                    min={70}
                    max={130}
                    value={contrast}
                    onChange={(e) => setContrast(Number(e.target.value))}
                  />
                  <span>{contrast}%</span>
                </label>
                <div className="screenshots-retouch-actions">
                  <button type="button" className="btn-muted" onClick={() => setRetouchOpen(false)}>
                    {t('screenshots.backPreview')}
                  </button>
                  <button type="button" className="btn-muted" onClick={() => { setBrightness(100); setContrast(100) }}>
                    {t('screenshots.resetFilters')}
                  </button>
                  <button type="button" className="btn-save" onClick={exportRetouched}>
                    {t('screenshots.exportEdited')}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      ) : null}
    </div>
  )
}
