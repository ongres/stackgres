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

function toLocalInput(iso) {
  const date = new Date(iso)
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const unique = (prefix) => `${prefix}-${Date.now().toString(36)}`

test('creates an S3 object storage with advanced options and dry run', async ({ page }) => {
  const name = unique('s3-e2e')
  await login(page)
  await page.goto('/admin/default/sgobjectstorages/new')

  await page.getByRole('textbox', { name: /Configuration Name/ }).fill(name)
  await page.getByRole('checkbox', { name: 'Advanced options' }).check()
  await page.getByRole('radio', { name: 'Amazon S3', exact: true }).check()
  await page.getByRole('textbox', { name: /Bucket/ }).fill('backups-bucket')
  await page.getByRole('textbox', { name: 'Region' }).fill('us-east-1')
  await page.getByRole('textbox', { name: /API Key/ }).fill('AKIA123')
  await page.getByRole('textbox', { name: /API Secret/ }).fill('s3cr3t')
  await page.getByRole('combobox', { name: /Storage Class/ }).selectOption('STANDARD_IA')

  await page.getByRole('button', { name: 'Dry Run' }).click()
  await expect(page.getByRole('heading', { name: 'Dry Run Summary' })).toBeVisible()
  await page.getByRole('button', { name: 'Close' }).click()
  await expect(page.getByRole('heading', { name: 'Dry Run Summary' })).toBeHidden()

  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgobjectstorages$/)
  const request = await lastRequest(page, 'POST', 'sgobjectstorages')
  expect(request.body).toEqual({
    metadata: { name, namespace: 'default' },
    spec: {
      type: 's3',
      s3: {
        bucket: 'backups-bucket',
        region: 'us-east-1',
        storageClass: 'STANDARD_IA',
        awsCredentials: { accessKeyId: 'AKIA123', secretAccessKey: 's3cr3t' },
      },
    },
  })
})

test('creates S3-compatible, GCS and Azure object storages with exact payloads', async ({ page }) => {
  await login(page)

  const s3c = unique('s3c-e2e')
  await page.goto('/admin/default/sgobjectstorages/new')
  await page.getByRole('textbox', { name: /Configuration Name/ }).fill(s3c)
  await page.getByRole('checkbox', { name: 'Advanced options' }).check()
  await page.getByRole('radio', { name: 'Amazon S3 API Compatible' }).check()
  await page.getByRole('textbox', { name: /Bucket/ }).fill('minio-bucket')
  await page.getByRole('textbox', { name: 'Endpoint' }).fill('https://minio.local:9000')
  await page.getByRole('checkbox', { name: 'Bucket URL Force Path Style' }).check()
  await page.getByRole('textbox', { name: /API Key/ }).fill('minio')
  await page.getByRole('textbox', { name: /API Secret/ }).fill('minio123')
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgobjectstorages$/)
  let request = await lastRequest(page, 'POST', 'sgobjectstorages')
  expect(request.body.spec).toEqual({
    type: 's3Compatible',
    s3Compatible: {
      bucket: 'minio-bucket',
      enablePathStyleAddressing: true,
      endpoint: 'https://minio.local:9000',
      awsCredentials: { accessKeyId: 'minio', secretAccessKey: 'minio123' },
    },
  })

  const gcs = unique('gcs-e2e')
  await page.goto('/admin/default/sgobjectstorages/new')
  await page.getByRole('textbox', { name: /Configuration Name/ }).fill(gcs)
  await page.getByRole('radio', { name: 'Google Cloud Storage' }).check()
  await page.getByRole('textbox', { name: /Bucket/ }).fill('gcs-bucket')
  await page.getByLabel(/Service Account JSON/).setInputFiles({
    name: 'sa.json',
    mimeType: 'application/json',
    buffer: Buffer.from('{"type":"service_account"}'),
  })
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgobjectstorages$/)
  request = await lastRequest(page, 'POST', 'sgobjectstorages')
  expect(request.body.spec).toEqual({
    type: 'gcs',
    gcs: {
      bucket: 'gcs-bucket',
      gcpCredentials: { serviceAccountJSON: '{"type":"service_account"}' },
    },
  })

  const azure = unique('az-e2e')
  await page.goto('/admin/default/sgobjectstorages/new')
  await page.getByRole('textbox', { name: /Configuration Name/ }).fill(azure)
  await page.getByRole('radio', { name: 'Azure Blob Storage' }).check()
  await page.getByRole('textbox', { name: /Bucket/ }).fill('azure-bucket')
  await page.getByRole('textbox', { name: /Account Name/ }).fill('storacct')
  await page.getByRole('textbox', { name: /Account Access Key/ }).fill('azkey')
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgobjectstorages$/)
  request = await lastRequest(page, 'POST', 'sgobjectstorages')
  expect(request.body.spec).toEqual({
    type: 'azureBlob',
    azureBlob: {
      bucket: 'azure-bucket',
      azureCredentials: { storageAccount: 'storacct', accessKey: 'azkey' },
    },
  })
})

