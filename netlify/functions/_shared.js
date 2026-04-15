'use strict'

const crypto = require('crypto')

const CORS_GET = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
}

const CORS_POST = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
}

function json(statusCode, body, extraHeaders = {}) {
  return {
    statusCode,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...extraHeaders,
    },
    body: JSON.stringify(body),
  }
}

/** Compare secrets without leaking length via timingSafeEqual length rule. */
function timingSafeTokenEqual(expected, received) {
  if (typeof expected !== 'string' || typeof received !== 'string') return false
  if (!expected.length || !received.length) return false
  const eh = crypto.createHash('sha256').update(expected, 'utf8').digest()
  const rh = crypto.createHash('sha256').update(received, 'utf8').digest()
  return crypto.timingSafeEqual(eh, rh)
}

module.exports = { CORS_GET, CORS_POST, json, timingSafeTokenEqual }
