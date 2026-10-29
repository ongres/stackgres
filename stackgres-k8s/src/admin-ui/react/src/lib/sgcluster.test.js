import { describe, expect, it } from 'vitest'
import {
  blankExpression,
  blankPreferredAffinityTerm,
  blankRequiredAffinityTerm,
  blankScriptEntry,
  buildSGCluster,
  cleanNodeAffinity,
  cleanUpScripts,
  cleanUpUserSuppliedSidecars,
  getReplicationSource,
  hasScripts,
  hydrateSGCluster,
  initialSGClusterState,
  tzCrontab,
} from './sgcluster'

describe('tzCrontab', () => {
  it('is a no-op when the timezone setting is utc or the offset is zero', () => {
    expect(tzCrontab('0 5 * * *', { timezone: 'utc', utcOffsetMinutes: 120 })).toBe('0 5 * * *')
    expect(tzCrontab('0 5 * * *', { timezone: 'local', utcOffsetMinutes: 0 })).toBe('0 5 * * *')
  })

  it('shifts hours to local time and back to UTC', () => {
    expect(tzCrontab('0 5 * * *', { timezone: 'local', utcOffsetMinutes: 120 })).toBe('0 7 * * *')
    expect(
      tzCrontab('0 7 * * *', { timezone: 'local', utcOffsetMinutes: 120, toLocal: false })
    ).toBe('0 5 * * *')
  })

  it('wraps around midnight and offsets day fields', () => {
    expect(tzCrontab('0 23 10 * 3', { timezone: 'local', utcOffsetMinutes: 120 })).toBe(
      '0 1 11 * 4'
    )
    expect(
      tzCrontab('0 1 1 3 *', { timezone: 'local', utcOffsetMinutes: 120, toLocal: false })
    ).toBe('0 23 28 2 *')
  })

  it('handles half-hour offsets by shifting minutes', () => {
    expect(tzCrontab('0 5 * * *', { timezone: 'local', utcOffsetMinutes: 330 })).toBe('30 10 * * *')
  })
})

describe('cleanNodeAffinity', () => {
  it('drops pristine terms entirely', () => {
    expect(cleanNodeAffinity([blankRequiredAffinityTerm()])).toEqual([])
    expect(cleanNodeAffinity([blankPreferredAffinityTerm()])).toEqual([])
  })

  it('keeps filled expressions and removes incomplete siblings', () => {
    const cleaned = cleanNodeAffinity([
      {
        matchExpressions: [
          { key: 'zone', operator: 'In', values: ['a'] },
          { key: '', operator: '', values: [''] },
        ],
        matchFields: [blankExpression()],
      },
    ])
    expect(cleaned).toEqual([
      { matchExpressions: [{ key: 'zone', operator: 'In', values: ['a'] }] },
    ])
  })

  it('preserves the preference wrapper and weight on preferred terms', () => {
    const cleaned = cleanNodeAffinity([
      {
        preference: {
          matchExpressions: [{ key: 'disk', operator: 'Exists' }],
          matchFields: [blankExpression()],
        },
        weight: 20,
      },
    ])
    expect(cleaned).toEqual([
      { preference: { matchExpressions: [{ key: 'disk', operator: 'Exists' }] }, weight: 20 },
    ])
  })
})

