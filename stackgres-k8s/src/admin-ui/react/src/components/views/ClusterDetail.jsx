import { Link, NavLink, Outlet, useParams } from 'react-router-dom'
import { Pencil } from 'lucide-react'
import { toast } from 'sonner'
import { api } from '../../lib/api'
import { useData, useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import CloneResourceDialog from '../CloneResourceDialog'
import DeleteResourceDialog from '../DeleteResourceDialog'
import RestartClusterDialog from '../RestartClusterDialog'
import { BreadcrumbActions } from '../layout/Breadcrumbs'

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
    <button type="button" onClick={promote} className="text-[hsl(var(--primary))] hover:underline">
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
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }
  if (cluster === undefined) {
    return (
      <section className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">
        <p>
          SGCluster <strong>{name}</strong> was not found in namespace <strong>{namespace}</strong>.
        </p>
        <Link to={`/${namespace}/sgclusters`} className="text-[hsl(var(--primary))] hover:underline">
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
      <nav aria-label="Cluster sections" className="tabs-scroll-wrap border-b">
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
          {canI('patch', 'sgclusters', namespace) && (
            <Link to={`${base}/edit`} className="flex items-center gap-1 text-[hsl(var(--primary))] hover:underline">
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
      </BreadcrumbActions>
      <div className="pt-4">
        <Outlet context={{ cluster }} />
      </div>
    </div>
  )
}
