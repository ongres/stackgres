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

test('sharded cluster detail renders status, config and monitoring tabs', async ({ page }) => {
  await login(page)
  await page.goto('/admin/default/sgshardedcluster/shard-uptodate')

  await expect(page.getByRole('heading', { name: 'Sharded Cluster', exact: true })).toBeVisible()
  await expect(page.getByRole('rowheader', { name: 'Coordinator' })).toBeVisible()
  await expect(page.getByRole('link', { name: 'shard-uptodate-coord-0' })).toBeVisible()

  const tabs = page.getByRole('navigation', { name: 'Sharded cluster sections' })
  await tabs.getByRole('link', { name: 'Configuration' }).click()
  await expect(page.getByRole('heading', { name: 'Sharded Cluster Details' })).toBeVisible()

  await tabs.getByRole('link', { name: 'Monitoring' }).click()
  await expect(page).toHaveURL(/monitor\/shard-uptodate-coord-0\/db-info$/)
  await expect(page.locator('iframe')).toBeVisible()
})

test('creates a namespace through the path-only POST', async ({ page }) => {
  const name = `ns-e2e-${Date.now().toString(36)}`
  await login(page)
  await page.goto('/admin/namespaces/new')

  await page.getByRole('textbox', { name: /Namespace Name/ }).fill(name)
  await page.getByRole('button', { name: 'Create Namespace' }).click()

  await expect(page).toHaveURL(/\/admin\/?$/)
  const request = await lastRequest(page, 'POST', 'namespaces')
  expect(request.body).toEqual({ name })
})
