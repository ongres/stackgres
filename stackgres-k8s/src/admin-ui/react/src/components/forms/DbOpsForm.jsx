import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useSettings } from '../../contexts/SettingsContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { fromDateTimeLocal } from '../../lib/datetime'
import { buildDbOps } from '../../lib/payloads'
import { generatedName, nameValidationMessage } from '../../lib/names'
import { prefix } from '../../lib/format'
import CrdFormShell from './CrdFormShell'
import { HelpTooltip } from '../HelpTooltip'
import { TolerationsEditor } from './clusterEditors'
import { Field, FieldGroup, FieldRow, Select, TextInput, Toggle, renderTooltip } from './fields'

const OPS = [
  'benchmark',
  'vacuum',
  'repack',
  'securityUpgrade',
  'minorVersionUpgrade',
  'majorVersionUpgrade',
  'restart',
]
const METHODS = [
  { value: 'InPlace', label: 'In Place' },
  { value: 'ReducedImpact', label: 'Reduced Impact' },
]
const INHERIT = [
  { value: 'inherit', label: 'Inherit from global settings' },
  { value: 'true', label: 'YES' },
  { value: 'false', label: 'NO' },
]
const VACUUM_DB_LABELS = {
  full: 'Full',
  freeze: 'Freeze',
  analyze: 'Analyze',
  disablePageSkipping: 'Page Skipping',
}
const REPACK_DB_LABELS = {
  noOrder: 'Order',
  noKillBackend: 'Kill Backend',
  noAnalyze: 'Analyze',
  excludeExtension: 'Exclude Extension',
}

// The Vue templates reword some tooltips of inverted (noX / disableX) flags.
const PAGE_SKIPPING_TRANSFORM = (t) => t.replace('Defaults to: `false`', 'Enabled by default')
const NO_ORDER_TRANSFORM = (t) =>
  t.replace('If true', 'Disable to').replace('Defaults to: `false`', 'Enabled by default')
const NO_KILL_BACKEND_TRANSFORM = (t) =>
  t.replace("If true don't", 'Disable to not').replace('Defaults to: `false`', 'Enabled by default')
const VACUUM_DB_TOOLTIPS = {
  full: 'sgdbops.spec.vacuum.databases.full',
  freeze: 'sgdbops.spec.vacuum.databases.freeze',
  analyze: 'sgdbops.spec.vacuum.databases.analyze',
  disablePageSkipping: (
    <HelpTooltip field="sgdbops.spec.vacuum.databases.disablePageSkipping" transform={PAGE_SKIPPING_TRANSFORM} />
  ),
}
const REPACK_DB_TOOLTIPS = {
  noOrder: <HelpTooltip field="sgdbops.spec.repack.databases.noOrder" transform={NO_ORDER_TRANSFORM} />,
  noKillBackend: (
    <HelpTooltip field="sgdbops.spec.repack.databases.noKillBackend" transform={NO_KILL_BACKEND_TRANSFORM} />
  ),
  noAnalyze: <HelpTooltip field="sgdbops.spec.repack.databases.noAnalyze" transform={NO_KILL_BACKEND_TRANSFORM} />,
  excludeExtension: 'sgdbops.spec.repack.databases.excludeExtension',
}

const opLabel = (op) => op.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())

function DurationSelects({ label, value, onChange, tooltip }) {
  const part = (key, max, unit) => (
    <label className="flex items-center gap-1">
      <select
        value={String(value[key])}
        onChange={(event) => onChange({ ...value, [key]: event.target.value })}
        className="px-2 py-1.5 text-sm border border-[hsl(var(--border))] rounded bg-[hsl(var(--input-background))]"
        aria-label={`${label} ${unit}`}
      >
        {Array.from({ length: max + 1 }, (_, i) => (
          <option key={i} value={String(i)}>
            {i}
          </option>
        ))}
      </select>
      {unit}
    </label>
  )
  return (
    <Field label={label} tooltip={tooltip}>
      <div className="flex flex-wrap gap-2 text-sm">
        {part('d', 30, 'days')}
        {part('h', 23, 'hours')}
        {part('m', 59, 'minutes')}
        {part('s', 59, 'seconds')}
      </div>
    </Field>
  )
}

