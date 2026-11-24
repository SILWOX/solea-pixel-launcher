/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { useI18n } from '../i18n/I18nContext'
import { renderActuSegmentBody } from './parseActuMarkup'

const DEMO_MARKUP = `# Actu Soleá
- **Gras** et *italique*
- Couleur §6or §ret §bdéfaut
> Citation type Discord
___
## Deuxième bloc
\`code\` et ~~barré~~ et ++surligné++
`

const ROWS: { code: string; descKey: string }[] = [
  { code: '# …', descKey: 'debug.actuSyntax.h1' },
  { code: '## …', descKey: 'debug.actuSyntax.h2' },
  { code: '### …', descKey: 'debug.actuSyntax.h3' },
  { code: '**texte**', descKey: 'debug.actuSyntax.bold' },
  { code: '*texte*', descKey: 'debug.actuSyntax.italic' },
  { code: '~~texte~~', descKey: 'debug.actuSyntax.strike' },
  { code: '`code`', descKey: 'debug.actuSyntax.code' },
  { code: '++texte++', descKey: 'debug.actuSyntax.mark' },
  { code: '- …', descKey: 'debug.actuSyntax.bullet' },
  { code: '> …', descKey: 'debug.actuSyntax.quote' },
  { code: '___', descKey: 'debug.actuSyntax.sep' },
  { code: '§x', descKey: 'debug.actuSyntax.mc' }
]

export function ActuSyntaxDebugSection() {
  const { t } = useI18n()
  return (
    <section className="debug-actu-syntax-panel" aria-labelledby="debug-actu-syntax-heading">
      <h2 id="debug-actu-syntax-heading" className="debug-toolbar-title">
        {t('debug.actuSyntaxTitle')}
      </h2>
      <p className="debug-actu-syntax-intro">{t('debug.actuSyntaxIntro')}</p>
      <ul className="debug-actu-syntax-list">
        {ROWS.map((row) => (
          <li key={row.descKey}>
            <code className="debug-actu-syntax-code">{row.code}</code>
            <span className="debug-actu-syntax-desc"> — {t(row.descKey)}</span>
          </li>
        ))}
      </ul>
      <p className="debug-actu-syntax-preview-label">{t('debug.actuSyntax.previewLabel')}</p>
      <div className="debug-actu-syntax-preview actu-bubble">{renderActuSegmentBody(DEMO_MARKUP, 'debug-actu-demo')}</div>
    </section>
  )
}
