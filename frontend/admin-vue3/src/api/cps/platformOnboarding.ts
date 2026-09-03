import request from '@/config/axios'

export type OnboardingMode = 'CREATE' | 'RECONFIGURE'
export type OnboardingStatus = 'DRAFT' | 'VALIDATING' | 'READY' | 'FAILED' | 'PUBLISHED'
export type OnboardingPageStatus = 'ALL' | 'INCOMPLETE' | 'READY' | 'ENABLED' | 'FAILED'

export interface PlatformForm {
  id?: number
  platformCode: string
  platformName: string
  platformLogo?: string
  defaultAdzoneId?: string
  platformServiceRate?: number
  sort?: number
  status: number
  extraConfig?: string
  extraConfigConfigured?: boolean
  remark?: string
  activeVendorCode?: string
}

export interface VendorForm {
  vendorCode: string
  vendorName: string
  vendorType: string
  platformCode: string
  /** New credential input only. The server never returns the stored value. */
  appKey?: string
  /** Compatibility alias for schema-driven forms that call an app key an API key. */
  apiKey?: string
  /** New credential input only. The server never returns the stored value. */
  appSecret?: string
  /** New credential input only. The server never returns the stored value. */
  authToken?: string
  apiBaseUrl?: string
  appKeyConfigured: boolean
  apiKeyConfigured: boolean
  appSecretConfigured: boolean
  authTokenConfigured: boolean
  apiBaseUrlConfigured: boolean
  defaultAdzoneId?: string
  extraConfig?: string
  extraConfigConfigured: boolean
  configuredFields: string[]
  priority?: number
  status: number
  remark?: string
}

export interface AdzoneForm {
  platformCode: string
  adzoneId: string
  adzoneName?: string
  adzoneType?: string
  relationType?: string
  relationId?: number
  externalRelationId?: string
  externalSpecialId?: string
  isDefault: number
  status: number
}

export interface RebateRuleForm {
  memberId?: number
  memberLevelId?: number
  platformCode: string
  rebateRate?: number
  /** API amount values are yuan (the backend VO uses BigDecimal). */
  minRebateAmount?: number
  /** API amount values are yuan (the backend VO uses BigDecimal). */
  maxRebateAmount?: number
  /** Actual rebate above this amount is frozen; API amount values are yuan. */
  freezeThresholdAmount?: number
  /** Number of days to keep qualifying rebate frozen before release. */
  freezeDays?: number
  status: number
  priority?: number
}

export interface OnboardingCheckItem {
  code: string
  fieldPath?: string
  message: string
  section?: string
}

export interface OnboardingCheckResult {
  success: boolean
  items: OnboardingCheckItem[]
}

export interface PlatformOnboardingPayload {
  platform: PlatformForm
  primaryVendorCode: string | null
  runtimeDefaultAdzoneId: string | null
  vendors: VendorForm[]
  adzones: AdzoneForm[]
  rebateRules: RebateRuleForm[]
}

export type PlatformSaveForm = Omit<PlatformForm, 'extraConfigConfigured'>
export type VendorSaveForm = Omit<
  VendorForm,
  | 'apiKey'
  | 'appKeyConfigured'
  | 'apiKeyConfigured'
  | 'appSecretConfigured'
  | 'authTokenConfigured'
  | 'apiBaseUrlConfigured'
  | 'extraConfigConfigured'
  | 'configuredFields'
>

export interface PlatformOnboardingSavePayload {
  platform: PlatformSaveForm
  primaryVendorCode: string
  runtimeDefaultAdzoneId: string
  vendors: VendorSaveForm[]
  adzones: AdzoneForm[]
  rebateRules: RebateRuleForm[]
}

/** Local workspace shape. It deliberately contains no stored credential values. */
export interface PlatformOnboardingDraft extends PlatformOnboardingPayload {
  id?: number
  platformCode: string
  primaryVendorCode: string
  runtimeDefaultAdzoneId: string
  mode: 'CREATE' | 'RECONFIGURE'
  draftVersion?: number
  configFingerprint?: string
  validatedFingerprint?: string
  status: 'DRAFT' | 'VALIDATING' | 'READY' | 'FAILED' | 'PUBLISHED'
  checkResult?: OnboardingCheckResult
  checkSummary?: string
  validatedAt?: string
  publishedAt?: string
}

/** Exact response envelope returned by CpsPlatformOnboardingDetailRespVO. */
export interface PlatformOnboardingDetail {
  id?: number
  platformCode: string
  mode: OnboardingMode
  draftVersion?: number
  configFingerprint?: string
  validatedFingerprint?: string
  status: OnboardingStatus
  checkSummary?: string
  validatedAt?: string
  publishedAt?: string
  payload?: PlatformOnboardingPayload
  runtimePayload?: PlatformOnboardingPayload
  draftPayload?: PlatformOnboardingPayload
}

