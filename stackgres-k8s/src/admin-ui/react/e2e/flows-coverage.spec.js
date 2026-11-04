import { test, expect } from '@playwright/test'

async function login(page) {
  await page.goto('/admin/')
  await page.getByLabel('Username').fill('admin')
  await page.getByLabel('Password').fill('password')
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page.getByRole('heading', { name: 'Used Namespaces' })).toBeVisible()
}

async function lastRequest(page, method, resource) {
  const log = await page.evaluate(() => fetch('/stackgres/requests-log').then((r) => r.json()))
  const matches = log.filter((entry) => entry.method === method && entry.resource === resource)
  return matches[matches.length - 1]
}

const unique = (prefix) => `${prefix}-${Date.now().toString(36)}`

test('restore-from-backup deep link preselects the backup and derives the name', async ({ page }) => {
  await login(page)
  await page.goto(
    '/admin/default/sgclusters/new?restoreFromBackup=uptodate-backup-2026-05-31&postgresVersion=16.2'
  )

  await expect(page.getByRole('heading', { name: 'Cluster Initialization' })).toBeVisible()
  await expect(page.getByRole('combobox', { name: 'Backup Selection' })).toHaveValue(
    'uptodate-backup-2026-05-31'
  )
  // The derived name lives on the Cluster step.
  await page.getByRole('navigation', { name: 'Form steps' })
    .getByRole('button', { name: 'Cluster', exact: true }).click()
  await expect(page.getByRole('textbox', { name: /Cluster Name/ })).toHaveValue(
    /^restore-from-uptodate-backup-2/
  )
})

test('cluster edit round-trips scheduling, services, metadata and replication', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/minor-outdated/edit')

  const steps = page.getByRole('navigation', { name: 'Form steps' })

  await steps.getByRole('button', { name: 'Scheduling' }).click()
  const selectors = page.getByRole('group', { name: 'Node Selectors' })
  await selectors.getByRole('button', { name: 'Add Node Selectors' }).click()
  await selectors.getByRole('textbox', { name: 'Key' }).fill('env')
  await selectors.getByRole('textbox', { name: 'Value' }).fill('prod')

  await steps.getByRole('button', { name: 'Services' }).click()
  const primaryPorts = page.getByRole('group', { name: 'Primary Custom Ports' })
  await primaryPorts.getByRole('button', { name: 'Add Port' }).click()
  await primaryPorts.getByRole('spinbutton', { name: 'Port', exact: true }).fill('6432')

  await steps.getByRole('button', { name: 'Metadata' }).click()
  const annotations = page.getByRole('group', { name: 'All Resources Annotations' })
  await annotations.getByRole('button', { name: 'Add All Resources Annotations' }).click()
  await annotations.getByRole('textbox', { name: 'Key' }).fill('team')
  await annotations.getByRole('textbox', { name: 'Value' }).fill('db')

  await steps.getByRole('button', { name: 'Pods Replication' }).click()
  await page.getByRole('button', { name: 'Add Group' }).click()
  await page.getByRole('textbox', { name: 'Group Name' }).fill('g1')
  await page.getByRole('combobox', { name: 'Group Role' }).selectOption('readonly')
  await page.getByRole('spinbutton', { name: 'Group Instances' }).fill('2')

  await steps.getByRole('button', { name: 'Non Production' }).click()
  await page.getByRole('combobox', { name: 'Cluster Pod Anti Affinity' }).selectOption('true')

  await page.getByRole('button', { name: 'Update Cluster' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgcluster\/minor-outdated$/)

  const request = await lastRequest(page, 'PUT', 'sgclusters')
  expect(request.body.spec.pods.scheduling.nodeSelector).toEqual({ env: 'prod' })
  expect(request.body.spec.postgresServices.primary.customPorts).toEqual([
    { appProtocol: null, name: null, nodePort: null, port: 6432, protocol: null, targetPort: null },
  ])
  expect(request.body.spec.metadata.annotations.allResources).toEqual({ team: 'db' })
  expect(request.body.spec.replication.groups).toEqual([
    { name: 'g1', role: 'readonly', instances: 2 },
  ])
  expect(request.body.spec.nonProductionOptions.disableClusterPodAntiAffinity).toBe(true)
})

test('sharded scripts keep their source binding after deleting a sibling', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgshardedclusters/new')

  const sections = page.getByRole('navigation', { name: 'Form sections' })
  await sections.getByRole('button', { name: 'Coordinator' }).click()
  await page.getByRole('checkbox', { name: 'Advanced options' }).check()
  const steps = page.getByRole('navigation', { name: 'Form steps' })
  await steps.getByRole('button', { name: 'Scripts' }).click()

  await page.getByRole('button', { name: 'Add Script' }).click()
  await page.getByRole('button', { name: 'Add Script' }).click()
  await page.getByRole('combobox', { name: 'Script 2 source' }).selectOption('init-schema')
  await page.locator('fieldset').filter({ hasText: 'Script #1' }).first()
    .getByRole('button', { name: 'Delete Script' }).click()

  await expect(page.getByRole('combobox', { name: 'Script 1 source' })).toHaveValue('init-schema')
})

