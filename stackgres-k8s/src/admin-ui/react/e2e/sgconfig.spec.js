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

test('edits the operator configuration and asserts the exact PUT payload', async ({ page }) => {
  await login(page)
  await page.goto('/admin/sgconfig/stackgres-operator')

  await expect(
    page.getByRole('heading', { name: 'Operator Configuration: stackgres-operator' })
  ).toBeVisible()
  await page.getByRole('link', { name: 'Edit' }).click()
  await expect(page).toHaveURL(/\/admin\/sgconfig\/stackgres-operator\/edit$/)

  const sections = page.getByRole('navigation', { name: 'Configuration sections' })

  await page.getByRole('combobox', { name: 'Type', exact: true }).selectOption('LoadBalancer')
  await page.getByRole('textbox', { name: 'Load Balancer IP' }).fill('10.0.0.9')

  await sections.getByRole('button', { name: 'Grafana' }).click()
  await page.getByRole('checkbox', { name: 'Auto Embed' }).check()
  await page.getByRole('textbox', { name: 'User', exact: true }).fill('monitor')

  await sections.getByRole('button', { name: 'Service Account' }).click()
  await page.getByRole('button', { name: 'Add row' }).click()
  await page.getByRole('textbox', { name: 'Key', exact: true }).fill('owner')
  await page.getByRole('textbox', { name: 'Value', exact: true }).fill('ops')

  await page.getByRole('button', { name: 'Update Configuration' }).click()
  await expect(page.getByText('updated successfully')).toBeVisible()
  await expect(page).toHaveURL(/\/admin\/sgconfig\/stackgres-operator$/)

  const request = await lastRequest(page, 'PUT', 'sgconfigs')
  expect(request.body.metadata).toEqual({
    name: 'stackgres-operator',
    namespace: 'stackgres',
    uid: 'cccc0000-0000-0000-0000-000000000001',
    creationTimestamp: '2026-01-01T08:00:00Z',
  })
  expect(request.body.spec.operator).toEqual({
    image: { name: 'stackgres/operator', tag: '1.18.4' },
  })
  expect(request.body.spec.adminui.service.type).toBe('LoadBalancer')
  expect(request.body.spec.adminui.service.loadBalancerIP).toBe('10.0.0.9')
  expect(request.body.spec.adminui.resources).toBeUndefined()
  expect(request.body.spec.grafana.autoEmbed).toBe(true)
  expect(request.body.spec.grafana.user).toBe('monitor')
  expect(request.body.spec.serviceAccount.annotations).toEqual({ owner: 'ops' })
})
