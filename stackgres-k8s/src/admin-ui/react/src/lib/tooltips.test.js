import { describe, expect, it } from 'vitest'
import { getTooltip, indexTooltips } from './tooltips'

const SCHEMAS = {
  ClusterDto: {
    type: 'object',
    properties: {
      spec: {
        type: 'object',
        description: 'The cluster spec',
        properties: {
          instances: { type: 'integer', description: 'Number of instances' },
          configurations: {
            type: 'object',
            properties: {
              backups: {
                type: 'array',
                items: {
                  type: 'object',
                  properties: {
                    cronSchedule: { type: 'string', description: 'Backup schedule' },
                    performance: {
                      type: 'object',
                      properties: {
                        maxNetworkBandwidth: { type: 'integer', description: 'Network cap' },
                      },
                    },
                  },
                },
              },
            },
          },
        },
      },
    },
  },
  ProfileDto: {
    type: 'object',
    properties: {
      spec: {
        type: 'object',
        properties: {
          hugePages: {
            type: 'object',
            properties: {
              'hugepages-2Mi': { type: 'string', description: '2Mi huge pages' },
            },
          },
        },
      },
    },
  },
}

describe('tooltips', () => {
  const tooltips = indexTooltips(JSON.parse(JSON.stringify(SCHEMAS)))

  it('renames schemas and resolves flattened object paths', () => {
    expect(getTooltip(tooltips, 'sgcluster.spec.instances')).toBe('Number of instances')
    expect(getTooltip(tooltips, 'sgcluster.spec')).toBe('The cluster spec')
  })

  it('copies array item properties up one level', () => {
    expect(getTooltip(tooltips, 'sgcluster.spec.configurations.backups.cronSchedule')).toBe(
      'Backup schedule'
    )
    // Deeper array children keep the properties wrapper, as in Vue.
    expect(
      getTooltip(
        tooltips,
        'sgcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth'
      )
    ).toBe('Network cap')
  })

  it('aliases sginstanceprofile to the ProfileDto schema and handles dotted keys', () => {
    expect(getTooltip(tooltips, 'sginstanceprofile.spec.hugePages.hugepages-2Mi')).toBe(
      '2Mi huge pages'
    )
  })

  it('falls back when the path or the tooltips are unavailable', () => {
    expect(getTooltip(tooltips, 'sgcluster.spec.nope')).toBe('Information not available')
    expect(getTooltip({}, 'sgcluster.spec.instances')).toBe('Information not available')
    expect(getTooltip({ error: 'x' }, 'sgcluster.spec.instances')).toBe('Information not available')
  })
})
