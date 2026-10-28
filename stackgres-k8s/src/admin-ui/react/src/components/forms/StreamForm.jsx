import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildStream, objectToRows } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextArea, TextInput, Toggle } from './fields'

const SOURCE_TYPES = ['SGCluster', 'Postgres']
const TARGET_TYPES = ['SGCluster', 'CloudEvent', 'PgLambda']
const sourceKey = (type) => (type === 'SGCluster' ? 'sgCluster' : 'postgres')
const targetKey = (type) =>
  ({ SGCluster: 'sgCluster', CloudEvent: 'cloudEvent', PgLambda: 'pgLambda' })[type]

const selector = () => ({ name: null, key: null })
const httpBlock = () => ({
  url: null,
  headers: null,
  connectTimeout: null,
  readTimeout: null,
  retryBackoffDelay: null,
  retryLimit: null,
  skipHostnameVerification: false,
})

const initialCrd = (namespace) => ({
  metadata: { name: '', namespace },
  spec: {
    maxRetries: null,
    source: { type: null },
    target: { type: null },
    pods: {
      persistentVolume: { size: { size: 1, unit: 'Gi' }, storageClass: null },
      resources: {
        claims: [],
        limits: [],
        requests: [],
        scheduling: {
          nodeAffinity: {
            requiredDuringSchedulingIgnoredDuringExecution: null,
            preferredDuringSchedulingIgnoredDuringExecution: null,
          },
          nodeSelector: null,
          priorityClassName: null,
          tolerations: null,
        },
      },
    },
  },
})

function SecretSelector({ title, value, onChange }) {
  const current = value || selector()
  return (
    <fieldset className="flex flex-wrap gap-3 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      <Field label="Secret Name">
        <TextInput value={current.name || ''} onChange={(name) => onChange({ ...current, name: name || null })} />
      </Field>
      <Field label="Secret Key">
        <TextInput value={current.key || ''} onChange={(key) => onChange({ ...current, key: key || null })} />
      </Field>
    </fieldset>
  )
}

