// Pure payload builders for the CRD create/edit forms, ported from the Vue
// forms' submit methods. Each takes the form state (plus the previously
// fetched resource in edit mode, whose metadata carries resourceVersion) and
// returns the exact request body. Keeping them pure lets tests assert
// payloads and keeps failed submits from corrupting form state (the Vue
// forms mutated state while building).

const has = (object, path) =>
  path.split('.').reduce((node, key) => (node && typeof node === 'object' ? node[key] : undefined), object) !==
  undefined

// cpu unit 'CPU' means no suffix; 'm' is appended. Memory always suffixed.
const cpuValue = (value, unit) => (unit !== 'CPU' ? `${value}${unit}` : value)

// {d,h,m,s} → ISO-8601 duration, null when every part is zero/empty.
export function isoDuration(duration) {
  const part = (value) => {
    const text = String(value ?? '')
    return text.length ? text : '0'
  }
  const iso = `P${part(duration.d)}DT${part(duration.h)}H${part(duration.m)}M${part(duration.s)}S`
  return iso === 'P0DT0H0M0S' ? null : iso
}

function parseContainers(containers, previousContainers) {
  const result = {}
  const previous =
    previousContainers && typeof previousContainers === 'object' ? previousContainers : {}
  containers.forEach((container) => {
    if (
      container.name.length &&
      (container.RAM.length ||
        container.CPU.length ||
        (container.hugePages1Gi || '').length ||
        (container.hugePages2Mi || '').length)
    ) {
      const previousContainer =
        previous[container.name] && typeof previous[container.name] === 'object'
          ? previous[container.name]
          : {}
      const previousHugePages =
        previousContainer.hugePages && typeof previousContainer.hugePages === 'object'
          ? previousContainer.hugePages
          : {}
      result[container.name] = {
        ...previousContainer,
        ...(container.CPU.length && { cpu: cpuValue(container.CPU, container.CPUUnit) }),
        ...(container.RAM.length && { memory: `${container.RAM}${container.RAMUnit}` }),
        ...(((container.hugePages2Mi || '').length || (container.hugePages1Gi || '').length) && {
          hugePages: {
            ...previousHugePages,
            ...((container.hugePages2Mi || '').length && {
              'hugepages-2Mi': `${container.hugePages2Mi}${container.hugePages2MiUnit}`,
            }),
            ...((container.hugePages1Gi || '').length && {
              'hugepages-1Gi': `${container.hugePages1Gi}${container.hugePages1GiUnit}`,
            }),
          },
        }),
      }
    }
  })
  return Object.keys(result).length ? result : null
}

export function buildInstanceProfile(state, previous = {}) {
  const requestsContainers = parseContainers(
    state.requests.containers,
    has(previous, 'spec.requests.containers') ? previous.spec.requests.containers : null
  )
  const requestsInitContainers = parseContainers(
    state.requests.initContainers,
    has(previous, 'spec.requests.initContainers') ? previous.spec.requests.initContainers : null
  )
  return {
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      cpu: cpuValue(state.cpu, state.cpuUnit),
      memory: `${state.ram}${state.ramUnit}`,
      ...((state.hugePages1Gi.length || state.hugePages2Mi.length) && {
        hugePages: {
          ...(has(previous, 'spec.hugePages') && previous.spec.hugePages),
          ...(state.hugePages2Mi.length && {
            'hugepages-2Mi': `${state.hugePages2Mi}${state.hugePages2MiUnit}`,
          }),
          ...(state.hugePages1Gi.length && {
            'hugepages-1Gi': `${state.hugePages1Gi}${state.hugePages1GiUnit}`,
          }),
        },
      } || { hugePages: null }),
      containers: parseContainers(
        state.containers,
        has(previous, 'spec.containers') ? previous.spec.containers : null
      ),
      initContainers: parseContainers(
        state.initContainers,
        has(previous, 'spec.initContainers') ? previous.spec.initContainers : null
      ),
      ...(((state.requests.RAM.length ||
        state.requests.CPU.length ||
        requestsContainers !== null ||
        requestsInitContainers !== null) && {
        requests: {
          ...(has(previous, 'spec.requests') && previous.spec.requests),
          ...(state.requests.CPU.length && {
            cpu: cpuValue(state.requests.CPU, state.requests.CPUUnit),
          }),
          ...(state.requests.RAM.length && {
            memory: `${state.requests.RAM}${state.requests.RAMUnit}`,
          }),
          containers: requestsContainers,
          initContainers: requestsInitContainers,
        },
      }) || { requests: null }),
    },
  }
}

