export default function PendingView({ title }) {
  return (
    <section className="p-8">
      <h2 className="text-xl font-semibold">{title}</h2>
      <p className="text-sm">This view is pending migration from the Vue console.</p>
    </section>
  )
}