function StringListEditor({ title, values, onChange }) {
  const list = values || []
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {list.map((entry, index) => (
        <div key={index} className="flex items-center gap-2">
          <TextInput value={entry} onChange={(value) => onChange(list.map((e, i) => (i === index ? value : e)))}
            aria-label={`${title} ${index + 1}`} />
          <button type="button" aria-label={`Delete ${title} ${index + 1}`}
            onClick={() => {
              const next = list.filter((_, i) => i !== index)
              onChange(next.length ? next : null)
            }}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => onChange([...list, ''])}>
        <Plus size={14} /> Add New
      </button>
    </fieldset>
  )
}

function HeadersEditor({ title, headers, onChange }) {
  const rows = headers || []
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {rows.map((row, index) => (
        <div key={index} className="flex items-end gap-2">
          <Field label="Header">
            <TextInput value={row.label} onChange={(label) => onChange(rows.map((r, i) => (i === index ? { ...r, label } : r)))} />
          </Field>
          <Field label="Value">
            <TextInput value={row.value} onChange={(value) => onChange(rows.map((r, i) => (i === index ? { ...r, value } : r)))} />
          </Field>
          <button type="button" aria-label={`Delete header ${index + 1}`}
            onClick={() => onChange(rows.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => onChange([...rows, { label: '', value: '' }])}>
        <Plus size={14} /> Add Header
      </button>
    </fieldset>
  )
}

function HttpFields({ http, onChange, urlRequired, urlError }) {
  const patch = (key, value) => onChange({ ...http, [key]: value })
  return (
    <>
      <Field label="URL" required={urlRequired} error={urlError}>
        <TextInput value={http.url || ''} onChange={(url) => patch('url', url || null)} required={urlRequired} />
      </Field>
      <div className="flex flex-wrap gap-3">
        <Field label="Connect Timeout" help="ISO 8601 duration">
          <TextInput value={http.connectTimeout || ''} onChange={(value) => patch('connectTimeout', value || null)} />
        </Field>
        <Field label="Read Timeout" help="ISO 8601 duration">
          <TextInput value={http.readTimeout || ''} onChange={(value) => patch('readTimeout', value || null)} />
        </Field>
        <Field label="Retry Backoff Delay">
          <TextInput type="number" min="0" value={http.retryBackoffDelay ?? ''}
            onChange={(value) => patch('retryBackoffDelay', value === '' ? null : Number(value))} />
        </Field>
        <Field label="Retry Limit">
          <TextInput type="number" min="0" value={http.retryLimit ?? ''}
            onChange={(value) => patch('retryLimit', value === '' ? null : Number(value))} />
        </Field>
      </div>
      <Toggle label="Skip Hostname Verification" checked={http.skipHostnameVerification || false}
        onChange={(value) => patch('skipHostnameVerification', value)} />
      <HeadersEditor title="Headers" headers={http.headers} onChange={(headers) => onChange({ ...http, headers })} />
    </>
  )
}

export default function StreamForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgstreams', namespace, routeName, isEdit)
  const data = useData()

  const [crd, setCrd] = useState(() => initialCrd(namespace))
  const [scriptSource, setScriptSource] = useState(null)
  const [seeded, setSeeded] = useState(false)

  if (isEdit && loaded && previous && !seeded) {
    const clone = JSON.parse(JSON.stringify(previous))
    const sizeText = String(clone.spec.pods?.persistentVolume?.size || '1Gi')
    clone.spec.pods = clone.spec.pods || {}
    clone.spec.pods.persistentVolume = {
      size: {
        size: Number((sizeText.match(/\d+/g) || ['1'])[0]),
        unit: (sizeText.match(/[a-zA-Z]+/g) || ['Gi'])[0],
      },
      storageClass: clone.spec.pods.persistentVolume?.storageClass ?? null,
    }
    const resources = clone.spec.pods.resources || {}
    resources.limits = resources.limits ? objectToRows(resources.limits, 'label') : []
    resources.requests = resources.requests ? objectToRows(resources.requests, 'label') : []
    resources.claims = resources.claims || []
    const scheduling = resources.scheduling || {}
    scheduling.nodeSelector = scheduling.nodeSelector
      ? objectToRows(scheduling.nodeSelector, 'label')
      : null
    scheduling.priorityClassName = scheduling.priorityClassName ?? null
    scheduling.tolerations = scheduling.tolerations ?? null
    scheduling.nodeAffinity = scheduling.nodeAffinity || {
      requiredDuringSchedulingIgnoredDuringExecution: null,
      preferredDuringSchedulingIgnoredDuringExecution: null,
    }
    resources.scheduling = scheduling
    clone.spec.pods.resources = resources
    const cloudHttp = clone.spec.target?.cloudEvent?.http
    if (cloudHttp?.headers) {
      cloudHttp.headers = objectToRows(cloudHttp.headers, 'label')
    }
    const knativeHttp = clone.spec.target?.pgLambda?.knative?.http
    if (knativeHttp?.headers) {
      knativeHttp.headers = objectToRows(knativeHttp.headers, 'label')
    }
    if (clone.spec.target?.type === 'PgLambda') {
      setScriptSource(
        clone.spec.target.pgLambda.scriptFrom
          ? Object.keys(clone.spec.target.pgLambda.scriptFrom)[0]
          : 'createNewScript'
      )
    }
    setCrd(clone)
    setSeeded(true)
  }

  const setSpec = (updater) =>
    setCrd((current) => {
      const next = JSON.parse(JSON.stringify(current))
      updater(next.spec)
      return next
    })

  const sKey = crd.spec.source.type ? sourceKey(crd.spec.source.type) : null
  const tKey = crd.spec.target.type ? targetKey(crd.spec.target.type) : null
  const source = sKey ? crd.spec.source[sKey] : null
  const target = tKey ? crd.spec.target[tKey] : null

  const setSourceType = (type) =>
    setSpec((spec) => {
      spec.source = { type: type || null }
      if (type === 'SGCluster') {
        spec.source.sgCluster = { name: null, database: null, username: selector(), password: selector(), includes: null, excludes: null }
      } else if (type === 'Postgres') {
        spec.source.postgres = { host: null, port: null, database: null, username: selector(), password: selector(), includes: null, excludes: null }
      }
    })

  const setTargetType = (type) => {
    setSpec((spec) => {
      spec.target = { type: type || null }
      if (type === 'SGCluster') {
        spec.target.sgCluster = { name: null, database: null, username: selector(), password: selector(), skipDdlImport: false, ddlImportRoleSkipFilter: null }
      } else if (type === 'CloudEvent') {
        spec.target.cloudEvent = { binding: 'http', format: 'json', http: httpBlock() }
      } else if (type === 'PgLambda') {
        spec.target.pgLambda = { script: null, scriptType: null, knative: { http: httpBlock() } }
      }
    })
    setScriptSource(null)
  }

  const clusters = (data.sgclusters || []).map((cluster) => cluster.metadata.name)

  const form = useCrdForm({
    kind: 'sgstreams',
    isEdit,
    buildPayload: () => buildStream(crd),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!crd.spec.source.type) errors.sourceType = 'A source type is required'
      if (!crd.spec.target.type) errors.targetType = 'A target type is required'
      if (crd.spec.source.type === 'SGCluster' && !crd.spec.source.sgCluster?.name)
        errors.sourceName = 'A source cluster is required'
      if (crd.spec.source.type === 'Postgres' && !crd.spec.source.postgres?.host)
        errors.sourceHost = 'A source host is required'
      if (crd.spec.target.type === 'SGCluster' && !crd.spec.target.sgCluster?.name)
        errors.targetName = 'A target cluster is required'
      if (crd.spec.target.type === 'CloudEvent' && !crd.spec.target.cloudEvent?.http?.url)
        errors.targetUrl = 'A target URL is required'
      if (crd.spec.target.type === 'PgLambda') {
        if (!crd.spec.target.pgLambda?.knative?.http?.url) errors.knativeUrl = 'A Knative URL is required'
        if (scriptSource === 'createNewScript' && !crd.spec.target.pgLambda?.script)
          errors.script = 'A script is required'
      }
      return errors
    },
    successPath: (payload) =>
      isEdit ? `/${namespace}/sgstream/${payload.metadata.name}` : `/${namespace}/sgstreams`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <CrdFormShell title={isEdit ? `Edit Stream: ${routeName}` : 'Create Stream'} form={form}>
      <Field label="Stream Name" required error={form.errors.name}>
        <TextInput value={crd.metadata.name} disabled={isEdit} required error={form.errors.name}
          onChange={(name) => setCrd({ ...crd, metadata: { ...crd.metadata, name } })} />
      </Field>
      <Field label="Maximum Retries" help="-1 retries forever">
        <TextInput type="number" min="-1" placeholder="-1" value={crd.spec.maxRetries ?? ''}
          onChange={(value) => setSpec((spec) => { spec.maxRetries = value === '' ? null : Number(value) })} />
      </Field>
      <Field label="Volume Size" required>
        <div className="flex gap-2">
          <TextInput type="number" min="1" required value={crd.spec.pods.persistentVolume.size.size}
            onChange={(value) => setSpec((spec) => { spec.pods.persistentVolume.size.size = Number(value) })} />
          <Select value={crd.spec.pods.persistentVolume.size.unit} options={['Mi', 'Gi', 'Ti']}
            onChange={(unit) => setSpec((spec) => { spec.pods.persistentVolume.size.unit = unit })} />
        </div>
      </Field>
      <Field label="Storage Class">
        <TextInput value={crd.spec.pods.persistentVolume.storageClass || ''}
          onChange={(value) => setSpec((spec) => { spec.pods.persistentVolume.storageClass = value || null })} />
      </Field>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Source</legend>
        <Field label="Source Type" required error={form.errors.sourceType}>
          <Select value={crd.spec.source.type || ''} onChange={setSourceType} options={SOURCE_TYPES}
            placeholder="Select type…" disabled={isEdit} required />
        </Field>
        {source && (
          <>
            {crd.spec.source.type === 'SGCluster' ? (
              <Field label="SGCluster Name" required error={form.errors.sourceName}>
                <Select value={source.name || ''} options={clusters} placeholder="Select a cluster…" required
                  onChange={(value) => setSpec((spec) => { spec.source[sKey].name = value || null })} />
              </Field>
            ) : (
              <div className="flex flex-wrap gap-3">
                <Field label="Host" required error={form.errors.sourceHost}>
                  <TextInput value={source.host || ''} required
                    onChange={(value) => setSpec((spec) => { spec.source[sKey].host = value || null })} />
                </Field>
                <Field label="Port">
                  <TextInput type="number" min="1" value={source.port ?? ''}
                    onChange={(value) => setSpec((spec) => { spec.source[sKey].port = value === '' ? null : Number(value) })} />
                </Field>
              </div>
            )}
            <Field label="Database">
              <TextInput value={source.database || ''}
                onChange={(value) => setSpec((spec) => { spec.source[sKey].database = value || null })} />
            </Field>
            <SecretSelector title="Username" value={source.username}
              onChange={(value) => setSpec((spec) => { spec.source[sKey].username = value })} />
            <SecretSelector title="Password" value={source.password}
              onChange={(value) => setSpec((spec) => { spec.source[sKey].password = value })} />
            <StringListEditor title="Includes" values={source.includes}
              onChange={(values) => setSpec((spec) => { spec.source[sKey].includes = values })} />
            <StringListEditor title="Excludes" values={source.excludes}
              onChange={(values) => setSpec((spec) => { spec.source[sKey].excludes = values })} />
          </>
        )}
      </fieldset>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Target</legend>
        <Field label="Target Type" required error={form.errors.targetType}>
          <Select value={crd.spec.target.type || ''} onChange={setTargetType} options={TARGET_TYPES}
            placeholder="Select type…" disabled={isEdit} required />
        </Field>
        {target && crd.spec.target.type === 'SGCluster' && (
          <>
            <Field label="SGCluster Name" required error={form.errors.targetName}>
              <Select value={target.name || ''} options={clusters} placeholder="Select a cluster…" required
                onChange={(value) => setSpec((spec) => { spec.target.sgCluster.name = value || null })} />
            </Field>
            <Field label="Database">
              <TextInput value={target.database || ''}
                onChange={(value) => setSpec((spec) => { spec.target.sgCluster.database = value || null })} />
            </Field>
            <SecretSelector title="Username" value={target.username}
              onChange={(value) => setSpec((spec) => { spec.target.sgCluster.username = value })} />
            <SecretSelector title="Password" value={target.password}
              onChange={(value) => setSpec((spec) => { spec.target.sgCluster.password = value })} />
            <Toggle label="Skip DDL Import" checked={target.skipDdlImport || false}
              onChange={(value) => setSpec((spec) => { spec.target.sgCluster.skipDdlImport = value })} />
            <Field label="DDL Import Role Skip Filter">
              <TextInput value={target.ddlImportRoleSkipFilter || ''}
                onChange={(value) => setSpec((spec) => { spec.target.sgCluster.ddlImportRoleSkipFilter = value || null })} />
            </Field>
          </>
        )}
        {target && crd.spec.target.type === 'CloudEvent' && (
          <>
            <div className="flex flex-wrap gap-3">
              <Field label="Binding">
                <Select value={target.binding || ''} options={['http']} placeholder="Default"
                  onChange={(value) => setSpec((spec) => { spec.target.cloudEvent.binding = value || null })} />
              </Field>
              <Field label="Format">
                <Select value={target.format || ''} options={['json']} placeholder="Default"
                  onChange={(value) => setSpec((spec) => { spec.target.cloudEvent.format = value || null })} />
              </Field>
            </div>
            {target.binding === 'http' && (
              <HttpFields http={target.http} urlRequired urlError={form.errors.targetUrl}
                onChange={(http) => setSpec((spec) => { spec.target.cloudEvent.http = http })} />
            )}
          </>
        )}
        {target && crd.spec.target.type === 'PgLambda' && (
          <>
            <Field label="Script Source" required>
              <Select value={scriptSource || ''} required placeholder="Select script source…"
                options={[
                  { value: 'createNewScript', label: 'Custom script' },
                  { value: 'secretKeyRef', label: 'From Secret' },
                  { value: 'configMapKeyRef', label: 'From ConfigMap' },
                ]}
                onChange={(value) => {
                  setScriptSource(value || null)
                  setSpec((spec) => {
                    if (value === 'secretKeyRef' || value === 'configMapKeyRef') {
                      delete spec.target.pgLambda.script
                      spec.target.pgLambda.scriptFrom = { [value]: { key: '', name: '' } }
                    } else {
                      delete spec.target.pgLambda.scriptFrom
                      spec.target.pgLambda.script = spec.target.pgLambda.script ?? null
                    }
                  })
                }} />
            </Field>
            {scriptSource && (
              <Field label="Script Type">
                <Select value={target.scriptType || ''} options={['javascript']} placeholder="Default"
                  onChange={(value) => setSpec((spec) => { spec.target.pgLambda.scriptType = value || null })} />
              </Field>
            )}
            {scriptSource === 'createNewScript' && (
              <Field label="Script" required error={form.errors.script}>
                <TextArea value={target.script || ''} required rows={8}
                  onChange={(value) => setSpec((spec) => { spec.target.pgLambda.script = value || null })} />
              </Field>
            )}
            {(scriptSource === 'secretKeyRef' || scriptSource === 'configMapKeyRef') && (
              <div className="flex flex-wrap gap-3">
                <Field label={`${scriptSource} Name`} required>
                  <TextInput value={target.scriptFrom?.[scriptSource]?.name || ''} required
                    onChange={(value) => setSpec((spec) => { spec.target.pgLambda.scriptFrom[scriptSource].name = value })} />
                </Field>
                <Field label={`${scriptSource} Key`} required>
                  <TextInput value={target.scriptFrom?.[scriptSource]?.key || ''} required
                    onChange={(value) => setSpec((spec) => { spec.target.pgLambda.scriptFrom[scriptSource].key = value })} />
                </Field>
              </div>
            )}
            <fieldset className="flex flex-col gap-3 border p-3">
              <legend className="px-1 font-semibold">Knative</legend>
              <HttpFields http={target.knative.http} urlRequired urlError={form.errors.knativeUrl}
                onChange={(http) => setSpec((spec) => { spec.target.pgLambda.knative.http = http })} />
            </fieldset>
          </>
        )}
      </fieldset>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Pods&apos; Resources</legend>
        <StringListEditor title="Claims" values={crd.spec.pods.resources.claims?.map((claim) => claim.name)}
          onChange={(values) =>
            setSpec((spec) => { spec.pods.resources.claims = (values || []).map((name) => ({ name })) })} />
        <HeadersEditor title="Limits" headers={crd.spec.pods.resources.limits}
          onChange={(rows) => setSpec((spec) => { spec.pods.resources.limits = rows })} />
        <HeadersEditor title="Requests" headers={crd.spec.pods.resources.requests}
          onChange={(rows) => setSpec((spec) => { spec.pods.resources.requests = rows })} />
        <Field label="Priority Class Name">
          <TextInput value={crd.spec.pods.resources.scheduling?.priorityClassName || ''}
            onChange={(value) =>
              setSpec((spec) => { spec.pods.resources.scheduling.priorityClassName = value || null })} />
        </Field>
        <HeadersEditor title="Node Selectors" headers={crd.spec.pods.resources.scheduling?.nodeSelector || []}
          onChange={(rows) =>
            setSpec((spec) => { spec.pods.resources.scheduling.nodeSelector = rows.length ? rows : null })} />
      </fieldset>
    </CrdFormShell>
  )
}
