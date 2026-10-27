import { useParams } from 'react-router-dom'
import { useSettings } from '../../contexts/SettingsContext'
import { formatTimestamp, tzOffset } from '../../lib/format'
import { useResourceDetail } from '../../hooks/useResourceDetail'

const POLL_MS = 10000

export default function DbOpsEvents() {
  const { namespace, name } = useParams()
  const { timezone } = useSettings()
  const events = useResourceDetail('sgdbops', namespace, name, 'events', '', POLL_MS)

  const stamp = (value) =>
    value
      ? `${formatTimestamp(value, 'date', timezone)} ${formatTimestamp(value, 'time', timezone)} ${tzOffset(timezone)}`
      : ''

  return (
    <section>
      <h2 className="mb-2 font-semibold uppercase">Operation Events</h2>
      {events && events.length ? (
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr>
              <th className="border-b p-2">First Timestamp</th>
              <th className="border-b p-2">Last Timestamp</th>
              <th className="border-b p-2">Component</th>
              <th className="border-b p-2">Message</th>
            </tr>
          </thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.metadata.uid}>
                <td className="border-b p-2">{stamp(event.firstTimestamp)}</td>
                <td className="border-b p-2">{stamp(event.lastTimestamp)}</td>
                <td className="border-b p-2">
                  {event.involvedObject
                    ? `${event.involvedObject.kind}/${event.involvedObject.name}`
                    : ''}
                </td>
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
