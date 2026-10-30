import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useNamespaces } from '../../hooks/useNamespaces'
import { nameValidationMessage } from '../../lib/names'
import { Field, TextInput } from './fields'

// Namespace creation (Vue CreateNamespaces): a single name field POSTed to
// /namespaces/{name} with no body. The Vue collision check only warned; here
// it still warns without blocking, matching that behavior.
export default function NamespaceForm() {
  const navigate = useNavigate()
  const namespaces = useNamespaces()
  const { refresh } = useData()
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const nameCollision = namespaces.includes(name)

  const submit = async (event) => {
    event.preventDefault()
    const nameError = nameValidationMessage(name)
    if (nameError) {
      setError(nameError)
      return
    }
    setError('')
    setSubmitting(true)
    try {
      await api.createNamespace(name)
      toast.success(`Namespace "${name}" created successfully`)
      refresh()
      navigate('/')
    } catch (apiError) {
      const detail = apiError instanceof ApiError && apiError.detail ? `: ${apiError.detail}` : ''
      toast.error(`${apiError.message}${detail}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="flex max-w-xl flex-col gap-4 p-8 text-sm">
      <h1 className="text-2xl font-semibold">Create Namespace</h1>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <Field
          label="Namespace Name"
          required
          error={error}
          help="An alphanumeric (and '-') string of up to 63 characters, not starting or ending with '-'"
        >
          <TextInput value={name} onChange={setName} required autoComplete="off" error={error} />
        </Field>
        {nameCollision && (
          <p role="alert">
            There&apos;s already a <strong>Namespace</strong> with the same name. Creating it again
            has no effect.
          </p>
        )}
        <div className="flex gap-3">
          <button type="submit" disabled={submitting} className="border p-2 font-semibold">
            Create Namespace
          </button>
          <button type="button" className="border p-2" onClick={() => navigate('/')}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}
