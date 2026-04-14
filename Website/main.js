/**
 * Solea Pixel — site vitrine (releases GitHub, scroll plein écran, machine à écrire).
 */
const CONFIG = {
  owner: 'SILWOX',
  repo: 'solea-pixel-launcher',
  get apiLatest() {
    return `https://api.github.com/repos/${this.owner}/${this.repo}/releases/latest`
  },
  get releasesLatestUrl() {
    return `https://github.com/${this.owner}/${this.repo}/releases/latest`
  },
}

let activeSectionId = 'hub'

const TYPEWRITER_WORDS = [
  'MYTHIC TRIALS',
  'PALAMOD RECREATED',
  'SOLEA OPTIMISED',
  'THE END OF WITHER STORM',
  'LAUNCHER OPTIMISÉ',
  'AELORIA',
  'SERVEUR 100% FREE',
]

let cachedReleaseVersion = ''
let releaseFetchResolved = false

function pickWindowsSetupAsset(assets) {
  if (!assets?.length) return null
  const setup = assets.find(
    (a) =>
      /\.exe$/i.test(a.name) &&
      (/setup/i.test(a.name) || /Solea-Pixel-Setup/i.test(a.name)),
  )
  if (setup) return setup
  const anyExe = assets.find((a) => /\.exe$/i.test(a.name))
  return anyExe ?? null
}

