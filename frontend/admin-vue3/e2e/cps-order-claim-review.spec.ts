import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

const ok = (data: unknown) => ({ code: 200, data, msg: '' })

const orderMenu = {
  id: 6223,
  name: 'CPS订单',
  type: 2,
  path: 'order',
  component: 'cps/order/index',
  componentName: 'CpsOrder',
  permission: 'cps:order:query',
  visible: true,
  keepAlive: true,
  alwaysShow: true
}

const claims = [
  {
    id: 9101,
    orderId: 8101,
    platformCode: 'taobao',
    platformOrderId: 'TB-CLAIM-20260730',
    candidateMemberId: 10001,
    result: 'PENDING_REVIEW',
    reviewStatus: 'PENDING_REVIEW',
    rejectReason: '订单号只能定位订单，需要人工核验归属',
    createTime: '2026-07-30T20:00:00'
  },
  {
    id: 9102,
    orderId: 8102,
    platformCode: 'eleme',
    platformOrderId: 'ELM-CLAIM-20260730',
    candidateMemberId: 10002,
    result: 'PENDING_REVIEW',
    reviewStatus: 'PENDING_REVIEW',
    rejectReason: '闪购订单未匹配到唯一有效 SID',
    createTime: '2026-07-30T20:01:00'
  }
]

async function mockAdminBootstrap(page: Page) {
  await page.addInitScript(() => {
    const expires = Date.now() + 60 * 60 * 1000
    const put = (key: string, value: unknown) =>
      localStorage.setItem(
        key,
        JSON.stringify({ c: Date.now(), e: expires, v: JSON.stringify(value) })
      )
    put('ACCESS_TOKEN', 'e2e-token')
    put('REFRESH_TOKEN', 'e2e-refresh')
    put('tenantId', 1)
  })

  await page.route('**/admin-api/**', (route) =>
    route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(null)) })
  )
  await page.route('**/admin-api/system/auth/get-permission-info', (route) =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(
        ok({
          user: { id: 1, nickname: 'E2E管理员', avatar: '', deptId: 1 },
          roles: ['admin'],
          permissions: ['cps:order:query', 'cps:order:attribution-bind'],
          menus: [
            {
              id: 6286,
              name: '联盟结算',
              type: 1,
              path: '/cps-settlement',
              component: null,
              visible: true,
              keepAlive: true,
              alwaysShow: true,
              children: [orderMenu]
            }
          ]
        })
      )
    })
  )
  await page.route('**/admin-api/system/auth/refresh-token**', (route) =>
    route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok('e2e-token')) })
  )
  await page.route('**/admin-api/system/menu/**', (route) =>
    route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok([orderMenu])) })
  )
}

test.describe('CPS order claim review', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test('approves and rejects pending claims with auditable notes', async ({ page }) => {
    await mockAdminBootstrap(page)
    const reviewBodies: Array<Record<string, unknown>> = []

    await page.route('**/admin-api/cps/order/page**', (route) =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ list: [], total: 0 }))
      })
    )
    await page.route('**/admin-api/cps/order/claim/page**', (route) => {
      const reviewStatus = new URL(route.request().url()).searchParams.get('reviewStatus')
      const list = claims.filter((claim) => claim.reviewStatus === reviewStatus)
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({ list, total: list.length }))
      })
    })
    await page.route('**/admin-api/cps/order/claim/review', async (route) => {
      const body = route.request().postDataJSON() as Record<string, unknown>
      reviewBodies.push(body)
      const claim = claims.find((item) => item.id === Number(body.claimId))
      if (claim) claim.reviewStatus = body.approved ? 'APPROVED' : 'REJECTED'
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(
          ok({
            status: claim?.reviewStatus,
            message: body.approved ? '订单申领审核通过' : '订单申领已拒绝'
          })
        )
      })
    })

    await page.goto('/cps-settlement/order', { waitUntil: 'domcontentloaded', timeout: 90_000 })
    await expect(page.getByText('TB-CLAIM-20260730')).toBeVisible({ timeout: 60_000 })
    await expect(page.getByText('ELM-CLAIM-20260730')).toBeVisible()

    const taobaoRow = page.getByRole('row').filter({ hasText: 'TB-CLAIM-20260730' })
    await taobaoRow.getByRole('button', { name: '通过' }).click()
    const approveDialog = page.getByRole('dialog', { name: '通过订单申领' })
    await approveDialog.getByPlaceholder('请输入联盟后台核验依据').fill('联盟后台订单明细与会员截图一致')
    await approveDialog.getByRole('button', { name: '确认通过' }).click()
    await expect(approveDialog).toBeHidden()
    await expect(page.getByText('TB-CLAIM-20260730')).toHaveCount(0)

    const elemeRow = page.getByRole('row').filter({ hasText: 'ELM-CLAIM-20260730' })
    await elemeRow.getByRole('button', { name: '拒绝' }).click()
    const rejectDialog = page.getByRole('dialog', { name: '拒绝订单申领' })
    await rejectDialog.getByPlaceholder('请输入联盟后台核验依据').fill('订单截图的下单账号与申领信息不一致')
    await rejectDialog.getByRole('button', { name: '确认拒绝' }).click()
    await expect(rejectDialog).toBeHidden()
    await expect(page.getByText('ELM-CLAIM-20260730')).toHaveCount(0)

    expect(reviewBodies).toEqual([
      {
        claimId: 9101,
        approved: true,
        auditNote: '联盟后台订单明细与会员截图一致'
      },
      {
        claimId: 9102,
        approved: false,
        auditNote: '订单截图的下单账号与申领信息不一致'
      }
    ])
  })
})
