import { Outlet, useParams } from 'react-router-dom'
import { Toaster } from 'sonner'
import { useAuth } from '../../contexts/AuthContext'
import { useTheme } from '../../contexts/ThemeContext'
import CommandPalette from '../CommandPalette'
import Login from '../Login'
import Breadcrumbs from './Breadcrumbs'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

export default function Shell() {
  const { isLoggedIn } = useAuth()
  const { theme } = useTheme()
  const { namespace } = useParams()

  return (
    <>
      <Toaster theme={theme} position="top-right" richColors />
      {isLoggedIn ? (
        <div className="min-h-screen flex flex-col">
          <CommandPalette />
          <TopBar />
          <div className="flex flex-1 pt-14">
            {namespace && (
              <div className="relative shrink-0 h-[calc(100vh-3.5rem)] sticky top-14 z-20 hidden md:block">
                <Sidebar />
              </div>
            )}
            <main className="flex-1 min-w-0 overflow-x-hidden" aria-label="Page content">
              <Breadcrumbs />
              <Outlet />
            </main>
          </div>
        </div>
      ) : (
        <Login />
      )}
    </>
  )
}
