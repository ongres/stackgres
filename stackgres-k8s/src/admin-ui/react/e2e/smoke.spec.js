import { test, expect } from '@playwright/test'

test('login, namespace navigation and RBAC-gated sidebar against the mock API', async ({
  page,
}) => {
  await page.goto('/admin/')

  await expect(page.getByRole('heading', { name: 'StackGres' })).toBeVisible()
  await page.getByLabel('Username').fill('admin')
  await page.getByLabel('Password').fill('password')
  await page.getByRole('button', { name: 'Login' }).click()

  await expect(page.getByRole('heading', { name: 'Namespaces' })).toBeVisible()
  await expect(page.getByTestId('namespaces')).toContainText('default')

  await page.getByRole('link', { name: 'default', exact: true }).click()
  await expect(page).toHaveURL(/\/admin\/default$/)

  const sidebar = page.getByRole('navigation', { name: 'Resources' })
  await expect(sidebar.getByRole('link', { name: 'StackGres Clusters', exact: true })).toBeVisible()
  await expect(sidebar.getByRole('link', { name: 'Database Operations', exact: true })).toBeVisible()

  await sidebar.getByRole('link', { name: 'StackGres Clusters', exact: true }).click()
  await expect(page).toHaveURL(/\/admin\/default\/sgclusters$/)
  await expect(page.getByRole('heading', { name: 'sgclusters' })).toBeVisible()

  await page.getByRole('button', { name: 'Logout' }).click()
  await expect(page.getByRole('button', { name: 'Login' })).toBeVisible()
})
