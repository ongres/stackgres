import { createContext, useContext, useState } from 'react'
import { getCookie, setCookie } from '../lib/cookies'

const SettingsContext = createContext()

// Shared with the Vue console through the same cookie.
const TIMEZONE_COOKIE = 'sgTimezone'
const FAR_FUTURE = 'Fri, 31 Dec 9999 23:59:59 GMT'

export function SettingsProvider({ children }) {
  const [timezone, setTimezoneState] = useState(() =>
    getCookie(TIMEZONE_COOKIE) === 'utc' ? 'utc' : 'local'
  )

  const toggleTimezone = () => {
    const newTimezone = timezone === 'local' ? 'utc' : 'local'
    setCookie(TIMEZONE_COOKIE, newTimezone, { expires: FAR_FUTURE })
    setTimezoneState(newTimezone)
  }

  return (
    <SettingsContext.Provider value={{ timezone, toggleTimezone }}>
      {children}
    </SettingsContext.Provider>
  )
}

export function useSettings() {
  const context = useContext(SettingsContext)
  if (!context) {
    throw new Error('useSettings must be used within a SettingsProvider')
  }
  return context
}
