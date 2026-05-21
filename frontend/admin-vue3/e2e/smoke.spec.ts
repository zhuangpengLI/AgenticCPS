import { expect, test } from '@playwright/test'

test.describe('admin-vue3 smoke', () => {
  test('opens the login entry without a blank page', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('button', { name: /^(登录|Login)$/i })).toBeVisible()
    await expect(page.locator('input[placeholder="请输入用户名"]').first()).toBeVisible()
    await expect(page.locator('input[placeholder="请输入密码"]').first()).toBeVisible()
  })
})
