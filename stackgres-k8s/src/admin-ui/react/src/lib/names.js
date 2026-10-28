// Resource-name helpers shared by the create forms.

// Kubernetes resource names must be lowercase RFC 1123 subdomains.
const RFC1123 = /^[a-z0-9]([-a-z0-9]*[a-z0-9])?$/

export function isValidName(name) {
  return typeof name === 'string' && name.length > 0 && name.length <= 253 && RFC1123.test(name)
}

export function nameValidationMessage(name) {
  if (!name) {
    return 'A name is required'
  }
  if (!isValidName(name)) {
    return "Names must consist of lowercase alphanumeric characters or '-', and must start and end with an alphanumeric character"
  }
  return ''
}

const pad = (n) => n.toString().padStart(2, '0')

// Locale-independent YYYY-MM-DD-HH-MM-SS stamp used to seed generated names
// ('bk' + stamp, 'op' + stamp). Numeric date components only — the browser
// locale must never leak into resource names (issue #3266).
export function dateStamp(timezone = 'local', now = new Date()) {
  const get = (name) => (timezone === 'local' ? now[`get${name}`]() : now[`getUTC${name}`]())
  return (
    `${get('FullYear')}-${pad(get('Month') + 1)}-${pad(get('Date'))}` +
    `-${pad(get('Hours'))}-${pad(get('Minutes'))}-${pad(get('Seconds'))}`
  )
}

export function generatedName(prefix, timezone) {
  return `${prefix}${dateStamp(timezone)}`
}
