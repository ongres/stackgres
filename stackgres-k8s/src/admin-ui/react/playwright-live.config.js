import { defineConfig } from '@playwright/test'

// Live-console configuration: runs the e2e-live suite against a deployed
// web console (the in-cluster `e2e/spec/ui` test), not the local mock.
// PLAYWRIGHT_BASE_URL must point at .../admin on the restapi service.
export default defineConfig({
  testDir: './e2e-live',
  timeout: Number(process.env.PLAYWRIGHT_TIMEOUT || 60000),
  retries: 1,
  use: {
    // Relative test URLs resolve against the base, so it must end in a slash.
    baseURL: (process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:8082/admin').replace(/\/?$/, '/'),
    ignoreHTTPSErrors: true,
    headless: true,
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
