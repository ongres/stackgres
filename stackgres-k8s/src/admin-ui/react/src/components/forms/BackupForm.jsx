import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useData } from '../../contexts/DataContext'
import { useSettings } from '../../contexts/SettingsContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildBackup } from '../../lib/payloads'
import { generatedName, nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextInput, Toggle } from './fields'

export default function BackupForm({ isEdit }) {
  const { namespace, backupname, name: clusterParam } = useParams()
  const { timezone } = useSettings()
  const data = useData()
  const { previous, loaded } = useExistingResource('sgbackups', namespace, backupname, isEdit)

  const [name, setName] = useState(backupname || generatedName('bk', timezone))
  const [sgCluster, setSgCluster] = useState(clusterParam || '')
  const [managedLifecycle, setManagedLifecycle] = useState(false)
  const [seeded, setSeeded] = useState(false)

  if (isEdit && loaded && previous && !seeded) {
    setSgCluster(previous.spec.sgCluster)
    setManagedLifecycle(previous.spec.managedLifecycle || false)
    setSeeded(true)
  }

  const clusters = (data.sgclusters || [])
    .filter((cluster) => cluster.metadata.namespace === namespace)
    .map((cluster) => cluster.metadata.name)

  const nameCollision =
    !isEdit &&
    (data.sgbackups || []).some(
      (backup) => backup.metadata.name === name && backup.metadata.namespace === namespace
    )

  const form = useCrdForm({
    kind: 'sgbackups',
    isEdit,
    buildPayload: () => buildBackup({ name, namespace, sgCluster, managedLifecycle }, previous || {}),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!sgCluster) errors.sgCluster = 'A target cluster is required'
      return errors
    },
    successPath: (payload) =>
      isEdit ? `/${namespace}/sgbackup/${payload.metadata.name}` : `/${namespace}/sgbackups`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  return (
    <CrdFormShell title={isEdit ? `Edit Backup: ${backupname}` : 'Create Backup'} form={form}>
      {nameCollision && (
        <p role="alert">
          There&apos;s already a <strong>SGBackup</strong> with the same name on this namespace.
          Please specify a different name or create the backup on another namespace
        </p>
      )}
      <Field label="Backup Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      <Field label="Backup Cluster" required error={form.errors.sgCluster}>
        <Select
          value={sgCluster}
          onChange={setSgCluster}
          options={clusters}
          placeholder="Choose a Cluster"
          disabled={isEdit}
          required
        />
      </Field>
      <Toggle label="Managed Lifecycle" checked={managedLifecycle} onChange={setManagedLifecycle} />
    </CrdFormShell>
  )
}
