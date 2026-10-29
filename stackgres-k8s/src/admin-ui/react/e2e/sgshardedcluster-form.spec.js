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

test('creates a sharded cluster with coordinator, shards and one override', async ({ page }) => {
  const name = `shard-e2e-${Date.now().toString(36)}`
  await login(page)
  await page.goto('/admin/default/sgshardedclusters/new')

  await page.getByRole('textbox', { name: /Sharded Cluster Name/ }).fill(name)
  await page.getByRole('textbox', { name: /Database/ }).fill('appdb')

  const sections = page.getByRole('navigation', { name: 'Form sections' })
  await sections.getByRole('button', { name: 'Coordinator' }).click()
  await page.getByRole('spinbutton', { name: /Number of Instances/ }).fill('2')

  await sections.getByRole('button', { name: 'Workers', exact: true }).click()
  await page.getByRole('spinbutton', { name: /Number of Clusters/ }).fill('3')
  await page.getByRole('combobox', { name: /Instance Profile/ }).selectOption('size-small')

  await sections.getByRole('button', { name: 'Overrides' }).click()
  await page.getByRole('button', { name: 'Add Override' }).click()
  await page.getByRole('combobox', { name: /Cluster Identifier/ }).selectOption('1')
  await page.getByRole('spinbutton', { name: /Number of Instances per Cluster/ }).fill('4')

  await page.getByRole('button', { name: 'Create Cluster' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgshardedclusters$/)

  const request = await lastRequest(page, 'POST', 'sgshardedclusters')
  expect(request.body.metadata).toEqual({ name, namespace: 'default' })
  expect(request.body.spec.profile).toBe('production')
  expect(request.body.spec.database).toBe('appdb')
  expect(request.body.spec.type).toBe('citus')
  expect(request.body.spec.postgres.ssl).toEqual({ enabled: true })
  expect(request.body.spec.replication).toBeNull()
  expect(request.body.spec.coordinator.instances).toBe(2)
  expect(request.body.spec.coordinator.replication).toEqual({ mode: 'sync-all' })
  expect(request.body.spec.workers.clusters).toBe(3)
  expect(request.body.spec.workers.instancesPerCluster).toBe(1)
  expect(request.body.spec.workers.sgInstanceProfile).toBe('size-small')
  expect(request.body.spec.workers.overrides).toEqual([
    {
      index: 1,
      instancesPerCluster: 4,
      sgInstanceProfile: null,
      configurations: null,
      managedSql: null,
      pods: {
        disableConnectionPooling: null,
        disablePostgresUtil: null,
        disableMetricsExporter: null,
        customVolumes: null,
        customInitContainers: null,
        customContainers: null,
        scheduling: null,
      },
      replication: null,
      metadata: null,
    },
  ])
})

test('edits a sharded cluster preserving the previous spec', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgshardedcluster/shard-uptodate/edit')

  const clusters = page.getByRole('navigation', { name: 'Form sections' })
  await clusters.getByRole('button', { name: 'Workers', exact: true }).click()
  const count = page.getByRole('spinbutton', { name: /Number of Clusters/ })
  await expect(count).toHaveValue(/\d+/)
  const next = Number(await count.inputValue()) + 1
  await count.fill(String(next))
  await page.getByRole('button', { name: 'Update Cluster' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgshardedcluster\/shard-uptodate$/)
  const request = await lastRequest(page, 'PUT', 'sgshardedclusters')
  expect(request.body.metadata.uid).toBe('22222222-0001-0000-0000-000000000001')
  expect(request.body.spec.database).toBe('myapp')
  expect(request.body.spec.workers.clusters).toBe(next)
  expect(request.body.spec.workers.sgInstanceProfile).toBe('size-small')
  expect(request.body.spec.coordinator.sgInstanceProfile).toBe('size-small')
  expect(request.body.spec.postgres.version).toBe('16.3')
})
