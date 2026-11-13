import { useOutletContext } from 'react-router-dom'
import SpecTree from '../SpecTree'

export default function ClusterConfig() {
  const { cluster } = useOutletContext()

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h2 className="text-lg font-semibold">Cluster Details</h2>
      <section>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">Metadata</h3>
        <ul className="flex flex-col gap-1">
          <li>
            <strong>Namespace:</strong> {cluster.metadata.namespace}
          </li>
          <li>
            <strong>Name:</strong> {cluster.metadata.name}
          </li>
        </ul>
      </section>
      <section>
        <h3 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">Specs</h3>
        <SpecTree value={cluster.spec} />
      </section>
    </section>
  )
}
