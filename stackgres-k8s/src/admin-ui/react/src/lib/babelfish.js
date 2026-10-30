// Babelfish Compass report parsing (Vue BabelfishCompass.parseCompassReport):
// the API answers {logs, report} where report is an HTML page whose <pre>
// holds the Compass text report. The text splits into a setup block (up to
// the first ==== divider), the report body (first ---- divider up to the
// Run Metrics marker) and the metrics block (Run Metrics marker to the last
// ==== divider). Key/value lines use ' : '; paths under /home/babelfish/ are
// internal and skipped, as in Vue.
const DIVIDER = /^={10,}/
const REPORT_START = /^-{10,}/
const METRICS = '--- Run Metrics'

const parsePairs = (lines) => {
  const pairs = {}
  lines.forEach((line) => {
    const index = line.indexOf(' : ')
    if (index === -1) {
      return
    }
    const key = line.slice(0, index).trim()
    const value = line.slice(index + 3).trim()
    if (key && value && !value.includes('/home/babelfish/')) {
      pairs[key] = value
    }
  })
  return pairs
}

export function parseCompassReport(reportText) {
  // Everything from the trailing BabelfishFeatures.cfg block on is dropped.
  const featuresIndex = reportText.indexOf('BabelfishFeatures.cfg file')
  const text = featuresIndex === -1 ? reportText : reportText.slice(0, featuresIndex)
  const lines = text.split('\n')

  const firstDivider = lines.findIndex((line) => DIVIDER.test(line))
  const reportStart = lines.findIndex((line) => REPORT_START.test(line))
  const metricsStart = lines.findIndex((line) => line.includes(METRICS))
  const lastDivider = lines.reduce(
    (last, line, index) => (DIVIDER.test(line) ? index : last),
    lines.length
  )

  return {
    setup: parsePairs(lines.slice(0, firstDivider === -1 ? 0 : firstDivider)),
    report: lines
      .slice(
        reportStart === -1 ? 0 : reportStart + 1,
        metricsStart === -1 ? lines.length : metricsStart
      )
      .join('\n')
      .trim(),
    metrics: parsePairs(
      metricsStart === -1 ? [] : lines.slice(metricsStart + 1, lastDivider)
    ),
  }
}

// Pulls the <pre> body out of the HTML report page.
export function extractReportText(reportHtml) {
  const match = /<pre[^>]*>([\s\S]*?)<\/pre>/i.exec(reportHtml || '')
  return match ? match[1] : reportHtml || ''
}
