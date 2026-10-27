import { useOutletContext } from 'react-router-dom'
import SpecTree from '../SpecTree'

export default function ClusterConfig() {
  const { cluster } = useOutletContext()

  return (
    <section className="flex flex-col gap-4 p-8 text-sm">
      <h2 className="text-xl font-semibold">Cluster Details</h2>
      <section>
        <h3 className="mb-2 font-semibold uppercase">Metadata</h3>
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
        <h3 className="mb-2 font-semibold uppercase">Specs</h3>
        <SpecTree value={cluster.spec} />
      </section>
    </section>
  )
}
