import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from '../../lib/api'
import { useCanI } from '../../contexts/PermissionsContext'
import { useFlatList } from '../../hooks/useFlatList'
import { nameValidationMessage } from '../../lib/names'
import SpecTree from '../SpecTree'
import { Field, TextInput, Toggle } from './fields'

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
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{legend}</legend>
      <div className="flex items-end gap-2">
        <Field label={`Search ${legend}`}>
          <TextInput value={search} onChange={setSearch} />
        </Field>
        <button type="button" className="border p-2" onClick={() => setSearch('')}>
          Clear
        </button>
      </div>
      {visible.length ? (
        <table className="w-full border-collapse text-left">
          <thead>
            <tr>
              <th className="border-b p-2">Bind</th>
              <th className="border-b p-2">Name</th>
              {withNamespace && <th className="border-b p-2">Namespace</th>}
              <th className="border-b p-2">API Groups</th>
              <th className="border-b p-2">Resources</th>
              <th className="border-b p-2">Verbs</th>
            </tr>
          </thead>
          <tbody>
            {visible.map((role) => (
              <tr key={`${role.metadata.namespace || ''}/${role.metadata.name}`}>
                <td className="border-b p-2">
                  <input type="checkbox" checked={isBound(role)}
                    aria-label={`Bind ${role.metadata.name}`}
                    onChange={() => onToggle(role)} />
                </td>
                <td className="border-b p-2">{role.metadata.name}</td>
                {withNamespace && <td className="border-b p-2">{role.metadata.namespace}</td>}
                <td className="border-b p-2">{joinRules(role, 'apiGroups')}</td>
                <td className="border-b p-2">{joinRules(role, 'resources')}</td>
                <td className="border-b p-2">{joinRules(role, 'verbs')}</td>
              </tr>
            ))}
          </tbody>
        </table>
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
    return <p className="p-8 text-sm">Loading data…</p>
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
    <section className="flex max-w-4xl flex-col gap-4 p-8 text-sm">
      <h1 className="text-2xl font-semibold">{isEdit ? `Edit User: ${routeName}` : 'Create User'}</h1>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <Field label="User Name" required tooltip="sgrole.metadata.name">
          <TextInput required disabled={isEdit} value={user.metadata.name}
            onChange={(value) => patch((next) => { next.metadata.name = value })} />
        </Field>
        {nameCollision && (
          <p role="alert">
            There&apos;s already a <strong>User</strong> with the same name. Please specify a
            different name
          </p>
        )}
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
        <Toggle label="Mandatory API Username" tooltip="sguser.apiUsernameNotBlank"
          checked={user.apiUsernameNotBlank || false}
          onChange={(value) => patch((next) => { next.apiUsernameNotBlank = value })} />
        <Field label="API Username" required={user.apiUsernameNotBlank} tooltip="sguser.apiUsername">
          <TextInput required={user.apiUsernameNotBlank} value={user.apiUsername || ''}
            onChange={(value) => patch((next) => { next.apiUsername = value || null })} />
        </Field>
        <RoleBindings legend="Roles" roles={roles} bound={user.roles} withNamespace
          onToggle={toggleBinding('roles')}
          emptyMessage="No roles are available on this installation" />
        <RoleBindings legend="Cluster Roles" roles={clusterRoles} bound={user.clusterRoles}
          onToggle={toggleBinding('clusterRoles')}
          emptyMessage="No cluster roles are available on this installation" />
        <div className="flex items-center gap-3">
          <button type="submit" disabled={submitting} className="border p-2 font-semibold">
            {isEdit ? 'Update User' : 'Create User'}
          </button>
          <button type="button" className="border p-2" onClick={() => setPreview(user)}>
            View Summary
          </button>
          <button type="button" className="border p-2" onClick={() => navigate('/manage/users')}>
            Cancel
          </button>
        </div>
      </form>
      {preview && (
        <section className="border p-4">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold uppercase">Summary</h2>
            <button type="button" onClick={() => setPreview(false)} className="underline">
              Close
            </button>
          </div>
          <SpecTree value={preview} />
        </section>
      )}
    </section>
  )
}
