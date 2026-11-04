import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { toast } from 'sonner'
import { getTooltip, indexTooltips } from '../lib/tooltips'

const TooltipsContext = createContext(null)

// The build ships the merged swagger at info/sg-tooltips.json next to the app
// (see admin-ui/build.sh); in development the file is optional and the UI
// degrades to no help icons, as the Vue console did.
export function TooltipsProvider({ children }) {
  const [tooltips, setTooltips] = useState(null)

  useEffect(() => {
    let cancelled = false
    fetch(`${import.meta.env.BASE_URL}info/sg-tooltips.json`)
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        return response.json()
      })
      .then((data) => {
        if (!cancelled) {
          setTooltips(indexTooltips(data.components.schemas))
        }
      })
      .catch(() => {
        if (cancelled) {
          return
        }
        setTooltips({ error: 'Information tooltips could not be loaded' })
        toast.info(
          <span>
            Information tooltips could not be loaded properly, help texts won&apos;t be
            available.
            <br />
            <br />
            This does not affect the usability of the web console. You can always refer
            to the{' '}
            <a href="https://stackgres.io/doc" target="_blank" rel="noreferrer" className="underline">
              official StackGres Documentation
            </a>{' '}
            for more information.
          </span>,
          { id: 'tooltips-load-error' }
        )
      })
    return () => {
      cancelled = true
    }
  }, [])

  const value = useMemo(() => ({ tooltips }), [tooltips])

  return <TooltipsContext.Provider value={value}>{children}</TooltipsContext.Provider>
}

export function useTooltips() {
  const context = useContext(TooltipsContext)
  const tooltips = context ? context.tooltips : null
  return useMemo(
    () => ({
      error: Boolean(tooltips && tooltips.error),
      getTooltip: (field) => getTooltip(tooltips, field),
    }),
    [tooltips]
  )
}
