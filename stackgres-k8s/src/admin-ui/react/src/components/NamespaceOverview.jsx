import { useParams } from 'react-router-dom'

export default function NamespaceOverview() {
  const { namespace, kind } = useParams()

  return (
    <section className="p-8">
      <h1 className="text-2xl font-semibold">{kind || namespace}</h1>
      <p className="text-sm">This view is pending migration from the Vue console.</p>
    </section>
  )
}
