import { useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ChevronDown, Plus } from 'lucide-react'
import { useCanI } from '../../contexts/PermissionsContext'
import { useNamespaces } from '../../hooks/useNamespaces'

// Global namespace switcher, centered in the top bar. On namespace-less
// routes (global dashboard, user management) it reads "All namespaces".
export default function NamespaceSelector() {
  const { namespace } = useParams()
  const namespaces = useNamespaces()
  const canI = useCanI()
  const [open, setOpen] = useState(false)
  const wrapRef = useRef(null)

  useEffect(() => {
    if (!open) {
      return undefined
    }
    const onMouseDown = (event) => {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) {
        setOpen(false)
      }
    }
    const onKeyDown = (event) => {
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onMouseDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onMouseDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  return (
    <div className="relative" ref={wrapRef}>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className="flex w-full min-w-[220px] max-w-[320px] items-center justify-between gap-2 rounded-md border px-2.5 py-1.5 text-left text-sm transition-colors hover:bg-[hsl(var(--foreground)/0.06)]"
      >
        {namespace ? (
          <span className="flex min-w-0 items-baseline gap-1">
            <span className="shrink-0 text-[hsl(var(--muted-foreground))]">Namespace:</span>
            <strong className="truncate font-semibold">{namespace}</strong>
          </span>
        ) : (
          <strong className="truncate font-semibold">All namespaces</strong>
        )}
        <ChevronDown className="h-4 w-4 shrink-0 text-[hsl(var(--muted-foreground))]" strokeWidth={1.5} />
      </button>
      {open && (
        <ul className="absolute left-0 top-full z-50 mt-1 max-h-[60vh] w-full min-w-[220px] overflow-auto rounded-md border bg-[hsl(var(--card))] p-1 text-sm shadow-lg">
          <li>
            <Link to="/" onClick={() => setOpen(false)}
              className="block rounded-md px-1.5 py-1 font-semibold transition-colors hover:bg-[hsl(var(--foreground)/0.06)]">
              All namespaces
            </Link>
          </li>
          {namespaces.map((candidate) => (
            <li key={candidate}>
              <Link to={`/${candidate}`} onClick={() => setOpen(false)}
                className="block truncate rounded-md px-1.5 py-1 transition-colors hover:bg-[hsl(var(--foreground)/0.06)]">
                {candidate}
              </Link>
            </li>
          ))}
          {canI('create', 'namespaces') && (
            <li>
              <Link to="/namespaces/new" onClick={() => setOpen(false)}
                className="flex items-center gap-1 rounded-md px-1.5 py-1 text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]">
                <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> New namespace
              </Link>
            </li>
          )}
        </ul>
      )}
    </div>
  )
}
