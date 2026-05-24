/**
 * Labyrinthe procédural : 2 orbes au départ, 3e après 67 s ; 3 vies ; sortie.
 * Après 60 s : léger mélange des murs ; 404 hors plateau tremblent.
 */
;(function () {
  /** Grille (impair × impair pour le carve DFS). */
  const CELL = 24
  const MAZE_COLS = 17
  const MAZE_ROWS = 13
  /** Orbes actifs : 2 au début, 3 une fois `THIRD_ORB_MS` écoulées. */
  const THIRD_ORB_MS = 67000
  const HARD_MS = 60000
  const SHAKE30_MS = 30000
  /** Délai entre deux « pas » des orbes (réduit la vitesse perçue). */
  const ENEMY_TICK_MS = 240
  const ENEMY_TICK_HARD_MS = 190

  /**
   * @param {object} o
   * @param {HTMLCanvasElement} o.canvas
   * @param {HTMLElement} o.root
   * @param {HTMLElement} o.fragLayer
   * @param {HTMLElement} o.hud
   * @param {() => boolean} o.fr
   * @param {Set<string>} o.keys
   * @param {() => void} o.onWin
   * @param {() => void} o.onLose
   * @param {(id: number) => void} o.setRaf
   * @param {() => number | null} o.getRaf
   * @param {(id: number) => void} o.cancelRaf
   * @param {() => boolean} o.isPlaying
   */
  function start(o) {
    const ctx = o.canvas.getContext('2d')
    if (!ctx) return () => {}

    let cols = 0
    let rows = 0
    let grid = /** @type {Uint8Array} */ (new Uint8Array(0))
    let px = 1
    let py = 1
    let ex = 0
    let ey = 0
    let lives = 3
    let t0 = 0
    let lastDoor = 0
    let fragIv = 0
    let nextMoveAt = 0
    let nextEnemyAt = 0
    const enemies = []

    function idx(c, r) {
      return r * cols + c
    }

    function carve() {
      cols = MAZE_COLS
      rows = MAZE_ROWS
      grid = new Uint8Array(cols * rows).fill(1)
      const stack = []
      function carveCell(c, r) {
        grid[idx(c, r)] = 0
      }
      carveCell(1, 1)
      stack.push([1, 1])
      const dirs = [
        [2, 0],
        [-2, 0],
        [0, 2],
        [0, -2],
      ]
      while (stack.length) {
        const [c, r] = stack[stack.length - 1]
        const neigh = dirs
          .map(([dc, dr]) => [c + dc, r + dr, dc, dr])
          .filter(([nc, nr]) => nc > 0 && nr > 0 && nc < cols - 1 && nr < rows - 1 && grid[idx(nc, nr)] === 1)
        if (!neigh.length) {
          stack.pop()
          continue
        }
        const pick = neigh[Math.floor(Math.random() * neigh.length)]
        const [nc, nr, dc, dr] = pick
        grid[idx(nc, nr)] = 0
        grid[idx(c + dc / 2, r + dr / 2)] = 0
        stack.push([nc, nr])
      }
      ex = cols - 2
      ey = rows - 2
      grid[idx(ex, ey)] = 0
      px = 1
      py = 1
      ensureExitReachable()
    }

    function ensureExitReachable() {
      const seen = new Set()
      const q = [[1, 1]]
      seen.add('1,1')
      while (q.length) {
        const [c, r] = q.shift()
        if (c === ex && r === ey) return
        for (const [dc, dr] of [
          [1, 0],
          [-1, 0],
          [0, 1],
          [0, -1],
        ]) {
          const nc = c + dc
          const nr = r + dr
          if (nc < 0 || nr < 0 || nc >= cols || nr >= rows) continue
          const k = `${nc},${nr}`
          if (grid[idx(nc, nr)] === 0 && !seen.has(k)) {
            seen.add(k)
            q.push([nc, nr])
          }
        }
      }
      for (let c = 1; c < cols - 1; c += 1) grid[idx(c, ey)] = 0
      for (let r = 1; r < rows - 1; r += 1) grid[idx(ex, r)] = 0
      grid[idx(1, 1)] = 0
      grid[idx(ex, ey)] = 0
    }

    function randomOpenCell() {
      let c = 0
      let r = 0
      for (let k = 0; k < 800; k += 1) {
        c = 1 + Math.floor(Math.random() * (cols - 2))
        r = 1 + Math.floor(Math.random() * (rows - 2))
        if (grid[idx(c, r)] === 0 && (c !== px || r !== py) && (c !== ex || r !== ey)) return [c, r]
      }
      return [3, 3]
    }

    function canSee(ec, er, pc, pr) {
      if (ec !== pc && er !== pr) return false
      if (ec === pc) {
        const a = Math.min(er, pr)
        const b = Math.max(er, pr)
        for (let r = a + 1; r < b; r += 1) {
          if (grid[idx(ec, r)] === 1) return false
        }
        return true
      }
      const a = Math.min(ec, pc)
      const b = Math.max(ec, pc)
      for (let c = a + 1; c < b; c += 1) {
        if (grid[idx(c, er)] === 1) return false
      }
      return true
    }

    function orbCountForElapsed(elapsed) {
      return elapsed >= THIRD_ORB_MS ? 3 : 2
    }

    function setupEnemies() {
      const n = orbCountForElapsed(performance.now() - t0)
      enemies.length = 0
      for (let i = 0; i < n; i += 1) {
        const [c, r] = randomOpenCell()
        enemies.push({
          c,
          r,
          col: ['#ff5b5b', '#ffb84d', '#6ecbff'][i % 3],
        })
      }
    }

    function mazeOffset() {
      const mw = cols * CELL
      const mh = rows * CELL
      return {
        ox: Math.max(12, (o.canvas.width - mw) / 2),
        oy: Math.max(28, (o.canvas.height - mh) / 2),
      }
    }

    function shuffleDoors(now) {
      if (now - lastDoor < 3800) return
      lastDoor = now
      const tries = 1 + Math.floor(Math.random() * 2)
      for (let n = 0; n < tries; n += 1) {
        const c = 2 + Math.floor(Math.random() * (cols - 4))
        const r = 2 + Math.floor(Math.random() * (rows - 4))
        if ((c === px && r === py) || (c === ex && r === ey)) continue
        if (grid[idx(c, r)] === 0) {
          if (Math.random() > 0.5 && (grid[idx(c - 1, r)] === 1 || grid[idx(c + 1, r)] === 1)) {
            grid[idx(c, r)] = 1
          }
        } else if (grid[idx(c, r)] === 1) {
          grid[idx(c, r)] = 0
        }
        grid[idx(ex, ey)] = 0
        grid[idx(1, 1)] = 0
        grid[idx(px, py)] = 0
      }
    }

    function spawnFrag() {
      const s = document.createElement('span')
      s.className = 'page-error-void-p2-frag page-error-void-p2-frag--maze'
      s.textContent = '404'
      s.style.left = `${2 + Math.random() * 92}vw`
      s.style.top = `${4 + Math.random() * 18}vh`
      s.style.setProperty('--rot', `${(Math.random() - 0.5) * 18}deg`)
      o.fragLayer.appendChild(s)
      window.setTimeout(() => {
        try {
          s.remove()
        } catch {
          /* ignore */
        }
      }, 3200)
    }

    function resetPlayer() {
      px = 1
      py = 1
    }

    function loseLife() {
      lives -= 1
      if (lives <= 0) {
        o.onLose()
        return true
      }
      resetPlayer()
      setupEnemies()
      return false
    }

    function frame(now) {
      if (!ctx || !o.isPlaying()) return
      const t = now - t0
      const hard = t >= HARD_MS
      if (hard) shuffleDoors(now)

      if (now >= nextMoveAt) {
        let moved = false
        if (o.keys.has('up') && py > 0 && grid[idx(px, py - 1)] === 0) {
          py -= 1
          moved = true
        } else if (o.keys.has('down') && py < rows - 1 && grid[idx(px, py + 1)] === 0) {
          py += 1
          moved = true
        } else if (o.keys.has('left') && px > 0 && grid[idx(px - 1, py)] === 0) {
          px -= 1
          moved = true
        } else if (o.keys.has('right') && px < cols - 1 && grid[idx(px + 1, py)] === 0) {
          px += 1
          moved = true
        }
        if (moved) nextMoveAt = now + 105
      }

      if (t >= THIRD_ORB_MS && enemies.length === 2) {
        for (let k = 0; k < 80; k += 1) {
          const [c, r] = randomOpenCell()
          if (!enemies.some((e) => e.c === c && e.r === r)) {
            enemies.push({ c, r, col: '#6ecbff' })
            break
          }
        }
      }

      const stepEnemies = now >= nextEnemyAt
      if (stepEnemies) {
        nextEnemyAt = now + (hard ? ENEMY_TICK_HARD_MS : ENEMY_TICK_MS)
      }
      for (const g of enemies) {
        if (stepEnemies) {
          const see = canSee(g.c, g.r, px, py)
          if (see) {
            const dx = Math.sign(px - g.c)
            const dy = Math.sign(py - g.r)
            if (dx !== 0 && grid[idx(g.c + dx, g.r)] === 0) g.c += dx
            else if (dy !== 0 && grid[idx(g.c, g.r + dy)] === 0) g.r += dy
          } else {
            const dirs = [
              [1, 0],
              [-1, 0],
              [0, 1],
              [0, -1],
            ]
            const sh = dirs[Math.floor(Math.random() * dirs.length)]
            const nc = g.c + sh[0]
            const nr = g.r + sh[1]
            if (nc > 0 && nr > 0 && nc < cols - 1 && nr < rows - 1 && grid[idx(nc, nr)] === 0) {
              g.c = nc
              g.r = nr
            }
          }
        }
        if (g.c === px && g.r === py) {
          if (loseLife()) return
        }
      }

      if (px === ex && py === ey) {
        o.onWin()
        return
      }

      const { ox, oy } = mazeOffset()
      ctx.fillStyle = '#04040a'
      ctx.fillRect(0, 0, o.canvas.width, o.canvas.height)
      const gTime = now / 1000
      for (let i = 0; i < 32; i += 1) {
        const gx = (i * 997) % o.canvas.width
        const gy = (i * 661) % o.canvas.height
        ctx.fillStyle = `rgba(60, 80, 140, ${0.03 + (i % 5) * 0.01})`
        ctx.fillRect(gx, gy, 2, 2)
      }

      ctx.strokeStyle = 'rgba(120, 160, 255, 0.35)'
      ctx.lineWidth = 1
      for (let r = 0; r < rows; r += 1) {
        for (let c = 0; c < cols; c += 1) {
          if (grid[idx(c, r)] === 1) {
            ctx.fillStyle = hard ? 'rgba(30, 36, 60, 0.95)' : 'rgba(24, 28, 48, 0.92)'
            ctx.fillRect(ox + c * CELL, oy + r * CELL, CELL + 0.5, CELL + 0.5)
          }
        }
      }

      ctx.fillStyle = '#7dffb3'
      ctx.fillRect(ox + ex * CELL + 3, oy + ey * CELL + 3, CELL - 6, CELL - 6)
      ctx.font = '10px system-ui,sans-serif'
      ctx.fillStyle = 'rgba(200,255,220,0.9)'
      ctx.fillText(o.fr() ? 'SORTIE' : 'EXIT', ox + ex * CELL - 2, oy + ey * CELL - 6)

      for (const g of enemies) {
        ctx.beginPath()
        ctx.fillStyle = g.col
        ctx.arc(ox + g.c * CELL + CELL / 2, oy + g.r * CELL + CELL / 2, CELL * 0.32, 0, Math.PI * 2)
        ctx.fill()
      }

      ctx.fillStyle = '#ffe566'
      ctx.beginPath()
      ctx.arc(ox + px * CELL + CELL / 2, oy + py * CELL + CELL / 2, CELL * 0.36, 0, Math.PI * 2)
      ctx.fill()
      ctx.fillStyle = '#2a2100'
      ctx.beginPath()
      ctx.arc(ox + px * CELL + CELL / 2 - 3, oy + py * CELL + CELL / 2 - 2, 2, 0, Math.PI * 2)
      ctx.arc(ox + px * CELL + CELL / 2 + 3, oy + py * CELL + CELL / 2 - 2, 2, 0, Math.PI * 2)
      ctx.fill()

      const sec = Math.floor(t / 1000)
      const shake = t > SHAKE30_MS ? 1 + (t - SHAKE30_MS) / 30000 : t / SHAKE30_MS
      let fi = 0
      o.fragLayer.querySelectorAll('.page-error-void-p2-frag--maze').forEach((el) => {
        fi += 1
        const a = shake * (3.2 + Math.sin(gTime * 11 + fi) * 2.4)
        const tx = Math.sin(gTime * 8.4 + fi * 0.7) * a
        const ty = Math.cos(gTime * 7.1 + fi * 0.5) * a
        el.style.transform = `translate(${tx}px, ${ty}px) rotate(var(--rot,0deg))`
      })

      o.hud.innerHTML = o.fr()
        ? `<div class="page-error-void-p2__hud-row">Vies : <strong>${lives}</strong> · Temps ${sec}s${hard ? ' · <span style="color:#ff9e9e">LABYRINTHE INSTABLE</span>' : ''}</div>`
        : `<div class="page-error-void-p2__hud-row">Lives: <strong>${lives}</strong> · Time ${sec}s${hard ? ' · <span style="color:#ff9e9e">MAZE UNSTABLE</span>' : ''}</div>`

      if (!o.isPlaying()) return
      const id = window.requestAnimationFrame(frame)
      o.setRaf(id)
    }

    carve()
    t0 = performance.now()
    nextEnemyAt = t0
    setupEnemies()
    lastDoor = t0
    fragIv = window.setInterval(spawnFrag, 2200)
    const id0 = window.requestAnimationFrame(frame)
    o.setRaf(id0)

    return () => {
      if (fragIv) window.clearInterval(fragIv)
      fragIv = 0
      const r = o.getRaf()
      if (r) o.cancelRaf(r)
      o.fragLayer.querySelectorAll('.page-error-void-p2-frag--maze').forEach((el) => el.remove())
    }
  }

  window.Solea404VoidMaze = { start }
})()
