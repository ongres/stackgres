// Timestamp, size and quantity helpers shared by the read-only views.
// All date math is done with the native Date/Intl APIs.

const pad = (n, width = 2) => String(n).padStart(width, '0')

// Splits an ISO timestamp into date / time / ms parts, converted to the
// browser's local time when timezone is 'local', kept in UTC otherwise.
export function formatTimestamp(isoString, part, timezone = 'local') {
  if (!isoString) {
    return ''
  }
  const date = new Date(isoString)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const get = (name) => (timezone === 'local' ? date[`get${name}`]() : date[`getUTC${name}`]())
  switch (part) {
    case 'date':
      return `${get('FullYear')}-${pad(get('Month') + 1)}-${pad(get('Date'))}`
    case 'time':
      return `${pad(get('Hours'))}:${pad(get('Minutes'))}:${pad(get('Seconds'))}`
    case 'ms':
      return `.${pad(get('Milliseconds'), 3)}`
    default:
      return (
        `${formatTimestamp(isoString, 'date', timezone)} ` +
        `${formatTimestamp(isoString, 'time', timezone)}`
      )
  }
}

// The ±HH:MM suffix shown next to timestamps.
export function tzOffset(timezone = 'local') {
  if (timezone !== 'local') {
    return '+00:00'
  }
  const offset = -new Date().getTimezoneOffset()
  const sign = offset < 0 ? '-' : '+'
  const absolute = Math.abs(offset)
  return `${sign}${pad(Math.floor(absolute / 60))}:${pad(absolute % 60)}`
}

const BYTE_UNITS = ['B', 'KiB', 'MiB', 'GiB', 'TiB', 'PiB']

export function formatBytes(bytes) {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value < 0) {
    return ''
  }
  let unit = 0
  let scaled = value
  while (scaled >= 1024 && unit < BYTE_UNITS.length - 1) {
    scaled /= 1024
    unit += 1
  }
  return `${Number(scaled.toFixed(2))} ${BYTE_UNITS[unit]}`
}

const QUANTITY_FACTORS = {
  Ki: 1024 ** 1,
  Mi: 1024 ** 2,
  Gi: 1024 ** 3,
  Ti: 1024 ** 4,
  Pi: 1024 ** 5,
  k: 1000,
  K: 1000,
  M: 1000 ** 2,
  G: 1000 ** 3,
  T: 1000 ** 4,
  P: 1000 ** 5,
}

// Kubernetes memory/storage quantity ("500Mi", "1.5Gi", "2048") → bytes,
// for sorting. Unparseable values sort first.
export function quantityToBytes(quantity) {
  if (quantity === null || quantity === undefined || quantity === '') {
    return -1
  }
  const match = String(quantity).match(/^([0-9.]+)\s*([A-Za-z]*)$/)
  if (!match) {
    return -1
  }
  const factor = match[2] ? QUANTITY_FACTORS[match[2]] : 1
  return factor ? parseFloat(match[1]) * factor : -1
}

// Kubernetes CPU quantity ("500m", "2") → millicores, for sorting.
export function cpuToMillis(quantity) {
  if (quantity === null || quantity === undefined || quantity === '') {
    return -1
  }
  const text = String(quantity)
  if (text.endsWith('m')) {
    return parseFloat(text)
  }
  const value = parseFloat(text)
  return Number.isFinite(value) ? value * 1000 : -1
}

export function prefix(text, length = 2) {
  return text ? String(text).slice(0, length) : ''
}

// Elapsed milliseconds → "HH:MM:SS".
export function formatDuration(milliseconds) {
  if (!Number.isFinite(milliseconds) || milliseconds < 0) {
    return ''
  }
  const seconds = Math.floor(milliseconds / 1000)
  return `${pad(Math.floor(seconds / 3600))}:${pad(Math.floor((seconds % 3600) / 60))}:${pad(seconds % 60)}`
}
