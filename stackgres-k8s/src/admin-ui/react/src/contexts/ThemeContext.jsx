import { createContext, useContext, useEffect, useState } from 'react'
import { getCookie, setCookie } from '../lib/cookies'

const ThemeContext = createContext()

// The preference is shared with the Vue console through the same cookie, so
// the theme survives switching between the two apps.
const THEME_COOKIE = 'sgTheme'
const FAR_FUTURE = 'Fri, 31 Dec 9999 23:59:59 GMT'

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(() => (getCookie(THEME_COOKIE) === 'dark' ? 'dark' : 'light'))

  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(theme)
  }, [theme])

  const setTheme = (newTheme) => {
    setCookie(THEME_COOKIE, newTheme, { expires: FAR_FUTURE })
    setThemeState(newTheme)
  }

  const toggleTheme = () => setTheme(theme === 'dark' ? 'light' : 'dark')

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider')
  }
  return context
}
