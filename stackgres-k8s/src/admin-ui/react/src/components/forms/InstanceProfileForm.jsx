import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus } from 'lucide-react'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildInstanceProfile } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, FieldGroup, FieldRow, Select, TextInput, renderTooltip } from './fields'

const MEMORY_UNITS = ['Mi', 'Gi']
const CPU_UNITS = ['CPU', 'm']

const blankContainer = () => ({
  name: '',
  CPU: '',
  CPUUnit: 'CPU',
  RAM: '',
  RAMUnit: 'Gi',
  hugePages1Gi: '',
  hugePages1GiUnit: 'Gi',
  hugePages2Mi: '',
  hugePages2MiUnit: 'Gi',
})

function unparseQuantity(quantity, units, fallbackUnit) {
  const text = String(quantity ?? '')
  const unit = units.find((candidate) => candidate !== 'CPU' && text.endsWith(candidate))
  return unit
    ? { value: text.slice(0, -unit.length), unit }
    : { value: text, unit: fallbackUnit }
}

function unparseContainers(map) {
  if (!map || !Object.keys(map).length) {
    return [blankContainer()]
  }
  return Object.entries(map).map(([name, spec]) => {
    const cpu = unparseQuantity(spec.cpu, CPU_UNITS, 'CPU')
    const ram = unparseQuantity(spec.memory, MEMORY_UNITS, 'Gi')
    const hp2 = unparseQuantity(spec.hugePages?.['hugepages-2Mi'], MEMORY_UNITS, 'Gi')
    const hp1 = unparseQuantity(spec.hugePages?.['hugepages-1Gi'], MEMORY_UNITS, 'Gi')
    return {
      name,
      CPU: cpu.value,
      CPUUnit: cpu.unit,
      RAM: ram.value,
      RAMUnit: ram.unit,
      hugePages2Mi: hp2.value,
      hugePages2MiUnit: hp2.unit,
      hugePages1Gi: hp1.value,
      hugePages1GiUnit: hp1.unit,
    }
  })
}

function QuantityInput({ label, value, unit, units, onValue, onUnit, required, error, tooltip }) {
  return (
    <Field label={label} required={required} error={error} tooltip={tooltip}>
      <div className="flex gap-2">
        <TextInput type="number" min="0" value={value} onChange={onValue} required={required} />
        <Select value={unit} onChange={onUnit} options={units} />
      </div>
    </Field>
  )
}

