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
    <table className="w-full border-collapse text-left text-sm">
      <thead>
        <tr>
          {cells.map((cell) => (
            <th key={cell.label} className="border-b p-2">
              {cell.label} <HelpTooltip text={cell.tooltip} />
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        <tr>
          {cells.map((cell) => (
            <td key={cell.label} className="border-b p-2">
              {cell.value}
            </td>
          ))}
        </tr>
      </tbody>
    </table>
  )
}

function ConnectionInfo({ cluster }) {
  const info = cluster.info || {}
  const primary = (cluster.stats?.pods || []).find((pod) => pod.role === 'primary')

  if (!primary) {
    return <p className="text-sm">No pods are available yet</p>
  }

  return (
    <details className="text-sm">
      <summary className="cursor-pointer font-semibold">Connection Information</summary>
      <ul className="flex flex-col gap-1 p-2">
        <li>
          <strong>Read Write DNS:</strong> <code>{info.primaryDns}</code>
        </li>
        <li>
          <strong>Read Only DNS:</strong> <code>{info.replicasDns}</code>
        </li>
        <li>
          <strong>Local psql:</strong>{' '}
          <code>
            kubectl exec -ti {primary.name} -c postgres-util --{' '}
            {info.superuserUsername ? `psql -U ${info.superuserUsername}` : 'psql'}
          </code>
        </li>
        {info.superuserSecretName && (
          <li>
            <strong>Password secret:</strong>{' '}
            <code>
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
    <section className="flex flex-col gap-4 p-8">
      <h2 className="text-xl font-semibold">Cluster</h2>
      <ConnectionInfo cluster={cluster} />
      <Totals cluster={cluster} />
      {pods.length ? (
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr>
              <th className="border-b p-2">
                Pod Name <HelpTooltip field="sgcluster.pods.name" />
              </th>
              <th className="border-b p-2">
                Role <HelpTooltip field="sgcluster.pods.role" />
              </th>
              <th className="border-b p-2">
                Status <HelpTooltip field="sgcluster.pods.status" />
              </th>
              <th className="border-b p-2">
                CPU <HelpTooltip text={cpuTooltip(tt, stats)} />
              </th>
              <th className="border-b p-2">
                Memory <HelpTooltip text={memoryTooltip(tt, stats)} />
              </th>
              <th className="border-b p-2">
                Disk{' '}
                <HelpTooltip
                  text={
                    tt('sgcluster.pods.diskUsed').slice(0, -2) +
                    ' / ' +
                    tt('sgcluster.pods.diskRequested')
                  }
                />
              </th>
              <th className="border-b p-2">
                Containers{' '}
                <HelpTooltip
                  text={
                    tt('sgcluster.pods.containersReady').slice(0, -2) +
                    ' / ' +
                    tt('sgcluster.pods.containers')
                  }
                />
              </th>
            </tr>
          </thead>
          <tbody>
            {pods.map((pod) => (
              <tr key={pod.name}>
                <td className="border-b p-2">{pod.name}</td>
                <td className="border-b p-2">{pod.role}</td>
                <td className="border-b p-2">{pod.status}</td>
                <td className="border-b p-2">
                  {pod.cpuRequested}
                  {pod.cpuPsiAvg60 || pod.averageLoad1m
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
            ))}
          </tbody>
        </table>
      ) : (
        <p className="text-sm">No pods yet available</p>
      )}
    </section>
  )
}
