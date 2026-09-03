import type {
  OnboardingPageItem,
  PlatformOnboardingDraft,
  PlatformOnboardingSavePayload,
  SaveDraftReq,
  VendorForm,
  VendorSaveForm
} from '@/api/cps/platformOnboarding'

const trim = (value: string): string => value.trim()

const trimOptional = (value: string | null | undefined): string | undefined =>
  value == null ? undefined : value.trim()

const compareText = (left: string, right: string): number => {
  if (left === right) return 0
  return left < right ? -1 : 1
}

const normalizeVendorForSave = (vendor: VendorForm): VendorSaveForm => ({
  vendorCode: trim(vendor.vendorCode),
  vendorName: trim(vendor.vendorName),
  vendorType: trim(vendor.vendorType),
  platformCode: trim(vendor.platformCode),
  appKey: trimOptional(vendor.appKey) || trimOptional(vendor.apiKey),
  appSecret: trimOptional(vendor.appSecret),
  authToken: trimOptional(vendor.authToken),
  apiBaseUrl: trimOptional(vendor.apiBaseUrl),
  defaultAdzoneId: trimOptional(vendor.defaultAdzoneId),
  extraConfig: trimOptional(vendor.extraConfig),
  priority: vendor.priority,
  status: vendor.status,
  remark: trimOptional(vendor.remark)
})

const normalizePayload = (draft: PlatformOnboardingDraft): PlatformOnboardingSavePayload => ({
  platform: {
    id: draft.platform.id,
    platformCode: trim(draft.platform.platformCode),
    platformName: trim(draft.platform.platformName),
    platformLogo: trimOptional(draft.platform.platformLogo),
    defaultAdzoneId: trimOptional(draft.platform.defaultAdzoneId),
    platformServiceRate: draft.platform.platformServiceRate,
    sort: draft.platform.sort,
    status: draft.platform.status,
    extraConfig: trimOptional(draft.platform.extraConfig),
    remark: trimOptional(draft.platform.remark),
    activeVendorCode: trimOptional(draft.platform.activeVendorCode)
  },
  primaryVendorCode: trim(draft.primaryVendorCode),
  runtimeDefaultAdzoneId: trim(draft.runtimeDefaultAdzoneId),
  vendors: draft.vendors
    .map(normalizeVendorForSave)
    .sort((left, right) => compareText(left.vendorCode, right.vendorCode)),
  adzones: draft.adzones
    .map((adzone) => ({
      platformCode: trim(adzone.platformCode),
      adzoneId: trim(adzone.adzoneId),
      adzoneName: trimOptional(adzone.adzoneName),
      adzoneType: trimOptional(adzone.adzoneType),
      relationType: trimOptional(adzone.relationType),
      relationId: adzone.relationId,
      externalRelationId: trimOptional(adzone.externalRelationId),
      externalSpecialId: trimOptional(adzone.externalSpecialId),
      isDefault: adzone.isDefault,
      status: adzone.status
    }))
    .sort((left, right) => compareText(left.adzoneId, right.adzoneId)),
  rebateRules: draft.rebateRules
    .map((rule) => ({
      memberId: rule.memberId,
      memberLevelId: rule.memberLevelId,
      platformCode: trim(rule.platformCode || draft.platformCode || draft.platform.platformCode),
      rebateRate: rule.rebateRate,
      minRebateAmount: rule.minRebateAmount,
      maxRebateAmount: rule.maxRebateAmount,
      freezeThresholdAmount: rule.freezeThresholdAmount,
      freezeDays: rule.freezeDays,
      status: rule.status ?? 1,
      priority: rule.priority ?? 0
    }))
    .sort((left, right) => {
      const leftKey = [
        left.platformCode,
        left.memberLevelId ?? 'DEFAULT',
        left.memberId ?? 'DEFAULT',
        left.priority ?? 0
      ].join(':')
      const rightKey = [
        right.platformCode,
        right.memberLevelId ?? 'DEFAULT',
        right.memberId ?? 'DEFAULT',
        right.priority ?? 0
      ].join(':')
      return compareText(leftKey, rightKey)
    })
})

export const createEmptyDraft = (platformCode = ''): PlatformOnboardingDraft => {
  const normalizedPlatformCode = trim(platformCode)
  return {
    platformCode: normalizedPlatformCode,
    mode: 'CREATE',
    status: 'DRAFT',
    platform: {
      platformCode: normalizedPlatformCode,
      platformName: '',
      status: 0
    },
    primaryVendorCode: '',
    runtimeDefaultAdzoneId: '',
    vendors: [],
    adzones: [],
    rebateRules: []
  }
}

