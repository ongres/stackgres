import { useCallback, useEffect, useState } from 'react'
import { api } from '../lib/api'
import { usePermissions } from '../contexts/PermissionsContext'

// Cluster-scoped collections outside the namespaced data caches (users,
// roles, clusterroles): fetched once when permissions are loaded, with a
// manual refresh. Returns null while loading; [] when the fetch fails.
export function useFlatList(kind, enabled = true) {
  const { loaded } = usePermissions()
  const [items, setItems] = useState(null)

  const refresh = useCallback(() => {
    if (!enabled) {
      return Promise.resolve()
    }
    return api
      .get(kind)
      .then(setItems)
      .catch(() => setItems([]))
  }, [kind, enabled])

  useEffect(() => {
    if (loaded) {
      refresh()
    }
  }, [loaded, refresh])

  return { items, refresh }
}
