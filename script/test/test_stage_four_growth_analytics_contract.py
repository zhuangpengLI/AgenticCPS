from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SERVICE = ROOT / "backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/service/growth/CpsGrowthAnalyticsService.java"
CONTROLLER = ROOT / "backend/qiji-module-cps/qiji-module-cps-biz/src/main/java/com/qiji/cps/module/cps/controller/admin/growth/CpsGrowthAnalyticsController.java"
TODO = ROOT / "docs/superpowers/plans/2026-07-13-stage-zero-p0-closure-todo.md"
PROJECT_MAP = ROOT / "docs/project-map.md"
PLAN = ROOT / "docs/superpowers/plans/2026-07-15-stage-four-growth-analytics.md"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def test_growth_analytics_service_exposes_stage_four_capabilities():
    text = read(SERVICE)

    for token in [
        "calculateRoi",
        "summarizeRisk",
        "assignExperiment",
        "reconcileTokenEvents",
        "validateBillingBoundary",
        "RoiFacts",
        "RiskThresholds",
        "ExperimentAssignment",
        "TokenReconciliationSummary",
        "BillingBoundaryDecision",
    ]:
        assert token in text

    for token in [
        "CPS_MISSING_SUCCESS",
        "PROCESSING_TIMEOUT",
        "ONLY_CONFIRMED_ASSET_EVENT_CONSUMPTION_ALLOWED",
        "CONSUME_CONFIRMED_ASSET_EVENT",
    ]:
        assert token in text


def test_growth_analytics_admin_api_is_read_only_and_permissioned():
    controller = read(CONTROLLER)
    service = read(SERVICE)

    for token in [
        '@RequestMapping("/cps/growth-analytics")',
        '@PostMapping("/roi")',
        '@PostMapping("/risk-summary")',
        '@PostMapping("/experiment/assign")',
        '@PostMapping("/token-reconciliation")',
        '@PostMapping("/billing-boundary/validate")',
        "cps:growth-analytics:query",
    ]:
        assert token in controller

    forbidden_writers = [
        "CpsRebateAssetService",
        "CpsOrderService",
        "CpsFreezeService",
        "CpsWithdrawService",
        "CpsRebateTokenExchangeService",
        "Mapper",
    ]
    combined = controller + service
    for token in forbidden_writers:
        assert token not in combined


def test_stage_four_docs_are_checked_with_external_gaps_preserved():
    todo = read(TODO)
    project_map = read(PROJECT_MAP)
    plan = read(PLAN)

    for task in ["S4-00", "S4-01", "S4-02", "S4-03", "S4-04", "S4-05", "S4-06"]:
        assert f"- [x] **{task}" in todo

    assert "真实 TokenHub 日切对账" in todo
    assert "仍需外部环境验收" in todo
    assert "P4 增长分析和生态协同" in project_map
    assert "/admin-api/cps/growth-analytics" in project_map
    assert "cps:growth-analytics:query" in project_map
    assert "真实 TokenHub / new-api 双系统日切对账" in plan
