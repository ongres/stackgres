export default function PendingView({ title }) {
  return (
    <section className="p-6">
      <h2 className="text-lg font-semibold">{title}</h2>
      <p className="text-sm text-[hsl(var(--muted-foreground))] italic">This view is pending migration from the Vue console.</p>
    </section>
  )
}
