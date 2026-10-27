import { useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { ChevronDown, Plus } from 'lucide-react'
import { useKindList } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { useNamespaces } from '../../hooks/useNamespaces'
import { detailPath } from '../../lib/kinds'

const SECTIONS = [
  { label: 'StackGres Clusters', kind: 'sgclusters', submenu: true },
  { label: 'StackGres Sharded Clusters', kind: 'sgshardedclusters', submenu: true },
  { label: 'StackGres Streams', kind: 'sgstreams', submenu: true },
  { label: 'Instance Profiles', kind: 'sginstanceprofiles', submenu: true },
  {
    label: 'Configurations',
    children: [
      { label: 'Postgres', kind: 'sgpgconfigs', submenu: true },
      { label: 'Connection Pooling', kind: 'sgpoolconfigs', submenu: true },
      { label: 'Object Storages', kind: 'sgobjectstorages', submenu: true },
      { label: 'Scripts', kind: 'sgscripts', submenu: true },
    ],
  },
  { label: 'Logs Servers', kind: 'sgdistributedlogs', submenu: true },
  { label: 'Cluster Backups', kind: 'sgbackups', submenu: false },
  { label: 'Database Operations', kind: 'sgdbops', submenu: false },
]

function SectionLink({ namespace, label, kind, submenu }) {
  const canI = useCanI()
  const items = useKindList(kind, namespace)

  if (!canI('any', kind, namespace)) {
    return null
  }

  return (
    <li>
      <div className="flex items-center justify-between gap-2">
        {canI('list', kind, namespace) ? (
          <NavLink to={`/${namespace}/${kind}`} end className="flex-1 py-1">
            {label}
          </NavLink>
        ) : (
          <span className="flex-1 py-1">{label}</span>
        )}
        {canI('create', kind, namespace) && (
          <Link
            to={`/${namespace}/${kind}/new`}
            title={`Create ${label}`}
            aria-label={`Create ${label}`}
          >
            <Plus size={16} />
          </Link>
        )}
      </div>
      {submenu && items && items.length > 0 && (
        <ul className="flex flex-col border-l pl-3">
          {items.map((item) => (
            <li key={item.metadata.name}>
              <NavLink to={detailPath(kind, namespace, item.metadata.name)} className="block py-0.5">
                {item.metadata.name}
              </NavLink>
            </li>
          ))}
        </ul>
      )}
    </li>
  )
}

export default function Sidebar() {
  const { namespace } = useParams()
  const namespaces = useNamespaces()
  const canI = useCanI()
  const [namespaceSelectorOpen, setNamespaceSelectorOpen] = useState(false)

  return (
    <nav aria-label="Resources" className="flex w-64 shrink-0 flex-col gap-4 border-r p-4">
      <div>
        <button
          type="button"
          onClick={() => setNamespaceSelectorOpen(!namespaceSelectorOpen)}
          aria-expanded={namespaceSelectorOpen}
          className="flex w-full items-center justify-between gap-2 border p-2 text-left text-sm"
        >
          <span>
            <span className="block text-xs uppercase">Namespace</span>
            <strong>{namespace}</strong>
          </span>
          <ChevronDown size={16} />
        </button>
        {namespaceSelectorOpen && (
          <ul className="border border-t-0 p-2 text-sm">
            <li>
              <Link to="/" onClick={() => setNamespaceSelectorOpen(false)} className="block py-1 font-semibold">
                All namespaces
              </Link>
            </li>
            {namespaces.map((candidate) => (
              <li key={candidate}>
                <Link
                  to={`/${candidate}`}
                  onClick={() => setNamespaceSelectorOpen(false)}
                  className="block py-1"
                >
                  {candidate}
                </Link>
              </li>
            ))}
            {canI('create', 'namespaces') && (
              <li>
                <Link
                  to="/namespaces/new"
                  onClick={() => setNamespaceSelectorOpen(false)}
                  className="block py-1"
                >
                  + New namespace
                </Link>
              </li>
            )}
          </ul>
        )}
      </div>

      <ul className="flex flex-col gap-1 text-sm">
        {SECTIONS.map((section) =>
          section.children ? (
            <li key={section.label} className="mt-2">
              <h3 className="text-xs font-semibold uppercase">{section.label}</h3>
              <ul className="flex flex-col gap-1 pl-2">
                {section.children.map((child) => (
                  <SectionLink key={child.kind} namespace={namespace} {...child} />
                ))}
              </ul>
            </li>
          ) : (
            <SectionLink key={section.kind} namespace={namespace} {...section} />
          )
        )}
      </ul>
    </nav>
  )
}