describe('scripts helpers', () => {
  it('hasScripts ignores pristine raw entries (fixing the Vue brace bug)', () => {
    const scripts = [{ scriptSpec: { continueOnError: false, managedVersions: true, scripts: [blankScriptEntry()] } }]
    const source = [{ base: 'createNewScript', entries: ['raw'] }]
    expect(hasScripts(scripts, source)).toBe(false)
  })

  it('hasScripts detects sgScript references and edited entries', () => {
    expect(hasScripts([{ sgScript: 'init-db' }], [{ base: 'init-db', entries: [] }])).toBe(true)
    const edited = [
      { scriptSpec: { scripts: [{ ...blankScriptEntry(), script: 'SELECT 1' }] } },
    ]
    expect(hasScripts(edited, [{ base: 'createNewScript', entries: ['raw'] }])).toBe(true)
    const fromRef = [
      { scriptSpec: { scripts: [{ scriptFrom: { secretKeyRef: { name: 's', key: 'k' } } }] } },
    ]
    expect(hasScripts(fromRef, [{ base: 'createNewScript', entries: ['secretKeyRef'] }])).toBe(true)
  })

  it('cleanUpScripts removes null and empty keys from inline entries', () => {
    const cleaned = cleanUpScripts({
      continueOnSGScriptError: false,
      scripts: [
        {
          scriptSpec: {
            continueOnError: false,
            managedVersions: true,
            scripts: [{ ...blankScriptEntry(), name: 'seed', script: 'SELECT 1' }],
          },
        },
        { sgScript: 'existing' },
      ],
    })
    expect(cleaned.scripts[0].scriptSpec.scripts[0]).toEqual({
      name: 'seed',
      storeStatusInDatabase: false,
      retryOnError: false,
      script: 'SELECT 1',
    })
    expect(cleaned.scripts[1]).toEqual({ sgScript: 'existing' })
  })
})

describe('cleanUpUserSuppliedSidecars', () => {
  it('drops unnamed volumes and empty container rows', () => {
    const cleaned = cleanUpUserSuppliedSidecars({
      customVolumes: [{ name: null }, { name: 'vol1', emptyDir: { medium: null, sizeLimit: null } }],
      customInitContainers: [
        {
          name: 'init',
          image: 'img',
          args: [null, 'serve'],
          command: [null],
          env: [{ name: null, value: null }, { name: 'MODE', value: 'fast' }],
          ports: [{ containerPort: null, hostIP: null, hostPort: null, name: null, protocol: null }],
          volumeMounts: [
            { mountPath: null, name: null, readOnly: false },
            { mountPath: '/data', name: 'vol1', readOnly: false },
          ],
        },
        { name: null, image: null },
      ],
      customContainers: [],
    })
    expect(cleaned.customVolumes).toEqual([
      { name: 'vol1', emptyDir: { medium: null, sizeLimit: null } },
    ])
    expect(cleaned.customInitContainers).toEqual([
      {
        name: 'init',
        image: 'img',
        args: ['serve'],
        command: null,
        env: [{ name: 'MODE', value: 'fast' }],
        ports: null,
        volumeMounts: [{ mountPath: '/data', name: 'vol1', readOnly: false }],
      },
    ])
    expect(cleaned.customContainers).toEqual([])
  })

  it('nulls customVolumes when every entry is unnamed', () => {
    expect(cleanUpUserSuppliedSidecars({ customVolumes: [{ name: '' }] }).customVolumes).toBeNull()
  })
})

