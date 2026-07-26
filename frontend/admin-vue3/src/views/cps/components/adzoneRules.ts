import type { AdzoneForm } from '@/api/cps/platformOnboarding'

export type AdzoneKind = 'GENERAL' | 'CHANNEL' | 'MEMBER'

/** Normalise legacy values before they enter a draft. */
export const normalizeAdzoneType = (value?: string): AdzoneKind => {
  const normalized = (value || '').trim().toUpperCase()
  if (normalized === 'CHANNEL' || normalized === 'RELATION') return 'CHANNEL'
  if (normalized === 'MEMBER' || normalized === 'SPECIAL') return 'MEMBER'
  return 'GENERAL'
}

export const isEnabled = (row: Pick<AdzoneForm, 'status'>): boolean => row.status === 1

export const isGeneralAdzone = (row: Pick<AdzoneForm, 'adzoneType'>): boolean =>
  normalizeAdzoneType(row.adzoneType) === 'GENERAL'

export const validateAdzoneRow = (row: Partial<AdzoneForm>): string[] => {
  const errors: string[] = []
  if (!row.adzoneId?.trim()) errors.push('推广位 ID 不能为空')
  const kind = normalizeAdzoneType(row.adzoneType)
  if (kind === 'CHANNEL' && !row.externalRelationId?.trim() && !row.relationId) {
    errors.push('渠道推广位需要 relationId 或 externalRelationId')
  }
  if (kind === 'MEMBER' && !row.externalSpecialId?.trim()) {
    errors.push('会员推广位需要 externalSpecialId')
  }
  return errors
}

export const validateAdzoneDraft = (
  rows: AdzoneForm[],
  runtimeDefaultAdzoneId: string
): string[] => {
  const errors = rows.flatMap((row, index) =>
    validateAdzoneRow(row).map((message) => `第 ${index + 1} 行：${message}`)
  )
  const enabledGeneral = rows.filter(
    (row) => isEnabled(row) && isGeneralAdzone(row)
  )
  if (enabledGeneral.length === 0) errors.push('至少需要一个启用的通用推广位')
  if (!runtimeDefaultAdzoneId?.trim()) errors.push('请选择运行时默认推广位')
  if (runtimeDefaultAdzoneId && !rows.some((row) => row.adzoneId === runtimeDefaultAdzoneId && isEnabled(row))) {
    errors.push('运行时默认推广位必须来自启用的推广位')
  }
  return errors
}

/** Parse pasted rows without calling the legacy partial-success endpoint. */
export const parseAdzoneBatch = (text: string, platformCode: string): AdzoneForm[] => {
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [adzoneId = '', adzoneName = '', adzoneType = 'GENERAL', relationId = '', externalRelationId = '', externalSpecialId = ''] = line.split(/[\t,，]/)
      return {
        platformCode,
        adzoneId: adzoneId.trim(),
        adzoneName: adzoneName.trim() || undefined,
        adzoneType: normalizeAdzoneType(adzoneType),
        relationId: relationId.trim() ? Number(relationId) : undefined,
        externalRelationId: externalRelationId.trim() || undefined,
        externalSpecialId: externalSpecialId.trim() || undefined,
        isDefault: 0,
        status: 1
      }
    })
}

export const normalizeAdzoneRow = (row: AdzoneForm): AdzoneForm => ({
  ...row,
  platformCode: row.platformCode.trim(),
  adzoneId: row.adzoneId.trim(),
  adzoneName: row.adzoneName?.trim() || undefined,
  adzoneType: normalizeAdzoneType(row.adzoneType),
  externalRelationId: row.externalRelationId?.trim() || undefined,
  externalSpecialId: row.externalSpecialId?.trim() || undefined
})
