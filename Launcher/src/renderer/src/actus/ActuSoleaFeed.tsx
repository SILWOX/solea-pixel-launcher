/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useI18n } from '../i18n/I18nContext'
import { renderActuSegmentBody } from './parseActuMarkup'
import { splitActuSegments } from './actusoleaSegments'
import { useActuSoleaFeed } from './useActuSoleaFeed'
type Props = {
  compact?: boolean
}

export function ActuSoleaFeed({ compact = false }: Props) {
  const { t } = useI18n()
  const { loading, error, data, fetchedAt, hasUrl, refresh } = useActuSoleaFeed()

  if (!hasUrl) {
    return (
      <div className={`actu-feed actu-feed--empty${compact ? ' actu-feed--compact' : ''}`}>
        <p className="actu-feed-empty">{t('actu.feedNoUrl')}</p>
        <code className="actu-feed-env-hint">VITE_ACTU_SOLEA_JSON_URL</code>
      </div>
    )
  }

  const segments = data ? splitActuSegments(data) : []
  const updatedLabel = fetchedAt ?? data?.updatedAt


  return (
    <div className={`actu-feed${compact ? ' actu-feed--compact' : ''}`}>
      <div className="actu-feed-toolbar">
        <button
          type="button"
          className="actu-feed-refresh btn-quiet"
          disabled={loading}
          onClick={() => void refresh()}
          aria-label={t('actu.refreshAria')}
        >
          {loading ? t('actu.refreshing') : t('actu.refresh')}
        </button>
        {updatedLabel ? (
          <time className="actu-feed-updated" dateTime={updatedLabel}>
            {t('actu.updatedAt', { date: updatedLabel })}
          </time>
        ) : null}
      </div>
      {error ? (
        <p className="actu-feed-error" role="alert">
          {t('actu.fetchError', { detail: error })}
        </p>
      ) : null}
      {segments.length === 0 && !loading && !error ? (
        <p className="actu-feed-empty">{t('actu.feedEmpty')}</p>
      ) : (
        <div className="actu-feed-bubbles">
          {segments.map((seg, idx) => (
            <article key={idx} className="actu-bubble">
              {renderActuSegmentBody(seg, `seg-${idx}`)}
            </article>
          ))}
        </div>
      )}
    </div>
  )
}
