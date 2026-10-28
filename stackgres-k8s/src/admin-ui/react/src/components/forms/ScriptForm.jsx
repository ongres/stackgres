import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildScript } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextArea, TextInput, Toggle } from './fields'

const SOURCES = [
  { value: 'raw', label: 'Raw script' },
  { value: 'secretKeyRef', label: 'Secret Key Ref' },
  { value: 'configMapKeyRef', label: 'ConfigMap Key Ref' },
]
const TRANSACTION_MODES = [
  { value: '', label: 'NONE' },
  { value: 'read-committed', label: 'Read Committed' },
  { value: 'repeatable-read', label: 'Repeatable Read' },
  { value: 'serializable', label: 'Serializable' },
]

const blankEntry = () => ({
  name: '',
  wrapInTransaction: null,
  storeStatusInDatabase: false,
  retryOnError: false,
  user: '',
  database: '',
  script: '',
})

function sourceOf(entry) {
  if (entry.scriptFrom?.secretKeyRef) return 'secretKeyRef'
  if (entry.scriptFrom?.configMapKeyRef) return 'configMapKeyRef'
  return 'raw'
}

export default function ScriptForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgscripts', namespace, routeName, isEdit)

  const [name, setName] = useState(routeName || '')
  const [continueOnError, setContinueOnError] = useState(false)
  const [managedVersions, setManagedVersions] = useState(true)
  const [scripts, setScripts] = useState([blankEntry()])
  const [scriptSource, setScriptSource] = useState(['raw'])
  const [seeded, setSeeded] = useState(false)

  if (isEdit && loaded && previous && !seeded) {
    const spec = previous.spec || {}
    setContinueOnError(spec.continueOnError || false)
    setManagedVersions(spec.managedVersions !== false)
    const entries = (spec.scripts || []).map((entry) => ({ ...blankEntry(), ...entry }))
    setScripts(entries.length ? entries : [blankEntry()])
    setScriptSource(entries.length ? entries.map(sourceOf) : ['raw'])
    setSeeded(true)
  }

  const updateEntry = (index, patch) =>
    setScripts(scripts.map((entry, i) => (i === index ? { ...entry, ...patch } : entry)))

  const changeSource = (index, source) => {
    setScriptSource(scriptSource.map((current, i) => (i === index ? source : current)))
    if (source === 'raw') {
      const entry = { ...scripts[index] }
      delete entry.scriptFrom
      setScripts(scripts.map((current, i) => (i === index ? entry : current)))
    } else {
      const entry = { ...scripts[index], scriptFrom: { [source]: { name: '', key: '' } } }
      delete entry.script
      setScripts(scripts.map((current, i) => (i === index ? { ...entry, script: '' } : current)))
    }
  }

  const uploadScript = (index) => (event) => {
    const file = event.target.files && event.target.files[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => updateEntry(index, { script: String(reader.result) })
    reader.readAsText(file)
  }

  const form = useCrdForm({
    kind: 'sgscripts',
    isEdit,
    buildPayload: () =>
      buildScript(
        { name, namespace, continueOnError, managedVersions, scripts, scriptSource },
        previous || {}
      ),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!payload.spec.scripts.length) errors.scripts = 'At least one complete script entry is required'
      return errors
    },
    successPath: (payload) =>
      isEdit ? `/${namespace}/sgscript/${payload.metadata.name}` : `/${namespace}/sgscripts`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <CrdFormShell title={isEdit ? `Edit Script: ${routeName}` : 'Create Script'} form={form}>
      <Field label="Script Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      <Toggle label="Continue on Error" checked={continueOnError} onChange={setContinueOnError} />
      <Toggle label="Managed Versions" checked={managedVersions} onChange={setManagedVersions} />
      {form.errors.scripts && (
        <p role="alert" className="text-sm" data-invalid>
          {form.errors.scripts}
        </p>
      )}
      {scripts.map((entry, index) => (
        <fieldset key={index} className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">
            Script Entry #{index + 1}
            {entry.name ? ` – ${entry.name}` : ''}
          </legend>
          <div className="flex flex-wrap gap-3">
            <Field label="Name">
              <TextInput value={entry.name} onChange={(value) => updateEntry(index, { name: value })} />
            </Field>
            <Field label="Database">
              <TextInput value={entry.database} onChange={(value) => updateEntry(index, { database: value })} />
            </Field>
            <Field label="User">
              <TextInput value={entry.user} onChange={(value) => updateEntry(index, { user: value })} />
            </Field>
            <Field label="Wrap in Transaction">
              <Select
                value={entry.wrapInTransaction || ''}
                onChange={(value) => updateEntry(index, { wrapInTransaction: value || null })}
                options={TRANSACTION_MODES}
              />
            </Field>
          </div>
          <div className="flex flex-wrap gap-4">
            <Toggle label="Store Status in Database" checked={entry.storeStatusInDatabase}
              onChange={(value) => updateEntry(index, { storeStatusInDatabase: value })} />
            <Toggle label="Retry on Error" checked={entry.retryOnError}
              onChange={(value) => updateEntry(index, { retryOnError: value })} />
          </div>
          <Field label="Script Source" required>
            <Select value={scriptSource[index]} onChange={(value) => changeSource(index, value)} options={SOURCES} required />
          </Field>
          {scriptSource[index] === 'raw' ? (
            <Field label="Script" required>
              <TextArea value={entry.script || ''} onChange={(value) => updateEntry(index, { script: value })} required rows={8} />
              <input type="file" onChange={uploadScript(index)} aria-label={`Upload script file for entry ${index + 1}`} />
            </Field>
          ) : (
            <div className="flex flex-wrap gap-3">
              <Field label={`${scriptSource[index]} Name`} required>
                <TextInput
                  value={entry.scriptFrom?.[scriptSource[index]]?.name || ''}
                  onChange={(value) =>
                    updateEntry(index, {
                      scriptFrom: {
                        [scriptSource[index]]: { ...entry.scriptFrom?.[scriptSource[index]], name: value },
                      },
                    })
                  }
                  required
                />
              </Field>
              <Field label={`${scriptSource[index]} Key`} required>
                <TextInput
                  value={entry.scriptFrom?.[scriptSource[index]]?.key || ''}
                  onChange={(value) =>
                    updateEntry(index, {
                      scriptFrom: {
                        [scriptSource[index]]: { ...entry.scriptFrom?.[scriptSource[index]], key: value },
                      },
                    })
                  }
                  required
                />
              </Field>
            </div>
          )}
          <button
            type="button"
            onClick={() => {
              setScripts(scripts.filter((_, i) => i !== index))
              setScriptSource(scriptSource.filter((_, i) => i !== index))
            }}
            className="flex items-center gap-1 self-start text-sm underline"
          >
            <Trash2 size={14} /> Delete Entry
          </button>
        </fieldset>
      ))}
      <button
        type="button"
        onClick={() => {
          setScripts([...scripts, blankEntry()])
          setScriptSource([...scriptSource, 'raw'])
        }}
        className="flex items-center gap-1 self-start text-sm underline"
      >
        <Plus size={14} /> Add Entry
      </button>
    </CrdFormShell>
  )
}
