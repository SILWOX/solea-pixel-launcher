/**
 * Fond : étoiles variées + filantes. Accueil : révélation progressive (peu → beaucoup).
 * FAQ : orbes latéraux (--ambient-orbs) s’estompent, ciel plus riche, étoiles or SOLEA PIXEL, plus de filantes.
 * prefers-reduced-motion : pas d’animations longues ni météores.
 */
;(function () {
  const canvas = document.getElementById('solea-hub-stars')
  if (!canvas || !canvas.getContext) return

  const ctx = canvas.getContext('2d', { alpha: true })
  if (!ctx) return

  const mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)')

  /**
   * @typedef {{
   *   x: number; y: number; r: number;
   *   a0: number; amp: number; spd: number; ph: number;
   *   tr: number; tg: number; tb: number;
   *   kind: 0 | 1 | 2 | 3;
   *   staticA?: number;
   *   reveal: number;
   *   isGold: boolean;
   * }} Star
   */

  /** @type {Star[]} */
  let stars = []
  /**
   * @typedef {{ x: number; y: number; vx: number; vy: number; born: number; duration: number; trail: number }} Meteor
   * @type {Meteor[]}
   */
  let meteors = []

  let w = 0
  let h = 0
  let dpr = 1
  let raf = 0
  let running = true
  let nextMeteorAt = 0
  let lastFrameTs = 0

  /** Révélation étoiles sur l’accueil (0 → 1) */
  let buildup = 0
  /** Ciel FAQ : intensité visuelle 0 → 1 */
  let faqSky = 0
  /** Opacité des blobs latéraux (1 → 0 en FAQ) */
  let orbFade = 1

  function activeSection() {
    return document.documentElement.dataset.activeSection || 'hub'
  }

  function pickNextMeteorDelay(faq) {
    const f = typeof faq === 'number' ? faq : 0
    const base = 9000 + Math.random() * 20000
    return base * (0.42 + 0.58 * (1 - f * 0.92))
  }

  function place() {
    stars = []
    meteors = []
    const area = w * h
    const n = Math.min(110, Math.max(40, Math.floor(area / 9800)))
    for (let i = 0; i < n; i += 1) {
      const roll = Math.random()
      /** @type {0|1|2|3} */
      let kind = 1
      if (roll < 0.38) kind = 0
      else if (roll < 0.78) kind = 1
      else if (roll < 0.94) kind = 2
      else kind = 3

      let r = 0.4
      let a0 = 0.1
      let amp = 0.08
      let spd = 0.0008

      if (kind === 0) {
        r = Math.random() * 0.45 + 0.2
        a0 = 0.04 + Math.random() * 0.08
        amp = 0.02 + Math.random() * 0.06
        spd = 0.001 + Math.random() * 0.0022
      } else if (kind === 1) {
        r = Math.random() * 0.85 + 0.35
        a0 = 0.09 + Math.random() * 0.12
        amp = 0.05 + Math.random() * 0.14
        spd = 0.00045 + Math.random() * 0.0015
      } else if (kind === 2) {
        r = Math.random() * 0.55 + 0.55
        a0 = 0.12 + Math.random() * 0.14
        amp = 0.08 + Math.random() * 0.16
        spd = 0.00035 + Math.random() * 0.001
      } else {
        r = Math.random() * 0.4 + 0.75
        a0 = 0.14 + Math.random() * 0.12
        amp = 0.1 + Math.random() * 0.18
        spd = 0.00055 + Math.random() * 0.0012
      }

      const warm = Math.random()
      const tr = warm > 0.55 ? 248 + Math.random() * 7 : 232 + Math.random() * 18
      const tg = 245 + Math.random() * 12
      const tb = warm > 0.4 ? 252 + Math.random() * 3 : 248 + Math.random() * 10

      stars.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r,
        a0,
        amp,
        spd,
        ph: Math.random() * Math.PI * 2,
        tr,
        tg,
        tb,
        kind,
        staticA: 0.08 + Math.random() * 0.28,
        reveal: Math.random(),
        isGold: Math.random() < 0.11,
      })
    }
    nextMeteorAt = performance.now() + (5000 + Math.random() * 9000)
  }

  /** @param {Star} s */
  function twinkleAlpha(s, t, faqBlend) {
    const boost = 1 + faqBlend * 0.22 * (s.isGold ? 1.35 : 1)
    const base = (s.a0 + s.amp * Math.sin(t * s.spd + s.ph)) * boost
    if (s.kind === 0) {
      return Math.max(0.04, Math.min(0.36, base * 0.85))
    }
    if (s.kind === 1) {
      return Math.max(0.06, Math.min(0.52, base))
    }
    if (s.kind === 2) {
      const slow = 0.55 + 0.45 * Math.sin(t * s.spd * 0.55 + s.ph * 0.5)
      const fast = 0.92 + 0.08 * Math.sin(t * s.spd * 2.8 + s.ph * 1.7)
      return Math.max(0.08, Math.min(0.58, (s.a0 + s.amp * slow) * fast * boost))
    }
    const beat = Math.sin(t * s.spd + s.ph)
    const sparkle = Math.pow(Math.max(0, beat), 3.2)
    return Math.max(0.1, Math.min(0.68, (s.a0 * 0.65 + s.amp * (0.35 + sparkle * 1.4)) * boost))
  }

  /**
   * @param {Star} s
   * @param {number} a
   * @param {number} faqBlend
   */
  function drawStar(s, a, faqBlend) {
    let tr = s.tr
    let tg = s.tg
    let tb = s.tb
    if (s.isGold && faqBlend > 0.06) {
      const u = faqBlend * (faqBlend > 0.4 ? 1 : 0.65)
      tr = tr * (1 - u) + 255 * 0.96 * u
      tg = tg * (1 - u) + 210 * u
      tb = tb * (1 - u) + 105 * u
    }

    const haloR = s.r * (s.kind >= 2 ? 3.1 : 2.5) * (1 + faqBlend * 0.08)
    const haloA = Math.min(0.32 + faqBlend * 0.08, a * (s.kind >= 2 ? 0.46 : 0.34))
    const coreA = Math.min(s.kind === 3 ? 0.78 : 0.62, a * (s.kind === 3 ? 1.18 : 1.05))

    ctx.beginPath()
    ctx.arc(s.x, s.y, haloR, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${tr},${tg},${tb},${haloA})`
    ctx.fill()

    ctx.beginPath()
    ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(${Math.min(255, tr + 8)},${Math.min(255, tg + 6)},${tb},${coreA})`
    ctx.fill()

    if (s.kind >= 2 && a > 0.22) {
      const flare = s.r * 1.85
      const fa = Math.min(0.22 + faqBlend * 0.08, a * 0.24)
      const lineW = s.kind === 3 ? 0.55 : 0.4
      ctx.strokeStyle = s.isGold && faqBlend > 0.35 ? `rgba(255,230,160,${fa})` : `rgba(255,255,255,${fa})`
      ctx.lineWidth = lineW
      ctx.beginPath()
      ctx.moveTo(s.x - flare, s.y)
      ctx.lineTo(s.x + flare, s.y)
      ctx.moveTo(s.x, s.y - flare)
      ctx.lineTo(s.x, s.y + flare)
      ctx.stroke()
    }

    if (s.isGold && faqBlend > 0.45 && a > 0.28) {
      const pulse = 0.55 + 0.45 * Math.sin(performance.now() * 0.004 + s.ph)
      ctx.beginPath()
      ctx.arc(s.x, s.y, s.r * 2.4, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(255,215,120,${0.06 * pulse * faqBlend})`
      ctx.fill()
    }
  }

  /** @param {Meteor} m */
  function drawMeteor(m, now, faqBlend) {
    const elapsed = now - m.born
    const u = elapsed / m.duration
    if (u >= 1) return false

    const headX = m.x + m.vx * elapsed
    const headY = m.y + m.vy * elapsed
    const len = m.trail * (0.35 + 0.65 * Math.sin(u * Math.PI))
    const nx = -m.vx
    const ny = -m.vy
    const mag = Math.hypot(nx, ny) || 1
    const tx = (nx / mag) * len
    const ty = (ny / mag) * len
    const tailX = headX + tx
    const tailY = headY + ty

    const fade = Math.sin(u * Math.PI)
    const headGlow = (0.55 + 0.45 * (1 - u)) * fade

    const g = ctx.createLinearGradient(tailX, tailY, headX, headY)
    const gold = faqBlend > 0.5
    if (gold) {
      g.addColorStop(0, `rgba(180,140,80,0)`)
      g.addColorStop(0.32, `rgba(255,220,160,${0.1 * headGlow})`)
      g.addColorStop(0.7, `rgba(255,248,220,${0.42 * headGlow})`)
      g.addColorStop(1, `rgba(255,252,235,${0.95 * headGlow})`)
    } else {
      g.addColorStop(0, `rgba(200,220,255,0)`)
      g.addColorStop(0.35, `rgba(230,238,255,${0.08 * headGlow})`)
      g.addColorStop(0.72, `rgba(255,255,255,${0.35 * headGlow})`)
      g.addColorStop(1, `rgba(255,252,245,${0.92 * headGlow})`)
    }

    ctx.strokeStyle = g
    ctx.lineWidth = 1.35 + (1 - u) * 0.9
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(tailX, tailY)
    ctx.lineTo(headX, headY)
    ctx.stroke()

    ctx.fillStyle = gold
      ? `rgba(255,245,210,${0.5 * headGlow})`
      : `rgba(255,255,255,${0.45 * headGlow})`
    ctx.beginPath()
    ctx.arc(headX, headY, 1.1 + (1 - u) * 0.8, 0, Math.PI * 2)
    ctx.fill()

    return true
  }

  function trySpawnMeteor(now, sec, faqBlend) {
    const maxMeteors = faqBlend > 0.78 ? 2 : 1
    if (meteors.length >= maxMeteors || now < nextMeteorAt) return
    if (w < 400 || h < 320) {
      nextMeteorAt = now + pickNextMeteorDelay(faqBlend)
      return
    }
    const skipChance = faqBlend > 0.55 ? 0.38 : 0.72
    if (Math.random() > skipChance) {
      nextMeteorAt = now + 900 + Math.random() * 3200
      return
    }

    const theta = Math.PI * (0.2 + Math.random() * 0.18)
    const speed = 0.38 + Math.random() * 0.34
    const vx = Math.cos(theta) * speed
    const vy = Math.sin(theta) * speed

    meteors.push({
      x: -80 - Math.random() * w * 0.35,
      y: -40 - Math.random() * h * 0.45,
      vx,
      vy,
      born: now,
      duration: 480 + Math.random() * (faqBlend > 0.6 ? 620 : 480),
      trail: 78 + Math.random() * (faqBlend > 0.5 ? 130 : 95),
    })
    nextMeteorAt = now + pickNextMeteorDelay(faqBlend)
  }

  function shouldDrawStar(s, sec, buildupVal) {
    if (sec === 'faq') return true
    const gate = Math.min(1, Math.max(0.06, buildupVal * 1.05 + 0.035))
    return s.reveal <= gate
  }

  function drawStatic() {
    const sec = activeSection()
    const fb = sec === 'faq' ? 0.85 : 0
    ctx.clearRect(0, 0, w, h)
    for (const s of stars) {
      if (!shouldDrawStar(s, sec, 1)) continue
      const a = (s.staticA ?? 0.2) * (sec === 'faq' ? 1.08 : 1)
      drawStar(s, a, fb)
    }
  }

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = window.innerWidth
    h = window.innerHeight
    canvas.width = Math.floor(w * dpr)
    canvas.height = Math.floor(h * dpr)
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    place()
    if (mqReduce.matches) {
      document.documentElement.style.setProperty('--ambient-orbs', activeSection() === 'faq' ? '0' : '1')
      drawStatic()
    }
  }

  function frame(t) {
    const now = performance.now()
    const dt = Math.min(48, lastFrameTs ? now - lastFrameTs : 16)
    lastFrameTs = now

    const sec = activeSection()
    const isHub = sec === 'hub'
    const isFaq = sec === 'faq'

    if (mqReduce.matches) {
      buildup = 1
      faqSky = isFaq ? 1 : 0
      orbFade = isFaq ? 0 : 1
    } else {
      if (isHub) {
        buildup = Math.min(1, buildup + dt * 0.000095)
      } else if (!isFaq) {
        buildup = Math.min(1, buildup + dt * 0.000018)
      }

      if (isFaq) {
        faqSky = Math.min(1, faqSky + dt * 0.00135)
        orbFade = Math.max(0, orbFade - dt * 0.00105)
      } else {
        faqSky = Math.max(0, faqSky - dt * 0.00115)
        orbFade = Math.min(1, orbFade + dt * 0.001)
      }
    }

    document.documentElement.style.setProperty('--ambient-orbs', orbFade.toFixed(4))
    canvas.classList.toggle('hub-stars--faq-sky', faqSky > 0.52)

    ctx.clearRect(0, 0, w, h)
    for (const s of stars) {
      if (!shouldDrawStar(s, sec, buildup)) continue
      const a = twinkleAlpha(s, t, faqSky)
      drawStar(s, a, faqSky)
    }

    if (!mqReduce.matches) {
      trySpawnMeteor(now, sec, faqSky)
      meteors = meteors.filter((m) => drawMeteor(m, now, faqSky))
    }

    raf = requestAnimationFrame(frame)
  }

  function start() {
    if (raf || mqReduce.matches) return
    lastFrameTs = performance.now()
    raf = requestAnimationFrame(frame)
  }

  function stop() {
    if (raf) cancelAnimationFrame(raf)
    raf = 0
  }

  function onReduce() {
    if (mqReduce.matches) {
      stop()
      meteors = []
      canvas.classList.remove('hub-stars--faq-sky')
      resize()
    } else {
      lastFrameTs = performance.now()
      resize()
      if (running) start()
    }
  }

  function onVis() {
    if (document.hidden) {
      running = false
      stop()
    } else {
      running = true
      lastFrameTs = performance.now()
      if (!mqReduce.matches) start()
    }
  }

  resize()
  window.addEventListener('resize', resize, { passive: true })
  mqReduce.addEventListener('change', onReduce)
  document.addEventListener('visibilitychange', onVis)
  if (!mqReduce.matches) start()

  window.addEventListener(
    'beforeunload',
    () => {
      running = false
      stop()
      document.documentElement.style.removeProperty('--ambient-orbs')
      window.removeEventListener('resize', resize)
      mqReduce.removeEventListener('change', onReduce)
      document.removeEventListener('visibilitychange', onVis)
    },
    { once: true },
  )
})()
