import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { useKindList } from '../../contexts/DataContext'
import { useCanI } from '../../contexts/PermissionsContext'
import { useSettings } from '../../contexts/SettingsContext'
import { fromDateTimeLocal, toDateTimeLocal } from '../../lib/datetime'
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
  const canI = useCanI()
  const { timezone } = useSettings()
  const allItems = useKindList(plural, namespace)
  const items =
    clusterScoped && allItems
      ? allItems.filter((item) => item.spec.sgCluster === clusterName)
      : allItems
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [operation, setOperation] = useState('')
  // Backups date-range filter (Vue SGBackups daterangepicker): values kept as
  // UTC timestamps, displayed by the inputs in the console timezone.
  const [dateStart, setDateStart] = useState('')
  const [dateEnd, setDateEnd] = useState('')

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
    if (dateStart && dateEnd && plural === 'sgbackups') {
      // Vue parity: only Completed backups have a stored timestamp to compare,
      // so the date filter hides everything else.
      const start = new Date(dateStart).getTime()
      const end = new Date(dateEnd).getTime()
      result = result.filter((item) => {
        if (item.status?.process?.status !== 'Completed') {
          return false
        }
        const stored = new Date(item.status.process.timing?.stored || '').getTime()
        return !Number.isNaN(stored) && stored >= start && stored <= end
      })
    }
    return result
  }, [items, status, operation, plural, dateStart, dateEnd])

  if (visible === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">{KINDS[plural].label}</h1>
      {hasToolbar && (
        <div className="flex flex-wrap items-center gap-2 text-sm">
          <input
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            placeholder="Search…"
            aria-label="Search"
            className="pl-3 pr-3 py-1.5 text-sm rounded border bg-[hsl(var(--input-background))] w-52 focus:outline-none focus:ring-1 focus:ring-[hsl(var(--primary))]"
          />
          <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value)} className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]">
              <option value="">All</option>
              {(plural === 'sgbackups' ? BACKUP_STATUSES : DBOPS_STATUSES).map((candidate) => (
                <option key={candidate}>{candidate}</option>
              ))}
            </select>
          </label>
          {plural === 'sgbackups' && (
            <>
              <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
                From
                <input type="datetime-local" step="1" className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]" aria-label="Backups from"
                  value={toDateTimeLocal(dateStart, timezone)}
                  onChange={(event) => setDateStart(fromDateTimeLocal(event.target.value, timezone))} />
              </label>
              <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
                To
                <input type="datetime-local" step="1" className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]" aria-label="Backups to"
                  value={toDateTimeLocal(dateEnd, timezone)}
                  min={toDateTimeLocal(dateStart, timezone) || undefined}
                  onChange={(event) => setDateEnd(fromDateTimeLocal(event.target.value, timezone))} />
              </label>
              {(dateStart || dateEnd) && (
                <button type="button" className="text-sm border rounded px-2 py-1 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]"
                  onClick={() => { setDateStart(''); setDateEnd('') }}>
                  Reset dates
                </button>
              )}
            </>
          )}
          {plural === 'sgdbops' && (
            <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
              Operation
              <select
                value={operation}
                onChange={(event) => setOperation(event.target.value)}
                className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]"
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
        emptyAction={
          !clusterScoped && canI('create', plural, namespace) ? (
            <Link
              to={`/${namespace}/${plural}/new`}
              className="mt-1 flex items-center gap-1 text-[hsl(var(--primary))] hover:underline"
            >
              <Plus className="h-4 w-4" strokeWidth={1.5} /> Create a new {KINDS[plural].crd}
            </Link>
          ) : null
        }
      />
    </section>
  )
}
