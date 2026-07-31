package com.qiji.cps.module.cps.enums;

import com.qiji.cps.framework.common.exception.ErrorCode;

/**
 * CPS 错误码枚举类
 * <p>
 * cps 系统，使用 1-100-000-000 段
 */
public interface CpsErrorCodeConstants {

    // ========== 平台配置 1-100-001-000 ==========
    ErrorCode PLATFORM_NOT_EXISTS = new ErrorCode(1_100_001_000, "CPS平台配置不存在");
    ErrorCode PLATFORM_CODE_DUPLICATE = new ErrorCode(1_100_001_001, "平台编码[{}]已存在");
    ErrorCode PLATFORM_IS_DISABLE = new ErrorCode(1_100_001_002, "CPS平台[{}]已禁用");
    ErrorCode PLATFORM_CAPABILITY_UNSUPPORTED = new ErrorCode(1_100_001_003, "平台[{}]不支持商品搜索");

    // ========== 推广位 1-100-002-000 ==========
    ErrorCode ADZONE_NOT_EXISTS = new ErrorCode(1_100_002_000, "推广位不存在");
    ErrorCode ADZONE_DEFAULT_EXISTS = new ErrorCode(1_100_002_001, "平台[{}]已存在默认推广位");
    ErrorCode ADZONE_CONFIG_INVALID = new ErrorCode(1_100_002_002, "推广位配置不合法：{}");

