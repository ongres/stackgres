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
      <h2 className="mb-2 text-xs font-semibold uppercase tracking-wider text-[hsl(var(--muted-foreground))]">Operation Events</h2>
      {events && events.length ? (
        <div className="rounded border overflow-auto">
          <table className="w-full table-auto text-left">
            <thead className="bg-[hsl(var(--muted))] text-left">
              <tr>
                <th className="px-4 py-2">First Timestamp</th>
                <th className="px-4 py-2">Last Timestamp</th>
                <th className="px-4 py-2">Component</th>
                <th className="px-4 py-2">Message</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event) => (
                <tr key={event.metadata.uid} className="border-t">
                  <td className="px-4 py-2">{stamp(event.firstTimestamp)}</td>
                  <td className="px-4 py-2">{stamp(event.lastTimestamp)}</td>
                  <td className="px-4 py-2">
                    {event.involvedObject
                      ? `${event.involvedObject.kind}/${event.involvedObject.name}`
                      : ''}
                  </td>
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
