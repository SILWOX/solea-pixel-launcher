/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useI18n } from '../i18n/I18nContext'

export function FeedActuNewsTabs({
  value,
  onChange,
  compact
}: {
  value: 'actu' | 'updates'
  onChange: (v: 'actu' | 'updates') => void
  compact?: boolean
}) {
  const { t } = useI18n()
  return (
    <div
      className={`feed-actu-news-tabs${compact ? ' feed-actu-news-tabs--compact' : ''}`}
      data-active={value}
      role="tablist"
      aria-label={t('actu.tablistAria')}
    >
      <span className="feed-actu-news-tabs__glider" aria-hidden />
      <button
        type="button"
        role="tab"
        aria-selected={value === 'actu'}
        className={`feed-actu-news-tab${value === 'actu' ? ' feed-actu-news-tab--on' : ''}`}
        onClick={() => onChange('actu')}
      >
        {t('actu.tabActu')}
      </button>
      <button
        type="button"
        role="tab"
        aria-selected={value === 'updates'}
        className={`feed-actu-news-tab${value === 'updates' ? ' feed-actu-news-tab--on' : ''}`}
        onClick={() => onChange('updates')}
      >
        {t('actu.tabUpdates')}
      </button>
    </div>
  )
}
