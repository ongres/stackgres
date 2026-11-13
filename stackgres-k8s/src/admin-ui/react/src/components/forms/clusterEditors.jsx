import { useRef } from 'react'
import { ChevronDown, ChevronUp, ExternalLink, Plus, Trash2 } from 'lucide-react'
import {
  blankContainer,
  blankCustomPort,
  blankExpression,
  blankPreferredAffinityTerm,
  blankRequiredAffinityTerm,
  blankToleration,
} from '../../lib/sgcluster'
import { HelpTooltip } from '../HelpTooltip'
import { Field, renderTooltip, Select, TextArea, TextInput, Toggle } from './fields'

// Vue chains .replace() calls on some boolean-flag descriptions.
const enabledDisabled = (text) => text.replace(/true/g, 'Enabled').replace('false', 'Disabled')

export const AFFINITY_OPERATORS = [
  { value: 'In', label: 'In' },
  { value: 'NotIn', label: 'Not In' },
  { value: 'Exists', label: 'Exists' },
  { value: 'DoesNotExists', label: 'Does Not Exists' },
  { value: 'Gt', label: 'Greater Than' },
  { value: 'Lt', label: 'Less Than' },
]

// ---------------------------------------------------------------------------
// Shared row editors

export function KeyValueRows({ legend, rows, keyName, onChange, tooltip }) {
  const list = rows || []
  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}{renderTooltip(tooltip)}</legend>
      {list.map((row, index) => (
        <div key={index} className="flex items-end gap-2">
          <Field label="Key">
            <TextInput value={row[keyName]}
              onChange={(value) => onChange(list.map((r, i) => (i === index ? { ...r, [keyName]: value } : r)))} />
          </Field>
          <Field label="Value">
            <TextInput value={row.value}
              onChange={(value) => onChange(list.map((r, i) => (i === index ? { ...r, value } : r)))} />
          </Field>
          <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete ${legend} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, { [keyName]: '', value: '' }])}>
        <Plus size={14} /> Add {legend}
      </button>
    </fieldset>
  )
}

export function StringRows({ legend, values, onChange, addLabel = 'Add', tooltip }) {
  const list = values || []
  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}{renderTooltip(tooltip)}</legend>
      {list.map((entry, index) => (
        <div key={index} className="flex items-center gap-2">
          <TextInput value={entry ?? ''} aria-label={`${legend} ${index + 1}`}
            onChange={(value) => onChange(list.map((e, i) => (i === index ? value : e)))} />
          <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete ${legend} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, ''])}>
        <Plus size={14} /> {addLabel}
      </button>
    </fieldset>
  )
}

// Node-affinity requirement editor shared by matchExpressions and matchFields,
// in both required and preferred variants (preferred collapses to one value).
export function ExpressionsEditor({ legend, expressions, onChange, preferred, tooltipPrefix }) {
  const list = expressions || []
  const update = (index, patch) => onChange(list.map((e, i) => (i === index ? { ...e, ...patch } : e)))
  const itemTooltip = (suffix) =>
    tooltipPrefix ? `${tooltipPrefix}.items.properties.${suffix}` : undefined
  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}{renderTooltip(tooltipPrefix)}</legend>
      {list.map((expression, index) => (
        <div key={index} className="flex flex-wrap items-end gap-2">
          <Field label="Key" tooltip={itemTooltip('key')}>
            <TextInput value={expression.key} onChange={(key) => update(index, { key })} />
          </Field>
          <Field label="Operator" tooltip={itemTooltip('operator')}>
            <Select value={expression.operator} options={AFFINITY_OPERATORS}
              placeholder="Select an operator" required={expression.key.length > 0}
              onChange={(operator) => {
                const patch = { operator }
                if (['Exists', 'DoesNotExists'].includes(operator)) {
                  patch.values = undefined
                } else if (
                  !Object.hasOwn(expression, 'values') ||
                  expression.values === undefined ||
                  (preferred && expression.values.length > 1)
                ) {
                  patch.values = ['']
                }
                update(index, patch)
              }} />
          </Field>
          {Object.hasOwn(expression, 'values') &&
            expression.values !== undefined &&
            expression.values.length > 0 &&
            !['', 'Exists', 'DoesNotExists'].includes(expression.operator) && (
              <Field label={['Gt', 'Lt'].includes(expression.operator) ? 'Value' : 'Values'}
                tooltip={itemTooltip('values')}>
                <div className="flex flex-col gap-1">
                  {expression.values.map((value, valueIndex) => (
                    <div key={valueIndex} className="flex items-center gap-2">
                      <TextInput value={value}
                        type={['Gt', 'Lt'].includes(expression.operator) ? 'number' : 'text'}
                        aria-label={`${legend} value ${valueIndex + 1}`}
                        onChange={(next) =>
                          update(index, { values: expression.values.map((v, i) => (i === valueIndex ? next : v)) })} />
                      {['In', 'NotIn'].includes(expression.operator) && (
                        <button type="button" className="p-1 rounded text-red-500 hover:bg-red-500/10" aria-label="Delete value"
                          onClick={() =>
                            update(index, { values: expression.values.filter((_, i) => i !== valueIndex) })}>
                          <Trash2 size={14} />
                        </button>
                      )}
                    </div>
                  ))}
                  {['In', 'NotIn'].includes(expression.operator) && (
                    <button type="button" className="self-start text-sm text-[hsl(var(--primary))] hover:underline"
                      onClick={() => update(index, { values: [...expression.values, ''] })}>
                      Add Value
                    </button>
                  )}
                </div>
              </Field>
            )}
          <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete ${legend} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, blankExpression()])}>
        <Plus size={14} /> Add Expression
      </button>
    </fieldset>
  )
}

