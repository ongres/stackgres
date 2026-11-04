import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildStream, objectToRows } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { HelpTooltip } from '../HelpTooltip'
import { NodeAffinityTerms, TolerationsEditor } from './clusterEditors'
import { Field, Select, TextArea, TextInput, Toggle, renderTooltip } from './fields'

const FORM_STEPS = ['stream', 'source', 'target', 'pods']
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

function SecretSelector({ title, value, onChange, tooltipPrefix }) {
  const current = value || selector()
  const tooltip = (suffix) => (tooltipPrefix ? `${tooltipPrefix}.${suffix}` : undefined)
  return (
    <fieldset className="flex flex-wrap gap-3 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      <Field label="Secret Name" tooltip={tooltip('name')}>
        <TextInput value={current.name || ''} onChange={(name) => onChange({ ...current, name: name || null })} />
      </Field>
      <Field label="Secret Key" tooltip={tooltip('key')}>
        <TextInput value={current.key || ''} onChange={(key) => onChange({ ...current, key: key || null })} />
      </Field>
    </fieldset>
  )
}

function StringListEditor({ title, values, onChange, tooltip }) {
  const list = values || []
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}{renderTooltip(tooltip)}</legend>
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

function HeadersEditor({ title, headers, onChange, rowLabel = 'Header', addLabel = 'Add Header', tooltip }) {
  const rows = headers || []
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}{renderTooltip(tooltip)}</legend>
      {rows.map((row, index) => (
        <div key={index} className="flex items-end gap-2">
          <Field label={rowLabel}>
            <TextInput value={row.label} onChange={(label) => onChange(rows.map((r, i) => (i === index ? { ...r, label } : r)))} />
          </Field>
          <Field label="Value">
            <TextInput value={row.value} onChange={(value) => onChange(rows.map((r, i) => (i === index ? { ...r, value } : r)))} />
          </Field>
          <button type="button" aria-label={`Delete ${rowLabel.toLowerCase()} ${index + 1}`}
            onClick={() => onChange(rows.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => onChange([...rows, { label: '', value: '' }])}>
        <Plus size={14} /> {addLabel}
      </button>
    </fieldset>
  )
}

function HttpFields({ http, onChange, urlRequired, urlError, tooltipPrefix }) {
  const patch = (key, value) => onChange({ ...http, [key]: value })
  const tooltip = (suffix) => (tooltipPrefix ? `${tooltipPrefix}.${suffix}` : undefined)
  return (
    <>
      <Field label="URL" required={urlRequired} error={urlError} tooltip={tooltip('url')}>
        <TextInput value={http.url || ''} onChange={(url) => patch('url', url || null)} required={urlRequired} />
      </Field>
      <div className="flex flex-wrap gap-3">
        <Field label="Connect Timeout" help="ISO 8601 duration" tooltip={tooltip('connectTimeout')}>
          <TextInput value={http.connectTimeout || ''} onChange={(value) => patch('connectTimeout', value || null)} />
        </Field>
        <Field label="Read Timeout" help="ISO 8601 duration" tooltip={tooltip('readTimeout')}>
          <TextInput value={http.readTimeout || ''} onChange={(value) => patch('readTimeout', value || null)} />
        </Field>
        <Field label="Retry Backoff Delay" tooltip={tooltip('retryBackoffDelay')}>
          <TextInput type="number" min="0" value={http.retryBackoffDelay ?? ''}
            onChange={(value) => patch('retryBackoffDelay', value === '' ? null : Number(value))} />
        </Field>
        <Field label="Retry Limit" tooltip={tooltip('retryLimit')}>
          <TextInput type="number" min="0" value={http.retryLimit ?? ''}
            onChange={(value) => patch('retryLimit', value === '' ? null : Number(value))} />
        </Field>
      </div>
      <Toggle label="Skip Hostname Verification" checked={http.skipHostnameVerification || false}
        onChange={(value) => patch('skipHostnameVerification', value)}
        tooltip={tooltip('skipHostnameVerification')} />
      <HeadersEditor title="Headers" headers={http.headers} onChange={(headers) => onChange({ ...http, headers })}
        tooltip={tooltip('headers')} />
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
  const [storageClasses, setStorageClasses] = useState(null)
  const [currentStep, setCurrentStep] = useState('stream')
  const scriptFileInput = useRef(null)

  useEffect(() => {
    api.get('storageclasses').then(setStorageClasses).catch(() => setStorageClasses(null))
  }, [])

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
  const nameCollision =
    !isEdit &&
    (data.sgstreams || []).some(
      (candidate) =>
        candidate.metadata.name === crd.metadata.name && candidate.metadata.namespace === namespace
    )

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
      <nav aria-label="Form steps" className="flex flex-wrap gap-3 border-b pb-2 text-sm">
        {FORM_STEPS.map((step) => (
          <button key={step} type="button" onClick={() => setCurrentStep(step)}
            className={currentStep === step ? 'font-semibold underline' : ''}>
            {step}
          </button>
        ))}
      </nav>
      {currentStep === 'stream' && (
      <>
      <h2 className="text-lg font-semibold">Stream Information</h2>
      <Field label="Stream Name" required error={form.errors.name} tooltip="sgstream.metadata.name">
        <TextInput value={crd.metadata.name} disabled={isEdit} required error={form.errors.name}
          onChange={(name) => setCrd({ ...crd, metadata: { ...crd.metadata, name } })} />
      </Field>
      {nameCollision && (
        <p role="alert">
          There&apos;s already a <strong>SGStream</strong> with the same name on this namespace.
          Please specify a different name or create the stream on another namespace
        </p>
      )}
      <Field label="Maximum Retries" help="-1 retries forever" tooltip="sgstream.spec.maxRetries">
        <TextInput type="number" min="-1" placeholder="-1" value={crd.spec.maxRetries ?? ''}
          onChange={(value) => setSpec((spec) => { spec.maxRetries = value === '' ? null : Number(value) })} />
      </Field>
      <h3 className="font-semibold">Persistent Volume{renderTooltip('sgstream.spec.persistentVolume')}</h3>
      <Field label="Volume Size" required tooltip="sgstream.spec.pods.persistentVolume.size">
        <div className="flex gap-2">
          <TextInput type="number" min="1" required value={crd.spec.pods.persistentVolume.size.size}
            onChange={(value) => setSpec((spec) => { spec.pods.persistentVolume.size.size = Number(value) })} />
          <Select value={crd.spec.pods.persistentVolume.size.unit} options={['Mi', 'Gi', 'Ti']}
            onChange={(unit) => setSpec((spec) => { spec.pods.persistentVolume.size.unit = unit })} />
        </div>
      </Field>
      <Field label="Storage Class" tooltip="sgstream.spec.pods.persistentVolume.storageClass">
        {storageClasses === null ? (
          <TextInput value={crd.spec.pods.persistentVolume.storageClass || ''}
            onChange={(value) => setSpec((spec) => { spec.pods.persistentVolume.storageClass = value || null })} />
        ) : (
          <Select value={crd.spec.pods.persistentVolume.storageClass || ''}
            disabled={!storageClasses.length}
            options={[
              {
                value: '',
                label: storageClasses.length ? 'Select Storage Class' : 'No storage classes available',
              },
              ...storageClasses,
            ]}
            onChange={(value) => setSpec((spec) => { spec.pods.persistentVolume.storageClass = value || null })} />
        )}
      </Field>

      </>
      )}

      {currentStep === 'source' && (
      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Source Information{renderTooltip('sgstream.spec.source')}</legend>
        <Field label="Source Type" required error={form.errors.sourceType} tooltip="sgstream.spec.source.type">
          <Select value={crd.spec.source.type || ''} onChange={setSourceType} options={SOURCE_TYPES}
            placeholder="Select type" disabled={isEdit} required />
        </Field>
        {source && (
          <>
            <h2 className="text-lg font-semibold">
              {crd.spec.source.type} Configuration{renderTooltip('sgstream.spec.source.' + sKey)}
            </h2>
            {crd.spec.source.type === 'SGCluster' ? (
              <Field label="SGCluster Name" required error={form.errors.sourceName}
                tooltip="sgstream.spec.source.sgCluster.name">
                <Select value={source.name || ''} options={clusters} placeholder="Select a cluster…" required
                  onChange={(value) => setSpec((spec) => { spec.source[sKey].name = value || null })} />
              </Field>
            ) : (
              <div className="flex flex-wrap gap-3">
                <Field label="Host" required error={form.errors.sourceHost}
                  tooltip="sgstream.spec.source.postgres.host">
                  <TextInput value={source.host || ''} required
                    onChange={(value) => setSpec((spec) => { spec.source[sKey].host = value || null })} />
                </Field>
                <Field label="Port" tooltip="sgstream.spec.source.postgres.port">
                  <TextInput type="number" min="1" value={source.port ?? ''}
                    onChange={(value) => setSpec((spec) => { spec.source[sKey].port = value === '' ? null : Number(value) })} />
                </Field>
              </div>
            )}
            <Field label="Database" tooltip={'sgstream.spec.source.' + sKey + '.database'}>
              <TextInput value={source.database || ''}
                onChange={(value) => setSpec((spec) => { spec.source[sKey].database = value || null })} />
            </Field>
            <SecretSelector title="Username" value={source.username}
              tooltipPrefix={'sgstream.spec.source.' + sKey + '.username'}
              onChange={(value) => setSpec((spec) => { spec.source[sKey].username = value })} />
            <SecretSelector title="Password" value={source.password}
              tooltipPrefix={'sgstream.spec.source.' + sKey + '.password'}
              onChange={(value) => setSpec((spec) => { spec.source[sKey].password = value })} />
            {/* The Vue template drops the sgstream prefix in these loop keys; fixed deliberately. */}
            <StringListEditor title="Includes" values={source.includes}
              tooltip={'sgstream.spec.source.' + sKey + '.includes'}
              onChange={(values) => setSpec((spec) => { spec.source[sKey].includes = values })} />
            <StringListEditor title="Excludes" values={source.excludes}
              tooltip={'sgstream.spec.source.' + sKey + '.excludes'}
              onChange={(values) => setSpec((spec) => { spec.source[sKey].excludes = values })} />
          </>
        )}
      </fieldset>

      )}

      {currentStep === 'target' && (
      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Target Information{renderTooltip('sgstream.spec.target')}</legend>
        <Field label="Target Type" required error={form.errors.targetType} tooltip="sgstream.spec.target.type">
          <Select value={crd.spec.target.type || ''} onChange={setTargetType} options={TARGET_TYPES}
            placeholder="Select type" disabled={isEdit} required />
        </Field>
        {target && (
          <h2 className="text-lg font-semibold">
            {crd.spec.target.type} Configuration{renderTooltip('sgstream.spec.target.' + tKey)}
          </h2>
        )}
        {target && crd.spec.target.type === 'SGCluster' && (
          <>
            <Field label="SGCluster Name" required error={form.errors.targetName}
              tooltip="sgstream.spec.target.sgCluster.name">
              <Select value={target.name || ''} options={clusters} placeholder="Select cluster" required
                onChange={(value) => setSpec((spec) => { spec.target.sgCluster.name = value || null })} />
            </Field>
            <Field label="Database" tooltip="sgstream.spec.target.sgCluster.database">
              <TextInput value={target.database || ''}
                onChange={(value) => setSpec((spec) => { spec.target.sgCluster.database = value || null })} />
            </Field>
            <SecretSelector title="Username" value={target.username}
              tooltipPrefix="sgstream.spec.target.sgCluster.username"
              onChange={(value) => setSpec((spec) => { spec.target.sgCluster.username = value })} />
            <SecretSelector title="Password" value={target.password}
              tooltipPrefix="sgstream.spec.target.sgCluster.password"
              onChange={(value) => setSpec((spec) => { spec.target.sgCluster.password = value })} />
            <Toggle label="Skip DDL Import" checked={target.skipDdlImport || false}
              onChange={(value) => setSpec((spec) => { spec.target.sgCluster.skipDdlImport = value })}
              tooltip="sgstream.spec.target.sgCluster.skipDdlImport" />
            <Field label="DDL Import Role Skip Filter" tooltip="sgstream.spec.target.sgCluster.ddlImportRoleSkipFilter">
              <TextInput value={target.ddlImportRoleSkipFilter || ''}
                onChange={(value) => setSpec((spec) => { spec.target.sgCluster.ddlImportRoleSkipFilter = value || null })} />
            </Field>
          </>
        )}
        {target && crd.spec.target.type === 'CloudEvent' && (
          <>
            <div className="flex flex-wrap gap-3">
              <Field label="Binding" tooltip="sgstream.spec.target.cloudEvent.binding">
                <Select value={target.binding || ''} options={['http']} placeholder="Default"
                  onChange={(value) => setSpec((spec) => { spec.target.cloudEvent.binding = value || null })} />
              </Field>
              <Field label="Format" tooltip="sgstream.spec.target.cloudEvent.format">
                <Select value={target.format || ''} options={['json']} placeholder="Default"
                  onChange={(value) => setSpec((spec) => { spec.target.cloudEvent.format = value || null })} />
              </Field>
            </div>
            {target.binding === 'http' && (
              <>
                <h3 className="font-semibold">HTTP Configuration{renderTooltip('sgstream.spec.target.cloudEvent.http')}</h3>
                <HttpFields http={target.http} urlRequired urlError={form.errors.targetUrl}
                  tooltipPrefix="sgstream.spec.target.cloudEvent.http"
                  onChange={(http) => setSpec((spec) => { spec.target.cloudEvent.http = http })} />
              </>
            )}
          </>
        )}
        {target && crd.spec.target.type === 'PgLambda' && (
          <>
            <h3 className="font-semibold">Script{renderTooltip('sgstream.spec.target.pgLambda.script')}</h3>
            <Field label="Script Source" required
              tooltip={<HelpTooltip text={'Determine the source from which the script should be loaded.'} />}>
              <Select value={scriptSource || ''} required placeholder="Select script source..."
                options={[
                  { value: 'createNewScript', label: 'Custom script' },
                  { value: '', label: '– OR –', disabled: true },
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
              <Field label="Script Type" tooltip="sgstream.spec.target.pgLambda.scriptType">
                <Select value={target.scriptType || ''} options={['javascript']} placeholder="Select script type..."
                  onChange={(value) => setSpec((spec) => { spec.target.pgLambda.scriptType = value || null })} />
              </Field>
            )}
            {scriptSource === 'createNewScript' && (
              <Field label="Script" required error={form.errors.script}>
                <TextArea value={target.script || ''} required rows={8} placeholder="Type a script..."
                  onChange={(value) => setSpec((spec) => { spec.target.pgLambda.script = value || null })} />
                <input ref={scriptFileInput} type="file" hidden
                  onChange={(event) => {
                    const file = event.target.files?.[0]
                    if (!file) return
                    const reader = new FileReader()
                    reader.onload = () =>
                      setSpec((spec) => { spec.target.pgLambda.script = reader.result })
                    reader.readAsText(file)
                    event.target.value = ''
                  }} />
                <button type="button" className="self-start text-sm underline"
                  onClick={() => scriptFileInput.current?.click()}>
                  …or upload a file
                </button>
              </Field>
            )}
            {(scriptSource === 'secretKeyRef' || scriptSource === 'configMapKeyRef') && (
              <>
                <h3 className="font-semibold">
                  {scriptSource === 'secretKeyRef' ? 'Secret Key Ref' : 'Config Map Key Ref'}
                  {renderTooltip('sgscript.spec.scripts.scriptFrom.properties.' + scriptSource)}
                </h3>
                <div className="flex flex-wrap gap-3">
                  <Field label="Name" required
                    tooltip={'sgscript.spec.scripts.scriptFrom.properties.' + scriptSource + '.properties.name'}>
                    <TextInput value={target.scriptFrom?.[scriptSource]?.name || ''} required
                      placeholder="Type a name.."
                      onChange={(value) => setSpec((spec) => { spec.target.pgLambda.scriptFrom[scriptSource].name = value })} />
                  </Field>
                  <Field label="Key" required
                    tooltip={'sgscript.spec.scripts.scriptFrom.properties.' + scriptSource + '.properties.key'}>
                    <TextInput value={target.scriptFrom?.[scriptSource]?.key || ''} required
                      placeholder="Type a key.."
                      onChange={(value) => setSpec((spec) => { spec.target.pgLambda.scriptFrom[scriptSource].key = value })} />
                  </Field>
                </div>
                {isEdit && target.scriptFrom?.configMapScript !== undefined && (
                  <Field label="ConfigMap Script">
                    <TextArea value={target.scriptFrom.configMapScript || ''} rows={8}
                      onChange={(value) =>
                        setSpec((spec) => { spec.target.pgLambda.scriptFrom.configMapScript = value })} />
                  </Field>
                )}
              </>
            )}
            <fieldset className="flex flex-col gap-3 border p-3">
              <legend className="px-1 font-semibold">Knative Configuration{renderTooltip('sgstream.spec.target.pgLambda.knative')}</legend>
              <HttpFields http={target.knative.http} urlRequired urlError={form.errors.knativeUrl}
                tooltipPrefix="sgstream.spec.target.pgLambda.knative.http"
                onChange={(http) => setSpec((spec) => { spec.target.pgLambda.knative.http = http })} />
            </fieldset>
          </>
        )}
      </fieldset>

      )}

      {currentStep === 'pods' && (
      <>
      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Pods&apos; Resources Information{renderTooltip('sgstream.spec.pods.resources')}</legend>
        <StringListEditor title="Claims" values={crd.spec.pods.resources.claims?.map((claim) => claim.name)}
          tooltip="sgstream.spec.pods.resources.claims"
          onChange={(values) =>
            setSpec((spec) => { spec.pods.resources.claims = (values || []).map((name) => ({ name })) })} />
        <HeadersEditor title="Limits" rowLabel="Limit" addLabel="Add Limit"
          tooltip="sgstream.spec.pods.resources.limits"
          headers={crd.spec.pods.resources.limits}
          onChange={(rows) => setSpec((spec) => { spec.pods.resources.limits = rows })} />
        <HeadersEditor title="Requests" rowLabel="Request" addLabel="Add Request"
          tooltip="sgstream.spec.pods.resources.requests"
          headers={crd.spec.pods.resources.requests}
          onChange={(rows) => setSpec((spec) => { spec.pods.resources.requests = rows })} />
      </fieldset>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Pods&apos; Scheduling</legend>
        <Field label="Priority Class Name" tooltip="sgstream.spec.pods.resources.scheduling.priorityClassName">
          <TextInput value={crd.spec.pods.resources.scheduling?.priorityClassName || ''}
            onChange={(value) =>
              setSpec((spec) => { spec.pods.resources.scheduling.priorityClassName = value || null })} />
        </Field>
        <HeadersEditor title="Node Selectors" rowLabel="Label" addLabel="Add Node Selector"
          tooltip="sgstream.spec.pods.resources.scheduling.nodeSelector"
          headers={crd.spec.pods.resources.scheduling?.nodeSelector || []}
          onChange={(rows) =>
            setSpec((spec) => { spec.pods.resources.scheduling.nodeSelector = rows.length ? rows : null })} />
        <TolerationsEditor tolerations={crd.spec.pods.resources.scheduling?.tolerations || []}
          tooltipPrefix="sgstream.spec.pods.resources.scheduling.tolerations"
          onChange={(tolerations) =>
            setSpec((spec) => {
              spec.pods.resources.scheduling.tolerations = tolerations.length ? tolerations : null
            })} />
        <NodeAffinityTerms legend="Node Affinity: Required During Scheduling Ignored During Execution"
          tooltipPrefix="sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution"
          terms={
            crd.spec.pods.resources.scheduling?.nodeAffinity
              ?.requiredDuringSchedulingIgnoredDuringExecution?.nodeSelectorTerms || []
          }
          onChange={(terms) =>
            setSpec((spec) => {
              spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution =
                terms.length ? { nodeSelectorTerms: terms } : null
            })} />
        <NodeAffinityTerms legend="Node Affinity: Preferred During Scheduling Ignored During Execution"
          tooltipPrefix="sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution"
          preferred
          terms={
            crd.spec.pods.resources.scheduling?.nodeAffinity
              ?.preferredDuringSchedulingIgnoredDuringExecution || []
          }
          onChange={(terms) =>
            setSpec((spec) => {
              spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution =
                terms.length ? terms : null
            })} />
      </fieldset>
      </>
      )}
    </CrdFormShell>
  )
}