test('object storage form requires a storage type', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgobjectstorages/new')
  await page.getByRole('textbox', { name: /Configuration Name/ }).fill(unique('notype'))
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page.getByRole('alert').first()).toContainText(/storage type/i)
  await expect(page).toHaveURL(/sgobjectstorages\/new$/)
})

test('creates a backup with a source cluster and managed lifecycle', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgbackups/new')

  const nameInput = page.getByRole('textbox', { name: /Backup Name/ })
  await expect(nameInput).toHaveValue(/^bk\d{4}(-\d{2}){5}$/)
  const name = unique('bk-e2e')
  await nameInput.fill(name)
  await page.getByRole('combobox', { name: /Backup Cluster/ }).selectOption('uptodate')
  await page.getByRole('checkbox', { name: 'Managed Lifecycle' }).check()
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgbackups$/)
  const request = await lastRequest(page, 'POST', 'sgbackups')
  expect(request.body).toEqual({
    metadata: { name, namespace: 'default' },
    spec: { sgCluster: 'uptodate', managedLifecycle: true },
  })
})

test('creates a script, adds a second entry and deletes the first', async ({ page }) => {
  const name = unique('script-e2e')
  await login(page)
  await page.goto('/admin/default/sgscripts/new')

  await page.getByRole('textbox', { name: /Script Name/ }).fill(name)
  const entry = (index) => page.locator('fieldset').filter({ hasText: `Script Entry #${index}` }).first()
  await entry(1).getByRole('textbox', { name: 'Name', exact: true }).fill('seed')
  await entry(1).getByRole('textbox', { name: 'Database', exact: true }).fill('demo')
  await entry(1).getByRole('textbox', { name: 'User', exact: true }).fill('postgres')
  await entry(1).getByRole('textbox', { name: /^Script\b/ }).fill('CREATE TABLE t ();')

  await page.getByRole('button', { name: 'Add Entry' }).click()
  await entry(2).getByRole('textbox', { name: /^Script\b/ }).fill('SELECT 2;')
  await entry(1).getByRole('button', { name: 'Delete Entry' }).click()
  await expect(page.getByText('CREATE TABLE t ();')).toBeHidden()

  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgscripts$/)
  const request = await lastRequest(page, 'POST', 'sgscripts')
  expect(request.body.metadata).toEqual({ name, namespace: 'default' })
  expect(request.body.spec.scripts).toEqual([
    { storeStatusInDatabase: false, retryOnError: false, script: 'SELECT 2;' },
  ])
})

