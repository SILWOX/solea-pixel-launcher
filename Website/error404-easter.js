/**
 * Easter egg 404 : 404 doré → modale Rick Roll → (après fermeture) fusée → compte à rebours → mode glitch / fissures → trou noir → reset.
 */
;(function () {
  const btn = document.getElementById('err-golden-404')
  const dialog = document.getElementById('err-easter-dialog')
  const claimBtn = document.getElementById('err-easter-claim')
  const dismissBtn = document.getElementById('err-easter-dismiss')
  const rocket = document.getElementById('err-easter-rocket')
  const voidStage = document.getElementById('err-void-stage')
  const voidHole = document.getElementById('err-void-hole')
  const voidReset = document.getElementById('err-void-reset')
  const voidStars = document.getElementById('err-void-stars')
  const voidMemeLayer = document.getElementById('err-void-meme-layer')
  if (!btn || !dialog || !claimBtn || !dismissBtn || !rocket || !voidStage || !voidHole || !voidReset || !voidStars || !voidMemeLayer) return

  const RICK = 'https://www.youtube.com/watch?v=dQw4w9WgXcQ'
  const mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)')
  const SPAWN_MIN_MS = 5000
  const SPAWN_MAX_MS = 10000
  const PHASE2_MIN_MS = 10000
  const PHASE2_MAX_MS = 15000
  const ROCKET_CATCH_R = 26
  const COUNTDOWN_SEC = 5
  /** Entrée depuis hors-écran (ms) — trajectoire courbe, plus lente que la phase poursuite */
  const ROCKET_ENTRY_MS = 2100
  /** Vitesse max vers le curseur après l’entrée (px/s) — vol intentionnellement lent */
  const ROCKET_HOMING_SPEED = 195
  const ROCKET_HOMING_NEAR = 0.072

  const VOID_SEQUENCE_MS = 15000
  const MEME_SPAWN_MIN_MS = 3200
  const MEME_SPAWN_MAX_MS = 7600
  const MEME_FIRST_MS_MIN = 1200
  const MEME_FIRST_MS_MAX = 2400

  const MEMES_FR = [
    'git push --force et prière',
    'ça compile sur ma machine',
    "j'ai relu le README",
    "c'était le cache CDN",
    'npm install… encore',
    '404 mais avec du style',
    'le bug était le Wi‑Fi',
    'tab vs espaces : trêve',
    'sudo rm -rf / reste un mythe',
    "j'ai seulement changé une ligne",
    'Stack Overflow en mode survie',
    'ça passera en prod',
    'Docker sur Windows : courage',
    "le linter est d'accord avec moi",
    'merge sans conflit (mensonge)',
  ]

  const MEMES_CLICK_FR = [
    'Clique.',
    'Appuie.',
    'Fais-le.',
    'Vas-y.',
    'Saisis ta chance.',
    'Un clic suffit.',
    'Ici. Maintenant.',
    'Ne résiste pas.',
    'Le vide t’invite.',
    'Allez, un clic.',
    'Tu sais que tu veux.',
    'C’est ton moment.',
  ]

  const MEMES_CLICK_EN = [
    'Click.',
    'Press it.',
    'Do it.',
    'Go on.',
    'Take your shot.',
    'One tap.',
    'Now.',
    'You know you want to.',
    'The void beckons.',
    'Just click.',
  ]

  const MEMES_EN = [
    'works on my machine™',
    'have you tried turning it off and on?',
    "it's a feature",
    '404 but make it fashion',
    'git blame the universe',
    'I read the docs* (*skimmed)',
    'npm waited. npm won.',
    'not a bug, cache',
    'send help stackoverflow',
    'tab gang rises',
    'one line change, 47 files',
    'LGTM energy',
    'CI is green on my branch',
    'repro steps: be lucky',
  ]

  let voidMemeTimer = null
  let voidMemeStopped = true
  let voidMemePaused = false

  let spawned = false
  let caught = false
  let mx = -99999
  let my = -99999
  let posX = 0
  let posY = 0
  let rafId = 0
  let phase2Timer = 0
  let phase2Started = false
  let rocketRaf = 0
  let rx = 0
  let ry = 0
  let countdownEl = null
  let glitchCap = null
  let glitchCursor = null
  let voidArmed = false

  const coarsePointer = typeof window.matchMedia === 'function' && window.matchMedia('(pointer: coarse)').matches
  const DODGE_RADIUS = coarsePointer ? 168 : 132
  const PUSH_STRENGTH = coarsePointer ? 40 : 34

  function setPhase(phase) {
    window.__solea404Phase = phase
  }
  setPhase('idle')

  function measure() {
    return btn.getBoundingClientRect()
  }

  function applyPos() {
    btn.style.left = `${Math.round(posX)}px`
    btn.style.top = `${Math.round(posY)}px`
  }

  function clamp() {
    const r = measure()
    const w = r.width || 168
    const h = r.height || 56
    const pad = 8
    posX = Math.max(pad, Math.min(window.innerWidth - w - pad, posX))
    posY = Math.max(pad, Math.min(window.innerHeight - h - pad, posY))
    applyPos()
  }

  function tickDodge() {
    if (!spawned || caught) return
    const r = measure()
    if (!r.width) return
    const cx = posX + r.width / 2
    const cy = posY + r.height / 2
    const dx = cx - mx
    const dy = cy - my
    const d = Math.hypot(dx, dy)
    if (d < DODGE_RADIUS && d > 0.01) {
      const t = (DODGE_RADIUS - d) / DODGE_RADIUS
      const push = t * t * PUSH_STRENGTH
      posX += (dx / d) * push
      posY += (dy / d) * push
      clamp()
    }
  }

  function loop() {
    if (!spawned || caught) {
      rafId = 0
      return
    }
    tickDodge()
    rafId = requestAnimationFrame(loop)
  }

  function onPointerMove(e) {
    mx = e.clientX
    my = e.clientY
    if (glitchCursor && window.__solea404Phase === 'glitch') {
      glitchCursor.style.left = `${e.clientX - 14}px`
      glitchCursor.style.top = `${e.clientY - 14}px`
    }
  }

  function spawn() {
    if (caught || mqReduce.matches) return
    btn.removeAttribute('hidden')
    btn.setAttribute('aria-hidden', 'false')
    void btn.offsetWidth
    const r = measure()
    const w = r.width
    const h = r.height
    posX = 16 + Math.random() * Math.max(8, window.innerWidth - w - 32)
    posY = 72 + Math.random() * Math.max(8, window.innerHeight - h - 88)
    applyPos()
    clamp()
    spawned = true
    if (!rafId) rafId = requestAnimationFrame(loop)
  }

  function stopLoop() {
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
  }

  function stopRocket() {
    if (rocketRaf) {
      cancelAnimationFrame(rocketRaf)
      rocketRaf = 0
    }
    rocket.setAttribute('hidden', '')
    rocket.setAttribute('aria-hidden', 'true')
    if (countdownEl) {
      countdownEl.remove()
      countdownEl = null
    }
  }

  function schedulePhase2() {
    if (phase2Started || !caught) return
    phase2Started = true
    if (mqReduce.matches) return
    const delay = PHASE2_MIN_MS + Math.random() * (PHASE2_MAX_MS - PHASE2_MIN_MS)
    phase2Timer = window.setTimeout(startRocketPhase, delay)
  }

  function pickRocketSpawn() {
    const w = window.innerWidth
    const h = window.innerHeight
    const margin = Math.min(w, h) * 0.16 + 56
    const edge = Math.floor(Math.random() * 4)
    if (edge === 0) return { x: (0.18 + Math.random() * 0.64) * w, y: -margin }
    if (edge === 1) return { x: w + margin, y: (0.12 + Math.random() * 0.76) * h }
    if (edge === 2) return { x: (0.18 + Math.random() * 0.64) * w, y: h + margin }
    return { x: -margin, y: (0.12 + Math.random() * 0.76) * h }
  }

  function lerpAngleDeg(a, b, t) {
    let d = b - a
    while (d > 180) d -= 360
    while (d < -180) d += 360
    return a + d * t
  }

  function startRocketPhase() {
    if (mqReduce.matches || window.__solea404Phase === 'void' || window.__solea404Phase === 'glitch') return
    setPhase('rocket')
    const s = pickRocketSpawn()
    const sx = s.x
    const sy = s.y
    const vw = window.innerWidth
    const vh = window.innerHeight
    const endX = vw * (0.34 + Math.random() * 0.32)
    const endY = vh * (0.28 + Math.random() * 0.32)
    const cx = (sx + endX) * 0.32 + vw * 0.5 * 0.68
    const cy = (sy + endY) * 0.32 + vh * 0.45 * 0.68

    rx = sx
    ry = sy
    rocket.style.setProperty('--rocket-deg', '0deg')
    rocket.style.left = `${rx}px`
    rocket.style.top = `${ry}px`
    rocket.removeAttribute('hidden')
    rocket.setAttribute('aria-hidden', 'false')

    const rocketT0 = performance.now()
    let displayAngle = (Math.atan2(endY - sy, endX - sx) * 180) / Math.PI

    countdownEl = document.createElement('div')
    countdownEl.className = 'page-error-rocket-countdown'
    countdownEl.setAttribute('aria-live', 'polite')
    countdownEl.hidden = true
    document.body.appendChild(countdownEl)

    let holdOnCursor = 0
    let lastRocketT = performance.now()
    const tick = (now) => {
      if (window.__solea404Phase !== 'rocket') {
        rocketRaf = 0
        return
      }
      const dt = Math.min(0.05, Math.max(0.001, (now - lastRocketT) / 1000))
      lastRocketT = now

      const elapsed = now - rocketT0
      const u = Math.min(1, elapsed / ROCKET_ENTRY_MS)
      const t = 1 - Math.pow(1 - u, 2.45)

      let vxr = 0
      let vyr = 0
      if (u < 1 - 1e-8) {
        const omt = 1 - t
        const newRx = omt * omt * sx + 2 * omt * t * cx + t * t * endX
        const newRy = omt * omt * sy + 2 * omt * t * cy + t * t * endY
        vxr = newRx - rx
        vyr = newRy - ry
        rx = newRx
        ry = newRy
      } else {
        const dx = mx - rx
        const dy = my - ry
        const dist = Math.hypot(dx, dy)
        const soft = Math.min(1, ROCKET_HOMING_NEAR + (1 - ROCKET_HOMING_NEAR) * Math.min(1, dist / 400))
        const cap = ROCKET_HOMING_SPEED * dt * soft
        const step = dist > 0.001 ? Math.min(cap, dist * 0.052) : 0
        if (dist > 0.35 && step > 0) {
          vxr = (dx / dist) * step
          vyr = (dy / dist) * step
        }
        rx += vxr
        ry += vyr
      }

      const vm = Math.hypot(vxr, vyr)
      const toCursorDeg = (Math.atan2(my - ry, mx - rx) * 180) / Math.PI
      const aimDeg = vm > 0.45 ? (Math.atan2(vyr, vxr) * 180) / Math.PI : toCursorDeg
      displayAngle = lerpAngleDeg(displayAngle, aimDeg, Math.min(1, 9 * dt))

      rocket.style.left = `${rx}px`
      rocket.style.top = `${ry}px`
      rocket.style.setProperty('--rocket-deg', `${displayAngle}deg`)

      const d = Math.hypot(rx - mx, ry - my)
      if (d < ROCKET_CATCH_R) {
        holdOnCursor += dt
        if (countdownEl) {
          countdownEl.hidden = false
          countdownEl.textContent = String(Math.max(0, Math.ceil(COUNTDOWN_SEC - holdOnCursor)))
        }
        if (holdOnCursor >= COUNTDOWN_SEC) {
          stopRocket()
          enterGlitchMode()
          rocketRaf = 0
          return
        }
      } else {
        holdOnCursor = 0
        if (countdownEl) countdownEl.hidden = true
      }
      rocketRaf = requestAnimationFrame(tick)
    }
    rocketRaf = requestAnimationFrame((t) => tick(t))
  }

  function spawnCrackVisual(clientX, clientY) {
    const crack = document.createElement('div')
    crack.className = 'page-error-crack'
    crack.style.left = `${clientX}px`
    crack.style.top = `${clientY}px`
    crack.style.setProperty('--crack-rot', `${(Math.random() - 0.5) * 52}deg`)
    crack.innerHTML =
      '<span class="page-error-crack__glow" aria-hidden="true"></span>' +
      '<span class="page-error-crack__core" aria-hidden="true"></span>' +
      '<span class="page-error-crack__jag page-error-crack__jag--1" aria-hidden="true"></span>' +
      '<span class="page-error-crack__jag page-error-crack__jag--2" aria-hidden="true"></span>' +
      '<span class="page-error-crack__jag page-error-crack__jag--3" aria-hidden="true"></span>' +
      '<span class="page-error-crack__ring" aria-hidden="true"></span>'
    document.body.appendChild(crack)
    window.setTimeout(() => crack.remove(), 4800)
  }

  function onGlitchPointerDown(e) {
    if (window.__solea404Phase !== 'glitch') return
    if (e.pointerType === 'mouse' && e.button !== 0) return
    if (e.target.closest('#err-void-reset, dialog, .lang-switch')) return
    if (window.Solea404Particles && typeof window.Solea404Particles.addCrack === 'function') {
      window.Solea404Particles.addCrack(e.clientX, e.clientY)
    }
    spawnCrackVisual(e.clientX, e.clientY)
  }

  function enterGlitchMode() {
    setPhase('glitch')
    document.body.classList.add('page-error--glitch-mode')
    glitchCap = document.createElement('div')
    glitchCap.className = 'page-error-glitch-cap'
    glitchCap.setAttribute('aria-hidden', 'true')
    document.body.appendChild(glitchCap)
    glitchCap.addEventListener('pointerdown', onGlitchPointerDown, { passive: true })

    glitchCursor = document.createElement('div')
    glitchCursor.className = 'page-error-glitch-cursor'
    glitchCursor.setAttribute('aria-hidden', 'true')
    document.body.appendChild(glitchCursor)
    glitchCursor.style.left = `${mx - 14}px`
    glitchCursor.style.top = `${my - 14}px`

    if (window.Solea404Particles && typeof window.Solea404Particles.setGlitchSuction === 'function') {
      window.Solea404Particles.setGlitchSuction(true)
    }

    window.__solea404OnAllAbsorbed = onAllAbsorbedOnce
  }

  function teardownGlitch() {
    document.body.classList.remove('page-error--glitch-mode')
    if (glitchCap) {
      glitchCap.removeEventListener('pointerdown', onGlitchPointerDown)
      glitchCap.remove()
      glitchCap = null
    }
    if (glitchCursor) {
      glitchCursor.remove()
      glitchCursor = null
    }
  }

  function wireCinemaVoidSuck() {
    const vx = window.innerWidth * 0.5
    const vy = window.innerHeight * 0.5
    const shell = document.querySelector('.page-error__shell')
    if (!shell) return

    const selectors = [
      '.page-error__top',
      '.page-error__code',
      '.page-error__title',
      '.page-error__lead',
      '.page-error__path-wrap',
      '.page-error__actions a',
    ]

    let i = 0
    for (const sel of selectors) {
      shell.querySelectorAll(sel).forEach((el) => {
        const r = el.getBoundingClientRect()
        if (r.width < 2 && r.height < 2) return
        const ecx = r.left + r.width / 2
        const ecy = r.top + r.height / 2
        const tx = vx - ecx
        const ty = vy - ecy
        const len = Math.hypot(tx, ty) || 1
        const px = (-ty / len) * 72
        const py = (tx / len) * 72
        const rz = 0.82 + Math.random() * 0.36
        el.style.setProperty('--void-tx', `${tx}px`)
        el.style.setProperty('--void-ty', `${ty}px`)
        el.style.setProperty('--void-px', `${px}px`)
        el.style.setProperty('--void-py', `${py}px`)
        el.style.setProperty('--void-rz', String(rz))
        el.style.setProperty('--void-delay', `${i * 0.12}s`)
        el.style.setProperty('--void-dur', `${13.2 + Math.random() * 1.6}s`)
        el.style.zIndex = String(50 + i)
        el.classList.add('page-error-cinema-suck')
        i += 1
      })
    }
  }

  function isFrLang() {
    const lang = (document.documentElement.getAttribute('lang') || 'en').toLowerCase()
    return lang.startsWith('fr')
  }

  function stopVoidMemeFloaters() {
    voidMemeStopped = true
    voidMemePaused = false
    if (voidMemeTimer != null) {
      window.clearTimeout(voidMemeTimer)
      voidMemeTimer = null
    }
    voidMemeLayer.innerHTML = ''
  }

  function pauseVoidMemeScheduler() {
    voidMemePaused = true
    if (voidMemeTimer != null) {
      window.clearTimeout(voidMemeTimer)
      voidMemeTimer = null
    }
  }

  function resumeVoidMemeScheduler() {
    voidMemePaused = false
    if (voidMemeStopped || mqReduce.matches) return
    const p = window.__solea404Phase
    if (p !== 'collapsed' && p !== 'void_sequel_star') return
    scheduleVoidMeme(true)
  }

  function spawnVoidMeme() {
    if (voidMemeStopped || voidMemePaused) return
    if (window.__solea404Phase !== 'collapsed' && window.__solea404Phase !== 'void_sequel_star') return
    const clickbait = window.__solea404MemeMode === 'clickbait'
    const list = clickbait ? (isFrLang() ? MEMES_CLICK_FR : MEMES_CLICK_EN) : isFrLang() ? MEMES_FR : MEMES_EN
    const text = list[Math.floor(Math.random() * list.length)]
    const span = document.createElement('span')
    span.className = 'page-error-void-meme'
    span.textContent = text
    span.setAttribute('aria-hidden', 'true')
    span.style.left = `${6 + Math.random() * 82}vw`
    span.style.top = `${8 + Math.random() * 68}vh`
    const dur = 5 + Math.random() * 5
    span.style.setProperty('--meme-dur', `${dur}s`)
    span.style.setProperty('--meme-a', String(0.12 + Math.random() * 0.1))
    voidMemeLayer.appendChild(span)
    window.setTimeout(() => {
      try {
        span.remove()
      } catch {
        /* ignore */
      }
    }, dur * 1000 + 240)
  }

  function scheduleVoidMeme(isFirst) {
    if (voidMemeStopped || voidMemePaused) return
    if (window.__solea404Phase !== 'collapsed' && window.__solea404Phase !== 'void_sequel_star') return
    if (mqReduce.matches) return
    const delay = isFirst
      ? MEME_FIRST_MS_MIN + Math.random() * (MEME_FIRST_MS_MAX - MEME_FIRST_MS_MIN)
      : MEME_SPAWN_MIN_MS + Math.random() * (MEME_SPAWN_MAX_MS - MEME_SPAWN_MIN_MS)
    voidMemeTimer = window.setTimeout(() => {
      voidMemeTimer = null
      if (voidMemeStopped) return
      spawnVoidMeme()
      scheduleVoidMeme(false)
    }, delay)
  }

  function populateVoidStars() {
    voidStars.innerHTML = ''
    const count = mqReduce.matches ? 52 : 155
    for (let i = 0; i < count; i += 1) {
      const s = document.createElement('span')
      s.className = 'page-error-void-star'
      if (Math.random() > 0.92) s.classList.add('page-error-void-star--bright')
      s.setAttribute('aria-hidden', 'true')
      s.style.left = `${Math.random() * 100}%`
      s.style.top = `${Math.random() * 100}%`
      const size = 1.25 + Math.random() * (s.classList.contains('page-error-void-star--bright') ? 3.2 : 2.6)
      s.style.width = `${size}px`
      s.style.height = `${size}px`
      if (!mqReduce.matches && Math.random() > 0.38) {
        const tw = 2.2 + Math.random() * 4.8
        s.style.animation = `page-error-void-star-twinkle ${tw}s ease-in-out infinite alternate`
        s.style.animationDelay = `${Math.random() * 4}s`
      } else {
        s.style.opacity = String(0.38 + Math.random() * 0.52)
      }
      voidStars.appendChild(s)
    }
  }

  function revealVoidEnd() {
    setPhase('collapsed')
    if (window.Solea404Particles && typeof window.Solea404Particles.setVoidPull === 'function') {
      window.Solea404Particles.setVoidPull(false)
    }
    populateVoidStars()
    voidStars.removeAttribute('hidden')
    voidStage.classList.add('page-error-void-stage--revealed')
    window.requestAnimationFrame(() => {
      voidReset.removeAttribute('hidden')
      if (window.SoleaI18n && typeof window.SoleaI18n.apply === 'function') window.SoleaI18n.apply()
    })
    voidMemeStopped = false
    voidMemePaused = false
    if (!mqReduce.matches) {
      scheduleVoidMeme(true)
    }
    if (window.Solea404VoidSequel && typeof window.Solea404VoidSequel.start === 'function') {
      window.Solea404VoidSequel.start({
        voidStage,
        voidMemeLayer,
        voidReset,
        voidStars,
        setPhase,
        isFrLang,
        mqReduce,
        stopVoidMemeFloaters,
        pauseVoidMemeScheduler,
        resumeVoidMemeScheduler,
      })
    }
  }

  function onAllAbsorbedOnce() {
    if (voidArmed) return
    voidArmed = true
    window.__solea404OnAllAbsorbed = null
    teardownGlitch()
    setPhase('void')
    if (window.Solea404Particles) {
      window.Solea404Particles.setGlitchSuction(false)
      window.Solea404Particles.setVoidPull(true)
    }
    voidStage.removeAttribute('hidden')
    voidStage.setAttribute('aria-hidden', 'false')
    voidStage.classList.add('page-error-void-stage--active')
    document.body.classList.add('page-error--void-collapse')
    voidHole.classList.add('page-error-void-hole--pull')
    wireCinemaVoidSuck()

    window.setTimeout(() => {
      revealVoidEnd()
    }, VOID_SEQUENCE_MS)
  }

  function victory() {
    caught = true
    spawned = false
    stopLoop()
    btn.setAttribute('hidden', '')
    btn.setAttribute('aria-hidden', 'true')
    if (typeof dialog.showModal === 'function') {
      dialog.showModal()
    }
  }

  btn.addEventListener(
    'click',
    (e) => {
      e.preventDefault()
      e.stopPropagation()
      if (!spawned || caught) return
      victory()
    },
    true
  )

  claimBtn.addEventListener('click', () => {
    window.open(RICK, '_blank', 'noopener,noreferrer')
    if (typeof dialog.close === 'function') dialog.close()
  })

  dismissBtn.addEventListener('click', () => {
    if (typeof dialog.close === 'function') dialog.close()
  })

  dialog.addEventListener('close', () => {
    if (caught) schedulePhase2()
  })

  voidReset.addEventListener('click', () => {
    if (window.__solea404VoidSequelActive) return
    stopVoidMemeFloaters()
    try {
      location.reload()
    } catch {
      location.href = '/404.html'
    }
  })

  window.addEventListener('pointermove', onPointerMove, { passive: true })
  window.addEventListener(
    'resize',
    () => {
      if (spawned && !caught) clamp()
    },
    { passive: true }
  )

  function boot() {
    if (mqReduce.matches) return
    const delay = SPAWN_MIN_MS + Math.random() * (SPAWN_MAX_MS - SPAWN_MIN_MS)
    window.setTimeout(spawn, delay)
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot)
  } else {
    boot()
  }
})()
