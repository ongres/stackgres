import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useOutletContext, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { useTheme } from '../../contexts/ThemeContext'
import {
  DEFAULT_DASHBOARD,
  defaultDashboard,
  fetchGrafanaDashboards,
  grafanaIframeSrc,
  TIME_RANGES,
} from '../../lib/grafana'

// Shared Grafana monitoring panel: the host view supplies the route base and
// the pod list (regular clusters pass their pods, sharded clusters the
// concatenated coordinator and shard pods).
export function MonitoringView({ base, pods }) {
  const { pod, dashboard: rawDashboard, range: rawRange = '' } = useParams()
  // The Vue router also accepts /monitor/:pod/:range (a time range directly
  // after the pod): when the dashboard segment is a known range key and not a
  // dashboard name, treat it as the range.
  const paramIsRange = Boolean(rawDashboard && !rawRange && TIME_RANGES[rawDashboard])
  const dashboardParam = paramIsRange ? '' : rawDashboard
  const range = paramIsRange ? rawDashboard : rawRange
  const { theme } = useTheme()
  const navigate = useNavigate()
  const [dashboards, setDashboards] = useState(null)
  const [unavailable, setUnavailable] = useState(false)

  const activePods = pods.filter((candidate) => candidate.status === 'Active')
  const inactivePods = pods.filter((candidate) => candidate.status !== 'Active')
  const selectedPod = pods.find((candidate) => candidate.name === pod)

  useEffect(() => {
    let cancelled = false
    fetchGrafanaDashboards()
      .then((list) => {
        if (!cancelled) {
          setDashboards(list)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setUnavailable(true)
          toast.error(
            "There was a problem when trying to access the list of Grafana's monitoring dashboards"
          )
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  // No pod in the URL: land on the primary pod and the default dashboard.
  useEffect(() => {
    if (!pod && activePods.length) {
      const primary = pods.find((candidate) => candidate.role === 'primary') || activePods[0]
      navigate(`${base}/${primary.name}/${DEFAULT_DASHBOARD}`, { replace: true })
    }
  }, [pod, pods, activePods, base, navigate])

  // Unknown dashboard or range in the URL: notify and fall back, as Vue does.
  useEffect(() => {
    if (!dashboards || !pod) {
      return
    }
    if (dashboardParam && !dashboards.some((candidate) => candidate.name === dashboardParam)) {
      toast.message(
        'The dashboard specified in the URL could not be found, so the default dashboard has been loaded instead'
      )
      navigate(`${base}/${pod}/${defaultDashboard(dashboards).name}`, { replace: true })
      return
    }
    if (range && !TIME_RANGES[range]) {
      toast.message('The time range specified in the URL is not valid, so it has been ignored')
      navigate(`${base}/${pod}/${dashboardParam || DEFAULT_DASHBOARD}`, { replace: true })
    }
  }, [dashboards, dashboardParam, range, pod, base, navigate])

  if (!activePods.length) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">No active pods have been found for this cluster</p>
  }
  if (unavailable) {
    return (
      <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">
        The list of Grafana monitoring dashboards is not available on this installation.
      </p>
    )
  }
  if (!dashboards || !pod || !selectedPod) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  const dashboard =
    dashboards.find((candidate) => candidate.name === dashboardParam) ||
    defaultDashboard(dashboards)
  const src = grafanaIframeSrc({
    dashboardUrl: dashboard.url,
    theme,
    podIp: selectedPod.ip,
    podName: selectedPod.name,
    range,
  })
  const goTo = (podName, dashboardName, rangeKey) =>
    navigate(`${base}/${podName}/${dashboardName}${rangeKey ? `/${rangeKey}` : ''}`)

  return (
    <section className="flex flex-col gap-3 px-6 pb-6 text-sm">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
          Dashboard
          <select
            value={dashboard.name}
            onChange={(event) => goTo(pod, event.target.value, range)}
            className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))] capitalize"
          >
            {dashboards.map((candidate) => (
              <option key={candidate.name} value={candidate.name} className="capitalize">
                {candidate.name.replaceAll('-', ' ')}
              </option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
          Time range
          <select
            value={range}
            onChange={(event) => goTo(pod, dashboard.name, event.target.value)}
            className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]"
          >
            <option value="">Default</option>
            {Object.entries(TIME_RANGES).map(([key, candidate]) => (
              <option key={key} value={key}>
                {candidate.label}
              </option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-1.5 text-[hsl(var(--muted-foreground))]">
          Node
          <select
            value={pod}
            onChange={(event) => goTo(event.target.value, dashboard.name, range)}
            className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]"
          >
            {activePods.map((candidate) => (
              <option key={candidate.name} value={candidate.name}>
                {candidate.name}
                {candidate.role === 'primary' ? ' (primary)' : ''}
              </option>
            ))}
            {inactivePods.length > 0 && (
              <option disabled>── Inactive nodes: ──</option>
            )}
            {inactivePods.map((candidate) => (
              <option key={candidate.name} disabled>
                {candidate.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {src && (
        <iframe
          title={`Grafana: ${dashboard.name}`}
          src={src}
          className="w-full border"
          style={{ height: 'calc(100vh - 285px)' }}
        />
      )}
    </section>
  )
}

export default function ClusterMonitoring() {
  const { namespace, name } = useParams()
  const { cluster } = useOutletContext()
  const pods = useMemo(() => cluster.pods || [], [cluster.pods])
  return <MonitoringView base={`/${namespace}/sgcluster/${name}/monitor`} pods={pods} />
}
