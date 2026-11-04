import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildScript } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { HelpTooltip } from '../HelpTooltip'
import { Field, Select, TextArea, TextInput, Toggle, renderTooltip } from './fields'

const SOURCES = [
  { value: 'raw', label: 'Raw script' },
  { value: 'secretKeyRef', label: 'From Secret' },
  { value: 'configMapKeyRef', label: 'From ConfigMap' },
]
const TRANSACTION_MODES = [
  { value: '', label: 'NONE' },
  { value: 'read-committed', label: 'READ COMMITTED' },
  { value: 'repeatable-read', label: 'REPEATABLE READ' },
  { value: 'serializable', label: 'SERIALIZABLE' },
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
  const data = useData()

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

  const moveEntry = (index, direction) => {
    const target = index + direction
    if (target < 0 || target >= scripts.length) return
    const nextScripts = [...scripts]
    ;[nextScripts[index], nextScripts[target]] = [nextScripts[target], nextScripts[index]]
    const nextSources = [...scriptSource]
    ;[nextSources[index], nextSources[target]] = [nextSources[target], nextSources[index]]
    setScripts(nextScripts)
    setScriptSource(nextSources)
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

  const nameCollision =
    !isEdit &&
    (data.sgscripts || []).some(
      (script) => script.metadata.name === name && script.metadata.namespace === namespace
    )
  const usedByClusters = (data.sgclusters || [])
    .filter(
      (cluster) =>
        cluster.metadata.namespace === namespace &&
        (cluster.spec?.managedSql?.scripts || []).some((entry) => entry.sgScript === routeName)
    )
    .map((cluster) => cluster.metadata.name)

  return (
    <CrdFormShell title={isEdit ? `Edit Script: ${routeName}` : 'Create Script'} form={form}>
      {isEdit && usedByClusters.length > 0 && (
        <p role="note" className="border p-2 text-sm">
          Please, be aware that some changes made to this script might affect every instance on the
          following cluster(s): {usedByClusters.join(', ')}
        </p>
      )}
      <Field label="Script Name" required error={form.errors.name} tooltip="sgscript.metadata.name">
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      {nameCollision && (
        <p role="alert">
          There&apos;s already an <strong>SGScript</strong> with the same name on this namespace.
          Please specify a different name or create the resource on another namespace
        </p>
      )}
      <h3 className="font-semibold">Scripts{renderTooltip('sgscript.spec.scripts')}</h3>
      <Toggle label="Continue on Error" checked={continueOnError} onChange={setContinueOnError}
        tooltip={
          <HelpTooltip field="sgscript.spec.continueOnError"
            transform={(t) => t.replace(/true/g, 'Enabled').replace('false', 'Disabled')} />
        } />
      <Toggle label="Managed Versions" checked={managedVersions} onChange={setManagedVersions}
        tooltip={
          <HelpTooltip field="sgscript.spec.managedVersions"
            transform={(t) => t.replace(/true/g, 'Enabled')} />
        } />
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
          <div className="flex gap-2">
            <button type="button" className="text-sm underline" disabled={index === 0}
              onClick={() => moveEntry(index, -1)}>
              Move Up
            </button>
            <button type="button" className="text-sm underline" disabled={index === scripts.length - 1}
              onClick={() => moveEntry(index, 1)}>
              Move Down
            </button>
          </div>
          {isEdit && Object.hasOwn(entry, 'version') && (
            <Field label="Version" tooltip="sgscript.spec.scripts.version">
              <TextInput type="number" value={entry.version}
                onChange={(value) => updateEntry(index, { version: value === '' ? '' : Number(value) })} />
            </Field>
          )}
          <div className="flex flex-wrap gap-3">
            <Field label="Name" tooltip="sgscript.spec.scripts.name">
              <TextInput value={entry.name} placeholder="Type a name..." onChange={(value) => updateEntry(index, { name: value })} />
            </Field>
            <Field label="Database" tooltip="sgscript.spec.scripts.database">
              <TextInput value={entry.database} placeholder="Type a database name..." onChange={(value) => updateEntry(index, { database: value })} />
            </Field>
            <Field label="User" tooltip="sgscript.spec.scripts.user">
              <TextInput value={entry.user} placeholder="Type a user name..." onChange={(value) => updateEntry(index, { user: value })} />
            </Field>
            <Field label="Wrap in Transaction" tooltip="sgscript.spec.scripts.wrapInTransaction">
              <Select
                value={entry.wrapInTransaction || ''}
                onChange={(value) => updateEntry(index, { wrapInTransaction: value || null })}
                options={TRANSACTION_MODES}
              />
            </Field>
          </div>
          <div className="flex flex-wrap gap-4">
            <Toggle label="Store Status in Databases" checked={entry.storeStatusInDatabase}
              onChange={(value) => updateEntry(index, { storeStatusInDatabase: value })}
              tooltip="sgscript.spec.scripts.storeStatusInDatabase" />
            <Toggle label="Retry on Error" checked={entry.retryOnError}
              onChange={(value) => updateEntry(index, { retryOnError: value })}
              tooltip="sgscript.spec.scripts.retryOnError" />
          </div>
          <Field label="Script Source" required
            tooltip={
              <HelpTooltip text={'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'} />
            }>
            <Select value={scriptSource[index]} onChange={(value) => changeSource(index, value)} options={SOURCES} required />
          </Field>
          {scriptSource[index] === 'raw' ? (
            <Field label="Script" required>
              <TextArea value={entry.script || ''} placeholder="Type a script..." onChange={(value) => updateEntry(index, { script: value })} required rows={8} />
              <input type="file" onChange={uploadScript(index)} aria-label={`Upload script file for entry ${index + 1}`} />
            </Field>
          ) : (
            <div className="flex flex-col gap-2">
            <h3 className="font-semibold">
              {scriptSource[index] === 'secretKeyRef' ? 'Secret Key Ref' : 'Config Map Key Ref'}
              {renderTooltip('sgscript.spec.scripts.scriptFrom.properties.' + scriptSource[index])}
            </h3>
            <div className="flex flex-wrap gap-3">
              <Field label="Name" required
                tooltip={'sgscript.spec.scripts.scriptFrom.properties.' + scriptSource[index] + '.properties.name'}>
                <TextInput
                  value={entry.scriptFrom?.[scriptSource[index]]?.name || ''}
                  onChange={(value) =>
                    updateEntry(index, {
                      scriptFrom: {
                        [scriptSource[index]]: { ...entry.scriptFrom?.[scriptSource[index]], name: value },
                      },
                    })
                  }
                  placeholder="Type a name.."
                  required
                />
              </Field>
              <Field label="Key" required
                tooltip={'sgscript.spec.scripts.scriptFrom.properties.' + scriptSource[index] + '.properties.key'}>
                <TextInput
                  value={entry.scriptFrom?.[scriptSource[index]]?.key || ''}
                  onChange={(value) =>
                    updateEntry(index, {
                      scriptFrom: {
                        [scriptSource[index]]: { ...entry.scriptFrom?.[scriptSource[index]], key: value },
                      },
                    })
                  }
                  placeholder="Type a key.."
                  required
                />
              </Field>
            </div>
            {isEdit && entry.scriptFrom?.configMapScript !== undefined && (
              <Field label="ConfigMap Script">
                <TextArea value={entry.scriptFrom.configMapScript || ''} rows={8}
                  onChange={(value) =>
                    updateEntry(index, {
                      scriptFrom: { ...entry.scriptFrom, configMapScript: value },
                    })} />
              </Field>
            )}
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
