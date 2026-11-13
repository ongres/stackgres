import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from '../lib/api'
import { useData } from '../contexts/DataContext'

// Type-the-name delete confirmation, ported from the Vue #delete modal.
// `onDeleted` lets hosts outside the namespaced data caches (users, roles)
// refresh their own lists after the delete.
export default function DeleteResourceDialog({ kind, namespace, name, redirect, onDeleted }) {
  const navigate = useNavigate()
  const { refresh } = useData()
  const [open, setOpen] = useState(false)
  const [confirmName, setConfirmName] = useState('')
  const [mismatch, setMismatch] = useState(false)

  const close = () => {
    setOpen(false)
    setConfirmName('')
    setMismatch(false)
  }

  const confirm = async (event) => {
    event.preventDefault()
    if (confirmName !== name) {
      setMismatch(true)
      return
    }
    try {
      await api.remove(kind, { metadata: { name, namespace } })
      toast.success(`Resource "${name}" deleted successfully`)
      refresh()
      if (onDeleted) {
        onDeleted()
      }
      close()
      if (redirect) {
        navigate(redirect)
      }
    } catch (error) {
      toast.error(error.message)
    }
  }

  return (
    <>
      <button type="button" onClick={() => setOpen(true)} className="text-sm text-[hsl(var(--primary))] hover:underline">
        Delete
      </button>
      {open && (
        <div role="dialog" aria-label="Delete resource" className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={close} aria-hidden="true" />
          <form onSubmit={confirm} className="relative mx-4 flex w-full max-w-md flex-col gap-3 rounded-lg border bg-[hsl(var(--card))] p-6 text-sm shadow-lg">
            <h2 className="text-lg font-semibold">This action is permanent</h2>
            <p>
              Are you sure you want to delete the {kind} <strong>{name}</strong>? Please type the
              exact name of the {kind} to confirm.
            </p>
            <input
              value={confirmName}
              onChange={(event) => setConfirmName(event.target.value)}
              aria-label="Confirm resource name"
              className="w-full border p-2 rounded bg-[hsl(var(--input-background))]"
            />
            {mismatch && (
              <p role="alert" className="text-sm text-red-500">The name does not match the resource you want to delete</p>
            )}
            <div className="flex gap-3">
              <button type="submit" className="px-4 py-2 bg-red-600 text-white rounded text-sm font-medium">
                Delete Item
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
