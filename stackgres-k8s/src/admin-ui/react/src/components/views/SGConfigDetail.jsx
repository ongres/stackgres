import { Link, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { useSGConfigs } from '../../hooks/useSGConfigs'
import SpecTree from '../SpecTree'

// Read-only operator configuration view, ported from the Vue SGConfig.vue:
// the full CRD rendered as a summary tree plus an RBAC-gated EDIT link.
export default function SGConfigDetail() {
  const { name } = useParams()
  const canI = useCanI()
  const { sgconfigs } = useSGConfigs()

  if (sgconfigs === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  const config = sgconfigs.find((candidate) => candidate.metadata.name === name)
  if (!config) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Operator configuration &quot;{name}&quot; not found.</p>
  }

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <header className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">Operator Configuration: {name}</h1>
        {canI('patch', 'sgconfigs') && (
          <Link to={`/sgconfig/${name}/edit`} className="flex items-center gap-1 text-[hsl(var(--primary))] hover:underline">
            <Pencil size={14} /> Edit
          </Link>
        )}
      </header>
      <SpecTree value={{ metadata: config.metadata, spec: config.spec }} />
    </section>
  )
}
