import { getCookie, setCookie, clearCookie } from './cookies'

const BASE = '/stackgres'
const TOKEN_COOKIE = 'sgToken'

// Sentinel kept from the Vue console: with OIDC the session lives in a
// server-managed cookie and no Authorization header must be sent.
export const OIDC_TOKEN = 'OIDC'

export class ApiError extends Error {
  constructor(status, message, detail) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
  }
}

export function getToken() {
  return getCookie(TOKEN_COOKIE)
}

export function setToken(token) {
  setCookie(TOKEN_COOKIE, token)
}

export function clearToken() {
  clearCookie(TOKEN_COOKIE)
}

// Session-level handlers, registered by the auth/permissions providers so the
// client stays free of UI concerns: 401 ends the session, 403 records the
// kind the user cannot access.
const handlers = {
  onUnauthorized: null,
  onForbidden: null,
}

export function onUnauthorized(callback) {
  handlers.onUnauthorized = callback
}

export function onForbidden(callback) {
  handlers.onForbidden = callback
}

async function parseBody(response) {
  const text = await response.text()
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

async function request(path, { method = 'GET', body, kind } = {}) {
  const headers = {}
  const token = getToken()
  if (token && token !== OIDC_TOKEN) {
    headers['Authorization'] = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  const response = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (!response.ok) {
    const payload = await parseBody(response)
    if (response.status === 401 && handlers.onUnauthorized) {
      handlers.onUnauthorized()
    }
    if (response.status === 403 && kind && handlers.onForbidden) {
      handlers.onForbidden(kind)
    }
    const message =
      (payload && payload.message) || response.statusText || `Request failed (${response.status})`
    const detail = (payload && payload.detail) || null
    throw new ApiError(response.status, message, detail)
  }

  if (response.status === 204) {
    return null
  }
  return parseBody(response)
}

// Resource name → API path, as in the Vue console's sgApi module.
const RESOURCES = {
  auth_type: '/auth/type',
  can_i: '/auth/rbac/can-i',
  login: '/auth/login',
  logout: '/auth/logout',
  namespaces: '/namespaces',
  sgclusters: '/sgclusters',
  sgshardedclusters: '/sgshardedclusters',
  sgstreams: '/sgstreams',
  sginstanceprofiles: '/sginstanceprofiles',
  sgbackups: '/sgbackups',
  sgpgconfigs: '/sgpgconfigs',
  sgpoolconfigs: '/sgpoolconfigs',
  sgobjectstorages: '/sgobjectstorages',
  sgscripts: '/sgscripts',
  sgdistributedlogs: '/sgdistributedlogs',
  sgdbops: '/sgdbops',
  storageclasses: '/storageclasses',
  applications: '/applications',
  sgconfigs: '/sgconfigs',
  users: '/users',
  roles: '/roles',
  clusterroles: '/clusterroles',
}

function resourcePath(resource) {
  const path = RESOURCES[resource]
  if (!path) {
    throw new Error(`Unknown API resource "${resource}"`)
  }
  return path
}

const dryRunQuery = (dryRun) => (dryRun ? '?dryRun=true' : '')

export const api = {
  get(resource) {
    return request(resourcePath(resource), { kind: resource })
  },
  create(resource, data, dryRun = false) {
    return request(`${resourcePath(resource)}${dryRunQuery(dryRun)}`, {
      method: 'POST',
      body: data,
      kind: resource,
    })
  },
  update(resource, data, dryRun = false) {
    return request(`${resourcePath(resource)}${dryRunQuery(dryRun)}`, {
      method: 'PUT',
      body: data,
      kind: resource,
    })
  },
  remove(resource, data, dryRun = false) {
    return request(`${resourcePath(resource)}${dryRunQuery(dryRun)}`, {
      method: 'DELETE',
      body: data,
      kind: resource,
    })
  },
  getResourceDetails(resource, namespace, name, details = '', query = '') {
    const tail = details ? `/${details}${query}` : ''
    return request(`/namespaces/${namespace}/${resource}/${name}${tail}`, { kind: resource })
  },
  getPostgresVersions(flavor) {
    return request(`/version/postgresql?flavor=${flavor}`)
  },
  getPostgresExtensions(version, flavor) {
    return request(`/extensions/${version}?flavor=${flavor}`)
  },
}
