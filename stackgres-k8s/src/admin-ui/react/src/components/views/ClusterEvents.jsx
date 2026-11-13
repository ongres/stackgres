import { Link, useParams } from 'react-router-dom'
import { useSettings } from '../../contexts/SettingsContext'
import { formatTimestamp, tzOffset } from '../../lib/format'
import { useResourceDetail } from '../../hooks/useResourceDetail'
import SpecTree from '../SpecTree'

const POLL_MS = 5000

function Timestamp({ value }) {
  const { timezone } = useSettings()
  if (!value) {
    return null
  }
  return (
    <span>
      {formatTimestamp(value, 'date', timezone)} {formatTimestamp(value, 'time', timezone)}{' '}
      {tzOffset(timezone)}
    </span>
  )
}

export default function ClusterEvents() {
  const { namespace, name, uid } = useParams()
  const events = useResourceDetail('sgclusters', namespace, name, 'events', '', POLL_MS)

  if (events === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading events…</p>
  }

  const sorted = [...events].sort(
    (a, b) => new Date(b.firstTimestamp) - new Date(a.firstTimestamp)
  )

  if (uid) {
    const event = sorted.find((candidate) => candidate.metadata.uid === uid)
    return (
      <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
        <h2 className="text-lg font-semibold">Event Details</h2>
        <Link to={`/${namespace}/sgcluster/${name}/events`} className="text-[hsl(var(--primary))] hover:underline">
          Close Details
        </Link>
        {event ? <SpecTree value={event} /> : <p className="text-sm text-[hsl(var(--muted-foreground))] italic">Event not found.</p>}
      </section>
    )
  }

  const showInvolvedObjects = sorted.some(
    (event) => event.involvedObject && event.involvedObject.kind !== 'SGCluster'
  )

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h2 className="text-lg font-semibold">Events</h2>
      {sorted.length ? (
        <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">First Timestamp</th>
                <th className="px-4 py-2">Last Timestamp</th>
                {showInvolvedObjects && <th className="px-4 py-2">Component</th>}
                <th className="px-4 py-2">Message</th>
              </tr>
            </thead>
            <tbody>
              {sorted.map((event) => (
                <tr key={event.metadata.uid} className="border-t">
                  <td className="px-4 py-2">
                    <Link to={`/${namespace}/sgcluster/${name}/event/${event.metadata.uid}`}>
                      <Timestamp value={event.firstTimestamp} />
                    </Link>
                  </td>
                  <td className="px-4 py-2">
                    <Timestamp value={event.lastTimestamp} />
                  </td>
                  {showInvolvedObjects && (
                    <td className="px-4 py-2">
                      {event.involvedObject
                        ? `${event.involvedObject.kind}/${event.involvedObject.name}`
                        : ''}
                    </td>
                  )}
                  <td className="px-4 py-2">{event.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-[hsl(var(--muted-foreground))] italic">No events have been found</p>
      )}
    </section>
  )
}