export interface OnboardingPageReq {
  pageNo: number
  pageSize: number
  keyword?: string
  platformName?: string
  platformCode?: string
  status?: OnboardingPageStatus
}

export interface OnboardingPageItem {
  platformCode: string
  platformName: string
  primaryVendorCode?: string
  backupVendorCount: number
  runtimeDefaultAdzoneId?: string
  defaultRebateRate?: number
  completionPercent: number
  missingItems: string[]
  connectionStatus?: string
  runtimeStatus?: number
  draftStatus?: OnboardingStatus
  updateTime?: string | number
}

export interface SaveDraftReq {
  platformCode: string
  draftVersion?: number
  payload: PlatformOnboardingSavePayload
}

export interface DeleteDraftReq {
  platformCode: string
  draftVersion?: number
}

export interface ValidateReq {
  payload: PlatformOnboardingSavePayload
}

export interface PublishReq {
  platformCode: string
  draftVersion: number
  configFingerprint: string
  enableAfterPublish: boolean
}

export interface VendorConfigField {
  name: string
  required: boolean
  sensitive: boolean
}

export interface VendorConfigSchema {
  fields: VendorConfigField[]
}

export interface VendorRetryPolicy {
  maxAttempts: number
  initialBackoffMillis: number
  maxBackoffMillis: number
  idempotentOnly: boolean
  retryOnTimeout: boolean
  retryOnRateLimit: boolean
  retryOnBusinessError: boolean
}

export interface VendorGovernancePolicy {
  timeoutMillis: number
  rateLimitPerMinute: number
  circuitBreakerFailureThreshold: number
  circuitBreakerOpenMillis: number
  tokenRefreshSupported: boolean
  metricsEnabled: boolean
  maskedDiagnosticsEnabled: boolean
  retryPolicy: VendorRetryPolicy
}

export interface VendorDescriptor {
  vendorCode: string
  platformCode: string
  vendorType: string
  capabilities: string[]
  configSchema: VendorConfigSchema
  governancePolicy: VendorGovernancePolicy
  sdkModule?: string
  version?: string
}

export interface PlatformCapability {
  platformCode: string
  platformName?: string
  capabilities: string[]
  vendors: VendorDescriptor[]
}

export const PlatformOnboardingApi = {
  getPage: (params: OnboardingPageReq) =>
    request.get<PageResult<OnboardingPageItem>>({
      url: '/cps/platform-onboarding/page',
      params
    }),

  getDetail: (platformCode: string) =>
    request.get<PlatformOnboardingDetail>({
      url: '/cps/platform-onboarding/get',
      params: { platformCode }
    }),

  saveDraft: (data: SaveDraftReq) =>
    request.post<PlatformOnboardingDetail>({
      url: '/cps/platform-onboarding/draft',
      data
    }),

  deleteDraft: (platformCode: string, draftVersion?: number) =>
    request.delete<boolean>({
      url: '/cps/platform-onboarding/draft',
      params: { platformCode, draftVersion }
    }),

  validate: (data: ValidateReq) =>
    request.post<OnboardingCheckResult>({
      url: '/cps/platform-onboarding/validate',
      data
    }),

  test: (platformCode: string, draftVersion: number) =>
    request.post<OnboardingCheckResult>({
      url: '/cps/platform-onboarding/test',
      data: { platformCode, draftVersion }
    }),

  testVendor: (platformCode: string, draftVersion: number, vendorCode: string) =>
    request.post<OnboardingCheckResult>({
      url: '/cps/platform-onboarding/test-vendor',
      data: { platformCode, draftVersion, vendorCode }
    }),

  publish: (data: PublishReq) =>
    request.post<PlatformOnboardingDetail>({
      url: '/cps/platform-onboarding/publish',
      data
    }),

  enable: (platformCode: string) =>
    request.put<boolean>({
      url: '/cps/platform-onboarding/enable',
      data: { platformCode }
    }),

  disable: (platformCode: string) =>
    request.put<boolean>({
      url: '/cps/platform-onboarding/disable',
      data: { platformCode }
    }),

  deleteBundle: (platformCode: string) =>
    request.delete<boolean>({
      url: '/cps/platform-onboarding/delete',
      params: { platformCode }
    }),

  getPlatformCapabilities: (platformCode?: string) =>
    request.get<PlatformCapability[]>({
      url: '/cps/platform-onboarding/platform-capabilities',
      params: { platformCode }
    }),

  getVendorDescriptors: (platformCode?: string) =>
    request.get<VendorDescriptor[]>({
      url: '/cps/platform-onboarding/vendor-descriptors',
      params: { platformCode }
    })
}
