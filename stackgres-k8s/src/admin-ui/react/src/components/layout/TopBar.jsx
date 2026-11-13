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
