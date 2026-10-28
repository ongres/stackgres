import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../../lib/api'
import { useData } from '../../contexts/DataContext'
import { useSettings } from '../../contexts/SettingsContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { buildDbOps } from '../../lib/payloads'
import { generatedName, nameValidationMessage } from '../../lib/names'
import { prefix } from '../../lib/format'
import CrdFormShell from './CrdFormShell'
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
const METHODS = ['InPlace', 'ReducedImpact']
const INHERIT = [
  { value: 'inherit', label: 'Inherit' },
  { value: 'true', label: 'Yes' },
  { value: 'false', label: 'No' },
]

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
function runAtToIso(value, timezone) {
  if (!value) return ''
  if (timezone === 'utc') {
    return `${value}:00Z`.replace(/:00:00Z$/, ':00Z')
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toISOString().replace(/\.\d{3}Z$/, 'Z')
}

export default function DbOpsForm() {
  const { namespace } = useParams()
  const { timezone } = useSettings()
  const data = useData()

  const [name, setName] = useState(generatedName('op', timezone))
  const [sgCluster, setSgCluster] = useState('')
  const [op, setOp] = useState('')
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
        tolerations: [],
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
      <Field label="Target Cluster" required error={form.errors.sgCluster}>
        <Select value={sgCluster} onChange={setSgCluster} required placeholder="Select a cluster…"
          options={clusters.map((c) => c.metadata.name)} />
      </Field>
      <Field label="Database Operation" required error={form.errors.op}>
        <Select value={op} onChange={setOp} required placeholder="Select an operation…"
          options={OPS.map((o) => ({ value: o, label: opLabel(o) }))} />
      </Field>

      <fieldset className="flex flex-col gap-3 border p-3">
        <legend className="px-1 font-semibold">Schedule</legend>
        <Field label={`Run At (${timezone === 'local' ? 'local time' : 'UTC'})`} help="Leave empty to run as soon as possible">
          <input type="datetime-local" step="1" value={runAtLocal}
            onChange={(event) => setRunAtLocal(event.target.value)} className="border p-2" />
        </Field>
        <DurationSelects label="Timeout" value={timeout} onChange={setTimeoutValue} />
        <Field label="Max Retries">
          <Select value={maxRetries} onChange={setMaxRetries} options={Array.from({ length: 11 }, (_, i) => String(i))} />
        </Field>
      </fieldset>

      {op === 'restart' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Restart</legend>
          <Field label="Method">
            <Select value={restart.method} onChange={(method) => setRestart({ ...restart, method })} options={METHODS} />
          </Field>
          <Toggle label="Pending Pods Only" checked={restart.onlyPendingRestart}
            onChange={(onlyPendingRestart) => setRestart({ ...restart, onlyPendingRestart })} />
        </fieldset>
      )}

      {op === 'securityUpgrade' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Security Upgrade</legend>
          <Field label="Method">
            <Select value={securityUpgrade.method} onChange={(method) => setSecurityUpgrade({ method })} options={METHODS} />
          </Field>
        </fieldset>
      )}

      {op === 'minorVersionUpgrade' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Minor Version Upgrade</legend>
          {!sgCluster ? (
            <p className="text-sm">You must first choose a target cluster</p>
          ) : minorTargets.length ? (
            <>
              <Field label="Method">
                <Select value={minorVersionUpgrade.method}
                  onChange={(method) => setMinorVersionUpgrade({ ...minorVersionUpgrade, method })} options={METHODS} />
              </Field>
              <Field label="Target Postgres Version" required error={form.errors.targetVersion}>
                <Select value={minorVersionUpgrade.postgresVersion}
                  onChange={(postgresVersion) => setMinorVersionUpgrade({ ...minorVersionUpgrade, postgresVersion })}
                  options={minorTargets} placeholder="Select a version…" required />
              </Field>
            </>
          ) : (
            <p className="text-sm">The cluster is already on the latest minor version</p>
          )}
        </fieldset>
      )}

      {op === 'majorVersionUpgrade' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Major Version Upgrade</legend>
          {!sgCluster ? (
            <p className="text-sm">You must first choose a target cluster</p>
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
                  options={majorTargets} placeholder="Select a version…" required />
              </Field>
              {majorVersionUpgrade.postgresVersion && (
                <Field label="Target Postgres Configuration" required error={form.errors.targetConfig}>
                  <Select value={majorVersionUpgrade.sgPostgresConfig}
                    onChange={(sgPostgresConfig) => setMajorVersionUpgrade({ ...majorVersionUpgrade, sgPostgresConfig })}
                    options={pgConfigs.map((c) => c.metadata.name)} placeholder="Select a configuration…" required />
                </Field>
              )}
            </>
          ) : (
            <p className="text-sm">No higher major version is available for this cluster</p>
          )}
        </fieldset>
      )}

      {op === 'vacuum' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Vacuum</legend>
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
          {vacuumPerDbs &&
            vacuumDbs.map((db, index) => (
              <div key={index} className="flex flex-wrap items-end gap-2 border p-2">
                <Field label="Database Name" required>
                  <TextInput value={db.name} required
                    onChange={(value) => setVacuumDbs(vacuumDbs.map((d, i) => (i === index ? { ...d, name: value } : d)))} />
                </Field>
                {['full', 'freeze', 'analyze', 'disablePageSkipping'].map((key) => (
                  <Field key={key} label={key}>
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
          <legend className="px-1 font-semibold">Repack</legend>
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
          {repackPerDbs &&
            repackDbs.map((db, index) => (
              <div key={index} className="flex flex-wrap items-end gap-2 border p-2">
                <Field label="Database Name" required>
                  <TextInput value={db.name} required
                    onChange={(value) => setRepackDbs(repackDbs.map((d, i) => (i === index ? { ...d, name: value } : d)))} />
                </Field>
                {['noOrder', 'noKillBackend', 'noAnalyze', 'excludeExtension'].map((key) => (
                  <Field key={key} label={key}>
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
            ))}
        </fieldset>
      )}

      {op === 'benchmark' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Benchmark</legend>
          <Field label="Type">
            <Select value={benchmark.type} onChange={() => {}} options={['pgbench']} />
          </Field>
          <Field label="Connection Type">
            <Select value={benchmark.connectionType}
              onChange={(connectionType) => setBenchmark({ ...benchmark, connectionType })}
              options={['primary-service', 'replicas-service']} />
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
        </fieldset>
      )}
    </CrdFormShell>
  )
}
