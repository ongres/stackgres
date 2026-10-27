import { useMemo, useState } from 'react'
import { useParams } from 'react-router-dom'
import { useKindList } from '../../contexts/DataContext'
import { useSettings } from '../../contexts/SettingsContext'
import { activeCondition, detailPath, KINDS } from '../../lib/kinds'
import DataTable from '../DataTable'
import { listColumns } from './listColumns'

const BACKUP_STATUSES = ['Completed', 'Running', 'Pending', 'Failed']
const DBOPS_STATUSES = ['Completed', 'Running', 'Failed']
const DBOPS_OPS = [
  'benchmark',
  'vacuum',
  'repack',
  'securityUpgrade',
  'minorVersionUpgrade',
  'majorVersionUpgrade',
  'restart',
]

// `clusterScoped` renders the sgbackups list inside a cluster's Backups tab:
// rows narrowed to that cluster, PG and Source Cluster columns hidden.
export default function ResourceList({ plural, clusterScoped = false }) {
  const { namespace, name: clusterName } = useParams()
  const { timezone } = useSettings()
  const allItems = useKindList(plural, namespace)
  const items =
    clusterScoped && allItems
      ? allItems.filter((item) => item.spec.sgCluster === clusterName)
      : allItems
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [operation, setOperation] = useState('')

  const { columns, defaultSort } = useMemo(() => {
    const config = listColumns(plural, timezone)
    if (clusterScoped) {
      config.columns = config.columns.filter((column) => !['pg', 'cluster'].includes(column.key))
    }
    return config
  }, [plural, timezone, clusterScoped])

  const hasToolbar = plural === 'sgbackups' || plural === 'sgdbops'

  const visible = useMemo(() => {
    if (!items) {
      return items
    }
    let result = items
    if (status && plural === 'sgbackups') {
      result = result.filter((item) => (item.status?.process?.status ?? 'Pending') === status)
    }
    if (status && plural === 'sgdbops') {
      result = result.filter((item) => activeCondition(item.status?.conditions) === status)
    }
    if (operation && plural === 'sgdbops') {
      result = result.filter((item) => item.spec.op === operation)
    }
    return result
  }, [items, status, operation, plural])

  if (visible === null) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <section className="flex flex-col gap-4 p-8">
      <h1 className="text-2xl font-semibold">{KINDS[plural].label}</h1>
      {hasToolbar && (
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Search…"
            aria-label="Search"
            className="border p-2"
          />
          <label className="flex items-center gap-1">
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value)} className="border p-1">
              <option value="">All</option>
              {(plural === 'sgbackups' ? BACKUP_STATUSES : DBOPS_STATUSES).map((candidate) => (
                <option key={candidate}>{candidate}</option>
              ))}
            </select>
          </label>
          {plural === 'sgdbops' && (
            <label className="flex items-center gap-1">
              Operation
              <select
                value={operation}
                onChange={(event) => setOperation(event.target.value)}
                className="border p-1"
              >
                <option value="">All</option>
                {DBOPS_OPS.map((candidate) => (
                  <option key={candidate}>{candidate}</option>
                ))}
              </select>
            </label>
          )}
        </div>
      )}
      <DataTable
        columns={columns}
        items={visible}
        filterText={keyword}
        defaultSort={defaultSort}
        rowLink={(item) => detailPath(plural, namespace, item.metadata.name)}
        emptyMessage={`No ${KINDS[plural].crd}s have been found`}
      />
    </section>
  )
}
