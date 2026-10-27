import { describe, expect, it } from 'vitest'
import {
  cpuToMillis,
  formatBytes,
  formatDuration,
  formatTimestamp,
  prefix,
  quantityToBytes,
  tzOffset,
} from './format'

describe('formatTimestamp', () => {
  const iso = '2026-10-13T09:59:19.123Z'

  it('splits date, time and ms in UTC mode', () => {
    expect(formatTimestamp(iso, 'date', 'utc')).toBe('2026-10-13')
    expect(formatTimestamp(iso, 'time', 'utc')).toBe('09:59:19')
    expect(formatTimestamp(iso, 'ms', 'utc')).toBe('.123')
  })

  it('returns empty for missing or invalid input', () => {
    expect(formatTimestamp('', 'date')).toBe('')
    expect(formatTimestamp('not-a-date', 'time')).toBe('')
  })

  it('reports +00:00 as the UTC offset', () => {
    expect(tzOffset('utc')).toBe('+00:00')
    expect(tzOffset('local')).toMatch(/^[+-]\d{2}:\d{2}$/)
  })
})

describe('quantities', () => {
  it('formats byte sizes', () => {
    expect(formatBytes(0)).toBe('0 B')
    expect(formatBytes(1536)).toBe('1.5 KiB')
    expect(formatBytes(3 * 1024 ** 3)).toBe('3 GiB')
    expect(formatBytes('oops')).toBe('')
  })

  it('parses kubernetes memory quantities for sorting', () => {
    expect(quantityToBytes('512Mi')).toBe(512 * 1024 ** 2)
    expect(quantityToBytes('1.5Gi')).toBe(1.5 * 1024 ** 3)
    expect(quantityToBytes('2048')).toBe(2048)
    expect(quantityToBytes(undefined)).toBe(-1)
  })

  it('parses kubernetes cpu quantities for sorting', () => {
    expect(cpuToMillis('500m')).toBe(500)
    expect(cpuToMillis('2')).toBe(2000)
    expect(cpuToMillis('')).toBe(-1)
  })
})

describe('misc', () => {
  it('prefixes text', () => {
    expect(prefix('16.4')).toBe('16')
    expect(prefix(undefined)).toBe('')
  })

  it('formats durations', () => {
    expect(formatDuration(3723000)).toBe('01:02:03')
    expect(formatDuration(-1)).toBe('')
  })
})
