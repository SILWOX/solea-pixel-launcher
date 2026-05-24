/**
 * Tests : `#chapter1` … `#chapter9` dans l’URL (ex. /404.html#chapter3).
 *
 * Implémenté aujourd’hui côté boot (error404-easter.js) :
 * - **#chapter1** — entre directement en mode **glitch** (curseur + fissures).
 * - **#chapter2** ou **#chapter3** — glitch puis absorption forcée ; **#chapter3**
 *   accélère aussi la phase **trou noir** (révélation espace plus rapide).
 * - **#chapter4**+ — au moment de l’espace, **pas** de quête météorite (étoile filante
 *   classique) grâce à `__solea404DeferLuckyUntilMeteor` si tu arrives à cette étape
 *   avec le hash déjà sur l’URL (recharge après progression).
 *
 * Les étapes suivantes (Pac → …) se enchaînent normalement après le flux easter.
 */
;(function () {
  function parse() {
    const m = /^#chapter(\d+)$/i.exec(window.location.hash || '')
    return m ? Math.min(9, Math.max(0, parseInt(m[1], 10))) : 0
  }
  window.__solea404Chapter = parse()
  window.addEventListener('hashchange', () => {
    window.__solea404Chapter = parse()
  })
})()
