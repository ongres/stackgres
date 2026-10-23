import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, onForbidden } from '../lib/api'
import { canI as checkPermission } from '../lib/permissions'
import { useAuth } from './AuthContext'

const PermissionsContext = createContext()

const EMPTY = { namespaced: [], unnamespaced: {} }

export function PermissionsProvider({ children }) {
  const { isLoggedIn } = useAuth()
  const [allowed, setAllowed] = useState(EMPTY)
  const [forbidden, setForbidden] = useState([])
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    if (!isLoggedIn) {
      setAllowed(EMPTY)
      setForbidden([])
      setLoaded(false)
      return
    }
    let cancelled = false
    api
      .get('can_i')
      .then((data) => {
        if (!cancelled) {
          setAllowed(data)
          setLoaded(true)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setLoaded(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [isLoggedIn])

  useEffect(() => {
    onForbidden((kind) =>
      setForbidden((previous) => (previous.includes(kind) ? previous : [...previous, kind]))
    )
    return () => onForbidden(null)
  }, [])

  const canI = useCallback(
    (action, kind, namespace = '') => checkPermission(allowed, action, kind, namespace),
    [allowed]
  )

  const value = { allowed, forbidden, loaded, canI }

  return <PermissionsContext.Provider value={value}>{children}</PermissionsContext.Provider>
}

export function usePermissions() {
  const context = useContext(PermissionsContext)
  if (!context) {
    throw new Error('usePermissions must be used within a PermissionsProvider')
  }
  return context
}

export function useCanI() {
  return usePermissions().canI
}
