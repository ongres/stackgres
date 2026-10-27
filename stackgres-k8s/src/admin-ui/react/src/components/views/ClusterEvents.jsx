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
    return <p className="p-8 text-sm">Loading events…</p>
  }

  const sorted = [...events].sort(
    (a, b) => new Date(b.firstTimestamp) - new Date(a.firstTimestamp)
  )

  if (uid) {
    const event = sorted.find((candidate) => candidate.metadata.uid === uid)
    return (
      <section className="flex flex-col gap-4 p-8 text-sm">
        <h2 className="text-xl font-semibold">Event Details</h2>
        <Link to={`/${namespace}/sgcluster/${name}/events`} className="underline">
          Close Details
        </Link>
        {event ? <SpecTree value={event} /> : <p>Event not found.</p>}
      </section>
    )
  }

  const showInvolvedObjects = sorted.some(
    (event) => event.involvedObject && event.involvedObject.kind !== 'SGCluster'
  )

  return (
    <section className="flex flex-col gap-4 p-8">
      <h2 className="text-xl font-semibold">Events</h2>
      {sorted.length ? (
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr>
              <th className="border-b p-2">First Timestamp</th>
              <th className="border-b p-2">Last Timestamp</th>
              {showInvolvedObjects && <th className="border-b p-2">Component</th>}
              <th className="border-b p-2">Message</th>
            </tr>
          </thead>
          <tbody>
            {sorted.map((event) => (
              <tr key={event.metadata.uid}>
                <td className="border-b p-2">
                  <Link to={`/${namespace}/sgcluster/${name}/event/${event.metadata.uid}`}>
                    <Timestamp value={event.firstTimestamp} />
                  </Link>
                </td>
                <td className="border-b p-2">
                  <Timestamp value={event.lastTimestamp} />
                </td>
                {showInvolvedObjects && (
                  <td className="border-b p-2">
                    {event.involvedObject
                      ? `${event.involvedObject.kind}/${event.involvedObject.name}`
                      : ''}
                  </td>
                )}
                <td className="border-b p-2">{event.message}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p className="text-sm">No events have been found</p>
      )}
    </section>
  )
}
