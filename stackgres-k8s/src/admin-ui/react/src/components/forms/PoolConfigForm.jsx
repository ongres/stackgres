import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildPoolConfig } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { DefaultParametersTable, Field, TextArea, TextInput } from './fields'

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
  const data = useData()

  const [name, setName] = useState(routeName || '')
  const [params, setParams] = useState('')
  const [seeded, setSeeded] = useState(false)
  const [defaultParams, setDefaultParams] = useState({})
  const [resolvedParams, setResolvedParams] = useState([])

  if (isEdit && loaded && previous && !seeded) {
    setParams(nonDefaultParams(previous.status))
    setDefaultParams(previous.status?.pgBouncer?.defaultParameters || {})
    setResolvedParams(previous.status?.pgBouncer?.['pgbouncer.ini'] || [])
    setSeeded(true)
  }

  // Vue fetches the defaults on mount with a dry-run create.
  useEffect(() => {
    if (isEdit) {
      return
    }
    api
      .create('sgpoolconfigs', { metadata: { name: 'get-default-params', namespace }, spec: {} }, true)
      .then((response) => {
        setDefaultParams(response?.status?.pgBouncer?.defaultParameters || {})
        setResolvedParams(response?.status?.pgBouncer?.['pgbouncer.ini'] || [])
      })
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const nameCollision =
    !isEdit &&
    (data.sgpoolconfigs || []).some(
      (config) => config.metadata.name === name && config.metadata.namespace === namespace
    )

  const clusters = (data.sgclusters || []).filter(
    (cluster) =>
      cluster.metadata.namespace === namespace &&
      cluster.spec?.configurations?.sgPoolingConfig === routeName
  )

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
      {isEdit && clusters.length > 0 && (
        <p role="note" className="border p-2 text-sm">
          Please, be aware that some changes made to this configuration might require a{' '}
          <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank" rel="noreferrer"
            className="underline">
            restart operation
          </a>{' '}
          on every instance on the following cluster(s):{' '}
          {clusters.map((cluster) => cluster.metadata.name).join(', ')}
        </p>
      )}
      <Field label="Configuration Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      {nameCollision && (
        <p role="alert">
          There&apos;s already a <strong>SGPoolingConfig</strong> with the same name on this
          namespace. Please specify a different name or create the configuration on another namespace
        </p>
      )}
      <Field label="PgBouncer Parameters" help="One parameter per line: parameter=value">
        <TextArea value={params} onChange={setParams} placeholder={'parameter = value'} rows={12} />
      </Field>
      <DefaultParametersTable
        intro="StackGres has set some default parameters to your configuration. If no value is specifically set for them, they will remain with the following default values:"
        defaults={defaultParams}
        resolved={resolvedParams}
        loading={false}
      />
    </CrdFormShell>
  )
}