/**
 * Builds the exact save request and strips every response-only configured marker.
 * Stored credentials are represented by those markers but are merged server-side.
 */
export const normalizeDraftForSave = (draft: PlatformOnboardingDraft): SaveDraftReq => ({
  platformCode: trim(draft.platformCode || draft.platform.platformCode),
  draftVersion: draft.draftVersion,
  payload: normalizePayload(draft)
})

export const isDirty = (
  original: PlatformOnboardingDraft | null | undefined,
  current: PlatformOnboardingDraft
): boolean => {
  if (!original) return true
  const originalValue = {
    platformCode: trim(original.platformCode || original.platform.platformCode),
    payload: normalizePayload(original)
  }
  const currentValue = {
    platformCode: trim(current.platformCode || current.platform.platformCode),
    payload: normalizePayload(current)
  }
  return JSON.stringify(originalValue) !== JSON.stringify(currentValue)
}

export const completionLabel = (item: OnboardingPageItem): string => {
  const percent = Math.min(100, Math.max(0, Math.round(item.completionPercent ?? 0)))
  if (percent === 100) return '100% · 已完成'
  return `${percent}% · 待补 ${item.missingItems?.length ?? 0} 项`
}

export const stepForFieldPath = (fieldPath: string | null | undefined): number => {
  if (!fieldPath) return 0
  const path = fieldPath
    .replace(/^\$\.?/, '')
    .replace(/^(payload|runtimePayload|draftPayload)\./, '')
  if (path === 'primaryVendorCode' || path.startsWith('vendors')) return 1
  if (path === 'runtimeDefaultAdzoneId' || path.startsWith('adzones')) return 2
  if (path.startsWith('rebateRules')) return 3
  if (
    path.startsWith('checkResult') ||
    path === 'status' ||
    path.includes('Fingerprint')
  ) {
    return 4
  }
  return 0
}

const hasConfiguredField = (vendor: VendorForm, fieldName: string): boolean =>
  vendor.configuredFields?.includes(fieldName) ?? false

/** Clears credential values while retaining the safe evidence used by masked inputs. */
export const maskConfiguredSecrets = (draft: PlatformOnboardingDraft): PlatformOnboardingDraft => ({
  ...draft,
  platform: { ...draft.platform, extraConfig: undefined },
  vendors: draft.vendors.map((vendor) => {
    const appKeyConfigured = Boolean(
      vendor.appKeyConfigured ||
        vendor.apiKeyConfigured ||
        trimOptional(vendor.appKey) ||
        trimOptional(vendor.apiKey) ||
        hasConfiguredField(vendor, 'appKey') ||
        hasConfiguredField(vendor, 'apiKey')
    )
    return {
      ...vendor,
      appKey: undefined,
      apiKey: undefined,
      appSecret: undefined,
      authToken: undefined,
      extraConfig: undefined,
      appKeyConfigured,
      apiKeyConfigured: appKeyConfigured,
      appSecretConfigured: Boolean(
        vendor.appSecretConfigured ||
          trimOptional(vendor.appSecret) ||
          hasConfiguredField(vendor, 'appSecret')
      ),
      authTokenConfigured: Boolean(
        vendor.authTokenConfigured ||
          trimOptional(vendor.authToken) ||
          hasConfiguredField(vendor, 'authToken')
      ),
      apiBaseUrlConfigured: Boolean(
        vendor.apiBaseUrlConfigured ||
          trimOptional(vendor.apiBaseUrl) ||
          hasConfiguredField(vendor, 'apiBaseUrl')
      ),
      configuredFields: [...(vendor.configuredFields ?? [])]
    }
  }),
  adzones: draft.adzones.map((adzone) => ({ ...adzone })),
  rebateRules: draft.rebateRules.map((rule) => ({
    ...rule,
    status: rule.status ?? 1,
    priority: rule.priority ?? 0
  })),
  checkResult: draft.checkResult
    ? {
        ...draft.checkResult,
        items: draft.checkResult.items.map((item) => ({ ...item }))
      }
    : undefined
})

export function amountCentToYuan(value: null): null
export function amountCentToYuan(value: undefined): undefined
export function amountCentToYuan(value: number): number
export function amountCentToYuan(value: number | null | undefined): number | null | undefined {
  if (value == null) return value
  return Math.round(value) / 100
}

export function amountYuanToCent(value: null): null
export function amountYuanToCent(value: undefined): undefined
export function amountYuanToCent(value: number): number
export function amountYuanToCent(value: number | null | undefined): number | null | undefined {
  if (value == null) return value
  const sign = value < 0 ? -1 : 1
  return sign * Math.round((Math.abs(value) + Number.EPSILON) * 100)
}
