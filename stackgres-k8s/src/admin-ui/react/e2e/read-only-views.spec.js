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
  await expect(clustersCard.getByRole('link', { name: 'uptodate' })).toBeVisible()
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

  await page.getByRole('link', { name: 'Configuration' }).click()
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
