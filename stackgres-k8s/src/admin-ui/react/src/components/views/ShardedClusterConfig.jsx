import { useOutletContext } from 'react-router-dom'
import SpecTree from '../SpecTree'
import { ShardedConditionAlerts, ShardedConnectionInfo } from './ShardedClusterDetail'

export default function ShardedClusterConfig() {
  const { cluster } = useOutletContext()

  return (
    <section className="flex flex-col gap-4 p-8">
      <h2 className="text-xl font-semibold">Sharded Cluster Details</h2>
      <ShardedConditionAlerts cluster={cluster} />
      <ShardedConnectionInfo cluster={cluster} />
      <SpecTree value={{ metadata: cluster.metadata, spec: cluster.spec }} />
    </section>
  )
}
