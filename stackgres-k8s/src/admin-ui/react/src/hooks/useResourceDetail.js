import { useEffect, useState } from 'react'
import { api } from '../lib/api'

// Fetches a resource detail endpoint (events, logs, stats…) with optional
// polling, as several Vue views do.
export function useResourceDetail(resource, namespace, name, detail, query = '', pollMs = 0) {
  const [data, setData] = useState(null)

  useEffect(() => {
    let cancelled = false
    const load = () => {
      api
        .getResourceDetails(resource, namespace, name, detail, query)
        .then((result) => {
          if (!cancelled) {
            setData(result)
          }
        })
        .catch(() => {})
    }
    load()
    const interval = pollMs ? setInterval(load, pollMs) : null
    return () => {
      cancelled = true
      if (interval) {
        clearInterval(interval)
      }
    }
  }, [resource, namespace, name, detail, query, pollMs])

  return data
}
