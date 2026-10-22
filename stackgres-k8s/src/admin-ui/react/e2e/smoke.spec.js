import { test, expect } from '@playwright/test'

test('the console loads under /admin/ and reaches the API through the proxy', async ({ page }) => {
  await page.goto('/admin/')

  await expect(page.getByRole('heading', { name: 'StackGres Web Console' })).toBeVisible()
  await expect(page.getByTestId('namespaces')).toContainText('default')
})
