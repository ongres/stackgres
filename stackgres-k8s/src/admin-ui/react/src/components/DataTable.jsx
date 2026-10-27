import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowDown, ArrowUp } from 'lucide-react'

// Generic read-only table: client-side sorting, text filtering and paging.
// columns: [{ key, header, cell(item) → node, sortValue?(item) → comparable }]
export default function DataTable({
  columns,
  items,
  rowLink,
  filterText = '',
  defaultSort = null,
  pageSize = 20,
  emptyMessage = 'No resources found',
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
    return <p className="p-4 text-sm">{emptyMessage}</p>
  }

  return (
    <div className="flex flex-col gap-2">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="border-b p-2">
                {column.sortValue ? (
                  <button
                    type="button"
                    onClick={() => toggleSort(column)}
                    className="flex items-center gap-1 font-semibold"
                  >
                    {column.header}
                    {sort && sort.key === column.key &&
                      (sort.direction === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />)}
                  </button>
                ) : (
                  column.header
                )}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {pageItems.map((item) => (
            <tr key={`${item.metadata.namespace}/${item.metadata.name}`}>
              {columns.map((column, index) => (
                <td key={column.key} className="border-b p-2 align-top">
                  {index === 0 && rowLink ? (
                    <Link to={rowLink(item)} className="underline">
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
      {pages > 1 && (
        <nav aria-label="Table pages" className="flex items-center justify-center gap-2 text-sm">
          {Array.from({ length: pages }, (_, index) => (
            <button
              key={index}
              type="button"
              onClick={() => setPage(index)}
              aria-current={index === currentPage ? 'page' : undefined}
              className={index === currentPage ? 'font-bold' : ''}
            >
              {index + 1}
            </button>
          ))}
        </nav>
      )}
    </div>
  )
}
