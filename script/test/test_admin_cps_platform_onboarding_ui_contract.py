from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


def read_utf8(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def test_platform_onboarding_api_exposes_the_complete_backend_route_contract():
    source = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")

    for route in [
        "/cps/platform-onboarding/page",
        "/cps/platform-onboarding/get",
        "/cps/platform-onboarding/draft",
        "/cps/platform-onboarding/validate",
        "/cps/platform-onboarding/test",
        "/cps/platform-onboarding/publish",
        "/cps/platform-onboarding/enable",
        "/cps/platform-onboarding/disable",
        "/cps/platform-onboarding/delete",
        "/cps/platform-onboarding/platform-capabilities",
        "/cps/platform-onboarding/vendor-descriptors",
    ]:
        assert route in source

    for api_function in [
        "getPage:",
        "getDetail:",
        "saveDraft:",
        "deleteDraft:",
        "validate:",
        "test:",
        "publish:",
        "enable:",
        "disable:",
        "deleteBundle:",
        "getPlatformCapabilities:",
        "getVendorDescriptors:",
    ]:
        assert api_function in source

    assert "draftVersion?: number" in source
    assert "params: { platformCode, draftVersion }" in source


def test_draft_and_safe_credential_types_match_the_onboarding_contract():
    source = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")

    for declaration in [
        "platformCode: string",
        "mode: 'CREATE' | 'RECONFIGURE'",
        "configFingerprint?: string",
        "validatedFingerprint?: string",
        "status: 'DRAFT' | 'VALIDATING' | 'READY' | 'FAILED' | 'PUBLISHED'",
        "platform: PlatformForm",
        "primaryVendorCode: string",
        "runtimeDefaultAdzoneId: string",
        "vendors: VendorForm[]",
        "adzones: AdzoneForm[]",
        "rebateRules: RebateRuleForm[]",
        "checkResult?: OnboardingCheckResult",
        "appKeyConfigured: boolean",
        "appSecretConfigured: boolean",
        "authTokenConfigured: boolean",
        "apiBaseUrlConfigured: boolean",
        "configuredFields: string[]",
        "appSecret?: string",
        "authToken?: string",
    ]:
        assert declaration in source


def test_form_model_exports_normalization_masking_and_amount_helpers():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/model.ts")

    for helper in [
        "createEmptyDraft",
        "normalizeDraftForSave",
        "isDirty",
        "completionLabel",
        "stepForFieldPath",
        "maskConfiguredSecrets",
        "amountCentToYuan",
        "amountYuanToCent",
    ]:
        assert f"export const {helper}" in source or f"export function {helper}" in source

    assert "Math.round" in source
    assert ".toLowerCase()" not in source


def test_platform_and_vendor_options_are_not_hard_coded_in_the_local_model():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/model.ts")

    for forbidden in [
        "PLATFORM_CODE_OPTIONS",
        "VENDOR_CODE_OPTIONS",
        "'taobao'",
        "'dataoke'",
        "'haodanku'",
    ]:
        assert forbidden not in source


def test_platform_center_exposes_required_actions_and_permissions():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/index.vue")

    for label in ["接入新平台", "配置完整度", "连接状态", "运行状态", "备用供应商"]:
        assert label in source

    for permission in [
        "cps:platform-onboarding:create",
        "cps:platform-onboarding:update",
        "cps:platform-onboarding:test",
        "cps:platform-onboarding:publish",
        "cps:platform-onboarding:delete",
    ]:
        assert permission in source

    for behavior in [
        "router.replace({ query: { mode: 'create' } })",
        "router.replace({ query: { mode: 'edit', platformCode",
        "PlatformOnboardingApi.getPage",
        "PlatformOnboardingApi.disable",
        "PlatformOnboardingApi.deleteDraft",
        "PlatformOnboardingApi.deleteBundle",
        "CompletionBadge",
        "route.query.mode",
    ]:
        assert behavior in source

    assert "runtimeStatus !== 1" in source or "runtimeStatus === 1" in source
    assert "handleSuccess" in source or "await reload" in source or "await getList" in source
