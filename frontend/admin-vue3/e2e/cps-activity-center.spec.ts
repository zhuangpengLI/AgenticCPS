import { expect, test } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

const ok = (data: unknown) => ({ code: 200, data, msg: '' })

const activityCards = [
  {
    id: 101,
    activityName: '大淘客 88VIP 专享',
    activityType: '会员权益',
    platformCode: 'taobao',
    platformName: '淘宝',
    shortDesc: '大淘客活动转链验证',
    rebateDesc: '最高返 20%',
    billingType: 'CPS',
    promotionCount: 30,
    sourceType: 'vendor',
    externalActivityId: 'dtk:101',
    tagText: '大淘客',
    jumpType: 'search',
    searchKeyword: '88VIP'
  },
  {
    id: 102,
    activityName: '好单库官方会场',
    activityType: '会员权益',
    platformCode: 'taobao',
    platformName: '淘宝',
    shortDesc: '好单库官方会场转链验证',
    rebateDesc: '官方活动佣金',
    billingType: 'CPS',
    promotionCount: 20,
    sourceType: 'vendor',
    externalActivityId: 'hdk:taobao:102',
    tagText: '好单库',
    jumpType: 'search',
    searchKeyword: '官方会场'
  },
  {
    id: 103,
    activityName: '运营搜索活动',
    activityType: '其他',
    platformCode: 'taobao',
    platformName: '淘宝',
    shortDesc: '仅提供站内商品搜索',
    rebateDesc: '以商品页为准',
    billingType: 'CPS',
    promotionCount: 10,
    sourceType: 'configured',
    externalActivityId: 'configured:103',
    tagText: '站内',
    jumpType: 'search',
    searchKeyword: '夏日清凉'
  },
  {
    id: 104,
    activityName: '供应商配置异常活动',
    activityType: '其他',
    platformCode: 'taobao',
    platformName: '淘宝',
    shortDesc: '验证失败状态不会伪装成功',
    rebateDesc: '不可转链',
    billingType: 'CPS',
    promotionCount: 0,
    sourceType: 'vendor',
    externalActivityId: 'dtk:104',
    tagText: '异常',
    jumpType: 'search',
    searchKeyword: '异常活动'
  }
]

const activityMenu = {
  id: 6201,
  name: '活动中心',
  type: 2,
  path: 'activity/square',
  component: 'cps/activity/square/index',
  componentName: 'CpsRebateActivitySquare',
  permission: 'cps:rebate-activity:query',
  visible: true,
  keepAlive: true,
  alwaysShow: true
}

const goodsSquareMenu = {
  id: 6218,
  name: '返利商品广场',
  type: 2,
  path: 'goods/square',
  component: 'cps/goods/square/index',
  componentName: 'CpsGoodsSquare',
  permission: 'cps:goods-square:query',
  visible: true,
  keepAlive: true,
  alwaysShow: true
}

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
          permissions: [
            'cps:rebate-activity:query',
            'cps:rebate-activity:create',
            'cps:rebate-activity:update',
            'cps:rebate-activity:delete',
            'cps:goods-square:query'
          ],
          menus: [
            {
              id: 6200,
              name: '联盟运营',
              type: 1,
              path: '/cps-ops',
              component: null,
              visible: true,
              keepAlive: true,
              alwaysShow: true,
              children: [activityMenu, goodsSquareMenu]
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
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok([activityMenu, goodsSquareMenu]))
    })
  )
}

async function mockGoodsSquareLanding(page: Page) {
  await page.route('**/admin-api/cps/goods-square/**', (route) => {
    const path = new URL(route.request().url()).pathname
    const data = path.endsWith('/meta')
      ? {
          platforms: [],
          vendors: [],
          activities: [],
          hotKeywords: [],
          categories: [],
          sortOptions: [],
          filterOptions: []
        }
      : path.endsWith('/search')
        ? { list: [], total: 0, pageNo: 1, pageSize: 20 }
        : []
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(data)) })
  })
}