export function NodeAffinityTerms({ legend, terms, onChange, preferred, tooltipPrefix }) {
  const list = terms || []
  const updateTerm = (index, term) => onChange(list.map((t, i) => (i === index ? term : t)))
  // The Vue keys: required terms live under .nodeSelectorTerms.items.properties,
  // preferred ones under .items.properties.preference.properties.
  const termsKey = tooltipPrefix
    ? preferred
      ? `${tooltipPrefix}.items`
      : `${tooltipPrefix}.nodeSelectorTerms`
    : undefined
  const expressionsPrefix = (suffix) =>
    tooltipPrefix
      ? preferred
        ? `${tooltipPrefix}.items.properties.preference.properties.${suffix}`
        : `${tooltipPrefix}.nodeSelectorTerms.items.properties.${suffix}`
      : undefined
  return (
    <fieldset className="flex flex-col gap-3 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}{renderTooltip(termsKey)}</legend>
      {list.map((term, index) => {
        const body = preferred ? term.preference : term
        const patchBody = (patch) =>
          updateTerm(index, preferred ? { ...term, preference: { ...body, ...patch } } : { ...term, ...patch })
        return (
          <fieldset key={index} className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm">Term #{index + 1}</legend>
            <ExpressionsEditor legend="Match Expressions" preferred={preferred}
              expressions={body.matchExpressions}
              tooltipPrefix={expressionsPrefix('matchExpressions')}
              onChange={(matchExpressions) => patchBody({ matchExpressions })} />
            <ExpressionsEditor legend="Match Fields" preferred={preferred}
              expressions={body.matchFields}
              tooltipPrefix={expressionsPrefix('matchFields')}
              onChange={(matchFields) => patchBody({ matchFields })} />
            {preferred && (
              <Field label="Weight" tooltip={tooltipPrefix ? `${tooltipPrefix}.weight` : undefined}>
                <TextInput type="number" min="1" max="100" value={term.weight}
                  onChange={(weight) => updateTerm(index, { ...term, weight: Number(weight) })} />
              </Field>
            )}
            <button type="button" className="self-start text-sm text-red-500 hover:underline"
              onClick={() => onChange(list.filter((_, i) => i !== index))}>
              Delete Term
            </button>
          </fieldset>
        )
      })}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() =>
          onChange([...list, preferred ? blankPreferredAffinityTerm() : blankRequiredAffinityTerm()])}>
        <Plus size={14} /> Add Term
      </button>
    </fieldset>
  )
}

export function TolerationsEditor({ tolerations, onChange, tooltipPrefix }) {
  const list = tolerations || []
  const update = (index, patch) => onChange(list.map((t, i) => (i === index ? { ...t, ...patch } : t)))
  const tooltip = (suffix) => (tooltipPrefix ? `${tooltipPrefix}.${suffix}` : undefined)
  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">Tolerations{renderTooltip(tooltipPrefix)}</legend>
      {list.map((toleration, index) => (
        <div key={index} className="flex flex-wrap items-end gap-2">
          <Field label="Key" tooltip={tooltip('key')}>
            <TextInput value={toleration.key || ''} onChange={(key) => update(index, { key })} />
          </Field>
          <Field label="Operator" tooltip={tooltip('operator')}>
            <Select value={toleration.operator || ''} options={['Equal', 'Exists']} placeholder="Select…"
              onChange={(operator) => {
                const next = { ...list[index], operator }
                if (operator === 'Exists') {
                  delete next.value
                } else {
                  next.value = ''
                }
                onChange(list.map((t, i) => (i === index ? next : t)))
              }} />
          </Field>
          {toleration.operator === 'Equal' && (
            <Field label="Value" tooltip={tooltip('value')}>
              <TextInput value={toleration.value || ''} onChange={(value) => update(index, { value })} />
            </Field>
          )}
          <Field label="Effect" tooltip={tooltip('effect')}>
            <Select value={toleration.effect || ''} placeholder="MatchAll"
              options={['NoSchedule', 'PreferNoSchedule', 'NoExecute']}
              onChange={(effect) => update(index, { effect: effect || null })} />
          </Field>
          {toleration.effect === 'NoExecute' && (
            <Field label="Toleration Seconds" tooltip={tooltip('tolerationSeconds')}>
              <TextInput type="number" min="0" value={toleration.tolerationSeconds ?? ''}
                onChange={(value) =>
                  update(index, { tolerationSeconds: value === '' ? null : Number(value) })} />
            </Field>
          )}
          <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete toleration ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, blankToleration()])}>
        <Plus size={14} /> Add Toleration
      </button>
    </fieldset>
  )
}

