import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, clearToken, onForbidden, onUnauthorized, setToken } from './api'

function mockFetch(status, body) {
  return vi.fn(() =>
    Promise.resolve({
      ok: status >= 200 && status < 300,
      status,
      statusText: 'status text',
      text: () => Promise.resolve(typeof body === 'string' ? body : JSON.stringify(body)),
    })
  )
}

describe('api client', () => {
  beforeEach(() => {
    clearToken()
  })

  afterEach(() => {
    clearToken()
    onUnauthorized(null)
    onForbidden(null)
    vi.unstubAllGlobals()
  })

  it('prefixes paths with /stackgres and parses JSON', async () => {
    const fetchMock = mockFetch(200, ['default'])
    vi.stubGlobal('fetch', fetchMock)

    const data = await api.get('namespaces')

    expect(fetchMock).toHaveBeenCalledWith('/stackgres/namespaces', expect.anything())
    expect(data).toEqual(['default'])
  })

  it('sends the bearer token except for the OIDC sentinel', async () => {
    const fetchMock = mockFetch(200, [])
    vi.stubGlobal('fetch', fetchMock)

    setToken('my-token')
    await api.get('namespaces')
    expect(fetchMock.mock.calls[0][1].headers['Authorization']).toBe('Bearer my-token')

    setToken('OIDC')
    await api.get('namespaces')
    expect(fetchMock.mock.calls[1][1].headers['Authorization']).toBeUndefined()
  })

  it('appends dryRun to writes and serializes the payload', async () => {
    const fetchMock = mockFetch(200, {})
    vi.stubGlobal('fetch', fetchMock)

    await api.create('sgclusters', { metadata: { name: 'c1' } }, true)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/stackgres/sgclusters?dryRun=true')
    expect(options.method).toBe('POST')
    expect(JSON.parse(options.body)).toEqual({ metadata: { name: 'c1' } })
    expect(options.headers['Content-Type']).toBe('application/json')
  })

  it('builds resource detail paths', async () => {
    const fetchMock = mockFetch(200, {})
    vi.stubGlobal('fetch', fetchMock)

    await api.getResourceDetails('sgclusters', 'default', 'pgsql', 'stats', '?range=1h')

    expect(fetchMock.mock.calls[0][0]).toBe('/stackgres/namespaces/default/sgclusters/pgsql/stats?range=1h')
  })

  it('normalizes errors with status, message and detail', async () => {
    vi.stubGlobal('fetch', mockFetch(400, { message: 'bad request', detail: 'name is invalid' }))

    const error = await api.get('namespaces').catch((caught) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('bad request')
    expect(error.detail).toBe('name is invalid')
  })

  it('notifies the unauthorized handler on 401', async () => {
    vi.stubGlobal('fetch', mockFetch(401, ''))
    const unauthorized = vi.fn()
    onUnauthorized(unauthorized)

    await api.get('namespaces').catch(() => {})

    expect(unauthorized).toHaveBeenCalled()
  })

  it('notifies the forbidden handler with the resource kind on 403', async () => {
    vi.stubGlobal('fetch', mockFetch(403, ''))
    const forbidden = vi.fn()
    onForbidden(forbidden)

    await api.get('sgbackups').catch(() => {})

    expect(forbidden).toHaveBeenCalledWith('sgbackups')
  })
})
