import { Link } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { userManagementPermissions } from '../../lib/permissions'
import { useFlatList } from '../../hooks/useFlatList'
import DeleteResourceDialog from '../DeleteResourceDialog'
import { HelpTooltip } from '../HelpTooltip'

const RULE_COLUMNS = ['apiGroups', 'nonResourceURLs', 'resourceNames', 'resources', 'verbs']
const joinRule = (values) => (values && values.length ? values.join(', ') : '')

// One row per rule, with the identity and action cells spanning all the
// role's rules (the Vue roles tables).
function RoleRows({ role, kind, detailPath, canPatch, canDelete, onDeleted, withNamespace }) {
  const rules = role.rules && role.rules.length ? role.rules : [null]
  return rules.map((rule, index) => (
    <tr key={`${role.metadata.name}-${index}`} className="border-t">
      {index === 0 && (
        <td className="px-4 py-2" rowSpan={rules.length}>
          <Link to={detailPath} className="text-[hsl(var(--primary))] hover:underline">
            {role.metadata.name}
          </Link>
        </td>
      )}
      {index === 0 && withNamespace && (
        <td className="px-4 py-2" rowSpan={rules.length}>
          {role.metadata.namespace}
        </td>
      )}
      {rule ? (
        RULE_COLUMNS.map((column) => (
          <td key={column} className="px-4 py-2">
            {joinRule(rule[column])}
          </td>
        ))
      ) : (
        <td className="px-4 py-2" colSpan={RULE_COLUMNS.length} />
      )}
      {index === 0 && (
        <td className="px-4 py-2" rowSpan={rules.length}>
          <span className="flex gap-2">
            {canPatch && (
              <Link to={`${detailPath}/edit`} aria-label={`Edit ${role.metadata.name}`} className="p-1 rounded text-[hsl(var(--primary))] hover:bg-[hsl(var(--primary)/0.1)]">
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
    <section className="flex flex-col gap-6 px-6 pb-6 text-sm">
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-sm font-semibold">Roles</h2>
          {canI('create', 'roles', 'any') && (
            <Link to="/manage/roles/new" className="text-[hsl(var(--primary))] hover:underline">
              Add New Role
            </Link>
          )}
        </div>
        {canI('list', 'roles', 'all') ? (
          <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Name <HelpTooltip field="sgrole.metadata.name" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Namespace <HelpTooltip field="sgrole.metadata.namespace" />
                  </span>
                </th>
                {roleHeaders.map((header) => (
                  <th key={header} className="px-4 py-2">
                    <span className="inline-flex items-center gap-1 whitespace-nowrap">
                      {header} <HelpTooltip field={'sgrole.rules.' + header} />
                    </span>
                  </th>
                ))}
                <th className="px-4 py-2" />
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
          </div>
        ) : (
          <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">You don&apos;t have enough permissions to list roles</p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-sm font-semibold">Cluster Roles</h2>
          {canI('create', 'clusterroles') && (
            <Link to="/manage/clusterroles/new" className="text-[hsl(var(--primary))] hover:underline">
              Add New Cluster Role
            </Link>
          )}
        </div>
        {canI('list', 'clusterroles') ? (
          <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Name <HelpTooltip field="sgclusterrole.metadata.name" />
                  </span>
                </th>
                {roleHeaders.map((header) => (
                  <th key={header} className="px-4 py-2">
                    <span className="inline-flex items-center gap-1 whitespace-nowrap">
                      {header} <HelpTooltip field={'sgclusterrole.rules.' + header} />
                    </span>
                  </th>
                ))}
                <th className="px-4 py-2" />
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
          </div>
        ) : (
          <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">You don&apos;t have enough permissions to list cluster roles</p>
        )}
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-4">
          <h2 className="text-sm font-semibold">Users</h2>
          {userPermissions.create && (
            <Link to="/manage/users/new" className="text-[hsl(var(--primary))] hover:underline">
              Add New User
            </Link>
          )}
        </div>
        {userPermissions.get ? (
          <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Name <HelpTooltip field="sguser.metadata.name" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    K8s Username <HelpTooltip field="sguser.k8sUsername" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    API Username <HelpTooltip field="sguser.apiUsername" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Roles <HelpTooltip field="sguser.roles" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Cluster Roles <HelpTooltip field="sguser.clusterRoles" />
                  </span>
                </th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody>
              {(users.items || []).map((user) => (
                <tr key={user.metadata.name} className="border-t">
                  <td className="px-4 py-2">
                    <Link to={`/manage/user/${user.metadata.name}`} className="text-[hsl(var(--primary))] hover:underline">
                      {user.metadata.name}
                    </Link>
                  </td>
                  <td className="px-4 py-2">{user.k8sUsername}</td>
                  <td className="px-4 py-2">{user.apiUsername || ''}</td>
                  <td className="px-4 py-2">{(user.roles || []).map((role) => role.name).join(', ')}</td>
                  <td className="px-4 py-2">
                    {(user.clusterRoles || []).map((role) => role.name).join(', ')}
                  </td>
                  <td className="px-4 py-2">
                    <span className="flex gap-2">
                      {userPermissions.patch && (
                        <Link to={`/manage/user/${user.metadata.name}/edit`} aria-label={`Edit ${user.metadata.name}`} className="p-1 rounded text-[hsl(var(--primary))] hover:bg-[hsl(var(--primary)/0.1)]">
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
          </div>
        ) : (
          <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">You don&apos;t have enough permissions to list users</p>
        )}
      </div>
    </section>
  )
}