test('creates a vacuum dbops with per-database overrides and runAt', async ({ page }) => {
  const name = unique('vacuum-e2e')
  await login(page)
  await page.goto('/admin/default/sgdbops/new')

  await page.getByLabel(/Operation Name/).fill(name)
  await page.getByLabel(/Target Cluster/).selectOption('uptodate')
  await page.getByRole('radio', { name: 'Vacuum' }).check()
  await page.locator('input[type="datetime-local"]').fill(toLocalInput('2026-12-01T05:00:30Z'))
  await page.getByRole('checkbox', { name: 'Freeze' }).check()
  await page.getByRole('checkbox', { name: 'Database Specific Options' }).check()
  await page.getByRole('textbox', { name: 'Name', exact: true }).fill('demo')
  await page.getByRole('combobox', { name: 'Full', exact: true }).selectOption('true')
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgdbops$/)
  const request = await lastRequest(page, 'POST', 'sgdbops')
  expect(request.body.spec.runAt).toBe('2026-12-01T05:00:30Z')
  expect(request.body.spec.vacuum).toEqual({
    full: false,
    freeze: true,
    analyze: true,
    disablePageSkipping: false,
    databases: [
      { name: 'demo', full: true, freeze: true, analyze: true, disablePageSkipping: false },
    ],
  })
})

test('creates security and minor version upgrade dbops', async ({ page }) => {
  await login(page)

  const security = unique('sec-e2e')
  await page.goto('/admin/default/sgdbops/new')
  await page.getByLabel(/Operation Name/).fill(security)
  await page.getByLabel(/Target Cluster/).selectOption('uptodate')
  await page.getByRole('radio', { name: 'Security Upgrade' }).check()
  await page.getByRole('combobox', { name: 'Method' }).selectOption('ReducedImpact')
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgdbops$/)
  let request = await lastRequest(page, 'POST', 'sgdbops')
  expect(request.body.spec.securityUpgrade).toEqual({ method: 'ReducedImpact' })

  const minor = unique('minor-e2e')
  await page.goto('/admin/default/sgdbops/new')
  await page.getByLabel(/Operation Name/).fill(minor)
  await page.getByLabel(/Target Cluster/).selectOption('minor-outdated')
  await page.getByRole('radio', { name: 'Minor Version Upgrade' }).check()
  const target = page.getByRole('combobox', { name: /Target Postgres Version/ })
  await expect(target).toBeVisible()
  const version = await target.locator('option').nth(1).getAttribute('value')
  await target.selectOption(version)
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgdbops$/)
  request = await lastRequest(page, 'POST', 'sgdbops')
  expect(request.body.spec.minorVersionUpgrade).toEqual({ method: 'InPlace', postgresVersion: version })
})

test('creates a major version upgrade dbops with a target configuration', async ({ page }) => {
  await login(page)
  // The upgrade needs a Postgres configuration for the target major version.
  await page.goto('/admin/')
  await page.evaluate(() =>
    fetch('/stackgres/sgpgconfigs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        metadata: { name: 'pg17-e2e', namespace: 'default' },
        spec: { postgresVersion: '17', 'postgresql.conf': '' },
      }),
    })
  )

  const name = unique('major-e2e')
  await page.goto('/admin/default/sgdbops/new')
  await page.getByLabel(/Operation Name/).fill(name)
  await page.getByLabel(/Target Cluster/).selectOption('uptodate')
  await page.getByRole('radio', { name: 'Major Version Upgrade' }).check()
  await page.getByRole('checkbox', { name: 'Hard Link Files' }).check()
  await page.getByRole('checkbox', { name: 'Check Clusters' }).check()
  await page.getByRole('textbox', { name: /Backup Path/ }).fill('/backups/major')
  await page.getByRole('combobox', { name: /Target Postgres Version/ }).selectOption('17.0')
  await page.getByRole('combobox', { name: /Target Postgres Configuration/ }).selectOption('pg17-e2e')
  await page.getByRole('button', { name: 'Create' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgdbops$/)
  const request = await lastRequest(page, 'POST', 'sgdbops')
  expect(request.body.spec.majorVersionUpgrade).toEqual({
    link: true,
    clone: false,
    check: true,
    backupPath: '/backups/major',
    postgresVersion: '17.0',
    sgPostgresConfig: 'pg17-e2e',
  })
})

