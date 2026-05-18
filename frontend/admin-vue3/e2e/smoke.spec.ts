import { expect, test } from '@playwright/test'

test.describe('admin-vue3 smoke', () => {
  test('opens the login entry without a blank page', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('button', { name: /登录|Login/i })).toBeVisible()
    await expect(page.getByPlaceholder(/用户名|username/i)).toBeVisible()
    await expect(page.getByPlaceholder(/密码|password/i)).toBeVisible()
  })
})
