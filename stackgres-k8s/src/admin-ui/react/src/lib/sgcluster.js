// Form state transforms for the SGCluster create/edit form, ported from the
// Vue CreateSGClusters.vue + sgclusterform.js mixin. The payload builder
// mirrors the Vue conditional spreads exactly: keys built with the
// `cond && {k:v} || {k:null}` idiom are emitted as explicit nulls when the
// condition is false, while bare `cond && {k:v}` keys are omitted.
import cronstrue from 'cronstrue'
import { objectToRows, rowsToObject } from './payloads'

const deep = (value) => JSON.parse(JSON.stringify(value))

export const isNull = (value) => value === null || value === undefined || value === ''

const hasProp = (object, path) => {
  let current = object
  for (const segment of path.split('.')) {
    if (current === null || current === undefined || !Object.hasOwn(current, segment)) {
      return false
    }
    current = current[segment]
  }
  return true
}

const isNullObject = (object) =>
  Object.values(object || {}).every((value) => value === null || value === undefined || value === '')

export const isNullObjectArray = (array) => (array || []).every((entry) => isNullObject(entry))

// ---------------------------------------------------------------------------
// Cron timezone shifting (Vue mixin.js tzCrontab). toLocal=true converts a
// stored UTC schedule for display; toLocal=false writes the local fields back
// to UTC. A no-op unless the timezone setting is 'local' and the browser has
// a non-zero UTC offset. The Vue version concatenated strings when fixing the
// minute field for 30/45-minute offsets; that is fixed here.
export function tzCrontab(baseCrontab, options = {}) {
  const {
    toLocal = true,
    timezone = 'local',
    utcOffsetMinutes = -new Date().getTimezoneOffset(),
  } = options

  if (!utcOffsetMinutes || timezone !== 'local') {
    return baseCrontab
  }

  const crontab = baseCrontab.split(' ')
  const isParsable = (value) => Number.isInteger(parseInt(value, 10))

  if (!isParsable(crontab[1])) {
    return baseCrontab
  }

  const tzOffset = utcOffsetMinutes / 60
  const dom = crontab[2]
  const dow = crontab[4]
  let modifier = 0

  crontab[1] = parseInt(crontab[1], 10) + tzOffset * (toLocal ? 1 : -1)

  if (!Number.isInteger(crontab[1])) {
    // Fix minutes offset if in timezone with 30/45min offsets
    if (isParsable(crontab[0])) {
      const minOffset = crontab[1] % 1

      if (crontab[0].includes('-')) {
        crontab[0] =
          parseInt(crontab[0].split('-')[0], 10) + minOffset * 60 +
          '-' +
          (parseInt(crontab[0].split('-')[1], 10) + minOffset * 60)
      } else if (crontab[0].includes('/')) {
        crontab[0] =
          parseInt(crontab[0].split('/')[0], 10) + minOffset * 60 +
          '/' +
          (parseInt(crontab[0].split('/')[1], 10) + minOffset * 60)
      } else {
        crontab[0] = parseInt(crontab[0], 10) + minOffset * 60
      }
    }

    crontab[1] = Math.trunc(crontab[1])
  }

  // Fix hour offset on 24h edges
  if (crontab[1] < 0) {
    modifier = -1
    crontab[1] = crontab[1] + 24
  } else if (crontab[1] >= 24) {
    modifier = 1
    crontab[1] = crontab[1] - 24
  }

  if (dom.includes('-')) {
    crontab[2] =
      parseInt(dom.split('-')[0], 10) + modifier + '-' + (parseInt(dom.split('-')[1], 10) + modifier)
  } else if (dom.includes('/')) {
    crontab[2] =
      parseInt(dom.split('/')[0], 10) + modifier + '/' + (parseInt(dom.split('/')[1], 10) + modifier)
  } else if (isParsable(dom)) {
    crontab[2] = parseInt(dom, 10) + modifier

    // Fix DOM offset on month edges
    if (crontab[2] < 1) {
      const month = crontab[3]
      if (isParsable(month) && parseInt(month, 10) > 1) {
        crontab[3] = parseInt(crontab[3], 10) - 1
      } else {
        crontab[3] = 12
      }

      if ([1, 3, 5, 7, 8, 10, 12].includes(crontab[3])) {
        crontab[2] = 31
      } else if ([4, 6, 9, 11].includes(crontab[3])) {
        crontab[2] = 30
      } else if (crontab[3] === 2) {
        crontab[2] = 28
      }
    } else if (
      crontab[2] > 31 ||
      (crontab[2] > 30 && ['4', '6', '9', '11'].includes(String(crontab[3]))) ||
      (crontab[2] > 28 && String(crontab[3]) === '2')
    ) {
      if (parseInt(crontab[3], 10) < 12) {
        crontab[3] = parseInt(crontab[3], 10) + 1
      } else {
        crontab[3] = 1
      }
      crontab[2] = 1
    }
  }

  if (!isParsable(dow) && dow.includes('-')) {
    crontab[4] =
      parseInt(dow.split('-')[0], 10) + modifier + '-' + (parseInt(dow.split('-')[1], 10) + modifier)
  } else if (dow.includes('/')) {
    crontab[4] =
      parseInt(dow.split('/')[0], 10) + modifier + '/' + (parseInt(dow.split('/')[1], 10) + modifier)
  } else if (dow !== '*') {
    crontab[4] = parseInt(dow, 10) + modifier
  }

  return crontab.join(' ')
}

