/**
 * Fond : halos dorés, « cellules » (coins arrondis), points — mouvement adouci (lerp + dt).
 * Respecte prefers-reduced-motion.
 */
;(function () {
  const canvas = document.getElementById('mr-particles')
  if (!canvas) return

  const CtxProto = typeof CanvasRenderingContext2D !== 'undefined' ? CanvasRenderingContext2D.prototype : null
  if (CtxProto && !CtxProto.roundRect) {
    CtxProto.roundRect = function (x, y, rw, rh, r) {
      const rad = Math.min(r, rw / 2, rh / 2)
      this.beginPath()
      this.moveTo(x + rad, y)
      this.arcTo(x + rw, y, x + rw, y + rh, rad)
      this.arcTo(x + rw, y + rh, x, y + rh, rad)
      this.arcTo(x, y + rh, x, y, rad)
      this.arcTo(x, y, x + rw, y, rad)
      this.closePath()
    }
  }

  const mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)')
  let reduce = mqReduce.matches
  mqReduce.addEventListener('change', () => {
    reduce = mqReduce.matches
    if (reduce && rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    } else {
      lastSec = performance.now() / 1000
    }
    if (!reduce) loop(performance.now())
  })

  const ctx = canvas.getContext('2d', { alpha: true })
  if (!ctx) return

  let w = 0
  let h = 0
  let dpr = 1
  let dots = []
  let cells = []
  let orbs = []
  let rafId = 0
  let lastSec = 0

  const GOLD = { r: 255, g: 210, b: 110 }
  const GOLD2 = { r: 240, g: 128, b: 24 }

  function resize() {
    const parent = canvas.parentElement
    const rw = parent ? parent.clientWidth : window.innerWidth
    const rh = parent ? parent.clientHeight : window.innerHeight
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = Math.max(1, Math.floor(rw))
    h = Math.max(1, Math.floor(rh))
    canvas.width = Math.floor(w * dpr)
    canvas.height = Math.floor(h * dpr)
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

    const area = w * h
    const nDots = Math.min(130, Math.max(40, Math.floor(area / 11000)))
    dots = []
    for (let i = 0; i < nDots; i++) {
      dots.push({
        x: Math.random() * w,
        y: Math.random() * h,
        tx: Math.random() * w,
        ty: Math.random() * h,
        r: 0.35 + Math.random() * 1.45,
        speed: 0.15 + Math.random() * 0.35,
        gold: Math.random() > 0.32,
        tw: Math.random() * Math.PI * 2,
      })
    }

    const nCells = Math.min(22, Math.max(6, Math.floor(area / 95000)))
    cells = []
    for (let i = 0; i < nCells; i++) {
      cells.push({
        x: Math.random() * w,
        y: Math.random() * h,
        tx: Math.random() * w,
        ty: Math.random() * h,
        rw: 14 + Math.random() * 36,
        rh: 10 + Math.random() * 28,
        rot: Math.random() * Math.PI * 2,
        vr: (Math.random() - 0.5) * 0.012,
        speed: 0.04 + Math.random() * 0.09,
        a: 0.04 + Math.random() * 0.1,
        gold: Math.random() > 0.45,
      })
    }

    orbs = []
    const nOrbs = Math.min(5, Math.max(3, Math.floor(area / 140000)))
    for (let i = 0; i < nOrbs; i++) {
      orbs.push({
        x: Math.random() * w,
        y: Math.random() * h,
        tx: Math.random() * w,
        ty: Math.random() * h,
        rad: 40 + Math.random() * 90,
        speed: 0.035 + Math.random() * 0.06,
        a: 0.06 + Math.random() * 0.08,
      })
    }
  }

  function pickNewTarget(p) {
    p.tx = Math.random() * w
    p.ty = Math.random() * h
  }

  function step(dt) {
    const d = Math.min(0.045, dt)
    for (const p of dots) {
      p.tw += d * 1.8
      p.x += (p.tx - p.x) * p.speed * d * 18 + Math.sin(p.tw) * 0.35 * d
      p.y += (p.ty - p.y) * p.speed * d * 18 - 0.12 * d
      if (Math.hypot(p.tx - p.x, p.ty - p.y) < 8) pickNewTarget(p)
      if (p.y < -8) {
        p.y = h + 4
        p.x = Math.random() * w
        pickNewTarget(p)
      }
      if (p.x < -6 || p.x > w + 6) pickNewTarget(p)
    }

    for (const c of cells) {
      c.rot += c.vr * (1 + d * 10)
      c.x += (c.tx - c.x) * c.speed * d * 12
      c.y += (c.ty - c.y) * c.speed * d * 12
      if (Math.hypot(c.tx - c.x, c.ty - c.y) < 12) {
        c.tx = Math.random() * w
        c.ty = Math.random() * h
      }
    }

    for (const o of orbs) {
      o.x += (o.tx - o.x) * o.speed * d * 10
      o.y += (o.ty - o.y) * o.speed * d * 10
      if (Math.hypot(o.tx - o.x, o.ty - o.y) < 20) {
        o.tx = Math.random() * w
        o.ty = Math.random() * h
      }
    }
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)

    for (const o of orbs) {
      const g = ctx.createRadialGradient(o.x, o.y, 0, o.x, o.y, o.rad)
      g.addColorStop(0, `rgba(${GOLD.r},${GOLD.g},${GOLD.b},${o.a})`)
      g.addColorStop(0.45, `rgba(${GOLD2.r},${GOLD2.g},${GOLD2.b},${o.a * 0.35})`)
      g.addColorStop(1, 'rgba(0,0,0,0)')
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(o.x, o.y, o.rad, 0, Math.PI * 2)
      ctx.fill()
    }

    for (const c of cells) {
      ctx.save()
      ctx.translate(c.x, c.y)
      ctx.rotate(c.rot)
      ctx.strokeStyle = c.gold
        ? `rgba(${GOLD.r},${GOLD.g},${GOLD.b},${c.a})`
        : `rgba(140, 170, 210,${c.a * 0.55})`
      ctx.lineWidth = 1.1
      ctx.beginPath()
      ctx.roundRect(-c.rw / 2, -c.rh / 2, c.rw, c.rh, 5)
      ctx.stroke()
      ctx.restore()
    }

    for (const p of dots) {
      const pulse = 0.82 + Math.sin(p.tw * 1.2) * 0.18
      const a = (p.gold ? 0.12 : 0.06) * pulse
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.gold ? `rgba(255, 210, 120, ${a})` : `rgba(170, 190, 220, ${a * 0.55})`
      ctx.fill()
    }
  }

  function loop(t) {
    if (reduce) return
    const now = t / 1000
    const dt = lastSec ? Math.min(0.04, Math.max(0.001, now - lastSec)) : 0.016
    lastSec = now
    step(dt)
    draw()
    rafId = requestAnimationFrame(loop)
  }

  function boot() {
    resize()
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
    lastSec = 0
    if (!reduce) loop(performance.now())
    else draw()
  }

  let tResize
  window.addEventListener(
    'resize',
    () => {
      clearTimeout(tResize)
      tResize = setTimeout(boot, 100)
    },
    { passive: true }
  )

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot)
  else boot()
})()
