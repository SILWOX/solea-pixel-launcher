/**
 * Rendu du mini-langage Actu Soleá (titres, markdown léger, § couleurs Minecraft).
 */
import { Fragment, type CSSProperties, type ReactNode } from 'react'
import { MINECRAFT_COLOR_HEX } from './minecraftTextCodes'

type McStyle = {
  color?: string
  bold?: boolean
  italic?: boolean
  underline?: boolean
  strike?: boolean
  obfuscated?: boolean
}

function styleReset(): McStyle {
  return {}
}

function applyMcCode(prev: McStyle, code: string): McStyle {
  const c = code.toLowerCase()
  if (c === 'r') return styleReset()
  if (c === 'l') return { ...prev, bold: true }
  if (c === 'o') return { ...prev, italic: true }
  if (c === 'm') return { ...prev, strike: true }
  if (c === 'n') return { ...prev, underline: true }
  if (c === 'k') return { ...prev, obfuscated: true }
  if (MINECRAFT_COLOR_HEX[c]) {
    return { color: MINECRAFT_COLOR_HEX[c] }
  }
  return prev
}

function spanStyle(s: McStyle): CSSProperties {
  const st: CSSProperties = {}
  if (s.color) st.color = s.color
  if (s.bold) st.fontWeight = 700
  if (s.italic) st.fontStyle = 'italic'
  if (s.underline) st.textDecoration = 'underline'
  if (s.strike) st.textDecoration = 'line-through'
  if (s.underline && s.strike) st.textDecoration = 'underline line-through'
  return st
}

function sanitizeHref(href: string): string {
  const t = href.trim()
  if (!t) return ''
  const lower = t.toLowerCase()
  if (lower.startsWith('javascript:') || lower.startsWith('data:')) return ''
  if (/^https?:\/\//i.test(t)) return t
  if (t.startsWith('/') && !t.startsWith('//')) return t
  if (lower.startsWith('mailto:')) return t
  return ''
}

function sanitizeImgSrc(src: string): string {
  const t = src.trim()
  if (!t) return ''
  const lower = t.toLowerCase()
  if (lower.startsWith('javascript:') || lower.startsWith('data:')) return ''
  if (/^https?:\/\//i.test(t)) return t
  if (t.startsWith('/') && !t.startsWith('//')) return t
  return ''
}

function escapeCssDim(v: string): string {
  const s = v.trim().slice(0, 96)
  if (!s || /[<>"';\\]/.test(s)) return ''
  return s
}

type LinkSplitPart = { kind: 'text'; text: string } | { kind: 'link'; label: string; href: string }

function splitMarkdownLinks(text: string): LinkSplitPart[] {
  const out: LinkSplitPart[] = []
  const re = /\[([^\]]*)\]\(([^)]+)\)/g
  let last = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) out.push({ kind: 'text', text: text.slice(last, m.index) })
    out.push({ kind: 'link', label: m[1]!, href: m[2]! })
    last = re.lastIndex
  }
  if (last < text.length) out.push({ kind: 'text', text: text.slice(last) })
  if (out.length === 0) out.push({ kind: 'text', text })
  return out
}

function renderMdChunksAsNodes(text: string, keyPrefix: string): ReactNode {
  const parts = parseMarkdownChunks(text)
  return (
    <>
      {parts.map((chunk, i) => wrapMdChunk(chunk.kind, chunk.text, `${keyPrefix}-w${i}`, `${keyPrefix}-mc${i}`))}
    </>
  )
}

/** Découpe liens `[lib](url)`, puis `**`, `*`, `` ` ``, `~~`, `++`, puis § sur chaque morceau. */
export function renderInlineActu(text: string, keyPrefix: string): ReactNode {
  if (!text) return null
  const linkParts = splitMarkdownLinks(text)
  return (
    <>
      {linkParts.map((part, pi) =>
        part.kind === 'link' ? (
          <Fragment key={`${keyPrefix}-lnk${pi}`}>
            {(() => {
              const href = sanitizeHref(part.href)
              const inner = renderMdChunksAsNodes(part.label, `${keyPrefix}-lnk${pi}-lab`)
              if (!href) return inner
              return (
                <a className="actu-link" href={href} target="_blank" rel="noopener noreferrer">
                  {inner}
                </a>
              )
            })()}
          </Fragment>
        ) : (
          <Fragment key={`${keyPrefix}-tx${pi}`}>{renderMdChunksAsNodes(part.text, `${keyPrefix}-tx${pi}`)}</Fragment>
        )
      )}
    </>
  )
}

