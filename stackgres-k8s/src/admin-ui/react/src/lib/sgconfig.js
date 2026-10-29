// Load/submit transforms for the operator SGConfig editor, ported from the
// Vue EditSGConfig: annotation/nodeSelector maps become editable rows on
// load and are serialized back on submit, and the jobs pod(Anti)Affinity
// matchLabels follow the same round-trip inside their terms. The resource
// sections the editor does not cover (deploy, operator, developer,
// prometheus, rbac, restapi…) ride along untouched.
import { objectToRows, rowsToObject } from './payloads'

const ensure = (parent, key, value) => {
  if (parent[key] === undefined || parent[key] === null) {
    parent[key] = value
  }
  return parent[key]
}

// Deep-clones the CRD spec and normalizes it into the editable shape.
export function normalizeSGConfigSpec(spec) {
  const result = JSON.parse(JSON.stringify(spec || {}))

  const adminui = ensure(result, 'adminui', {})
  const adminuiResources = ensure(adminui, 'resources', {})
  ensure(adminuiResources, 'requests', { cpu: '', memory: '' })
  ensure(adminuiResources, 'limits', { cpu: '', memory: '' })
  ensure(adminui, 'service', {})
  ensure(result, 'authentication', {})
  ensure(result, 'cert', {})
  ensure(result, 'extensions', {})
  ensure(result, 'grafana', {})
  const jobs = ensure(result, 'jobs', {})
  const jobsResources = ensure(jobs, 'resources', {})
  ensure(jobsResources, 'requests', { cpu: '', memory: '' })
  ensure(jobsResources, 'limits', { cpu: '', memory: '' })
  ensure(result, 'serviceAccount', {})

  jobs.annotations = jobs.annotations ? objectToRows(jobs.annotations, 'label') : []
  jobs.nodeSelector = jobs.nodeSelector ? objectToRows(jobs.nodeSelector, 'label') : []
  const jobsServiceAccount = ensure(jobs, 'serviceAccount', {})
  jobsServiceAccount.annotations = jobsServiceAccount.annotations
    ? objectToRows(jobsServiceAccount.annotations, 'label')
    : []
  jobsServiceAccount.repoCredentials = jobsServiceAccount.repoCredentials || []
  jobs.tolerations = jobs.tolerations || []
  result.serviceAccount.annotations = result.serviceAccount.annotations
    ? objectToRows(result.serviceAccount.annotations, 'label')
    : []
  result.serviceAccount.repoCredentials = result.serviceAccount.repoCredentials || []

  const affinity = jobs.affinity
  if (affinity) {
    ;['podAffinity', 'podAntiAffinity'].forEach((kind) => {
      const block = affinity[kind]
      if (!block) return
      ;(block.preferredDuringSchedulingIgnoredDuringExecution || []).forEach((term) => {
        ;['labelSelector', 'namespaceSelector'].forEach((selector) => {
          const target = term.podAffinityTerm?.[selector]
          if (target) {
            target.matchLabels = target.matchLabels ? objectToRows(target.matchLabels, 'label') : []
          }
        })
      })
      ;(block.requiredDuringSchedulingIgnoredDuringExecution || []).forEach((term) => {
        ;['labelSelector', 'namespaceSelector'].forEach((selector) => {
          if (term[selector]) {
            term[selector].matchLabels = term[selector].matchLabels
              ? objectToRows(term[selector].matchLabels, 'label')
              : []
          }
        })
      })
    })
  }

  return result
}

const cleanResources = (resources) => {
  if (!resources) return undefined
  const clean = {}
  ;['requests', 'limits'].forEach((group) => {
    const values = resources[group]
    if (!values) return
    const entry = {}
    if (values.cpu) entry.cpu = values.cpu
    if (values.memory) entry.memory = values.memory
    if (Object.keys(entry).length) clean[group] = entry
  })
  return Object.keys(clean).length ? clean : undefined
}

// Editable shape → the PUT body: metadata preserved from the loaded CRD with
// the route name forced, rows serialized back to maps.
export function buildSGConfig(spec, previousMetadata, name) {
  const result = JSON.parse(JSON.stringify(spec))

  const jobs = result.jobs || {}
  if (jobs.annotations !== undefined) {
    jobs.annotations = rowsToObject(jobs.annotations, 'label')
  }
  if (jobs.nodeSelector !== undefined) {
    jobs.nodeSelector = rowsToObject(jobs.nodeSelector, 'label')
  }
  if (jobs.serviceAccount?.annotations !== undefined) {
    jobs.serviceAccount.annotations = rowsToObject(jobs.serviceAccount.annotations, 'label')
  }
  if (result.serviceAccount?.annotations !== undefined) {
    result.serviceAccount.annotations = rowsToObject(result.serviceAccount.annotations, 'label')
  }
  const adminuiResources = cleanResources(result.adminui?.resources)
  if (result.adminui) {
    if (adminuiResources) result.adminui.resources = adminuiResources
    else delete result.adminui.resources
  }
  const jobsResources = cleanResources(jobs.resources)
  if (jobsResources) jobs.resources = jobsResources
  else delete jobs.resources

  const affinity = jobs.affinity
  if (affinity) {
    ;['podAffinity', 'podAntiAffinity'].forEach((kind) => {
      const block = affinity[kind]
      if (!block) return
      ;(block.preferredDuringSchedulingIgnoredDuringExecution || []).forEach((term) => {
        ;['labelSelector', 'namespaceSelector'].forEach((selector) => {
          const target = term.podAffinityTerm?.[selector]
          if (target) {
            target.matchLabels =
              target.matchLabels && target.matchLabels.length
                ? rowsToObject(target.matchLabels, 'label')
                : {}
          }
        })
      })
      ;(block.requiredDuringSchedulingIgnoredDuringExecution || []).forEach((term) => {
        ;['labelSelector', 'namespaceSelector'].forEach((selector) => {
          if (term[selector]) {
            term[selector].matchLabels =
              term[selector].matchLabels && term[selector].matchLabels.length
                ? rowsToObject(term[selector].matchLabels, 'label')
                : {}
          }
        })
      })
    })
  }

  return {
    metadata: {
      ...(previousMetadata || {}),
      name,
      namespace: (previousMetadata && previousMetadata.namespace) || 'stackgres',
    },
    spec: result,
  }
}
