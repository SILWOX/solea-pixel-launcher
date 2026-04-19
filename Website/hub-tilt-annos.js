/**
 * Accueil — étiquettes mockup : i18n aléatoire, palette, positions sans chevauchement.
 */
;(function () {
  const POOL_SIZE = 10
  const PALETTES = ['accent', 'blue', 'violet', 'green', 'gold']
  const FLOAT_CLASSES = ['tilt-anno--float-a', 'tilt-anno--float-b', 'tilt-anno--float-c']
  /** Marge minimale entre deux cartes (px, coordonnées viewport) */
  const OVERLAP_PAD = 12

  const POS = [
    { top: '7%', left: '-4%', right: 'auto', bottom: 'auto' },
    { top: '12%', left: '1%', right: 'auto', bottom: 'auto' },
    { top: '22%', left: '-2%', right: 'auto', bottom: 'auto' },
    { top: '18%', left: 'auto', right: '-3%', bottom: 'auto' },
    { top: '30%', left: 'auto', right: '-5%', bottom: 'auto' },
    { top: '38%', left: 'auto', right: '-2%', bottom: 'auto' },
    { top: '46%', left: '-3%', right: 'auto', bottom: 'auto' },
    { top: '52%', left: 'auto', right: '-4%', bottom: 'auto' },
    { top: '62%', left: '-1%', right: 'auto', bottom: 'auto' },
    { top: 'auto', left: '-2%', right: 'auto', bottom: '14%' },
    { top: 'auto', left: 'auto', right: '-5%', bottom: '11%' },
    { top: 'auto', left: '3%', right: 'auto', bottom: '8%' },
  ]

  function shuffle(arr) {
    const a = [...arr]
    for (let i = a.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[a[i], a[j]] = [a[j], a[i]]
    }
    return a
  }

  function pickUnique(n, max) {
    return shuffle([...Array(max).keys()]).slice(0, n)
  }

  function applyPosition(el, spec) {
    el.style.top = spec.top
    el.style.right = spec.right
    el.style.bottom = spec.bottom
    el.style.left = spec.left
  }

  function clearNudge(el) {
    el.style.marginTop = ''
    el.style.marginLeft = ''
    delete el.dataset.vBump
    delete el.dataset.hBump
  }

  function setFloatClass(el, cls) {
    FLOAT_CLASSES.forEach((c) => el.classList.remove(c))
    el.classList.add(cls)
  }

  function rectRel(el, container) {
    const er = el.getBoundingClientRect()
    const cr = container.getBoundingClientRect()
    return {
      left: er.left - cr.left,
      top: er.top - cr.top,
      right: er.right - cr.left,
      bottom: er.bottom - cr.top,
    }
  }

  function overlaps(a, b, pad) {
    const p = pad ?? 0
    return !(
      a.right + p <= b.left ||
      a.left - p >= b.right ||
      a.bottom + p <= b.top ||
      a.top - p >= b.bottom
    )
  }

  function collidesWithAny(rect, others, pad) {
    for (const o of others) {
      if (overlaps(rect, o, pad)) return true
    }
    return false
  }

  /**
   * Choisit une position par carte sans recouvrement (mesure réelle post-texte).
   */
  function placeWithoutOverlap(slots, root) {
    for (const el of slots) clearNudge(el)

    const placedRects = []
    const tryOrderBase = shuffle([...Array(POS.length).keys()])

    for (let si = 0; si < slots.length; si += 1) {
      const el = slots[si]
      const tryOrder = si === 0 ? tryOrderBase : shuffle([...Array(POS.length).keys()])
      let ok = false

      for (const pi of tryOrder) {
        applyPosition(el, POS[pi])
        void root.offsetHeight
        const r = rectRel(el, root)
        if (!collidesWithAny(r, placedRects, OVERLAP_PAD)) {
          placedRects.push(r)
          ok = true
          break
        }
      }

      if (!ok) {
        applyPosition(el, POS[(si * 5) % POS.length])
        void root.offsetHeight
        let r = rectRel(el, root)
        let bump = 0
        while (collidesWithAny(r, placedRects, OVERLAP_PAD) && bump < 160) {
          bump += 14
          el.style.marginTop = `${bump}px`
          void root.offsetHeight
          r = rectRel(el, root)
        }
        placedRects.push(r)
      }
    }

    /** Passe finale : éloigne encore les paires qui se touchent (texte long, resize) */
    for (let pass = 0; pass < 24; pass += 1) {
      void root.offsetHeight
      let fixed = false
      for (let i = 0; i < slots.length; i += 1) {
        for (let j = i + 1; j < slots.length; j += 1) {
          const ri = rectRel(slots[i], root)
          const rj = rectRel(slots[j], root)
          if (!overlaps(ri, rj, OVERLAP_PAD)) continue
          fixed = true
          const dy = rj.top <= ri.top ? -12 : 12
          const cur = parseFloat(slots[j].dataset.vBump || '0')
          const next = cur + dy
          slots[j].dataset.vBump = String(next)
          slots[j].style.marginTop = `${next}px`
        }
      }
      if (!fixed) break
    }
  }

  function refreshTexts(slots) {
    const t = window.SoleaI18n?.t
    if (!t) return
    for (const el of slots) {
      const id = el.dataset.poolId
      if (id == null) continue
      const label = el.querySelector('.tilt-anno__label')
      const text = el.querySelector('.tilt-anno__text')
      if (label) label.textContent = t(`tilt.pool.${id}.title`)
      if (text) text.textContent = t(`tilt.pool.${id}.sub`)
    }
  }

  function init() {
    const root = document.getElementById('tilt-inner')
    if (!root) return
    const slots = [...root.querySelectorAll('[data-tilt-anno-slot]')]
    if (slots.length !== 3) return

    const poolIds = pickUnique(3, POOL_SIZE)
    const floatOrder = shuffle([...FLOAT_CLASSES])

    slots.forEach((el, i) => {
      el.dataset.poolId = String(poolIds[i])
      el.dataset.tiltPal = PALETTES[Math.floor(Math.random() * PALETTES.length)]
      setFloatClass(el, floatOrder[i])
    })

    refreshTexts(slots)

    const runLayout = () => placeWithoutOverlap(slots, root)
    runLayout()
    requestAnimationFrame(runLayout)
    requestAnimationFrame(() => requestAnimationFrame(runLayout))

    window.addEventListener('solea-lang-change', () => {
      refreshTexts(slots)
      requestAnimationFrame(runLayout)
    })

    let resizeT = 0
    window.addEventListener(
      'resize',
      () => {
        window.clearTimeout(resizeT)
        resizeT = window.setTimeout(runLayout, 120)
      },
      { passive: true },
    )
  }

  init()
})()
