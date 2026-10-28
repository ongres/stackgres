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

test('creates an instance profile and asserts the exact payload', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sginstanceprofiles/new')

  await page.getByLabel(/Profile Name/).fill('size-xs')
  await page.getByLabel('RAM', { exact: false }).first().fill('2')
  await page.getByLabel('CPU', { exact: false }).first().fill('500')
  await page.locator('select').nth(1).selectOption('m')
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sginstanceprofiles$/)
  await expect(page.getByRole('link', { name: 'size-xs' }).first()).toBeVisible()

  const request = await lastRequest(page, 'POST', 'sginstanceprofiles')
  expect(request.body).toEqual({
    metadata: { name: 'size-xs', namespace: 'default' },
    spec: {
      cpu: '500m',
      memory: '2Gi',
      hugePages: null,
      containers: null,
      initContainers: null,
      requests: null,
    },
  })
})

test('rejects an invalid resource name client-side', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sginstanceprofiles/new')

  await page.getByLabel(/Profile Name/).fill('Invalid Name')
  await page.getByLabel('RAM', { exact: false }).first().fill('2')
  await page.getByLabel('CPU', { exact: false }).first().fill('1')
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page.getByRole('alert').first()).toContainText(/lowercase/)
  await expect(page).toHaveURL(/sginstanceprofiles\/new$/)
})

test('dry run shows the server-materialised summary without persisting', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgpgconfigs/new')

  await page.getByLabel(/Configuration Name/).fill('dryrun-pg')
  await page.getByLabel(/Postgres Version/).selectOption({ index: 1 })
  await page.getByRole('button', { name: 'Dry Run' }).click()

  await expect(page.getByRole('heading', { name: 'Dry Run Summary' })).toBeVisible()
  const request = await lastRequest(page, 'POST', 'sgpgconfigs')
  expect(request.dryRun).toBe(true)
  await expect(page).toHaveURL(/sgpgconfigs\/new$/)
})

test('edits a pool config preserving previous metadata', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgpoolconfigs/new')
  await page.getByLabel(/Configuration Name/).fill('pool-edit-me')
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgpoolconfigs$/)

  await page.goto('/admin/default/sgpoolconfig/pool-edit-me/edit')
  await page.getByLabel(/PgBouncer Parameters/).fill('max_client_conn=400')
  await page.getByRole('button', { name: 'Update' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgpoolconfig\/pool-edit-me$/)

  const request = await lastRequest(page, 'PUT', 'sgpoolconfigs')
  expect(request.body.kind).toBe('StackGresConnectionPoolingConfig')
  expect(request.body.spec.pgBouncer['pgbouncer.ini']).toBe('max_client_conn=400')
})

test('creates a benchmark dbops with the exact payload', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgdbops/new')

  await page.getByLabel(/Operation Name/).fill('bench-e2e')
  await page.getByLabel(/Target Cluster/).selectOption('uptodate')
  await page.locator('form').getByLabel(/Database Operation/).selectOption('benchmark')
  await page.getByLabel(/Database Size/).first().fill('2')
  await page.getByLabel('Duration minutes').selectOption('5')
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgdbops$/)
  const request = await lastRequest(page, 'POST', 'sgdbops')
  expect(request.body.spec).toEqual({
    sgCluster: 'uptodate',
    op: 'benchmark',
    maxRetries: 0,
    benchmark: {
      type: 'pgbench',
      connectionType: 'primary-service',
      pgbench: {
        databaseSize: '2Gi',
        duration: 'P0DT0H5M0S',
        usePreparedStatements: false,
        concurrentClients: 1,
        threads: 1,
      },
    },
  })
})

test('restart dialog creates the restart dbops and delete removes a resource', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/uptodate')

  await page.getByRole('button', { name: 'Restart' }).click()
  const dialog = page.getByRole('dialog', { name: 'Restart Cluster' })
  await expect(dialog).toContainText('RESTART sgdbop')
  await dialog.getByRole('button', { name: 'Restart' }).click()
  await expect(page.getByText('Restart operation created successfully')).toBeVisible()

  const restart = await lastRequest(page, 'POST', 'sgdbops')
  expect(restart.body.spec).toEqual({
    sgCluster: 'uptodate',
    op: 'restart',
    restart: { method: 'InPlace', onlyPendingRestart: false },
  })
  expect(restart.body.metadata.name).toMatch(/^op\d{4}(-\d{2}){5}$/)

  await page.goto('/admin/default/sginstanceprofile/size-xs')
  await page.getByRole('button', { name: 'Delete' }).click()
  const deleteDialog = page.getByRole('dialog', { name: 'Delete resource' })
  await deleteDialog.getByLabel('Confirm resource name').fill('wrong-name')
  await deleteDialog.getByRole('button', { name: 'Delete Item' }).click()
  await expect(deleteDialog.getByRole('alert')).toBeVisible()
  await deleteDialog.getByLabel('Confirm resource name').fill('size-xs')
  await deleteDialog.getByRole('button', { name: 'Delete Item' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sginstanceprofiles$/)
})
