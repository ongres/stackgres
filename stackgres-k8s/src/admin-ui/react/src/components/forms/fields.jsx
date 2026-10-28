import { cn } from '../../lib/utils'

// Minimal controlled field primitives for the CRD forms. Purely functional
// styling; the visual pass belongs to the design-system work.

export function Field({ label, required, error, children, help }) {
  return (
    <label className="flex flex-col gap-1 text-sm">
      <span>
        {label}
        {required && <span aria-hidden="true"> *</span>}
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
      {options.map((option) =>
        typeof option === 'string' ? (
          <option key={option} value={option}>
            {option}
          </option>
        ) : (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        )
      )}
    </select>
  )
}

export function Toggle({ label, checked, onChange }) {
  return (
    <label className="flex items-center gap-2 text-sm">
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      {label}
    </label>
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
