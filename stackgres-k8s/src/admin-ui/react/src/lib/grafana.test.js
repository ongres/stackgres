import { describe, expect, it } from 'vitest'
import { defaultDashboard, grafanaIframeSrc, TIME_RANGES } from './grafana'
import { decodeDeltaValues, statementsCategories, statementsLatencies } from './benchmark'

describe('grafanaIframeSrc', () => {
  const base = { dashboardUrl: '/d/abc/db-info', theme: 'dark', podIp: '10.0.0.5', podName: 'c-0' }

  it('builds the embed URL with theme, kiosk and template vars', () => {
    expect(grafanaIframeSrc(base)).toBe(
      '/d/abc/db-info?theme=dark&kiosk&var-instance=10.0.0.5&var-pod=c-0'
    )
  })

  it('appends with & when the dashboard URL already has a query', () => {
    expect(grafanaIframeSrc({ ...base, dashboardUrl: '/d/abc/db-info?orgId=1' })).toContain(
      '?orgId=1&theme=dark&kiosk&'
    )
  })

  it('appends the pre-encoded range fragment for a known range only', () => {
    expect(grafanaIframeSrc({ ...base, range: 'last-1-hour' })).toContain('&from=now-1h&to=now')
    expect(grafanaIframeSrc({ ...base, range: 'yesterday' })).toContain('&from=now-1d%2Fd')
    expect(grafanaIframeSrc({ ...base, range: 'nope' })).not.toContain('&from=')
  })

  it('returns empty without a dashboard URL', () => {
    expect(grafanaIframeSrc({ ...base, dashboardUrl: '' })).toBe('')
  })

  it('prefers the db-info dashboard as default', () => {
    const dashboards = [{ name: 'queries' }, { name: 'db-info' }]
    expect(defaultDashboard(dashboards).name).toBe('db-info')
    expect(defaultDashboard([{ name: 'queries' }]).name).toBe('queries')
  })

  it('keeps every time range pre-encoded', () => {
    Object.values(TIME_RANGES).forEach(({ range }) => {
      expect(range.startsWith('&from=')).toBe(true)
      expect(range).not.toContain('/')
    })
  })
})

describe('benchmark transforms', () => {
  it('decodes delta-encoded TPS values with a prefix sum', () => {
    expect(decodeDeltaValues([100, 5, -3, 10])).toEqual([100, 105, 102, 112])
    expect(decodeDeltaValues([])).toEqual([])
    expect(decodeDeltaValues(undefined)).toEqual([])
  })

  it('maps statements to categories and latencies', () => {
    const statements = [
      { command: 'BEGIN;', latency: 1.2 },
      { command: 'UPDATE accounts', latency: 3.4 },
    ]
    expect(statementsCategories(statements)).toEqual(['BEGIN;', 'UPDATE accounts'])
    expect(statementsLatencies(statements)).toEqual([1.2, 3.4])
  })
})
