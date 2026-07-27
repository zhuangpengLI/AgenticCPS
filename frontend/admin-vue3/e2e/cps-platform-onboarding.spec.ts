import { expect, test } from '@playwright/test'
import type { Page, Route } from '@playwright/test'

const ok = (data: unknown) => ({ code: 200, data, msg: '' })

type PlatformScenario = {
  platformCode: string
  platformName: string
  vendorCode: string
  vendorName: string
  rebateRate: number
}

const platformScenarios: PlatformScenario[] = [
  { platformCode: 'taobao', platformName: '淘宝', vendorCode: 'dataoke', vendorName: '大淘客', rebateRate: 80 },
  { platformCode: 'jd', platformName: '京东', vendorCode: 'dataoke', vendorName: '大淘客', rebateRate: 75 },
  { platformCode: 'pdd', platformName: '拼多多', vendorCode: 'dataoke', vendorName: '大淘客', rebateRate: 70 },
  { platformCode: 'douyin', platformName: '抖音', vendorCode: 'official', vendorName: '抖音官方', rebateRate: 65 }
]

const scenarioFor = (platformCode = 'taobao') => platformScenarios.find((item) => item.platformCode === platformCode) || platformScenarios[0]
const vendorName = (vendorCode: string, platformCode: string) => vendorCode === 'haodanku' ? '好单库' : vendorCode === 'official' ? `${scenarioFor(platformCode).platformName}官方` : '大淘客'
const payload = (vendor = 'dataoke', platformCode = 'taobao') => ({
  platform: { platformCode, platformName: scenarioFor(platformCode).platformName, status: 0 },
  primaryVendorCode: vendor,
  runtimeDefaultAdzoneId: 'pid-default',
  vendors: [{
    vendorCode: vendor,
    vendorName: vendorName(vendor, platformCode),
    vendorType: vendor === 'official' ? 'official' : 'aggregator',
    platformCode,
    appKeyConfigured: true,
    apiKeyConfigured: true,
    appSecretConfigured: vendor !== 'haodanku',
    authTokenConfigured: false,
    apiBaseUrlConfigured: true,
    extraConfigConfigured: false,
    configuredFields: vendor === 'haodanku' ? ['appKey', 'apiBaseUrl'] : ['appKey', 'appSecret'],
    priority: 1,
    status: 1
  }],
  adzones: [{ platformCode, adzoneId: 'pid-default', adzoneName: '默认位', adzoneType: 'GENERAL', isDefault: 1, status: 1 }],
  rebateRules: [{ platformCode, rebateRate: scenarioFor(platformCode).rebateRate, status: 1, priority: 0 }]
})

const detail = (vendor = 'dataoke', overrides: Record<string, unknown> = {}, platformCode = 'taobao') => ({
  platformCode, mode: 'RECONFIGURE', draftVersion: 3,
  configFingerprint: 'fp-3', validatedFingerprint: undefined, status: 'DRAFT',
  payload: payload(vendor, platformCode), runtimePayload: payload(platformCode === 'douyin' ? 'official' : 'dataoke', platformCode), draftPayload: payload(vendor, platformCode), ...overrides
})