export function cronFields(crontab) {
  const [min, hour, dom, month, dow] = (crontab || '').split(' ')
  return { min: min || '', hour: hour || '', dom: dom || '', month: month || '', dow: dow || '' }
}

export const isCronScheduleUnset = (fields) =>
  [fields.min, fields.hour, fields.dom, fields.month, fields.dow].some((field) => field === '')

export const joinCronFields = (fields) =>
  [fields.min, fields.hour, fields.dom, fields.month, fields.dow].join(' ')

// Human-readable schedule preview (the Vue prettyCRON filter, rendered with
// cronstrue instead of prettycron): the stored UTC crontab shifted to the
// display timezone.
export function cronPreview(cronSchedule, timezone) {
  if (!cronSchedule) {
    return 'never'
  }
  try {
    return cronstrue.toString(tzCrontab(cronSchedule, { toLocal: true, timezone }))
  } catch {
    return 'never'
  }
}

// ---------------------------------------------------------------------------
// Pristine shapes used by the repeaters: untouched rows are dropped before
// the payload is built.

export const blankExpression = () => ({ key: '', operator: '', values: [''] })
export const blankRequiredAffinityTerm = () => ({
  matchExpressions: [blankExpression()],
  matchFields: [blankExpression()],
})
export const blankPreferredAffinityTerm = () => ({
  preference: { matchExpressions: [blankExpression()], matchFields: [blankExpression()] },
  weight: 1,
})
export const blankToleration = () => ({
  key: '',
  operator: 'Equal',
  value: null,
  effect: null,
  tolerationSeconds: null,
})
export const blankScriptEntry = () => ({
  name: '',
  wrapInTransaction: null,
  storeStatusInDatabase: false,
  retryOnError: false,
  user: '',
  database: '',
  script: '',
})
export const blankCustomPort = () => ({
  appProtocol: null,
  name: null,
  nodePort: null,
  port: null,
  protocol: null,
  targetPort: null,
})
export const blankContainer = () => ({
  name: null,
  image: null,
  imagePullPolicy: null,
  args: [null],
  command: [null],
  workingDir: null,
  env: [{ name: null, value: null }],
  ports: [{ containerPort: null, hostIP: null, hostPort: null, name: null, protocol: null }],
  volumeMounts: [
    {
      mountPath: null,
      mountPropagation: null,
      name: null,
      readOnly: false,
      subPath: null,
      subPathExpr: null,
    },
  ],
})

const PRISTINE_EXPRESSIONS = JSON.stringify([blankExpression()])
const PRISTINE_SCRIPT_ENTRY = JSON.stringify(blankScriptEntry())
const PRISTINE_SCRIPT_FROM = JSON.stringify({ name: '', key: '' })
const PRISTINE_LABEL_ROW = JSON.stringify({ label: '', value: '' })
const PRISTINE_TOLERATION = JSON.stringify(blankToleration())

export const hasNodeSelectors = (rows) =>
  (rows || []).some((row) => JSON.stringify(row) !== PRISTINE_LABEL_ROW)

export const hasTolerations = (tolerations) =>
  (tolerations || []).some((toleration) => JSON.stringify(toleration) !== PRISTINE_TOLERATION)

