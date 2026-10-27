import { activeCondition } from '../../lib/kinds'
import {
  cpuToMillis,
  formatBytes,
  formatDuration,
  formatTimestamp,
  prefix,
  quantityToBytes,
  tzOffset,
} from '../../lib/format'

const name = { key: 'name', header: 'Name', cell: (item) => item.metadata.name, sortValue: (item) => item.metadata.name }

const bool = (value, fallback) => String(value ?? fallback)

function timestampCell(value, timezone) {
  if (!value) {
    return ''
  }
  return `${formatTimestamp(value, 'date', timezone)} ${formatTimestamp(value, 'time', timezone)} ${tzOffset(timezone)}`
}

// Column sets per kind, ported from the Vue list views. `timezone` feeds the
// timestamp columns; sortValue keeps the Vue sort types (memory/cpu/timestamp).
export function listColumns(plural, timezone) {
  switch (plural) {
    case 'sgclusters':
      return {
        defaultSort: { key: 'name', direction: 'asc' },
        columns: [
          { ...name, header: 'StackGres Cluster' },
          {
            key: 'instances',
            header: 'Instances',
            cell: (item) => item.spec.instances,
            sortValue: (item) => item.spec.instances,
          },
          {
            key: 'cpu',
            header: 'CPU',
            cell: (item) => item.stats?.cpuRequested ?? '',
            sortValue: (item) => cpuToMillis(item.stats?.cpuRequested),
          },
          {
            key: 'memory',
            header: 'Memory',
            cell: (item) => (item.stats?.memoryRequested ?? '').replace('.00', ''),
            sortValue: (item) => quantityToBytes(item.stats?.memoryRequested),
          },
          {
            key: 'disk',
            header: 'Disk',
            cell: (item) => item.spec.pods?.persistentVolume?.size ?? '',
            sortValue: (item) => quantityToBytes(item.spec.pods?.persistentVolume?.size),
          },
          {
            key: 'health',
            header: 'Health',
            cell: (item) => `${item.podsReady ?? 0} / ${item.spec.instances}`,
          },
        ],
      }
    case 'sgshardedclusters':
      return {
        defaultSort: { key: 'name', direction: 'asc' },
        columns: [
          { ...name, header: 'StackGres Sharded Cluster' },
          {
            key: 'coordinatorInstances',
            header: 'Coordinator Instances',
            cell: (item) => item.spec.coordinator?.instances,
            sortValue: (item) => item.spec.coordinator?.instances,
          },
          {
            key: 'shards',
            header: 'Clusters / Instances per Shard',
            cell: (item) =>
              `${item.spec.shards?.clusters ?? item.spec.workers?.clusters ?? ''} / ${
                item.spec.shards?.instancesPerCluster ?? item.spec.workers?.instancesPerCluster ?? ''
              }`,
          },
          {
            key: 'cpu',
            header: 'CPU',
            cell: (item) =>
              `${item.stats?.coordinator?.cpuRequested ?? '-'} / ${item.stats?.workers?.cpuRequested ?? '-'}`,
            sortValue: (item) => cpuToMillis(item.stats?.coordinator?.cpuRequested),
          },
          {
            key: 'memory',
            header: 'Memory',
            cell: (item) =>
              `${(item.stats?.coordinator?.memoryRequested ?? '-').replace('.00', '')} / ${(
                item.stats?.workers?.memoryRequested ?? '-'
              ).replace('.00', '')}`,
            sortValue: (item) => quantityToBytes(item.stats?.coordinator?.memoryRequested),
          },
          {
            key: 'disk',
            header: 'Disk',
            cell: (item) =>
              `${item.spec.coordinator?.pods?.persistentVolume?.size ?? ''} / ${
                item.spec.shards?.pods?.persistentVolume?.size ??
                item.spec.workers?.pods?.persistentVolume?.size ??
                ''
              }`,
          },
        ],
      }
    case 'sgstreams':
      return {
        defaultSort: { key: 'name', direction: 'asc' },
        columns: [
          { ...name, header: 'StackGres Stream' },
          {
            key: 'sourceType',
            header: 'Source Type',
            cell: (item) => item.spec.source?.type,
            sortValue: (item) => item.spec.source?.type ?? '',
          },
          {
            key: 'source',
            header: 'Source',
            cell: (item) =>
              item.spec.source?.type === 'SGCluster'
                ? item.spec.source?.sgCluster?.name
                : item.spec.source?.postgres?.host,
          },
          {
            key: 'targetType',
            header: 'Target Type',
            cell: (item) => item.spec.target?.type,
            sortValue: (item) => item.spec.target?.type ?? '',
          },
          {
            key: 'target',
            header: 'Target',
            cell: (item) => {
              const target = item.spec.target || {}
              if (target.type === 'SGCluster') return target.sgCluster?.name
              if (target.type === 'CloudEvent') return target.cloudEvent?.http?.url
              return target.pgLambda?.knative?.http?.url
            },
          },
          {
            key: 'disk',
            header: 'Disk',
            cell: (item) => item.spec.pods?.persistentVolume?.size ?? '',
            sortValue: (item) => quantityToBytes(item.spec.pods?.persistentVolume?.size),
          },
        ],
      }
    case 'sgbackups':
      return {
        defaultSort: { key: 'timestamp', direction: 'desc' },
        columns: [
          {
            key: 'timestamp',
            header: 'Timestamp',
            cell: (item) =>
              item.status?.process?.status === 'Completed'
                ? timestampCell(item.status?.process?.timing?.stored, timezone)
                : '',
            sortValue: (item) => item.status?.process?.timing?.stored ?? '',
          },
          {
            key: 'managedLifecycle',
            header: 'Managed Lifecycle',
            cell: (item) => bool(item.spec.managedLifecycle, false),
            sortValue: (item) => bool(item.spec.managedLifecycle, false),
          },
          {
            key: 'status',
            header: 'Status',
            cell: (item) => item.status?.process?.status ?? 'Pending',
            sortValue: (item) => item.status?.process?.status ?? 'Pending',
          },
          {
            key: 'type',
            header: 'Type',
            cell: (item) => (item.status?.volumeSnapshot ? 'Snapshot' : 'Base Backup'),
          },
          {
            key: 'size',
            header: 'Size',
            cell: (item) => formatBytes(item.status?.backupInformation?.size?.compressed),
            sortValue: (item) => quantityToBytes(item.status?.backupInformation?.size?.uncompressed),
          },
          {
            key: 'pg',
            header: 'PG',
            cell: (item) => prefix(item.status?.backupInformation?.postgresVersion),
            sortValue: (item) => item.status?.backupInformation?.postgresVersion ?? '',
          },
          name,
          {
            key: 'cluster',
            header: 'Source Cluster',
            cell: (item) => item.spec.sgCluster,
            sortValue: (item) => item.spec.sgCluster ?? '',
          },
        ],
      }
    case 'sgdbops':
      return {
        defaultSort: { key: 'start', direction: 'asc' },
        columns: [
          {
            key: 'start',
            header: 'Start',
            cell: (item) =>
              timestampCell(item.spec.runAt || item.status?.opStarted, timezone) || 'ASAP',
            sortValue: (item) => item.spec.runAt || item.status?.opStarted || '',
          },
          {
            key: 'op',
            header: 'Operation',
            cell: (item) => item.spec.op,
            sortValue: (item) => item.spec.op,
          },
          name,
          {
            key: 'status',
            header: 'Status',
            cell: (item) => activeCondition(item.status?.conditions),
          },
          {
            key: 'cluster',
            header: 'Target Cluster',
            cell: (item) => item.spec.sgCluster,
            sortValue: (item) => item.spec.sgCluster ?? '',
          },
          {
            key: 'elapsed',
            header: 'Elapsed',
            cell: (item) => {
              if (!item.status?.opStarted) return ''
              const running = activeCondition(item.status?.conditions) === 'Running'
              const finish = running
                ? Date.now()
                : new Date(
                    (item.status?.conditions || []).find((c) => c.status === 'True')
                      ?.lastTransitionTime || item.status.opStarted
                  ).getTime()
              return formatDuration(finish - new Date(item.status.opStarted).getTime())
            },
          },
          {
            key: 'retries',
            header: 'Current / Max Retries',
            cell: (item) => `${item.status?.opRetries ?? 0} / ${item.spec.maxRetries ?? 1}`,
          },
          {
            key: 'timedOut',
            header: 'Timed Out',
            cell: (item) =>
              (item.status?.conditions || []).some(
                (c) => c.status === 'True' && c.type === 'Failed' && c.reason === 'OperationTimedOut'
              )
                ? 'YES'
                : 'NO',
          },
        ],
      }
    case 'sginstanceprofiles':
      return {
        defaultSort: { key: 'ram', direction: 'asc' },
        columns: [
          name,
          {
            key: 'ram',
            header: 'RAM',
            cell: (item) => item.spec.memory,
            sortValue: (item) => quantityToBytes(item.spec.memory),
          },
          {
            key: 'hugepages2Mi',
            header: 'Huge Pages 2Mi',
            cell: (item) => item.spec.hugePages?.['hugepages-2Mi'] ?? '',
            sortValue: (item) => quantityToBytes(item.spec.hugePages?.['hugepages-2Mi']),
          },
          {
            key: 'hugepages1Gi',
            header: 'Huge Pages 1Gi',
            cell: (item) => item.spec.hugePages?.['hugepages-1Gi'] ?? '',
            sortValue: (item) => quantityToBytes(item.spec.hugePages?.['hugepages-1Gi']),
          },
          {
            key: 'cpu',
            header: 'CPU',
            cell: (item) => item.spec.cpu,
            sortValue: (item) => cpuToMillis(item.spec.cpu),
          },
        ],
      }
    case 'sgpgconfigs':
      return {
        defaultSort: { key: 'name', direction: 'asc' },
        columns: [
          name,
          {
            key: 'pg',
            header: 'PG',
            cell: (item) => item.spec.postgresVersion,
            sortValue: (item) => item.spec.postgresVersion ?? '',
          },
          {
            key: 'parameters',
            header: 'Parameters',
            cell: (item) =>
              (item.status?.['postgresql.conf'] || [])
                .map((parameter) => `${parameter.parameter}: ${parameter.value}`)
                .join('; '),
          },
        ],
      }
    case 'sgpoolconfigs':
      return {
        defaultSort: { key: 'name', direction: 'desc' },
        columns: [
          name,
          {
            key: 'parameters',
            header: 'Parameters',
            cell: (item) =>
              (item.status?.pgBouncer?.['pgbouncer.ini'] || [])
                .map((parameter) => `${parameter.parameter}: ${parameter.value}`)
                .join('; '),
          },
        ],
      }
    case 'sgobjectstorages':
      return {
        defaultSort: { key: 'name', direction: 'desc' },
        columns: [
          name,
          {
            key: 'type',
            header: 'Type',
            cell: (item) => item.spec.type,
            sortValue: (item) => item.spec.type ?? '',
          },
          {
            key: 'bucket',
            header: 'Bucket',
            cell: (item) => item.spec[item.spec.type]?.bucket ?? '',
          },
        ],
      }
    case 'sgscripts':
      return {
        defaultSort: { key: 'name', direction: 'asc' },
        columns: [
          name,
          {
            key: 'managedVersions',
            header: 'Managed Versions',
            cell: (item) => bool(item.spec.managedVersions, true),
            sortValue: (item) => bool(item.spec.managedVersions, true),
          },
          {
            key: 'continueOnError',
            header: 'Continue on Error',
            cell: (item) => bool(item.spec.continueOnError, false),
            sortValue: (item) => bool(item.spec.continueOnError, false),
          },
          {
            key: 'scriptCount',
            header: 'Script Count',
            cell: (item) => (item.spec.scripts || []).length,
          },
          {
            key: 'defaultScript',
            header: 'Default Script',
            cell: (item) =>
              item.status?.clusters?.length &&
              item.metadata.name === `${item.status.clusters[0]}-default`
                ? 'YES'
                : 'NO',
          },
        ],
      }
    case 'sgdistributedlogs':
      return {
        defaultSort: { key: 'name', direction: 'asc' },
        columns: [
          name,
          {
            key: 'disk',
            header: 'Disk',
            cell: (item) => item.spec.persistentVolume?.size ?? '',
            sortValue: (item) => quantityToBytes(item.spec.persistentVolume?.size),
          },
          {
            key: 'profile',
            header: 'Instance Profile',
            cell: (item) => item.spec.sgInstanceProfile ?? '',
          },
        ],
      }
    default:
      return { defaultSort: { key: 'name', direction: 'asc' }, columns: [name] }
  }
}
