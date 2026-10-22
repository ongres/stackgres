import { useEffect, useState } from 'react'

// Temporary landing page while the console views are ported from the Vue app.
// It exercises the pieces the scaffold must prove: routing under /admin/,
// Tailwind styling, and the /stackgres API proxy.
export default function Placeholder() {
  const [namespaces, setNamespaces] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch('/stackgres/namespaces')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`The API responded with status ${response.status}`)
        }
        return response.json()
      })
      .then(setNamespaces)
      .catch((apiError) => setError(apiError.message))
  }, [])

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-4 p-8">
      <h1 className="text-2xl font-semibold">StackGres Web Console</h1>
      <p className="text-sm">React migration in progress — see the migration epic.</p>
      {namespaces && (
        <p data-testid="namespaces" className="text-sm">
          API reachable, namespaces: {namespaces.join(', ')}
        </p>
      )}
      {error && (
        <p data-testid="api-error" className="text-sm">
          API not reachable: {error}
        </p>
      )}
    </main>
  )
}