export function CustomPortsEditor({ legend, ports, onChange, tooltipPrefix }) {
  const list = ports || []
  const update = (index, patch) => onChange(list.map((p, i) => (i === index ? { ...p, ...patch } : p)))
  const tooltip = (suffix) => (tooltipPrefix ? `${tooltipPrefix}.${suffix}` : undefined)
  const requiredPort = (port) =>
    Object.entries(port).some(([key, value]) => key !== 'port' && value !== null && value !== '')
  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}{renderTooltip(tooltipPrefix)}</legend>
      {list.map((port, index) => (
        <div key={index} className="flex flex-wrap items-end gap-2">
          <Field label="Application Protocol" tooltip={tooltip('appProtocol')}>
            <TextInput value={port.appProtocol ?? ''}
              onChange={(value) => update(index, { appProtocol: value || null })} />
          </Field>
          <Field label="Name" tooltip={tooltip('name')}>
            <TextInput value={port.name ?? ''} onChange={(value) => update(index, { name: value || null })} />
          </Field>
          <Field label="Node Port" tooltip={tooltip('nodePort')}>
            <TextInput type="number" value={port.nodePort ?? ''}
              onChange={(value) => update(index, { nodePort: value === '' ? null : Number(value) })} />
          </Field>
          <Field label="Port" required={requiredPort(port)} tooltip={tooltip('port')}>
            <TextInput type="number" required={requiredPort(port)} value={port.port ?? ''}
              onChange={(value) => update(index, { port: value === '' ? null : Number(value) })} />
          </Field>
          <Field label="Protocol" tooltip={tooltip('protocol')}>
            <Select value={port.protocol ?? ''} placeholder="Select…" options={['TCP', 'UDP', 'SCTP']}
              onChange={(value) => update(index, { protocol: value || null })} />
          </Field>
          <Field label="Target Port" tooltip={tooltip('targetPort')}>
            <TextInput value={port.targetPort ?? ''}
              onChange={(value) => update(index, { targetPort: value || null })} />
          </Field>
          <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete ${legend} ${index + 1}`}
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, blankCustomPort()])}>
        <Plus size={14} /> Add Port
      </button>
    </fieldset>
  )
}

export function ContainersEditor({ legend, containers, onChange, tooltipPrefix }) {
  const list = containers || []
  const update = (index, patch) => onChange(list.map((c, i) => (i === index ? { ...c, ...patch } : c)))
  const tooltip = (suffix) => (tooltipPrefix ? `${tooltipPrefix}.${suffix}` : undefined)
  const nameRequired = (container) =>
    Boolean(container.image || container.imagePullPolicy || container.workingDir)
  return (
    <fieldset className="flex flex-col gap-3 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{legend}{renderTooltip(tooltipPrefix)}</legend>
      {list.map((container, index) => (
        <fieldset key={index} className="flex flex-col gap-2 rounded-lg border p-4">
          <legend className="px-2 text-sm">{legend} #{index + 1}</legend>
          <div className="flex flex-wrap gap-2">
            <Field label="Name" required={nameRequired(container)} tooltip={tooltip('name')}>
              <TextInput required={nameRequired(container)} value={container.name ?? ''}
                onChange={(value) => update(index, { name: value || null })} />
            </Field>
            <Field label="Image" tooltip={tooltip('image')}>
              <TextInput value={container.image ?? ''}
                onChange={(value) => update(index, { image: value || null })} />
            </Field>
            <Field label="Image Pull Policy" tooltip={tooltip('imagePullPolicy')}>
              <TextInput value={container.imagePullPolicy ?? ''}
                onChange={(value) => update(index, { imagePullPolicy: value || null })} />
            </Field>
            <Field label="Working Directory" tooltip={tooltip('workingDir')}>
              <TextInput value={container.workingDir ?? ''}
                onChange={(value) => update(index, { workingDir: value || null })} />
            </Field>
          </div>
          <StringRows legend="Arguments" values={(container.args || []).map((a) => a ?? '')}
            addLabel="Add Argument" tooltip={tooltip('args')}
            onChange={(values) => update(index, { args: values.map((v) => (v === '' ? null : v)) })} />
          <StringRows legend="Command" values={(container.command || []).map((c) => c ?? '')}
            addLabel="Add Command" tooltip={tooltip('command')}
            onChange={(values) => update(index, { command: values.map((v) => (v === '' ? null : v)) })} />
          <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm font-medium">Environment Variables{renderTooltip(tooltip('env'))}</legend>
            {(container.env || []).map((entry, envIndex) => (
              <div key={envIndex} className="flex items-end gap-2">
                <Field label="Name" required={Boolean(entry.value)}>
                  <TextInput required={Boolean(entry.value)} value={entry.name ?? ''}
                    onChange={(value) =>
                      update(index, {
                        env: container.env.map((e, i) => (i === envIndex ? { ...e, name: value || null } : e)),
                      })} />
                </Field>
                <Field label="Value">
                  <TextInput value={entry.value ?? ''}
                    onChange={(value) =>
                      update(index, {
                        env: container.env.map((e, i) => (i === envIndex ? { ...e, value: value || null } : e)),
                      })} />
                </Field>
                <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete environment variable ${envIndex + 1}`}
                  onClick={() => update(index, { env: container.env.filter((_, i) => i !== envIndex) })}>
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
            <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
              onClick={() => update(index, { env: [...(container.env || []), { name: null, value: null }] })}>
              <Plus size={14} /> Add Variable
            </button>
          </fieldset>
          <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm font-medium">Ports{renderTooltip(tooltip('ports'))}</legend>
            {(container.ports || []).map((port, portIndex) => (
              <div key={portIndex} className="flex flex-wrap items-end gap-2">
                <Field label="Name" tooltip={tooltip('ports.items.properties.name')}>
                  <TextInput value={port.name ?? ''}
                    onChange={(value) =>
                      update(index, {
                        ports: container.ports.map((p, i) => (i === portIndex ? { ...p, name: value || null } : p)),
                      })} />
                </Field>
                <Field label="Host IP" tooltip={tooltip('ports.items.properties.hostIP')}>
                  <TextInput value={port.hostIP ?? ''}
                    onChange={(value) =>
                      update(index, {
                        ports: container.ports.map((p, i) => (i === portIndex ? { ...p, hostIP: value || null } : p)),
                      })} />
                </Field>
                <Field label="Host Port" tooltip={tooltip('ports.items.properties.hostPort')}>
                  <TextInput type="number" value={port.hostPort ?? ''}
                    onChange={(value) =>
                      update(index, {
                        ports: container.ports.map((p, i) =>
                          i === portIndex ? { ...p, hostPort: value === '' ? null : Number(value) } : p
                        ),
                      })} />
                </Field>
                <Field label="Container Port" tooltip={tooltip('ports.items.properties.containerPort')}>
                  <TextInput type="number" value={port.containerPort ?? ''}
                    onChange={(value) =>
                      update(index, {
                        ports: container.ports.map((p, i) =>
                          i === portIndex ? { ...p, containerPort: value === '' ? null : Number(value) } : p
                        ),
                      })} />
                </Field>
                <Field label="Protocol" tooltip={tooltip('ports.items.properties.protocol')}>
                  <Select value={port.protocol ?? ''} placeholder="Select…" options={['TCP', 'UDP', 'SCTP']}
                    onChange={(value) =>
                      update(index, {
                        ports: container.ports.map((p, i) =>
                          i === portIndex ? { ...p, protocol: value || null } : p
                        ),
                      })} />
                </Field>
                <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete port ${portIndex + 1}`}
                  onClick={() => update(index, { ports: container.ports.filter((_, i) => i !== portIndex) })}>
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
            <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
              onClick={() =>
                update(index, {
                  ports: [
                    ...(container.ports || []),
                    { containerPort: null, hostIP: null, hostPort: null, name: null, protocol: null },
                  ],
                })}>
              <Plus size={14} /> Add Port
            </button>
          </fieldset>
          <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm font-medium">Volume Mounts{renderTooltip(tooltip('volumeMounts'))}</legend>
            {(container.volumeMounts || []).map((mount, mountIndex) => {
              const patchMount = (patch) =>
                update(index, {
                  volumeMounts: container.volumeMounts.map((m, i) =>
                    i === mountIndex ? { ...m, ...patch } : m
                  ),
                })
              return (
                <div key={mountIndex} className="flex flex-wrap items-end gap-2">
                  <Field label="Name" required={Boolean(mount.mountPath)}
                    tooltip={tooltip('volumeMounts.items.properties.name')}>
                    <TextInput required={Boolean(mount.mountPath)} value={mount.name ?? ''}
                      onChange={(value) => patchMount({ name: value || null })} />
                  </Field>
                  <Toggle label="Read Only" checked={mount.readOnly || false}
                    tooltip={tooltip('volumeMounts.items.properties.readOnly')}
                    onChange={(value) => patchMount({ readOnly: value })} />
                  <Field label="Mount Path" required={Boolean(mount.name)}
                    tooltip={tooltip('volumeMounts.items.properties.mountPath')}>
                    <TextInput required={Boolean(mount.name)} value={mount.mountPath ?? ''}
                      onChange={(value) => patchMount({ mountPath: value || null })} />
                  </Field>
                  <Field label="Mount Propagation"
                    tooltip={tooltip('volumeMounts.items.properties.mountPropagation')}>
                    <TextInput value={mount.mountPropagation ?? ''}
                      onChange={(value) => patchMount({ mountPropagation: value || null })} />
                  </Field>
                  <Field label="Sub Path" tooltip={tooltip('volumeMounts.items.properties.subPath')}>
                    <TextInput disabled={Boolean(mount.subPathExpr)} value={mount.subPath ?? ''}
                      onChange={(value) => patchMount({ subPath: value || null })} />
                  </Field>
                  <Field label="Sub Path Expression"
                    tooltip={tooltip('volumeMounts.items.properties.subPathExpr')}>
                    <TextInput disabled={Boolean(mount.subPath)} value={mount.subPathExpr ?? ''}
                      onChange={(value) => patchMount({ subPathExpr: value || null })} />
                  </Field>
                  <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete volume mount ${mountIndex + 1}`}
                    onClick={() =>
                      update(index, {
                        volumeMounts: container.volumeMounts.filter((_, i) => i !== mountIndex),
                      })}>
                    <Trash2 size={16} />
                  </button>
                </div>
              )
            })}
            <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
              onClick={() =>
                update(index, {
                  volumeMounts: [
                    ...(container.volumeMounts || []),
                    {
                      mountPath: null,
                      mountPropagation: null,
                      name: null,
                      readOnly: false,
                      subPath: null,
                      subPathExpr: null,
                    },
                  ],
                })}>
              <Plus size={14} /> Add Volume Mount
            </button>
          </fieldset>
          <button type="button" className="self-start text-sm text-red-500 hover:underline"
            onClick={() => onChange(list.filter((_, i) => i !== index))}>
            Delete {legend} #{index + 1}
          </button>
        </fieldset>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, blankContainer()])}>
        <Plus size={14} /> Add {legend}
      </button>
    </fieldset>
  )
}


