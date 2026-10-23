import { Link } from 'react-router-dom'
import { useNamespaces } from '../hooks/useNamespaces'

export default function HomePage() {
  const namespaces = useNamespaces()

  return (
    <section className="flex flex-col gap-4 p-8">
      <h1 className="text-2xl font-semibold">Namespaces</h1>
      <ul data-testid="namespaces" className="flex flex-col gap-1">
        {namespaces.map((namespace) => (
          <li key={namespace}>
            <Link to={`/${namespace}`} className="underline">
              {namespace}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  )
}
