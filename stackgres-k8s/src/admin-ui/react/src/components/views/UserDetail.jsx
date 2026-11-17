import { Link, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { userManagementPermissions } from '../../lib/permissions'
import { useFlatList } from '../../hooks/useFlatList'
import SpecTree from '../SpecTree'
import { BreadcrumbActions } from '../layout/Breadcrumbs'

export default function UserDetail() {
  const { name } = useParams()
  const canI = useCanI()
  const permissions = userManagementPermissions(canI)
  const { items } = useFlatList('users', permissions.get)

  if (items === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }
  const user = items.find((candidate) => candidate.metadata.name === name)
  if (!user) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">User &quot;{name}&quot; not found.</p>
  }

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">User: {name}</h1>
      <BreadcrumbActions>
        {permissions.patch && (
          <Link to={`/manage/user/${name}/edit`} className="flex items-center gap-1 text-[hsl(var(--primary))] hover:underline">
            <Pencil size={14} /> Edit
          </Link>
        )}
      </BreadcrumbActions>
      <SpecTree value={user} />
    </section>
  )
}
