import { test, expect } from '@playwright/test'

async function login(page) {
  await page.goto('/admin/')
  await page.getByLabel('Username').fill('admin')
  await page.getByLabel('Password').fill('password')
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page.getByRole('heading', { name: 'Used Namespaces' })).toBeVisible()
}

test('monitoring tab embeds grafana with pod and range selection', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/uptodate')

  await page.getByRole('link', { name: 'Monitoring' }).click()
  // No pod in the URL: redirected to the primary pod and default dashboard.
  await expect(page).toHaveURL(/\/monitor\/uptodate-0\/db-info$/)

  const iframe = page.locator('iframe[title^="Grafana"]')
  await expect(iframe).toHaveAttribute(
    'src',
    /\/d\/mock-db-info\/db-info\?theme=(light|dark)&kiosk&var-instance=10\.244\.\d+\.\d+&var-pod=uptodate-0$/
  )

  await page.getByLabel('Dashboard').selectOption('queries')
  await expect(page).toHaveURL(/\/monitor\/uptodate-0\/queries$/)

  await page.getByLabel('Time range').selectOption('last-1-hour')
  await expect(page).toHaveURL(/\/monitor\/uptodate-0\/queries\/last-1-hour$/)
  await expect(iframe).toHaveAttribute('src', /&from=now-1h&to=now$/)
})

test('unknown dashboard in the URL falls back to the default', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgcluster/uptodate/monitor/uptodate-0/not-a-dashboard')
  await expect(page).toHaveURL(/\/monitor\/uptodate-0\/db-info$/)
})

test('benchmark dbops detail renders the pgbench charts', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgdbop/pgbench-sample')
  await expect(page.getByRole('heading', { name: 'Operation Details' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'TPS over-time' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Statements' })).toBeVisible()
  await expect(page.locator('.apexcharts-canvas').first()).toBeVisible()
})
