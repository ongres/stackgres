import { describe, expect, it } from 'vitest'
import { extractReportText, parseCompassReport } from './babelfish'

const REPORT = [
  'Babelfish Compass v.2023-12 : compass',
  'Report name : myreport',
  'Session log : /home/babelfish/session/log.txt',
  'Input files : demo.sql',
  '================================================================================',
  '--------------------------------------------------------------------------------',
  'Summary of SQL features',
  'Compatibility estimate: 98%',
  '--- Run Metrics ---',
  'Run time : 2 seconds',
  'SQL lines processed : 120',
  '================================================================================',
  'BabelfishFeatures.cfg file : v.4.2.0',
].join('\n')

describe('parseCompassReport', () => {
  it('splits setup, report body and metrics, skipping internal paths', () => {
    const parsed = parseCompassReport(REPORT)
    expect(parsed.setup).toEqual({
      'Babelfish Compass v.2023-12': 'compass',
      'Report name': 'myreport',
      'Input files': 'demo.sql',
    })
    expect(parsed.report).toContain('Summary of SQL features')
    expect(parsed.report).not.toContain('Run Metrics')
    expect(parsed.metrics).toEqual({
      'Run time': '2 seconds',
      'SQL lines processed': '120',
    })
  })

  it('extracts the pre body from the HTML page', () => {
    expect(extractReportText('<html><body><pre>abc</pre></body></html>')).toBe('abc')
    expect(extractReportText('plain')).toBe('plain')
  })
})
