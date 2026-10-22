import { describe, expect, it } from 'vitest'
import { cn } from './utils'

describe('cn', () => {
  it('joins conditional class names', () => {
    const condition = Boolean(0)
    expect(cn('a', condition && 'b', 'c')).toBe('a c')
  })

  it('resolves conflicting tailwind utilities keeping the last one', () => {
    expect(cn('p-2', 'p-4')).toBe('p-4')
  })
})
