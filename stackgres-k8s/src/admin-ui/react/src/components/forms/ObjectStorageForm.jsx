import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useCrdForm } from '../../hooks/useCrdForm'
import { useExistingResource } from '../../hooks/useExistingResource'
import { buildObjectStorage } from '../../lib/payloads'
import { nameValidationMessage } from '../../lib/names'
import CrdFormShell from './CrdFormShell'
import { Field, Select, TextInput, Toggle } from './fields'

const MASK = '******'
const TYPES = [
  { value: 's3', label: 'Amazon S3' },
  { value: 's3Compatible', label: 'S3 Compatible' },
  { value: 'gcs', label: 'Google Cloud Storage' },
  { value: 'azureBlob', label: 'Azure Blob Storage' },
]
const S3_STORAGE_CLASSES = ['STANDARD', 'STANDARD_IA', 'REDUCED_REDUNDANCY']

export default function ObjectStorageForm({ isEdit }) {
  const { namespace, name: routeName } = useParams()
  const { previous, loaded } = useExistingResource('sgobjectstorages', namespace, routeName, isEdit)

  const [name, setName] = useState(routeName || '')
  const [type, setType] = useState('')
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
      <Field label="Configuration Name" required error={form.errors.name}>
        <TextInput value={name} onChange={setName} disabled={isEdit} required error={form.errors.name} />
      </Field>
      <Field label="Storage Type" required error={form.errors.type}>
        <Select value={type} onChange={setType} options={TYPES} placeholder="Select a type…" required />
      </Field>

      {type === 's3' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Amazon S3</legend>
          <Field label="Bucket" required error={form.errors.bucket}>
            <TextInput value={s3.bucket} onChange={(bucket) => setS3({ ...s3, bucket })} required />
          </Field>
          <Field label="Region">
            <TextInput value={s3.region} onChange={(region) => setS3({ ...s3, region })} />
          </Field>
          <Field label="API Key" required>
            <TextInput value={s3.accessKeyId} onChange={(accessKeyId) => setS3({ ...s3, accessKeyId })} required />
          </Field>
          <Field label="API Secret" required>
            <TextInput type="password" value={s3.secretAccessKey} onChange={(secretAccessKey) => setS3({ ...s3, secretAccessKey })} required />
          </Field>
          <Field label="Storage Class">
            <Select value={s3.storageClass} onChange={(storageClass) => setS3({ ...s3, storageClass })} options={S3_STORAGE_CLASSES} placeholder="Default" />
          </Field>
        </fieldset>
      )}

      {type === 's3Compatible' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">S3 Compatible</legend>
          <Field label="Bucket" required error={form.errors.bucket}>
            <TextInput value={s3Compatible.bucket} onChange={(bucket) => setS3Compatible({ ...s3Compatible, bucket })} required />
          </Field>
          <Field label="Endpoint">
            <TextInput value={s3Compatible.endpoint} onChange={(endpoint) => setS3Compatible({ ...s3Compatible, endpoint })} />
          </Field>
          <Field label="Region">
            <TextInput value={s3Compatible.region} onChange={(region) => setS3Compatible({ ...s3Compatible, region })} />
          </Field>
          <Field label="API Key" required>
            <TextInput value={s3Compatible.accessKeyId} onChange={(accessKeyId) => setS3Compatible({ ...s3Compatible, accessKeyId })} required />
          </Field>
          <Field label="API Secret" required>
            <TextInput type="password" value={s3Compatible.secretAccessKey} onChange={(secretAccessKey) => setS3Compatible({ ...s3Compatible, secretAccessKey })} required />
          </Field>
          <Toggle label="Bucket URL Force Path Style" checked={s3Compatible.enablePathStyleAddressing}
            onChange={(enablePathStyleAddressing) => setS3Compatible({ ...s3Compatible, enablePathStyleAddressing })} />
          <Field label="Storage Class">
            <Select value={s3Compatible.storageClass} onChange={(storageClass) => setS3Compatible({ ...s3Compatible, storageClass })} options={S3_STORAGE_CLASSES} placeholder="Default" />
          </Field>
        </fieldset>
      )}

      {type === 'gcs' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Google Cloud Storage</legend>
          <Field label="Bucket" required error={form.errors.bucket}>
            <TextInput value={gcs.bucket} onChange={(bucket) => setGcs({ ...gcs, bucket })} required />
          </Field>
          <Toggle label="Fetch Credentials from Metadata Service" checked={gcs.fetchCredentials}
            onChange={(fetchCredentials) => setGcs({ ...gcs, fetchCredentials })} />
          {!gcs.fetchCredentials && (
            <Field
              label={`${isEdit ? 'Set New ' : ''}Service Account JSON`}
              required={!isEdit}
              help={isEdit && gcs.serviceAccountJSON === MASK ? 'Current credentials are kept unless a new file is uploaded' : ''}
            >
              <input type="file" accept=".json,application/json" onChange={uploadJSON} required={!isEdit} />
            </Field>
          )}
        </fieldset>
      )}

      {type === 'azureBlob' && (
        <fieldset className="flex flex-col gap-3 border p-3">
          <legend className="px-1 font-semibold">Azure Blob Storage</legend>
          <Field label="Bucket" required error={form.errors.bucket}>
            <TextInput value={azureBlob.bucket} onChange={(bucket) => setAzureBlob({ ...azureBlob, bucket })} required />
          </Field>
          <Field label="Account Name" required>
            <TextInput value={azureBlob.storageAccount} onChange={(storageAccount) => setAzureBlob({ ...azureBlob, storageAccount })} required />
          </Field>
          <Field label="Account Access Key" required>
            <TextInput type="password" value={azureBlob.accessKey} onChange={(accessKey) => setAzureBlob({ ...azureBlob, accessKey })} required />
          </Field>
        </fieldset>
      )}
    </CrdFormShell>
  )
}
