import { useCallback, useEffect, useState } from 'react'
import { getActuSoleaJsonUrl } from './actusoleaConfig'
import type { ActuSoleaJson } from './actusoleaTypes'
import {
  ACTU_SOLEA_UPDATED_EVENT,
  fetchAndCacheActuSolea,
  readActuSoleaCache,
  readActuSoleaFetchedAt
} from './actusoleaFetch'

export type ActuSoleaFeedState = {
  loading: boolean
  error: string | null
  data: ActuSoleaJson | null
  fetchedAt: string | null
  hasUrl: boolean
}

export function useActuSoleaFeed(): ActuSoleaFeedState & {
  refresh: () => Promise<void>
} {
  const url = getActuSoleaJsonUrl()
  const hasUrl = Boolean(url)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [data, setData] = useState<ActuSoleaJson | null>(() => readActuSoleaCache())
  const [fetchedAt, setFetchedAt] = useState<string | null>(() => readActuSoleaFetchedAt())

  useEffect(() => {
    const sync = () => {
      setData(readActuSoleaCache())
      setFetchedAt(readActuSoleaFetchedAt())
    }
    window.addEventListener(ACTU_SOLEA_UPDATED_EVENT, sync)
    return () => window.removeEventListener(ACTU_SOLEA_UPDATED_EVENT, sync)
  }, [])

  const refresh = useCallback(async () => {
    const u = getActuSoleaJsonUrl()
    if (!u) {
      setError(null)
      setData(readActuSoleaCache())
      return
    }
    setLoading(true)
    setError(null)
    try {
      const r = await fetchAndCacheActuSolea()
      if (r.ok) {
        setData(r.data)
        setFetchedAt(readActuSoleaFetchedAt())
        window.dispatchEvent(new Event(ACTU_SOLEA_UPDATED_EVENT))
      } else if (r.error !== 'no_url') {
        setError(r.error)
        if (!readActuSoleaCache()) setData(null)
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e)
      setError(msg)
    } finally {
      setLoading(false)
    }
  }, [])

  return { loading, error, data, fetchedAt, hasUrl, refresh }
}
