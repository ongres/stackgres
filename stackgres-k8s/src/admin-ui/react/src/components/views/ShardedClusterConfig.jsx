import { useOutletContext } from 'react-router-dom'
import SpecTree from '../SpecTree'
import { ShardedConditionAlerts, ShardedConnectionInfo } from './ShardedClusterDetail'

export default function ShardedClusterConfig() {
  const { cluster } = useOutletContext()

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <div className="flex items-center justify-between gap-4">
        <h2 className="text-lg font-semibold">Sharded Cluster Details</h2>
        <ShardedConnectionInfo cluster={cluster} />
      </div>
      <ShardedConditionAlerts cluster={cluster} />
      <SpecTree value={{ metadata: cluster.metadata, spec: cluster.spec }} />
    </section>
  )
}