export function buildPgConfig(state, previous = {}) {
  return {
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      postgresVersion: state.postgresVersion,
      'postgresql.conf': state.params,
    },
  }
}

export function buildPoolConfig(state, previous = {}) {
  return {
    kind: 'StackGresConnectionPoolingConfig',
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      pgBouncer: {
        ...(has(previous, 'spec.pgBouncer') && previous.spec.pgBouncer),
        'pgbouncer.ini': state.params,
      },
    },
  }
}

const MASK = '******'

// Masked-secret protocol: a credential field still showing the mask is
// untouched — resend secretKeySelectors verbatim only when EVERY masked
// field of the backend is untouched; otherwise send the new plaintext.
export function buildObjectStorage(state, previous = {}) {
  const { editMode } = state
  const sendPlain = (value) => !editMode || value !== MASK
  const config = {
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      type: state.type,
    },
  }
  for (const backend of ['s3', 's3Compatible', 'gcs', 'azureBlob']) {
    if (backend !== state.type) {
      delete config.spec[backend]
    }
  }
  switch (state.type) {
    case 's3':
      config.spec.s3 = {
        ...(has(previous, 'spec.s3') && previous.spec.s3),
        bucket: state.s3.bucket,
        ...(state.s3.region.length && { region: state.s3.region }),
        ...(state.s3.storageClass.length && { storageClass: state.s3.storageClass }),
        awsCredentials: {
          ...(has(previous, 'spec.s3.awsCredentials') && previous.spec.s3.awsCredentials),
          ...(sendPlain(state.s3.accessKeyId) && { accessKeyId: state.s3.accessKeyId }),
          ...(sendPlain(state.s3.secretAccessKey) && {
            secretAccessKey: state.s3.secretAccessKey,
          }),
          ...(editMode &&
            state.s3.accessKeyId === MASK &&
            state.s3.secretAccessKey === MASK && { secretKeySelectors: state.secretKeySelectors }),
        },
      }
      break
    case 's3Compatible':
      config.spec.s3Compatible = {
        ...(has(previous, 'spec.s3Compatible') && previous.spec.s3Compatible),
        bucket: state.s3Compatible.bucket,
        ...(state.s3Compatible.enablePathStyleAddressing && { enablePathStyleAddressing: true }),
        ...(state.s3Compatible.endpoint.length && { endpoint: state.s3Compatible.endpoint }),
        ...(state.s3Compatible.region.length && { region: state.s3Compatible.region }),
        ...(state.s3Compatible.storageClass.length && {
          storageClass: state.s3Compatible.storageClass,
        }),
        awsCredentials: {
          ...(has(previous, 'spec.s3Compatible.awsCredentials') &&
            previous.spec.s3Compatible.awsCredentials),
          ...(sendPlain(state.s3Compatible.accessKeyId) && {
            accessKeyId: state.s3Compatible.accessKeyId,
          }),
          ...(sendPlain(state.s3Compatible.secretAccessKey) && {
            secretAccessKey: state.s3Compatible.secretAccessKey,
          }),
          ...(editMode &&
            state.s3Compatible.accessKeyId === MASK &&
            state.s3Compatible.secretAccessKey === MASK && {
              secretKeySelectors: state.secretKeySelectors,
            }),
        },
      }
      break
    case 'gcs':
      config.spec.gcs = {
        ...(has(previous, 'spec.gcs') && previous.spec.gcs),
        bucket: state.gcs.bucket,
        gcpCredentials: {
          ...(has(previous, 'spec.gcs.gcpCredentials') && previous.spec.gcs.gcpCredentials),
          ...(state.gcs.fetchCredentials && { fetchCredentialsFromMetadataService: true }),
          ...(!state.gcs.fetchCredentials && {
            ...(sendPlain(state.gcs.serviceAccountJSON) && {
              serviceAccountJSON: state.gcs.serviceAccountJSON,
            }),
            ...(editMode &&
              state.gcs.serviceAccountJSON === MASK && {
                secretKeySelectors: state.secretKeySelectors,
              }),
          }),
        },
      }
      break
    case 'azureBlob':
      config.spec.azureBlob = {
        ...(has(previous, 'spec.azureBlob') && previous.spec.azureBlob),
        bucket: state.azureBlob.bucket,
        azureCredentials: {
          ...(has(previous, 'spec.azureBlob.azureCredentials') &&
            previous.spec.azureBlob.azureCredentials),
          ...(sendPlain(state.azureBlob.storageAccount) && {
            storageAccount: state.azureBlob.storageAccount,
          }),
          ...(sendPlain(state.azureBlob.accessKey) && { accessKey: state.azureBlob.accessKey }),
          ...(editMode &&
            state.azureBlob.storageAccount === MASK &&
            state.azureBlob.accessKey === MASK && {
              secretKeySelectors: state.secretKeySelectors,
            }),
        },
      }
      break
  }
  return config
}

