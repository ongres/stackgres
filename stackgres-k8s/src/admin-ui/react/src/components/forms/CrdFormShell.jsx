import { useNavigate, useParams } from 'react-router-dom'
import SummaryDialog from './SummaryDialog'
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
    <section className="flex max-w-3xl flex-col gap-4 px-6 pb-6 text-sm">
      <h1 className="text-lg font-semibold">{title}</h1>
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
            className="text-sm border rounded px-3 py-1.5 text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] hover:bg-[hsl(var(--muted))]"
          >
            Dry Run
          </button>
        </div>
      </form>
      {preview && (
        <SummaryDialog title="Dry Run Summary" value={preview} onClose={() => setPreview(null)} />
      )}
    </section>
  )
}
