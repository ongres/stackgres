import { Link, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { useFlatList } from '../../hooks/useFlatList'
import SpecTree from '../SpecTree'

// Detail view shared by roles and cluster roles (the Vue SGRole component).
export default function RoleDetail({ kind }) {
  const { name } = useParams()
  const canI = useCanI()
  const clusterScoped = kind === 'clusterroles'
  const { items } = useFlatList(
    kind,
    clusterScoped ? canI('list', 'clusterroles') : canI('list', 'roles', 'any')
  )

  if (items === null) {
    return <p className="p-8 text-sm">Loading data…</p>
  }
  const role = items.find((candidate) => candidate.metadata.name === name)
  if (!role) {
    return (
      <p className="p-8 text-sm">
        {clusterScoped ? 'Cluster role' : 'Role'} &quot;{name}&quot; not found.
      </p>
    )
  }

  const canPatch = clusterScoped
    ? canI('patch', 'clusterroles')
    : canI('patch', 'roles', role.metadata.namespace)
  const editPath = `/manage/${clusterScoped ? 'clusterrole' : 'role'}/${name}/edit`

  return (
    <section className="flex flex-col gap-4 p-8 text-sm">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">
          {clusterScoped ? 'Cluster Role' : 'Role'}: {name}
        </h1>
        {canPatch && (
          <Link to={editPath} className="flex items-center gap-1 underline">
            <Pencil size={14} /> Edit
          </Link>
        )}
      </header>
      <SpecTree value={role} />
    </section>
  )
}
