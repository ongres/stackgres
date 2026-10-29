import { describe, expect, it } from 'vitest'
import {
  blankOverride,
  buildSGShardedCluster,
  cleanupOverrides,
  hydrateSGShardedCluster,
  initialSGShardedClusterState,
  scriptSourcesFor,
} from './sgshardedcluster'

const base = () => {
  const state = initialSGShardedClusterState('default', { timezone: 'utc' })
  state.name = 'shard-demo'
  state.database = 'demo'
  return state
}

describe('initialSGShardedClusterState', () => {
  it('differs from the regular cluster defaults where the Vue form does', () => {
    const state = base()
    expect(state.shardingType).toBe('citus')
    expect(state.ssl.enabled).toBe(true)
    expect(state.backups[0].paths).toEqual([])
    expect(state.backups[0]).not.toHaveProperty('path')
    expect(state.coordinator.replication.mode).toBe('sync-all')
    expect(state.workers.replication.mode).toBe('async')
    expect(state.workers.overrides).toEqual([])
  })
})

describe('buildSGShardedCluster', () => {
  it('builds the default create payload with the sharded null-vs-omit semantics', () => {
    const payload = buildSGShardedCluster(base())
    expect(payload.metadata).toEqual({ name: 'shard-demo', namespace: 'default' })
    expect(payload.spec.profile).toBe('production')
    expect(payload.spec.database).toBe('demo')
    expect(payload.spec.type).toBe('citus')
    expect(payload.spec.configurations).toEqual({
      observability: { prometheusAutobind: false },
      backups: null,
    })
    expect(payload.spec.distributedLogs).toBeNull()
    expect(payload.spec.replication).toBeNull()
    expect(payload.spec.nonProductionOptions).toBeUndefined()
    expect(payload.spec.metadata).toBeUndefined()
    expect(payload.spec.postgresServices).toEqual({
      coordinator: {
        primary: { enabled: true, type: 'ClusterIP' },
        any: { enabled: true, type: 'ClusterIP' },
      },
      workers: { primaries: { enabled: true, type: 'ClusterIP' } },
    })
    expect(payload.spec.postgres).toEqual({
      version: 'latest',
      extensions: null,
      flavor: 'vanilla',
      ssl: { enabled: true },
    })
    expect(payload.spec.coordinator).toEqual({
      instances: 1,
      pods: {
        persistentVolume: { size: '1Gi' },
        disableMetricsExporter: false,
        customVolumes: null,
        customInitContainers: null,
        customContainers: null,
        scheduling: null,
      },
      // the Vue coordinator default mode is sync-all, which is non-async and
      // therefore always emitted
      replication: { mode: 'sync-all' },
      metadata: null,
    })
    expect(payload.spec.workers).toEqual({
      clusters: 1,
      instancesPerCluster: 1,
      pods: {
        persistentVolume: { size: '1Gi' },
        disableMetricsExporter: false,
        customVolumes: null,
        customInitContainers: null,
        customContainers: null,
        scheduling: null,
      },
      replication: null,
      metadata: null,
      overrides: null,
    })
  })

  it('normalizes backup paths and emits section settings when set', () => {
    const state = base()
    state.managedBackups = true
    state.backups[0].sgObjectStorage = 'store'
    state.backups[0].paths = ['/coordinator', '', null]
    state.replication = { mode: 'sync', syncInstances: 2 }
    state.coordinator.sgInstanceProfile = 'size-m'
    state.coordinator.configurations.sgPostgresConfig = 'pg16'
    state.coordinator.pods.disableConnectionPooling = true
    state.coordinator.pods.scheduling.nodeSelector = [{ label: 'disk', value: 'ssd' }]
    state.workers.clusters = 4
    state.workers.instancesPerCluster = 2

    const payload = buildSGShardedCluster(state)
    expect(payload.spec.configurations.backups[0].paths).toEqual(['/coordinator', null, null])
    expect(payload.spec.replication).toEqual({ mode: 'sync', syncInstances: 2 })
    expect(payload.spec.coordinator.sgInstanceProfile).toBe('size-m')
    expect(payload.spec.coordinator.configurations).toEqual({ sgPostgresConfig: 'pg16' })
    expect(payload.spec.coordinator.pods.disableConnectionPooling).toBe(true)
    expect(payload.spec.coordinator.pods.scheduling).toEqual({
      nodeSelector: { disk: 'ssd' },
      tolerations: null,
      nodeAffinity: null,
    })
    expect(payload.spec.workers.clusters).toBe(4)
    expect(payload.spec.workers.instancesPerCluster).toBe(2)
  })

  it('nulls every path when none is set and keeps ssl selectors only when complete', () => {
    const state = base()
    state.managedBackups = true
    state.backups[0].paths = ['', null]
    state.ssl.certificateSecretKeySelector = { name: 'certs', key: '' }
    const payload = buildSGShardedCluster(state)
    expect(payload.spec.configurations.backups[0].paths).toBeNull()
    expect(payload.spec.postgres.ssl).toEqual({ enabled: true })
  })

  it('merges previous metadata and spec sections on edit', () => {
    const state = base()
    const previous = {
      metadata: { name: 'shard-demo', namespace: 'default', uid: 'u9' },
      spec: {
        coordinator: { managedSql: { scripts: [{ sgScript: 'init' }] } },
        postgresServices: { coordinator: { primary: { nodePorts: { pgport: 30001 } } } },
      },
    }
    const payload = buildSGShardedCluster(state, previous)
    expect(payload.metadata.uid).toBe('u9')
    expect(payload.spec.coordinator.managedSql).toEqual({ scripts: [{ sgScript: 'init' }] })
    expect(payload.spec.postgresServices.coordinator.primary.nodePorts).toEqual({ pgport: 30001 })
    expect(payload.spec.postgresServices.coordinator.primary.enabled).toBe(true)
  })
})