async function mockAdminBootstrapAndMenu(page: Page, permissions = [
  'cps:platform-onboarding:query', 'cps:platform-onboarding:create',
  'cps:platform-onboarding:update', 'cps:platform-onboarding:test',
  'cps:platform-onboarding:publish', 'cps:platform-onboarding:delete'
]) {
  await page.addInitScript(() => {
    const expires = Date.now() + 60 * 60 * 1000
    const put = (key: string, value: unknown) => localStorage.setItem(key, JSON.stringify({ c: Date.now(), e: expires, v: JSON.stringify(value) }))
    put('ACCESS_TOKEN', 'e2e-token')
    put('REFRESH_TOKEN', 'e2e-refresh')
    put('tenantId', 1)
  })
  await page.route('**/admin-api/system/auth/get-permission-info', (route) => route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify(ok({
      user: { id: 1, nickname: 'E2E管理员', avatar: '', deptId: 1 }, roles: ['admin'], permissions,
      menus: [{ id: 6287, name: '联盟配置', type: 1, path: '/cps-config', component: null, children: [{
        id: 6297, name: '平台配置中心', type: 2, path: 'platform-onboarding',
        component: 'cps/platformOnboarding/index', componentName: 'CpsPlatformOnboarding', permission: 'cps:platform-onboarding:query',
        visible: true, keepAlive: true, alwaysShow: true
      }] }]
    }))
  }))
  await page.route('**/admin-api/system/auth/login', (route) => route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify(ok({ accessToken: 'e2e-token', refreshToken: 'e2e-refresh' }))
  }))
  await page.route('**/admin-api/system/menu/**', (route) => route.fulfill({
    contentType: 'application/json',
    body: JSON.stringify(ok([{ id: 6297, name: '平台配置中心', type: 2, path: 'platform-onboarding', component: 'cps/platformOnboarding/index', componentName: 'CpsPlatformOnboarding', visible: true, keepAlive: true, alwaysShow: true }]))
  }))
  await page.route('**/admin-api/system/auth/refresh-token**', (route) => route.fulfill({
    contentType: 'application/json', body: JSON.stringify(ok({ accessToken: 'e2e-token', refreshToken: 'e2e-refresh' }))
  }))
}

