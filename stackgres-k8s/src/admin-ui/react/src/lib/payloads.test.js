import { describe, expect, it } from 'vitest'
import {
  buildBackup,
  buildDbOps,
  buildInstanceProfile,
  buildObjectStorage,
  buildPgConfig,
  buildPoolConfig,
  buildRestartDbOps,
  buildScript,
  isoDuration,
} from './payloads'

describe('isoDuration', () => {
  it('builds ISO-8601 durations and nulls the zero duration', () => {
    expect(isoDuration({ d: 0, h: 0, m: 0, s: 0 })).toBeNull()
    expect(isoDuration({ d: '', h: '', m: '', s: '' })).toBeNull()
    expect(isoDuration({ d: '1', h: '2', m: '0', s: '30' })).toBe('P1DT2H0M30S')
    expect(isoDuration({ d: 0, h: '1', m: 0, s: 0 })).toBe('P0DT1H0M0S')
  })
})

describe('buildInstanceProfile', () => {
  const blankRow = {
    name: '',
    CPU: '',
    CPUUnit: 'CPU',
    RAM: '',
    RAMUnit: 'Gi',
    hugePages1Gi: '',
    hugePages1GiUnit: 'Gi',
    hugePages2Mi: '',
    hugePages2MiUnit: 'Gi',
  }
  const base = {
    name: 'size-m',
    namespace: 'default',
    cpu: '2',
    cpuUnit: 'CPU',
    ram: '4',
    ramUnit: 'Gi',
    hugePages1Gi: '',
    hugePages1GiUnit: 'Gi',
    hugePages2Mi: '',
    hugePages2MiUnit: 'Gi',
    containers: [blankRow],
    initContainers: [blankRow],
    requests: { RAM: '', RAMUnit: 'Gi', CPU: '', CPUUnit: 'CPU', containers: [blankRow], initContainers: [blankRow] },
  }

  it('builds the minimal profile with explicit nulls as the Vue form did', () => {
    expect(buildInstanceProfile(base)).toEqual({
      metadata: { name: 'size-m', namespace: 'default' },
      spec: {
        cpu: '2',
        memory: '4Gi',
        hugePages: null,
        containers: null,
        initContainers: null,
        requests: null,
      },
    })
  })

  it('appends units, maps containers by name and keeps millicpu suffix', () => {
    const payload = buildInstanceProfile({
      ...base,
      cpu: '500',
      cpuUnit: 'm',
      hugePages2Mi: '2',
      containers: [
        { ...blankRow, name: 'pgbouncer', CPU: '250', CPUUnit: 'm', RAM: '256', RAMUnit: 'Mi' },
        blankRow,
      ],
    })
    expect(payload.spec.cpu).toBe('500m')
    expect(payload.spec.hugePages).toEqual({ 'hugepages-2Mi': '2Gi' })
    expect(payload.spec.containers).toEqual({
      pgbouncer: { cpu: '250m', memory: '256Mi' },
    })
  })

  it('preserves previous metadata and unknown spec fields on edit', () => {
    const previous = {
      metadata: { name: 'size-m', namespace: 'default', resourceVersion: '42', uid: 'u1' },
      spec: { cpu: '1', memory: '2Gi', somethingUnknown: true },
    }
    const payload = buildInstanceProfile(base, previous)
    expect(payload.metadata.resourceVersion).toBe('42')
    expect(payload.spec.somethingUnknown).toBe(true)
    expect(payload.spec.cpu).toBe('2')
  })
})

describe('buildPgConfig / buildPoolConfig', () => {
  it('builds the pgconfig payload', () => {
    expect(
      buildPgConfig({ name: 'pg16', namespace: 'default', postgresVersion: '16', params: 'work_mem=16MB' })
    ).toEqual({
      metadata: { name: 'pg16', namespace: 'default' },
      spec: { postgresVersion: '16', 'postgresql.conf': 'work_mem=16MB' },
    })
  })

  it('builds the poolconfig payload with the legacy kind', () => {
    const payload = buildPoolConfig({ name: 'pool1', namespace: 'default', params: 'max_client_conn=200' })
    expect(payload.kind).toBe('StackGresConnectionPoolingConfig')
    expect(payload.spec.pgBouncer['pgbouncer.ini']).toBe('max_client_conn=200')
  })
})