describe('buildSGCluster', () => {
  const base = () => initialSGClusterState('default', { timezone: 'utc' })

  it('builds the default create payload with explicit nulls', () => {
    const state = base()
    state.name = 'demo'
    const payload = buildSGCluster(state)
    expect(payload.metadata).toEqual({ name: 'demo', namespace: 'default' })
    expect(payload.spec.profile).toBe('production')
    expect(payload.spec.instances).toBe(1)
    expect(payload.spec.sgInstanceProfile).toBeNull()
    expect(payload.spec.pods).toEqual({
      persistentVolume: { size: '1Gi' },
      disableConnectionPooling: null,
      disableMetricsExporter: null,
      disablePostgresUtil: null,
      metadata: null,
      customVolumes: null,
      customInitContainers: null,
      customContainers: null,
    })
    expect(payload.spec.configurations).toEqual({
      observability: { prometheusAutobind: false },
      sgPostgresConfig: null,
      backups: null,
      sgPoolingConfig: null,
    })
    expect(payload.spec.distributedLogs).toBeNull()
    expect(payload.spec.initialData).toBeNull()
    expect(payload.spec.managedSql).toBeNull()
    expect(payload.spec.replicateFrom).toBeNull()
    expect(payload.spec.replication).toEqual({ role: 'ha-read', mode: 'async' })
    expect(payload.spec.nonProductionOptions).toBeUndefined()
    expect(payload.spec.metadata).toBeUndefined()
    expect(payload.spec.postgresServices).toEqual({
      primary: { enabled: true, type: 'ClusterIP' },
      replicas: { enabled: true, type: 'ClusterIP' },
    })
    expect(payload.spec.postgres).toEqual({
      version: 'latest',
      extensions: null,
      flavor: 'vanilla',
      ssl: { enabled: false },
    })
  })

  it('emits scheduling, managed backups, scripts and replication groups when set', () => {
    const state = base()
    state.name = 'full'
    state.resourceProfile = 'size-s'
    state.storageClass = 'ssd'
    state.nodeSelector = [{ label: 'disk', value: 'ssd' }]
    state.tolerations = [
      { key: 'db', operator: 'Equal', value: 'true', effect: null, tolerationSeconds: null },
    ]
    state.managedBackups = true
    state.backups[0].sgObjectStorage = 'store'
    state.managedSql = {
      continueOnSGScriptError: false,
      scripts: [{ sgScript: 'init' }],
    }
    state.scriptSource = [{ base: 'init', entries: [] }]
    state.replication = {
      role: 'ha',
      mode: 'sync',
      syncInstances: 2,
      groups: [
        { name: 'g1', role: 'readonly', instances: 2 },
        { name: '', role: 'ha-read', instances: '' },
      ],
    }
    state.selectedExtensions = [
      { name: 'pg_stat_statements', version: '1.10', publisher: 'c', repository: 'r' },
    ]
    state.ssl = {
      enabled: true,
      certificateSecretKeySelector: { name: 'certs', key: 'tls.crt' },
      privateKeySecretKeySelector: { name: '', key: '' },
    }

    const payload = buildSGCluster(state)
    expect(payload.spec.sgInstanceProfile).toBe('size-s')
    expect(payload.spec.pods.persistentVolume).toEqual({ size: '1Gi', storageClass: 'ssd' })
    expect(payload.spec.pods.scheduling).toEqual({
      nodeSelector: { disk: 'ssd' },
      tolerations: state.tolerations,
      nodeAffinity: null,
    })
    expect(payload.spec.configurations.backups).toBe(state.backups)
    expect(payload.spec.managedSql).toEqual({
      continueOnSGScriptError: false,
      scripts: [{ sgScript: 'init' }],
    })
    expect(payload.spec.replication).toEqual({
      role: 'ha',
      mode: 'sync',
      syncInstances: 2,
      groups: [{ name: 'g1', role: 'readonly', instances: 2 }],
    })
    expect(payload.spec.postgres.extensions).toEqual(state.selectedExtensions)
    expect(payload.spec.postgres.ssl).toEqual({
      enabled: true,
      certificateSecretKeySelector: { name: 'certs', key: 'tls.crt' },
    })
  })

  it('merges previous metadata and spec sections on edit', () => {
    const state = base()
    state.name = 'edited'
    state.pgConfig = 'pg16-custom'
    const previous = {
      metadata: { name: 'edited', namespace: 'default', uid: 'u1', resourceVersion: '7' },
      spec: {
        configurations: { sgPostgresConfig: 'old', patroni: { initialConfig: { x: 1 } } },
        pods: { persistentVolume: { size: '5Gi' }, managementPolicy: 'OrderedReady' },
        postgres: { version: '16.2', flavor: 'vanilla', ssl: { enabled: true } },
      },
    }
    const payload = buildSGCluster(state, previous)
    expect(payload.metadata.uid).toBe('u1')
    expect(payload.metadata.resourceVersion).toBe('7')
    expect(payload.spec.configurations.patroni).toEqual({ initialConfig: { x: 1 } })
    expect(payload.spec.configurations.sgPostgresConfig).toBe('pg16-custom')
    expect(payload.spec.pods.managementPolicy).toBe('OrderedReady')
    expect(payload.spec.pods.persistentVolume.size).toBe('1Gi')
    expect(payload.spec.postgres.version).toBe('latest')
    expect(payload.spec.postgres.ssl).toEqual({ enabled: false })
  })
})

