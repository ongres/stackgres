import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api } from '../../lib/api'
import { useSettings } from '../../contexts/SettingsContext'
import { formatTimestamp, tzOffset } from '../../lib/format'

const ERROR_LEVELS = [
  'PANIC',
  'CRITICAL',
  'FATAL',
  'LOG',
  'ERROR',
  'WARNING',
  'NOTICE',
  'INFO',
  'DEBUG',
]
const LOG_TYPES = { pg: 'Postgres', pa: 'Patroni' }
const RECORDS = 50
const LIVE_POLL_MS = 3000

const DETAIL_FIELDS = [
  ['Message', 'message'],
  ['Type', 'logType'],
  ['Pod Name', 'podName'],
  ['Role', 'role'],
  ['User', 'userName'],
  ['Database', 'databaseName'],
  ['Process ID', 'processId'],
  ['Connection From', 'connectionFrom'],
  ['Error Level', 'errorLevel'],
  ['Application Name', 'applicationName'],
]

export default function ClusterLogs() {
  const { namespace, name } = useParams()
  const { timezone } = useSettings()
  const [logs, setLogs] = useState(null)
  const [text, setText] = useState('')
  const [logType, setLogType] = useState('')
  const [errorLevel, setErrorLevel] = useState('')
  const [live, setLive] = useState(true)
  const [expanded, setExpanded] = useState(null)
  const cursorRef = useRef(null)

  const buildQuery = useCallback(
    (fromCursor) => {
      const params = new URLSearchParams()
      params.set('sort', fromCursor ? 'asc' : 'desc')
      params.set('records', String(RECORDS))
      if (logType) params.set('logType', logType)
      if (errorLevel) params.set('errorLevel', errorLevel)
      if (text) params.set('text', text)
      if (fromCursor) params.set('from', fromCursor)
      return `?${params.toString()}`
    },
    [logType, errorLevel, text]
  )

  const load = useCallback(
    async (append) => {
      const fromCursor = append && cursorRef.current ? cursorRef.current : null
      try {
        const entries = await api.getResourceDetails(
          'sgclusters',
          namespace,
          name,
          'logs',
          buildQuery(fromCursor)
        )
        const ordered = fromCursor ? entries : [...entries].reverse()
        if (ordered.length) {
          const last = ordered[ordered.length - 1]
          cursorRef.current = `${last.logTime},${last.logTimeIndex}`
        }
        setLogs((previous) => (append && previous ? [...previous, ...ordered] : ordered))
      } catch {
        setLogs((previous) => previous ?? [])
      }
    },
    [namespace, name, buildQuery]
  )

  useEffect(() => {
    cursorRef.current = null
    setLogs(null)
    load(false)
  }, [load])

  useEffect(() => {
    if (!live) {
      return
    }
    const interval = setInterval(() => load(true), LIVE_POLL_MS)
    return () => clearInterval(interval)
  }, [live, load])

  return (
    <section className="flex flex-col gap-4 p-8">
      <div className="flex flex-wrap items-center gap-3 text-sm">
        <h2 className="text-xl font-semibold">Logs</h2>
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Search…"
          aria-label="Search logs"
          className="border p-2"
        />
        <label className="flex items-center gap-1">
          Type
          <select value={logType} onChange={(event) => setLogType(event.target.value)} className="border p-1">
            <option value="">All</option>
            {Object.entries(LOG_TYPES).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-1">
          Error Level
          <select
            value={errorLevel}
            onChange={(event) => setErrorLevel(event.target.value)}
            className="border p-1"
          >
            <option value="">All</option>
            {ERROR_LEVELS.map((level) => (
              <option key={level}>{level}</option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-1">
          <input type="checkbox" checked={live} onChange={() => setLive(!live)} />
          Live
        </label>
      </div>
      {logs === null ? (
        <p className="text-sm">Loading logs…</p>
      ) : logs.length ? (
        <ul className="flex flex-col font-mono text-xs">
          {logs.map((entry, index) => (
            <li key={`${entry.logTime}-${entry.logTimeIndex}-${index}`} className="border-b">
              <button
                type="button"
                onClick={() => setExpanded(expanded === index ? null : index)}
                className="flex w-full items-baseline gap-2 p-1 text-left"
              >
                <span data-status={entry.errorLevel}>{entry.errorLevel}</span>
                <span>
                  {formatTimestamp(entry.logTime, 'date', timezone)}{' '}
                  {formatTimestamp(entry.logTime, 'time', timezone)}
                  {formatTimestamp(entry.logTime, 'ms', timezone)} {tzOffset(timezone)}
                </span>
                <span>{entry.podName}</span>
                <span className="flex-1 truncate">{entry.message}</span>
              </button>
              {expanded === index && (
                <ul className="flex flex-col gap-1 p-2">
                  {DETAIL_FIELDS.map(([label, field]) =>
                    entry[field] ? (
                      <li key={field}>
                        <strong>{label}:</strong> {entry[field]}
                      </li>
                    ) : null
                  )}
                </ul>
              )}
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm">No logs match your search terms</p>
      )}
      {logs && logs.length >= RECORDS && (
        <button type="button" onClick={() => load(true)} className="self-start border p-2 text-sm">
          Load more
        </button>
      )}
    </section>
  )
}
