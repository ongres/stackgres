import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowDown, ArrowUp, Inbox } from 'lucide-react'
import { renderTooltip } from './forms/fields'

// Generic read-only table: client-side sorting, text filtering and paging.
// columns: [{ key, header, cell(item) → node, sortValue?(item) → comparable,
// tooltip? (help-tooltip key or element, shown next to the header) }]
export default function DataTable({
  columns,
  items,
  rowLink,
  filterText = '',
  defaultSort = null,
  pageSize = 20,
  emptyMessage = 'No resources found',
  emptyAction = null,
}) {
  const [sort, setSort] = useState(defaultSort)
  const [page, setPage] = useState(0)

  const sorted = useMemo(() => {
    let visible = items
    if (filterText) {
      const needle = filterText.toLowerCase()
      visible = items.filter((item) => JSON.stringify(item).toLowerCase().includes(needle))
    }
    if (!sort) {
      return visible
    }
    const column = columns.find((candidate) => candidate.key === sort.key)
    if (!column || !column.sortValue) {
      return visible
    }
    return [...visible].sort((a, b) => {
      const left = column.sortValue(a)
      const right = column.sortValue(b)
      if (left === right) return 0
      const result = left > right ? 1 : -1
      return sort.direction === 'desc' ? -result : result
    })
  }, [items, filterText, sort, columns])

  const pages = Math.max(1, Math.ceil(sorted.length / pageSize))
  const currentPage = Math.min(page, pages - 1)
  const pageItems = sorted.slice(currentPage * pageSize, (currentPage + 1) * pageSize)

  const toggleSort = (column) => {
    if (!column.sortValue) {
      return
    }
    setSort((previous) =>
      previous && previous.key === column.key
        ? { key: column.key, direction: previous.direction === 'asc' ? 'desc' : 'asc' }
        : { key: column.key, direction: 'asc' }
    )
  }

  if (!sorted.length) {
    return (
      <div className="flex flex-col items-center gap-2 rounded border bg-[hsl(var(--card))] px-6 py-12 text-center shadow-sm">
        <Inbox className="h-8 w-8 text-[hsl(var(--muted-foreground))]" strokeWidth={1.25} aria-hidden="true" />
        <p className="text-sm text-[hsl(var(--muted-foreground))]">{emptyMessage}</p>
        {emptyAction}
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="rounded border overflow-auto">
      <table className="w-full table-auto text-left">
        <thead className="bg-[hsl(var(--muted))] sticky top-0 z-10">
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="px-4 py-2">
                <span className="inline-flex items-center whitespace-nowrap">
                  {column.sortValue ? (
                    <button
                      type="button"
                      onClick={() => toggleSort(column)}
                      className="inline-flex items-center gap-1 font-semibold"
                    >
                      {column.header}
                      {sort && sort.key === column.key &&
                        (sort.direction === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />)}
                    </button>
                  ) : (
                    column.header
                  )}
                  {renderTooltip(column.tooltip)}
                </span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {pageItems.map((item) => (
            <tr key={`${item.metadata.namespace}/${item.metadata.name}`} className="border-t">
              {columns.map((column, index) => (
                <td key={column.key} className="px-4 py-2 align-top">
                  {index === 0 && rowLink ? (
                    <Link to={rowLink(item)} className="text-[hsl(var(--primary))] hover:underline">
                      {column.cell(item)}
                    </Link>
                  ) : (
                    column.cell(item)
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      </div>
      {pages > 1 && (
        <nav aria-label="Table pages" className="flex items-center justify-center gap-2 text-sm">
          {Array.from({ length: pages }, (_, index) => (
            <button
              key={index}
              type="button"
              onClick={() => setPage(index)}
              aria-current={index === currentPage ? 'page' : undefined}
              className={`px-2 py-1 rounded hover:bg-[hsl(var(--muted))] ${
                index === currentPage
                  ? 'font-semibold text-[hsl(var(--primary))]'
                  : 'text-[hsl(var(--muted-foreground))]'
              }`}
            >
              {index + 1}
            </button>
          ))}
        </nav>
      )}
    </div>
  )
}