function ContainerRows({ title, rows, setRows, hugePages = false, tooltip, keyPrefix }) {
  const update = (index, patch) =>
    setRows(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  return (
    <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
      <legend className="px-2 text-sm font-medium">{title}{renderTooltip(tooltip)}</legend>
      {rows.map((row, index) => (
        <div key={index} className="flex flex-col gap-2 rounded-lg border p-4">
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-sm font-semibold">
              Container #{index + 1}{row.name.length ? `: ${row.name}` : ''}
            </h3>
            <button
              type="button"
              onClick={() => setRows(rows.filter((_, i) => i !== index))}
              className="text-sm text-red-500 hover:underline"
              aria-label={`Delete ${title} row ${index + 1}`}
            >
              Delete Container
            </button>
          </div>
          <FieldRow>
            <Field label="Container Name">
              <TextInput
                value={row.name}
                onChange={(name) => update(index, { name })}
                required={row.RAM.length > 0 || row.CPU.length > 0}
              />
            </Field>
          </FieldRow>
          <FieldGroup title="Container Specs">
            <FieldRow>
              <QuantityInput
                label="RAM"
                value={row.RAM}
                unit={row.RAMUnit}
                units={MEMORY_UNITS}
                onValue={(RAM) => update(index, { RAM })}
                onUnit={(RAMUnit) => update(index, { RAMUnit })}
                required={row.name.length > 0 || row.CPU.length > 0}
                tooltip={keyPrefix && keyPrefix + '.memory'}
              />
              <QuantityInput
                label="CPU"
                value={row.CPU}
                unit={row.CPUUnit}
                units={CPU_UNITS}
                onValue={(CPU) => update(index, { CPU })}
                onUnit={(CPUUnit) => update(index, { CPUUnit })}
                required={row.name.length > 0 || row.RAM.length > 0}
                tooltip={keyPrefix && keyPrefix + '.cpu'}
              />
            </FieldRow>
          </FieldGroup>
          {hugePages && (
            <FieldGroup
              title={
                <>Container Huge Pages Specs{renderTooltip(keyPrefix && keyPrefix + '.hugePages')}</>
              }
            >
              <FieldRow>
                <QuantityInput
                  label="Huge Pages 2Mi"
                  value={row.hugePages2Mi}
                  unit={row.hugePages2MiUnit}
                  units={MEMORY_UNITS}
                  onValue={(hugePages2Mi) => update(index, { hugePages2Mi })}
                  onUnit={(hugePages2MiUnit) => update(index, { hugePages2MiUnit })}
                  tooltip={keyPrefix && keyPrefix + '.hugePages.properties.hugepages-2Mi'}
                />
                <QuantityInput
                  label="Huge Pages 1Gi"
                  value={row.hugePages1Gi}
                  unit={row.hugePages1GiUnit}
                  units={MEMORY_UNITS}
                  onValue={(hugePages1Gi) => update(index, { hugePages1Gi })}
                  onUnit={(hugePages1GiUnit) => update(index, { hugePages1GiUnit })}
                  tooltip={keyPrefix && keyPrefix + '.hugePages.properties.hugepages-1Gi'}
                />
              </FieldRow>
            </FieldGroup>
          )}
        </div>
      ))}
      <button
        type="button"
        onClick={() => setRows([...rows, blankContainer()])}
        className="flex items-center gap-1 self-start rounded-md px-1.5 py-1 text-sm text-[hsl(var(--primary))] transition-colors hover:bg-[hsl(var(--primary)/0.1)]"
      >
        <Plus size={14} /> Add Container
      </button>
    </fieldset>
  )
}

const FORM_STEPS = [
  ['profile', 'Profile'],
  ['containers', 'Containers'],
  ['initContainers', 'Init Containers'],
  ['requests', 'Requests'],
]

export default function InstanceProfileForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sginstanceprofiles', namespace, routeName, isEdit)
  const data = useData()
  const [currentStep, setCurrentStep] = useState('profile')

  const [name, setName] = useState(routeName || '')
  const [ram, setRam] = useState('')
  const [ramUnit, setRamUnit] = useState('Gi')
  const [cpu, setCpu] = useState('')
  const [cpuUnit, setCpuUnit] = useState('CPU')
  const [hugePages2Mi, setHugePages2Mi] = useState('')
  const [hugePages2MiUnit, setHugePages2MiUnit] = useState('Gi')
  const [hugePages1Gi, setHugePages1Gi] = useState('')
  const [hugePages1GiUnit, setHugePages1GiUnit] = useState('Gi')
  const [containers, setContainers] = useState([blankContainer()])
  const [initContainers, setInitContainers] = useState([blankContainer()])
  const [requests, setRequests] = useState({
    RAM: '',
    RAMUnit: 'Gi',
    CPU: '',
    CPUUnit: 'CPU',
    containers: [blankContainer()],
    initContainers: [blankContainer()],
  })
  const [seeded, setSeeded] = useState(false)

  if (isEdit && loaded && previous && !seeded) {
    const spec = previous.spec || {}
    const specCpu = unparseQuantity(spec.cpu, CPU_UNITS, 'CPU')
    const specRam = unparseQuantity(spec.memory, MEMORY_UNITS, 'Gi')
    const hp2 = unparseQuantity(spec.hugePages?.['hugepages-2Mi'], MEMORY_UNITS, 'Gi')
    const hp1 = unparseQuantity(spec.hugePages?.['hugepages-1Gi'], MEMORY_UNITS, 'Gi')
    setCpu(specCpu.value)
    setCpuUnit(specCpu.unit)
    setRam(specRam.value)
    setRamUnit(specRam.unit)
    setHugePages2Mi(hp2.value)
    setHugePages2MiUnit(hp2.unit)
    setHugePages1Gi(hp1.value)
    setHugePages1GiUnit(hp1.unit)
    setContainers(unparseContainers(spec.containers))
    setInitContainers(unparseContainers(spec.initContainers))
    setRequests((current) => ({
      ...current,
      RAM: unparseQuantity(spec.requests?.memory, MEMORY_UNITS, 'Gi').value,
      RAMUnit: unparseQuantity(spec.requests?.memory, MEMORY_UNITS, 'Gi').unit,
      CPU: unparseQuantity(spec.requests?.cpu, CPU_UNITS, 'CPU').value,
      CPUUnit: unparseQuantity(spec.requests?.cpu, CPU_UNITS, 'CPU').unit,
      containers: unparseContainers(spec.requests?.containers),
      initContainers: unparseContainers(spec.requests?.initContainers),
    }))
    setSeeded(true)
  }

  const buildPayload = () =>
    buildInstanceProfile(
      {
        name,
        namespace,
        cpu,
        cpuUnit,
        ram,
        ramUnit,
        hugePages2Mi,
        hugePages2MiUnit,
        hugePages1Gi,
        hugePages1GiUnit,
        containers,
        initContainers,
        requests,
      },
      previous || {}
    )

  const form = useCrdForm({
    kind: 'sginstanceprofiles',
    isEdit,
    buildPayload,
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!ram.length) errors.ram = 'RAM is required'
      if (!cpu.length) errors.cpu = 'CPU is required'
      return errors
    },
    successPath: (payload) =>
      isEdit
        ? `/${namespace}/sginstanceprofile/${payload.metadata.name}`
        : `/${namespace}/sginstanceprofiles`,
  })

  if (isEdit && !loaded) {
    return <p className="p-6 text-sm text-[hsl(var(--muted-foreground))] italic">Loading data…</p>
  }

  const nameCollision =
    !isEdit &&
    (data.sginstanceprofiles || []).some(
      (profile) => profile.metadata.name === name && profile.metadata.namespace === namespace
    )
  const clusters = (data.sgclusters || []).filter(
    (cluster) =>
      cluster.metadata.namespace === namespace && cluster.spec?.sgInstanceProfile === routeName
  )

  return (
    <CrdFormShell title={isEdit ? `Edit Profile: ${routeName}` : 'Create Instance Profile'} form={form}>
      {isEdit && clusters.length > 0 && (
        <p role="note" className="flex items-center gap-3 border border-yellow-500/30 bg-yellow-500/10 text-yellow-600 dark:text-yellow-400 rounded px-3 py-2 text-sm">
          Please, be aware that any changes made to this instance profile will require a{' '}
          <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank" rel="noreferrer"
            className="text-[hsl(var(--primary))] hover:underline">
            restart operation
          </a>{' '}
          on every instance on the following cluster(s):{' '}
          {clusters.map((cluster) => cluster.metadata.name).join(', ')}
        </p>
      )}
      <nav aria-label="Form steps" className="flex flex-wrap gap-1 border-b pb-2 text-sm">
        {FORM_STEPS.map(([step, label]) => (
          <button key={step} type="button" onClick={() => setCurrentStep(step)}
            className={`px-2.5 py-1 rounded-md transition-colors ${currentStep === step ? 'bg-[hsl(var(--primary)/0.15)] text-[hsl(var(--primary))] font-medium' : 'text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))]'}`}>
            {label}
          </button>
        ))}
      </nav>
      {currentStep === 'profile' && (
        <>
          <FieldGroup title="Profile Information">
            <FieldRow>
              <Field label="Profile Name" required error={form.errors.name} tooltip="sgprofile.metadata.name">
                <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
              </Field>
              <QuantityInput label="RAM" value={ram} unit={ramUnit} units={MEMORY_UNITS} onValue={setRam} onUnit={setRamUnit} required error={form.errors.ram} tooltip="sgprofile.spec.memory" />
              <QuantityInput label="CPU" value={cpu} unit={cpuUnit} units={CPU_UNITS} onValue={setCpu} onUnit={setCpuUnit} required error={form.errors.cpu} tooltip="sgprofile.spec.cpu" />
            </FieldRow>
            {nameCollision && (
              <p role="alert" className="text-sm text-red-500">
                There&apos;s already a <strong>SGInstanceProfile</strong> with the same name on this
                namespace. Please specify a different name or create the profile on another namespace
              </p>
            )}
          </FieldGroup>
          <FieldGroup title={<>Huge Pages Specs{renderTooltip('sgprofile.spec.hugePages')}</>}>
            <FieldRow>
              <QuantityInput label="Huge Pages 2Mi" value={hugePages2Mi} unit={hugePages2MiUnit} units={MEMORY_UNITS} onValue={setHugePages2Mi} onUnit={setHugePages2MiUnit} tooltip="sgprofile.spec.hugePages.hugepages-2Mi" />
              <QuantityInput label="Huge Pages 1Gi" value={hugePages1Gi} unit={hugePages1GiUnit} units={MEMORY_UNITS} onValue={setHugePages1Gi} onUnit={setHugePages1GiUnit} tooltip="sgprofile.spec.hugePages.hugepages-1Gi" />
            </FieldRow>
          </FieldGroup>
        </>
      )}
      {currentStep === 'containers' && (
        <ContainerRows title="Containers" rows={containers} setRows={setContainers} hugePages
          tooltip="sgprofile.spec.containers"
          keyPrefix="sgprofile.spec.containers.additionalProperties.properties" />
      )}
      {currentStep === 'initContainers' && (
        <ContainerRows title="Init Containers" rows={initContainers} setRows={setInitContainers} hugePages
          tooltip="sgprofile.spec.initContainers"
          keyPrefix="sgprofile.spec.initContainers.additionalProperties.properties" />
      )}
      {currentStep === 'requests' && (
        <fieldset className="flex flex-col gap-2 rounded-lg border p-4">
          <legend className="px-2 text-sm font-medium">Requests{renderTooltip('sgprofile.spec.requests')}</legend>
          <FieldRow>
            <QuantityInput label="RAM" value={requests.RAM} unit={requests.RAMUnit} units={MEMORY_UNITS}
              onValue={(RAM) => setRequests({ ...requests, RAM })}
              onUnit={(RAMUnit) => setRequests({ ...requests, RAMUnit })}
              tooltip="sgprofile.spec.requests.memory" />
            <QuantityInput label="CPU" value={requests.CPU} unit={requests.CPUUnit} units={CPU_UNITS}
              onValue={(CPU) => setRequests({ ...requests, CPU })}
              onUnit={(CPUUnit) => setRequests({ ...requests, CPUUnit })}
              tooltip="sgprofile.spec.requests.cpu" />
          </FieldRow>
          <ContainerRows title="Containers Specs" rows={requests.containers}
            setRows={(rows) => setRequests({ ...requests, containers: rows })}
            tooltip="sgprofile.spec.requests.containers"
            keyPrefix="sgprofile.spec.requests.containers.additionalProperties.properties" />
          <ContainerRows title="Init Containers Specs" rows={requests.initContainers}
            setRows={(rows) => setRequests({ ...requests, initContainers: rows })}
            tooltip="sgprofile.spec.requests.initContainers"
            keyPrefix="sgprofile.spec.requests.initContainers.additionalProperties.properties" />
        </fieldset>
      )}
    </CrdFormShell>
  )
}
