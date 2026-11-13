import { useOutletContext } from 'react-router-dom'
import { useTooltips } from '../../contexts/TooltipsContext'
import { HelpTooltip } from '../HelpTooltip'

// Composed header tooltips from the Vue ClusterStatus template; the
// .slice(0, -2) strips the descriptions' trailing ".\n".
const cpuTooltip = (tt, stats) =>
  tt('sgcluster.pods.cpuRequested').slice(0, -2) +
  ' (' +
  (stats.cpuPsiAvg60 != null
    ? tt('sgcluster.pods.cpuPsiAvg60')
    : tt('sgcluster.pods.averageLoad1m')) +
  ')'

const memoryTooltip = (tt, stats) =>
  stats.memoryPsiAvg60 != null
    ? tt('sgcluster.pods.memoryPsiAvg60')
    : tt('sgcluster.pods.memoryRequested')

function Totals({ cluster }) {
  const { getTooltip: tt } = useTooltips()
  const stats = cluster.stats || {}
  const primary = (stats.pods || []).find((pod) => pod.role === 'primary')

  const cells = [
    {
      label: 'Total CPU',
      tooltip: cpuTooltip(tt, stats),
      value: stats.cpuRequested
        ? `${stats.cpuRequested} (avg. load ${stats.cpuPsiAvg60 ?? stats.averageLoad1m ?? '-'})`
        : '-',
    },
    {
      label: 'Total Memory',
      tooltip: memoryTooltip(tt, stats),
      value: stats.memoryPsiAvg60 ?? stats.memoryRequested ?? '-',
    },
    {
      label: 'Primary Node Disk',
      tooltip:
        tt('sgcluster.pods.diskUsed').slice(0, -2) +
        ' / ' +
        tt('sgcluster.spec.pods.persistentVolume.size'),
      value: primary ? `${primary.diskUsed ?? '-'} / ${primary.diskRequested ?? '-'}` : '-',
    },
    {
      label: 'Total Allocated Disk',
      tooltip: tt('sgclusterstats.diskRequested'),
      value: stats.diskRequested ?? '-',
    },
    {
      label: 'Instances',
      tooltip: tt('sgcluster.podsReady').slice(0, -2) + ' / ' + tt('sgcluster.spec.instances'),
      value: `${cluster.podsReady ?? 0} / ${cluster.spec.instances}`,
    },
  ]

  return (
    <div className="rounded border overflow-auto">
      <table className="w-full table-auto text-left">
        <thead className="bg-[hsl(var(--muted))] text-left">
          <tr>
            {cells.map((cell) => (
              <th key={cell.label} className="px-4 py-2">
                <span className="inline-flex items-center gap-1 whitespace-nowrap">
                  {cell.label} <HelpTooltip text={cell.tooltip} />
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr className="border-t">
            {cells.map((cell) => (
              <td key={cell.label} className="px-4 py-2">
                {cell.value}
              </td>
            ))}
          </tr>
        </tbody>
      </table>
    </div>
  )
}

function ConnectionInfo({ cluster }) {
  const info = cluster.info || {}
  const primary = (cluster.stats?.pods || []).find((pod) => pod.role === 'primary')

  if (!primary) {
    return <p className="text-sm text-[hsl(var(--muted-foreground))] italic">No pods are available yet</p>
  }

  return (
    <details className="text-sm">
      <summary className="cursor-pointer text-sm font-semibold">Connection Information</summary>
      <ul className="flex flex-col gap-1 p-2">
        <li>
          <strong>Read Write DNS:</strong> <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">{info.primaryDns}</code>
        </li>
        <li>
          <strong>Read Only DNS:</strong> <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">{info.replicasDns}</code>
        </li>
        <li>
          <strong>Local psql:</strong>{' '}
          <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">
            kubectl exec -ti {primary.name} -c postgres-util --{' '}
            {info.superuserUsername ? `psql -U ${info.superuserUsername}` : 'psql'}
          </code>
        </li>
        {info.superuserSecretName && (
          <li>
            <strong>Password secret:</strong>{' '}
            <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">
              {info.superuserSecretName} / {info.superuserPasswordKey}
            </code>
          </li>
        )}
      </ul>
    </details>
  )
}

export default function ClusterStatus() {
  const { cluster } = useOutletContext()
  const { getTooltip: tt } = useTooltips()
  const stats = cluster.stats || {}
  const pods = stats.pods || []

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h2 className="text-lg font-semibold">Cluster</h2>
      <ConnectionInfo cluster={cluster} />
      <Totals cluster={cluster} />
      {pods.length ? (
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
                    CPU <HelpTooltip text={cpuTooltip(tt, stats)} />
                  </span>
                </th>
                <th className="px-4 py-2">
                  <span className="inline-flex items-center gap-1 whitespace-nowrap">
                    Memory <HelpTooltip text={memoryTooltip(tt, stats)} />
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
              {pods.map((pod) => (
                <tr key={pod.name} className="border-t">
                  <td className="px-4 py-2">{pod.name}</td>
                  <td className="px-4 py-2">{pod.role}</td>
                  <td className="px-4 py-2">{pod.status}</td>
                  <td className="px-4 py-2">
                    {pod.cpuRequested}
                    {pod.cpuPsiAvg60 || pod.averageLoad1m
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
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-[hsl(var(--muted-foreground))] italic">No pods yet available</p>
      )}
    </section>
  )
}