// Controlled Postgres extensions browser (Vue CreateSGClusters extensions
// step): license and text filters, per-extension version select, expandable
// details. `extensions` is the parsed list for the active flavor/version
// (null while loading); selection state lives in the host form.
export function ExtensionsBrowser({
  extensions,
  selectedExtensions,
  search,
  onSearch,
  license,
  onLicense,
  view,
  onView,
  onToggle,
  onVersion,
}) {
  const selectedIndex = (name) => selectedExtensions.findIndex((selected) => selected.name === name)
  const visibleExtensions = (extensions || []).filter((extension) => {
    if (license === 'opensource' ? extension.name === 'timescaledb_tsl' : extension.name !== 'timescaledb_tsl') {
      return false
    }
    if (
      search.length &&
      !(extension.name + (extension.description || '') + (extension.tags || []).toString()).includes(search)
    ) {
      return false
    }
    return extension.versions.length > 0
  })

  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap items-end gap-3">
        <Field label="Search Extensions">
          <TextInput value={search} onChange={onSearch} />
        </Field>
        <button type="button" className="border p-2" onClick={() => { onSearch(''); onView(-1) }}>
          Clear
        </button>
        <Field label="License">
          <Select value={license}
            options={[{ value: 'opensource', label: 'Open Source' }, { value: 'nonopensource', label: 'Non Open Source' }]}
            onChange={onLicense} />
        </Field>
      </div>
      {license === 'nonopensource' && (
        <p>
          Non open source extensions may require a license from their publisher to be used in
          production environments.
        </p>
      )}
      {extensions === null && <p>Loading extensions…</p>}
      <ul className="flex flex-col gap-2">
        {visibleExtensions.map((extension, index) => (
          <li key={extension.name} className="border p-3">
            <div className="flex flex-wrap items-center gap-3">
              <input type="checkbox" aria-label={`Enable ${extension.name}`}
                checked={selectedIndex(extension.name) !== -1}
                disabled={!extension.versions.length || !extension.selectedVersion.length}
                onChange={() => onToggle(extension)} />
              <span className="font-semibold">
                {extension.name}
                {extension.url && (
                  <a href={extension.url} target="_blank" rel="noreferrer" aria-label={`${extension.name} website`}>
                    <ExternalLink size={12} className="ml-1 inline" />
                  </a>
                )}
              </span>
              <select className="border p-1" aria-label={`${extension.name} version`}
                value={
                  selectedIndex(extension.name) !== -1
                    ? selectedExtensions[selectedIndex(extension.name)].version
                    : extension.selectedVersion
                }
                onChange={(event) => onVersion(extension.name, event.target.value)}>
                <option value="">Select version…</option>
                {extension.versions.map((version) => (
                  <option key={version}>{version}</option>
                ))}
              </select>
              <span className="flex-1">{extension.abstract}</span>
              <button type="button" aria-label={`Toggle ${extension.name} details`}
                onClick={() => onView(view === index ? -1 : index)}>
                {view === index ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
              </button>
            </div>
            {view === index && (
              <div className="mt-2 flex flex-col gap-1">
                {extension.description && <p>{extension.description}</p>}
                {Boolean(extension.tags?.length) && <p>Tags: {extension.tags.join(', ')}</p>}
                {extension.source && (
                  <a href={extension.source} target="_blank" rel="noreferrer" className="underline">
                    Source
                  </a>
                )}
              </div>
            )}
          </li>
        ))}
        {!visibleExtensions.length && extensions !== null && <li>No extensions match your search</li>}
      </ul>
    </div>
  )
}

