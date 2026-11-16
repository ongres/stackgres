import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useParams } from 'react-router-dom'
import {
  BookOpen,
  LifeBuoy,
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
} from 'lucide-react'
import { RESOURCE_ICONS } from './resourceIcons'
import { api } from '../../lib/api'
import { useKindList } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { detailPath } from '../../lib/kinds'

// Cloud-console sidebar: a 56px icon rail plus a 256px contextual tree panel
// for the active section; collapsing hides the panel (persisted, forced below
// 1024px) and hovering a rail item then shows the panel as a flyout.
const SECTIONS = [
  { id: 'sgclusters', label: 'StackGres Clusters', kind: 'sgclusters', submenu: true, icon: RESOURCE_ICONS.sgclusters },
  { id: 'sgshardedclusters', label: 'StackGres Sharded Clusters', kind: 'sgshardedclusters', submenu: true, icon: RESOURCE_ICONS.sgshardedclusters },
  { id: 'sgstreams', label: 'StackGres Streams', kind: 'sgstreams', submenu: true, icon: RESOURCE_ICONS.sgstreams },
  { id: 'sginstanceprofiles', label: 'Instance Profiles', kind: 'sginstanceprofiles', submenu: true, icon: RESOURCE_ICONS.sginstanceprofiles },
  {
    id: 'configurations',
    label: 'Configurations',
    icon: RESOURCE_ICONS.configurations,
    children: [
      { label: 'Postgres', kind: 'sgpgconfigs', submenu: true, icon: RESOURCE_ICONS.sgpgconfigs },
      { label: 'Connection Pooling', kind: 'sgpoolconfigs', submenu: true, icon: RESOURCE_ICONS.sgpoolconfigs },
      { label: 'Object Storages', kind: 'sgobjectstorages', submenu: true, icon: RESOURCE_ICONS.sgobjectstorages },
      { label: 'Scripts', kind: 'sgscripts', submenu: true, icon: RESOURCE_ICONS.sgscripts },
    ],
  },
  { id: 'sgdistributedlogs', label: 'Logs Servers', kind: 'sgdistributedlogs', submenu: true, icon: RESOURCE_ICONS.sgdistributedlogs },
  { id: 'sgbackups', label: 'Cluster Backups', kind: 'sgbackups', submenu: false, icon: RESOURCE_ICONS.sgbackups },
  { id: 'sgdbops', label: 'Database Operations', kind: 'sgdbops', submenu: false, icon: RESOURCE_ICONS.sgdbops },
]

const COLLAPSED_KEY = 'sgSidebarCollapsed'

const railButtonClasses = (active) =>
  `flex flex-col items-center justify-center w-10 h-10 rounded-md transition-colors ${
    active
      ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))]'
      : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--foreground)/0.06)]'
  }`

const subItemClasses = ({ isActive }) =>
  `text-xs truncate flex items-center gap-1 px-1.5 py-0.5 rounded-md transition-colors hover:bg-[hsl(var(--foreground)/0.06)] hover:text-[hsl(var(--foreground))] ${
    isActive
      ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))] font-medium'
      : 'text-[hsl(var(--muted-foreground))]'
  }`

// The cloud sidebar's ASCII-tree connectors.
const treeItemClasses = (isLast) =>
  `relative min-w-0 pl-3 pt-0.5 pb-0.5 before:content-[''] before:absolute before:left-0 before:top-1/2 before:w-2.5 before:border-t before:border-[hsl(var(--border))] ${
    isLast
      ? "after:content-[''] after:absolute after:left-[-1px] after:top-1/2 after:bottom-0 after:w-[1px] after:bg-[hsl(var(--surface-nav))]"
      : ''
  }`

function sectionForPath(pathname, namespace) {
  for (const section of SECTIONS) {
    const kinds = section.children ? section.children.map((child) => child.kind) : [section.kind]
    for (const kind of kinds) {
      if (
        pathname.startsWith(`/${namespace}/${kind}`) ||
        pathname.startsWith(`/${namespace}/${kind.slice(0, -1)}/`)
      ) {
        return section.id
      }
    }
  }
  if (pathname.startsWith(`/${namespace}/application/`)) {
    return 'applications'
  }
  return null
}

