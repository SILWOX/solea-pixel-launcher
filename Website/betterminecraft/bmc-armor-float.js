/**
 * Mannequins 3D (skinview3d) + merge armure (armor-merge.mjs) — fond latéral BMC.
 */
import { loadSkinToCanvas, loadArmorToCanvas } from './armor-merge.mjs'

const STEVE = '/assets/bmc-showcase/steve-skin.png'
const EMERALD_L1 = '/assets/bmc-showcase/emerald-armor-layer1.png'
const EMERALD_L2 = '/assets/bmc-showcase/emerald-armor-layer2.png'
const ENDERITE_L1 = '/assets/bmc-showcase/enderite-armor-layer1.png'
const ENDERITE_L2 = '/assets/bmc-showcase/enderite-armor-layer2.png'

function waitSkinview3d() {
  return new Promise((resolve) => {
    if (globalThis.skinview3d) {
      resolve(globalThis.skinview3d)
      return
    }
    const id = setInterval(() => {
      if (globalThis.skinview3d) {
        clearInterval(id)
        resolve(globalThis.skinview3d)
      }
    }, 30)
  })
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.decoding = 'async'
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error(`Failed to load ${src}`))
    img.src = src
  })
}

function mergeSteveAndArmor(steveImg, l1, l2) {
  const merged = document.createElement('canvas')
  loadSkinToCanvas(merged, steveImg)
  const armor = document.createElement('canvas')
  loadArmorToCanvas(armor, l1, l2)
  const ctx = merged.getContext('2d')
  ctx.drawImage(armor, 0, 0)
  return merged
}

function createViewer(sv, canvasEl, skinCanvas, reduceMotion) {
  const rect = canvasEl.getBoundingClientRect()
  const w = Math.max(120, Math.round(rect.width || 160))
  const h = Math.max(200, Math.round(rect.height || 260))

  const viewer = new sv.SkinViewer({
    canvas: canvasEl,
    width: w,
    height: h,
    skin: skinCanvas,
    alpha: true,
  })

  viewer.background = null
  viewer.nameTag = null
  if (typeof viewer.loadCape === 'function') viewer.loadCape(null)

  viewer.globalLight.intensity = 0.5
  viewer.cameraLight.intensity = 0.82
  viewer.zoom = 0.78

  if (!reduceMotion) {
    viewer.autoRotate = true
  }

  return viewer
}

function bindResize(canvasEl, viewer) {
  if (!viewer || typeof ResizeObserver === 'undefined') return () => {}
  const ro = new ResizeObserver(() => {
    const r = canvasEl.getBoundingClientRect()
    const w = Math.max(100, Math.round(r.width))
    const h = Math.max(180, Math.round(r.height))
    viewer.width = w
    viewer.height = h
  })
  ro.observe(canvasEl.parentElement || canvasEl)
  return () => ro.disconnect()
}

async function main() {
  const left = document.getElementById('bmc-armor-float-emerald')
  const right = document.getElementById('bmc-armor-float-enderite')
  if (!left || !right) return

  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

  try {
    const sv = await waitSkinview3d()
    const steve = await loadImage(STEVE)
    const [e1, e2, x1, x2] = await Promise.all([
      loadImage(EMERALD_L1),
      loadImage(EMERALD_L2),
      loadImage(ENDERITE_L1),
      loadImage(ENDERITE_L2),
    ])

    const skinEmerald = mergeSteveAndArmor(steve, e1, e2)
    const skinEnderite = mergeSteveAndArmor(steve, x1, x2)

    const v1 = createViewer(sv, left, skinEmerald, reduceMotion)
    const v2 = createViewer(sv, right, skinEnderite, reduceMotion)

    const off1 = bindResize(left, v1)
    const off2 = bindResize(right, v2)

    window.addEventListener(
      'solea-lang-change',
      () => {
        /* texture identique toutes langues — rien */
      },
      { passive: true },
    )

    window.addEventListener(
      'pagehide',
      () => {
        off1()
        off2()
        try {
          v1.dispose()
        } catch {
          /* ignore */
        }
        try {
          v2.dispose()
        } catch {
          /* ignore */
        }
      },
      { once: true },
    )
  } catch (e) {
    console.warn('[bmc-armor-float]', e)
    left.closest('.bmc-float-armor')?.classList.add('bmc-float-armor--failed')
    right.closest('.bmc-float-armor')?.classList.add('bmc-float-armor--failed')
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', main)
} else {
  main()
}
