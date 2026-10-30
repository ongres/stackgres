import { useState } from 'react'
import { toast } from 'sonner'
import { Plus, Trash2 } from 'lucide-react'
import { api } from '../../lib/api'
import { extractReportText, parseCompassReport } from '../../lib/babelfish'
import { Field, TextInput } from './../forms/fields'

// Babelfish Compass application (Vue BabelfishCompass): a report name plus
// one or more SQL files POSTed as multipart, with the returned Compass text
// report split into setup / body / metrics. The report body is rendered as
// text, not injected HTML (the Vue v-html of server HTML is not ported).
export default function BabelfishCompass() {
  const [reportName, setReportName] = useState('')
  const [files, setFiles] = useState([null])
  const [processing, setProcessing] = useState(false)
  const [result, setResult] = useState(null)

  const setFile = (index) => (event) =>
    setFiles(files.map((file, i) => (i === index ? event.target.files?.[0] || null : file)))

  const submit = async (event) => {
    event.preventDefault()
    const selected = files.filter(Boolean)
    if (!selected.length) {
      toast.error('At least one SQL file is required')
      return
    }
    setProcessing(true)
    try {
      const formData = new FormData()
      formData.append('reportName', reportName)
      selected.forEach((file) => formData.append('sqlFiles', file))
      const response = await api.runApplication('com.ongres', 'babelfish-compass', formData)
      setResult({
        ...parseCompassReport(extractReportText(response?.report || '')),
        files: selected.map((file) => file.name),
      })
    } catch {
      toast.error('There was an error when trying to process your file. Please try again!')
      setResult(null)
    } finally {
      setProcessing(false)
    }
  }

  return (
    <section className="flex flex-col gap-4 p-8 text-sm">
      <h1 className="text-2xl font-semibold">Babelfish Compass</h1>
      <div className="flex flex-col gap-6 lg:flex-row">
        <form onSubmit={submit} className="flex w-full flex-col gap-3 lg:w-1/3">
          <Field label="Report Name" required>
            <TextInput required value={reportName} onChange={setReportName} />
          </Field>
          <fieldset className="flex flex-col gap-2 border p-3">
            <legend className="px-1 font-semibold">SQL Files</legend>
            {files.map((file, index) => (
              <div key={index} className="flex items-center gap-2">
                <input type="file" required={index === 0} onChange={setFile(index)}
                  aria-label={`SQL file ${index + 1}`} />
                {index > 0 && (
                  <button type="button" aria-label={`Delete SQL file ${index + 1}`}
                    onClick={() => setFiles(files.filter((_, i) => i !== index))}>
                    <Trash2 size={16} />
                  </button>
                )}
              </div>
            ))}
            <button type="button" className="flex items-center gap-1 self-start text-sm underline"
              onClick={() => setFiles([...files, null])}>
              <Plus size={14} /> Add File
            </button>
          </fieldset>
          <button type="submit" disabled={processing} className="self-start border p-2 font-semibold">
            {processing ? 'Processing…' : 'Send'}
          </button>
        </form>
        <div className="w-full lg:w-2/3">
          {result ? (
            <div className="flex flex-col gap-3">
              <table className="w-fit border-collapse text-left">
                <tbody>
                  <tr>
                    <th className="border-b p-2">Report Name</th>
                    <td className="border-b p-2">{reportName}</td>
                  </tr>
                  {Object.entries(result.setup).map(([key, value]) => (
                    <tr key={key}>
                      <th className="border-b p-2">{key}</th>
                      <td className="border-b p-2">{value}</td>
                    </tr>
                  ))}
                  {Object.entries(result.metrics).map(([key, value]) => (
                    <tr key={key}>
                      <th className="border-b p-2">{key}</th>
                      <td className="border-b p-2">{value}</td>
                    </tr>
                  ))}
                  <tr>
                    <th className="border-b p-2">Files Processed</th>
                    <td className="border-b p-2">{result.files.join(', ')}</td>
                  </tr>
                </tbody>
              </table>
              <pre className="overflow-auto border p-3 font-mono" style={{ maxHeight: 'calc(100vh - 320px)' }}>
                {result.report}
              </pre>
            </div>
          ) : (
            <p>Submit a report to see the Compass assessment here.</p>
          )}
        </div>
      </div>
    </section>
  )
}
