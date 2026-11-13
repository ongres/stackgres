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
    <article className="bg-[hsl(var(--card))] rounded shadow border">
      <header className="flex items-center justify-between gap-2 border-b p-3">
        <Link to={listPath(plural, namespace)} className="font-semibold text-[hsl(var(--primary))] hover:underline">
          {kind.crd} <span className="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium border bg-[hsl(var(--muted))]">({items ? items.length : '-'})</span>
        </Link>
        {canI('create', plural, namespace) && (
          <Link
            to={`${listPath(plural, namespace)}/new`}
            title={`Create ${kind.crd}`}
            aria-label={`Create ${kind.crd}`}
            className="p-1 rounded-md text-[hsl(var(--primary))] hover:bg-[hsl(var(--primary)/0.1)]"
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
                <Link to={`${listPath(plural, namespace)}/new`} className="text-[hsl(var(--primary))] hover:underline">
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
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">Namespace Overview: {namespace}</h1>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        {Object.keys(KINDS).map((plural) => (
          <KindCard key={plural} plural={plural} namespace={namespace} />
        ))}
      </div>
    </section>
  )
}