// Entries not fully specified for their source are dropped, and null/empty
// keys removed, as the Vue cleanupScripts did — but pure, and without the
// cached CR's status leaking into the PUT body.
export function buildScript(state, previous = {}) {
  const scripts = []
  state.scripts.forEach((entry, index) => {
    const source = state.scriptSource[index]
    const cleaned = {}
    Object.entries(entry).forEach(([key, value]) => {
      if (value === null || (typeof value === 'string' && !value.length)) {
        return
      }
      cleaned[key] = value
    })
    if (source === 'raw') {
      delete cleaned.scriptFrom
      if (cleaned.script && cleaned.script.length) {
        scripts.push(cleaned)
      }
    } else {
      delete cleaned.script
      const ref = cleaned.scriptFrom && cleaned.scriptFrom[source]
      if (ref && ref.name.length && ref.key.length) {
        scripts.push(cleaned)
      }
    }
  })
  return {
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      continueOnError: state.continueOnError,
      managedVersions: state.managedVersions,
      scripts,
    },
  }
}

export function buildBackup(state, previous = {}) {
  return {
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      sgCluster: state.sgCluster,
      managedLifecycle: state.managedLifecycle,
    },
  }
}

const BLANK_TOLERATION = JSON.stringify({
  key: '',
  operator: 'Equal',
  value: null,
  effect: null,
  tolerationSeconds: null,
})

export function buildDbOps(state) {
  const tolerations = state.tolerations.filter(
    (toleration) => JSON.stringify(toleration) !== BLANK_TOLERATION
  )
  const dbOps = {
    metadata: { name: state.name, namespace: state.namespace },
    spec: {
      sgCluster: state.sgCluster,
      op: state.op,
      ...(state.runAt.length && { runAt: state.runAt }),
      ...(isoDuration(state.timeout) && { timeout: isoDuration(state.timeout) }),
      maxRetries: Number(state.maxRetries),
      ...(tolerations.length && { scheduling: { tolerations } }),
    },
  }

  switch (state.op) {
    case 'vacuum': {
      const vacuum = { ...state.vacuum }
      if (state.vacuumPerDbs) {
        vacuum.databases = state.vacuumDbs.map((db) => {
          const resolved = { name: db.name }
          ;['full', 'freeze', 'analyze', 'disablePageSkipping'].forEach((key) => {
            resolved[key] = db[key] === 'inherit' ? state.vacuum[key] : db[key]
          })
          return resolved
        })
      }
      dbOps.spec.vacuum = vacuum
      break
    }
    case 'repack': {
      const repack = {
        noOrder: state.repack.noOrder,
        waitTimeout: isoDuration(state.repack.waitTimeout),
        noKillBackend: state.repack.noKillBackend,
        noAnalyze: state.repack.noAnalyze,
        excludeExtension: state.repack.excludeExtension,
      }
      if (state.repackPerDbs) {
        repack.databases = state.repackDbs.map((db) => ({
          name: db.name,
          noOrder: db.noOrder === 'inherit' ? state.repack.noOrder : db.noOrder,
          waitTimeout: db.inheritTimeout
            ? isoDuration(state.repack.waitTimeout)
            : isoDuration(db.waitTimeout),
          noKillBackend: db.noKillBackend === 'inherit' ? state.repack.noKillBackend : db.noKillBackend,
          noAnalyze: db.noAnalyze === 'inherit' ? state.repack.noAnalyze : db.noAnalyze,
          excludeExtension:
            db.excludeExtension === 'inherit' ? state.repack.excludeExtension : db.excludeExtension,
        }))
      }
      dbOps.spec.repack = repack
      break
    }
    case 'benchmark': {
      const settings = state.benchmark[state.benchmark.type]
      dbOps.spec.benchmark = {
        type: state.benchmark.type,
        connectionType: state.benchmark.connectionType,
        [state.benchmark.type]: {
          databaseSize: `${settings.databaseSize.size}${settings.databaseSize.unit}`,
          duration: isoDuration(settings.duration),
          usePreparedStatements: settings.usePreparedStatements,
          concurrentClients: Number(settings.concurrentClients),
          threads: Number(settings.threads),
        },
      }
      break
    }
    default:
      dbOps.spec[state.op] = state[state.op]
      break
  }
  return dbOps
}

