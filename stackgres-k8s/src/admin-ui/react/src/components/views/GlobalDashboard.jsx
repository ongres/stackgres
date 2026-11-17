import { Link } from 'react-router-dom'
import { useData } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { useNamespaces } from '../../hooks/useNamespaces'
import { KINDS, listPath } from '../../lib/kinds'

export default function GlobalDashboard() {
  const namespaces = useNamespaces()
  const data = useData()
  const canI = useCanI()

  const countIn = (plural, namespace) => {
    const items = data[plural]
    if (items === null) {
      return '-'
    }
    return items.filter((item) => item.metadata.namespace === namespace).length
  }

  const usedNamespaces = namespaces.filter((namespace) =>
    Object.keys(KINDS).some((plural) => {
      const items = data[plural]
      return items && items.some((item) => item.metadata.namespace === namespace)
    })
  )

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">
        {usedNamespaces.length ? 'Used Namespaces' : 'Select a namespace'}
      </h1>
      <div className="flex flex-wrap justify-center gap-4 [&>article]:w-full md:[&>article]:w-[calc(50%-0.5rem)] xl:[&>article]:w-[calc(25%-0.75rem)]" data-testid="namespaces">
        {usedNamespaces.map((namespace) => (
          <article key={namespace} className="bg-[hsl(var(--card))] rounded shadow border">
            <header className="flex items-center justify-between border-b p-3">
              <Link to={`/${namespace}`} className="font-semibold text-[hsl(var(--primary))] hover:underline">
                {namespace}
              </Link>
              {canI('create', 'sgclusters', namespace) && (
                <Link to={`/${namespace}/sgclusters/new`} className="text-xs font-medium text-[hsl(var(--primary))] hover:underline uppercase">
                  Create Cluster
                </Link>
              )}
            </header>
            <ul className="flex flex-col gap-1 p-3 text-sm">
              {Object.entries(KINDS).map(([plural, kind]) =>
                canI('list', plural, namespace) ? (
                  <li key={plural} className="flex items-center justify-between gap-2">
                    <Link to={listPath(plural, namespace)}>{kind.crd}</Link>
                    <span className="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium border bg-[hsl(var(--muted))]">{countIn(plural, namespace)}</span>
                  </li>
                ) : null
              )}
            </ul>
          </article>
        ))}
      </div>
    </section>
  )
}
