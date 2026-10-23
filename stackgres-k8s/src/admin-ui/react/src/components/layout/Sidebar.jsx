import { useState } from 'react'
import { Link, NavLink, useParams } from 'react-router-dom'
import { ChevronDown, Plus } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { useNamespaces } from '../../hooks/useNamespaces'

const SECTIONS = [
  { label: 'StackGres Clusters', kind: 'sgclusters' },
  { label: 'StackGres Sharded Clusters', kind: 'sgshardedclusters' },
  { label: 'StackGres Streams', kind: 'sgstreams' },
  { label: 'Instance Profiles', kind: 'sginstanceprofiles' },
  {
    label: 'Configurations',
    children: [
      { label: 'Postgres', kind: 'sgpgconfigs' },
      { label: 'Connection Pooling', kind: 'sgpoolconfigs' },
      { label: 'Object Storages', kind: 'sgobjectstorages' },
      { label: 'Scripts', kind: 'sgscripts' },
    ],
  },
  { label: 'Logs Servers', kind: 'sgdistributedlogs' },
  { label: 'Cluster Backups', kind: 'sgbackups' },
  { label: 'Database Operations', kind: 'sgdbops' },
]

function SectionLink({ namespace, label, kind }) {
  const canI = useCanI()

  if (!canI('any', kind, namespace)) {
    return null
  }

  return (
    <li className="flex items-center justify-between gap-2">
      {canI('list', kind, namespace) ? (
        <NavLink to={`/${namespace}/${kind}`} className="flex-1 py-1">
          {label}
        </NavLink>
      ) : (
        <span className="flex-1 py-1">{label}</span>
      )}
      {canI('create', kind, namespace) && (
        <Link to={`/${namespace}/${kind}/new`} title={`Create ${label}`} aria-label={`Create ${label}`}>
          <Plus size={16} />
        </Link>
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
