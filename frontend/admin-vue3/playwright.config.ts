import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL || 'http://127.0.0.1:5173'
const skipWebServer = process.env.E2E_SKIP_WEB_SERVER === 'true'
const browserChannel = process.env.E2E_BROWSER_CHANNEL || (process.env.CI ? undefined : 'msedge')
const headless = process.env.E2E_HEADLESS === 'false' ? false : undefined

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  outputDir: 'test-results',
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  use: {
    baseURL,
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
    headless,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], channel: browserChannel }
    }
  ],
  webServer: skipWebServer
    ? undefined
    : {
        command: 'pnpm dev:e2e',
        env: {
          ...process.env,
          GOMAXPROCS: process.env.GOMAXPROCS || '2',
          NODE_OPTIONS: process.env.NODE_OPTIONS || '--max-old-space-size=4096',
          VITE_E2E: 'true'
        },
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 180_000
      }
})
