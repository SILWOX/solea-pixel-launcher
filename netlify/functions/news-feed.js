'use strict'

const { createClient } = require('@supabase/supabase-js')
const { CORS_GET, json } = require('./_shared')

const PLACEHOLDER = {
  version: 1,
  updatedAt: new Date().toISOString(),
  segments: [
    '## Solea Pixel\n\nLive news will appear here once Supabase env vars are set on Netlify (see repo `docs/supabase-news-schema.sql`).',
  ],
}

exports.handler = async (event) => {
  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 204, headers: CORS_GET, body: '' }
  }
  if (event.httpMethod !== 'GET') {
    return json(405, { error: 'method_not_allowed' }, CORS_GET)
  }

  const url = process.env.SUPABASE_URL
  const anon = process.env.SUPABASE_ANON_KEY
  if (!url || !anon) {
    return json(200, PLACEHOLDER, CORS_GET)
  }

  try {
    const supabase = createClient(url, anon)
    const { data, error } = await supabase
      .from('news_posts')
      .select('title, body, sort_order, updated_at')
      .eq('is_published', true)
      .order('sort_order', { ascending: false })
      .order('updated_at', { ascending: false })

    if (error) {
      console.error('news-feed', error)
      return json(
        200,
        {
          version: 1,
          updatedAt: new Date().toISOString(),
          segments: [
            '## Solea Pixel\n\nThe news service is temporarily unavailable. Please try again later.',
          ],
        },
        CORS_GET,
      )
    }

    const rows = data || []
    let segments = rows.map((row) => {
      const t = (row.title || '').trim()
      const b = (row.body || '').trim()
      if (t) return `## ${t}\n\n${b}`
      return b.length > 0 ? b : '## \n\n_(empty)_'
    })

    if (segments.length === 0) {
      segments = ['## Solea Pixel\n\nNo published posts yet. Check back soon!']
    }

    let updatedAt = new Date().toISOString()
    if (rows.length > 0) {
      const latest = rows.reduce((acc, r) => {
        const u = r.updated_at
        if (!u) return acc
        return !acc || u > acc ? u : acc
      }, '')
      if (latest) updatedAt = latest
    }

    return json(
      200,
      { version: 1, updatedAt, segments },
      {
        ...CORS_GET,
        'Cache-Control': 'public, max-age=30, s-maxage=30',
      },
    )
  } catch (e) {
    console.error('news-feed', e)
    return json(200, PLACEHOLDER, CORS_GET)
  }
}
