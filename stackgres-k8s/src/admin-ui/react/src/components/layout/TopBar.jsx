import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { Clock, LogOut, Moon, RotateCw, Sun, User } from 'lucide-react'
import { useAuth } from '../../contexts/AuthContext'
import { useData } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { useSettings } from '../../contexts/SettingsContext'
import { useTheme } from '../../contexts/ThemeContext'
import { userManagementPermissions } from '../../lib/permissions'
import NamespaceSelector from './NamespaceSelector'

const ICON_BUTTON =
  'p-2 rounded-md hover:bg-[hsl(var(--muted))] text-[hsl(var(--muted-foreground))] transition-colors'

export default function TopBar() {
  const { logout } = useAuth()
  const canI = useCanI()
  const { refresh } = useData()
  const { theme, toggleTheme } = useTheme()
  const { timezone, toggleTimezone } = useSettings()
  const [version, setVersion] = useState(null)

  useEffect(() => {
    fetch('/admin/info/sg-info.json')
      .then((response) => (response.ok ? response.json() : null))
      .then((info) => setVersion(info && info.version))
      .catch(() => {})
  }, [])

  const onToggleTimezone = () => {
    toggleTimezone()
    const newZone =
      timezone === 'local' ? 'UTC' : Intl.DateTimeFormat().resolvedOptions().timeZone
    toast.message(`Timestamps are now displayed in ${newZone}`)
  }

  return (
    <header className="h-14 border-b bg-[hsl(var(--surface-nav))] text-[hsl(var(--surface-nav-foreground))] flex items-center px-4 shrink-0 fixed top-0 left-0 right-0 z-50">
      <Link to="/" className="flex items-center gap-2.5">
        {/* The Vue console's StackGres mark, ported verbatim from NavBar.vue */}
        <svg xmlns="http://www.w3.org/2000/svg" width="29.997" height="25.348" aria-hidden="true"><path d="M0 14.125l6.78 7.769h16.438l6.779-7.769-6.779-7.769H6.78z" fill="#42a8c8"/><path fill="#426d88" d="M6.78 21.894h16.443v3.455H6.78z"/><path d="M6.78 25.348L0 17.574v-3.45l6.78 7.77z" fill="#428bb4"/><path d="M23.218 25.348l6.779-7.769v-3.454l-6.779 7.769z" fill="#16657c"/><g><path d="M28.213 12.882c0-2.882-5.92-5.219-13.215-5.219s-13.21 2.336-13.21 5.219 5.915 5.219 13.21 5.219 13.215-2.337 13.215-5.219z" fill="#39b54a"/><path d="M28.213 12.882c0 2.882-5.92 5.219-13.215 5.219s-13.21-2.336-13.21-5.219v2.873c.91 2.533 6.525 5.219 13.21 5.219s12.3-2.687 13.215-5.219v-2.873z" fill="#009245"/></g><g><path d="M.678 8.302l14.323 8.3 14.323-8.3-14.323-8.3z" fill="#f2c63f"/><path d="M.678 8.302v3.235l14.323 8.3v-3.235z" fill="#f2b136"/><path d="M29.324 8.302L15 16.602v3.235l14.324-8.3z" fill="#f2a130"/></g><g><path d="M3.846 10.368l22.307-3.242-5.657-6.5z" fill="#ff7124"/><path d="M3.846 10.368l22.307-3.242v2.928L3.846 13.295z" fill="#d93d1b"/></g></svg>
        <span className="flex items-start gap-1">
          <span className="text-lg leading-none text-[hsl(var(--surface-nav-foreground))]">
            Stack<strong className="font-bold">Gres</strong>
          </span>
          {version && (
            <sup className="hidden sm:inline text-[10px] leading-none tracking-wide text-[hsl(var(--muted-foreground))]" title="StackGres version">
              {version}
            </sup>
          )}
        </span>
      </Link>
      <div className="absolute left-1/2 top-1/2 hidden -translate-x-1/2 -translate-y-1/2 sm:block">
        <NamespaceSelector />
      </div>
      <div className="flex-1" />
      <div className="flex items-center gap-1">
        {userManagementPermissions(canI).get && (
          <Link to="/manage/users" className={ICON_BUTTON} title="User management" aria-label="User management">
            <User className="h-5 w-5" strokeWidth={1.5} />
          </Link>
        )}
        <button type="button" className={ICON_BUTTON} onClick={refresh} title="Reload data" aria-label="Reload data">
          <RotateCw className="h-5 w-5" strokeWidth={1.5} />
        </button>
        <button
          type="button"
          className={ICON_BUTTON}
          onClick={onToggleTimezone}
          title={`Timezone: ${timezone === 'local' ? 'Local' : 'UTC'}`}
          aria-label="Toggle timezone"
        >
          <Clock className="h-5 w-5" strokeWidth={1.5} />
        </button>
        <button
          type="button"
          className={ICON_BUTTON}
          onClick={toggleTheme}
          title={`Theme: ${theme}`}
          aria-label="Toggle theme"
        >
          {theme === 'dark' ? (
            <Sun className="h-5 w-5" strokeWidth={1.5} />
          ) : (
            <Moon className="h-5 w-5" strokeWidth={1.5} />
          )}
        </button>
        <button type="button" className={ICON_BUTTON} onClick={logout} title="Logout" aria-label="Logout">
          <LogOut className="h-5 w-5" strokeWidth={1.5} />
        </button>
      </div>
    </header>
  )
}
