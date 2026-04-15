/**
 * Admin actus — appelle `/.netlify/functions/news-admin` avec Bearer NEWS_ADMIN_TOKEN.
 * Aperçu : même rendu que le site / launcher (SoleaActuMarkup).
 */
;(function () {
  const STORAGE_KEY = 'solea_news_admin_token'
  const API = '/.netlify/functions/news-admin'

  const el = (id) => document.getElementById(id)

  /** Même secret que côté Netlify après trim ; enlève BOM / espaces insécables souvent collés au collage. */
  function normalizeToken(raw) {
    if (raw == null) return ''
    return String(raw)
      .replace(/^\uFEFF/, '')
      .replace(/\u00A0/g, ' ')
      .replace(/[\u200B-\u200D\uFEFF]/g, '')
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

  function showMsg(node, text, kind) {
    if (!node) return
    node.hidden = !text
    node.textContent = text || ''
    node.className = 'msg' + (kind ? ` ${kind}` : '')
  }

  function friendlyError(err) {
    const s = err instanceof Error ? err.message : String(err)
    if (s.startsWith('not_configured')) {
      return s
    }
    if (s === 'unauthorized' || s.startsWith('unauthorized')) {
      return (
        'unauthorized — Le token ne correspond pas à NEWS_ADMIN_TOKEN sur Netlify (contexte Production). ' +
        'Vérifie qu’il n’y a pas d’espace ou de retour à la ligne en trop dans la variable Netlify, ' +
        'redéploie après modification, puis colle à nouveau le token et clique « Enregistrer dans cette session ».'
      )
    }
    return s
  }

  async function api(action, extra) {
    const token = getToken()
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
        err = `not_configured — Manquantes côté Netlify (Functions / Production) : ${data.missing.join(', ')}. Ouvre Site configuration → Environment variables, complète ces noms exactement, puis Deploys → Trigger deploy.`
      } else if (data.error === 'not_configured') {
        err =
          'not_configured — Variables serveur incomplètes. Vérifie SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, NEWS_ADMIN_TOKEN (même orthographe, avec valeurs sur le contexte Production).'
      }
      throw new Error(err)
    }
    return data
  }

  function escapeHtml(s) {
    return String(s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
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
    el('editor-panel').hidden = false
    schedulePreview()
  }

  async function refreshList() {
    const list = el('post-list')
    const msg = el('list-msg')
    const hint = el('list-hint')
    if (hint) {
      hint.hidden = true
      hint.textContent = ''
    }
    if (!getToken()) {
      list.innerHTML = ''
      showMsg(msg, 'Enregistre un token pour lister les articles.', 'err')
      el('editor-panel').hidden = true
      return
    }
    showMsg(msg, '', '')
    list.innerHTML = '<li>Chargement…</li>'
    try {
      const data = await api('list')
      const posts = data.posts || []
      list.innerHTML = ''
      if (posts.length === 0) {
        list.innerHTML = '<li class="post-meta">Aucun article.</li>'
        return
      }
      for (const p of posts) {
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
      el('editor-panel').hidden = false
    } catch (e) {
      list.innerHTML = ''
      const fe = friendlyError(e)
      showMsg(msg, fe, 'err')
      if (hint && String(fe).includes('SUPABASE')) {
        hint.hidden = false
        hint.textContent =
          'Astuce : dans Netlify → Environment variables, les noms doivent être exactement SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY, NEWS_ADMIN_TOKEN.'
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

  el('btn-save-token').addEventListener('click', () => {
    const v = normalizeToken(el('token').value)
    if (!v) {
      showMsg(el('auth-msg'), 'Token vide.', 'err')
      return
    }
    setToken(v)
    el('token').value = v
    showMsg(el('auth-msg'), 'Token enregistré pour cette session.', 'ok')
    void refreshList()
  })

  el('btn-clear-token').addEventListener('click', () => {
    setToken('')
    el('token').value = ''
    showMsg(el('auth-msg'), 'Token effacé.', 'ok')
    void refreshList()
  })

  el('btn-refresh').addEventListener('click', () => void refreshList())

  el('btn-new').addEventListener('click', () => {
    resetForm()
    el('editor-panel').hidden = false
  })

  el('edit-body').addEventListener('input', schedulePreview)
  el('edit-title').addEventListener('input', schedulePreview)

  el('btn-save').addEventListener('click', async () => {
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
  })

  const existing = getToken()
  if (existing) el('token').value = existing
  void refreshList()
  schedulePreview()
})()
