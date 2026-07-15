import { expect, test } from '@playwright/test'

test.describe('admin-vue3 smoke', () => {
  test('opens the login entry without a blank page', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveURL(/\/login/)
    await expect(page.getByRole('button', { name: /^(登录|Login)$/i })).toBeVisible()
    await expect(page.locator('input[placeholder="请输入租户名称"]').first()).toHaveValue(
      'AgenticCPS'
    )
    await expect(page.locator('input[placeholder="请输入用户名"]').first()).toBeVisible()
    await expect(page.locator('input[placeholder="请输入密码"]').first()).toBeVisible()
    await expect(page.getByText('萌新必读')).toHaveCount(0)
  })

  test('uses AgenticCPS when an old tenant name is cached', async ({ page }) => {
    await page.addInitScript(() => {
      const cachedLoginForm = {
        tenantName: '芋道源码',
        username: '',
        password: '',
        rememberMe: true
      }
      localStorage.setItem(
        'loginForm',
        JSON.stringify({
          c: Date.now(),
          e: Date.now() + 60_000,
          v: JSON.stringify(cachedLoginForm)
        })
      )
    })

    await page.goto('/login')

    await expect(page.locator('input[placeholder="请输入租户名称"]').first()).toHaveValue(
      'AgenticCPS'
    )
  })
})
