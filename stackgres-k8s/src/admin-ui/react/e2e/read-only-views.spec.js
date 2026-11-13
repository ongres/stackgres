import { test, expect } from '@playwright/test'

async function login(page) {
  await page.goto('/admin/')
  await page.getByLabel('Username').fill('admin')
  await page.getByLabel('Password').fill('password')
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page.getByRole('heading', { name: 'Used Namespaces' })).toBeVisible()
}

test('namespace overview lists resources per kind', async ({ page }) => {
  await login(page)
  await page.getByTestId('namespaces').getByRole('link', { name: 'default', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Namespace Overview: default' })).toBeVisible()
  const clustersCard = page.locator('article', { hasText: 'SGCluster (' }).first()
  await expect(clustersCard.getByRole('link', { name: 'uptodate', exact: true })).toBeVisible()
})

test('cluster list renders stats columns and navigates to the tabs', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgclusters')
  await expect(page.getByRole('heading', { name: 'StackGres Clusters' })).toBeVisible()
  await expect(page.getByRole('columnheader', { name: 'Instances' })).toBeVisible()

  await page.getByRole('link', { name: 'uptodate' }).first().click()
  await expect(page).toHaveURL(/\/admin\/default\/sgcluster\/uptodate$/)
  await expect(page.getByRole('cell', { name: 'primary' }).first()).toBeVisible()

  await page.getByRole('link', { name: 'Events' }).click()
  await expect(page.getByRole('cell', { name: /Cluster created successfully/ }).first()).toBeVisible()

  await page.getByRole('link', { name: 'Configuration', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Cluster Details' })).toBeVisible()
})

test('resource detail renders spec and status trees', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sginstanceprofiles')
  await expect(page.getByRole('heading', { name: 'Instance Profiles' })).toBeVisible()
  const firstRow = page.locator('tbody tr').first()
  await firstRow.locator('a').first().click()
  await expect(page.getByRole('heading', { name: 'Profile Details' })).toBeVisible()
  await expect(page.getByText('Specs')).toBeVisible()
})

test('sidebar shows per-item submenus', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgclusters')
  const sidebar = page.getByRole('navigation', { name: 'Resources' })
  await expect(sidebar.getByRole('link', { name: 'uptodate', exact: true })).toBeVisible()
})

// Local-frame string for a datetime-local input; the test browser shares the
// host timezone, and the console default timezone setting is 'local'.
function toLocalInput(iso) {
  const date = new Date(iso)
  const pad = (part) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

test('logs view filters by date range', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/uptodate/logs')
  await expect(page.locator('li').filter({ hasText: 'checkpoint' }).first()).toBeVisible()

  await page.getByLabel('Logs from').fill(toLocalInput('2026-10-23T10:00:10Z'))
  await page.getByLabel('Logs to').fill(toLocalInput('2026-10-23T10:00:20Z'))

  // Both bounds are inclusive for plain timestamps (sub-second log times sort
  // after the bare 'from' second): seconds 10..20.
  await expect(page.locator('ul.font-mono > li')).toHaveCount(11)
  await page.getByRole('button', { name: 'Reset dates' }).click()
  await expect(page.locator('ul.font-mono > li')).toHaveCount(40)
})

test('backups list filters completed backups by stored date range', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgbackups')
  const rows = page.locator('tbody tr')
  await expect(rows.first()).toBeVisible()
  const allRows = await rows.count()
  expect(allRows).toBeGreaterThan(1)

  // Non-zero seconds: Chromium strips ':00' seconds from datetime-local
  // values, which Playwright's fill verification rejects as malformed.
  await page.getByLabel('Backups from').fill(toLocalInput('2026-05-31T00:00:01Z'))
  await page.getByLabel('Backups to').fill(toLocalInput('2026-05-31T23:59:59Z'))
  await expect(rows).toHaveCount(1)

  await page.getByRole('button', { name: 'Reset dates' }).click()
  await expect(rows).toHaveCount(allRows)
})
