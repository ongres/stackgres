import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import CloneResourceDialog from '../CloneResourceDialog'
import DeleteResourceDialog from '../DeleteResourceDialog'
import { BreadcrumbActions } from '../layout/Breadcrumbs'

// Condition alerts shared by the sharded Status and Configuration tabs: a
// pending-restart notice and a components-updated warning.
export function ShardedConditionAlerts({ cluster }) {
  const conditions = cluster.status?.conditions || []
  const pendingRestart = conditions.find(
    (condition) => condition.type === 'PendingRestart' && condition.status === 'True'
  )
  const componentsOutdated = conditions.find(
    (condition) =>
      condition.type === 'ComponentsUpdated' &&
      (condition.status === 'False' || condition.reason !== 'UpToDate')
  )
  if (!pendingRestart && !componentsOutdated) {
    return null
  }
  return (
    <div className="flex flex-col gap-1 text-sm">
      {pendingRestart && <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">A restart is pending for this sharded cluster</p>}
      {componentsOutdated && <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">{componentsOutdated.message || 'Components are not up to date'}</p>}
    </div>
  )
}

// Connection panel shared by the sharded Status and Configuration tabs (the
// Vue components duplicated this markup; the broken hasPods read in the
// Config tab is fixed by computing readiness from the stats sections).
export function ShardedConnectionInfo({ cluster }) {
  const info = cluster.info || {}
  const stats = cluster.stats || {}
  const podsReady = Boolean(stats.coordinator?.podsReady && stats.workers?.podsReady)
  const coordinator = (cluster.status?.clusters || [])[0]

  if (!podsReady) {
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
          <strong>Read Only DNS:</strong> <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">{info.readsDns}</code>
        </li>
        {coordinator && (
          <li>
            <strong>Local psql:</strong>{' '}
            <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">
              kubectl exec -ti {coordinator} -c postgres-util --{' '}
              {info.superuserUsername && info.superuserUsername !== 'postgres'
                ? `psql -U ${info.superuserUsername}`
                : 'psql'}
            </code>
          </li>
        )}
        <li>
          <strong>External psql:</strong>{' '}
          <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">
            psql -h {info.primaryDns} -U {info.superuserUsername || 'postgres'}
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
        {cluster.spec?.postgres?.flavor === 'babelfish' && (
          <li>
            <strong>Babelfish (port 1433) password secret:</strong>{' '}
            <code className="rounded bg-[hsl(var(--secondary))] px-1.5 py-0.5">
              {cluster.metadata.name} / babelfish-password
            </code>
          </li>
        )}
      </ul>
    </details>
  )
}

// Tab chrome for /:namespace/sgshardedcluster/:name/* — the Vue HeaderSection
// shows Status, Configuration and (when Grafana is embedded) Monitoring;
// backups/logs/events tabs are SGCluster-only.
export default function ShardedClusterDetail() {
  const { namespace, name } = useParams()
  const canI = useCanI()
  const items = useKindList('sgshardedclusters', namespace)
  const cluster = useResource('sgshardedclusters', namespace, name)

  if (items === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }
  if (cluster === undefined) {
    return (
      <section className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">
        <p>
          SGShardedCluster <strong>{name}</strong> was not found in namespace{' '}
          <strong>{namespace}</strong>.
        </p>
        <Link to={`/${namespace}/sgshardedclusters`} className="text-[hsl(var(--primary))] hover:underline">
          Back to StackGres Sharded Clusters
        </Link>
      </section>
    )
  }

  const base = `/${namespace}/sgshardedcluster/${name}`
  const tabs = [
    { label: 'Status', to: base, end: true },
    { label: 'Configuration', to: `${base}/config` },
    ...(cluster.grafanaEmbedded ? [{ label: 'Monitoring', to: `${base}/monitor` }] : []),
  ]

  return (
    <div>
      <nav aria-label="Sharded cluster sections" className="tabs-scroll-wrap border-b">
        <div className="flex overflow-x-auto px-6">
        {tabs.map((tab) => (
          <NavLink
            key={tab.label}
            to={tab.to}
            end={tab.end}
            className={({ isActive }) =>
              `px-4 sm:px-6 h-[43px] font-medium text-sm flex items-center gap-1.5 whitespace-nowrap ${isActive ? 'border-b-2 border-[hsl(var(--primary))] text-[hsl(var(--primary))]' : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]'}`
            }
          >
            {tab.label}
          </NavLink>
        ))}
        </div>
      </nav>
      <BreadcrumbActions>
          {canI('patch', 'sgshardedclusters', namespace) && (
            <Link to={`${base}/edit`} className="flex items-center gap-1 text-[hsl(var(--primary))] hover:underline">
              <Pencil size={14} /> Edit
            </Link>
          )}
          {canI('create', 'sgshardedclusters', namespace) && (
            <CloneResourceDialog kind="sgshardedclusters" item={cluster} />
          )}
          {canI('delete', 'sgshardedclusters', namespace) && (
            <DeleteResourceDialog
              kind="sgshardedclusters"
              namespace={namespace}
              name={name}
              redirect={`/${namespace}/sgshardedclusters`}
            />
          )}
      </BreadcrumbActions>
      <div className="pt-4">
        <Outlet context={{ cluster }} />
      </div>
    </div>
  )
}
