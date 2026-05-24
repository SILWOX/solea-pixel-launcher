/**
 * Suite easter 404 après le vide : étoile « zéro », reconstruction façon accueil,
 * mini-jeu d’esquive d’éclairs, dialogue SOLEA / 404.
 */
;(function () {
  const LUCKY_MS_MIN = 5000
  const LUCKY_MS_MAX = 15000
  const REBUILD_MS_MIN = 30000
  const REBUILD_MS_MAX = 60000
  const END_GAME_MS = 60000
  const EXPLOSION_MS = 950
  /** Écran noir après le flash d’explosion, avant le plateau « reconstruction » */
  const POST_EXPLOSION_BLACK_MS = 1000
  const DODGES_NEEDED = 10
  const LIGHTNING_HIT_R = 34
  const MIST_TUTORIAL_MS = 4500

  /** @type {ReturnType<typeof init> | null} */
  let run = null

  /**
   * @param {object} d
   * @param {HTMLElement} d.voidStage
   * @param {HTMLElement} d.voidMemeLayer
   * @param {HTMLButtonElement} d.voidReset
   * @param {HTMLElement} d.voidStars
   * @param {(p: string) => void} d.setPhase
   * @param {() => boolean} d.isFrLang
   * @param {MediaQueryList} d.mqReduce
   * @param {() => void} d.stopVoidMemeFloaters
   * @param {() => void} d.pauseVoidMemeScheduler
   * @param {() => void} d.resumeVoidMemeScheduler
   */
  function start(d) {
    if (d.mqReduce.matches) return
    if (run) return
    run = init(d)
    run.boot()
  }

  function init(d) {
    const luckyWrap = document.getElementById('err-void-lucky-wrap')
    const luckyBtn = document.getElementById('err-void-lucky-star')
    const filCanvas = document.getElementById('err-void-zero-canvas')
    const explosion = document.getElementById('err-void-explosion')
    const rebuild = document.getElementById('err-void-rebuild')
    const piecesEl = document.getElementById('err-void-rebuild-pieces')
    const rebuildEnd = document.getElementById('err-void-rebuild-end')
    const mist = document.getElementById('err-void-mist')
    const mistCanvas = document.getElementById('err-void-mist-canvas')
    const mistHud = document.getElementById('err-void-mist-hud')
    const mistTutorial = document.getElementById('err-void-mist-tutorial')
    const finaleDialog = document.getElementById('err-void-sequel-finale')
    const dialogueBody = document.getElementById('err-void-sequel-dialogue-body')
    const btnContinue = document.getElementById('err-void-sequel-continue')
    const btnHome = document.getElementById('err-void-sequel-gohome')

    if (
      !luckyWrap ||
      !luckyBtn ||
      !filCanvas ||
      !explosion ||
      !rebuild ||
      !piecesEl ||
      !rebuildEnd ||
      !mist ||
      !mistCanvas ||
      !mistHud ||
      !mistTutorial ||
      !finaleDialog ||
      !dialogueBody ||
      !btnContinue ||
      !btnHome
    ) {
      return { boot() {} }
    }

    const fr = () => d.isFrLang()
    let luckyTimer = 0
    let filRaf = 0
    let mx = window.innerWidth * 0.5
    let my = window.innerHeight * 0.45
    let starCx = 0
    let starCy = 0
    let rebuildTotalMs = REBUILD_MS_MIN + Math.random() * (REBUILD_MS_MAX - REBUILD_MS_MIN)
    let rebuildStarted = 0
    let endBtnShown = false
    let mistState = 'off'
    let dodgeCount = 0
    let bolts = []
    let nextBoltAt = 0
    let mistRaf = 0
    let onMoveRef = null
    let onResizeRef = null
    let stepIv = 0
    let pointerForFilaments = null
    let luckyConsumed = false
    let meteorQuestIv = 0
    let meteorOrbRaf = 0
    let meteorBtnEl = null
    /** @type {{ x:number,y:number,vx:number,vy:number,s:number}[]} */
    let mistMeteors = []
    let mistPlayT0 = 0

    const PIECES = () =>
      fr()
        ? [
            { k: 'brand', t: 'SOLEA PIXEL' },
            { k: 'nav', t: 'Accueil · À propos · FAQ' },
            { k: 'btn', t: 'Télécharger' },
            { k: 'rail', t: '' },
            { k: 'card', t: 'Launcher Windows' },
            { k: 'social', t: 'Discord · YouTube' },
            { k: 'hub', t: '#hub' },
            { k: 'blob', t: '' },
            { k: 'license', t: 'Licence SPL' },
            { k: 'news', t: 'Actualités' },
          ]
        : [
            { k: 'brand', t: 'SOLEA PIXEL' },
            { k: 'nav', t: 'Home · About · FAQ' },
            { k: 'btn', t: 'Download' },
            { k: 'rail', t: '' },
            { k: 'card', t: 'Windows launcher' },
            { k: 'social', t: 'Discord · YouTube' },
            { k: 'hub', t: '#hub' },
            { k: 'blob', t: '' },
            { k: 'license', t: 'SPL License' },
            { k: 'news', t: 'News' },
          ]

    const BANTER_PACKS = fr()
      ? [
          [
            { w: 'solea', t: 'Tu comptes les visiteurs ou les bugs ?' },
            { w: '404', t: 'Les deux. Spoiler : tu perds.' },
            { w: 'solea', t: 'Je note ton attitude dans le changelog.' },
          ],
          [
            { w: 'solea', t: 'Encore en retard sur la prod ?' },
            { w: '404', t: 'Je suis la prod. Tu es l’invité surprise.' },
            { w: 'solea', t: '…Touché.' },
          ],
          [
            { w: 'solea', t: 'CSS ou hasard ?' },
            { w: '404', t: 'Oui.' },
            { w: 'solea', t: 'Réponse typique 404.' },
          ],
        ]
      : [
          [
            { w: 'solea', t: 'Counting visitors or bugs?' },
            { w: '404', t: 'Both. Spoiler: you lose.' },
            { w: 'solea', t: 'Noted for the changelog.' },
          ],
          [
            { w: 'solea', t: 'Late to prod again?' },
            { w: '404', t: 'I am prod. You’re the surprise guest.' },
            { w: 'solea', t: '…Fair.' },
          ],
          [
            { w: 'solea', t: 'CSS or chaos?' },
            { w: '404', t: 'Yes.' },
            { w: 'solea', t: 'Classic 404 energy.' },
          ],
        ]

    function setSequelActive(on) {
      window.__solea404VoidSequelActive = !!on
    }

    function placeLuckyStar() {
      const pad = 12
      const avoidY0 = window.innerHeight * 0.42
      const avoidY1 = window.innerHeight * 0.78
      let x = pad + Math.random() * (window.innerWidth - 2 * pad - 120)
      let y = pad + Math.random() * (window.innerHeight * 0.38)
      if (Math.random() > 0.5) {
        y = avoidY1 + Math.random() * (window.innerHeight - avoidY1 - pad - 80)
      }
      luckyWrap.style.left = `${x}px`
      luckyWrap.style.top = `${y}px`
    }

    function syncStarCenter() {
      const r = luckyBtn.getBoundingClientRect()
      starCx = r.left + r.width / 2
      starCy = r.top + r.height / 2
    }

    function resizeFilCanvas() {
      const s = d.voidStage.getBoundingClientRect()
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      filCanvas.width = Math.max(1, Math.floor(s.width * dpr))
      filCanvas.height = Math.max(1, Math.floor(s.height * dpr))
      filCanvas.style.width = `${s.width}px`
      filCanvas.style.height = `${s.height}px`
      const ctx = filCanvas.getContext('2d')
      if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }

    function drawFilaments() {
      const ctx = filCanvas.getContext('2d')
      if (!ctx) return
      const s = d.voidStage.getBoundingClientRect()
      ctx.clearRect(0, 0, s.width, s.height)
      syncStarCenter()
      const sx = starCx - s.left
      const sy = starCy - s.top
      const tx = mx - s.left
      const ty = my - s.top
      const n = 7
      for (let i = 0; i < n; i += 1) {
        const t = i / (n - 1) - 0.5
        const ox = (ty - sy) * 0.08 * t + (Math.sin(performance.now() / 420 + i) * 6 + t * 22)
        const oy = -(tx - sx) * 0.08 * t + (Math.cos(performance.now() / 380 + i) * 5)
        const mx1 = sx + (tx - sx) * 0.35 + ox
        const my1 = sy + (ty - sy) * 0.35 + oy
        const g = ctx.createLinearGradient(sx, sy, tx, ty)
        g.addColorStop(0, `rgba(180, 240, 255, ${0.55 + i * 0.05})`)
        g.addColorStop(0.45, `rgba(255, 120, 220, ${0.28})`)
        g.addColorStop(1, `rgba(120, 200, 255, ${0.05 + i * 0.02})`)
        ctx.strokeStyle = g
        ctx.lineWidth = 2.2 - i * 0.18
        ctx.lineCap = 'round'
        ctx.beginPath()
        ctx.moveTo(sx, sy)
        ctx.quadraticCurveTo(mx1, my1, tx, ty)
        ctx.stroke()
      }
      filRaf = window.requestAnimationFrame(drawFilaments)
    }

    function floodClickbait() {
      const arr = fr()
        ? [
            'Clique.',
            'Appuie.',
            'Fais-le.',
            'Vas-y.',
            'Saisis ta chance.',
            'Un clic suffit.',
            'Ici. Maintenant.',
            'Allez, un clic.',
          ]
        : ['Click.', 'Press it.', 'Do it.', 'Go on.', 'Take your shot.', 'One tap.', 'Now.', 'Just click.']
      d.voidMemeLayer.querySelectorAll('.page-error-void-meme').forEach((el) => {
        if (!el.classList.contains('page-error-void-meme--bait')) {
          el.textContent = arr[Math.floor(Math.random() * arr.length)]
        }
      })
      for (let j = 0; j < 10; j += 1) {
        const span = document.createElement('span')
        span.className = 'page-error-void-meme page-error-void-meme--bait'
        const arr = fr()
          ? ['Clique.', 'Appuie.', 'Fais-le.', 'Vas-y.', 'Saisis ta chance.', 'Un clic suffit.', "Le vide t'invite.", 'Tu sais.']
          : ['Click.', 'Press it.', 'Do it.', 'Go on.', 'Take your shot.', 'One tap.', 'The void beckons.', 'You know.']
        span.textContent = arr[Math.floor(Math.random() * arr.length)]
        span.setAttribute('aria-hidden', 'true')
        span.style.left = `${4 + Math.random() * 88}vw`
        span.style.top = `${6 + Math.random() * 72}vh`
        span.style.setProperty('--meme-dur', `${4 + Math.random() * 4}s`)
        span.style.setProperty('--meme-a', String(0.22 + Math.random() * 0.12))
        d.voidMemeLayer.appendChild(span)
      }
      window.__solea404MemeMode = 'clickbait'
    }

    function clearBaitMemes() {
      d.voidMemeLayer.querySelectorAll('.page-error-void-meme--bait').forEach((el) => el.remove())
      window.__solea404MemeMode = 'normal'
    }

    function onLuckyEnter() {
      d.pauseVoidMemeScheduler()
      floodClickbait()
    }

    function onLuckyLeave() {
      clearBaitMemes()
      d.resumeVoidMemeScheduler()
    }

    function beginMeteorStarGate() {
      const stage = d.voidStage
      function clearMeteorLoop() {
        if (meteorQuestIv) {
          window.clearInterval(meteorQuestIv)
          meteorQuestIv = 0
        }
      }
      function trySpawnMeteor() {
        if (luckyConsumed || meteorBtnEl) return
        if (Math.random() > 0.42) return
        meteorBtnEl = document.createElement('button')
        meteorBtnEl.type = 'button'
        meteorBtnEl.className = 'page-error-void-meteor'
        meteorBtnEl.setAttribute('aria-label', fr() ? 'Météorite' : 'Meteorite')
        meteorBtnEl.textContent = '☄'
        const pad = 36
        meteorBtnEl.style.left = `${pad + Math.random() * Math.max(40, window.innerWidth - pad * 2 - 72)}px`
        meteorBtnEl.style.top = `${pad + Math.random() * Math.max(40, window.innerHeight * 0.5)}px`
        stage.appendChild(meteorBtnEl)
        meteorBtnEl.addEventListener(
          'click',
          (ev) => {
            ev.preventDefault()
            ev.stopPropagation()
            clearMeteorLoop()
            meteorBtnEl?.remove()
            meteorBtnEl = null
            d.voidReset.setAttribute('hidden', '')
            const orb = document.createElement('button')
            orb.type = 'button'
            orb.className = 'page-error-void-grand-star'
            orb.setAttribute('aria-label', fr() ? 'Étoile' : 'Star')
            orb.innerHTML =
              '<span class="page-error-void-grand-star__rays" aria-hidden="true"></span>' +
              '<span class="page-error-void-grand-star__disc" aria-hidden="true"></span>' +
              '<span class="page-error-void-grand-star__core" aria-hidden="true"></span>'
            stage.appendChild(orb)
            let ox = window.innerWidth * 0.48
            let oy = window.innerHeight * 0.38
            let ang = Math.random() * Math.PI * 2
            function orbTick(now) {
              if (luckyConsumed) return
              ang += 0.0024
              ox = window.innerWidth * 0.42 + Math.cos(ang) * Math.min(180, window.innerWidth * 0.18)
              oy = window.innerHeight * 0.32 + Math.sin(ang * 0.85) * Math.min(120, window.innerHeight * 0.16)
              orb.style.left = `${ox}px`
              orb.style.top = `${oy}px`
              meteorOrbRaf = window.requestAnimationFrame(orbTick)
            }
            meteorOrbRaf = window.requestAnimationFrame(orbTick)
            orb.addEventListener(
              'click',
              (e2) => {
                if (luckyConsumed) return
                if (meteorOrbRaf) window.cancelAnimationFrame(meteorOrbRaf)
                meteorOrbRaf = 0
                orb.remove()
                onLuckyClick(e2)
              },
              { once: true },
            )
          },
          { once: true },
        )
      }
      meteorQuestIv = window.setInterval(trySpawnMeteor, 2200)
      trySpawnMeteor()
    }

    function showLuckyStar() {
      setSequelActive(true)
      d.setPhase('void_sequel_star')
      d.voidStage.classList.add('page-error-void-stage--sequel-star')
      d.voidReset.setAttribute('hidden', '')
      placeLuckyStar()
      luckyWrap.removeAttribute('hidden')
      filCanvas.removeAttribute('hidden')
      luckyWrap.classList.add('page-error-void-lucky-wrap--in')
      resizeFilCanvas()
      if (!filRaf) filRaf = window.requestAnimationFrame(drawFilaments)
    }

    function onPointerMove(e) {
      mx = e.clientX
      my = e.clientY
    }

    function onLuckyClick(e) {
      e.preventDefault()
      e.stopPropagation()
      if (luckyConsumed) return
      luckyConsumed = true
      if (meteorQuestIv) {
        window.clearInterval(meteorQuestIv)
        meteorQuestIv = 0
      }
      meteorBtnEl?.remove()
      meteorBtnEl = null
      if (luckyTimer) {
        window.clearTimeout(luckyTimer)
        luckyTimer = 0
      }
      if (pointerForFilaments) {
        window.removeEventListener('pointermove', pointerForFilaments, { passive: true })
        pointerForFilaments = null
      }
      if (filRaf) {
        window.cancelAnimationFrame(filRaf)
        filRaf = 0
      }
      luckyWrap.classList.remove('page-error-void-lucky-wrap--in')
      luckyWrap.setAttribute('hidden', '')
      filCanvas.setAttribute('hidden', '')
      d.voidStage.classList.remove('page-error-void-stage--sequel-star')
      d.stopVoidMemeFloaters()
      d.setPhase('void_sequel_explosion')
      explosion.removeAttribute('hidden')
      explosion.classList.remove('page-error-void-explosion--hold')
      explosion.classList.add('page-error-void-explosion--bang')
      window.setTimeout(() => {
        explosion.classList.remove('page-error-void-explosion--bang')
        explosion.classList.add('page-error-void-explosion--hold')
        window.setTimeout(() => {
          explosion.classList.remove('page-error-void-explosion--hold')
          explosion.setAttribute('hidden', '')
          startFakeHomeBypass()
        }, POST_EXPLOSION_BLACK_MS)
      }, EXPLOSION_MS)
    }

    function startFakeHomeBypass() {
      d.setPhase('void_sequel_rebuild')
      d.voidStage.classList.add('page-error-void-stage--sequel-rebuild')
      rebuild.classList.add('page-error-void-rebuild--active', 'page-error-void-rebuild--fake')
      rebuild.removeAttribute('hidden')
      rebuild.setAttribute('aria-hidden', 'false')
      piecesEl.replaceChildren()
      rebuildEnd.setAttribute('hidden', '')
      endBtnShown = false
      rebuildStarted = performance.now()
      const errsFr = [
        'ERR_NULL_POINTER',
        'SIGNAL_LOST',
        'STACK_UNDERFLOW',
        'NO_ROUTE',
        'KERNEL_PANIC_LITE',
        'TIMEOUT (∞)',
      ]
      const errsEn = [
        'ERR_NULL_POINTER',
        'SIGNAL_LOST',
        'STACK_UNDERFLOW',
        'NO_ROUTE',
        'KERNEL_PANIC_LITE',
        'TIMEOUT (∞)',
      ]
      const errs = fr() ? errsFr : errsEn
      function toast(msg) {
        const t = document.createElement('div')
        t.className = 'page-error-fake-toast'
        t.textContent = msg
        rebuild.appendChild(t)
        window.setTimeout(() => {
          try {
            t.remove()
          } catch {
            /* ignore */
          }
        }, 2400)
      }
      const shell = document.createElement('div')
      shell.className = 'page-error-fake-home'
      shell.innerHTML = `
        <div class="page-error-fake-home__bar">
          <span class="page-error-fake-home__logo">SOLEA PIXEL</span>
          <span class="page-error-fake-home__tag">404</span>
        </div>
        <p class="page-error-fake-home__lead">${fr() ? 'Ce n’est pas le vrai site — piège de navigation.' : 'This is not the real site — a navigation trap.'}</p>
        <nav class="page-error-fake-home__nav" aria-label="fake">
          <button type="button" class="page-error-fake-fbtn">${fr() ? 'Accueil' : 'Home'}</button>
          <button type="button" class="page-error-fake-fbtn">${fr() ? 'Télécharger' : 'Download'}</button>
          <button type="button" class="page-error-fake-fbtn">${fr() ? 'À propos' : 'About'}</button>
          <button type="button" class="page-error-fake-fbtn">${fr() ? 'Actu' : 'News'}</button>
          <button type="button" class="page-error-fake-fbtn">${fr() ? 'FAQ' : 'FAQ'}</button>
        </nav>
        <p class="page-error-fake-home__hint">${fr() ? 'Trouve le bouton caché pour continuer.' : 'Find the hidden button to continue.'}</p>
        <button type="button" class="page-error-fake-bypass" id="err-fake-404-bypass">404 Bypass</button>
      `
      piecesEl.appendChild(shell)
      shell.querySelectorAll('.page-error-fake-fbtn').forEach((b) => {
        b.addEventListener('click', () => toast(errs[Math.floor(Math.random() * errs.length)]))
      })
      const bypass = shell.querySelector('#err-fake-404-bypass')
      if (bypass) {
        bypass.addEventListener('click', () => {
          toast(fr() ? 'Contournement accepté.' : 'Bypass accepted.')
          window.setTimeout(() => onRebuildEndClick(), 520)
        })
      }
    }

    function wireDrag(el) {
      let down = false
      let ox = 0
      let oy = 0
      el.addEventListener('pointerdown', (e) => {
        down = true
        el.setPointerCapture(e.pointerId)
        const r = el.getBoundingClientRect()
        ox = e.clientX - r.left
        oy = e.clientY - r.top
      })
      el.addEventListener('pointermove', (e) => {
        if (!down) return
        const s = rebuild.getBoundingClientRect()
        const x = e.clientX - s.left - ox
        const y = e.clientY - s.top - oy
        const xp = (x / s.width) * 100
        const yp = (y / s.height) * 100
        el.style.left = `${Math.max(0, Math.min(96, xp))}%`
        el.style.top = `${Math.max(0, Math.min(94, yp))}%`
      })
      el.addEventListener('pointerup', () => {
        down = false
      })
      el.addEventListener('pointercancel', () => {
        down = false
      })
    }

    function showEndGameBtn() {
      if (endBtnShown) return
      endBtnShown = true
      rebuildEnd.removeAttribute('hidden')
    }

    function onRebuildEndClick() {
      rebuildEnd.setAttribute('hidden', '')
      document.body.classList.add('page-error--void-dissolve')
      window.setTimeout(() => {
        rebuild.classList.remove('page-error-void-rebuild--active')
        rebuild.setAttribute('hidden', '')
        rebuild.setAttribute('aria-hidden', 'true')
        d.voidStage.classList.remove('page-error-void-stage--sequel-rebuild')
        d.voidStars.setAttribute('hidden', '')
        piecesEl.replaceChildren()
        startMist()
      }, 1100)
    }

    function resizeMistCanvas() {
      const dpr = Math.min(window.devicePixelRatio || 1, 2)
      mistCanvas.width = Math.floor(window.innerWidth * dpr)
      mistCanvas.height = Math.floor(window.innerHeight * dpr)
      mistCanvas.style.width = `${window.innerWidth}px`
      mistCanvas.style.height = `${window.innerHeight}px`
      const ctx = mistCanvas.getContext('2d')
      if (ctx) ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    }

    function drawMistFrame() {
      if (mistState === 'won' || mist.hasAttribute('hidden')) return
      const ctx = mistCanvas.getContext('2d')
      if (!ctx) return
      const w = window.innerWidth
      const h = window.innerHeight
      ctx.clearRect(0, 0, w, h)
      ctx.fillStyle = 'rgba(14, 0, 6, 0.88)'
      ctx.fillRect(0, 0, w, h)
      const t = performance.now() / 1000
      ctx.fillStyle = `rgba(120, 10, 30, ${0.12 + Math.sin(t) * 0.04})`
      ctx.fillRect(0, 0, w, h)

      const g2 = ctx.createRadialGradient(w * 0.2, h * 0.85, 0, w * 0.5, h * 0.5, h * 0.9)
      g2.addColorStop(0, 'rgba(80, 20, 60, 0.25)')
      g2.addColorStop(1, 'transparent')
      ctx.fillStyle = g2
      ctx.fillRect(0, 0, w, h)

      for (const m of mistMeteors) {
        m.x += m.vx * 0.45
        m.y += m.vy * 0.45
        if (m.x < -40) m.x = w + 20
        if (m.x > w + 40) m.x = -20
        if (m.y < -40) m.y = h + 20
        if (m.y > h + 40) m.y = -20
        ctx.save()
        ctx.translate(m.x, m.y)
        ctx.rotate(m.s + t * 0.4)
        ctx.fillStyle = 'rgba(160, 90, 110, 0.35)'
        ctx.fillRect(-10, -3, 22, 6)
        ctx.restore()
      }

      const playAge = mistPlayT0 ? (performance.now() - mistPlayT0) / 1000 : 0
      const shakeAmp = 2 + playAge * 0.35 + dodgeCount * 0.22
      ctx.save()
      ctx.font = 'bold 38px "Press Start 2P", ui-monospace, monospace'
      ctx.textAlign = 'center'
      ctx.fillStyle = `rgba(255, 200, 210, ${0.14 + Math.min(0.22, playAge * 0.015)})`
      const sx = Math.sin(playAge * 11.7 + dodgeCount) * shakeAmp * 0.42
      const sy = Math.cos(playAge * 9.2 - dodgeCount * 0.3) * shakeAmp * 0.38
      ctx.fillText('404', w * 0.5 + sx, 52 + sy)
      ctx.restore()

      for (const b of bolts) {
        const u = (performance.now() - b.t0) / b.dur
        if (u >= 1) continue
        const cv = b.curve || 0
        const x = b.x0 + (b.x1 - b.x0) * u + cv * 90 * Math.sin(u * Math.PI)
        const y = b.y0 + (b.y1 - b.y0) * u - cv * 70 * Math.sin(u * Math.PI)
        ctx.strokeStyle = `rgba(255, 245, 255, ${0.85 * (1 - u)})`
        ctx.lineWidth = 3 + (1 - u) * 4
        ctx.beginPath()
        ctx.moveTo(b.x0, b.y0)
        ctx.lineTo(x, y)
        ctx.stroke()
      }

      ctx.save()
      ctx.fillStyle = '#ffb7e8'
      ctx.shadowColor = 'rgba(255, 150, 220, 0.9)'
      ctx.shadowBlur = 18
      ctx.beginPath()
      ctx.arc(mx, my, 11, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = '#fff5fc'
      ctx.beginPath()
      ctx.arc(mx - 4, my - 3, 3, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()

      if (mistState !== 'won' && !mist.hasAttribute('hidden')) {
        mistRaf = window.requestAnimationFrame(drawMistFrame)
      }
    }

    function spawnBolt() {
      const w = window.innerWidth
      const h = window.innerHeight
      const edge = Math.floor(Math.random() * 4)
      let x0 = 0
      let y0 = 0
      if (edge === 0) {
        x0 = Math.random() * w
        y0 = -20
      } else if (edge === 1) {
        x0 = w + 20
        y0 = Math.random() * h
      } else if (edge === 2) {
        x0 = Math.random() * w
        y0 = h + 20
      } else {
        x0 = -20
        y0 = Math.random() * h
      }
      const spread = 55 + Math.random() * 95
      const x1 = mx + (Math.random() - 0.5) * spread
      const y1 = my + (Math.random() - 0.5) * spread
      bolts.push({
        x0,
        y0,
        x1,
        y1,
        t0: performance.now(),
        dur: 280 + Math.random() * 420,
        scored: false,
        curve: (Math.random() - 0.5) * 0.55,
      })
    }

    function stepBolts() {
      const now = performance.now()
      bolts = bolts.filter((b) => now - b.t0 < b.dur + 120)
      for (const b of bolts) {
        const u = (now - b.t0) / b.dur
        if (u < 0.98 || b.scored) continue
        b.scored = true
        const x = b.x1
        const y = b.y1
        const dist = Math.hypot(x - mx, y - my)
        if (dist < LIGHTNING_HIT_R) {
          dodgeCount = 0
          mistHud.textContent = fr()
            ? "Touché ! Esquive 10 éclairs d'affilée — recommence."
            : 'Hit! Dodge 10 bolts in a row — try again.'
        } else {
          dodgeCount += 1
          mistHud.textContent = fr()
            ? `Esquives : ${dodgeCount} / ${DODGES_NEEDED}`
            : `Dodges: ${dodgeCount} / ${DODGES_NEEDED}`
          if (dodgeCount >= DODGES_NEEDED) {
            mistState = 'won'
            if (mistRaf) window.cancelAnimationFrame(mistRaf)
            mistRaf = 0
            document.body.classList.remove('page-error--sequel-mist')
            mist.setAttribute('hidden', '')
            document.body.classList.remove('page-error--void-dissolve')
            if (onMoveRef) window.removeEventListener('pointermove', onMoveRef, { passive: true })
            if (onResizeRef) window.removeEventListener('resize', onResizeRef, { passive: true })
            if (stepIv) window.clearInterval(stepIv)
            stepIv = 0
            openDialogue()
          }
        }
      }
    }

    function startMist() {
      d.setPhase('void_sequel_mist')
      document.body.classList.add('page-error--sequel-mist')
      mist.removeAttribute('hidden')
      resizeMistCanvas()
      dodgeCount = 0
      bolts = []
      mistState = 'tutorial'
      mistHud.textContent = ''
      mistTutorial.removeAttribute('hidden')
      mistTutorial.textContent = fr()
        ? "Brume rouge : tu es une petite fée rose. Des éclairs courbes et des météorites défilent en fond. Esquive 10 éclairs d’affilée. Si tu es touché, le compteur repart à zéro."
        : "Red mist: you're a tiny pink fairy. Curved lightning and drifting meteors fill the backdrop. Dodge 10 bolts in a row. If you're hit, the count resets to zero."
      window.setTimeout(() => {
        mistTutorial.setAttribute('hidden', '')
        mistState = 'play'
        mistPlayT0 = performance.now()
        mistMeteors = []
        for (let i = 0; i < 18; i += 1) {
          mistMeteors.push({
            x: Math.random() * window.innerWidth,
            y: Math.random() * window.innerHeight,
            vx: (Math.random() - 0.5) * 2.4,
            vy: (Math.random() - 0.5) * 2.4,
            s: Math.random() * Math.PI * 2,
          })
        }
        nextBoltAt = performance.now() + 500
        mistHud.textContent = fr() ? `Esquives : 0 / ${DODGES_NEEDED}` : `Dodges: 0 / ${DODGES_NEEDED}`
      }, MIST_TUTORIAL_MS)

      onMoveRef = (e) => {
        mx = e.clientX
        my = e.clientY
      }
      window.addEventListener('pointermove', onMoveRef, { passive: true })
      onResizeRef = () => resizeMistCanvas()
      window.addEventListener('resize', onResizeRef, { passive: true })

      stepIv = window.setInterval(() => {
        if (mistState !== 'play') return
        const now = performance.now()
        if (now >= nextBoltAt) {
          spawnBolt()
          const rush = mistPlayT0 && now - mistPlayT0 > 12000 ? 0.75 : 1
          nextBoltAt = now + (520 + Math.random() * 820) * rush
        }
        stepBolts()
      }, 45)

      if (!mistRaf) mistRaf = window.requestAnimationFrame(drawMistFrame)
    }

    function typeLine(el, text, done) {
      el.textContent = ''
      let i = 0
      const id = window.setInterval(() => {
        i += 1
        el.textContent = text.slice(0, i)
        if (i >= text.length) {
          window.clearInterval(id)
          done()
        }
      }, 22)
    }

    function openDialogue() {
      d.setPhase('void_sequel_dialogue')
      dialogueBody.replaceChildren()
      const pack = BANTER_PACKS[Math.floor(Math.random() * BANTER_PACKS.length)]
      const lines = [
        {
          w: 'solea',
          t: fr() ? 'Je rêve ou une histoire se crée dans mon 404 ?' : 'Am I dreaming or is a story writing itself in my 404?',
        },
        {
          w: '404',
          t: fr() ? 'Laisse-moi, je veux vivre ma vie.' : 'Let me live my life.',
        },
        ...pack,
      ]
      let idx = 0

      function next() {
        if (idx >= lines.length) {
          btnContinue.removeAttribute('hidden')
          btnContinue.removeAttribute('disabled')
          btnHome.removeAttribute('hidden')
          return
        }
        const row = document.createElement('div')
        row.className = `page-error-void-sequel-line page-error-void-sequel-line--${lines[idx].w}`
        const lab = document.createElement('span')
        lab.className = 'page-error-void-sequel-line__who'
        lab.textContent = lines[idx].w === 'solea' ? 'SOLEA PIXEL' : '404'
        const tx = document.createElement('p')
        tx.className = 'page-error-void-sequel-line__text'
        row.appendChild(lab)
        row.appendChild(tx)
        dialogueBody.appendChild(row)
        typeLine(tx, lines[idx].t, () => {
          idx += 1
          window.setTimeout(next, 380)
        })
      }

      btnContinue.setAttribute('hidden', '')
      btnContinue.setAttribute('disabled', '')
      btnHome.setAttribute('hidden', '')
      if (typeof finaleDialog.showModal === 'function') {
        finaleDialog.showModal()
      }
      if (window.SoleaI18n && typeof window.SoleaI18n.apply === 'function') window.SoleaI18n.apply()
      next()
    }

    function boot() {
      luckyConsumed = false
      rebuild.classList.remove('page-error-void-rebuild--active', 'page-error-void-rebuild--fake')
      rebuild.setAttribute('hidden', '')
      rebuild.setAttribute('aria-hidden', 'true')
      d.voidStage.classList.remove('page-error-void-stage--sequel-star', 'page-error-void-stage--sequel-rebuild')
      luckyTimer = 0
      if (window.__solea404DeferLuckyUntilMeteor) {
        beginMeteorStarGate()
      } else {
        luckyTimer = window.setTimeout(showLuckyStar, LUCKY_MS_MIN + Math.random() * (LUCKY_MS_MAX - LUCKY_MS_MIN))
      }
      pointerForFilaments = onPointerMove
      window.addEventListener('pointermove', pointerForFilaments, { passive: true })
      luckyBtn.addEventListener('pointerenter', onLuckyEnter)
      luckyBtn.addEventListener('pointerleave', onLuckyLeave)
      luckyBtn.addEventListener('click', onLuckyClick)
      rebuildEnd.addEventListener('click', onRebuildEndClick)
      btnHome.addEventListener('click', () => {
        setSequelActive(false)
        if (typeof finaleDialog.close === 'function') finaleDialog.close()
        window.location.assign('/')
      })
      btnContinue.addEventListener('click', () => {
        setSequelActive(false)
        if (typeof finaleDialog.close === 'function') finaleDialog.close()
        if (window.Solea404VoidPhase2 && typeof window.Solea404VoidPhase2.start === 'function') {
          window.Solea404VoidPhase2.start()
        }
      })
      window.addEventListener('resize', resizeFilCanvas, { passive: true })
    }

    return { boot }
  }

  window.__solea404VoidSequelActive = false
  window.Solea404VoidSequel = { start }
})()
