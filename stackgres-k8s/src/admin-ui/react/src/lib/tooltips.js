// Field help tooltips, ported from the Vue console: the build ships
// /admin/info/sg-tooltips.json (the restapi swagger merged with the CRD
// descriptions); schemas are renamed sg<type> and flattened so that
// getTooltip('sgcluster.spec.instances') walks to a description.

// The Vue cleanupTooltips: object schemas collapse to their properties
// (keeping the description), and array schemas get their item properties
// copied up one level. Deeper array children keep their `properties` wrapper,
// which is why some Vue keys carry literal `.properties.` segments.
export function cleanupTooltips(object) {
  Object.keys(object).forEach((key) => {
    const entry = object[key]
    if (!entry || typeof entry !== 'object' || !Object.hasOwn(entry, 'type')) {
      return
    }
    if (entry.type !== 'array' && Object.hasOwn(entry, 'properties')) {
      const description = Object.hasOwn(entry, 'description') ? entry.description : ''
      object[key] = entry.properties
      object[key].description = description
      cleanupTooltips(object[key])
    } else if (
      entry.type === 'array' &&
      entry.items &&
      Object.hasOwn(entry.items, 'properties')
    ) {
      Object.keys(entry.items.properties).forEach((item) => {
        entry[item] = entry.items.properties[item]
      })
    }
  })
}

// The Vue setTooltips mutation: ClusterDto → sgcluster, etc. The swagger
// names the instance profile schema ProfileDto, while the templates ask for
// sginstanceprofile — both names are indexed.
export function indexTooltips(schemas) {
  const tooltips = {}
  Object.keys(schemas).forEach((key) => {
    tooltips[`sg${key.replace('Dto', '').toLowerCase()}`] = schemas[key]
  })
  if (tooltips.sgprofile && !tooltips.sginstanceprofile) {
    tooltips.sginstanceprofile = tooltips.sgprofile
  }
  cleanupTooltips(tooltips)
  return tooltips
}

const SPECIAL_CASES = {
  'sgpostgresconfig.spec.postgresql.conf': ['sgpostgresconfig', 'spec', 'postgresql.conf'],
  'sgpoolingconfig.spec.pgBouncer.pgbouncer.ini': ['sgpoolingconfig', 'spec', 'pgBouncer', 'pgbouncer.ini'],
  'sginstanceprofile.spec.hugePages.hugepages-1Gi': ['sginstanceprofile', 'spec', 'hugePages', 'hugepages-1Gi'],
  'sginstanceprofile.spec.hugePages.hugepages-2Mi': ['sginstanceprofile', 'spec', 'hugePages', 'hugepages-2Mi'],
}

const FALLBACK = 'Information not available'

export function getTooltip(tooltips, field) {
  if (!tooltips || !Object.keys(tooltips).length || tooltips.error) {
    return FALLBACK
  }
  const segments = SPECIAL_CASES[field] || field.split('.')
  let current = tooltips
  for (const segment of segments) {
    if (!current || typeof current !== 'object' || !Object.hasOwn(current, segment)) {
      return FALLBACK
    }
    current = current[segment]
  }
  return current && typeof current === 'object' && Object.hasOwn(current, 'description')
    ? current.description
    : FALLBACK
}
