import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { api } from '../lib/api'
import { useAuth } from './AuthContext'
import { usePermissions } from './PermissionsContext'

const DataContext = createContext()

// Kinds bulk-loaded and cached for the sidebar, overviews and list views,
// as in the Vue console's fetchAPI.
export const LIST_KINDS = [
  'sgclusters',
  'sgshardedclusters',
  'sgstreams',
  'sginstanceprofiles',
  'sgbackups',
  'sgpgconfigs',
  'sgpoolconfigs',
  'sgobjectstorages',
  'sgscripts',
  'sgdistributedlogs',
  'sgdbops',
]

const POLL_INTERVAL_MS = 30000

// Kinds whose items get their /stats detail attached (as item.stats), as the
// Vue console does for cluster CPU/memory/disk columns and pods tables.
const STATS_KINDS = ['sgclusters', 'sgshardedclusters']

// null = not loaded yet, [] = loaded and empty.
const initialCaches = () => Object.fromEntries(LIST_KINDS.map((kind) => [kind, null]))

export function DataProvider({ children }) {
  const { isLoggedIn } = useAuth()
  const { loaded: permissionsLoaded, canI } = usePermissions()
  const [caches, setCaches] = useState(initialCaches)
  const [ready, setReady] = useState(false)
  const activeRef = useRef(true)

  const fetchKind = useCallback(
    async (kind) => {
      if (!canI('list', kind, 'any')) {
        return
      }
      try {
        const data = await api.get(kind)
        if (STATS_KINDS.includes(kind)) {
          await Promise.all(
            data.map(async (item) => {
              try {
                item.stats = await api.getResourceDetails(
                  kind,
                  item.metadata.namespace,
                  item.metadata.name,
                  'stats'
                )
              } catch {
                item.stats = {}
              }
            })
          )
        }
        if (activeRef.current) {
          setCaches((previous) => ({ ...previous, [kind]: data }))
        }
      } catch {
        // kept as-is: a failed refresh preserves the previous data
      }
    },
    [canI]
  )

  const refresh = useCallback(async () => {
    await Promise.all(LIST_KINDS.map(fetchKind))
    if (activeRef.current) {
      setReady(true)
    }
  }, [fetchKind])

  useEffect(() => {
    activeRef.current = true
    if (!isLoggedIn || !permissionsLoaded) {
      setCaches(initialCaches())
      setReady(false)
      return
    }
    refresh()
    const interval = setInterval(refresh, POLL_INTERVAL_MS)
    return () => {
      activeRef.current = false
      clearInterval(interval)
    }
  }, [isLoggedIn, permissionsLoaded, refresh])

  const value = { ...caches, ready, refresh }

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>
}

export function useData() {
  const context = useContext(DataContext)
  if (!context) {
    throw new Error('useData must be used within a DataProvider')
  }
  return context
}

// Items of one kind, optionally narrowed to a namespace. Returns null while
// the kind has not been loaded yet.
export function useKindList(kind, namespace = '') {
  const data = useData()
  const items = data[kind]
  if (items === null || items === undefined) {
    return null
  }
  return namespace ? items.filter((item) => item.metadata.namespace === namespace) : items
}

export function useResource(kind, namespace, name) {
  const items = useKindList(kind, namespace)
  if (items === null) {
    return null
  }
  return items.find((item) => item.metadata.name === name) || undefined
}
