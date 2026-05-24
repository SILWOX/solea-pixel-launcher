/**
 * Better Minecraft teaser — animations au scroll + textures flottantes optionnelles.
 * Pour ajouter des items : dans le HTML, des <img class="bmc-float-piece" src="/assets/bmc-float/ton.png" alt="" />.
 */
;(function () {
  const mqReduce = window.matchMedia('(prefers-reduced-motion: reduce)')

  function initReveal() {
    if (mqReduce.matches) return
    const els = document.querySelectorAll('.bmc-reveal')
    if (!els.length || !('IntersectionObserver' in window)) {
      els.forEach((el) => el.classList.add('bmc-reveal--in'))
      return
    }
    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (!e.isIntersecting) continue
          e.target.classList.add('bmc-reveal--in')
          io.unobserve(e.target)
        }
      },
      { root: null, rootMargin: '0px 0px -8% 0px', threshold: 0.08 },
    )
    els.forEach((el) => io.observe(el))
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initReveal)
  } else {
    initReveal()
  }
})()