test('creates an SGCluster-to-SGCluster stream with pods scheduling', async ({ page }) => {
  const name = unique('stream2-e2e')
  await login(page)
  await page.goto('/admin/default/sgstreams/new')

  await page.getByLabel(/Stream Name/).fill(name)
  await page.getByRole('spinbutton', { name: /Maximum Retries/ }).fill('5')
  const steps = page.getByRole('navigation', { name: 'Form steps' })

  await steps.getByRole('button', { name: 'source' }).click()
  await page.getByLabel(/Source Type/).selectOption('SGCluster')
  await page.getByLabel(/SGCluster Name/).selectOption('uptodate')
  await page.getByRole('group', { name: 'Includes' }).getByRole('button', { name: 'Add New' }).click()
  await page.getByRole('textbox', { name: 'Includes 1' }).fill('public.*')

  await steps.getByRole('button', { name: 'target' }).click()
  await page.getByLabel(/Target Type/).selectOption('SGCluster')
  await page.getByRole('combobox', { name: /SGCluster Name/ }).selectOption('minor-outdated')
  await page.getByRole('checkbox', { name: 'Skip DDL Import' }).check()
  await page.getByRole('textbox', { name: /DDL Import Role Skip Filter/ }).fill('postgres')

  await steps.getByRole('button', { name: 'pods' }).click()
  await page.getByRole('group', { name: 'Node Selectors' }).getByRole('button', { name: 'Add Node Selector' }).click()
  await page.getByRole('group', { name: 'Node Selectors' }).getByRole('textbox', { name: 'Label' }).fill('disk')
  await page.getByRole('group', { name: 'Node Selectors' }).getByRole('textbox', { name: 'Value' }).fill('ssd')
  await page.getByRole('button', { name: 'Add Toleration' }).click()
  await page.getByRole('textbox', { name: 'Key', exact: true }).fill('db-only')
  await page.getByRole('combobox', { name: 'Operator', exact: true }).selectOption('Exists')

  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgstreams$/)
  const request = await lastRequest(page, 'POST', 'sgstreams')
  expect(request.body.spec.maxRetries).toBe(5)
  expect(request.body.spec.source.sgCluster.includes).toEqual(['public.*'])
  expect(request.body.spec.target.sgCluster).toEqual({
    name: 'minor-outdated',
    database: null,
    username: null,
    password: null,
    skipDdlImport: true,
    ddlImportRoleSkipFilter: 'postgres',
  })
  expect(request.body.spec.pods.resources.scheduling.nodeSelector).toEqual({ disk: 'ssd' })
  expect(request.body.spec.pods.resources.scheduling.tolerations).toEqual([
    { key: 'db-only', operator: 'Exists', effect: null, tolerationSeconds: null },
  ])
})

test('creates an instance profile with huge pages and container specs', async ({ page }) => {
  const name = unique('profile-e2e')
  await login(page)
  await page.goto('/admin/default/sginstanceprofiles/new')

  await page.getByLabel(/Profile Name/).fill(name)
  await page.getByLabel('RAM', { exact: false }).first().fill('4')
  await page.getByLabel('CPU', { exact: false }).first().fill('2')
  await page.getByRole('spinbutton', { name: /Huge Pages 2Mi/ }).fill('1')

  const steps = page.getByRole('navigation', { name: 'Form steps' })
  await steps.getByRole('button', { name: 'Containers', exact: true }).click()
  const row = page.locator('fieldset').filter({ hasText: 'Container #1' }).first()
  await row.getByRole('textbox', { name: 'Container Name' }).fill('pgbouncer')
  await row.getByLabel('RAM', { exact: false }).first().fill('512')
  await row.locator('select').first().selectOption('Mi')
  await row.getByLabel('CPU', { exact: false }).first().fill('250')
  await row.locator('select').nth(1).selectOption('m')

  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sginstanceprofiles$/)
  const request = await lastRequest(page, 'POST', 'sginstanceprofiles')
  expect(request.body.spec.cpu).toBe('2')
  expect(request.body.spec.memory).toBe('4Gi')
  expect(request.body.spec.hugePages).toEqual({ 'hugepages-2Mi': '1Gi' })
  expect(request.body.spec.containers).toEqual({
    pgbouncer: { cpu: '250m', memory: '512Mi' },
  })
})

