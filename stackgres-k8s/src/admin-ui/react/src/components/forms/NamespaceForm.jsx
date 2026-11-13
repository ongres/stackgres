import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useNamespaces } from '../../hooks/useNamespaces'
import { nameValidationMessage } from '../../lib/names'
import { HelpTooltip } from '../HelpTooltip'
import { Field, FieldGroup, FieldRow, TextInput } from './fields'

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
    <section className="flex max-w-xl flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">Create Namespace</h1>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <FieldGroup>
          <FieldRow>
            <Field
              label="Namespace Name"
              required
              error={error}
              help="An alphanumeric (and '-') string of up to 63 characters, not starting or ending with '-'"
              tooltip={
                <HelpTooltip text={'Namespace name. Following Kubernetes naming conventions, it must be an alphanumeric (a-z, and 0-9) string, with a maximum length of 63 characters, with the - character allowed anywhere except the first or last character.'} />
              }
            >
              <TextInput value={name} onChange={setName} required autoComplete="off" error={error} />
            </Field>
          </FieldRow>
          {nameCollision && (
            <p role="alert" className="text-sm text-red-500">
              There&apos;s already a <strong>Namespace</strong> with the same name. Creating it again
              has no effect.
            </p>
          )}
        </FieldGroup>
        <div className="flex gap-3">
          <button type="submit" disabled={submitting} className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]">
            Create Namespace
          </button>
          <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" onClick={() => navigate('/')}>
            Cancel
          </button>
        </div>
      </form>
    </section>
  )
}
