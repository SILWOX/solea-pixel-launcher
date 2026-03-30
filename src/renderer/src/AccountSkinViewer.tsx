import { forwardRef, useEffect, useImperativeHandle, useRef } from 'react'
import { IdleAnimation, NameTagObject, RunningAnimation, SkinViewer } from 'skinview3d'
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
      if (animation === 'idle') {
        v.animation = new IdleAnimation()
        return
      }
      v.animation = new RunningAnimation()
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
      if (skinDataUrl) {
        const m = model === 'auto-detect' ? 'auto-detect' : model
        void v.loadSkin(skinDataUrl, { model: m, ears: 'load-only' })
      } else {
        v.resetSkin()
      }
    }, [skinDataUrl, model])

    useEffect(() => {
      const v = viewerRef.current
      if (!v || v.disposed) return
      if (capeDataUrl) {
        void v.loadCape(capeDataUrl, { backEquipment: 'cape' })
      } else {
        v.resetCape()
      }
    }, [capeDataUrl])

    return <div className="account-skin-viewer-host" ref={hostRef} />
  }
)
