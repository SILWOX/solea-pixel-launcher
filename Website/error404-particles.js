/**
 * Fond 404 : même logique que content-particles (points, cellules, halos) — palette rouge.
 * « Particules » texte 404 en pseudo-3D + bulles au clic (window, pour ne pas bloquer la UI).
 */
;(function () {
  const canvas = document.getElementById('err-particles')
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
  /** @type {{ x:number,y:number,z:number,tx:number,ty:number,vx:number,vy:number,rot:number,vr:number,speed:number,tw:number,fontPx:number }[]} */
  let float404 = []
  /** @type {{ cx:number,cy:number,r:number,life:number,maxLife:number,w:number,hue:number,kind:'ring'|'fill' }[]} */
  let bubbles = []
  let rafId = 0
  let lastSec = 0

  /** @type {{ x:number,y:number,w:number }[]} coords canvas logiques */
  let suctionCracks = []
  let glitchSuction = false
  let voidPull = false
  let voidStartTime = 0
  const VOID_DURATION_MS = 15000
  let voidTriggered = false
  const MAX_CRACKS = 24
  /** @type {{ x:number,y:number,r:number,sp:number }[]} */
  let spaceStars = []

  const R1 = { r: 255, g: 90, b: 100 }
  const R2 = { r: 200, g: 30, b: 55 }
  const R_DIM = { r: 120, g: 70, b: 88 }

  function resize() {
    const rw = window.innerWidth
    const rh = window.innerHeight
    dpr = Math.min(window.devicePixelRatio || 1, 2)
    w = Math.max(1, Math.floor(rw))
    h = Math.max(1, Math.floor(rh))
    canvas.width = Math.floor(w * dpr)
    canvas.height = Math.floor(h * dpr)
    canvas.style.width = `${w}px`
    canvas.style.height = `${h}px`
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

    const area = w * h
    const nDots = Math.min(120, Math.max(36, Math.floor(area / 12000)))
    dots = []
    for (let i = 0; i < nDots; i++) {
      dots.push({
        x: Math.random() * w,
        y: Math.random() * h,
        tx: Math.random() * w,
        ty: Math.random() * h,
        r: 0.35 + Math.random() * 1.4,
        speed: 0.14 + Math.random() * 0.32,
        hot: Math.random() > 0.35,
        tw: Math.random() * Math.PI * 2,
      })
    }

    const nCells = Math.min(20, Math.max(5, Math.floor(area / 100000)))
    cells = []
    for (let i = 0; i < nCells; i++) {
      cells.push({
        x: Math.random() * w,
        y: Math.random() * h,
        tx: Math.random() * w,
        ty: Math.random() * h,
        rw: 14 + Math.random() * 34,
        rh: 10 + Math.random() * 26,
        rot: Math.random() * Math.PI * 2,
        vr: (Math.random() - 0.5) * 0.011,
        speed: 0.038 + Math.random() * 0.085,
        a: 0.05 + Math.random() * 0.1,
        hot: Math.random() > 0.5,
      })
    }

    orbs = []
    const nOrbs = Math.min(5, Math.max(3, Math.floor(area / 150000)))
    for (let i = 0; i < nOrbs; i++) {
      orbs.push({
        x: Math.random() * w,
        y: Math.random() * h,
        tx: Math.random() * w,
        ty: Math.random() * h,
        rad: 36 + Math.random() * 88,
        speed: 0.032 + Math.random() * 0.055,
        a: 0.055 + Math.random() * 0.08,
      })
    }

    float404 = []
    const n404 = Math.min(14, Math.max(7, Math.floor(area / 95000)))
    for (let i = 0; i < n404; i++) {
      float404.push({
        x: Math.random() * w,
        y: Math.random() * h,
        z: 0.25 + Math.random() * 0.75,
        tx: Math.random() * w,
        ty: Math.random() * h,
        vx: 0,
        vy: 0,
        rot: (Math.random() - 0.5) * 0.5,
        vr: (Math.random() - 0.5) * 0.004,
        speed: 0.05 + Math.random() * 0.12,
        tw: Math.random() * Math.PI * 2,
        fontPx: 11 + Math.floor(Math.random() * 10),
      })
    }

    if (glitchSuction) initSpaceStars()
  }

  function pickNewTarget(p) {
    p.tx = Math.random() * w
    p.ty = Math.random() * h
  }

  function initSpaceStars() {
    const n = Math.min(280, Math.max(100, Math.floor((w * h) / 7200)))
    spaceStars = []
    for (let i = 0; i < n; i++) {
      spaceStars.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: 0.15 + Math.random() * 1.35,
        sp: 0.4 + Math.random() * 1.15,
      })
    }
  }

  function drawSpaceBackdrop() {
    if (!glitchSuction || spaceStars.length === 0) return
    const nCr = suctionCracks.length
    const vis = Math.min(1, 0.05 + nCr * 0.048 + (nCr >= 10 ? 0.14 : 0))
    const t = performance.now() / 1000
    if (vis > 0.02) {
      const g = Math.min(0.38, nCr * 0.026)
      ctx.fillStyle = `rgba(6, 4, 22, ${g * vis})`
      ctx.fillRect(0, 0, w, h)
    }
    for (const s of spaceStars) {
      const tw = 0.55 + Math.sin(t * s.sp * 2.8 + s.x * 0.01) * 0.45
      const a = vis * tw * (0.25 + s.r * 0.35)
      ctx.beginPath()
      ctx.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(230, 236, 255, ${a})`
      ctx.fill()
    }
  }

  function step(dt) {
    const d = Math.min(0.045, dt)
    for (const p of dots) {
      p.tw += d * 1.7
      p.x += (p.tx - p.x) * p.speed * d * 17 + Math.sin(p.tw) * 0.32 * d
      p.y += (p.ty - p.y) * p.speed * d * 17 - 0.1 * d
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
      c.x += (c.tx - c.x) * c.speed * d * 11
      c.y += (c.ty - c.y) * c.speed * d * 11
      if (Math.hypot(c.tx - c.x, c.ty - c.y) < 12) {
        c.tx = Math.random() * w
        c.ty = Math.random() * h
      }
    }

    for (const o of orbs) {
      o.x += (o.tx - o.x) * o.speed * d * 9.5
      o.y += (o.ty - o.y) * o.speed * d * 9.5
      if (Math.hypot(o.tx - o.x, o.ty - o.y) < 18) {
        o.tx = Math.random() * w
        o.ty = Math.random() * h
      }
    }

    if (voidPull && voidStartTime > 0) {
      const elapsed = performance.now() - voidStartTime
      const u = Math.min(1, elapsed / VOID_DURATION_MS)
      const smooth = u * u * (3 - 2 * u)
      const pullF = (110 + 4200 * smooth) * d
      const pullP = (85 + 3000 * smooth) * d
      const pullC = (62 + 2100 * smooth) * d
      const pullO = (52 + 1650 * smooth) * d
      const spin = (0.42 + smooth * 2.85) * d
      const cx = w * 0.5
      const cy = h * 0.5
      for (const f of float404) {
        const dx = cx - f.x
        const dy = cy - f.y
        const dist = Math.hypot(dx, dy) || 1
        const nx = dx / dist
        const ny = dy / dist
        const px = -ny
        const py = nx
        const spiral = spin * (12 + dist * 0.04)
        f.x += nx * pullF + px * spiral
        f.y += ny * pullF + py * spiral
        f.rot += d * (2.2 + smooth * 4)
      }
      for (const p of dots) {
        const dx = cx - p.x
        const dy = cy - p.y
        const dist = Math.hypot(dx, dy) || 1
        const nx = dx / dist
        const ny = dy / dist
        const spiral = spin * (10 + dist * 0.035)
        p.x += nx * pullP + -ny * spiral
        p.y += ny * pullP + nx * spiral
      }
      for (const c of cells) {
        const dx = cx - c.x
        const dy = cy - c.y
        const dist = Math.hypot(dx, dy) || 1
        const nx = dx / dist
        const ny = dy / dist
        const spiral = spin * (8 + dist * 0.02)
        c.x += nx * pullC + -ny * spiral
        c.y += ny * pullC + nx * spiral
      }
      for (const o of orbs) {
        const dx = cx - o.x
        const dy = cy - o.y
        const dist = Math.hypot(dx, dy) || 1
        const nx = dx / dist
        const ny = dy / dist
        const spiral = spin * (6 + dist * 0.015)
        o.x += nx * pullO + -ny * spiral
        o.y += ny * pullO + nx * spiral
        o.a *= 1 - 0.35 * d * smooth
      }
    } else {
      for (const f of float404) {
        f.tw += d * (1.2 + f.z * 2)
        const nCr = suctionCracks.length
        if (glitchSuction && nCr > 0) {
          const ramp = Math.min(1, 0.08 + nCr * 0.095)
          let ax = 0
          let ay = 0
          for (const cr of suctionCracks) {
            const dx = cr.x - f.x
            const dy = cr.y - f.y
            const dist = Math.hypot(dx, dy)
            const falloff = dist * dist * 0.00085 + 28 + (1 - ramp) * 55
            const pull = ((cr.w || 1) * (88 + 620 * ramp) * d) / falloff
            if (dist > 0.01) {
              ax += (dx / dist) * pull
              ay += (dy / dist) * pull
            }
          }
          f.x += ax
          f.y += ay
        }
        const sp = f.speed * (0.35 + f.z * 0.9)
        const wander = glitchSuction ? Math.max(0.04, 0.34 - nCr * 0.022) : 1
        f.x += (f.tx - f.x) * sp * d * 14 * wander + f.vx * d
        f.y += (f.ty - f.y) * sp * d * 14 * wander + f.vy * d
        f.vx *= Math.pow(0.88, d * 60)
        f.vy *= Math.pow(0.88, d * 60)
        f.rot += f.vr * (1 + f.z) * (1 + d * 8)
        f.rot += Math.sin(f.tw) * 0.0008
        if (!glitchSuction && Math.hypot(f.tx - f.x, f.ty - f.y) < 16) pickNewTarget(f)
        if (!glitchSuction) {
          if (f.x < -40) f.x = w + 20
          if (f.x > w + 40) f.x = -20
          if (f.y < -30) f.y = h + 10
          if (f.y > h + 30) f.y = -10
        }
      }

      if (glitchSuction) {
        for (let i = float404.length - 1; i >= 0; i--) {
          const f = float404[i]
          for (const cr of suctionCracks) {
            if (Math.hypot(f.x - cr.x, f.y - cr.y) < 11) {
              float404.splice(i, 1)
              break
            }
          }
        }
        const nCr2 = suctionCracks.length
        const ramp2 = nCr2 > 0 ? Math.min(1, 0.1 + nCr2 * 0.09) : 0
        for (const p of dots) {
          let pullx = 0
          let pully = 0
          for (const cr of suctionCracks) {
            const dx = cr.x - p.x
            const dy = cr.y - p.y
            const dist = Math.hypot(dx, dy)
            const pull = ((cr.w || 1) * (28 + 140 * ramp2) * d) / (dist * dist * 0.0014 + 72)
            if (dist > 0.01) {
              pullx += (dx / dist) * pull
              pully += (dy / dist) * pull
            }
          }
          p.x += pullx
          p.y += pully
        }
      }
    }

    for (let i = bubbles.length - 1; i >= 0; i--) {
      const b = bubbles[i]
      b.life -= d
      b.r += b.w * d * 420
      if (b.life <= 0) bubbles.splice(i, 1)
    }
  }

  function drawOrbs() {
    const nCr = suctionCracks.length
    const spaceDim = glitchSuction ? Math.min(0.9, nCr * 0.05) : 0
    for (const o of orbs) {
      const oa = Math.max(0.02, o.a * (1 - spaceDim * 0.88))
      const g = ctx.createRadialGradient(o.x, o.y, 0, o.x, o.y, o.rad)
      g.addColorStop(0, `rgba(${R1.r},${R1.g},${R1.b},${oa})`)
      g.addColorStop(0.42, `rgba(${R2.r},${R2.g},${R2.b},${oa * 0.38})`)
      g.addColorStop(1, 'rgba(0,0,0,0)')
      ctx.fillStyle = g
      ctx.beginPath()
      ctx.arc(o.x, o.y, o.rad, 0, Math.PI * 2)
      ctx.fill()
    }
  }

  function drawCells() {
    const nCr = suctionCracks.length
    const spaceDim = glitchSuction ? Math.min(0.9, nCr * 0.05) : 0
    const ca = 1 - spaceDim * 0.75
    for (const c of cells) {
      ctx.save()
      ctx.translate(c.x, c.y)
      ctx.rotate(c.rot)
      ctx.strokeStyle = c.hot
        ? `rgba(${R1.r},${R1.g},${R1.b},${c.a * ca})`
        : `rgba(${R_DIM.r},${R_DIM.g},${R_DIM.b},${c.a * 0.65 * ca})`
      ctx.lineWidth = 1.05
      ctx.beginPath()
      ctx.roundRect(-c.rw / 2, -c.rh / 2, c.rw, c.rh, 5)
      ctx.stroke()
      ctx.restore()
    }
  }

  function drawDots() {
    const nCr = suctionCracks.length
    const spaceDim = glitchSuction ? Math.min(0.9, nCr * 0.05) : 0
    const dim = 1 - spaceDim * 0.5
    for (const p of dots) {
      const pulse = 0.82 + Math.sin(p.tw * 1.15) * 0.18
      const a = (p.hot ? 0.11 : 0.055) * pulse * dim
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.hot
        ? `rgba(255, 120, 130, ${a})`
        : `rgba(${R_DIM.r},${R_DIM.g},${R_DIM.b},${a * 0.55})`
      ctx.fill()
    }
  }

  function draw404Floaters() {
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const text = '404'
    for (const f of float404) {
      const depth = f.z
      const scale = 0.42 + depth * 0.95
      const alpha = 0.12 + depth * 0.38
      const px = f.fontPx * scale
      ctx.font = `${px}px "Press Start 2P", ui-monospace, monospace`
      const extr = Math.max(1, Math.round(2 * depth))
      ctx.save()
      ctx.translate(f.x, f.y)
      ctx.rotate(f.rot)
      for (let k = extr; k >= 1; k--) {
        ctx.fillStyle = `rgba(40, 0, 8, ${alpha * 0.55})`
        ctx.fillText(text, k * 0.9, k * 0.85)
      }
      ctx.fillStyle = `rgba(255, 130, 140, ${alpha * 0.95})`
      ctx.fillText(text, 0, 0)
      ctx.lineWidth = 0.6
      ctx.strokeStyle = `rgba(255, 200, 200, ${alpha * 0.35})`
      ctx.strokeText(text, 0, 0)
      ctx.restore()
    }
  }

  function drawBubbles() {
    for (const b of bubbles) {
      const t = 1 - b.life / b.maxLife
      const a = Math.max(0, b.life / b.maxLife) * (1 - t * 0.4)
      const hue = (b.hue + t * 28) % 360
      if (b.kind === 'fill') {
        const g = ctx.createRadialGradient(b.cx, b.cy, 0, b.cx, b.cy, b.r)
        g.addColorStop(0, `hsla(${hue}, 85%, 72%, ${a * 0.15})`)
        g.addColorStop(0.55, `hsla(${(hue + 40) % 360}, 70%, 55%, ${a * 0.08})`)
        g.addColorStop(1, 'transparent')
        ctx.fillStyle = g
        ctx.beginPath()
        ctx.arc(b.cx, b.cy, b.r, 0, Math.PI * 2)
        ctx.fill()
      } else {
        ctx.strokeStyle = `hsla(${hue}, 75%, 70%, ${a * 0.55})`
        ctx.lineWidth = b.w * (1 + t * 0.35)
        ctx.beginPath()
        ctx.arc(b.cx, b.cy, b.r, 0, Math.PI * 2)
        ctx.stroke()
        ctx.strokeStyle = `hsla(${(hue + 120) % 360}, 60%, 75%, ${a * 0.22})`
        ctx.lineWidth = b.w * 0.45
        ctx.beginPath()
        ctx.arc(b.cx, b.cy, Math.max(0, b.r - 2), 0, Math.PI * 2)
        ctx.stroke()
      }
    }
  }

  function draw() {
    ctx.clearRect(0, 0, w, h)
    drawSpaceBackdrop()
    drawOrbs()
    drawCells()
    draw404Floaters()
    drawDots()
    drawBubbles()
    maybeAbsorbedCallback()
  }

  function maybeAbsorbedCallback() {
    if (!glitchSuction || voidTriggered) return
    const overload = suctionCracks.length >= 14
    const empty = float404.length === 0
    if (empty || overload) {
      voidTriggered = true
      if (overload && float404.length > 0) float404.length = 0
      try {
        window.__solea404OnAllAbsorbed?.()
      } catch {
        /* ignore */
      }
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

  function spawnBubblesAt(cx, cy, count, strong) {
    const baseHue = 0 + Math.random() * 18
    for (let i = 0; i < count; i++) {
      const off = (i / count) * 14
      bubbles.push({
        cx: cx + (Math.random() - 0.5) * (strong ? 28 : 12),
        cy: cy + (Math.random() - 0.5) * (strong ? 22 : 10),
        r: 2 + Math.random() * (strong ? 8 : 4),
        life: (strong ? 0.95 : 0.65) + Math.random() * 0.35,
        maxLife: 0,
        w: strong ? 1.35 : 0.95,
        hue: baseHue + off + i * 6,
        kind: Math.random() > 0.35 ? 'ring' : 'fill',
      })
      bubbles[bubbles.length - 1].maxLife = bubbles[bubbles.length - 1].life
    }
  }

  function clientToCanvas(clientX, clientY) {
    const rect = canvas.getBoundingClientRect()
    const sx = ((clientX - rect.left) / rect.width) * w
    const sy = ((clientY - rect.top) / rect.height) * h
    return { x: sx, y: sy }
  }

  function hitFloat404(px, py) {
    ctx.save()
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const text = '404'
    for (let i = float404.length - 1; i >= 0; i--) {
      const f = float404[i]
      const scale = 0.42 + f.z * 0.95
      const fontPx = f.fontPx * scale
      ctx.font = `${fontPx}px "Press Start 2P", ui-monospace, monospace`
      const m = ctx.measureText(text)
      const tw = m.width
      const th = fontPx * 1.1
      const cos = Math.cos(f.rot)
      const sin = Math.sin(f.rot)
      const dx = px - f.x
      const dy = py - f.y
      const lx = dx * cos + dy * sin
      const ly = -dx * sin + dy * cos
      if (lx >= -tw / 2 - 8 && lx <= tw / 2 + 8 && ly >= -th / 2 - 8 && ly <= th / 2 + 8) {
        ctx.restore()
        return f
      }
    }
    ctx.restore()
    return null
  }

  function onPointerDown(e) {
    if (reduce) return
    const ph404 = window.__solea404Phase
    if (
      ph404 === 'glitch' ||
      ph404 === 'void' ||
      ph404 === 'collapsed' ||
      (typeof ph404 === 'string' && ph404.startsWith('void_sequel'))
    )
      return
    if (e.pointerType === 'mouse' && e.button !== 0) return
    const t = e.target
    if (t && t.closest && t.closest('a, button, input, textarea, select, label, .lang-switch, dialog'))
      return
    const { x, y } = clientToCanvas(e.clientX, e.clientY)
    const hit = hitFloat404(x, y)
    if (hit) {
      const dx = hit.x - x
      const dy = hit.y - y
      const len = Math.hypot(dx, dy) || 1
      const push = 180 * (0.4 + hit.z * 0.6)
      hit.vx += (dx / len) * push
      hit.vy += (dy / len) * push
      hit.vr += (Math.random() - 0.5) * 0.02
      spawnBubblesAt(hit.x, hit.y, 10, true)
      spawnBubblesAt(x, y, 4, false)
    } else {
      spawnBubblesAt(x, y, 5, false)
    }
  }

  window.addEventListener('pointerdown', onPointerDown, { passive: true })

  function clientToCanvasLogical(clientX, clientY) {
    const rect = canvas.getBoundingClientRect()
    const sx = ((clientX - rect.left) / rect.width) * w
    const sy = ((clientY - rect.top) / rect.height) * h
    return { x: sx, y: sy }
  }

  window.Solea404Particles = {
    /** Test / chapitres : déclenche la fin d’absorption comme si tous les 404 flottants avaient été aspirés */
    forceAbsorb() {
      if (!glitchSuction) return
      float404.length = 0
      maybeAbsorbedCallback()
    },
    addCrack(clientX, clientY) {
      if (!glitchSuction || suctionCracks.length >= MAX_CRACKS) return
      const { x, y } = clientToCanvasLogical(clientX, clientY)
      suctionCracks.push({ x, y, w: 0.85 + Math.random() * 0.45 })
    },
    setGlitchSuction(on) {
      glitchSuction = !!on
      if (on) {
        initSpaceStars()
      } else {
        suctionCracks.length = 0
        voidPull = false
        voidStartTime = 0
        spaceStars = []
      }
    },
    setVoidPull(on) {
      voidPull = !!on
      voidStartTime = on ? performance.now() : 0
    },
    resetVortexState() {
      suctionCracks.length = 0
      glitchSuction = false
      voidPull = false
      voidStartTime = 0
      voidTriggered = false
      spaceStars = []
      window.__solea404OnAllAbsorbed = null
    },
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      document.fonts.ready.then(boot).catch(boot)
    })
  } else {
    document.fonts.ready.then(boot).catch(boot)
  }
})()
