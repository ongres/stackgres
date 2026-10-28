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
      <button type="button" onClick={show} className="underline">
        Clone
      </button>
      {open && (
        <div role="dialog" aria-label="Clone resource" className="fixed inset-0 z-10 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={close} aria-hidden="true" />
          <form onSubmit={submit} className="relative flex max-w-md flex-col gap-3 border bg-white p-6 text-sm">
            <h2 className="text-lg font-semibold">Clone {item.metadata.name}</h2>
            {kind === 'sgclusters' && (
              <p>Only the cluster specification is cloned; no source data is copied.</p>
            )}
            <label className="flex flex-col gap-1">
              Namespace
              <select value={namespace} onChange={(event) => setNamespace(event.target.value)} className="border p-2">
                {namespaces.map((candidate) => (
                  <option key={candidate}>{candidate}</option>
                ))}
              </select>
            </label>
            <label className="flex flex-col gap-1">
              Name
              <input value={name} onChange={(event) => setName(event.target.value)} className="border p-2" />
            </label>
            {collision && <p role="alert">A resource with this name already exists in the target namespace</p>}
            {error && !collision && <p role="alert">{error}</p>}
            <div className="flex gap-3">
              <button type="submit" disabled={collision} className="border p-2 font-semibold">
                Clone
              </button>
              <button type="button" onClick={close} className="border p-2">
                Cancel
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  )
}