export function buildRestartDbOps({ name, namespace, sgCluster }) {
  return {
    metadata: { name, namespace },
    spec: {
      sgCluster,
      op: 'restart',
      restart: { method: 'InPlace', onlyPendingRestart: false },
    },
  }
}

// key/value rows → object, skipping rows with an empty key or value
// (the Vue parseProps), null when nothing qualifies unless keepEmpty.
export function rowsToObject(rows, keyName = 'annotation') {
  const result = {}
  ;(rows || []).forEach((row) => {
    const key = row[keyName]
    if (key && String(key).length && row.value && String(row.value).length) {
      result[key] = row.value
    }
  })
  return result
}

export function objectToRows(object, keyName = 'annotation') {
  return Object.entries(object || {}).map(([key, value]) => ({ [keyName]: key, value }))
}

const BLANK_DL_TOLERATION = JSON.stringify({
  key: '',
  operator: 'Equal',
  value: null,
  effect: null,
  tolerationSeconds: null,
})

export function buildDistributedLogs(state, previous = {}) {
  const has = (object, path) =>
    path
      .split('.')
      .reduce(
        (node, key) => (node && typeof node === 'object' ? node[key] : undefined),
        object
      ) !== undefined
  const nodeSelector = rowsToObject(state.nodeSelector, 'label')
  const tolerations = (state.tolerations || []).filter(
    (toleration) => JSON.stringify(toleration) !== BLANK_DL_TOLERATION
  )
  const annotationsAll = rowsToObject(state.annotationsAll)
  const annotationsPods = rowsToObject(state.annotationsPods)
  const annotationsServices = rowsToObject(state.annotationsServices)

  return {
    metadata: {
      ...(has(previous, 'metadata') && previous.metadata),
      name: state.name,
      namespace: state.namespace,
    },
    spec: {
      ...(has(previous, 'spec') && previous.spec),
      profile: state.profile,
      persistentVolume: {
        ...(has(previous, 'spec.persistentVolume') && previous.spec.persistentVolume),
        size: `${state.volumeSize}${state.volumeUnit}`,
        ...(state.storageClass && state.storageClass.length && { storageClass: state.storageClass }),
      },
      ...((state.resourceProfile.length && { sgInstanceProfile: state.resourceProfile }) || {
        sgInstanceProfile: null,
      }),
      ...((has(previous, 'spec.configurations') || state.pgConfig.length) && {
        configurations: {
          ...(has(previous, 'spec.configurations') && previous.spec.configurations),
          ...((state.pgConfig.length && { sgPostgresConfig: state.pgConfig }) || {
            sgPostgresConfig: null,
          }),
        },
      }),
      ...((has(previous, 'spec.nonProductionOptions') ||
        state.clusterPodAntiAffinity !== null ||
        state.patroniResourceRequirements !== null ||
        state.clusterResourceRequirements !== null) && {
        nonProductionOptions: {
          ...(has(previous, 'spec.nonProductionOptions') && previous.spec.nonProductionOptions),
          ...((state.clusterPodAntiAffinity !== null && {
            disableClusterPodAntiAffinity: state.clusterPodAntiAffinity,
          }) || { disableClusterPodAntiAffinity: null }),
          ...((state.patroniResourceRequirements !== null && {
            disablePatroniResourceRequirements: state.patroniResourceRequirements,
          }) || { disablePatroniResourceRequirements: null }),
          ...((state.clusterResourceRequirements !== null && {
            disableClusterResourceRequirements: state.clusterResourceRequirements,
          }) || { disableClusterResourceRequirements: null }),
        },
      }),
      ...((has(previous, 'spec.scheduling') ||
        Object.keys(nodeSelector).length ||
        tolerations.length) && {
        scheduling: {
          ...(has(previous, 'spec.scheduling') && previous.spec.scheduling),
          ...((Object.keys(nodeSelector).length && { nodeSelector }) || { nodeSelector: null }),
          ...((tolerations.length && { tolerations }) || { tolerations: null }),
        },
      }),
      postgresServices: {
        ...(has(previous, 'spec.postgresServices') && previous.spec.postgresServices),
        primary: {
          ...(has(previous, 'spec.postgresServices.primary') &&
            previous.spec.postgresServices.primary),
          type: state.postgresServicesPrimaryType,
          ...(state.postgresServicesPrimaryLoadBalancerIP.length && {
            loadBalancerIP: state.postgresServicesPrimaryLoadBalancerIP,
          }),
        },
        replicas: {
          ...(has(previous, 'spec.postgresServices.replicas') &&
            previous.spec.postgresServices.replicas),
          enabled: state.postgresServicesReplicas,
          type: state.postgresServicesReplicasType,
          ...(state.postgresServicesReplicasLoadBalancerIP.length && {
            loadBalancerIP: state.postgresServicesReplicasLoadBalancerIP,
          }),
        },
      },
      ...((has(previous, 'spec.metadata') ||
        Object.keys(annotationsAll).length ||
        Object.keys(annotationsPods).length ||
        Object.keys(annotationsServices).length) && {
        metadata: {
          ...(has(previous, 'spec.metadata') && previous.spec.metadata),
          annotations: {
            ...(has(previous, 'spec.metadata.annotations') && previous.spec.metadata.annotations),
            ...(Object.keys(annotationsAll).length && { allResources: annotationsAll }),
            ...(Object.keys(annotationsPods).length && { pods: annotationsPods }),
            ...(Object.keys(annotationsServices).length && { services: annotationsServices }),
          },
        },
      }),
    },
  }
}

