import { Link, useParams } from 'react-router-dom'

export default function Breadcrumbs() {
  const { namespace, kind, name } = useParams()

  if (!namespace) {
    return null
  }

  return (
    <nav aria-label="Breadcrumb" className="p-4 text-sm">
      <ol className="flex items-center gap-2">
        <li>
          <Link to={`/${namespace}`}>{namespace}</Link>
        </li>
        {kind && (
          <li className="flex items-center gap-2">
            <span aria-hidden="true">/</span>
            <Link to={`/${namespace}/${kind}`}>{kind}</Link>
          </li>
        )}
        {name && (
          <li className="flex items-center gap-2">
            <span aria-hidden="true">/</span>
            <span aria-current="page">{name}</span>
          </li>
        )}
      </ol>
    </nav>
  )
}
