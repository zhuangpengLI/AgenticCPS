import re
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


def test_unified_menu_replaces_four_visible_entries():
    all_sql = read_utf8("backend/sql/module/cps-all-in-one.sql")
    update_sql = read_utf8("backend/sql/module/cps-update.sql")
    assert "平台配置中心" in all_sql
    assert "cps/platformOnboarding/index" in all_sql
    assert "cps:platform-onboarding:publish" in all_sql
    assert re.search(r"-- 修改时间：2026-07-(23|24) [0-9:]+\n-- 目的：.*平台配置中心", update_sql)
    for sql in (all_sql, update_sql):
        assert "WHERE `id` IN (6229, 6251, 6256, 6261)" in sql
        assert "`visible` = b'0'" in sql
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
