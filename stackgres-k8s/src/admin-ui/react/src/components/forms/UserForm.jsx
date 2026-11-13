import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from '../../lib/api'
import { useCanI } from '../../contexts/PermissionsContext'
import { useFlatList } from '../../hooks/useFlatList'
import { nameValidationMessage } from '../../lib/names'
import SummaryDialog from './SummaryDialog'
import { Field, FieldGroup, FieldRow, TextInput, Toggle } from './fields'

const joinRules = (role, column) =>
  `[ ${(role.rules || [])
    .flatMap((rule) => rule?.[column] || [])
    .join(', ')} ]`

// Role/cluster-role binding picker: a searchable list of all roles with a
// bind checkbox per row (the Vue CreateUser fieldsets).
function RoleBindings({ legend, roles, bound, onToggle, withNamespace, emptyMessage }) {
  const [search, setSearch] = useState('')
  const visible = (roles || []).filter(
    (role) => !search.length || role.metadata.name.includes(search)
  )
  const isBound = (role) => (bound || []).some((entry) => entry.name === role.metadata.name)

  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}</legend>
      <div className="flex items-end gap-2">
        <Field label={`Search ${legend}`}>
          <TextInput value={search} onChange={setSearch} />
        </Field>
        <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" onClick={() => setSearch('')}>
          Clear
        </button>
      </div>
      {visible.length ? (
        <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">Bind</th>
                <th className="px-4 py-2">Name</th>
                {withNamespace && <th className="px-4 py-2">Namespace</th>}
                <th className="px-4 py-2">API Groups</th>
                <th className="px-4 py-2">Resources</th>
                <th className="px-4 py-2">Verbs</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((role) => (
                <tr key={`${role.metadata.namespace || ''}/${role.metadata.name}`} className="border-t">
                  <td className="px-4 py-2">
                    <input type="checkbox" checked={isBound(role)}
                      aria-label={`Bind ${role.metadata.name}`}
                      onChange={() => onToggle(role)} />
                  </td>
                  <td className="px-4 py-2">{role.metadata.name}</td>
                  {withNamespace && <td className="px-4 py-2">{role.metadata.namespace}</td>}
                  <td className="px-4 py-2">{joinRules(role, 'apiGroups')}</td>
                  <td className="px-4 py-2">{joinRules(role, 'resources')}</td>
                  <td className="px-4 py-2">{joinRules(role, 'verbs')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p>{emptyMessage}</p>
      )}
    </fieldset>
  )
}

export default function UserForm({ isEdit = false }) {
  const { name: routeName } = useParams()
  const navigate = useNavigate()
  const canI = useCanI()
  const { items: users, refresh } = useFlatList('users')
  const { items: roles } = useFlatList('roles', canI('list', 'roles', 'any'))
  const { items: clusterRoles } = useFlatList('clusterroles', canI('list', 'clusterroles'))

  const [user, setUser] = useState(
    isEdit
      ? null
      : {
          metadata: { namespace: '', name: '' },
          k8sUsername: null,
          password: null,
          apiUsername: null,
          apiUsernameNotBlank: false,
          roles: null,
          clusterRoles: null,
        }
  )
  const [preview, setPreview] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  if (isEdit && user === null && users) {
    const existing = users.find((candidate) => candidate.metadata.name === routeName)
    if (existing) {
      setUser(JSON.parse(JSON.stringify(existing)))
    }
  }

  if (user === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  const patch = (updater) =>
    setUser((current) => {
      const next = JSON.parse(JSON.stringify(current))
      updater(next)
      return next
    })

  const nameCollision =
    !isEdit && (users || []).some((candidate) => candidate.metadata.name === user.metadata.name)

  // Vue toggleRoleBinding: bind {name, namespace?}; unbinding back to an
  // empty list resets the array to null.
  const toggleBinding = (kind) => (role) =>
    patch((next) => {
      const binding = {
        name: role.metadata.name,
        ...(Object.hasOwn(role.metadata, 'namespace') &&
          role.metadata.namespace && { namespace: role.metadata.namespace }),
      }
      const current = next[kind] || []
      if (current.some((entry) => entry.name === binding.name)) {
        const remaining = current.filter((entry) => entry.name !== binding.name)
        next[kind] = remaining.length ? remaining : null
      } else {
        next[kind] = [...current, binding]
      }
    })

  const submit = async (event) => {
    event.preventDefault()
    const nameError = nameValidationMessage(user.metadata.name)
    if (nameError) {
      toast.error(nameError)
      return
    }
    if (nameCollision) {
      toast.error(`A user named "${user.metadata.name}" already exists`)
      return
    }
    setSubmitting(true)
    try {
      if (isEdit) {
        await api.update('users', user)
        toast.success(`User "${user.metadata.name}" updated successfully`)
        refresh()
        navigate(`/manage/user/${user.metadata.name}`)
      } else {
        await api.create('users', user)
        toast.success(`User "${user.metadata.name}" created successfully`)
        refresh()
        navigate('/manage/users')
      }
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="flex max-w-4xl flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">{isEdit ? `Edit User: ${routeName}` : 'Create User'}</h1>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <FieldGroup>
          <FieldRow>
            <Field label="User Name" required tooltip="sgrole.metadata.name">
              <TextInput required disabled={isEdit} value={user.metadata.name}
                onChange={(value) => patch((next) => { next.metadata.name = value })} />
            </Field>
          </FieldRow>
          {nameCollision && (
            <p role="alert" className="text-sm text-red-500">
              There&apos;s already a <strong>User</strong> with the same name. Please specify a
              different name
            </p>
          )}
        </FieldGroup>
        <FieldGroup title="User Information">
          <FieldRow>
            <Field label="K8s Username" required tooltip="sguser.k8sUsername">
              <TextInput required value={user.k8sUsername || ''}
                onChange={(value) => patch((next) => { next.k8sUsername = value || null })} />
            </Field>
            {!isEdit && (
              <Field label="Password" required tooltip="sguser.password">
                <TextInput type="password" required value={user.password || ''}
                  onChange={(value) => patch((next) => { next.password = value || null })} />
              </Field>
            )}
          </FieldRow>
          <FieldRow>
            <Toggle label="Mandatory API Username" tooltip="sguser.apiUsernameNotBlank"
              checked={user.apiUsernameNotBlank || false}
              onChange={(value) => patch((next) => { next.apiUsernameNotBlank = value })} />
            <Field label="API Username" required={user.apiUsernameNotBlank} tooltip="sguser.apiUsername">
              <TextInput required={user.apiUsernameNotBlank} value={user.apiUsername || ''}
                onChange={(value) => patch((next) => { next.apiUsername = value || null })} />
            </Field>
          </FieldRow>
        </FieldGroup>
        <FieldGroup title="Role Binding">
          <RoleBindings legend="Roles" roles={roles} bound={user.roles} withNamespace
            onToggle={toggleBinding('roles')}
            emptyMessage="No roles are available on this installation" />
          <RoleBindings legend="Cluster Roles" roles={clusterRoles} bound={user.clusterRoles}
            onToggle={toggleBinding('clusterRoles')}
            emptyMessage="No cluster roles are available on this installation" />
        </FieldGroup>
        <div className="flex items-center gap-3">
          <button type="submit" disabled={submitting} className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]">
            {isEdit ? 'Update User' : 'Create User'}
          </button>
          <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" onClick={() => setPreview(user)}>
            View Summary
          </button>
          <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" onClick={() => navigate('/manage/users')}>
            Cancel
          </button>
        </div>
      </form>
      {preview && (
        <SummaryDialog value={preview} onClose={() => setPreview(false)} />
      )}
    </section>
  )
}
