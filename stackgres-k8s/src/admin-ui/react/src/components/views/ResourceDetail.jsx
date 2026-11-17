import { Link, useParams } from 'react-router-dom'
import { useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { KINDS, listPath } from '../../lib/kinds'
import CloneResourceDialog from '../CloneResourceDialog'
import DeleteResourceDialog from '../DeleteResourceDialog'
import { BreadcrumbActions } from '../layout/Breadcrumbs'
import SpecTree from '../SpecTree'
import { HelpTooltip } from '../HelpTooltip'
import BenchmarkResults from './BenchmarkResults'
import DbOpsEvents from './DbOpsEvents'
import MinorVersionUpgradeOverview from './MinorVersionUpgradeOverview'

const HEADINGS = {
  sginstanceprofiles: 'Profile Details',
  sgpgconfigs: 'Configuration Details',
  sgpoolconfigs: 'Configuration Details',
  sgobjectstorages: 'Object Storage Details',
  sgscripts: 'Script Details',
  sgdistributedlogs: 'Logs Server Details',
  sgstreams: 'Stream Details',
  sgbackups: 'Backup Details',
  sgdbops: 'Operation Details',
}

export default function ResourceDetail({ plural }) {
  const { namespace, name, backupname } = useParams()
  const resourceName = plural === 'sgbackups' ? (backupname ?? name) : name
  const canI = useCanI()
  const items = useKindList(plural, namespace)
  const resource = useResource(plural, namespace, resourceName)
  // sg-tooltips index key: the CRD name lowercased (SGPostgresConfig →
  // sgpostgresconfig), matching the Vue templates' getTooltip() roots.
  const tooltipRoot = KINDS[plural].crd.toLowerCase()

  if (items === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }
  if (resource === undefined) {
    return (
      <section className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">
        <p>
          {KINDS[plural].crd} <strong>{resourceName}</strong> was not found in namespace{' '}
          <strong>{namespace}</strong>.
        </p>
        <Link to={listPath(plural, namespace)} className="text-[hsl(var(--primary))] hover:underline">
          Back to {KINDS[plural].label}
        </Link>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">{HEADINGS[plural] || `${KINDS[plural].crd} Details`}</h1>
      <BreadcrumbActions>
          {plural !== 'sgdbops' && canI('patch', plural, namespace) && (
            <Link to={`/${namespace}/${KINDS[plural].singular}/${resourceName}/edit`} className="text-[hsl(var(--primary))] hover:underline">
              Edit
            </Link>
          )}
          {!['sgbackups', 'sgdbops'].includes(plural) && canI('create', plural, namespace) && (
            <CloneResourceDialog kind={plural} item={resource} />
          )}
          {canI('delete', plural, namespace) && (
            <DeleteResourceDialog
              kind={plural}
              namespace={namespace}
              name={resourceName}
              redirect={listPath(plural, namespace)}
            />
          )}
      </BreadcrumbActions>
      <div className="grid grid-cols-1 gap-6 text-sm lg:grid-cols-2">
        <div className="flex flex-col gap-4">
          <section>
            <h2 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">Metadata</h2>
            <ul className="flex flex-col gap-1">
              <li>
                <strong>Namespace</strong>
                <HelpTooltip field={`${tooltipRoot}.metadata.namespace`} />
                <strong>:</strong>{' '}
                <Link to={`/${namespace}`} className="text-[hsl(var(--primary))] hover:underline">
                  {namespace}
                </Link>
              </li>
              <li>
                <strong>Name</strong>
                <HelpTooltip field={`${tooltipRoot}.metadata.name`} />
                <strong>:</strong> {resource.metadata.name}
              </li>
              {resource.metadata.uid && (
                <li>
                  <strong>UID:</strong> {resource.metadata.uid}
                </li>
              )}
            </ul>
          </section>
          <section>
            <h2 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">Specs</h2>
            <SpecTree value={resource.spec} tooltipPath={`${tooltipRoot}.spec`} />
          </section>
        </div>
        <div className="flex flex-col gap-4">
          {resource.status && (
            <section>
              <h2 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">Status</h2>
              <SpecTree value={resource.status} tooltipPath={`${tooltipRoot}.status`} />
            </section>
          )}
          {plural === 'sgdbops' && (
            <>
              <BenchmarkResults dbop={resource} />
              <MinorVersionUpgradeOverview dbop={resource} />
              <DbOpsEvents />
            </>
          )}
        </div>
      </div>
    </section>
  )
}
