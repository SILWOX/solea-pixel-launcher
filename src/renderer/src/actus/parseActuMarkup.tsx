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

/** Découpe `**`, `*`, `` ` ``, `~~`, `++`, puis applique § sur chaque morceau. */
export function renderInlineActu(text: string, keyPrefix: string): ReactNode {
  if (!text) return null
  const parts = parseMarkdownChunks(text)
  return (
    <>
      {parts.map((chunk, i) => wrapMdChunk(chunk.kind, chunk.text, `${keyPrefix}-w${i}`, `${keyPrefix}-mc${i}`))}
    </>
  )
}

type MdKind = 'text' | 'bold' | 'italic' | 'code' | 'strike' | 'highlight'

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
  return <Fragment key={key}>{inner}</Fragment>
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
