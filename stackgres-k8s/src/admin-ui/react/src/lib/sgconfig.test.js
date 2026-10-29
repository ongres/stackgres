import { describe, expect, it } from 'vitest'
import { buildSGConfig, normalizeSGConfigSpec } from './sgconfig'

describe('normalizeSGConfigSpec', () => {
  it('turns annotation and selector maps into editable rows', () => {
    const spec = normalizeSGConfigSpec({
      jobs: {
        annotations: { team: 'db' },
        nodeSelector: { disk: 'ssd' },
        serviceAccount: { annotations: { owner: 'ops' } },
      },
      serviceAccount: { annotations: { billing: 'x' } },
    })
    expect(spec.jobs.annotations).toEqual([{ label: 'team', value: 'db' }])
    expect(spec.jobs.nodeSelector).toEqual([{ label: 'disk', value: 'ssd' }])
    expect(spec.jobs.serviceAccount.annotations).toEqual([{ label: 'owner', value: 'ops' }])
    expect(spec.serviceAccount.annotations).toEqual([{ label: 'billing', value: 'x' }])
  })

  it('default-initializes the sections the template reads unguarded', () => {
    const spec = normalizeSGConfigSpec({})
    expect(spec.adminui.resources.requests).toEqual({ cpu: '', memory: '' })
    expect(spec.jobs.resources.limits).toEqual({ cpu: '', memory: '' })
    expect(spec.grafana).toEqual({})
    expect(spec.jobs.tolerations).toEqual([])
  })

  it('converts affinity matchLabels inside preferred and required terms', () => {
    const spec = normalizeSGConfigSpec({
      jobs: {
        affinity: {
          podAffinity: {
            preferredDuringSchedulingIgnoredDuringExecution: [
              { podAffinityTerm: { labelSelector: { matchLabels: { app: 'pg' } } }, weight: 1 },
            ],
            requiredDuringSchedulingIgnoredDuringExecution: [
              { labelSelector: { matchLabels: { tier: 'db' } }, topologyKey: 'zone' },
            ],
          },
        },
      },
    })
    const block = spec.jobs.affinity.podAffinity
    expect(
      block.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector
        .matchLabels
    ).toEqual([{ label: 'app', value: 'pg' }])
    expect(
      block.requiredDuringSchedulingIgnoredDuringExecution[0].labelSelector.matchLabels
    ).toEqual([{ label: 'tier', value: 'db' }])
  })
})

describe('buildSGConfig', () => {
  it('round-trips rows back to maps and preserves metadata and unknown sections', () => {
    const original = {
      jobs: {
        annotations: { team: 'db' },
        serviceAccount: { annotations: { owner: 'ops' } },
      },
      serviceAccount: { annotations: {} },
      operator: { image: { name: 'stackgres/operator' } },
      prometheus: { allowAutobind: true },
    }
    const editable = normalizeSGConfigSpec(original)
    const payload = buildSGConfig(
      editable,
      { name: 'x', namespace: 'stackgres', resourceVersion: '9', uid: 'u' },
      'stackgres-operator'
    )
    expect(payload.metadata).toEqual({
      name: 'stackgres-operator',
      namespace: 'stackgres',
      resourceVersion: '9',
      uid: 'u',
    })
    expect(payload.spec.jobs.annotations).toEqual({ team: 'db' })
    expect(payload.spec.jobs.serviceAccount.annotations).toEqual({ owner: 'ops' })
    expect(payload.spec.serviceAccount.annotations).toEqual({})
    expect(payload.spec.operator).toEqual({ image: { name: 'stackgres/operator' } })
    expect(payload.spec.prometheus).toEqual({ allowAutobind: true })
  })

  it('serializes affinity matchLabels back and defaults namespace', () => {
    const editable = normalizeSGConfigSpec({
      jobs: {
        affinity: {
          podAntiAffinity: {
            requiredDuringSchedulingIgnoredDuringExecution: [
              { labelSelector: { matchLabels: { tier: 'db' } }, topologyKey: 'zone' },
            ],
          },
        },
      },
    })
    const payload = buildSGConfig(editable, null, 'conf')
    expect(payload.metadata.namespace).toBe('stackgres')
    expect(
      payload.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[0]
        .labelSelector.matchLabels
    ).toEqual({ tier: 'db' })
  })

  it('drops empty resource groups instead of sending ram-null artifacts', () => {
    const editable = normalizeSGConfigSpec({ adminui: {}, jobs: {} })
    const payload = buildSGConfig(editable, null, 'conf')
    expect(payload.spec.adminui.resources).toBeUndefined()
    expect(payload.spec.jobs.resources).toBeUndefined()
  })
})
