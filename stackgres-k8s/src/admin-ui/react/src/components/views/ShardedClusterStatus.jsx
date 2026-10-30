import { Link, useOutletContext, useParams } from 'react-router-dom'
import { useKindList } from '../../contexts/DataContext'
import { ShardedConditionAlerts, ShardedConnectionInfo } from './ShardedClusterDetail'

// Summary row for one section (coordinator or workers) of the sharded stats.
// The Vue avg-load and primary-disk cells read off the wrong objects and never
// rendered; here they come from the section stats and its primary pod.
function sectionCells(label, stats, instances) {
  const section = stats || {}
  const primary = (section.pods || []).find((pod) => pod.role === 'primary')
  return {
    label,
    cpu: section.cpuRequested
      ? `${section.cpuRequested}${
          section.cpuPsiAvg60 ?? section.averageLoad1m
            ? ` (avg. load ${section.cpuPsiAvg60 ?? section.averageLoad1m})`
            : ''
        }`
      : '-',
    memory: section.memoryPsiAvg60 ?? section.memoryRequested ?? '-',
    primaryDisk: primary ? `${primary.diskUsed ?? '-'} / ${primary.diskRequested ?? '-'}` : '-',
    disk: section.diskRequested ?? '-',
    instances: `${section.podsReady ?? 0} / ${instances ?? '-'}`,
  }
}

export default function ShardedClusterStatus() {
  const { namespace } = useParams()
  const { cluster } = useOutletContext()
  const sgclusters = useKindList('sgclusters', namespace)

  const memberNames = cluster.status?.clusters || []
  const stats = cluster.stats || {}
  const hasPods = Boolean(stats.coordinator?.pods?.length || stats.workers?.pods?.length)

  if (!memberNames.length || !hasPods) {
    return <p className="p-8 text-sm">No data yet available</p>
  }

  const rows = [
    sectionCells('Coordinator', stats.coordinator, cluster.spec?.coordinator?.instances),
    sectionCells('Shards', stats.workers, cluster.spec?.workers?.instancesPerCluster),
  ]

  const members = (sgclusters || []).filter((candidate) =>
    memberNames.includes(candidate.metadata.name)
  )
  const memberOf = (podName) => podName.substring(0, podName.lastIndexOf('-'))

  return (
    <section className="flex flex-col gap-4 p-8">
      <h2 className="text-xl font-semibold">Sharded Cluster</h2>
      <ShardedConditionAlerts cluster={cluster} />
      <ShardedConnectionInfo cluster={cluster} />
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr>
            <th className="border-b p-2" />
            <th className="border-b p-2">Total CPU</th>
            <th className="border-b p-2">Total Memory</th>
            <th className="border-b p-2">Primary Node Disk</th>
            <th className="border-b p-2">Total Allocated Disk</th>
            <th className="border-b p-2">Instances</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.label}>
              <th className="border-b p-2">{row.label}</th>
              <td className="border-b p-2">{row.cpu}</td>
              <td className="border-b p-2">{row.memory}</td>
              <td className="border-b p-2">{row.primaryDisk}</td>
              <td className="border-b p-2">{row.disk}</td>
              <td className="border-b p-2">{row.instances}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {members.some((member) => member.stats?.pods?.length) ? (
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr>
              <th className="border-b p-2">Pod Name</th>
              <th className="border-b p-2">Role</th>
              <th className="border-b p-2">Status</th>
              <th className="border-b p-2">CPU</th>
              <th className="border-b p-2">Memory</th>
              <th className="border-b p-2">Disk</th>
              <th className="border-b p-2">Containers</th>
            </tr>
          </thead>
          <tbody>
            {members.flatMap((member) =>
              (member.stats?.pods || []).map((pod) => (
                <tr key={pod.name}>
                  <td className="border-b p-2">
                    <Link to={`/${namespace}/sgcluster/${memberOf(pod.name)}`} className="underline">
                      {pod.name}
                    </Link>
                  </td>
                  <td className="border-b p-2">{pod.role}</td>
                  <td className="border-b p-2">{pod.status}</td>
                  <td className="border-b p-2">
                    {pod.cpuRequested}
                    {pod.status !== 'Pending' && (pod.cpuPsiAvg60 || pod.averageLoad1m)
                      ? ` (avg. load ${pod.cpuPsiAvg60 ?? pod.averageLoad1m})`
                      : ''}
                  </td>
                  <td className="border-b p-2">{pod.memoryPsiAvg60 ?? pod.memoryRequested}</td>
                  <td className="border-b p-2">
                    {pod.diskUsed ?? '-'} / {pod.diskRequested ?? '-'}
                  </td>
                  <td className="border-b p-2">
                    {pod.containersReady} / {pod.containers}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      ) : (
        <p className="text-sm">Loading cluster status…</p>
      )}
    </section>
  )
}
