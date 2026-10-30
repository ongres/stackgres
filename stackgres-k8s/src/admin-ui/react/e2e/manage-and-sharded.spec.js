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

test('user management lists roles, cluster roles and users', async ({ page }) => {
  await login(page)
  await page.goto('/admin/manage/users')

  await expect(page.getByRole('heading', { name: 'Roles', exact: true })).toBeVisible()
  await expect(page.getByRole('link', { name: 'reader' }).first()).toBeVisible()
  await expect(page.getByRole('link', { name: 'stackgres-restapi-admin' }).first()).toBeVisible()
  await expect(page.getByRole('link', { name: 'admin', exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'get, list, watch' })).toBeVisible()
})

test('creates a user with role bindings and asserts the exact payload', async ({ page }) => {
  const name = `user-e2e-${Date.now().toString(36)}`
  await login(page)
  await page.goto('/admin/manage/users/new')

  await page.getByRole('textbox', { name: /User Name/ }).fill(name)
  await page.getByRole('textbox', { name: /K8s Username/ }).fill('jane')
  await page.getByRole('textbox', { name: 'Password', exact: true }).fill('secret123')
  await page.getByRole('checkbox', { name: 'Bind reader' }).check()
  await page.getByRole('checkbox', { name: 'Bind stackgres-restapi-admin' }).check()
  await page.getByRole('button', { name: 'Create User' }).click()

  await expect(page).toHaveURL(/\/admin\/manage\/users$/)
  const request = await lastRequest(page, 'POST', 'users')
  expect(request.body).toEqual({
    metadata: { namespace: '', name },
    k8sUsername: 'jane',
    password: 'secret123',
    apiUsername: null,
    apiUsernameNotBlank: false,
    roles: [{ name: 'reader', namespace: 'default' }],
    clusterRoles: [{ name: 'stackgres-restapi-admin' }],
  })
})

test('creates a role with rules and asserts the exact payload', async ({ page }) => {
  const name = `role-e2e-${Date.now().toString(36)}`
  await login(page)
  await page.goto('/admin/manage/roles/new')

  await page.getByRole('textbox', { name: /Role Name/ }).fill(name)
  await page.getByRole('combobox', { name: /Role Namespace/ }).selectOption('default')
  await page.getByRole('button', { name: 'Add Rule' }).click()
  const addEntry = (group) =>
    page.getByRole('group', { name: group, exact: true }).getByRole('button', { name: 'Add New' }).click()
  await addEntry('apiGroups')
  await page.getByRole('textbox', { name: 'apiGroups 1' }).fill('stackgres.io')
  await addEntry('resources')
  await page.getByRole('textbox', { name: 'resources 1' }).fill('sgclusters')
  await addEntry('verbs')
  await page.getByRole('textbox', { name: 'verbs 1' }).fill('get')
  await page.getByRole('button', { name: 'Create Role' }).click()

  await expect(page).toHaveURL(/\/admin\/manage\/users$/)
  const request = await lastRequest(page, 'POST', 'roles')
  expect(request.body).toEqual({
    metadata: { namespace: 'default', name },
    rules: [
      {
        apiGroups: ['stackgres.io'],
        nonResourceURLs: null,
        resourceNames: null,
        resources: ['sgclusters'],
        verbs: ['get'],
      },
    ],
  })
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
