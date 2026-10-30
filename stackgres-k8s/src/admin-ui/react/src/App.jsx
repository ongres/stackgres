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
import ClusterMonitoring from './components/views/ClusterMonitoring'
import NamespaceOverview from './components/views/NamespaceOverview'
import PendingView from './components/views/PendingView'
import ResourceDetail from './components/views/ResourceDetail'
import ResourceList from './components/views/ResourceList'
import SGConfigDetail from './components/views/SGConfigDetail'
import ShardedClusterConfig from './components/views/ShardedClusterConfig'
import ShardedClusterDetail from './components/views/ShardedClusterDetail'
import ShardedClusterMonitoring from './components/views/ShardedClusterMonitoring'
import ShardedClusterStatus from './components/views/ShardedClusterStatus'
import RoleDetail from './components/views/RoleDetail'
import UserDetail from './components/views/UserDetail'
import UserManagement from './components/views/UserManagement'
import BackupForm from './components/forms/BackupForm'
import DbOpsForm from './components/forms/DbOpsForm'
import DistributedLogsForm from './components/forms/DistributedLogsForm'
import InstanceProfileForm from './components/forms/InstanceProfileForm'
import NamespaceForm from './components/forms/NamespaceForm'
import ObjectStorageForm from './components/forms/ObjectStorageForm'
import PgConfigForm from './components/forms/PgConfigForm'
import PoolConfigForm from './components/forms/PoolConfigForm'
import RoleForm from './components/forms/RoleForm'
import ScriptForm from './components/forms/ScriptForm'
import SGClusterForm from './components/forms/SGClusterForm'
import SGConfigForm from './components/forms/SGConfigForm'
import SGShardedClusterForm from './components/forms/SGShardedClusterForm'
import StreamForm from './components/forms/StreamForm'
import UserForm from './components/forms/UserForm'
import { KINDS } from './lib/kinds'

// Simple CRDs: one list route (plural) and one detail route (singular),
// sharing the generic components. Clusters get their own tabbed detail.
const SIMPLE_KINDS = Object.keys(KINDS).filter(
  (plural) => !['sgclusters', 'sgshardedclusters'].includes(plural)
)

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
                    <Route path="sgbackups/new" element={<BackupForm />} />
                    <Route path="sgbackup/:backupname" element={<ResourceDetail plural="sgbackups" />} />
                    <Route path="sgbackup/:backupname/edit" element={<BackupForm isEdit />} />
                    <Route path="logs" element={<ClusterLogs />} />
                    <Route path="events" element={<ClusterEvents />} />
                    <Route path="event/:uid" element={<ClusterEvents />} />
                    <Route path="monitor" element={<ClusterMonitoring />} />
                    <Route path="monitor/:pod" element={<ClusterMonitoring />} />
                    <Route path="monitor/:pod/:dashboard" element={<ClusterMonitoring />} />
                    <Route path="monitor/:pod/:dashboard/:range" element={<ClusterMonitoring />} />
                  </Route>

                  <Route path=":namespace/sgshardedclusters" element={<ResourceList plural="sgshardedclusters" />} />
                  <Route path=":namespace/sgshardedcluster/:name" element={<ShardedClusterDetail />}>
                    <Route index element={<ShardedClusterStatus />} />
                    <Route path="config" element={<ShardedClusterConfig />} />
                    <Route path="monitor" element={<ShardedClusterMonitoring />} />
                    <Route path="monitor/:pod" element={<ShardedClusterMonitoring />} />
                    <Route path="monitor/:pod/:dashboard" element={<ShardedClusterMonitoring />} />
                    <Route path="monitor/:pod/:dashboard/:range" element={<ShardedClusterMonitoring />} />
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

                  <Route path=":namespace/sgclusters/new" element={<SGClusterForm />} />
                  <Route path=":namespace/sgcluster/:name/edit" element={<SGClusterForm isEdit />} />
                  <Route path=":namespace/sgshardedclusters/new" element={<SGShardedClusterForm />} />
                  <Route path=":namespace/sgshardedcluster/:name/edit" element={<SGShardedClusterForm isEdit />} />
                  <Route path=":namespace/sginstanceprofiles/new" element={<InstanceProfileForm />} />
                  <Route path=":namespace/sginstanceprofile/:name/edit" element={<InstanceProfileForm isEdit />} />
                  <Route path=":namespace/sgpgconfigs/new" element={<PgConfigForm />} />
                  <Route path=":namespace/sgpgconfig/:name/edit" element={<PgConfigForm isEdit />} />
                  <Route path=":namespace/sgpoolconfigs/new" element={<PoolConfigForm />} />
                  <Route path=":namespace/sgpoolconfig/:name/edit" element={<PoolConfigForm isEdit />} />
                  <Route path=":namespace/sgobjectstorages/new" element={<ObjectStorageForm />} />
                  <Route path=":namespace/sgobjectstorage/:name/edit" element={<ObjectStorageForm isEdit />} />
                  <Route path=":namespace/sgscripts/new" element={<ScriptForm />} />
                  <Route path=":namespace/sgscript/:name/edit" element={<ScriptForm isEdit />} />
                  <Route path=":namespace/sgbackups/new" element={<BackupForm />} />
                  <Route path=":namespace/sgbackup/:backupname/edit" element={<BackupForm isEdit />} />
                  <Route path=":namespace/sgdbops/new" element={<DbOpsForm />} />
                  <Route path=":namespace/sgdistributedlogs/new" element={<DistributedLogsForm />} />
                  <Route path=":namespace/sgdistributedlog/:name/edit" element={<DistributedLogsForm isEdit />} />
                  <Route path=":namespace/sgstreams/new" element={<StreamForm />} />
                  <Route path=":namespace/sgstream/:name/edit" element={<StreamForm isEdit />} />

                  <Route path="namespaces/new" element={<NamespaceForm />} />
                  <Route path=":namespace/application/:name/*" element={<PendingView title="Application" />} />
                  <Route path="sgconfig/:name" element={<SGConfigDetail />} />
                  <Route path="sgconfig/:name/edit" element={<SGConfigForm />} />
                  <Route path="manage/users" element={<UserManagement />} />
                  <Route path="manage/users/new" element={<UserForm />} />
                  <Route path="manage/user/:name" element={<UserDetail />} />
                  <Route path="manage/user/:name/edit" element={<UserForm isEdit />} />
                  <Route path="manage/roles/new" element={<RoleForm kind="roles" />} />
                  <Route path="manage/role/:name" element={<RoleDetail kind="roles" />} />
                  <Route path="manage/role/:name/edit" element={<RoleForm kind="roles" isEdit />} />
                  <Route path="manage/clusterroles/new" element={<RoleForm kind="clusterroles" />} />
                  <Route path="manage/clusterrole/:name" element={<RoleDetail kind="clusterroles" />} />
                  <Route path="manage/clusterrole/:name/edit" element={<RoleForm kind="clusterroles" isEdit />} />
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
