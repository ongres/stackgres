import { useNavigate, useParams } from 'react-router-dom'
import SpecTree from '../SpecTree'
import { FormActions } from './fields'

// Common chrome for the CRD forms: heading, the form element, submit /
// dry-run / cancel actions and the dry-run result panel.
export default function CrdFormShell({ title, form, children }) {
  const navigate = useNavigate()
  const { namespace } = useParams()
  const { submit, dryRun, preview, setPreview, submitting } = form

  const cancel = () => {
    if (window.history.length > 2) {
      navigate(-1)
    } else {
      navigate(`/${namespace}`)
    }
  }

  return (
    <section className="flex max-w-3xl flex-col gap-4 p-8">
      <h1 className="text-2xl font-semibold">{title}</h1>
      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault()
          submit()
        }}
      >
        {children}
        <div className="flex items-center gap-3">
          <FormActions
            onCancel={cancel}
            submitting={submitting}
            submitLabel={form.isEdit ? 'Update' : 'Create'}
          />
          <button
            type="button"
            onClick={dryRun}
            disabled={submitting}
            title="Dry run evaluates the request against the API without persisting it"
            className="border p-2"
          >
            Dry Run
          </button>
        </div>
      </form>
      {preview && (
        <section className="border p-4 text-sm">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold uppercase">Dry Run Summary</h2>
            <button type="button" onClick={() => setPreview(null)} className="underline">
              Close
            </button>
          </div>
          <SpecTree value={preview} />
        </section>
      )}
    </section>
  )
}
