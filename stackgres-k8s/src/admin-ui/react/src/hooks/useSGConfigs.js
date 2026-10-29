import { useEffect, useState } from 'react'
import { api } from '../lib/api'
import { usePermissions } from '../contexts/PermissionsContext'

// SGConfig is cluster-scoped and outside the namespaced data caches: a small
// hook keeps the list with a manual refresh.
export function useSGConfigs() {
  const { loaded } = usePermissions()
  const [sgconfigs, setSgconfigs] = useState(null)

  const refreshConfigs = () => api.get('sgconfigs').then(setSgconfigs).catch(() => {})

  useEffect(() => {
    if (loaded) {
      refreshConfigs()
    }
  }, [loaded])

  return { sgconfigs, refreshConfigs }
}