async function mockActivityApi(
  page: Page,
  centerRequests: URLSearchParams[] = [],
  promotionBodies: Array<Record<string, unknown>> = []
) {
  await page.route('**/admin-api/cps/rebate-activity/center**', (route) => {
    const url = new URL(route.request().url())
    centerRequests.push(url.searchParams)
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(
        ok({
          tabs: [
            { platformCode: 'hot', platformName: '热门', activityCount: activityCards.length },
            { platformCode: 'taobao', platformName: '淘宝', activityCount: activityCards.length },
            { platformCode: 'jd', platformName: '京东', activityCount: 0 }
          ],
          billingTypeOptions: [
            { value: 'all', label: '全部', count: activityCards.length },
            { value: 'CPS', label: 'CPS', count: activityCards.length }
          ],
          cards: activityCards,
          total: activityCards.length,
          pageNo: 1,
          pageSize: 10
        })
      )
    })
  })

  await page.route('**/admin-api/cps/adzone/list-by-platform**', (route) =>
    route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(
        ok([
          {
            id: 1,
            platformCode: 'taobao',
            adzoneId: 'mm_123_456_789',
            adzoneName: '默认推广位',
            isDefault: 1,
            status: 1,
            createTime: '2026-07-30T00:00:00'
          }
        ])
      )
    })
  )

  await page.route('**/admin-api/cps/rebate-activity/promotion', async (route: Route) => {
    const body = route.request().postDataJSON() as Record<string, unknown>
    promotionBodies.push(body)
    const activityId = Number(body.activityId)
    const base = {
      activityId,
      activityName: activityCards.find((item) => item.id === activityId)?.activityName || '',
      platformCode: 'taobao',
      adzoneId: body.adzoneId,
      channelTag: body.channelTag
    }
    const resultById: Record<number, Record<string, unknown>> = {
      101: {
        ...base,
        linkStatus: 'SUCCESS',
        linkType: 'EXTERNAL_PROMOTION',
        linkMessage: '大淘客活动转链成功',
        promotionUrl: 'https://uland.taobao.com/coupon/edetail?e=dataoke-e2e',
        tpwd: '￥大淘客口令￥',
        promotionContent: '大淘客活动推广文案'
      },
      102: {
        ...base,
        linkStatus: 'SUCCESS',
        linkType: 'EXTERNAL_PROMOTION',
        linkMessage: '好单库活动转链成功',
        promotionUrl: 'https://s.click.taobao.com/haodanku-e2e',
        tpwd: '￥好单库口令￥',
        promotionContent: '好单库活动推广文案'
      },
      103: {
        ...base,
        linkStatus: 'INTERNAL_FALLBACK',
        linkType: 'INTERNAL_LANDING',
        linkMessage: '该活动仅提供站内商品搜索',
        promotionUrl:
          '/cps-ops/goods/square?platformCode=taobao&keyword=%E5%A4%8F%E6%97%A5%E6%B8%85%E5%87%89'
      },
      104: {
        ...base,
        linkStatus: 'FAILED',
        linkType: 'NONE',
        linkMessage: '大淘客配置缺失，无法生成活动推广链接'
      }
    }
    return route.fulfill({
      contentType: 'application/json',
      body: JSON.stringify(ok(resultById[activityId]))
    })
  })
}

async function openActivityCenter(page: Page) {
  await page.goto('/cps-ops/activity/square', { waitUntil: 'domcontentloaded', timeout: 90_000 })
  await expect(page.getByText('活动中心', { exact: true }).first()).toBeVisible({ timeout: 60_000 })
  await expect(page.getByRole('heading', { name: '大淘客 88VIP 专享' })).toBeVisible()
}

async function openPromotion(page: Page, activityName: string) {
  const card = page.getByRole('article').filter({ hasText: activityName })
  await card.getByRole('button', { name: '推广' }).click()
  const dialog = page.getByRole('dialog', { name: '生成活动推广内容' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '生成链接' }).click()
  return dialog
}

