import { describe, expect, it } from 'vitest'
import { dateStamp, generatedName, isValidName, nameValidationMessage } from './names'
import { compact } from '../hooks/useCrdForm'

describe('isValidName', () => {
  it('accepts RFC 1123 subdomain names', () => {
    expect(isValidName('my-cluster-01')).toBe(true)
    expect(isValidName('a')).toBe(true)
  })

  it('rejects invalid names', () => {
    expect(isValidName('')).toBe(false)
    expect(isValidName('My-Cluster')).toBe(false)
    expect(isValidName('-starts-with-dash')).toBe(false)
    expect(isValidName('ends-with-dash-')).toBe(false)
    expect(isValidName('has spaces')).toBe(false)
    expect(isValidName('op2026-10-13-9시')).toBe(false)
    expect(isValidName('a'.repeat(254))).toBe(false)
  })

  it('produces a message only for invalid names', () => {
    expect(nameValidationMessage('ok-name')).toBe('')
    expect(nameValidationMessage('')).toMatch(/required/)
    expect(nameValidationMessage('Bad Name')).toMatch(/lowercase/)
  })
})

describe('dateStamp', () => {
  it('is locale-independent and RFC 1123-safe', () => {
    const fixed = new Date('2026-10-13T09:59:19Z')
    expect(dateStamp('utc', fixed)).toBe('2026-10-13-09-59-19')
    expect(isValidName(generatedName('op', 'utc'))).toBe(true)
    expect(generatedName('bk', 'utc')).toMatch(/^bk\d{4}(-\d{2}){5}$/)
  })
})

describe('compact', () => {
  it('drops empty strings, empty objects and empty arrays recursively', () => {
    expect(
      compact({
        keep: 'x',
        zero: 0,
        no: false,
        empty: '',
        nested: { inner: '', deeper: {} },
        list: [],
        full: { a: 1 },
      })
    ).toEqual({ keep: 'x', zero: 0, no: false, full: { a: 1 } })
  })

  it('keeps array entries and compacts inside them', () => {
    expect(compact({ scripts: [{ name: 's1', script: 'SELECT 1;', note: '' }] })).toEqual({
      scripts: [{ name: 's1', script: 'SELECT 1;' }],
    })
  })
})