type MdKind = 'text' | 'bold' | 'italic' | 'code' | 'strike' | 'highlight' | 'spoiler'

function parseMarkdownChunks(text: string): Array<{ kind: MdKind; text: string }> {
  const out: Array<{ kind: MdKind; text: string }> = []
  let i = 0
  while (i < text.length) {
    if (text.startsWith('**', i)) {
      const end = text.indexOf('**', i + 2)
      if (end !== -1) {
        out.push({ kind: 'bold', text: text.slice(i + 2, end) })
        i = end + 2
        continue
      }
    }
    if (text.startsWith('++', i)) {
      const end = text.indexOf('++', i + 2)
      if (end !== -1) {
        out.push({ kind: 'highlight', text: text.slice(i + 2, end) })
        i = end + 2
        continue
      }
    }
    if (text.startsWith('||', i)) {
      const end = text.indexOf('||', i + 2)
      if (end !== -1) {
        out.push({ kind: 'spoiler', text: text.slice(i + 2, end) })
        i = end + 2
        continue
      }
    }
    if (text.startsWith('~~', i)) {
      const end = text.indexOf('~~', i + 2)
      if (end !== -1) {
        out.push({ kind: 'strike', text: text.slice(i + 2, end) })
        i = end + 2
        continue
      }
    }
    if (text[i] === '`') {
      const end = text.indexOf('`', i + 1)
      if (end !== -1) {
        out.push({ kind: 'code', text: text.slice(i + 1, end) })
        i = end + 1
        continue
      }
    }
    if (text[i] === '*' && !text.startsWith('**', i)) {
      const end = text.indexOf('*', i + 1)
      if (end !== -1 && !text.startsWith('**', end)) {
        out.push({ kind: 'italic', text: text.slice(i + 1, end) })
        i = end + 1
        continue
      }
    }
    let next = text.length
    const tryIdx = (p: number) => {
      if (p !== -1 && p < next) next = p
    }
    tryIdx(text.indexOf('**', i))
    tryIdx(text.indexOf('++', i))
    tryIdx(text.indexOf('||', i))
    tryIdx(text.indexOf('~~', i))
    if (text[i] === '`') tryIdx(text.indexOf('`', i + 1))
    const star = text.indexOf('*', i)
    if (star !== -1 && !text.startsWith('**', star)) tryIdx(star)
    if (next > i) {
      out.push({ kind: 'text', text: text.slice(i, next) })
      i = next
    } else {
      out.push({ kind: 'text', text: text.slice(i) })
      break
    }
  }
  return out
}

function renderMcSpans(text: string, keyPrefix: string): ReactNode[] {
  const nodes: ReactNode[] = []
  let style: McStyle = styleReset()
  let buf = ''
  let flush = (suffix: string) => {
    if (!buf) return
    const st = spanStyle(style)
    const hasObf = style.obfuscated && buf.length > 0
    const content = hasObf ? obfuscatePlaceholder(buf) : buf
    nodes.push(
      <span key={`${keyPrefix}${suffix}`} style={st} className={hasObf ? 'actu-mc-obfuscated' : undefined}>
        {content}
      </span>
    )
    buf = ''
  }

  for (let i = 0; i < text.length; i++) {
    if (text[i] === '§' && i + 1 < text.length) {
      flush(`f${i}`)
      style = applyMcCode(style, text[i + 1])
      i++
      continue
    }
    buf += text[i]
  }
  flush('end')
  return nodes
}

function obfuscatePlaceholder(s: string): string {
  const glyphs = '▒░█▄▀■□'
  let o = ''
  for (let i = 0; i < s.length; i++) {
    o += s[i] === ' ' || s[i] === '\n' ? s[i] : glyphs[i % glyphs.length]!
  }
  return o
}

