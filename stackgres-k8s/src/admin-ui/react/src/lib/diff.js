// Deep "non-default" filter for the form summary (Vue's SHOW DEFAULT VALUES
// toggle): with the toggle off, only the parts of the payload that differ
// from the pristine form's payload are shown. metadata is always kept.

const isPlainObject = (value) =>
  value !== null && typeof value === 'object' && !Array.isArray(value)

const equal = (a, b) => JSON.stringify(a) === JSON.stringify(b)

export function omitDefaults(value, defaults) {
  if (!isPlainObject(value) || !isPlainObject(defaults)) {
    return value
  }
  const result = {}
  Object.keys(value).forEach((key) => {
    if (key === 'metadata') {
      result[key] = value[key]
      return
    }
    if (!Object.hasOwn(defaults, key)) {
      result[key] = value[key]
      return
    }
    if (isPlainObject(value[key]) && isPlainObject(defaults[key])) {
      const nested = omitDefaults(value[key], defaults[key])
      if (isPlainObject(nested) && Object.keys(nested).length === 0) {
        return
      }
      result[key] = nested
      return
    }
    if (!equal(value[key], defaults[key])) {
      result[key] = value[key]
    }
  })
  return result
}
