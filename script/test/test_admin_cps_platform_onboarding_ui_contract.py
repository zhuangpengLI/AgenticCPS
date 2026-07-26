from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def read_utf8(relative_path: str) -> str:
    return (ROOT / relative_path).read_text(encoding="utf-8")


def test_workspace_has_five_steps_and_draft_publish_actions():
    source = read_utf8("frontend/admin-vue3/src/views/cps/platformOnboarding/workspace.vue")
    for title in ["平台信息", "API供应商", "推广位", "返利配置", "检测与启用"]:
        assert title in source
    for action in ["保存草稿", "连接测试", "发布但保持禁用", "发布并启用"]:
        assert action in source
    for marker in ["draftVersion", "configFingerprint", "validatedFingerprint", "status === 'READY'", "onBeforeRouteLeave"]:
        assert marker in source


def test_each_step_exposes_validate():
    for name in ["PlatformStep", "VendorStep", "AdzoneStep", "RebateStep"]:
        source = read_utf8(f"frontend/admin-vue3/src/views/cps/platformOnboarding/components/{name}.vue")
        assert "defineExpose" in source
        assert "validate" in source


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
