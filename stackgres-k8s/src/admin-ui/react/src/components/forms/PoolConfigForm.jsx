import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildPoolConfig } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, TextArea, TextInput } from './fields'

function nonDefaultParams(status) {
  const defaults = status?.pgBouncer?.defaultParameters || {}
  return (status?.pgBouncer?.['pgbouncer.ini'] || [])
    .filter((parameter) => defaults[parameter.parameter] !== parameter.value)
    .map((parameter) => `${parameter.parameter}=${parameter.value}`)
    .join('\n')
}

export default function PoolConfigForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgpoolconfigs', namespace, routeName, isEdit)

  const [name, setName] = useState(routeName || '')
  const [params, setParams] = useState('')
  const [seeded, setSeeded] = useState(false)

  if (isEdit && loaded && previous && !seeded) {
    setParams(nonDefaultParams(previous.status))
    setSeeded(true)
  }

  const form = useCrdForm({
    kind: 'sgpoolconfigs',
    isEdit,
    buildPayload: () => buildPoolConfig({ name, namespace, params }, previous || {}),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      return errors
    },
    successPath: (payload) =>
      isEdit ? `/${namespace}/sgpoolconfig/${payload.metadata.name}` : `/${namespace}/sgpoolconfigs`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <CrdFormShell
      title={
        isEdit
          ? `Edit Connection Pooling Configuration: ${routeName}`
          : 'Create Connection Pooling Configuration'
      }
      form={form}
    >
      <Field label="Configuration Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      <Field label="PgBouncer Parameters" help="One parameter per line: parameter=value">
        <TextArea value={params} onChange={setParams} placeholder={'parameter=value'} rows={12} />
      </Field>
    </CrdFormShell>
  )
}