describe('hydrateSGCluster', () => {
  const cluster = {
    metadata: { name: 'live', namespace: 'default' },
    spec: {
      profile: 'testing',
      instances: 3,
      sgInstanceProfile: 'size-m',
      postgres: {
        version: '16.2',
        flavor: 'vanilla',
        extensions: [{ name: 'postgis', version: '3.4' }],
      },
      pods: {
        persistentVolume: { size: '20Gi', storageClass: 'ssd' },
        disableMetricsExporter: true,
        scheduling: { nodeSelector: { disk: 'ssd' } },
      },
      configurations: {
        sgPostgresConfig: 'pg16',
        backups: [{ sgObjectStorage: 'store', cronSchedule: '30 2 * * *', retention: 7 }],
      },
      managedSql: {
        scripts: [
          { sgScript: 'live-default' },
          {
            scriptSpec: {
              scripts: [
                { name: 'seed', script: 'SELECT 1', version: 2 },
                { scriptFrom: { secretKeyRef: { name: 's', key: 'k' } } },
              ],
            },
          },
        ],
      },
      postgresServices: { primary: { enabled: true, type: 'LoadBalancer' } },
      replicateFrom: { instance: { sgCluster: 'other' } },
      initialData: { restore: { fromBackup: { name: 'bk1' }, downloadDiskConcurrency: 2 } },
    },
  }

  it('maps the spec into form state with the Vue hydration bugs fixed', () => {
    const state = hydrateSGCluster(cluster, { timezone: 'utc' })
    expect(state.name).toBe('live')
    expect(state.profile).toBe('testing')
    expect(state.volumeSize).toBe(20)
    expect(state.volumeUnit).toBe('Gi')
    expect(state.storageClass).toBe('ssd')
    expect(state.metricsExporter).toBe(false)
    expect(state.enableMonitoring).toBe(false)
    expect(state.pgConfig).toBe('pg16')
    expect(state.managedBackups).toBe(true)
    expect(state.cronSchedule[0]).toEqual({ min: '30', hour: '2', dom: '*', month: '*', dow: '*' })
    expect(state.nodeSelector).toEqual([{ label: 'disk', value: 'ssd' }])
    expect(state.scriptSource).toEqual([
      { base: 'live-default', entries: [] },
      { base: 'createNewScript', entries: ['raw', 'secretKeyRef'] },
    ])
    expect(state.managedSql.continueOnSGScriptError).toBe(false)
    expect(state.replicateFromSource).toBe('cluster')
    expect(state.replication.role).toBe('ha-read')
    expect(state.postgresServices.primary.type).toBe('LoadBalancer')
    expect(state.postgresServices.replicas.type).toBe('ClusterIP')
    expect(state.selectedExtensions).toEqual([{ name: 'postgis', version: '3.4' }])
    expect(state.restoreBackup).toBe('bk1')
    expect(state.downloadDiskConcurrency).toBe(2)
  })

  it('getReplicationSource distinguishes the four source shapes', () => {
    expect(getReplicationSource({ spec: {} })).toBe('')
    expect(getReplicationSource({ spec: { replicateFrom: { instance: { sgCluster: 'x' } } } })).toBe(
      'cluster'
    )
    expect(
      getReplicationSource({ spec: { replicateFrom: { instance: { external: { host: 'h' } } } } })
    ).toBe('external')
    expect(getReplicationSource({ spec: { replicateFrom: { storage: {} } } })).toBe('storage')
    expect(
      getReplicationSource({
        spec: { replicateFrom: { instance: { external: {} }, storage: {} } },
      })
    ).toBe('external-storage')
  })
})
