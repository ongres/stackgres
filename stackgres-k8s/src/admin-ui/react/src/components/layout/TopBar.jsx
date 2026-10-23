import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { Clock, LogOut, Moon, Sun } from 'lucide-react'
import { useAuth } from '../../contexts/AuthContext'
import { useSettings } from '../../contexts/SettingsContext'
import { useTheme } from '../../contexts/ThemeContext'

export default function TopBar() {
  const { logout } = useAuth()
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
    <header className="flex items-center justify-between gap-4 border-b p-4">
      <Link to="/" className="flex items-baseline gap-2 font-semibold">
        StackGres
        {version && <span className="text-xs font-normal">{version}</span>}
      </Link>
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={onToggleTimezone}
          title={`Timezone: ${timezone === 'local' ? 'Local' : 'UTC'}`}
          aria-label="Toggle timezone"
        >
          <Clock size={18} />
        </button>
        <button
          type="button"
          onClick={toggleTheme}
          title={`Theme: ${theme}`}
          aria-label="Toggle theme"
        >
          {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
        </button>
        <button type="button" onClick={logout} title="Logout" aria-label="Logout">
          <LogOut size={18} />
        </button>
      </div>
    </header>
  )
}
