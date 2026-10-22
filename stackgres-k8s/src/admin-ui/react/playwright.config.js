import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:8082',
    headless: true,
    screenshot: 'only-on-failure',
  },
  webServer: {
    command: 'npm run dev:mock',
    port: 8082,
    reuseExistingServer: true,
    timeout: 30000,
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
