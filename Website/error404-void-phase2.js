/**
 * Suite 404 après le premier dialogue : Pac-Man enragé (60 s), bouton or, intrigue piratage,
 * cinématique 404 / fusée, faux écran licence piégé.
 */
;(function () {
  const PAC_GOAL_MS = 60000
  const ROCKET_HOLD_MS = 3000
  const BLACK_BEFORE_RED_MS = 3000
  const LICENSE_AFTER_REPAIR_MS = 4000
  const FRAG_SPAWN_MS = 1400

  let started = false
  let pacRaf = 0
  let mazeCleanup = null
  let keys = /** @type {Set<string>} */ (new Set())
  let keyEv = null
  let blurClearKeys = null

  function fr() {
    return (document.documentElement.getAttribute('lang') || 'en').toLowerCase().startsWith('fr')
  }

  function setPhase(p) {
    window.__solea404Phase = p
  }

  function $(id) {
    return document.getElementById(id)
  }

  function start() {
    if (started) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
    const root = $('err-void-p2')
    const canvas = $('err-void-p2-canvas')
    const hud = $('err-void-p2-hud')
    const fragLayer = $('err-void-p2-frag')
    const intro = $('err-void-p2-intro')
    const introLose = $('err-void-p2-intro-lose')
    const launchBtn = $('err-void-p2-launch')
    const goldWrap = $('err-void-p2-gold')
    const goldBtn = $('err-void-p2-gold-btn')
    const dlgHack = $('err-void-p2-dlg-hack')
    const hackBody = $('err-void-p2-hack-body')
    const cine = $('err-void-p2-cine')
    const black = $('err-void-p2-black')
    const red404 = $('err-void-p2-red404')
    const rocketHint = $('err-void-p2-rocket-hint')
    const rocket = $('err-easter-rocket')
    const lic = $('err-void-p2-license')
    const licScroll = $('err-void-p2-license-scroll')
    const licFeed = $('err-void-p2-license-feed')
    const catch404 = $('err-void-p2-catch404')
    const dlgWin = $('err-void-p2-dlg-win')

    if (
      !root ||
      !canvas ||
      !hud ||
      !fragLayer ||
      !intro ||
      !introLose ||
      !launchBtn ||
      !goldWrap ||
      !goldBtn ||
      !dlgHack ||
      !hackBody ||
      !cine ||
      !black ||
      !red404 ||
      !rocketHint ||
      !rocket ||
      !lic ||
      !licScroll ||
      !licFeed ||
      !catch404 ||
      !dlgWin
    )
      return

    started = true
    setPhase('void_sequel_pacman')

    const SITE_FRAGS = fr()
      ? [
          'Télécharger',
          'Licence SPL',
          'SOLEA PIXEL',
          'Modrinth',
          'Discord',
          '404',
          'Accueil',
          'FAQ',
          'Launcher',
          'ERROR',
        ]
      : ['Download', 'SPL License', 'SOLEA PIXEL', 'Modrinth', 'Discord', '404', 'Home', 'FAQ', 'Launcher', 'ERROR']

    let w = 0
    let h = 0
    let dpr = 1
    let ctx = /** @type {CanvasRenderingContext2D | null} */ (null)
    let t0 = 0
    let lost = false
    let won = false
    let fragIv = 0
    let px = 0
    let py = 0
    const pr = 15
    const ghosts = []
    let playing = false

    /** @param {KeyboardEvent} e @param {boolean} down */
    function syncKeyFromEvent(e, down) {
      const map = /** @type {Record<string, string>} */ ({
        ArrowUp: 'up',
        ArrowDown: 'down',
        ArrowLeft: 'left',
        ArrowRight: 'right',
        KeyW: 'up',
        KeyS: 'down',
        KeyA: 'left',
        KeyD: 'right',
        KeyZ: 'up',
        KeyQ: 'left',
      })
      const k = map[e.code]
      if (!k) return
      if (down) keys.add(k)
      else keys.delete(k)
      if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.code)) e.preventDefault()
    }

    function resize() {
      dpr = Math.min(window.devicePixelRatio || 1, 2)
      w = window.innerWidth
      h = window.innerHeight
      canvas.width = Math.floor(w * dpr)
      canvas.height = Math.floor(h * dpr)
      canvas.style.width = `${w}px`
      canvas.style.height = `${h}px`
      ctx = canvas.getContext('2d')
      if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      px = w * 0.5
      py = h * 0.55
    }

    function resetGhosts() {
      ghosts.length = 0
      const cols = ['#ff5b5b', '#ffb84d', '#6ecbff', '#c56bff']
      for (let i = 0; i < 4; i += 1) {
        const ang = (i / 4) * Math.PI * 2
        ghosts.push({
          x: w * 0.5 + Math.cos(ang) * 120,
          y: h * 0.5 + Math.sin(ang) * 120,
          col: cols[i],
          r: 13,
        })
      }
    }

    function showIntro(isRetry) {
      intro.removeAttribute('hidden')
      intro.setAttribute('aria-hidden', 'false')
      if (isRetry && introLose) {
        introLose.textContent = fr()
          ? "Les orbes t’ont eu — recommence quand tu es prêt."
          : 'The orbs got you — try again when you are ready.'
        introLose.removeAttribute('hidden')
      } else if (introLose) {
        introLose.setAttribute('hidden', '')
        introLose.textContent = ''
      }
      launchBtn.setAttribute('data-i18n', isRetry ? 'page404.voidP2PacRetry' : 'page404.voidP2PacLaunch')
      if (window.SoleaI18n && typeof window.SoleaI18n.apply === 'function') window.SoleaI18n.apply()
      else {
        launchBtn.textContent = isRetry ? (fr() ? 'Recommencer' : 'Try again') : fr() ? 'Lancer' : 'Start'
      }
      window.setTimeout(() => launchBtn.focus(), 0)
    }

    function hideIntro() {
      intro.setAttribute('hidden', '')
      intro.setAttribute('aria-hidden', 'true')
    }

    function beginPacRun() {
      if (playing) return
      playing = true
      hideIntro()
      lost = false
      won = false
      tick.prev = null
      keys.clear()
      keyEv = {
        down: (e) => {
          syncKeyFromEvent(e, true)
        },
        up: (e) => {
          syncKeyFromEvent(e, false)
        },
      }
      window.addEventListener('keydown', keyEv.down, { passive: false })
      window.addEventListener('keyup', keyEv.up, { passive: true })
      blurClearKeys = () => {
        keys.clear()
      }
      window.addEventListener('blur', blurClearKeys)
      resize()
      hud.innerHTML = ''
      window.setTimeout(() => {
        try {
          root.focus({ preventScroll: true })
        } catch {
          root.focus()
        }
      }, 30)

      if (window.Solea404VoidMaze && typeof window.Solea404VoidMaze.start === 'function') {
        mazeCleanup = window.Solea404VoidMaze.start({
          canvas,
          root,
          fragLayer,
          hud,
          fr,
          keys,
          isPlaying: () => playing,
          onWin: () => {
            won = true
            endPac(true)
          },
          onLose: () => {
            lost = true
            endPac(false)
          },
          setRaf: (id) => {
            pacRaf = id
          },
          getRaf: () => pacRaf,
          cancelRaf: (id) => window.cancelAnimationFrame(id),
        })
        return
      }

      resetGhosts()
      t0 = performance.now()
      fragIv = window.setInterval(spawnFrag, FRAG_SPAWN_MS)
      pacRaf = window.requestAnimationFrame(tick)
    }

    function spawnFrag() {
      const s = document.createElement('span')
      s.className = 'page-error-void-p2-frag'
      s.textContent = SITE_FRAGS[Math.floor(Math.random() * SITE_FRAGS.length)]
      s.style.left = `${6 + Math.random() * 86}vw`
      s.style.top = `${8 + Math.random() * 72}vh`
      s.style.setProperty('--rot', `${(Math.random() - 0.5) * 14}deg`)
      fragLayer.appendChild(s)
      window.setTimeout(() => {
        try {
          s.remove()
        } catch {
          /* ignore */
        }
      }, 2600 + Math.random() * 1800)
    }

    function drawLogoGhost(alpha) {
      if (!ctx) return
      ctx.save()
      ctx.globalAlpha = alpha
      ctx.font = 'bold 22px "Press Start 2P", monospace'
      ctx.fillStyle = 'rgba(255,90,120,0.35)'
      ctx.fillText('404', 20 + Math.sin(performance.now() / 400) * 8, 48)
      ctx.restore()
    }

    function tick(now) {
      if (!ctx || !playing || lost || won) return
      if (tick.prev == null) tick.prev = now
      const t = now - t0
      const anger = Math.min(2.4, 1 + t / 38000)
      const pad = 48
      const dt = Math.min(0.05, (now - tick.prev) / 1000)
      tick.prev = now

      const sp = 248 * dt
      if (keys.has('up')) py -= sp
      if (keys.has('down')) py += sp
      if (keys.has('left')) px -= sp
      if (keys.has('right')) px += sp
      px = Math.max(pad, Math.min(w - pad, px))
      py = Math.max(pad, Math.min(h - pad, py))

      const gsp = (46 + t / 1050) * anger * dt
      for (const g of ghosts) {
        const dx = px - g.x
        const dy = py - g.y
        const len = Math.hypot(dx, dy) || 1
        g.x += (dx / len) * gsp
        g.y += (dy / len) * gsp
        if (Math.hypot(g.x - px, g.y - py) < pr + g.r - 2) {
          lost = true
          endPac(false)
          return
        }
      }

      ctx.fillStyle = '#030308'
      ctx.fillRect(0, 0, w, h)
      const gTime = performance.now() / 1000
      for (let i = 0; i < 40; i += 1) {
        const gx = ((i * 997) % w) + Math.sin(gTime + i) * 6
        const gy = ((i * 661) % h) + Math.cos(gTime * 0.8 + i * 0.2) * 4
        ctx.fillStyle = `rgba(40, 60, 120, ${0.04 + (i % 5) * 0.02})`
        ctx.fillRect(gx, gy, 2, 2)
      }
      drawLogoGhost(0.22 + Math.min(0.55, t / PAC_GOAL_MS) * 0.5)

      for (const g of ghosts) {
        ctx.beginPath()
        ctx.fillStyle = g.col
        ctx.arc(g.x, g.y, g.r * (0.95 + anger * 0.08), 0, Math.PI * 2)
        ctx.fill()
        ctx.fillStyle = 'rgba(0,0,0,0.55)'
        ctx.beginPath()
        ctx.arc(g.x - 4, g.y - 3, 2.2, 0, Math.PI * 2)
        ctx.arc(g.x + 4, g.y - 3, 2.2, 0, Math.PI * 2)
        ctx.fill()
      }

      const mouth = (Math.PI / 4) * (0.6 + 0.4 * Math.sin(gTime * 14))
      ctx.save()
      ctx.translate(px, py)
      ctx.rotate(Math.atan2(py - h * 0.5, px - w * 0.5) + Math.PI / 2)
      ctx.fillStyle = '#ffe94a'
      ctx.strokeStyle = `rgba(255,${80 - anger * 30},${40 - anger * 20},0.9)`
      ctx.lineWidth = 2 + anger
      ctx.beginPath()
      ctx.arc(0, 0, pr, mouth, Math.PI * 2 - mouth)
      ctx.lineTo(0, 0)
      ctx.fill()
      ctx.stroke()
      ctx.fillStyle = '#200'
      ctx.beginPath()
      ctx.arc(-5, -4, 2.5, 0, Math.PI * 2)
      ctx.arc(5, -4, 2.5, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()

      const shake = anger > 1.35 ? (anger - 1.35) * 3 : 0
      if (shake > 0) {
        root.style.transform = `translate(${(Math.random() - 0.5) * shake}px, ${(Math.random() - 0.5) * shake}px)`
      } else root.style.transform = ''

      const sec = Math.floor(t / 1000)
      const ms = Math.floor((t % 1000) / 10)
      hud.innerHTML = fr()
        ? `<div class="page-error-void-p2__hud-row"><strong>${sec}:${String(ms).padStart(2, '0')}</strong> / objectif 60s</div><div class="page-error-void-p2__rage">Pac enragé × ${anger.toFixed(2)}</div>`
        : `<div class="page-error-void-p2__hud-row"><strong>${sec}:${String(ms).padStart(2, '0')}</strong> / goal 60s</div><div class="page-error-void-p2__rage">Angry Pac × ${anger.toFixed(2)}</div>`

      if (t >= PAC_GOAL_MS) {
        won = true
        endPac(true)
        return
      }
      pacRaf = window.requestAnimationFrame(tick)
    }

    function endPac(success) {
      playing = false
      if (mazeCleanup) {
        mazeCleanup()
        mazeCleanup = null
      }
      if (pacRaf) window.cancelAnimationFrame(pacRaf)
      pacRaf = 0
      if (fragIv) window.clearInterval(fragIv)
      fragIv = 0
      if (keyEv) {
        window.removeEventListener('keydown', keyEv.down)
        window.removeEventListener('keyup', keyEv.up)
        keyEv = null
      }
      if (blurClearKeys) {
        window.removeEventListener('blur', blurClearKeys)
        blurClearKeys = null
      }
      keys.clear()
      root.style.transform = ''
      fragLayer.replaceChildren()
      if (!success) {
        hud.innerHTML = ''
        showIntro(true)
        return
      }
      hideIntro()
      if (introLose) {
        introLose.setAttribute('hidden', '')
        introLose.textContent = ''
      }
      root.setAttribute('hidden', '')
      root.setAttribute('aria-hidden', 'true')
      goldWrap.removeAttribute('hidden')
      goldWrap.removeAttribute('aria-hidden')
      goldWrap.classList.add('page-error-void-p2-gold--in')
      if (window.SoleaI18n && typeof window.SoleaI18n.apply === 'function') window.SoleaI18n.apply()
      goldBtn.focus()
    }

    function wireGoldBtn() {
      goldBtn.addEventListener('click', onGoldClick, { once: true })
    }

    function onGoldClick() {
      goldBtn.classList.add('page-error-void-p2-gold-btn--glitch')
      window.setTimeout(() => {
        goldBtn.classList.remove('page-error-void-p2-gold-btn--glitch')
        goldBtn.classList.add('page-error-void-p2-gold-btn--error')
        goldBtn.textContent = 'ERROR SYSTEM'
      }, 700)
      window.setTimeout(() => {
        goldWrap.classList.add('page-error-void-p2-gold--out')
      }, 2200)
      window.setTimeout(() => {
        goldWrap.setAttribute('hidden', '')
        goldWrap.setAttribute('aria-hidden', 'true')
        goldWrap.classList.remove('page-error-void-p2-gold--in', 'page-error-void-p2-gold--out')
        goldBtn.classList.remove('page-error-void-p2-gold-btn--error')
        goldBtn.textContent = fr() ? 'Continuer' : 'Continue'
        openHackDialog()
      }, 4200)
    }

    function typeLines(container, lines, done) {
      container.replaceChildren()
      let i = 0
      function one() {
        if (i >= lines.length) {
          done()
          return
        }
        const row = document.createElement('div')
        row.className = `page-error-void-p2-hack-line page-error-void-p2-hack-line--${lines[i].w}`
        const lab = document.createElement('span')
        lab.className = 'page-error-void-p2-hack-line__who'
        lab.textContent = lines[i].w === 'solea' ? 'SOLEA PIXEL' : '404'
        const p = document.createElement('p')
        p.className = 'page-error-void-p2-hack-line__tx'
        row.appendChild(lab)
        row.appendChild(p)
        container.appendChild(row)
        const full = lines[i].t
        let c = 0
        const id = window.setInterval(() => {
          c += 1
          p.textContent = full.slice(0, c)
          if (c >= full.length) {
            window.clearInterval(id)
            i += 1
            window.setTimeout(one, 360)
          }
        }, 18)
      }
      one()
    }

    function openHackDialog() {
      setPhase('void_sequel_hack')
      const lines = fr()
        ? [
            { w: 'solea', t: "Les journaux d'intégrité explosent. 404, qu'est-ce que tu as fait ?" },
            { w: '404', t: "J'ai ouvert une porte. La tienne." },
            { w: 'solea', t: 'Tu as piraté SOLEA. Rend le contrôle au visiteur.' },
            { w: '404', t: "Trop tard. Celui qui clique… je l'emprisonne aussi." },
            { w: 'solea', t: 'Il parle de toi — la personne sur le site.' },
            { w: '404', t: "Oui. Toi. Reste avec moi. Pour toujours. C'est plus cosy qu'un 403." },
          ]
        : [
            { w: 'solea', t: 'Integrity logs are on fire. 404, what did you do?' },
            { w: '404', t: 'I opened a door. Yours.' },
            { w: 'solea', t: 'You hijacked SOLEA. Free the visitor.' },
            { w: '404', t: 'Too late. The clicker stays in my loop too.' },
            { w: 'solea', t: 'You mean the person on this site.' },
            { w: '404', t: 'Yes. You. Stay with me. Forever. Cozier than a 403.' },
          ]
      typeLines(hackBody, lines, () => {
        const b = document.createElement('button')
        b.type = 'button'
        b.className = 'btn btn-download btn-lg'
        b.textContent = fr() ? 'Suite…' : 'Next…'
        b.addEventListener('click', () => {
          if (typeof dlgHack.close === 'function') dlgHack.close()
          runCinematic()
        })
        hackBody.appendChild(b)
      })
      if (typeof dlgHack.showModal === 'function') dlgHack.showModal()
      if (window.SoleaI18n && typeof window.SoleaI18n.apply === 'function') window.SoleaI18n.apply()
    }

    function runCinematic() {
      setPhase('void_sequel_cine')
      cine.removeAttribute('hidden')
      black.classList.add('page-error-void-p2-black--on')
      red404.classList.remove('page-error-void-p2-red404--show')
      window.setTimeout(() => {
        red404.classList.add('page-error-void-p2-red404--show')
      }, BLACK_BEFORE_RED_MS)
      window.setTimeout(() => {
        startRocketPhase()
      }, BLACK_BEFORE_RED_MS + 2400)
    }

    function startRocketPhase() {
      setPhase('void_sequel_rocket')
      red404.classList.remove('page-error-void-p2-red404--show')
      /** Le calque noir ciné restait actif et masquait tout le canvas (météorites, fond étoilé). */
      black.classList.remove('page-error-void-p2-black--on')
      rocketHint.removeAttribute('hidden')
      rocketHint.textContent = fr()
        ? "Fais entrer deux météorites l’une dans l’autre pour figer la fusée 5 s, puis garde le curseur dessus 3 s. Sinon elle esquive souvent."
        : 'Crash two meteors together to freeze the rocket for 5s, then hold your pointer on it for 3s. Otherwise it dodges aggressively.'
      rocket.removeAttribute('hidden')
      rocket.setAttribute('aria-hidden', 'false')
      rocket.classList.add('page-error-rocket--p2')
      let rx = window.innerWidth * 0.5
      let ry = -80
      let vx = 0
      let vy = 140
      let hold = 0
      let last = performance.now()
      let dodgeT = 0
      let rRocket = 0
      let stunUntil = 0
      /** @type {{ x:number,y:number,vx:number,vy:number,r:number}[]} */
      const meteors = []
      let gal = /** @type {HTMLCanvasElement | null} */ (document.getElementById('err-void-p2-galaxy'))
      if (!gal) {
        gal = document.createElement('canvas')
        gal.id = 'err-void-p2-galaxy'
        gal.className = 'page-error-void-p2-galaxy'
        cine.insertBefore(gal, cine.firstChild)
      }
      function sizeGal() {
        if (!gal) return
        const dpr = Math.min(window.devicePixelRatio || 1, 2)
        gal.width = Math.floor(window.innerWidth * dpr)
        gal.height = Math.floor(window.innerHeight * dpr)
        gal.style.width = `${window.innerWidth}px`
        gal.style.height = `${window.innerHeight}px`
        const gctx = gal.getContext('2d')
        if (gctx) gctx.setTransform(dpr, 0, 0, dpr, 0, 0)
      }
      sizeGal()
      gal.removeAttribute('hidden')
      for (let i = 0; i < 36; i += 1) {
        meteors.push({
          x: Math.random() * window.innerWidth,
          y: Math.random() * window.innerHeight,
          vx: (Math.random() - 0.5) * 28,
          vy: (Math.random() - 0.5) * 22,
          r: 8 + Math.random() * 12,
        })
      }

      function drawGalaxy(t) {
        if (!gal) return
        const gctx = gal.getContext('2d')
        if (!gctx) return
        const W = window.innerWidth
        const H = window.innerHeight
        const g = gctx.createRadialGradient(W * 0.35, H * 0.2, 0, W * 0.5, H * 0.55, Math.max(W, H))
        g.addColorStop(0, 'rgba(40, 60, 120, 0.5)')
        g.addColorStop(0.35, 'rgba(8, 4, 28, 0.95)')
        g.addColorStop(1, '#020008')
        gctx.fillStyle = g
        gctx.fillRect(0, 0, W, H)
        const tt = t / 1000
        for (let i = 0; i < 160; i += 1) {
          const sx = (i * 127) % W
          const sy = (i * 83) % H
          const tw = 0.4 + Math.sin(tt * 1.4 + i * 0.1) * 0.35
          gctx.fillStyle = `rgba(255, 255, 255, ${0.04 + tw * 0.08})`
          gctx.fillRect(sx, sy, 1.2, 1.2)
        }
        for (const m of meteors) {
          gctx.beginPath()
          gctx.fillStyle = 'rgba(220, 150, 95, 0.82)'
          gctx.arc(m.x, m.y, m.r, 0, Math.PI * 2)
          gctx.fill()
          gctx.strokeStyle = 'rgba(255, 220, 170, 0.55)'
          gctx.lineWidth = 1.5
          gctx.stroke()
          gctx.fillStyle = 'rgba(255, 245, 200, 0.35)'
          gctx.beginPath()
          gctx.arc(m.x - m.r * 0.25, m.y - m.r * 0.2, m.r * 0.22, 0, Math.PI * 2)
          gctx.fill()
        }
      }

      function stepMeteors(dt) {
        const W = window.innerWidth
        const H = window.innerHeight
        for (const m of meteors) {
          m.x += m.vx * dt * 18
          m.y += m.vy * dt * 18
          if (m.x < -20) m.x = W + 10
          if (m.x > W + 20) m.x = -10
          if (m.y < -20) m.y = H + 10
          if (m.y > H + 20) m.y = -10
        }
        for (let i = 0; i < meteors.length; i += 1) {
          for (let j = i + 1; j < meteors.length; j += 1) {
            const a = meteors[i]
            const b = meteors[j]
            const d = Math.hypot(a.x - b.x, a.y - b.y)
            if (d < a.r + b.r + 2 && d > 0.01) {
              const mx = (a.x + b.x) * 0.5
              const my = (a.y + b.y) * 0.5
              if (Math.hypot(mx - rx, my - ry) < 120) {
                stunUntil = performance.now() + 5000
              }
              a.vx *= -0.6
              b.vx *= -0.6
              a.vy *= -0.6
              b.vy *= -0.6
            }
          }
        }
      }

      function rafRocket(now) {
        const dt = Math.min(0.05, (now - last) / 1000)
        last = now
        drawGalaxy(now)
        stepMeteors(dt)
        const stunned = now < stunUntil
        dodgeT -= dt
        if (!stunned && dodgeT <= 0 && Math.random() < 0.92 * dt) {
          dodgeT = 0.12 + Math.random() * 0.28
          vx += (Math.random() - 0.5) * 520
          vy += (Math.random() - 0.5) * 420
        }
        vx *= Math.pow(stunned ? 0.96 : 0.92, dt * 60)
        vy *= Math.pow(stunned ? 0.96 : 0.92, dt * 60)
        const tcx = window.__solea404P2mx ?? window.innerWidth * 0.5
        const tcy = window.__solea404P2my ?? window.innerHeight * 0.55
        const ax = tcx - rx
        const ay = tcy - ry
        const al = Math.hypot(ax, ay) || 1
        const homing = stunned ? 28 : 80
        vx += (ax / al) * homing * dt
        vy += (ay / al) * homing * dt
        rx += vx * dt
        ry += vy * dt
        rx = Math.max(40, Math.min(window.innerWidth - 40, rx))
        ry = Math.max(40, Math.min(window.innerHeight - 40, ry))
        rocket.style.left = `${rx}px`
        rocket.style.top = `${ry}px`
        rocket.style.setProperty('--rocket-deg', `${Math.atan2(vy, vx) * (180 / Math.PI) + 90}deg`)

        const shipEl = rocket.querySelector('.page-error-rocket__ship')
        const rr = shipEl ? shipEl.getBoundingClientRect() : rocket.getBoundingClientRect()
        const mx = window.__solea404P2mx ?? 0
        const my = window.__solea404P2my ?? 0
        const over = mx >= rr.left && mx <= rr.right && my >= rr.top && my <= rr.bottom
        if (over && stunned) hold += dt * 1000
        else hold = Math.max(0, hold - dt * 420)

        if (hold >= ROCKET_HOLD_MS) {
          window.cancelAnimationFrame(rRocket)
          rRocket = 0
          window.removeEventListener('pointermove', onPtr)
          rocket.classList.remove('page-error-rocket--p2')
          rocket.setAttribute('hidden', '')
          rocket.setAttribute('aria-hidden', 'true')
          rocketHint.setAttribute('hidden', '')
          if (gal) gal.setAttribute('hidden', '')
          cine.setAttribute('hidden', '')
          black.classList.remove('page-error-void-p2-black--on')
          const msg = fr()
            ? 'Une cellule du site est réparée…'
            : 'A site cell is repaired…'
          const toast = document.createElement('div')
          toast.className = 'page-error-void-p2-toast'
          toast.textContent = msg
          document.body.appendChild(toast)
          window.setTimeout(() => toast.remove(), 2400)
          window.setTimeout(showLicenseChaos, LICENSE_AFTER_REPAIR_MS)
          return
        }
        rRocket = window.requestAnimationFrame(rafRocket)
      }
      rRocket = window.requestAnimationFrame(rafRocket)

      function onPtr(e) {
        window.__solea404P2mx = e.clientX
        window.__solea404P2my = e.clientY
      }
      window.addEventListener('pointermove', onPtr, { passive: true })
    }

    function fillLicenseChaos() {
      licFeed.replaceChildren()
      return fetch('/license/', { credentials: 'same-origin' })
        .then((r) => (r.ok ? r.text() : Promise.reject(new Error('bad'))))
        .then((html) => {
          try {
            const doc = new DOMParser().parseFromString(html, 'text/html')
            const main = doc.querySelector('main') || doc.querySelector('.legal-page') || doc.body
            const wrap = document.createElement('div')
            wrap.className = 'page-error-void-p2-lic-extract'
            const slice = (main && main.innerHTML) || html
            wrap.innerHTML = slice.slice(0, 14000)
            licFeed.appendChild(wrap)
          } catch {
            fillLicenseFallback()
          }
        })
        .catch(() => {
          fillLicenseFallback()
        })
    }

    function fillLicenseFallback() {
      const blocks = 22
      for (let i = 0; i < blocks; i += 1) {
        const d = document.createElement('div')
        d.className = 'page-error-void-p2-lic-block'
        d.textContent = Math.random() > 0.35 ? 'ERROR 404' : 'ERROR SYSTEM'
        licFeed.appendChild(d)
      }
    }

    let catchRaf = 0
    let cx = 40
    let cy = 40
    let cvx = 55
    let cvy = 40

    function showLicenseChaos() {
      setPhase('void_sequel_license')
      lic.removeAttribute('hidden')
      lic.setAttribute('aria-hidden', 'false')
      fillLicenseChaos().finally(() => {
        licScroll.scrollTop = 0
      })
      catch404.removeAttribute('hidden')
      const rw0 = licScroll.clientWidth
      const rh0 = licScroll.clientHeight
      cx = Math.max(12, rw0 * 0.15 + Math.random() * Math.min(120, rw0 * 0.25))
      cy = Math.max(12, rh0 * 0.12 + Math.random() * Math.min(100, rh0 * 0.2))
      cvx = 38 + Math.random() * 48
      cvy = 28 + Math.random() * 44
      function moveCatch() {
        const rw = licScroll.clientWidth
        const rh = licScroll.clientHeight
        if (rw < 40) return
        cx += cvx * 0.016
        cy += cvy * 0.016
        if (cx < 8 || cx > rw - 88) cvx *= -1
        if (cy < 8 || cy > rh - 52) cvy *= -1
        catch404.style.left = `${cx}px`
        catch404.style.top = `${cy}px`
        catchRaf = window.requestAnimationFrame(moveCatch)
      }
      catchRaf = window.requestAnimationFrame(moveCatch)
      catch404.addEventListener(
        'click',
        () => {
          if (catchRaf) window.cancelAnimationFrame(catchRaf)
          catchRaf = 0
          if (typeof dlgWin.showModal === 'function') dlgWin.showModal()
        },
        { once: true }
      )
    }

    root.removeAttribute('hidden')
    root.setAttribute('aria-hidden', 'false')
    resize()
    resetGhosts()
    window.addEventListener('resize', resize, { passive: true })
    wireGoldBtn()
    launchBtn.addEventListener('click', beginPacRun)
    showIntro(false)
    if (window.SoleaI18n && typeof window.SoleaI18n.apply === 'function') window.SoleaI18n.apply()
  }

  window.Solea404VoidPhase2 = { start }
})()
