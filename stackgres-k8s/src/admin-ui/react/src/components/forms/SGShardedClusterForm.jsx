import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { Plus, Trash2 } from 'lucide-react'
import { api, ApiError } from '../../lib/api'
import { useData, useKindList } from '../../contexts/DataContext'
import { useSettings } from '../../contexts/SettingsContext'
import { nameValidationMessage } from '../../lib/names'
import {
  blankScriptEntry,
  cronPreview,
  isCronScheduleUnset,
  joinCronFields,
  tzCrontab,
} from '../../lib/sgcluster'
import {
  blankOverride,
  buildSGShardedCluster,
  hydrateSGShardedCluster,
  initialSGShardedClusterState,
} from '../../lib/sgshardedcluster'
import {
  ContainersEditor,
  CustomPortsEditor,
  CustomVolumesEditor,
  ExtensionsBrowser,
  KeyValueRows,
  ManagedSqlEditor,
  NodeAffinityTerms,
  TolerationsEditor,
} from './clusterEditors'
import SummaryDialog from './SummaryDialog'
import { Field, FieldGroup, FieldRow, renderTooltip, Select, TextInput, Toggle } from './fields'
import { HelpTooltip } from '../HelpTooltip'

const deep = (value) => JSON.parse(JSON.stringify(value))

const FORM_SECTIONS = ['general', 'coordinator', 'workers', 'overrides']
const SECTION_LABELS = {
  general: 'General',
  coordinator: 'Coordinator',
  workers: 'Workers',
  overrides: 'Overrides',
}

const FORM_STEPS = {
  general: ['cluster', 'extensions', 'backups', 'sidecars', 'pods-replication', 'metadata', 'non-production'],
  coordinator: ['coordinator', 'sidecars', 'scripts', 'pods-replication', 'services', 'metadata', 'scheduling'],
  workers: ['workers', 'sidecars', 'scripts', 'pods-replication', 'services', 'metadata', 'scheduling'],
  overrides: ['workers', 'sidecars', 'scripts', 'pods-replication', 'metadata', 'scheduling'],
}

// Basic-mode step cutoff per section (the Vue basicSteps map).
const BASIC_STEPS = { general: 3, coordinator: 2, workers: 2, overrides: 2 }

const STEP_LABELS = {
  cluster: 'Cluster',
  extensions: 'Extensions',
  backups: 'Backups',
  sidecars: 'Sidecars',
  'pods-replication': 'Pods Replication',
  metadata: 'Metadata',
  'non-production': 'Non Production',
  coordinator: 'Coordinator',
  workers: 'Workers',
  scripts: 'Scripts',
  services: 'Services',
  scheduling: 'Scheduling',
}

const SCRIPTS_INTRO = 'Use this option to run a set of scripts on your cluster.'
// Per-step section headings and intro copy, verbatim from the Vue fieldset
// headers, keyed `${section}.${step}`.
const STEP_HEADINGS = {
  'general.cluster': { title: 'General Information' },
  'general.extensions': {
    title: 'Postgres Extensions',
    tooltip: 'sgshardedcluster.spec.postgres.extensions',
  },
  'general.backups': { title: 'Backups' },
  'general.sidecars': { title: 'Sidecars' },
  'general.pods-replication': { title: 'Replication' },
  'general.metadata': { title: 'Metadata' },
  'general.non-production': { title: 'Non Production Settings' },
  'coordinator.coordinator': { title: 'Coordinator Information' },
  'coordinator.sidecars': { title: 'Sidecars' },
  'coordinator.scripts': {
    title: 'Managed SQL',
    intro: SCRIPTS_INTRO,
    tooltip: 'sgshardedcluster.spec.coordinator.managedSql.scripts',
  },
  'coordinator.pods-replication': { title: 'Replication' },
  'coordinator.services': { title: 'Customize generated Kubernetes service' },
  'coordinator.metadata': { title: 'Metadata' },
  'coordinator.scheduling': { title: 'Pods Scheduling' },
  'workers.workers': { title: 'Shards Information' },
  'workers.sidecars': { title: 'Sidecars' },
  'workers.scripts': {
    title: 'Managed SQL',
    intro: SCRIPTS_INTRO,
    tooltip: 'sgshardedcluster.spec.workers.managedSql.scripts',
  },
  'workers.pods-replication': { title: 'Replication' },
  'workers.services': { title: 'Customize generated Kubernetes service' },
  'workers.metadata': { title: 'Metadata' },
  'workers.scheduling': { title: 'Pods Scheduling' },
  'overrides.workers': { title: 'Shards Information' },
  'overrides.sidecars': { title: 'Sidecars' },
  'overrides.scripts': {
    title: 'Managed SQL',
    intro: SCRIPTS_INTRO,
    tooltip: 'sgshardedcluster.spec.workers.overrides.managedSql.scripts',
  },
  'overrides.pods-replication': { title: 'Replication' },
  'overrides.metadata': { title: 'Metadata' },
  'overrides.scheduling': { title: 'Pods Scheduling' },
}

const REPLICATION_MODES = ['async', 'sync', 'strict-sync', 'sync-all', 'strict-sync-all']

function StepHeader({ title, intro, tooltip }) {
  return (
    <header className="flex flex-col gap-1 border-b pb-2">
      <h2 className="text-sm font-semibold">{title}{renderTooltip(tooltip)}</h2>
      {intro && <p className="text-[hsl(var(--muted-foreground))]">{intro}</p>}
    </header>
  )
}

// Replication mode + sync instances editor shared by the top-level and
// per-section replication steps. Changing mode adds or removes the
// syncInstances key like the Vue @change handler did.
function ReplicationFields({ replication, maxSyncInstances, onChange, tooltipPrefix }) {
  return (
    <FieldRow>
      <Field label="Mode" required tooltip={tooltipPrefix ? `${tooltipPrefix}.mode` : undefined}>
        <Select required value={replication.mode} options={REPLICATION_MODES}
          onChange={(mode) => {
            const next = { ...replication, mode }
            if (['sync', 'strict-sync'].includes(mode)) {
              if (!Object.hasOwn(next, 'syncInstances') || !next.syncInstances) {
                next.syncInstances = 1
              }
            } else {
              delete next.syncInstances
            }
            onChange(next)
          }} />
      </Field>
      {['sync', 'strict-sync'].includes(replication.mode) && (
        <Field label="Sync Instances"
          tooltip={tooltipPrefix ? `${tooltipPrefix}.syncInstances` : undefined}>
          <TextInput type="number" min="1" max={Math.max(maxSyncInstances, 1)}
            value={replication.syncInstances ?? ''}
            onChange={(value) =>
              onChange({ ...replication, syncInstances: value === '' ? '' : Number(value) })} />
        </Field>
      )}
    </FieldRow>
  )
}

