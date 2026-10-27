// Recursive read-only renderer for a CRD spec/status object: nested
// definition lists, arrays as indexed entries, primitives as label/value.
export default function SpecTree({ value, label }) {
  if (value === null || value === undefined) {
    return null
  }

  if (Array.isArray(value)) {
    if (!value.length) {
      return null
    }
    return (
      <li>
        <strong>{label}</strong>
        <ul className="border-l pl-4">
          {value.map((entry, index) => (
            <SpecTree key={index} value={entry} label={`#${index + 1}`} />
          ))}
        </ul>
      </li>
    )
  }

  if (typeof value === 'object') {
    const entries = Object.entries(value)
    if (!entries.length) {
      return null
    }
    const body = (
      <ul className={label ? 'border-l pl-4' : 'flex flex-col gap-1'}>
        {entries.map(([key, child]) => (
          <SpecTree key={key} value={child} label={key} />
        ))}
      </ul>
    )
    return label ? (
      <li>
        <strong>{label}</strong>
        {body}
      </li>
    ) : (
      body
    )
  }

  return (
    <li className="flex gap-2">
      <strong>{label}:</strong>
      <span className="break-all">{String(value)}</span>
    </li>
  )
}
