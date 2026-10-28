import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Command } from 'cmdk'
import { useData, LIST_KINDS } from '../contexts/DataContext'
import { useNamespaces } from '../hooks/useNamespaces'
import { detailPath, KINDS, listPath } from '../lib/kinds'

// Global ⌘K / Ctrl+K palette, following the cloud-console pattern: jump to
// any namespace, resource list or resource.
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
    <div className="fixed inset-0 z-20 flex items-start justify-center pt-24">
      <div className="absolute inset-0 bg-black/50" onClick={() => setOpen(false)} aria-hidden="true" />
      <Command label="Command palette" className="relative w-full max-w-lg border bg-white text-sm">
        <Command.Input autoFocus placeholder="Jump to…" className="w-full border-b p-3 outline-none" />
        <Command.List className="max-h-80 overflow-y-auto p-2">
          <Command.Empty className="p-2">No results found.</Command.Empty>
          <Command.Group heading="Namespaces">
            {namespaces.map((namespace) => (
              <Command.Item key={namespace} value={`namespace ${namespace}`} onSelect={() => go(`/${namespace}`)} className="cursor-pointer p-2 data-[selected=true]:underline">
                {namespace}
              </Command.Item>
            ))}
          </Command.Group>
          {namespaces.map((namespace) => (
            <Command.Group key={`lists-${namespace}`} heading={`Lists in ${namespace}`}>
              {Object.entries(KINDS).map(([plural, kind]) => (
                <Command.Item
                  key={`${namespace}-${plural}`}
                  value={`${namespace} ${kind.label}`}
                  onSelect={() => go(listPath(plural, namespace))}
                  className="cursor-pointer p-2 data-[selected=true]:underline"
                >
                  {namespace} / {kind.label}
                </Command.Item>
              ))}
            </Command.Group>
          ))}
          <Command.Group heading="Resources">
            {LIST_KINDS.flatMap((plural) =>
              (data[plural] || []).map((item) => (
                <Command.Item
                  key={`${plural}-${item.metadata.namespace}-${item.metadata.name}`}
                  value={`${KINDS[plural].crd} ${item.metadata.namespace} ${item.metadata.name}`}
                  onSelect={() => go(detailPath(plural, item.metadata.namespace, item.metadata.name))}
                  className="cursor-pointer p-2 data-[selected=true]:underline"
                >
                  {item.metadata.namespace} / {item.metadata.name}
                  <span className="ml-2 text-xs">{KINDS[plural].crd}</span>
                </Command.Item>
              ))
            )}
          </Command.Group>
        </Command.List>
      </Command>
    </div>
  )
}