// Per-section metadata step: cluster-pods labels plus the five annotation
// groups, stored as editable rows on the section.
function SectionMetadata({ metadata, onChange, tooltipPrefix }) {
  const patchRows = (group, key, rows) =>
    onChange({
      ...metadata,
      [group]: { ...metadata[group], [key]: rows },
    })
  return (
    <div className="flex flex-col gap-3">
      <KeyValueRows legend="Cluster Pods Labels" keyName="label" rows={metadata.labels.clusterPods}
        tooltip={`${tooltipPrefix}.labels.clusterPods`}
        onChange={(rows) => patchRows('labels', 'clusterPods', rows)} />
      <KeyValueRows legend="All Resources Annotations" keyName="annotation"
        rows={metadata.annotations.allResources}
        tooltip={`${tooltipPrefix}.annotations.allResources`}
        onChange={(rows) => patchRows('annotations', 'allResources', rows)} />
      <KeyValueRows legend="Cluster Pods Annotations" keyName="annotation"
        rows={metadata.annotations.clusterPods}
        tooltip={`${tooltipPrefix}.annotations.clusterPods`}
        onChange={(rows) => patchRows('annotations', 'clusterPods', rows)} />
      <KeyValueRows legend="Services Annotations" keyName="annotation"
        rows={metadata.annotations.services}
        tooltip={`${tooltipPrefix}.annotations.services`}
        onChange={(rows) => patchRows('annotations', 'services', rows)} />
      <KeyValueRows legend="Primary Service Annotations" keyName="annotation"
        rows={metadata.annotations.primaryService}
        tooltip={`${tooltipPrefix}.annotations.primaryService`}
        onChange={(rows) => patchRows('annotations', 'primaryService', rows)} />
      <KeyValueRows legend="Replicas Service Annotations" keyName="annotation"
        rows={metadata.annotations.replicasService}
        tooltip={`${tooltipPrefix}.annotations.replicasService`}
        onChange={(rows) => patchRows('annotations', 'replicasService', rows)} />
    </div>
  )
}

// Per-section scheduling step over section.pods.scheduling.
function SectionScheduling({ scheduling, onChange, isEdit, tooltipPrefix }) {
  const affinity = scheduling.nodeAffinity
  return (
    <div className="flex flex-col gap-3">
      {isEdit && (
        <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">Changing scheduling settings on a running cluster may trigger pod restarts.</p>
      )}
      <KeyValueRows legend="Node Selectors" keyName="label" rows={scheduling.nodeSelector}
        tooltip={`${tooltipPrefix}.nodeSelector`}
        onChange={(rows) => onChange({ ...scheduling, nodeSelector: rows })} />
      <TolerationsEditor tolerations={scheduling.tolerations}
        tooltipPrefix={`${tooltipPrefix}.tolerations`}
        onChange={(tolerations) => onChange({ ...scheduling, tolerations })} />
      <NodeAffinityTerms legend="Node Affinity Required"
        tooltipPrefix={`${tooltipPrefix}.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution`}
        terms={affinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms}
        onChange={(terms) =>
          onChange({
            ...scheduling,
            nodeAffinity: {
              ...affinity,
              requiredDuringSchedulingIgnoredDuringExecution: { nodeSelectorTerms: terms },
            },
          })} />
      <NodeAffinityTerms legend="Node Affinity Preferred" preferred
        tooltipPrefix={`${tooltipPrefix}.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution`}
        terms={affinity.preferredDuringSchedulingIgnoredDuringExecution}
        onChange={(terms) =>
          onChange({
            ...scheduling,
            nodeAffinity: { ...affinity, preferredDuringSchedulingIgnoredDuringExecution: terms },
          })} />
    </div>
  )
}

