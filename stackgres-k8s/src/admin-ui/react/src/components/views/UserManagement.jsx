import { Link } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { userManagementPermissions } from '../../lib/permissions'
import { useFlatList } from '../../hooks/useFlatList'
import DeleteResourceDialog from '../DeleteResourceDialog'

const RULE_COLUMNS = ['apiGroups', 'nonResourceURLs', 'resourceNames', 'resources', 'verbs']
const joinRule = (values) => (values && values.length ? values.join(', ') : '')

// One row per rule, with the identity and action cells spanning all the
// role's rules (the Vue roles tables).
function RoleRows({ role, kind, detailPath, canPatch, canDelete, onDeleted, withNamespace }) {
  const rules = role.rules && role.rules.length ? role.rules : [null]
  return rules.map((rule, index) => (
    <tr key={`${role.metadata.name}-${index}`}>
      {index === 0 && (
        <td className="border-b p-2" rowSpan={rules.length}>
          <Link to={detailPath} className="underline">
            {role.metadata.name}
          </Link>
        </td>
      )}
      {index === 0 && withNamespace && (
        <td className="border-b p-2" rowSpan={rules.length}>
          {role.metadata.namespace}
        </td>
      )}
      {rule ? (
        RULE_COLUMNS.map((column) => (
          <td key={column} className="border-b p-2">
            {joinRule(rule[column])}
          </td>
        ))
      ) : (
        <td className="border-b p-2" colSpan={RULE_COLUMNS.length} />
      )}
      {index === 0 && (
        <td className="border-b p-2" rowSpan={rules.length}>
          <span className="flex gap-2">
            {canPatch && (
              <Link to={`${detailPath}/edit`} aria-label={`Edit ${role.metadata.name}`}>
                <Pencil size={14} />
              </Link>
            )}
            {canDelete && (
              <DeleteResourceDialog
                kind={kind}
                namespace={role.metadata.namespace}
                name={role.metadata.name}
                onDeleted={onDeleted}
              />
            )}
          </span>
        </td>
      )}
    </tr>
  ))
}

export default function UserManagement() {
  const canI = useCanI()
  const userPermissions = userManagementPermissions(canI)
  const roles = useFlatList('roles', canI('list', 'roles', 'all'))
  const clusterRoles = useFlatList('clusterroles', canI('list', 'clusterroles'))
  const users = useFlatList('users', userPermissions.get)

  const roleHeaders = ['apiGroups', 'nonResourceURLs', 'resourceNames', 'resources', 'verbs']

  return (
    <section className="flex flex-col gap-6 p-8 text-sm">
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-xl font-semibold">Roles</h2>
          {canI('create', 'roles', 'any') && (
            <Link to="/manage/roles/new" className="underline">
              Add New Role
            </Link>
          )}
        </div>
        {canI('list', 'roles', 'all') ? (
          <table className="w-full border-collapse text-left">
            <thead>
              <tr>
                <th className="border-b p-2">Name</th>
                <th className="border-b p-2">Namespace</th>
                {roleHeaders.map((header) => (
                  <th key={header} className="border-b p-2">
                    {header}
                  </th>
                ))}
                <th className="border-b p-2" />
              </tr>
            </thead>
            <tbody>
              {(roles.items || []).map((role) => (
                <RoleRows
                  key={`${role.metadata.namespace}/${role.metadata.name}`}
                  role={role}
                  kind="roles"
                  withNamespace
                  detailPath={`/manage/role/${role.metadata.name}`}
                  canPatch={canI('patch', 'roles', role.metadata.namespace)}
                  canDelete={canI('delete', 'roles', role.metadata.namespace)}
                  onDeleted={roles.refresh}
                />
              ))}
            </tbody>
          </table>
        ) : (
          <p role="note">You don&apos;t have enough permissions to list roles</p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-xl font-semibold">Cluster Roles</h2>
          {canI('create', 'clusterroles') && (
            <Link to="/manage/clusterroles/new" className="underline">
              Add New Cluster Role
            </Link>
          )}
        </div>
        {canI('list', 'clusterroles') ? (
          <table className="w-full border-collapse text-left">
            <thead>
              <tr>
                <th className="border-b p-2">Name</th>
                {roleHeaders.map((header) => (
                  <th key={header} className="border-b p-2">
                    {header}
                  </th>
                ))}
                <th className="border-b p-2" />
              </tr>
            </thead>
            <tbody>
              {(clusterRoles.items || []).map((role) => (
                <RoleRows
                  key={role.metadata.name}
                  role={role}
                  kind="clusterroles"
                  detailPath={`/manage/clusterrole/${role.metadata.name}`}
                  canPatch={canI('patch', 'clusterroles')}
                  canDelete={canI('delete', 'clusterroles')}
                  onDeleted={clusterRoles.refresh}
                />
              ))}
            </tbody>
          </table>
        ) : (
          <p role="note">You don&apos;t have enough permissions to list cluster roles</p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-xl font-semibold">Users</h2>
          {userPermissions.create && (
            <Link to="/manage/users/new" className="underline">
              Add New User
            </Link>
          )}
        </div>
        {userPermissions.get ? (
          <table className="w-full border-collapse text-left">
            <thead>
              <tr>
                <th className="border-b p-2">Name</th>
                <th className="border-b p-2">K8s Username</th>
                <th className="border-b p-2">API Username</th>
                <th className="border-b p-2">Roles</th>
                <th className="border-b p-2">Cluster Roles</th>
                <th className="border-b p-2" />
              </tr>
            </thead>
            <tbody>
              {(users.items || []).map((user) => (
                <tr key={user.metadata.name}>
                  <td className="border-b p-2">
                    <Link to={`/manage/user/${user.metadata.name}`} className="underline">
                      {user.metadata.name}
                    </Link>
                  </td>
                  <td className="border-b p-2">{user.k8sUsername}</td>
                  <td className="border-b p-2">{user.apiUsername || ''}</td>
                  <td className="border-b p-2">{(user.roles || []).map((role) => role.name).join(', ')}</td>
                  <td className="border-b p-2">
                    {(user.clusterRoles || []).map((role) => role.name).join(', ')}
                  </td>
                  <td className="border-b p-2">
                    <span className="flex gap-2">
                      {userPermissions.patch && (
                        <Link to={`/manage/user/${user.metadata.name}/edit`} aria-label={`Edit ${user.metadata.name}`}>
                          <Pencil size={14} />
                        </Link>
                      )}
                      {userPermissions.delete && (
                        <DeleteResourceDialog
                          kind="users"
                          namespace={user.metadata.namespace}
                          name={user.metadata.name}
                          onDeleted={users.refresh}
                        />
                      )}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p role="note">You don&apos;t have enough permissions to list users</p>
        )}
      </div>
    </section>
  )
}