// Drops untouched affinity terms and expressions without key/operator. Works
// for both required terms ({matchExpressions, matchFields}) and preferred
// terms ({preference, weight}). The Vue original spliced inside forEach
// (skipping siblings) — this filters instead.
export function cleanNodeAffinity(affinity) {
  if (!affinity || !affinity.length) {
    return []
  }
  return deep(affinity)
    .map((term) => {
      const body = term.preference || term
      ;['matchExpressions', 'matchFields'].forEach((kind) => {
        if (!body[kind]) {
          return
        }
        if (JSON.stringify(body[kind]) === PRISTINE_EXPRESSIONS) {
          delete body[kind]
          return
        }
        body[kind] = body[kind].filter(
          (expression) => expression.key.length && expression.operator.length
        )
        if (!body[kind].length) {
          delete body[kind]
        }
      })
      return term
    })
    .filter((term) => Object.keys(term.preference || term).length > 0)
}

// Deletes null/empty keys from every inline script entry (Vue cleanUpScripts).
export function cleanUpScripts(managedSql) {
  const result = deep(managedSql)
  ;(result.scripts || []).forEach((base) => {
    if (!base.scriptSpec) {
      return
    }
    ;(base.scriptSpec.scripts || []).forEach((entry) => {
      Object.keys(entry).forEach((key) => {
        if (entry[key] === null || entry[key] === '') {
          delete entry[key]
        }
      })
    })
  })
  return result
}

// True when any base script points at an SGScript or any inline entry has
// been edited away from its pristine shape. The Vue original compared raw
// entries against a JSON string missing its braces (never matched); fixed.
export function hasScripts(scripts, scriptSource) {
  return (scripts || []).some((base, baseIndex) => {
    if (base.sgScript && base.sgScript.length) {
      return true
    }
    return (base.scriptSpec?.scripts || []).some((entry, entryIndex) => {
      const type = scriptSource?.[baseIndex]?.entries?.[entryIndex] || 'raw'
      if (type === 'raw') {
        return JSON.stringify(entry) !== PRISTINE_SCRIPT_ENTRY
      }
      return JSON.stringify(entry.scriptFrom?.[type] || { name: '', key: '' }) !== PRISTINE_SCRIPT_FROM
    })
  })
}

// Drops empty rows from the user-supplied sidecars (Vue
// cleanUpUserSuppliedSidecars, with its bitwise-& guard fixed).
export function cleanUpUserSuppliedSidecars(pods) {
  const result = deep(pods)

  if (Object.hasOwn(result, 'customVolumes')) {
    const volumes = (result.customVolumes || []).filter((volume) => !isNull(volume.name))
    result.customVolumes = volumes.length ? volumes : null
  }

  ;['customInitContainers', 'customContainers'].forEach((kind) => {
    if (!Object.hasOwn(result, kind) || isNull(result[kind])) {
      return
    }
    result[kind] = result[kind]
      .map((container) => {
        const clean = { ...container }
        if (Object.hasOwn(clean, 'args')) {
          const args = (clean.args || []).filter((arg) => !isNull(arg))
          clean.args = args.length ? args : null
        }
        if (Object.hasOwn(clean, 'command')) {
          const command = (clean.command || []).filter((entry) => !isNull(entry))
          clean.command = command.length ? command : null
        }
        if (Object.hasOwn(clean, 'env')) {
          const env = (clean.env || []).filter((entry) => !isNull(entry.name))
          clean.env = env.length ? env : null
        }
        if (Object.hasOwn(clean, 'ports')) {
          const ports = (clean.ports || []).filter((port) => !isNullObject(port))
          clean.ports = ports.length ? ports : null
        }
        if (Object.hasOwn(clean, 'volumeMounts')) {
          const mounts = (clean.volumeMounts || []).filter(
            (mount) => !isNull(mount.name) && !isNull(mount.mountPath)
          )
          clean.volumeMounts = mounts.length ? mounts : null
        }
        return clean
      })
      .filter((container) => !isNull(container.name))
  })

  return result
}

// ---------------------------------------------------------------------------
// Initial form state (Vue data() defaults) and edit hydration.

export const DEFAULT_BACKUP = () => ({
  path: null,
  compression: 'lz4',
  cronSchedule: '0 5 * * *',
  retention: 5,
  performance: { maxNetworkBandwidth: '', maxDiskBandwidth: '', uploadDiskConcurrency: 1 },
  sgObjectStorage: '',
  useVolumeSnapshot: false,
  volumeSnapshotClass: null,
  fastVolumeSnapshot: false,
})

