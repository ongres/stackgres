import { useMemo } from 'react'
import { useOutletContext, useParams } from 'react-router-dom'
import { MonitoringView } from './ClusterMonitoring'

// Sharded monitoring: the pod list is the coordinator pods followed by all
// shard pods, flat, as in the Vue component (which crashed when the workers
// stats were missing — guarded here).
export default function ShardedClusterMonitoring() {
  const { namespace, name } = useParams()
  const { cluster } = useOutletContext()
  const pods = useMemo(
    () => [...(cluster.stats?.coordinator?.pods || []), ...(cluster.stats?.workers?.pods || [])],
    [cluster.stats]
  )
  return <MonitoringView base={`/${namespace}/sgshardedcluster/${name}/monitor`} pods={pods} />
}