test('creates an advanced distributed logs server with the exact payload', async ({ page }) => {
  const name = unique('logs-adv-e2e')
  await login(page)
  await page.goto('/admin/default/sgdistributedlogs/new')

  await page.getByLabel(/Server Name/).fill(name)
  await page.getByRole('combobox', { name: 'Profile', exact: true }).selectOption('testing')
  await page.getByRole('checkbox', { name: 'Advanced options' }).check()
  await page.getByRole('combobox', { name: 'Type', exact: true }).first().selectOption('LoadBalancer')
  await page.getByRole('textbox', { name: 'Load Balancer IP' }).first().fill('10.0.0.5')
  await page.getByRole('group', { name: 'All Resources' }).getByRole('button', { name: 'Add Annotation' }).click()
  await page.getByRole('group', { name: 'All Resources' }).getByRole('textbox', { name: 'Annotation' }).last().fill('team')
  await page.getByRole('group', { name: 'All Resources' }).getByRole('textbox', { name: 'Value' }).last().fill('db')
  await page.getByRole('combobox', { name: 'Cluster Pod Anti Affinity' }).selectOption('true')

  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgdistributedlogs$/)
  const request = await lastRequest(page, 'POST', 'sgdistributedlogs')
  expect(request.body.spec.profile).toBe('testing')
  expect(request.body.spec.postgresServices.primary).toEqual({
    type: 'LoadBalancer',
    loadBalancerIP: '10.0.0.5',
  })
  expect(request.body.spec.metadata.annotations.allResources).toEqual({ team: 'db' })
  expect(request.body.spec.nonProductionOptions.disableClusterPodAntiAffinity).toBe(true)
})

test('monitoring toggle interlocks metrics exporter and prometheus autobind', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgclusters/new')
  await page.getByRole('button', { name: /Custom/ }).click()
  await page.getByRole('checkbox', { name: 'Advanced options' }).check()
  const steps = page.getByRole('navigation', { name: 'Form steps' })

  await page.getByRole('checkbox', { name: 'Monitoring' }).check()
  await steps.getByRole('button', { name: 'Sidecars' }).click()
  await expect(page.getByRole('checkbox', { name: 'Metrics Exporter' })).toBeChecked()
  await expect(page.getByRole('checkbox', { name: 'Prometheus Autobind' })).toBeChecked()

  await page.getByRole('checkbox', { name: 'Metrics Exporter' }).uncheck()
  await steps.getByRole('button', { name: 'Cluster' }).click()
  await expect(page.getByRole('checkbox', { name: 'Monitoring' })).not.toBeChecked()
})

test('creates a babelfish-flavored cluster with the feature gate', async ({ page }) => {
  const name = unique('babel-e2e')
  await login(page)
  await page.goto('/admin/default/sgclusters/new')
  await page.getByRole('button', { name: /Custom/ }).click()

  await page.getByRole('textbox', { name: /Cluster Name/ }).fill(name)
  await page.getByRole('combobox', { name: 'Postgres Flavor' }).selectOption('babelfish')
  await page.getByRole('checkbox', { name: 'Babelfish Experimental Feature' }).check()
  await page.getByRole('button', { name: 'Create Cluster' }).click()

  await expect(page).toHaveURL(/\/admin\/default\/sgclusters$/)
  const request = await lastRequest(page, 'POST', 'sgclusters')
  expect(request.body.spec.postgres.flavor).toBe('babelfish')
  expect(request.body.spec.nonProductionOptions.enabledFeatureGates).toEqual(['babelfish-flavor'])
})