    // ========== 订单 1-100-003-000 ==========
    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_100_003_000, "CPS订单不存在");
    ErrorCode ORDER_ALREADY_EXISTS = new ErrorCode(1_100_003_001, "CPS订单[{}]已存在");
    ErrorCode ORDER_STATUS_INVALID = new ErrorCode(1_100_003_002, "CPS订单状态不合法");
    ErrorCode ORDER_ATTRIBUTION_BIND_INVALID = new ErrorCode(1_100_003_003, "订单归因绑定失败：{}");
    ErrorCode ORDER_CLAIM_INVALID = new ErrorCode(1_100_003_004, "订单申领请求不合法：{}");
    ErrorCode ORDER_CLAIM_NOT_EXISTS = new ErrorCode(1_100_003_005, "订单申领记录不存在");
    ErrorCode ORDER_CLAIM_STATUS_INVALID = new ErrorCode(1_100_003_006, "订单申领状态不允许当前操作");

    // ========== 返利配置 1-100-004-000 ==========
    ErrorCode REBATE_CONFIG_NOT_EXISTS = new ErrorCode(1_100_004_000, "返利配置不存在");
    ErrorCode REBATE_CONFIG_DUPLICATE = new ErrorCode(1_100_004_001, "等级[{}]+平台[{}]的返利配置已存在");

    // ========== 返利记录 1-100-005-000 ==========
    ErrorCode REBATE_RECORD_NOT_EXISTS = new ErrorCode(1_100_005_000, "返利记录不存在");

    // ========== 返利账户 1-100-006-000 ==========
    ErrorCode REBATE_ACCOUNT_NOT_EXISTS = new ErrorCode(1_100_006_000, "返利账户不存在");
    ErrorCode REBATE_ACCOUNT_BALANCE_NOT_ENOUGH = new ErrorCode(1_100_006_001, "返利账户可用余额不足");
    ErrorCode REBATE_ACCOUNT_IS_FROZEN = new ErrorCode(1_100_006_002, "返利账户已冻结");

    // ========== 提现 1-100-007-000 ==========
    ErrorCode WITHDRAW_NOT_EXISTS = new ErrorCode(1_100_007_000, "提现申请不存在");
    ErrorCode WITHDRAW_STATUS_INVALID = new ErrorCode(1_100_007_001, "提现状态不合法，无法执行此操作");
    ErrorCode WITHDRAW_AMOUNT_MIN = new ErrorCode(1_100_007_002, "提现金额不能低于最低限额");
    ErrorCode WITHDRAW_DAILY_LIMIT = new ErrorCode(1_100_007_003, "今日提现次数已达上限");

    // ========== 统计 1-100-008-000 ==========
    ErrorCode STATISTICS_NOT_EXISTS = new ErrorCode(1_100_008_000, "统计记录不存在");

    // ========== MCP 1-100-009-000 ==========
    ErrorCode MCP_API_KEY_NOT_EXISTS = new ErrorCode(1_100_009_000, "MCP API Key不存在");
    ErrorCode MCP_API_KEY_EXPIRED = new ErrorCode(1_100_009_001, "MCP API Key已过期");
    ErrorCode MCP_API_KEY_DISABLED = new ErrorCode(1_100_009_002, "MCP API Key已禁用");

    // ========== 转链 1-100-010-000 ==========
    ErrorCode TRANSFER_RECORD_NOT_EXISTS = new ErrorCode(1_100_010_000, "转链记录不存在");

    // ========== 冻结 1-100-011-000 ==========
    ErrorCode FREEZE_CONFIG_NOT_EXISTS = new ErrorCode(1_100_011_000, "冻结配置不存在");
    ErrorCode FREEZE_RECORD_NOT_EXISTS = new ErrorCode(1_100_011_001, "冻结记录不存在");
    ErrorCode FREEZE_RECORD_STATUS_INVALID = new ErrorCode(1_100_011_002, "冻结记录状态不合法，无法执行此操作");

    // ========== 风控 1-100-012-000 ==========
    ErrorCode RISK_RULE_NOT_EXISTS = new ErrorCode(1_100_012_000, "风控规则不存在");
    ErrorCode RISK_TRANSFER_BLOCKED = new ErrorCode(1_100_012_001, "转链请求被风控拦截");

    // ========== API供应商 1-100-013-000 ==========
    ErrorCode VENDOR_NOT_EXISTS = new ErrorCode(1_100_013_000, "API供应商配置不存在");
    ErrorCode VENDOR_PLATFORM_DUPLICATE = new ErrorCode(1_100_013_001, "供应商[{}]+平台[{}]的配置已存在");
    ErrorCode DIDI_UNION_REQUEST_FAILED = new ErrorCode(1_100_013_002, "滴滴联盟请求失败：{}");
    ErrorCode DIDI_UNION_CONFIG_INVALID = new ErrorCode(1_100_013_003, "滴滴联盟配置不合法：{}");
    ErrorCode VENDOR_CAPABILITY_NOT_READY = new ErrorCode(1_100_013_004, "供应商[{}]+平台[{}]未完成能力验收，禁止启用：{}");

    // ========== 返利兑换 Token 1-100-014-000 ==========
    ErrorCode REBATE_EXCHANGE_NOT_EXISTS = new ErrorCode(1_100_014_000, "返利兑换订单不存在");
    ErrorCode REBATE_EXCHANGE_STATUS_INVALID = new ErrorCode(1_100_014_001, "返利兑换订单状态不合法");
    ErrorCode REBATE_EXCHANGE_AMOUNT_INVALID = new ErrorCode(1_100_014_002, "兑换金额必须大于0");
    ErrorCode REBATE_EXCHANGE_IDEMPOTENCY_KEY_REQUIRED = new ErrorCode(1_100_014_003, "兑换幂等键不能为空");
    ErrorCode REBATE_EXCHANGE_TOKEN_FAILED = new ErrorCode(1_100_014_004, "Token兑换失败：{}");
    ErrorCode OPENAPI_SIGNATURE_INVALID = new ErrorCode(1_100_014_005, "开放接口签名无效");
    ErrorCode OPENAPI_HEADER_MISSING = new ErrorCode(1_100_014_006, "缺少开放接口请求头：{}");

    // ========== 返利活动 1-100-015-000 ==========
    ErrorCode REBATE_ACTIVITY_NOT_EXISTS = new ErrorCode(1_100_015_000, "返利活动不存在");
    ErrorCode REBATE_ACTIVITY_JUMP_INVALID = new ErrorCode(1_100_015_001, "返利活动跳转配置不合法：{}");

    // ========== 选品库 1-100-016-000 ==========
    ErrorCode SELECTION_THEME_NOT_EXISTS = new ErrorCode(1_100_016_000, "选品主题不存在");
    ErrorCode SELECTION_THEME_CODE_DUPLICATE = new ErrorCode(1_100_016_001, "选品主题编码[{}]已存在");
    ErrorCode SELECTION_THEME_STATUS_INVALID = new ErrorCode(1_100_016_002, "选品主题状态不合法：{}");
    ErrorCode SELECTION_THEME_ITEM_NOT_EXISTS = new ErrorCode(1_100_016_003, "选品主题商品不存在");

    // ========== 平台接入 1-100-017-000 ==========
    ErrorCode ONBOARDING_DRAFT_NOT_EXISTS = new ErrorCode(1_100_017_000, "平台接入草稿不存在");
    ErrorCode ONBOARDING_DRAFT_VERSION_CONFLICT = new ErrorCode(1_100_017_001, "平台接入草稿版本冲突，请刷新后重试");
    ErrorCode ONBOARDING_CONFIG_INVALID = new ErrorCode(1_100_017_002, "平台接入配置不合法：{}");
    ErrorCode ONBOARDING_TEST_REQUIRED = new ErrorCode(1_100_017_003, "平台接入配置尚未通过测试");
    ErrorCode ONBOARDING_PLATFORM_ENABLED = new ErrorCode(1_100_017_004, "平台已启用，请先停用后再删除");
    ErrorCode ONBOARDING_PUBLISH_CONFLICT = new ErrorCode(1_100_017_005, "草稿配置与已检测配置不一致，请重新检测");
    ErrorCode ADZONE_RELATION_REQUIRED = new ErrorCode(1_100_017_006, "推广位类型[{}]必须配置关联信息");
    ErrorCode REBATE_CONFIG_AMOUNT_RANGE_INVALID = new ErrorCode(1_100_017_007, "返利配置最小金额不能大于最大金额");

}
