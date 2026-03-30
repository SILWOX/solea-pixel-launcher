import { useRef } from 'react'
import { RELEASE_NOTES } from './releaseNotes'
import { useI18n } from './i18n/I18nContext'
import { useFocusTrap } from './a11y/useFocusTrap'

type WhatsNewModalProps = {
  version: string
  onDismiss: () => void
}

export function WhatsNewModal({ version, onDismiss }: WhatsNewModalProps) {
  const { t, locale } = useI18n()
  const dialogRef = useRef<HTMLDivElement>(null)
  const block = RELEASE_NOTES[version]
  const body = block ? (locale === 'fr' ? block.fr : block.en) : `${version}`

  useFocusTrap(true, dialogRef, { onEscape: onDismiss })

  return (
    <div className="whatsnew-backdrop" role="presentation" onClick={onDismiss}>
      <div
        ref={dialogRef}
        className="whatsnew-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="whatsnew-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="whatsnew-title">{t('whatsNew.title')}</h2>
        <p className="whatsnew-ver">{version}</p>
        <p className="whatsnew-body">{body}</p>
        <button type="button" className="btn-save" onClick={onDismiss}>
          {t('whatsNew.close')}
        </button>
      </div>
    </div>
  )
}
