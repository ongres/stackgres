import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './contexts/AuthContext'
import { DataProvider } from './contexts/DataContext'
import { PermissionsProvider } from './contexts/PermissionsContext'
import { SettingsProvider } from './contexts/SettingsContext'
import { ThemeProvider } from './contexts/ThemeContext'
import Shell from './components/layout/Shell'
import ClusterConfig from './components/views/ClusterConfig'
import ClusterDetail from './components/views/ClusterDetail'
import ClusterEvents from './components/views/ClusterEvents'
import ClusterLogs from './components/views/ClusterLogs'
import ClusterStatus from './components/views/ClusterStatus'
import GlobalDashboard from './components/views/GlobalDashboard'
import NamespaceOverview from './components/views/NamespaceOverview'
import PendingView from './components/views/PendingView'
import ResourceDetail from './components/views/ResourceDetail'
import ResourceList from './components/views/ResourceList'
import { KINDS } from './lib/kinds'

// Simple CRDs: one list route (plural) and one detail route (singular),
// sharing the generic components. Clusters get their own tabbed detail.
const SIMPLE_KINDS = Object.keys(KINDS).filter((plural) => plural !== 'sgclusters')

export default function App() {
  return (
    <ThemeProvider>
      <SettingsProvider>
        <AuthProvider>
          <PermissionsProvider>
            <DataProvider>
              <Routes>
                <Route element={<Shell />}>
                  <Route index element={<GlobalDashboard />} />
                  <Route path="index.html" element={<Navigate to="/" replace />} />
                  <Route path=":namespace" element={<NamespaceOverview />} />

                  <Route path=":namespace/sgclusters" element={<ResourceList plural="sgclusters" />} />
                  <Route path=":namespace/sgcluster/:name" element={<ClusterDetail />}>
                    <Route index element={<ClusterStatus />} />
                    <Route path="config" element={<ClusterConfig />} />
                    <Route path="sgbackups" element={<ResourceList plural="sgbackups" clusterScoped />} />
                    <Route path="logs" element={<ClusterLogs />} />
                    <Route path="events" element={<ClusterEvents />} />
                    <Route path="event/:uid" element={<ClusterEvents />} />
                    <Route path="monitor/*" element={<PendingView title="Monitoring" />} />
                  </Route>

                  {SIMPLE_KINDS.map((plural) => (
                    <Route key={plural} path={`:namespace/${plural}`} element={<ResourceList plural={plural} />} />
                  ))}
                  {SIMPLE_KINDS.map((plural) => (
                    <Route
                      key={`${plural}-detail`}
                      path={`:namespace/${KINDS[plural].singular}/:name`}
                      element={<ResourceDetail plural={plural} />}
                    />
                  ))}

                  <Route path=":namespace/application/:name/*" element={<PendingView title="Application" />} />
                  <Route path="sgconfig/:name" element={<PendingView title="Operator Configuration" />} />
                  <Route path="manage/*" element={<PendingView title="User Management" />} />
                  <Route path="*" element={<PendingView title="Not Found" />} />
                </Route>
              </Routes>
            </DataProvider>
          </PermissionsProvider>
        </AuthProvider>
      </SettingsProvider>
    </ThemeProvider>
  )
}
