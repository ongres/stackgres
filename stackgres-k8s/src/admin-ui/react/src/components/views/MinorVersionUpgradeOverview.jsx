import { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import Chart from 'react-apexcharts'
import { useSettings } from '../../contexts/SettingsContext'
import { activeCondition } from '../../lib/kinds'
import { formatTimestamp, tzOffset } from '../../lib/format'
import { useResourceDetail } from '../../hooks/useResourceDetail'

const STATS_POLL_MS = 5000

// Live overview of a minorVersionUpgrade SGDbOps, ported from the Vue SGDbOps
// view: the upgrade log timeline and one card per initial instance with a
// real-time connections sparkline fed by the 5s cluster-stats poll.
export default function MinorVersionUpgradeOverview({ dbop }) {
  const { namespace } = useParams()
  const { timezone } = useSettings()
  const status = activeCondition(dbop.status?.conditions)
  const running = status === 'Running' || status === 'Completed'
  const stats = useResourceDetail(
    'sgclusters',
    namespace,
    dbop.spec.sgCluster,
    'stats',
    '',
    running ? STATS_POLL_MS : 0
  )
  const connectionsRef = useRef({})
  const [, setTick] = useState(0)

  useEffect(() => {
    if (!stats) {
      return
    }
    ;(stats.pods || []).forEach((pod) => {
      if (pod.connections === undefined) {
        return
      }
      const history = connectionsRef.current[pod.name] || []
      connectionsRef.current[pod.name] = [...history.slice(-60), pod.connections]
    })
    setTick((previous) => previous + 1)
  }, [stats])

  if (dbop.spec.op !== 'minorVersionUpgrade' || !dbop.status?.minorVersionUpgrade) {
    return null
  }

  const stamp = (value) =>
    value
      ? `${formatTimestamp(value, 'date', timezone)} ${formatTimestamp(value, 'time', timezone)} ${tzOffset(timezone)}`
      : ''

  const upgrade = dbop.status.minorVersionUpgrade
  const timeline = [
    { label: 'Operation started', time: dbop.status.opStarted },
    { label: 'Switchover initiated', time: upgrade.switchoverInitiated },
    { label: 'Switchover finalized', time: upgrade.switchoverFinalized },
    ...(dbop.status.conditions || [])
      .filter((condition) => condition.status === 'True')
      .map((condition) => ({
        label: `The operation has ${condition.type}`,
        time: condition.lastTransitionTime,
      })),
  ].filter((entry) => entry.time)

  const statsPods = stats?.pods || []
  const pods = (upgrade.initialInstances || []).map(
    (name) => statsPods.find((pod) => pod.name === name) || { name }
  )

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h3 className="mb-2 font-semibold uppercase">Upgrade Log</h3>
        <table className="w-full border-collapse text-left text-sm">
          <tbody>
            {timeline.map((entry) => (
              <tr key={entry.label}>
                <td className="border-b p-2">{stamp(entry.time)}</td>
                <td className="border-b p-2">{entry.label}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div>
        <h3 className="mb-2 font-semibold uppercase">Upgrade Overview</h3>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          {pods.map((pod) => (
            <article key={pod.name} className="flex flex-col gap-1 border p-3 text-sm">
              <strong>{pod.name}</strong>
              <span>{pod.role === 'primary' ? 'Primary' : pod.role ? 'Replica' : ''}</span>
              {pod.componentVersions?.postgresql && (
                <span>Postgres {pod.componentVersions.postgresql}</span>
              )}
              {pod.connections !== undefined && (
                <div title="Real Time Connections">
                  <span>Connections: {pod.connections}</span>
                  <Chart
                    type="line"
                    height={45}
                    options={{
                      chart: { id: `${pod.name}-connections`, sparkline: { enabled: true } },
                      stroke: { width: 2 },
                      tooltip: { enabled: false },
                      colors: pod.role === 'primary' ? ['#36A8FF'] : ['#5db4be'],
                    }}
                    series={[
                      { name: `${pod.name}-connections`, data: connectionsRef.current[pod.name] || [] },
                    ]}
                  />
                </div>
              )}
              {pod.status && <span>Status: {pod.status}</span>}
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}
