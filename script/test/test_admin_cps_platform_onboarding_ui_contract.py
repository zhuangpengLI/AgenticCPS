import re
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


def test_platform_code_select_uses_chinese_labels_for_local_life_platforms():
    options_source = read_utf8("frontend/admin-vue3/src/api/cps/apiVendor.ts")
    step_source = read_utf8(
        "frontend/admin-vue3/src/views/cps/platformOnboarding/components/PlatformStep.vue"
    )

    for option in [
        "{ label: '美团联盟', value: 'meituan' }",
        "{ label: '抖音联盟', value: 'douyin' }",
        "{ label: '滴滴联盟', value: 'didi' }",
        "{ label: '唯品会', value: 'vip' }",
    ]:
        assert option in options_source

    assert "getPlatformLabel" in options_source
    assert "getPlatformLabel" in step_source
    assert ":value=\"item.platformCode\"" in step_source


def test_rebate_amounts_follow_the_current_backend_yuan_contract_and_mask_extra_config():
    api_source = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")
    model_source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/model.ts")

    assert "API amount values are yuan" in api_source
    assert "Integer cents in the API contract" not in api_source
    assert "left.platformCode" in model_source
    assert "left.memberId" in model_source
    assert "extraConfig: undefined" in model_source
    assert "platform: { ...draft.platform, extraConfig: undefined }" in model_source


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
    assert ':formatter="dateFormatter"' in source
    assert "import { dateFormatter } from '@/utils/formatTime'" in source

    api_source = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")
    assert "updateTime?: string | number" in api_source


def test_platform_center_keeps_row_actions_on_one_line():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/index.vue")

    assert 'class="flex flex-nowrap items-center justify-center whitespace-nowrap"' in source


def test_workspace_has_five_steps_and_draft_publish_actions():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/workspace.vue")
    for title in ["平台信息", "API供应商", "推广位", "返利配置", "检测与启用"]:
        assert title in source
    for action in ["保存草稿", "连接测试", "发布但保持禁用", "发布并启用"]:
        assert action in source
    for marker in ["draftVersion", "configFingerprint", "validatedFingerprint", "status === 'READY'", "onBeforeRouteLeave"]:
        assert marker in source
    assert "draftPayload" in source
    assert "!dirty.value" in source


def test_each_step_exposes_validate():
    for name in ["PlatformStep", "VendorStep", "AdzoneStep", "RebateStep"]:
        source = read_utf8(f"frontend/admin-vue3/src/views/cps/platformOnboarding/components/{name}.vue")
        assert "defineExpose" in source
        assert "validate" in source


def test_rebate_priority_is_visible_required_and_defaulted():
    dialog = read_utf8(
        "frontend/admin-vue3/src/views/cps/platformOnboarding/components/RebateRuleDialog.vue"
    )
    step = read_utf8(
        "frontend/admin-vue3/src/views/cps/platformOnboarding/components/RebateStep.vue"
    )

    priority_field = '<el-form-item label="优先级" required>'
    assert priority_field in dialog
    assert dialog.index(priority_field) < dialog.index("<el-collapse")
    assert "priority: 0" in dialog
    assert "请输入非负整数优先级" in dialog
    assert "row.priority == null" in step
    assert "row.priority < 0" in step

    model = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/model.ts")
    assert "status: rule.status ?? 1" in model
    assert "priority: rule.priority ?? 0" in model


def test_workspace_uses_safe_draft_and_never_legacy_partial_batch_api():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/AdzoneBatchDialog.vue")
    rules = read_utf8("frontend/admin-vue3/src/views/cps/components/adzoneRules.ts")
    assert "parseAdzoneBatch" in source
    assert "validateAdzoneRow" in source
    assert "batch-create" not in source
    assert "全有或全无" in source
    assert "externalRelationId" in rules and "externalSpecialId" in rules


def test_sensitive_fields_are_flagged_and_result_is_desensitized():
    vendor = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/VendorEditorDialog.vue")
    result = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/CheckResultPanel.vue")
    rebate = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/RebateRuleDialog.vue")
    assert "已配置（留空则保持不变）" in vendor
    assert "type=\"password\"" in vendor
    assert "请求 payload JSON" not in result
    assert "不支持个人会员返利规则" in rebate
    assert "MemberLevelSelect" in rebate