function KindTree({ namespace, kind, submenu }) {
  const canI = useCanI()
  const items = useKindList(kind, namespace)
  const canCreate = canI('create', kind, namespace)

  if (!canCreate && (!submenu || !items || !items.length)) {
    return <p className="ml-2 pt-1 text-xs text-[hsl(var(--muted-foreground))] italic">No entries</p>
  }

  return (
    <ul className="ml-2 border-l border-[hsl(var(--border))]">
      {canCreate && (
        <li className={treeItemClasses(!submenu || !items || items.length === 0)}>
          <Link to={`/${namespace}/${kind}/new`}
            className="flex items-center gap-1 rounded-md px-1.5 py-0.5 text-xs text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]">
            <Plus className="h-3 w-3 shrink-0" strokeWidth={1.5} /> Add New
          </Link>
        </li>
      )}
      {submenu &&
        (items || []).map((item, index) => (
          <li key={item.metadata.name} className={treeItemClasses(index === items.length - 1)}>
            <NavLink to={detailPath(kind, namespace, item.metadata.name)} className={subItemClasses}>
              <span className="truncate">{item.metadata.name}</span>
            </NavLink>
          </li>
        ))}
    </ul>
  )
}

function SectionPanel({ namespace, section, applications }) {
  const canI = useCanI()

  return (
    <div className="w-64 border-r flex flex-col h-full bg-[hsl(var(--surface-nav))] text-[hsl(var(--surface-nav-foreground))]">
      <div className="min-h-[60px] border-b px-4 py-2 flex items-center gap-1">
        {section.id === 'applications' ? (
          <span className="flex-1 px-1 py-0.5 text-sm font-semibold">Applications</span>
        ) : section.children ? (
          <span className="flex-1 px-1 py-0.5 text-sm font-semibold">{section.label}</span>
        ) : (
          <Link to={`/${namespace}/${section.kind}`}
            className="flex-1 rounded-md px-1 py-0.5 text-sm font-semibold transition-colors hover:bg-[hsl(var(--foreground)/0.06)]">
            {section.label}
          </Link>
        )}
      </div>
      <div className="relative flex-1 overflow-auto px-3 pb-3 pt-2">
        {section.id === 'applications' ? (
          <ul className="flex flex-col gap-0.5">
            {(applications || []).map((application) => (
              <li key={application.name}>
                <NavLink to={`/${namespace}/application/${application.name}`} className={subItemClasses}>
                  <RESOURCE_ICONS.applications className="h-3.5 w-3.5 shrink-0" />
                  <span className="truncate">{application.name}</span>
                </NavLink>
              </li>
            ))}
          </ul>
        ) : section.children ? (
          <div className="flex flex-col gap-3">
            {section.children
              .filter((child) => canI('any', child.kind, namespace))
              .map((child) => (
                <div key={child.kind}>
                  <Link to={`/${namespace}/${child.kind}`}
                    className="flex items-center gap-1.5 rounded-md px-1 py-0.5 text-sm font-semibold transition-colors hover:bg-[hsl(var(--foreground)/0.06)]">
                    <child.icon className="h-3.5 w-3.5 shrink-0 text-[hsl(var(--muted-foreground))]" />
                    {child.label}
                  </Link>
                  <KindTree namespace={namespace} kind={child.kind} submenu={child.submenu} />
                </div>
              ))}
          </div>
        ) : (
          <KindTree namespace={namespace} kind={section.kind} submenu={section.submenu} />
        )}
      </div>
    </div>
  )
}

