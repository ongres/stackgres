// Small status marker for backups/dbops rows; the status text doubles as the
// hook for the design-system pass to color.
export default function StatusDot({ status }) {
  if (!status) {
    return null
  }
  return (
    <span
      title={status}
      data-status={status}
      aria-label={status}
      className="inline-block h-2 w-2 rounded-full border"
    />
  )
}
