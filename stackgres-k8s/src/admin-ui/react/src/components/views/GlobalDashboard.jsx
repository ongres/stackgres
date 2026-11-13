import { Link, useNavigate } from 'react-router-dom'
import { useData } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { useNamespaces } from '../../hooks/useNamespaces'
import { KINDS, listPath } from '../../lib/kinds'

export default function GlobalDashboard() {
  const namespaces = useNamespaces()
  const data = useData()
  const canI = useCanI()
  const navigate = useNavigate()

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
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3" data-testid="namespaces">
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
      <label className="flex max-w-xs flex-col gap-1 text-sm">
        Go to namespace
        <select
          value=""
          onChange={(event) => event.target.value && navigate(`/${event.target.value}`)}
          className="w-full border p-2 rounded bg-[hsl(var(--input-background))]"
        >
          <option value="">Select a namespace…</option>
          {namespaces.map((namespace) => (
            <option key={namespace} value={namespace}>
              {namespace}
            </option>
          ))}
        </select>
      </label>
    </section>
  )
}