export function initialSGClusterState(namespace, options = {}) {
  const tzOptions = { timezone: options.timezone, utcOffsetMinutes: options.utcOffsetMinutes }
  const seededCron =
    options.timezone === 'local'
      ? tzCrontab('0 5 * * *', { ...tzOptions, toLocal: true })
      : '0 5 * * *'

  return {
    name: '',
    namespace,
    profile: 'production',
    instances: 1,
    resourceProfile: '',
    postgresVersion: options.postgresVersion || 'latest',
    flavor: 'vanilla',
    babelfishFeatureGates: false,
    ssl: {
      enabled: false,
      certificateSecretKeySelector: { name: '', key: '' },
      privateKeySecretKeySelector: { name: '', key: '' },
    },
    pgConfig: null,
    connectionPoolingConfig: null,
    connPooling: true,
    storageClass: '',
    volumeSize: 1,
    volumeUnit: 'Gi',
    distributedLogs: '',
    retention: '',
    prometheusAutobind: false,
    metricsExporter: true,
    postgresUtil: true,
    enableMonitoring: false,
    podsMetadata: [],
    nodeSelector: [],
    tolerations: [],
    requiredAffinity: [],
    preferredAffinity: [],
    managedSql: { continueOnSGScriptError: false, scripts: [] },
    scriptSource: [],
    annotationsAll: [],
    annotationsPods: [],
    annotationsServices: [],
    postgresServicesPrimaryAnnotations: [],
    postgresServicesReplicasAnnotations: [],
    selectedExtensions: [],
    managedBackups: false,
    backups: [DEFAULT_BACKUP()],
    cronSchedule: [cronFields(seededCron)],
    pods: { customVolumes: [], customInitContainers: [], customContainers: [] },
    customVolumesType: [],
    postgresServices: {
      primary: { enabled: true, type: 'ClusterIP', loadBalancerIP: '' },
      replicas: { enabled: true, type: 'ClusterIP', loadBalancerIP: '' },
    },
    replication: { role: 'ha-read', mode: 'async', syncInstances: 1, groups: [] },
    replicateFromSource: '',
    replicateFrom: {},
    restoreBackup: '',
    downloadDiskConcurrency: 1,
    pitr: '',
    clusterPodAntiAffinity: null,
    patroniResourceRequirements: null,
    clusterResourceRequirements: null,
    configurations: { sgPostgresConfig: null, sgPoolingConfig: null },
  }
}

export function getReplicationSource(cluster) {
  const replicateFrom = cluster?.spec?.replicateFrom
  if (!replicateFrom) {
    return ''
  }
  if (replicateFrom.instance?.sgCluster) {
    return 'cluster'
  }
  if (replicateFrom.instance?.external && replicateFrom.storage) {
    return 'external-storage'
  }
  if (replicateFrom.instance?.external) {
    return 'external'
  }
  if (replicateFrom.storage) {
    return 'storage'
  }
  return ''
}

