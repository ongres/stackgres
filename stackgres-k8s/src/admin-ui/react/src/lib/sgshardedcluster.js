// Form state transforms for the SGShardedCluster create/edit form, ported
// from the Vue CreateSGShardedClusters.vue (which reuses the sgclusterform
// mixin). The payload builder mirrors the Vue conditional spreads exactly:
// per-section scheduling/replication/metadata are explicit nulls when empty,
// per-section configurations/managedSql/sgInstanceProfile are omitted, and
// overrides go through cleanupOverrides (delete = omit, assignment = null).
import { objectToRows, rowsToObject } from './payloads'
import {
  cleanNodeAffinity,
  cleanUpScripts,
  cleanUpUserSuppliedSidecars,
  cronFields,
  DEFAULT_BACKUP,
  hasNodeSelectors,
  hasScripts,
  hasTolerations,
  isNull,
  isNullObjectArray,
  tzCrontab,
} from './sgcluster'

const deep = (value) => JSON.parse(JSON.stringify(value))

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

const blankServiceAnnotationGroups = () => ({
  allResources: [],
  clusterPods: [],
  services: [],
  primaryService: [],
  replicasService: [],
})

// Section defaults (Vue data() `coordinator`/`workers`); the editable shape
// keeps labels/annotations/nodeSelector as rows and the volume size split
// into {size, unit}.
export const blankShardedSection = (role) => ({
  ...(role === 'coordinator' ? { instances: 1 } : { clusters: 1, instancesPerCluster: 1 }),
  sgInstanceProfile: '',
  replication: { mode: role === 'coordinator' ? 'sync-all' : 'async' },
  configurations: { sgPostgresConfig: '', sgPoolingConfig: '' },
  managedSql: { continueOnSGScriptError: false, scripts: [] },
  metadata: { labels: { clusterPods: [] }, annotations: blankServiceAnnotationGroups() },
  pods: {
    disableConnectionPooling: false,
    disablePostgresUtil: false,
    disableMetricsExporter: false,
    persistentVolume: { size: { size: 1, unit: 'Gi' }, storageClass: '' },
    customVolumes: [],
    customInitContainers: [],
    customContainers: [],
    scheduling: {
      nodeSelector: [],
      tolerations: [],
      nodeAffinity: {
        requiredDuringSchedulingIgnoredDuringExecution: { nodeSelectorTerms: [] },
        preferredDuringSchedulingIgnoredDuringExecution: [],
      },
    },
  },
})

// New override entry (Vue pushOverride, with its script-shape mismatch fixed:
// managedSql starts with no scripts and the script source list agrees).
export const blankOverride = () => ({
  index: null,
  instancesPerCluster: null,
  sgInstanceProfile: '',
  replication: { mode: 'async' },
  configurations: { sgPostgresConfig: '', sgPoolingConfig: '' },
  managedSql: { continueOnSGScriptError: false, scripts: [] },
  metadata: { labels: { clusterPods: [] }, annotations: blankServiceAnnotationGroups() },
  pods: {
    disableConnectionPooling: false,
    disablePostgresUtil: false,
    disableMetricsExporter: false,
    persistentVolume: { size: { size: '', unit: '' }, storageClass: '' },
    customVolumes: [],
    customInitContainers: [],
    customContainers: [],
    scheduling: {
      nodeSelector: [],
      tolerations: [],
      nodeAffinity: {
        requiredDuringSchedulingIgnoredDuringExecution: { nodeSelectorTerms: [] },
        preferredDuringSchedulingIgnoredDuringExecution: [],
      },
    },
  },
})

