import { useState } from 'react'
import { Check, Copy, Plug, X } from 'lucide-react'

function CopyButton({ value }) {
  const [copied, setCopied] = useState(false)

  const copy = () => {
    navigator.clipboard.writeText(value)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <button
      type="button"
      onClick={copy}
      title="Copy to clipboard"
      aria-label={copied ? 'Copied' : 'Copy to clipboard'}
      className="ml-auto shrink-0 rounded-md p-1.5 text-[hsl(var(--muted-foreground))] transition-colors hover:bg-[hsl(var(--muted))] hover:text-[hsl(var(--foreground))]"
    >
      {copied ? (
        <Check className="h-3.5 w-3.5 text-[hsl(var(--primary))]" strokeWidth={1.5} />
      ) : (
        <Copy className="h-3.5 w-3.5" strokeWidth={1.5} />
      )}
    </button>
  )
}

// Connection details behind a heading-level button: clicking opens a floating
// dialog with the rows (aligned labels, monospace values, one-click copy).
// `disabledReason` renders the trigger disabled with the reason as tooltip.
export default function ConnectionInfoDialog({ rows, disabledReason = null }) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        disabled={Boolean(disabledReason)}
        title={disabledReason || undefined}
        className="flex shrink-0 items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm font-medium transition-colors hover:bg-[hsl(var(--muted))] disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Plug className="h-4 w-4" strokeWidth={1.5} aria-hidden="true" />
        Connection Information
      </button>
      {open && (
        <div role="dialog" aria-label="Connection information" className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => setOpen(false)} aria-hidden="true" />
          <div className="relative mx-4 flex w-full max-w-2xl flex-col rounded-lg border bg-[hsl(var(--card))] text-sm shadow-lg">
            <header className="flex items-center justify-between border-b px-6 py-4">
              <h2 className="text-lg font-semibold">Connection Information</h2>
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Close"
                className="rounded-md p-1.5 text-[hsl(var(--muted-foreground))] transition-colors hover:bg-[hsl(var(--muted))] hover:text-[hsl(var(--foreground))]"
              >
                <X className="h-4 w-4" strokeWidth={1.5} />
              </button>
            </header>
            <div className="divide-y">
              {rows.map((row) => (
                <div key={row.label} className="flex items-center gap-3 px-6 py-2.5">
                  <span className="w-40 shrink-0 text-[hsl(var(--muted-foreground))]">{row.label}</span>
                  <code className="overflow-x-auto whitespace-nowrap rounded bg-[hsl(var(--secondary))] px-2 py-1 font-mono">
                    {row.value}
                  </code>
                  <CopyButton value={row.value} />
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </>
  )
}
