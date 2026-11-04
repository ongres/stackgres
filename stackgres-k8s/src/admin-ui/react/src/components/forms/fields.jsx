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
      <span>
        {label}
        {required && <span aria-hidden="true"> *</span>}
        {renderTooltip(tooltip)}
      </span>
      {children}
      {help && <span className="text-xs">{help}</span>}
      {error && (
        <span role="alert" className="text-xs" data-invalid>
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
      className={cn('border p-2', error && 'border-2')}
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
      className="border p-2 font-mono"
      {...rest}
    />
  )
}

export function Select({ value, onChange, options, placeholder, ...rest }) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      className="border p-2"
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
      <h2 className="text-lg font-semibold">Default Parameters</h2>
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
                      className="underline"
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

export function FormActions({ onSubmit, onCancel, submitting, submitLabel }) {
  return (
    <div className="flex gap-3">
      <button
        type="submit"
        disabled={submitting}
        onClick={onSubmit}
        className="border p-2 font-semibold"
      >
        {submitLabel}
      </button>
      <button type="button" onClick={onCancel} className="border p-2">
        Cancel
      </button>
    </div>
  )
}
