import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { Plus, Trash2 } from 'lucide-react'
import { api, ApiError } from '../../lib/api'
import { useSGConfigs } from '../../hooks/useSGConfigs'
import { buildSGConfig, normalizeSGConfigSpec } from '../../lib/sgconfig'
import SpecTree from '../SpecTree'
import { Field, Select, TextInput, Toggle } from './fields'

const STEPS = {
  adminui: 'Admin UI',
  authentication: 'Authentication',
  cert: 'Certificate',
  containerRegistry: 'Container Registry',
  extensions: 'Extensions',
  grafana: 'Grafana',
  imagePullPolicy: 'Image Pull Policy',
  jobs: 'Jobs',
  serviceAccount: 'Service Account',
}
const AFFINITY_OPERATORS = [
  { value: 'In', label: 'In' },
  { value: 'NotIn', label: 'NotIn' },
  { value: 'Exists', label: 'Exists' },
  { value: 'DoesNotExists', label: 'Does Not Exists' },
  { value: 'Gt', label: 'Greater Than' },
  { value: 'Lt', label: 'Less Than' },
]
const blankExpression = () => ({ key: '', operator: '', values: [''] })
const blankSelector = () => ({ matchExpressions: [blankExpression()], matchLabels: [{ label: '', value: '' }] })
const blankPodTerm = () => ({
  labelSelector: blankSelector(),
  namespaceSelector: blankSelector(),
  namespaces: [''],
  topologyKey: '',
})

