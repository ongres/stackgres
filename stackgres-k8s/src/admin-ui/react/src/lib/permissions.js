// RBAC check over the /auth/rbac/can-i response:
//   { namespaced: [{ namespace, resources: { kind: [verbs] } }],
//     unnamespaced: { kind: [verbs] } }
//
// namespace semantics (as in the Vue console's iCan):
//   'all'   → every known namespace must allow the verb
//   'any'   → at least one namespace allows it
//   '<ns>'  → that namespace allows it
//   ''      → cluster-scoped kind, checked against unnamespaced
// action 'any' means "any verb at all is allowed on the kind".
export function canI(allowed, action = 'any', kind, namespace = '') {
  const namespaced = (allowed && allowed.namespaced) || []
  const unnamespaced = (allowed && allowed.unnamespaced) || {}

  const verbsIn = (entry) => (entry && entry.resources && entry.resources[kind]) || []
  const allows = (verbs) => (action === 'any' ? verbs.length > 0 : verbs.includes(action))

  if (namespace === 'all') {
    return namespaced.length > 0 && namespaced.every((entry) => allows(verbsIn(entry)))
  }
  if (namespace === 'any') {
    return namespaced.some((entry) => allows(verbsIn(entry)))
  }
  if (namespace) {
    const entry = namespaced.find((candidate) => candidate.namespace === namespace)
    return allows(verbsIn(entry))
  }
  return allows(unnamespaced[kind] || [])
}

// The Vue havePermissionsTo map: user management does not check a `users`
// kind but the raw Kubernetes resources the API manipulates. `namespace` is
// 'any' on the /manage routes (they carry no namespace parameter).
export function userManagementPermissions(canI, namespace = 'any') {
  return {
    get:
      canI('list', 'secrets', 'any') &&
      canI('list', 'rolebindings', 'any') &&
      canI('list', 'clusterrolebindings'),
    create:
      canI('create', 'secrets', namespace) &&
      canI('list', 'rolebindings', namespace) &&
      canI('create', 'rolebindings', namespace) &&
      canI('patch', 'rolebindings', namespace) &&
      canI('list', 'clusterrolebindings') &&
      canI('create', 'clusterrolebindings') &&
      canI('patch', 'clusterrolebindings'),
    patch:
      canI('get', 'secrets', namespace) &&
      canI('patch', 'secrets', namespace) &&
      canI('list', 'rolebindings', namespace) &&
      canI('create', 'rolebindings', namespace) &&
      canI('patch', 'rolebindings', namespace) &&
      canI('delete', 'rolebindings', namespace) &&
      canI('list', 'clusterrolebindings') &&
      canI('create', 'clusterrolebindings') &&
      canI('patch', 'clusterrolebindings') &&
      canI('delete', 'clusterrolebindings'),
    delete:
      canI('delete', 'secrets', namespace) &&
      canI('list', 'rolebindings', namespace) &&
      canI('patch', 'rolebindings', namespace) &&
      canI('delete', 'rolebindings', namespace) &&
      canI('list', 'clusterrolebindings') &&
      canI('patch', 'clusterrolebindings') &&
      canI('delete', 'clusterrolebindings'),
  }
}
