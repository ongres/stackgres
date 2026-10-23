import { Outlet, useParams } from 'react-router-dom'
import { Toaster } from 'sonner'
import { useAuth } from '../../contexts/AuthContext'
import { useTheme } from '../../contexts/ThemeContext'
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
      <Toaster theme={theme} position="top-right" />
      {isLoggedIn ? (
        <div className="flex min-h-screen flex-col">
          <TopBar />
          <div className="flex flex-1">
            {namespace && <Sidebar />}
            <main className="flex-1">
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
