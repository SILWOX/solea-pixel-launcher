/** AETHER UI — V1 | Solea Pixel Launcher (proprietary interface layer). */
import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react'
import {
  CrouchAnimation,
  FlyingAnimation,
  HitAnimation,
  IdleAnimation,
  NameTagObject,
  RunningAnimation,
  SkinViewer,
  WalkingAnimation,
  WaveAnimation
} from 'skinview3d'
import type { SkinViewerAnimation } from './launcherTypes'

export type AccountSkinViewerHandle = {
  exportPng: () => string | null
}

type AccountSkinViewerProps = {
  skinDataUrl: string | null
  model: 'slim' | 'default' | 'auto-detect'
  capeDataUrl: string | null
  playerName: string
  viewerBackground: string
  animation: SkinViewerAnimation
  reduceMotion: boolean
}

const NAMETAG_FONT = '44px "Mac Minecraft", "Segoe UI", sans-serif'

function useMinecraftFontReady() {
  useEffect(() => {
    void document.fonts.load(NAMETAG_FONT)
  }, [])
}

export const AccountSkinViewer = forwardRef<AccountSkinViewerHandle, AccountSkinViewerProps>(
  function AccountSkinViewer(
    { skinDataUrl, model, capeDataUrl, playerName, viewerBackground, animation, reduceMotion },
    ref
  ) {
    useMinecraftFontReady()
    const hostRef = useRef<HTMLDivElement>(null)
    const viewerRef = useRef<SkinViewer | null>(null)
    const skinLoadGen = useRef(0)
    const capeLoadGen = useRef(0)

    useImperativeHandle(ref, () => ({
      exportPng: () => {
        const v = viewerRef.current
        if (!v || v.disposed) return null
        try {
          return v.canvas.toDataURL('image/png')
        } catch {
          return null
        }
      }
    }))

    useEffect(() => {
      const host = hostRef.current
      if (!host) return

      const canvas = document.createElement('canvas')
      host.appendChild(canvas)

      const w = Math.floor(host.clientWidth) || 400
      const h = Math.floor(host.clientHeight) || 480

      const viewer = new SkinViewer({
        canvas,
        width: w,
        height: h,
        enableControls: true,
        background: viewerBackground || '#141416',
        zoom: 0.86,
        model: 'auto-detect'
      })

      viewer.controls.enablePan = false
      viewer.controls.minDistance = 14
      viewer.controls.maxDistance = 52
      viewerRef.current = viewer

      const ro = new ResizeObserver(() => {
        if (viewer.disposed || !hostRef.current) return
        const { clientWidth, clientHeight } = hostRef.current
        if (clientWidth > 0 && clientHeight > 0) {
          viewer.setSize(clientWidth, clientHeight)
          viewer.adjustCameraDistance()
        }
      })
      ro.observe(host)

      return () => {
        ro.disconnect()
        viewer.dispose()
        viewerRef.current = null
        canvas.remove()
      }
    }, [])

    useEffect(() => {
      const v = viewerRef.current
      if (!v || v.disposed) return
      v.background = viewerBackground || '#141416'
    }, [viewerBackground])

    useEffect(() => {
      const v = viewerRef.current
      if (!v || v.disposed) return
      if (reduceMotion || animation === 'none') {
        v.animation = null
        return
      }
      switch (animation) {
        case 'idle':
          v.animation = new IdleAnimation()
          break
        case 'walk':
          v.animation = new WalkingAnimation()
          break
        case 'run':
          v.animation = new RunningAnimation()
          break
        case 'fly':
          v.animation = new FlyingAnimation()
          break
        case 'wave':
          v.animation = new WaveAnimation('right')
          break
        case 'wave_left':
          v.animation = new WaveAnimation('left')
          break
        case 'crouch':
          v.animation = new CrouchAnimation()
          break
        case 'hit':
          v.animation = new HitAnimation()
          break
        default:
          v.animation = null
      }
    }, [animation, reduceMotion])

    useEffect(() => {
      const v = viewerRef.current
      if (!v || v.disposed) return
      const name = playerName.trim()
      if (!name) {
        v.nameTag = null
        return
      }
      v.nameTag = new NameTagObject(name, {
        font: NAMETAG_FONT,
        repaintAfterLoaded: true,
        textStyle: '#ffffff',
        backgroundStyle: 'rgba(0,0,0,0.4)'
      })
    }, [playerName])

    useEffect(() => {
      const v = viewerRef.current
      if (!v || v.disposed) return
      const gen = ++skinLoadGen.current
      if (skinDataUrl) {
        const m = model === 'auto-detect' ? 'auto-detect' : model
        const out = v.loadSkin(skinDataUrl, { model: m, ears: 'load-only' })
        void Promise.resolve(out).then(
          () => {
            if (skinLoadGen.current !== gen || v.disposed) return
          },
          () => {
            if (skinLoadGen.current !== gen || v.disposed) return
          }
        )
      } else {
        v.resetSkin()
      }
    }, [skinDataUrl, model])

    useEffect(() => {
      const v = viewerRef.current
      if (!v || v.disposed) return
      const gen = ++capeLoadGen.current
      if (capeDataUrl) {
        const out = v.loadCape(capeDataUrl, { backEquipment: 'cape' })
        void Promise.resolve(out).then(
          () => {
            if (capeLoadGen.current !== gen || v.disposed) return
          },
          () => {
            if (capeLoadGen.current !== gen || v.disposed) return
          }
        )
      } else {
        v.resetCape()
      }
    }, [capeDataUrl])

    return <div className="account-skin-viewer-host" ref={hostRef} />
  }
)