// Controlled custom-volumes editor (Vue customVolumes repeater + the parallel
// customVolumesType array): emptyDir/configMap/secret variants with their
// items. onChange receives the next volumes and types arrays together.
export function CustomVolumesEditor({ volumes, types, onChange, tooltipPrefix }) {
  const list = volumes || []
  const change = (nextVolumes, nextTypes) => onChange(nextVolumes, nextTypes || types)
  const updateVolume = (index, volume) => change(list.map((v, i) => (i === index ? volume : v)))
  const isNamed = (volume) => volume.name !== null && volume.name !== ''
  const tooltip = (suffix) => (tooltipPrefix ? `${tooltipPrefix}.${suffix}` : undefined)

  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">Custom Volumes{renderTooltip(tooltipPrefix)}</legend>
      <p>List of volumes that can be mounted by custom containers belonging to the pod</p>
      {list.map((volume, index) => {
        const type = types[index]
        return (
          <fieldset key={index} className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm">Volume #{index + 1}</legend>
            <div className="flex items-end gap-2">
              <Field label="Name" required={type !== null} tooltip={tooltip('name')}>
                <TextInput required={type !== null} value={volume.name ?? ''}
                  onChange={(value) => updateVolume(index, { ...volume, name: value || null })} />
              </Field>
              <Field label="Type"
                tooltip={tooltipPrefix && <HelpTooltip text="Specifies the type of volume to be used" />}>
                <Select value={type ?? ''} placeholder="Choose one..."
                  options={['emptyDir', 'configMap', 'secret']}
                  onChange={(value) => {
                    const nextType = value || null
                    const next = { name: volume.name ?? null }
                    if (nextType === 'emptyDir') {
                      next.emptyDir = { medium: null, sizeLimit: null }
                    } else if (nextType === 'configMap') {
                      next.configMap = {
                        name: null,
                        optional: true,
                        defaultMode: null,
                        items: [{ key: null, mode: null, path: null }],
                      }
                    } else if (nextType === 'secret') {
                      next.secret = {
                        secretName: null,
                        optional: true,
                        defaultMode: null,
                        items: [{ key: null, mode: null, path: null }],
                      }
                    }
                    change(
                      list.map((v, i) => (i === index ? next : v)),
                      types.map((t, i) => (i === index ? nextType : t))
                    )
                  }} />
              </Field>
              <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete volume ${index + 1}`}
                onClick={() =>
                  change(list.filter((_, i) => i !== index), types.filter((_, i) => i !== index))}>
                <Trash2 size={16} />
              </button>
            </div>
            {type === 'emptyDir' && (
              <div className="flex gap-2">
                <Field label="Medium" tooltip={tooltip('emptyDir.properties.medium')}>
                  <TextInput value={volume.emptyDir?.medium ?? ''}
                    onChange={(value) =>
                      updateVolume(index, { ...volume, emptyDir: { ...volume.emptyDir, medium: value || null } })} />
                </Field>
                <Field label="Size Limit" tooltip={tooltip('emptyDir.properties.sizeLimit')}>
                  <TextInput value={volume.emptyDir?.sizeLimit ?? ''}
                    onChange={(value) =>
                      updateVolume(index, { ...volume, emptyDir: { ...volume.emptyDir, sizeLimit: value || null } })} />
                </Field>
              </div>
            )}
            {(type === 'configMap' || type === 'secret') && (
              <div className="flex flex-col gap-2">
                <div className="flex items-end gap-2">
                  <Field label={type === 'configMap' ? 'ConfigMap Name' : 'Secret Name'}
                    tooltip={tooltip(
                      type === 'configMap' ? 'configMap.properties.name' : 'secret.properties.secretName'
                    )}>
                    <TextInput
                      value={(type === 'configMap' ? volume.configMap?.name : volume.secret?.secretName) ?? ''}
                      onChange={(value) =>
                        updateVolume(index, {
                          ...volume,
                          [type]: {
                            ...volume[type],
                            ...(type === 'configMap' ? { name: value || null } : { secretName: value || null }),
                          },
                        })} />
                  </Field>
                  <Toggle label="Optional" checked={volume[type]?.optional || false}
                    tooltip={tooltip(`${type}.properties.optional`)}
                    onChange={(value) =>
                      updateVolume(index, { ...volume, [type]: { ...volume[type], optional: value } })} />
                  <Field label="Default Mode" tooltip={tooltip(`${type}.properties.defaultMode`)}>
                    <TextInput type="number" min="0" value={volume[type]?.defaultMode ?? ''}
                      onChange={(value) =>
                        updateVolume(index, {
                          ...volume,
                          [type]: { ...volume[type], defaultMode: value === '' ? null : Number(value) },
                        })} />
                  </Field>
                </div>
                {(volume[type]?.items || []).map((item, itemIndex) => (
                  <div key={itemIndex} className="flex items-end gap-2">
                    <Field label="Item Key" required={isNamed(volume)}
                      tooltip={tooltip(`${type}.properties.items.items.properties.key`)}>
                      <TextInput value={item.key ?? ''}
                        onChange={(value) =>
                          updateVolume(index, {
                            ...volume,
                            [type]: {
                              ...volume[type],
                              items: volume[type].items.map((it, i) =>
                                i === itemIndex ? { ...it, key: value || null } : it
                              ),
                            },
                          })} />
                    </Field>
                    <Field label="Item Mode"
                      tooltip={tooltip(`${type}.properties.items.items.properties.mode`)}>
                      <TextInput type="number" value={item.mode ?? ''}
                        onChange={(value) =>
                          updateVolume(index, {
                            ...volume,
                            [type]: {
                              ...volume[type],
                              items: volume[type].items.map((it, i) =>
                                i === itemIndex ? { ...it, mode: value === '' ? null : Number(value) } : it
                              ),
                            },
                          })} />
                    </Field>
                    <Field label="Item Path" required={isNamed(volume)}
                      tooltip={tooltip(`${type}.properties.items.items.properties.path`)}>
                      <TextInput value={item.path ?? ''}
                        onChange={(value) =>
                          updateVolume(index, {
                            ...volume,
                            [type]: {
                              ...volume[type],
                              items: volume[type].items.map((it, i) =>
                                i === itemIndex ? { ...it, path: value || null } : it
                              ),
                            },
                          })} />
                    </Field>
                    <button type="button" className="p-1.5 rounded text-red-500 hover:bg-red-500/10" aria-label={`Delete item ${itemIndex + 1}`}
                      onClick={() =>
                        updateVolume(index, {
                          ...volume,
                          [type]: {
                            ...volume[type],
                            items: volume[type].items.filter((_, i) => i !== itemIndex),
                          },
                        })}>
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
                <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
                  onClick={() =>
                    updateVolume(index, {
                      ...volume,
                      [type]: {
                        ...volume[type],
                        items: [...(volume[type].items || []), { key: null, mode: null, path: null }],
                      },
                    })}>
                  <Plus size={14} /> Add Item
                </button>
              </div>
            )}
          </fieldset>
        )
      })}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => change([...list, { name: null }], [...types, null])}>
        <Plus size={14} /> Add Volume
      </button>
    </fieldset>
  )
}

// Controlled managedSql editor (Vue scripts step): base scripts referencing an
// SGScript or an inline scriptSpec, entry repeater with raw /secretKeyRef/
// configMapKeyRef sources, reorder, and file upload for raw entries.
// `sources` is the parallel [{base, entries[]}] descriptor array.
export function ManagedSqlEditor({ value, sources, onChange, scriptOptions, isEdit, blankEntry, tooltipPrefix }) {
  const fileInput = useRef(null)
  const fileTarget = useRef(null)
  const scripts = value.scripts || []
  // Entry fields resolve against the SGScript schema regardless of the host
  // form, as in Vue; only continueOnSGScriptError belongs to the host CRD.
  const sg = (key, transform) =>
    tooltipPrefix ? <HelpTooltip field={key} transform={transform} /> : undefined
  const deepCopy = (object) => JSON.parse(JSON.stringify(object))
  const commit = (mutate) => {
    const nextValue = deepCopy(value)
    const nextSources = deepCopy(sources)
    mutate(nextValue, nextSources)
    onChange(nextValue, nextSources)
  }
  const isDefaultScript = (scriptName) => Boolean(scriptName?.endsWith('-default'))

  const uploadScript = (event) => {
    const file = event.target.files?.[0]
    if (!file || !fileTarget.current) {
      return
    }
    const { base, entry } = fileTarget.current
    const reader = new FileReader()
    reader.onload = () =>
      commit((next) => {
        next.scripts[base].scriptSpec.scripts[entry].script = reader.result
      })
    reader.readAsText(file)
    event.target.value = ''
  }

  return (
    <div className="flex flex-col gap-3">
      <input ref={fileInput} type="file" hidden onChange={uploadScript} />
      {scripts.map((base, baseIndex) => {
        const source = sources[baseIndex]
        const defaultScript = isDefaultScript(base.sgScript)
        const showBody =
          (!isEdit && source.base === 'createNewScript') || (isEdit && Object.hasOwn(base, 'scriptSpec'))
        return (
          <fieldset key={baseIndex} className="flex flex-col gap-2 rounded-lg border p-4">
            <legend className="px-2 text-sm font-medium">Script #{baseIndex + 1}</legend>
            <div className="flex gap-2">
              {!defaultScript && (
                <button type="button" className="text-sm text-red-500 hover:underline"
                  onClick={() =>
                    commit((next, nextSources) => {
                      next.scripts.splice(baseIndex, 1)
                      nextSources.splice(baseIndex, 1)
                    })}>
                  Delete Script
                </button>
              )}
              <button type="button" className="text-sm text-[hsl(var(--primary))] hover:underline" disabled={baseIndex === 0}
                onClick={() =>
                  commit((next, nextSources) => {
                    ;[next.scripts[baseIndex - 1], next.scripts[baseIndex]] = [
                      next.scripts[baseIndex],
                      next.scripts[baseIndex - 1],
                    ]
                    ;[nextSources[baseIndex - 1], nextSources[baseIndex]] = [
                      nextSources[baseIndex],
                      nextSources[baseIndex - 1],
                    ]
                  })}>
                Move Up
              </button>
              <button type="button" className="text-sm text-[hsl(var(--primary))] hover:underline"
                disabled={baseIndex === scripts.length - 1}
                onClick={() =>
                  commit((next, nextSources) => {
                    ;[next.scripts[baseIndex + 1], next.scripts[baseIndex]] = [
                      next.scripts[baseIndex],
                      next.scripts[baseIndex + 1],
                    ]
                    ;[nextSources[baseIndex + 1], nextSources[baseIndex]] = [
                      nextSources[baseIndex],
                      nextSources[baseIndex + 1],
                    ]
                  })}>
                Move Down
              </button>
            </div>
            <Field label="Script Source"
              tooltip={tooltipPrefix &&
                <HelpTooltip text="Determine the source from which the script should be loaded." />}>
              <Select value={source.base} aria-label={`Script ${baseIndex + 1} source`}
                disabled={isEdit && defaultScript && Object.hasOwn(base, 'scriptSpec')}
                options={[{ value: 'createNewScript', label: 'Create new script' }, ...scriptOptions]}
                onChange={(selected) =>
                  commit((next, nextSources) => {
                    nextSources[baseIndex].base = selected
                    if (selected !== 'createNewScript') {
                      next.scripts[baseIndex] = { sgScript: selected }
                      nextSources[baseIndex].entries = []
                    } else {
                      next.scripts[baseIndex] = {
                        scriptSpec: {
                          continueOnError: false,
                          managedVersions: true,
                          scripts: [blankEntry()],
                        },
                      }
                      nextSources[baseIndex].entries = ['raw']
                    }
                  })} />
            </Field>
            {showBody && base.scriptSpec && (
              <div className="flex flex-col gap-2">
                <Toggle label="Continue on Error" checked={base.scriptSpec.continueOnError || false}
                  tooltip={sg('sgscript.spec.continueOnError', enabledDisabled)}
                  onChange={(checked) =>
                    commit((next) => {
                      next.scripts[baseIndex].scriptSpec.continueOnError = checked
                    })} />
                <Toggle label="Managed Versions" checked={base.scriptSpec.managedVersions || false}
                  tooltip={sg('sgscript.spec.managedVersions', (text) => text.replace(/true/g, 'Enabled'))}
                  onChange={(checked) =>
                    commit((next) => {
                      next.scripts[baseIndex].scriptSpec.managedVersions = checked
                    })} />
                {base.scriptSpec.scripts.map((entry, entryIndex) => {
                  const entryType = source.entries[entryIndex]
                  const patchEntry = (mutate) =>
                    commit((next) => {
                      mutate(next.scripts[baseIndex].scriptSpec.scripts[entryIndex])
                    })
                  return (
                    <fieldset key={entryIndex} className="flex flex-col gap-2 rounded-lg border p-4">
                      <legend className="px-2 text-sm">Entry #{entryIndex + 1}</legend>
                      <div className="flex gap-2">
                        <button type="button" className="text-sm text-red-500 hover:underline" disabled={defaultScript}
                          onClick={() =>
                            commit((next, nextSources) => {
                              next.scripts[baseIndex].scriptSpec.scripts.splice(entryIndex, 1)
                              nextSources[baseIndex].entries.splice(entryIndex, 1)
                            })}>
                          Delete Entry
                        </button>
                      </div>
                      {isEdit && Object.hasOwn(entry, 'version') && (
                        <Field label="Version" tooltip={sg('sgscript.spec.scripts.version')}>
                          <TextInput type="number" value={entry.version}
                            onChange={(input) =>
                              patchEntry((draft) => {
                                draft.version = input === '' ? '' : Number(input)
                              })} />
                        </Field>
                      )}
                      <div className="flex flex-wrap gap-2">
                        <Field label="Name" tooltip={sg('sgscript.spec.scripts.name')}>
                          <TextInput disabled={defaultScript} value={entry.name ?? ''}
                            placeholder="Type a name..."
                            onChange={(input) => patchEntry((draft) => { draft.name = input })} />
                        </Field>
                        <Field label="Database" tooltip={sg('sgscript.spec.scripts.database')}>
                          <TextInput disabled={defaultScript} value={entry.database ?? ''}
                            placeholder="Type a database name..."
                            onChange={(input) => patchEntry((draft) => { draft.database = input })} />
                        </Field>
                        <Field label="User" tooltip={sg('sgscript.spec.scripts.user')}>
                          <TextInput disabled={defaultScript} value={entry.user ?? ''}
                            placeholder="Type a user name..."
                            onChange={(input) => patchEntry((draft) => { draft.user = input })} />
                        </Field>
                      </div>
                      <Field label="Wrap in Transaction" tooltip={sg('sgscript.spec.scripts.wrapInTransaction')}>
                        <Select disabled={defaultScript} value={entry.wrapInTransaction ?? ''}
                          options={[
                            { value: '', label: 'NONE' },
                            { value: 'read-committed', label: 'READ COMMITTED' },
                            { value: 'repeatable-read', label: 'REPEATABLE READ' },
                            { value: 'serializable', label: 'SERIALIZABLE' },
                          ]}
                          onChange={(input) =>
                            patchEntry((draft) => {
                              draft.wrapInTransaction = input === '' ? null : input
                            })} />
                      </Field>
                      <Toggle label="Store Status in Databases" checked={entry.storeStatusInDatabase || false}
                        tooltip={sg('sgscript.spec.scripts.storeStatusInDatabase')}
                        onChange={(checked) =>
                          patchEntry((draft) => { draft.storeStatusInDatabase = checked })} />
                      <Toggle label="Retry on Error" checked={entry.retryOnError || false}
                        tooltip={sg('sgscript.spec.scripts.retryOnError')}
                        onChange={(checked) => patchEntry((draft) => { draft.retryOnError = checked })} />
                      <Field label="Entry Source" required
                        tooltip={tooltipPrefix &&
                          <HelpTooltip text={'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'} />}>
                        <Select required disabled={defaultScript} value={entryType}
                          aria-label={`Script ${baseIndex + 1} entry ${entryIndex + 1} source`}
                          options={[
                            { value: 'raw', label: 'Raw script' },
                            { value: 'secretKeyRef', label: 'From Secret' },
                            { value: 'configMapKeyRef', label: 'From ConfigMap' },
                          ]}
                          onChange={(selected) =>
                            commit((next, nextSources) => {
                              nextSources[baseIndex].entries[entryIndex] = selected
                              const draft = next.scripts[baseIndex].scriptSpec.scripts[entryIndex]
                              if (selected === 'raw') {
                                delete draft.scriptFrom
                                if (!Object.hasOwn(draft, 'script')) {
                                  draft.script = ''
                                }
                              } else {
                                delete draft.script
                                draft.scriptFrom = { [selected]: { name: '', key: '' } }
                              }
                            })} />
                      </Field>
                      {entryType === 'raw' && (!isEdit || Object.hasOwn(entry, 'script')) && (
                        <Field label="Script">
                          <TextArea disabled={defaultScript} value={entry.script ?? ''}
                            placeholder="Type a script..."
                            aria-label={`Script ${baseIndex + 1} entry ${entryIndex + 1} body`}
                            onChange={(input) => patchEntry((draft) => { draft.script = input })} />
                          {!isEdit && (
                            <button type="button" className="self-start text-sm text-[hsl(var(--primary))] hover:underline"
                              onClick={() => {
                                fileTarget.current = { base: baseIndex, entry: entryIndex }
                                fileInput.current?.click()
                              }}>
                              …or upload a file
                            </button>
                          )}
                        </Field>
                      )}
                      {entryType !== 'raw' && entry.scriptFrom && (
                        <>
                          <h3 className="font-semibold">
                            {entryType === 'secretKeyRef' ? 'Secret Key Ref' : 'Config Map Key Ref'}
                            {renderTooltip(
                              sg(`sgscript.spec.scripts.scriptFrom.properties.${entryType}`)
                            )}
                          </h3>
                          <div className="flex gap-2">
                            <Field label="Name" required
                              tooltip={sg(
                                `sgscript.spec.scripts.scriptFrom.properties.${entryType}.properties.name`
                              )}>
                              <TextInput required disabled={defaultScript}
                                placeholder="Type a name.."
                                value={entry.scriptFrom[entryType]?.name || ''}
                                onChange={(input) =>
                                  patchEntry((draft) => {
                                    draft.scriptFrom[entryType].name = input
                                  })} />
                            </Field>
                            <Field label="Key" required
                              tooltip={sg(
                                `sgscript.spec.scripts.scriptFrom.properties.${entryType}.properties.key`
                              )}>
                              <TextInput required disabled={defaultScript}
                                placeholder="Type a key.."
                                value={entry.scriptFrom[entryType]?.key || ''}
                                onChange={(input) =>
                                  patchEntry((draft) => {
                                    draft.scriptFrom[entryType].key = input
                                  })} />
                            </Field>
                          </div>
                        </>
                      )}
                      {isEdit && entry.scriptFrom?.configMapScript !== undefined && (
                        <Field label="ConfigMap Script">
                          <TextArea value={entry.scriptFrom.configMapScript}
                            onChange={(input) =>
                              patchEntry((draft) => {
                                draft.scriptFrom.configMapScript = input
                              })} />
                        </Field>
                      )}
                    </fieldset>
                  )
                })}
                <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
                  disabled={defaultScript}
                  onClick={() =>
                    commit((next, nextSources) => {
                      next.scripts[baseIndex].scriptSpec.scripts.push(blankEntry())
                      nextSources[baseIndex].entries.push('raw')
                    })}>
                  <Plus size={14} /> Add Entry
                </button>
              </div>
            )}
          </fieldset>
        )
      })}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() =>
          commit((next, nextSources) => {
            next.scripts.push({
              continueOnError: false,
              scriptSpec: { continueOnError: false, managedVersions: true, scripts: [blankEntry()] },
            })
            nextSources.push({ base: 'createNewScript', entries: ['raw'] })
          })}>
        <Plus size={14} /> Add Script
      </button>
      {scripts.length > 0 && (
        <Toggle label="Continue on SGScripts Error" checked={value.continueOnSGScriptError}
          tooltip={tooltipPrefix &&
            <HelpTooltip field={`${tooltipPrefix}.continueOnSGScriptError`} transform={enabledDisabled} />}
          onChange={(checked) =>
            commit((next) => {
              next.continueOnSGScriptError = checked
            })} />
      )}
    </div>
  )
}
