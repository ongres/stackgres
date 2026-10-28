import { useEffect, useState } from 'react'
import { toast } from 'sonner'
import { api } from '../lib/api'

// Edit mode: fetch the resource fresh so the payload spreads current
// metadata (resourceVersion included) and unknown spec fields. The Vue forms
// mixed three strategies (pre-submit GET, store clone, store alias); a fresh
// GET at form load covers them all.
export function useExistingResource(kind, namespace, name, enabled) {
  const [previous, setPrevious] = useState(null)
  const [loaded, setLoaded] = useState(!enabled)

  useEffect(() => {
    if (!enabled) {
      return
    }
    let cancelled = false
    api
      .getResourceDetails(kind, namespace, name)
      .then((data) => {
        if (!cancelled) {
          setPrevious(data)
          setLoaded(true)
        }
      })
      .catch((error) => {
        if (!cancelled) {
          toast.error(`Could not load ${name}: ${error.message}`)
          setLoaded(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [kind, namespace, name, enabled])

  return { previous, loaded }
}
