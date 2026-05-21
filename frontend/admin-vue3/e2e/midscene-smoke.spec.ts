import { expect, test } from './fixture'

const hasMidsceneModelConfig = Boolean(
  process.env.MIDSCENE_MODEL_API_KEY &&
    process.env.MIDSCENE_MODEL_BASE_URL &&
    process.env.MIDSCENE_MODEL_NAME &&
    process.env.MIDSCENE_MODEL_FAMILY
)

test.describe('admin-vue3 Midscene smoke', () => {
  test.skip(
    !hasMidsceneModelConfig,
    'Set MIDSCENE_MODEL_API_KEY, MIDSCENE_MODEL_BASE_URL, MIDSCENE_MODEL_NAME, and MIDSCENE_MODEL_FAMILY to run Midscene-assisted E2E.'
  )

  test('recognizes the login entry with AI while keeping deterministic assertions', async ({
    page,
    aiAssert
  }) => {
    await page.goto('/')

    await expect(page).toHaveURL(/\/login/)
    await aiAssert('The page is a login screen with username and password fields and a login button.')
    await expect(page.getByRole('button', { name: /^(登录|Login)$/i })).toBeVisible()
    await expect(page.locator('input[placeholder="请输入用户名"]').first()).toBeVisible()
    await expect(page.locator('input[placeholder="请输入密码"]').first()).toBeVisible()
  })
})
