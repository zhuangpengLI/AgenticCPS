import { expect, test, type Page, type Route } from '@playwright/test'

const mallBaseUrl = process.env.MALL_H5_BASE_URL?.replace(/\/$/, '')
const ok = (data: unknown = {}) => ({ code: 0, data, msg: '' })

async function openMall(page: Page, route: string, loggedIn = false) {
  await page.addInitScript((hasLogin) => {
    if (hasLogin) localStorage.setItem('token', 'mall-cps-e2e-token')
    else localStorage.removeItem('token')
  }, loggedIn)
  await page.goto(`${mallBaseUrl}/#${route}`)
}

function fulfill(route: Route, data: unknown = {}) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(ok(data))
  })
}

test.describe('mall H5 返利中心移动端闭环', () => {
  test.skip(!mallBaseUrl, '设置 MALL_H5_BASE_URL 后运行 mall H5 用例')
  test.use({ viewport: { width: 390, height: 844 } })

  test('游客可搜索、比价和查看详情，页面无横向溢出', async ({ page }) => {
    const goods = {
      platformCode: 'taobao',
      goodsId: '10001',
      goodsSign: 'goods-sign',
      title: '移动端返利测试商品',
      mainPic: '/static/goods-empty.png',
      actualPrice: 49.9,
      originalPrice: 69.9,
      couponPrice: 20,
      couponConditions: 50,
      couponRemainNum: 88,
      estimateRebateAmount: 3.25,
      monthSales: 1200
    }
    await page.route('**/app-api/**', async (route) => {
      const url = new URL(route.request().url())
      if (url.pathname.endsWith('/cps/goods/search'))
        return fulfill(route, { list: [goods], total: 1 })
      if (url.pathname.endsWith('/cps/goods/compare')) {
        return fulfill(route, {
          cheapestGoods: goods,
          highestRebateGoods: goods,
          bestOverallGoods: goods
        })
      }
      if (url.pathname.endsWith('/cps/goods/detail')) return fulfill(route, goods)
      return fulfill(route)
    })

    await openMall(page, '/pages/cps/goods')
    await page.getByPlaceholder('输入商品关键词').fill('手机')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await expect(page.getByText('移动端返利测试商品')).toBeVisible()
    await page.getByRole('button', { name: '跨平台比价' }).click()
    await expect(page.getByText('最低价')).toBeVisible()
    await page.getByText('移动端返利测试商品').click()
    await expect(page.getByText('优惠信息')).toBeVisible()
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)
    ).toBe(true)
  })

  test('游客点击领券只弹登录，不提前调用转链接口', async ({ page }) => {
    let linkRequests = 0
    const goods = {
      platformCode: 'taobao',
      goodsId: '10001',
      goodsSign: 'goods-sign',
      title: '游客返利商品',
      mainPic: '/static/goods-empty.png',
      actualPrice: 19.9,
      couponPrice: 5,
      estimateRebateAmount: 1.2
    }
    await page.route('**/app-api/**', async (route) => {
      const pathname = new URL(route.request().url()).pathname
      if (pathname.endsWith('/cps/goods/search')) return fulfill(route, { list: [goods], total: 1 })
      if (pathname.endsWith('/cps/goods/link')) linkRequests += 1
      return fulfill(route)
    })

    await openMall(page, '/pages/cps/goods')
    await page.getByPlaceholder('输入商品关键词').fill('耳机')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await page.getByRole('button', { name: '领券', exact: true }).click()
    await expect(page.getByText('短信登录')).toBeVisible()
    expect(linkRequests).toBe(0)
  })

  test('订单找回和提现超时重试复用幂等键，超额提现前端拦截', async ({ page }) => {
    const claimBodies: Record<string, unknown>[] = []
    const withdrawBodies: Record<string, unknown>[] = []
    await page.route('**/app-api/**', async (route) => {
      const request = route.request()
      const pathname = new URL(request.url()).pathname
      if (pathname.endsWith('/cps/order/page')) return fulfill(route, { list: [], total: 0 })
      if (pathname.endsWith('/cps/order/claim/list')) return fulfill(route, [])
      if (pathname.endsWith('/cps/order/claim') && request.method() === 'POST') {
        claimBodies.push(request.postDataJSON())
        if (claimBodies.length === 1) return route.abort('timedout')
        return fulfill(route, { status: 'PENDING_REVIEW' })
      }
      if (pathname.endsWith('/cps/rebate/account')) {
        return fulfill(route, { withdrawableBalance: 100, frozenBalance: 0, debtBalance: 0 })
      }
      if (pathname.endsWith('/cps/withdraw/page')) return fulfill(route, { list: [], total: 0 })
      if (pathname.endsWith('/cps/withdraw/create') && request.method() === 'POST') {
        withdrawBodies.push(request.postDataJSON())
        if (withdrawBodies.length === 1) return route.abort('timedout')
        return fulfill(route, 1)
      }
      return fulfill(route)
    })

    await openMall(page, '/pages/cps/order', true)
    await page.getByRole('button', { name: '找回订单' }).click()
    await page.getByPlaceholder('请输入平台订单号').fill('ORDER-RETRY-1')
    const claimButton = page.getByRole('button', { name: '提交审核' })
    await claimButton.click()
    await expect(claimButton).toBeEnabled()
    await claimButton.click()
    expect(claimBodies).toHaveLength(2)
    expect(claimBodies[1].idempotencyKey).toBe(claimBodies[0].idempotencyKey)

    await openMall(page, '/pages/cps/withdraw', true)
    await page.getByPlaceholder('0.00').fill('101')
    await page.getByPlaceholder('请输入收款账号').fill('safe-test-account')
    await page.getByRole('button', { name: '确认提现' }).click()
    expect(withdrawBodies).toHaveLength(0)
    await page.getByPlaceholder('0.00').fill('10')
    const withdrawButton = page.getByRole('button', { name: '确认提现' })
    await withdrawButton.click()
    await expect(withdrawButton).toBeEnabled()
    await withdrawButton.click()
    expect(withdrawBodies).toHaveLength(2)
    expect(withdrawBodies[1].idempotencyKey).toBe(withdrawBodies[0].idempotencyKey)
  })

  test('旧 commission 地址跳转后保留商品查询参数', async ({ page }) => {
    await page.route('**/app-api/**', (route) => fulfill(route))
    await openMall(
      page,
      '/pages/commission/goods-detail?platformCode=taobao&goodsId=10001&goodsSign=a%2Bb'
    )
    await expect(page).toHaveURL(/#\/pages\/cps\/goods-detail/)
    const url = page.url()
    expect(url).toContain('platformCode=taobao')
    expect(url).toContain('goodsId=10001')
    expect(url).toMatch(/goodsSign=(?:a%252Bb|a%2Bb)/)
  })
})
