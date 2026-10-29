import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildDistributedLogs, objectToRows } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextInput, Toggle } from './fields'

const SERVICE_TYPES = ['ClusterIP', 'LoadBalancer', 'NodePort']
const TRISTATE = [
  { value: '', label: 'Default' },
  { value: 'false', label: 'Enable' },
  { value: 'true', label: 'Disable' },
]
const EFFECTS = [
  { value: '', label: 'MatchAll' },
  { value: 'NoSchedule', label: 'NoSchedule' },
  { value: 'PreferNoSchedule', label: 'PreferNoSchedule' },
  { value: 'NoExecute', label: 'NoExecute' },
]

function KeyValueRows({ title, rows, setRows, keyLabel = 'Annotation' }) {
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {rows.map((row, index) => (
        <div key={index} className="flex items-end gap-2">
          <Field label={keyLabel}>
            <TextInput value={row.annotation ?? row.label ?? ''}
              onChange={(value) =>
                setRows(rows.map((r, i) => (i === index ? { ...r, [row.annotation !== undefined ? 'annotation' : 'label']: value } : r)))} />
          </Field>
          <Field label="Value">
            <TextInput value={row.value}
              onChange={(value) => setRows(rows.map((r, i) => (i === index ? { ...r, value } : r)))} />
          </Field>
          <button type="button" aria-label={`Delete ${title} row ${index + 1}`}
            onClick={() => setRows(rows.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start text-sm underline"
        onClick={() => setRows([...rows, rows[0] && rows[0].label !== undefined ? { label: '', value: '' } : { annotation: '', value: '' }])}>
        <Plus size={14} /> {keyLabel === 'Label' ? 'Add Node Selector' : 'Add Annotation'}
      </button>
    </fieldset>
  )
}

export default function DistributedLogsForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgdistributedlogs', namespace, routeName, isEdit)
  const data = useData()
  const canI = useCanI()

  const [name, setName] = useState(routeName || '')
  const [profile, setProfile] = useState('production')
  const [volumeSize, setVolumeSize] = useState('1')
  const [volumeUnit, setVolumeUnit] = useState('Gi')
  const [storageClass, setStorageClass] = useState('')
  const [storageClasses, setStorageClasses] = useState(null)
  const [resourceProfile, setResourceProfile] = useState('')
  const [pgConfig, setPgConfig] = useState('')
  const [advanced, setAdvanced] = useState(false)
  const [primaryType, setPrimaryType] = useState('ClusterIP')
  const [primaryLoadBalancerIP, setPrimaryLoadBalancerIP] = useState('')
  const [replicasEnabled, setReplicasEnabled] = useState(true)
  const [replicasType, setReplicasType] = useState('ClusterIP')
  const [replicasLoadBalancerIP, setReplicasLoadBalancerIP] = useState('')
  const [annotationsAll, setAnnotationsAll] = useState([{ annotation: '', value: '' }])
  const [annotationsPods, setAnnotationsPods] = useState([{ annotation: '', value: '' }])
  const [annotationsServices, setAnnotationsServices] = useState([{ annotation: '', value: '' }])
  const [nodeSelector, setNodeSelector] = useState([{ label: '', value: '' }])
  const [tolerations, setTolerations] = useState([])
  const [podAntiAffinity, setPodAntiAffinity] = useState(null)
  const [patroniRequirements, setPatroniRequirements] = useState(null)
  const [clusterRequirements, setClusterRequirements] = useState(null)
  const [seeded, setSeeded] = useState(false)

  useEffect(() => {
    api.get('storageclasses').then(setStorageClasses).catch(() => {})
  }, [])

  if (isEdit && loaded && previous && !seeded) {
    const spec = previous.spec || {}
    setAdvanced(true)
    const sizeText = String(spec.persistentVolume?.size || '1Gi')
    setVolumeSize((sizeText.match(/\d+/g) || ['1'])[0])
    setVolumeUnit((sizeText.match(/[a-zA-Z]+/g) || ['Gi'])[0])
    setStorageClass(spec.persistentVolume?.storageClass || '')
    setProfile(spec.profile || 'production')
    setResourceProfile(spec.sgInstanceProfile || '')
    setPgConfig(spec.configurations?.sgPostgresConfig || '')
    setPodAntiAffinity(spec.nonProductionOptions?.disableClusterPodAntiAffinity ?? null)
    setPatroniRequirements(spec.nonProductionOptions?.disablePatroniResourceRequirements ?? null)
    setClusterRequirements(spec.nonProductionOptions?.disableClusterResourceRequirements ?? null)
    setNodeSelector(
      spec.scheduling?.nodeSelector
        ? objectToRows(spec.scheduling.nodeSelector, 'label')
        : [{ label: '', value: '' }]
    )
    setTolerations(spec.scheduling?.tolerations || [])
    setAnnotationsAll(
      spec.metadata?.annotations?.allResources
        ? objectToRows(spec.metadata.annotations.allResources)
        : [{ annotation: '', value: '' }]
    )
    setAnnotationsPods(
      spec.metadata?.annotations?.pods
        ? objectToRows(spec.metadata.annotations.pods)
        : [{ annotation: '', value: '' }]
    )
    setAnnotationsServices(
      spec.metadata?.annotations?.services
        ? objectToRows(spec.metadata.annotations.services)
        : [{ annotation: '', value: '' }]
    )
    setPrimaryType(spec.postgresServices?.primary?.type || 'ClusterIP')
    setPrimaryLoadBalancerIP(spec.postgresServices?.primary?.loadBalancerIP || '')
    setReplicasEnabled(spec.postgresServices?.replicas?.enabled ?? false)
    setReplicasType(spec.postgresServices?.replicas?.type || 'ClusterIP')
    setReplicasLoadBalancerIP(spec.postgresServices?.replicas?.loadBalancerIP || '')
    setSeeded(true)
  }

  const profiles = (data.sginstanceprofiles || []).filter((p) => p.metadata.namespace === namespace)
  const pgConfigs = (data.sgpgconfigs || []).filter((p) => p.metadata.namespace === namespace)

  const form = useCrdForm({
    kind: 'sgdistributedlogs',
    isEdit,
    buildPayload: () =>
      buildDistributedLogs(
        {
          name, namespace, profile, volumeSize, volumeUnit, storageClass, resourceProfile, pgConfig,
          clusterPodAntiAffinity: podAntiAffinity,
          patroniResourceRequirements: patroniRequirements,
          clusterResourceRequirements: clusterRequirements,
          nodeSelector, tolerations, annotationsAll, annotationsPods, annotationsServices,
          postgresServicesPrimaryType: primaryType,
          postgresServicesPrimaryLoadBalancerIP: primaryLoadBalancerIP,
          postgresServicesReplicas: replicasEnabled,
          postgresServicesReplicasType: replicasType,
          postgresServicesReplicasLoadBalancerIP: replicasLoadBalancerIP,
        },
        previous || {}
      ),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!volumeSize.length) errors.volumeSize = 'A volume size is required'
      return errors
    },
    successPath: (payload) =>
      isEdit
        ? `/${namespace}/sgdistributedlog/${payload.metadata.name}`
        : `/${namespace}/sgdistributedlogs`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  const nameCollision =
    !isEdit &&
    (data.sgdistributedlogs || []).some(
      (server) => server.metadata.name === name && server.metadata.namespace === namespace
    )

  const tristate = (value, onChange) => (
    <Select value={value === null ? '' : String(value)}
      onChange={(next) => onChange(next === '' ? null : next === 'true')} options={TRISTATE} />
  )

  return (
    <CrdFormShell title={isEdit ? `Edit Logs Server: ${routeName}` : 'Create Logs Server'} form={form}>
      <h2 className="text-lg font-semibold">Cluster Information</h2>
      <Field label="Server Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      {nameCollision && (
        <p role="alert">
          There&apos;s already a <strong>SGDistributedLogs</strong> with the same name on this
          namespace. Please specify a different name or create the server on another namespace.
        </p>
      )}
      <Field label="Profile">
        <Select value={profile} onChange={setProfile} options={['production', 'testing', 'development']} />
      </Field>
      {profile !== 'production' && (
        <div role="note">
          <p>By choosing this Profile, the following defaults are overwritten:</p>
          <ul className="list-disc pl-6">
            <li>Cluster Pod Anti Affinity is set to Disable.</li>
            {profile === 'development' && (
              <>
                <li>Patroni Resource Requirements is set to Disable.</li>
                <li>Cluster Resource Requirements is set to Disable.</li>
              </>
            )}
          </ul>
        </div>
      )}
      <h3 className="font-semibold">Persistent Volume</h3>
      <Field label="Volume Size" required error={form.errors.volumeSize}>
        <div className="flex gap-2">
          <TextInput type="number" min="1" value={volumeSize} onChange={setVolumeSize} disabled={isEdit} required />
          <Select value={volumeUnit} onChange={setVolumeUnit} options={['Mi', 'Gi', 'Ti']} disabled={isEdit} />
        </div>
      </Field>
      <Field label="Storage Class">
        {storageClasses === null ? (
          <TextInput value={storageClass} onChange={setStorageClass} />
        ) : (
          <Select value={storageClass} onChange={setStorageClass} options={storageClasses}
            disabled={!storageClasses.length}
            placeholder={storageClasses.length ? 'Select Storage Class' : 'No storage classes available'} />
        )}
      </Field>
      <h3 className="font-semibold">Pods Resources</h3>
      <Field label="Instance Profile">
        <Select value={resourceProfile}
          onChange={(value) => {
            if (value === 'createNewResource') {
              window.open(`/admin/${namespace}/sginstanceprofiles/new`, '_blank')
              return
            }
            setResourceProfile(value)
          }}
          options={[
            ...profiles.map((p) => ({
              value: p.metadata.name,
              label: `${p.metadata.name} (Cores: ${p.spec.cpu}, RAM: ${p.spec.memory}B)`,
            })),
            ...(canI('create', 'sginstanceprofiles', namespace)
              ? [
                  ...(profiles.length ? [{ value: '', label: '– OR –', disabled: true }] : []),
                  { value: 'createNewResource', label: 'Create new profile' },
                ]
              : []),
          ]}
          placeholder="Default (Cores: 1, RAM: 2GiB)" />
      </Field>
      <Field label="Postgres Configuration">
        <Select value={pgConfig}
          onChange={(value) => {
            if (value === 'createNewResource') {
              window.open(`/admin/${namespace}/sgpgconfigs/new`, '_blank')
              return
            }
            setPgConfig(value)
          }}
          options={[
            ...pgConfigs.map((p) => p.metadata.name),
            ...(canI('create', 'sgpgconfigs', namespace)
              ? [
                  ...(pgConfigs.length ? [{ value: '', label: '– OR –', disabled: true }] : []),
                  { value: 'createNewResource', label: 'Create new configuration' },
                ]
              : []),
          ]}
          placeholder="Default" />
      </Field>

      <Toggle label="Advanced options" checked={advanced} onChange={setAdvanced} />
      {advanced && (
        <>
          <fieldset className="flex flex-col gap-3 border p-3">
            <legend className="px-1 font-semibold">Customize generated Kubernetes service</legend>
            <h3 className="font-semibold">Primary Service</h3>
            <Toggle label="Service" checked disabled onChange={() => {}} />
            <Field label="Type">
              <Select value={primaryType} onChange={setPrimaryType} options={SERVICE_TYPES} />
            </Field>
            <Field label="Load Balancer IP">
              <TextInput value={primaryLoadBalancerIP} onChange={setPrimaryLoadBalancerIP} />
            </Field>
            <h3 className="font-semibold">Replicas Service</h3>
            <Toggle label="Service" checked={replicasEnabled} onChange={setReplicasEnabled} />
            <Field label="Type">
              <Select value={replicasType} onChange={setReplicasType} options={SERVICE_TYPES} />
            </Field>
            <Field label="Load Balancer IP">
              <TextInput value={replicasLoadBalancerIP} onChange={setReplicasLoadBalancerIP} />
            </Field>
          </fieldset>
          <h2 className="text-lg font-semibold">Metadata</h2>
          <h3 className="font-semibold">Resources Metadata</h3>
          <KeyValueRows title="All Resources" rows={annotationsAll} setRows={setAnnotationsAll} />
          <KeyValueRows title="Cluster Pods" rows={annotationsPods} setRows={setAnnotationsPods} />
          <KeyValueRows title="Services" rows={annotationsServices} setRows={setAnnotationsServices} />
          <h2 className="text-lg font-semibold">Scheduling</h2>
          <KeyValueRows title="Node Selectors" rows={nodeSelector} setRows={setNodeSelector} keyLabel="Label" />
          <fieldset className="flex flex-col gap-2 border p-3">
            <legend className="px-1 font-semibold">Node Tolerations</legend>
            {tolerations.map((toleration, index) => (
              <div key={index} className="flex flex-col gap-2">
                <h4 className="font-semibold">Toleration #{index + 1}</h4>
                <div className="flex flex-wrap items-end gap-2">
                <Field label="Key">
                  <TextInput value={toleration.key}
                    onChange={(key) => setTolerations(tolerations.map((t, i) => (i === index ? { ...t, key } : t)))} />
                </Field>
                <Field label="Operator">
                  <Select value={toleration.operator}
                    onChange={(operator) =>
                      setTolerations(tolerations.map((t, i) =>
                        i === index ? { ...t, operator, value: operator === 'Exists' ? undefined : '' } : t))}
                    options={['Equal', 'Exists']} />
                </Field>
                {toleration.operator === 'Equal' && (
                  <Field label="Value">
                    <TextInput value={toleration.value || ''}
                      onChange={(value) => setTolerations(tolerations.map((t, i) => (i === index ? { ...t, value } : t)))} />
                  </Field>
                )}
                <Field label="Effect">
                  <Select value={toleration.effect || ''}
                    onChange={(effect) =>
                      setTolerations(tolerations.map((t, i) => (i === index ? { ...t, effect: effect || null } : t)))}
                    options={EFFECTS} />
                </Field>
                {toleration.effect === 'NoExecute' && (
                  <Field label="Toleration Seconds">
                    <TextInput type="number" min="0" value={toleration.tolerationSeconds ?? ''}
                      onChange={(seconds) =>
                        setTolerations(tolerations.map((t, i) =>
                          i === index ? { ...t, tolerationSeconds: seconds === '' ? null : Number(seconds) } : t))} />
                  </Field>
                )}
                <button type="button" aria-label={`Delete toleration ${index + 1}`}
                  onClick={() => setTolerations(tolerations.filter((_, i) => i !== index))}>
                  <Trash2 size={16} />
                </button>
                </div>
              </div>
            ))}
            <button type="button" className="flex items-center gap-1 self-start text-sm underline"
              onClick={() =>
                setTolerations([...tolerations, { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null }])}>
              <Plus size={14} /> Add Toleration
            </button>
          </fieldset>
          {isEdit && (
            <p role="note" className="border p-2 text-sm">
              Please, be aware that any changes made to the <code>Scheduling</code> will require a{' '}
              <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank" rel="noreferrer"
                className="underline">
                restart operation
              </a>{' '}
              on the logs server
            </p>
          )}
          <fieldset className="flex flex-col gap-3 border p-3">
            <legend className="px-1 font-semibold">Non Production Settings</legend>
            <Field label="Cluster Pod Anti Affinity">{tristate(podAntiAffinity, setPodAntiAffinity)}</Field>
            <Field label="Patroni Resource Requirements">{tristate(patroniRequirements, setPatroniRequirements)}</Field>
            <Field label="Cluster Resource Requirements">{tristate(clusterRequirements, setClusterRequirements)}</Field>
          </fieldset>
        </>
      )}
    </CrdFormShell>
  )
}
