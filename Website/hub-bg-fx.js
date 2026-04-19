/**
 * Fond accueil : petits cubes filaires (RAW_VERTS + arêtes) + glitch éventuel.
 */
;(function () {
  const canvas = document.getElementById('solea-hub-bg-fx')
  if (!canvas || !canvas.getContext) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const ctx = canvas.getContext('2d', { alpha: true })
  if (!ctx) return

  const CUBE_EDGES = [
    [0, 1],
    [1, 2],
    [2, 3],
    [3, 0],
    [4, 5],
    [5, 6],
    [6, 7],
    [7, 4],
    [0, 4],
    [1, 5],
    [2, 6],
    [3, 7],
  ]

  const RAW_VERTS = [
    [-1, -1, -1],
    [1, -1, -1],
    [1, 1, -1],
    [-1, 1, -1],
    [-1, -1, 1],
    [1, -1, 1],
    [1, 1, 1],
    [-1, 1, 1],
  ]

  const scheme = {
    ink: { r: 248, g: 250, b: 252 },
    glitch: { r: 255, g: 72, b: 88 },
    lineMul: 1,
  }

  let w = 0
  let h = 0
  let dpr = 1
  let raf = 0
  let running = true
  let scrollY = 0

  /** @type {{ px: number; py: number; pz: number; half: number; ax: number; ay: number; az: number; wx: number; wy: number; wz: number; rotStep: number; driftAx: number; driftAy: number; driftAmp: number; driftFreq: number; aBase: number; layer: number; bornAt: number }[]} */
  let cubes = []

  const ptr = { tx: 0, ty: 0, lx: 0, ly: 0 }
  let nextGlitchAt = 0
  /** @type {null | (typeof cubes)[0]} */
  let glitchRef = null
  let glitchUntil = 0

  const snapRoot = document.getElementById('contenu-principal')
  const mqLight = window.matchMedia('(prefers-color-scheme: light)')

  function applyColorScheme() {
    if (mqLight.matches) {
      scheme.ink = { r: 36, g: 40, b: 50 }
      scheme.glitch = { r: 180, g: 40, b: 52 }
      scheme.lineMul = 1.4
    } else {
      scheme.ink = { r: 248, g: 250, b: 252 }
      scheme.glitch = { r: 255, g: 72, b: 88 }
      scheme.lineMul = 1
    }
  }

  function clamp(v, lo, hi) {
    return Math.max(lo, Math.min(hi, v))
  }

  function activeSectionId() {
    return document.documentElement.dataset.activeSection || 'hub'
  }

  /** Cubes denses (téléchargements) vs léger — ne pas confondre avec l’URL tant qu’un fondu de sortie est actif */
  let cubeArrayHeavy = false
  /** 0..1 : fondu d’entrée / sortie du lot « dense » (téléchargements) */
  let denseBlend = 1
  let denseBlendTarget = 1
  let lastObservedDataSection = ''

  function cubeCountForDensity(area, heavy) {
    if (heavy) {
      return clamp(14 + Math.floor(area / 95000), 16, 28)
    }
    return clamp(4 + Math.floor(area / 240000), 5, 8)
  }

  function rotate(v, ax, ay, az) {
    let { x, y, z } = v
    const cx = Math.cos(ax)
    const sx = Math.sin(ax)
    let ny = y * cx - z * sx
    let nz = y * sx + z * cx
    y = ny
    z = nz
    const cy = Math.cos(ay)
    const sy = Math.sin(ay)
    let nx = x * cy + z * sy
    nz = -x * sy + z * cy
    x = nx
    z = nz
    const cz = Math.cos(az)
    const sz = Math.sin(az)
    nx = x * cz - y * sz
    ny = x * sz + y * cz
    return { x: nx, y: ny, z: nz }
  }

  function project(wx, wy, wz, originX, originY) {
    const focal = 340
    const denom = Math.max(52, focal - wz)
    const sc = focal / denom
    return { x: originX + wx * sc, y: originY + wy * sc, sc }
  }

  function wrapCube(p, half) {
    const m = half > 55 ? 160 + half * 0.35 : 110
    if (p.px < -m) p.px = w + m
    if (p.px > w + m) p.px = -m
    if (p.py < -m) p.py = h + m
    if (p.py > h + m) p.py = -m
  }

  function initCubes() {
    cubes = []
    const area = w * h
    const heavy = cubeArrayHeavy
    const n = cubeCountForDensity(area, heavy)
    const downloadsBoost = heavy
    const bornAt = performance.now()
    for (let i = 0; i < n; i += 1) {
      const roll = Math.random()
      let half
      if (roll < 0.38) {
        half = 4.5 + Math.random() * 6.2
      } else if (roll < 0.82) {
        half = 11 + Math.random() * 10
      } else {
        half = 24 + Math.random() * 20
      }

      const layer = Math.floor(Math.random() * 3)
      const layerDrift = layer === 0 ? 0.52 : layer === 2 ? 1.22 : 1
      const layerRot = layer === 0 ? 0.82 : layer === 2 ? 1.12 : 1
      const layerAlpha = layer === 0 ? 0.82 : layer === 2 ? 1.08 : 1

      const rotScale = 0.07 + Math.random() * 0.62
      const rotStep = (0.0095 + Math.random() * 0.024) * layerRot * (downloadsBoost ? 1.08 : 1)

      cubes.push({
        px: Math.random() * w,
        py: Math.random() * h,
        pz: (Math.random() - 0.5) * 62,
        half,
        ax: Math.random() * Math.PI * 2,
        ay: Math.random() * Math.PI * 2,
        az: Math.random() * Math.PI * 2,
        wx: (Math.random() - 0.5) * 2 * rotScale,
        wy: (Math.random() - 0.5) * 2 * rotScale,
        wz: (Math.random() - 0.5) * 2 * rotScale,
        rotStep,
        driftAx: Math.random() * Math.PI * 2,
        driftAy: Math.random() * Math.PI * 2,
        driftAmp: (0.06 + Math.random() * 0.26) * layerDrift * (downloadsBoost ? 1.22 : 1),
        driftFreq: (0.00004 + Math.random() * 0.00012) * (downloadsBoost ? 1.15 : 1),
        aBase:
          (half > 20 ? 0.04 + Math.random() * 0.05 : 0.075 + Math.random() * 0.09) *
          layerAlpha *
          (downloadsBoost ? 1.28 : 1),
        layer,
        bornAt,
      })
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
    if (w < 32 || h < 32) return
    ptr.tx = ptr.lx = w * 0.5
    ptr.ty = ptr.ly = h * 0.5
    nextGlitchAt = performance.now() + 2000 + Math.random() * 4000
    applyColorScheme()
    lastObservedDataSection = activeSectionId()
    cubeArrayHeavy = lastObservedDataSection === 'downloads'
    denseBlend = 1
    denseBlendTarget = 1
    initCubes()
  }

  const DENSE_BLEND_LERP = 0.075
  const DENSE_FADE_OUT_THRESHOLD = 0.04
  const SPAWN_FADE_MS = 560

  function syncCubesWithDataSection() {
    if (w < 32 || h < 32) return
    const now = document.documentElement.dataset.activeSection || 'hub'
    if (now === lastObservedDataSection) return
    lastObservedDataSection = now

    const wantHeavy = now === 'downloads'

    if (wantHeavy && !cubeArrayHeavy) {
      cubeArrayHeavy = true
      initCubes()
      denseBlend = 0
      denseBlendTarget = 1
      return
    }

    if (wantHeavy && cubeArrayHeavy) {
      denseBlendTarget = 1
      return
    }

    if (!wantHeavy && cubeArrayHeavy) {
      denseBlendTarget = 0
    }
  }

  function buildCubeVerts(c) {
    const verts = []
    for (const rv of RAW_VERTS) {
      const rx = rv[0] * c.half
      const ry = rv[1] * c.half
      const rz = rv[2] * c.half
      const p = rotate({ x: rx, y: ry, z: rz }, c.ax, c.ay, c.az)
      verts.push(project(p.x, p.y, p.z + c.pz, c.px, c.py))
    }
    return verts
  }

  function evolveCube(c, t) {
    c.ax += c.wx * c.rotStep
    c.ay += c.wy * c.rotStep
    c.az += c.wz * c.rotStep
    c.px += Math.sin(t * c.driftFreq + c.driftAx) * c.driftAmp
    c.py += Math.cos(t * c.driftFreq * 0.88 + c.driftAy) * c.driftAmp * 0.92
    c.pz += Math.sin(t * 0.000042 + c.ax * 0.5) * 0.055
    c.pz = clamp(c.pz, -58, 58)
    wrapCube(c, c.half)
  }

  function drawCubeWire(c, t) {
    const verts = buildCubeVerts(c)

    const depth = (verts[0].sc + verts[6].sc) * 0.5
    const dlBoost = cubeArrayHeavy ? 1.22 : 1
    const spawnFade = Math.min(1, Math.max(0, (t - c.bornAt) / SPAWN_FADE_MS))
    let alpha =
      c.aBase *
      clamp(0.5 + depth * 0.42, 0.35, 1.08) *
      scheme.lineMul *
      dlBoost *
      spawnFade *
      denseBlend

    const { r: R, g: G, b: B } = scheme.ink
    const { r: Gr, g: Gg, b: Gb } = scheme.glitch
    const glitchOn = glitchRef === c && t < glitchUntil
    const lw = clamp(0.48 + (c.half / 95) * 0.5 + depth * 0.2, 0.42, 1.12)

    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.miterLimit = 10

    function strokeEdges(offX, offY, stroke, lineW) {
      ctx.strokeStyle = stroke
      ctx.lineWidth = lineW
      for (const [a, b] of CUBE_EDGES) {
        const va = verts[a]
        const vb = verts[b]
        ctx.beginPath()
        ctx.moveTo(va.x + offX, va.y + offY)
        ctx.lineTo(vb.x + offX, vb.y + offY)
        ctx.stroke()
      }
    }

    if (glitchOn) {
      const gx = (Math.random() - 0.5) * 3.5
      const gy = (Math.random() - 0.5) * 2.5
      strokeEdges(gx, gy, `rgba(${R},${G},${B},${alpha * 1.15})`, lw * 1.05)
      strokeEdges(-gx * 0.6, gy, `rgba(${Gr},${Gg},${Gb},${alpha * 0.35})`, lw * 0.9)
    } else {
      strokeEdges(0, 0, `rgba(${R},${G},${B},${alpha})`, lw)
    }
  }

  function scheduleGlitch(t) {
    if (t < nextGlitchAt) return
    nextGlitchAt = t + 3200 + Math.random() * 7000
    glitchRef = null
    if (cubes.length && Math.random() < 0.72) {
      glitchRef = cubes[Math.floor(Math.random() * cubes.length)]
      glitchUntil = t + 55 + Math.random() * 95
    }
  }

  function onPtr(e) {
    ptr.tx = e.clientX
    ptr.ty = e.clientY
  }

  function onScroll() {
    if (snapRoot) scrollY = snapRoot.scrollTop
  }

  function frame(now) {
    if (!running) return
    const t = now || performance.now()
    ptr.lx += (ptr.tx - ptr.lx) * 0.065
    ptr.ly += (ptr.ty - ptr.ly) * 0.065

    const shiftX = (ptr.lx - w * 0.5) * 0.024 + scrollY * 0.0035
    const shiftY = (ptr.ly - h * 0.5) * 0.019 - scrollY * 0.011

    ctx.clearRect(0, 0, w, h)
    scheduleGlitch(t)

    denseBlend += (denseBlendTarget - denseBlend) * DENSE_BLEND_LERP
    if (denseBlendTarget === 1 && denseBlend > 0.998) denseBlend = 1
    if (
      denseBlendTarget === 0 &&
      denseBlend < DENSE_FADE_OUT_THRESHOLD &&
      cubeArrayHeavy
    ) {
      cubeArrayHeavy = false
      denseBlend = 1
      denseBlendTarget = 1
      initCubes()
    }

    ctx.save()
    ctx.translate(shiftX, shiftY)

    for (const c of cubes) {
      evolveCube(c, t)
      drawCubeWire(c, t)
    }

    ctx.restore()

    raf = window.requestAnimationFrame(frame)
  }

  function start() {
    if (raf) return
    raf = window.requestAnimationFrame(frame)
  }

  function stop() {
    if (raf) window.cancelAnimationFrame(raf)
    raf = 0
  }

  function onVis() {
    if (document.hidden) {
      running = false
      stop()
    } else {
      running = true
      start()
    }
  }

  function onSchemeChange() {
    applyColorScheme()
  }

  applyColorScheme()
  resize()

  const sectionObs = new MutationObserver(() => {
    syncCubesWithDataSection()
  })
  sectionObs.observe(document.documentElement, { attributes: true, attributeFilter: ['data-active-section'] })

  window.addEventListener('resize', resize, { passive: true })
  window.addEventListener('pointermove', onPtr, { passive: true })
  if (snapRoot) snapRoot.addEventListener('scroll', onScroll, { passive: true })
  mqLight.addEventListener('change', onSchemeChange)
  document.addEventListener('visibilitychange', onVis)
  start()

  window.addEventListener(
    'beforeunload',
    () => {
      running = false
      stop()
      window.removeEventListener('resize', resize)
      sectionObs.disconnect()
      window.removeEventListener('pointermove', onPtr)
      if (snapRoot) snapRoot.removeEventListener('scroll', onScroll)
      mqLight.removeEventListener('change', onSchemeChange)
      document.removeEventListener('visibilitychange', onVis)
    },
    { once: true }
  )
})()
