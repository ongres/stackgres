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
import { TolerationsEditor } from './clusterEditors'
import { Field, Select, TextInput, Toggle } from './fields'

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

const opLabel = (op) => op.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase())

function DurationSelects({ label, value, onChange }) {
  const part = (key, max, unit) => (
    <label className="flex items-center gap-1">
      <select
        value={String(value[key])}
        onChange={(event) => onChange({ ...value, [key]: event.target.value })}
        className="border p-1"
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
    <Field label={label}>
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
      <Field label="Operation Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} required error={form.errors.name} />
      </Field>
      {nameCollision && (
        <p role="alert">
          There&apos;s already a <strong>SGDbOps</strong> with the same name on this namespace.
          Please specify a different name or create the operation on another namespace.
        </p>
      )}
      <Field label="Target Cluster" required error={form.errors.sgCluster}>
        <Select value={sgCluster} onChange={setSgCluster} required placeholder="Choose a Cluster"
          options={clusters.map((c) => c.metadata.name)} />
      </Field>
      <fieldset className="flex flex-col gap-2 border p-3">
        <legend className="px-1 font-semibold">Database Operation *</legend>
        {form.errors.op && <p role="alert">{form.errors.op}</p>}
        <div className="flex flex-wrap gap-3">
          {OPS.map((candidate) => (
            <label key={candidate} className="flex items-center gap-2 border p-2">
              <input type="radio" name="op" value={candidate} checked={op === candidate}
                onChange={() => setOp(candidate)} />
              {opLabel(candidate)}
            </label>
          ))}
        </div>
      </fieldset>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">General Operation Details</legend>
        <Field label={`Run At (${timezone === 'local' ? 'local time' : 'UTC'})`} help="Leave empty to run as soon as possible">
          <input type="datetime-local" step="1" value={runAtLocal}
            onChange={(event) => setRunAtLocal(event.target.value)} className="border p-2" />
        </Field>
        {timezone === 'utc' && runAtLocal.length > 0 && (
          <p role="note">
            Bear in mind &quot;Run At&quot; times are expressed in UTC (Coordinated Universal Time).
          </p>
        )}
        <DurationSelects label="Timeout" value={timeout} onChange={setTimeoutValue} />
        <Field label="Max Retries">
          <Select value={maxRetries} onChange={setMaxRetries} options={Array.from({ length: 11 }, (_, i) => String(i))} />
        </Field>
      </fieldset>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Node Tolerations</legend>
        <TolerationsEditor tolerations={tolerations} onChange={setTolerations} />
      </fieldset>

      {op === 'restart' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Restart Details</legend>
          <Field label="Method">
            <Select value={restart.method} onChange={(method) => setRestart({ ...restart, method })} options={METHODS} />
          </Field>
          <Field label="Pods to Restart">
            <Toggle label="Pending Pods Only" checked={restart.onlyPendingRestart}
              onChange={(onlyPendingRestart) => setRestart({ ...restart, onlyPendingRestart })} />
          </Field>
        </fieldset>
      )}

      {op === 'securityUpgrade' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Security Upgrade Details</legend>
          <Field label="Method">
            <Select value={securityUpgrade.method} onChange={(method) => setSecurityUpgrade({ method })} options={METHODS} />
          </Field>
        </fieldset>
      )}

      {op === 'minorVersionUpgrade' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Minor Version Upgrade Details</legend>
          {!sgCluster ? (
            <p className="text-sm">
              You must first choose a target cluster to be able to set the upgrade details
            </p>
          ) : minorTargets.length ? (
            <>
              <Field label="Method">
                <Select value={minorVersionUpgrade.method}
                  onChange={(method) => setMinorVersionUpgrade({ ...minorVersionUpgrade, method })} options={METHODS} />
              </Field>
              <Field label="Target Postgres Version" required error={form.errors.targetVersion}>
                <Select value={minorVersionUpgrade.postgresVersion}
                  onChange={(postgresVersion) => setMinorVersionUpgrade({ ...minorVersionUpgrade, postgresVersion })}
                  options={minorTargets} placeholder="Choose version..." required />
              </Field>
            </>
          ) : (
            <p className="text-sm">
              Your cluster is already set to use Postgres {currentVersion} which is the latest minor
              version available for Postgres {currentMajor}. If you prefer, you could perform a Major
              Version Upgrade to Postgres {Number(currentMajor) + 1}.
            </p>
          )}
        </fieldset>
      )}

      {op === 'majorVersionUpgrade' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Major Version Upgrade Details</legend>
          {!sgCluster ? (
            <p className="text-sm">
              You must first choose a target cluster to be able to set the upgrade details
            </p>
          ) : majorTargets.length ? (
            <>
              <Toggle label="Hard Link Files" checked={majorVersionUpgrade.link}
                onChange={(link) => setMajorVersionUpgrade({ ...majorVersionUpgrade, link })} />
              <Toggle label="Clone Files" checked={majorVersionUpgrade.clone}
                onChange={(clone) => setMajorVersionUpgrade({ ...majorVersionUpgrade, clone })} />
              <Toggle label="Check Clusters" checked={majorVersionUpgrade.check}
                onChange={(check) => setMajorVersionUpgrade({ ...majorVersionUpgrade, check })} />
              <Field label="Backup Path">
                <TextInput value={majorVersionUpgrade.backupPath || ''}
                  onChange={(backupPath) =>
                    setMajorVersionUpgrade({ ...majorVersionUpgrade, backupPath: backupPath.length ? backupPath : null })} />
              </Field>
              <Field label="Target Postgres Version" required error={form.errors.targetVersion}>
                <Select value={majorVersionUpgrade.postgresVersion}
                  onChange={(postgresVersion) =>
                    setMajorVersionUpgrade({ ...majorVersionUpgrade, postgresVersion, sgPostgresConfig: '' })}
                  options={majorTargets} placeholder="Choose version..." required />
              </Field>
              {majorVersionUpgrade.postgresVersion && (
                <Field label="Target Postgres Configuration" required error={form.errors.targetConfig}>
                  <Select value={majorVersionUpgrade.sgPostgresConfig}
                    onChange={(sgPostgresConfig) => setMajorVersionUpgrade({ ...majorVersionUpgrade, sgPostgresConfig })}
                    options={pgConfigs.map((c) => c.metadata.name)} placeholder="Choose config..." required />
                </Field>
              )}
            </>
          ) : (
            <p className="text-sm">
              Your cluster is already set to use Postgres {currentMajor} which is the latest major
              version available.
            </p>
          )}
        </fieldset>
      )}

      {op === 'vacuum' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Vacuum Details</legend>
          <Toggle label="Full Vacuum" checked={vacuum.full} onChange={(full) => setVacuum({ ...vacuum, full })} />
          <Toggle label="Freeze" checked={vacuum.freeze} onChange={(freeze) => setVacuum({ ...vacuum, freeze })} />
          <Toggle label="Analyze" checked={vacuum.analyze} onChange={(analyze) => setVacuum({ ...vacuum, analyze })} />
          <Toggle label="Page Skipping" checked={!vacuum.disablePageSkipping}
            onChange={(enabled) => setVacuum({ ...vacuum, disablePageSkipping: !enabled })} />
          <Toggle label="Database Specific Options" checked={vacuumPerDbs}
            onChange={(enabled) => {
              setVacuumPerDbs(enabled)
              if (enabled && !vacuumDbs.length)
                setVacuumDbs([{ name: '', full: 'inherit', freeze: 'inherit', analyze: 'inherit', disablePageSkipping: 'inherit' }])
            }} />
          {vacuumPerDbs && <h4 className="font-semibold">Databases</h4>}
          {vacuumPerDbs &&
            vacuumDbs.map((db, index) => (
              <div key={index} className="flex flex-col gap-2 border p-2">
                <h5 className="font-semibold">
                  Database #{index + 1}{db.name ? ` – ${db.name}` : ''}
                </h5>
                <div className="flex flex-wrap items-end gap-2">
                  <Field label="Name" required>
                    <TextInput value={db.name} required placeholder="Type a name..."
                      onChange={(value) => setVacuumDbs(vacuumDbs.map((d, i) => (i === index ? { ...d, name: value } : d)))} />
                  </Field>
                  {['full', 'freeze', 'analyze', 'disablePageSkipping'].map((key) => (
                    <Field key={key} label={VACUUM_DB_LABELS[key]}>
                      {dbSelect(db[key], (value) =>
                        setVacuumDbs(vacuumDbs.map((d, i) =>
                          i === index ? { ...d, [key]: value === 'inherit' ? 'inherit' : value === 'true' } : d)))}
                    </Field>
                  ))}
                  <button type="button" aria-label={`Delete database ${index + 1}`}
                    onClick={() => setVacuumDbs(vacuumDbs.filter((_, i) => i !== index))}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          {vacuumPerDbs && (
            <button type="button" className="flex items-center gap-1 self-start text-sm underline"
              onClick={() =>
                setVacuumDbs([...vacuumDbs, { name: '', full: 'inherit', freeze: 'inherit', analyze: 'inherit', disablePageSkipping: 'inherit' }])}>
              <Plus size={14} /> Add Database
            </button>
          )}
        </fieldset>
      )}

      {op === 'repack' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Repack Details</legend>
          <Toggle label="Order" checked={!repack.noOrder} onChange={(enabled) => setRepack({ ...repack, noOrder: !enabled })} />
          <Toggle label="Kill Backend" checked={!repack.noKillBackend} onChange={(enabled) => setRepack({ ...repack, noKillBackend: !enabled })} />
          <Toggle label="Analyze" checked={!repack.noAnalyze} onChange={(enabled) => setRepack({ ...repack, noAnalyze: !enabled })} />
          <Toggle label="Exclude Extension" checked={repack.excludeExtension}
            onChange={(excludeExtension) => setRepack({ ...repack, excludeExtension })} />
          <DurationSelects label="Wait Timeout" value={repack.waitTimeout}
            onChange={(waitTimeout) => setRepack({ ...repack, waitTimeout })} />
          <Toggle label="Database Specific Options" checked={repackPerDbs}
            onChange={(enabled) => {
              setRepackPerDbs(enabled)
              if (enabled && !repackDbs.length)
                setRepackDbs([{ name: '', noOrder: 'inherit', inheritTimeout: true, waitTimeout: { d: 0, h: 0, m: 0, s: 0 }, noKillBackend: 'inherit', noAnalyze: 'inherit', excludeExtension: 'inherit' }])
            }} />
          {repackPerDbs && <h3 className="font-semibold">Databases</h3>}
          {repackPerDbs &&
            repackDbs.map((db, index) => (
              <div key={index} className="flex flex-col gap-2 border p-2">
                <h3 className="font-semibold">
                  Database #{index + 1}{db.name ? ` – ${db.name}` : ''}
                </h3>
                <div className="flex flex-wrap items-end gap-2">
                  <Field label="Name" required>
                    <TextInput value={db.name} required placeholder="Type a name..."
                      onChange={(value) => setRepackDbs(repackDbs.map((d, i) => (i === index ? { ...d, name: value } : d)))} />
                  </Field>
                  {['noOrder', 'noKillBackend', 'noAnalyze', 'excludeExtension'].map((key) => (
                    <Field key={key} label={REPACK_DB_LABELS[key]}>
                      {dbSelect(db[key], (value) =>
                        setRepackDbs(repackDbs.map((d, i) =>
                          i === index ? { ...d, [key]: value === 'inherit' ? 'inherit' : value === 'true' } : d)))}
                    </Field>
                  ))}
                  <button type="button" aria-label={`Delete database ${index + 1}`}
                    onClick={() => setRepackDbs(repackDbs.filter((_, i) => i !== index))}>
                    <Trash2 size={16} />
                  </button>
                </div>
                <Field label="Wait Timeout">
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
                  <DurationSelects label="Custom Wait Timeout" value={db.waitTimeout}
                    onChange={(waitTimeout) =>
                      setRepackDbs(repackDbs.map((d, i) => (i === index ? { ...d, waitTimeout } : d)))} />
                )}
              </div>
            ))}
          {repackPerDbs && (
            <button type="button" className="flex items-center gap-1 self-start text-sm underline"
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
        </fieldset>
      )}

      {op === 'benchmark' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Benchmark Details</legend>
          <Field label="Type">
            <Select value={benchmark.type} onChange={() => {}} options={['pgbench']} />
          </Field>
          <Field label="Connection Type">
            <Select value={benchmark.connectionType}
              onChange={(connectionType) => setBenchmark({ ...benchmark, connectionType })}
              options={[
                { value: 'primary-service', label: 'Connect to the Primary Service' },
                { value: 'replicas-service', label: 'Connect to the Replicas Service' },
              ]} />
          </Field>
          <Field label="Database Size" required error={form.errors.databaseSize}>
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
              setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, usePreparedStatements } })} />
          <Field label="Concurrent Clients">
            <TextInput type="number" min="1" value={benchmark.pgbench.concurrentClients}
              onChange={(concurrentClients) =>
                setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, concurrentClients } })} />
          </Field>
          <Field label="Threads">
            <TextInput type="number" min="1" value={benchmark.pgbench.threads}
              onChange={(threads) => setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, threads } })} />
          </Field>
          <DurationSelects label="Duration" value={benchmark.pgbench.duration}
            onChange={(duration) => setBenchmark({ ...benchmark, pgbench: { ...benchmark.pgbench, duration } })} />
          <p role="note">
            The pgbench Benchmark Duration does not include database preparation that, depending on
            the selected size and environment, may take from some seconds to hours.
          </p>
        </fieldset>
      )}
    </CrdFormShell>
  )
}
