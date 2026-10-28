import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from '../lib/api'
import { useData } from '../contexts/DataContext'

// Shared submit flow for the CRD create/edit forms: client-side validation,
// name-collision check against the cached list, create vs update, API error
// reporting, data refresh and redirect — the React replacement for the Vue
// mixin's checkRequired/nameColission/notify machinery.
export function useCrdForm({ kind, isEdit, existing, buildPayload, validate, successPath }) {
  const navigate = useNavigate()
  const { refresh, ...caches } = useData()
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [preview, setPreview] = useState(null)

  const submit = useCallback(async () => {
    const payload = buildPayload()
    const validationErrors = validate ? validate(payload) : {}

    if (!isEdit) {
      const list = caches[kind] || []
      const collision = list.some(
        (item) =>
          item.metadata.name === payload.metadata.name &&
          item.metadata.namespace === payload.metadata.namespace
      )
      if (collision) {
        validationErrors.name = `A resource named "${payload.metadata.name}" already exists in this namespace`
      }
    }

    if (Object.keys(validationErrors).length) {
      setErrors(validationErrors)
      toast.error('Please fix the highlighted fields before submitting')
      return false
    }
    setErrors({})
    setSubmitting(true)

    try {
      if (isEdit) {
        await api.update(kind, payload)
        toast.success(`The resource "${payload.metadata.name}" has been updated`)
      } else {
        await api.create(kind, payload)
        toast.success(`The resource "${payload.metadata.name}" has been created`)
      }
      refresh()
      navigate(successPath(payload))
      return true
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
      return false
    } finally {
      setSubmitting(false)
    }
  }, [kind, isEdit, buildPayload, validate, successPath, caches, refresh, navigate])

  // Sends the payload with ?dryRun=true and returns the server-materialised
  // resource (defaults applied) for the summary panel.
  const dryRun = useCallback(async () => {
    const payload = buildPayload()
    setSubmitting(true)
    try {
      const result = isEdit ? await api.update(kind, payload, true) : await api.create(kind, payload, true)
      setPreview(result)
      return result
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
      return null
    } finally {
      setSubmitting(false)
    }
  }, [kind, isEdit, buildPayload])

  return { submit, dryRun, preview, setPreview, errors, submitting, existing, isEdit }
}

// Drops object entries whose value is undefined, empty string or empty
// object/array, so payloads only carry what the user set (parity with the
// Vue forms' conditional spreads).
export function compact(object) {
  if (Array.isArray(object)) {
    return object.map(compact)
  }
  if (object === null || typeof object !== 'object') {
    return object
  }
  const result = {}
  Object.entries(object).forEach(([key, value]) => {
    if (value === undefined || value === '') {
      return
    }
    const compacted = compact(value)
    if (
      compacted !== null &&
      typeof compacted === 'object' &&
      !Array.isArray(compacted) &&
      !Object.keys(compacted).length
    ) {
      return
    }
    if (Array.isArray(compacted) && !compacted.length) {
      return
    }
    result[key] = compacted
  })
  return result
}
