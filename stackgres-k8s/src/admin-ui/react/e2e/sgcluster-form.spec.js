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

test('creates a minimal-template cluster with the exact payload', async ({ page }) => {
  const name = `minimal-e2e-${Date.now().toString(36)}`
  await login(page)
  await page.goto('/admin/default/sgclusters/new')

  await page.getByRole('button', { name: /Minimal/ }).click()
  await page.getByRole('textbox', { name: /Cluster Name/ }).fill(name)
  await page.getByRole('button', { name: 'Create Cluster' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgclusters$/)
  const request = await lastRequest(page, 'POST', 'sgclusters')
  expect(request.body).toEqual({
    metadata: { name, namespace: 'default' },
    spec: {
      profile: 'development',
      instances: 1,
      sgInstanceProfile: null,
      pods: {
        persistentVolume: { size: '1Gi' },
        disableConnectionPooling: true,
        disableMetricsExporter: null,
        disablePostgresUtil: null,
        metadata: null,
        customVolumes: null,
        customInitContainers: null,
        customContainers: null,
      },
      configurations: {
        observability: { prometheusAutobind: false },
        sgPostgresConfig: null,
        backups: null,
        sgPoolingConfig: null,
      },
      distributedLogs: null,
      initialData: null,
      managedSql: null,
      replicateFrom: null,
      replication: { role: 'ha-read', mode: 'async' },
      postgresServices: {
        primary: { enabled: true, type: 'ClusterIP' },
        replicas: { enabled: true, type: 'ClusterIP' },
      },
      postgres: {
        version: 'latest',
        extensions: null,
        flavor: 'vanilla',
        ssl: { enabled: false },
      },
    },
  })
})

test('creates a custom cluster with extensions and managed backups', async ({ page }) => {
  const name = `custom-e2e-${Date.now().toString(36)}`
  await login(page)
  await page.goto('/admin/default/sgclusters/new')

  await page.getByRole('button', { name: /Custom/ }).click()
  await page.getByRole('textbox', { name: /Cluster Name/ }).fill(name)

  const steps = page.getByRole('navigation', { name: 'Form steps' })
  await steps.getByRole('button', { name: 'Extensions' }).click()
  await page.getByRole('checkbox', { name: 'Enable postgis' }).check()

  await steps.getByRole('button', { name: 'Backups' }).click()
  await page.getByRole('checkbox', { name: 'Managed Backups' }).check()
  await page.getByRole('combobox', { name: /Object Storage/ }).selectOption('minio-backups')

  await page.getByRole('button', { name: 'Create Cluster' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgclusters$/)

  const request = await lastRequest(page, 'POST', 'sgclusters')
  expect(request.body.spec.profile).toBe('production')
  expect(request.body.spec.pods.disableConnectionPooling).toBeNull()
  expect(request.body.spec.postgres.extensions).toEqual([
    {
      name: 'postgis',
      version: '3.4.2',
      publisher: 'com.ongres',
      repository: 'https://extensions.stackgres.io/postgres/repository',
    },
  ])
  expect(request.body.spec.configurations.backups).toEqual([
    {
      path: null,
      compression: 'lz4',
      cronSchedule: '0 5 * * *',
      retention: 5,
      performance: { maxNetworkBandwidth: '', maxDiskBandwidth: '', uploadDiskConcurrency: 1 },
      sgObjectStorage: 'minio-backups',
      useVolumeSnapshot: false,
      volumeSnapshotClass: null,
      fastVolumeSnapshot: false,
    },
  ])
})

test('edits a cluster preserving the previous spec and metadata', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/uptodate/edit')

  const instances = page.getByRole('spinbutton', { name: /Number of Instances/ })
  await expect(instances).toHaveValue(/\d+/)
  const next = Number(await instances.inputValue()) + 1
  await instances.fill(String(next))
  await page.getByRole('button', { name: 'Update Cluster' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgcluster\/uptodate$/)
  const request = await lastRequest(page, 'PUT', 'sgclusters')
  expect(request.body.metadata.uid).toBe('11111111-0001-0000-0000-000000000001')
  expect(request.body.spec.instances).toBe(next)
  expect(request.body.spec.sgInstanceProfile).toBe('size-small')
  expect(request.body.spec.pods.persistentVolume.size).toBe('10Gi')
  expect(request.body.spec.configurations.sgPostgresConfig).toBe('pgconfig-default')
  expect(request.body.spec.configurations.sgPoolingConfig).toBe('poolconfig-default')
  expect(request.body.spec.postgres.version).toBe('16.3')
  expect(request.body.spec.postgres.flavor).toBe('vanilla')
})
