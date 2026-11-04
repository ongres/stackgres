import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '../../lib/api'
import { useData, useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import CloneResourceDialog from '../CloneResourceDialog'
import DeleteResourceDialog from '../DeleteResourceDialog'
import RestartClusterDialog from '../RestartClusterDialog'

// Promote action for standby clusters (the Vue HeaderSection promoteCluster):
// a PUT of the cluster with spec.replicateFrom nulled; the link shows only
// while the cluster replicates from a source.
function PromoteClusterButton({ cluster }) {
  const { refresh } = useData()

  const promote = async () => {
    const promoted = JSON.parse(JSON.stringify(cluster))
    delete promoted.stats
    promoted.spec.replicateFrom = null
    try {
      await api.update('sgclusters', promoted)
      toast.success(`Cluster "${cluster.metadata.name}" promoted successfully`)
      refresh()
    } catch {
      toast.error(`Cluster "${cluster.metadata.name}" could not be promoted`)
    }
  }

  return (
    <button type="button" onClick={promote} className="underline">
      Promote
    </button>
  )
}

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
      <nav aria-label="Cluster sections" className="flex items-center gap-4 border-b px-8 pt-2 text-sm">
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
          {canI('patch', 'sgclusters', namespace) && (
            <Link to={`${base}/edit`} className="flex items-center gap-1 underline">
              <Pencil size={14} /> Edit
            </Link>
          )}
          {canI('create', 'sgdbops', namespace) && (
            <RestartClusterDialog namespace={namespace} cluster={name} />
          )}
          {cluster.spec.replicateFrom && canI('patch', 'sgclusters', namespace) && (
            <PromoteClusterButton cluster={cluster} />
          )}
          {canI('create', 'sgclusters', namespace) && (
            <CloneResourceDialog kind="sgclusters" item={cluster} />
          )}
          {canI('delete', 'sgclusters', namespace) && (
            <DeleteResourceDialog
              kind="sgclusters"
              namespace={namespace}
              name={name}
              redirect={`/${namespace}/sgclusters`}
            />
          )}
        </div>
      </nav>
      <Outlet context={{ cluster }} />
    </div>
  )
}
