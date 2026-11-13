import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { Plus, Trash2 } from 'lucide-react'
import { api, ApiError } from '../../lib/api'
import { useCanI } from '../../contexts/PermissionsContext'
import { useFlatList } from '../../hooks/useFlatList'
import { useNamespaces } from '../../hooks/useNamespaces'
import { nameValidationMessage } from '../../lib/names'
import { HelpTooltip } from '../HelpTooltip'
import SummaryDialog from './SummaryDialog'
import { Field, FieldGroup, FieldRow, Select, TextInput } from './fields'

const RULE_KEYS = ['apiGroups', 'nonResourceURLs', 'resourceNames', 'resources', 'verbs']
const blankRule = () => ({
  apiGroups: null,
  nonResourceURLs: null,
  resourceNames: null,
  resources: null,
  verbs: null,
})

// Rules repeater shared by roles and cluster roles: each rule holds the five
// fixed list keys, lazily created and reset to null when emptied, as in Vue.
function RulesEditor({ rules, onChange }) {
  const list = rules || []
  const updateRule = (index, rule) => onChange(list.map((r, i) => (i === index ? rule : r)))
  return (
    <fieldset className="flex flex-col gap-3 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">
        Rules <HelpTooltip field="sgrole.rules" />
      </legend>
      {list.map((rule, index) => (
        <fieldset key={index} className="flex flex-col gap-2 rounded-lg border p-4">
          <legend className="px-2 text-sm">Rule #{index + 1}</legend>
          <button type="button" className="self-start text-sm text-red-500 hover:underline"
            onClick={() => {
              const remaining = list.filter((_, i) => i !== index)
              onChange(remaining.length ? remaining : null)
            }}>
            Delete Rule
          </button>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {RULE_KEYS.map((key) => (
              <fieldset key={key} className="flex flex-col gap-2 rounded-lg border p-4">
                <legend className="px-2 text-sm">
                  {key} <HelpTooltip field={'sgrole.rules.' + key} />
                </legend>
                {(rule[key] || []).map((entry, entryIndex) => (
                  <div key={entryIndex} className="flex items-center gap-2">
                    <TextInput value={entry} aria-label={`${key} ${entryIndex + 1}`}
                      onChange={(value) =>
                        updateRule(index, {
                          ...rule,
                          [key]: rule[key].map((e, i) => (i === entryIndex ? value : e)),
                        })} />
                    <button type="button" aria-label={`Delete ${key} ${entryIndex + 1}`} className="p-1.5 rounded text-red-500 hover:bg-red-500/10"
                      onClick={() => {
                        const remaining = rule[key].filter((_, i) => i !== entryIndex)
                        updateRule(index, { ...rule, [key]: remaining.length ? remaining : null })
                      }}>
                      <Trash2 size={14} />
                    </button>
                  </div>
                ))}
                <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
                  onClick={() => updateRule(index, { ...rule, [key]: [...(rule[key] || []), ''] })}>
                  <Plus size={14} /> Add New
                </button>
              </fieldset>
            ))}
          </div>
        </fieldset>
      ))}
      <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
        onClick={() => onChange([...list, blankRule()])}>
        <Plus size={14} /> Add Rule
      </button>
    </fieldset>
  )
}

// Role and cluster-role form (the Vue CreateRole component, shared).
export default function RoleForm({ kind, isEdit = false }) {
  const { name: routeName } = useParams()
  const navigate = useNavigate()
  const canI = useCanI()
  const namespaces = useNamespaces()
  const clusterScoped = kind === 'clusterroles'
  const { items, refresh } = useFlatList(kind)

  const [role, setRole] = useState(
    isEdit ? null : { metadata: { namespace: '', name: '' }, rules: null }
  )
  const [preview, setPreview] = useState(false)
  const [submitting, setSubmitting] = useState(false)

  if (isEdit && role === null && items) {
    const existing = items.find((candidate) => candidate.metadata.name === routeName)
    if (existing) {
      setRole(JSON.parse(JSON.stringify(existing)))
    }
  }

  if (role === null) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  const patch = (updater) =>
    setRole((current) => {
      const next = JSON.parse(JSON.stringify(current))
      updater(next)
      return next
    })

  const label = clusterScoped ? 'Cluster Role' : 'Role'
  const singular = clusterScoped ? 'clusterrole' : 'role'
  const nameCollision =
    !isEdit &&
    (items || []).some(
      (candidate) =>
        candidate.metadata.name === role.metadata.name &&
        (clusterScoped || candidate.metadata.namespace === role.metadata.namespace)
    )
  const namespaceOptions = namespaces.filter((candidate) =>
    canI(isEdit ? 'patch' : 'create', 'roles', candidate)
  )

  const submit = async (event) => {
    event.preventDefault()
    const nameError = nameValidationMessage(role.metadata.name)
    if (nameError) {
      toast.error(nameError)
      return
    }
    if (nameCollision) {
      toast.error(`A ${label.toLowerCase()} named "${role.metadata.name}" already exists`)
      return
    }
    setSubmitting(true)
    try {
      if (isEdit) {
        await api.update(kind, role)
        toast.success(`${label} "${role.metadata.name}" updated successfully`)
        refresh()
        navigate(`/manage/${singular}/${role.metadata.name}`)
      } else {
        await api.create(kind, role)
        toast.success(`${label} "${role.metadata.name}" created successfully`)
        refresh()
        navigate('/manage/users')
      }
    } catch (error) {
      const detail = error instanceof ApiError && error.detail ? `: ${error.detail}` : ''
      toast.error(`${error.message}${detail}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="flex max-w-4xl flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">
        {isEdit ? `Edit ${label}: ${routeName}` : `Create ${label}`}
      </h1>
      <form onSubmit={submit} className="flex flex-col gap-3">
        <FieldGroup>
          <FieldRow>
            <Field label={`${label} Name`} required tooltip="sgrole.metadata.name">
              <TextInput required disabled={isEdit} value={role.metadata.name}
                onChange={(value) => patch((next) => { next.metadata.name = value })} />
            </Field>
            {!clusterScoped && (
              <Field label="Role Namespace" required tooltip="sgrole.metadata.namespace">
                <Select required disabled={isEdit} value={role.metadata.namespace}
                  options={namespaceOptions} placeholder="Select a namespace…"
                  onChange={(value) => patch((next) => { next.metadata.namespace = value })} />
              </Field>
            )}
          </FieldRow>
          {nameCollision && (
            <p role="alert" className="text-sm text-red-500">
              There&apos;s already a <strong>{label}</strong> with the same name. Please specify a
              different name
            </p>
          )}
        </FieldGroup>
        <RulesEditor rules={role.rules} onChange={(rules) => patch((next) => { next.rules = rules })} />
        <div className="flex items-center gap-3">
          <button type="submit" disabled={submitting} className="px-4 py-2 bg-[hsl(var(--primary))] text-white rounded text-sm font-medium hover:bg-[hsl(var(--primary)/0.85)]">
            {isEdit ? `Update ${label}` : `Create ${label}`}
          </button>
          <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" onClick={() => setPreview(role)}>
            View Summary
          </button>
          <button type="button" className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]" onClick={() => navigate('/manage/users')}>
            Cancel
          </button>
        </div>
      </form>
      {preview && (
        <SummaryDialog value={preview} onClose={() => setPreview(false)} />
      )}
    </section>
  )
}
