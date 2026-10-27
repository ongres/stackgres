import { Link, useParams } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { useKindList } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { activeCondition, detailPath, KINDS, listPath } from '../../lib/kinds'
import StatusDot from '../StatusDot'

function KindCard({ plural, namespace }) {
  const canI = useCanI()
  const items = useKindList(plural, namespace)
  const kind = KINDS[plural]

  if (!canI('any', plural, namespace)) {
    return null
  }

  const statusFor = (item) => {
    if (plural === 'sgbackups') {
      return item.status?.process?.status
    }
    if (plural === 'sgdbops') {
      return activeCondition(item.status?.conditions)
    }
    return null
  }

  return (
    <article className="border">
      <header className="flex items-center justify-between gap-2 border-b p-3">
        <Link to={listPath(plural, namespace)} className="font-semibold underline">
          {kind.crd} <span className="text-xs font-normal">({items ? items.length : '-'})</span>
        </Link>
        {canI('create', plural, namespace) && (
          <Link
            to={`${listPath(plural, namespace)}/new`}
            title={`Create ${kind.crd}`}
            aria-label={`Create ${kind.crd}`}
          >
            <Plus size={16} />
          </Link>
        )}
      </header>
      <ul className="flex flex-col gap-1 p-3 text-sm">
        {items && items.length ? (
          items.map((item) => (
            <li key={item.metadata.name} className="flex items-center gap-2">
              <StatusDot status={statusFor(item)} />
              <Link to={detailPath(plural, namespace, item.metadata.name)}>
                {item.metadata.name}
              </Link>
            </li>
          ))
        ) : (
          <li>
            No {kind.crd}s have been found
            {canI('create', plural, namespace) && (
              <>
                , would you like to{' '}
                <Link to={`${listPath(plural, namespace)}/new`} className="underline">
                  create a new one?
                </Link>
              </>
            )}
          </li>
        )}
      </ul>
    </article>
  )
}

export default function NamespaceOverview() {
  const { namespace } = useParams()

  return (
    <section className="flex flex-col gap-4 p-8">
      <h1 className="text-2xl font-semibold">Namespace Overview: {namespace}</h1>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {Object.keys(KINDS).map((plural) => (
          <KindCard key={plural} plural={plural} namespace={namespace} />
        ))}
      </div>
    </section>
  )
}