function wrapMdChunk(kind: MdKind, text: string, key: string, mcKey: string): ReactNode {
  const inner = <>{renderMcSpans(text, mcKey)}</>
  if (kind === 'text') return <Fragment key={key}>{inner}</Fragment>
  if (kind === 'bold') return <strong key={key}>{inner}</strong>
  if (kind === 'italic') return <em key={key}>{inner}</em>
  if (kind === 'code')
    return (
      <code key={key} className="actu-inline-code">
        {inner}
      </code>
    )
  if (kind === 'strike')
    return (
      <s key={key} className="actu-inline-strike">
        {inner}
      </s>
    )
  if (kind === 'highlight')
    return (
      <mark key={key} className="actu-inline-mark">
        {inner}
      </mark>
    )
  if (kind === 'spoiler')
    return (
      <span key={key} className="actu-spoiler" title="Hover / survol pour afficher">
        <span className="actu-spoiler__inner">{inner}</span>
      </span>
    )
  return <Fragment key={key}>{inner}</Fragment>
}

function parseImgDirectiveBlock(
  lines: string[],
  startIdx: number
): { props: Record<string, string>; nextIndex: number } | null {
  if (lines[startIdx]!.trim() !== ':::img') return null
  const props: Record<string, string> = {}
  let j = startIdx + 1
  while (j < lines.length) {
    const tr = lines[j]!.trim()
    if (tr === ':::') return { props, nextIndex: j + 1 }
    const m = /^([a-zA-Z][\w-]*)\s*:\s*(.*)$/.exec(tr)
    if (m) props[m[1]!.toLowerCase()] = m[2]!.trim()
    j++
  }
  return null
}

function renderActuImgFigure(props: Record<string, string>, segmentKey: string, bi: number): ReactNode {
  const src = sanitizeImgSrc(props.src || '')
  if (!src) {
    return (
      <p key={`${segmentKey}-imgerr-${bi}`} className="actu-p actu-img-err">
        Image : <code>src:</code> manquant ou URL non autorisée (https ou chemin /…).
      </p>
    )
  }
  const alignRaw = String(props.align || 'center').toLowerCase()
  const alignClass =
    alignRaw === 'left' ? 'actu-figure--left' : alignRaw === 'right' ? 'actu-figure--right' : 'actu-figure--center'
  const imgStyle: CSSProperties = {}
  const w = escapeCssDim(props.width || '')
  const mw = escapeCssDim(props.maxwidth || props['max-width'] || '')
  const h = escapeCssDim(props.height || '')
  if (w) imgStyle.width = w
  if (mw) imgStyle.maxWidth = mw
  if (h) imgStyle.height = h
  const rounded = props.rounded || props.radius || ''
  if (rounded) {
    imgStyle.borderRadius = /^\d+$/.test(String(rounded)) ? `${rounded}px` : escapeCssDim(String(rounded)) || undefined
  }
  const fit = String(props.objectfit || props['object-fit'] || '').toLowerCase()
  if (/^(contain|cover|fill|none|scale-down)$/.test(fit)) imgStyle.objectFit = fit as CSSProperties['objectFit']
  const shadow = String(props.shadow || '').toLowerCase()
  if (shadow === 'sm') imgStyle.boxShadow = '0 2px 10px rgba(0,0,0,.22)'
  else if (shadow === 'md') imgStyle.boxShadow = '0 6px 20px rgba(0,0,0,.35)'
  const linkHref = sanitizeHref(props.link || props.href || '')
  const newTab = /^(1|true|yes|oui)$/i.test(String(props.newtab || props['new-tab'] || ''))
  const imgEl = <img className="actu-img__el" src={src} alt={props.alt || ''} loading="lazy" style={imgStyle} />
  const wrapped =
    linkHref ? (
      <a className="actu-img__link" href={linkHref} target={newTab ? '_blank' : undefined} rel="noopener noreferrer">
        {imgEl}
      </a>
    ) : (
      imgEl
    )
  const capRaw = props.caption?.trim()
  return (
    <figure key={`${segmentKey}-fig-${bi}`} className={`actu-figure ${alignClass}`}>
      {wrapped}
      {capRaw ? (
        <figcaption className="actu-figure__caption">{renderInlineActu(capRaw, `${segmentKey}-cap-${bi}`)}</figcaption>
      ) : null}
    </figure>
  )
}

