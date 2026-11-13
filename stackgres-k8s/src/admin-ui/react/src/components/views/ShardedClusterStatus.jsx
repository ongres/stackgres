import { Link, useOutletContext, useParams } from 'react-router-dom'
import { useKindList } from '../../contexts/DataContext'
import { useTooltips } from '../../contexts/TooltipsContext'
import { HelpTooltip } from '../HelpTooltip'
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
  const { getTooltip: tt } = useTooltips()
  const sgclusters = useKindList('sgclusters', namespace)

  const memberNames = cluster.status?.clusters || []
  const stats = cluster.stats || {}
  const hasPods = Boolean(stats.coordinator?.pods?.length || stats.workers?.pods?.length)

  if (!memberNames.length || !hasPods) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">No data yet available</p>
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
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h2 className="text-lg font-semibold">Sharded Cluster</h2>
      <ShardedConditionAlerts cluster={cluster} />
      <ShardedConnectionInfo cluster={cluster} />
      <div className="rounded border overflow-auto">
        <table className="w-full table-auto text-left">
          <thead className="bg-[hsl(var(--muted))] text-left">
            <tr>
              <th className="px-4 py-2" />
              <th className="px-4 py-2">
                <span className="inline-flex items-center gap-1 whitespace-nowrap">
                  Total CPU{' '}
                  <HelpTooltip
                    text={tt('sgshardedcluster.stats.coordinator.cpuRequested').slice(0, -2)}
                  />
                </span>
              </th>
              <th className="px-4 py-2">
                <span className="inline-flex items-center gap-1 whitespace-nowrap">
                  Total Memory <HelpTooltip field="sgshardedcluster.stats.coordinator.memoryRequested" />
                </span>
              </th>
              <th className="px-4 py-2">
                <span className="inline-flex items-center gap-1 whitespace-nowrap">
                  Primary Node Disk{' '}
                  <HelpTooltip
                    text={
                      tt('sgshardedcluster.stats.coordinator.diskUsed').slice(0, -2) +
                      ' / ' +
                      tt('sgshardedcluster.spec.coordinator.pods.persistentVolume.size')
                    }
                  />
                </span>
              </th>
              <th className="px-4 py-2">
                <span className="inline-flex items-center gap-1 whitespace-nowrap">
                  Total Allocated Disk{' '}
                  <HelpTooltip field="sgshardedcluster.stats.coordinator.diskRequested" />
                </span>
              </th>
              <th className="px-4 py-2">
                <span className="inline-flex items-center gap-1 whitespace-nowrap">
                  Instances{' '}
                  <HelpTooltip
                    text={
                      tt('sgshardedcluster.stats.coordinator.podsReady') +
                      ' / ' +
                      tt('sgshardedcluster.spec.coordinator.instances') +
                      ' – ' +
                      tt('sgshardedcluster.stats.workers.podsReady') +
                      ' / ' +
                      tt('sgshardedcluster.spec.workers.instancesPerCluster')
                    }
                  />
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.label} className="border-t">
                <th className="px-4 py-2">{row.label}</th>
                <td className="px-4 py-2">{row.cpu}</td>
                <td className="px-4 py-2">{row.memory}</td>
                <td className="px-4 py-2">{row.primaryDisk}</td>
                <td className="px-4 py-2">{row.disk}</td>
                <td className="px-4 py-2">{row.instances}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {members.some((member) => member.stats?.pods?.length) ? (
        <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Pod Name <HelpTooltip field="sgcluster.pods.name" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Role <HelpTooltip field="sgcluster.pods.role" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Status <HelpTooltip field="sgcluster.pods.status" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    CPU <HelpTooltip text={tt('sgcluster.pods.cpuRequested').slice(0, -2)} />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Memory <HelpTooltip field="sgcluster.pods.memoryRequested" />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Disk{' '}
                    <HelpTooltip
                      text={
                        tt('sgcluster.pods.diskUsed').slice(0, -2) +
                        ' / ' +
                        tt('sgcluster.pods.diskRequested')
                      }
                    />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Containers{' '}
                    <HelpTooltip
                      text={
                        tt('sgcluster.pods.containersReady').slice(0, -2) +
                        ' / ' +
                        tt('sgcluster.pods.containers')
                      }
                    />
                  </span>
                </th>
              </tr>
            </thead>
            <tbody>
              {members.flatMap((member) =>
                (member.stats?.pods || []).map((pod) => (
                  <tr key={pod.name} className="border-t">
                    <td className="px-4 py-2">
                      <Link to={`/${namespace}/sgcluster/${memberOf(pod.name)}`} className="text-[hsl(var(--primary))] hover:underline">
                        {pod.name}
                      </Link>
                    </td>
                    <td className="px-4 py-2">{pod.role}</td>
                    <td className="px-4 py-2">{pod.status}</td>
                    <td className="px-4 py-2">
                      {pod.cpuRequested}
                      {pod.status !== 'Pending' && (pod.cpuPsiAvg60 || pod.averageLoad1m)
                        ? ` (avg. load ${pod.cpuPsiAvg60 ?? pod.averageLoad1m})`
                        : ''}
                    </td>
                    <td className="px-4 py-2">{pod.memoryPsiAvg60 ?? pod.memoryRequested}</td>
                    <td className="px-4 py-2">
                      {pod.diskUsed ?? '-'} / {pod.diskRequested ?? '-'}
                    </td>
                    <td className="px-4 py-2">
                      {pod.containersReady} / {pod.containers}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-[hsl(var(--muted-foreground))] italic">Loading cluster status…</p>
      )}
    </section>
  )
}