export function initialSGShardedClusterState(namespace, options = {}) {
  const seededCron =
    options.timezone === 'local'
      ? tzCrontab('0 5 * * *', {
          toLocal: true,
          timezone: options.timezone,
          utcOffsetMinutes: options.utcOffsetMinutes,
        })
      : '0 5 * * *'
  const backup = DEFAULT_BACKUP()
  delete backup.path
  backup.paths = []

  return {
    name: '',
    namespace,
    database: '',
    shardingType: 'citus',
    profile: 'production',
    postgresVersion: options.postgresVersion || 'latest',
    flavor: 'vanilla',
    babelfishFeatureGates: false,
    ssl: {
      enabled: true,
      certificateSecretKeySelector: { name: '', key: '' },
      privateKeySecretKeySelector: { name: '', key: '' },
    },
    enableMonitoring: false,
    prometheusAutobind: false,
    distributedLogs: '',
    retention: '',
    managedBackups: false,
    backups: [backup],
    cronSchedule: [cronFields(seededCron)],
    selectedExtensions: [],
    replication: { mode: 'async', syncInstances: 1 },
    podsMetadata: [],
    annotationsAll: [],
    annotationsPods: [],
    annotationsServices: [],
    postgresServicesPrimaryAnnotations: [],
    postgresServicesReplicasAnnotations: [],
    clusterPodAntiAffinity: null,
    patroniResourceRequirements: null,
    clusterResourceRequirements: null,
    postgresServices: {
      coordinator: {
        primary: { enabled: true, type: 'ClusterIP', loadBalancerIP: '' },
        any: { enabled: true, type: 'ClusterIP', loadBalancerIP: '' },
        customPorts: [],
      },
      workers: {
        primaries: { enabled: true, type: 'ClusterIP', loadBalancerIP: '' },
        customPorts: [],
      },
    },
    coordinator: blankShardedSection('coordinator'),
    workers: { ...blankShardedSection('workers'), overrides: [] },
    scriptSource: { coordinator: [], workers: [], overrides: [] },
    customVolumesType: { coordinator: [], workers: [], overrides: [] },
  }
}

