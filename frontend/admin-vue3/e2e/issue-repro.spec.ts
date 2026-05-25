import { expect, test } from '@playwright/test'

test.describe('issue reproduction template', () => {
  test.skip('replace this test with the user issue reproduction steps', async ({ page }) => {
    await page.goto('/')

    // 1. Arrange: prepare test data, route, account, or API mocks.
    // 2. Act: execute the exact user-reported steps.
    // 3. Assert: capture the broken behavior before fixing code.
    await expect(page).toHaveTitle(/AgenticCPS|AgenticCPS|管理后台/)
  })
})
