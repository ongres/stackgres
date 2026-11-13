import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildPgConfig } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { DefaultParametersTable, Field, FieldGroup, FieldRow, Select, TextArea, TextInput } from './fields'

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
  const [majorVersions, setMajorVersions] = useState([])
  const [seeded, setSeeded] = useState(false)
  const [defaultParams, setDefaultParams] = useState({})
  const [resolvedParams, setResolvedParams] = useState([])
  const [loadingDefaults, setLoadingDefaults] = useState(false)

  useEffect(() => {
    api
      .getPostgresVersions('vanilla')
      .then((response) => {
        const majors = []
        ;(response.postgresql || []).forEach((version) => {
          const major = version.substring(0, version.indexOf('.'))
          if (!majors.includes(major)) {
            majors.push(major)
          }
        })
        setMajorVersions(majors)
      })
      .catch(() => {})
  }, [])

  if (isEdit && loaded && previous && !seeded) {
    setPostgresVersion(previous.spec.postgresVersion)
    setParams(nonDefaultParams(previous.status))
    setDefaultParams(previous.status?.defaultParameters || {})
    setResolvedParams(previous.status?.['postgresql.conf'] || [])
    setSeeded(true)
  }

  // Vue getDefaultParams: a dry-run POST whose status carries the StackGres
  // defaults and the resolved configuration, refreshed on version change.
  useEffect(() => {
    if (!postgresVersion || isEdit) {
      return
    }
    let cancelled = false
    setLoadingDefaults(true)
    api
      .create(
        'sgpgconfigs',
        {
          metadata: { name: name.length ? name : 'get-default-params', namespace },
          spec: { postgresVersion },
        },
        true
      )
      .then((response) => {
        if (!cancelled) {
          setDefaultParams(response?.status?.defaultParameters || {})
          setResolvedParams(response?.status?.['postgresql.conf'] || [])
        }
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) {
          setLoadingDefaults(false)
        }
      })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [postgresVersion])

  const nameCollision =
    !isEdit &&
    (data.sgpgconfigs || []).some(
      (config) => config.metadata.name === name && config.metadata.namespace === namespace
    )

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
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  return (
    <CrdFormShell
      title={isEdit ? `Edit Postgres Configuration: ${routeName}` : 'Create Postgres Configuration'}
      form={form}
    >
      {isEdit && clusters.length > 0 && (
        <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
          Please, be aware that some changes made to this configuration might require a{' '}
          <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank" rel="noreferrer"
            className="text-[hsl(var(--primary))] hover:underline">
            restart operation
          </a>{' '}
          on every instance on the following cluster(s):{' '}
          {clusters.map((cluster) => cluster.metadata.name).join(', ')}
        </p>
      )}
      <FieldGroup>
        <FieldRow>
          <Field label="Configuration Name" required error={form.errors.name} tooltip="sgpostgresconfig.metadata.name">
            <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
          </Field>
          <Field label="Postgres Version" required error={form.errors.postgresVersion} tooltip="sgpostgresconfig.spec.postgresVersion">
            <Select
              value={postgresVersion}
              onChange={setPostgresVersion}
              options={majorVersions}
              placeholder="Select Major Postgres Version"
              disabled={isEdit}
              required
            />
          </Field>
        </FieldRow>
        {nameCollision && (
          <p role="alert" className="text-sm text-red-500">
            There&apos;s already a <strong>SGPostgresConfig</strong> with the same name on this
            namespace. Please specify a different name or create the configuration on another namespace
          </p>
        )}
      </FieldGroup>
      <Field label="Parameters" help="One parameter per line: parameter=value" tooltip="sgpostgresconfig.spec.postgresql.conf">
        <TextArea value={params} onChange={setParams} placeholder={'parameter = value'} rows={12} />
      </Field>
      <DefaultParametersTable
        intro="StackGres sets some default parameters for your configuration. If no value is specifically set for them, they will remain with the following default values:"
        defaults={defaultParams}
        resolved={resolvedParams}
        loading={loadingDefaults}
      />
    </CrdFormShell>
  )
}
