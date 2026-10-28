import { Link, useParams } from 'react-router-dom'
import { useKindList, useResource } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { KINDS, listPath } from '../../lib/kinds'
import CloneResourceDialog from '../CloneResourceDialog'
import DeleteResourceDialog from '../DeleteResourceDialog'
import SpecTree from '../SpecTree'
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

  if (items === null) {
    return <p className="p-8 text-sm">Loading data…</p>
  }
  if (resource === undefined) {
    return (
      <section className="p-8 text-sm">
        <p>
          {KINDS[plural].crd} <strong>{resourceName}</strong> was not found in namespace{' '}
          <strong>{namespace}</strong>.
        </p>
        <Link to={listPath(plural, namespace)} className="underline">
          Back to {KINDS[plural].label}
        </Link>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4 p-8">
      <div className="flex items-center justify-between gap-4">
        <h1 className="text-2xl font-semibold">{HEADINGS[plural] || `${KINDS[plural].crd} Details`}</h1>
        <div className="flex gap-3 text-sm">
          {plural !== 'sgdbops' && canI('patch', plural, namespace) && (
            <Link to={`/${namespace}/${KINDS[plural].singular}/${resourceName}/edit`} className="underline">
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
        </div>
      </div>
      <div className="grid grid-cols-1 gap-6 text-sm lg:grid-cols-2">
        <div className="flex flex-col gap-4">
          <section>
            <h2 className="mb-2 font-semibold uppercase">Metadata</h2>
            <ul className="flex flex-col gap-1">
              <li>
                <strong>Namespace:</strong>{' '}
                <Link to={`/${namespace}`} className="underline">
                  {namespace}
                </Link>
              </li>
              <li>
                <strong>Name:</strong> {resource.metadata.name}
              </li>
              {resource.metadata.uid && (
                <li>
                  <strong>UID:</strong> {resource.metadata.uid}
                </li>
              )}
            </ul>
          </section>
          <section>
            <h2 className="mb-2 font-semibold uppercase">Specs</h2>
            <SpecTree value={resource.spec} />
          </section>
        </div>
        <div className="flex flex-col gap-4">
          {resource.status && (
            <section>
              <h2 className="mb-2 font-semibold uppercase">Status</h2>
              <SpecTree value={resource.status} />
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
