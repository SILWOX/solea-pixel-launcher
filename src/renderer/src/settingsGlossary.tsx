import type { TFunction } from './i18n/I18nContext'

export type SettingsGlossaryKey =
  | 'afterLaunch'
  | 'networkCard'
  | 'downloadThreads'
  | 'networkTimeout'
  | 'javaPath'
  | 'javaVersion'
  | 'jvmArgs'
  | 'azureId'
  | 'networkSlow'
  | 'diagLaunch'
  | 'ram'
  | 'resolution'
  | 'instanceFolder'
  | 'verifyFiles'

export function SettingsGlossaryTrigger({
  gkey,
  openKey,
  setOpenKey,
  t,
  discordUrl
}: {
  gkey: SettingsGlossaryKey
  openKey: SettingsGlossaryKey | null
  setOpenKey: (k: SettingsGlossaryKey | null) => void
  t: TFunction
  discordUrl: string
}) {
  const open = openKey === gkey
  const title = t(`settings.glossary.${gkey}.title`)
  const body = t(`settings.glossary.${gkey}.body`)
  return (
    <div className="settings-glossary-wrap">
      <button
        type="button"
        className="settings-glossary-btn"
        aria-expanded={open}
        aria-label={title}
        onClick={(e) => {
          e.preventDefault()
          e.stopPropagation()
          setOpenKey(open ? null : gkey)
        }}
      >
        <svg className="settings-glossary-ico" viewBox="0 0 24 24" fill="none" aria-hidden>
          <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2" />
          <path
            d="M9.5 9.5a2.5 2.5 0 015 0c0 2-2.5 1.5-2.5 4M12 17h.01"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
      </button>
      {open ? (
        <div className="settings-glossary-pop" role="tooltip">
          <p className="settings-glossary-pop-title">{title}</p>
          <p className="settings-glossary-pop-body">{body}</p>
          <button
            type="button"
            className="btn-linkish settings-glossary-more"
            onClick={() => void window.solea.openExternalUrl(discordUrl)}
          >
            {t('home.glossary.more')}
          </button>
        </div>
      ) : null}
    </div>
  )
}
