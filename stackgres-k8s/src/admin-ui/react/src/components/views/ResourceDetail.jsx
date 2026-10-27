import { Link, useParams } from 'react-router-dom'
import { useKindList, useResource } from '../../contexts/DataContext'
import { KINDS, listPath } from '../../lib/kinds'
import SpecTree from '../SpecTree'
import DbOpsEvents from './DbOpsEvents'

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
  const { namespace, name } = useParams()
  const items = useKindList(plural, namespace)
  const resource = useResource(plural, namespace, name)

  if (items === null) {
    return <p className="p-8 text-sm">Loading data…</p>
  }
  if (resource === undefined) {
    return (
      <section className="p-8 text-sm">
        <p>
          {KINDS[plural].crd} <strong>{name}</strong> was not found in namespace{' '}
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
      <h1 className="text-2xl font-semibold">{HEADINGS[plural] || `${KINDS[plural].crd} Details`}</h1>
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
          {plural === 'sgdbops' && <DbOpsEvents />}
        </div>
      </div>
    </section>
  )
}