describe('buildObjectStorage', () => {
  const base = {
    editMode: false,
    name: 'store1',
    namespace: 'default',
    type: 's3',
    secretKeySelectors: {},
    s3: { bucket: 'b', region: '', storageClass: '', accessKeyId: 'AK', secretAccessKey: 'SK' },
    s3Compatible: {
      bucket: '',
      endpoint: '',
      region: '',
      storageClass: '',
      enablePathStyleAddressing: false,
      accessKeyId: '',
      secretAccessKey: '',
    },
    gcs: { bucket: '', fetchCredentials: false, serviceAccountJSON: '' },
    azureBlob: { bucket: '', storageAccount: '', accessKey: '' },
  }

  it('builds s3 credentials in create mode', () => {
    expect(buildObjectStorage(base).spec.s3.awsCredentials).toEqual({
      accessKeyId: 'AK',
      secretAccessKey: 'SK',
    })
  })

  it('resends secretKeySelectors only when every masked field is untouched', () => {
    const selectors = { accessKeyId: { name: 'sec', key: 'id' } }
    const untouched = buildObjectStorage({
      ...base,
      editMode: true,
      secretKeySelectors: selectors,
      s3: { ...base.s3, accessKeyId: '******', secretAccessKey: '******' },
    })
    expect(untouched.spec.s3.awsCredentials).toEqual({ secretKeySelectors: selectors })

    const partiallyChanged = buildObjectStorage({
      ...base,
      editMode: true,
      secretKeySelectors: selectors,
      s3: { ...base.s3, accessKeyId: 'NEW', secretAccessKey: '******' },
    })
    expect(partiallyChanged.spec.s3.awsCredentials).toEqual({ accessKeyId: 'NEW' })
  })

  it('drops other backends when the type changed on edit', () => {
    const payload = buildObjectStorage(
      { ...base, editMode: true, type: 's3' },
      { spec: { type: 'gcs', gcs: { bucket: 'old' } } }
    )
    expect(payload.spec.gcs).toBeUndefined()
    expect(payload.spec.type).toBe('s3')
  })

  it('gcs sends metadata-service flag instead of credentials', () => {
    const payload = buildObjectStorage({
      ...base,
      type: 'gcs',
      gcs: { bucket: 'g', fetchCredentials: true, serviceAccountJSON: '' },
    })
    expect(payload.spec.gcs.gcpCredentials).toEqual({ fetchCredentialsFromMetadataService: true })
  })
})

describe('buildScript', () => {
  it('drops incomplete entries and strips empty keys without leaking status', () => {
    const payload = buildScript(
      {
        name: 'init',
        namespace: 'default',
        continueOnError: false,
        managedVersions: true,
        scripts: [
          { name: 's1', wrapInTransaction: null, storeStatusInDatabase: false, retryOnError: false, user: '', database: '', script: 'SELECT 1;' },
          { name: '', wrapInTransaction: null, storeStatusInDatabase: false, retryOnError: false, user: '', database: '', script: '' },
          { name: 'from-secret', wrapInTransaction: null, storeStatusInDatabase: false, retryOnError: false, user: '', database: '', scriptFrom: { secretKeyRef: { name: 'sec', key: 'sql' } } },
        ],
        scriptSource: ['raw', 'raw', 'secretKeyRef'],
      },
      { metadata: { resourceVersion: '7' }, spec: {}, status: { clusters: ['c1'] } }
    )
    expect(payload.spec.scripts).toHaveLength(2)
    expect(payload.spec.scripts[0]).toEqual({
      name: 's1',
      storeStatusInDatabase: false,
      retryOnError: false,
      script: 'SELECT 1;',
    })
    expect(payload.spec.scripts[1].scriptFrom.secretKeyRef).toEqual({ name: 'sec', key: 'sql' })
    expect(payload.metadata.resourceVersion).toBe('7')
    expect(payload.status).toBeUndefined()
  })
})

