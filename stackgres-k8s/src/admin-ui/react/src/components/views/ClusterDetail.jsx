import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'

// Tab chrome for /:namespace/sgcluster/:name/* — the Vue HeaderSection tabs.
export default function ClusterDetail() {
  const { namespace, name } = useParams()
  const canI = useCanI()
  const items = useKindList('sgclusters', namespace)
  const cluster = useResource('sgclusters', namespace, name)

  if (items === null) {
    return <p className="p-8 text-sm">Loading data…</p>
  }
  if (cluster === undefined) {
    return (
      <section className="p-8 text-sm">
        <p>
          SGCluster <strong>{name}</strong> was not found in namespace <strong>{namespace}</strong>.
        </p>
        <Link to={`/${namespace}/sgclusters`} className="underline">
          Back to StackGres Clusters
        </Link>
      </section>
    )
  }

  const base = `/${namespace}/sgcluster/${name}`
  const tabs = [
    { label: 'Status', to: base, end: true },
    { label: 'Configuration', to: `${base}/config` },
    ...(canI('list', 'sgbackups', namespace) ? [{ label: 'Backups', to: `${base}/sgbackups` }] : []),
    ...(cluster.spec.distributedLogs && canI('list', 'sgdistributedlogs', namespace)
      ? [{ label: 'Logs', to: `${base}/logs` }]
      : []),
    ...(cluster.grafanaEmbedded ? [{ label: 'Monitoring', to: `${base}/monitor` }] : []),
    { label: 'Events', to: `${base}/events` },
  ]

  return (
    <div>
      <nav aria-label="Cluster sections" className="flex gap-4 border-b px-8 pt-2 text-sm">
        {tabs.map((tab) => (
          <NavLink
            key={tab.label}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) => `pb-2 ${isActive ? 'border-b-2 font-semibold' : ''}`}
          >
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <Outlet context={{ cluster }} />
    </div>
  )
}
