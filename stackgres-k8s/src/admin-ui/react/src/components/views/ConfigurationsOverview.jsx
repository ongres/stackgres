import { useParams } from 'react-router-dom'
import { KindCard } from './NamespaceOverview'

const CONFIG_KINDS = ['sgpgconfigs', 'sgpoolconfigs', 'sgobjectstorages', 'sgscripts']

// Landing page for the Configurations rail section: one overview card per
// configuration kind, like the namespace overview.
export default function ConfigurationsOverview() {
  const { namespace } = useParams()

  return (
    <section className="flex flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">Configurations: {namespace}</h1>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        {CONFIG_KINDS.map((plural) => (
          <KindCard key={plural} plural={plural} namespace={namespace} />
        ))}
      </div>
    </section>
  )
}