describe('buildBackup / buildDbOps / restart dialog', () => {
  it('builds the backup payload', () => {
    expect(
      buildBackup({ name: 'bk1', namespace: 'default', sgCluster: 'uptodate', managedLifecycle: true })
    ).toEqual({
      metadata: { name: 'bk1', namespace: 'default' },
      spec: { sgCluster: 'uptodate', managedLifecycle: true },
    })
  })

  const dbopsBase = {
    name: 'op1',
    namespace: 'default',
    sgCluster: 'uptodate',
    op: 'restart',
    runAt: '',
    timeout: { d: 0, h: 0, m: 0, s: 0 },
    maxRetries: '0',
    tolerations: [{ key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null }],
    restart: { method: 'InPlace', onlyPendingRestart: false },
    vacuum: { full: false, freeze: false, analyze: true, disablePageSkipping: false },
    vacuumPerDbs: false,
    vacuumDbs: [],
    repack: { noOrder: false, waitTimeout: { d: 0, h: 0, m: 0, s: 0 }, noKillBackend: false, noAnalyze: false, excludeExtension: false },
    repackPerDbs: false,
    repackDbs: [],
    benchmark: {
      type: 'pgbench',
      connectionType: 'primary-service',
      pgbench: {
        databaseSize: { size: '1', unit: 'Gi' },
        duration: { d: 0, h: 0, m: '5', s: 0 },
        usePreparedStatements: false,
        concurrentClients: '4',
        threads: '2',
      },
    },
  }

  it('builds a restart op dropping blank tolerations and the zero timeout', () => {
    expect(buildDbOps(dbopsBase)).toEqual({
      metadata: { name: 'op1', namespace: 'default' },
      spec: {
        sgCluster: 'uptodate',
        op: 'restart',
        maxRetries: 0,
        restart: { method: 'InPlace', onlyPendingRestart: false },
      },
    })
  })

  it('resolves vacuum per-database inherit values without mutating state', () => {
    const state = {
      ...dbopsBase,
      op: 'vacuum',
      vacuumPerDbs: true,
      vacuumDbs: [{ name: 'app', full: 'inherit', freeze: true, analyze: 'inherit', disablePageSkipping: 'inherit' }],
    }
    const payload = buildDbOps(state)
    expect(payload.spec.vacuum.databases).toEqual([
      { name: 'app', full: false, freeze: true, analyze: true, disablePageSkipping: false },
    ])
    expect(state.vacuumDbs[0].full).toBe('inherit')
    expect(state.vacuum.databases).toBeUndefined()
  })

  it('builds the benchmark payload with size unit, iso duration and numbers', () => {
    const payload = buildDbOps({ ...dbopsBase, op: 'benchmark', runAt: '2026-11-01T10:00:00Z' })
    expect(payload.spec.runAt).toBe('2026-11-01T10:00:00Z')
    expect(payload.spec.benchmark).toEqual({
      type: 'pgbench',
      connectionType: 'primary-service',
      pgbench: {
        databaseSize: '1Gi',
        duration: 'P0DT0H5M0S',
        usePreparedStatements: false,
        concurrentClients: 4,
        threads: 2,
      },
    })
  })

  it('builds the restart-dialog payload exactly as the Vue dialog', () => {
    expect(buildRestartDbOps({ name: 'op2026', namespace: 'default', sgCluster: 'pgsql' })).toEqual({
      metadata: { name: 'op2026', namespace: 'default' },
      spec: { sgCluster: 'pgsql', op: 'restart', restart: { method: 'InPlace', onlyPendingRestart: false } },
    })
  })
})