export default function SGShardedClusterForm({ isEdit = false }) {
  const { namespace, name: routeName } = useParams()
  const navigate = useNavigate()
  const { timezone } = useSettings()
  const { refresh } = useData()

  const sgshardedclusters = useKindList('sgshardedclusters')
  const profiles = useKindList('sginstanceprofiles', namespace)
  const pgConfigs = useKindList('sgpgconfigs', namespace)
  const poolConfigs = useKindList('sgpoolconfigs', namespace)
  const objectStorages = useKindList('sgobjectstorages', namespace)
  const sgscripts = useKindList('sgscripts', namespace)
  const logsClusters = useKindList('sgdistributedlogs')

  const [state, setState] = useState(() =>
    isEdit ? null : initialSGShardedClusterState(namespace, { timezone })
  )
  const [currentSection, setCurrentSection] = useState('general')
  const [currentStep, setCurrentStep] = useState({
    general: 'cluster',
    coordinator: 'coordinator',
    workers: 'workers',
    overrides: 'workers',
  })
  const [overrideIndex, setOverrideIndex] = useState(0)
  const [advancedMode, setAdvancedMode] = useState(isEdit)
  const [storageClasses, setStorageClasses] = useState(null)
  const [versionsList, setVersionsList] = useState({})
  const [extensionsList, setExtensionsList] = useState({ vanilla: {}, babelfish: {} })
  const [searchExtension, setSearchExtension] = useState('')
  const [extLicense, setExtLicense] = useState('opensource')
  const [viewExtension, setViewExtension] = useState(-1)
  const [preview, setPreview] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const editCluster =
    isEdit && sgshardedclusters
      ? sgshardedclusters.find(
          (cluster) => cluster.metadata.name === routeName && cluster.metadata.namespace === namespace
        )
      : null
  if (isEdit && state === null && editCluster) {
    setState(hydrateSGShardedCluster(editCluster, { timezone }))
  }

  useEffect(() => {
    api.get('storageclasses').then(setStorageClasses).catch(() => setStorageClasses(null))
  }, [])

  const flavor = state?.flavor || 'vanilla'
  const postgresVersion = state?.postgresVersion || 'latest'

  useEffect(() => {
    if (versionsList[flavor]) {
      return
    }
    api
      .getPostgresVersions(flavor)
      .then((data) => {
        const grouped = {}
        ;(data.postgresql || []).forEach((version) => {
          const major = version.substring(0, version.indexOf('.'))
          grouped[major] = grouped[major] || []
          grouped[major].push(version)
        })
        setVersionsList((previous) => ({ ...previous, [flavor]: grouped }))
      })
      .catch(() => {})
  }, [flavor, versionsList])

  const majors = Object.keys(versionsList[flavor] || {}).sort((a, b) => Number(b) - Number(a))
  const shortPostgresVersion =
    postgresVersion === 'latest' ? majors[0] || '' : postgresVersion.substring(0, 2)
  const extensions = extensionsList[flavor]?.[postgresVersion] || null

  useEffect(() => {
    if (!state || extensionsList[flavor]?.[postgresVersion]) {
      return
    }
    api
      .getPostgresExtensions(postgresVersion, flavor)
      .then((data) => {
        const parsed = (data.extensions || []).map((extension) => ({
          ...extension,
          selectedVersion: extension.versions.length ? extension.versions[0] : '',
        }))
        parsed.sort((a, b) => (a.name > b.name ? 1 : b.name > a.name ? -1 : 0))
        setExtensionsList((previous) => ({
          ...previous,
          [flavor]: { ...previous[flavor], [postgresVersion]: parsed },
        }))
      })
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [flavor, postgresVersion, state === null])

  if (isEdit && state === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  const patch = (updater) =>
    setState((current) => {
      const next = deep(current)
      updater(next)
      return next
    })

  // Patches the active coordinator/workers/override section.
  const patchSection = (updater) =>
    patch((draft) => {
      const target =
        currentSection === 'overrides' ? draft.workers.overrides[overrideIndex] : draft[currentSection]
      updater(target, draft)
    })

  const section =
    currentSection === 'general'
      ? null
      : currentSection === 'overrides'
        ? state.workers.overrides[overrideIndex]
        : state[currentSection]
  const sectionScriptSource =
    currentSection === 'overrides'
      ? state.scriptSource.overrides[overrideIndex] || []
      : state.scriptSource[currentSection]
  const sectionVolumeTypes =
    currentSection === 'overrides'
      ? state.customVolumesType.overrides[overrideIndex] || []
      : state.customVolumesType[currentSection]

  // Tooltip key prefix for the active coordinator/workers/override section;
  // overrides resolve against sgshardedcluster.spec.workers.overrides, as in Vue.
  const sectionTooltipPrefix =
    currentSection === 'overrides'
      ? 'sgshardedcluster.spec.workers.overrides'
      : `sgshardedcluster.spec.${currentSection}`

  const steps = FORM_STEPS[currentSection]
  const visibleSteps = steps.filter((step, index) => advancedMode || index < BASIC_STEPS[currentSection])
  const activeStep = currentStep[currentSection]
  const activeStepIndex = steps.indexOf(activeStep)
  const setStep = (step) => setCurrentStep((previous) => ({ ...previous, [currentSection]: step }))

  // --- monitoring interlocks (Vue toggleMonitoring / checkEnableMonitoring) --

  const toggleMonitoring = (value) =>
    patch((draft) => {
      draft.enableMonitoring = value
      draft.prometheusAutobind = value
      draft.coordinator.pods.disableMetricsExporter = !value
      draft.workers.pods.disableMetricsExporter = !value
    })
  const checkEnableMonitoring = (draft) => {
    draft.enableMonitoring =
      draft.prometheusAutobind &&
      !draft.coordinator.pods.disableMetricsExporter &&
      !draft.workers.pods.disableMetricsExporter
  }

  // --- extensions browser -----------------------------------------------------

  const setExtension = (extension) => {
    const index = state.selectedExtensions.findIndex((selected) => selected.name === extension.name)
    if (index === -1 && !extension.selectedVersion.length) {
      toast.message('You must first select a version for the extension you wish to enable')
      return
    }
    patch((draft) => {
      if (index !== -1) {
        draft.selectedExtensions.splice(index, 1)
      } else {
        draft.selectedExtensions.push({
          name: extension.name,
          version: extension.selectedVersion,
          publisher: extension.publisher,
          repository: extension.repository,
        })
      }
    })
  }

  const updateExtVersion = (extensionName, version) => {
    setExtensionsList((previous) => ({
      ...previous,
      [flavor]: {
        ...previous[flavor],
        [postgresVersion]: previous[flavor][postgresVersion].map((extension) =>
          extension.name === extensionName ? { ...extension, selectedVersion: version } : extension
        ),
      },
    }))
    patch((draft) => {
      const selected = draft.selectedExtensions.find((entry) => entry.name === extensionName)
      if (selected) {
        selected.version = version
      }
    })
  }

  // --- backups ---------------------------------------------------------------

  const changeCronField = (field, value) =>
    patch((draft) => {
      draft.cronSchedule[0][field] = value
      const fields = draft.cronSchedule[0]
      draft.backups[0].cronSchedule = isCronScheduleUnset(fields)
        ? null
        : tzCrontab(joinCronFields(fields), { toLocal: false, timezone })
    })

  // --- overrides -------------------------------------------------------------

  const pushOverride = () =>
    patch((draft) => {
      draft.workers.overrides.push(blankOverride())
      draft.scriptSource.overrides.push([])
      draft.customVolumesType.overrides.push([])
    })

  const deleteOverride = (index) => {
    patch((draft) => {
      draft.workers.overrides.splice(index, 1)
      draft.scriptSource.overrides.splice(index, 1)
      draft.customVolumesType.overrides.splice(index, 1)
    })
    setOverrideIndex((previous) => (index <= previous && previous > 0 ? previous - 1 : previous))
    setStep('workers')
  }

  const takenOverrideIndexes = state.workers.overrides.map((override) => override.index)

  // --- submit ----------------------------------------------------------------

  const nameCollision =
    !isEdit &&
    (sgshardedclusters || []).some(
      (cluster) => cluster.metadata.name === state.name && cluster.metadata.namespace === namespace
    )

  const createCluster = async ({ previewOnly = false, dryRunFlag = false } = {}) => {
    const nameError = nameValidationMessage(state.name)
    if (nameError) {
      toast.error(nameError)
      return
    }
    if (nameCollision) {
      toast.error(`A sharded cluster named "${state.name}" already exists in this namespace`)
      return
    }
    setSubmitting(true)
    try {
      let previous = null
      try {
        previous = await api.getResourceDetails('sgshardedclusters', namespace, state.name)
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 404) {
          toast.error(error.message)
          return
        }
      }
      const cluster = buildSGShardedCluster(state, previous)
      if (previewOnly) {
        setPreview({ cluster, dryRun: false })
        return
      }
      const response = isEdit
        ? await api.update('sgshardedclusters', cluster, dryRunFlag)
        : await api.create('sgshardedclusters', cluster, dryRunFlag)
      if (dryRunFlag) {
        setPreview({ cluster: response || cluster, dryRun: true })
        return
      }
      toast.success(`Sharded cluster "${state.name}" ${isEdit ? 'updated' : 'created'} successfully`)
      refresh()
      navigate(isEdit ? `/${namespace}/sgshardedcluster/${state.name}` : `/${namespace}/sgshardedclusters`)
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
    } finally {
      setSubmitting(false)
    }
  }

  // --- option lists ----------------------------------------------------------

  const profileOptions = (profiles || []).map((profile) => ({
    value: profile.metadata.name,
    label: `${profile.metadata.name} (Cores: ${profile.spec.cpu}, RAM: ${profile.spec.memory}B)`,
  }))
  const pgConfigOptions = (pgConfigs || [])
    .filter((config) => config.spec.postgresVersion === shortPostgresVersion)
    .map((config) => config.metadata.name)
  const poolConfigOptions = (poolConfigs || []).map((config) => config.metadata.name)
  const objectStorageOptions = (objectStorages || []).map((storage) => storage.metadata.name)
  const scriptOptions = (sgscripts || []).map((script) => script.metadata.name)
  const logsClusterOptions = (logsClusters || []).map((cluster) =>
    cluster.metadata.namespace === namespace
      ? cluster.metadata.name
      : `${cluster.metadata.namespace}.${cluster.metadata.name}`
  )

  // --- step renderers ----------------------------------------------------------

  const generalClusterStep = (
    <div className="flex flex-col gap-4">
      <FieldGroup>
        <FieldRow>
          <Field label="Sharded Cluster Name" required tooltip="sgshardedcluster.metadata.name">
            <TextInput required disabled={isEdit} value={state.name}
              onChange={(value) => patch((draft) => { draft.name = value })} />
          </Field>
          <Field label="Profile" tooltip="sgshardedcluster.spec.profile">
            <Select value={state.profile} options={['production', 'testing', 'development']}
              onChange={(value) => patch((draft) => { draft.profile = value })} />
          </Field>
        </FieldRow>
        {nameCollision && (
          <p role="alert" className="text-sm text-red-500">
            A sharded cluster named &quot;{state.name}&quot; already exists in this namespace
          </p>
        )}
        {state.profile !== 'production' && (
          <div role="note" className="border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
            <p>By choosing this Profile, the following defaults are overwritten:</p>
            <ul className="list-disc pl-6">
              <li>Cluster Pod Anti Affinity is set to Disable.</li>
              {state.profile === 'development' && (
                <>
                  <li>Patroni Resource Requirements is set to Disable.</li>
                  <li>Cluster Resource Requirements is set to Disable.</li>
                </>
              )}
            </ul>
          </div>
        )}
      </FieldGroup>
      <FieldGroup title="Sharding">
        <FieldRow>
          <Field label="Database" required tooltip="sgshardedcluster.spec.database">
            <TextInput required disabled={isEdit} value={state.database}
              onChange={(value) => patch((draft) => { draft.database = value })} />
          </Field>
          <Field label="Type" required tooltip="sgshardedcluster.spec.type">
            <Select required disabled value={state.shardingType} options={['citus']}
              placeholder="Choose one…" onChange={() => {}} />
          </Field>
        </FieldRow>
      </FieldGroup>
      <FieldGroup title="Postgres">
        <FieldRow>
          <Field label="Postgres Version" required tooltip="sgshardedcluster.spec.postgres.version">
            <select required className="border p-2 rounded bg-[hsl(var(--input-background))]" value={postgresVersion} aria-label="Postgres Version"
              onChange={(event) => patch((draft) => { draft.postgresVersion = event.target.value })}>
              <option value="latest">Latest</option>
              {majors.map((major) => (
                <optgroup key={major} label={`Postgres ${major}`}>
                  {versionsList[flavor][major].map((version) => (
                    <option key={version} value={version}>
                      {version}
                    </option>
                  ))}
                </optgroup>
              ))}
            </select>
          </Field>
        </FieldRow>
      </FieldGroup>
      <FieldGroup title="SSL Connections">
        <Toggle label="SSL Connections" checked={state.ssl.enabled}
          tooltip="sgshardedcluster.spec.postgres.ssl.enabled"
          onChange={(value) => patch((draft) => { draft.ssl.enabled = value })} />
        {state.ssl.enabled && (
          <>
            <FieldRow>
              <Field label="Certificate Secret Name"
                tooltip="sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector.name">
                <TextInput value={state.ssl.certificateSecretKeySelector.name}
                  onChange={(value) =>
                    patch((draft) => { draft.ssl.certificateSecretKeySelector.name = value })} />
              </Field>
              <Field label="Certificate Secret Key"
                tooltip="sgshardedcluster.spec.postgres.ssl.certificateSecretKeySelector.key">
                <TextInput value={state.ssl.certificateSecretKeySelector.key}
                  onChange={(value) =>
                    patch((draft) => { draft.ssl.certificateSecretKeySelector.key = value })} />
              </Field>
            </FieldRow>
            <FieldRow>
              <Field label="Private Key Secret Name"
                tooltip="sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector.name">
                <TextInput value={state.ssl.privateKeySecretKeySelector.name}
                  onChange={(value) =>
                    patch((draft) => { draft.ssl.privateKeySecretKeySelector.name = value })} />
              </Field>
              <Field label="Private Key Secret Key"
                tooltip="sgshardedcluster.spec.postgres.ssl.privateKeySecretKeySelector.key">
                <TextInput value={state.ssl.privateKeySecretKeySelector.key}
                  onChange={(value) =>
                    patch((draft) => { draft.ssl.privateKeySecretKeySelector.key = value })} />
              </Field>
            </FieldRow>
          </>
        )}
      </FieldGroup>
      <FieldGroup title="Monitoring">
        <Toggle label="Monitoring" checked={state.enableMonitoring} onChange={toggleMonitoring}
          tooltip={
            <HelpTooltip text="StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack." />
          } />
      </FieldGroup>
      <FieldGroup title={<>Distributed Logs{renderTooltip('sgcluster.spec.distributedLogs')}</>}>
        <FieldRow>
          <Field label="Logs Cluster" tooltip="sgcluster.spec.distributedLogs.sgDistributedLogs">
            <Select value={state.distributedLogs}
              options={[{ value: '', label: 'Select Logs Cluster' }, ...logsClusterOptions]}
              onChange={(value) => patch((draft) => { draft.distributedLogs = value })} />
          </Field>
          {Boolean(state.distributedLogs.length) && (
            <Field label="Retention" tooltip="sgcluster.spec.distributedLogs.retention">
              <TextInput value={state.retention}
                onChange={(value) => patch((draft) => { draft.retention = value })} />
            </Field>
          )}
        </FieldRow>
      </FieldGroup>
    </div>
  )

  const generalBackupsStep = (
    <div className="flex flex-col gap-4">
      <FieldGroup>
        <FieldRow>
          <Toggle label="Managed Backups" checked={state.managedBackups}
            tooltip={
              <HelpTooltip text="If enabled, allows specifying backup configurations to automate periodical backups" />
            }
            onChange={(value) => patch((draft) => { draft.managedBackups = value })} />
          {state.managedBackups && (
            <Field label="Object Storage" required
              tooltip="sgshardedcluster.spec.configurations.backups.sgObjectStorage">
              <Select required value={state.backups[0].sgObjectStorage}
                options={[
                  {
                    value: '',
                    label: objectStorageOptions.length ? 'Select Storage' : 'No object storage available',
                  },
                  ...objectStorageOptions,
                ]}
                onChange={(value) => patch((draft) => { draft.backups[0].sgObjectStorage = value })} />
            </Field>
          )}
        </FieldRow>
      </FieldGroup>
      {state.managedBackups && (
        <>
          <h4 className="text-sm font-semibold">
            Backup Schedule{renderTooltip('sgshardedcluster.spec.configurations.backups.cronSchedule')}
          </h4>
          <div className="flex flex-wrap gap-2">
            {[
              ['Minute', 'min'],
              ['Hour', 'hour'],
              ['Day of Month', 'dom'],
              ['Month', 'month'],
              ['Day of Week', 'dow'],
            ].map(([label, field]) => (
              <Field key={field} label={label}>
                <TextInput value={state.cronSchedule[0][field]}
                  onChange={(value) => changeCronField(field, value)} />
              </Field>
            ))}
          </div>
          <p>
            <strong>That is: </strong>
            {!isCronScheduleUnset(state.cronSchedule[0])
              ? cronPreview(state.backups[0].cronSchedule, timezone)
              : 'never'}
          </p>
          <FieldGroup title="Base Backup Details">
            <FieldRow>
              <Field label="Retention Window (max. number of base backups)"
                tooltip="sgshardedcluster.spec.configurations.backups.retention">
                <TextInput type="number" value={state.backups[0].retention}
                  onChange={(value) =>
                    patch((draft) => { draft.backups[0].retention = value === '' ? '' : Number(value) })} />
              </Field>
              <Field label="Compression Method"
                tooltip="sgshardedcluster.spec.configurations.backups.compression">
                <Select value={state.backups[0].compression} placeholder="Select a method"
                  options={[
                    { value: 'lz4', label: 'LZ4' },
                    { value: 'lzma', label: 'LZMA' },
                    { value: 'brotli', label: 'Brotli' },
                  ]}
                  onChange={(value) => patch((draft) => { draft.backups[0].compression = value })} />
              </Field>
            </FieldRow>
          </FieldGroup>
          <FieldGroup title="Performance Details">
            <FieldRow>
              <Field label="Max Network Bandwidth"
                tooltip="sgshardedcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth">
                <TextInput type="number" min="0" value={state.backups[0].performance.maxNetworkBandwidth}
                  onChange={(value) =>
                    patch((draft) => {
                      draft.backups[0].performance.maxNetworkBandwidth = value === '' ? '' : Number(value)
                    })} />
              </Field>
              <Field label="Max Disk Bandwidth"
                tooltip="sgshardedcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth">
                <TextInput type="number" min="0" value={state.backups[0].performance.maxDiskBandwidth}
                  onChange={(value) =>
                    patch((draft) => {
                      draft.backups[0].performance.maxDiskBandwidth = value === '' ? '' : Number(value)
                    })} />
              </Field>
              <Field label="Upload Disk Concurrency"
                tooltip="sgshardedcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency">
                <TextInput type="number" value={state.backups[0].performance.uploadDiskConcurrency}
                  onChange={(value) =>
                    patch((draft) => {
                      draft.backups[0].performance.uploadDiskConcurrency = value === '' ? '' : Number(value)
                    })} />
              </Field>
            </FieldRow>
          </FieldGroup>
          <FieldGroup title="Snapshot Details">
            <FieldRow>
              <Toggle label="Use Volume Snapshots" checked={state.backups[0].useVolumeSnapshot}
                tooltip="sgshardedcluster.spec.configurations.backups.useVolumeSnapshot"
                onChange={(value) => patch((draft) => { draft.backups[0].useVolumeSnapshot = value })} />
              {state.backups[0].useVolumeSnapshot && (
                <>
                  <Field label="Volume Snapshot Class"
                    tooltip="sgshardedcluster.spec.configurations.backups.volumeSnapshotClass">
                    <TextInput value={state.backups[0].volumeSnapshotClass ?? ''}
                      onChange={(value) =>
                        patch((draft) => { draft.backups[0].volumeSnapshotClass = value || null })} />
                  </Field>
                  <Toggle label="Fast Volume Snapshot" checked={state.backups[0].fastVolumeSnapshot}
                    tooltip="sgshardedcluster.spec.configurations.backups.fastVolumeSnapshot"
                    onChange={(value) =>
                      patch((draft) => { draft.backups[0].fastVolumeSnapshot = value })} />
                </>
              )}
            </FieldRow>
          </FieldGroup>
          <FieldGroup title="Extra Details">
            <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
              <legend className="px-2 text-sm font-medium">Paths (leave empty if unsure)</legend>
              {state.backups[0].paths.map((path, index) => (
                <div key={index} className="flex items-end gap-2">
                  <Field label={`${index === 0 ? 'Coordinator Path' : `Shard Path #${index}`} (leave empty if unsure)`}
                    tooltip="sgshardedcluster.spec.configurations.backups.paths">
                    <TextInput value={path ?? ''}
                      onChange={(value) =>
                        patch((draft) => { draft.backups[0].paths[index] = value })} />
                  </Field>
                  <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete path ${index + 1}`}
                    onClick={() => patch((draft) => { draft.backups[0].paths.splice(index, 1) })}>
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
              <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
                onClick={() => patch((draft) => { draft.backups[0].paths.push(null) })}>
                <Plus size={14} /> Add Path
              </button>
            </fieldset>
          </FieldGroup>
        </>
      )}
    </div>
  )

  const generalSidecarsStep = (
    <div className="flex flex-col gap-4">
      <FieldGroup title="Monitoring">
        <p>
          Enable Prometheus metrics scraping via service monitors. Check the{' '}
          <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank"
            rel="noreferrer" className="text-[hsl(var(--primary))] hover:underline">
            Installation -&gt; Monitoring
          </a>{' '}
          section for information on how to enable in StackGres Grafana dashboard integration.
        </p>
        <Toggle label="Prometheus Autobind" checked={state.prometheusAutobind}
          tooltip="sgcluster.spec.configurations.observability.prometheusAutobind"
          onChange={(value) =>
            patch((draft) => {
              draft.prometheusAutobind = value
              checkEnableMonitoring(draft)
            })} />
      </FieldGroup>
    </div>
  )

  const generalMetadataStep = (
    <div className="flex flex-col gap-3">
      <KeyValueRows legend="Cluster Pods Labels" keyName="label" rows={state.podsMetadata}
        tooltip="sgshardedcluster.spec.metadata.labels.clusterPods"
        onChange={(rows) => patch((draft) => { draft.podsMetadata = rows })} />
      <KeyValueRows legend="All Resources Annotations" keyName="annotation" rows={state.annotationsAll}
        tooltip="sgshardedcluster.spec.metadata.annotations.allResources"
        onChange={(rows) => patch((draft) => { draft.annotationsAll = rows })} />
      <KeyValueRows legend="Cluster Pods Annotations" keyName="annotation" rows={state.annotationsPods}
        tooltip="sgshardedcluster.spec.metadata.annotations.clusterPods"
        onChange={(rows) => patch((draft) => { draft.annotationsPods = rows })} />
      <KeyValueRows legend="Services Annotations" keyName="annotation" rows={state.annotationsServices}
        tooltip="sgshardedcluster.spec.metadata.annotations.services"
        onChange={(rows) => patch((draft) => { draft.annotationsServices = rows })} />
      <KeyValueRows legend="Primary Service Annotations" keyName="annotation"
        rows={state.postgresServicesPrimaryAnnotations}
        tooltip="sgshardedcluster.spec.metadata.annotations.primaryService"
        onChange={(rows) => patch((draft) => { draft.postgresServicesPrimaryAnnotations = rows })} />
      <KeyValueRows legend="Replicas Service Annotations" keyName="annotation"
        rows={state.postgresServicesReplicasAnnotations}
        tooltip="sgshardedcluster.spec.metadata.annotations.replicasService"
        onChange={(rows) => patch((draft) => { draft.postgresServicesReplicasAnnotations = rows })} />
    </div>
  )

  const nonProductionValue = (value) => (value === null ? '' : String(value))
  const parseNonProduction = (value) => (value === '' ? null : value === 'true')
  const generalNonProductionStep = (
    <FieldRow>
      {[
        ['Cluster Pod Anti Affinity', 'clusterPodAntiAffinity', 'disableClusterPodAntiAffinity'],
        ['Patroni Resource Requirements', 'patroniResourceRequirements', 'disablePatroniResourceRequirements'],
        ['Cluster Resource Requirements', 'clusterResourceRequirements', 'disableClusterResourceRequirements'],
      ].map(([label, key, tooltipKey]) => (
        <Field key={key} label={label}
          tooltip={
            <HelpTooltip field={`sgshardedcluster.spec.nonProductionOptions.${tooltipKey}`}
              transform={(t) => t.replace('Set this property to true','Disable this property')} />
          }>
          <Select value={nonProductionValue(state[key])}
            options={[
              { value: '', label: 'Default' },
              { value: 'false', label: 'Enable' },
              { value: 'true', label: 'Disable' },
            ]}
            onChange={(value) => patch((draft) => { draft[key] = parseNonProduction(value) })} />
        </Field>
      ))}
    </FieldRow>
  )

  const sectionInfoStep = () => {
    const override = currentSection === 'overrides'
    const workers = currentSection !== 'coordinator'
    const volume = section.pods.persistentVolume
    return (
      <div className="flex flex-col gap-4">
        {override && (
          <FieldGroup>
            <FieldRow>
              <Field label="Cluster Identifier" required
                tooltip="sgshardedcluster.spec.workers.overrides.index">
                <Select required value={section.index === null ? '' : String(section.index)}
                  placeholder="Choose one…"
                  options={Array.from({ length: parseInt(state.workers.clusters, 10) || 0 }, (_, i) => ({
                    value: String(i),
                    label: String(i),
                    disabled: takenOverrideIndexes.includes(i) && section.index !== i,
                  })).map(({ value, label }) => ({ value, label }))}
                  onChange={(value) =>
                    patchSection((target) => { target.index = value === '' ? null : Number(value) })} />
              </Field>
            </FieldRow>
          </FieldGroup>
        )}
        {workers && !override && (
          <FieldGroup title="Clusters">
            <FieldRow>
              <Field label="Number of Clusters" required
                tooltip="sgshardedcluster.spec.workers.clusters">
                <TextInput type="number" min="1" max="16" required value={section.clusters}
                  onChange={(value) =>
                    patchSection((target) => { target.clusters = value === '' ? '' : Number(value) })} />
              </Field>
            </FieldRow>
            {state.workers.clusters < state.workers.overrides.length && (
              <p role="alert" className="text-sm text-red-500">
                There are more shard overrides than clusters; remove the extra overrides in the
                Overrides section.
              </p>
            )}
          </FieldGroup>
        )}
        <FieldGroup title="Instances">
          <FieldRow>
            {!workers && (
              <Field label="Number of Instances" required
                tooltip="sgshardedcluster.spec.coordinator.instances">
                <TextInput type="number" min="0" required value={section.instances}
                  onChange={(value) =>
                    patchSection((target) => { target.instances = value === '' ? '' : Number(value) })} />
              </Field>
            )}
            {workers && (
              <Field label="Number of Instances per Cluster" required={!override}
                tooltip={`${sectionTooltipPrefix}.instancesPerCluster`}>
                <TextInput type="number" min="1" max="16" required={!override}
                  value={section.instancesPerCluster ?? ''}
                  onChange={(value) =>
                    patchSection((target) => {
                      target.instancesPerCluster = value === '' ? (override ? null : '') : Number(value)
                    })} />
              </Field>
            )}
            <Field label="Instance Profile" tooltip={`${sectionTooltipPrefix}.sgInstanceProfile`}>
              <Select value={section.sgInstanceProfile}
                options={[
                  { value: '', label: override ? 'Select profile' : 'Default (Cores: 1, RAM: 2GiB)' },
                  ...profileOptions,
                ]}
                onChange={(value) => patchSection((target) => { target.sgInstanceProfile = value })} />
            </Field>
          </FieldRow>
        </FieldGroup>
        <FieldGroup title="Postgres">
          <FieldRow>
            <Field label="Postgres Configuration"
              tooltip={`${sectionTooltipPrefix}.configurations.sgPostgresConfig`}>
              <Select value={section.configurations.sgPostgresConfig}
                options={[
                  { value: '', label: override ? 'Select configuration' : 'Default' },
                  ...pgConfigOptions,
                ]}
                onChange={(value) =>
                  patchSection((target) => { target.configurations.sgPostgresConfig = value })} />
            </Field>
          </FieldRow>
        </FieldGroup>
        <FieldGroup title="Pods Storage">
          <FieldRow>
            <div className="flex items-end gap-2">
              <Field label="Volume Size" required={!override}
                tooltip={`${sectionTooltipPrefix}.pods.persistentVolume.size`}>
                <TextInput type="number" min="0" required={!override} value={volume.size.size}
                  onChange={(value) =>
                    patchSection((target) => { target.pods.persistentVolume.size.size = value })} />
              </Field>
              <Field label="Volume Unit" required={!override}>
                <Select required={!override || volume.size.size !== ''} value={volume.size.unit}
                  placeholder={override ? 'Select Unit' : undefined}
                  options={['Mi', 'Gi', 'Ti']}
                  onChange={(value) =>
                    patchSection((target) => { target.pods.persistentVolume.size.unit = value })} />
              </Field>
            </div>
            <Field label="Storage Class"
              tooltip={`${sectionTooltipPrefix}.pods.persistentVolume.storageClass`}>
              {storageClasses === null ? (
                <TextInput value={volume.storageClass}
                  onChange={(value) =>
                    patchSection((target) => { target.pods.persistentVolume.storageClass = value })} />
              ) : (
                <Select value={volume.storageClass} disabled={!storageClasses.length}
                  options={[
                    {
                      value: '',
                      label: storageClasses.length ? 'Select Storage Class' : 'No storage classes available',
                    },
                    ...storageClasses,
                  ]}
                  onChange={(value) =>
                    patchSection((target) => { target.pods.persistentVolume.storageClass = value })} />
              )}
            </Field>
          </FieldRow>
        </FieldGroup>
      </div>
    )
  }

  const sectionSidecarsStep = () => (
    <div className="flex flex-col gap-4">
      <FieldGroup title="Connection Pooling">
        <p>
          To solve the Postgres connection fan-in problem (handling large number of incoming
          connections) StackGres includes by default a connection pooler fronting every Postgres
          instance. It is deployed as a sidecar. You may opt-out as well as tune the connection
          pooler configuration.
        </p>
        <FieldRow>
          <Toggle label="Connection Pooling" checked={!section.pods.disableConnectionPooling}
            tooltip={`${sectionTooltipPrefix}.configurations.sgPoolingConfig`}
            onChange={(value) =>
              patchSection((target) => { target.pods.disableConnectionPooling = !value })} />
          {!section.pods.disableConnectionPooling && (
            <Field label="Connection Pooling Configuration"
              tooltip={`${sectionTooltipPrefix}.configurations.sgPoolingConfig`}>
              <Select value={section.configurations.sgPoolingConfig}
                options={[
                  { value: '', label: currentSection === 'overrides' ? 'Select configuration' : 'Default' },
                  ...poolConfigOptions,
                ]}
                onChange={(value) =>
                  patchSection((target) => { target.configurations.sgPoolingConfig = value })} />
            </Field>
          )}
        </FieldRow>
      </FieldGroup>
      <FieldGroup title="Postgres Utils">
        <p>
          Sidecar container with Postgres administration tools. Optional (on by default; recommended
          for troubleshooting).
        </p>
        <Toggle label="Postgres Utils" checked={!section.pods.disablePostgresUtil}
          tooltip={
            <HelpTooltip field={`${sectionTooltipPrefix}.pods.disablePostgresUtil`}
              transform={(t) => t.replace('If set to `true`', 'If disabled')} />
          }
          onChange={(value) =>
            patchSection((target) => { target.pods.disablePostgresUtil = !value })} />
      </FieldGroup>
      <FieldGroup title="Monitoring">
        <p>
          Enable Prometheus metrics scraping via service monitors. Check the{' '}
          <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank"
            rel="noreferrer" className="text-[hsl(var(--primary))] hover:underline">
            Installation -&gt; Monitoring
          </a>{' '}
          section for information on how to enable in StackGres Grafana dashboard integration.
        </p>
        <Toggle label="Metrics Exporter" checked={!section.pods.disableMetricsExporter}
          tooltip={
            <HelpTooltip field={`${sectionTooltipPrefix}.pods.disableMetricsExporter`}
              transform={(t) =>
                t.replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')} />
          }
          onChange={(value) =>
            patchSection((target, draft) => {
              target.pods.disableMetricsExporter = !value
              checkEnableMonitoring(draft)
            })} />
      </FieldGroup>
      <h2 className="text-sm font-semibold">User-Supplied Pods Sidecars</h2>
      <CustomVolumesEditor volumes={section.pods.customVolumes} types={sectionVolumeTypes}
        tooltipPrefix={`${sectionTooltipPrefix}.pods.customVolumes`}
        onChange={(volumes, types) =>
          patchSection((target, draft) => {
            target.pods.customVolumes = volumes
            if (currentSection === 'overrides') {
              draft.customVolumesType.overrides[overrideIndex] = types
            } else {
              draft.customVolumesType[currentSection] = types
            }
          })} />
      <ContainersEditor legend="Custom Init Container" containers={section.pods.customInitContainers}
        tooltipPrefix={`${sectionTooltipPrefix}.pods.customInitContainers`}
        onChange={(containers) =>
          patchSection((target) => { target.pods.customInitContainers = containers })} />
      <ContainersEditor legend="Custom Container" containers={section.pods.customContainers}
        tooltipPrefix={`${sectionTooltipPrefix}.pods.customContainers`}
        onChange={(containers) =>
          patchSection((target) => { target.pods.customContainers = containers })} />
    </div>
  )

  const sectionScriptsStep = () => (
    <ManagedSqlEditor
      value={section.managedSql}
      sources={sectionScriptSource}
      scriptOptions={scriptOptions}
      isEdit={isEdit}
      blankEntry={blankScriptEntry}
      tooltipPrefix={`${sectionTooltipPrefix}.managedSql`}
      onChange={(managedSql, sources) =>
        patchSection((target, draft) => {
          target.managedSql = managedSql
          if (currentSection === 'overrides') {
            draft.scriptSource.overrides[overrideIndex] = sources
          } else {
            draft.scriptSource[currentSection] = sources
          }
        })}
    />
  )

  const sectionReplicationStep = () => {
    const maxSync =
      currentSection === 'coordinator'
        ? Number(state.coordinator.instances) - 1
        : currentSection === 'workers'
          ? Number(state.workers.instancesPerCluster) - 1
          : Number(section.instancesPerCluster ?? state.workers.instancesPerCluster) - 1
    return (
      <ReplicationFields replication={section.replication} maxSyncInstances={maxSync}
        tooltipPrefix={`${sectionTooltipPrefix}.replication`}
        onChange={(replication) => patchSection((target) => { target.replication = replication })} />
    )
  }

  const servicesStep = () => {
    const services =
      currentSection === 'coordinator'
        ? [
            ['primary', 'Primary Service'],
            ['any', 'Any Service'],
          ]
        : [['primaries', 'Primaries Service']]
    const group = state.postgresServices[currentSection]
    const servicesPrefix = `sgshardedcluster.spec.postgresServices.${currentSection}`
    return (
      <div className="flex flex-col gap-4">
        {services.map(([key, label]) => (
          <fieldset key={key} className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm font-medium">
              {label}{renderTooltip(`${servicesPrefix}.${key}`)}
            </legend>
            <FieldRow>
              <Toggle label="Enabled" checked={group[key].enabled}
                tooltip={`${servicesPrefix}.${key}.enabled`}
                onChange={(value) =>
                  patch((draft) => { draft.postgresServices[currentSection][key].enabled = value })} />
              <Field label="Type" required tooltip={`${servicesPrefix}.${key}.type`}>
                <Select required value={group[key].type} aria-label={`${label} type`}
                  options={['ClusterIP', 'LoadBalancer', 'NodePort']}
                  onChange={(value) =>
                    patch((draft) => { draft.postgresServices[currentSection][key].type = value })} />
              </Field>
              <Field label="Load Balancer IP" tooltip={`${servicesPrefix}.${key}.loadBalancerIP`}>
                <TextInput value={group[key].loadBalancerIP || ''} aria-label={`${label} load balancer IP`}
                  onChange={(value) =>
                    patch((draft) => {
                      draft.postgresServices[currentSection][key].loadBalancerIP = value
                    })} />
              </Field>
            </FieldRow>
          </fieldset>
        ))}
        <CustomPortsEditor legend="Custom Ports" ports={group.customPorts || []}
          tooltipPrefix={`${servicesPrefix}.customPorts`}
          onChange={(ports) =>
            patch((draft) => { draft.postgresServices[currentSection].customPorts = ports })} />
      </div>
    )
  }

  const stepContent = () => {
    if (currentSection === 'general') {
      switch (activeStep) {
        case 'cluster':
          return generalClusterStep
        case 'extensions':
          return (
            <ExtensionsBrowser extensions={extensions} selectedExtensions={state.selectedExtensions}
              search={searchExtension} onSearch={setSearchExtension} license={extLicense}
              onLicense={setExtLicense} view={viewExtension} onView={setViewExtension}
              onToggle={setExtension} onVersion={updateExtVersion} />
          )
        case 'backups':
          return generalBackupsStep
        case 'sidecars':
          return generalSidecarsStep
        case 'pods-replication':
          return (
            <ReplicationFields replication={state.replication}
              tooltipPrefix="sgshardedcluster.spec.replication"
              maxSyncInstances={Number(state.workers.instancesPerCluster) - 1}
              onChange={(replication) => patch((draft) => { draft.replication = replication })} />
          )
        case 'metadata':
          return generalMetadataStep
        case 'non-production':
          return generalNonProductionStep
        default:
          return null
      }
    }
    if (currentSection === 'overrides' && !state.workers.overrides.length) {
      return null
    }
    switch (activeStep) {
      case 'coordinator':
      case 'workers':
        return sectionInfoStep()
      case 'sidecars':
        return sectionSidecarsStep()
      case 'scripts':
        return sectionScriptsStep()
      case 'pods-replication':
        return sectionReplicationStep()
      case 'services':
        return servicesStep()
      case 'metadata':
        return (
          <SectionMetadata metadata={section.metadata}
            tooltipPrefix={`${sectionTooltipPrefix}.metadata`}
            onChange={(metadata) => patchSection((target) => { target.metadata = metadata })} />
        )
      case 'scheduling':
        return (
          <SectionScheduling scheduling={section.pods.scheduling} isEdit={isEdit}
            tooltipPrefix={`${sectionTooltipPrefix}.pods.scheduling`}
            onChange={(scheduling) =>
              patchSection((target) => { target.pods.scheduling = scheduling })} />
        )
      default:
        return null
    }
  }

  const showEmptyOverrides = currentSection === 'overrides' && !state.workers.overrides.length
  const heading = STEP_HEADINGS[`${currentSection}.${activeStep}`]

  return (
    <section className="flex max-w-5xl flex-col gap-4 px-6 pb-6 text-sm">
      <header className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">
          {isEdit ? 'Edit' : 'Create'} Sharded Cluster{isEdit ? `: ${state.name}` : ''}
        </h1>
        <Toggle label="Advanced options" checked={advancedMode}
          onChange={(value) => {
            setAdvancedMode(value)
            if (!value && activeStepIndex >= BASIC_STEPS[currentSection]) {
              setStep(FORM_STEPS[currentSection][0])
            }
          }} />
      </header>
      <nav aria-label="Form sections" className="flex gap-1 border-b pb-2 text-sm">
        {FORM_SECTIONS.map((candidate) => (
          <button key={candidate} type="button" onClick={() => setCurrentSection(candidate)}
            className={`px-2.5 py-1 rounded-md transition-colors ${currentSection === candidate ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))] font-medium' : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]'}`}>
            {SECTION_LABELS[candidate]}
          </button>
        ))}
      </nav>
      {currentSection === 'overrides' && state.workers.overrides.length > 0 && (
        <nav aria-label="Overrides" className="flex items-center gap-1 border-b pb-2 text-sm">
          {state.workers.overrides.map((override, index) => (
            <span key={index} className="flex items-center gap-1">
              <button type="button" onClick={() => setOverrideIndex(index)}
                className={`px-2.5 py-1 rounded-md transition-colors ${index === overrideIndex ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))] font-medium' : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]'}`}>
                Override #{index}
              </button>
              <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete override ${index}`}
                onClick={() => deleteOverride(index)}>
                <Trash2 size={14} />
              </button>
            </span>
          ))}
          <button type="button" className="flex items-center gap-1 rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
            disabled={state.workers.overrides.length >= state.workers.clusters}
            onClick={pushOverride}>
            <Plus size={14} /> Add Override
          </button>
        </nav>
      )}
      {!showEmptyOverrides && (
        <nav aria-label="Form steps" className="flex flex-wrap gap-1 border-b pb-2 text-sm">
          {visibleSteps.map((step) => (
            <button key={step} type="button" onClick={() => setStep(step)}
              className={`px-2.5 py-1 rounded-md transition-colors ${activeStep === step ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))] font-medium' : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]'}`}>
              {STEP_LABELS[step]}
            </button>
          ))}
        </nav>
      )}
      {showEmptyOverrides ? (
        <div className="flex flex-col items-start gap-3">
          <p>No shard overrides defined yet.</p>
          <button type="button" className="flex items-center gap-1 rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
            disabled={state.workers.overrides.length >= state.workers.clusters}
            onClick={pushOverride}>
            <Plus size={14} /> Add Override
          </button>
        </div>
      ) : (
        <form
          onSubmit={(event) => {
            event.preventDefault()
            createCluster()
          }}
          className="flex flex-col gap-4">
          {heading && <StepHeader {...heading} />}
          {stepContent()}
          <div className="flex items-center justify-end gap-2 border-t pt-4 mt-4">
            <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" disabled={activeStepIndex === 0}
              onClick={() => setStep(steps[activeStepIndex - 1])}>
              Previous
            </button>
            <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]"
              disabled={
                activeStepIndex === steps.length - 1 ||
                (!advancedMode && activeStepIndex === BASIC_STEPS[currentSection] - 1)
              }
              onClick={() => setStep(steps[activeStepIndex + 1])}>
              Next
            </button>
            <button type="submit" disabled={submitting} className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]">
              {isEdit ? 'Update Cluster' : 'Create Cluster'}
            </button>
            <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" disabled={submitting}
              onClick={() => createCluster({ previewOnly: true })}>
              View Summary
            </button>
            <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" disabled={submitting}
              onClick={() => createCluster({ dryRunFlag: true })}>
              Dry Run
            </button>
            <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]"
              onClick={() =>
                navigate(
                  isEdit
                    ? `/${namespace}/sgshardedcluster/${state.name}`
                    : `/${namespace}/sgshardedclusters`
                )}>
              Cancel
            </button>
          </div>
        </form>
      )}
      {preview && (
        <SummaryDialog
          title={preview.dryRun ? 'Dry Run Summary' : 'Summary'}
          value={preview.cluster}
          onClose={() => setPreview(null)}
          defaults={
            preview.dryRun
              ? null
              : buildSGShardedCluster(initialSGShardedClusterState(namespace, { timezone }))
          }
        />
      )}
    </section>
  )
}