async function mockOnboardingApi(page: Page, options: { testStatus?: 'FAILED' | 'SUCCESS'; runtimeVendor?: string; draftVendor?: string; staleConflict?: boolean; nullRuntimeDefaultAdzone?: boolean; trace?: string[] } = {}) {
  let current = detail(options.draftVendor || options.runtimeVendor || 'dataoke', { status: 'DRAFT' })
  let runtimeVendor = options.runtimeVendor || 'dataoke'
  if (options.nullRuntimeDefaultAdzone) {
    const incompletePayload = { ...payload(options.draftVendor || options.runtimeVendor || 'dataoke'), runtimeDefaultAdzoneId: null }
    current = detail(options.draftVendor || options.runtimeVendor || 'dataoke', {
      status: 'DRAFT',
      payload: incompletePayload,
      runtimePayload: incompletePayload,
      draftPayload: incompletePayload
    })
  }
  await page.route('**/admin-api/cps/platform-onboarding/**', async (route: Route) => {
    const url = new URL(route.request().url())
    const path = url.pathname
    const method = route.request().method()
    if (path.endsWith('/page')) {
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok({
        list: [{ platformCode: 'taobao', platformName: '淘宝', primaryVendorCode: runtimeVendor, backupVendorCount: 1,
          runtimeDefaultAdzoneId: 'pid-default', defaultRebateRate: 80, completionPercent: 100, missingItems: [],
          connectionStatus: 'SUCCESS', runtimeStatus: 1, draftStatus: options.draftVendor ? 'DRAFT' : undefined, updateTime: 1784604831000 }], total: 1
      })) })
    }
    if (path.endsWith('/platform-capabilities')) return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(platformScenarios.map((item) => ({ platformCode: item.platformCode, platformName: item.platformName, capabilities: ['SEARCH'], vendors: [] })))) })
    if (path.endsWith('/vendor-descriptors')) {
      const descriptorFor = (vendorCode: string, platformCode: string) => ({ vendorCode, platformCode, vendorType: vendorCode === 'official' ? 'official' : 'aggregator', capabilities: ['SEARCH'], configSchema: { fields: vendorCode === 'haodanku' ? [{ name: 'appKey', required: true, sensitive: true }, { name: 'apiBaseUrl', required: true, sensitive: false }] : [{ name: 'appKey', required: true, sensitive: true }, { name: 'appSecret', required: true, sensitive: true }] }, governancePolicy: { timeoutMillis: 1000, rateLimitPerMinute: 60, circuitBreakerFailureThreshold: 3, circuitBreakerOpenMillis: 1000, tokenRefreshSupported: false, metricsEnabled: true, maskedDiagnosticsEnabled: true, retryPolicy: { maxAttempts: 1, initialBackoffMillis: 1, maxBackoffMillis: 1, idempotentOnly: true, retryOnTimeout: false, retryOnRateLimit: false, retryOnBusinessError: false } } })
      const descriptors = [descriptorFor('dataoke', 'taobao'), descriptorFor('haodanku', 'taobao'), descriptorFor('dataoke', 'jd'), descriptorFor('dataoke', 'pdd'), descriptorFor('official', 'douyin')]
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(descriptors)) })
    }
    if (path.endsWith('/get')) return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(current)) })
    if (path.endsWith('/draft') && method === 'POST') {
      if (options.staleConflict) {
        return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ code: 409, data: null, msg: '草稿版本冲突' }) })
      }
      const body = route.request().postDataJSON() as { platformCode?: string; payload?: any }
      const savedPayload = body.payload || (current as any).payload
      const platformCode = body.platformCode || savedPayload?.platform?.platformCode || (current as any).platformCode
      options.trace?.push(`draft:${platformCode}`)
      current = { ...current, platformCode, draftVersion: (current.draftVersion as number) + 1, configFingerprint: 'fp-next', validatedFingerprint: undefined, status: 'DRAFT', payload: savedPayload, draftPayload: savedPayload }
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(current)) })
    }
    if (path.endsWith('/test')) {
      const body = route.request().postDataJSON() as { platformCode?: string }
      options.trace?.push(`test:${body.platformCode}`)
      const success = options.testStatus !== 'FAILED'
      return route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify(ok({
          success,
          items: [{ section: '供应商', code: 'VENDOR_CONNECTION', message: success ? '连接成功（已脱敏）' : '凭证无效（已脱敏）' }]
        }))
      })
    }
    if (path.endsWith('/publish')) {
      const body = route.request().postDataJSON() as { platformCode?: string; enableAfterPublish?: boolean }
      options.trace?.push(`publish:${body.platformCode}:${Boolean(body.enableAfterPublish)}`)
      const publishedPayload = { ...(current as any).draftPayload, primaryVendorCode: options.draftVendor || (current as any).draftPayload.primaryVendorCode }
      if (body.enableAfterPublish) runtimeVendor = publishedPayload.primaryVendorCode
      current = { ...current, status: 'PUBLISHED', validatedFingerprint: current.configFingerprint, payload: publishedPayload, runtimePayload: body.enableAfterPublish ? publishedPayload : (current as any).runtimePayload, draftPayload: publishedPayload, runtimeStatus: body.enableAfterPublish ? 1 : 0 }
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(current)) })
    }
    if (path.endsWith('/disable') || path.endsWith('/enable') || path.endsWith('/delete')) return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok(true)) })
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify(ok({})) })
  })
}

async function openPlatformCenter(page: Page) {
  await page.goto('/cps-config/platform-onboarding')
  await expect(page.getByText('平台配置中心').first()).toBeVisible()
}

async function openReconfigureWorkspace(page: Page, platformCode = 'taobao') {
  await page.goto(`/cps-config/platform-onboarding?mode=edit&platformCode=${platformCode}`)
  await expect(page.getByText('平台接入工作台')).toBeVisible()
}

