package cn.iocoder.yudao.module.cps.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

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

    // ========== 推广位 1-100-002-000 ==========
    ErrorCode ADZONE_NOT_EXISTS = new ErrorCode(1_100_002_000, "推广位不存在");
    ErrorCode ADZONE_DEFAULT_EXISTS = new ErrorCode(1_100_002_001, "平台[{}]已存在默认推广位");

    // ========== 订单 1-100-003-000 ==========
    ErrorCode ORDER_NOT_EXISTS = new ErrorCode(1_100_003_000, "CPS订单不存在");
    ErrorCode ORDER_ALREADY_EXISTS = new ErrorCode(1_100_003_001, "CPS订单[{}]已存在");
    ErrorCode ORDER_STATUS_INVALID = new ErrorCode(1_100_003_002, "CPS订单状态不合法");

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

}
