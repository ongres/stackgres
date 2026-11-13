import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import Chart from 'react-apexcharts'
import { toast } from 'sonner'
import { Plus, Trash2, Sparkles } from 'lucide-react'
import { api, ApiError } from '../../lib/api'
import { useData, useKindList } from '../../contexts/DataContext'
import { useSettings } from '../../contexts/SettingsContext'
import { fromDateTimeLocal, toDateTimeLocal } from '../../lib/datetime'
import { nameValidationMessage } from '../../lib/names'
import {
  blankScriptEntry,
  buildSGCluster,
  cronPreview,
  hydrateSGCluster,
  initialSGClusterState,
  isCronScheduleUnset,
  joinCronFields,
  tzCrontab,
} from '../../lib/sgcluster'
import { HelpTooltip } from '../HelpTooltip'
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
import { Field, renderTooltip, Select, TextArea, TextInput, Toggle, FieldGroup, FieldRow } from './fields'

const deep = (value) => JSON.parse(JSON.stringify(value))

// Literal help texts carried over verbatim from CreateSGClusters.vue.
const TEMPLATE_PICKER_TOOLTIP = `Express templates provide predefined specs to suit most needs:
                                \n* **Minimal:** includes basic features, ideal for development or testing purposes on minimal environments
                                    \n  * **Cluster Profile:** Development
                                    \n  * **Instance Profile:** Cores: 1, RAM: 2GiB
                                    \n  * **Volume Size:** 1GiB
                                    \n  * **Connection Pooling:** Disabled
                                    \n  * **Postgres Extensions:** Not available
                                \n* **Basic:** includes the most standard features and some customizable specs, ideal for testing purposes
                                    \n  * **Cluster Profile:** Testing (customizable)
                                    \n  * **Instance Profile:** Cores: 1, RAM: 2GiB (customizable)
                                    \n  * **Volume Size:** 1GiB
                                    \n  * **Distributed Logs:** Enabled
                                    \n  * **Connection Pooling:** Enabled
                                    \n  * **Postgres Extensions:** Available
                                \n* **Full:** packed with fully customizable standard features, ideal for testing or production purposes
                                    \n  * **Cluster Profile:** Production (customizable)
                                    \n  * **Instance Profile:** Cores: 1, RAM: 2GiB (customizable)
                                    \n  * **Volume Size:** 1Gib (customizable)
                                    \n  * **Distributed Logs:** Enabled (customizable)
                                    \n  * **Postgres Extensions:** Available
                                    \n  * **Postgres Configuration:** Customizable
                                    \n  * **Connection Pooling:** Enabled
                                    \n  * **Connection Pooling Configuration:** Customizable
                                \n* **Custom:** fully customizable cluster creation packed with every feature available. Includes both, basic and advanced specs, ideal for development, testing or production environments.
                        `

const MONITORING_TOOLTIP =
  "StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack."

const DISTRIBUTED_LOGS_TOOLTIP =
  'Allows sending Postgres and Patroni logs to a central SGDistributedLogs instance. Optional: if not enabled, logs are sent to the standard output.'

const BABELFISH_TOOLTIP =
  "Enables Babelfish for PostgreSQL project, from <a href='https://babelfishpg.org' target='_blank'>babelfishpg.org</a>, adding a SQL Server compatibility layer"

const MANAGED_BACKUPS_TOOLTIP =
  'If enabled, allows specifying backup configurations to automate periodical backups'

const FORM_STEPS = {
  minimal: ['information'],
  basic: ['information', 'extensions'],
  full: ['information', 'extensions', 'configurations'],
  custom: [
    'cluster',
    'extensions',
    'backups',
    'initialization',
    'replicate-from',
    'scripts',
    'sidecars',
    'pods-replication',
    'services',
    'metadata',
    'scheduling',
    'non-production',
  ],
}

const STEP_LABELS = {
  information: 'Information',
  configurations: 'Configurations',
  cluster: 'Cluster',
  extensions: 'Extensions',
  backups: 'Backups',
  initialization: 'Initialization',
  'replicate-from': 'Replicate From',
  scripts: 'Scripts',
  sidecars: 'Sidecars',
  'pods-replication': 'Pods Replication',
  services: 'Services',
  metadata: 'Metadata',
  scheduling: 'Scheduling',
  'non-production': 'Non Production',
}

// Template cards (Vue express setups): a three-bar gauge lit progressively,
// sparkles for the fully manual option.
const TEMPLATES = [
  { id: 'minimal', label: 'Minimal', bars: 1 },
  { id: 'basic', label: 'Basic', bars: 2 },
  { id: 'full', label: 'Full', bars: 3 },
  { id: 'custom', label: 'Custom', bars: null },
]

function TemplateIcon({ bars }) {
  if (bars === null) {
    return <Sparkles className="h-10 w-10 text-[hsl(var(--primary))]" strokeWidth={1.5} aria-hidden="true" />
  }
  return (
    <span className="flex h-10 items-end gap-1" aria-hidden="true">
      {[1, 2, 3].map((bar) => (
        <span
          key={bar}
          className={`w-2 rounded-sm ${bar === 1 ? 'h-5' : bar === 2 ? 'h-7' : 'h-10'} ${
            bar <= bars ? 'bg-[hsl(var(--primary))]' : 'bg-[hsl(var(--primary)/0.2)]'
          }`}
        />
      ))}
    </span>
  )
}

const CLUSTER_PROFILES = ['production', 'testing', 'development']

