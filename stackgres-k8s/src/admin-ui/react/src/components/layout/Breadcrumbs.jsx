import { Link, useLocation } from 'react-router-dom'
import { ChevronRight, Home } from 'lucide-react'
import { RESOURCE_ICONS } from './resourceIcons'

// Detail routes use the singular kind segment (/:namespace/sgcluster/:name);
// render those crumbs as their plural list kind, linking to the list route.
// (sgconfig stays singular: it has no list route.)
const SINGULAR_TO_PLURAL = {
  sgcluster: 'sgclusters',
  sgshardedcluster: 'sgshardedclusters',
  sgstream: 'sgstreams',
  sginstanceprofile: 'sginstanceprofiles',
  sgpgconfig: 'sgpgconfigs',
  sgpoolconfig: 'sgpoolconfigs',
  sgobjectstorage: 'sgobjectstorages',
  sgscript: 'sgscripts',
  sgdistributedlog: 'sgdistributedlogs',
  sgbackup: 'sgbackups',
}

const SEGMENT_ICONS = {
  ...RESOURCE_ICONS,
  sgconfig: RESOURCE_ICONS.configurations,
}

// Breadcrumbs from the path segments: namespace / kind / name / section.
export default function Breadcrumbs() {
  const { pathname } = useLocation()
  const segments = pathname.split('/').filter(Boolean)

  if (!segments.length) {
    return null
  }

  const crumbs = segments.map((segment, index) => {
    const plural = SINGULAR_TO_PLURAL[segment]
    return {
      label: plural || segment,
      to: plural
        ? '/' + [...segments.slice(0, index), plural].join('/')
        : '/' + segments.slice(0, index + 1).join('/'),
      Icon: SEGMENT_ICONS[plural || segment],
    }
  })

  return (
    <nav
      aria-label="Breadcrumb"
      className="min-h-[60px] flex items-center overflow-x-auto whitespace-nowrap px-6 py-2 text-sm"
    >
      <ol className="flex items-center gap-1.5">
        <li className="flex items-center">
          <Link to="/" className="text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]" title="Home">
            <Home className="h-4 w-4" strokeWidth={1.5} />
          </Link>
        </li>
        {crumbs.map((crumb, index) => (
          <li key={crumb.to} className="flex items-center gap-1.5">
            <ChevronRight className="h-3.5 w-3.5 shrink-0 text-[hsl(var(--muted-foreground))]" strokeWidth={1.5} aria-hidden="true" />
            {index < crumbs.length - 1 ? (
              <Link to={crumb.to} className="flex shrink-0 items-center gap-1.5 text-[hsl(var(--primary))] hover:underline">
                {crumb.Icon && <crumb.Icon className="h-4 w-4 shrink-0" aria-hidden="true" />}
                {crumb.label}
              </Link>
            ) : (
              <span aria-current="page" className="flex items-center gap-1.5 truncate font-semibold">
                {crumb.Icon && <crumb.Icon className="h-4 w-4 shrink-0" aria-hidden="true" />}
                {crumb.label}
              </span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
}