function StringRows({ title, values, onChange, addLabel = 'Add' }) {
  const list = values || []
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {list.map((entry, index) => (
        <div key={index} className="flex items-center gap-2">
          <TextInput value={entry} aria-label={`${title} ${index + 1}`}
            onChange={(value) => onChange(list.map((e, i) => (i === index ? value : e)))} />
          <button type="button" aria-label={`Delete ${title} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => onChange([...list, ''])}>
        <Plus size={14} /> {addLabel}
      </button>
    </fieldset>
  )
}

function LabelRows({ title, rows, onChange }) {
  const list = rows || []
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {list.map((row, index) => (
        <div key={index} className="flex items-end gap-2">
          <Field label="Key">
            <TextInput value={row.label}
              onChange={(label) => onChange(list.map((r, i) => (i === index ? { ...r, label } : r)))} />
          </Field>
          <Field label="Value">
            <TextInput value={row.value}
              onChange={(value) => onChange(list.map((r, i) => (i === index ? { ...r, value } : r)))} />
          </Field>
          <button type="button" aria-label={`Delete ${title} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => onChange([...list, { label: '', value: '' }])}>
        <Plus size={14} /> Add row
      </button>
    </fieldset>
  )
}

function ExpressionsEditor({ title, expressions, onChange }) {
  const list = expressions || []
  const update = (index, patch) => onChange(list.map((e, i) => (i === index ? { ...e, ...patch } : e)))
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {list.map((expression, index) => (
        <div key={index} className="flex flex-wrap items-end gap-2">
          <Field label="Key">
            <TextInput value={expression.key} onChange={(key) => update(index, { key })} />
          </Field>
          <Field label="Operator">
            <Select value={expression.operator} required={expression.key.length > 0}
              options={AFFINITY_OPERATORS} placeholder="Select operator…"
              onChange={(operator) => {
                const patch = { operator }
                if (operator === 'Exists' || operator === 'DoesNotExists') {
                  patch.values = undefined
                } else if (!expression.values || !expression.values.length) {
                  patch.values = ['']
                } else if ((operator === 'Gt' || operator === 'Lt') && expression.values.length > 1) {
                  patch.values = [expression.values[0]]
                }
                update(index, patch)
              }} />
          </Field>
          {expression.values && !['', 'Exists', 'DoesNotExists'].includes(expression.operator) && (
            <Field label={['Gt', 'Lt'].includes(expression.operator) ? 'Value' : 'Values'}>
              <div className="flex flex-col gap-1">
                {expression.values.map((value, valueIndex) => (
                  <div key={valueIndex} className="flex items-center gap-2">
                    <TextInput value={value}
                      type={['Gt', 'Lt'].includes(expression.operator) ? 'number' : 'text'}
                      aria-label={`${title} value ${valueIndex + 1}`}
                      onChange={(next) =>
                        update(index, { values: expression.values.map((v, i) => (i === valueIndex ? next : v)) })} />
                    {['In', 'NotIn'].includes(expression.operator) && (
                      <button type="button" aria-label="Delete value"
                        onClick={() =>
                          update(index, { values: expression.values.filter((_, i) => i !== valueIndex) })}>
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                ))}
                {['In', 'NotIn'].includes(expression.operator) && (
                  <button type="button" className="self-start text-sm underline"
                    onClick={() => update(index, { values: [...expression.values, ''] })}>
                    Add Value
                  </button>
                )}
              </div>
            </Field>
          )}
          <button type="button" aria-label={`Delete ${title} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => onChange([...list, blankExpression()])}>
        <Plus size={14} /> Add Expression
      </button>
    </fieldset>
  )
}

function SelectorEditor({ title, selector, onChange }) {
  const current = selector || blankSelector()
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      <ExpressionsEditor title="Match Expressions" expressions={current.matchExpressions}
        onChange={(matchExpressions) => onChange({ ...current, matchExpressions })} />
      <LabelRows title="Match Labels" rows={current.matchLabels}
        onChange={(matchLabels) => onChange({ ...current, matchLabels })} />
    </fieldset>
  )
}

function PodAffinityEditor({ title, block, onChange }) {
  const current = block || {}
  const preferred = current.preferredDuringSchedulingIgnoredDuringExecution || []
  const required = current.requiredDuringSchedulingIgnoredDuringExecution || []
  const patch = (key, value) => onChange({ ...current, [key]: value })
  return (
    <fieldset className="flex flex-col gap-3 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      <h4 className="font-semibold">Preferred</h4>
      {preferred.map((term, index) => (
        <fieldset key={index} className="flex flex-col gap-2 border p-3">
          <legend className="px-1">Term #{index + 1}</legend>
          <SelectorEditor title="Label Selector" selector={term.podAffinityTerm.labelSelector}
            onChange={(labelSelector) =>
              patch('preferredDuringSchedulingIgnoredDuringExecution',
                preferred.map((t, i) => (i === index ? { ...t, podAffinityTerm: { ...t.podAffinityTerm, labelSelector } } : t)))} />
          <SelectorEditor title="Namespace Selector" selector={term.podAffinityTerm.namespaceSelector}
            onChange={(namespaceSelector) =>
              patch('preferredDuringSchedulingIgnoredDuringExecution',
                preferred.map((t, i) => (i === index ? { ...t, podAffinityTerm: { ...t.podAffinityTerm, namespaceSelector } } : t)))} />
          <StringRows title="Namespaces" values={term.podAffinityTerm.namespaces} addLabel="Add Namespace"
            onChange={(namespaces) =>
              patch('preferredDuringSchedulingIgnoredDuringExecution',
                preferred.map((t, i) => (i === index ? { ...t, podAffinityTerm: { ...t.podAffinityTerm, namespaces } } : t)))} />
          <Field label="Topology Key">
            <TextInput value={term.podAffinityTerm.topologyKey || ''}
              onChange={(topologyKey) =>
                patch('preferredDuringSchedulingIgnoredDuringExecution',
                  preferred.map((t, i) => (i === index ? { ...t, podAffinityTerm: { ...t.podAffinityTerm, topologyKey } } : t)))} />
          </Field>
          <Field label="Weight">
            <TextInput type="number" min="1" max="100" value={term.weight}
              onChange={(weight) =>
                patch('preferredDuringSchedulingIgnoredDuringExecution',
                  preferred.map((t, i) => (i === index ? { ...t, weight: Number(weight) } : t)))} />
          </Field>
          <button type="button" className="self-start text-sm underline"
            onClick={() =>
              patch('preferredDuringSchedulingIgnoredDuringExecution', preferred.filter((_, i) => i !== index))}>
            Delete Term
          </button>
        </fieldset>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() =>
          patch('preferredDuringSchedulingIgnoredDuringExecution', [...preferred, { podAffinityTerm: blankPodTerm(), weight: 1 }])}>
        <Plus size={14} /> Add Preferred Term
      </button>
      <h4 className="font-semibold">Required</h4>
      {required.map((term, index) => (
        <fieldset key={index} className="flex flex-col gap-2 border p-3">
          <legend className="px-1">Term #{index + 1}</legend>
          <SelectorEditor title="Label Selector" selector={term.labelSelector}
            onChange={(labelSelector) =>
              patch('requiredDuringSchedulingIgnoredDuringExecution',
                required.map((t, i) => (i === index ? { ...t, labelSelector } : t)))} />
          <SelectorEditor title="Namespace Selector" selector={term.namespaceSelector}
            onChange={(namespaceSelector) =>
              patch('requiredDuringSchedulingIgnoredDuringExecution',
                required.map((t, i) => (i === index ? { ...t, namespaceSelector } : t)))} />
          <StringRows title="Namespaces" values={term.namespaces} addLabel="Add Namespace"
            onChange={(namespaces) =>
              patch('requiredDuringSchedulingIgnoredDuringExecution',
                required.map((t, i) => (i === index ? { ...t, namespaces } : t)))} />
          <Field label="Topology Key">
            <TextInput value={term.topologyKey || ''}
              onChange={(topologyKey) =>
                patch('requiredDuringSchedulingIgnoredDuringExecution',
                  required.map((t, i) => (i === index ? { ...t, topologyKey } : t)))} />
          </Field>
          <button type="button" className="self-start text-sm underline"
            onClick={() =>
              patch('requiredDuringSchedulingIgnoredDuringExecution', required.filter((_, i) => i !== index))}>
            Delete Term
          </button>
        </fieldset>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() =>
          patch('requiredDuringSchedulingIgnoredDuringExecution', [...required, blankPodTerm()])}>
        <Plus size={14} /> Add Required Term
      </button>
    </fieldset>
  )
}

function NodeAffinityEditor({ block, onChange }) {
  const current = block || {}
  const preferred = current.preferredDuringSchedulingIgnoredDuringExecution || []
  const required = current.requiredDuringSchedulingIgnoredDuringExecution?.nodeSelectorTerms || []
  const patch = (key, value) => onChange({ ...current, [key]: value })
  return (
    <fieldset className="flex flex-col gap-3 border p-3">
      <legend className="px-1 font-semibold">Node Affinity</legend>
      <h4 className="font-semibold">Preferences</h4>
      {preferred.map((term, index) => (
        <fieldset key={index} className="flex flex-col gap-2 border p-3">
          <legend className="px-1">Preference #{index + 1}</legend>
          <ExpressionsEditor title="Match Expressions" expressions={term.preference.matchExpressions}
            onChange={(matchExpressions) =>
              patch('preferredDuringSchedulingIgnoredDuringExecution',
                preferred.map((t, i) => (i === index ? { ...t, preference: { ...t.preference, matchExpressions } } : t)))} />
          <ExpressionsEditor title="Match Fields" expressions={term.preference.matchFields}
            onChange={(matchFields) =>
              patch('preferredDuringSchedulingIgnoredDuringExecution',
                preferred.map((t, i) => (i === index ? { ...t, preference: { ...t.preference, matchFields } } : t)))} />
          <Field label="Weight">
            <TextInput type="number" min="1" max="100" value={term.weight}
              onChange={(weight) =>
                patch('preferredDuringSchedulingIgnoredDuringExecution',
                  preferred.map((t, i) => (i === index ? { ...t, weight: Number(weight) } : t)))} />
          </Field>
          <button type="button" className="self-start text-sm underline"
            onClick={() =>
              patch('preferredDuringSchedulingIgnoredDuringExecution', preferred.filter((_, i) => i !== index))}>
            Delete Preference
          </button>
        </fieldset>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() =>
          patch('preferredDuringSchedulingIgnoredDuringExecution', [
            ...preferred,
            { preference: { matchExpressions: [blankExpression()], matchFields: [blankExpression()] }, weight: 1 },
          ])}>
        <Plus size={14} /> Add Preference
      </button>
      <h4 className="font-semibold">Node Selector Terms</h4>
      {required.map((term, index) => (
        <fieldset key={index} className="flex flex-col gap-2 border p-3">
          <legend className="px-1">Term #{index + 1}</legend>
          <ExpressionsEditor title="Match Expressions" expressions={term.matchExpressions}
            onChange={(matchExpressions) =>
              patch('requiredDuringSchedulingIgnoredDuringExecution', {
                nodeSelectorTerms: required.map((t, i) => (i === index ? { ...t, matchExpressions } : t)),
              })} />
          <ExpressionsEditor title="Match Fields" expressions={term.matchFields}
            onChange={(matchFields) =>
              patch('requiredDuringSchedulingIgnoredDuringExecution', {
                nodeSelectorTerms: required.map((t, i) => (i === index ? { ...t, matchFields } : t)),
              })} />
          <button type="button" className="self-start text-sm underline"
            onClick={() =>
              patch('requiredDuringSchedulingIgnoredDuringExecution', {
                nodeSelectorTerms: required.filter((_, i) => i !== index),
              })}>
            Delete Term
          </button>
        </fieldset>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() =>
          patch('requiredDuringSchedulingIgnoredDuringExecution', {
            nodeSelectorTerms: [...required, { matchExpressions: [blankExpression()], matchFields: [blankExpression()] }],
          })}>
        <Plus size={14} /> Add Term
      </button>
    </fieldset>
  )
}

export default function SGConfigForm() {
  const { name } = useParams()
  const navigate = useNavigate()
  const { sgconfigs, refreshConfigs } = useSGConfigs()
  const config = (sgconfigs || []).find((candidate) => candidate.metadata.name === name)

  const [spec, setSpec] = useState(null)
  const [previousMetadata, setPreviousMetadata] = useState(null)
  const [currentStep, setCurrentStep] = useState('adminui')
  const [preview, setPreview] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  if (config && spec === null) {
    setSpec(normalizeSGConfigSpec(config.spec))
    setPreviousMetadata(JSON.parse(JSON.stringify(config.metadata)))
  }

  if (!spec) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  const patch = (updater) =>
    setSpec((current) => {
      const next = JSON.parse(JSON.stringify(current))
      updater(next)
      return next
    })
  const resources = (section, group, key) => (
    <Field label={`${group === 'requests' ? 'Requests' : 'Limits'} ${key === 'memory' ? 'RAM' : 'CPU'}`}>
      <TextInput value={spec[section].resources?.[group]?.[key] || ''}
        onChange={(value) => patch((s) => { s[section].resources[group][key] = value })} />
    </Field>
  )

  const submit = async (event) => {
    event.preventDefault()
    setSubmitting(true)
    try {
      const payload = buildSGConfig(spec, previousMetadata, name)
      await api.update('sgconfigs', payload)
      toast.success(`Configuration "${name}" updated successfully`)
      refreshConfigs()
      navigate(`/sgconfig/${name}`)
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
    } finally {
      setSubmitting(false)
    }
  }

  const jobs = spec.jobs
  const affinity = jobs.affinity || {}

  return (
    <section className="flex max-w-4xl flex-col gap-4 p-8">
      <h1 className="text-2xl font-semibold">Edit Operator Configuration: {name}</h1>
      <nav aria-label="Configuration sections" className="flex flex-wrap gap-3 border-b pb-2 text-sm">
        {Object.entries(STEPS).map(([step, label]) => (
          <button key={step} type="button" onClick={() => setCurrentStep(step)}
            className={currentStep === step ? 'font-semibold underline' : ''}>
            {label}
          </button>
        ))}
      </nav>
      <form onSubmit={submit} className="flex flex-col gap-4 text-sm">
        {currentStep === 'adminui' && (
          <>
            <div className="flex flex-wrap gap-4">
              {resources('adminui', 'requests', 'memory')}
              {resources('adminui', 'requests', 'cpu')}
              {resources('adminui', 'limits', 'memory')}
              {resources('adminui', 'limits', 'cpu')}
            </div>
            <Toggle label="Expose HTTP" checked={spec.adminui.service.exposeHTTP || false}
              onChange={(value) => patch((s) => { s.adminui.service.exposeHTTP = value })} />
            <Field label="Load Balancer IP">
              <TextInput value={spec.adminui.service.loadBalancerIP || ''}
                onChange={(value) => patch((s) => { s.adminui.service.loadBalancerIP = value })} />
            </Field>
            <StringRows title="Load Balancer Source Ranges" addLabel="Add Range"
              values={spec.adminui.service.loadBalancerSourceRanges}
              onChange={(values) => patch((s) => { s.adminui.service.loadBalancerSourceRanges = values })} />
            <Field label="Type">
              <Select value={spec.adminui.service.type || ''} placeholder="Select a method"
                options={[{ value: 'NodePort', label: 'Node Port' }, { value: 'LoadBalancer', label: 'Load Balancer' }]}
                onChange={(value) => patch((s) => { s.adminui.service.type = value })} />
            </Field>
            {spec.adminui.service.type === 'NodePort' && (
              <div className="flex gap-4">
                <Field label="HTTPS Node Port">
                  <TextInput type="number" value={spec.adminui.service.nodePort ?? ''}
                    onChange={(value) => patch((s) => { s.adminui.service.nodePort = value === '' ? undefined : Number(value) })} />
                </Field>
                <Field label="HTTP Node Port">
                  <TextInput type="number" value={spec.adminui.service.nodePortHTTP ?? ''}
                    onChange={(value) => patch((s) => { s.adminui.service.nodePortHTTP = value === '' ? undefined : Number(value) })} />
                </Field>
              </div>
            )}
          </>
        )}

        {currentStep === 'authentication' && (
          <>
            <Field label="Type">
              <Select value={spec.authentication.type || ''} placeholder="Select auth method"
                options={[{ value: 'jwt', label: 'JWT' }, { value: 'oidc', label: 'OIDC' }]}
                onChange={(value) =>
                  patch((s) => {
                    s.authentication.type = value
                    if (value === 'oidc' && !s.authentication.oidc) {
                      s.authentication.oidc = {
                        authServerUrl: '', clientId: '',
                        clientIdSecretRef: { key: '', name: '', optional: true },
                        credentialsSecret: '',
                        credentialsSecretSecretRef: { key: '', name: '', optional: true },
                        tlsVerification: '',
                      }
                    }
                    if (value !== 'oidc') {
                      delete s.authentication.oidc
                    }
                  })} />
            </Field>
            {spec.authentication.type === 'jwt' && (
              <>
                <Toggle label="Create Admin Secret" checked={spec.authentication.createAdminSecret || false}
                  onChange={(value) => patch((s) => { s.authentication.createAdminSecret = value })} />
                <Field label="User">
                  <TextInput value={spec.authentication.user || ''}
                    onChange={(value) => patch((s) => { s.authentication.user = value })} />
                </Field>
                <Field label="Password">
                  <TextInput type="password" value={spec.authentication.password || ''}
                    onChange={(value) => patch((s) => { s.authentication.password = value })} />
                </Field>
              </>
            )}
            {spec.authentication.type === 'oidc' && spec.authentication.oidc && (
              <div className="flex flex-col gap-3">
                <Field label="Authentication Server Url">
                  <TextInput value={spec.authentication.oidc.authServerUrl}
                    onChange={(value) => patch((s) => { s.authentication.oidc.authServerUrl = value })} />
                </Field>
                <Field label="TLS Verification">
                  <Select value={spec.authentication.oidc.tlsVerification || ''} placeholder="Select method"
                    options={[{ value: 'none', label: 'None' }, { value: 'required', label: 'Required' }, { value: 'certificate-validation', label: 'Certificate Validation' }]}
                    onChange={(value) => patch((s) => { s.authentication.oidc.tlsVerification = value })} />
                </Field>
                <Field label="Client ID">
                  <TextInput value={spec.authentication.oidc.clientId}
                    onChange={(value) => patch((s) => { s.authentication.oidc.clientId = value })} />
                </Field>
                <div className="flex gap-4">
                  <Field label="Client ID Secret Reference Name">
                    <TextInput value={spec.authentication.oidc.clientIdSecretRef?.name || ''}
                      onChange={(value) => patch((s) => { s.authentication.oidc.clientIdSecretRef.name = value })} />
                  </Field>
                  <Field label="Client ID Secret Reference Key">
                    <TextInput value={spec.authentication.oidc.clientIdSecretRef?.key || ''}
                      onChange={(value) => patch((s) => { s.authentication.oidc.clientIdSecretRef.key = value })} />
                  </Field>
                </div>
                <Field label="Credentials Secret">
                  <TextInput value={spec.authentication.oidc.credentialsSecret}
                    onChange={(value) => patch((s) => { s.authentication.oidc.credentialsSecret = value })} />
                </Field>
                <div className="flex gap-4">
                  <Field label="Credentials Secret Reference Name">
                    <TextInput value={spec.authentication.oidc.credentialsSecretSecretRef?.name || ''}
                      onChange={(value) => patch((s) => { s.authentication.oidc.credentialsSecretSecretRef.name = value })} />
                  </Field>
                  <Field label="Credentials Secret Reference Key">
                    <TextInput value={spec.authentication.oidc.credentialsSecretSecretRef?.key || ''}
                      onChange={(value) => patch((s) => { s.authentication.oidc.credentialsSecretSecretRef.key = value })} />
                  </Field>
                </div>
              </div>
            )}
          </>
        )}

        {currentStep === 'cert' && (
          <div className="flex flex-col gap-3">
            {[
              ['Auto Approve', 'autoapprove', 'toggle'],
              ['Certificate Duration', 'certDuration', 'number'],
              ['Create Certificate for Operator', 'createForOperator', 'toggle'],
              ['Create Certificate for Web API', 'createForWebApi', 'toggle'],
              ['Regenerate Certificate', 'regenerateCert', 'toggle'],
              ['Regenerate Web Certificate', 'regenerateWebCert', 'toggle'],
              ['Regenerate Web RSA', 'regenerateWebRsa', 'toggle'],
              ['Reset Certificates', 'resetCerts', 'toggle'],
              ['Secret Name', 'secretName', 'text'],
              ['Web Certificate Duration', 'webCertDuration', 'number'],
              ['Web RSA Duration', 'webRsaDuration', 'number'],
              ['Web Secret Name', 'webSecretName', 'text'],
            ].map(([label, key, type]) =>
              type === 'toggle' ? (
                <Toggle key={key} label={label} checked={spec.cert[key] || false}
                  onChange={(value) => patch((s) => { s.cert[key] = value })} />
              ) : (
                <Field key={key} label={label}>
                  <TextInput type={type} value={spec.cert[key] ?? ''}
                    onChange={(value) =>
                      patch((s) => { s.cert[key] = type === 'number' ? (value === '' ? undefined : Number(value)) : value })} />
                </Field>
              )
            )}
          </div>
        )}

        {currentStep === 'containerRegistry' && (
          <Field label="Container Registry">
            <TextInput value={spec.containerRegistry || ''}
              onChange={(value) => patch((s) => { s.containerRegistry = value })} />
          </Field>
        )}

        {currentStep === 'extensions' && (
          <StringRows title="Repository URLs" addLabel="Add URL" values={spec.extensions.repositoryUrls}
            onChange={(values) => patch((s) => { s.extensions.repositoryUrls = values })} />
        )}

        {currentStep === 'grafana' && (
          <div className="flex flex-col gap-3">
            <Toggle label="Auto Embed" checked={spec.grafana.autoEmbed || false}
              onChange={(value) => patch((s) => { s.grafana.autoEmbed = value })} />
            {[
              ['Dashboard Config Map', 'dashboardConfigMap'], ['Dashboard ID', 'dashboardId'],
              ['Datasource Name', 'datasourceName'], ['User', 'user'], ['Password', 'password'],
              ['Schema', 'schema'], ['Secret Name', 'secretName'], ['Secret Namespace', 'secretNamespace'],
              ['Secret User Key', 'secretUserKey'], ['Secret Password Key', 'secretPasswordKey'],
              ['Token', 'token'], ['URL', 'url'], ['Web Host', 'webHost'],
            ].map(([label, key]) => (
              <Field key={key} label={label}>
                <TextInput type={key === 'password' ? 'password' : 'text'} value={spec.grafana[key] || ''}
                  onChange={(value) => patch((s) => { s.grafana[key] = value })} />
              </Field>
            ))}
          </div>
        )}

        {currentStep === 'imagePullPolicy' && (
          <Field label="Image Pull Policy">
            <TextInput value={spec.imagePullPolicy || ''}
              onChange={(value) => patch((s) => { s.imagePullPolicy = value })} />
          </Field>
        )}

        {currentStep === 'jobs' && (
          <div className="flex flex-col gap-4">
            <NodeAffinityEditor block={affinity.nodeAffinity}
              onChange={(nodeAffinity) => patch((s) => { s.jobs.affinity = { ...(s.jobs.affinity || {}), nodeAffinity } })} />
            <PodAffinityEditor title="Pod Affinity" block={affinity.podAffinity}
              onChange={(podAffinity) => patch((s) => { s.jobs.affinity = { ...(s.jobs.affinity || {}), podAffinity } })} />
            <PodAffinityEditor title="Pod Anti Affinity" block={affinity.podAntiAffinity}
              onChange={(podAntiAffinity) => patch((s) => { s.jobs.affinity = { ...(s.jobs.affinity || {}), podAntiAffinity } })} />
            <LabelRows title="Annotations" rows={jobs.annotations}
              onChange={(rows) => patch((s) => { s.jobs.annotations = rows })} />
            <LabelRows title="Node Selector" rows={jobs.nodeSelector}
              onChange={(rows) => patch((s) => { s.jobs.nodeSelector = rows })} />
            <div className="flex flex-wrap gap-4">
              {resources('jobs', 'requests', 'memory')}
              {resources('jobs', 'requests', 'cpu')}
              {resources('jobs', 'limits', 'memory')}
              {resources('jobs', 'limits', 'cpu')}
            </div>
            <LabelRows title="Service Account Annotations" rows={jobs.serviceAccount.annotations}
              onChange={(rows) => patch((s) => { s.jobs.serviceAccount.annotations = rows })} />
            <StringRows title="Service Account Repository Credentials" addLabel="Add URL"
              values={jobs.serviceAccount.repoCredentials}
              onChange={(values) => patch((s) => { s.jobs.serviceAccount.repoCredentials = values })} />
            <fieldset className="flex flex-col gap-2 border p-3">
              <legend className="px-1 font-semibold">Tolerations</legend>
              {jobs.tolerations.map((toleration, index) => (
                <div key={index} className="flex flex-wrap items-end gap-2">
                  <Field label="Key">
                    <TextInput value={toleration.key || ''}
                      onChange={(key) => patch((s) => { s.jobs.tolerations[index].key = key })} />
                  </Field>
                  <Field label="Operator">
                    <Select value={toleration.operator || ''} options={['Equal', 'Exists']} placeholder="Select…"
                      onChange={(operator) =>
                        patch((s) => {
                          s.jobs.tolerations[index].operator = operator
                          if (operator === 'Exists') delete s.jobs.tolerations[index].value
                          else s.jobs.tolerations[index].value = ''
                        })} />
                  </Field>
                  {toleration.operator === 'Equal' && (
                    <Field label="Value">
                      <TextInput value={toleration.value || ''}
                        onChange={(value) => patch((s) => { s.jobs.tolerations[index].value = value })} />
                    </Field>
                  )}
                  <Field label="Effect">
                    <Select value={toleration.effect || ''} placeholder="MatchAll"
                      options={['NoSchedule', 'PreferNoSchedule', 'NoExecute']}
                      onChange={(effect) => patch((s) => { s.jobs.tolerations[index].effect = effect || null })} />
                  </Field>
                  {toleration.effect === 'NoExecute' && (
                    <Field label="Toleration Seconds">
                      <TextInput type="number" min="0" value={toleration.tolerationSeconds ?? ''}
                        onChange={(value) =>
                          patch((s) => { s.jobs.tolerations[index].tolerationSeconds = value === '' ? null : Number(value) })} />
                    </Field>
                  )}
                  <button type="button" aria-label={`Delete toleration ${index + 1}`}
                    onClick={() => patch((s) => { s.jobs.tolerations.splice(index, 1) })}>
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
              <button type="button" className="flex items-center gap-1 self-start text-sm underline"
                onClick={() =>
                  patch((s) => {
                    s.jobs.tolerations.push({ effect: '', key: '', operator: '', tolerationSeconds: null, value: '' })
                  })}>
                <Plus size={14} /> Add Toleration
              </button>
            </fieldset>
          </div>
        )}

        {currentStep === 'serviceAccount' && (
          <>
            <LabelRows title="Annotations" rows={spec.serviceAccount.annotations}
              onChange={(rows) => patch((s) => { s.serviceAccount.annotations = rows })} />
            <StringRows title="Repository Credentials" addLabel="Add URL"
              values={spec.serviceAccount.repoCredentials}
              onChange={(values) => patch((s) => { s.serviceAccount.repoCredentials = values })} />
          </>
        )}

        <div className="flex items-center gap-3">
          <button type="submit" disabled={submitting} className="border p-2 font-semibold">
            Update Configuration
          </button>
          <button type="button" className="border p-2"
            onClick={() => setPreview(buildSGConfig(spec, previousMetadata, name))}>
            View Summary
          </button>
          <button type="button" className="border p-2" onClick={() => navigate(`/sgconfig/${name}`)}>
            Cancel
          </button>
        </div>
      </form>
      {preview && (
        <section className="border p-4 text-sm">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold uppercase">Summary</h2>
            <button type="button" onClick={() => setPreview(null)} className="underline">
              Close
            </button>
          </div>
          <SpecTree value={preview} />
        </section>
      )}
    </section>
  )
}
