// Grafana embedding helpers for the monitoring views.
//
// The console origin serves (via the operator's nginx) a /grafana-list
// endpoint with the embeddable dashboards as [{ name, url }], and proxies the
// /d/<uid>/<slug> dashboard pages themselves. The iframe URL reproduces the
// Vue console's parameters verbatim: theme, bare kiosk, var-instance (pod
// IP), var-pod (pod name), and a from/to range fragment.

export async function fetchGrafanaDashboards() {
  const response = await fetch('/grafana-list')
  if (!response.ok) {
    throw new Error(`The dashboards list responded with status ${response.status}`)
  }
  const data = await response.json()
  if (!Array.isArray(data) || !data.length) {
    throw new Error('The dashboards list is empty')
  }
  return data
}

export const DEFAULT_DASHBOARD = 'db-info'

export function defaultDashboard(dashboards) {
  return dashboards.find((dashboard) => dashboard.name === DEFAULT_DASHBOARD) || dashboards[0]
}

export function grafanaIframeSrc({ dashboardUrl, theme, podIp, podName, range = '' }) {
  if (!dashboardUrl) {
    return ''
  }
  return (
    dashboardUrl +
    (dashboardUrl.includes('?') ? '&' : '?') +
    `theme=${theme}&kiosk&` +
    `var-instance=${podIp ?? ''}&var-pod=${podName}` +
    (range && TIME_RANGES[range] ? TIME_RANGES[range].range : '')
  )
}

// Range fragments are pre-encoded ("%2F" = "/"), as Grafana expects them.
export const TIME_RANGES = {
  'last-5-minutes': { label: 'Last 5 minutes', range: '&from=now-5m&to=now' },
  'last-15-minutes': { label: 'Last 15 minutes', range: '&from=now-15m&to=now' },
  'last-30-minutes': { label: 'Last 30 minutes', range: '&from=now-30m&to=now' },
  'last-1-hour': { label: 'Last 1 hour', range: '&from=now-1h&to=now' },
  'last-3-hours': { label: 'Last 3 hours', range: '&from=now-3h&to=now' },
  'last-6-hours': { label: 'Last 6 hours', range: '&from=now-6h&to=now' },
  'last-12-hours': { label: 'Last 12 hours', range: '&from=now-12h&to=now' },
  'last-24-hours': { label: 'Last 24 hours', range: '&from=now-24h&to=now' },
  'last-2-days': { label: 'Last 2 days', range: '&from=now-2d&to=now' },
  'last-7-days': { label: 'Last 7 days', range: '&from=now-7d&to=now' },
  'last-30-days': { label: 'Last 30 days', range: '&from=now-30d&to=now' },
  'last-90-days': { label: 'Last 90 days', range: '&from=now-90d&to=now' },
  'last-6-months': { label: 'Last 6 months', range: '&from=now-6M&to=now' },
  'last-1-year': { label: 'Last 1 year', range: '&from=now-1y&to=now' },
  'last-3-years': { label: 'Last 3 years', range: '&from=now-3y&to=now' },
  'last-5-years': { label: 'Last 5 years', range: '&from=now-5y&to=now' },
  yesterday: { label: 'Yesterday', range: '&from=now-1d%2Fd&to=now-1d%2Fd' },
  'day-before-yesterday': { label: 'Day before yesterday', range: '&from=now-2d%2Fd&to=now-2d%2Fd' },
  'this-day-last-week': { label: 'This day last week', range: '&from=now-7d%2Fd&to=now-7d%2Fd' },
  'previous-week': { label: 'Previous week', range: '&from=now-1w%2Fw&to=now-1w%2Fw' },
  'previous-month': { label: 'Previous month', range: '&from=now-1M%2FM&to=now-1M%2FM' },
  'previous-year': { label: 'Previous year', range: '&from=now-1y%2Fy&to=now-1y%2Fy' },
  today: { label: 'Today', range: '&from=now%2Fd&to=now%2Fd' },
  'today-so-far': { label: 'Today so far', range: '&from=now%2Fd&to=now' },
  'this-week': { label: 'This week', range: '&from=now%2Fw&to=now%2Fw' },
  'this-week-so-far': { label: 'This week so far', range: '&from=now%2Fw&to=now' },
  'this-month': { label: 'This month', range: '&from=now%2FM&to=now%2FM' },
  'this-month-so-far': { label: 'This month so far', range: '&from=now%2FM&to=now' },
  'this-year': { label: 'This year', range: '&from=now%2Fy&to=now%2Fy' },
  'this-year-so-far': { label: 'This year so far', range: '&from=now%2Fy&to=now' },
}