// Edit-mode hydration (the Vue `cluster` computed), with its known bugs
// fixed: volumeSize becomes a number instead of a regex match array, and
// replication/postgresServices/managedSql get proper defaults.
export function hydrateSGCluster(cluster, options = {}) {
  const spec = deep(cluster.spec || {})
  const state = initialSGClusterState(cluster.metadata.namespace, options)

  state.name = cluster.metadata.name
  state.flavor = spec.postgres?.flavor || 'vanilla'
  state.babelfishFeatureGates = Boolean(
    spec.nonProductionOptions?.enabledFeatureGates?.includes('babelfish-flavor')
  )
  state.postgresVersion = spec.postgres?.version || 'latest'
  if (spec.postgres?.ssl?.enabled) {
    state.ssl = {
      enabled: true,
      certificateSecretKeySelector: spec.postgres.ssl.certificateSecretKeySelector || {
        name: '',
        key: '',
      },
      privateKeySecretKeySelector: spec.postgres.ssl.privateKeySecretKeySelector || {
        name: '',
        key: '',
      },
    }
  }
  state.profile = spec.profile || 'production'
  state.instances = spec.instances
  state.resourceProfile = spec.sgInstanceProfile || ''
  state.pgConfig = spec.configurations?.sgPostgresConfig ?? null
  state.storageClass = spec.pods?.persistentVolume?.storageClass || ''

  const size = spec.pods?.persistentVolume?.size || '1Gi'
  state.volumeSize = parseInt(size.match(/\d+/g)?.[0] ?? '1', 10)
  state.volumeUnit = (size.match(/[a-zA-Z]+/g) || ['Gi']).join('')

  state.connPooling = !spec.pods?.disableConnectionPooling
  state.connectionPoolingConfig = spec.configurations?.sgPoolingConfig ?? ''
  state.managedBackups = Boolean(spec.configurations?.backups && spec.configurations.backups.length)
  if (state.managedBackups) {
    state.backups = deep(spec.configurations.backups)
    if (!state.backups[0].performance) {
      state.backups[0].performance = {
        maxNetworkBandwidth: '',
        maxDiskBandwidth: '',
        uploadDiskConcurrency: 1,
      }
    }
    state.cronSchedule = [
      state.backups[0].cronSchedule
        ? cronFields(
            tzCrontab(state.backups[0].cronSchedule, {
              toLocal: true,
              timezone: options.timezone,
              utcOffsetMinutes: options.utcOffsetMinutes,
            })
          )
        : { min: '', hour: '', dom: '', month: '', dow: '' },
    ]
  }
  state.distributedLogs = spec.distributedLogs?.sgDistributedLogs || ''
  state.retention = spec.distributedLogs?.retention || ''
  state.replicateFrom = spec.replicateFrom ? deep(spec.replicateFrom) : {}
  state.replicateFromSource = getReplicationSource(cluster)
  if (spec.replication) {
    state.replication = {
      syncInstances: 1,
      groups: [],
      ...deep(spec.replication),
    }
  }
  state.prometheusAutobind = spec.configurations?.observability?.prometheusAutobind ?? false
  state.clusterPodAntiAffinity = spec.nonProductionOptions?.disableClusterPodAntiAffinity ?? null
  state.patroniResourceRequirements =
    spec.nonProductionOptions?.disablePatroniResourceRequirements ?? null
  state.clusterResourceRequirements =
    spec.nonProductionOptions?.disableClusterResourceRequirements ?? null
  state.metricsExporter = !spec.pods?.disableMetricsExporter
  state.enableMonitoring =
    !Object.hasOwn(spec.pods || {}, 'disableMetricsExporter') &&
    spec.configurations?.observability?.prometheusAutobind !== undefined
  state.postgresUtil = !spec.pods?.disablePostgresUtil
  state.podsMetadata = objectToRows(spec.metadata?.labels?.clusterPods, 'label')
  state.nodeSelector = objectToRows(spec.pods?.scheduling?.nodeSelector, 'label')
  state.tolerations = deep(spec.pods?.scheduling?.tolerations || [])
  state.preferredAffinity = deep(
    spec.pods?.scheduling?.nodeAffinity?.preferredDuringSchedulingIgnoredDuringExecution || []
  )
  state.requiredAffinity = deep(
    spec.pods?.scheduling?.nodeAffinity?.requiredDuringSchedulingIgnoredDuringExecution
      ?.nodeSelectorTerms || []
  )

  state.pods.customVolumes = deep(spec.pods?.customVolumes || [])
  state.customVolumesType = state.pods.customVolumes.map((volume) => {
    const type = ['emptyDir', 'configMap', 'secret'].find((candidate) =>
      Object.hasOwn(volume, candidate)
    )
    if ((type === 'configMap' || type === 'secret') && !volume[type].items) {
      volume[type].items = []
    }
    return type || null
  })
  state.pods.customInitContainers = deep(spec.pods?.customInitContainers || [])
  state.pods.customContainers = deep(spec.pods?.customContainers || [])

  if (spec.managedSql) {
    state.managedSql = deep(spec.managedSql)
    if (!Object.hasOwn(state.managedSql, 'continueOnSGScriptError')) {
      state.managedSql.continueOnSGScriptError = false
    }
    state.scriptSource = (state.managedSql.scripts || []).map((base) => ({
      base: base.sgScript || 'createNewScript',
      entries: (base.scriptSpec?.scripts || []).map((entry) => {
        if (Object.hasOwn(entry, 'script')) {
          return 'raw'
        }
        if (entry.scriptFrom?.secretKeyRef) {
          return 'secretKeyRef'
        }
        return 'configMapKeyRef'
      }),
    }))
  }

  state.annotationsAll = objectToRows(spec.metadata?.annotations?.allResources, 'annotation')
  state.annotationsPods = objectToRows(spec.metadata?.annotations?.clusterPods, 'annotation')
  state.annotationsServices = objectToRows(spec.metadata?.annotations?.services, 'annotation')
  state.postgresServicesPrimaryAnnotations = objectToRows(
    spec.metadata?.annotations?.primaryService,
    'annotation'
  )
  state.postgresServicesReplicasAnnotations = objectToRows(
    spec.metadata?.annotations?.replicasService,
    'annotation'
  )
  if (spec.postgresServices) {
    state.postgresServices = {
      primary: { enabled: true, type: 'ClusterIP', loadBalancerIP: '', ...spec.postgresServices.primary },
      replicas: { enabled: true, type: 'ClusterIP', loadBalancerIP: '', ...spec.postgresServices.replicas },
    }
  }
  state.selectedExtensions = deep(spec.postgres?.extensions || [])
  state.restoreBackup = spec.initialData?.restore?.fromBackup?.name || ''
  state.downloadDiskConcurrency = spec.initialData?.restore?.downloadDiskConcurrency ?? 1
  state.pitr = spec.initialData?.restore?.fromBackup?.pointInTimeRecovery?.restoreToTimestamp || ''

  return state
}