// The SGStream payload: the form state mirrors the CRD, this transform ports
// the Vue parseCrdSpec (size join, rows→objects, credential nulling).
export function buildStream(crd) {
  const parsed = JSON.parse(JSON.stringify(crd))
  const size = parsed.spec?.pods?.persistentVolume?.size
  if (size && typeof size === 'object') {
    parsed.spec.pods.persistentVolume.size = `${size.size}${size.unit}`
  }
  ;['requests', 'limits'].forEach((prop) => {
    const rows = parsed.spec?.pods?.resources?.[prop]
    if (rows !== undefined) {
      parsed.spec.pods.resources[prop] =
        rows && rows.length ? rowsToObject(rows, 'label') : null
    }
  })
  const scheduling = parsed.spec?.pods?.resources?.scheduling
  if (scheduling && scheduling.nodeSelector !== undefined) {
    scheduling.nodeSelector =
      scheduling.nodeSelector && scheduling.nodeSelector.length
        ? rowsToObject(scheduling.nodeSelector, 'label')
        : null
  }
  if (parsed.spec.target.type === 'CloudEvent' && parsed.spec.target.cloudEvent?.http?.headers) {
    parsed.spec.target.cloudEvent.http.headers = rowsToObject(
      parsed.spec.target.cloudEvent.http.headers,
      'label'
    )
  }
  if (parsed.spec.target.type === 'PgLambda' && parsed.spec.target.pgLambda?.knative?.http?.headers) {
    parsed.spec.target.pgLambda.knative.http.headers = rowsToObject(
      parsed.spec.target.pgLambda.knative.http.headers,
      'label'
    )
  }
  ;[
    ['source', ['sgCluster', 'postgres']],
    ['target', ['sgCluster']],
  ].forEach(([parent, types]) => {
    types.forEach((type) => {
      ;['username', 'password'].forEach((key) => {
        const selector = parsed.spec?.[parent]?.[type]?.[key]
        if (selector && selector.key === null && selector.name === null) {
          parsed.spec[parent][type][key] = null
        }
      })
    })
  })
  return parsed
}

// Clone payload: a deep copy of the cached resource with the new identity,
// plus the SGCluster-specific cleanups of the Vue clone dialog. As in Vue,
// status/uid/resourceVersion are not stripped (the API ignores them on POST).
export function buildClonePayload(kind, item, { name, namespace }, originalName) {
  const clone = JSON.parse(JSON.stringify(item))
  clone.metadata.name = name
  clone.metadata.namespace = namespace
  if (kind === 'sgclusters') {
    if (clone.spec.initialData?.restore) {
      delete clone.spec.initialData.restore
      if (!Object.keys(clone.spec.initialData).length) {
        delete clone.spec.initialData
      }
    }
    const logs = clone.spec.distributedLogs
    if (logs?.sgDistributedLogs && !logs.sgDistributedLogs.includes('.')) {
      logs.sgDistributedLogs = `${item.metadata.namespace}.${logs.sgDistributedLogs}`
    }
    ;(clone.spec.initialData?.scripts || []).forEach((script) => {
      if (script.scriptFrom?.configMapKeyRef) {
        delete script.scriptFrom.configMapScript
      }
    })
    if (clone.spec.managedSql?.scripts) {
      clone.spec.managedSql.scripts = clone.spec.managedSql.scripts.filter(
        (script) => !(script.sgScript && script.sgScript === `${originalName}-default`)
      )
    }
  }
  return clone
}
