import { useState } from 'react'
import { toast } from 'sonner'
import { api } from '../lib/api'
import { useData } from '../contexts/DataContext'
import { useSettings } from '../contexts/SettingsContext'
import { buildRestartDbOps } from '../lib/payloads'
import { generatedName } from '../lib/names'

// The restart-cluster confirmation of the Vue NavBar, as a self-contained
// button + dialog. The generated name is shown (and locale-independent).
export default function RestartClusterDialog({ namespace, cluster }) {
  const { timezone } = useSettings()
  const { refresh } = useData()
  const [restartName, setRestartName] = useState(null)

  const open = () => setRestartName(generatedName('op', timezone))
  const close = () => setRestartName(null)

  const execute = async () => {
    const payload = buildRestartDbOps({ name: restartName, namespace, sgCluster: cluster })
    try {
      await api.create('sgdbops', payload)
      toast.success('Restart operation created successfully')
      refresh()
    } catch (error) {
      toast.error(error.message)
    }
    close()
  }

  return (
    <>
      <button type="button" onClick={open} className="underline">
        Restart
      </button>
      {restartName && (
        <div role="dialog" aria-label="Restart Cluster" className="fixed inset-0 z-10 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={close} aria-hidden="true" />
          <div className="relative flex max-w-md flex-col gap-3 border bg-white p-6 text-sm">
            <h2 className="text-lg font-semibold">Restart Cluster</h2>
            <p>
              This operation will create a new <strong>RESTART sgdbop</strong> with the name{' '}
              <strong>{restartName}</strong> and the cluster <strong>{cluster}</strong> will be
              restarted as soon as possible.
            </p>
            <p>Are you sure you want to proceed?</p>
            <div className="flex gap-3">
              <button type="button" onClick={execute} className="border p-2 font-semibold">
                Restart
              </button>
              <button type="button" onClick={close} className="border p-2">
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