def test_workspace_preserves_runtime_invariants_before_publish():
    vendor = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/VendorEditorDialog.vue")
    vendor_step = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/VendorStep.vue")
    adzone = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/AdzoneStep.vue")
    rebate = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/components/RebateRuleDialog.vue")
    assert "descriptors?.length" in vendor and "status: 1" in vendor
    assert "当前不自动故障切换" in vendor_step
    assert "只在故障切换时使用" not in vendor_step
    assert "item.isDefault =" in adzone and "row.status === 1" in adzone
    assert "title=\"高级设置\"" in rebate and "status: 1" in rebate


def test_vendor_step_runs_a_real_single_vendor_backend_test():
    api = read_utf8("frontend/admin-vue3/src/api/cps/platformOnboarding.ts")
    workspace = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/workspace.vue")
    vendor_step = read_utf8(
        "frontend/admin-vue3/src/views/cps/platformOnboarding/components/VendorStep.vue"
    )

    assert "/cps/platform-onboarding/test-vendor" in api
    assert "testVendor:" in api
    assert "PlatformOnboardingApi.testVendor" in workspace
    assert "@test-vendor=\"runVendorTest\"" in workspace
    assert "emit('test-vendor', row)" in vendor_step
    assert "VENDOR_PREFLIGHT" not in vendor_step


def test_vendor_editor_uses_selects_for_enumerated_vendor_fields():
    source = read_utf8(
        "frontend/admin-vue3/src/views/cps/platformOnboarding/components/VendorEditorDialog.vue"
    )

    assert 'v-model="form.vendorCode" filterable @change="applyDescriptor"' in source
    assert 'v-model="form.vendorName" filterable @change="applyVendorName"' in source
    assert source.count(':disabled="editing"') >= 2
    assert 'v-model="form.vendorType"' in source
    assert "VENDOR_CODE_OPTIONS" in source
    assert "VENDOR_TYPE_OPTIONS" in source

    active_descriptor = source[
        source.index("const activeDescriptor") : source.index("const fields")
    ]
    assert active_descriptor.index("props.descriptors?.find") < active_descriptor.index(
        "props.descriptor?.vendorCode"
    )


def test_vendor_editor_preserves_common_credential_names_and_translates_other_labels():
    source = read_utf8(
        "frontend/admin-vue3/src/views/cps/platformOnboarding/components/VendorEditorDialog.vue"
    )

    assert ':label="fieldLabel(field.name)"' in source
    for field_name in ["appKey", "appSecret", "apiKey"]:
        assert f"{field_name}: '{field_name}'" in source
    for field_name, chinese_label in {
        "apiBaseUrl": "接口基础地址",
        "authToken": "授权令牌",
        "defaultAdzoneId": "备用推广位",
        "timeoutMs": "请求超时（毫秒）",
        "rateLimitPerMinute": "每分钟请求上限",
        "retryMaxAttempts": "最大重试次数",
    }.items():
        assert f"{field_name}: '{chinese_label}'" in source
    assert "FIELD_LABELS[name] || name" in source


def test_unified_menu_replaces_four_visible_entries():
    all_sql = read_utf8("backend/sql/module/cps-all-in-one.sql")
    update_sql = read_utf8("backend/sql/module/cps-update.sql")
    assert "平台配置中心" in all_sql
    assert "cps/platformOnboarding/index" in all_sql
    assert "cps:platform-onboarding:publish" in all_sql
    assert re.search(r"-- 修改时间：2026-07-(23|24) [0-9:]+\n-- 目的：.*平台配置中心", update_sql)
    assert "WHERE `id` IN (6229, 6251, 6256, 6261)" in update_sql
    assert "`visible` = b'0'" in update_sql
    for menu_id in (6229, 6251, 6256, 6261):
        assert any(line.startswith(f"({menu_id},") and "b'0', b'1'" in line for line in all_sql.splitlines())
    assert re.search(r"\(6297,\s*'平台配置中心'.*'cps/platformOnboarding/index'.*b'1'", all_sql, re.S)
    assert re.search(r"\(6303,.*'cps:platform-onboarding:publish'", all_sql, re.S)
    unified_update = re.search(r"UPDATE `system_menu`\s+SET(?P<body>.*?)WHERE `id` = 6297", update_sql, re.S)
    assert unified_update, "migration must repair the existing unified menu row idempotently"
    for field in ["`name`", "`permission`", "`parent_id`", "`path`", "`component`", "`status`", "`visible`"]:
        assert field in unified_update.group("body")
    for permission in [
        "cps:platform-onboarding:query",
        "cps:platform-onboarding:create",
        "cps:platform-onboarding:update",
        "cps:platform-onboarding:test",
        "cps:platform-onboarding:publish",
        "cps:platform-onboarding:delete",
    ]:
        assert permission in all_sql
        assert permission in update_sql
