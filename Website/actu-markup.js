/**
 * Rendu HTML du mini-langage actu (aligné sur le launcher : parseActuMarkup).
 * Utilisé par le site vitrine et la page admin (aperçu).
 */
;(function (global) {
  const MC_COLORS = {
    '0': '#000000',
    '1': '#0000aa',
    '2': '#00aa00',
    '3': '#00aaaa',
    '4': '#aa0000',
    '5': '#aa00aa',
    '6': '#ffaa00',
    '7': '#aaaaaa',
    '8': '#555555',
    '9': '#5555ff',
    a: '#55ff55',
    b: '#55ffff',
    c: '#ff5555',
    d: '#ff55ff',
    e: '#ffff55',
    f: '#ffffff',
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
  }

  /** href autorisés pour <a> et liens sur images. */
  function sanitizeHref(href) {
    const t = String(href).trim()
    if (!t) return ''
    const lower = t.toLowerCase()
    if (lower.startsWith('javascript:') || lower.startsWith('data:')) return ''
    if (/^https?:\/\//i.test(t)) return t
    if (t.startsWith('/') && !t.startsWith('//')) return t
    if (lower.startsWith('mailto:')) return t
    return ''
  }

  /** src autorisés pour <img>. */
  function sanitizeImgSrc(src) {
    const t = String(src).trim()
    if (!t) return ''
    const lower = t.toLowerCase()
    if (lower.startsWith('javascript:') || lower.startsWith('data:')) return ''
    if (/^https?:\/\//i.test(t)) return t
    if (t.startsWith('/') && !t.startsWith('//')) return t
    return ''
  }

  /** Valeur CSS pour width / max-width / height (pas de ; ni guillemets). */
  function escapeCssDim(v) {
    const s = String(v).trim().slice(0, 96)
    if (!s || /[<>"';\\]/.test(s)) return ''
    return s
  }

  /**
   * Découpe le texte en morceaux texte + liens markdown [libellé](url).
   * Le libellé est re-parsé avec le markdown léger (gras, §, etc.).
   */
  function splitMarkdownLinks(text) {
    const out = []
    const re = /\[([^\]]*)\]\(([^)]+)\)/g
    let last = 0
    let m
    while ((m = re.exec(text)) !== null) {
      if (m.index > last) out.push({ kind: 'text', text: text.slice(last, m.index) })
      out.push({ kind: 'link', label: m[1], href: m[2] })
      last = re.lastIndex
    }
    if (last < text.length) out.push({ kind: 'text', text: text.slice(last) })
    if (out.length === 0) out.push({ kind: 'text', text })
    return out
  }

  function styleReset() {
    return { color: '', bold: false, italic: false, underline: false, strike: false }
  }

  function applyMcCode(prev, code) {
    const c = String(code).toLowerCase()
    if (c === 'r') return styleReset()
    if (c === 'l') return { ...prev, bold: true }
    if (c === 'o') return { ...prev, italic: true }
    if (c === 'm') return { ...prev, strike: true }
    if (c === 'n') return { ...prev, underline: true }
    if (MC_COLORS[c]) return { ...prev, color: MC_COLORS[c] }
    return prev
  }

  function styleToAttr(st) {
    const parts = []
    if (st.color) parts.push(`color:${st.color}`)
    if (st.bold) parts.push('font-weight:700')
    if (st.italic) parts.push('font-style:italic')
    const deco = []
    if (st.underline) deco.push('underline')
    if (st.strike) deco.push('line-through')
    if (deco.length) parts.push(`text-decoration:${deco.join(' ')}`)
    return parts.length ? ` style="${parts.join(';')}"` : ''
  }

  function renderMcSpansToHtml(text) {
    if (!text) return ''
    let style = styleReset()
    let buf = ''
    let out = ''
    const flush = () => {
      if (!buf) return
      out += `<span${styleToAttr(style)}>${escapeHtml(buf)}</span>`
      buf = ''
    }
    for (let i = 0; i < text.length; i++) {
      if (text[i] === '§' && i + 1 < text.length) {
        flush()
        style = applyMcCode(style, text[i + 1])
        i++
        continue
      }
      buf += text[i]
    }
    flush()
    return out
  }

  /** ** ~~ ` * ++ ||spoiler|| */
  function parseMarkdownChunks(text) {
    const out = []
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
      const tryIdx = (p) => {
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

  function wrapMdChunkToHtml(chunk) {
    const inner = renderMcSpansToHtml(chunk.text)
    switch (chunk.kind) {
      case 'bold':
        return `<strong>${inner}</strong>`
      case 'italic':
        return `<em>${inner}</em>`
      case 'code':
        return `<code class="actu-inline-code">${escapeHtml(chunk.text)}</code>`
      case 'strike':
        return `<s class="actu-inline-strike">${inner}</s>`
      case 'highlight':
        return `<mark class="actu-inline-mark">${inner}</mark>`
      case 'spoiler':
        return `<span class="actu-spoiler" title="Hover / survol pour afficher"><span class="actu-spoiler__inner">${inner}</span></span>`
      default:
        return inner
    }
  }

  function renderInlineToHtml(text) {
    return splitMarkdownLinks(text)
      .map((part) => {
        if (part.kind === 'link') {
          const href = sanitizeHref(part.href)
          const inner = parseMarkdownChunks(part.label).map(wrapMdChunkToHtml).join('')
          if (!href) return inner
          return `<a class="actu-link" href="${escapeHtml(href)}" target="_blank" rel="noopener noreferrer">${inner}</a>`
        }
        return parseMarkdownChunks(part.text).map(wrapMdChunkToHtml).join('')
      })
      .join('')
  }

  function parseImgDirectiveBlock(lines, startIdx) {
    if (lines[startIdx].trim() !== ':::img') return null
    const props = Object.create(null)
    let j = startIdx + 1
    while (j < lines.length) {
      const tr = lines[j].trim()
      if (tr === ':::') {
        return { html: renderImgFigureHtml(props), nextIndex: j + 1 }
      }
      const m = /^([a-zA-Z][\w-]*)\s*:\s*(.*)$/.exec(lines[j].trim())
      if (m) props[m[1].toLowerCase()] = m[2].trim()
      j++
    }
    return null
  }

  function renderImgFigureHtml(props) {
    const rawSrc = props.src || ''
    const src = sanitizeImgSrc(rawSrc)
    if (!src) {
      return '<p class="actu-p actu-img-err">Image : propriété <code>src:</code> manquante ou URL non autorisée (https ou chemin absolu /…).</p>'
    }
    const alt = escapeHtml(props.alt || '')
    const align = String(props.align || 'center')
      .toLowerCase()
      .replace(/[^a-z]/g, '')
    const alignClass =
      align === 'left' ? 'actu-figure--left' : align === 'right' ? 'actu-figure--right' : 'actu-figure--center'
    const imgSt = []
    const w = escapeCssDim(props.width || '')
    const mw = escapeCssDim(props.maxwidth || props['max-width'] || '')
    const h = escapeCssDim(props.height || '')
    if (w) imgSt.push(`width:${w}`)
    if (mw) imgSt.push(`max-width:${mw}`)
    if (h) imgSt.push(`height:${h}`)
    const rounded = props.rounded || props.radius || ''
    if (rounded) {
      const r = /^\d+$/.test(String(rounded)) ? `${rounded}px` : escapeCssDim(String(rounded))
      if (r) imgSt.push(`border-radius:${r}`)
    }
    const fit = String(props.objectfit || props['object-fit'] || '').toLowerCase()
    if (/^(contain|cover|fill|none|scale-down)$/.test(fit)) imgSt.push(`object-fit:${fit}`)
    const shadow = String(props.shadow || '').toLowerCase()
    if (shadow === 'sm') imgSt.push('box-shadow:0 2px 10px rgba(0,0,0,.22)')
    else if (shadow === 'md') imgSt.push('box-shadow:0 6px 20px rgba(0,0,0,.35)')
    const imgTag = `<img class="actu-img__el" src="${escapeHtml(src)}" alt="${alt}" loading="lazy"${
      imgSt.length ? ` style="${imgSt.join(';')}"` : ''
    } />`
    const linkHref = sanitizeHref(props.link || props.href || '')
    const newTab = /^(1|true|yes|oui)$/i.test(String(props.newtab || props['new-tab'] || ''))
    let inner = imgTag
    if (linkHref) {
      const tgt = newTab ? ' target="_blank"' : ''
      inner = `<a class="actu-img__link" href="${escapeHtml(linkHref)}" rel="noopener noreferrer"${tgt}>${imgTag}</a>`
    }
    let cap = ''
    if (props.caption && String(props.caption).trim()) {
      cap = `<figcaption class="actu-figure__caption">${renderInlineToHtml(String(props.caption).trim())}</figcaption>`
    }
    const figClass = `actu-figure ${alignClass}`
    return `<figure class="${figClass}">${inner}${cap}</figure>`
  }

  function renderBlockBodyToHtml(raw) {
    const lines = String(raw).replace(/\r\n/g, '\n').split('\n')
    const blocks = []
    let i = 0
    let bi = 0
    while (i < lines.length) {
      const line = lines[i]
      const trimmed = line.trim()

      if (trimmed === '') {
        i++
        continue
      }

      if (trimmed === '___') {
        blocks.push(`<hr class="actu-hr" />`)
        i++
        continue
      }

      if (trimmed === ':::img') {
        const imgBlock = parseImgDirectiveBlock(lines, i)
        if (imgBlock) {
          blocks.push(imgBlock.html)
          i = imgBlock.nextIndex
          continue
        }
        blocks.push(
          '<p class="actu-p actu-img-err">Bloc <code>:::img</code> incomplet : termine avec une ligne <code>:::</code> seule.</p>'
        )
        i++
        continue
      }

      if (trimmed.startsWith('```')) {
        i++
        const codeLines = []
        while (i < lines.length) {
          if (lines[i].trim() === '```') {
            i++
            break
          }
          codeLines.push(lines[i])
          i++
        }
        blocks.push(
          `<pre class="actu-pre"><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`,
        )
        continue
      }

      if (trimmed.startsWith('### ')) {
        blocks.push(
          `<h4 class="actu-h actu-h--3">${renderInlineToHtml(trimmed.slice(4))}</h4>`,
        )
        i++
        continue
      }
      if (trimmed.startsWith('## ')) {
        blocks.push(
          `<h3 class="actu-h actu-h--2">${renderInlineToHtml(trimmed.slice(3))}</h3>`,
        )
        i++
        continue
      }
      if (trimmed.startsWith('#') && !trimmed.startsWith('##')) {
        blocks.push(
          `<h2 class="actu-h actu-h--1">${renderInlineToHtml(trimmed.replace(/^#\s*/, ''))}</h2>`,
        )
        i++
        continue
      }

      if (trimmed.startsWith('- ')) {
        const items = []
        while (i < lines.length) {
          const L = lines[i].trimStart()
          if (!L.startsWith('- ')) break
          items.push(L.slice(2))
          i++
        }
        blocks.push(
          `<ul class="actu-ul">${items
            .map((it) => `<li class="actu-li">${renderInlineToHtml(it)}</li>`)
            .join('')}</ul>`,
        )
        continue
      }

      if (trimmed.startsWith('>')) {
        const qs = []
        while (i < lines.length) {
          const L = lines[i].trimStart()
          if (!L.startsWith('>')) break
          qs.push(L.replace(/^>\s?/, ''))
          i++
        }
        blocks.push(
          `<blockquote class="actu-blockquote">${qs
            .map((q) => `<p class="actu-bq-line">${renderInlineToHtml(q)}</p>`)
            .join('')}</blockquote>`,
        )
        continue
      }

      const para = []
      while (i < lines.length) {
        const L = lines[i]
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
      const inner = ptext
        .split('\n')
        .map((ln, j) => (j ? '<br />' : '') + renderInlineToHtml(ln))
        .join('')
      blocks.push(`<p class="actu-p">${inner}</p>`)
      bi++
    }
    return `<div class="actu-body">${blocks.join('')}</div>`
  }

  /**
   * Un segment complet (ex. "## Titre\n\nCorps…") → HTML sûr pour carte actu.
   */
  function renderSegmentCardHtml(segment) {
    const raw = String(segment).trim()
    const lines = raw.split('\n')
    let titleHtml = ''
    let bodyRaw = raw
    if (lines[0] && /^##\s+/.test(lines[0].trim())) {
      const t = lines[0].replace(/^##\s+/, '').trim()
      titleHtml = `<h3 class="live-news-card__title actu-h actu-h--2">${renderInlineToHtml(t)}</h3>`
      bodyRaw = lines.slice(1).join('\n').replace(/^\s*\n/, '')
    }
    const bodyHtml = renderBlockBodyToHtml(bodyRaw)
    return `${titleHtml}<div class="live-news-card__body">${bodyHtml}</div>`
  }

  global.SoleaActuMarkup = {
    escapeHtml,
    renderInlineToHtml,
    renderBlockBodyToHtml,
    renderSegmentCardHtml,
  }
})(typeof window !== 'undefined' ? window : globalThis)