export default function Sidebar() {
  const { namespace } = useParams()
  const { pathname } = useLocation()
  const canI = useCanI()
  const [applications, setApplications] = useState(null)
  const [collapsed, setCollapsed] = useState(() => {
    try {
      if (window.matchMedia('(max-width: 1023px)').matches) return true
      // Rail-only by default; the tree panel is opt-in and persisted.
      return localStorage.getItem(COLLAPSED_KEY) !== 'false'
    } catch {
      return true
    }
  })
  const [flyout, setFlyout] = useState(null)

  useEffect(() => {
    api
      .get('applications')
      .then((response) => setApplications(response?.applications || []))
      .catch(() => setApplications([]))
  }, [])

  // Force-collapse on narrow viewports, as the cloud console does.
  useEffect(() => {
    const media = window.matchMedia('(max-width: 1023px)')
    const onChange = () => {
      if (media.matches) setCollapsed(true)
    }
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [])

  useEffect(() => {
    document.documentElement.classList.toggle('sidebar-collapsed', collapsed)
  }, [collapsed])

  const toggleCollapsed = () => {
    setCollapsed((current) => {
      try {
        localStorage.setItem(COLLAPSED_KEY, String(!current))
      } catch {
        /* storage may be unavailable */
      }
      return !current
    })
    setFlyout(null)
  }

  const activeId = sectionForPath(pathname, namespace)
  const visibleSections = SECTIONS.filter((section) =>
    section.children
      ? section.children.some((child) => canI('any', child.kind, namespace))
      : canI('any', section.kind, namespace)
  )
  const hasApplications = Boolean(applications && applications.length)
  const activeSection =
    activeId === 'applications'
      ? { id: activeId }
      : visibleSections.find((section) => section.id === activeId) || visibleSections[0]
  const flyoutSection =
    flyout === 'applications'
      ? { id: flyout }
      : SECTIONS.find((section) => section.id === flyout)

  return (
    <nav
      role="navigation"
      aria-label="Resources"
      className="relative flex h-full"
      onMouseLeave={() => setFlyout(null)}
    >
      <div className="w-14 border-r flex flex-col items-center bg-[hsl(var(--surface-nav))] text-[hsl(var(--surface-nav-foreground))]">
        <div className="flex flex-col items-center gap-1 py-3">
          {visibleSections.map((section) => (
            <NavLink
              key={section.id}
              to={`/${namespace}/${section.children ? section.children[0].kind : section.kind}`}
              title={section.label}
              aria-label={section.label}
              className={() => railButtonClasses(activeId === section.id)}
              onMouseEnter={() => (collapsed ? setFlyout(section.id) : null)}
            >
              <section.icon className="h-5 w-5" />
            </NavLink>
          ))}
          {hasApplications && (
            <>
              <hr className="w-6 my-1 border-t border-[hsl(var(--border))]" />
              <NavLink
                to={`/${namespace}/application/${applications[0].name}`}
                title="Applications"
                aria-label="Applications"
                className={() => railButtonClasses(activeId === 'applications')}
                onMouseEnter={() => (collapsed ? setFlyout('applications') : null)}
              >
                <RESOURCE_ICONS.applications className="h-5 w-5" />
              </NavLink>
            </>
          )}
        </div>
        <div className="mt-auto flex flex-col items-center gap-1 py-3 border-t w-full">
          <a href="https://stackgres.io/doc/" target="_blank" rel="noreferrer" title="Documentation"
            aria-label="Documentation" className={railButtonClasses(false)}>
            <BookOpen className="h-5 w-5" strokeWidth={1.5} />
          </a>
          <a href="https://stackgres.io/support/" target="_blank" rel="noreferrer" title="Support"
            aria-label="Support" className={railButtonClasses(false)}>
            <LifeBuoy className="h-5 w-5" strokeWidth={1.5} />
          </a>
          <button type="button" onClick={toggleCollapsed}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            className={railButtonClasses(false)}>
            {collapsed ? (
              <PanelLeftOpen className="h-5 w-5" strokeWidth={1.5} />
            ) : (
              <PanelLeftClose className="h-5 w-5" strokeWidth={1.5} />
            )}
          </button>
        </div>
      </div>

      {!collapsed && activeSection && (
        <SectionPanel namespace={namespace} section={activeSection} applications={applications} />
      )}

      {collapsed && flyoutSection && (
        <div className="absolute left-14 top-0 h-full z-40 shadow-2xl" data-tree-flyout>
          <SectionPanel namespace={namespace} section={flyoutSection} applications={applications} />
        </div>
      )}
    </nav>
  )
}
