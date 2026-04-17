/**
 * Hub /content/ — Modrinth v3, catégories Modpacks / Mods / Resource packs / Autres.
 */
;(function () {
  const ORG_SLUG = 'soleapixel'
  const API_ORG = `https://api.modrinth.com/v3/organization/${ORG_SLUG}`
  const API_PROJECTS = `https://api.modrinth.com/v3/organization/${ORG_SLUG}/projects`
  const WEB_ORG = `https://modrinth.com/organization/${ORG_SLUG}`

  /** Blocs principaux : sépare clairement modpack / mod / resource pack, puis le reste. */
  const CATEGORY_BUCKETS = [
    {
      id: 'modpack',
      types: ['modpack'],
      titleKey: 'content.type.modpack',
      descKey: 'content.catDescModpack',
      tocClass: 'mr-toc__link--modpack',
    },
    {
      id: 'mod',
      types: ['mod'],
      titleKey: 'content.type.mod',
      descKey: 'content.catDescMod',
      tocClass: 'mr-toc__link--mod',
    },
    {
      id: 'resourcepack',
      types: ['resourcepack'],
      titleKey: 'content.type.resourcepack',
      descKey: 'content.catDescResourcepack',
      tocClass: 'mr-toc__link--resourcepack',
    },
    {
      id: 'more',
      types: ['shader', 'plugin', 'datapack', 'other'],
      titleKey: 'content.mainMore',
      descKey: 'content.catDescMore',
      tocClass: 'mr-toc__link--more',
    },
  ]

  const TYPE_PATH = {
    mod: 'mod',
    modpack: 'modpack',
    resourcepack: 'resourcepack',
    shader: 'shader',
    plugin: 'plugin',
    datapack: 'datapack',
  }

  const LOADER_LABELS = {
    forge: 'Forge',
    neoforge: 'NeoForge',
    fabric: 'Fabric',
    quilt: 'Quilt',
    datapack: 'Data pack',
    mrpack: 'Mrpack',
    paper: 'Paper',
    purpur: 'Purpur',
    spigot: 'Spigot',
    bukkit: 'Bukkit',
    velocity: 'Velocity',
    waterfall: 'Waterfall',
    bungeecord: 'BungeeCord',
    risugami: 'Risugami',
    canvas: 'Canvas',
    vanilla: 'Vanilla',
  }

  const ICONS = {
    modpack: `<svg class="mr-cat__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M12 2l9 5v10l-9 5-9-5V7l9-5z"/><path d="M12 22V12"/><path d="m3 7 9 5 9-5"/></svg>`,
    mod: `<svg class="mr-cat__svg mr-cat__svg--fill" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><rect x="4" y="4" width="7" height="7" rx="1.5" opacity="0.95"/><rect x="13" y="4" width="7" height="7" rx="1.5" opacity="0.85"/><rect x="4" y="13" width="7" height="7" rx="1.5" opacity="0.85"/><rect x="13" y="13" width="7" height="7" rx="1.5" opacity="0.7"/></svg>`,
    resourcepack: `<svg class="mr-cat__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><circle cx="8.5" cy="10.5" r="1.5"/><path d="M21 17l-5-5-4 4-3-3"/></svg>`,
    more: `<svg class="mr-cat__svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M12 3v2M12 19v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M3 12h2M19 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41"/><circle cx="12" cy="12" r="3.5"/></svg>`,
  }

  function esc(s) {
    if (s == null) return ''
    const d = document.createElement('div')
    d.textContent = String(s)
    return d.innerHTML
  }

  function t(key, vars) {
    const fn = window.SoleaI18n && window.SoleaI18n.t
    let s = fn ? fn(key) : key
    if (vars && typeof s === 'string') {
      Object.keys(vars).forEach((k) => {
        s = s.replace(new RegExp(`\\{${k}\\}`, 'g'), String(vars[k]))
      })
    }
    return s
  }

  function projectPageUrl(slug, primaryType) {
    const seg = TYPE_PATH[primaryType]
    if (seg) return `https://modrinth.com/${seg}/${encodeURIComponent(slug)}`
    return `https://modrinth.com/project/${encodeURIComponent(slug)}`
  }

  function primaryType(p) {
    const types = p.project_types
    if (Array.isArray(types) && types.length) return types[0]
    return 'other'
  }

  function coverImage(p) {
    const gal = p.gallery
    if (Array.isArray(gal) && gal.length) {
      const feat = gal.find((g) => g.featured)
      const pick = feat || gal[0]
      if (pick && pick.url) return pick.url
    }
    return p.icon_url || ''
  }

  function sortByDownloads(a, b) {
    return (b.downloads || 0) - (a.downloads || 0)
  }

  function nf(n) {
    const lang = window.SoleaI18n && window.SoleaI18n.getLang ? window.SoleaI18n.getLang() : 'en'
    try {
      return new Intl.NumberFormat(lang === 'fr' ? 'fr-FR' : 'en-US', { maximumFractionDigits: 0 }).format(n)
    } catch {
      return String(n)
    }
  }

  function versionRank(v) {
    const parts = String(v).split('.').map((x) => {
      const n = parseInt(x, 10)
      return Number.isFinite(n) ? n : 0
    })
    return (parts[0] || 0) * 1e6 + (parts[1] || 0) * 1e3 + (parts[2] || 0)
  }

  function topVersions(versions, max) {
    if (!Array.isArray(versions) || !versions.length) return []
    const u = [...new Set(versions.map(String))]
    u.sort((a, b) => versionRank(b) - versionRank(a))
    return u.slice(0, max)
  }

  function labelLoader(id) {
    const k = String(id || '').toLowerCase()
    return LOADER_LABELS[k] || (id ? id.charAt(0).toUpperCase() + id.slice(1) : '')
  }

  function chipsHtml(p) {
    const chips = []
    const gv = topVersions(p.game_versions, 4)
    gv.forEach((v) => chips.push(`<span class="mr-chip mr-chip--version">${esc(v)}</span>`))
    const loaders = Array.isArray(p.loaders) ? p.loaders.slice(0, 4) : []
    const extraMrpack = Array.isArray(p.mrpack_loaders) ? p.mrpack_loaders : []
    const loaderSet = [...new Set([...loaders, ...extraMrpack].map(String))]
    loaderSet.forEach((L) => {
      const lab = labelLoader(L)
      if (lab) chips.push(`<span class="mr-chip mr-chip--loader">${esc(lab)}</span>`)
    })
    const cats = Array.isArray(p.categories) ? p.categories.slice(0, 2) : []
    cats.forEach((c) => {
      const pretty = String(c).replace(/-/g, ' ')
      chips.push(`<span class="mr-chip mr-chip--tag">${esc(pretty)}</span>`)
    })
    if (!chips.length) return ''
    return `<div class="mr-card__chips">${chips.join('')}</div>`
  }

  function membersMountEl(root) {
    return (root && root.querySelector('[data-mr-members]')) || document.querySelector('[data-mr-members]')
  }

  function renderMembers(members, root) {
    const el = membersMountEl(root)
    if (!el || !Array.isArray(members)) return
    el.innerHTML = members
      .filter((m) => m.accepted !== false)
      .map((m) => {
        const u = m.user || {}
        const rawName = u.username || '—'
        const name = esc(rawName)
        const ph = esc(String(rawName).charAt(0).toUpperCase())
        const role = esc(m.role || '')
        const av = u.avatar_url
          ? `<img src="${esc(u.avatar_url)}" alt="" width="56" height="56" loading="lazy" />`
          : `<span class="mr-member__ph" aria-hidden="true">${ph}</span>`
        const prof = u.username ? `https://modrinth.com/user/${encodeURIComponent(u.username)}` : WEB_ORG
        return `<a class="mr-member" href="${esc(prof)}"><span class="mr-member__avatar">${av}</span><span class="mr-member__body"><span class="mr-member__name">${name}</span><span class="mr-member__role">${role}</span></span></a>`
      })
      .join('')
  }

  function cardHtml(p, bucketId) {
    const pt = primaryType(p)
    const url = projectPageUrl(p.slug, pt)
    const img = coverImage(p)
    const imgTag = img
      ? `<img class="mr-card__img" src="${esc(img)}" alt="" loading="lazy" />`
      : `<div class="mr-card__img mr-card__img--empty" aria-hidden="true"></div>`
    const chips = chipsHtml(p)
    const bClass = bucketId ? ` mr-card--${esc(bucketId)}` : ''
    return `<article class="mr-card${bClass}" data-type="${esc(pt)}">
  <a class="mr-card__media" href="${esc(url)}">${imgTag}</a>
  <div class="mr-card__body">
    ${chips}
    <h3 class="mr-card__title"><a href="${esc(url)}">${esc(p.name)}</a></h3>
    <p class="mr-card__sum">${esc(p.summary || '')}</p>
    <div class="mr-card__divider" aria-hidden="true"></div>
    <div class="mr-card__foot">
      <div class="mr-card__stats"><span>${esc(t('content.statsDl', { n: nf(p.downloads || 0) }))}</span><span>${esc(t('content.statsFollow', { n: nf(p.followers || 0) }))}</span></div>
      <div class="mr-card__actions">
        <a class="mr-btn mr-btn--ghost" href="${esc(url)}">${esc(t('content.view'))}</a>
        <a class="mr-btn mr-btn--gold" href="${esc(url)}">${esc(t('content.download'))}</a>
      </div>
    </div>
  </div>
</article>`
  }

  function collectProjectsByBucket(projects) {
    const out = CATEGORY_BUCKETS.map((b) => ({
      ...b,
      projects: [],
    }))
    const typeToBucket = {}
    CATEGORY_BUCKETS.forEach((b) => {
      b.types.forEach((ty) => {
        typeToBucket[ty] = b.id
      })
    })

    projects.forEach((p) => {
      const pt = primaryType(p)
      const bid = Object.prototype.hasOwnProperty.call(typeToBucket, pt) ? typeToBucket[pt] : 'more'
      const bucket = out.find((o) => o.id === bid)
      if (bucket) bucket.projects.push(p)
    })

    out.forEach((b) => b.projects.sort(sortByDownloads))
    return out
  }

  let railScrollCleanup = null

  function setRailLayoutMode(on) {
    document.getElementById('mr-layout')?.classList.toggle('mr-layout--has-rail', !!on)
  }

  function renderRail(buckets) {
    const rail = document.querySelector('[data-mr-rail]')
    if (!rail) return

    const parts = []
    parts.push(`<p class="mr-rail__title">${esc(t('content.railTitle'))}</p>`)
    parts.push(`<nav class="mr-rail__nav" aria-label="${esc(t('content.tocNav'))}">`)
    parts.push(
      `<a class="mr-rail__a" href="#contenu-principal"><span class="mr-rail__mark"></span><span class="mr-rail__txt">${esc(t('content.railHero'))}</span></a>`
    )
    parts.push(
      `<a class="mr-rail__a" href="#catalogue"><span class="mr-rail__mark"></span><span class="mr-rail__txt">${esc(t('content.railCatalog'))}</span></a>`
    )
    buckets
      .filter((b) => b.projects.length > 0)
      .forEach((b) => {
        const title = t(b.titleKey)
        const label = title === b.titleKey ? b.id : title
        parts.push(
          `<a class="mr-rail__a mr-rail__a--${esc(b.id)}" href="#cat-${esc(b.id)}"><span class="mr-rail__n">${b.projects.length}</span><span class="mr-rail__txt">${esc(label)}</span></a>`
        )
      })
    parts.push(`</nav>`)
    rail.innerHTML = parts.join('')
    rail.hidden = false
    setRailLayoutMode(true)
    initRailScrollSpy(rail)
  }

  function initRailScrollSpy(rail) {
    if (railScrollCleanup) {
      railScrollCleanup()
      railScrollCleanup = null
    }
    const links = [...rail.querySelectorAll('.mr-rail__a[href^="#"]')]
    if (!links.length) return

    const setActive = () => {
      const marker = window.scrollY + Math.min(160, window.innerHeight * 0.18)
      let activeHref = links[0].getAttribute('href')
      for (const a of links) {
        const href = a.getAttribute('href')
        const id = href && href.startsWith('#') ? href.slice(1) : ''
        const el = id ? document.getElementById(id) : null
        if (!el) continue
        const top = el.getBoundingClientRect().top + window.scrollY
        if (top <= marker + 1) activeHref = href
      }
      links.forEach((a) => a.classList.toggle('mr-rail__a--active', a.getAttribute('href') === activeHref))
    }

    let ticking = false
    const onScroll = () => {
      if (ticking) return
      ticking = true
      requestAnimationFrame(() => {
        ticking = false
        setActive()
      })
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    window.addEventListener('resize', onScroll, { passive: true })
    setActive()
    railScrollCleanup = () => {
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('resize', onScroll)
    }
  }

  function renderCategorySection(bucket) {
    const { id, projects, titleKey, descKey } = bucket
    if (!projects.length) return ''
    const title = t(titleKey)
    const desc = t(descKey)
    const icon = ICONS[id] || ICONS.more
    const cards = projects.map((p) => cardHtml(p, id)).join('')
    const headTitle = title === titleKey ? id : title
    return `<section class="mr-cat mr-cat--${esc(id)}" id="cat-${esc(id)}" data-mr-cat="${esc(id)}">
  <header class="mr-cat__header">
    <div class="mr-cat__iconwrap" aria-hidden="true">${icon}</div>
    <div class="mr-cat__headtext">
      <div class="mr-cat__headrow">
        <h2 class="mr-cat__title">${esc(headTitle)}</h2>
        <span class="mr-cat__badge">${projects.length}</span>
      </div>
      <p class="mr-cat__desc">${esc(desc)}</p>
    </div>
  </header>
  <div class="mr-grid">${cards}</div>
</section>`
  }

  function renderProjects(projects, root) {
    const mount = root.querySelector('[data-mr-projects]')
    if (!mount) return

    const buckets = collectProjectsByBucket(Array.isArray(projects) ? projects : [])
    const any = buckets.some((b) => b.projects.length > 0)
    if (!any) {
      mount.innerHTML = `<p class="mr-empty">${esc(t('content.empty'))}</p>`
    } else {
      const sections = buckets.map(renderCategorySection).filter(Boolean).join('')
      mount.innerHTML = `<div class="mr-catstack">${sections}</div>`
    }
    renderRail(buckets)
  }

  function setStatus(root, key) {
    const el = root.querySelector('[data-mr-status]')
    if (!el) return
    el.textContent = key ? t(key) : ''
    el.hidden = !key
  }

  function onLangChange() {
    const root = document.getElementById('mr-hub-root')
    if (!root || !root.dataset.mrProjectsCache) return
    try {
      const projects = JSON.parse(root.dataset.mrProjectsCache)
      renderProjects(projects, root)
      const org = JSON.parse(root.dataset.mrOrgCache || '{}')
      renderMembers(org.members || [], root)
    } catch {
      /* ignore */
    }
  }

  function init() {
    const root = document.getElementById('mr-hub-root')
    if (!root) return

    window.addEventListener('solea-lang-change', onLangChange)

    setRailLayoutMode(false)
    setStatus(root, 'content.loading')
    const projMount = root.querySelector('[data-mr-projects]')
    const memMount = membersMountEl(root)
    if (projMount) projMount.innerHTML = ''
    if (memMount) memMount.innerHTML = ''

    ;(async () => {
      try {
        const [orgRes, projRes] = await Promise.all([fetch(API_ORG), fetch(API_PROJECTS)])
        if (!orgRes.ok) throw new Error('org')
        if (!projRes.ok) throw new Error('projects')
        const org = await orgRes.json()
        const projects = await projRes.json()
        const list = Array.isArray(projects) ? projects : []
        root.dataset.mrProjectsCache = JSON.stringify(list)
        root.dataset.mrOrgCache = JSON.stringify(org)
        renderMembers(org.members || [], root)
        renderProjects(list, root)
        setStatus(root, null)
        const heroName = root.querySelector('[data-mr-org-name]')
        const heroDesc = root.querySelector('[data-mr-org-desc]')
        const heroIcon = root.querySelector('[data-mr-org-icon]')
        if (heroName) heroName.textContent = org.name || ORG_SLUG
        if (heroDesc) heroDesc.textContent = org.description || ''
        if (heroIcon && org.icon_url) {
          heroIcon.src = org.icon_url
          heroIcon.hidden = false
        }
      } catch {
        setStatus(root, 'content.error')
        const rail = document.querySelector('[data-mr-rail]')
        if (rail) {
          rail.innerHTML = ''
          rail.hidden = true
        }
        setRailLayoutMode(false)
        if (railScrollCleanup) {
          railScrollCleanup()
          railScrollCleanup = null
        }
      }
    })()
  }

  window.SoleaModrinthHub = { init }
})()
