/**
 * Fond accueil : étoiles lisibles mais pas agressives (scintillement doux).
 * Sous les cubes hub-bg-fx ; figées si prefers-reduced-motion.
 */
;(function () {
  const canvas = document.getElementById('solea-hub-stars')
  if (!canvas || !canvas.getContext) return

  const ctx = canvas.getContext('2d', { alpha: true })
  if (!ctx) return

  const mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)')

  /** @type {{ x: number; y: number; r: number; a0: number; amp: number; spd: number; ph: number }[]} */
  let stars = []
  let w = 0
  let h = 0
  let dpr = 1
  let raf = 0
  let running = true

  function place() {
    stars = []
    const area = w * h
    const n = Math.min(96, Math.max(32, Math.floor(area / 12000)))
    for (let i = 0; i < n; i += 1) {
      stars.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: Math.random() * 0.95 + 0.38,
        a0: 0.1 + Math.random() * 0.16,
        amp: 0.045 + Math.random() * 0.12,
        spd: 0.0005 + Math.random() * 0.0014,
        ph: Math.random() * Math.PI * 2,
      })
    }
  }

  function drawStar(s, a) {
    const core = Math.min(0.58, a * 1.05)
    const halo = Math.min(0.22, a * 0.35)
    ctx.beginPath()
    ctx.arc(s.x, s.y, s.r * 2.6, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255,255,255,${halo})`
    ctx.fill()
    ctx.beginPath()
    ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255,252,248,${core})`
    ctx.fill()
  }

  function drawStatic() {
    ctx.clearRect(0, 0, w, h)
    for (const s of stars) {
      const a = Math.min(0.36, s.a0 + s.amp * 0.45)
      drawStar(s, a)
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
    if (mqReduce.matches) drawStatic()
  }

  function frame(t) {
    ctx.clearRect(0, 0, w, h)
    for (const s of stars) {
      const tw = s.a0 + s.amp * Math.sin(t * s.spd + s.ph)
      const a = Math.max(0.06, Math.min(0.45, tw))
      drawStar(s, a)
    }
    raf = requestAnimationFrame(frame)
  }

  function start() {
    if (raf || mqReduce.matches) return
    raf = requestAnimationFrame(frame)
  }

  function stop() {
    if (raf) cancelAnimationFrame(raf)
    raf = 0
  }

  function onReduce() {
    if (mqReduce.matches) {
      stop()
      resize()
    } else {
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
      window.removeEventListener('resize', resize)
      mqReduce.removeEventListener('change', onReduce)
      document.removeEventListener('visibilitychange', onVis)
    },
    { once: true },
  )
})()
