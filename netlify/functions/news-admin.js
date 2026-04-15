'use strict'

const { createClient } = require('@supabase/supabase-js')
const { CORS_POST, json, timingSafeTokenEqual } = require('./_shared')

function isUuid(s) {
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(s)
}

exports.handler = async (event) => {
  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 204, headers: CORS_POST, body: '' }
  }
  if (event.httpMethod !== 'POST') {
    return json(405, { error: 'method_not_allowed' }, CORS_POST)
  }

  const url = process.env.SUPABASE_URL
  const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY
  const adminToken = process.env.NEWS_ADMIN_TOKEN

  if (!url || !serviceKey || !adminToken) {
    return json(503, { error: 'not_configured' }, CORS_POST)
  }

  const auth =
    (event.headers && (event.headers.authorization || event.headers.Authorization)) || ''
  const m = /^Bearer\s+(.+)$/i.exec(String(auth))
  const token = m ? m[1].trim() : ''
  if (!timingSafeTokenEqual(adminToken, token)) {
    return json(401, { error: 'unauthorized' }, CORS_POST)
  }

  let payload
  try {
    payload = JSON.parse(event.body || '{}')
  } catch {
    return json(400, { error: 'invalid_json' }, CORS_POST)
  }

  const action = payload.action
  if (typeof action !== 'string') {
    return json(400, { error: 'missing_action' }, CORS_POST)
  }

  const supabase = createClient(url, serviceKey)

  try {
    if (action === 'list') {
      const { data, error } = await supabase
        .from('news_posts')
        .select('*')
        .order('sort_order', { ascending: false })
        .order('updated_at', { ascending: false })
      if (error) throw error
      return json(200, { posts: data || [] }, CORS_POST)
    }

    if (action === 'create') {
      const title = typeof payload.title === 'string' ? payload.title : ''
      const body = typeof payload.body === 'string' ? payload.body : ''
      const is_published = Boolean(payload.is_published)
      const sort_order =
        typeof payload.sort_order === 'number' && Number.isFinite(payload.sort_order)
          ? Math.trunc(payload.sort_order)
          : 0
      const { data, error } = await supabase
        .from('news_posts')
        .insert({ title, body, is_published, sort_order })
        .select()
        .single()
      if (error) throw error
      return json(200, { post: data }, CORS_POST)
    }

    if (action === 'update') {
      const id = typeof payload.id === 'string' ? payload.id : ''
      if (!isUuid(id)) return json(400, { error: 'invalid_id' }, CORS_POST)
      const patch = { updated_at: new Date().toISOString() }
      if (typeof payload.title === 'string') patch.title = payload.title
      if (typeof payload.body === 'string') patch.body = payload.body
      if (typeof payload.is_published === 'boolean') patch.is_published = payload.is_published
      if (typeof payload.sort_order === 'number' && Number.isFinite(payload.sort_order)) {
        patch.sort_order = Math.trunc(payload.sort_order)
      }
      const { data, error } = await supabase
        .from('news_posts')
        .update(patch)
        .eq('id', id)
        .select()
        .maybeSingle()
      if (error) throw error
      if (!data) return json(404, { error: 'not_found' }, CORS_POST)
      return json(200, { post: data }, CORS_POST)
    }

    if (action === 'delete') {
      const id = typeof payload.id === 'string' ? payload.id : ''
      if (!isUuid(id)) return json(400, { error: 'invalid_id' }, CORS_POST)
      const { error } = await supabase.from('news_posts').delete().eq('id', id)
      if (error) throw error
      return json(200, { ok: true }, CORS_POST)
    }

    return json(400, { error: 'unknown_action' }, CORS_POST)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    console.error('news-admin', msg)
    return json(500, { error: 'server', detail: msg }, CORS_POST)
  }
}
