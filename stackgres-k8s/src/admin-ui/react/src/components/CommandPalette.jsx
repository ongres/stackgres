import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Command } from 'cmdk'
import { Search } from 'lucide-react'
import { useData, LIST_KINDS } from '../contexts/DataContext'
import { useNamespaces } from '../hooks/useNamespaces'
import { detailPath, KINDS, listPath } from '../lib/kinds'

// Global ⌘K / Ctrl+K palette, following the cloud-console pattern: jump to
// any namespace, resource list or resource.
const GROUP_CLASSES =
  '[&_[cmdk-group-heading]]:px-2.5 [&_[cmdk-group-heading]]:py-1.5 [&_[cmdk-group-heading]]:text-[11px] [&_[cmdk-group-heading]]:font-medium [&_[cmdk-group-heading]]:uppercase [&_[cmdk-group-heading]]:tracking-wider [&_[cmdk-group-heading]]:text-[hsl(var(--muted-foreground))]'
export default function CommandPalette() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const namespaces = useNamespaces()
  const data = useData()

  useEffect(() => {
    const onKey = (event) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        setOpen((current) => !current)
      }
      if (event.key === 'Escape') {
        setOpen(false)
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [])

  if (!open) {
    return null
  }

  const go = (path) => {
    setOpen(false)
    navigate(path)
  }

  return (
    <div className="fixed inset-0 z-[100] flex items-start justify-center px-4 pt-[15vh]">
      <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} aria-hidden="true" />
      <Command label="Command palette" className="relative w-[560px] max-w-full overflow-hidden rounded-lg border bg-[hsl(var(--popover))] text-[hsl(var(--popover-foreground))] shadow-2xl">
        <div className="flex items-center gap-2 border-b px-3">
          <Search className="h-4 w-4 shrink-0 text-[hsl(var(--muted-foreground))]" strokeWidth={1.5} />
          <Command.Input autoFocus placeholder="Jump to…" className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:text-[hsl(var(--muted-foreground))]" />
          <kbd className="rounded border px-1.5 py-0.5 text-[10px] text-[hsl(var(--muted-foreground))]">esc</kbd>
        </div>
        <Command.List className="max-h-[320px] overflow-auto p-1.5">
          <Command.Empty className="py-6 text-center text-sm text-[hsl(var(--muted-foreground))]">No results found.</Command.Empty>
          <Command.Group heading="Namespaces" className={GROUP_CLASSES}>
            {namespaces.map((namespace) => (
              <Command.Item key={namespace} value={`namespace ${namespace}`} onSelect={() => go(`/${namespace}`)} className="flex items-center gap-2 px-2.5 py-2 text-sm rounded-md cursor-pointer data-[selected=true]:bg-[hsl(var(--primary)/0.15)] data-[selected=true]:text-[hsl(var(--primary))]">
                {namespace}
              </Command.Item>
            ))}
          </Command.Group>
          {namespaces.map((namespace) => (
            <Command.Group key={`lists-${namespace}`} heading={`Lists in ${namespace}`} className={GROUP_CLASSES}>
              {Object.entries(KINDS).map(([plural, kind]) => (
                <Command.Item
                  key={`${namespace}-${plural}`}
                  value={`${namespace} ${kind.label}`}
                  onSelect={() => go(listPath(plural, namespace))}
                  className="flex items-center gap-2 px-2.5 py-2 text-sm rounded-md cursor-pointer data-[selected=true]:bg-[hsl(var(--primary)/0.15)] data-[selected=true]:text-[hsl(var(--primary))]"
                >
                  {namespace} / {kind.label}
                </Command.Item>
              ))}
            </Command.Group>
          ))}
          <Command.Group heading="Resources" className={GROUP_CLASSES}>
            {LIST_KINDS.flatMap((plural) =>
              (data[plural] || []).map((item) => (
                <Command.Item
                  key={`${plural}-${item.metadata.namespace}-${item.metadata.name}`}
                  value={`${KINDS[plural].crd} ${item.metadata.namespace} ${item.metadata.name}`}
                  onSelect={() => go(detailPath(plural, item.metadata.namespace, item.metadata.name))}
                  className="flex items-center gap-2 px-2.5 py-2 text-sm rounded-md cursor-pointer data-[selected=true]:bg-[hsl(var(--primary)/0.15)] data-[selected=true]:text-[hsl(var(--primary))]"
                >
                  {item.metadata.namespace} / {item.metadata.name}
                  <span className="ml-auto text-[11px] text-[hsl(var(--muted-foreground))]">{KINDS[plural].crd}</span>
                </Command.Item>
              ))
            )}
          </Command.Group>
        </Command.List>
      </Command>
    </div>
  )
}