// The Vue flavor cards' inline logos (CreateSGClusters.vue); the outline path
// follows the text color so both themes keep contrast.
const VANILLA_LOGO = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50"><path d="M49.1 30c-.3-.9-1.1-1.5-2-1.7-.5-.1-1 0-1.6.1-1.1.2-1.9.3-2.5.3 2.2-3.8 4.1-8.1 5.1-12.2 1.7-6.6.8-9.6-.3-10.9C45 2 40.9.1 35.9 0c-2.6 0-5 .5-6.2.9-1.1-.2-2.3-.3-3.6-.3-2.4 0-4.5.5-6.3 1.5-1-.3-2.6-.8-4.4-1.1C11.1.3 7.6.8 5 2.6 2 4.8.5 8.6.8 13.9c.1 1.7 1 6.8 2.5 11.7.9 2.8 1.8 5.1 2.7 6.9 1.3 2.5 2.8 4 4.4 4.5.9.3 2.5.5 4.2-.9.2.3.5.5.9.8.5.3 1.1.6 1.7.7 2.2.5 4.2.4 5.9-.3v2.1c.1 2.5.3 4.5.8 5.9 0 .1.1.2.1.3.3.8.7 2.1 1.8 3.1 1.1 1.1 2.5 1.4 3.7 1.4.6 0 1.2-.1 1.7-.2 1.9-.4 4-1 5.5-3.2 1.4-2.1 2.2-5.2 2.3-10.1v-.7h.4c1.9.1 4.2-.3 5.6-1 1.4-.5 5-2.4 4.1-4.9z" fill="currentColor"/><path fill="#336791" d="M45.9 30.5c-5.6 1.2-6-.8-6-.8 6-8.8 8.5-20.1 6.3-22.8-5.9-7.5-16-4-16.2-3.9h-.1c-1.1-.2-2.4-.4-3.8-.4-2.6 0-4.5.7-6 1.8 0 0-18.1-7.5-17.3 9.4.3 3.6 5.2 27.2 11.1 20 2.2-2.6 4.3-4.8 4.3-4.8 1 .7 2.3 1 3.6.9l.1-.1v1c-1.5 1.7-1.1 2-4.1 2.6-3.1.6-1.3 1.8-.1 2.1 1.4.4 4.7.9 7-2.3l-.1.4c.6.5 1 3.1.9 5.5-.1 2.4-.1 4 .3 5.3.5 1.3.9 4.2 4.9 3.3C34 47 35.8 45.1 36 42c.2-2.2.6-1.9.6-3.9l.3-.9c.4-3 .1-4 2.1-3.5h.5c1.5.1 3.5-.2 4.7-.8 2.7-1 4.1-3 1.7-2.4z"/><g fill="#FFF"><path d="M47.7 30.3c-.2-.7-.9-.9-2.1-.6-3.4.7-4.6.2-5-.1 2.7-4.1 4.8-8.9 6-13.5.6-2.2.9-4.2.9-5.8 0-1.8-.3-3.1-.9-3.9-2.5-3.3-6.3-5-10.8-5-3.1 0-5.7.8-6.2 1-1.1-.3-2.2-.4-3.5-.4-2.3 0-4.3.5-6 1.7-.8-.3-2.6-.9-4.9-1.3-4-.6-7.1-.2-9.4 1.4-2.7 1.8-3.9 5.2-3.7 10 .1 1.6 1 6.6 2.4 11.3 1.9 6.3 4 9.8 6.2 10.5.3.1.6.1.9.1.8 0 1.8-.4 2.8-1.6 1.7-2 3.3-3.7 3.9-4.4.9.5 1.8.7 2.8.7v.1c-.2.2-.3.4-.5.6-.7.9-.8 1-3 1.5-.6.1-2.2.5-2.3 1.6 0 1.2 1.9 1.8 2.1 1.8.8.2 1.5.3 2.2.3 1.7 0 3.2-.6 4.5-1.7 0 4.4.1 8.8.7 10.2.4 1.1 1.5 3.8 4.9 3.8.5 0 1-.1 1.6-.2 3.5-.8 5-2.3 5.6-5.7.3-1.8.9-6.2 1.1-8.6.5.2 1.2.2 2 .2 1.6 0 3.4-.3 4.5-.9 1.4-.4 3.7-1.8 3.2-3.1zm-8.3-15.8c0 .7-.1 1.3-.2 2s-.2 1.4-.2 2.3c0 .9.1 1.8.2 2.6.2 1.8.4 3.5-.4 5.3-.1-.2-.3-.5-.4-.8-.1-.2-.3-.7-.6-1.2-1.2-2.2-4.1-7.3-2.6-9.4.3-.5 1.4-1.2 4.2-.8zM36 2.7c4.1.1 7.3 1.6 9.5 4.5 1.7 2.2-.2 12.3-5.7 21.1l-.2-.2s0-.1-.1-.1c1.4-2.4 1.2-4.7.9-6.8-.1-.9-.2-1.7-.2-2.4 0-.8.1-1.5.2-2.2.1-.8.3-1.7.2-2.7v-.4c-.1-1-1.2-3.8-3.4-6.4-1.2-1.4-3-3-5.4-4.1 1.2-.1 2.6-.3 4.2-.3zM13.4 33.3c-1.1 1.3-1.9 1.1-2.1 1-1.7-.6-3.6-4.1-5.3-9.6-1.5-4.8-2.3-9.6-2.4-11-.3-4.3.8-7.3 3-8.9 3.7-2.6 9.7-1 12.2-.3l-.1.1c-4 4-3.9 10.9-3.9 11.4v.7c.1 1.2.2 3.3-.1 5.8-.3 2.3.4 4.5 1.9 6.1l.5.5c-.7.7-2.2 2.3-3.7 4.2zm4.2-5.7c-1.2-1.3-1.8-3.1-1.5-4.9.4-2.6.2-4.8.2-6v-.4c.6-.5 3.3-2 5.2-1.5.9.2 1.4.8 1.6 1.8 1.2 5.3.2 7.6-.7 9.4-.2.4-.3.7-.5 1.1l-.1.3c-.3.7-.5 1.4-.7 2-1.2-.2-2.5-.8-3.5-1.8zm.2 7.2c-.4-.1-.7-.3-.9-.4.2-.1.5-.2 1-.3 2.5-.5 2.9-.9 3.8-2 .2-.2.4-.5.7-.9.5-.5.7-.4 1-.3.3.1.6.5.7.9.1.2.1.6-.1.9-1.7 2.6-4.3 2.6-6.2 2.1zm13.3 12.3c-3.1.7-4.2-.9-4.9-2.7-.5-1.2-.7-6.4-.5-12.2V32c0-.1 0-.3-.1-.4-.2-.8-.8-1.5-1.5-1.8-.3-.1-.8-.3-1.4-.2.1-.6.4-1.2.6-1.8l.1-.3c.1-.3.3-.6.4-1 .8-1.9 2-4.4.7-10.2-.5-2.2-2-3.2-4.4-3-1.4.1-2.7.7-3.4 1.1-.1.1-.3.1-.4.2.2-2.2.9-6.3 3.4-8.8 1.6-1.6 3.8-2.4 6.4-2.4 5.2.1 8.5 2.7 10.3 4.9 1.6 1.9 2.5 3.8 2.8 4.8-2.6-.3-4.4.2-5.3 1.5-2 2.8 1.1 8.2 2.5 10.8.3.5.5.9.6 1.1.5 1.1 1.1 1.9 1.5 2.5.1.2.3.3.4.5-.8.2-2.2.8-2.1 3.4-.1 1.3-.8 7.5-1.2 9.7-.5 2.8-1.5 3.9-4.5 4.5zM44 32.3c-.8.4-2.2.7-3.4.7-1.4.1-2.1-.2-2.3-.3-.1-1.6.5-1.8 1.2-2 .1 0 .2-.1.3-.1.1 0 .1.1.2.1 1.1.8 3.2.8 6.1.2-.5.5-1.1 1-2.1 1.4z"/><path d="M22.4 16.2c.1-.4-.6-.7-1.2-.8-.5-.1-1 0-1.2.2-.1.1-.2.2-.2.3 0 .2.1.4.2.6.3.3.6.6 1 .6h.2c.6 0 1.2-.5 1.2-.9zM36.8 15.1c-.6.1-1.2.3-1.1.7 0 .3.5.7 1.1.7h.1c.4-.1.7-.3.8-.4.2-.2.3-.5.3-.6 0-.4-.6-.5-1.2-.4z"/></g></svg>`

const BABELFISH_LOGO = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50"><path fill="#3BF" d="M28.6 32c-.7-1.3-2.1-1.9-3.5-1.7-1.4.1-2.7-.9-2.9-2.3-.2-1.8.5-1.2 1.7-6.1.4-1.8.2-3.8-.9-5.4-.8-1.3-2.2-2-3.4-1.2-1.2.8-2 1.9-2.5 3.2-1.9 5 .9 8.8 3.5 10.9 1.1.9 1.6 2.4 1.3 3.9-.2 1.2.4 2.1 1.3 3 1.4-1.2 1.4-2.8.7-4.5l.2-.2c1.3 1.5 3.2 1.5 4.5.4zm-7.5-12.8c-.4.2-.9.1-1.2-.3-.2-.4-.1-.9.3-1.3.4-.2.9-.1 1.3.3.1.6 0 1.1-.4 1.3z"/><path fill="currentColor" d="M32.3 2.5h-.7c-1.5.2-3 .5-4.4 1-1.5.4-11.3-7-20.6-1.4C1.2 5.5-.6 12.6 2.8 18c.4.7.9 1.3 1.5 1.9 1.6 1.9 2.4 4.4 2.1 6.8-.7 5.2 2.3 9.1 7 9.1 2.1.1 4.1-.3 6.1-1.1.6-.3 1.1-.9 1.3-1.6.2-.9-.2-1.9-.9-2.5-4.2-3.6-5.6-8-3.9-12.5.5-1.6 1.6-3 3-3.9.5-.4 1.3-.5 1.9-.5 1.5.1 2.8.9 3.5 2.2 1.2 2 1.5 4.2 1 6.4-.3 1.5-.9 3-1.5 4.5-.6 1.4.2 2 1.3 2.2 1 .2 2.1.1 3-.2 3.7-1.3 8-.3 9.4 6.4.5 3.2-.2 6.6-3.4 7.7 0 0-6.3 2.2-5-8.4v-.4c0-.2-.2-.3-.4-.3-.1 0-.2.1-.2.1-.6.4-1.3.6-2 .5-.4 0-.7.3-.8.7-.2 1-.8 2-1.6 2.7l-.5.4c-.2.2-.3.5-.3.7 1.8 13.1 12 11 12 11 5.8-.4 13.5-7 13.5-30.7 0-8.3-7.4-16.7-16.6-16.7zm8.1 20.1c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>`

const FLAVOR_CARDS = [
  { value: 'vanilla', label: 'PostgreSQL Community', logo: VANILLA_LOGO },
  { value: 'babelfish', label: 'Babelfish (Experimental)', logo: BABELFISH_LOGO },
]

// Per-step section headings and intro copy, verbatim from the Vue fieldset
// headers.
const STEP_HEADINGS = {
  information: { title: 'Cluster Information' },
  cluster: { title: 'Cluster Information' },
  extensions: { title: 'Postgres Extensions', tooltip: 'sgcluster.spec.postgres.extensions' },
  configurations: { title: 'Configurations' },
  backups: { title: 'Backups' },
  initialization: {
    title: 'Cluster Initialization',
    intro:
      'Select a backup from the following timeline to initialize the cluster with the data from an existing backup.',
  },
  'replicate-from': {
    title: 'Replicate From',
    intro: 'Use this option to initialize the cluster with the data from an existing source.',
  },
  scripts: { title: 'Managed SQL', intro: 'Use this option to run a set of scripts on your cluster.' },
  sidecars: { title: 'Sidecars' },
  'pods-replication': { title: 'Replication' },
  services: { title: 'Customize generated Kubernetes service' },
  metadata: { title: 'Metadata' },
  scheduling: { title: 'Pods Scheduling' },
  'non-production': { title: 'Non Production Settings' },
}

function StepHeader({ title, intro, tooltip }) {
  return (
    <header className="flex flex-col gap-1 border-b pb-2">
      <h2 className="text-sm font-semibold">{title}{renderTooltip(tooltip)}</h2>
      {intro && <p className="text-[hsl(var(--muted-foreground))]">{intro}</p>}
    </header>
  )
}