// The daterangepicker of the Vue form is replaced with a native
// datetime-local input; the value converts to UTC ISO on build, honoring the
// console's local/utc timezone preference as the Vue setRunAt did.
const runAtToIso = fromDateTimeLocal

export default function DbOpsForm() {
  const { namespace } = useParams()
  const { timezone } = useSettings()
  const data = useData()

  const [name, setName] = useState(generatedName('op', timezone))
  const [sgCluster, setSgCluster] = useState('')
  const [op, setOp] = useState('')
  const [tolerations, setTolerations] = useState([])
  const [runAtLocal, setRunAtLocal] = useState('')
  const [timeout, setTimeoutValue] = useState({ d: 0, h: 0, m: 0, s: 0 })
  const [maxRetries, setMaxRetries] = useState('0')
  const [restart, setRestart] = useState({ method: 'InPlace', onlyPendingRestart: false })
  const [securityUpgrade, setSecurityUpgrade] = useState({ method: 'InPlace' })
  const [minorVersionUpgrade, setMinorVersionUpgrade] = useState({ method: 'InPlace', postgresVersion: '' })
  const [majorVersionUpgrade, setMajorVersionUpgrade] = useState({
    link: false, clone: false, check: false, backupPath: null, postgresVersion: '', sgPostgresConfig: '',
  })
  const [vacuum, setVacuum] = useState({ full: false, freeze: false, analyze: true, disablePageSkipping: false })
  const [vacuumPerDbs, setVacuumPerDbs] = useState(false)
  const [vacuumDbs, setVacuumDbs] = useState([])
  const [repack, setRepack] = useState({
    noOrder: false, waitTimeout: { d: 0, h: 0, m: 0, s: 0 }, noKillBackend: false, noAnalyze: false, excludeExtension: false,
  })
  const [repackPerDbs, setRepackPerDbs] = useState(false)
  const [repackDbs, setRepackDbs] = useState([])
  const [benchmark, setBenchmark] = useState({
    type: 'pgbench',
    connectionType: 'primary-service',
    pgbench: {
      databaseSize: { size: '1', unit: 'Gi' },
      duration: { d: 0, h: 0, m: 0, s: 0 },
      usePreparedStatements: false,
      concurrentClients: '1',
      threads: '1',
    },
  })
  const [versions, setVersions] = useState([])

  useEffect(() => {
    api
      .getPostgresVersions('vanilla')
      .then((response) => setVersions(response.postgresql || []))
      .catch(() => {})
  }, [])

  const clusters = (data.sgclusters || []).filter((c) => c.metadata.namespace === namespace)
  const nameCollision = (data.sgdbops || []).some(
    (candidate) => candidate.metadata.name === name && candidate.metadata.namespace === namespace
  )
  const cluster = clusters.find((c) => c.metadata.name === sgCluster)
  const currentVersion = cluster?.spec?.postgres?.version || ''
  const currentMajor = prefix(currentVersion)
  const minorTargets = versions.filter(
    (v) => prefix(v) === currentMajor && v !== currentVersion
  )
  const majorTargets = versions.filter((v) => Number(prefix(v)) === Number(currentMajor) + 1)
  const pgConfigs = (data.sgpgconfigs || []).filter(
    (c) =>
      c.metadata.namespace === namespace &&
      c.spec.postgresVersion === prefix(majorVersionUpgrade.postgresVersion)
  )

  const form = useCrdForm({
    kind: 'sgdbops',
    isEdit: false,
    buildPayload: () =>
      buildDbOps({
        name,
        namespace,
        sgCluster,
        op,
        runAt: runAtToIso(runAtLocal, timezone),
        timeout,
        maxRetries,
        tolerations,
        restart,
        securityUpgrade,
        minorVersionUpgrade,
        majorVersionUpgrade,
        vacuum,
        vacuumPerDbs,
        vacuumDbs,
        repack,
        repackPerDbs,
        repackDbs,
        benchmark,
      }),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!sgCluster) errors.sgCluster = 'A target cluster is required'
      if (!op) errors.op = 'An operation is required'
      if (op === 'minorVersionUpgrade' && !minorVersionUpgrade.postgresVersion)
        errors.targetVersion = 'A target Postgres version is required'
      if (op === 'majorVersionUpgrade') {
        if (!majorVersionUpgrade.postgresVersion) errors.targetVersion = 'A target Postgres version is required'
        else if (!majorVersionUpgrade.sgPostgresConfig) errors.targetConfig = 'A target Postgres configuration is required'
      }
      if (op === 'benchmark' && !benchmark.pgbench.databaseSize.size)
        errors.databaseSize = 'A database size is required'
      return errors
    },
    successPath: () => `/${namespace}/sgdbops`,
  })

  const dbSelect = (value, onChange) => (
    <Select value={String(value)} onChange={onChange} options={INHERIT} />
  )

  return (
    <CrdFormShell title="Create Database Operation" form={form}>
      <FieldGroup>
        <FieldRow>
          <Field label="Operation Name" required error={form.errors.name} tooltip="sgdbops.metadata.name">
            <TextInput value={name} onChange={setName} required error={form.errors.name} />
          </Field>
          <Field label="Target Cluster" required error={form.errors.sgCluster} tooltip="sgdbops.spec.sgCluster">
            <Select value={sgCluster} onChange={setSgCluster} required placeholder="Choose a Cluster"
              options={clusters.map((c) => c.metadata.name)} />
          </Field>
        </FieldRow>
        {nameCollision && (
          <p role="alert" className="text-sm text-red-500">
            There&apos;s already a <strong>SGDbOps</strong> with the same name on this namespace.
            Please specify a different name or create the operation on another namespace.
          </p>
        )}
      </FieldGroup>
      <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
        <legend className="px-2 text-sm font-medium">Database Operation *{renderTooltip('sgdbops.spec.op')}</legend>
        {form.errors.op && <p role="alert" className="text-sm text-red-500">{form.errors.op}</p>}
        <div className="flex flex-wrap gap-3">
          {OPS.map((candidate) => (
            <label key={candidate} className={`flex items-start gap-4 p-4 border rounded-lg cursor-pointer transition-colors ${op === candidate ? 'border-[hsl(var(--primary))] bg-[hsl(var(--primary)/0.05)] dark:bg-[hsl(var(--primary)/0.1)]' : 'border-[hsl(var(--border))] hover:border-[hsl(var(--muted-foreground))]'}`}>
              <input type="radio" name="op" value={candidate} checked={op === candidate}
                onChange={() => setOp(candidate)} />
              {opLabel(candidate)}
            </label>
          ))}
        </div>
      </fieldset>

      <FieldGroup title="General Operation Details">
        <FieldRow>
          <Field label={`Run At (${timezone === 'local' ? 'local time' : 'UTC'})`} help="Leave empty to run as soon as possible"
            tooltip={
              <HelpTooltip field="sgdbops.spec.runAt"
                transform={(t) => (timezone === 'local' ? t.replace('UTC ', '') : t)} />
            }>
            <input type="datetime-local" step="1" value={runAtLocal}
              onChange={(event) => setRunAtLocal(event.target.value)} className="w-full border p-2 rounded bg-[hsl(var(--input-background))]" />
          </Field>
          <DurationSelects label="Timeout" value={timeout} onChange={setTimeoutValue} tooltip="sgdbops.spec.timeout" />
        </FieldRow>
        {timezone === 'utc' && runAtLocal.length > 0 && (
          <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
            Bear in mind &quot;Run At&quot; times are expressed in UTC (Coordinated Universal Time).
          </p>
        )}
        <FieldRow>
          <Field label="Max Retries" tooltip="sgdbops.spec.maxRetries">
            <Select value={maxRetries} onChange={setMaxRetries} options={Array.from({ length: 11 }, (_, i) => String(i))} />
          </Field>
        </FieldRow>
      </FieldGroup>

      <FieldGroup title="Node Tolerations">
        <TolerationsEditor tolerations={tolerations} onChange={setTolerations}
          tooltipPrefix="sgdbops.spec.scheduling.tolerations" />
      </FieldGroup>

      {op === 'restart' && (
        <FieldGroup title="Restart Details">
          <FieldRow>
            <Field label="Method" tooltip="sgdbops.spec.restart.method">
              <Select value={restart.method} onChange={(method) => setRestart({ ...restart, method })} options={METHODS} />
            </Field>
            <Field label="Pods to Restart" tooltip="sgdbops.spec.restart.onlyPendingRestart">
              <Toggle label="Pending Pods Only" checked={restart.onlyPendingRestart}
                onChange={(onlyPendingRestart) => setRestart({ ...restart, onlyPendingRestart })} />
            </Field>
          </FieldRow>
        </FieldGroup>
      )}

      {op === 'securityUpgrade' && (
        <FieldGroup title="Security Upgrade Details">
          <FieldRow>
            <Field label="Method" tooltip="sgdbops.spec.securityUpgrade.method">
              <Select value={securityUpgrade.method} onChange={(method) => setSecurityUpgrade({ method })} options={METHODS} />
            </Field>
          </FieldRow>
        </FieldGroup>
      )}

      {op === 'minorVersionUpgrade' && (
        <FieldGroup title="Minor Version Upgrade Details">
          {!sgCluster ? (
            <p className="text-sm">
              You must first choose a target cluster to be able to set the upgrade details
            </p>
          ) : minorTargets.length ? (
            <FieldRow>
              <Field label="Method" tooltip="sgdbops.spec.minorVersionUpgrade.method">
                <Select value={minorVersionUpgrade.method}
                  onChange={(method) => setMinorVersionUpgrade({ ...minorVersionUpgrade, method })} options={METHODS} />
              </Field>
              <Field label="Target Postgres Version" required error={form.errors.targetVersion}
                tooltip="sgdbops.spec.minorVersionUpgrade.postgresVersion">
                <Select value={minorVersionUpgrade.postgresVersion}
                  onChange={(postgresVersion) => setMinorVersionUpgrade({ ...minorVersionUpgrade, postgresVersion })}
                  options={minorTargets} placeholder="Choose version..." required />
              </Field>
            </FieldRow>
          ) : (
            <p className="text-sm">
              Your cluster is already set to use Postgres {currentVersion} which is the latest minor
              version available for Postgres {currentMajor}. If you prefer, you could perform a Major
              Version Upgrade to Postgres {Number(currentMajor) + 1}.
            </p>
          )}
        </FieldGroup>
      )}

      {op === 'majorVersionUpgrade' && (
        <FieldGroup title="Major Version Upgrade Details">
          {!sgCluster ? (
            <p className="text-sm">
              You must first choose a target cluster to be able to set the upgrade details
            </p>
          ) : majorTargets.length ? (
            <FieldRow>
              <Toggle label="Hard Link Files" checked={majorVersionUpgrade.link}
                onChange={(link) => setMajorVersionUpgrade({ ...majorVersionUpgrade, link })}
                tooltip="sgdbops.spec.majorVersionUpgrade.link" />
              <Toggle label="Clone Files" checked={majorVersionUpgrade.clone}
                onChange={(clone) => setMajorVersionUpgrade({ ...majorVersionUpgrade, clone })}
                tooltip="sgdbops.spec.majorVersionUpgrade.clone" />
              <Toggle label="Check Clusters" checked={majorVersionUpgrade.check}
                onChange={(check) => setMajorVersionUpgrade({ ...majorVersionUpgrade, check })}
                tooltip="sgdbops.spec.majorVersionUpgrade.check" />
              <Field label="Backup Path" tooltip="sgdbops.spec.majorVersionUpgrade.backupPath">
                <TextInput value={majorVersionUpgrade.backupPath || ''}
                  onChange={(backupPath) =>
                    setMajorVersionUpgrade({ ...majorVersionUpgrade, backupPath: backupPath.length ? backupPath : null })} />
              </Field>
              {/* The Vue form reuses the minor-version key here; fixed deliberately. */}
              <Field label="Target Postgres Version" required error={form.errors.targetVersion}
                tooltip="sgdbops.spec.majorVersionUpgrade.postgresVersion">
                <Select value={majorVersionUpgrade.postgresVersion}
                  onChange={(postgresVersion) =>
                    setMajorVersionUpgrade({ ...majorVersionUpgrade, postgresVersion, sgPostgresConfig: '' })}
                  options={majorTargets} placeholder="Choose version..." required />
              </Field>
              {majorVersionUpgrade.postgresVersion && (
                <Field label="Target Postgres Configuration" required error={form.errors.targetConfig}
                  tooltip="sgdbops.spec.majorVersionUpgrade.sgPostgresConfig">
                  <Select value={majorVersionUpgrade.sgPostgresConfig}
                    onChange={(sgPostgresConfig) => setMajorVersionUpgrade({ ...majorVersionUpgrade, sgPostgresConfig })}
                    options={pgConfigs.map((c) => c.metadata.name)} placeholder="Choose config..." required />
                </Field>
              )}
            </FieldRow>
          ) : (
            <p className="text-sm">
              Your cluster is already set to use Postgres {currentMajor} which is the latest major
              version available.
            </p>
          )}
        </FieldGroup>
      )}

      {op === 'vacuum' && (
        <FieldGroup title="Vacuum Details">
          <FieldRow>
            <Toggle label="Full Vacuum" checked={vacuum.full} onChange={(full) => setVacuum({ ...vacuum, full })}
              tooltip="sgdbops.spec.vacuum.full" />
            <Toggle label="Freeze" checked={vacuum.freeze} onChange={(freeze) => setVacuum({ ...vacuum, freeze })}
              tooltip="sgdbops.spec.vacuum.freeze" />
            <Toggle label="Analyze" checked={vacuum.analyze} onChange={(analyze) => setVacuum({ ...vacuum, analyze })}
              tooltip="sgdbops.spec.vacuum.analyze" />
            <Toggle label="Page Skipping" checked={!vacuum.disablePageSkipping}
              onChange={(enabled) => setVacuum({ ...vacuum, disablePageSkipping: !enabled })}
              tooltip={
                <HelpTooltip field="sgdbops.spec.vacuum.disablePageSkipping" transform={PAGE_SKIPPING_TRANSFORM} />
              } />
            <Toggle label="Database Specific Options" checked={vacuumPerDbs}
              tooltip="sgdbops.spec.vacuum.databases"
              onChange={(enabled) => {
                setVacuumPerDbs(enabled)
                if (enabled && !vacuumDbs.length)
                  setVacuumDbs([{ name: '', full: 'inherit', freeze: 'inherit', analyze: 'inherit', disablePageSkipping: 'inherit' }])
              }} />
          </FieldRow>
          {vacuumPerDbs && <h4 className="text-sm font-semibold">Databases{renderTooltip('sgdbops.spec.vacuum.databases')}</h4>}
          {vacuumPerDbs &&
            vacuumDbs.map((db, index) => (
              <div key={index} className="flex flex-col gap-2 rounded-lg border p-4">
                <h5 className="text-sm font-semibold">
                  Database #{index + 1}{db.name ? ` – ${db.name}` : ''}
                </h5>
                <div className="flex flex-wrap items-end gap-2">
                  <Field label="Name" required tooltip="sgdbops.spec.vacuum.databases.name">
                    <TextInput value={db.name} required placeholder="Type a name..."
                      onChange={(value) => setVacuumDbs(vacuumDbs.map((d, i) => (i === index ? { ...d, name: value } : d)))} />
                  </Field>
                  {['full', 'freeze', 'analyze', 'disablePageSkipping'].map((key) => (
                    <Field key={key} label={VACUUM_DB_LABELS[key]} tooltip={VACUUM_DB_TOOLTIPS[key]}>
                      {dbSelect(db[key], (value) =>
                        setVacuumDbs(vacuumDbs.map((d, i) =>
                          i === index ? { ...d, [key]: value === 'inherit' ? 'inherit' : value === 'true' } : d)))}
                    </Field>
                  ))}
                  <button type="button" aria-label={`Delete database ${index + 1}`} className="p-1.5 rounded text-red-500 hover:bg-red-500/10"
                    onClick={() => setVacuumDbs(vacuumDbs.filter((_, i) => i !== index))}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          {vacuumPerDbs && (
            <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
              onClick={() =>
                setVacuumDbs([...vacuumDbs, { name: '', full: 'inherit', freeze: 'inherit', analyze: 'inherit', disablePageSkipping: 'inherit' }])}>
              <Plus size={14} /> Add Database
            </button>
          )}
        </FieldGroup>
      )}

      {op === 'repack' && (
        <FieldGroup title="Repack Details">
          <FieldRow>
            <Toggle label="Order" checked={!repack.noOrder} onChange={(enabled) => setRepack({ ...repack, noOrder: !enabled })}
              tooltip={<HelpTooltip field="sgdbops.spec.repack.noOrder" transform={NO_ORDER_TRANSFORM} />} />
            <Toggle label="Kill Backend" checked={!repack.noKillBackend} onChange={(enabled) => setRepack({ ...repack, noKillBackend: !enabled })}
              tooltip={<HelpTooltip field="sgdbops.spec.repack.noKillBackend" transform={NO_KILL_BACKEND_TRANSFORM} />} />
            <Toggle label="Analyze" checked={!repack.noAnalyze} onChange={(enabled) => setRepack({ ...repack, noAnalyze: !enabled })}
              tooltip={<HelpTooltip field="sgdbops.spec.repack.noAnalyze" transform={NO_KILL_BACKEND_TRANSFORM} />} />
            <Toggle label="Exclude Extension" checked={repack.excludeExtension}
              onChange={(excludeExtension) => setRepack({ ...repack, excludeExtension })}
              tooltip="sgdbops.spec.repack.excludeExtension" />
            <DurationSelects label="Wait Timeout" value={repack.waitTimeout}
              onChange={(waitTimeout) => setRepack({ ...repack, waitTimeout })}
              tooltip="sgdbops.spec.repack.waitTimeout" />
            <Toggle label="Database Specific Options" checked={repackPerDbs}
              tooltip="sgdbops.spec.repack.databases"
              onChange={(enabled) => {
                setRepackPerDbs(enabled)
                if (enabled && !repackDbs.length)
                  setRepackDbs([{ name: '', noOrder: 'inherit', inheritTimeout: true, waitTimeout: { d: 0, h: 0, m: 0, s: 0 }, noKillBackend: 'inherit', noAnalyze: 'inherit', excludeExtension: 'inherit' }])
              }} />
          </FieldRow>
          {repackPerDbs && <h3 className="text-sm font-semibold">Databases{renderTooltip('sgdbops.spec.repack.databases')}</h3>}
          {repackPerDbs &&
            repackDbs.map((db, index) => (
              <div key={index} className="flex flex-col gap-2 rounded-lg border p-4">
                <h3 className="text-sm font-semibold">
                  Database #{index + 1}{db.name ? ` – ${db.name}` : ''}
                </h3>
                <div className="flex flex-wrap items-end gap-2">
                  <Field label="Name" required tooltip="sgdbops.spec.repack.databases.name">
                    <TextInput value={db.name} required placeholder="Type a name..."
                      onChange={(value) => setRepackDbs(repackDbs.map((d, i) => (i === index ? { ...d, name: value } : d)))} />
                  </Field>
                  {['noOrder', 'noKillBackend', 'noAnalyze', 'excludeExtension'].map((key) => (
                    <Field key={key} label={REPACK_DB_LABELS[key]} tooltip={REPACK_DB_TOOLTIPS[key]}>
                      {dbSelect(db[key], (value) =>
                        setRepackDbs(repackDbs.map((d, i) =>
                          i === index ? { ...d, [key]: value === 'inherit' ? 'inherit' : value === 'true' } : d)))}
                    </Field>
                  ))}
                  <button type="button" aria-label={`Delete database ${index + 1}`} className="p-1.5 rounded text-red-500 hover:bg-red-500/10"
                    onClick={() => setRepackDbs(repackDbs.filter((_, i) => i !== index))}>
                    <Trash2 size={16} />
                  </button>
                </div>
                <Field label="Wait Timeout" tooltip="sgdbops.spec.repack.databases.waitTimeout">
                  <Select
                    value={db.inheritTimeout ? 'inherit' : 'custom'}
                    options={[
                      { value: 'inherit', label: 'Inherit from global settings' },
                      { value: 'custom', label: 'Set custom timeout' },
                    ]}
                    onChange={(value) =>
                      setRepackDbs(repackDbs.map((d, i) =>
                        i === index ? { ...d, inheritTimeout: value === 'inherit' } : d))} />
                </Field>
                {!db.inheritTimeout && (
                  <DurationSelects label="Custom Wait Timeout" tooltip="sgdbops.spec.repack.databases.waitTimeout" value={db.waitTimeout}
                    onChange={(waitTimeout) =>
                      setRepackDbs(repackDbs.map((d, i) => (i === index ? { ...d, waitTimeout } : d)))} />
                )}
              </div>
            ))}
          {repackPerDbs && (
            <button type="button" className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
              onClick={() =>
                setRepackDbs([
                  ...repackDbs,
                  {
                    name: '',
                    noOrder: 'inherit',
                    inheritTimeout: true,
                    waitTimeout: { d: 0, h: 0, m: 0, s: 0 },
                    noKillBackend: 'inherit',
                    noAnalyze: 'inherit',
                    excludeExtension: 'inherit',
                  },
                ])}>
              <Plus size={14} /> Add Database
            </button>
          )}
        </FieldGroup>
      )}

      {op === 'benchmark' && (
        <FieldGroup title="Benchmark Details">
          <FieldRow>
            <Field label="Type" tooltip="sgdbops.spec.benchmark.type">
              <Select value={benchmark.type} onChange={() => {}} options={['pgbench']} />
            </Field>
            <Field label="Connection Type" tooltip="sgdbops.spec.benchmark.connectionType">
              <Select value={benchmark.connectionType}
                onChange={(connectionType) => setBenchmark({ ...benchmark, connectionType })}
                options={[
                  { value: 'primary-service', label: 'Connect to the Primary Service' },
                  { value: 'replicas-service', label: 'Connect to the Replicas Service' },
                ]} />
            </Field>
          </FieldRow>
          <FieldRow>
            <Field label="Database Size" required error={form.errors.databaseSize} tooltip="sgdbops.spec.benchmark.pgbench.databaseSize">
              <div className="flex gap-2">
                <TextInput type="number" min="1" value={benchmark.pgbench.databaseSize.size} required
                  onChange={(size) =>
                    setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, databaseSize: { ...benchmark.pgbench.databaseSize, size } } })} />
                <Select value={benchmark.pgbench.databaseSize.unit}
                  onChange={(unit) =>
                    setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, databaseSize: { ...benchmark.pgbench.databaseSize, unit } } })}
                  options={['Mi', 'Gi', 'Ti']} />
              </div>
            </Field>
            <Toggle label="Prepared Statements" checked={benchmark.pgbench.usePreparedStatements}
              onChange={(usePreparedStatements) =>
                setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, usePreparedStatements } })}
              tooltip="sgdbops.spec.benchmark.pgbench.usePreparedStatements" />
            <Field label="Concurrent Clients" tooltip="sgdbops.spec.benchmark.pgbench.concurrentClients">
              <TextInput type="number" min="1" value={benchmark.pgbench.concurrentClients}
                onChange={(concurrentClients) =>
                  setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, concurrentClients } })} />
            </Field>
            <Field label="Threads" tooltip="sgdbops.spec.benchmark.pgbench.threads">
              <TextInput type="number" min="1" value={benchmark.pgbench.threads}
                onChange={(threads) => setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, threads } })} />
            </Field>
            <DurationSelects label="Duration" tooltip="sgdbops.spec.benchmark.pgbench.duration" value={benchmark.pgbench.duration}
              onChange={(duration) => setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, duration } })} />
          </FieldRow>
          <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
            The pgbench Benchmark Duration does not include database preparation that, depending on
            the selected size and environment, may take from some seconds to hours.
          </p>
        </FieldGroup>
      )}
    </CrdFormShell>
  )
}
