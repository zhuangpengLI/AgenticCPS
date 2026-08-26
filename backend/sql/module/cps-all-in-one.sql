-- ============================================================
-- CPS联盟返利系统 - 完整数据库建表脚本（All-in-One）
-- Version: 2.0
-- Date: 2026-07-24
-- Description: 新库全量初始化脚本，整合 CPS 核心表、P0 返利兑换、活动中心、选品库、CPX 扩展表、菜单与权限。
-- Maintenance:
--   1. 本文件只保存新库全量脚本。
--   2. 现有数据库增量更新维护在 backend/sql/module/cps-update.sql。
--   3. 不要在 backend/sql/mysql 新建 CPS all-in-one 或零散 CPS 更新脚本。
-- 包含表：
--   1.  cps_platform                     CPS平台配置表
--   2.  cps_adzone                       推广位（PID）管理表
--   3.  cps_order                        CPS订单表（含冻结与淘宝归因字段）
--   4.  cps_order_status_event           订单状态事件表
--   5.  cps_order_sync_failure           订单同步失败恢复队列表
--   6.  cps_platform_bill_row            平台账单导入行表
--   7.  cps_platform_bill_diff           平台账单对账差异表
--   8.  cps_rebate_config                返利配置表
--   9.  cps_rebate_record                返利记录表（含冻结关联字段）
--   10. cps_rebate_account               会员返利账户表
--   11. cps_withdraw                     提现申请表
--   12. cps_mcp_api_key                  MCP API Key管理表
--   13. cps_mcp_access_log               MCP访问日志表
--   14. cps_openapi_access_log           OpenAPI访问审计日志表
--   15. cps_transfer_record              CPS转链记录表
--   16. cps_freeze_config                冻结解冻配置表
--   17. cps_freeze_record                冻结解冻记录表
--   18. cps_rebate_asset_ledger          返利资产流水表
--   19. cps_rebate_asset_migration_check 返利资产迁移核对表
--   20. cps_rebate_debt                  返利债务表
--   21. cps_order_attribution_log        订单归因日志表
--   22. cps_order_sync_checkpoint        订单同步检查点表
--   23. cps_rebate_asset_policy          返利资产策略表
--   24. cps_rebate_token_exchange_order  CPS返利兑换Token订单表
--   25. cps_order_sync_log               订单同步日志表
--   26. cps_statistics                   统计数据表
--   27. cps_risk_rule                    风控规则表
--   28. cps_api_vendor                   CPS API供应商配置表
--   29. cps_didi_callback_event          滴滴订单/领券回调审计与幂等表
--   29. cps_platform_onboarding_draft    CPS平台接入草稿表
--   30. cps_rebate_activity              CPS返利活动表
--   31. cps_selection_theme              选品主题表
--   32. cps_selection_theme_item         选品主题商品快照表
--   33. cps_selection_ai_review          AI选品人工复核审计表
--   34. cps_goods_master                 CPS商品主档表
--   35. cps_goods_source_mapping         CPS商品来源映射表
--   36. cps_goods_price_snapshot         CPS商品价格快照表
--   37. cps_coupon_pool                  CPS券池表
--   38. cps_marketing_short_link         CPS营销短链表
--   39. cps_marketing_click_event        CPS营销点击事件表
--   40. cpx_task                         CPX任务表
--   41. cpx_offer                        CPX任务报价表
--   42. cpx_material                     CPX素材表
--   43. cpx_platform_profile             CPX平台档案表
--   44. cpx_article                      CPX资讯文章表
--   45. cpx_tracking_link                CPX追踪链接表
--   46. cpx_event                        CPX事件表
--   47. cpx_conversion                   CPX转化表
--   48. cpx_settlement_record            CPX结算记录表
--   49. cpx_lead_detail                  CPX线索详情表
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. CPS平台配置表
-- ----------------------------
DROP TABLE IF EXISTS `cps_platform`;
CREATE TABLE `cps_platform` (
  `id`                   bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`        varchar(32)  NOT NULL COMMENT '平台编码（唯一标识）',
  `platform_name`        varchar(64)  NOT NULL COMMENT '平台名称',
  `platform_logo`        varchar(255)          DEFAULT NULL COMMENT '平台Logo图片URL',
  `app_key`              varchar(255)          DEFAULT NULL COMMENT 'AppKey',
  `app_secret`           varchar(255)          DEFAULT NULL COMMENT 'AppSecret（加密存储）',
  `api_base_url`         varchar(255)          DEFAULT NULL COMMENT 'API基础URL',
  `auth_token`           varchar(255)          DEFAULT NULL COMMENT '授权令牌',
  `default_adzone_id`    varchar(128)          DEFAULT NULL COMMENT '默认推广位ID',
  `platform_service_rate` decimal(10,4)         DEFAULT '0.0000' COMMENT '平台服务费率（百分比）',
  `sort`                 int                   DEFAULT '0' COMMENT '排序权重',
  `status`               tinyint      NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `extra_config`         text                  COMMENT '扩展配置（JSON格式）',
  `remark`               varchar(512)          DEFAULT NULL COMMENT '备注',
  `creator`              varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`          datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`            bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  `active_vendor_code`   varchar(32)           DEFAULT 'dataoke' COMMENT '当前激活的供应商编码（用于路由选择）',
  `supported_vendors`    varchar(256)          DEFAULT 'dataoke' COMMENT '支持的供应商列表（逗号分隔）',
  `active_unique_key`    varchar(128) GENERATED ALWAYS AS (IF(`deleted` = b'0', CONCAT(`tenant_id`, ':', `platform_code`), NULL)) STORED COMMENT '未删除平台租户唯一键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cps_platform_active` (`active_unique_key`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS平台配置表';

-- 唯品会由好单库供应商接入；初始保持禁用，待管理员配置供应商凭证后启用。
INSERT INTO `cps_platform` (`platform_code`, `platform_name`, `platform_logo`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `platform_service_rate`, `sort`, `status`, `extra_config`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `active_vendor_code`, `supported_vendors`) VALUES
('vip', '唯品会', NULL, NULL, NULL, NULL, NULL, NULL, 0.0000, 70, 0, NULL, '请先配置好单库唯品会供应商凭证，确认账号已开通唯品会权限后再启用', 'system', '2026-08-07 17:30:00', 'system', '2026-08-07 17:30:00', b'0', 1, 'haodanku', 'haodanku');

-- ----------------------------
-- 2. 推广位（PID）管理表
-- ----------------------------
DROP TABLE IF EXISTS `cps_adzone`;
CREATE TABLE `cps_adzone` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code` varchar(32)  NOT NULL COMMENT '所属平台编码',
  `adzone_id`     varchar(128) NOT NULL COMMENT '推广位ID',
  `adzone_name`   varchar(128)          DEFAULT NULL COMMENT '推广位名称',
  `adzone_type`   varchar(32)           DEFAULT 'general' COMMENT '推广位类型（general:通用 channel:渠道专属 member:用户专属）',
  `relation_type` varchar(32)           DEFAULT NULL COMMENT '关联类型（channel/member）',
  `relation_id`   bigint                DEFAULT NULL COMMENT '关联渠道或用户ID',
  `external_relation_id` varchar(128)    DEFAULT NULL COMMENT '淘宝联盟渠道关系ID（orderScene=2）',
  `external_special_id`  varchar(128)    DEFAULT NULL COMMENT '淘宝联盟会员运营ID（orderScene=3）',
  `is_default`    tinyint      NOT NULL DEFAULT '0' COMMENT '是否默认（0否 1是）',
  `status`        tinyint      NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `creator`       varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`     bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  `active_unique_key` varchar(191) GENERATED ALWAYS AS (IF(`deleted` = b'0', CONCAT(CHAR_LENGTH(CAST(`tenant_id` AS CHAR)), ':', CAST(`tenant_id` AS CHAR), CHAR_LENGTH(`platform_code`), ':', `platform_code`, CHAR_LENGTH(`adzone_id`), ':', `adzone_id`), NULL)) STORED COMMENT '未删除推广位租户唯一键（长度前缀编码）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cps_adzone_active` (`active_unique_key`) USING BTREE,
  KEY `idx_platform_code` (`platform_code`) USING BTREE,
  KEY `idx_adzone_id` (`adzone_id`) USING BTREE,
  KEY `idx_external_relation_id` (`platform_code`,`external_relation_id`) USING BTREE,
  KEY `idx_external_special_id` (`platform_code`,`external_special_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广位（PID）管理表';

-- ----------------------------
-- 3. CPS订单表（含Phase7冻结字段）
-- ----------------------------
DROP TABLE IF EXISTS `cps_order`;
CREATE TABLE `cps_order` (
  `id`                     bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`          varchar(32)   NOT NULL COMMENT '平台编码',
  `platform_order_id`      varchar(128)  NOT NULL COMMENT '平台订单号',
  `parent_order_id`        varchar(128)           DEFAULT NULL COMMENT '父订单号',
  `member_id`              bigint                 DEFAULT NULL COMMENT '会员ID（归因后）',
  `member_nickname`        varchar(128)           DEFAULT NULL COMMENT '会员昵称',
  `item_id`                varchar(128)           DEFAULT NULL COMMENT '商品ID',
  `item_title`             varchar(512)           DEFAULT NULL COMMENT '商品标题',
  `item_pic`               varchar(255)           DEFAULT NULL COMMENT '商品主图',
  `item_price`             decimal(10,2)          DEFAULT '0.00' COMMENT '商品原价',
  `final_price`            decimal(10,2)          DEFAULT '0.00' COMMENT '券后价',
  `coupon_amount`          decimal(10,2)          DEFAULT '0.00' COMMENT '优惠券金额',
  `commission_rate`        decimal(10,4)          DEFAULT '0.0000' COMMENT '佣金比例（万分比）',
  `commission_amount`      decimal(10,2)          DEFAULT '0.00' COMMENT '预估佣金金额',
  `estimate_rebate`        decimal(10,2)          DEFAULT '0.00' COMMENT '预估返利金额',
  `real_rebate`            decimal(10,2)          DEFAULT '0.00' COMMENT '实际返利金额',
  `adzone_id`              varchar(128)           DEFAULT NULL COMMENT '推广位ID',
  `external_info`          varchar(255)           DEFAULT NULL COMMENT '外部追踪参数',
  `special_id`             varchar(128)           DEFAULT NULL COMMENT '淘宝会员运营ID',
  `relation_id`            varchar(128)           DEFAULT NULL COMMENT '淘宝渠道关系ID',
  `order_scene`            tinyint                DEFAULT NULL COMMENT '淘宝订单场景（1常规 2渠道 3会员运营）',
  `attribution_source`     varchar(32)            DEFAULT NULL COMMENT '会员归因来源',
  `order_status`           varchar(32)   NOT NULL DEFAULT 'created' COMMENT '订单状态（created:已下单 paid:已付款 received:已收货 settled:已结算 rebate_received:已到账 refunded:已退款 invalid:已失效）',
  `sync_time`              datetime               DEFAULT NULL COMMENT '同步时间',
  `settle_time`            datetime               DEFAULT NULL COMMENT '结算时间',
  `rebate_time`            datetime               DEFAULT NULL COMMENT '返利入账时间',
  `refund_time`            datetime               DEFAULT NULL COMMENT '退款时间',
  `confirm_receipt_time`   datetime               DEFAULT NULL COMMENT '确认收货时间',
  `rebate_freeze_status`   varchar(16)            DEFAULT NULL COMMENT '返利冻结状态（NULL/pending:待冻结 frozen:已冻结 unfreezing:解冻中 unfreezed:已解冻）',
  `plan_unfreeze_time`     datetime               DEFAULT NULL COMMENT '计划解冻时间',
  `actual_unfreeze_time`   datetime               DEFAULT NULL COMMENT '实际解冻时间',
  `platform_confirm_time`  datetime               DEFAULT NULL COMMENT '平台确认时间',
  `retry_count`            int                    DEFAULT '0' COMMENT '同步重试次数',
  `last_sync_error`        varchar(512)           DEFAULT NULL COMMENT '最后同步错误信息',
  `last_sync_result`       varchar(32)            DEFAULT NULL COMMENT '最后同步结果（SUCCESS/PARTIAL/FAILED）',
  `raw_platform_status_summary` varchar(512)       DEFAULT NULL COMMENT '原始平台状态摘要',
  `status_version`         int           NOT NULL DEFAULT '0' COMMENT '订单状态变更版本号',
  `rebate_settle_retry_count` int        NOT NULL DEFAULT '0' COMMENT '返利结算重试次数',
  `rebate_settle_next_retry_time` datetime         DEFAULT NULL COMMENT '下次返利结算重试时间',
  `rebate_settle_last_error` varchar(512)           DEFAULT NULL COMMENT '最近返利结算待处理或失败原因',
  `creator`                varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`            datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`              bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_platform_order` (`tenant_id`, `platform_code`, `platform_order_id`) USING BTREE,
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_special_id` (`platform_code`,`special_id`) USING BTREE,
  KEY `idx_relation_id` (`platform_code`,`relation_id`) USING BTREE,
  KEY `idx_order_status` (`order_status`) USING BTREE,
  KEY `idx_order_rebate_settle_retry` (`tenant_id`, `order_status`, `rebate_settle_next_retry_time`, `create_time`, `id`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_platform_code` (`platform_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS订单表';

-- ----------------------------
-- 3.1 订单状态事件表
-- ----------------------------
DROP TABLE IF EXISTS `cps_order_status_event`;
CREATE TABLE `cps_order_status_event` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`           bigint                DEFAULT NULL COMMENT '订单ID',
  `platform_code`      varchar(32)  NOT NULL COMMENT '平台编码',
  `platform_order_id`  varchar(128) NOT NULL COMMENT '平台订单号',
  `source_type`        varchar(32)  NOT NULL COMMENT '事件来源（ORDER_SYNC/MANUAL_SYNC/COMPENSATION）',
  `source_batch_no`    varchar(128)          DEFAULT NULL COMMENT '同步批次号',
  `raw_status`         varchar(64)           DEFAULT NULL COMMENT '平台原始状态值',
  `raw_status_summary` varchar(512)          DEFAULT NULL COMMENT '供应商原始状态摘要',
  `previous_status`    varchar(32)           DEFAULT NULL COMMENT '变更前系统状态',
  `mapped_status`      varchar(32)  NOT NULL COMMENT '本次映射出的系统状态',
  `current_status`     varchar(32)  NOT NULL COMMENT '事件后订单当前系统状态',
  `event_time`         datetime     NOT NULL COMMENT '事件时间',
  `status_version`     int          NOT NULL DEFAULT '0' COMMENT '订单状态版本',
  `downgrade_rejected` bit(1)                DEFAULT b'0' COMMENT '是否拒绝状态降级',
  `reject_reason`      varchar(512)          DEFAULT NULL COMMENT '拒绝降级原因',
  `creator`            varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`          bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_status_event_order` (`tenant_id`, `order_id`, `status_version`) USING BTREE,
  KEY `idx_status_event_platform_order` (`tenant_id`, `platform_code`, `platform_order_id`, `event_time`) USING BTREE,
  KEY `idx_status_event_batch` (`tenant_id`, `source_batch_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS订单状态事件表';

-- ----------------------------
-- 3.2 订单同步失败恢复队列表
-- ----------------------------
DROP TABLE IF EXISTS `cps_order_sync_failure`;
CREATE TABLE `cps_order_sync_failure` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`      varchar(32)  NOT NULL COMMENT '平台编码',
  `vendor_code`        varchar(64)  NOT NULL COMMENT '供应商编码',
  `order_scene`        tinyint      NOT NULL DEFAULT '0' COMMENT '订单场景',
  `query_type`         varchar(16)  NOT NULL COMMENT '查询时间类型',
  `pagination_mode`    varchar(16)           DEFAULT NULL COMMENT '分页模式（PAGE/CURSOR）',
  `page_no`            int                   DEFAULT NULL COMMENT '失败页码',
  `next_cursor`        varchar(255)          DEFAULT NULL COMMENT '失败游标',
  `sync_batch_no`      varchar(255)          DEFAULT NULL COMMENT '同步批次号',
  `failure_stage`      varchar(32)  NOT NULL COMMENT '失败阶段',
  `request_snapshot`   varchar(1000)         DEFAULT NULL COMMENT '脱敏请求快照',
  `raw_summary`        varchar(2000)         DEFAULT NULL COMMENT '脱敏原始摘要',
  `failure_reason`     varchar(1000)         DEFAULT NULL COMMENT '失败原因',
  `status`             varchar(16)  NOT NULL COMMENT '恢复状态（PENDING/RETRYING/DEAD/RESOLVED）',
  `retry_count`        int          NOT NULL DEFAULT '0' COMMENT '已重试次数',
  `max_retry_count`    int          NOT NULL DEFAULT '3' COMMENT '最大重试次数',
  `next_retry_time`    datetime              DEFAULT NULL COMMENT '下次重试时间',
  `last_replay_time`   datetime              DEFAULT NULL COMMENT '最近人工重放时间',
  `replay_operator_id` bigint                DEFAULT NULL COMMENT '人工重放操作人',
  `replay_audit_note`  varchar(500)          DEFAULT NULL COMMENT '人工重放审计说明',
  `idempotency_key`    varchar(128) NOT NULL COMMENT '失败记录幂等键',
  `version`            int          NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `creator`            varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`          bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_failure_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_sync_failure_status` (`tenant_id`, `status`, `next_retry_time`) USING BTREE,
  KEY `idx_sync_failure_platform` (`tenant_id`, `platform_code`, `vendor_code`, `order_scene`, `query_type`) USING BTREE,
  KEY `idx_sync_failure_batch` (`tenant_id`, `sync_batch_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步失败恢复队列表';

-- ----------------------------
-- 3.3 平台账单导入行表
-- ----------------------------
DROP TABLE IF EXISTS `cps_platform_bill_row`;
CREATE TABLE `cps_platform_bill_row` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`     varchar(32)  NOT NULL COMMENT '平台编码',
  `vendor_code`       varchar(64)           DEFAULT NULL COMMENT '供应商编码',
  `bill_batch_no`     varchar(128) NOT NULL COMMENT '平台账单批次号',
  `platform_order_id` varchar(128) NOT NULL COMMENT '平台订单号',
  `parent_order_id`   varchar(128)          DEFAULT NULL COMMENT '父订单号',
  `bill_status`       varchar(64)           DEFAULT NULL COMMENT '平台账单状态',
  `commission_amount` decimal(18,2)         DEFAULT NULL COMMENT '平台账单佣金金额',
  `refund_amount`     decimal(18,2)         DEFAULT NULL COMMENT '平台账单退款金额',
  `order_time`        datetime              DEFAULT NULL COMMENT '账单下单时间',
  `settle_time`       datetime              DEFAULT NULL COMMENT '账单结算时间',
  `refund_time`       datetime              DEFAULT NULL COMMENT '账单退款时间',
  `source_file_name`  varchar(255)          DEFAULT NULL COMMENT '账单来源文件名',
  `raw_summary`       varchar(2000)         DEFAULT NULL COMMENT '平台账单原始摘要',
  `idempotency_key`   varchar(128) NOT NULL COMMENT '导入行幂等键',
  `version`           int          NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `creator`           varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_bill_row_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_platform_bill_row_order` (`tenant_id`, `platform_code`, `platform_order_id`) USING BTREE,
  KEY `idx_platform_bill_row_batch` (`tenant_id`, `bill_batch_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台账单导入行表';

-- ----------------------------
-- 3.4 平台账单对账差异表
-- ----------------------------
DROP TABLE IF EXISTS `cps_platform_bill_diff`;
CREATE TABLE `cps_platform_bill_diff` (
  `id`                      bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_row_id`             bigint                DEFAULT NULL COMMENT '平台账单导入行ID',
  `order_id`                bigint                DEFAULT NULL COMMENT '本地订单ID',
  `platform_code`           varchar(32)  NOT NULL COMMENT '平台编码',
  `vendor_code`             varchar(64)           DEFAULT NULL COMMENT '供应商编码',
  `bill_batch_no`           varchar(128) NOT NULL COMMENT '平台账单批次号',
  `platform_order_id`       varchar(128) NOT NULL COMMENT '平台订单号',
  `diff_type`               varchar(32)  NOT NULL COMMENT '差异类型',
  `diff_status`             varchar(32)  NOT NULL COMMENT '差异状态（PENDING/HANDLED/REPULL_REQUESTED）',
  `diff_summary`            varchar(512)          DEFAULT NULL COMMENT '差异摘要',
  `order_commission_amount` decimal(18,2)         DEFAULT NULL COMMENT '本地订单佣金金额',
  `bill_commission_amount`  decimal(18,2)         DEFAULT NULL COMMENT '平台账单佣金金额',
  `bill_refund_amount`      decimal(18,2)         DEFAULT NULL COMMENT '平台账单退款金额',
  `order_status`            varchar(32)           DEFAULT NULL COMMENT '本地订单状态',
  `bill_status`             varchar(64)           DEFAULT NULL COMMENT '平台账单状态',
  `order_settle_time`       datetime              DEFAULT NULL COMMENT '本地订单结算时间',
  `bill_settle_time`        datetime              DEFAULT NULL COMMENT '平台账单结算时间',
  `handle_conclusion`       varchar(64)           DEFAULT NULL COMMENT '处理结论',
  `handle_audit_note`       varchar(500)          DEFAULT NULL COMMENT '处理审计说明',
  `handle_operator_id`      bigint                DEFAULT NULL COMMENT '处理操作人',
  `handle_time`             datetime              DEFAULT NULL COMMENT '处理时间',
  `idempotency_key`         varchar(160) NOT NULL COMMENT '差异幂等键',
  `version`                 int          NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `creator`                 varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                 varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                 bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`               bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_bill_diff_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_platform_bill_diff_status` (`tenant_id`, `diff_status`, `diff_type`) USING BTREE,
  KEY `idx_platform_bill_diff_order` (`tenant_id`, `platform_code`, `platform_order_id`) USING BTREE,
  KEY `idx_platform_bill_diff_batch` (`tenant_id`, `bill_batch_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台账单对账差异表';

-- ----------------------------
-- 4. 返利配置表
-- ----------------------------
DROP TABLE IF EXISTS `cps_rebate_config`;
CREATE TABLE `cps_rebate_config` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`         bigint                 DEFAULT NULL COMMENT '会员ID（NULL表示无会员限制）',
  `member_level_id`   bigint                 DEFAULT NULL COMMENT '会员等级ID（NULL表示无等级限制）',
  `platform_code`     varchar(32)            DEFAULT NULL COMMENT '平台编码（NULL表示全平台）',
  `rebate_rate`       decimal(10,4) NOT NULL COMMENT '返利比例（百分比）',
  `max_rebate_amount` decimal(10,2)          DEFAULT '0.00' COMMENT '单笔最大返利金额（0表示不限）',
  `min_rebate_amount` decimal(10,2)          DEFAULT '0.00' COMMENT '单笔最小返利金额（0表示不限）',
  `status`            tinyint       NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `priority`          int           NOT NULL DEFAULT '0' COMMENT '优先级（数字越大优先级越高）',
  `creator`           varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_member_level_id` (`member_level_id`) USING BTREE,
  KEY `idx_platform_code` (`platform_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利配置表';

-- ----------------------------
-- 5. 返利记录表（含Phase7冻结关联字段）
-- ----------------------------
DROP TABLE IF EXISTS `cps_rebate_record`;
CREATE TABLE `cps_rebate_record` (
  `id`                   bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`            bigint        NOT NULL COMMENT '会员ID',
  `order_id`             bigint                 DEFAULT NULL COMMENT '订单ID',
  `platform_code`        varchar(32)   NOT NULL COMMENT '平台编码',
  `platform_order_id`    varchar(128)  NOT NULL COMMENT '平台订单号',
  `item_id`              varchar(128)           DEFAULT NULL COMMENT '商品ID',
  `item_title`           varchar(512)           DEFAULT NULL COMMENT '商品标题',
  `order_amount`         decimal(10,2)          DEFAULT '0.00' COMMENT '订单金额',
  `commission_amount`    decimal(10,2)          DEFAULT '0.00' COMMENT '可分配佣金',
  `rebate_rate`          decimal(10,4)          DEFAULT '0.0000' COMMENT '返利比例',
  `rebate_amount`        decimal(10,2)          DEFAULT '0.00' COMMENT '返利金额',
  `rebate_amount_cent`   bigint                 DEFAULT NULL COMMENT '返利金额（分，V2优先读取）',
  `rebate_config_id`     bigint                 DEFAULT NULL COMMENT '结算时匹配的返利配置ID快照',
  `member_level_id_snapshot` bigint             DEFAULT NULL COMMENT '结算时会员等级ID快照',
  `idempotency_key`      varchar(128)           DEFAULT NULL COMMENT '资金操作幂等键',
  `rebate_type`          varchar(32)   NOT NULL DEFAULT 'rebate' COMMENT '返利类型（rebate:返利入账 refund:返利扣回 adjust:系统调整）',
  `rebate_status`        varchar(32)   NOT NULL DEFAULT 'pending' COMMENT '返利状态（pending:待结算 Rcptd:已到账 refunded:已扣回）',
  `preceding_rebate_id`  bigint                 DEFAULT NULL COMMENT '前序返利ID（扣回时关联）',
  `freeze_record_id`     bigint                 DEFAULT NULL COMMENT '关联的冻结记录ID',
  `remark`               varchar(512)           DEFAULT NULL COMMENT '备注',
  `creator`              varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`            bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_order_rebate_type` (`tenant_id`, `order_id`, `rebate_type`) USING BTREE,
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_order_id` (`order_id`) USING BTREE,
  KEY `idx_platform_order_id` (`platform_order_id`) USING BTREE,
  KEY `idx_rebate_type` (`rebate_type`) USING BTREE,
  KEY `idx_rebate_status` (`rebate_status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利记录表';

-- ----------------------------
-- 6. 会员返利账户表（Phase4新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_rebate_account`;
CREATE TABLE `cps_rebate_account` (
  `id`                bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`         bigint        NOT NULL COMMENT '会员ID',
  `total_rebate`      decimal(10,2)          DEFAULT '0.00' COMMENT '累计返利总额',
  `available_balance` decimal(10,2)          DEFAULT '0.00' COMMENT '可用余额',
  `frozen_balance`    decimal(10,2)          DEFAULT '0.00' COMMENT '冻结余额',
  `debt_balance`      decimal(10,2)          DEFAULT '0.00' COMMENT '欠款余额',
  `withdrawn_amount`  decimal(10,2)          DEFAULT '0.00' COMMENT '已提现金额',
  `status`            tinyint       NOT NULL DEFAULT '1' COMMENT '状态（0冻结 1正常）',
  `version`           int           NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `creator`           varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`       datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`          bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_member_id` (`tenant_id`, `member_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员返利账户表';

-- ----------------------------
-- 7. 提现申请表（含Phase4扩展字段）
-- ----------------------------
DROP TABLE IF EXISTS `cps_withdraw`;
CREATE TABLE `cps_withdraw` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`             bigint        NOT NULL COMMENT '会员ID',
  `withdraw_no`           varchar(64)   NOT NULL COMMENT '提现单号',
  `withdraw_type`         varchar(32)   NOT NULL DEFAULT 'alipay' COMMENT '提现类型（alipay:支付宝 wechat:微信 bank:银行卡）',
  `withdraw_account`      varchar(128)  NOT NULL COMMENT '提现账户',
  `withdraw_account_name` varchar(64)            DEFAULT NULL COMMENT '账户名称',
  `amount`                decimal(10,2) NOT NULL COMMENT '提现金额',
  `amount_cent`           bigint        NOT NULL COMMENT '提现金额（分，V2资金计算字段）',
  `fee_amount`            decimal(10,2)          DEFAULT '0.00' COMMENT '手续费',
  `actual_amount`         decimal(10,2) NOT NULL COMMENT '实际到账金额',
  `status`                varchar(32)   NOT NULL DEFAULT 'created' COMMENT '状态（created:已申请 reviewing:审核中 passed:审核通过 rejected:审核驳回 SUCCESS:成功 failed:失败 refunded:已退回）',
  `audit_user_id`         bigint                 DEFAULT NULL COMMENT '审核人ID',
  `audit_time`            datetime               DEFAULT NULL COMMENT '审核时间',
  `review_note`           varchar(512)           DEFAULT NULL COMMENT '审核备注',
  `transaction_no`        varchar(64)            DEFAULT NULL COMMENT '转账单号',
  `transfer_status`       varchar(32)            DEFAULT NULL COMMENT '打款状态（WAITING:待打款 PROCESSING:打款中 SUCCESS:成功 FAILED:失败）',
  `transfer_time`         datetime               DEFAULT NULL COMMENT '转账时间',
  `transfer_error`        varchar(512)           DEFAULT NULL COMMENT '转账错误信息',
  `freeze_record_id`      bigint                 DEFAULT NULL COMMENT '统一资产冻结记录ID',
  `idempotency_key`       varchar(64)   NOT NULL COMMENT '提现请求幂等键（租户内唯一）',
  `status_version`        int           NOT NULL DEFAULT '0' COMMENT '状态CAS版本',
  `pay_transfer_id`       bigint                 DEFAULT NULL COMMENT 'Pay模块转账单ID',
  `transfer_channel_code` varchar(32)            DEFAULT NULL COMMENT 'Pay模块转账渠道编码',
  `retry_count`           int           NOT NULL DEFAULT '0' COMMENT '补偿重试次数',
  `next_retry_time`       datetime               DEFAULT NULL COMMENT '下次补偿时间',
  `last_attempt_time`     datetime               DEFAULT NULL COMMENT '最近打款尝试时间',
  `creator`               varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`) USING BTREE,
  UNIQUE KEY `uk_withdraw_tenant_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE,
  KEY `idx_audit_user_id` (`audit_user_id`) USING BTREE,
  KEY `idx_transfer_status` (`transfer_status`) USING BTREE,
  KEY `idx_withdraw_compensation` (`tenant_id`, `status`, `next_retry_time`) USING BTREE,
  KEY `idx_pay_transfer_id` (`pay_transfer_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

-- ----------------------------
-- 8. 统计数据表（含Phase5扩展字段）

-- ----------------------------
-- 9. MCP API Key管理表（Phase6定版，含tenant_id）
-- ----------------------------
DROP TABLE IF EXISTS `cps_mcp_api_key`;
CREATE TABLE `cps_mcp_api_key` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`          varchar(64)  NOT NULL COMMENT 'API Key名称（标识用途）',
  `key_value`     varchar(128) NOT NULL COMMENT 'API Key值（创建后只展示一次，存储SHA-256摘要前缀）',
  `description`   varchar(255)          DEFAULT NULL COMMENT '描述（接入方信息、用途说明）',
  `status`        tinyint      NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `expire_time`   datetime              DEFAULT NULL COMMENT '过期时间（NULL=永不过期）',
  `last_use_time` datetime              DEFAULT NULL COMMENT '最后使用时间',
  `use_count`     bigint       NOT NULL DEFAULT '0' COMMENT '累计调用次数',
  `creator`       varchar(64)           DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`       varchar(64)           DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`     bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_value` (`key_value`, `deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MCP API Key管理表';

-- ----------------------------
-- 10. MCP访问日志表（Phase6定版，含tenant_id）
-- ----------------------------
DROP TABLE IF EXISTS `cps_mcp_access_log`;
CREATE TABLE `cps_mcp_access_log` (
  `id`             bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `api_key_id`     bigint                DEFAULT NULL COMMENT 'API Key ID（NULL=匿名访问）',
  `member_id`      bigint                DEFAULT NULL COMMENT '会员编号',
  `actor_user_id`  bigint                DEFAULT NULL COMMENT '实际调用用户编号',
  `actor_user_type` varchar(16)           DEFAULT NULL COMMENT '实际调用用户类型（ADMIN/MEMBER）',
  `conversation_id` bigint                DEFAULT NULL COMMENT 'AI对话编号',
  `mcp_client_name` varchar(128)          DEFAULT NULL COMMENT 'MCP Client名称',
  `invocation_source` varchar(32)         DEFAULT NULL COMMENT '调用来源',
  `trace_id`       varchar(64)           DEFAULT NULL COMMENT '链路追踪编号',
  `tool_name`      varchar(64)  NOT NULL COMMENT '调用的Tool名称',
  `request_params` text                  DEFAULT NULL COMMENT '请求参数（JSON）',
  `response_data`  text                  DEFAULT NULL COMMENT '响应数据摘要',
  `status`         tinyint      NOT NULL DEFAULT '1' COMMENT '调用状态（0失败 1成功）',
  `error_message`  varchar(512)          DEFAULT NULL COMMENT '错误信息（失败时记录）',
  `duration_ms`    int          NOT NULL DEFAULT '0' COMMENT '耗时（毫秒）',
  `client_ip`      varchar(64)           DEFAULT NULL COMMENT '客户端IP',
  `creator`        varchar(64)           DEFAULT '' COMMENT '创建者',
  `create_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)           DEFAULT '' COMMENT '更新者',
  `update_time`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`      bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_api_key_id` (`api_key_id`) USING BTREE,
  KEY `idx_tool_name` (`tool_name`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='MCP访问日志表';

-- ----------------------------
-- 11. OpenAPI访问审计日志表
-- ----------------------------
DROP TABLE IF EXISTS `cps_openapi_access_log`;
CREATE TABLE `cps_openapi_access_log` (
  `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id`          varchar(64)           DEFAULT NULL COMMENT '调用方应用ID',
  `request_method`  varchar(16)           DEFAULT NULL COMMENT 'HTTP方法',
  `request_uri`     varchar(255)          DEFAULT NULL COMMENT '请求路径',
  `idempotency_key` varchar(128)          DEFAULT NULL COMMENT '幂等键',
  `request_headers` text                  DEFAULT NULL COMMENT '脱敏请求头快照JSON',
  `status`          tinyint      NOT NULL DEFAULT '0' COMMENT '调用状态（0失败 1成功）',
  `failure_reason`  varchar(128)          DEFAULT NULL COMMENT '失败原因',
  `client_ip`       varchar(64)           DEFAULT NULL COMMENT '客户端IP',
  `creator`         varchar(64)           DEFAULT '' COMMENT '创建者',
  `create_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         varchar(64)           DEFAULT '' COMMENT '更新者',
  `update_time`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_openapi_app_time` (`tenant_id`, `app_id`, `create_time`) USING BTREE,
  KEY `idx_openapi_failure` (`tenant_id`, `failure_reason`, `create_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI访问审计日志表';

-- ----------------------------
-- 12. CPS转链记录表（Phase7新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_transfer_record`;
CREATE TABLE `cps_transfer_record` (
  `id`                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`          bigint       NOT NULL COMMENT '会员ID',
  `platform_code`      varchar(32)  NOT NULL COMMENT '平台编码',
  `vendor_code`        varchar(32)           DEFAULT NULL COMMENT '供应商编码',
  `activity_id`        bigint                DEFAULT NULL COMMENT '活动ID',
  `attribution_type`   varchar(32)           DEFAULT NULL COMMENT '归因令牌类型',
  `attribution_token`  varchar(64)           DEFAULT NULL COMMENT '不透明归因令牌',
  `original_content`   varchar(500)          DEFAULT NULL COMMENT '原始口令/链接',
  `item_id`            varchar(64)           DEFAULT NULL COMMENT '商品ID',
  `item_title`         varchar(512)          DEFAULT NULL COMMENT '商品标题',
  `promotion_url`      varchar(500)          DEFAULT NULL COMMENT '推广链接',
  `tao_command`        varchar(500)          DEFAULT NULL COMMENT '生成的淘口令',
  `platform_order_id`  varchar(64)           DEFAULT NULL COMMENT '关联的订单号',
  `adzone_id`          varchar(128)          DEFAULT NULL COMMENT '推广位ID',
  `expire_time`        datetime              DEFAULT NULL COMMENT '过期时间',
  `status`             tinyint      NOT NULL DEFAULT '1' COMMENT '状态（0无效 1有效）',
  `creator`            varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`            varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`            bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`          bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_platform_order_id` (`platform_order_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  UNIQUE KEY `uk_transfer_attribution_token` (`tenant_id`, `vendor_code`, `platform_code`, `attribution_type`, `attribution_token`, `deleted`) USING BTREE,
  KEY `idx_transfer_attribution_lookup` (`tenant_id`, `vendor_code`, `platform_code`, `attribution_type`, `attribution_token`, `status`, `expire_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS转链记录表';

-- ----------------------------
-- 12. 冻结解冻配置表（Phase7新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_freeze_config`;
CREATE TABLE `cps_freeze_config` (
  `id`             bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`  varchar(32)          DEFAULT NULL COMMENT '平台编码（NULL表示全平台）',
  `min_amount_cent` bigint     NOT NULL DEFAULT '0' COMMENT '返利金额下限（分，包含）',
  `max_amount_cent` bigint              DEFAULT NULL COMMENT '返利金额上限（分，不包含；NULL表示无上限）',
  `unfreeze_days`  int         NOT NULL DEFAULT '15' COMMENT '解冻天数（资格时间后天数）',
  `status`         tinyint     NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `remark`         varchar(255)         DEFAULT NULL COMMENT '备注',
  `creator`        varchar(64)          DEFAULT NULL COMMENT '创建人',
  `create_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`        varchar(64)          DEFAULT NULL COMMENT '更新人',
  `update_time`    datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        bit(1)               DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`      bigint      NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_platform_amount` (`tenant_id`, `platform_code`, `status`, `min_amount_cent`, `max_amount_cent`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冻结解冻配置表';

-- 初始化默认配置（全局15天解冻）
INSERT INTO `cps_freeze_config` (`platform_code`, `min_amount_cent`, `max_amount_cent`, `unfreeze_days`, `status`, `remark`)
VALUES (NULL, 0, NULL, 15, 1, '全平台全金额默认配置-资格时间后15天解冻');

-- ----------------------------
-- 13. 冻结解冻记录表（Phase7新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_freeze_record`;
CREATE TABLE `cps_freeze_record` (
  `id`                   bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`            bigint        NOT NULL COMMENT '会员ID',
  `order_id`             bigint                 DEFAULT NULL COMMENT '订单ID',
  `platform_order_id`    varchar(64)            DEFAULT NULL COMMENT '平台订单号',
  `business_type`        varchar(32)            DEFAULT NULL COMMENT '业务类型（ORDER_REBATE/TOKEN_EXCHANGE）',
  `business_id`          varchar(64)            DEFAULT NULL COMMENT '业务单号',
  `idempotency_key`      varchar(64)            DEFAULT NULL COMMENT '幂等键',
  `freeze_amount`        decimal(10,2) NOT NULL COMMENT '冻结金额',
  `amount_cent`          bigint        NOT NULL COMMENT '冻结金额（分，V2优先读取）',
  `freeze_config_id`     bigint                 DEFAULT NULL COMMENT '匹配的冻结配置ID快照',
  `freeze_days_snapshot` int                    DEFAULT NULL COMMENT '冻结天数快照',
  `eligible_time`        datetime               DEFAULT NULL COMMENT '冻结资格时间（收货与平台结算时间取晚）',
  `unfreeze_time`        datetime      NOT NULL COMMENT '计划解冻时间',
  `actual_unfreeze_time` datetime               DEFAULT NULL COMMENT '实际解冻时间',
  `status`               varchar(16)   NOT NULL DEFAULT 'frozen' COMMENT '状态（frozen:已冻结 unfreezing:解冻中 unfreezed:已解冻）',
  `manual_unfreeze_reason` varchar(512)         DEFAULT NULL COMMENT '管理员手动解冻原因',
  `manual_unfreeze_operator_id` bigint          DEFAULT NULL COMMENT '管理员手动解冻操作人ID',
  `creator`              varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`              varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`          datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`              bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`            bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_business_idempotency` (`tenant_id`, `business_type`, `idempotency_key`) USING BTREE,
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_business_id` (`business_type`, `business_id`) USING BTREE,
  KEY `idx_unfreeze_time` (`unfreeze_time`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冻结解冻记录表';

-- ----------------------------
-- 14. 阶段一资金与归因安全基线
-- ----------------------------
DROP TABLE IF EXISTS `cps_rebate_asset_ledger`;
CREATE TABLE `cps_rebate_asset_ledger` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`             bigint       NOT NULL COMMENT '会员ID',
  `source_system`         varchar(32)  NOT NULL COMMENT '来源系统',
  `business_type`         varchar(32)  NOT NULL COMMENT '业务类型',
  `business_id`           varchar(128) NOT NULL COMMENT '业务单号',
  `order_id`              bigint                DEFAULT NULL COMMENT 'CPS订单ID',
  `platform_order_id`     varchar(128)          DEFAULT NULL COMMENT '平台订单号',
  `idempotency_key`       varchar(128) NOT NULL COMMENT '幂等键',
  `available_change_cent` bigint       NOT NULL DEFAULT '0' COMMENT '可用余额变更（分）',
  `frozen_change_cent`    bigint       NOT NULL DEFAULT '0' COMMENT '冻结余额变更（分）',
  `debt_change_cent`      bigint       NOT NULL DEFAULT '0' COMMENT '欠款余额变更（分）',
  `available_before_cent` bigint       NOT NULL COMMENT '变更前可用余额（分）',
  `available_after_cent`  bigint       NOT NULL COMMENT '变更后可用余额（分）',
  `frozen_before_cent`    bigint       NOT NULL COMMENT '变更前冻结余额（分）',
  `frozen_after_cent`     bigint       NOT NULL COMMENT '变更后冻结余额（分）',
  `debt_before_cent`      bigint       NOT NULL COMMENT '变更前欠款余额（分）',
  `debt_after_cent`       bigint       NOT NULL COMMENT '变更后欠款余额（分）',
  `operator_type`         varchar(32)  NOT NULL COMMENT '操作主体类型（SYSTEM/ADMIN/MEMBER/SERVICE）',
  `operator_id`           varchar(128)          DEFAULT NULL COMMENT '操作主体ID',
  `reason`                varchar(512) NOT NULL COMMENT '资金变更原因',
  `creator`               varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)           DEFAULT NULL COMMENT '创建后不得更新，仅保留BaseDO兼容字段',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建后不得更新，仅保留BaseDO兼容字段',
  `deleted`               bit(1)                DEFAULT b'0' COMMENT '固定为未删除；资产流水只允许追加',
  `tenant_id`             bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_ledger_idempotency` (`tenant_id`, `business_type`, `idempotency_key`) USING BTREE,
  KEY `idx_asset_ledger_member_time` (`tenant_id`, `member_id`, `create_time`) USING BTREE,
  KEY `idx_asset_ledger_order` (`tenant_id`, `order_id`) USING BTREE,
  KEY `idx_asset_ledger_business` (`tenant_id`, `business_type`, `business_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利资产不可变流水表（只允许INSERT，禁止UPDATE/DELETE）';

DROP TABLE IF EXISTS `cps_rebate_asset_migration_check`;
CREATE TABLE `cps_rebate_asset_migration_check` (
  `id`                                 bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no`                           varchar(64)  NOT NULL COMMENT '预检批次号',
  `tenant_id`                          bigint       NOT NULL COMMENT '租户编号',
  `duplicate_account_count`            bigint       NOT NULL DEFAULT '0' COMMENT '重复账户组数',
  `duplicate_order_count`              bigint       NOT NULL DEFAULT '0' COMMENT '重复订单组数',
  `duplicate_rebate_record_count`      bigint       NOT NULL DEFAULT '0' COMMENT '重复返利主记录组数',
  `duplicate_ledger_idempotency_count` bigint       NOT NULL DEFAULT '0' COMMENT '重复资产幂等键组数',
  `duplicate_freeze_idempotency_count` bigint       NOT NULL DEFAULT '0' COMMENT '重复冻结幂等键组数',
  `account_ledger_mismatch_count`      bigint       NOT NULL DEFAULT '0' COMMENT '账户净资产与流水不一致账户数',
  `freeze_account_mismatch_count`      bigint       NOT NULL DEFAULT '0' COMMENT '冻结记录与账户冻结余额不一致账户数',
  `missing_opening_balance_count`      bigint       NOT NULL DEFAULT '0' COMMENT '缺失期初流水账户数',
  `orphan_ledger_count`                bigint       NOT NULL DEFAULT '0' COMMENT '找不到同租户账户的资产流水数',
  `orphan_active_freeze_count`         bigint       NOT NULL DEFAULT '0' COMMENT '找不到同租户账户的有效冻结记录数',
  `ready`                              tinyint      NOT NULL DEFAULT '0' COMMENT '是否允许进入发布B审批',
  `operator_id`                        varchar(64)  NOT NULL COMMENT '执行人',
  `executed_at`                        datetime     NOT NULL COMMENT '执行时间',
  `summary`                            varchar(512) NOT NULL COMMENT '检查摘要',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_migration_check_tenant_batch` (`tenant_id`, `batch_no`) USING BTREE,
  KEY `idx_migration_check_tenant_time` (`tenant_id`, `executed_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利资产V2迁移预检不可变归档（只允许INSERT）';

DROP TABLE IF EXISTS `cps_rebate_debt`;
CREATE TABLE `cps_rebate_debt` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`             bigint       NOT NULL COMMENT '会员ID',
  `order_id`              bigint                DEFAULT NULL COMMENT '来源订单ID',
  `platform_order_id`     varchar(128)          DEFAULT NULL COMMENT '来源平台订单号',
  `source_business_id`    varchar(128) NOT NULL COMMENT '来源退款或调整业务单号',
  `idempotency_key`       varchar(128) NOT NULL COMMENT '欠款操作幂等键',
  `original_debt_cent`    bigint       NOT NULL COMMENT '原始欠款（分）',
  `repaid_debt_cent`      bigint       NOT NULL DEFAULT '0' COMMENT '已偿还欠款（分）',
  `waived_debt_cent`      bigint       NOT NULL DEFAULT '0' COMMENT '已减免欠款（分）',
  `outstanding_debt_cent` bigint       NOT NULL COMMENT '未偿还欠款（分）',
  `status`                varchar(16)  NOT NULL DEFAULT 'OPEN' COMMENT '状态（OPEN/PARTIAL/CLEARED/WAIVED）',
  `last_reminder_time`    datetime              DEFAULT NULL COMMENT '最近站内提醒时间',
  `next_reminder_time`    datetime              DEFAULT NULL COMMENT '下次站内提醒时间',
  `reminder_end_time`     datetime              DEFAULT NULL COMMENT '提醒截止时间',
  `last_sms_time`         datetime              DEFAULT NULL COMMENT '最近短信提醒时间',
  `reminder_count`        int          NOT NULL DEFAULT '0' COMMENT '站内提醒次数',
  `sms_count`             int          NOT NULL DEFAULT '0' COMMENT '短信提醒次数',
  `notification_status`   varchar(16)           DEFAULT NULL COMMENT '通知状态（PENDING/SUCCESS/FAILED）',
  `notification_failure_reason` varchar(512)    DEFAULT NULL COMMENT '通知失败原因',
  `creator`               varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_debt_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_debt_source` (`tenant_id`, `source_business_id`) USING BTREE,
  KEY `idx_debt_member_status` (`tenant_id`, `member_id`, `status`, `create_time`) USING BTREE,
  KEY `idx_debt_reminder` (`tenant_id`, `status`, `next_reminder_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利欠款账表';

DROP TABLE IF EXISTS `cps_order_attribution_log`;
CREATE TABLE `cps_order_attribution_log` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`              bigint                DEFAULT NULL COMMENT 'CPS订单ID',
  `platform_code`         varchar(32)  NOT NULL COMMENT '平台编码',
  `platform_order_id`     varchar(128) NOT NULL COMMENT '平台订单号',
  `candidate_member_id`   bigint                DEFAULT NULL COMMENT '候选会员ID',
  `attributed_member_id`  bigint                DEFAULT NULL COMMENT '最终归因会员ID',
  `attribution_source`    varchar(32)           DEFAULT NULL COMMENT '归因来源',
  `binding_type`          varchar(32)           DEFAULT NULL COMMENT '可信绑定类型',
  `binding_id`            varchar(128)          DEFAULT NULL COMMENT '可信绑定标识',
  `action`                varchar(32)  NOT NULL COMMENT '动作（AUTO/MANUAL/REBIND/CLAIM/APPROVED/REJECTED）',
  `result`                varchar(16)  NOT NULL COMMENT '结果（BOUND/REJECTED/CONFLICT/UNATTRIBUTED/PENDING_SYNC）',
  `reject_reason`         varchar(512)          DEFAULT NULL COMMENT '拒绝或冲突原因',
  `operator_type`         varchar(32)  NOT NULL COMMENT '操作主体类型',
  `operator_id`           varchar(128)          DEFAULT NULL COMMENT '操作主体ID',
  `idempotency_key`        varchar(128)          DEFAULT NULL COMMENT '幂等键',
  `review_status`          varchar(32)           DEFAULT NULL COMMENT '复核状态（PENDING_SYNC/PENDING_REVIEW/APPROVED/REJECTED/CONFLICT/ASSET_LOCKED）',
  `review_audit_note`      varchar(500)          DEFAULT NULL COMMENT '复核说明',
  `review_operator_id`     bigint                DEFAULT NULL COMMENT '复核操作人ID',
  `review_time`            datetime              DEFAULT NULL COMMENT '复核时间',
  `creator`               varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attribution_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_attribution_order` (`tenant_id`, `platform_code`, `platform_order_id`, `create_time`) USING BTREE,
  KEY `idx_attribution_member` (`tenant_id`, `attributed_member_id`, `create_time`) USING BTREE,
  KEY `idx_attribution_result` (`tenant_id`, `result`, `create_time`) USING BTREE,
  KEY `idx_attribution_claim_review` (`tenant_id`, `action`, `review_status`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单归因审计日志表';

DROP TABLE IF EXISTS `cps_order_sync_checkpoint`;
CREATE TABLE `cps_order_sync_checkpoint` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`         varchar(32)  NOT NULL COMMENT '平台编码',
  `vendor_code`           varchar(32)  NOT NULL DEFAULT 'OFFICIAL' COMMENT '供应商编码',
  `order_scene`           tinyint      NOT NULL DEFAULT '1' COMMENT '订单场景',
  `query_type`            varchar(32)  NOT NULL COMMENT '查询类型（INCREMENTAL/FULL/COMPENSATION）',
  `pagination_mode`       varchar(16)           DEFAULT NULL COMMENT '分页模式（PAGE/CURSOR）',
  `next_cursor`           varchar(512)          DEFAULT NULL COMMENT '下一页游标',
  `next_page_no`          int                   DEFAULT NULL COMMENT '下一页页码',
  `watermark_time`        datetime              DEFAULT NULL COMMENT '最近成功水位时间',
  `query_end_time`        datetime              DEFAULT NULL COMMENT '当前分页窗口固定结束时间（完成前不得漂移）',
  `last_sync_status`      varchar(16)           DEFAULT NULL COMMENT '最近同步状态（SUCCESS/PARTIAL/FAILED）',
  `last_success_count`    int          NOT NULL DEFAULT '0' COMMENT '最近成功条数',
  `last_failure_count`    int          NOT NULL DEFAULT '0' COMMENT '最近失败条数',
  `failure_summary`       text                  DEFAULT NULL COMMENT '失败订单摘要',
  `version`               int          NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `creator`               varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_checkpoint` (`tenant_id`, `platform_code`, `vendor_code`, `order_scene`, `query_type`) USING BTREE,
  KEY `idx_sync_checkpoint_status` (`tenant_id`, `last_sync_status`, `update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步成功水位表';

DROP TABLE IF EXISTS `cps_rebate_asset_policy`;
CREATE TABLE `cps_rebate_asset_policy` (
  `id`                         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `v2_enabled`                 tinyint     NOT NULL DEFAULT '0' COMMENT '是否启用V2资产写入（0否 1是）',
  `migration_ready`            tinyint     NOT NULL DEFAULT '0' COMMENT '发布B与最新预检批次已绑定（0否 1是）',
  `latest_ready_check_batch_no` varchar(64)         DEFAULT NULL COMMENT '最近一次通过并获发布B批准的预检批次',
  `ready_check_time`           datetime             DEFAULT NULL COMMENT '上述预检批次执行时间',
  `read_only`                  tinyint     NOT NULL DEFAULT '0' COMMENT '资产操作只读开关（0否 1是）',
  `large_debt_threshold_cent`  bigint      NOT NULL DEFAULT '10000' COMMENT '大额欠款阈值（分，默认100元）',
  `reminder_interval_days`     int         NOT NULL DEFAULT '7' COMMENT '普通站内提醒间隔天数',
  `normal_reminder_days`       int         NOT NULL DEFAULT '30' COMMENT '普通欠款提醒持续天数',
  `large_reminder_days`        int         NOT NULL DEFAULT '180' COMMENT '大额欠款提醒持续天数',
  `sms_interval_days`          int         NOT NULL DEFAULT '30' COMMENT '大额欠款短信最小间隔天数',
  `creator`                    varchar(64)          DEFAULT NULL COMMENT '创建人',
  `create_time`                datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                    varchar(64)          DEFAULT NULL COMMENT '更新人',
  `update_time`                datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                    bit(1)               DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`                  bigint      NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_policy_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户返利资产V2策略表';

-- ----------------------------
-- 14.1 返利兑换AI Token订单表（生态P0新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_rebate_token_exchange_order`;
CREATE TABLE `cps_rebate_token_exchange_order` (
  `id`                       bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `exchange_order_no`        varchar(64)   NOT NULL COMMENT 'CPS兑换订单号',
  `member_id`                bigint        NOT NULL COMMENT '会员ID',
  `source_system`            varchar(32)   NOT NULL COMMENT '来源系统',
  `source_asset`             varchar(32)   NOT NULL COMMENT '来源资产',
  `source_amount`            decimal(10,2) NOT NULL COMMENT '来源金额（元）',
  `target_asset`             varchar(32)   NOT NULL COMMENT '目标资产',
  `target_tokens`            bigint                 DEFAULT NULL COMMENT '目标Token数',
  `exchange_rate`            decimal(12,4)          DEFAULT NULL COMMENT '兑换比例',
  `freeze_record_id`         bigint                 DEFAULT NULL COMMENT '冻结记录ID',
  `aitoken_exchange_order_id` varchar(64)            DEFAULT NULL COMMENT 'aitoken兑换订单号',
  `status`                   varchar(20)   NOT NULL COMMENT '状态',
  `failure_reason`           varchar(500)           DEFAULT NULL COMMENT '失败原因',
  `idempotency_key`          varchar(64)   NOT NULL COMMENT '幂等键',
  `completed_at`             datetime               DEFAULT NULL COMMENT '完成时间',
  `retry_count`              int           NOT NULL DEFAULT '0' COMMENT '补偿重试次数',
  `next_retry_time`          datetime               DEFAULT NULL COMMENT '下次补偿时间',
  `last_compensation_at`     datetime               DEFAULT NULL COMMENT '最近补偿时间',
  `status_version`           int           NOT NULL DEFAULT '0' COMMENT '状态变更版本',
  `creator`                  varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`              datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                  varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`              datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                  bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`                bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exchange_tenant_order_no` (`tenant_id`, `exchange_order_no`) USING BTREE,
  UNIQUE KEY `uk_exchange_tenant_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_member_id` (`member_id`) USING BTREE,
  KEY `idx_exchange_compensation` (`tenant_id`, `status`, `next_retry_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利兑换AI Token订单表';

-- ----------------------------
-- 14. CPS订单同步日志表（Phase3A新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_order_sync_log`;
CREATE TABLE `cps_order_sync_log` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`     varchar(32)  NOT NULL COMMENT '平台编码（taobao/jd/pdd/douyin）',
  `sync_type`         tinyint      NOT NULL DEFAULT '1' COMMENT '同步类型（1=增量同步 2=全量补偿）',
  `query_type`        tinyint      NOT NULL DEFAULT '4' COMMENT '查询时间维度（1=下单时间 2=付款时间 3=结算时间 4=更新时间）',
  `query_start_time`  datetime              DEFAULT NULL COMMENT '查询开始时间',
  `query_end_time`    datetime              DEFAULT NULL COMMENT '查询结束时间',
  `sync_status`       tinyint               DEFAULT NULL COMMENT '同步状态（1=成功 2=失败 3=部分成功）',
  `total_count`       int                   DEFAULT '0' COMMENT '拉取到的订单总数',
  `new_count`         int                   DEFAULT '0' COMMENT '新增订单数',
  `update_count`      int                   DEFAULT '0' COMMENT '更新订单数',
  `skip_count`        int                   DEFAULT '0' COMMENT '忽略订单数（重复/无效）',
  `sync_start_time`   datetime              DEFAULT NULL COMMENT '同步开始时间',
  `sync_end_time`     datetime              DEFAULT NULL COMMENT '同步结束时间',
  `cost_ms`           bigint                DEFAULT '0' COMMENT '耗时（毫秒）',
  `error_msg`         varchar(500)          DEFAULT NULL COMMENT '错误信息（同步失败时记录）',
  `creator`           varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_platform_code` (`platform_code`) USING BTREE,
  KEY `idx_sync_status` (`sync_status`) USING BTREE,
  KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS订单同步日志表';

-- ----------------------------
-- 8. CPS统计数据表（Phase5新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_statistics`;
CREATE TABLE `cps_statistics` (
  `id`                        bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date`                 date          NOT NULL COMMENT '统计日期',
  `platform_code`             varchar(32)   NOT NULL DEFAULT 'total' COMMENT '平台编码（total=全平台）',
  `order_count`               int           NOT NULL DEFAULT '0' COMMENT '订单总数',
  `new_order_count`           int           NOT NULL DEFAULT '0' COMMENT '新增订单数（不含退款）',
  `order_amount`              decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
  `commission_amount`         decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '佣金总额',
  `settled_commission_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '已结算佣金',
  `pending_commission_amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '待结算佣金',
  `rebate_amount`             decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '返利总额',
  `profit_amount`             decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '平台利润（佣金-返利）',
  `active_member_count`       int           NOT NULL DEFAULT '0' COMMENT '活跃会员数（当日有下单）',
  `creator`                   varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`               datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                   varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`               datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                   bit(1)                 DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`                 bigint        NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date_platform` (`stat_date`, `platform_code`, `tenant_id`) USING BTREE,
  KEY `idx_stat_date` (`stat_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS统计数据表';

-- ----------------------------
-- 15. CPS风控规则表（Phase7新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_risk_rule`;
CREATE TABLE `cps_risk_rule` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_type`    varchar(32)  NOT NULL COMMENT '规则类型（rate_limit:频率限制 blacklist:黑名单）',
  `target_type`  varchar(32)  NOT NULL DEFAULT 'member' COMMENT '目标类型（member:会员 ip:IP）',
  `target_value` varchar(128)          DEFAULT NULL COMMENT '目标值（blacklist类型：会员ID或IP地址）',
  `limit_count`  int                   DEFAULT NULL COMMENT '限制次数（rate_limit类型：每日最大转链次数）',
  `status`       tinyint      NOT NULL DEFAULT '1' COMMENT '状态（0禁用 1启用）',
  `remark`       varchar(255)          DEFAULT NULL COMMENT '备注',
  `creator`      varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`      varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`    bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_rule_type` (`rule_type`) USING BTREE,
  KEY `idx_target_value` (`target_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS风控规则表';

-- 默认规则：每日转链频率限制 100 次
INSERT INTO `cps_risk_rule` (`rule_type`, `target_type`, `limit_count`, `status`, `remark`)
VALUES ('rate_limit', 'member', 100, 1, '每日转链次数默认上限');

-- ----------------------------
-- 16. 补充复合索引（Phase7性能优化）
-- ----------------------------
ALTER TABLE `cps_order`
  ADD INDEX `idx_member_status` (`member_id`, `order_status`) USING BTREE,
  ADD INDEX `idx_platform_create` (`platform_code`, `create_time`) USING BTREE;

ALTER TABLE `cps_rebate_record`
  ADD INDEX `idx_member_status` (`member_id`, `rebate_status`) USING BTREE;

ALTER TABLE `cps_transfer_record`
  ADD INDEX `idx_member_create` (`member_id`, `create_time`) USING BTREE;

-- ----------------------------
-- 15. CPS API供应商配置表（多供应商架构改造新增）
-- ----------------------------
DROP TABLE IF EXISTS `cps_api_vendor`;
CREATE TABLE `cps_api_vendor` (
  `id`                bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `vendor_code`       varchar(32)  NOT NULL COMMENT '供应商编码: dataoke/haodanku/miaoyouquan/shihuizhu/official',
  `vendor_name`       varchar(64)  NOT NULL COMMENT '供应商名称',
  `vendor_type`       varchar(16)  NOT NULL COMMENT '供应商类型: aggregator/official',
  `platform_code`     varchar(32)  NOT NULL COMMENT '电商平台编码: taobao/jd/pdd/vip/meituan/douyin',
  `app_key`           varchar(128) NOT NULL COMMENT 'API Key',
  `app_secret`        varchar(256) NOT NULL COMMENT 'API Secret（加密存储）',
  `api_base_url`      varchar(256) NOT NULL COMMENT 'API基础URL',
  `auth_token`        varchar(512)          DEFAULT NULL COMMENT '授权令牌（OAuth2 token / unionId等）',
  `default_adzone_id` varchar(128)          DEFAULT NULL COMMENT '默认推广位ID',
  `extra_config`      text                  COMMENT '扩展配置（JSON），存储供应商特有参数',
  `priority`          int          NOT NULL DEFAULT '0' COMMENT '优先级，数字越大优先级越高',
  `status`            tinyint      NOT NULL DEFAULT '1' COMMENT '状态: 0=禁用, 1=启用',
  `remark`            varchar(256)          DEFAULT NULL COMMENT '备注',
  `creator`           varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`           varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           bit(1)                DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`         bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  `active_unique_key` varchar(191) GENERATED ALWAYS AS (IF(`deleted` = b'0', CONCAT(CHAR_LENGTH(CAST(`tenant_id` AS CHAR)), ':', CAST(`tenant_id` AS CHAR), CHAR_LENGTH(`vendor_code`), ':', `vendor_code`, CHAR_LENGTH(`platform_code`), ':', `platform_code`), NULL)) STORED COMMENT '未删除供应商租户唯一键（长度前缀编码）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cps_api_vendor_active` (`active_unique_key`) USING BTREE,
  INDEX `idx_platform_code` (`platform_code`) USING BTREE,
  INDEX `idx_vendor_code` (`vendor_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS API供应商配置表';

DROP TABLE IF EXISTS `cps_didi_callback_event`;
CREATE TABLE `cps_didi_callback_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_type` varchar(16) NOT NULL COMMENT '事件类型：ORDER/REWARD',
  `idempotency_key` varchar(256) NOT NULL COMMENT '幂等键',
  `app_key` varchar(128) NOT NULL COMMENT '滴滴 App-Key',
  `trace_id` varchar(128) DEFAULT NULL COMMENT '领券 trace_id',
  `platform_order_id` varchar(128) DEFAULT NULL COMMENT '滴滴订单号',
  `activity_id` varchar(64) DEFAULT NULL COMMENT '活动ID',
  `source_id` varchar(255) DEFAULT NULL COMMENT '推广来源ID',
  `reward_sent` bit(1) DEFAULT NULL COMMENT '是否实际发券',
  `retry_times` int DEFAULT NULL COMMENT '滴滴重试次数',
  `process_status` varchar(16) NOT NULL COMMENT 'PROCESSING/SUCCESS/FAILED',
  `failure_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `request_body` longtext NOT NULL COMMENT '原始请求体',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_didi_callback_idempotency` (`tenant_id`,`idempotency_key`) USING BTREE,
  KEY `idx_didi_callback_order` (`tenant_id`,`platform_order_id`) USING BTREE,
  KEY `idx_didi_callback_trace` (`tenant_id`,`trace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='滴滴联盟回调事件审计表';

-- 好单库唯品会配置占位：不内置真实凭证，管理员填写 apikey 后再启用。
INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES
('haodanku', '好单库', 'aggregator', 'vip', '', '', 'https://v2.api.haodanku.com', NULL, NULL, '{"searchPath":"/vip_goods_search","convertPath":"/vip_ratesurl"}', 100, 0, '请配置好单库 apikey 和唯品会 PID；普通账号使用 v2 搜索与转链，v3 订单能力按官方权限开通', 'system', '2026-08-07 17:30:00', 'system', '2026-08-11 16:15:00', b'0', 1);

-- ----------------------------
-- 31. CPS平台接入草稿表
-- ----------------------------
DROP TABLE IF EXISTS `cps_platform_onboarding_draft`;
CREATE TABLE `cps_platform_onboarding_draft` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code`         varchar(32)  NOT NULL COMMENT '平台编码',
  `mode`                  varchar(16)  NOT NULL COMMENT '接入模式（CREATE/RECONFIGURE）',
  `payload_ciphertext`    longtext     NOT NULL COMMENT '加密后的配置草稿JSON',
  `draft_version`         int          NOT NULL DEFAULT '1' COMMENT '草稿乐观锁版本',
  `config_fingerprint`    varchar(64)           DEFAULT NULL COMMENT '当前配置指纹',
  `validated_fingerprint` varchar(64)           DEFAULT NULL COMMENT '最近校验通过的配置指纹',
  `status`                varchar(16)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT/VALIDATING/READY/FAILED/PUBLISHED）',
  `check_summary`         text                  COMMENT '最近校验摘要',
  `validated_at`          datetime              DEFAULT NULL COMMENT '最近校验时间',
  `published_at`          datetime              DEFAULT NULL COMMENT '发布时间',
  `creator`               varchar(64)           DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)           DEFAULT NULL COMMENT '更新人',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint       NOT NULL DEFAULT '0' COMMENT '租户编号',
  `active_unique_key`     varchar(128) GENERATED ALWAYS AS (IF(`deleted` = b'0', CONCAT(`tenant_id`, ':', `platform_code`), NULL)) STORED COMMENT '未删除草稿租户平台唯一键',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cps_platform_onboarding_draft_active` (`active_unique_key`) USING BTREE,
  KEY `idx_cps_platform_onboarding_draft_status` (`tenant_id`, `status`, `update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS平台接入草稿表';



-- ----------------------------
-- CPS extended schema and menus merged into the canonical module all-in-one script.
-- Future CPS database script updates must be made only in backend/sql/module/cps-all-in-one.sql.
-- ----------------------------

-- ----------------------------
-- Table structure for cps_rebate_activity
-- ----------------------------
DROP TABLE IF EXISTS `cps_rebate_activity`;
CREATE TABLE `cps_rebate_activity`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `activity_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '活动名称',
  `activity_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '专题类型',
  `platform_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '平台编码',
  `main_pic` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '活动主图',
  `short_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '短描述',
  `rebate_desc` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '返利/奖励文案',
  `billing_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CPS' COMMENT '计费类型：CPS/CPA/CPS+CPA',
  `promotion_count` int NOT NULL DEFAULT 0 COMMENT '推广数',
  `source_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'configured' COMMENT '来源类型：configured/vendor',
  `external_activity_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '外部活动ID',
  `promotion_activity_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '供应商活动转链参数ID',
  `vendor_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '供应商活动转链元数据JSON',
  `tag_text` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '标签文案',
  `jump_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none' COMMENT '跳转类型：search/url/none',
  `jump_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '跳转地址',
  `search_keyword` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '搜索关键词',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `start_time` datetime NULL DEFAULT NULL COMMENT '上线时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '下线时间',
  `remark` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cps_rebate_activity_center` (`tenant_id`, `deleted`, `status`, `platform_code`, `billing_type`, `start_time`, `end_time`, `sort`) USING BTREE,
  KEY `idx_cps_rebate_activity_external` (`source_type`, `external_activity_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS返利活动表';

-- ----------------------------
-- Records of cps_rebate_activity
-- ----------------------------
BEGIN;
INSERT INTO `cps_rebate_activity` (`id`, `activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, '88VIP开通奖励(优酷版)', '会员权益', 'taobao', '', '淘宝 88VIP 会员年卡活动', '最高7元/张', 'CPA', 1405, 'configured', 'taobao-88vip-youku', '热门', 'search', '', '88VIP', 1, 1, '2025-08-30 00:00:00', '2026-12-31 23:59:59', '', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0', 1);
INSERT INTO `cps_rebate_activity` (`id`, `activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2, '闪购品牌日 单单有福利', '本地生活', 'taobao', '', '淘宝闪购品牌福利活动', '--', 'CPS', 1325, 'configured', 'taobao-flash-brand', '最新', 'search', '', '淘宝闪购', 2, 1, '2025-05-20 00:00:00', '2027-05-13 23:59:59', '', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0', 1);
INSERT INTO `cps_rebate_activity` (`id`, `activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (3, '美团外卖节', '外卖', 'meituan', '', '美团外卖节 帮你吃更好', '预估红包3%、页面：0.1...', 'CPS', 707, 'configured', 'meituan-waimai-festival', '外卖', 'search', '', '美团外卖', 3, 1, '2025-12-23 00:00:00', '2027-12-31 23:59:59', '', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0', 1);
INSERT INTO `cps_rebate_activity` (`id`, `activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (4, '淘票票', '电影票', 'taobao', '', '淘票票买券活动', '2元/张', 'CPS+CPA', 606, 'configured', 'taopiaopiao-ticket', '票券', 'search', '', '淘票票', 4, 1, NULL, NULL, '', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0', 1);

-- ----------------------------
-- Table structure for cps_selection_theme
-- ----------------------------
DROP TABLE IF EXISTS `cps_selection_theme`;
CREATE TABLE `cps_selection_theme`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主题ID',
  `theme_code` varchar(64) NOT NULL COMMENT '主题编码',
  `theme_name` varchar(128) NOT NULL COMMENT '主题名称',
  `theme_type` varchar(32) NOT NULL DEFAULT 'CUSTOM' COMMENT '主题类型',
  `promotion_event` varchar(64) DEFAULT NULL COMMENT '大促标识',
  `platform_codes` varchar(255) DEFAULT NULL COMMENT '平台范围，逗号分隔',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `cover_pic` varchar(512) DEFAULT NULL COMMENT '封面图',
  `description` varchar(512) DEFAULT NULL COMMENT '描述',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签，逗号分隔',
  `rule_json` text COMMENT '主题规则JSON',
  `ai_prompt` varchar(1024) DEFAULT NULL COMMENT 'AI Prompt',
  `goods_square_visible` tinyint NOT NULL DEFAULT 1 COMMENT '是否展示到返利商品广场：0否 1是',
  `ai_summary` varchar(1024) DEFAULT NULL COMMENT 'AI建议摘要',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
  `start_time` datetime DEFAULT NULL COMMENT '上线时间',
  `end_time` datetime DEFAULT NULL COMMENT '下线时间',
  `refresh_status` varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '刷新状态',
  `last_refresh_time` datetime DEFAULT NULL COMMENT '最后刷新时间',
  `refresh_message` varchar(500) DEFAULT NULL COMMENT '刷新结果摘要或失败原因',
  `refresh_started_time` datetime DEFAULT NULL COMMENT '当前刷新租约开始时间',
  `refresh_batch_no` varchar(64) DEFAULT NULL COMMENT '当前刷新租约批次号',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_selection_theme_code` (`tenant_id`, `theme_code`, `deleted`) USING BTREE,
  KEY `idx_cps_selection_theme_page` (`tenant_id`, `deleted`, `status`, `goods_square_visible`, `promotion_event`, `sort`) USING BTREE,
  KEY `idx_cps_selection_theme_platform` (`tenant_id`, `deleted`, `platform_codes`, `vendor_code`) USING BTREE,
  KEY `idx_cps_selection_theme_window` (`tenant_id`, `deleted`, `status`, `start_time`, `end_time`) USING BTREE,
  KEY `idx_cps_selection_theme_refresh_lease` (`tenant_id`, `deleted`, `theme_type`, `status`, `refresh_status`, `refresh_started_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS选品主题表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_selection_theme_item
-- ----------------------------
DROP TABLE IF EXISTS `cps_selection_theme_item`;
CREATE TABLE `cps_selection_theme_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品快照ID',
  `theme_id` bigint NOT NULL COMMENT '主题ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(64) NOT NULL DEFAULT '' COMMENT '供应商编码',
  `goods_id` varchar(128) NOT NULL COMMENT '商品ID',
  `goods_sign` varchar(255) NOT NULL DEFAULT '' COMMENT 'goodsSign',
  `title` varchar(512) DEFAULT NULL COMMENT '商品标题',
  `main_pic` varchar(1024) DEFAULT NULL COMMENT '主图',
  `original_price` decimal(18,2) DEFAULT NULL COMMENT '原价',
  `actual_price` decimal(18,2) DEFAULT NULL COMMENT '券后价',
  `coupon_price` decimal(18,2) DEFAULT NULL COMMENT '优惠券金额',
  `commission_rate` decimal(10,4) DEFAULT NULL COMMENT '佣金率',
  `commission_amount` decimal(18,2) DEFAULT NULL COMMENT '预估佣金',
  `month_sales` bigint DEFAULT NULL COMMENT '近30天销量',
  `shop_name` varchar(255) DEFAULT NULL COMMENT '店铺',
  `brand_name` varchar(255) DEFAULT NULL COMMENT '品牌',
  `category_name` varchar(255) DEFAULT NULL COMMENT '类目',
  `activity_tag` varchar(255) DEFAULT NULL COMMENT '活动标签',
  `rank_tag` varchar(255) DEFAULT NULL COMMENT '榜单标签',
  `selling_point` varchar(512) DEFAULT NULL COMMENT '卖点文案',
  `recommend_score` decimal(10,2) DEFAULT NULL COMMENT '推荐分',
  `recommend_reason` varchar(1024) DEFAULT NULL COMMENT '推荐理由',
  `top_flag` tinyint NOT NULL DEFAULT 0 COMMENT '置顶标识',
  `manual_adjusted` tinyint NOT NULL DEFAULT 0 COMMENT '是否经过人工排序、置顶或状态调整：0否 1是',
  `status` varchar(32) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED/DISABLED',
  `source_type` varchar(32) NOT NULL DEFAULT 'MANUAL' COMMENT '来源类型',
  `item_link` varchar(1024) DEFAULT NULL COMMENT '原始链接',
  `raw_data` mediumtext COMMENT '第三方原始快照',
  `snapshot_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_selection_theme_item_goods` (`tenant_id`, `theme_id`, `platform_code`, `vendor_code`, `goods_id`, `goods_sign`, `deleted`) USING BTREE,
  KEY `idx_cps_selection_theme_item_list` (`tenant_id`, `deleted`, `theme_id`, `status`, `top_flag`, `sort`) USING BTREE,
  KEY `idx_cps_selection_theme_item_platform` (`tenant_id`, `deleted`, `platform_code`, `source_type`) USING BTREE,
  KEY `idx_cps_selection_theme_item_score` (`tenant_id`, `deleted`, `theme_id`, `recommend_score`) USING BTREE,
  KEY `idx_cps_selection_theme_item_refresh` (`tenant_id`, `deleted`, `theme_id`, `source_type`, `snapshot_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS选品主题商品快照表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_selection_ai_review
-- ----------------------------
DROP TABLE IF EXISTS `cps_selection_ai_review`;
CREATE TABLE `cps_selection_ai_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '复核记录ID',
  `review_context_id` varchar(128) NOT NULL COMMENT '分析结果上下文ID',
  `owner_user_id` bigint NOT NULL COMMENT '复核状态所属管理员ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(32) NOT NULL DEFAULT '' COMMENT '供应商编码',
  `goods_id` varchar(128) NOT NULL COMMENT '商品ID',
  `goods_sign` varchar(255) NOT NULL DEFAULT '' COMMENT '商品签名',
  `title` varchar(255) DEFAULT NULL COMMENT '商品标题快照',
  `main_pic` varchar(1024) DEFAULT NULL COMMENT '商品主图快照',
  `review_status` varchar(32) NOT NULL COMMENT '复核状态：CONFIRMED/WITHDRAWN',
  `reviewer_id` bigint NOT NULL COMMENT '复核管理员ID',
  `review_time` datetime NOT NULL COMMENT '复核时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '复核备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_selection_ai_review_goods` (`tenant_id`, `owner_user_id`, `review_context_id`, `platform_code`, `vendor_code`, `goods_id`, `goods_sign`, `deleted`) USING BTREE,
  KEY `idx_cps_selection_ai_review_context` (`tenant_id`, `deleted`, `owner_user_id`, `review_context_id`, `review_status`) USING BTREE,
  KEY `idx_cps_selection_ai_review_operator` (`tenant_id`, `deleted`, `reviewer_id`, `review_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI选品人工复核审计表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_member_goods_record
-- ----------------------------
DROP TABLE IF EXISTS `cps_member_goods_record`;
CREATE TABLE `cps_member_goods_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `record_type` varchar(16) NOT NULL COMMENT '记录类型：BROWSE/FAVORITE',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `goods_id` varchar(128) NOT NULL DEFAULT '' COMMENT '平台商品ID',
  `goods_sign` varchar(512) NOT NULL DEFAULT '' COMMENT '平台商品签名',
  `identity_key` char(64) NOT NULL COMMENT '标准化商品身份SHA-256摘要',
  `title` varchar(512) DEFAULT NULL COMMENT '商品标题展示快照',
  `main_pic` varchar(1024) DEFAULT NULL COMMENT '商品主图展示快照',
  `original_price_cent` bigint DEFAULT NULL COMMENT '商品原价展示快照（分）',
  `actual_price_cent` bigint DEFAULT NULL COMMENT '券后价展示快照（分）',
  `coupon_price_cent` bigint DEFAULT NULL COMMENT '优惠券金额展示快照（分）',
  `estimate_rebate_amount_cent` bigint DEFAULT NULL COMMENT '预估返利金额展示快照（分）',
  `month_sales` bigint DEFAULT NULL COMMENT '近30天销量展示快照',
  `shop_name` varchar(255) DEFAULT NULL COMMENT '店铺名称展示快照',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  `active_unique_key` char(64) GENERATED ALWAYS AS (IF(`deleted` = b'0', `identity_key`, NULL)) STORED COMMENT '未删除记录商品身份唯一键',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_member_goods_record_active` (`tenant_id`, `member_id`, `record_type`, `active_unique_key`) USING BTREE,
  KEY `idx_cps_member_goods_record_page` (`tenant_id`, `member_id`, `record_type`, `deleted`, `update_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS会员商品浏览收藏展示快照表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_goods_master
-- ----------------------------
DROP TABLE IF EXISTS `cps_goods_master`;
CREATE TABLE `cps_goods_master` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品主档ID',
  `master_code` varchar(64) NOT NULL COMMENT '商品主档编码',
  `standard_title` varchar(512) DEFAULT NULL COMMENT '标准商品标题',
  `brand_name` varchar(255) DEFAULT NULL COMMENT '品牌',
  `category_name` varchar(255) DEFAULT NULL COMMENT '类目',
  `main_pic` varchar(1024) DEFAULT NULL COMMENT '主图',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_goods_master_code` (`tenant_id`, `master_code`, `deleted`) USING BTREE,
  KEY `idx_cps_goods_master_page` (`tenant_id`, `deleted`, `status`, `category_name`, `brand_name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS商品主档表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_goods_source_mapping
-- ----------------------------
DROP TABLE IF EXISTS `cps_goods_source_mapping`;
CREATE TABLE `cps_goods_source_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品来源映射ID',
  `master_id` bigint NOT NULL COMMENT '商品主档ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(64) NOT NULL DEFAULT '' COMMENT '供应商编码',
  `external_goods_id` varchar(128) NOT NULL COMMENT '外部商品ID',
  `goods_sign` varchar(255) NOT NULL DEFAULT '' COMMENT 'goodsSign',
  `item_link` varchar(1024) DEFAULT NULL COMMENT '原始商品链接',
  `source_title` varchar(512) DEFAULT NULL COMMENT '来源商品标题',
  `source_main_pic` varchar(1024) DEFAULT NULL COMMENT '来源商品主图',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `last_snapshot_time` datetime DEFAULT NULL COMMENT '最近快照时间',
  `raw_data` mediumtext COMMENT '第三方来源原始数据',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_goods_source` (`tenant_id`, `platform_code`, `vendor_code`, `external_goods_id`, `goods_sign`, `deleted`) USING BTREE,
  KEY `idx_cps_goods_source_master` (`tenant_id`, `deleted`, `master_id`, `status`) USING BTREE,
  KEY `idx_cps_goods_source_page` (`tenant_id`, `deleted`, `platform_code`, `vendor_code`, `last_snapshot_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS商品来源映射表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_goods_price_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `cps_goods_price_snapshot`;
CREATE TABLE `cps_goods_price_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品价格快照ID',
  `master_id` bigint NOT NULL COMMENT '商品主档ID',
  `source_mapping_id` bigint NOT NULL COMMENT '商品来源映射ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(64) NOT NULL DEFAULT '' COMMENT '供应商编码',
  `external_goods_id` varchar(128) NOT NULL COMMENT '外部商品ID',
  `goods_sign` varchar(255) NOT NULL DEFAULT '' COMMENT 'goodsSign',
  `original_price` int DEFAULT NULL COMMENT '原价（分）',
  `actual_price` int DEFAULT NULL COMMENT '券后价（分）',
  `coupon_price` int DEFAULT NULL COMMENT '优惠券金额（分）',
  `coupon_start_time` datetime DEFAULT NULL COMMENT '券开始时间',
  `coupon_end_time` datetime DEFAULT NULL COMMENT '券结束时间',
  `commission_rate` decimal(10,4) DEFAULT NULL COMMENT '佣金率',
  `commission_amount` int DEFAULT NULL COMMENT '预估佣金（分）',
  `month_sales` bigint DEFAULT NULL COMMENT '近30天销量',
  `shop_name` varchar(255) DEFAULT NULL COMMENT '店铺',
  `activity_tag` varchar(255) DEFAULT NULL COMMENT '活动标签',
  `snapshot_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
  `raw_data` mediumtext COMMENT '第三方原始快照',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cps_goods_price_master` (`tenant_id`, `deleted`, `master_id`, `snapshot_time`) USING BTREE,
  KEY `idx_cps_goods_price_source` (`tenant_id`, `deleted`, `source_mapping_id`, `snapshot_time`) USING BTREE,
  KEY `idx_cps_goods_price_platform` (`tenant_id`, `deleted`, `platform_code`, `vendor_code`, `snapshot_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS商品价格快照表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_coupon_pool
-- ----------------------------
DROP TABLE IF EXISTS `cps_coupon_pool`;
CREATE TABLE `cps_coupon_pool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '券池ID',
  `master_id` bigint DEFAULT NULL COMMENT '商品主档ID',
  `source_mapping_id` bigint DEFAULT NULL COMMENT '商品来源映射ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(64) NOT NULL DEFAULT '' COMMENT '供应商编码',
  `external_goods_id` varchar(128) NOT NULL COMMENT '外部商品ID',
  `goods_sign` varchar(255) NOT NULL DEFAULT '' COMMENT 'goodsSign',
  `coupon_id` varchar(128) DEFAULT NULL COMMENT '第三方优惠券ID',
  `coupon_name` varchar(255) DEFAULT NULL COMMENT '优惠券名称',
  `coupon_amount` int DEFAULT NULL COMMENT '优惠金额（分）',
  `threshold_amount` int DEFAULT NULL COMMENT '使用门槛（分）',
  `start_time` datetime DEFAULT NULL COMMENT '有效期开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '有效期结束时间',
  `stock_total` int DEFAULT NULL COMMENT '总库存',
  `stock_remain` int DEFAULT NULL COMMENT '剩余库存，NULL表示未知或不限量',
  `status` varchar(32) NOT NULL DEFAULT 'VALID' COMMENT '状态：VALID/DISABLED/EXPIRED/OUT_OF_STOCK',
  `source_type` varchar(32) NOT NULL DEFAULT 'VENDOR_SYNC' COMMENT '来源类型',
  `activity_id` bigint DEFAULT NULL COMMENT '关联活动ID',
  `theme_id` bigint DEFAULT NULL COMMENT '关联选品主题ID',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `raw_data` mediumtext COMMENT '第三方原始券数据',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cps_coupon_goods` (`tenant_id`, `deleted`, `platform_code`, `vendor_code`, `external_goods_id`, `goods_sign`) USING BTREE,
  KEY `idx_cps_coupon_status_time` (`tenant_id`, `deleted`, `status`, `start_time`, `end_time`) USING BTREE,
  KEY `idx_cps_coupon_activity_theme` (`tenant_id`, `deleted`, `activity_id`, `theme_id`) USING BTREE,
  KEY `idx_cps_coupon_source_mapping` (`tenant_id`, `deleted`, `source_mapping_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS券池表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cps_marketing_short_link
-- ----------------------------
DROP TABLE IF EXISTS `cps_marketing_short_link`;
CREATE TABLE `cps_marketing_short_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '营销短链ID',
  `short_code` varchar(32) NOT NULL COMMENT '不可枚举短码',
  `target_url` varchar(1024) NOT NULL COMMENT '目标跳转链接',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(64) DEFAULT NULL COMMENT '供应商编码',
  `transfer_record_id` bigint DEFAULT NULL COMMENT '已有转链记录ID',
  `campaign_id` varchar(128) DEFAULT NULL COMMENT '营销活动ID',
  `creative_id` varchar(128) DEFAULT NULL COMMENT '素材ID',
  `channel_code` varchar(128) DEFAULT NULL COMMENT '渠道编码',
  `member_attribution_hash` varchar(64) DEFAULT NULL COMMENT '会员归因摘要，不存明文会员ID',
  `request_hash` varchar(64) NOT NULL COMMENT '幂等请求摘要',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0禁用 1启用）',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `access_count` bigint NOT NULL DEFAULT 0 COMMENT '访问次数',
  `last_access_time` datetime DEFAULT NULL COMMENT '最近访问时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_marketing_short_code` (`tenant_id`, `short_code`, `deleted`) USING BTREE,
  UNIQUE KEY `uk_cps_marketing_request_hash` (`tenant_id`, `request_hash`, `deleted`) USING BTREE,
  KEY `idx_cps_marketing_short_page` (`tenant_id`, `deleted`, `platform_code`, `vendor_code`, `campaign_id`, `channel_code`) USING BTREE,
  KEY `idx_cps_marketing_short_transfer` (`tenant_id`, `deleted`, `transfer_record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS营销短链表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 27. CPS营销点击事件表
-- ----------------------------
DROP TABLE IF EXISTS `cps_marketing_click_event`;
CREATE TABLE `cps_marketing_click_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '营销点击事件ID',
  `click_id` varchar(64) NOT NULL COMMENT '点击唯一ID',
  `short_code` varchar(32) NOT NULL COMMENT '短码',
  `short_link_id` bigint DEFAULT NULL COMMENT '短链ID',
  `campaign_id` varchar(128) DEFAULT NULL COMMENT '营销活动ID',
  `creative_id` varchar(128) DEFAULT NULL COMMENT '素材ID',
  `channel_code` varchar(128) DEFAULT NULL COMMENT '渠道编码',
  `member_attribution_hash` varchar(64) DEFAULT NULL COMMENT '归因摘要',
  `ip_hash` varchar(64) DEFAULT NULL COMMENT 'IP摘要',
  `user_agent_hash` varchar(64) DEFAULT NULL COMMENT 'User-Agent摘要',
  `device_hash` varchar(64) DEFAULT NULL COMMENT '设备摘要',
  `dedupe_key` varchar(64) NOT NULL COMMENT '去重摘要',
  `trusted_source` varchar(64) DEFAULT NULL COMMENT '可信来源',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0无效 1有效）',
  `click_time` datetime NOT NULL COMMENT '点击时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_marketing_click_id` (`tenant_id`, `click_id`, `deleted`) USING BTREE,
  UNIQUE KEY `uk_cps_marketing_click_dedupe` (`tenant_id`, `dedupe_key`, `deleted`) USING BTREE,
  KEY `idx_cps_marketing_click_short` (`tenant_id`, `deleted`, `short_code`, `short_link_id`) USING BTREE,
  KEY `idx_cps_marketing_click_funnel` (`tenant_id`, `deleted`, `campaign_id`, `creative_id`, `channel_code`, `click_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS营销点击事件表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- CPX core tables: task, offer, material, article, platform profile, tracking, event, conversion and settlement
-- CPS orders remain in cps_order; non-CPS traffic and rewards are recorded here.
-- ----------------------------
DROP TABLE IF EXISTS `cpx_task`;
CREATE TABLE `cpx_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务编号',
  `task_name` varchar(128) NOT NULL COMMENT '任务名称',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `promotion_method` varchar(16) NOT NULL DEFAULT 'CPS' COMMENT '推广方式：CPS/CPA/CPL/CPM/CPC/OCPA/OCPC/MIXED',
  `task_type` varchar(32) DEFAULT NULL COMMENT '任务类型',
  `offer_type` varchar(32) DEFAULT NULL COMMENT 'Offer类型',
  `title` varchar(255) DEFAULT NULL COMMENT '展示标题',
  `short_desc` varchar(512) DEFAULT NULL COMMENT '任务简介',
  `reward_desc` varchar(255) DEFAULT NULL COMMENT '奖励文案',
  `budget_amount` int DEFAULT NULL COMMENT '总预算，单位分',
  `daily_budget_amount` int DEFAULT NULL COMMENT '日预算，单位分',
  `reward_amount` int DEFAULT NULL COMMENT '固定奖励，单位分',
  `reward_rate` int DEFAULT NULL COMMENT '奖励比例，万分比',
  `member_reward_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否开启会员奖励；CPC/oCPC 默认关闭',
  `dedupe_window_seconds` int DEFAULT NULL COMMENT '去重窗口秒数',
  `frequency_limit` int DEFAULT NULL COMMENT '频控次数',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1上线 2下线',
  `priority` int NOT NULL DEFAULT 20 COMMENT '排序优先级；CPS 默认更高',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签',
  `material_json` text COMMENT '素材配置JSON',
  `rule_json` text COMMENT '审核/结算规则JSON',
  `landing_url` varchar(1024) DEFAULT NULL COMMENT '落地页',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_task_no` (`tenant_id`, `task_no`, `deleted`) USING BTREE,
  KEY `idx_cpx_task_hall` (`tenant_id`, `deleted`, `status`, `promotion_method`, `platform_code`, `priority`, `start_time`, `end_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX任务表';

DROP TABLE IF EXISTS `cpx_offer`;
CREATE TABLE `cpx_offer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'OfferID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `offer_code` varchar(64) NOT NULL COMMENT 'Offer编码',
  `offer_name` varchar(128) NOT NULL COMMENT 'Offer名称',
  `promotion_method` varchar(16) NOT NULL COMMENT '推广方式',
  `reward_amount` int DEFAULT NULL COMMENT '奖励金额，单位分',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `rule_json` text COMMENT '规则JSON',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_offer_code` (`tenant_id`, `offer_code`, `deleted`) USING BTREE,
  KEY `idx_cpx_offer_task` (`tenant_id`, `deleted`, `task_id`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX Offer表';

DROP TABLE IF EXISTS `cpx_material`;
CREATE TABLE `cpx_material` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '素材ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `offer_id` bigint DEFAULT NULL COMMENT 'OfferID',
  `material_type` varchar(32) NOT NULL COMMENT '素材类型',
  `title` varchar(255) DEFAULT NULL COMMENT '标题',
  `content` text COMMENT '内容',
  `image_url` varchar(1024) DEFAULT NULL COMMENT '图片',
  `landing_url` varchar(1024) DEFAULT NULL COMMENT '落地页',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cpx_material_task` (`tenant_id`, `deleted`, `task_id`, `offer_id`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX素材表';

DROP TABLE IF EXISTS `cpx_platform_profile`;
CREATE TABLE `cpx_platform_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '平台档案ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `platform_name` varchar(128) NOT NULL COMMENT '平台名称',
  `platform_logo` varchar(512) DEFAULT NULL COMMENT '平台Logo',
  `supported_methods` varchar(128) DEFAULT NULL COMMENT '支持计费模型',
  `api_base_url` varchar(512) DEFAULT NULL COMMENT '接口地址',
  `callback_url` varchar(512) DEFAULT NULL COMMENT '回调地址',
  `import_template` text COMMENT '导入模板说明',
  `health_status` varchar(32) DEFAULT 'UNKNOWN' COMMENT '健康状态',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `remark` varchar(512) DEFAULT NULL COMMENT '运营说明',
  `extra_config` text COMMENT '扩展配置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_platform_profile_code` (`tenant_id`, `platform_code`, `deleted`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX平台对接档案表';

DROP TABLE IF EXISTS `cpx_article`;
CREATE TABLE `cpx_article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资讯ID',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `category` varchar(32) DEFAULT NULL COMMENT '分类',
  `summary` varchar(512) DEFAULT NULL COMMENT '摘要',
  `cover_url` varchar(1024) DEFAULT NULL COMMENT '封面',
  `content` mediumtext COMMENT '正文',
  `platform_code` varchar(32) DEFAULT NULL COMMENT '关联平台',
  `promotion_method` varchar(16) DEFAULT NULL COMMENT '关联计费模型',
  `related_task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1发布 2下线',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cpx_article_list` (`tenant_id`, `deleted`, `status`, `category`, `promotion_method`, `publish_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX资讯表';

DROP TABLE IF EXISTS `cpx_tracking_link`;
CREATE TABLE `cpx_tracking_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '追踪链接ID',
  `tracking_id` varchar(64) NOT NULL COMMENT '追踪ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `offer_id` bigint DEFAULT NULL COMMENT 'OfferID',
  `material_id` bigint DEFAULT NULL COMMENT '素材ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `promotion_method` varchar(16) NOT NULL COMMENT '推广方式',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `adzone_id` varchar(128) DEFAULT NULL COMMENT '推广位',
  `channel_code` varchar(64) DEFAULT NULL COMMENT '渠道编码',
  `target_url` varchar(1024) DEFAULT NULL COMMENT '目标地址',
  `tracking_url` varchar(1024) DEFAULT NULL COMMENT '追踪地址',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_tracking_id` (`tenant_id`, `tracking_id`, `deleted`) USING BTREE,
  KEY `idx_cpx_tracking_member` (`tenant_id`, `deleted`, `member_id`, `task_id`, `promotion_method`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX追踪链接表';

DROP TABLE IF EXISTS `cpx_event`;
CREATE TABLE `cpx_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件ID',
  `event_id` varchar(64) NOT NULL COMMENT '事件编号',
  `tracking_id` varchar(64) DEFAULT NULL COMMENT '追踪ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `promotion_method` varchar(16) NOT NULL COMMENT '推广方式',
  `event_type` varchar(32) NOT NULL COMMENT '事件类型',
  `source_event_id` varchar(128) NOT NULL COMMENT '外部事件ID',
  `idempotency_key` varchar(255) NOT NULL COMMENT '幂等键',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(512) DEFAULT NULL COMMENT 'UA',
  `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
  `raw_payload` mediumtext COMMENT '原始载荷',
  `valid_flag` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否有效',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_event_idempotency` (`tenant_id`, `idempotency_key`, `deleted`) USING BTREE,
  KEY `idx_cpx_event_task` (`tenant_id`, `deleted`, `task_id`, `event_type`, `event_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX事件账本表';

DROP TABLE IF EXISTS `cpx_conversion`;
CREATE TABLE `cpx_conversion` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '转化ID',
  `conversion_no` varchar(64) NOT NULL COMMENT '转化编号',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `tracking_id` varchar(64) DEFAULT NULL COMMENT '追踪ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `promotion_method` varchar(16) NOT NULL COMMENT '推广方式',
  `source_event_id` varchar(128) NOT NULL COMMENT '来源事件ID',
  `target_event_type` varchar(32) DEFAULT NULL COMMENT '目标事件类型',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `amount` int DEFAULT NULL COMMENT '交易/成本金额，单位分',
  `reward_amount` int DEFAULT NULL COMMENT '奖励金额，单位分',
  `conversion_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '转化状态',
  `settlement_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态',
  `confirmed_time` datetime DEFAULT NULL COMMENT '确认时间',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_conversion_source` (`tenant_id`, `platform_code`, `promotion_method`, `source_event_id`, `deleted`) USING BTREE,
  KEY `idx_cpx_conversion_member` (`tenant_id`, `deleted`, `member_id`, `settlement_status`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX转化表';

DROP TABLE IF EXISTS `cpx_settlement_record`;
CREATE TABLE `cpx_settlement_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '结算记录ID',
  `settlement_no` varchar(64) NOT NULL COMMENT '结算编号',
  `conversion_id` bigint NOT NULL COMMENT '转化ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `promotion_method` varchar(16) NOT NULL COMMENT '推广方式',
  `member_id` bigint DEFAULT NULL COMMENT '会员ID',
  `amount` int DEFAULT NULL COMMENT '结算金额，单位分',
  `reward_amount` int DEFAULT NULL COMMENT '会员奖励，单位分',
  `settlement_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态：PENDING/FROZEN/AVAILABLE/DEDUCTED/REVERSED',
  `freeze_record_id` bigint DEFAULT NULL COMMENT '返利冻结记录ID',
  `rebate_record_id` bigint DEFAULT NULL COMMENT '返利记录ID',
  `idempotency_key` varchar(255) NOT NULL COMMENT '幂等键',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cpx_settlement_idem` (`tenant_id`, `idempotency_key`, `deleted`) USING BTREE,
  KEY `idx_cpx_settlement_member` (`tenant_id`, `deleted`, `member_id`, `settlement_status`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX结算记录表';

DROP TABLE IF EXISTS `cpx_lead_detail`;
CREATE TABLE `cpx_lead_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '线索详情ID',
  `conversion_id` bigint DEFAULT NULL COMMENT '转化ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `tracking_id` varchar(64) DEFAULT NULL COMMENT '追踪ID',
  `contact_hash` varchar(128) DEFAULT NULL COMMENT '联系方式摘要',
  `encrypted_contact` text COMMENT '加密联系方式',
  `consent_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '用户授权标识',
  `review_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
  `review_reason` varchar(512) DEFAULT NULL COMMENT '审核原因',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cpx_lead_task` (`tenant_id`, `deleted`, `task_id`, `review_status`, `create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPX线索详情表';
COMMIT;

-- ----------------------------
-- CPS menu records
-- ----------------------------
-- 菜单设计：保留一套 CPS 联盟菜单树，覆盖当前 admin-vue3 页面与后端 CPS 管理端权限。
BEGIN;
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
-- 一级菜单
(6200, '联盟运营', '', 1, 70, 0, '/cps-ops', 'ep:shopping-cart', NULL, NULL, 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-07-09 00:00:00', b'0'),
(6286, '联盟结算', '', 1, 71, 0, '/cps-settlement', 'ep:wallet', NULL, NULL, 0, b'1', b'1', b'1', '1', '2026-07-09 00:00:00', '1', '2026-07-09 00:00:00', b'0'),
(6287, '联盟配置', '', 1, 72, 0, '/cps-config', 'ep:setting', NULL, NULL, 0, b'1', b'1', b'1', '1', '2026-07-09 00:00:00', '1', '2026-07-09 00:00:00', b'0'),

-- 运营工作台
(6201, '活动中心', 'cps:rebate-activity:query', 2, 10, 6200, 'activity/square', 'ep:present', 'cps/activity/square/index', 'CpsRebateActivitySquare', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6202, '活动查询', 'cps:rebate-activity:query', 3, 1, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6203, '活动创建', 'cps:rebate-activity:create', 3, 2, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6204, '活动更新', 'cps:rebate-activity:update', 3, 3, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6205, '活动删除', 'cps:rebate-activity:delete', 3, 4, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6206, '返利工具箱', 'cps:toolbox:query', 2, 20, 6200, 'toolbox', 'ep:tools', 'cps/toolbox/index', 'CpsRebateToolbox', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6207, '工具箱查询', 'cps:toolbox:query', 3, 1, 6206, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6208, '工具箱转链', 'cps:toolbox:link', 3, 2, 6206, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6209, '选品库', 'cps:selection-theme:query', 2, 30, 6200, 'selection/theme', 'ep:collection', 'cps/selection/theme/index', 'CpsSelectionTheme', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6210, '选品库查询', 'cps:selection-theme:query', 3, 1, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6211, '选品库创建', 'cps:selection-theme:create', 3, 2, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6212, '选品库更新', 'cps:selection-theme:update', 3, 3, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6213, '选品库删除', 'cps:selection-theme:delete', 3, 4, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6214, '选品库发布', 'cps:selection-theme:publish', 3, 5, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6215, '选品库下线', 'cps:selection-theme:offline', 3, 6, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),

-- 商品与转链
(6218, '返利商品广场', 'cps:goods-square:query', 2, 40, 6200, 'goods/square', 'ep:goods', 'cps/goods/square/index', 'CpsGoodsSquare', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6219, '商品广场查询', 'cps:goods-square:query', 3, 1, 6218, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6220, '商品广场转链', 'cps:goods-square:link', 3, 2, 6218, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6221, '转链记录', 'cps:transfer-record:query', 2, 50, 6200, 'transfer', 'ep:connection', 'cps/transfer/index', 'CpsTransferRecord', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6222, '转链记录查询', 'cps:transfer-record:query', 3, 1, 6221, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),

-- 订单与返利
(6223, 'CPS订单', 'cps:order:query', 2, 10, 6286, 'order', 'ep:document', 'cps/order/index', 'CpsOrder', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6224, '订单查询', 'cps:order:query', 3, 1, 6223, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6225, '订单同步', 'cps:order:sync', 3, 2, 6223, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6296, '人工归因绑定', 'cps:order:attribution-bind', 3, 3, 6223, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-14 00:00:00', '1', '2026-07-14 00:00:00', b'0'),
(6284, '订单删除', 'cps:order:delete', 3, 4, 6223, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-06 00:00:00', '1', '2026-07-06 00:00:00', b'0'),
(6226, '返利记录', 'cps:rebate-record:query', 2, 20, 6286, 'rebate/record', 'ep:list', 'cps/rebate/record/index', 'CpsRebateRecord', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6227, '返利记录查询', 'cps:rebate-record:query', 3, 1, 6226, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6228, '返利退款回扣', 'cps:rebate-record:reverse', 3, 2, 6226, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6285, '返利记录删除', 'cps:rebate-record:delete', 3, 3, 6226, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-06 00:00:00', '1', '2026-07-06 00:00:00', b'0'),
(6229, '返利配置', 'cps:rebate-config:query', 2, 10, 6287, 'rebate/config', 'ep:setting', 'cps/rebate/config/index', 'CpsRebateConfig', 0, b'0', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6230, '返利配置查询', 'cps:rebate-config:query', 3, 1, 6229, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6231, '返利配置创建', 'cps:rebate-config:create', 3, 2, 6229, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6232, '返利配置更新', 'cps:rebate-config:update', 3, 3, 6229, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6233, '返利配置删除', 'cps:rebate-config:delete', 3, 4, 6229, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),

-- 资金与风控
(6234, '提现管理', 'cps:withdraw:query', 2, 30, 6286, 'withdraw', 'ep:wallet', 'cps/withdraw/index', 'CpsWithdraw', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6235, '提现查询', 'cps:withdraw:query', 3, 1, 6234, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6236, '提现审核', 'cps:withdraw:audit', 3, 2, 6234, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6237, '冻结管理', 'cps:freeze-config:query', 2, 40, 6286, 'freeze', 'ep:lock', 'cps/freeze/index', 'CpsFreeze', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6238, '冻结配置查询', 'cps:freeze-config:query', 3, 1, 6237, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6239, '冻结配置创建', 'cps:freeze-config:create', 3, 2, 6237, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6240, '冻结配置更新', 'cps:freeze-config:update', 3, 3, 6237, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6241, '冻结配置删除', 'cps:freeze-config:delete', 3, 4, 6237, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6242, '冻结记录查询', 'cps:freeze-record:query', 3, 5, 6237, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6243, '冻结记录手动解冻', 'cps:freeze-record:unfreeze', 3, 6, 6237, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6290, '资产安全中心', 'cps:rebate-debt:query', 2, 45, 6286, 'asset', 'ep:money', 'cps/asset/index', 'CpsAssetSafety', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'),
(6291, '返利欠款查询', 'cps:rebate-debt:query', 3, 1, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'),
(6292, '返利欠款调整', 'cps:rebate-debt:adjust', 3, 2, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'),
(6293, '资产流水查询', 'cps:rebate-asset-ledger:query', 3, 3, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'),
(6294, '资产策略查询', 'cps:rebate-asset-policy:query', 3, 4, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'),
(6295, '资产策略更新', 'cps:rebate-asset-policy:update', 3, 5, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'),
(6244, '风控规则', 'cps:risk-rule:query', 2, 50, 6286, 'risk', 'ep:warning', 'cps/risk/index', 'CpsRiskRule', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6245, '风控规则查询', 'cps:risk-rule:query', 3, 1, 6244, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6246, '风控规则创建', 'cps:risk-rule:create', 3, 2, 6244, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6247, '风控规则更新', 'cps:risk-rule:update', 3, 3, 6244, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6248, '风控规则删除', 'cps:risk-rule:delete', 3, 4, 6244, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6249, '数据统计', 'cps:statistics:query', 2, 60, 6286, 'statistics', 'ep:data-analysis', 'cps/statistics/index', 'CpsStatistics', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6250, '统计查询', 'cps:statistics:query', 3, 1, 6249, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),

-- 基础配置
(6251, '平台配置', 'cps:platform:query', 2, 20, 6287, 'platform', 'ep:platform', 'cps/platform/index', 'CpsPlatform', 0, b'0', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6252, '平台配置查询', 'cps:platform:query', 3, 1, 6251, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6253, '平台配置创建', 'cps:platform:create', 3, 2, 6251, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6254, '平台配置更新', 'cps:platform:update', 3, 3, 6251, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6255, '平台配置删除', 'cps:platform:delete', 3, 4, 6251, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6256, '推广位管理', 'cps:adzone:query', 2, 30, 6287, 'adzone', 'ep:link', 'cps/adzone/index', 'CpsAdzone', 0, b'0', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6257, '推广位查询', 'cps:adzone:query', 3, 1, 6256, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6258, '推广位创建', 'cps:adzone:create', 3, 2, 6256, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6259, '推广位更新', 'cps:adzone:update', 3, 3, 6256, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6260, '推广位删除', 'cps:adzone:delete', 3, 4, 6256, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6261, 'API供应商管理', 'cps:api-vendor:query', 2, 40, 6287, 'api-vendor', 'ep:connection', 'cps/apiVendor/index', 'CpsApiVendor', 0, b'0', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6262, '供应商查询', 'cps:api-vendor:query', 3, 1, 6261, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6263, '供应商创建', 'cps:api-vendor:create', 3, 2, 6261, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6264, '供应商更新', 'cps:api-vendor:update', 3, 3, 6261, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),
(6265, '供应商删除', 'cps:api-vendor:delete', 3, 4, 6261, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0'),

-- 平台配置中心（稳定 ID 不占用既有 6200-6296）
(6297, '平台配置中心', 'cps:platform-onboarding:query', 2, 10, 6287, 'platform-onboarding', 'ep:setting', 'cps/platformOnboarding/index', 'CpsPlatformOnboarding', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0'),
(6298, '平台配置中心查询', 'cps:platform-onboarding:query', 3, 1, 6297, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0'),
(6299, '平台配置中心创建', 'cps:platform-onboarding:create', 3, 2, 6297, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0'),
(6300, '平台配置中心更新', 'cps:platform-onboarding:update', 3, 3, 6297, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0'),
(6301, '平台配置中心删除', 'cps:platform-onboarding:delete', 3, 4, 6297, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0'),
(6302, '平台配置中心测试', 'cps:platform-onboarding:test', 3, 5, 6297, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0'),
(6303, '平台配置中心发布', 'cps:platform-onboarding:publish', 3, 6, 6297, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00', '1', '2026-07-24 00:00:00', b'0');
COMMIT;

-- CPX extension menus: CPS remains the primary business under the upgraded CPX alliance.
BEGIN;
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES
(6282, 'CPX看板', 'cpx:dashboard:query', 2, 60, 6200, 'cpx/dashboard', 'ep:data-analysis', 'cpx/dashboard/index', 'CpxDashboard', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6283, '看板查询', 'cpx:dashboard:query', 3, 1, 6282, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6270, '任务中心', 'cpx:task:query', 2, 70, 6200, 'cpx/task', 'ep:promotion', 'cpx/task/index', 'CpxTask', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6271, '任务查询', 'cpx:task:query', 3, 1, 6270, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6272, '任务创建', 'cpx:task:create', 3, 2, 6270, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6273, '任务更新', 'cpx:task:update', 3, 3, 6270, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6274, '资讯中心', 'cpx:article:query', 2, 80, 6200, 'cpx/article', 'ep:reading', 'cpx/article/index', 'CpxArticle', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6275, '资讯查询', 'cpx:article:query', 3, 1, 6274, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6276, '资讯创建', 'cpx:article:create', 3, 2, 6274, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6280, '资讯更新', 'cpx:article:update', 3, 3, 6274, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6277, '平台对接中心', 'cpx:platform:query', 2, 50, 6287, 'cpx/platform-profile', 'ep:connection', 'cpx/platformProfile/index', 'CpxPlatformProfile', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6278, '平台档案查询', 'cpx:platform:query', 3, 1, 6277, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6279, '平台档案创建', 'cpx:platform:create', 3, 2, 6277, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0'),
(6281, '平台档案更新', 'cpx:platform:update', 3, 3, 6277, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-26 00:00:00', '1', '2026-05-26 00:00:00', b'0');
COMMIT;

-- ----------------------------
-- CPS menu authorization alignment
-- ----------------------------
BEGIN;
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, 6286, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_role_menu` rm
WHERE rm.`deleted` = b'0'
  AND rm.`menu_id` IN (6223, 6226, 6234, 6237, 6244, 6249)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` existing_rm
    WHERE existing_rm.`role_id` = rm.`role_id`
      AND existing_rm.`menu_id` = 6286
      AND existing_rm.`tenant_id` = rm.`tenant_id`
      AND existing_rm.`deleted` = b'0'
  );

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, 6287, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_role_menu` rm
WHERE rm.`deleted` = b'0'
  AND rm.`menu_id` IN (6229, 6251, 6256, 6261, 6277)
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` existing_rm
    WHERE existing_rm.`role_id` = rm.`role_id`
      AND existing_rm.`menu_id` = 6287
      AND existing_rm.`tenant_id` = rm.`tenant_id`
      AND existing_rm.`deleted` = b'0'
  );

-- Preserve the four original configuration permission boundaries when granting
-- the unified page. Query/test require all four legacy pages; mutations require
-- the corresponding action in all four domains; publish requires create+update.
INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT eligible.`role_id`, eligible.`target_menu_id`, '1', NOW(), '1', NOW(), b'0', eligible.`tenant_id`
FROM (
  SELECT rm.`role_id`, rm.`tenant_id`, mapping.`target_menu_id`
  FROM `system_role_menu` rm
  JOIN (
    SELECT 6297 AS `target_menu_id`, 6229 AS `source_menu_id`, 4 AS `required_count`
    UNION ALL SELECT 6297, 6251, 4
    UNION ALL SELECT 6297, 6256, 4
    UNION ALL SELECT 6297, 6261, 4
    UNION ALL SELECT 6298 AS `target_menu_id`, 6229 AS `source_menu_id`, 4 AS `required_count`
    UNION ALL SELECT 6298, 6251, 4
    UNION ALL SELECT 6298, 6256, 4
    UNION ALL SELECT 6298, 6261, 4
    UNION ALL SELECT 6299 AS `target_menu_id`, 6231 AS `source_menu_id`, 4 AS `required_count`
    UNION ALL SELECT 6299, 6253, 4
    UNION ALL SELECT 6299, 6258, 4
    UNION ALL SELECT 6299, 6263, 4
    UNION ALL SELECT 6300 AS `target_menu_id`, 6232 AS `source_menu_id`, 4 AS `required_count`
    UNION ALL SELECT 6300, 6254, 4
    UNION ALL SELECT 6300, 6259, 4
    UNION ALL SELECT 6300, 6264, 4
    UNION ALL SELECT 6301 AS `target_menu_id`, 6233 AS `source_menu_id`, 4 AS `required_count`
    UNION ALL SELECT 6301, 6255, 4
    UNION ALL SELECT 6301, 6260, 4
    UNION ALL SELECT 6301, 6265, 4
    UNION ALL SELECT 6302 AS `target_menu_id`, 6229 AS `source_menu_id`, 4 AS `required_count`
    UNION ALL SELECT 6302, 6251, 4
    UNION ALL SELECT 6302, 6256, 4
    UNION ALL SELECT 6302, 6261, 4
    UNION ALL SELECT 6303 AS `target_menu_id`, 6231 AS `source_menu_id`, 8 AS `required_count`
    UNION ALL SELECT 6303, 6253, 8
    UNION ALL SELECT 6303, 6258, 8
    UNION ALL SELECT 6303, 6263, 8
    UNION ALL SELECT 6303, 6232, 8
    UNION ALL SELECT 6303, 6254, 8
    UNION ALL SELECT 6303, 6259, 8
    UNION ALL SELECT 6303, 6264, 8
  ) mapping ON mapping.`source_menu_id` = rm.`menu_id`
  WHERE rm.`deleted` = b'0'
  GROUP BY rm.`role_id`, rm.`tenant_id`, mapping.`target_menu_id`, mapping.`required_count`
  HAVING COUNT(DISTINCT rm.`menu_id`) = mapping.`required_count`
) eligible
WHERE NOT EXISTS (
  SELECT 1 FROM `system_role_menu` existing_rm
  WHERE existing_rm.`role_id` = eligible.`role_id`
    AND existing_rm.`menu_id` = eligible.`target_menu_id`
    AND existing_rm.`tenant_id` = eligible.`tenant_id`
    AND existing_rm.`deleted` = b'0'
);

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT rm.`role_id`, 6296, '1', NOW(), '1', NOW(), b'0', rm.`tenant_id`
FROM `system_role_menu` rm
WHERE rm.`deleted` = b'0'
  AND rm.`menu_id` = 6225
  AND NOT EXISTS (
    SELECT 1 FROM `system_role_menu` existing_rm
    WHERE existing_rm.`role_id` = rm.`role_id`
      AND existing_rm.`menu_id` = 6296
      AND existing_rm.`tenant_id` = rm.`tenant_id`
      AND existing_rm.`deleted` = b'0'
  );

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6286),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '6200')
  AND NOT JSON_CONTAINS(`menu_ids`, '6286');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6287),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '6200')
  AND NOT JSON_CONTAINS(`menu_ids`, '6287');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6296),
    `updater` = '1',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '6225')
  AND NOT JSON_CONTAINS(`menu_ids`, '6296');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6297),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6229, 6251, 6256, 6261]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6297');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6298),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6229, 6251, 6256, 6261]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6298');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6299),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6231, 6253, 6258, 6263]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6299');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6300),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6232, 6254, 6259, 6264]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6300');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6301),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6233, 6255, 6260, 6265]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6301');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6302),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6229, 6251, 6256, 6261]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6302');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6303),
    `updater` = '1', `update_time` = NOW()
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6231, 6253, 6258, 6263, 6232, 6254, 6259, 6264]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6303');
COMMIT;

INSERT INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
                         `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`, `deleted`)
SELECT 'Jutuike order sync', 2, 'cpsJutuikeOrderSyncJob', '{"hours":2,"queryType":4}',
       '0 10 * * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'cpsJutuikeOrderSyncJob' AND `deleted` = b'0'
);

INSERT INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
                         `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`, `deleted`)
SELECT 'AI saved filter refresh', 2, 'cpsAiSavedFilterRefreshJob', NULL,
       '0 */30 * * * ?', 1, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'cpsAiSavedFilterRefreshJob' AND `deleted` = b'0'
);

SET FOREIGN_KEY_CHECKS = 1;