test.describe('CPS activity center promotion links', () => {
  test.describe.configure({ mode: 'serial', timeout: 120_000 })

  test.beforeEach(async ({ page }) => {
    await mockAdminBootstrap(page)
    await mockGoodsSquareLanding(page)
  })

  test('queries activities with the selected platform and keyword', async ({ page }) => {
    const centerRequests: URLSearchParams[] = []
    await mockActivityApi(page, centerRequests)
    await openActivityCenter(page)

    await page.locator('.platform-tab').filter({ hasText: '淘宝' }).click()
    await page.getByPlaceholder('请输入搜索关键字').fill('88VIP')
    await page.getByPlaceholder('请输入搜索关键字').press('Enter')

    await expect
      .poll(() =>
        centerRequests.some(
          (params) => params.get('platformCode') === 'taobao' && params.get('keyword') === '88VIP'
        )
      )
      .toBe(true)
    const matched = centerRequests.find(
      (params) => params.get('platformCode') === 'taobao' && params.get('keyword') === '88VIP'
    )
    expect(matched?.get('pageNo')).toBe('1')
    expect(matched?.get('pageSize')).toBe('10')
    expect(matched?.get('billingType')).toBe('all')
    expect(matched?.get('sortMode')).toBe('hot')
  })

  test('shows real Dataoke and Haodanku links as external successes', async ({ page }) => {
    const promotionBodies: Array<Record<string, unknown>> = []
    await mockActivityApi(page, [], promotionBodies)
    await openActivityCenter(page)

    const dataokeDialog = await openPromotion(page, '大淘客 88VIP 专享')
    await expect(dataokeDialog.locator('.el-alert--success')).toContainText('大淘客活动转链成功')
    await expect(dataokeDialog.getByRole('cell', { name: '活动链接' })).toBeVisible()
    await expect(
      dataokeDialog.getByRole('row').filter({ hasText: '活动链接' }).locator('input')
    ).toHaveValue('https://uland.taobao.com/coupon/edetail?e=dataoke-e2e')
    await dataokeDialog.getByRole('button', { name: '关闭', exact: true }).click()

    const haodankuDialog = await openPromotion(page, '好单库官方会场')
    await expect(haodankuDialog.locator('.el-alert--success')).toContainText('好单库活动转链成功')
    await expect(
      haodankuDialog.getByRole('row').filter({ hasText: '活动链接' }).locator('input')
    ).toHaveValue('https://s.click.taobao.com/haodanku-e2e')

    expect(promotionBodies).toHaveLength(2)
    expect(promotionBodies.every((body) => !('landingBaseUrl' in body))).toBe(true)
    expect(JSON.stringify(promotionBodies)).not.toContain('localhost')
  })

  test('labels an internal fallback and opens the canonical goods-square route', async ({
    page
  }) => {
    await mockActivityApi(page)
    await openActivityCenter(page)

    const dialog = await openPromotion(page, '运营搜索活动')
    await expect(dialog.locator('.el-alert--warning')).toContainText('该活动仅提供站内商品搜索')
    await expect(dialog.getByRole('cell', { name: '站内落地页' })).toBeVisible()
    await expect(
      dialog.getByRole('row').filter({ hasText: '站内落地页' }).locator('input')
    ).not.toHaveValue(/localhost/)
    await dialog.getByRole('button', { name: '打开站内落地页' }).click()

    await expect(page).toHaveURL(
      /\/cps-ops\/goods\/square\?platformCode=taobao&keyword=%E5%A4%8F%E6%97%A5%E6%B8%85%E5%87%89/,
      { timeout: 60_000 }
    )
  })

  test('shows a conversion failure without any copyable promotion table', async ({ page }) => {
    await mockActivityApi(page)
    await openActivityCenter(page)

    const dialog = await openPromotion(page, '供应商配置异常活动')
    await expect(dialog.locator('.el-alert--error')).toContainText(
      '大淘客配置缺失，无法生成活动推广链接'
    )
    await expect(dialog.getByRole('table')).toHaveCount(0)
    await expect(dialog.getByRole('button', { name: '复制推广文案' })).toHaveCount(0)
    await expect(dialog.getByRole('button', { name: '打开站内落地页' })).toHaveCount(0)
  })
})
