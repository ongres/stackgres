// Small status marker for backups/dbops rows, colored like the cloud
// console's status dots (lib/colors.js there): green for healthy/terminal
// success, red for failures, yellow for pending/unknown.
const DOT_COLORS = {
  Completed: 'bg-green-500',
  Running: 'bg-yellow-500 animate-pulse',
  Pending: 'bg-yellow-500 animate-pulse',
  Failed: 'bg-red-500',
}

export default function StatusDot({ status }) {
  if (!status) {
    return null
  }
  return (
    <span
      title={status}
      data-status={status}
      aria-label={status}
      className={`inline-block h-2 w-2 shrink-0 rounded-full ${DOT_COLORS[status] || 'bg-yellow-500'}`}
    />
  )
}
