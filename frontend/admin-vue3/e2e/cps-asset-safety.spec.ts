import { expect, test } from '@playwright/test'

test.describe('CPS asset safety admin modules', () => {
  test('Vite compiles the asset center and controlled freeze pages', async ({ request }) => {
    const assetPage = await request.get('/src/views/cps/asset/index.vue')
    expect(assetPage.ok()).toBeTruthy()
    const assetModule = await assetPage.text()
    expect(assetModule).toContain('CpsAssetSafety')
    expect(assetModule).toContain('submitAdjust')
    expect(assetModule).toContain('saveAssetPolicy')
    expect(assetModule).toContain('bootstrapPolicy')
    expect(assetModule).toContain('confirmMigration')
    expect(assetModule).toContain('启用返利资产')
    expect(assetModule).toContain('message.confirm')
    expect(assetModule).not.toContain('message.prompt')
    expect(assetModule).not.toContain('资产 V2')
    expect(assetModule).not.toContain('资产V2')

    const freezePage = await request.get('/src/views/cps/freeze/index.vue')
    expect(freezePage.ok()).toBeTruthy()
    const freezeModule = await freezePage.text()
    expect(freezeModule).toContain('manualUnfreeze')
    expect(freezeModule).toContain('idempotencyKey')
  })
})
