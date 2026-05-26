import { expect, test } from '@playwright/test'
import { readFile } from 'node:fs/promises'
import { resolve } from 'node:path'

test('DocAlert does not render page-level documentation prompts', async () => {
  const source = await readFile(resolve('src/components/DocAlert/index.vue'), 'utf-8')

  expect(source).not.toContain('<el-alert')
  expect(source).not.toContain('doc.iocoder.cn')
})