// ---------------------------------------------------------------------------

export default function SGClusterForm({ isEdit = false }) {
  const { namespace, name: routeName } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { timezone } = useSettings()
  const { refresh } = useData()

  const sgclusters = useKindList('sgclusters')
  const profiles = useKindList('sginstanceprofiles', namespace)
  const pgConfigs = useKindList('sgpgconfigs', namespace)
  const poolConfigs = useKindList('sgpoolconfigs', namespace)
  const objectStorages = useKindList('sgobjectstorages', namespace)
  const sgscripts = useKindList('sgscripts', namespace)
  const sgbackups = useKindList('sgbackups', namespace)
  const logsClusters = useKindList('sgdistributedlogs')

  const restoreFromBackupParam = searchParams.get('restoreFromBackup') || ''
  const queryPostgresVersion = searchParams.get('postgresVersion') || ''

  const [state, setState] = useState(() =>
    isEdit
      ? null
      : initialSGClusterState(namespace, {
          timezone,
          postgresVersion: queryPostgresVersion || 'latest',
        })
  )
  const [formTemplate, setFormTemplate] = useState(() =>
    isEdit || restoreFromBackupParam ? 'custom' : null
  )
  const [currentStep, setCurrentStep] = useState(() =>
    restoreFromBackupParam ? 'initialization' : isEdit ? 'cluster' : null
  )
  const [advancedMode, setAdvancedMode] = useState(isEdit || Boolean(restoreFromBackupParam))
  const [enableDistributedLogs, setEnableDistributedLogs] = useState(false)
  const [configsText, setConfigsText] = useState({ sgPostgresConfig: null, sgPoolingConfig: null })
  const [storageClasses, setStorageClasses] = useState(null)
  const [versionsList, setVersionsList] = useState({})
  const [extensionsList, setExtensionsList] = useState({ vanilla: {}, babelfish: {} })
  const [searchExtension, setSearchExtension] = useState('')
  const [extLicense, setExtLicense] = useState('opensource')
  const [viewExtension, setViewExtension] = useState(-1)
  const [preview, setPreview] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const editCluster =
    isEdit && sgclusters
      ? sgclusters.find(
          (cluster) => cluster.metadata.name === routeName && cluster.metadata.namespace === namespace
        )
      : null
  if (isEdit && state === null && editCluster) {
    setState(hydrateSGCluster(editCluster, { timezone }))
  }

  useEffect(() => {
    api.get('storageclasses').then(setStorageClasses).catch(() => setStorageClasses(null))
  }, [])

  // Preselect the backup named in ?restoreFromBackup and derive the cluster
  // name from it, as the Vue chart mounted() handler did.
  const restoreParamApplied = useRef(false)
  useEffect(() => {
    if (
      isEdit ||
      restoreParamApplied.current ||
      !restoreFromBackupParam ||
      !state ||
      !sgbackups ||
      !sgbackups.some((backup) => backup.metadata.name === restoreFromBackupParam)
    ) {
      return
    }
    restoreParamApplied.current = true
    const suffix = `-${Date.now().toString(36)}`
    const generated = `restore-from-${restoreFromBackupParam}`
      .substring(0, 44 - suffix.length)
      .replace(/-+$/, '')
    setState((current) => ({
      ...current,
      restoreBackup: restoreFromBackupParam,
      name: current.name.length ? current.name : `${generated}${suffix}`,
    }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sgbackups, state === null])

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

  // --- template wizard -----------------------------------------------------

  const setupTemplate = (template, reset = false) => {
    if (reset) {
      setFormTemplate(null)
      setCurrentStep(null)
      setEnableDistributedLogs(false)
      setConfigsText({ sgPostgresConfig: null, sgPoolingConfig: null })
      patch((draft) => {
        draft.profile = 'production'
        draft.pgConfig = null
        draft.connectionPoolingConfig = null
      })
      return
    }
    setFormTemplate(template)
    setCurrentStep(FORM_STEPS[template][0])
    patch((draft) => {
      if (template === 'minimal') {
        draft.connPooling = false
        draft.profile = 'development'
      } else if (template === 'basic') {
        draft.profile = 'testing'
      }
      if (!['minimal', 'custom'].includes(template)) {
        draft.enableMonitoring = true
        draft.prometheusAutobind = true
        draft.metricsExporter = true
      }
    })
    if (!['minimal', 'custom'].includes(template)) {
      setEnableDistributedLogs(true)
    }
  }

  const steps = formTemplate ? FORM_STEPS[formTemplate] : []
  const visibleSteps = steps.filter((step, index) => {
    if (formTemplate === 'custom' && !advancedMode && index >= 3) {
      return false
    }
    if (step === 'initialization' && isEdit && !state.restoreBackup.length) {
      return false
    }
    return true
  })
  const currentStepIndex = steps.indexOf(currentStep)
  const stepBy = (direction) => {
    let index = currentStepIndex + direction
    while (index >= 0 && index < steps.length && !visibleSteps.includes(steps[index])) {
      index += direction
    }
    if (index >= 0 && index < steps.length) {
      setCurrentStep(steps[index])
    }
  }

  // --- monitoring interlocks (Vue checkenableMonitoring & friends) ----------

  const changeEnableMonitoring = (value) =>
    patch((draft) => {
      draft.enableMonitoring = value
      if (value) {
        draft.metricsExporter = true
        draft.prometheusAutobind = true
      } else {
        draft.prometheusAutobind = false
      }
    })
  const changeMetricsExporter = (value) =>
    patch((draft) => {
      draft.metricsExporter = value
      if (!value) {
        draft.enableMonitoring = false
      } else if (draft.prometheusAutobind) {
        draft.enableMonitoring = true
      }
    })
  const changePrometheusAutobind = (value) =>
    patch((draft) => {
      draft.prometheusAutobind = value
      if (!value) {
        draft.enableMonitoring = false
      } else if (draft.metricsExporter) {
        draft.enableMonitoring = true
      }
    })

  // --- postgres specs validation (Vue validatePostgresSpecs) ----------------

  const validatePostgresSpecs = (draftFlavor, draftVersion) => {
    const next = deep(state)
    next.flavor = draftFlavor
    next.postgresVersion = draftVersion
    if (draftFlavor === 'vanilla') {
      next.babelfishFeatureGates = false
    }
    const messages = []
    const available = extensionsList[draftFlavor]?.[draftVersion]
    if (available && next.selectedExtensions.length) {
      const missing = next.selectedExtensions.filter(
        (selected) =>
          !available.find(
            (extension) =>
              extension.name === selected.name && extension.versions.includes(selected.version)
          )
      )
      if (missing.length) {
        next.selectedExtensions = next.selectedExtensions.filter(
          (selected) => !missing.find((m) => m.name === selected.name)
        )
        messages.push(
          `The following extensions are not available on your preferred postgres flavor and version and have been disabled: ${missing
            .map((m) => m.name)
            .join(', ')}`
        )
      }
    }
    if (
      next.pgConfig &&
      pgConfigs &&
      !pgConfigs.find(
        (config) =>
          config.metadata.name === next.pgConfig &&
          config.spec.postgresVersion ===
            (draftVersion === 'latest' ? majors[0] || '' : draftVersion.substring(0, 2))
      )
    ) {
      next.pgConfig = ''
      messages.push(
        'The postgres configuration you selected is not available on the postgres version you chose and has been unset'
      )
    }
    setState(next)
    messages.forEach((message) => toast.message(message))
  }

  // --- extensions browser ----------------------------------------------------

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

  // --- PITR / initialization -------------------------------------------------

  const pitrBackups = (sgbackups || [])
    .filter(
      (backup) =>
        backup.status?.process?.status === 'Completed' &&
        (backup.status?.backupInformation?.postgresVersion || '').substring(0, 2) ===
          shortPostgresVersion
    )
    .map((backup) => ({
      x: backup.status.process.timing.stored,
      y: 1,
      name: backup.metadata.name,
      cluster: backup.spec.sgCluster,
      uid: backup.metadata.uid,
      isSnapshot: Object.hasOwn(backup.status, 'volumeSnapshot'),
    }))
    .sort((a, b) => (a.x > b.x ? 1 : -1))

  const restoreBackupIndex = pitrBackups.findIndex((backup) => backup.name === state.restoreBackup)

  const setPitrBackup = (dataPointIndex) =>
    patch((draft) => {
      if (restoreBackupIndex === dataPointIndex) {
        draft.restoreBackup = ''
        draft.pitr = ''
      } else {
        draft.restoreBackup = pitrBackups[dataPointIndex].name
        draft.pitr = ''
      }
    })

  const selectedPitrBackup = restoreBackupIndex !== -1 ? pitrBackups[restoreBackupIndex] : null
  const nextPitrBackup =
    restoreBackupIndex !== -1 && restoreBackupIndex + 1 < pitrBackups.length
      ? pitrBackups[restoreBackupIndex + 1]
      : null

  // The input's value, min and max are all interpreted by the browser in the
  // displayed frame (local or UTC), so every bound goes through the converter.
  const toInputFrame = (timestamp) => toDateTimeLocal(timestamp, timezone)
  const pitrDisplayValue = toInputFrame(state.pitr)

  const changePitr = (value) =>
    patch((draft) => {
      draft.pitr = fromDateTimeLocal(value, timezone)
    })

  // --- scripts ---------------------------------------------------------------

  // --- replicate from --------------------------------------------------------

  const blankUsers = () => ({
    superuser: { username: { name: '', key: '' }, password: { name: '', key: '' } },
    replication: { username: { name: '', key: '' }, password: { name: '', key: '' } },
    authenticator: { username: { name: '', key: '' }, password: { name: '', key: '' } },
  })

  const setReplicationSource = (source) =>
    patch((draft) => {
      draft.replicateFromSource = source
      if (!source.length) {
        draft.replicateFrom = {}
        return
      }
      const target = draft.replicateFrom
      if (source === 'cluster') {
        target.instance = { sgCluster: '' }
        delete target.storage
      }
      if (source.includes('external') && !target.instance?.external) {
        target.instance = { external: { host: '', port: '' } }
      }
      if (source === 'external') {
        delete target.storage
      }
      if (source.includes('storage') && !target.storage) {
        target.storage = {
          sgObjectStorage: '',
          path: '',
          performance: { downloadConcurrency: '', maxDiskBandwidth: '', maxNetworkBandwidth: '' },
        }
      }
      if (source === 'storage') {
        delete target.instance
      }
      if (['external', 'storage', 'external-storage'].includes(source)) {
        if (!target.users) {
          target.users = blankUsers()
        }
      } else {
        delete target.users
        delete target.storage
      }
    })

  // --- submit ----------------------------------------------------------------

  const nameCollision =
    !isEdit &&
    (sgclusters || []).some(
      (cluster) => cluster.metadata.name === state.name && cluster.metadata.namespace === namespace
    )

  const createTemplateResources = async (draft, dryRunFlag) => {
    const generatedName = `gen-${draft.name}-${Date.now().toString(36)}`
    if (enableDistributedLogs) {
      try {
        await api.create(
          'sgdistributedlogs',
          { metadata: { name: generatedName, namespace }, spec: { persistentVolume: { size: '1Gi' } } },
          dryRunFlag
        )
        if (!dryRunFlag) {
          draft.distributedLogs = generatedName
        }
      } catch (error) {
        toast.error(`There was an error when trying to create the distributed logs dependency: ${error.message}`)
      }
    }
    if (configsText.sgPostgresConfig !== null && configsText.sgPostgresConfig !== '') {
      try {
        await api.create(
          'sgpgconfigs',
          {
            metadata: { name: generatedName, namespace },
            spec: { postgresVersion: shortPostgresVersion, 'postgresql.conf': configsText.sgPostgresConfig },
          },
          dryRunFlag
        )
        if (!dryRunFlag) {
          draft.pgConfig = generatedName
        }
      } catch (error) {
        toast.error(`There was an error when trying to create the postgres configuration dependency: ${error.message}`)
      }
    }
    if (configsText.sgPoolingConfig !== null && configsText.sgPoolingConfig !== '') {
      try {
        await api.create(
          'sgpoolconfigs',
          {
            metadata: { name: generatedName, namespace },
            spec: { pgBouncer: { 'pgbouncer.ini': configsText.sgPoolingConfig } },
          },
          dryRunFlag
        )
        if (!dryRunFlag) {
          draft.connectionPoolingConfig = generatedName
        }
      } catch (error) {
        toast.error(`There was an error when trying to create the pooling configuration dependency: ${error.message}`)
      }
    }
  }

  const createCluster = async ({ previewOnly = false, dryRunFlag = false } = {}) => {
    const nameError = nameValidationMessage(state.name)
    if (nameError) {
      toast.error(nameError)
      return
    }
    if (nameCollision) {
      toast.error(`A cluster named "${state.name}" already exists in this namespace`)
      return
    }
    setSubmitting(true)
    try {
      let previous = null
      try {
        previous = await api.getResourceDetails('sgclusters', namespace, state.name)
      } catch (error) {
        if (!(error instanceof ApiError) || error.status !== 404) {
          toast.error(error.message)
          return
        }
      }
      const effective = deep(state)
      if (!previewOnly && formTemplate !== 'custom' && !isEdit) {
        await createTemplateResources(effective, dryRunFlag)
      }
      const cluster = buildSGCluster(effective, previous)
      if (previewOnly) {
        setPreview({ cluster, dryRun: false })
        return
      }
      const response = isEdit
        ? await api.update('sgclusters', cluster, dryRunFlag)
        : await api.create('sgclusters', cluster, dryRunFlag)
      if (dryRunFlag) {
        setPreview({ cluster: response || cluster, dryRun: true })
        return
      }
      toast.success(`Cluster "${state.name}" ${isEdit ? 'updated' : 'created'} successfully`)
      refresh()
      navigate(isEdit ? `/${namespace}/sgcluster/${state.name}` : `/${namespace}/sgclusters`)
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
    } finally {
      setSubmitting(false)
    }
  }

  // --- option lists ----------------------------------------------------------

  const profileOptions = (profiles || []).map((profile) => profile.metadata.name)
  const pgConfigOptions = (pgConfigs || [])
    .filter((config) => config.spec.postgresVersion === shortPostgresVersion)
    .map((config) => config.metadata.name)
  const poolConfigOptions = (poolConfigs || []).map((config) => config.metadata.name)
  const objectStorageOptions = (objectStorages || []).map((storage) => storage.metadata.name)
  const scriptOptions = (sgscripts || []).map((script) => script.metadata.name)
  const clusterOptions = (sgclusters || [])
    .filter((cluster) => cluster.metadata.namespace === namespace)
    .map((cluster) => cluster.metadata.name)
  const logsClusterOptions = (logsClusters || []).map((cluster) =>
    cluster.metadata.namespace === namespace
      ? cluster.metadata.name
      : `${cluster.metadata.namespace}.${cluster.metadata.name}`
  )

  // --- render helpers --------------------------------------------------------

  const identityGroups = (
    <>
      <FieldGroup>
        <FieldRow>
          <Field label="Cluster Name" required tooltip="sgcluster.metadata.name">
            <TextInput required disabled={isEdit} value={state.name}
              onChange={(value) => patch((draft) => { draft.name = value })} />
          </Field>
          {formTemplate !== 'minimal' && (
            <Field label="Profile" tooltip="sgcluster.spec.profile">
              <Select value={state.profile} options={CLUSTER_PROFILES}
                onChange={(value) => patch((draft) => { draft.profile = value })} />
            </Field>
          )}
        </FieldRow>
        {nameCollision && (
          <p role="alert" className="text-sm text-red-500">A cluster named &quot;{state.name}&quot; already exists in this namespace</p>
        )}
      </FieldGroup>
      <FieldGroup title="Instances">
        <FieldRow>
          <Field label="Number of Instances" required tooltip="sgcluster.spec.instances">
            <TextInput type="number" min="0" required value={state.instances}
              onChange={(value) => patch((draft) => { draft.instances = value === '' ? '' : Number(value) })} />
          </Field>
          {formTemplate !== 'minimal' && (
            <Field label="Instance Profile" tooltip="sgcluster.spec.sgInstanceProfile">
              <Select value={state.resourceProfile}
                options={[{ value: '', label: 'Default (Cores: 1, RAM: 2GiB)' }, ...profileOptions]}
                onChange={(value) => patch((draft) => { draft.resourceProfile = value })} />
            </Field>
          )}
        </FieldRow>
      </FieldGroup>
    </>
  )

  const informationFields = (full) => (
    <div className="flex flex-col gap-4">
      {identityGroups}
      <FieldGroup title="Postgres">
        <FieldRow>{postgresVersionPicker}</FieldRow>
      </FieldGroup>
      {full && (
        <>
          <FieldGroup title="Pods Storage">
            <FieldRow>{volumeFields}</FieldRow>
          </FieldGroup>
          <FieldGroup title="Sidecars">
            <Toggle label="Monitoring" checked={state.enableMonitoring} onChange={changeEnableMonitoring}
              tooltip={<HelpTooltip text={MONITORING_TOOLTIP} />} />
            <Toggle label="Distributed Logs" checked={enableDistributedLogs}
              onChange={setEnableDistributedLogs} tooltip={<HelpTooltip text={DISTRIBUTED_LOGS_TOOLTIP} />} />
          </FieldGroup>
        </>
      )}
    </div>
  )

  const postgresVersionPicker = (
    <Field label="Postgres Version" required tooltip="sgcluster.spec.postgres.version">
      <select required className="border p-2 rounded bg-[hsl(var(--input-background))]" value={postgresVersion} aria-label="Postgres Version"
        onChange={(event) => validatePostgresSpecs(flavor, event.target.value)}>
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
  )

  const volumeFields = (
    <div className="flex items-end gap-2">
      <Field label="Volume Size" required tooltip="sgcluster.spec.pods.persistentVolume.size">
        <TextInput type="number" min="0" required value={state.volumeSize}
          onChange={(value) => patch((draft) => { draft.volumeSize = value })} />
      </Field>
      <Field label="Volume Unit" required>
        <Select required value={state.volumeUnit} options={['Mi', 'Gi', 'Ti']}
          onChange={(value) => patch((draft) => { draft.volumeUnit = value })} />
      </Field>
    </div>
  )

  const extensionsStep = (
    <ExtensionsBrowser
      extensions={extensions}
      selectedExtensions={state.selectedExtensions}
      search={searchExtension}
      onSearch={setSearchExtension}
      license={extLicense}
      onLicense={setExtLicense}
      view={viewExtension}
      onView={setViewExtension}
      onToggle={setExtension}
      onVersion={updateExtVersion}
    />
  )

  const configurationsStep = (
    <div className="flex flex-col gap-3">
      <Field label="Postgres Configuration" tooltip="sgcluster.spec.configurations.sgPostgresConfig">
        <Select value={state.pgConfig ?? ''}
          options={[
            { value: '', label: 'Default' },
            ...pgConfigOptions,
            { value: 'createNewResource', label: 'Create new configuration…' },
          ]}
          onChange={(value) => {
            if (value !== 'createNewResource') {
              setConfigsText((previous) => ({ ...previous, sgPostgresConfig: null }))
            }
            patch((draft) => {
              draft.pgConfig = value === '' ? null : value
            })
          }} />
      </Field>
      {state.pgConfig === 'createNewResource' && (
        <Field label="postgresql.conf parameters">
          <TextArea value={configsText.sgPostgresConfig || ''}
            onChange={(value) => setConfigsText((previous) => ({ ...previous, sgPostgresConfig: value }))} />
        </Field>
      )}
      <Field label="Connection Pooling Configuration" tooltip="sgcluster.spec.configurations.sgPoolingConfig">
        <Select value={state.connectionPoolingConfig ?? ''}
          options={[
            { value: '', label: 'Default' },
            ...poolConfigOptions,
            { value: 'createNewResource', label: 'Create new configuration…' },
          ]}
          onChange={(value) => {
            if (value !== 'createNewResource') {
              setConfigsText((previous) => ({ ...previous, sgPoolingConfig: null }))
            }
            patch((draft) => {
              draft.connectionPoolingConfig = value === '' ? null : value
            })
          }} />
      </Field>
      {state.connectionPoolingConfig === 'createNewResource' && (
        <Field label="pgbouncer.ini parameters">
          <TextArea value={configsText.sgPoolingConfig || ''}
            onChange={(value) => setConfigsText((previous) => ({ ...previous, sgPoolingConfig: value }))} />
        </Field>
      )}
    </div>
  )

  const clusterStep = (
    <div className="flex flex-col gap-4">
      {identityGroups}
      {state.profile !== 'production' && (
        <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
          Non-production profiles relax resource requirements and anti-affinity; do not use them for
          production workloads.
        </p>
      )}
      <FieldGroup title="Postgres">
        <div className="flex flex-col gap-1 text-sm">
          <span className="text-[hsl(var(--muted-foreground))]">
            Postgres Flavor<span aria-hidden="true"> *</span>
            <HelpTooltip field="sgcluster.spec.postgres.flavor" />
          </span>
          <div role="radiogroup" aria-label="Postgres Flavor" className="grid max-w-xl grid-cols-2 gap-3">
            {FLAVOR_CARDS.map((card) => (
              <button key={card.value} type="button" role="radio" aria-checked={flavor === card.value}
                onClick={() => validatePostgresSpecs(card.value, postgresVersion)}
                className={`flex flex-col items-center gap-4 rounded-lg border p-6 transition-colors ${
                  flavor === card.value
                    ? 'border-[hsl(var(--primary))] bg-[hsl(var(--primary)/0.05)] dark:bg-[hsl(var(--primary)/0.1)]'
                    : 'border-[hsl(var(--border))] bg-[hsl(var(--muted)/0.5)] hover:border-[hsl(var(--muted-foreground))]'
                }`}>
                <span aria-hidden="true" className="h-12 w-12 [&>svg]:h-full [&>svg]:w-full"
                  dangerouslySetInnerHTML={{ __html: card.logo }} />
                <span className="text-sm font-medium">{card.label}</span>
              </button>
            ))}
          </div>
        </div>
        {flavor === 'babelfish' && !isEdit && (
          <Toggle label="Babelfish Experimental Feature" checked={state.babelfishFeatureGates}
            tooltip={<HelpTooltip text={BABELFISH_TOOLTIP} />}
            onChange={(value) => patch((draft) => { draft.babelfishFeatureGates = value })} />
        )}
        <FieldRow>
          {postgresVersionPicker}
          <Field label="Postgres Configuration" tooltip="sgcluster.spec.configurations.sgPostgresConfig">
            <Select value={state.pgConfig ?? ''}
              options={[{ value: '', label: 'Default' }, ...pgConfigOptions]}
              onChange={(value) => patch((draft) => { draft.pgConfig = value === '' ? null : value })} />
          </Field>
        </FieldRow>
      </FieldGroup>
      <FieldGroup title="SSL Connections">
        <Toggle label="SSL Connections" checked={state.ssl.enabled}
          tooltip="sgcluster.spec.postgres.ssl.enabled"
          onChange={(value) => patch((draft) => { draft.ssl.enabled = value })} />
        {state.ssl.enabled && (
          <>
            <FieldRow>
              <Field label="Certificate Secret Name"
                tooltip="sgcluster.spec.postgres.ssl.certificateSecretKeySelector.name">
                <TextInput value={state.ssl.certificateSecretKeySelector.name}
                  onChange={(value) => patch((draft) => { draft.ssl.certificateSecretKeySelector.name = value })} />
              </Field>
              <Field label="Certificate Secret Key"
                tooltip="sgcluster.spec.postgres.ssl.certificateSecretKeySelector.key">
                <TextInput value={state.ssl.certificateSecretKeySelector.key}
                  onChange={(value) => patch((draft) => { draft.ssl.certificateSecretKeySelector.key = value })} />
              </Field>
            </FieldRow>
            <FieldRow>
              <Field label="Private Key Secret Name"
                tooltip="sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.name">
                <TextInput value={state.ssl.privateKeySecretKeySelector.name}
                  onChange={(value) => patch((draft) => { draft.ssl.privateKeySecretKeySelector.name = value })} />
              </Field>
              <Field label="Private Key Secret Key"
                tooltip="sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.key">
                <TextInput value={state.ssl.privateKeySecretKeySelector.key}
                  onChange={(value) => patch((draft) => { draft.ssl.privateKeySecretKeySelector.key = value })} />
              </Field>
            </FieldRow>
          </>
        )}
      </FieldGroup>
      <FieldGroup title="Pods Storage">
        <FieldRow>
          {volumeFields}
          <Field label="Storage Class" tooltip="sgcluster.spec.pods.persistentVolume.storageClass">
            {storageClasses === null ? (
              <TextInput value={state.storageClass}
                onChange={(value) => patch((draft) => { draft.storageClass = value })} />
            ) : (
              <Select value={state.storageClass} disabled={!storageClasses.length}
                options={[{ value: '', label: 'Select Storage Class' }, ...storageClasses]}
                onChange={(value) => patch((draft) => { draft.storageClass = value })} />
            )}
          </Field>
        </FieldRow>
      </FieldGroup>
      <FieldGroup title="Monitoring">
        <Toggle label="Monitoring" checked={state.enableMonitoring} onChange={changeEnableMonitoring}
          tooltip={<HelpTooltip text={MONITORING_TOOLTIP} />} />
      </FieldGroup>
      <FieldGroup title="Distributed Logs">
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

  const backupsStep = (
    <div className="flex flex-col gap-3">
      <Toggle label="Managed Backups" checked={state.managedBackups}
        tooltip={<HelpTooltip text={MANAGED_BACKUPS_TOOLTIP} />}
        onChange={(value) => patch((draft) => { draft.managedBackups = value })} />
      {state.managedBackups && (
        <>
          <Field label="Object Storage" required
            tooltip="sgcluster.spec.configurations.backups.sgObjectStorage">
            <Select required value={state.backups[0].sgObjectStorage}
              options={[{ value: '', label: 'Select Object Storage' }, ...objectStorageOptions]}
              onChange={(value) => patch((draft) => { draft.backups[0].sgObjectStorage = value })} />
          </Field>
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
          <Field label="Retention Window" tooltip="sgcluster.spec.configurations.backups.retention">
            <TextInput type="number" value={state.backups[0].retention}
              onChange={(value) =>
                patch((draft) => { draft.backups[0].retention = value === '' ? '' : Number(value) })} />
          </Field>
          <Field label="Compression Method" tooltip="sgcluster.spec.configurations.backups.compression">
            <Select value={state.backups[0].compression} options={['lz4', 'lzma', 'brotli']}
              onChange={(value) => patch((draft) => { draft.backups[0].compression = value })} />
          </Field>
          <div className="flex flex-wrap gap-2">
            <Field label="Max Network Bandwidth"
              tooltip="sgcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth">
              <TextInput type="number" min="0" value={state.backups[0].performance.maxNetworkBandwidth}
                onChange={(value) =>
                  patch((draft) => {
                    draft.backups[0].performance.maxNetworkBandwidth = value === '' ? '' : Number(value)
                  })} />
            </Field>
            <Field label="Max Disk Bandwidth"
              tooltip="sgcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth">
              <TextInput type="number" min="0" value={state.backups[0].performance.maxDiskBandwidth}
                onChange={(value) =>
                  patch((draft) => {
                    draft.backups[0].performance.maxDiskBandwidth = value === '' ? '' : Number(value)
                  })} />
            </Field>
            <Field label="Upload Disk Concurrency"
              tooltip="sgcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency">
              <TextInput type="number" value={state.backups[0].performance.uploadDiskConcurrency}
                onChange={(value) =>
                  patch((draft) => {
                    draft.backups[0].performance.uploadDiskConcurrency = value === '' ? '' : Number(value)
                  })} />
            </Field>
          </div>
          <Toggle label="Use Volume Snapshots" checked={state.backups[0].useVolumeSnapshot}
            tooltip="sgcluster.spec.configurations.backups.useVolumeSnapshot"
            onChange={(value) => patch((draft) => { draft.backups[0].useVolumeSnapshot = value })} />
          {state.backups[0].useVolumeSnapshot && (
            <>
              <Field label="Volume Snapshot Class"
                tooltip="sgcluster.spec.configurations.backups.volumeSnapshotClass">
                <TextInput value={state.backups[0].volumeSnapshotClass ?? ''}
                  onChange={(value) =>
                    patch((draft) => { draft.backups[0].volumeSnapshotClass = value || null })} />
              </Field>
              <Toggle label="Fast Volume Snapshot" checked={state.backups[0].fastVolumeSnapshot}
                tooltip="sgcluster.spec.configurations.backups.fastVolumeSnapshot"
                onChange={(value) => patch((draft) => { draft.backups[0].fastVolumeSnapshot = value })} />
            </>
          )}
          <Field label="Backups Path" tooltip="sgcluster.spec.configurations.backups.path">
            <TextInput value={state.backups[0].path ?? ''}
              onChange={(value) => patch((draft) => { draft.backups[0].path = value || null })} />
          </Field>
        </>
      )}
    </div>
  )

  const initializationStep =
    isEdit && !state.restoreBackup.length ? (
      <p>Data initialization is only available on cluster creation.</p>
    ) : (
      <div className="flex flex-col gap-3">
        {!isEdit && pitrBackups.length > 0 && (
          <Chart
            type="line"
            height={200}
            series={[{ data: pitrBackups }]}
            options={{
              chart: {
                id: 'pitr',
                toolbar: { show: false },
                events: {
                  dataPointSelection: (event, context, config) => setPitrBackup(config.dataPointIndex),
                },
              },
              xaxis: { type: 'datetime', max: Date.now() },
              yaxis: { show: false },
              stroke: { width: 0 },
              markers: { size: 5, strokeWidth: 2, hover: { size: 7 } },
              tooltip: {
                custom: ({ dataPointIndex }) =>
                  `<div class="p-2">${pitrBackups[dataPointIndex].name}<br/>${pitrBackups[dataPointIndex].x}<br/>${pitrBackups[dataPointIndex].cluster}</div>`,
              },
              annotations: {
                xaxis: [
                  { x: Date.now(), borderColor: '#A68BF9', label: { text: 'Current Time' } },
                  ...(state.pitr.length
                    ? [
                        {
                          x: new Date(state.pitr).getTime(),
                          borderColor: '#5ad2ff',
                          label: { text: 'PITR' },
                        },
                      ]
                    : []),
                ],
              },
            }}
          />
        )}
        {!isEdit && !pitrBackups.length && (
          <p>No completed backups are available for this postgres version in this namespace.</p>
        )}
        {!isEdit && pitrBackups.length > 0 && (
          <Field label="Backup Selection" tooltip="sgcluster.spec.initialData.restore.fromBackup">
            <Select value={state.restoreBackup} aria-label="Backup Selection"
              options={[{ value: '', label: 'None' }, ...pitrBackups.map((backup) => backup.name)]}
              onChange={(value) =>
                patch((draft) => {
                  draft.restoreBackup = value
                  draft.pitr = ''
                })} />
          </Field>
        )}
        {Boolean(state.restoreBackup.length) && (
          <>
            <Field label="Restore To Timestamp"
              tooltip="sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp">
              <input type="datetime-local" step="1" className="border p-2 rounded bg-[hsl(var(--input-background))]" disabled={isEdit}
                aria-label="Restore To Timestamp" value={pitrDisplayValue}
                min={selectedPitrBackup ? toInputFrame(selectedPitrBackup.x) : undefined}
                max={toInputFrame(nextPitrBackup ? nextPitrBackup.x : new Date().toISOString())}
                onChange={(event) => changePitr(event.target.value)} />
            </Field>
            <Field label="Download Disk Concurrency"
              tooltip="sgcluster.spec.initialData.restore.downloadDiskConcurrency">
              <TextInput type="number" min="1" disabled={isEdit} value={state.downloadDiskConcurrency}
                onChange={(value) =>
                  patch((draft) => {
                    draft.downloadDiskConcurrency = value === '' ? '' : Number(value)
                  })} />
            </Field>
          </>
        )}
      </div>
    )

  const replicateFromStep = (
    <div className="flex flex-col gap-3">
      <Field label="Replicate From" tooltip="sgcluster.spec.replicateFrom">
        <Select value={state.replicateFromSource} aria-label="Replicate From"
          options={[
            { value: '', label: 'No Replication' },
            { value: 'cluster', label: 'Local Cluster' },
            { value: 'external', label: 'External Instance' },
            { value: 'storage', label: 'Object Storage' },
            { value: 'external-storage', label: 'External Instance + Object Storage' },
          ]}
          onChange={setReplicationSource} />
      </Field>
      {state.replicateFromSource === 'cluster' && (
        <Field label="Cluster" required tooltip="sgcluster.spec.replicateFrom.instance.sgCluster">
          <Select required value={state.replicateFrom.instance?.sgCluster || ''}
            options={[{ value: '', label: 'Select Cluster' }, ...clusterOptions]}
            onChange={(value) => patch((draft) => { draft.replicateFrom.instance.sgCluster = value })} />
        </Field>
      )}
      {state.replicateFromSource.includes('external') && (
        <div className="flex gap-2">
          <Field label="Host" required tooltip="sgcluster.spec.replicateFrom.instance.external.host">
            <TextInput required value={state.replicateFrom.instance?.external?.host || ''}
              onChange={(value) => patch((draft) => { draft.replicateFrom.instance.external.host = value })} />
          </Field>
          <Field label="Port" required tooltip="sgcluster.spec.replicateFrom.instance.external.port">
            <TextInput type="number" min="0" required
              value={state.replicateFrom.instance?.external?.port ?? ''}
              onChange={(value) =>
                patch((draft) => {
                  draft.replicateFrom.instance.external.port = value === '' ? '' : Number(value)
                })} />
          </Field>
        </div>
      )}
      {state.replicateFromSource.includes('storage') && (
        <>
          <Field label="Object Storage" required
            tooltip="sgcluster.spec.replicateFrom.storage.sgObjectStorage">
            <Select required value={state.replicateFrom.storage?.sgObjectStorage || ''}
              options={[{ value: '', label: 'Select Object Storage' }, ...objectStorageOptions]}
              onChange={(value) =>
                patch((draft) => { draft.replicateFrom.storage.sgObjectStorage = value })} />
          </Field>
          <Field label="Path" required tooltip="sgcluster.spec.replicateFrom.storage.path">
            <TextInput required value={state.replicateFrom.storage?.path || ''}
              onChange={(value) => patch((draft) => { draft.replicateFrom.storage.path = value })} />
          </Field>
          <div className="flex flex-wrap gap-2">
            {[
              ['Download Concurrency', 'downloadConcurrency'],
              ['Max Disk Bandwidth', 'maxDiskBandwidth'],
              ['Max Network Bandwidth', 'maxNetworkBandwidth'],
            ].map(([label, key]) => (
              <Field key={key} label={label}
                tooltip={`sgcluster.spec.replicateFrom.storage.performance.${key}`}>
                <TextInput type="number" value={state.replicateFrom.storage?.performance?.[key] ?? ''}
                  onChange={(value) =>
                    patch((draft) => {
                      draft.replicateFrom.storage.performance[key] = value === '' ? '' : Number(value)
                    })} />
              </Field>
            ))}
          </div>
        </>
      )}
      {['external', 'storage', 'external-storage'].includes(state.replicateFromSource) &&
        state.replicateFrom.users &&
        ['superuser', 'replication', 'authenticator'].map((role) => (
          <fieldset key={role} className="flex flex-wrap gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm font-medium capitalize">
              {role}{renderTooltip(`sgcluster.spec.replicateFrom.users.${role}`)}
            </legend>
            {['username', 'password'].map((credential) =>
              ['name', 'key'].map((part) => (
                <Field key={`${credential}-${part}`}
                  label={`${credential === 'username' ? 'Username' : 'Password'} Secret ${part === 'name' ? 'Name' : 'Key'}`}
                  tooltip={`sgcluster.spec.replicateFrom.users.${role}.${credential}.${part}`}
                  required>
                  <TextInput required value={state.replicateFrom.users[role][credential][part]}
                    onChange={(value) =>
                      patch((draft) => { draft.replicateFrom.users[role][credential][part] = value })} />
                </Field>
              ))
            )}
          </fieldset>
        ))}
    </div>
  )

  const scriptsStep = (
    <ManagedSqlEditor
      tooltipPrefix="sgcluster.spec.managedSql"
      value={state.managedSql}
      sources={state.scriptSource}
      scriptOptions={scriptOptions}
      isEdit={isEdit}
      blankEntry={blankScriptEntry}
      onChange={(managedSql, scriptSource) =>
        patch((draft) => {
          draft.managedSql = managedSql
          draft.scriptSource = scriptSource
        })}
    />
  )

  const sidecarsStep = (
    <div className="flex flex-col gap-3">
      <h3 className="text-sm font-semibold">Connection Pooling</h3>
      <p>
        To solve the Postgres connection fan-in problem (handling large number of incoming
        connections) StackGres includes by default a connection pooler fronting every Postgres
        instance. It is deployed as a sidecar. You may opt-out as well as tune the connection
        pooler configuration.
      </p>
      <Toggle label="Connection Pooling" checked={state.connPooling}
        tooltip="sgcluster.spec.configurations.sgPoolingConfig"
        onChange={(value) =>
          patch((draft) => {
            draft.connPooling = value
            draft.connectionPoolingConfig = ''
          })} />
      {state.connPooling && (
        <Field label="Connection Pooling Configuration"
          tooltip="sgcluster.spec.configurations.sgPoolingConfig">
          <Select value={state.connectionPoolingConfig ?? ''}
            options={[{ value: '', label: 'Default' }, ...poolConfigOptions]}
            onChange={(value) =>
              patch((draft) => { draft.connectionPoolingConfig = value === '' ? null : value })} />
        </Field>
      )}
      <h3 className="text-sm font-semibold">Postgres Utils</h3>
      <p>
        Sidecar container with Postgres administration tools. Optional (on by default; recommended
        for troubleshooting).
      </p>
      <Toggle label="Postgres Utils" checked={state.postgresUtil}
        tooltip={<HelpTooltip field="sgcluster.spec.pods.disablePostgresUtil"
          transform={(text) => text.replace('If set to `true`', 'If disabled')} />}
        onChange={(value) => patch((draft) => { draft.postgresUtil = value })} />
      <h3 className="text-sm font-semibold">Monitoring</h3>
      <p>
        Enable Prometheus metrics scraping via service monitors. Check the{' '}
        <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank"
          rel="noreferrer" className="text-[hsl(var(--primary))] hover:underline">
          Installation -&gt; Monitoring
        </a>{' '}
        section for information on how to enable in StackGres Grafana dashboard integration.
      </p>
      <Toggle label="Metrics Exporter" checked={state.metricsExporter} onChange={changeMetricsExporter}
        tooltip={<HelpTooltip field="sgcluster.spec.pods.disableMetricsExporter"
          transform={(text) =>
            text.replace('If set to `true`', 'If disabled')
              .replace('Recommended', 'Recommended to be disabled')} />} />
      <Toggle label="Prometheus Autobind" checked={state.prometheusAutobind}
        tooltip="sgcluster.spec.configurations.observability.prometheusAutobind"
        onChange={changePrometheusAutobind} />
      {!state.enableMonitoring && (
        <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
          Both Metrics Exporter and Prometheus Autobind should be enabled for monitoring to work.
        </p>
      )}
      <h2 className="text-sm font-semibold">User-Supplied Pods Sidecars</h2>
      <CustomVolumesEditor tooltipPrefix="sgcluster.spec.pods.customVolumes"
        volumes={state.pods.customVolumes} types={state.customVolumesType}
        onChange={(volumes, types) =>
          patch((draft) => {
            draft.pods.customVolumes = volumes
            draft.customVolumesType = types
          })} />
      <ContainersEditor legend="Custom Init Container" containers={state.pods.customInitContainers}
        tooltipPrefix="sgcluster.spec.pods.customInitContainers"
        onChange={(containers) => patch((draft) => { draft.pods.customInitContainers = containers })} />
      <ContainersEditor legend="Custom Container" containers={state.pods.customContainers}
        tooltipPrefix="sgcluster.spec.pods.customContainers"
        onChange={(containers) => patch((draft) => { draft.pods.customContainers = containers })} />
    </div>
  )

  const podsReplicationStep = (
    <div className="flex flex-col gap-3">
      <Field label="Role" required tooltip="sgcluster.spec.replication.role">
        <Select required value={state.replication.role} options={['ha-read', 'ha']}
          onChange={(value) => patch((draft) => { draft.replication.role = value })} />
      </Field>
      <Field label="Mode" required tooltip="sgcluster.spec.replication.mode">
        <Select required value={state.replication.mode} options={['async', 'sync', 'strict-sync']}
          onChange={(value) => patch((draft) => { draft.replication.mode = value })} />
      </Field>
      {['sync', 'strict-sync'].includes(state.replication.mode) && (
        <Field label="Sync Instances" tooltip="sgcluster.spec.replication.syncInstances">
          <TextInput type="number" min="1" max={Math.max(Number(state.instances) - 1, 1)}
            value={state.replication.syncInstances}
            onChange={(value) =>
              patch((draft) => {
                draft.replication.syncInstances = value === '' ? '' : Number(value)
              })} />
        </Field>
      )}
      <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
        <legend className="px-2 text-sm font-medium">
          Replication Groups{renderTooltip('sgcluster.spec.replication.groups')}
        </legend>
        {(state.replication.groups || []).map((group, index) => (
          <div key={index} className="flex items-end gap-2">
            <Field label="Group Name" tooltip="sgcluster.spec.replication.groups.name">
              <TextInput value={group.name}
                onChange={(value) =>
                  patch((draft) => { draft.replication.groups[index].name = value })} />
            </Field>
            <Field label="Group Role" required={group.name.length > 0}
              tooltip="sgcluster.spec.replication.groups.role">
              <Select required={group.name.length > 0} value={group.role}
                options={['ha-read', 'ha', 'readonly', 'none']}
                onChange={(value) =>
                  patch((draft) => { draft.replication.groups[index].role = value })} />
            </Field>
            <Field label="Group Instances"
              tooltip="sgcluster.spec.replication.groups.instances"
              required={group.name.length > 0 || group.role !== 'ha-read'}>
              <TextInput type="number" min="1" value={group.instances}
                required={group.name.length > 0 || group.role !== 'ha-read'}
                onChange={(value) =>
                  patch((draft) => {
                    draft.replication.groups[index].instances = value === '' ? '' : Number(value)
                  })} />
            </Field>
            <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete group ${index + 1}`}
              onClick={() =>
                patch((draft) => { draft.replication.groups.splice(index, 1) })}>
              <Trash2 size={16} />
            </button>
          </div>
        ))}
        <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
          onClick={() =>
            patch((draft) => {
              if (!draft.replication.groups) {
                draft.replication.groups = []
              }
              draft.replication.groups.push({ name: '', role: 'ha-read', instances: '' })
            })}>
          <Plus size={14} /> Add Group
        </button>
      </fieldset>
    </div>
  )

  const servicesStep = (
    <div className="flex flex-col gap-4">
      {['primary', 'replicas'].map((service) => (
        <fieldset key={service} className="flex flex-col gap-2 rounded-lg border p-4">
          <legend className="px-2 text-sm font-medium capitalize">
            {service} Service{renderTooltip(`sgcluster.spec.postgresServices.${service}`)}
          </legend>
          <Toggle label="Enabled" checked={state.postgresServices[service].enabled}
            tooltip={`sgcluster.spec.postgresServices.${service}.enabled`}
            onChange={(value) =>
              patch((draft) => { draft.postgresServices[service].enabled = value })} />
          <Field label="Type" required tooltip={`sgcluster.spec.postgresServices.${service}.type`}>
            <Select required value={state.postgresServices[service].type}
              aria-label={`${service} service type`}
              options={['ClusterIP', 'LoadBalancer', 'NodePort']}
              onChange={(value) =>
                patch((draft) => { draft.postgresServices[service].type = value })} />
          </Field>
          <Field label="Load Balancer IP"
            tooltip={`sgcluster.spec.postgresServices.${service}.loadBalancerIP`}>
            <TextInput value={state.postgresServices[service].loadBalancerIP || ''}
              aria-label={`${service} load balancer IP`}
              onChange={(value) =>
                patch((draft) => { draft.postgresServices[service].loadBalancerIP = value })} />
          </Field>
          <CustomPortsEditor legend={`${service === 'primary' ? 'Primary' : 'Replicas'} Custom Ports`}
            tooltipPrefix={`sgcluster.spec.postgresServices.${service}.customPorts`}
            ports={state.postgresServices[service].customPorts || []}
            onChange={(ports) =>
              patch((draft) => { draft.postgresServices[service].customPorts = ports })} />
        </fieldset>
      ))}
    </div>
  )

  const metadataStep = (
    <div className="flex flex-col gap-3">
      <KeyValueRows legend="Cluster Pods Labels" keyName="label" rows={state.podsMetadata}
        tooltip="sgcluster.spec.metadata.labels.clusterPods"
        onChange={(rows) => patch((draft) => { draft.podsMetadata = rows })} />
      <KeyValueRows legend="All Resources Annotations" keyName="annotation" rows={state.annotationsAll}
        tooltip="sgcluster.spec.metadata.annotations.allResources"
        onChange={(rows) => patch((draft) => { draft.annotationsAll = rows })} />
      <KeyValueRows legend="Cluster Pods Annotations" keyName="annotation" rows={state.annotationsPods}
        tooltip="sgcluster.spec.metadata.annotations.clusterPods"
        onChange={(rows) => patch((draft) => { draft.annotationsPods = rows })} />
      <KeyValueRows legend="Services Annotations" keyName="annotation" rows={state.annotationsServices}
        tooltip="sgcluster.spec.metadata.annotations.services"
        onChange={(rows) => patch((draft) => { draft.annotationsServices = rows })} />
      <KeyValueRows legend="Primary Service Annotations" keyName="annotation"
        tooltip="sgcluster.spec.metadata.annotations.primaryService"
        rows={state.postgresServicesPrimaryAnnotations}
        onChange={(rows) =>
          patch((draft) => { draft.postgresServicesPrimaryAnnotations = rows })} />
      <KeyValueRows legend="Replicas Service Annotations" keyName="annotation"
        tooltip="sgcluster.spec.metadata.annotations.replicasService"
        rows={state.postgresServicesReplicasAnnotations}
        onChange={(rows) =>
          patch((draft) => { draft.postgresServicesReplicasAnnotations = rows })} />
    </div>
  )

  const schedulingStep = (
    <div className="flex flex-col gap-3">
      {isEdit && (
        <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">Changing scheduling settings on a running cluster may trigger pod restarts.</p>
      )}
      <KeyValueRows legend="Node Selectors" keyName="label" rows={state.nodeSelector}
        tooltip="sgcluster.spec.pods.scheduling.nodeSelector"
        onChange={(rows) => patch((draft) => { draft.nodeSelector = rows })} />
      <TolerationsEditor tooltipPrefix="sgcluster.spec.pods.scheduling.tolerations"
        tolerations={state.tolerations}
        onChange={(tolerations) => patch((draft) => { draft.tolerations = tolerations })} />
      <NodeAffinityTerms legend="Node Affinity Required" terms={state.requiredAffinity}
        tooltipPrefix="sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution"
        onChange={(terms) => patch((draft) => { draft.requiredAffinity = terms })} />
      <NodeAffinityTerms legend="Node Affinity Preferred" preferred terms={state.preferredAffinity}
        tooltipPrefix="sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution"
        onChange={(terms) => patch((draft) => { draft.preferredAffinity = terms })} />
    </div>
  )

  const nonProductionValue = (value) => (value === null ? '' : String(value))
  const parseNonProduction = (value) => (value === '' ? null : value === 'true')
  const nonProductionStep = (
    <div className="flex flex-col gap-3">
      {[
        ['Cluster Pod Anti Affinity', 'clusterPodAntiAffinity', 'disableClusterPodAntiAffinity'],
        ['Patroni Resource Requirements', 'patroniResourceRequirements', 'disablePatroniResourceRequirements'],
        ['Cluster Resource Requirements', 'clusterResourceRequirements', 'disableClusterResourceRequirements'],
      ].map(([label, key, tooltipKey]) => (
        <Field key={key} label={label}
          tooltip={<HelpTooltip field={`sgcluster.spec.nonProductionOptions.${tooltipKey}`}
            transform={(text) => text.replace('Set this property to true', 'Disable this property')} />}>
          <Select value={nonProductionValue(state[key])}
            options={[
              { value: '', label: 'Default' },
              { value: 'false', label: 'Enable' },
              { value: 'true', label: 'Disable' },
            ]}
            onChange={(value) => patch((draft) => { draft[key] = parseNonProduction(value) })} />
        </Field>
      ))}
    </div>
  )

  const stepContent = {
    information: () => informationFields(formTemplate === 'full'),
    configurations: () => configurationsStep,
    cluster: () => clusterStep,
    extensions: () => extensionsStep,
    backups: () => backupsStep,
    initialization: () => initializationStep,
    'replicate-from': () => replicateFromStep,
    scripts: () => scriptsStep,
    sidecars: () => sidecarsStep,
    'pods-replication': () => podsReplicationStep,
    services: () => servicesStep,
    metadata: () => metadataStep,
    scheduling: () => schedulingStep,
    'non-production': () => nonProductionStep,
  }

  // --- render ----------------------------------------------------------------

  if (!formTemplate) {
    return (
      <section className="flex max-w-4xl flex-col gap-4 px-6 pb-6 text-sm">
        <h1 className="text-lg font-semibold">Create Cluster</h1>
        <p>
          To create your cluster easily, you can choose a template from our predefined express
          setups or select <strong>&quot;Custom&quot;</strong> for personalized and advanced
          settings.
          <HelpTooltip text={TEMPLATE_PICKER_TOOLTIP} />
        </p>
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {TEMPLATES.map((template) => (
            <button key={template.id} type="button"
              className="flex flex-col items-center gap-6 rounded-lg border bg-[hsl(var(--muted)/0.5)] px-4 py-10 transition-colors hover:border-[hsl(var(--primary))]"
              onClick={() => setupTemplate(template.id)}>
              <TemplateIcon bars={template.bars} />
              <span className="text-sm">{template.label}</span>
            </button>
          ))}
        </div>
        <Link to={`/${namespace}/sgclusters`} className="text-[hsl(var(--primary))] hover:underline">
          Cancel
        </Link>
      </section>
    )
  }

  const nextDisabled =
    currentStepIndex === steps.length - 1 ||
    (formTemplate === 'custom' && !advancedMode && currentStepIndex === 2)

  return (
    <section className="flex max-w-5xl flex-col gap-4 px-6 pb-6 text-sm">
      <header className="flex items-center justify-between">
        <h1 className="text-lg font-semibold">
          {isEdit ? `Edit Cluster: ${state.name}` : 'Create Cluster'}
        </h1>
        {formTemplate === 'custom' && (
          <Toggle label="Advanced options" checked={advancedMode}
            onChange={(value) => {
              setAdvancedMode(value)
              if (!value && currentStepIndex > 2) {
                setCurrentStep(FORM_STEPS.custom[0])
              }
            }} />
        )}
      </header>
      {formTemplate !== 'minimal' && (
        <nav aria-label="Form steps" className="flex flex-wrap gap-1 border-b pb-2 text-sm">
          {visibleSteps.map((step) => (
            <button key={step} type="button" onClick={() => setCurrentStep(step)}
              className={`px-2.5 py-1 rounded-md transition-colors ${currentStep === step ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))] font-medium' : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]'}`}>
              {STEP_LABELS[step]}
            </button>
          ))}
        </nav>
      )}
      <form
        onSubmit={(event) => {
          event.preventDefault()
          createCluster()
        }}
        className="flex flex-col gap-4">
        {STEP_HEADINGS[currentStep] && <StepHeader {...STEP_HEADINGS[currentStep]} />}
        {stepContent[currentStep]?.()}
        <div className="flex items-center justify-end gap-2 border-t pt-4 mt-4">
          {formTemplate !== 'minimal' && (
            <>
              <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" disabled={currentStepIndex === 0}
                onClick={() => stepBy(-1)}>
                Previous
              </button>
              <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" disabled={nextDisabled}
                onClick={() => stepBy(1)}>
                Next
              </button>
            </>
          )}
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
              isEdit ? navigate(`/${namespace}/sgcluster/${state.name}`) : setupTemplate(null, true)}>
            Cancel
          </button>
        </div>
      </form>
      {preview && (
        <SummaryDialog
          title={preview.dryRun ? 'Dry Run Summary' : 'Summary'}
          value={preview.cluster}
          onClose={() => setPreview(null)}
          defaults={
            preview.dryRun
              ? null
              : buildSGCluster(
                  initialSGClusterState(namespace, { timezone, postgresVersion: 'latest' })
                )
          }
        />
      )}
    </section>
  )
}