describe('cleanupOverrides', () => {
  it('reduces an untouched override to nulls and omissions', () => {
    const override = blankOverride()
    override.index = 2
    const [cleaned] = cleanupOverrides([override], [[]])
    expect(cleaned.index).toBe(2)
    expect(cleaned).not.toHaveProperty('instancesPerCluster')
    expect(cleaned.sgInstanceProfile).toBeNull()
    expect(cleaned.configurations).toBeNull()
    expect(cleaned.managedSql).toBeNull()
    expect(cleaned.pods).not.toHaveProperty('persistentVolume')
    expect(cleaned.pods.disableConnectionPooling).toBeNull()
    expect(cleaned.pods.customVolumes).toBeNull()
    expect(cleaned.pods.scheduling).toBeNull()
    expect(cleaned.replication).toBeNull()
    expect(cleaned.metadata).toBeNull()
  })

  it('serializes configured overrides', () => {
    const override = blankOverride()
    override.index = 0
    override.instancesPerCluster = 3
    override.sgInstanceProfile = 'size-l'
    override.configurations.sgPoolingConfig = 'pool-fast'
    override.pods.persistentVolume = { size: { size: 5, unit: 'Gi' }, storageClass: 'ssd' }
    override.pods.disableMetricsExporter = true
    override.replication = { mode: 'strict-sync', syncInstances: 1 }
    override.metadata.labels.clusterPods = [{ label: 'tier', value: 'shard' }]
    const [cleaned] = cleanupOverrides([override], [[]])
    expect(cleaned.instancesPerCluster).toBe(3)
    expect(cleaned.sgInstanceProfile).toBe('size-l')
    expect(cleaned.configurations).toEqual({ sgPoolingConfig: 'pool-fast', sgPostgresConfig: null })
    expect(cleaned.pods.persistentVolume).toEqual({ size: '5Gi', storageClass: 'ssd' })
    expect(cleaned.pods.disableMetricsExporter).toBe(true)
    expect(cleaned.replication).toEqual({ mode: 'strict-sync', syncInstances: 1 })
    expect(cleaned.metadata).toEqual({ annotations: null, labels: { clusterPods: { tier: 'shard' } } })
  })
})

describe('hydrateSGShardedCluster', () => {
  const cluster = {
    metadata: { name: 'shard-live', namespace: 'default' },
    spec: {
      profile: 'testing',
      database: 'appdb',
      type: 'citus',
      postgres: { version: '16.3', flavor: 'vanilla', ssl: { enabled: true } },
      configurations: { observability: { prometheusAutobind: true } },
      coordinator: {
        instances: 2,
        pods: { persistentVolume: { size: '10Gi' } },
        managedSql: { scripts: [{ sgScript: 'init' }, { scriptSpec: { scripts: [{ script: 'SELECT 1' }] } }] },
      },
      workers: {
        clusters: 3,
        instancesPerCluster: 2,
        pods: {
          persistentVolume: { size: '20Gi', storageClass: 'ssd' },
          scheduling: { nodeSelector: { disk: 'ssd' } },
        },
        overrides: [
          { index: 1, pods: { persistentVolume: { size: '40Gi' } } },
        ],
      },
      postgresServices: { coordinator: { primary: { enabled: true, type: 'LoadBalancer' } } },
    },
  }

  it('maps sections into the editable shape with proper defaults', () => {
    const state = hydrateSGShardedCluster(cluster, { timezone: 'utc' })
    expect(state.database).toBe('appdb')
    expect(state.coordinator.instances).toBe(2)
    expect(state.coordinator.pods.persistentVolume).toEqual({
      size: { size: 10, unit: 'Gi' },
      storageClass: '',
    })
    expect(state.coordinator.replication).toEqual({ mode: 'async' })
    expect(state.scriptSource.coordinator).toEqual([
      { base: 'init', entries: [] },
      { base: 'createNewScript', entries: ['raw'] },
    ])
    expect(state.workers.pods.scheduling.nodeSelector).toEqual([{ label: 'disk', value: 'ssd' }])
    expect(state.workers.overrides[0].index).toBe(1)
    expect(state.workers.overrides[0].pods.persistentVolume.size).toEqual({ size: 40, unit: 'Gi' })
    expect(state.postgresServices.coordinator.primary.type).toBe('LoadBalancer')
    expect(state.postgresServices.coordinator.any.type).toBe('ClusterIP')
    expect(state.postgresServices.workers.primaries.type).toBe('ClusterIP')
    expect(state.enableMonitoring).toBe(true)
    expect(state.replication).toEqual({ mode: 'async', syncInstances: 1 })
  })

  it('scriptSourcesFor distinguishes entry shapes', () => {
    expect(
      scriptSourcesFor({
        scripts: [
          { scriptSpec: { scripts: [{ scriptFrom: { secretKeyRef: {} } }, { scriptFrom: { configMapKeyRef: {} } }] } },
        ],
      })
    ).toEqual([{ base: 'createNewScript', entries: ['secretKeyRef', 'configMapKeyRef'] }])
  })
})
