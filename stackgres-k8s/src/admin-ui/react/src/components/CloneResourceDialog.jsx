import { useState } from 'react'
import { toast } from 'sonner'
import { api } from '../lib/api'
import { useData } from '../contexts/DataContext'
import { useNamespaces } from '../hooks/useNamespaces'
import { buildClonePayload } from '../lib/payloads'
import { nameValidationMessage } from '../lib/names'

// Clone dialog ported from the Vue NavBar #clone modal: pick a target
// namespace and name, POST a deep copy of the resource.
export default function CloneResourceDialog({ kind, item }) {
  const namespaces = useNamespaces()
  const { refresh, ...caches } = useData()
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [namespace, setNamespace] = useState(item.metadata.namespace)
  const [error, setError] = useState('')

  const show = () => {
    setName(`copy-of-${item.metadata.name}`)
    setNamespace(item.metadata.namespace)
    setError('')
    setOpen(true)
  }
  const close = () => setOpen(false)

  const collision = (caches[kind] || []).some(
    (candidate) => candidate.metadata.name === name && candidate.metadata.namespace === namespace
  )

  const submit = async (event) => {
    event.preventDefault()
    const nameError = nameValidationMessage(name)
    if (nameError) {
      setError(nameError)
      return
    }
    if (collision) {
      setError(`A resource named "${name}" already exists in this namespace`)
      return
    }
    const payload = buildClonePayload(kind, item, { name, namespace }, item.metadata.name)
    try {
      await api.create(kind, payload)
      toast.success(`Resource "${name}" cloned successfully`)
      refresh()
      close()
    } catch (apiError) {
      toast.error(apiError.message)
      close()
    }
  }

  return (
    <>
      <button type="button" onClick={show} className="text-sm text-[hsl(var(--primary))] hover:underline">
        Clone
      </button>
      {open && (
        <div role="dialog" aria-label="Clone resource" className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={close} aria-hidden="true" />
          <form onSubmit={submit} className="relative mx-4 flex w-full max-w-md flex-col gap-3 rounded-lg border bg-[hsl(var(--card))] p-6 text-sm shadow-lg">
            <h2 className="text-lg font-semibold">Clone {item.metadata.name}</h2>
            {kind === 'sgclusters' && (
              <p>Only the cluster specification is cloned; no source data is copied.</p>
            )}
            <label className="flex flex-col gap-1">
              Namespace
              <select value={namespace} onChange={(event) => setNamespace(event.target.value)} className="w-full border p-2 rounded bg-[hsl(var(--input-background))]">
                {namespaces.map((candidate) => (
                  <option key={candidate}>{candidate}</option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              Name
              <input value={name} onChange={(event) => setName(event.target.value)} className="w-full border p-2 rounded bg-[hsl(var(--input-background))]" />
            </label>
            {collision && <p role="alert" className="text-sm text-red-500">A resource with this name already exists in the target namespace</p>}
            {error && !collision && <p role="alert" className="text-sm text-red-500">{error}</p>}
            <div className="flex gap-3">
              <button type="submit" disabled={collision} className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]">
                Clone
              </button>
              <button type="button" onClick={close} className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]">
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  )
}
