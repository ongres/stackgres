import { describe, expect, it } from 'vitest'
import { fromDateTimeLocal, toDateTimeLocal } from './datetime'

describe('datetime-local conversions', () => {
  it('formats and parses UTC-frame values verbatim', () => {
    expect(toDateTimeLocal('2026-05-31T02:04:30Z', 'utc')).toBe('2026-05-31T02:04:30')
    expect(fromDateTimeLocal('2026-05-31T02:04:30', 'utc')).toBe('2026-05-31T02:04:30Z')
    expect(fromDateTimeLocal('2026-05-31T02:04', 'utc')).toBe('2026-05-31T02:04:00Z')
  })

  it('round-trips local-frame values back to the same UTC timestamp', () => {
    const timestamp = '2026-05-31T02:04:30Z'
    const shown = toDateTimeLocal(timestamp, 'local')
    expect(fromDateTimeLocal(shown, 'local')).toBe(timestamp)
  })

  it('returns empty strings for empty or invalid input', () => {
    expect(toDateTimeLocal('', 'utc')).toBe('')
    expect(toDateTimeLocal('not-a-date', 'local')).toBe('')
    expect(fromDateTimeLocal('', 'local')).toBe('')
  })
})