async function fillMinimumDraft(page: Page, scenario = platformScenarios[0]) {
  await page.getByLabel('平台编码').click()
  await page.getByRole('option', { name: scenario.platformName }).click()
  await page.getByLabel('平台名称').fill(scenario.platformName)
  await page.getByRole('button', { name: '下一步' }).click()

  await page.getByRole('button', { name: '添加供应商' }).click()
  await page.getByLabel('供应商编码').click()
  await page.getByRole('option', { name: scenario.vendorCode, exact: true }).click()
  await page.getByLabel('供应商名称').fill(scenario.vendorName)
  await page.getByLabel('appKey').fill('e2e-key')
  await page.getByLabel('appSecret').fill('e2e-secret')
  await page.getByRole('button', { name: '确定' }).click()
  await page.getByRole('button', { name: '下一步' }).click()

  await page.getByRole('button', { name: '添加推广位' }).click()
  await page.getByLabel('推广位 ID').fill(`pid-${scenario.platformCode}`)
  await page.getByLabel('名称').fill('默认位')
  await page.getByRole('button', { name: '确定' }).click()
  await page.getByRole('button', { name: '下一步' }).click()

  await page.getByRole('button', { name: '添加返利规则' }).click()
  await expect(page.getByLabel('优先级')).toBeVisible()
  await expect(page.getByLabel('优先级')).toHaveValue('0')
  await page.getByLabel('返利比例').fill(String(scenario.rebateRate))
  await page.getByRole('button', { name: '确定' }).click()
  await page.getByRole('button', { name: '下一步' }).click()
}

