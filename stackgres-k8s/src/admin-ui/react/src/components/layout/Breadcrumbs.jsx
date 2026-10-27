import { Link, useLocation } from 'react-router-dom'

// Breadcrumbs from the path segments: namespace / kind / name / section.
export default function Breadcrumbs() {
  const { pathname } = useLocation()
  const segments = pathname.split('/').filter(Boolean)

  if (!segments.length) {
    return null
  }

  const crumbs = segments.map((segment, index) => ({
    label: segment,
    to: '/' + segments.slice(0, index + 1).join('/'),
  }))

  return (
    <nav aria-label="Breadcrumb" className="p-4 text-sm">
      <ol className="flex flex-wrap items-center gap-2">
        {crumbs.map((crumb, index) => (
          <li key={crumb.to} className="flex items-center gap-2">
            {index > 0 && <span aria-hidden="true">/</span>}
            {index < crumbs.length - 1 ? (
              <Link to={crumb.to} className="underline">
                {crumb.label}
              </Link>
            ) : (
              <span aria-current="page">{crumb.label}</span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
}
