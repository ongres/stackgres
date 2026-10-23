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