test.describe('CPS platform onboarding', () => {
  test('advances past rebate configuration when an incomplete draft has a null runtime default adzone', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page)
    await mockOnboardingApi(page, { nullRuntimeDefaultAdzone: true })
    await openReconfigureWorkspace(page)
    const pageError = new Promise<string | null>((resolve) => {
      const timeout = setTimeout(() => resolve(null), 1000)
      page.once('pageerror', (error) => {
        clearTimeout(timeout)
        resolve(error.message)
      })
    })
    await page.getByText('返利配置', { exact: true }).click()
    await page.getByRole('button', { name: '下一步' }).click()
    expect(await pageError).toBeNull()
    await expect(page.getByRole('button', { name: '连接测试' })).toBeVisible()
  })

  test('keeps required rebate fields visible and advances to review', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page)
    await mockOnboardingApi(page)
    await openPlatformCenter(page)
    await expect(page.getByText('2026-07-21 11:33:51')).toBeVisible()
    await page.getByRole('button', { name: '接入新平台' }).click()
    await fillMinimumDraft(page)
    await expect(page.getByRole('button', { name: '连接测试' })).toBeVisible()
  })

  for (const scenario of platformScenarios) {
    test(`completes save, connection test, and enabled publication for ${scenario.platformName}`, async ({ page }) => {
      const trace: string[] = []
      await mockAdminBootstrapAndMenu(page)
      await mockOnboardingApi(page, { trace })
      await openPlatformCenter(page)
      await page.getByRole('button', { name: '接入新平台' }).click()
      await fillMinimumDraft(page, scenario)
      await page.getByRole('button', { name: '连接测试' }).click()
      await expect(page.getByText('连接测试通过')).toBeVisible()
      await expect(page.getByRole('button', { name: '发布并启用' })).toBeEnabled()
      await page.getByRole('button', { name: '发布并启用' }).click()
      await expect(page.getByText('已发布并启用')).toBeVisible()
      expect(trace).toEqual([
        `draft:${scenario.platformCode}`,
        `test:${scenario.platformCode}`,
        `publish:${scenario.platformCode}:true`
      ])
    })
  }

  test('saves a failed first-time setup as an incomplete draft', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page); await mockOnboardingApi(page, { testStatus: 'FAILED' }); await openPlatformCenter(page)
    await page.getByRole('button', { name: '接入新平台' }).click(); await expect(page.getByText('平台信息')).toBeVisible(); await fillMinimumDraft(page)
    await page.getByRole('button', { name: '连接测试' }).click(); await expect(page.getByText('连接测试失败')).toBeVisible()
    await page.getByRole('button', { name: '保存草稿' }).click(); await expect(page.getByText('草稿已保存').last()).toBeVisible(); await expect(page.getByRole('button', { name: '发布并启用' })).toBeDisabled()
  })

  test('keeps runtime summary until a reconfiguration draft is published', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page); await mockOnboardingApi(page, { runtimeVendor: 'dataoke', draftVendor: 'haodanku', testStatus: 'SUCCESS' }); await openReconfigureWorkspace(page)
    await expect(page.getByRole('cell', { name: '淘宝 / 大淘客', exact: true })).toBeVisible(); await expect(page.getByRole('cell', { name: '淘宝 / 好单库（可继续完善）', exact: true })).toBeVisible()
    await page.getByText('检测与启用', { exact: true }).click(); await page.getByRole('button', { name: '连接测试' }).click(); await page.getByRole('button', { name: '发布并启用' }).click(); await expect(page.getByRole('cell', { name: 'haodanku', exact: true })).toBeVisible()
  })

  test('resumes an existing draft and exposes backup vendor management', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page); await mockOnboardingApi(page, { draftVendor: 'haodanku' }); await openPlatformCenter(page)
    await page.getByRole('button', { name: '配置', exact: true }).click(); await expect(page.getByText('平台接入工作台')).toBeVisible(); await page.getByText('API供应商', { exact: true }).click(); await expect(page.getByRole('cell', { name: '好单库', exact: true })).toBeVisible(); await expect(page.getByText('备用供应商')).toBeVisible()
  })

  test('recognizes Haodanku apikey-only credentials as configured', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page)
    await mockOnboardingApi(page, { runtimeVendor: 'dataoke', draftVendor: 'haodanku' })
    await openReconfigureWorkspace(page)
    await page.getByText('API供应商', { exact: true }).click()
    const haodankuRow = page.getByRole('row').filter({ hasText: '好单库' })
    await expect(haodankuRow.getByText('已配置（已脱敏）')).toBeVisible()
    await haodankuRow.getByRole('button', { name: '测试' }).click()
    await expect(page.getByText('检查成功')).toBeVisible()
  })

  test('changes runtime default adzone and validates advanced rebate rules', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page); await mockOnboardingApi(page); await openReconfigureWorkspace(page); await page.getByText('推广位', { exact: true }).click(); await expect(page.getByText('运行时默认推广位只能有一个')).toBeVisible(); await page.getByRole('button', { name: '添加推广位' }).click(); await expect(page.getByRole('dialog', { name: '推广位' })).toBeVisible(); await page.getByRole('button', { name: '取消' }).click(); await page.getByText('返利配置', { exact: true }).click(); await page.getByRole('button', { name: '添加返利规则' }).click(); await expect(page.getByRole('dialog', { name: '返利规则' })).toBeVisible(); await expect(page.getByText('平台工作台仅支持平台默认和会员等级规则')).toBeVisible()
  })

  test('surfaces stale draft version conflict without publishing', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page); await mockOnboardingApi(page, { staleConflict: true }); await openReconfigureWorkspace(page); await page.getByText('平台信息', { exact: true }).click(); await page.getByLabel('平台名称').fill('淘宝-冲突'); await page.getByText('检测与启用', { exact: true }).click(); await page.getByRole('button', { name: '连接测试' }).click(); await expect(page.getByText('草稿版本冲突')).toBeVisible(); await expect(page.getByRole('button', { name: '发布并启用' })).toBeDisabled()
  })

  test('separates disable-before-delete actions', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page); await mockOnboardingApi(page); await openPlatformCenter(page); await expect(page.getByRole('button', { name: '禁用' })).toBeVisible(); await expect(page.getByText('删除运行配置')).toHaveCount(0)
  })

  test('hides mutation actions when permissions are absent', async ({ page }) => {
    await mockAdminBootstrapAndMenu(page, ['cps:platform-onboarding:query']); await mockOnboardingApi(page); await openPlatformCenter(page); await expect(page.getByRole('button', { name: '接入新平台' })).toHaveCount(0); await expect(page.getByRole('button', { name: '配置', exact: true })).toHaveCount(0)
  })
})
