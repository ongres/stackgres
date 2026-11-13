import { describe, expect, it } from 'vitest'
import { omitDefaults } from './diff'

const DEFAULTS = {
  metadata: { name: '', namespace: 'default' },
  spec: {
    instances: 1,
    profile: 'development',
    pods: { persistentVolume: { size: '1Gi' }, disableConnectionPooling: true },
    postgres: { version: 'latest', flavor: 'vanilla', ssl: { enabled: false } },
    replication: { role: 'ha-read', mode: 'async' },
  },
}

describe('omitDefaults', () => {
  it('keeps metadata and drops subtrees equal to the defaults', () => {
    const value = JSON.parse(JSON.stringify(DEFAULTS))
    value.metadata.name = 'test'
    value.spec.instances = 3
    value.spec.pods.persistentVolume.size = '10Gi'
    expect(omitDefaults(value, DEFAULTS)).toEqual({
      metadata: { name: 'test', namespace: 'default' },
      spec: {
        instances: 3,
        pods: { persistentVolume: { size: '10Gi' } },
      },
    })
  })

  it('keeps keys missing from the defaults and differing arrays', () => {
    const value = JSON.parse(JSON.stringify(DEFAULTS))
    value.spec.managedSql = { scripts: [{ sgScript: 'init' }] }
    value.spec.postgres.extensions = [{ name: 'ltree' }]
    const result = omitDefaults(value, DEFAULTS)
    expect(result.spec.managedSql).toEqual({ scripts: [{ sgScript: 'init' }] })
    expect(result.spec.postgres).toEqual({ extensions: [{ name: 'ltree' }] })
  })

  it('returns everything when there are no defaults to compare', () => {
    expect(omitDefaults(DEFAULTS, null)).toEqual(DEFAULTS)
  })
})