// ---------------------------------------------------------------------------
// Payload builder — a direct transliteration of the Vue createCluster()
// cluster literal, including its explicit-null-vs-omit semantics.
export function buildSGCluster(state, previous = null) {
  const requiredAffinity = cleanNodeAffinity(state.requiredAffinity)
  const preferredAffinity = cleanNodeAffinity(state.preferredAffinity)
  const managedSql = cleanUpScripts(state.managedSql)
  const pods = cleanUpUserSuppliedSidecars(state.pods)

  const podsMetadataMap = rowsToObject(state.podsMetadata, 'label')
  const nodeSelectorMap = rowsToObject(state.nodeSelector, 'label')
  const annotationsAll = rowsToObject(state.annotationsAll, 'annotation')
  const annotationsPods = rowsToObject(state.annotationsPods, 'annotation')
  const annotationsServices = rowsToObject(state.annotationsServices, 'annotation')
  const primaryServiceAnnotations = rowsToObject(state.postgresServicesPrimaryAnnotations, 'annotation')
  const replicasServiceAnnotations = rowsToObject(state.postgresServicesReplicasAnnotations, 'annotation')
  const notEmpty = (object) => Object.keys(object).length > 0

  return {
    metadata: {
      ...(hasProp(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(hasProp(previous, 'spec') && previous.spec),
      profile: state.profile,
      instances: state.instances,
      ...((String(state.resourceProfile).length && { sgInstanceProfile: state.resourceProfile }) || {
        sgInstanceProfile: null,
      }),
      pods: {
        ...(hasProp(previous, 'spec.pods') && previous.spec.pods),
        persistentVolume: {
          size: `${state.volumeSize}${state.volumeUnit}`,
          ...(state.storageClass !== undefined &&
            state.storageClass.length && { storageClass: state.storageClass }),
        },
        ...((!state.connPooling && { disableConnectionPooling: true }) || {
          disableConnectionPooling: null,
        }),
        ...((!state.metricsExporter && { disableMetricsExporter: true }) || {
          disableMetricsExporter: null,
        }),
        ...((!state.postgresUtil && { disablePostgresUtil: true }) || {
          disablePostgresUtil: null,
        }),
        ...((notEmpty(podsMetadataMap) && { metadata: { labels: podsMetadataMap } }) || {
          metadata: null,
        }),
        ...((hasProp(previous, 'spec.pods.scheduling') ||
          hasNodeSelectors(state.nodeSelector) ||
          hasTolerations(state.tolerations) ||
          requiredAffinity.length ||
          preferredAffinity.length) && {
          scheduling: {
            ...(hasProp(previous, 'spec.pods.scheduling') && previous.spec.pods.scheduling),
            ...((hasNodeSelectors(state.nodeSelector) && { nodeSelector: nodeSelectorMap }) || {
              nodeSelector: null,
            }),
            ...((hasTolerations(state.tolerations) && { tolerations: state.tolerations }) || {
              tolerations: null,
            }),
            ...(((requiredAffinity.length || preferredAffinity.length) && {
              nodeAffinity: {
                ...(requiredAffinity.length && {
                  requiredDuringSchedulingIgnoredDuringExecution: {
                    nodeSelectorTerms: requiredAffinity,
                  },
                }),
                ...(preferredAffinity.length && {
                  preferredDuringSchedulingIgnoredDuringExecution: preferredAffinity,
                }),
              },
            }) || { nodeAffinity: null }),
          },
        }),
        ...((Object.hasOwn(pods, 'customVolumes') &&
          !isNull(pods.customVolumes) && { customVolumes: pods.customVolumes }) || {
          customVolumes: null,
        }),
        ...((Object.hasOwn(pods, 'customInitContainers') &&
          pods.customInitContainers &&
          pods.customInitContainers.length && { customInitContainers: pods.customInitContainers }) || {
          customInitContainers: null,
        }),
        ...((Object.hasOwn(pods, 'customContainers') &&
          pods.customContainers &&
          pods.customContainers.length && { customContainers: pods.customContainers }) || {
          customContainers: null,
        }),
      },
      ...((hasProp(previous, 'spec.configurations') ||
        !isNull(state.pgConfig) ||
        state.managedBackups ||
        !isNull(state.connectionPoolingConfig) ||
        !isNull(state.prometheusAutobind)) && {
        configurations: {
          ...(hasProp(previous, 'spec.configurations') && previous.spec.configurations),
          observability: {
            ...(hasProp(previous, 'spec.configurations.observability') &&
              previous.spec.configurations.observability),
            ...(!isNull(state.prometheusAutobind) && {
              prometheusAutobind: state.prometheusAutobind,
            }),
          },
          ...((!isNull(state.pgConfig) && { sgPostgresConfig: state.pgConfig }) || {
            sgPostgresConfig: null,
          }),
          ...((state.managedBackups && { backups: state.backups }) || { backups: null }),
          ...((!isNull(state.connectionPoolingConfig) && {
            sgPoolingConfig: state.connectionPoolingConfig,
          }) || { sgPoolingConfig: null }),
        },
      }),
      ...(((hasProp(previous, 'spec.distributedLogs') || state.distributedLogs.length) && {
        distributedLogs: {
          ...(hasProp(previous, 'spec.distributedLogs') && previous.spec.distributedLogs),
          ...(state.distributedLogs.length && { sgDistributedLogs: state.distributedLogs }),
          ...(String(state.retention).length && { retention: state.retention }),
        },
      }) || { distributedLogs: null }),
      ...(((hasProp(previous, 'spec.initialData') || state.restoreBackup.length) && {
        initialData: {
          ...(hasProp(previous, 'spec.initialData') && previous.spec.initialData),
          ...(((hasProp(previous, 'spec.initialData.restore') || state.restoreBackup.length) && {
            restore: {
              ...(hasProp(previous, 'spec.initialData.restore') && previous.spec.initialData.restore),
              ...((state.restoreBackup.length && {
                fromBackup: {
                  ...(hasProp(previous, 'spec.initialData.restore.fromBackup') &&
                    previous.spec.initialData.restore.fromBackup),
                  name: state.restoreBackup,
                  ...(((hasProp(previous, 'spec.initialData.restore.fromBackup.pointInTimeRecovery') ||
                    state.pitr.length) && {
                    pointInTimeRecovery: {
                      ...(hasProp(
                        previous,
                        'spec.initialData.restore.fromBackup.pointInTimeRecovery'
                      ) && previous.spec.initialData.restore.fromBackup.pointInTimeRecovery),
                      ...((state.pitr.length && { restoreToTimestamp: state.pitr }) || {
                        restoreToTimestamp: null,
                      }),
                    },
                  }) || { pointInTimeRecovery: null }),
                },
              }) || { fromBackup: null }),
              ...(((hasProp(previous, 'spec.initialData.restore.downloadDiskConcurrency') ||
                state.downloadDiskConcurrency !== 1) && {
                downloadDiskConcurrency: state.downloadDiskConcurrency,
              }) || { downloadDiskConcurrency: null }),
            },
          }) || { restore: null }),
        },
      }) || { initialData: null }),
      ...((hasScripts(state.managedSql.scripts, state.scriptSource) && { managedSql }) || {
        managedSql: null,
      }),
      ...((state.replicateFromSource.length && { replicateFrom: state.replicateFrom }) || {
        replicateFrom: null,
      }),
      replication: {
        role: state.replication.role,
        mode: state.replication.mode,
        ...(['sync', 'strict-sync'].includes(state.replication.mode) && {
          syncInstances: state.replication.syncInstances,
        }),
        ...(Object.hasOwn(state.replication, 'groups') &&
          state.replication.groups.find((group) => group.instances > 0) !== undefined && {
            groups: state.replication.groups.filter((group) => group.instances > 0),
          }),
      },
      ...((hasProp(previous, 'spec.nonProductionOptions') ||
        state.clusterPodAntiAffinity != null ||
        state.patroniResourceRequirements != null ||
        state.clusterResourceRequirements != null ||
        (state.flavor === 'babelfish' && state.babelfishFeatureGates)) && {
        nonProductionOptions: {
          ...(hasProp(previous, 'spec.nonProductionOptions') && previous.spec.nonProductionOptions),
          ...((state.clusterPodAntiAffinity != null && {
            disableClusterPodAntiAffinity: state.clusterPodAntiAffinity,
          }) || { disableClusterPodAntiAffinity: null }),
          ...((state.patroniResourceRequirements != null && {
            disablePatroniResourceRequirements: state.patroniResourceRequirements,
          }) || { disablePatroniResourceRequirements: null }),
          ...((state.clusterResourceRequirements != null && {
            disableClusterResourceRequirements: state.clusterResourceRequirements,
          }) || { disableClusterResourceRequirements: null }),
          ...((state.flavor === 'babelfish' &&
            state.babelfishFeatureGates && { enabledFeatureGates: ['babelfish-flavor'] }) || {
            enabledFeatureGates: null,
          }),
        },
      }),
      ...((hasProp(previous, 'spec.metadata') ||
        notEmpty(annotationsAll) ||
        notEmpty(annotationsPods) ||
        notEmpty(annotationsServices) ||
        notEmpty(primaryServiceAnnotations) ||
        notEmpty(replicasServiceAnnotations) ||
        notEmpty(podsMetadataMap)) && {
        metadata: {
          ...(hasProp(previous, 'spec.metadata') && previous.spec.metadata),
          ...(((hasProp(previous, 'spec.metadata') ||
            notEmpty(annotationsAll) ||
            notEmpty(annotationsPods) ||
            notEmpty(annotationsServices) ||
            notEmpty(primaryServiceAnnotations) ||
            notEmpty(replicasServiceAnnotations)) && {
            annotations: {
              ...(hasProp(previous, 'spec.metadata.annotations') &&
                previous.spec.metadata.annotations),
              ...(notEmpty(annotationsAll) && { allResources: annotationsAll }),
              ...(notEmpty(annotationsPods) && { clusterPods: annotationsPods }),
              ...(notEmpty(annotationsServices) && { services: annotationsServices }),
              ...(notEmpty(primaryServiceAnnotations) && {
                primaryService: primaryServiceAnnotations,
              }),
              ...(notEmpty(replicasServiceAnnotations) && {
                replicasService: replicasServiceAnnotations,
              }),
            },
          }) || { annotations: null }),
          ...(((hasProp(previous, 'spec.metadata.labels') || notEmpty(podsMetadataMap)) && {
            labels: {
              ...(hasProp(previous, 'spec.metadata.labels') && previous.spec.metadata.labels),
              ...(notEmpty(podsMetadataMap) && { clusterPods: podsMetadataMap }),
            },
          }) || { labels: null }),
        },
      }),
      postgresServices: {
        ...(hasProp(previous, 'spec.postgresServices') && previous.spec.postgresServices),
        primary: {
          ...(hasProp(previous, 'spec.postgresServices.primary') &&
            previous.spec.postgresServices.primary),
          enabled: state.postgresServices.primary.enabled,
          type: state.postgresServices.primary.type,
          ...(hasProp(state.postgresServices, 'primary.loadBalancerIP') &&
            state.postgresServices.primary.loadBalancerIP.length && {
              loadBalancerIP: state.postgresServices.primary.loadBalancerIP,
            }),
          ...(Object.hasOwn(state.postgresServices.primary, 'customPorts') &&
            !isNullObjectArray(state.postgresServices.primary.customPorts) && {
              customPorts: state.postgresServices.primary.customPorts,
            }),
        },
        replicas: {
          ...(hasProp(previous, 'spec.postgresServices.replicas') &&
            previous.spec.postgresServices.replicas),
          enabled: state.postgresServices.replicas.enabled,
          type: state.postgresServices.replicas.type,
          ...(hasProp(state.postgresServices, 'replicas.loadBalancerIP') &&
            state.postgresServices.replicas.loadBalancerIP.length && {
              loadBalancerIP: state.postgresServices.replicas.loadBalancerIP,
            }),
          ...(Object.hasOwn(state.postgresServices.replicas, 'customPorts') &&
            !isNullObjectArray(state.postgresServices.replicas.customPorts) && {
              customPorts: state.postgresServices.replicas.customPorts,
            }),
        },
      },
      postgres: {
        ...(hasProp(previous, 'spec.postgres') && previous.spec.postgres),
        version: state.postgresVersion,
        ...((state.selectedExtensions.length && { extensions: state.selectedExtensions }) || {
          extensions: null,
        }),
        flavor: state.flavor,
        ...((state.ssl.enabled && {
          ssl: {
            enabled: true,
            ...((state.ssl.certificateSecretKeySelector.key ||
              state.ssl.certificateSecretKeySelector.name) && {
              certificateSecretKeySelector: state.ssl.certificateSecretKeySelector,
            }),
            ...((state.ssl.privateKeySecretKeySelector.key ||
              state.ssl.privateKeySecretKeySelector.name) && {
              privateKeySecretKeySelector: state.ssl.privateKeySecretKeySelector,
            }),
          },
        }) || { ssl: { enabled: false } }),
      },
    },
  }
}
