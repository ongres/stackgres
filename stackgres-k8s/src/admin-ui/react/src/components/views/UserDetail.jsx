import { Link, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { userManagementPermissions } from '../../lib/permissions'
import { useFlatList } from '../../hooks/useFlatList'
import SpecTree from '../SpecTree'

export default function UserDetail() {
  const { name } = useParams()
  const canI = useCanI()
  const permissions = userManagementPermissions(canI)
  const { items } = useFlatList('users', permissions.get)

  if (items === null) {
    return <p className="p-8 text-sm">Loading data…</p>
  }
  const user = items.find((candidate) => candidate.metadata.name === name)
  if (!user) {
    return <p className="p-8 text-sm">User &quot;{name}&quot; not found.</p>
  }

  return (
    <section className="flex flex-col gap-4 p-8 text-sm">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">User: {name}</h1>
        {permissions.patch && (
          <Link to={`/manage/user/${name}/edit`} className="flex items-center gap-1 underline">
            <Pencil size={14} /> Edit
          </Link>
        )}
      </header>
      <SpecTree value={user} />
    </section>
  )
}
