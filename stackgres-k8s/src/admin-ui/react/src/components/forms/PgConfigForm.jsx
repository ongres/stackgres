import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildPgConfig } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextArea, TextInput } from './fields'

// Edit seeds the textarea with only the non-default parameters, like the Vue
// form's getParams.
function nonDefaultParams(status) {
  const defaults = status?.defaultParameters || {}
  return (status?.['postgresql.conf'] || [])
    .filter((parameter) => defaults[parameter.parameter] !== parameter.value)
    .map((parameter) => `${parameter.parameter}=${parameter.value}`)
    .join('\n')
}

export default function PgConfigForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgpgconfigs', namespace, routeName, isEdit)
  const data = useData()

  const [name, setName] = useState(routeName || '')
  const [postgresVersion, setPostgresVersion] = useState('')
  const [params, setParams] = useState('')
  const [versions, setVersions] = useState([])
  const [seeded, setSeeded] = useState(false)

  useEffect(() => {
    api
      .getPostgresVersions('vanilla')
      .then((response) => setVersions([...(response.postgresql || [])]))
      .catch(() => {})
  }, [])

  if (isEdit && loaded && previous && !seeded) {
    setPostgresVersion(previous.spec.postgresVersion)
    setParams(nonDefaultParams(previous.status))
    setSeeded(true)
  }

  const clusters = (data.sgclusters || []).filter(
    (cluster) =>
      cluster.metadata.namespace === namespace &&
      cluster.spec?.configurations?.sgPostgresConfig === routeName
  )

  const form = useCrdForm({
    kind: 'sgpgconfigs',
    isEdit,
    buildPayload: () => buildPgConfig({ name, namespace, postgresVersion, params }, previous || {}),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!postgresVersion) errors.postgresVersion = 'A Postgres version is required'
      return errors
    },
    successPath: (payload) =>
      isEdit ? `/${namespace}/sgpgconfig/${payload.metadata.name}` : `/${namespace}/sgpgconfigs`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <CrdFormShell
      title={isEdit ? `Edit Postgres Configuration: ${routeName}` : 'Create Postgres Configuration'}
      form={form}
    >
      {isEdit && clusters.length > 0 && (
        <p className="border p-2 text-sm">
          This configuration is used by {clusters.map((cluster) => cluster.metadata.name).join(', ')};
          changing parameters may require a cluster restart.
        </p>
      )}
      <Field label="Configuration Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      <Field label="Postgres Version" required error={form.errors.postgresVersion}>
        <Select
          value={postgresVersion}
          onChange={setPostgresVersion}
          options={versions}
          placeholder="Select a version…"
          disabled={isEdit}
          required
        />
      </Field>
      <Field label="Parameters" help="One parameter per line: parameter=value">
        <TextArea value={params} onChange={setParams} placeholder={'parameter=value'} rows={12} />
      </Field>
    </CrdFormShell>
  )
}
