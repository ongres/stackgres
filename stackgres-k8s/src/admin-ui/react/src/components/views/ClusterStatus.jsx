import { useOutletContext } from 'react-router-dom'

function Totals({ cluster }) {
  const stats = cluster.stats || {}
  const primary = (stats.pods || []).find((pod) => pod.role === 'primary')

  const cells = [
    {
      label: 'Total CPU',
      value: stats.cpuRequested
        ? `${stats.cpuRequested} (avg. load ${stats.cpuPsiAvg60 ?? stats.averageLoad1m ?? '-'})`
        : '-',
    },
    { label: 'Total Memory', value: stats.memoryPsiAvg60 ?? stats.memoryRequested ?? '-' },
    {
      label: 'Primary Node Disk',
      value: primary ? `${primary.diskUsed ?? '-'} / ${primary.diskRequested ?? '-'}` : '-',
    },
    { label: 'Total Allocated Disk', value: stats.diskRequested ?? '-' },
    { label: 'Instances', value: `${cluster.podsReady ?? 0} / ${cluster.spec.instances}` },
  ]

  return (
    <table className="w-full border-collapse text-left text-sm">
      <thead>
        <tr>
          {cells.map((cell) => (
            <th key={cell.label} className="border-b p-2">
              {cell.label}
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
  const pods = cluster.stats?.pods || []

  return (
    <section className="flex flex-col gap-4 p-8">
      <h2 className="text-xl font-semibold">Cluster</h2>
      <ConnectionInfo cluster={cluster} />
      <Totals cluster={cluster} />
      {pods.length ? (
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
