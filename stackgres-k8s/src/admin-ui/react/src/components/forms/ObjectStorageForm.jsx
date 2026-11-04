import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useData } from '../../contexts/DataContext'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildObjectStorage } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextInput, Toggle, renderTooltip } from './fields'

const MASK = '******'
const TYPES = [
  { value: 's3', label: 'Amazon S3' },
  { value: 's3Compatible', label: 'Amazon S3 API Compatible' },
  { value: 'gcs', label: 'Google Cloud Storage' },
  { value: 'azureBlob', label: 'Azure Blob Storage' },
]
const S3_STORAGE_CLASSES = [
  { value: 'STANDARD', label: 'Standard' },
  { value: 'STANDARD_IA', label: 'Infrequent Access' },
  { value: 'REDUCED_REDUNDANCY', label: 'Reduced Redundancy' },
]

export default function ObjectStorageForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgobjectstorages', namespace, routeName, isEdit)
  const data = useData()

  const [name, setName] = useState(routeName || '')
  const [type, setType] = useState('')
  const [advancedMode, setAdvancedMode] = useState(false)
  const [secretKeySelectors, setSecretKeySelectors] = useState({})
  const [s3, setS3] = useState({ bucket: '', region: '', storageClass: '', accessKeyId: '', secretAccessKey: '' })
  const [s3Compatible, setS3Compatible] = useState({
    bucket: '', endpoint: '', region: '', storageClass: '',
    enablePathStyleAddressing: false, accessKeyId: '', secretAccessKey: '',
  })
  const [gcs, setGcs] = useState({ bucket: '', fetchCredentials: false, serviceAccountJSON: '' })
  const [azureBlob, setAzureBlob] = useState({ bucket: '', storageAccount: '', accessKey: '' })
  const [seeded, setSeeded] = useState(false)

  if (isEdit && loaded && previous && !seeded) {
    const spec = previous.spec || {}
    setType(spec.type)
    if (spec.type === 's3' && spec.s3) {
      const masked = spec.s3.awsCredentials?.secretKeySelectors ? MASK : ''
      setS3({
        bucket: spec.s3.bucket || '', region: spec.s3.region || '',
        storageClass: spec.s3.storageClass || '', accessKeyId: masked, secretAccessKey: masked,
      })
      setSecretKeySelectors(spec.s3.awsCredentials?.secretKeySelectors || {})
    }
    if (spec.type === 's3Compatible' && spec.s3Compatible) {
      const masked = spec.s3Compatible.awsCredentials?.secretKeySelectors ? MASK : ''
      setS3Compatible({
        bucket: spec.s3Compatible.bucket || '', endpoint: spec.s3Compatible.endpoint || '',
        region: spec.s3Compatible.region || '', storageClass: spec.s3Compatible.storageClass || '',
        enablePathStyleAddressing: spec.s3Compatible.enablePathStyleAddressing || false,
        accessKeyId: masked, secretAccessKey: masked,
      })
      setSecretKeySelectors(spec.s3Compatible.awsCredentials?.secretKeySelectors || {})
    }
    if (spec.type === 'gcs' && spec.gcs) {
      setGcs({
        bucket: spec.gcs.bucket || '',
        fetchCredentials: spec.gcs.gcpCredentials?.fetchCredentialsFromMetadataService || false,
        serviceAccountJSON: spec.gcs.gcpCredentials?.secretKeySelectors ? MASK : '',
      })
      setSecretKeySelectors(spec.gcs.gcpCredentials?.secretKeySelectors || {})
    }
    if (spec.type === 'azureBlob' && spec.azureBlob) {
      setAzureBlob({ bucket: spec.azureBlob.bucket || '', storageAccount: MASK, accessKey: MASK })
      setSecretKeySelectors(spec.azureBlob.azureCredentials?.secretKeySelectors || {})
    }
    setSeeded(true)
  }

  const form = useCrdForm({
    kind: 'sgobjectstorages',
    isEdit,
    buildPayload: () =>
      buildObjectStorage(
        { editMode: isEdit, name, namespace, type, secretKeySelectors, s3, s3Compatible, gcs, azureBlob },
        previous || {}
      ),
    validate: (payload) => {
      const errors = {}
      const nameError = nameValidationMessage(payload.metadata.name)
      if (nameError) errors.name = nameError
      if (!type) errors.type = 'A storage type is required'
      const backend = { s3, s3Compatible, gcs, azureBlob }[type]
      if (backend && !backend.bucket.length) errors.bucket = 'A bucket is required'
      return errors
    },
    successPath: (payload) =>
      isEdit
        ? `/${namespace}/sgobjectstorage/${payload.metadata.name}`
        : `/${namespace}/sgobjectstorages`,
  })

  if (isEdit && !loaded) {
    return <p className="p-8 text-sm">Loading data…</p>
  }

  const nameCollision =
    !isEdit &&
    (data.sgobjectstorages || []).some(
      (storage) => storage.metadata.name === name && storage.metadata.namespace === namespace
    )

  const uploadJSON = (event) => {
    const file = event.target.files && event.target.files[0]
    if (!file) {
      setGcs({ ...gcs, serviceAccountJSON: isEdit ? MASK : '' })
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      setGcs({ ...gcs, serviceAccountJSON: String(reader.result) })
      setSecretKeySelectors({})
    }
    reader.readAsText(file)
  }

  return (
    <CrdFormShell
      title={isEdit ? `Edit Object Storage: ${routeName}` : 'Create Object Storage'}
      form={form}
    >
      <Field label="Configuration Name" required error={form.errors.name} tooltip="sgobjectstorage.metadata.name">
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      {nameCollision && (
        <p role="alert">
          There&apos;s already an <strong>SGObjectStorage</strong> with the same name on this
          namespace. Please specify a different name or create the configuration on another namespace
        </p>
      )}
      <Toggle label="Advanced options" checked={advancedMode} onChange={setAdvancedMode} />
      <fieldset className="flex flex-col gap-2 border p-3">
        <legend className="px-1 font-semibold">Storage Type *{renderTooltip('sgobjectstorage.spec.type')}</legend>
        {form.errors.type && <p role="alert">{form.errors.type}</p>}
        <div className="flex flex-wrap gap-3">
          {TYPES.map((candidate) => (
            <label key={candidate.value} className="flex items-center gap-2 border p-2">
              <input type="radio" name="storageType" value={candidate.value}
                checked={type === candidate.value} onChange={() => setType(candidate.value)} />
              {candidate.label}
            </label>
          ))}
        </div>
      </fieldset>

      {type === 's3' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Amazon S3 Configuration</legend>
          <Field label="Bucket" required error={form.errors.bucket} tooltip="sgobjectstorage.spec.s3.bucket">
            <TextInput value={s3.bucket} onChange={(bucket) => setS3({ ...s3, bucket })} required />
          </Field>
          {advancedMode && (
            <Field label="Region" tooltip="sgobjectstorage.spec.s3.region">
              <TextInput value={s3.region} onChange={(region) => setS3({ ...s3, region })} />
            </Field>
          )}
          <Field label="API Key" required tooltip="sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.accessKeyId">
            <TextInput value={s3.accessKeyId} onChange={(accessKeyId) => setS3({ ...s3, accessKeyId })} required />
          </Field>
          <Field label="API Secret" required tooltip="sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey">
            <TextInput type="password" value={s3.secretAccessKey} onChange={(secretAccessKey) => setS3({ ...s3, secretAccessKey })} required />
          </Field>
          {advancedMode && (
            <Field label="Storage Class" tooltip="sgobjectstorage.spec.s3.storageClass">
              <Select value={s3.storageClass} onChange={(storageClass) => setS3({ ...s3, storageClass })} options={S3_STORAGE_CLASSES} placeholder="Select Storage Class..." />
            </Field>
          )}
        </fieldset>
      )}

      {type === 's3Compatible' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">AS3 Compatible Configuration</legend>
          <Field label="Bucket" required error={form.errors.bucket} tooltip="sgobjectstorage.spec.s3Compatible.bucket">
            <TextInput value={s3Compatible.bucket} onChange={(bucket) => setS3Compatible({ ...s3Compatible, bucket })} required />
          </Field>
          {advancedMode && (
            <>
              <Field label="Endpoint" tooltip="sgobjectstorage.spec.s3Compatible.endpoint">
                <TextInput value={s3Compatible.endpoint} onChange={(endpoint) => setS3Compatible({ ...s3Compatible, endpoint })} />
              </Field>
              <Field label="Region" tooltip="sgobjectstorage.spec.s3Compatible.region">
                <TextInput value={s3Compatible.region} onChange={(region) => setS3Compatible({ ...s3Compatible, region })} />
              </Field>
            </>
          )}
          <Field label="API Key" required tooltip="sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId">
            <TextInput value={s3Compatible.accessKeyId} onChange={(accessKeyId) => setS3Compatible({ ...s3Compatible, accessKeyId })} required />
          </Field>
          <Field label="API Secret" required tooltip="sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey">
            <TextInput type="password" value={s3Compatible.secretAccessKey} onChange={(secretAccessKey) => setS3Compatible({ ...s3Compatible, secretAccessKey })} required />
          </Field>
          {advancedMode && (
            <>
              <Field label="Enable Path Style Addressing" tooltip="sgobjectstorage.spec.s3Compatible.enablePathStyleAddressing">
                <Toggle label="Bucket URL Force Path Style" checked={s3Compatible.enablePathStyleAddressing}
                  onChange={(enablePathStyleAddressing) => setS3Compatible({ ...s3Compatible, enablePathStyleAddressing })} />
              </Field>
              <Field label="Storage Class" tooltip="sgobjectstorage.spec.s3Compatible.storageClass">
                <Select value={s3Compatible.storageClass} onChange={(storageClass) => setS3Compatible({ ...s3Compatible, storageClass })} options={S3_STORAGE_CLASSES} placeholder="Select Storage Class..." />
              </Field>
            </>
          )}
        </fieldset>
      )}

      {type === 'gcs' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Google Cloud Storage Configuration</legend>
          <Field label="Bucket" required error={form.errors.bucket} tooltip="sgobjectstorage.spec.gcs.bucket">
            <TextInput value={gcs.bucket} onChange={(bucket) => setGcs({ ...gcs, bucket })} required />
          </Field>
          <Toggle label="Fetch Credentials from Metadata Service" checked={gcs.fetchCredentials}
            onChange={(fetchCredentials) => setGcs({ ...gcs, fetchCredentials })}
            tooltip="sgobjectstorage.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService" />
          {!gcs.fetchCredentials && (
            <Field
              label={`${isEdit ? 'Set New ' : ''}Service Account JSON`}
              required={!isEdit}
              tooltip="sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON"
              help={isEdit && gcs.serviceAccountJSON === MASK ? 'Current credentials are kept unless a new file is uploaded' : ''}
            >
              <input type="file" accept=".json,application/json" onChange={uploadJSON} required={!isEdit} />
            </Field>
          )}
          {!gcs.fetchCredentials && isEdit && (
            <>
              <p className="text-center font-semibold">– OR –</p>
              <div className="flex flex-wrap gap-3">
                <Field label="Service Account JSON Name" tooltip="sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.name">
                  <TextInput value={secretKeySelectors.serviceAccountJSON?.name || ''}
                    onChange={(value) =>
                      setSecretKeySelectors({
                        ...secretKeySelectors,
                        serviceAccountJSON: { ...secretKeySelectors.serviceAccountJSON, name: value },
                      })} />
                </Field>
                <Field label="Service Account JSON Key" tooltip="sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.key">
                  <TextInput value={secretKeySelectors.serviceAccountJSON?.key || ''}
                    onChange={(value) =>
                      setSecretKeySelectors({
                        ...secretKeySelectors,
                        serviceAccountJSON: { ...secretKeySelectors.serviceAccountJSON, key: value },
                      })} />
                </Field>
              </div>
            </>
          )}
        </fieldset>
      )}

      {type === 'azureBlob' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Microsoft Azure Configuration</legend>
          <Field label="Bucket" required error={form.errors.bucket} tooltip="sgobjectstorage.spec.azureBlob.bucket">
            <TextInput value={azureBlob.bucket} onChange={(bucket) => setAzureBlob({ ...azureBlob, bucket })} required />
          </Field>
          <Field label="Account Name" required tooltip="sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount">
            <TextInput value={azureBlob.storageAccount} onChange={(storageAccount) => setAzureBlob({ ...azureBlob, storageAccount })} required />
          </Field>
          <Field label="Account Access Key" required tooltip="sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey">
            <TextInput type="password" value={azureBlob.accessKey} onChange={(accessKey) => setAzureBlob({ ...azureBlob, accessKey })} required />
          </Field>
        </fieldset>
      )}
    </CrdFormShell>
  )
}