/** Parse un bloc de texte multi-lignes (un segment / une bulle). */
export function renderActuSegmentBody(raw: string, segmentKey: string): ReactNode {
  const lines = raw.replace(/\r\n/g, '\n').split('\n')
  const blocks: ReactNode[] = []
  let i = 0
  let bi = 0
  while (i < lines.length) {
    const line = lines[i]!
    const t = line.trimEnd()
    const trimmed = t.trim()

    if (trimmed === '') {
      i++
      continue
    }

    if (trimmed === '___') {
      blocks.push(
        <hr key={`${segmentKey}-hr-${bi++}`} className="actu-hr" />
      )
      i++
      continue
    }

    if (trimmed === ':::img') {
      const imgBlock = parseImgDirectiveBlock(lines, i)
      if (imgBlock) {
        blocks.push(renderActuImgFigure(imgBlock.props, segmentKey, bi++))
        i = imgBlock.nextIndex
        continue
      }
      blocks.push(
        <p key={`${segmentKey}-imgbad-${bi++}`} className="actu-p actu-img-err">
          Bloc <code>:::img</code> incomplet : termine avec une ligne <code>:::</code> seule.
        </p>
      )
      i++
      continue
    }

    if (trimmed.startsWith('```')) {
      i++
      const codeLines: string[] = []
      while (i < lines.length) {
        if (lines[i]!.trim() === '```') {
          i++
          break
        }
        codeLines.push(lines[i]!)
        i++
      }
      blocks.push(
        <pre key={`${segmentKey}-pre-${bi++}`} className="actu-pre">
          <code>{codeLines.join('\n')}</code>
        </pre>
      )
      continue
    }

    if (trimmed.startsWith('### ')) {
      const content = trimmed.slice(4)
      const hk = `${segmentKey}-h3-${bi}`
      blocks.push(
        <h4 key={`${segmentKey}-h-${bi++}`} className="actu-h actu-h--3">
          {renderInlineActu(content, hk)}
        </h4>
      )
      i++
      continue
    }
    if (trimmed.startsWith('## ')) {
      const content = trimmed.slice(3)
      const hk = `${segmentKey}-h2-${bi}`
      blocks.push(
        <h3 key={`${segmentKey}-h-${bi++}`} className="actu-h actu-h--2">
          {renderInlineActu(content, hk)}
        </h3>
      )
      i++
      continue
    }
    /* `# Titre` ou `#Titre` (sans espace), mais pas `##`. */
    if (trimmed.startsWith('#') && !trimmed.startsWith('##')) {
      const content = trimmed.slice(1).trimStart()
      const hk = `${segmentKey}-h1-${bi}`
      blocks.push(
        <h2 key={`${segmentKey}-h-${bi++}`} className="actu-h actu-h--1">
          {renderInlineActu(content, hk)}
        </h2>
      )
      i++
      continue
    }

    if (trimmed.startsWith('- ')) {
      const items: string[] = []
      while (i < lines.length) {
        const L = lines[i]!.trimStart()
        if (!L.startsWith('- ')) break
        items.push(L.slice(2))
        i++
      }
      blocks.push(
        <ul key={`${segmentKey}-ul-${bi++}`} className="actu-ul">
          {items.map((it, j) => (
            <li key={j} className="actu-li">
              {renderInlineActu(it, `${segmentKey}-li-${bi}-${j}`)}
            </li>
          ))}
        </ul>
      )
      continue
    }

    if (trimmed.startsWith('>')) {
      const qs: string[] = []
      while (i < lines.length) {
        const L = lines[i]!.trimStart()
        if (!L.startsWith('>')) break
        qs.push(L.replace(/^>\s?/, ''))
        i++
      }
      blocks.push(
        <blockquote key={`${segmentKey}-bq-${bi++}`} className="actu-blockquote">
          {qs.map((q, j) => (
            <p key={j} className="actu-bq-line">
              {renderInlineActu(q, `${segmentKey}-bq-${bi}-${j}`)}
            </p>
          ))}
        </blockquote>
      )
      continue
    }

    const para: string[] = []
    while (i < lines.length) {
      const L = lines[i]!
      const tr = L.trim()
      if (tr === '') break
      if (tr === '___') break
      if (tr === ':::img') break
      if (tr.startsWith('```')) break
      if (tr.startsWith('#')) break
      if (tr.startsWith('- ')) break
      if (tr.startsWith('>')) break
      para.push(L)
      i++
    }
    const ptext = para.join('\n')
    blocks.push(
      <p key={`${segmentKey}-p-${bi++}`} className="actu-p">
        {ptext.split('\n').map((ln, j) => (
          <Fragment key={j}>
            {j > 0 ? <br /> : null}
            {renderInlineActu(ln, `${segmentKey}-pl-${bi}-${j}`)}
          </Fragment>
        ))}
      </p>
    )
  }

  return <div className="actu-body">{blocks}</div>
}
