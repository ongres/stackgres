import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import {
  api,
  getToken,
  setToken,
  clearToken,
  onUnauthorized,
  OIDC_TOKEN,
} from '../lib/api'

const AuthContext = createContext()

export function AuthProvider({ children }) {
  const [authType, setAuthType] = useState(null)
  const [token, setTokenState] = useState(() => getToken())

  useEffect(() => {
    api
      .get('auth_type')
      .then((data) => setAuthType(data.type))
      .catch(() => setAuthType('JWT'))
  }, [])

  const endSession = useCallback(() => {
    clearToken()
    setTokenState('')
  }, [])

  useEffect(() => {
    onUnauthorized(endSession)
    return () => onUnauthorized(null)
  }, [endSession])

  const login = useCallback(async (username, password) => {
    const data = await api.create('login', { username, password })
    setToken(data.access_token)
    setTokenState(data.access_token)
  }, [])

  const loginExternal = useCallback(() => {
    clearToken()
    window.location.replace(
      `${window.location.origin}/stackgres/auth/external?redirectTo=${window.location.href}`
    )
  }, [])

  const logout = useCallback(async () => {
    if (token === OIDC_TOKEN) {
      await api.get('logout').catch(() => {})
    }
    endSession()
  }, [token, endSession])

  const value = {
    authType,
    isLoggedIn: Boolean(token),
    login,
    loginExternal,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
