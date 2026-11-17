import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { Link, useLocation } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import { RESOURCE_ICONS } from './resourceIcons'

// Views portal their page actions (Edit, Clone, Delete…) into the right side
// of the breadcrumb row through this slot.
export function BreadcrumbActions({ children }) {
  const [target, setTarget] = useState(null)

  useEffect(() => {
    setTarget(document.getElementById('breadcrumb-actions'))
  }, [])

  return target ? createPortal(children, target) : null
}

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

  // Detail pages end at the tab name: monitoring picks (pod, dashboard,
  // range) are view state, not locations, and the implicit Status tab gets a
  // synthesized crumb on the bare detail path. The namespace home likewise
  // synthesizes an overview crumb so the trail is never a lone name.
  const detailKind = SINGULAR_TO_PLURAL[segments[1]]
  let shown = segments
  let syntheticTab = null
  if (segments.length === 1) {
    syntheticTab = 'overview'
  } else if (detailKind && segments.length === 3) {
    syntheticTab = 'status'
  } else if (detailKind && segments[3] === 'monitor') {
    shown = segments.slice(0, 4)
  }

  const crumbs = shown.map((segment, index) => {
    const plural = SINGULAR_TO_PLURAL[segment]
    return {
      label: plural || segment,
      to: plural
        ? '/' + [...segments.slice(0, index), plural].join('/')
        : '/' + segments.slice(0, index + 1).join('/'),
      Icon: SEGMENT_ICONS[plural || segment],
    }
  })

  if (syntheticTab) {
    crumbs.push({ label: syntheticTab, to: '/' + segments.join('/') + '#' + syntheticTab })
  }

  return (
    <nav
      aria-label="Breadcrumb"
      className="min-h-[60px] border-b flex items-center overflow-x-auto whitespace-nowrap px-6 py-2 text-sm"
    >
      <ol className="flex items-center gap-1.5">
        {crumbs.map((crumb, index) => (
          <li key={crumb.to} className="flex items-center gap-1.5">
            {index > 0 && (
              <ChevronRight className="h-3.5 w-3.5 shrink-0 text-[hsl(var(--muted-foreground))]" strokeWidth={1.5} aria-hidden="true" />
            )}
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
      <div id="breadcrumb-actions" className="ml-auto flex items-center gap-3 pl-4 text-sm" />
    </nav>
  )
}