// Vue setScriptsSource: derive the script-source descriptors for a section's
// managedSql from the shapes of its entries.
export function scriptSourcesFor(managedSql) {
  return (managedSql?.scripts || []).map((base) => ({
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

const parseVolumeSize = (size) => ({
  size: parseInt(String(size).match(/\d+/g)?.[0] ?? '1', 10),
  unit: (String(size).match(/[a-zA-Z]+/g) || ['Gi']).join(''),
})

// Normalizes one coordinator/workers/override section from CRD shape to the
// editable shape the form reads unguarded.
function normalizeSection(section, role) {
  const base = role === 'override' ? blankOverride() : blankShardedSection(role)
  const result = { ...base, ...deep(section) }

  result.sgInstanceProfile = result.sgInstanceProfile || ''
  result.replication = { mode: 'async', ...(section.replication || {}) }
  result.configurations = {
    sgPostgresConfig: '',
    sgPoolingConfig: '',
    ...(section.configurations || {}),
  }
  result.configurations.sgPostgresConfig = result.configurations.sgPostgresConfig || ''
  result.configurations.sgPoolingConfig = result.configurations.sgPoolingConfig || ''
  if (!result.managedSql || !result.managedSql.scripts) {
    result.managedSql = { continueOnSGScriptError: false, scripts: [] }
  } else if (!Object.hasOwn(result.managedSql, 'continueOnSGScriptError')) {
    result.managedSql.continueOnSGScriptError = false
  }

  const pods = { ...base.pods, ...(deep(section.pods || {})) }
  pods.disableConnectionPooling = Boolean(pods.disableConnectionPooling)
  pods.disablePostgresUtil = Boolean(pods.disablePostgresUtil)
  pods.disableMetricsExporter = Boolean(pods.disableMetricsExporter)
  if (pods.persistentVolume && typeof pods.persistentVolume.size === 'string') {
    pods.persistentVolume = {
      size: parseVolumeSize(pods.persistentVolume.size),
      storageClass: pods.persistentVolume.storageClass || '',
    }
  } else if (!pods.persistentVolume) {
    pods.persistentVolume = deep(base.pods.persistentVolume)
  }
  pods.customVolumes = pods.customVolumes || []
  pods.customVolumes.forEach((volume) => {
    ;['configMap', 'secret'].forEach((kind) => {
      if (volume[kind] && !volume[kind].items) {
        volume[kind].items = []
      }
    })
  })
  pods.customInitContainers = pods.customInitContainers || []
  pods.customContainers = pods.customContainers || []
  const scheduling = pods.scheduling || {}
  pods.scheduling = {
    nodeSelector: objectToRows(scheduling.nodeSelector, 'label'),
    tolerations: scheduling.tolerations || [],
    nodeAffinity: {
      requiredDuringSchedulingIgnoredDuringExecution: {
        nodeSelectorTerms:
          scheduling.nodeAffinity?.requiredDuringSchedulingIgnoredDuringExecution
            ?.nodeSelectorTerms || [],
      },
      preferredDuringSchedulingIgnoredDuringExecution:
        scheduling.nodeAffinity?.preferredDuringSchedulingIgnoredDuringExecution || [],
    },
  }
  result.pods = pods

  const metadata = section.metadata || {}
  result.metadata = {
    labels: { clusterPods: objectToRows(metadata.labels?.clusterPods, 'label') },
    annotations: {
      allResources: objectToRows(metadata.annotations?.allResources, 'annotation'),
      clusterPods: objectToRows(metadata.annotations?.clusterPods, 'annotation'),
      services: objectToRows(metadata.annotations?.services, 'annotation'),
      primaryService: objectToRows(metadata.annotations?.primaryService, 'annotation'),
      replicasService: objectToRows(metadata.annotations?.replicasService, 'annotation'),
    },
  }

  const volumeTypes = pods.customVolumes.map(
    (volume) =>
      ['emptyDir', 'configMap', 'secret'].find((kind) => Object.hasOwn(volume, kind)) || null
  )

  return { section: result, volumeTypes }
}

// Edit-mode hydration (the Vue `cluster` computed), with its known bugs
// fixed: replication/postgresServices get proper defaults instead of `false`
// and the observability read is guarded.
export function hydrateSGShardedCluster(cluster, options = {}) {
  const spec = deep(cluster.spec || {})
  const state = initialSGShardedClusterState(cluster.metadata.namespace, options)

  state.name = cluster.metadata.name
  state.database = spec.database || ''
  state.shardingType = spec.type || 'citus'
  state.profile = spec.profile || 'production'
  state.postgresVersion = spec.postgres?.version || 'latest'
  state.flavor = spec.postgres?.flavor || 'vanilla'
  state.babelfishFeatureGates = Boolean(
    spec.nonProductionOptions?.enabledFeatureGates?.includes('babelfish-flavor')
  )
  state.ssl = {
    enabled: Boolean(spec.postgres?.ssl?.enabled),
    certificateSecretKeySelector: spec.postgres?.ssl?.certificateSecretKeySelector || {
      name: '',
      key: '',
    },
    privateKeySecretKeySelector: spec.postgres?.ssl?.privateKeySecretKeySelector || {
      name: '',
      key: '',
    },
  }
  state.selectedExtensions = deep(spec.postgres?.extensions || [])
  state.distributedLogs = spec.distributedLogs?.sgDistributedLogs || ''
  state.retention = spec.distributedLogs?.retention || ''
  state.prometheusAutobind = spec.configurations?.observability?.prometheusAutobind ?? false
  state.clusterPodAntiAffinity = spec.nonProductionOptions?.disableClusterPodAntiAffinity ?? null
  state.patroniResourceRequirements =
    spec.nonProductionOptions?.disablePatroniResourceRequirements ?? null
  state.clusterResourceRequirements =
    spec.nonProductionOptions?.disableClusterResourceRequirements ?? null

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
    if (!state.backups[0].paths) {
      state.backups[0].paths = []
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

  if (spec.replication) {
    state.replication = { syncInstances: 1, ...deep(spec.replication) }
  }
  state.podsMetadata = objectToRows(spec.metadata?.labels?.clusterPods, 'label')
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
    const service = (candidate) => ({
      enabled: true,
      type: 'ClusterIP',
      loadBalancerIP: '',
      ...(candidate || {}),
    })
    state.postgresServices = {
      coordinator: {
        primary: service(spec.postgresServices.coordinator?.primary),
        any: service(spec.postgresServices.coordinator?.any),
        customPorts: spec.postgresServices.coordinator?.customPorts || [],
      },
      workers: {
        primaries: service(spec.postgresServices.workers?.primaries),
        customPorts: spec.postgresServices.workers?.customPorts || [],
      },
    }
  }

  ;['coordinator', 'workers'].forEach((role) => {
    const { section, volumeTypes } = normalizeSection(spec[role] || {}, role)
    state[role] = section
    state.scriptSource[role] = scriptSourcesFor(section.managedSql)
    state.customVolumesType[role] = volumeTypes
  })
  state.workers.overrides = (spec.workers?.overrides || []).map((override, index) => {
    const { section, volumeTypes } = normalizeSection(override, 'override')
    section.index = override.index ?? null
    section.instancesPerCluster = override.instancesPerCluster ?? null
    state.scriptSource.overrides[index] = scriptSourcesFor(section.managedSql)
    state.customVolumesType.overrides[index] = volumeTypes
    return section
  })

  state.enableMonitoring =
    state.prometheusAutobind &&
    !state.coordinator.pods.disableMetricsExporter &&
    !state.workers.pods.disableMetricsExporter

  return state
}

// ---------------------------------------------------------------------------
// Payload builder helpers

const sectionAffinity = (section) => ({
  required: cleanNodeAffinity(
    section.pods?.scheduling?.nodeAffinity?.requiredDuringSchedulingIgnoredDuringExecution
      ?.nodeSelectorTerms || []
  ),
  preferred: cleanNodeAffinity(
    section.pods?.scheduling?.nodeAffinity?.preferredDuringSchedulingIgnoredDuringExecution || []
  ),
})

const sectionScheduling = (section, previousScheduling, withPreviousSpread) => {
  const { required, preferred } = sectionAffinity(section)
  const scheduling = section.pods?.scheduling || {}
  const active =
    (withPreviousSpread && previousScheduling !== undefined) ||
    hasNodeSelectors(scheduling.nodeSelector) ||
    hasTolerations(scheduling.tolerations) ||
    required.length ||
    preferred.length
  if (!active) {
    return null
  }
  return {
    ...(withPreviousSpread && previousScheduling !== undefined && previousScheduling),
    ...((hasNodeSelectors(scheduling.nodeSelector) && {
      nodeSelector: rowsToObject(scheduling.nodeSelector, 'label'),
    }) || { nodeSelector: null }),
    ...((hasTolerations(scheduling.tolerations) && { tolerations: scheduling.tolerations }) || {
      tolerations: null,
    }),
    ...(((required.length || preferred.length) && {
      nodeAffinity: {
        ...(required.length && {
          requiredDuringSchedulingIgnoredDuringExecution: { nodeSelectorTerms: required },
        }),
        ...(preferred.length && {
          preferredDuringSchedulingIgnoredDuringExecution: preferred,
        }),
      },
    }) || { nodeAffinity: null }),
  }
}

const sectionReplication = (section) =>
  section.replication?.mode && section.replication.mode !== 'async'
    ? {
        mode: section.replication.mode,
        ...(['sync', 'strict-sync'].includes(section.replication.mode) && {
          syncInstances: section.replication.syncInstances,
        }),
      }
    : null

const sectionMetadata = (section) => {
  const labels = rowsToObject(section.metadata?.labels?.clusterPods, 'label')
  const annotationGroups = Object.fromEntries(
    ['allResources', 'clusterPods', 'services', 'primaryService', 'replicasService'].map((key) => [
      key,
      rowsToObject(section.metadata?.annotations?.[key], 'annotation'),
    ])
  )
  const hasAnnotations = Object.values(annotationGroups).some((group) => Object.keys(group).length)
  const hasLabels = Object.keys(labels).length > 0
  if (!hasAnnotations && !hasLabels) {
    return null
  }
  return {
    ...(hasAnnotations
      ? {
          annotations: Object.fromEntries(
            Object.entries(annotationGroups).filter(([, group]) => Object.keys(group).length)
          ),
        }
      : { annotations: null }),
    ...(hasLabels ? { labels: { clusterPods: labels } } : { labels: null }),
  }
}

// Vue cleanupOverrides: editable overrides → CRD shape, mutating a clone.
// Deleted keys are omitted from the payload; assigned nulls are explicit.
export function cleanupOverrides(overrides, scriptSourceOverrides) {
  const result = deep(overrides)
  result.forEach((override, overrideIndex) => {
    if (override.instancesPerCluster === null || override.instancesPerCluster === '') {
      delete override.instancesPerCluster
    }

    override.sgInstanceProfile =
      override.sgInstanceProfile && String(override.sgInstanceProfile).length
        ? override.sgInstanceProfile
        : null

    const configurations = override.configurations || {}
    if (
      (configurations.sgPoolingConfig && configurations.sgPoolingConfig.length) ||
      (configurations.sgPostgresConfig && configurations.sgPostgresConfig.length)
    ) {
      override.configurations = {
        sgPoolingConfig:
          configurations.sgPoolingConfig && configurations.sgPoolingConfig.length
            ? configurations.sgPoolingConfig
            : null,
        sgPostgresConfig:
          configurations.sgPostgresConfig && configurations.sgPostgresConfig.length
            ? configurations.sgPostgresConfig
            : null,
      }
    } else {
      override.configurations = null
    }

    if (override.managedSql?.scripts) {
      override.managedSql = hasScripts(
        override.managedSql.scripts,
        scriptSourceOverrides?.[overrideIndex]
      )
        ? cleanUpScripts(override.managedSql)
        : null
    }

    const volume = override.pods.persistentVolume || {}
    const sizeSet =
      volume.size && ![null, ''].includes(volume.size.size) && ![null, ''].includes(volume.size.unit)
    const storageClassSet = volume.storageClass && volume.storageClass.length
    if (sizeSet || storageClassSet) {
      if (sizeSet) {
        override.pods.persistentVolume.size = `${volume.size.size}${volume.size.unit}`
      } else {
        delete override.pods.persistentVolume.size
      }
      if (!storageClassSet) {
        delete override.pods.persistentVolume.storageClass
      }
    } else {
      delete override.pods.persistentVolume
    }

    override.pods.disableConnectionPooling = override.pods.disableConnectionPooling ? true : null
    override.pods.disablePostgresUtil = override.pods.disablePostgresUtil ? true : null
    override.pods.disableMetricsExporter = override.pods.disableMetricsExporter ? true : null

    const sidecars = cleanUpUserSuppliedSidecars(override.pods)
    override.pods.customVolumes = Object.hasOwn(sidecars, 'customVolumes')
      ? sidecars.customVolumes
      : null
    override.pods.customInitContainers =
      Object.hasOwn(sidecars, 'customInitContainers') &&
      sidecars.customInitContainers &&
      sidecars.customInitContainers.length
        ? sidecars.customInitContainers
        : null
    override.pods.customContainers =
      Object.hasOwn(sidecars, 'customContainers') &&
      sidecars.customContainers &&
      sidecars.customContainers.length
        ? sidecars.customContainers
        : null

    override.pods.scheduling = sectionScheduling(override, undefined, false)
    override.replication = sectionReplication(override)
    override.metadata = sectionMetadata(override)
  })
  return result
}

const buildSection = (state, role, previous) => {
  const section = state[role]
  const previousSection = previous?.spec?.[role]
  const configurations = section.configurations || {}
  const volume = section.pods.persistentVolume
  const sidecars = cleanUpUserSuppliedSidecars(section.pods)
  const scheduling = sectionScheduling(
    section,
    hasProp(previous, `spec.${role}.pods.scheduling`) ? previousSection.pods.scheduling : undefined,
    true
  )

  return {
    ...(previousSection !== undefined && previousSection),
    ...(role === 'coordinator'
      ? { instances: section.instances }
      : { clusters: section.clusters, instancesPerCluster: section.instancesPerCluster }),
    ...(section.sgInstanceProfile &&
      section.sgInstanceProfile.length && { sgInstanceProfile: section.sgInstanceProfile }),
    ...((hasProp(previous, `spec.${role}.configurations`) ||
      configurations.sgPoolingConfig.length ||
      configurations.sgPostgresConfig.length) && {
      configurations: {
        ...(hasProp(previous, `spec.${role}.configurations`) && previousSection.configurations),
        ...(configurations.sgPoolingConfig &&
          configurations.sgPoolingConfig.length && {
            sgPoolingConfig: configurations.sgPoolingConfig,
          }),
        ...(configurations.sgPostgresConfig &&
          configurations.sgPostgresConfig.length && {
            sgPostgresConfig: configurations.sgPostgresConfig,
          }),
      },
    }),
    ...(hasScripts(section.managedSql.scripts, state.scriptSource[role]) && {
      managedSql: cleanUpScripts(section.managedSql),
    }),
    pods: {
      ...(hasProp(previous, `spec.${role}.pods`) && previousSection.pods),
      persistentVolume: {
        size: `${volume.size.size}${volume.size.unit}`,
        ...(volume.storageClass &&
          volume.storageClass.length && { storageClass: volume.storageClass }),
      },
      ...(section.pods.disableConnectionPooling && {
        disableConnectionPooling: section.pods.disableConnectionPooling,
      }),
      ...(section.pods.disablePostgresUtil && {
        disablePostgresUtil: section.pods.disablePostgresUtil,
      }),
      ...(Object.hasOwn(section.pods, 'disableMetricsExporter') && {
        disableMetricsExporter: section.pods.disableMetricsExporter,
      }),
      ...((Object.hasOwn(section.pods, 'customVolumes') &&
        !isNullObjectArray(section.pods.customVolumes) && {
          customVolumes: section.pods.customVolumes,
        }) || { customVolumes: null }),
      ...((Object.hasOwn(sidecars, 'customInitContainers') &&
        sidecars.customInitContainers &&
        sidecars.customInitContainers.length && {
          customInitContainers: sidecars.customInitContainers,
        }) || { customInitContainers: null }),
      ...((Object.hasOwn(sidecars, 'customContainers') &&
        sidecars.customContainers &&
        sidecars.customContainers.length && {
          customContainers: sidecars.customContainers,
        }) || { customContainers: null }),
      scheduling,
    },
    replication:
      hasProp(previous, `spec.${role}.replication`) || sectionReplication(section)
        ? sectionReplication(section)
        : null,
    metadata:
      hasProp(previous, `spec.${role}.metadata`) || sectionMetadata(section)
        ? {
            ...(hasProp(previous, `spec.${role}.metadata`) && previousSection.metadata),
            ...(sectionMetadata(section) || { annotations: null, labels: null }),
          }
        : null,
  }
}

// ---------------------------------------------------------------------------
// Payload builder — a direct transliteration of the Vue createCluster()
// cluster literal in CreateSGShardedClusters.vue.
export function buildSGShardedCluster(state, previous = null) {
  const annotationsAll = rowsToObject(state.annotationsAll, 'annotation')
  const annotationsPods = rowsToObject(state.annotationsPods, 'annotation')
  const annotationsServices = rowsToObject(state.annotationsServices, 'annotation')
  const primaryServiceAnnotations = rowsToObject(state.postgresServicesPrimaryAnnotations, 'annotation')
  const replicasServiceAnnotations = rowsToObject(state.postgresServicesReplicasAnnotations, 'annotation')
  const podsMetadataMap = rowsToObject(state.podsMetadata, 'label')
  const notEmpty = (object) => Object.keys(object).length > 0

  const backups = state.managedBackups
    ? deep(state.backups).map((backup) => {
        if (backup.paths && backup.paths.some((path) => path)) {
          backup.paths = backup.paths.map((path) => path || null)
        } else {
          backup.paths = null
        }
        return backup
      })
    : null

  const services = state.postgresServices

  return {
    metadata: {
      ...(hasProp(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(hasProp(previous, 'spec') && previous.spec),
      profile: state.profile,
      database: state.database,
      type: state.shardingType,
      ...((hasProp(previous, 'spec.configurations') ||
        state.managedBackups ||
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
          ...((state.managedBackups && { backups }) || { backups: null }),
        },
      }),
      ...(((hasProp(previous, 'spec.distributedLogs') || state.distributedLogs.length) && {
        distributedLogs: {
          ...(hasProp(previous, 'spec.distributedLogs') && previous.spec.distributedLogs),
          ...(state.distributedLogs.length && { sgDistributedLogs: state.distributedLogs }),
          ...(state.retention && String(state.retention).length && { retention: state.retention }),
        },
      }) || { distributedLogs: null }),
      ...(((hasProp(previous, 'spec.replication') || state.replication.mode !== 'async') && {
        replication: {
          mode: state.replication.mode,
          ...(['sync', 'strict-sync'].includes(state.replication.mode) && {
            syncInstances: state.replication.syncInstances,
          }),
        },
      }) || { replication: null }),
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
        coordinator: {
          ...(hasProp(previous, 'spec.postgresServices.coordinator') &&
            previous.spec.postgresServices.coordinator),
          primary: {
            ...(hasProp(previous, 'spec.postgresServices.coordinator.primary') &&
              previous.spec.postgresServices.coordinator.primary),
            enabled: services.coordinator.primary.enabled,
            type: services.coordinator.primary.type,
            ...(services.coordinator.primary.loadBalancerIP &&
              services.coordinator.primary.loadBalancerIP.length && {
                loadBalancerIP: services.coordinator.primary.loadBalancerIP,
              }),
          },
          any: {
            ...(hasProp(previous, 'spec.postgresServices.coordinator.any') &&
              previous.spec.postgresServices.coordinator.any),
            enabled: services.coordinator.any.enabled,
            type: services.coordinator.any.type,
            ...(services.coordinator.any.loadBalancerIP &&
              services.coordinator.any.loadBalancerIP.length && {
                loadBalancerIP: services.coordinator.any.loadBalancerIP,
              }),
          },
          ...(Object.hasOwn(services.coordinator, 'customPorts') &&
            !isNullObjectArray(services.coordinator.customPorts) && {
              customPorts: services.coordinator.customPorts,
            }),
        },
        workers: {
          ...(hasProp(previous, 'spec.postgresServices.workers') &&
            previous.spec.postgresServices.workers),
          primaries: {
            ...(hasProp(previous, 'spec.postgresServices.workers.primaries') &&
              previous.spec.postgresServices.workers.primaries),
            enabled: services.workers.primaries.enabled,
            type: services.workers.primaries.type,
            ...(services.workers.primaries.loadBalancerIP &&
              services.workers.primaries.loadBalancerIP.length && {
                loadBalancerIP: services.workers.primaries.loadBalancerIP,
              }),
          },
          ...(Object.hasOwn(services.workers, 'customPorts') &&
            !isNullObjectArray(services.workers.customPorts) && {
              customPorts: services.workers.customPorts,
            }),
        },
      },
      postgres: {
        ...(hasProp(previous, 'spec.postgres') && previous.spec.postgres),
        version: state.postgresVersion,
        ...((state.selectedExtensions && state.selectedExtensions.length && {
          extensions: state.selectedExtensions,
        }) || { extensions: null }),
        flavor: state.flavor,
        ssl: {
          enabled: state.ssl.enabled,
          ...(state.ssl.certificateSecretKeySelector.name.length &&
            state.ssl.certificateSecretKeySelector.key.length && {
              certificateSecretKeySelector: state.ssl.certificateSecretKeySelector,
            }),
          ...(state.ssl.privateKeySecretKeySelector.name.length &&
            state.ssl.privateKeySecretKeySelector.key.length && {
              privateKeySecretKeySelector: state.ssl.privateKeySecretKeySelector,
            }),
        },
      },
      coordinator: buildSection(state, 'coordinator', previous),
      workers: {
        ...buildSection(state, 'workers', previous),
        ...((state.workers.overrides && state.workers.overrides.length && {
          overrides: cleanupOverrides(state.workers.overrides, state.scriptSource.overrides),
        }) || { overrides: null }),
      },
    },
  }
}
