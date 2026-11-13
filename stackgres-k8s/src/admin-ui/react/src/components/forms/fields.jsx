import { cn } from '../../lib/utils'
import { HelpTooltip } from '../HelpTooltip'

// Minimal controlled field primitives for the CRD forms. Purely functional
// styling; the visual pass belongs to the design-system work.

// `tooltip` is either a tooltip key string (resolved against sg-tooltips.json)
// or a ready <HelpTooltip> element for literals/transformed texts.
export function renderTooltip(tooltip) {
  if (!tooltip) {
    return null
  }
  return typeof tooltip === 'string' ? <HelpTooltip field={tooltip} /> : tooltip
}

export function Field({ label, required, error, children, help, tooltip }) {
  return (
    <label className="flex flex-col gap-1 text-sm">
      <span className="text-[hsl(var(--muted-foreground))]">
        {label}
        {required && <span aria-hidden="true"> *</span>}
        {renderTooltip(tooltip)}
      </span>
      {children}
      {help && <span className="text-xs text-[hsl(var(--muted-foreground))]">{help}</span>}
      {error && (
        <span role="alert" className="text-xs text-red-500" data-invalid>
          {error}
        </span>
      )}
    </label>
  )
}

export function TextInput({ value, onChange, error, ...rest }) {
  return (
    <input
      value={value}
      onChange={(event) => onChange(event.target.value)}
      className={cn('border p-2 rounded bg-[hsl(var(--input-background))]', error && 'border-red-500')}
      aria-invalid={error ? 'true' : undefined}
      {...rest}
    />
  )
}

export function TextArea({ value, onChange, rows = 6, ...rest }) {
  return (
    <textarea
      value={value}
      onChange={(event) => onChange(event.target.value)}
      rows={rows}
      className="border p-2 rounded bg-[hsl(var(--input-background))] font-mono"
      {...rest}
    />
  )
}

export function Select({ value, onChange, options, placeholder, ...rest }) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      className="border p-2 rounded bg-[hsl(var(--input-background))]"
      {...rest}
    >
      {placeholder && <option value="">{placeholder}</option>}
      {options.map((option, index) =>
        typeof option === 'string' ? (
          <option key={option} value={option}>
            {option}
          </option>
        ) : (
          <option key={`${option.value}-${index}`} value={option.value} disabled={option.disabled}>
            {option.label}
          </option>
        )
      )}
    </select>
  )
}

export function Toggle({ label, checked, onChange, disabled, tooltip }) {
  return (
    <label className="flex items-center gap-2 text-sm">
      <input type="checkbox" checked={checked} disabled={disabled}
        onChange={(event) => onChange(event.target.checked)} />
      {label}
      {renderTooltip(tooltip)}
    </label>
  )
}

// "Default Parameters" table of the config forms: the resolved configuration
// rows still carrying their StackGres default value.
export function DefaultParametersTable({ intro, defaults, resolved, loading }) {
  const rows = (resolved || []).filter(
    (parameter) =>
      Object.hasOwn(defaults || {}, parameter.parameter) &&
      defaults[parameter.parameter] === parameter.value
  )
  if (!rows.length && !loading) {
    return null
  }
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-sm font-semibold">Default Parameters</h2>
      <p>{intro}</p>
      {loading ? (
        <p>Loading default parameters…</p>
      ) : (
        <table className="w-fit text-left">
          <tbody>
            {rows.map((parameter) => (
              <tr key={parameter.parameter}>
                <td className="pr-4 font-mono">
                  {parameter.documentationLink ? (
                    <a href={parameter.documentationLink} target="_blank" rel="noreferrer"
                      className="text-[hsl(var(--primary))] hover:underline"
                      title={`Read documentation about ${parameter.parameter}`}>
                      {parameter.parameter}
                    </a>
                  ) : (
                    parameter.parameter
                  )}
                </td>
                <td className="font-mono">{parameter.value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}

// Vue's deliberate two-column field layout: each group is an optional small
// heading plus 50/50 rows, separated from the next group by a rule.
export function FieldGroup({ title, children }) {
  return (
    <div className="flex flex-col gap-3 border-b pb-4">
      {title && <h3 className="text-sm font-semibold">{title}</h3>}
      {children}
    </div>
  )
}

export function FieldRow({ children }) {
  return <div className="grid grid-cols-1 gap-4 md:grid-cols-2">{children}</div>
}

export function FormActions({ onSubmit, onCancel, submitting, submitLabel }) {
  return (
    <div className="flex gap-3">
      <button
        type="submit"
        disabled={submitting}
        onClick={onSubmit}
        className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]"
      >
        {submitLabel}
      </button>
      <button type="button" onClick={onCancel}
        className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]">
        Cancel
      </button>
    </div>
  )
}
