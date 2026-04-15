/**
 * Admin actus — connexion par token (NEWS_ADMIN_TOKEN), puis `/.netlify/functions/news-admin`.
 * La connexion appelle `action: verify` pour valider le secret avant d’afficher l’éditeur.
 */
;(function () {
  const STORAGE_KEY = 'solea_news_admin_token'
  const API = '/.netlify/functions/news-admin'

  const el = (id) => document.getElementById(id)

  function normalizeToken(raw) {
    if (raw == null) return ''
    return String(raw)
      .replace(/^\uFEFF/, '')
      .replace(/\u00A0/g, ' ')
      .replace(/[\u200B-\u200D\uFEFF]/g, '')
      .replace(/\r\n/g, '\n')
      .split('\n')
      .map((l) => l.trimEnd())
      .join('\n')
      .trim()
  }

  function getToken() {
    try {
      const s = sessionStorage.getItem(STORAGE_KEY)
      const n = normalizeToken(s)
      if (n) return n
    } catch {
      /* ignore */
    }
    return ''
  }

  function setToken(v) {
    try {
      const n = normalizeToken(v)
      if (n) sessionStorage.setItem(STORAGE_KEY, n)
      else sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      /* ignore */
    }
  }

  /** @type {Array<Record<string, unknown>>} */
  let postsCache = []

  function showMsg(node, text, kind) {
    if (!node) return
    node.hidden = !text
    node.textContent = text || ''
    node.className = 'msg' + (kind ? ` ${kind}` : '')
  }

  function friendlyError(err) {
    const s = err instanceof Error ? err.message : String(err)
    if (s.includes('unknown_action')) {
      return (
        'Le site Netlify n’a pas encore la dernière fonction `news-admin` (action `verify`). ' +
        'Pousse le dépôt et **redéploie** Production, puis réessaie.'
      )
    }
    if (s.startsWith('not_configured')) {
      return s
    }
    if (s === 'unauthorized' || s.startsWith('unauthorized')) {
      return (
        'Accès refusé — le mot de passe ne correspond pas à NEWS_ADMIN_TOKEN sur Netlify (Production). ' +
        'Vérifie la variable (pas d’espace en trop, pas de guillemets), redéploie si tu l’as modifiée, puis reconnecte-toi.'
      )
    }
    return s
  }

  async function apiRequest(token, action, extra) {
    if (!token) throw new Error('Token manquant')
    const res = await fetch(API, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ action, ...extra }),
    })
    const data = await res.json().catch(() => ({}))
    if (!res.ok) {
      const d = data && data.detail ? ` — ${data.detail}` : ''
      let err = (data.error || res.statusText || 'Erreur') + d
      if (data.error === 'not_configured' && Array.isArray(data.missing) && data.missing.length) {
        err = `not_configured — Manquantes côté Netlify : ${data.missing.join(', ')}. Environment variables → Production → redeploy.`
      } else if (data.error === 'not_configured') {
        err =
          'not_configured — Vérifie SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, NEWS_ADMIN_TOKEN sur Netlify (Production).'
      }
      throw new Error(err)
    }
    return data
  }

  async function api(action, extra) {
    return apiRequest(getToken(), action, extra)
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
  }

  function showLogin() {
    const login = el('screen-login')
    const app = el('screen-app')
    if (app) {
      app.setAttribute('hidden', '')
      app.setAttribute('aria-hidden', 'true')
    }
    if (login) {
      login.removeAttribute('hidden')
      login.removeAttribute('aria-hidden')
    }
  }

  function showApp() {
    const login = el('screen-login')
    const app = el('screen-app')
    if (login) {
      login.setAttribute('hidden', '')
      login.setAttribute('aria-hidden', 'true')
    }
    if (app) {
      app.removeAttribute('hidden')
      app.removeAttribute('aria-hidden')
    }
  }

  async function connectWithToken(raw) {
    const token = normalizeToken(raw)
    if (!token) {
      showMsg(el('login-msg'), 'Mot de passe vide.', 'err')
      return false
    }
    try {
      await apiRequest(token, 'verify', {})
      setToken(token)
      showMsg(el('login-msg'), '', '')
      showApp()
      return true
    } catch (e) {
      setToken('')
      showMsg(el('login-msg'), friendlyError(e), 'err')
      return false
    }
  }

  function logout() {
    setToken('')
    const lt = el('login-token')
    if (lt) lt.value = ''
    postsCache = []
    el('post-list').innerHTML = ''
    showMsg(el('list-msg'), '', '')
    resetForm()
    showLogin()
  }

  function buildPreviewSegment() {
    const title = (el('edit-title') && el('edit-title').value.trim()) || 'Sans titre'
    const body = (el('edit-body') && el('edit-body').value) || ''
    return `## ${title}\n\n${body}`
  }

  let previewTimer = null
  function schedulePreview() {
    window.clearTimeout(previewTimer)
    previewTimer = window.setTimeout(runPreview, 100)
  }

  function runPreview() {
    const root = el('preview-root')
    const count = el('edit-body-count')
    if (!root) return
    const ta = el('edit-body')
    if (count && ta) {
      count.textContent = `${ta.value.length} / 12000 caractères`
    }
    if (!window.SoleaActuMarkup || typeof window.SoleaActuMarkup.renderSegmentCardHtml !== 'function') {
      root.innerHTML = '<p class="post-meta">Chargement du moteur de rendu…</p>'
      return
    }
    try {
      const html = window.SoleaActuMarkup.renderSegmentCardHtml(buildPreviewSegment())
      root.innerHTML = `<article class="live-news-card">${html}</article>`
    } catch (e) {
      root.innerHTML = `<p class="msg err">${escapeHtml(e instanceof Error ? e.message : String(e))}</p>`
    }
  }

  function insertAround(before, after) {
    const ta = el('edit-body')
    if (!ta) return
    const s = ta.selectionStart
    const e = ta.selectionEnd
    const v = ta.value
    const sel = v.slice(s, e) || 'texte'
    ta.value = v.slice(0, s) + before + sel + after + v.slice(e)
    const ns = s + before.length
    const ne = ns + sel.length
    ta.selectionStart = ns
    ta.selectionEnd = ne
    ta.focus()
    schedulePreview()
  }

  function insertAtCursor(text) {
    const ta = el('edit-body')
    if (!ta) return
    const s = ta.selectionStart
    const v = ta.value
    ta.value = v.slice(0, s) + text + v.slice(s)
    const n = s + text.length
    ta.selectionStart = n
    ta.selectionEnd = n
    ta.focus()
    schedulePreview()
  }

  function resetForm() {
    el('edit-id').value = ''
    el('edit-title').value = ''
    el('edit-body').value = ''
    el('edit-sort').value = '0'
    el('edit-published').checked = false
    showMsg(el('editor-msg'), '', '')
    schedulePreview()
  }

  function fillForm(p) {
    el('edit-id').value = p.id || ''
    el('edit-title').value = p.title || ''
    el('edit-body').value = p.body || ''
    el('edit-sort').value = String(p.sort_order ?? 0)
    el('edit-published').checked = Boolean(p.is_published)
    schedulePreview()
    el('editor-panel').scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  function getFilterQuery() {
    const f = el('post-filter')
    return f ? normalizeToken(f.value).toLowerCase() : ''
  }

  function renderPostList(posts) {
    const list = el('post-list')
    const msg = el('list-msg')
    const hint = el('list-hint')
    if (hint) {
      hint.hidden = true
      hint.textContent = ''
    }
    showMsg(msg, '', '')
    list.innerHTML = ''
    const q = getFilterQuery()
    const filtered = q
      ? posts.filter((p) => String(p.title || '').toLowerCase().includes(q) || String(p.id || '').toLowerCase().includes(q))
      : posts

    if (filtered.length === 0) {
      list.innerHTML =
        '<li class="post-meta">' +
        (posts.length === 0
          ? 'Aucun article — crée-en un avec le formulaire ci-dessus puis <strong>Enregistrer</strong>.'
          : 'Aucun article ne correspond au filtre.') +
        '</li>'
      if (posts.length === 0 && hint) {
        hint.hidden = false
        hint.textContent = 'Coche « Publié » pour afficher l’article sur le site et dans le launcher.'
      }
      return
    }

    for (const p of filtered) {
      const li = document.createElement('li')
      const left = document.createElement('div')
      left.innerHTML = `<div class="post-title">${escapeHtml(p.title || '(sans titre)')}</div>
        <div class="post-meta">${p.is_published ? 'Publié' : 'Brouillon'} · tri ${p.sort_order ?? 0} · <code>${escapeHtml(p.id)}</code></div>`
      const actions = document.createElement('div')
      actions.className = 'row'
      actions.style.margin = '0'
      const bEdit = document.createElement('button')
      bEdit.type = 'button'
      bEdit.className = 'secondary'
      bEdit.textContent = 'Modifier'
      bEdit.addEventListener('click', () => fillForm(p))
      const bDel = document.createElement('button')
      bDel.type = 'button'
      bDel.className = 'danger'
      bDel.textContent = 'Supprimer'
      bDel.addEventListener('click', async () => {
        if (!confirm('Supprimer cet article ?')) return
        try {
          await api('delete', { id: p.id })
          await refreshList()
          resetForm()
        } catch (e) {
          showMsg(msg, friendlyError(e), 'err')
        }
      })
      actions.appendChild(bEdit)
      actions.appendChild(bDel)
      li.appendChild(left)
      li.appendChild(actions)
      list.appendChild(li)
    }
  }

  async function refreshList() {
    const msg = el('list-msg')
    const hint = el('list-hint')
    if (!getToken()) {
      postsCache = []
      el('post-list').innerHTML = ''
      return
    }
    el('post-list').innerHTML = '<li>Chargement…</li>'
    try {
      const data = await api('list')
      postsCache = data.posts || []
      renderPostList(postsCache)
    } catch (e) {
      postsCache = []
      el('post-list').innerHTML = ''
      const fe = friendlyError(e)
      showMsg(msg, fe, 'err')
      if (String(fe).includes('unauthorized') || String(fe).includes('Accès refusé')) {
        logout()
        showMsg(el('login-msg'), fe, 'err')
      }
    }
  }

  document.querySelectorAll('.toolbar .tool').forEach((btn) => {
    btn.addEventListener('click', () => {
      const w = btn.getAttribute('data-wrap')
      if (w) {
        const parts = w.split('::')
        if (parts.length === 2) insertAround(parts[0], parts[1])
        return
      }
      const line = btn.getAttribute('data-line')
      if (line) {
        insertAtCursor(line)
        return
      }
      const block = btn.getAttribute('data-block')
      if (block) {
        const mid = block.indexOf('|')
        if (mid !== -1) {
          const a = block.slice(0, mid)
          const b = block.slice(mid + 1)
          insertAround(a, b)
        }
      }
    })
  })

  el('btn-login').addEventListener('click', async () => {
    const raw = el('login-token').value
    showMsg(el('login-msg'), 'Vérification…', 'hint')
    const ok = await connectWithToken(raw)
    if (ok) {
      el('login-token').value = ''
      await refreshList()
      schedulePreview()
    }
  })

  el('login-token').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      el('btn-login').click()
    }
  })

  el('btn-logout').addEventListener('click', () => logout())

  el('btn-refresh').addEventListener('click', () => void refreshList())

  el('post-filter')?.addEventListener('input', () => renderPostList(postsCache))

  function focusEditor() {
    el('editor-panel').scrollIntoView({ behavior: 'smooth', block: 'start' })
    const titleInput = el('edit-title')
    if (titleInput) window.setTimeout(() => titleInput.focus(), 320)
  }

  el('btn-create-post')?.addEventListener('click', () => {
    resetForm()
    focusEditor()
  })

  el('btn-new').addEventListener('click', () => {
    resetForm()
  })

  el('btn-duplicate').addEventListener('click', () => {
    const title = el('edit-title').value.trim()
    const body = el('edit-body').value
    el('edit-id').value = ''
    el('edit-title').value = title ? `${title} (copie)` : ''
    el('edit-body').value = body
    el('edit-published').checked = false
    showMsg(el('editor-msg'), 'Brouillon dupliqué — enregistre pour créer un nouvel article.', 'ok')
    schedulePreview()
    focusEditor()
  })

  el('edit-body').addEventListener('input', schedulePreview)
  el('edit-title').addEventListener('input', schedulePreview)

  async function saveArticle() {
    const msg = el('editor-msg')
    showMsg(msg, '', '')
    const id = el('edit-id').value.trim()
    const title = el('edit-title').value
    const body = el('edit-body').value
    const sort_order = Number(el('edit-sort').value)
    const is_published = el('edit-published').checked
    try {
      if (id) {
        await api('update', {
          id,
          title,
          body,
          sort_order: Number.isFinite(sort_order) ? sort_order : 0,
          is_published,
        })
        showMsg(msg, 'Article mis à jour.', 'ok')
      } else {
        const r = await api('create', {
          title,
          body,
          sort_order: Number.isFinite(sort_order) ? sort_order : 0,
          is_published,
        })
        if (r.post && r.post.id) el('edit-id').value = r.post.id
        showMsg(msg, 'Article créé.', 'ok')
      }
      await refreshList()
      schedulePreview()
    } catch (e) {
      showMsg(msg, friendlyError(e), 'err')
    }
  }

  el('btn-save').addEventListener('click', () => void saveArticle())

  document.addEventListener('keydown', (e) => {
    if (!el('screen-app') || el('screen-app').hidden) return
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
      e.preventDefault()
      void saveArticle()
    }
  })

  async function boot() {
    const existing = getToken()
    if (existing) {
      const ok = await connectWithToken(existing)
      if (ok) {
        await refreshList()
        schedulePreview()
        return
      }
    }
    showLogin()
    schedulePreview()
  }

  void boot()
})()