function normalizeVersion(tag) {
  if (!tag) return ''
  return tag.replace(/^v/i, '')
}

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function applyHubVersionLine() {
  const versionEl = document.getElementById('hub-version')
  if (!versionEl || !window.SoleaI18n) return
  const t = window.SoleaI18n.t
  if (!releaseFetchResolved) {
    versionEl.textContent = t('hub.versionLoading')
    versionEl.classList.add('hub__version--pending')
    return
  }
  versionEl.classList.remove('hub__version--pending', 'hub__version--slow')
  if (!cachedReleaseVersion) {
    versionEl.textContent = t('hub.versionFallback')
    return
  }
  const raw = t('hub.versionOk').replace(/\{v\}/g, escapeHtml(cachedReleaseVersion))
  versionEl.innerHTML = raw.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

function replaceLocationHash(hashNoPound) {
  try {
    const u = new URL(window.location.href)
    u.hash = hashNoPound ? `#${hashNoPound}` : ''
    history.replaceState(null, '', `${u.pathname}${u.search}${u.hash}`)
  } catch {
    history.replaceState(null, '', hashNoPound ? `#${hashNoPound}` : '')
  }
}

async function loadLatestRelease() {
  const btn = document.getElementById('btn-download-win')
  const versionEl = document.getElementById('hub-version')
  const statVersion = document.getElementById('stat-version')
  const t = (k) => (window.SoleaI18n ? window.SoleaI18n.t(k) : k)

  releaseFetchResolved = false
  if (versionEl) {
    versionEl.classList.add('hub__version--pending')
    versionEl.classList.remove('hub__version--slow')
  }

  const slowTimer = window.setTimeout(() => {
    if (releaseFetchResolved || !versionEl || !window.SoleaI18n) return
    versionEl.classList.add('hub__version--slow')
    versionEl.textContent = window.SoleaI18n.t('hub.versionSlow')
  }, 1600)

  const fallback = () => {
    window.clearTimeout(slowTimer)
    releaseFetchResolved = true
    cachedReleaseVersion = ''
    if (btn) btn.href = CONFIG.releasesLatestUrl
    if (versionEl) {
      versionEl.classList.remove('hub__version--pending', 'hub__version--slow')
      versionEl.textContent = t('hub.versionFallback')
    }
    if (statVersion) statVersion.textContent = '—'
  }

  try {
    const res = await fetch(CONFIG.apiLatest, {
      headers: { Accept: 'application/vnd.github+json', 'X-GitHub-Api-Version': '2022-11-28' },
    })
    if (!res.ok) {
      fallback()
      return
    }
    const data = await res.json()
    const asset = pickWindowsSetupAsset(data.assets ?? [])
    const version = normalizeVersion(data.tag_name)
    cachedReleaseVersion = version || '—'
    releaseFetchResolved = true
    window.clearTimeout(slowTimer)

    if (asset?.browser_download_url && btn) btn.href = asset.browser_download_url
    else if (btn) btn.href = CONFIG.releasesLatestUrl

    applyHubVersionLine()
    if (statVersion) statVersion.textContent = version || '—'
  } catch {
    fallback()
  }
}

function initTilt() {
  const wrap = document.getElementById('tilt-wrap')
  const inner = document.getElementById('tilt-inner')
  if (!wrap || !inner) return

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  let ax = 0
  let ay = 0
  let px = 0
  let py = 0
  let pointerInside = false
  let raf = 0

  const tick = () => {
    const toward = pointerInside ? 0.16 : 0.09
    px += (ax - px) * toward
    py += (ay - py) * toward

    const hover = wrap.matches(':hover')
    const maxDeg = hover ? 9 : 6.5
    const rotX = -py * 2 * maxDeg
    const rotY = px * 2 * maxDeg

    const pop = hover ? 1.055 : 1
    const micro = 1 + Math.min(0.022, Math.hypot(px, py) * 0.04)
    const scale = pop * micro
    const tz = hover ? 20 : 6 + 14 * Math.min(1, Math.hypot(px, py) * 2)

    inner.style.transform = `rotateX(${rotX}deg) rotateY(${rotY}deg) translateZ(${tz}px) scale3d(${scale}, ${scale}, ${scale})`

    const settled = Math.abs(px) < 0.004 && Math.abs(py) < 0.004
    if (settled && !hover) {
      inner.style.transform = ''
      raf = 0
      return
    }
    raf = requestAnimationFrame(tick)
  }

  const schedule = () => {
    if (!raf) raf = requestAnimationFrame(tick)
  }

  const setPointer = (e) => {
    if (!e.isPrimary) return
    const r = wrap.getBoundingClientRect()
    if (r.width < 1 || r.height < 1) return
    ax = (e.clientX - r.left) / r.width - 0.5
    ay = (e.clientY - r.top) / r.height - 0.5
    pointerInside = true
    schedule()
  }

  wrap.addEventListener('pointerenter', () => {
    pointerInside = true
    schedule()
  })

  wrap.addEventListener('pointermove', setPointer, { passive: true })

  const release = () => {
    pointerInside = false
    ax = 0
    ay = 0
    schedule()
  }

  wrap.addEventListener('pointerleave', release)
  wrap.addEventListener('pointercancel', release)
}

function scrollSectionToTop(el, root, behavior) {
  const rr = root.getBoundingClientRect()
  const er = el.getBoundingClientRect()
  const nextTop = root.scrollTop + (er.top - rr.top)
  root.scrollTo({ top: Math.max(0, nextTop), behavior })
}

function openLegalDialog() {
  const dialog = document.getElementById('legal-dialog')
  if (!dialog) return
  if (typeof dialog.showModal === 'function') {
    dialog.showModal()
  } else {
    dialog.setAttribute('open', '')
  }
  dialog.querySelector('.legal-dialog__close')?.focus()
}

function initLegalDialog() {
  const dialog = document.getElementById('legal-dialog')
  const frame = dialog?.querySelector('.legal-dialog__frame')
  if (!dialog || !frame) return

  frame.addEventListener('click', (e) => {
    if (e.target === frame) dialog.close()
  })

  dialog.querySelectorAll('[data-legal-dismiss]').forEach((btn) => {
    btn.addEventListener('click', () => dialog.close())
  })

  document.body.addEventListener('click', (e) => {
    const trigger = e.target.closest('a.js-legal-modal-open')
    if (!trigger) return
    if (e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || e.button !== 0) return
    e.preventDefault()
    openLegalDialog()
  })
}

function openSmartscreenHelpDialog() {
  const dialog = document.getElementById('smartscreen-help-dialog')
  if (!dialog) return
  if (typeof dialog.showModal === 'function') {
    dialog.showModal()
  } else {
    dialog.setAttribute('open', '')
  }
  dialog.querySelector('.legal-dialog__close')?.focus()
}

function initSmartscreenHelpDialog() {
  const dialog = document.getElementById('smartscreen-help-dialog')
  const frame = dialog?.querySelector('.legal-dialog__frame')
  if (!dialog || !frame) return

  frame.addEventListener('click', (e) => {
    if (e.target === frame) dialog.close()
  })

  dialog.querySelectorAll('[data-smartscreen-dismiss]').forEach((btn) => {
    btn.addEventListener('click', () => dialog.close())
  })

  document.getElementById('btn-smartscreen-more')?.addEventListener('click', () => {
    openSmartscreenHelpDialog()
  })
}

function initSnapPage() {
  const root = document.querySelector('.snap-root')
  if (!root) return

  if ('scrollRestoration' in history) {
    history.scrollRestoration = 'manual'
  }

  const sections = [...root.querySelectorAll('.site-section[id]')]
  const navLinks = [...document.querySelectorAll('.nav-links a[href^="#"]')]
  const fabTop = document.getElementById('fab-top')

  let lockNavSyncUntil = 0

  const setActiveNav = (id) => {
    activeSectionId = id || 'hub'
    document.documentElement.dataset.activeSection = activeSectionId
    navLinks.forEach((a) => {
      const h = a.getAttribute('href')
      a.classList.toggle('is-active', h === `#${id}`)
    })
  }

  const updateFabVisibility = () => {
    if (!fabTop) return
    const show = root.scrollTop > 180
    fabTop.hidden = !show
  }

  const scrollToId = (id, behavior = 'smooth') => {
    const el = document.getElementById(id)
    if (!el || !root.contains(el)) return
    const duration = behavior === 'smooth' ? 820 : 140
    lockNavSyncUntil = performance.now() + duration
    setActiveNav(id)
    scrollSectionToTop(el, root, behavior)
    replaceLocationHash(id)
    updateFabVisibility()
  }

  document.querySelectorAll('a[href^="#"]').forEach((a) => {
    const href = a.getAttribute('href')
    if (!href || href === '#') return
    const id = href.slice(1)
    const target = document.getElementById(id)
    if (!target || !root.contains(target)) return
    a.addEventListener('click', (e) => {
      if (e.ctrlKey || e.metaKey || e.shiftKey || e.altKey || e.button !== 0) return
      e.preventDefault()
      scrollToId(id, 'smooth')
    })
  })

  const syncFromScroll = () => {
    if (performance.now() < lockNavSyncUntil) return
    const maxScroll = Math.max(0, root.scrollHeight - root.clientHeight)
    if (sections.length && root.scrollTop >= maxScroll - 6) {
      setActiveNav(sections[sections.length - 1].id)
      return
    }
    const rr = root.getBoundingClientRect()
    let best = sections[0]
    let bestVis = -1
    for (const s of sections) {
      const sr = s.getBoundingClientRect()
      const top = Math.max(sr.top, rr.top)
      const bottom = Math.min(sr.bottom, rr.bottom)
      const vis = Math.max(0, bottom - top)
      if (vis > bestVis) {
        bestVis = vis
        best = s
      }
    }
    if (best?.id) setActiveNav(best.id)
    updateFabVisibility()
  }

  root.addEventListener(
    'scroll',
    () => {
      requestAnimationFrame(syncFromScroll)
    },
    { passive: true },
  )

  fabTop?.addEventListener('click', () => {
    const first = sections[0]
    if (first) scrollToId(first.id, 'smooth')
    else root.scrollTo({ top: 0, behavior: 'smooth' })
  })

  const hasValidSectionHash = (hashRaw) => {
    if (!hashRaw || hashRaw === 'legal') return false
    const el = document.getElementById(hashRaw)
    return Boolean(el && root.contains(el))
  }

  const snapToHomeUnlessDeepLink = () => {
    const raw = (location.hash || '').replace(/^#/, '')
    if (raw === 'legal') return
    if (hasValidSectionHash(raw)) return
    root.scrollTop = 0
    replaceLocationHash('hub')
    setActiveNav('hub')
    updateFabVisibility()
  }

  const applyInitialRoute = () => {
    const raw = (location.hash || '').replace(/^#/, '')
    if (raw === 'legal') {
      openLegalDialog()
      replaceLocationHash('hub')
      root.scrollTop = 0
      setActiveNav('hub')
      updateFabVisibility()
      return
    }
    const valid = hasValidSectionHash(raw)

    if (!valid) {
      root.scrollTop = 0
      replaceLocationHash('hub')
      setActiveNav('hub')
      updateFabVisibility()
      return
    }

    scrollToId(raw, 'auto')
  }

  applyInitialRoute()

  window.addEventListener('load', () => {
    snapToHomeUnlessDeepLink()
  })
  window.addEventListener('pageshow', () => {
    snapToHomeUnlessDeepLink()
  })
  window.addEventListener('hashchange', () => {
    const raw = (location.hash || '').replace(/^#/, '')
    if (!raw) {
      root.scrollTop = 0
      replaceLocationHash('hub')
      setActiveNav('hub')
      updateFabVisibility()
      return
    }
    if (raw === 'legal') {
      openLegalDialog()
      replaceLocationHash(activeSectionId || 'hub')
      return
    }
    const el = document.getElementById(raw)
    if (el && root.contains(el)) scrollToId(raw, 'smooth')
  })
}

function initTypewriter() {
  const el = document.getElementById('typewriter-target')
  if (!el) return

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    el.textContent = TYPEWRITER_WORDS.join(' · ')
    return
  }

  let wi = 0
  let pos = 0
  let pause = 0
  let del = false

  window.setInterval(() => {
    if (document.hidden) return
    if (pause > 0) {
      pause--
      return
    }
    const w = TYPEWRITER_WORDS[wi % TYPEWRITER_WORDS.length]
    if (!del) {
      pos++
      el.textContent = w.slice(0, pos)
      if (pos >= w.length) {
        del = true
        pause = 28
      }
    } else {
      pos--
      el.textContent = w.slice(0, Math.max(0, pos))
      if (pos <= 0) {
        del = false
        wi++
        pause = 10
      }
    }
  }, 42)
}

function initSectionInview(sectionId, inviewClass) {
  const el = document.getElementById(sectionId)
  if (!el) return

  const mark = () => {
    el.classList.add(inviewClass)
  }

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    mark()
    return
  }

  const root = document.querySelector('.snap-root')
  const io = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) {
        mark()
        io.disconnect()
      }
    },
    { root: root ?? undefined, threshold: 0.08, rootMargin: '0px 0px -6% 0px' },
  )

  queueMicrotask(() => {
    io.observe(el)
  })

  window.setTimeout(() => {
    if (el.classList.contains(inviewClass)) return
    const r = el.getBoundingClientRect()
    const vh = window.innerHeight || 0
    if (r.top < vh * 0.92 && r.bottom > vh * 0.08) mark()
  }, 450)
}

function initFaqAriaExpanded() {
  document.querySelectorAll('.faq__item').forEach((details) => {
    const sum = details.querySelector('summary')
    if (!sum) return
    const sync = () => {
      sum.setAttribute('aria-expanded', details.open ? 'true' : 'false')
    }
    sync()
    details.addEventListener('toggle', sync)
  })
}

document.addEventListener('DOMContentLoaded', () => {
  if (window.SoleaI18n) window.SoleaI18n.init()
  initLegalDialog()
  initSmartscreenHelpDialog()
  void loadLatestRelease()
  initTilt()
  initSnapPage()
  initFaqAriaExpanded()
  initSectionInview('hub', 'hub--inview')
  initSectionInview('features', 'features--inview')
  initSectionInview('news', 'news--inview')
  initSectionInview('downloads', 'downloads--inview')
  initSectionInview('faq', 'faq--inview')
  initTypewriter()

  window.addEventListener('solea-lang-change', () => {
    applyHubVersionLine()
  })
})
