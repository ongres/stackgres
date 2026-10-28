import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Plus, Trash2 } from 'lucide-react'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildInstanceProfile } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextInput } from './fields'

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

function QuantityInput({ label, value, unit, units, onValue, onUnit, required, error }) {
  return (
    <Field label={label} required={required} error={error}>
      <div className="flex gap-2">
        <TextInput type="number" min="0" value={value} onChange={onValue} required={required} />
        <Select value={unit} onChange={onUnit} options={units} />
      </div>
    </Field>
  )
}

function ContainerRows({ title, rows, setRows }) {
  const update = (index, patch) =>
    setRows(rows.map((row, i) => (i === index ? { ...row, ...patch } : row)))
  return (
    <fieldset className="flex flex-col gap-2 border p-3">
      <legend className="px-1 font-semibold">{title}</legend>
      {rows.map((row, index) => (
        <div key={index} className="flex flex-wrap items-end gap-2">
          <Field label="Container Name">
            <TextInput
              value={row.name}
              onChange={(name) => update(index, { name })}
              required={row.RAM.length > 0 || row.CPU.length > 0}
            />
          </Field>
          <QuantityInput
            label="RAM"
            value={row.RAM}
            unit={row.RAMUnit}
            units={MEMORY_UNITS}
            onValue={(RAM) => update(index, { RAM })}
            onUnit={(RAMUnit) => update(index, { RAMUnit })}
            required={row.name.length > 0 || row.CPU.length > 0}
          />
          <QuantityInput
            label="CPU"
            value={row.CPU}
            unit={row.CPUUnit}
            units={CPU_UNITS}
            onValue={(CPU) => update(index, { CPU })}
            onUnit={(CPUUnit) => update(index, { CPUUnit })}
            required={row.name.length > 0 || row.RAM.length > 0}
          />
          <button
            type="button"
            onClick={() => setRows(rows.filter((_, i) => i !== index))}
            title="Delete row"
            aria-label={`Delete ${title} row ${index + 1}`}
          >
            <Trash2 size={16} />
          </button>
        </div>
      ))}
      <button
        type="button"
        onClick={() => setRows([...rows, blankContainer()])}
        className="flex items-center gap-1 self-start text-sm underline"
      >
        <Plus size={14} /> Add row
      </button>
    </fieldset>
  )
}

export default function InstanceProfileForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sginstanceprofiles', namespace, routeName, isEdit)

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
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <CrdFormShell title={isEdit ? `Edit Profile: ${routeName}` : 'Create Instance Profile'} form={form}>
      <Field label="Profile Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      <div className="flex flex-wrap gap-4">
        <QuantityInput label="RAM" value={ram} unit={ramUnit} units={MEMORY_UNITS} onValue={setRam} onUnit={setRamUnit} required error={form.errors.ram} />
        <QuantityInput label="CPU" value={cpu} unit={cpuUnit} units={CPU_UNITS} onValue={setCpu} onUnit={setCpuUnit} required error={form.errors.cpu} />
      </div>
      <div className="flex flex-wrap gap-4">
        <QuantityInput label="Huge Pages 2Mi" value={hugePages2Mi} unit={hugePages2MiUnit} units={MEMORY_UNITS} onValue={setHugePages2Mi} onUnit={setHugePages2MiUnit} />
        <QuantityInput label="Huge Pages 1Gi" value={hugePages1Gi} unit={hugePages1GiUnit} units={MEMORY_UNITS} onValue={setHugePages1Gi} onUnit={setHugePages1GiUnit} />
      </div>
      <ContainerRows title="Containers" rows={containers} setRows={setContainers} />
      <ContainerRows title="Init Containers" rows={initContainers} setRows={setInitContainers} />
      <fieldset className="flex flex-col gap-2 border p-3">
        <legend className="px-1 font-semibold">Requests</legend>
        <div className="flex flex-wrap gap-4">
          <QuantityInput label="RAM" value={requests.RAM} unit={requests.RAMUnit} units={MEMORY_UNITS}
            onValue={(RAM) => setRequests({ ...requests, RAM })}
            onUnit={(RAMUnit) => setRequests({ ...requests, RAMUnit })} />
          <QuantityInput label="CPU" value={requests.CPU} unit={requests.CPUUnit} units={CPU_UNITS}
            onValue={(CPU) => setRequests({ ...requests, CPU })}
            onUnit={(CPUUnit) => setRequests({ ...requests, CPUUnit })} />
        </div>
        <ContainerRows title="Containers Requests" rows={requests.containers}
          setRows={(rows) => setRequests({ ...requests, containers: rows })} />
        <ContainerRows title="Init Containers Requests" rows={requests.initContainers}
          setRows={(rows) => setRequests({ ...requests, initContainers: rows })} />
      </fieldset>
    </CrdFormShell>
  )
}
