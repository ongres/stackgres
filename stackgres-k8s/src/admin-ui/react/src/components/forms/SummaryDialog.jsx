import { useMemo, useState } from 'react'
import SpecTree from '../SpecTree'
import { omitDefaults } from '../../lib/diff'

// The Vue form summary popup: a centered dialog over the form with the
// resource tree and — when the host provides the pristine form's payload via
// `defaults` — a "Show default values" switch. Off (the initial state, as in
// Vue) only the parts of the payload that differ from the defaults are shown;
// on, the complete payload.
export default function SummaryDialog({ title = 'Summary', value, onClose, defaults }) {
  const [showDefaults, setShowDefaults] = useState(false)

  const shown = useMemo(
    () => (defaults && !showDefaults ? omitDefaults(value, defaults) : value),
    [value, defaults, showDefaults]
  )

  return (
    <div role="dialog" aria-label={title} className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} aria-hidden="true" />
      <section className="relative mx-4 flex max-h-[80vh] w-full max-w-xl flex-col gap-3 overflow-hidden rounded-lg border bg-[hsl(var(--card))] p-6 text-sm shadow-lg">
        <div className="flex items-center justify-between gap-4">
          <h2 className="text-lg font-semibold">{title}</h2>
          <div className="flex items-center gap-4">
            {defaults && (
              <label className="flex cursor-pointer items-center gap-2 whitespace-nowrap text-xs text-[hsl(var(--muted-foreground))]">
                Show default values
                <button
                  type="button"
                  role="switch"
                  aria-checked={showDefaults}
                  onClick={() => setShowDefaults(!showDefaults)}
                  className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
                    showDefaults ? 'bg-[hsl(var(--primary))]' : 'bg-[hsl(var(--muted))]'
                  }`}
                >
                  <span
                    className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${
                      showDefaults ? 'translate-x-4' : 'translate-x-0.5'
                    }`}
                  />
                </button>
              </label>
            )}
            <button type="button" onClick={onClose} className="text-sm text-[hsl(var(--primary))] hover:underline">
              Close
            </button>
          </div>
        </div>
        <div className="overflow-auto">
          <SpecTree value={shown} />
        </div>
      </section>
    </div>
  )
}
