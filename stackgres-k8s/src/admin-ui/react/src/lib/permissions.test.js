import { describe, expect, it } from 'vitest'
import { canI } from './permissions'

const allowed = {
  namespaced: [
    { namespace: 'default', resources: { sgclusters: ['list', 'get', 'create'] } },
    { namespace: 'demo', resources: { sgclusters: ['list'] } },
  ],
  unnamespaced: {
    namespaces: ['list', 'get', 'create'],
    storageclasses: ['list'],
  },
}

describe('canI', () => {
  it('checks a verb in a concrete namespace', () => {
    expect(canI(allowed, 'create', 'sgclusters', 'default')).toBe(true)
    expect(canI(allowed, 'create', 'sgclusters', 'demo')).toBe(false)
  })

  it('treats action "any" as any verb present', () => {
    expect(canI(allowed, 'any', 'sgclusters', 'demo')).toBe(true)
    expect(canI(allowed, 'any', 'sgbackups', 'demo')).toBe(false)
  })

  it('requires every namespace with "all" and at least one with "any"', () => {
    expect(canI(allowed, 'list', 'sgclusters', 'all')).toBe(true)
    expect(canI(allowed, 'create', 'sgclusters', 'all')).toBe(false)
    expect(canI(allowed, 'create', 'sgclusters', 'any')).toBe(true)
  })

  it('checks unnamespaced kinds when no namespace is given', () => {
    expect(canI(allowed, 'create', 'namespaces')).toBe(true)
    expect(canI(allowed, 'create', 'storageclasses')).toBe(false)
  })

  it('denies kinds missing from the permissions map instead of throwing', () => {
    expect(canI(allowed, 'list', 'unknownkind', 'default')).toBe(false)
    expect(canI(allowed, 'list', 'unknownkind', 'nosuchnamespace')).toBe(false)
    expect(canI(allowed, 'list', 'unknownkind')).toBe(false)
    expect(canI({}, 'list', 'sgclusters', 'all')).toBe(false)
  })
})