test('promotes a standby cluster by nulling replicateFrom', async ({ page }) => {
  const name = unique('standby-e2e')
  await login(page)
  await page.evaluate((clusterName) =>
    fetch('/stackgres/sgclusters', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        metadata: { name: clusterName, namespace: 'default' },
        spec: {
          instances: 1,
          postgres: { version: '16.3', flavor: 'vanilla' },
          pods: { persistentVolume: { size: '5Gi' } },
          replicateFrom: { instance: { sgCluster: 'uptodate' } },
          postgresServices: {
            primary: { enabled: true, type: 'ClusterIP' },
            replicas: { enabled: true, type: 'ClusterIP' },
          },
        },
      }),
    })
  , name)

  await page.goto(`/admin/default/sgcluster/${name}`)
  await page.getByRole('button', { name: 'Promote' }).click()
  await expect(page.getByText(`Cluster "${name}" promoted successfully`)).toBeVisible()

  const request = await lastRequest(page, 'PUT', 'sgclusters')
  expect(request.body.metadata.name).toBe(name)
  expect(request.body.spec.replicateFrom).toBeNull()
  await expect(page.getByRole('button', { name: 'Promote' })).toBeHidden()
})

test('edits a role appending a rule entry and preserving the rest', async ({ page }) => {
  await login(page)
  await page.goto('/admin/manage/role/reader/edit')

  await expect(page.getByRole('textbox', { name: 'apiGroups 1' })).toHaveValue('stackgres.io')
  await page.getByRole('group', { name: 'nonResourceURLs', exact: true })
    .getByRole('button', { name: 'Add New' }).click()
  await page.getByRole('textbox', { name: 'nonResourceURLs 1' }).fill('/healthz')
  await page.getByRole('button', { name: 'Update Role' }).click()

  await expect(page).toHaveURL(/\/admin\/manage\/role\/reader$/)
  const request = await lastRequest(page, 'PUT', 'roles')
  expect(request.body.metadata).toMatchObject({ name: 'reader', namespace: 'default' })
  expect(request.body.rules[0].apiGroups).toEqual(['stackgres.io'])
  expect(request.body.rules[0].nonResourceURLs).toEqual(['/healthz'])
  expect(request.body.rules[0].verbs).toEqual(['get', 'list', 'watch'])
})

test('creates and edits a cluster role without a namespace', async ({ page }) => {
  const name = unique('crole-e2e')
  await login(page)
  await page.goto('/admin/manage/clusterroles/new')

  await page.getByRole('textbox', { name: /Cluster Role Name/ }).fill(name)
  await page.getByRole('button', { name: 'Create Cluster Role' }).click()
  await expect(page).toHaveURL(/\/admin\/manage\/users$/)
  let request = await lastRequest(page, 'POST', 'clusterroles')
  expect(request.body).toEqual({ metadata: { namespace: '', name }, rules: null })

  await page.goto(`/admin/manage/clusterrole/${name}/edit`)
  await page.getByRole('button', { name: 'Add Rule' }).click()
  await page.getByRole('group', { name: 'verbs', exact: true })
    .getByRole('button', { name: 'Add New' }).click()
  await page.getByRole('textbox', { name: 'verbs 1' }).fill('list')
  await page.getByRole('button', { name: 'Update Cluster Role' }).click()

  await expect(page).toHaveURL(new RegExp(`/admin/manage/clusterrole/${name}$`))
  request = await lastRequest(page, 'PUT', 'clusterroles')
  expect(request.body.rules).toEqual([
    {
      apiGroups: null,
      nonResourceURLs: null,
      resourceNames: null,
      resources: null,
      verbs: ['list'],
    },
  ])
})

test('edits a user toggling a role binding off', async ({ page }) => {
  await login(page)
  await page.goto('/admin/manage/user/viewer/edit')

  const readerBind = page.getByRole('checkbox', { name: 'Bind reader' })
  await expect(readerBind).toBeChecked()
  await readerBind.uncheck()
  await page.getByRole('button', { name: 'Update User' }).click()

  await expect(page).toHaveURL(/\/admin\/manage\/user\/viewer$/)
  const request = await lastRequest(page, 'PUT', 'users')
  expect(request.body.metadata.name).toBe('viewer')
  expect(request.body.roles).toBeNull()
  expect(request.body.k8sUsername).toBe('viewer')
})

test('deletes a script through the type-the-name confirmation', async ({ page }) => {
  const name = unique('del-script-e2e')
  await login(page)
  await page.evaluate((scriptName) =>
    fetch('/stackgres/sgscripts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        metadata: { name: scriptName, namespace: 'default' },
        spec: { scripts: [{ script: 'SELECT 1;' }] },
      }),
    })
  , name)

  await page.goto(`/admin/default/sgscript/${name}`)
  await page.getByRole('button', { name: 'Delete' }).click()
  const dialog = page.getByRole('dialog', { name: 'Delete resource' })
  await dialog.getByLabel('Confirm resource name').fill(name)
  await dialog.getByRole('button', { name: 'Delete Item' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgscripts$/)
  const request = await lastRequest(page, 'DELETE', 'sgscripts')
  expect(request.body).toEqual({ metadata: { name, namespace: 'default' } })
})

test('unknown resources render the not-found view', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/does-not-exist')
  await expect(page.getByText('was not found')).toBeVisible()
})

test('the topbar shows the StackGres version from sg-info.json', async ({ page }) => {
  await login(page)
  await expect(page.getByText('development')).toBeVisible()
})

test('logout tears down the console chrome', async ({ page }) => {
  await login(page)
  await page.getByRole('button', { name: 'Logout' }).click()
  await expect(page.getByLabel('Username')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Used Namespaces' })).toBeHidden()
})
