import { Routes, Route } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { PermissionsProvider } from './contexts/PermissionsContext'
import { SettingsProvider } from './contexts/SettingsContext'
import { ThemeProvider } from './contexts/ThemeContext'
import Shell from './components/layout/Shell'
import HomePage from './components/HomePage'
import NamespaceOverview from './components/NamespaceOverview'

export default function App() {
  return (
    <ThemeProvider>
      <SettingsProvider>
        <AuthProvider>
          <PermissionsProvider>
            <Routes>
              <Route element={<Shell />}>
                <Route index element={<HomePage />} />
                <Route path=":namespace" element={<NamespaceOverview />} />
                <Route path=":namespace/:kind" element={<NamespaceOverview />} />
                <Route path=":namespace/:kind/:name/*" element={<NamespaceOverview />} />
              </Route>
            </Routes>
          </PermissionsProvider>
        </AuthProvider>
      </SettingsProvider>
    </ThemeProvider>
  )
}
