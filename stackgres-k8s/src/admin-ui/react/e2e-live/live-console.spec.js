import { test, expect } from '@playwright/test'

// Smoke suite against a real deployed console (see playwright-live.config.js).
// Credentials and the seeded cluster come from the environment; every write
// cleans up after itself.
const USERNAME = process.env.LIVE_USERNAME || 'admin'
const PASSWORD = process.env.LIVE_PASSWORD || ''
const NAMESPACE = process.env.LIVE_NAMESPACE || 'default'
const CLUSTER = process.env.LIVE_CLUSTER || ''

async function login(page) {
  await page.goto('./')
  await page.getByLabel('Username').fill(USERNAME)
  await page.getByLabel('Password').fill(PASSWORD)
  await page.getByRole('button', { name: 'Login' }).click()
  await expect(page.getByRole('heading', { name: 'Used Namespaces' })).toBeVisible()
}

test('logs in and shows the global dashboard', async ({ page }) => {
  await login(page)
  await expect(page.getByTestId('namespaces')).toBeVisible()
})

test('shows the StackGres version from sg-info.json', async ({ page }) => {
  await login(page)
  const info = await page.evaluate(() =>
    fetch('/admin/info/sg-info.json').then((response) => response.json())
  )
  expect(typeof info.version).toBe('string')
  expect(info.version.length).toBeGreaterThan(0)
  await expect(page.getByText(info.version).first()).toBeVisible()
})

test('lists the seeded cluster and renders its detail tabs', async ({ page }) => {
  test.skip(!CLUSTER, 'LIVE_CLUSTER not provided')
  await login(page)
  await page.goto(`./${NAMESPACE}/sgclusters`)
  await expect(page.getByRole('link', { name: CLUSTER, exact: true }).first()).toBeVisible()

  await page.goto(`./${NAMESPACE}/sgcluster/${CLUSTER}`)
  await expect(page.getByRole('cell', { name: 'primary' }).first()).toBeVisible()
  await page.getByRole('link', { name: 'Configuration' }).click()
  await expect(page.getByRole('heading', { name: 'Cluster Details' })).toBeVisible()
})

test('creates and deletes a postgres configuration end to end', async ({ page }) => {
  const name = `live-pgconfig-${Date.now().toString(36)}`
  await login(page)
  await page.goto(`./${NAMESPACE}/sgpgconfigs/new`)

  await page.getByLabel(/Configuration Name/).fill(name)
  await page.getByLabel(/Postgres Version/).selectOption({ index: 1 })
  await page.getByLabel(/Parameters/).fill('max_connections=150')
  await page.getByRole('button', { name: 'Create' }).click()
  await expect(page).toHaveURL(new RegExp(`/${NAMESPACE}/sgpgconfigs$`))
  await expect(page.getByRole('link', { name, exact: true }).first()).toBeVisible()

  await page.goto(`./${NAMESPACE}/sgpgconfig/${name}`)
  await page.getByRole('button', { name: 'Delete' }).click()
  const dialog = page.getByRole('dialog', { name: 'Delete resource' })
  await dialog.getByLabel('Confirm resource name').fill(name)
  await dialog.getByRole('button', { name: 'Delete Item' }).click()
  await expect(page).toHaveURL(new RegExp(`/${NAMESPACE}/sgpgconfigs$`))
})

test('unknown resources render the not-found view inside the app', async ({ page }) => {
  await login(page)
  await page.goto(`./${NAMESPACE}/sgcluster/does-not-exist-${Date.now().toString(36)}`)
  await expect(page.getByText('was not found')).toBeVisible()
})

test('logout returns to the login form', async ({ page }) => {
  await login(page)
  await page.getByRole('button', { name: 'Logout' }).click()
  await expect(page.getByLabel('Username')).toBeVisible()
})
