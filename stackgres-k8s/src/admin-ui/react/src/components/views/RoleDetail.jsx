import { Link, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { useFlatList } from '../../hooks/useFlatList'
import SpecTree from '../SpecTree'
import { BreadcrumbActions } from '../layout/Breadcrumbs'

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
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }
  const role = items.find((candidate) => candidate.metadata.name === name)
  if (!role) {
    return (
      <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">
        {clusterScoped ? 'Cluster role' : 'Role'} &quot;{name}&quot; not found.
      </p>
    )
  }

  const canPatch = clusterScoped
    ? canI('patch', 'clusterroles')
    : canI('patch', 'roles', role.metadata.namespace)
  const editPath = `/manage/${clusterScoped ? 'clusterrole' : 'role'}/${name}/edit`

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">
        {clusterScoped ? 'Cluster Role' : 'Role'}: {name}
      </h1>
      <BreadcrumbActions>
        {canPatch && (
          <Link to={editPath} className="flex items-center gap-1 text-[hsl(var(--primary))] hover:underline">
            <Pencil size={14} /> Edit
          </Link>
        )}
      </BreadcrumbActions>
      <SpecTree value={role} />
    </section>
  )
}
