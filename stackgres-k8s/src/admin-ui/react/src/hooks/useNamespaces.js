import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import { useAuth } from '../contexts/AuthContext'

export function useNamespaces() {
  const { isLoggedIn } = useAuth()
  const [namespaces, setNamespaces] = useState([])

  useEffect(() => {
    if (!isLoggedIn) {
      setNamespaces([])
      return
    }
    let cancelled = false
    api
      .get('namespaces')
      .then((data) => {
        if (!cancelled) {
          setNamespaces(data)
        }
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [isLoggedIn])

  return namespaces
}
