// Conversions between the UTC timestamps the API speaks (YYYY-MM-DDTHH:mm:ssZ)
// and the strings a datetime-local input displays and emits. The input shows
// wall time in the frame selected by the console timezone setting (local or
// UTC); browsers interpret its value, min and max attributes in that same
// frame, so every bound passed to the input must go through toDateTimeLocal.

const pad = (part) => String(part).padStart(2, '0')

// UTC timestamp (or anything Date can parse) → input-frame string.
export function toDateTimeLocal(timestamp, timezone) {
  if (!timestamp) {
    return ''
  }
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  if (timezone === 'local') {
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  }
  return date.toISOString().slice(0, 19)
}

// Input-frame string → UTC timestamp. datetime-local emits YYYY-MM-DDTHH:mm
// when seconds are zero and YYYY-MM-DDTHH:mm:ss otherwise.
export function fromDateTimeLocal(value, timezone) {
  if (!value) {
    return ''
  }
  if (timezone === 'local') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? '' : date.toISOString().slice(0, 19) + 'Z'
  }
  return value.length === 16 ? `${value}:00Z` : `${value}Z`
}
