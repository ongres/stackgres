import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import CloneResourceDialog from '../CloneResourceDialog'
import DeleteResourceDialog from '../DeleteResourceDialog'

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
      {pendingRestart && <p role="note">A restart is pending for this sharded cluster</p>}
      {componentsOutdated && <p role="note">{componentsOutdated.message || 'Components are not up to date'}</p>}
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
          <strong>Read Only DNS:</strong> <code>{info.readsDns}</code>
        </li>
        {coordinator && (
          <li>
            <strong>Local psql:</strong>{' '}
            <code>
              kubectl exec -ti {coordinator} -c postgres-util --{' '}
              {info.superuserUsername && info.superuserUsername !== 'postgres'
                ? `psql -U ${info.superuserUsername}`
                : 'psql'}
            </code>
          </li>
        )}
        <li>
          <strong>External psql:</strong>{' '}
          <code>
            psql -h {info.primaryDns} -U {info.superuserUsername || 'postgres'}
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
        {cluster.spec?.postgres?.flavor === 'babelfish' && (
          <li>
            <strong>Babelfish (port 1433) password secret:</strong>{' '}
            <code>
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
    return <p className="p-8 text-sm">Loading data…</p>
  }
  if (cluster === undefined) {
    return (
      <section className="p-8 text-sm">
        <p>
          SGShardedCluster <strong>{name}</strong> was not found in namespace{' '}
          <strong>{namespace}</strong>.
        </p>
        <Link to={`/${namespace}/sgshardedclusters`} className="underline">
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
      <nav aria-label="Sharded cluster sections" className="flex items-center gap-4 border-b px-8 pt-2 text-sm">
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
        <div className="ml-auto flex gap-3 pb-2">
          {canI('patch', 'sgshardedclusters', namespace) && (
            <Link to={`${base}/edit`} className="flex items-center gap-1 underline">
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
        </div>
      </nav>
      <Outlet context={{ cluster }} />
    </div>
  )
}
