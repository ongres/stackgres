// Kind metadata shared by the sidebar, overviews, breadcrumbs and routes.
// `plural` is both the API resource and the list URL segment; `singular` is
// the detail URL segment, kept identical to the Vue console's URL space.
export const KINDS = {
  sgclusters: { label: 'StackGres Clusters', singular: 'sgcluster', crd: 'SGCluster' },
  sgshardedclusters: {
    label: 'StackGres Sharded Clusters',
    singular: 'sgshardedcluster',
    crd: 'SGShardedCluster',
  },
  sgstreams: { label: 'StackGres Streams', singular: 'sgstream', crd: 'SGStream' },
  sginstanceprofiles: {
    label: 'Instance Profiles',
    singular: 'sginstanceprofile',
    crd: 'SGInstanceProfile',
  },
  sgpgconfigs: { label: 'Postgres Configurations', singular: 'sgpgconfig', crd: 'SGPostgresConfig' },
  sgpoolconfigs: {
    label: 'Connection Pooling Configurations',
    singular: 'sgpoolconfig',
    crd: 'SGPoolingConfig',
  },
  sgobjectstorages: {
    label: 'Object Storages',
    singular: 'sgobjectstorage',
    crd: 'SGObjectStorage',
  },
  sgscripts: { label: 'Scripts', singular: 'sgscript', crd: 'SGScript' },
  sgdistributedlogs: {
    label: 'Logs Servers',
    singular: 'sgdistributedlog',
    crd: 'SGDistributedLogs',
  },
  sgbackups: { label: 'Cluster Backups', singular: 'sgbackup', crd: 'SGBackup' },
  sgdbops: { label: 'Database Operations', singular: 'sgdbop', crd: 'SGDbOps' },
}

export function detailPath(plural, namespace, name) {
  return `/${namespace}/${KINDS[plural].singular}/${name}`
}

export function listPath(plural, namespace) {
  return `/${namespace}/${plural}`
}

// First condition with status 'True' names the current state (dbops, etc.).
export function activeCondition(conditions) {
  const active = (conditions || []).find((condition) => condition.status === 'True')
  return active ? active.type : ''
}
