-- ============================================================
-- CPS联盟返利系统 - 现有数据库增量更新脚本
-- Description:
--   1. 本文件只保存现有数据库升级 SQL。
--   2. 新库全量建库脚本维护在 backend/sql/module/cps-all-in-one.sql。
--   3. 每段更新必须保留修改时间记录，便于排查和同步。
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 修改时间：2026-07-04 00:00:00
-- 目的：扩大 CPS 活动跳转地址字段，适配第三方活动长链接。
-- ============================================================
ALTER TABLE `cps_rebate_activity`
  MODIFY COLUMN `jump_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '跳转地址';

-- ============================================================
-- 修改时间：2026-07-06 00:00:00
-- 目的：补充 CPS 订单删除权限。
-- ============================================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6284, '订单删除', 'cps:order:delete', 3, 3, 6223, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-06 00:00:00', '1', '2026-07-06 00:00:00', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 6284 OR `permission` = 'cps:order:delete'
);

-- ============================================================
-- 修改时间：2026-07-06 00:00:00
-- 目的：补充 CPS 返利记录删除权限。
-- ============================================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6285, '返利记录删除', 'cps:rebate-record:delete', 3, 3, 6226, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-06 00:00:00', '1', '2026-07-06 00:00:00', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` WHERE `id` = 6285 OR `permission` = 'cps:rebate-record:delete'
);

-- ============================================================
-- 修改时间：2026-07-06 00:00:00
-- 目的：移除独立返利查询菜单，返利查询统一进入返利工具箱。
-- ============================================================
BEGIN;
DELETE FROM `system_role_menu` WHERE `menu_id` IN (6216, 6217);
DELETE FROM `system_menu` WHERE `id` IN (6216, 6217);
COMMIT;

-- ============================================================
-- 修改时间：2026-07-07 00:00:00
-- 目的：补充大淘客淘宝高效转链归因字段。
-- ============================================================
ALTER TABLE `cps_adzone`
  ADD COLUMN `external_relation_id` varchar(128) DEFAULT NULL COMMENT '淘宝联盟渠道关系ID（orderScene=2）' AFTER `relation_id`,
  ADD COLUMN `external_special_id` varchar(128) DEFAULT NULL COMMENT '淘宝联盟会员运营ID（orderScene=3）' AFTER `external_relation_id`,
  ADD KEY `idx_external_relation_id` (`platform_code`, `external_relation_id`) USING BTREE,
  ADD KEY `idx_external_special_id` (`platform_code`, `external_special_id`) USING BTREE;

ALTER TABLE `cps_order`
  ADD COLUMN `special_id` varchar(128) DEFAULT NULL COMMENT '淘宝会员运营ID' AFTER `external_info`,
  ADD COLUMN `relation_id` varchar(128) DEFAULT NULL COMMENT '淘宝渠道关系ID' AFTER `special_id`,
  ADD COLUMN `order_scene` tinyint DEFAULT NULL COMMENT '淘宝订单场景（1常规 2渠道 3会员运营）' AFTER `relation_id`,
  ADD COLUMN `attribution_source` varchar(32) DEFAULT NULL COMMENT '会员归因来源' AFTER `order_scene`,
  ADD KEY `idx_special_id` (`platform_code`, `special_id`) USING BTREE,
  ADD KEY `idx_relation_id` (`platform_code`, `relation_id`) USING BTREE;

-- ============================================================
-- 修改时间：2026-07-07 00:00:00
-- 目的：增加选品主题是否展示到返利商品广场的开关。
-- ============================================================
ALTER TABLE `cps_selection_theme`
  ADD COLUMN `goods_square_visible` tinyint NOT NULL DEFAULT 1 COMMENT '是否展示到返利商品广场：0否 1是' AFTER `status`;

DROP INDEX `idx_cps_selection_theme_page` ON `cps_selection_theme`;
CREATE INDEX `idx_cps_selection_theme_page`
  ON `cps_selection_theme` (`tenant_id`, `deleted`, `status`, `goods_square_visible`, `promotion_event`, `sort`) USING BTREE;

-- ============================================================
-- 修改时间：2026-07-09 00:00:00
-- 目的：拆分联盟菜单为运营、结算、配置三个顶级分组，并同步角色与租户套餐菜单。
-- ============================================================
BEGIN;

UPDATE `system_menu`
SET `name` = '联盟运营',
    `type` = 1,
    `sort` = 70,
    `parent_id` = 0,
    `path` = '/cps-ops',
    `icon` = 'ep:shopping-cart',
    `component` = NULL,
    `component_name` = NULL,
    `updater` = '1',
    `update_time` = '2026-07-09 00:00:00',
    `deleted` = b'0'
WHERE `id` = 6200;

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6286, '联盟结算', '', 1, 71, 0, '/cps-settlement', 'ep:wallet', NULL, NULL, 0, b'1', b'1', b'1', '1', '2026-07-09 00:00:00', '1', '2026-07-09 00:00:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6286);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6287, '联盟配置', '', 1, 72, 0, '/cps-config', 'ep:setting', NULL, NULL, 0, b'1', b'1', b'1', '1', '2026-07-09 00:00:00', '1', '2026-07-09 00:00:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6287);

UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 10, `path` = 'activity/square', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6201;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 20, `path` = 'toolbox', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6206;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 30, `path` = 'selection/theme', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6209;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 40, `path` = 'goods/square', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6218;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 50, `path` = 'transfer', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6221;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 60, `path` = 'cpx/dashboard', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6282;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 70, `path` = 'cpx/task', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6270;
UPDATE `system_menu` SET `parent_id` = 6200, `sort` = 80, `path` = 'cpx/article', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6274;

UPDATE `system_menu` SET `parent_id` = 6286, `sort` = 10, `path` = 'order', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6223;
UPDATE `system_menu` SET `parent_id` = 6286, `sort` = 20, `path` = 'rebate/record', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6226;
UPDATE `system_menu` SET `parent_id` = 6286, `sort` = 30, `path` = 'withdraw', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6234;
UPDATE `system_menu` SET `parent_id` = 6286, `sort` = 40, `path` = 'freeze', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6237;
UPDATE `system_menu` SET `parent_id` = 6286, `sort` = 50, `path` = 'risk', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6244;
UPDATE `system_menu` SET `parent_id` = 6286, `sort` = 60, `path` = 'statistics', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6249;

UPDATE `system_menu` SET `parent_id` = 6287, `sort` = 10, `path` = 'rebate/config', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6229;
UPDATE `system_menu` SET `parent_id` = 6287, `sort` = 20, `path` = 'platform', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6251;
UPDATE `system_menu` SET `parent_id` = 6287, `sort` = 30, `path` = 'adzone', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6256;
UPDATE `system_menu` SET `parent_id` = 6287, `sort` = 40, `path` = 'api-vendor', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6261;
UPDATE `system_menu` SET `parent_id` = 6287, `sort` = 50, `path` = 'cpx/platform-profile', `updater` = '1', `update_time` = '2026-07-09 00:00:00' WHERE `id` = 6277;

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

COMMIT;

-- ============================================================
-- 修改时间：2026-07-10 00:00:00
-- 目的：允许平台基础记录不重复保存供应商密钥和默认推广位，适配滴滴联盟等供应商配置。
-- 说明：MODIFY COLUMN 重复执行结果一致，不写入固定租户或平台种子数据。
-- ============================================================
ALTER TABLE `cps_platform`
  MODIFY COLUMN `app_key` varchar(255) NULL DEFAULT NULL COMMENT 'AppKey',
  MODIFY COLUMN `app_secret` varchar(255) NULL DEFAULT NULL COMMENT 'AppSecret（加密存储）',
  MODIFY COLUMN `default_adzone_id` varchar(128) NULL DEFAULT NULL COMMENT '默认推广位ID';

-- ============================================================
-- 修改时间：2026-07-10 16:00:00
-- 目的：补充 MCP 调用的会员身份、对话、客户端与链路追踪审计字段。
-- ============================================================
ALTER TABLE `cps_mcp_access_log`
  ADD COLUMN `member_id` bigint NULL DEFAULT NULL COMMENT '会员编号' AFTER `api_key_id`,
  ADD COLUMN `actor_user_id` bigint NULL DEFAULT NULL COMMENT '实际调用用户编号' AFTER `member_id`,
  ADD COLUMN `actor_user_type` varchar(16) NULL DEFAULT NULL COMMENT '实际调用用户类型（ADMIN/MEMBER）' AFTER `actor_user_id`,
  ADD COLUMN `conversation_id` bigint NULL DEFAULT NULL COMMENT 'AI对话编号' AFTER `actor_user_type`,
  ADD COLUMN `mcp_client_name` varchar(128) NULL DEFAULT NULL COMMENT 'MCP Client名称' AFTER `conversation_id`,
  ADD COLUMN `invocation_source` varchar(32) NULL DEFAULT NULL COMMENT '调用来源' AFTER `mcp_client_name`,
  ADD COLUMN `trace_id` varchar(64) NULL DEFAULT NULL COMMENT '链路追踪编号' AFTER `invocation_source`;

-- ============================================================
-- 修改时间：2026-07-13 00:00:00
-- 目的：阶段一资金与归因安全基线，发布A兼容结构。
-- 发布约束：本段只增加兼容字段、新表和普通索引；默认不启用V2资产写入。
-- ============================================================
ALTER TABLE `cps_order`
  ADD COLUMN `last_sync_result` varchar(32) NULL DEFAULT NULL COMMENT '最后同步结果（SUCCESS/PARTIAL/FAILED）' AFTER `last_sync_error`,
  ADD COLUMN `raw_platform_status_summary` varchar(512) NULL DEFAULT NULL COMMENT '原始平台状态摘要' AFTER `last_sync_result`,
  ADD COLUMN `status_version` int NOT NULL DEFAULT 0 COMMENT '订单状态变更版本号' AFTER `raw_platform_status_summary`,
  ADD COLUMN `rebate_settle_retry_count` int NOT NULL DEFAULT 0 COMMENT '返利结算重试次数' AFTER `status_version`,
  ADD COLUMN `rebate_settle_next_retry_time` datetime NULL DEFAULT NULL COMMENT '下次返利结算重试时间' AFTER `rebate_settle_retry_count`,
  ADD COLUMN `rebate_settle_last_error` varchar(512) NULL DEFAULT NULL COMMENT '最近返利结算待处理或失败原因' AFTER `rebate_settle_next_retry_time`,
  MODIFY COLUMN `rebate_freeze_status` varchar(16) NULL DEFAULT NULL COMMENT '返利冻结状态（NULL/pending:待冻结 frozen:已冻结 unfreezing:解冻中 unfreezed:已解冻）',
  ADD KEY `idx_tenant_platform_order_precheck` (`tenant_id`, `platform_code`, `platform_order_id`) USING BTREE,
  ADD KEY `idx_order_rebate_settle_retry` (`tenant_id`, `order_status`, `rebate_settle_next_retry_time`, `create_time`, `id`) USING BTREE;

ALTER TABLE `cps_rebate_account`
  ADD COLUMN `debt_balance` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '欠款余额' AFTER `frozen_balance`,
  ADD KEY `idx_tenant_member_precheck` (`tenant_id`, `member_id`) USING BTREE;

ALTER TABLE `cps_rebate_config`
  ADD COLUMN `member_id` bigint NULL DEFAULT NULL COMMENT '会员ID（NULL表示无会员限制）' AFTER `id`,
  ADD KEY `idx_member_id` (`member_id`) USING BTREE;

ALTER TABLE `cps_rebate_record`
  ADD COLUMN `rebate_amount_cent` bigint NULL DEFAULT NULL COMMENT '返利金额（分，V2优先读取）' AFTER `rebate_amount`,
  ADD COLUMN `rebate_config_id` bigint NULL DEFAULT NULL COMMENT '结算时匹配的返利配置ID快照' AFTER `rebate_amount_cent`,
  ADD COLUMN `member_level_id_snapshot` bigint NULL DEFAULT NULL COMMENT '结算时会员等级ID快照' AFTER `rebate_config_id`,
  ADD COLUMN `idempotency_key` varchar(128) NULL DEFAULT NULL COMMENT '资金操作幂等键' AFTER `member_level_id_snapshot`,
  ADD KEY `idx_tenant_order_rebate_precheck` (`tenant_id`, `order_id`, `rebate_type`) USING BTREE;

-- 修改时间：2026-07-14 15:40:00
-- 目的：P2-S2-04 追加不可变订单状态事件和供应商原始状态摘要。
CREATE TABLE IF NOT EXISTS `cps_order_status_event` (
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

-- 修改时间：2026-07-14 15:58:00
-- 目的：P2-S2-05 建设订单同步失败恢复队列、重试/死信状态和人工重放审计。
CREATE TABLE IF NOT EXISTS `cps_order_sync_failure` (
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

-- 修改时间：2026-07-14 16:15:00
-- 目的：P2-S2-06 建设平台账单导入行、对账差异单、人工处理结论和重拉请求审计。
CREATE TABLE IF NOT EXISTS `cps_platform_bill_row` (
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

CREATE TABLE IF NOT EXISTS `cps_platform_bill_diff` (
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

ALTER TABLE `cps_freeze_config`
  ADD COLUMN `min_amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '返利金额下限（分，包含）' AFTER `platform_code`,
  ADD COLUMN `max_amount_cent` bigint NULL DEFAULT NULL COMMENT '返利金额上限（分，不包含；NULL表示无上限）' AFTER `min_amount_cent`,
  ADD KEY `idx_tenant_platform_amount` (`tenant_id`, `platform_code`, `status`, `min_amount_cent`, `max_amount_cent`) USING BTREE;

ALTER TABLE `cps_freeze_record`
  ADD COLUMN `amount_cent` bigint NULL DEFAULT NULL COMMENT '冻结金额（分，V2优先读取；历史异常值留空待核对）' AFTER `freeze_amount`,
  ADD COLUMN `freeze_config_id` bigint NULL DEFAULT NULL COMMENT '匹配的冻结配置ID快照' AFTER `amount_cent`,
  ADD COLUMN `freeze_days_snapshot` int NULL DEFAULT NULL COMMENT '冻结天数快照' AFTER `freeze_config_id`,
  ADD COLUMN `eligible_time` datetime NULL DEFAULT NULL COMMENT '冻结资格时间（收货与平台结算时间取晚）' AFTER `freeze_days_snapshot`,
  ADD COLUMN `manual_unfreeze_reason` varchar(512) NULL DEFAULT NULL COMMENT '管理员手动解冻原因' AFTER `status`,
  ADD COLUMN `manual_unfreeze_operator_id` bigint NULL DEFAULT NULL COMMENT '管理员手动解冻操作人ID' AFTER `manual_unfreeze_reason`,
  ADD KEY `idx_tenant_business_idempotency_precheck` (`tenant_id`, `business_type`, `idempotency_key`) USING BTREE;

-- 仅回填可确认的正金额历史冻结记录；NULL/零/负数必须在启用V2前人工核对，禁止静默按0处理。
UPDATE `cps_freeze_record`
SET `amount_cent` = ROUND(`freeze_amount` * 100)
WHERE `amount_cent` IS NULL AND `freeze_amount` IS NOT NULL AND `freeze_amount` > 0;

INSERT INTO `cps_freeze_config`
  (`platform_code`, `min_amount_cent`, `max_amount_cent`, `unfreeze_days`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT NULL, 0, NULL, 15, 1, '全平台全金额默认配置-资格时间后15天解冻', '1', NOW(), '1', NOW(), b'0', 0
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_freeze_config`
  WHERE `tenant_id` = 0 AND `deleted` = b'0' AND `platform_code` IS NULL
    AND `min_amount_cent` = 0 AND `max_amount_cent` IS NULL
);

CREATE TABLE IF NOT EXISTS `cps_rebate_asset_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `source_system` varchar(32) NOT NULL COMMENT '来源系统',
  `business_type` varchar(32) NOT NULL COMMENT '业务类型',
  `business_id` varchar(128) NOT NULL COMMENT '业务单号',
  `order_id` bigint DEFAULT NULL COMMENT 'CPS订单ID',
  `platform_order_id` varchar(128) DEFAULT NULL COMMENT '平台订单号',
  `idempotency_key` varchar(128) NOT NULL COMMENT '幂等键',
  `available_change_cent` bigint NOT NULL DEFAULT 0 COMMENT '可用余额变更（分）',
  `frozen_change_cent` bigint NOT NULL DEFAULT 0 COMMENT '冻结余额变更（分）',
  `debt_change_cent` bigint NOT NULL DEFAULT 0 COMMENT '欠款余额变更（分）',
  `available_before_cent` bigint NOT NULL COMMENT '变更前可用余额（分）',
  `available_after_cent` bigint NOT NULL COMMENT '变更后可用余额（分）',
  `frozen_before_cent` bigint NOT NULL COMMENT '变更前冻结余额（分）',
  `frozen_after_cent` bigint NOT NULL COMMENT '变更后冻结余额（分）',
  `debt_before_cent` bigint NOT NULL COMMENT '变更前欠款余额（分）',
  `debt_after_cent` bigint NOT NULL COMMENT '变更后欠款余额（分）',
  `operator_type` varchar(32) NOT NULL COMMENT '操作主体类型',
  `operator_id` varchar(128) DEFAULT NULL COMMENT '操作主体ID',
  `reason` varchar(512) NOT NULL COMMENT '资金变更原因',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '创建后不得更新，仅保留BaseDO兼容字段',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建后不得更新，仅保留BaseDO兼容字段',
  `deleted` bit(1) DEFAULT b'0' COMMENT '固定为未删除；资产流水只允许追加',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_ledger_idempotency` (`tenant_id`, `business_type`, `idempotency_key`) USING BTREE,
  KEY `idx_asset_ledger_member_time` (`tenant_id`, `member_id`, `create_time`) USING BTREE,
  KEY `idx_asset_ledger_order` (`tenant_id`, `order_id`) USING BTREE,
  KEY `idx_asset_ledger_business` (`tenant_id`, `business_type`, `business_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利资产不可变流水表（只允许INSERT，禁止UPDATE/DELETE）';

CREATE TABLE IF NOT EXISTS `cps_rebate_asset_migration_check` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `batch_no` varchar(64) NOT NULL COMMENT '预检批次号',
  `tenant_id` bigint NOT NULL COMMENT '租户编号',
  `duplicate_account_count` bigint NOT NULL DEFAULT 0 COMMENT '重复账户组数',
  `duplicate_order_count` bigint NOT NULL DEFAULT 0 COMMENT '重复订单组数',
  `duplicate_rebate_record_count` bigint NOT NULL DEFAULT 0 COMMENT '重复返利主记录组数',
  `duplicate_ledger_idempotency_count` bigint NOT NULL DEFAULT 0 COMMENT '重复资产幂等键组数',
  `duplicate_freeze_idempotency_count` bigint NOT NULL DEFAULT 0 COMMENT '重复冻结幂等键组数',
  `account_ledger_mismatch_count` bigint NOT NULL DEFAULT 0 COMMENT '账户净资产与流水不一致账户数',
  `freeze_account_mismatch_count` bigint NOT NULL DEFAULT 0 COMMENT '冻结记录与账户冻结余额不一致账户数',
  `missing_opening_balance_count` bigint NOT NULL DEFAULT 0 COMMENT '缺失期初流水账户数',
  `orphan_ledger_count` bigint NOT NULL DEFAULT 0 COMMENT '找不到同租户账户的资产流水数',
  `orphan_active_freeze_count` bigint NOT NULL DEFAULT 0 COMMENT '找不到同租户账户的有效冻结记录数',
  `ready` tinyint NOT NULL DEFAULT 0 COMMENT '是否允许进入发布B审批',
  `operator_id` varchar(64) NOT NULL COMMENT '执行人',
  `executed_at` datetime NOT NULL COMMENT '执行时间',
  `summary` varchar(512) NOT NULL COMMENT '检查摘要',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_migration_check_tenant_batch` (`tenant_id`, `batch_no`) USING BTREE,
  KEY `idx_migration_check_tenant_time` (`tenant_id`, `executed_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利资产V2迁移预检不可变归档（只允许INSERT）';

CREATE TABLE IF NOT EXISTS `cps_rebate_debt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id` bigint NOT NULL COMMENT '会员ID',
  `order_id` bigint DEFAULT NULL COMMENT '来源订单ID',
  `platform_order_id` varchar(128) DEFAULT NULL COMMENT '来源平台订单号',
  `source_business_id` varchar(128) NOT NULL COMMENT '来源退款或调整业务单号',
  `idempotency_key` varchar(128) NOT NULL COMMENT '欠款操作幂等键',
  `original_debt_cent` bigint NOT NULL COMMENT '原始欠款（分）',
  `repaid_debt_cent` bigint NOT NULL DEFAULT 0 COMMENT '已偿还欠款（分）',
  `waived_debt_cent` bigint NOT NULL DEFAULT 0 COMMENT '已减免欠款（分）',
  `outstanding_debt_cent` bigint NOT NULL COMMENT '未偿还欠款（分）',
  `status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT '状态（OPEN/PARTIAL/CLEARED/WAIVED）',
  `last_reminder_time` datetime DEFAULT NULL COMMENT '最近站内提醒时间',
  `next_reminder_time` datetime DEFAULT NULL COMMENT '下次站内提醒时间',
  `reminder_end_time` datetime DEFAULT NULL COMMENT '提醒截止时间',
  `last_sms_time` datetime DEFAULT NULL COMMENT '最近短信提醒时间',
  `reminder_count` int NOT NULL DEFAULT 0 COMMENT '站内提醒次数',
  `sms_count` int NOT NULL DEFAULT 0 COMMENT '短信提醒次数',
  `notification_status` varchar(16) DEFAULT NULL COMMENT '通知状态',
  `notification_failure_reason` varchar(512) DEFAULT NULL COMMENT '通知失败原因',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_debt_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  KEY `idx_debt_source` (`tenant_id`, `source_business_id`) USING BTREE,
  KEY `idx_debt_member_status` (`tenant_id`, `member_id`, `status`, `create_time`) USING BTREE,
  KEY `idx_debt_reminder` (`tenant_id`, `status`, `next_reminder_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返利欠款账表';

CREATE TABLE IF NOT EXISTS `cps_order_attribution_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` bigint DEFAULT NULL COMMENT 'CPS订单ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `platform_order_id` varchar(128) NOT NULL COMMENT '平台订单号',
  `candidate_member_id` bigint DEFAULT NULL COMMENT '候选会员ID',
  `attributed_member_id` bigint DEFAULT NULL COMMENT '最终归因会员ID',
  `attribution_source` varchar(32) DEFAULT NULL COMMENT '归因来源',
  `binding_type` varchar(32) DEFAULT NULL COMMENT '可信绑定类型',
  `binding_id` varchar(128) DEFAULT NULL COMMENT '可信绑定标识',
  `action` varchar(32) NOT NULL COMMENT '动作（AUTO/MANUAL/REBIND）',
  `result` varchar(16) NOT NULL COMMENT '结果（BOUND/REJECTED/CONFLICT/UNATTRIBUTED）',
  `reject_reason` varchar(512) DEFAULT NULL COMMENT '拒绝或冲突原因',
  `operator_type` varchar(32) NOT NULL COMMENT '操作主体类型',
  `operator_id` varchar(128) DEFAULT NULL COMMENT '操作主体ID',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_attribution_order` (`tenant_id`, `platform_code`, `platform_order_id`, `create_time`) USING BTREE,
  KEY `idx_attribution_member` (`tenant_id`, `attributed_member_id`, `create_time`) USING BTREE,
  KEY `idx_attribution_result` (`tenant_id`, `result`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单归因审计日志表';

CREATE TABLE IF NOT EXISTS `cps_order_sync_checkpoint` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(32) NOT NULL DEFAULT 'OFFICIAL' COMMENT '供应商编码',
  `order_scene` tinyint NOT NULL DEFAULT 1 COMMENT '订单场景',
  `query_type` varchar(32) NOT NULL COMMENT '查询类型',
  `pagination_mode` varchar(16) DEFAULT NULL COMMENT '分页模式（PAGE/CURSOR）',
  `next_cursor` varchar(512) DEFAULT NULL COMMENT '下一页游标',
  `next_page_no` int DEFAULT NULL COMMENT '下一页页码',
  `watermark_time` datetime DEFAULT NULL COMMENT '最近成功水位时间',
  `query_end_time` datetime DEFAULT NULL COMMENT '当前分页窗口固定结束时间（完成前不得漂移）',
  `last_sync_status` varchar(16) DEFAULT NULL COMMENT '最近同步状态',
  `last_success_count` int NOT NULL DEFAULT 0 COMMENT '最近成功条数',
  `last_failure_count` int NOT NULL DEFAULT 0 COMMENT '最近失败条数',
  `failure_summary` text COMMENT '失败订单摘要',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_checkpoint` (`tenant_id`, `platform_code`, `vendor_code`, `order_scene`, `query_type`) USING BTREE,
  KEY `idx_sync_checkpoint_status` (`tenant_id`, `last_sync_status`, `update_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步成功水位表';

CREATE TABLE IF NOT EXISTS `cps_rebate_asset_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `v2_enabled` tinyint NOT NULL DEFAULT 0 COMMENT '是否启用V2资产写入',
  `migration_ready` tinyint NOT NULL DEFAULT 0 COMMENT '发布B唯一键、期初流水与冻结对账是否已核验',
  `latest_ready_check_batch_no` varchar(64) DEFAULT NULL COMMENT '最近一次通过并获发布B批准的预检批次',
  `ready_check_time` datetime DEFAULT NULL COMMENT '上述预检批次执行时间',
  `migration_approval_ref` varchar(128) DEFAULT NULL COMMENT '发布B变更单/审批单号',
  `read_only` tinyint NOT NULL DEFAULT 0 COMMENT '资产操作只读开关',
  `large_debt_threshold_cent` bigint NOT NULL DEFAULT 10000 COMMENT '大额欠款阈值（分）',
  `reminder_interval_days` int NOT NULL DEFAULT 7 COMMENT '普通站内提醒间隔天数',
  `normal_reminder_days` int NOT NULL DEFAULT 30 COMMENT '普通欠款提醒持续天数',
  `large_reminder_days` int NOT NULL DEFAULT 180 COMMENT '大额欠款提醒持续天数',
  `sms_interval_days` int NOT NULL DEFAULT 30 COMMENT '大额欠款短信最小间隔天数',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_policy_tenant` (`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户返利资产V2策略表';

-- ============================================================
-- 阶段一V2启用前置预检（必须人工执行并确认以下结果均为0行）
-- 任一查询返回数据时必须停止该租户V2启用；禁止自动合并、删除或修复历史资金数据。
-- 以下唯一键DDL必须在预检清零后执行，建议作为发布B受控变更单独执行。
-- ============================================================
SELECT `tenant_id`, `platform_code`, `platform_order_id`, COUNT(*) AS `duplicate_count`
FROM `cps_order`
WHERE `deleted` = b'0'
GROUP BY `tenant_id`, `platform_code`, `platform_order_id`
HAVING COUNT(*) > 1;

SELECT `tenant_id`, `member_id`, COUNT(*) AS `duplicate_count`
FROM `cps_rebate_account`
WHERE `deleted` = b'0'
GROUP BY `tenant_id`, `member_id`
HAVING COUNT(*) > 1;

SELECT `tenant_id`, `order_id`, `rebate_type`, COUNT(*) AS `duplicate_count`
FROM `cps_rebate_record`
WHERE `deleted` = b'0' AND `order_id` IS NOT NULL
GROUP BY `tenant_id`, `order_id`, `rebate_type`
HAVING COUNT(*) > 1;

SELECT `tenant_id`, `business_type`, `idempotency_key`, COUNT(*) AS `duplicate_count`
FROM `cps_rebate_asset_ledger`
WHERE `deleted` = b'0'
GROUP BY `tenant_id`, `business_type`, `idempotency_key`
HAVING COUNT(*) > 1;

SELECT `tenant_id`, `business_type`, `idempotency_key`, COUNT(*) AS `duplicate_count`
FROM `cps_freeze_record`
WHERE `deleted` = b'0' AND `business_type` IS NOT NULL AND `idempotency_key` IS NOT NULL
GROUP BY `tenant_id`, `business_type`, `idempotency_key`
HAVING COUNT(*) > 1;

SELECT account.`tenant_id`, account.`id`, account.`member_id`,
       ROUND((COALESCE(account.`available_balance`, 0)
              + COALESCE(account.`frozen_balance`, 0)
              - COALESCE(account.`debt_balance`, 0)) * 100) AS `account_net_cent`,
       COALESCE(ledger.`net_cent`, 0) AS `ledger_net_cent`
FROM `cps_rebate_account` account
LEFT JOIN (
  SELECT `tenant_id`, `member_id`,
         SUM(`available_change_cent` + `frozen_change_cent` - `debt_change_cent`) AS `net_cent`
  FROM `cps_rebate_asset_ledger`
  WHERE `deleted` = b'0'
  GROUP BY `tenant_id`, `member_id`
) ledger ON ledger.`tenant_id` = account.`tenant_id` AND ledger.`member_id` = account.`member_id`
WHERE account.`deleted` = b'0'
  AND ROUND((COALESCE(account.`available_balance`, 0)
             + COALESCE(account.`frozen_balance`, 0)
             - COALESCE(account.`debt_balance`, 0)) * 100) <> COALESCE(ledger.`net_cent`, 0);

SELECT account.`tenant_id`, account.`id`, account.`member_id`,
       ROUND(COALESCE(account.`frozen_balance`, 0) * 100) AS `account_frozen_cent`,
       COALESCE(freeze_summary.`frozen_cent`, 0) AS `record_frozen_cent`
FROM `cps_rebate_account` account
LEFT JOIN (
  SELECT `tenant_id`, `member_id`,
         SUM(COALESCE(`amount_cent`, ROUND(`freeze_amount` * 100))) AS `frozen_cent`
  FROM `cps_freeze_record`
  WHERE `deleted` = b'0' AND `status` = 'frozen'
  GROUP BY `tenant_id`, `member_id`
) freeze_summary
  ON freeze_summary.`tenant_id` = account.`tenant_id`
 AND freeze_summary.`member_id` = account.`member_id`
WHERE account.`deleted` = b'0'
  AND ROUND(COALESCE(account.`frozen_balance`, 0) * 100)
      <> COALESCE(freeze_summary.`frozen_cent`, 0);

SELECT account.`tenant_id`, account.`id`, account.`member_id`
FROM `cps_rebate_account` account
WHERE account.`deleted` = b'0'
  AND NOT EXISTS (
    SELECT 1 FROM `cps_rebate_asset_ledger` ledger
    WHERE ledger.`tenant_id` = account.`tenant_id` AND ledger.`deleted` = b'0'
      AND ledger.`business_type` = 'OPENING_BALANCE'
      AND ledger.`business_id` = CAST(account.`id` AS CHAR)
  );

SELECT ledger.`tenant_id`, ledger.`id`, ledger.`member_id`, ledger.`business_type`, ledger.`business_id`
FROM `cps_rebate_asset_ledger` ledger
LEFT JOIN `cps_rebate_account` account
  ON account.`tenant_id` = ledger.`tenant_id`
 AND account.`member_id` = ledger.`member_id`
 AND account.`deleted` = b'0'
WHERE ledger.`deleted` = b'0' AND account.`id` IS NULL;

SELECT freeze_record.`tenant_id`, freeze_record.`id`, freeze_record.`member_id`,
       freeze_record.`business_type`, freeze_record.`business_id`
FROM `cps_freeze_record` freeze_record
LEFT JOIN `cps_rebate_account` account
  ON account.`tenant_id` = freeze_record.`tenant_id`
 AND account.`member_id` = freeze_record.`member_id`
 AND account.`deleted` = b'0'
WHERE freeze_record.`deleted` = b'0'
  AND freeze_record.`status` = 'frozen'
  AND account.`id` IS NULL;

SELECT `tenant_id`, `id`, `business_type`, `business_id`, `freeze_amount`, `amount_cent`
FROM `cps_freeze_record`
WHERE `deleted` = b'0' AND (`amount_cent` IS NULL OR `amount_cent` <= 0);

-- 发布B唯一键切换：仅允许在上述预检结果均为0行后，由变更平台单独执行以下语句。
-- 这里故意保持注释，避免发布A执行增量脚本时绕过人工预检并直接改变资金唯一约束。
-- 先核对现有索引的真实名称与列顺序；不得假设旧库索引名固定：
-- SELECT `table_name`, `index_name`, `non_unique`,
--        GROUP_CONCAT(`column_name` ORDER BY `seq_in_index`) AS `columns_in_order`
-- FROM `information_schema`.`statistics`
-- WHERE `table_schema` = DATABASE()
--   AND `table_name` IN ('cps_order', 'cps_rebate_account', 'cps_rebate_record', 'cps_freeze_record')
-- GROUP BY `table_name`, `index_name`, `non_unique`
-- ORDER BY `table_name`, `index_name`;
--
-- 受控执行模板：把 <EXISTING_INDEX_NAME> 替换为上一步确认的旧唯一索引名；
-- 若目标唯一索引已存在且列顺序正确，则跳过对应 ALTER。一次只执行一张表并保留变更单证据。
-- ALTER TABLE `cps_order`
--   DROP INDEX `<EXISTING_ORDER_UNIQUE_INDEX_NAME>`,
--   ADD UNIQUE KEY `uk_tenant_platform_order` (`tenant_id`, `platform_code`, `platform_order_id`) USING BTREE;
--
-- ALTER TABLE `cps_rebate_account`
--   DROP INDEX `<EXISTING_ACCOUNT_UNIQUE_INDEX_NAME>`,
--   ADD UNIQUE KEY `uk_tenant_member_id` (`tenant_id`, `member_id`) USING BTREE;
--
-- ALTER TABLE `cps_rebate_record`
--   ADD UNIQUE KEY `uk_tenant_order_rebate_type` (`tenant_id`, `order_id`, `rebate_type`) USING BTREE;
--
-- ALTER TABLE `cps_freeze_record`
--   DROP INDEX `uk_business_idempotency`,
--   ADD UNIQUE KEY `uk_tenant_business_idempotency` (`tenant_id`, `business_type`, `idempotency_key`) USING BTREE;
--
-- 完成上述唯一键、每个历史账户OPENING_BALANCE期初流水、历史冻结金额与账户冻结余额对账后，
-- 才可由发布变更单按租户执行；禁止直接改字段，必须使用本 SQL 或管理端受控确认接口：
-- UPDATE `cps_rebate_asset_policy` policy
-- JOIN (
--   SELECT `tenant_id`, `batch_no`, `executed_at`, `ready`
--   FROM `cps_rebate_asset_migration_check`
--   WHERE `tenant_id` = ?
--   ORDER BY `id` DESC
--   LIMIT 1
-- ) latest ON latest.`tenant_id` = policy.`tenant_id`
-- SET policy.`migration_ready` = 1,
--     policy.`latest_ready_check_batch_no` = latest.`batch_no`,
--     policy.`ready_check_time` = latest.`executed_at`
-- WHERE policy.`tenant_id` = ?
--   AND latest.`batch_no` = ?
--   AND latest.`ready` = 1
--   AND policy.`v2_enabled` = 0
--   AND policy.`deleted` = b'0';

-- ============================================================
-- 修改时间：2026-07-13 12:30:00
-- 目的：资产安全中心菜单与权限。
-- ============================================================
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6290, '资产安全中心', 'cps:rebate-debt:query', 2, 45, 6286, 'asset', 'ep:money', 'cps/asset/index', 'CpsAssetSafety', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6290 OR `component_name` = 'CpsAssetSafety');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6291, '返利欠款查询', 'cps:rebate-debt:query', 3, 1, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6291 OR `permission` = 'cps:rebate-debt:query');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6292, '返利欠款调整', 'cps:rebate-debt:adjust', 3, 2, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6292 OR `permission` = 'cps:rebate-debt:adjust');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6293, '资产流水查询', 'cps:rebate-asset-ledger:query', 3, 3, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6293 OR `permission` = 'cps:rebate-asset-ledger:query');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6294, '资产策略查询', 'cps:rebate-asset-policy:query', 3, 4, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6294 OR `permission` = 'cps:rebate-asset-policy:query');

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6295, '资产策略更新', 'cps:rebate-asset-policy:update', 3, 5, 6290, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-07-13 12:30:00', '1', '2026-07-13 12:30:00', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6295 OR `permission` = 'cps:rebate-asset-policy:update');

-- ============================================================
-- 修改时间：2026-07-13 16:00:00
-- 目的：返利兑换 Token 增加可恢复补偿水位与多租户幂等约束。
-- ============================================================
ALTER TABLE `cps_rebate_token_exchange_order`
  ADD COLUMN `retry_count` int NOT NULL DEFAULT 0 COMMENT '补偿重试次数' AFTER `completed_at`,
  ADD COLUMN `next_retry_time` datetime NULL DEFAULT NULL COMMENT '下次补偿时间' AFTER `retry_count`,
  ADD COLUMN `last_compensation_at` datetime NULL DEFAULT NULL COMMENT '最近补偿时间' AFTER `next_retry_time`,
  ADD COLUMN `status_version` int NOT NULL DEFAULT 0 COMMENT '状态变更版本' AFTER `last_compensation_at`;

ALTER TABLE `cps_rebate_token_exchange_order`
  DROP INDEX `uk_exchange_order_no`,
  DROP INDEX `uk_idempotency_key`,
  DROP INDEX `idx_status`,
  ADD UNIQUE KEY `uk_exchange_tenant_order_no` (`tenant_id`, `exchange_order_no`) USING BTREE,
  ADD UNIQUE KEY `uk_exchange_tenant_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  ADD KEY `idx_exchange_compensation` (`tenant_id`, `status`, `next_retry_time`) USING BTREE;

-- ============================================================
-- 修改时间：2026-07-13 17:45:00
-- 目的：提现申请接入统一资产冻结、PayTransfer 幂等恢复与逐租户补偿。
-- ============================================================
ALTER TABLE `cps_withdraw`
  ADD COLUMN `amount_cent` bigint NOT NULL DEFAULT 0 COMMENT '提现金额（分，V2资金计算字段）' AFTER `amount`,
  ADD COLUMN `freeze_record_id` bigint NULL DEFAULT NULL COMMENT '统一资产冻结记录ID' AFTER `transfer_error`,
  ADD COLUMN `idempotency_key` varchar(64) NULL DEFAULT NULL COMMENT '提现请求幂等键（租户内唯一）' AFTER `freeze_record_id`,
  ADD COLUMN `status_version` int NOT NULL DEFAULT 0 COMMENT '状态CAS版本' AFTER `idempotency_key`,
  ADD COLUMN `pay_transfer_id` bigint NULL DEFAULT NULL COMMENT 'Pay模块转账单ID' AFTER `status_version`,
  ADD COLUMN `transfer_channel_code` varchar(32) NULL DEFAULT NULL COMMENT 'Pay模块转账渠道编码' AFTER `pay_transfer_id`,
  ADD COLUMN `retry_count` int NOT NULL DEFAULT 0 COMMENT '补偿重试次数' AFTER `transfer_channel_code`,
  ADD COLUMN `next_retry_time` datetime NULL DEFAULT NULL COMMENT '下次补偿时间' AFTER `retry_count`,
  ADD COLUMN `last_attempt_time` datetime NULL DEFAULT NULL COMMENT '最近打款尝试时间' AFTER `next_retry_time`,
  ADD UNIQUE KEY `uk_withdraw_tenant_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE,
  ADD KEY `idx_withdraw_compensation` (`tenant_id`, `status`, `next_retry_time`) USING BTREE,
  ADD KEY `idx_pay_transfer_id` (`pay_transfer_id`) USING BTREE;

UPDATE `cps_withdraw`
SET `amount_cent` = ROUND(`amount` * 100)
WHERE `amount_cent` = 0 AND `amount` > 0;

-- ============================================================
-- 修改时间：2026-07-13 18:25:00
-- 目的：增加 CPS OpenAPI 签名失败不可变审计日志。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_openapi_access_log` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OpenAPI访问审计日志表';

-- ============================================================
-- 修改时间：2026-07-14 14:40:00
-- 目的：阶段1营销核心增加轻量商品主档、来源映射与价格快照；仅用于检索、运营、推荐和转链前展示，不作为结算事实。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_goods_master` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS商品主档表';

CREATE TABLE IF NOT EXISTS `cps_goods_source_mapping` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS商品来源映射表';

CREATE TABLE IF NOT EXISTS `cps_goods_price_snapshot` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS商品价格快照表';

-- ============================================================
-- 修改时间：2026-07-14 15:05:00
-- 目的：阶段1营销核心增加券池；仅用于活动、主题、转链前展示的可用券过滤，不作为订单佣金、返利比例、冻结或资产入账事实来源。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_coupon_pool` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '券池ID',
  `master_id` bigint DEFAULT NULL COMMENT '商品主档ID',
  `source_mapping_id` bigint DEFAULT NULL COMMENT '商品来源映射ID',
  `platform_code` varchar(32) NOT NULL COMMENT '平台编码',
  `vendor_code` varchar(64) NOT NULL DEFAULT '' COMMENT '供应商编码',
  `external_goods_id` varchar(128) NOT NULL COMMENT '外部商品ID',
  `goods_sign` varchar(255) NOT NULL DEFAULT '' COMMENT 'goodsSign',
  `coupon_id` varchar(128) NOT NULL COMMENT '外部优惠券ID',
  `coupon_name` varchar(255) DEFAULT NULL COMMENT '优惠券名称',
  `coupon_amount` int DEFAULT NULL COMMENT '优惠券金额（分）',
  `threshold_amount` int DEFAULT NULL COMMENT '使用门槛金额（分）',
  `start_time` datetime DEFAULT NULL COMMENT '券开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '券结束时间',
  `stock_total` int DEFAULT NULL COMMENT '总库存',
  `stock_remain` int DEFAULT NULL COMMENT '剩余库存',
  `status` varchar(32) NOT NULL DEFAULT 'VALID' COMMENT '状态（VALID有效 DISABLED失效）',
  `source_type` varchar(32) NOT NULL DEFAULT 'VENDOR_SYNC' COMMENT '来源类型',
  `activity_id` bigint DEFAULT NULL COMMENT '活动ID',
  `theme_id` bigint DEFAULT NULL COMMENT '选品主题ID',
  `last_sync_time` datetime DEFAULT NULL COMMENT '最近同步时间',
  `raw_data` mediumtext COMMENT '第三方原始券数据',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_cps_coupon_pool_coupon` (`tenant_id`, `platform_code`, `vendor_code`, `coupon_id`, `deleted`) USING BTREE,
  KEY `idx_cps_coupon_pool_goods` (`tenant_id`, `deleted`, `platform_code`, `vendor_code`, `external_goods_id`, `goods_sign`) USING BTREE,
  KEY `idx_cps_coupon_pool_usable` (`tenant_id`, `deleted`, `status`, `start_time`, `end_time`, `stock_remain`) USING BTREE,
  KEY `idx_cps_coupon_pool_activity` (`tenant_id`, `deleted`, `activity_id`, `theme_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS券池表';

-- ============================================================
-- 修改时间：2026-07-14 15:35:00
-- 目的：阶段1营销核心增加自有短链映射；复用已有转链目标，仅记录营销元数据、不可枚举短码和归因摘要，不复制供应商转链或结算链路。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_marketing_short_link` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS营销短链表';

-- ============================================================
-- 修改时间：2026-07-14 16:20:00
-- 目的：阶段1营销核心增加点击事件表；仅记录短链点击、渠道、设备与归因摘要，用于营销漏斗分析，不作为结算事实。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_marketing_click_event` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS营销点击事件表';
-- ============================================================
-- 修改时间：2026-07-23 00:00:00
-- 目的：新增租户隔离、密文存储的平台接入草稿，并修正平台与推广位的未删除记录唯一约束。
-- 说明：生成列允许历史软删记录保留；索引与列变更通过 information_schema 判定，可重复执行。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_platform_onboarding_draft` (
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

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_platform'
      AND `column_name` = 'active_unique_key'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_platform` ADD COLUMN `active_unique_key` varchar(128) GENERATED ALWAYS AS (IF(`deleted` = b''0'', CONCAT(`tenant_id`, '':'', `platform_code`), NULL)) STORED COMMENT ''未删除平台租户唯一键'''
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

-- 历史库可能存在同租户、同平台、同推广位的多条未删除记录。
-- 唯一索引创建前保留最大 ID 的最新记录，并软删除其余记录；重复执行不会再次修改数据。
UPDATE `cps_adzone` AS `older`
    INNER JOIN `cps_adzone` AS `newer`
ON `older`.`tenant_id` = `newer`.`tenant_id`
    AND `older`.`platform_code` = `newer`.`platform_code`
    AND `older`.`adzone_id` = `newer`.`adzone_id`
    AND `older`.`id` < `newer`.`id`
    SET `older`.`deleted` = b'1',
        `older`.`updater` = 'platform-onboarding-migration',
        `older`.`update_time` = CURRENT_TIMESTAMP
WHERE `older`.`deleted` = b'0'
  AND `newer`.`deleted` = b'0';

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_platform'
      AND `index_name` = 'uk_cps_platform_active'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_platform` ADD UNIQUE INDEX `uk_cps_platform_active` (`active_unique_key`) USING BTREE'
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_platform'
      AND `index_name` = 'uk_platform_code'
  ),
  'ALTER TABLE `cps_platform` DROP INDEX `uk_platform_code`',
  'SELECT 1'
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_adzone'
      AND `column_name` = 'active_unique_key'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_adzone` ADD COLUMN `active_unique_key` varchar(191) GENERATED ALWAYS AS (IF(`deleted` = b''0'', CONCAT(CHAR_LENGTH(CAST(`tenant_id` AS CHAR)), '':'', CAST(`tenant_id` AS CHAR), CHAR_LENGTH(`platform_code`), '':'', `platform_code`, CHAR_LENGTH(`adzone_id`), '':'', `adzone_id`), NULL)) STORED COMMENT ''未删除推广位租户唯一键（长度前缀编码）'''
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_adzone'
      AND `index_name` = 'uk_cps_adzone_active'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_adzone` ADD UNIQUE INDEX `uk_cps_adzone_active` (`active_unique_key`) USING BTREE'
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

-- ============================================================
-- 修改时间：2026-07-24 16:45:00
-- 目的：供应商唯一约束仅覆盖未删除记录，允许重复接入循环保留任意数量的历史软删记录。
-- 说明：先移除包含 deleted 的旧唯一索引，再清理未删除重复行，最后建立生成列唯一索引。
-- ============================================================
SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_api_vendor'
      AND `index_name` = 'uk_vendor_platform'
  ),
  'ALTER TABLE `cps_api_vendor` DROP INDEX `uk_vendor_platform`',
  'SELECT 1'
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

-- 历史库若存在同租户、同供应商、同平台的多条未删除记录，保留最大 ID 的最新记录。
UPDATE `cps_api_vendor` AS `older`
    INNER JOIN `cps_api_vendor` AS `newer`
ON `older`.`tenant_id` = `newer`.`tenant_id`
    AND `older`.`vendor_code` = `newer`.`vendor_code`
    AND `older`.`platform_code` = `newer`.`platform_code`
    AND `older`.`id` < `newer`.`id`
    SET `older`.`deleted` = b'1',
        `older`.`updater` = 'platform-onboarding-migration',
        `older`.`update_time` = CURRENT_TIMESTAMP
WHERE `older`.`deleted` = b'0'
  AND `newer`.`deleted` = b'0';

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_api_vendor'
      AND `column_name` = 'active_unique_key'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_api_vendor` ADD COLUMN `active_unique_key` varchar(191) GENERATED ALWAYS AS (IF(`deleted` = b''0'', CONCAT(CHAR_LENGTH(CAST(`tenant_id` AS CHAR)), '':'', CAST(`tenant_id` AS CHAR), CHAR_LENGTH(`vendor_code`), '':'', `vendor_code`, CHAR_LENGTH(`platform_code`), '':'', `platform_code`), NULL)) STORED COMMENT ''未删除供应商租户唯一键（长度前缀编码）'''
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

SET @cps_onboarding_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`statistics`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_api_vendor'
      AND `index_name` = 'uk_cps_api_vendor_active'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_api_vendor` ADD UNIQUE INDEX `uk_cps_api_vendor_active` (`active_unique_key`) USING BTREE'
);
PREPARE cps_onboarding_stmt FROM @cps_onboarding_sql;
EXECUTE cps_onboarding_stmt;
DEALLOCATE PREPARE cps_onboarding_stmt;

-- ============================================================
-- 修改时间：2026-07-24 18:00:00
-- 目的：将统一平台配置中心设为可见入口，并隐藏四个旧配置页面。
-- ============================================================
UPDATE `system_menu`
SET `visible` = b'0', `updater` = 'platform-onboarding',
    `update_time` = '2026-07-24 18:00:00'
WHERE `id` IN (6229, 6251, 6256, 6261) AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
 `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6297, '平台配置中心', 'cps:platform-onboarding:query', 2, 10, 6287,
       'platform-onboarding', 'ep:setting', 'cps/platformOnboarding/index',
       'CpsPlatformOnboarding', 0, b'1', b'1', b'1', '1',
       '2026-07-24 00:00:00', 'platform-onboarding', '2026-07-24 18:00:00', b'0'
    WHERE NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 6297);

UPDATE `system_menu`
SET `name` = '平台配置中心',
    `permission` = 'cps:platform-onboarding:query',
    `type` = 2,
    `sort` = 10,
    `parent_id` = 6287,
    `path` = 'platform-onboarding',
    `icon` = 'ep:setting',
    `component` = 'cps/platformOnboarding/index',
    `component_name` = 'CpsPlatformOnboarding',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `id` = 6297 AND `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`,
 `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT menu_id, menu_name, menu_permission, 3, menu_sort, 6297, '', '', '', '',
       0, b'1', b'1', b'1', '1', '2026-07-24 00:00:00',
       'platform-onboarding', '2026-07-24 18:00:00', b'0'
FROM (
         SELECT 6298 AS menu_id, '平台配置中心查询' AS menu_name,
                'cps:platform-onboarding:query' AS menu_permission, 1 AS menu_sort
         UNION ALL SELECT 6299, '平台配置中心创建', 'cps:platform-onboarding:create', 2
         UNION ALL SELECT 6300, '平台配置中心更新', 'cps:platform-onboarding:update', 3
         UNION ALL SELECT 6301, '平台配置中心删除', 'cps:platform-onboarding:delete', 4
         UNION ALL SELECT 6302, '平台配置中心测试', 'cps:platform-onboarding:test', 5
         UNION ALL SELECT 6303, '平台配置中心发布', 'cps:platform-onboarding:publish', 6
     ) AS onboarding_menu
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` existing WHERE existing.`id` = onboarding_menu.menu_id
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

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6297),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6229, 6251, 6256, 6261]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6297');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6298),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6229, 6251, 6256, 6261]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6298');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6299),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6231, 6253, 6258, 6263]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6299');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6300),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6232, 6254, 6259, 6264]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6300');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6301),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6233, 6255, 6260, 6265]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6301');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6302),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6229, 6251, 6256, 6261]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6302');

UPDATE `system_tenant_package`
SET `menu_ids` = JSON_ARRAY_APPEND(`menu_ids`, '$', 6303),
    `updater` = 'platform-onboarding', `update_time` = '2026-07-24 18:00:00'
WHERE `deleted` = b'0' AND JSON_VALID(`menu_ids`)
  AND JSON_CONTAINS(`menu_ids`, '[6231, 6253, 6258, 6263, 6232, 6254, 6259, 6264]')
  AND NOT JSON_CONTAINS(`menu_ids`, '6303');

-- ============================================================
-- 修改时间：2026-07-27 11:10:00
-- 目的：为历史平台表补齐供应商路由字段，保持旧库升级结构与全量建库结构一致。
-- 说明：通过 information_schema 判断字段是否存在，可安全重复执行。
-- ============================================================
SET @cps_platform_vendor_routing_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_platform'
      AND `column_name` = 'active_vendor_code'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_platform` ADD COLUMN `active_vendor_code` varchar(32) DEFAULT ''dataoke'' COMMENT ''当前激活的供应商编码（用于路由选择）'' AFTER `tenant_id`'
);
PREPARE cps_platform_vendor_routing_stmt FROM @cps_platform_vendor_routing_sql;
EXECUTE cps_platform_vendor_routing_stmt;
DEALLOCATE PREPARE cps_platform_vendor_routing_stmt;

SET @cps_platform_vendor_routing_sql = IF(
  EXISTS (
    SELECT 1
    FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_platform'
      AND `column_name` = 'supported_vendors'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_platform` ADD COLUMN `supported_vendors` varchar(256) DEFAULT ''dataoke'' COMMENT ''支持的供应商列表（逗号分隔）'' AFTER `active_vendor_code`'
);
PREPARE cps_platform_vendor_routing_stmt FROM @cps_platform_vendor_routing_sql;
EXECUTE cps_platform_vendor_routing_stmt;
DEALLOCATE PREPARE cps_platform_vendor_routing_stmt;

-- ============================================================
-- 修改时间：2026-07-30 16:00:00
-- 目的：分离第三方活动记录ID与转链参数，保留活动转链元数据，并修正无效的88VIP空链接种子。
-- 说明：字段变更通过 information_schema 判断，可安全重复执行。
-- ============================================================
SET @cps_rebate_activity_metadata_sql = IF(
  EXISTS (
    SELECT 1 FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_rebate_activity'
      AND `column_name` = 'promotion_activity_id'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_rebate_activity` ADD COLUMN `promotion_activity_id` varchar(128) DEFAULT NULL COMMENT ''供应商活动转链参数ID'' AFTER `external_activity_id`'
);
PREPARE cps_rebate_activity_metadata_stmt FROM @cps_rebate_activity_metadata_sql;
EXECUTE cps_rebate_activity_metadata_stmt;
DEALLOCATE PREPARE cps_rebate_activity_metadata_stmt;

SET @cps_rebate_activity_metadata_sql = IF(
  EXISTS (
    SELECT 1 FROM `information_schema`.`columns`
    WHERE `table_schema` = DATABASE()
      AND `table_name` = 'cps_rebate_activity'
      AND `column_name` = 'vendor_metadata'
  ),
  'SELECT 1',
  'ALTER TABLE `cps_rebate_activity` ADD COLUMN `vendor_metadata` text DEFAULT NULL COMMENT ''供应商活动转链元数据JSON'' AFTER `promotion_activity_id`'
);
PREPARE cps_rebate_activity_metadata_stmt FROM @cps_rebate_activity_metadata_sql;
EXECUTE cps_rebate_activity_metadata_stmt;
DEALLOCATE PREPARE cps_rebate_activity_metadata_stmt;

UPDATE `cps_rebate_activity`
SET `jump_type` = 'search',
    `search_keyword` = '88VIP',
    `updater` = 'activity-link-reliability',
    `update_time` = '2026-07-30 16:00:00'
WHERE `source_type` = 'configured'
  AND `external_activity_id` = 'taobao-88vip-youku'
  AND (`jump_url` IS NULL OR `jump_url` = '');

-- ============================================================
-- 修改时间：2026-07-30 22:20:00
-- 目的：好单库闪购活动转链增加安全 SID 映射，并补齐订单申领审核字段与查询索引。
-- 说明：字段和索引均通过 information_schema 判断，可安全重复执行。
-- ============================================================
SET @cps_transfer_attribution_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_transfer_record' AND `column_name` = 'vendor_code'),
  'SELECT 1',
  'ALTER TABLE `cps_transfer_record` ADD COLUMN `vendor_code` varchar(32) DEFAULT NULL COMMENT ''供应商编码'' AFTER `platform_code`'
);
PREPARE cps_transfer_attribution_stmt FROM @cps_transfer_attribution_sql;
EXECUTE cps_transfer_attribution_stmt;
DEALLOCATE PREPARE cps_transfer_attribution_stmt;

SET @cps_transfer_attribution_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_transfer_record' AND `column_name` = 'activity_id'),
  'SELECT 1',
  'ALTER TABLE `cps_transfer_record` ADD COLUMN `activity_id` bigint DEFAULT NULL COMMENT ''活动ID'' AFTER `vendor_code`'
);
PREPARE cps_transfer_attribution_stmt FROM @cps_transfer_attribution_sql;
EXECUTE cps_transfer_attribution_stmt;
DEALLOCATE PREPARE cps_transfer_attribution_stmt;

SET @cps_transfer_attribution_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_transfer_record' AND `column_name` = 'attribution_type'),
  'SELECT 1',
  'ALTER TABLE `cps_transfer_record` ADD COLUMN `attribution_type` varchar(32) DEFAULT NULL COMMENT ''归因令牌类型'' AFTER `activity_id`'
);
PREPARE cps_transfer_attribution_stmt FROM @cps_transfer_attribution_sql;
EXECUTE cps_transfer_attribution_stmt;
DEALLOCATE PREPARE cps_transfer_attribution_stmt;

SET @cps_transfer_attribution_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_transfer_record' AND `column_name` = 'attribution_token'),
  'SELECT 1',
  'ALTER TABLE `cps_transfer_record` ADD COLUMN `attribution_token` varchar(64) DEFAULT NULL COMMENT ''不透明归因令牌'' AFTER `attribution_type`'
);
PREPARE cps_transfer_attribution_stmt FROM @cps_transfer_attribution_sql;
EXECUTE cps_transfer_attribution_stmt;
DEALLOCATE PREPARE cps_transfer_attribution_stmt;

SET @cps_transfer_attribution_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`statistics`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_transfer_record' AND `index_name` = 'uk_transfer_attribution_token'),
  'SELECT 1',
  'ALTER TABLE `cps_transfer_record` ADD UNIQUE KEY `uk_transfer_attribution_token` (`tenant_id`, `vendor_code`, `platform_code`, `attribution_type`, `attribution_token`, `deleted`) USING BTREE'
);
PREPARE cps_transfer_attribution_stmt FROM @cps_transfer_attribution_sql;
EXECUTE cps_transfer_attribution_stmt;
DEALLOCATE PREPARE cps_transfer_attribution_stmt;

SET @cps_transfer_attribution_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`statistics`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_transfer_record' AND `index_name` = 'idx_transfer_attribution_lookup'),
  'SELECT 1',
  'ALTER TABLE `cps_transfer_record` ADD KEY `idx_transfer_attribution_lookup` (`tenant_id`, `vendor_code`, `platform_code`, `attribution_type`, `attribution_token`, `status`, `expire_time`) USING BTREE'
);
PREPARE cps_transfer_attribution_stmt FROM @cps_transfer_attribution_sql;
EXECUTE cps_transfer_attribution_stmt;
DEALLOCATE PREPARE cps_transfer_attribution_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `column_name` = 'idempotency_key'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD COLUMN `idempotency_key` varchar(128) DEFAULT NULL COMMENT ''幂等键'' AFTER `operator_id`'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `column_name` = 'review_status'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD COLUMN `review_status` varchar(32) DEFAULT NULL COMMENT ''申领审核状态'' AFTER `idempotency_key`'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `column_name` = 'review_audit_note'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD COLUMN `review_audit_note` varchar(500) DEFAULT NULL COMMENT ''审核说明'' AFTER `review_status`'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `column_name` = 'review_operator_id'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD COLUMN `review_operator_id` bigint DEFAULT NULL COMMENT ''审核操作人ID'' AFTER `review_audit_note`'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`columns`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `column_name` = 'review_time'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD COLUMN `review_time` datetime DEFAULT NULL COMMENT ''审核时间'' AFTER `review_operator_id`'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`statistics`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `index_name` = 'uk_attribution_idempotency'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD UNIQUE KEY `uk_attribution_idempotency` (`tenant_id`, `idempotency_key`) USING BTREE'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

SET @cps_order_claim_sql = IF(
  EXISTS (SELECT 1 FROM `information_schema`.`statistics`
          WHERE `table_schema` = DATABASE() AND `table_name` = 'cps_order_attribution_log' AND `index_name` = 'idx_attribution_claim_review'),
  'SELECT 1',
  'ALTER TABLE `cps_order_attribution_log` ADD KEY `idx_attribution_claim_review` (`tenant_id`, `action`, `review_status`, `create_time`) USING BTREE'
);
PREPARE cps_order_claim_stmt FROM @cps_order_claim_sql;
EXECUTE cps_order_claim_stmt;
DEALLOCATE PREPARE cps_order_claim_stmt;

-- ============================================================
-- 修改时间：2026-08-04 00:00:00
-- 目的：新增会员 CPS 商品足迹与收藏展示快照，支持租户隔离、软删除与幂等操作。
-- 说明：快照金额以分存储且允许为空；本表严禁用于订单归因、返利结算或资产变更。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_member_goods_record` (
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS会员商品浏览收藏展示快照表';

-- ============================================================
-- 修改时间：2026-08-07 17:30:00
-- 目的：声明唯品会支持好单库供应商，并提供无真实凭证的可配置占位记录。
-- 说明：已有平台仅补充支持列表；已有供应商配置、凭证、授权状态和启停状态均不覆盖，可安全重复执行。
-- ============================================================
INSERT INTO `cps_platform` (`platform_code`, `platform_name`, `platform_logo`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `platform_service_rate`, `sort`, `status`, `extra_config`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `active_vendor_code`, `supported_vendors`)
SELECT 'vip', '唯品会', NULL, NULL, NULL, NULL, NULL, NULL, 0.0000, 70, 0, NULL, '请先配置好单库唯品会供应商凭证，确认账号已开通唯品会权限后再启用', 'system', '2026-08-07 17:30:00', 'system', '2026-08-07 17:30:00', b'0', 1, 'haodanku', 'haodanku'
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_platform`
  WHERE `tenant_id` = 1 AND `platform_code` = 'vip' AND `deleted` = b'0'
);

UPDATE `cps_platform`
 SET `supported_vendors` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM COALESCE(`supported_vendors`, '')), ''), 'haodanku'),
     `update_time` = '2026-08-07 17:30:00'
 WHERE `tenant_id` = 1
   AND `platform_code` = 'vip'
   AND `deleted` = b'0'
   AND FIND_IN_SET('haodanku', REPLACE(COALESCE(`supported_vendors`, ''), ' ', '')) = 0;

INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'haodanku', '好单库', 'aggregator', 'vip', '', '', 'https://v2.api.haodanku.com', NULL, NULL, '{"searchPath":"/vip_goods_search","convertPath":"/vip_ratesurl"}', 100, 0, '请配置好单库 apikey 和唯品会 PID；普通账号使用 v2 搜索与转链，v3 订单能力按官方权限开通', 'system', '2026-08-07 17:30:00', 'system', '2026-08-11 16:15:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'haodanku' AND `platform_code` = 'vip' AND `deleted` = b'0'
);

-- ============================================================
-- 修改时间：2026-08-11 16:15:00
-- 目的：将唯品会普通账号搜索与转链配置迁移到官方 v2 接口，移除高佣转链依赖。
-- 说明：仅修正接口地址、路径说明，不覆盖 apikey、PID、授权信息或启停状态。
-- ============================================================
UPDATE `cps_api_vendor`
SET `api_base_url` = 'https://v2.api.haodanku.com',
    `extra_config` = JSON_SET(
      CASE WHEN JSON_VALID(`extra_config`) THEN `extra_config` ELSE '{}' END,
      '$.searchPath', '/vip_goods_search',
      '$.convertPath', '/vip_ratesurl'
    ),
    `remark` = '请配置好单库 apikey 和唯品会 PID；普通账号使用 v2 搜索与转链，v3 订单能力按官方权限开通',
    `updater` = 'system',
    `update_time` = '2026-08-11 16:15:00'
WHERE `vendor_code` = 'haodanku'
  AND `platform_code` = 'vip'
  AND `deleted` = b'0';

-- ============================================================
-- 修改时间：2026-10-22 12:15:00
-- 目的：CPS三级分销推广关系表(cps_promoter_relation)、多级分账规则配置表(cps_split_rule)、分账记录角色扩展与净佣金池瓜分引擎支持
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.2', '2026-10-22 12:15:00', 'cps-update.sql', 'CPS三级分销推广关系表(cps_promoter_relation)、多级分账规则配置表(cps_split_rule)、分账记录角色扩展与净佣金池瓜分引擎支持', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.2' AND `schema_update_time` = '2026-10-22 12:15:00'
);

-- ============================================================
-- 修改时间：2026-10-22 14:42:48
-- 目的：新增会员提现账户管理表(cps_withdraw_account)与提现安全风控审计支持
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.3', '2026-10-22 14:42:48', 'cps-update.sql', '新增会员提现账户管理表(cps_withdraw_account)与提现安全风控审计支持', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.3' AND `schema_update_time` = '2026-10-22 14:42:48'
);

-- ============================================================
-- 修改时间：2026-10-26 11:54:01
-- 目的：初始化抖音联盟官方OpenAPI平台配置与供应商接入元数据
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.5', '2026-10-26 11:54:01', 'cps-update.sql', '初始化抖音联盟官方OpenAPI平台配置与供应商接入元数据', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.5' AND `schema_update_time` = '2026-10-26 11:54:01'
);

-- ============================================================
-- 修改时间：2026-10-26 17:11:54
-- 目的：初始化穿山甲电商联盟（CSJ）抖音电商CPS供应商接入元数据
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.7', '2026-10-26 17:11:54', 'cps-update.sql', '初始化穿山甲电商联盟（CSJ）抖音电商CPS供应商接入元数据', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.7' AND `schema_update_time` = '2026-10-26 17:11:54'
);

-- ============================================================
-- 修改时间：2026-10-27 09:45:00
-- 目的：修复美团官方与穿山甲抖音官方活动主图失效URL
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.8', '2026-10-27 09:45:00', 'cps-update.sql', '修复美团官方与穿山甲抖音官方活动主图失效URL', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.8' AND `schema_update_time` = '2026-10-27 09:45:00'
);

-- ============================================================
-- 修改时间：2026-10-27 10:00:00
-- 目的：清洗聚推客活动奖励字段HTML标签并同步佣金率
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.9', '2026-10-27 10:00:00', 'cps-update.sql', '清洗聚推客活动奖励字段HTML标签并同步佣金率', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.9' AND `schema_update_time` = '2026-10-27 10:00:00'
);

-- ============================================================
-- 修改时间：2026-10-28 11:36:51
-- 目的：初始化淘宝闪购联盟官方OpenAPI平台配置与SID会员归因
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.10', '2026-10-28 11:36:51', 'cps-update.sql', '初始化淘宝闪购联盟官方OpenAPI平台配置与SID会员归因', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.10' AND `schema_update_time` = '2026-10-28 11:36:51'
);

-- ============================================================
-- 修改时间：2026-10-28 22:01:10
-- 目的：初始化唯品会开放平台（VOP）官方OpenAPI供应商配置与推广位PID绑定
-- ============================================================
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.12', '2026-10-28 22:01:10', 'cps-update.sql', '初始化唯品会开放平台（VOP）官方OpenAPI供应商配置与推广位PID绑定', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.12' AND `schema_update_time` = '2026-10-28 22:01:10'
);

SET FOREIGN_KEY_CHECKS = 1;
-- ============================================================
-- Modified: 2026-08-12 18:35:00
-- Purpose: register the hourly Jutuike order synchronization job.
-- Note: created paused; enable it after the Jutuike union vendor credentials are verified.
-- ============================================================
INSERT INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
                         `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`, `deleted`)
SELECT 'Jutuike order sync', 2, 'cpsJutuikeOrderSyncJob', '{"hours":2,"queryType":4}',
       '0 10 * * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'cpsJutuikeOrderSyncJob' AND `deleted` = b'0'
);
-- 2026-08-13 18:00:00 滴滴联盟订单回推与领券回传接入
CREATE TABLE IF NOT EXISTS `cps_didi_callback_event` (
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

-- ============================================================
-- 修改时间：2026-08-25 18:20:00
-- 目的：为 AI 选品保存条件记录刷新结果摘要，支持工作台展示跳过/失败原因。
-- ============================================================
ALTER TABLE `cps_selection_theme`
  ADD COLUMN `refresh_message` varchar(500) DEFAULT NULL COMMENT '刷新结果摘要或失败原因' AFTER `last_refresh_time`;

INSERT INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
                         `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`, `deleted`)
SELECT 'AI saved filter refresh', 2, 'cpsAiSavedFilterRefreshJob', NULL,
       '0 */30 * * * ?', 1, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'cpsAiSavedFilterRefreshJob' AND `deleted` = b'0'
);

-- ============================================================
-- 修改时间：2026-08-26 16:30:00
-- 目的：补齐 AI 选品人工复核审计，并为保存条件刷新增加带超时接管的 CAS 租约与旧快照索引。
-- ============================================================
ALTER TABLE `cps_selection_theme`
  ADD COLUMN `refresh_started_time` datetime DEFAULT NULL COMMENT '当前刷新租约开始时间' AFTER `refresh_message`,
  ADD COLUMN `refresh_batch_no` varchar(64) DEFAULT NULL COMMENT '当前刷新租约批次号' AFTER `refresh_started_time`,
  ADD KEY `idx_cps_selection_theme_refresh_lease`
    (`tenant_id`, `deleted`, `theme_type`, `status`, `refresh_status`, `refresh_started_time`);

ALTER TABLE `cps_selection_theme_item`
  ADD COLUMN `manual_adjusted` tinyint NOT NULL DEFAULT 0 COMMENT '是否经过人工排序、置顶或状态调整：0否 1是' AFTER `top_flag`,
  ADD KEY `idx_cps_selection_theme_item_refresh`
    (`tenant_id`, `deleted`, `theme_id`, `source_type`, `snapshot_time`);

CREATE TABLE IF NOT EXISTS `cps_selection_ai_review` (
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

-- ============================================================
-- 修改时间：2026-08-31 14:50:00
-- 目的：大淘客近30天订单补偿批次、窗口、请求审计及退款报表导入。
-- 说明：本次最新增量统一追加于文件尾部，避免打乱历史更新顺序。
-- ============================================================
CREATE TABLE IF NOT EXISTS `cps_order_sync_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT, `platform_code` varchar(32) NOT NULL, `vendor_code` varchar(64) NOT NULL DEFAULT 'OFFICIAL',
  `order_scene` tinyint NOT NULL DEFAULT 0, `enabled` tinyint NOT NULL DEFAULT 1, `lookback_days` int NOT NULL DEFAULT 30,
  `overlap_minutes` int NOT NULL DEFAULT 5, `max_concurrency` int NOT NULL DEFAULT 1, `rate_limit_per_minute` int NOT NULL DEFAULT 60,
  `creator` varchar(64) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_order_sync_policy` (`tenant_id`,`platform_code`,`vendor_code`,`order_scene`,`deleted`), KEY `idx_order_sync_policy_enabled` (`tenant_id`,`enabled`,`platform_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步策略';
CREATE TABLE IF NOT EXISTS `cps_order_sync_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT, `batch_no` varchar(64) NOT NULL, `batch_type` varchar(32) NOT NULL, `query_type` tinyint NOT NULL,
  `platform_code` varchar(32) NOT NULL, `vendor_code` varchar(64) NOT NULL DEFAULT 'OFFICIAL', `start_time` datetime NOT NULL, `end_time` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING', `total_windows` int NOT NULL DEFAULT 0, `success_windows` int NOT NULL DEFAULT 0, `failed_windows` int NOT NULL DEFAULT 0, `retry_windows` int NOT NULL DEFAULT 0, `failure_summary` varchar(1000) DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) DEFAULT NULL, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_order_sync_batch_no` (`tenant_id`,`batch_no`), KEY `idx_order_sync_batch_status` (`tenant_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步补偿批次';
CREATE TABLE IF NOT EXISTS `cps_order_sync_window` (
  `id` bigint NOT NULL AUTO_INCREMENT, `batch_id` bigint NOT NULL, `platform_code` varchar(32) NOT NULL, `vendor_code` varchar(64) NOT NULL DEFAULT 'OFFICIAL', `order_scene` tinyint NOT NULL DEFAULT 0, `query_type` tinyint NOT NULL, `window_start` datetime NOT NULL, `window_end` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING', `pagination_mode` varchar(16) DEFAULT NULL, `next_cursor` varchar(512) DEFAULT NULL, `next_page_no` int DEFAULT 1, `retry_count` int NOT NULL DEFAULT 0, `max_retry_count` int NOT NULL DEFAULT 5, `next_retry_time` datetime DEFAULT NULL, `lease_owner` varchar(128) DEFAULT NULL, `lease_until` datetime DEFAULT NULL, `last_error_code` varchar(64) DEFAULT NULL, `last_error_message` varchar(1000) DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) DEFAULT NULL, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_order_sync_window_claim` (`tenant_id`,`status`,`next_retry_time`,`lease_until`), KEY `idx_order_sync_window_batch` (`tenant_id`,`batch_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步补偿窗口';
CREATE TABLE IF NOT EXISTS `cps_order_sync_attempt` (
  `id` bigint NOT NULL AUTO_INCREMENT, `window_id` bigint NOT NULL, `attempt_no` int NOT NULL, `request_summary` varchar(2000) DEFAULT NULL, `http_status` int DEFAULT NULL, `upstream_code` varchar(64) DEFAULT NULL, `upstream_message` varchar(1000) DEFAULT NULL, `error_category` varchar(32) DEFAULT NULL, `success` tinyint NOT NULL DEFAULT 0, `started_at` datetime NOT NULL, `finished_at` datetime DEFAULT NULL, `cost_ms` bigint DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) DEFAULT NULL, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_order_sync_attempt` (`tenant_id`,`window_id`,`attempt_no`), KEY `idx_order_sync_attempt_window` (`tenant_id`,`window_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单同步窗口请求尝试';
CREATE TABLE IF NOT EXISTS `cps_refund_report_import` (
  `id` bigint NOT NULL AUTO_INCREMENT, `batch_no` varchar(64) NOT NULL, `source` varchar(32) NOT NULL DEFAULT 'DATAOKE', `file_name` varchar(255) NOT NULL, `file_hash` varchar(128) NOT NULL, `period_start` datetime DEFAULT NULL, `period_end` datetime DEFAULT NULL, `status` varchar(20) NOT NULL DEFAULT 'PENDING', `total_rows` int NOT NULL DEFAULT 0, `matched_rows` int NOT NULL DEFAULT 0, `diff_rows` int NOT NULL DEFAULT 0, `failure_reason` varchar(1000) DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) DEFAULT NULL, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_refund_report_hash` (`tenant_id`,`source`,`file_hash`), KEY `idx_refund_report_status` (`tenant_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维权退款报表导入批次';
CREATE TABLE IF NOT EXISTS `cps_refund_report_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT, `import_id` bigint NOT NULL, `platform_code` varchar(32) NOT NULL, `platform_order_id` varchar(128) NOT NULL, `refund_type` varchar(64) DEFAULT NULL, `refund_amount` decimal(18,2) DEFAULT NULL, `refund_time` datetime DEFAULT NULL, `match_status` varchar(32) NOT NULL DEFAULT 'PENDING', `order_id` bigint DEFAULT NULL, `difference_reason` varchar(500) DEFAULT NULL, `asset_ledger_id` bigint DEFAULT NULL, `idempotency_key` varchar(160) NOT NULL,
  `creator` varchar(64) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `updater` varchar(64) DEFAULT NULL, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `deleted` bit(1) NOT NULL DEFAULT b'0', `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_refund_report_detail` (`tenant_id`,`import_id`,`platform_code`,`platform_order_id`), UNIQUE KEY `uk_refund_report_detail_key` (`tenant_id`,`idempotency_key`), KEY `idx_refund_report_detail_match` (`tenant_id`,`match_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维权退款报表明细';

-- ============================================================
-- 修改时间：2026-09-01 10:30:00
-- 目的：启用默认五分钟订单拉取，并保留订单补偿批次的窗口执行器。
-- ============================================================
UPDATE `infra_job`
SET `deleted` = b'1', `updater` = 'system', `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `handler_name` = 'cpsOrderSyncCompensationJob';

UPDATE `infra_job` AS duplicate_job
INNER JOIN `infra_job` AS retained_job
    ON retained_job.`handler_name` = duplicate_job.`handler_name`
   AND retained_job.`deleted` = b'0'
   AND retained_job.`id` < duplicate_job.`id`
SET duplicate_job.`deleted` = b'1', duplicate_job.`updater` = 'system', duplicate_job.`update_time` = NOW()
WHERE duplicate_job.`deleted` = b'0' AND duplicate_job.`handler_name` = 'cpsOrderSyncJob';

UPDATE `infra_job`
SET `name` = 'CPS大淘客订单同步', `status` = 1,
    `handler_param` = '{"hours":3,"queryType":4,"platformCode":"taobao"}',
    `cron_expression` = '0 */5 * * * ?', `retry_count` = 2,
    `retry_interval` = 60, `monitor_timeout` = 900,
    `updater` = 'system', `update_time` = NOW()
WHERE `deleted` = b'0' AND `handler_name` = 'cpsOrderSyncJob';

INSERT INTO `infra_job` (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,`monitor_timeout`,`creator`,`updater`,`deleted`)
SELECT 'CPS大淘客订单同步', 1, 'cpsOrderSyncJob', '{"hours":3,"queryType":4,"platformCode":"taobao"}', '0 */5 * * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='cpsOrderSyncJob' AND `deleted`=b'0');

UPDATE `infra_job`
SET `name` = 'CPS订单补偿批次执行', `status` = 1,
    `handler_param` = '{"limit":10}', `cron_expression` = '0 */1 * * * ?',
    `retry_count` = 2, `retry_interval` = 60, `monitor_timeout` = 900,
    `updater` = 'system', `update_time` = NOW()
WHERE `deleted` = b'0' AND `handler_name` = 'cpsOrderSyncBatchExecutionJob';

INSERT INTO `infra_job` (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,`monitor_timeout`,`creator`,`updater`,`deleted`)
SELECT 'CPS订单补偿批次执行', 1, 'cpsOrderSyncBatchExecutionJob', '{"limit":10}', '0 */1 * * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='cpsOrderSyncBatchExecutionJob' AND `deleted`=b'0');

-- ============================================================
-- 修改时间：2026-09-01 11:00:00
-- 目的：注册 CPS 返利结算任务，确保平台已结算订单进入返利冻结流程。
-- ============================================================
UPDATE `infra_job`
SET `name` = 'CPS返利结算', `status` = 1,
    `handler_param` = '{"batchSize":200}', `cron_expression` = '0 0 * * * ?',
    `retry_count` = 2, `retry_interval` = 60, `monitor_timeout` = 900,
    `updater` = 'system', `update_time` = NOW()
WHERE `deleted` = b'0' AND `handler_name` = 'cpsRebateSettleJob';

INSERT INTO `infra_job` (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,`monitor_timeout`,`creator`,`updater`,`deleted`)
SELECT 'CPS返利结算', 1, 'cpsRebateSettleJob', '{"batchSize":200}', '0 0 * * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='cpsRebateSettleJob' AND `deleted`=b'0');

-- ============================================================
-- 修改时间：2026-09-01 19:29:26
-- 目的：支持按实际返利金额阈值决定冻结，小额返利可直接入账。
-- ============================================================
ALTER TABLE `cps_rebate_config`
  ADD COLUMN `freeze_threshold_amount` decimal(10,2) NULL DEFAULT NULL COMMENT '冻结阈值（元，实际返利大于该金额才冻结，空或0表示不冻结）' AFTER `min_rebate_amount`,
  ADD COLUMN `freeze_days` int NULL DEFAULT NULL COMMENT '冻结天数（配置冻结阈值时必填）' AFTER `freeze_threshold_amount`;

-- ============================================================
-- 修改时间：2026-09-02 15:03:08
-- 目的：注册 CPS 返利冻结到期自动解冻任务，闭合“冻结 -> 实际返利”生命周期。
-- ============================================================
UPDATE `infra_job`
SET `name` = 'CPS返利冻结自动解冻', `status` = 1,
    `handler_param` = '{"batchSize":500}', `cron_expression` = '0 0/10 * * * ?',
    `retry_count` = 2, `retry_interval` = 60, `monitor_timeout` = 900,
    `updater` = 'system', `update_time` = NOW()
WHERE `deleted` = b'0' AND `handler_name` = 'cpsFreezeUnfreezeJob';

INSERT INTO `infra_job` (`name`,`status`,`handler_name`,`handler_param`,`cron_expression`,`retry_count`,`retry_interval`,`monitor_timeout`,`creator`,`updater`,`deleted`)
SELECT 'CPS返利冻结自动解冻', 1, 'cpsFreezeUnfreezeJob', '{"batchSize":500}', '0 0/10 * * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (SELECT 1 FROM `infra_job` WHERE `handler_name`='cpsFreezeUnfreezeJob' AND `deleted`=b'0');

-- ============================================================
-- 修改时间：2026-09-03 00:00:00
-- 目的：记录资产V2发布B迁移核验的变更单/审批单号，完善受控启用审计链路。
-- ============================================================
ALTER TABLE `cps_rebate_asset_policy`
  ADD COLUMN `migration_approval_ref` varchar(128) DEFAULT NULL COMMENT '发布B变更单/审批单号' AFTER `ready_check_time`;

-- ============================================================
-- 修改时间：2026-09-03 12:33:02
-- 目的：统一由冻结管理按返利金额配置冻结规则，默认满10元冻结7天。
-- ============================================================
UPDATE `cps_freeze_config`
SET `min_amount_cent` = 1000,
    `unfreeze_days` = 7,
    `remark` = '全平台返利满10元冻结，资格时间后7天解冻',
    `update_time` = NOW()
WHERE `deleted` = b'0'
  AND `platform_code` IS NULL
  AND (`min_amount_cent` IS NULL OR `min_amount_cent` = 0)
  AND `max_amount_cent` IS NULL
  AND `unfreeze_days` = 15
  AND `remark` LIKE '%默认配置%';

-- ============================================================
-- 修改时间：2026-09-03 15:00:00
-- 目的：移除独立 CPS 数据统计菜单，统计内容统一展示在首页。
-- ============================================================
UPDATE `system_menu`
SET `visible` = b'0', `updater` = '1', `update_time` = NOW()
WHERE `id` IN (6249, 6250) AND `deleted` = b'0';

-- 修改时间：2026-09-18 16:49:04
-- AI capability audit metadata; no request/response body copied.
ALTER TABLE cps_mcp_access_log
 ADD COLUMN `role_code` varchar(64) NULL,
 ADD COLUMN `role_version` int NULL,
 ADD COLUMN `skill_code` varchar(64) NULL,
 ADD COLUMN `skill_version` int NULL,
 ADD COLUMN `policy_result` varchar(32) NULL,
 ADD COLUMN `reject_reason` varchar(128) NULL;

-- ============================================================
-- 修改时间：2026-10-16 14:50:00
-- 目的：创建全系统数据库结构版本与更新时间追踪表（system_schema_version）及 cps 兼容视图，记录当前结构版本与更新时间。
CREATE OR REPLACE VIEW `cps_schema_version` AS SELECT * FROM `system_schema_version` WHERE `module` = 'cps';

-- ============================================================

INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.0', '2026-10-16 14:50:00', 'cps-update.sql', '增加数据库结构版本与更新时间追踪表 system_schema_version，确立结构时间点与增量更新规范', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.0' AND `schema_update_time` = '2026-10-16 14:50:00'
);



-- ============================================================
-- 修改时间：2026-10-22 10:48:46
-- 目的：选品库商品支持开启价格趋势跟踪功能，支持开关、定时任务定期拉取与商品详情价格趋势查看
-- ============================================================

-- 1. cps_selection_theme_item 增加价格趋势跟踪开关与最后跟踪时间
ALTER TABLE `cps_selection_theme_item`
  ADD COLUMN `price_track_status` tinyint NOT NULL DEFAULT 0 COMMENT '价格趋势跟踪：0关闭 1开启',
  ADD COLUMN `price_track_time` datetime NULL DEFAULT NULL COMMENT '最后价格跟踪抓取时间';

ALTER TABLE `cps_selection_theme_item`
  ADD INDEX `idx_cps_selection_theme_item_price_track` (`tenant_id`, `deleted`, `price_track_status`);

-- 2. cps_goods_price_snapshot 增加选品主题商品ID关联，并调整关联字段允许默认值
ALTER TABLE `cps_goods_price_snapshot`
  ADD COLUMN `selection_item_id` bigint NULL DEFAULT NULL COMMENT '选品主题商品ID' AFTER `id`,
  MODIFY COLUMN `master_id` bigint NOT NULL DEFAULT 0 COMMENT '商品主档ID',
  MODIFY COLUMN `source_mapping_id` bigint NOT NULL DEFAULT 0 COMMENT '商品来源映射ID';

ALTER TABLE `cps_goods_price_snapshot`
  ADD INDEX `idx_cps_goods_price_item` (`tenant_id`, `deleted`, `selection_item_id`, `snapshot_time`),
  ADD INDEX `idx_cps_goods_price_external` (`tenant_id`, `deleted`, `platform_code`, `external_goods_id`, `snapshot_time`);

-- 3. infra_job 增加定时拉取商品价格任务
INSERT INTO `infra_job` (`name`, `status`, `handler_name`, `handler_param`, `cron_expression`,
                         `retry_count`, `retry_interval`, `monitor_timeout`, `creator`, `updater`, `deleted`)
SELECT 'CPS选品商品价格趋势跟踪拉取', 1, 'cpsGoodsPriceTrackingJob', '{"batchSize":50}',
       '0 0 */2 * * ?', 2, 60, 900, 'system', 'system', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_job` WHERE `handler_name` = 'cpsGoodsPriceTrackingJob' AND `deleted` = b'0'
);

-- 4. 记录结构版本更新
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.1', '2026-10-22 10:48:46', 'cps-update.sql', '选品库商品支持开启价格趋势跟踪功能，支持开关、定时任务拉取与商品详情价格趋势查看', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.1' AND `schema_update_time` = '2026-10-22 10:48:46'
);


-- ============================================================
-- 修改时间：2026-10-22 12:15:00
-- 目的：CPS 三级分销推广与多级分账功能支持：
-- 1. 新增推广员关系表 cps_promoter_relation (记录会员上下级链条、反规范化直推间推、专属邀请码、团队业绩)
-- 2. 新增多级分账规则配置表 cps_split_rule (平台留存率、自购返利比例、一级提成比例、二级提成比例、合伙人级差预留、新客首单锁粉开关、归属冲突策略)
-- 3. 扩展 cps_rebate_record 支持分账角色(split_role)、出单人溯源(source_member_id)、分账规则ID与快照(split_rule_id/snapshot)
-- 4. 插入默认全平台多级分账规则种子数据
-- ============================================================

-- 1. 新增推广员关系表 cps_promoter_relation
CREATE TABLE IF NOT EXISTS `cps_promoter_relation` (
  `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id`             bigint       NOT NULL DEFAULT 0 COMMENT '租户ID',
  `member_id`             bigint       NOT NULL COMMENT '会员用户ID',
  `parent_member_id`      bigint       DEFAULT NULL COMMENT '一级直接推荐人ID',
  `grandparent_member_id` bigint       DEFAULT NULL COMMENT '二级间接推荐人ID',
  `invite_code`           varchar(32)  NOT NULL COMMENT '会员专属邀请码',
  `promoter_status`       tinyint      NOT NULL DEFAULT 1 COMMENT '分销资格状态: 0未激活/冻结 1正常有效',
  `promoter_level`        tinyint      NOT NULL DEFAULT 1 COMMENT '推广员等级: 1普通会员 2VIP推广员 3团长/合伙人',
  `bind_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定上级时间',
  `team_count_l1`         int          NOT NULL DEFAULT 0 COMMENT '一级直推下线总人数',
  `team_count_l2`         int          NOT NULL DEFAULT 0 COMMENT '二级间推下线总人数',
  `team_order_count`      int          NOT NULL DEFAULT 0 COMMENT '团队累计出单量',
  `total_rebate_cent`     bigint       NOT NULL DEFAULT 0 COMMENT '累计自购返利总额(分)',
  `total_split_cent`      bigint       NOT NULL DEFAULT 0 COMMENT '累计推广分账提成总额(分)',
  `creator`               varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`           datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_member` (`tenant_id`, `member_id`, `deleted`),
  UNIQUE KEY `uk_tenant_invite_code` (`tenant_id`, `invite_code`, `deleted`),
  KEY `idx_parent` (`tenant_id`, `parent_member_id`, `deleted`),
  KEY `idx_grandparent` (`tenant_id`, `grandparent_member_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS推广员分销关系与团队概况表';

-- 2. 新增多级分账规则配置表 cps_split_rule
CREATE TABLE IF NOT EXISTS `cps_split_rule` (
  `id`                               bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id`                        bigint        NOT NULL DEFAULT 0 COMMENT '租户ID',
  `rule_name`                        varchar(64)   NOT NULL COMMENT '规则名称',
  `platform_code`                    varchar(32)   DEFAULT NULL COMMENT '适用平台(NULL表示全平台通用)',
  `member_level_id`                  bigint        DEFAULT NULL COMMENT '适用出单人等级(NULL表示通用)',
  `platform_share_rate`              decimal(5,4)  NOT NULL DEFAULT 0.2000 COMMENT '平台留存比例(如0.20表示扣留20%)',
  `self_rebate_rate`                 decimal(5,4)  NOT NULL DEFAULT 0.6000 COMMENT '自购/出单人返利比例(占净佣金池的60%)',
  `parent_rebate_rate`               decimal(5,4)  NOT NULL DEFAULT 0.2500 COMMENT '一级直接推荐人提成比例(占净佣金池的25%)',
  `grandparent_rebate_rate`          decimal(5,4)  NOT NULL DEFAULT 0.1500 COMMENT '二级间接推荐人提成比例(占净佣金池的15%)',
  `partner_spread_rate`              decimal(5,4)  NOT NULL DEFAULT 0.0000 COMMENT '合伙人/团长级差预留',
  `attribution_priority`             tinyint       NOT NULL DEFAULT 1 COMMENT '归属冲突优先级: 1关系优先 2转链推广优先',
  `auto_lock_inviter_on_first_order` tinyint       NOT NULL DEFAULT 1 COMMENT '新客首单是否自动锁粉绑定转链推广人: 0否 1是',
  `freeze_days`                      int           NOT NULL DEFAULT 15 COMMENT '冻结结算天数(天)',
  `status`                           tinyint       NOT NULL DEFAULT 1 COMMENT '规则状态: 0停用 1启用',
  `priority`                         int           NOT NULL DEFAULT 0 COMMENT '匹配优先级(越高越优先)',
  `creator`                          varchar(64)   DEFAULT '' COMMENT '创建者',
  `create_time`                      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`                          varchar(64)   DEFAULT '' COMMENT '更新者',
  `update_time`                      datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`                          bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_rule_match` (`tenant_id`, `platform_code`, `member_level_id`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CPS多级分账规则配置表';

-- 3. cps_rebate_record 扩展分账角色、来源人、分账规则与快照
ALTER TABLE `cps_rebate_record`
  ADD COLUMN `split_role` varchar(32) NOT NULL DEFAULT 'SELF' COMMENT '分账角色: SELF(自购/自返), PARENT_L1(一级推广), GRANDPARENT_L2(二级推广), PARTNER(合伙人级差), PLATFORM(平台留存)' AFTER `rebate_type`,
  ADD COLUMN `source_member_id` bigint NULL DEFAULT NULL COMMENT '产生订单的消费者会员ID' AFTER `split_role`,
  ADD COLUMN `split_rule_id` bigint NULL DEFAULT NULL COMMENT '匹配的分账规则ID' AFTER `source_member_id`,
  ADD COLUMN `split_rule_snapshot` text NULL DEFAULT NULL COMMENT '分账比例与参数快照JSON' AFTER `split_rule_id`;

ALTER TABLE `cps_rebate_record`
  DROP INDEX `uk_tenant_order_rebate_type`,
  ADD UNIQUE KEY `uk_tenant_order_rebate_split` (`tenant_id`, `order_id`, `rebate_type`, `split_role`),
  ADD INDEX `idx_order_split` (`tenant_id`, `order_id`, `split_role`, `deleted`);

-- 4. 插入全平台默认多级分账规则种子数据 (租户 0 与 1 通用兜底)
INSERT INTO `cps_split_rule` (
  `tenant_id`, `rule_name`, `platform_code`, `member_level_id`,
  `platform_share_rate`, `self_rebate_rate`, `parent_rebate_rate`, `grandparent_rebate_rate`,
  `partner_spread_rate`, `attribution_priority`, `auto_lock_inviter_on_first_order`,
  `freeze_days`, `status`, `priority`, `creator`, `updater`, `deleted`
)
SELECT 0, '全局默认多级分账规则', NULL, NULL,
       0.2000, 0.6000, 0.2500, 0.1500,
       0.0000, 1, 1,
       15, 1, 0, 'system', 'system', b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_split_rule` WHERE `rule_name` = '全局默认多级分账规则' AND `deleted` = b'0'
);

-- 5. 记录数据库结构版本更新
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.2', '2026-10-22 12:15:00', 'cps-update.sql', 'CPS三级分销推广关系表(cps_promoter_relation)、多级分账规则配置表(cps_split_rule)、分账记录角色扩展与净佣金池瓜分引擎支持', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.2' AND `schema_update_time` = '2026-10-22 12:15:00'
);

-- ----------------------------------------------------------------------------------------------------
-- 修改时间：2026-10-22 14:45:00
-- 描述：新增会员提现账户管理表(cps_withdraw_account)与提现单安全风控字段扩展
-- ----------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `cps_withdraw_account` (
  `id`                    bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`             bigint        NOT NULL COMMENT '会员用户ID',
  `type`                  varchar(32)   NOT NULL COMMENT '账户类型（alipay:支付宝 wechat:微信 bank:银行卡）',
  `account`               varchar(128)  NOT NULL COMMENT '提现账号（支付宝账号/微信OpenID/银行卡号）',
  `account_name`          varchar(64)   NOT NULL COMMENT '真实姓名（必须与收款实名一致）',
  `bank_name`             varchar(64)            DEFAULT NULL COMMENT '开户行（银行卡时有效）',
  `is_default`            bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否默认账户（0:否 1:是）',
  `status`                tinyint       NOT NULL DEFAULT 0 COMMENT '状态（0:正常 1:禁用）',
  `last_used_time`        datetime               DEFAULT NULL COMMENT '最近使用时间',
  `creator`               varchar(64)            DEFAULT NULL COMMENT '创建人',
  `create_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`               varchar(64)            DEFAULT NULL COMMENT '更新人',
  `update_time`           datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`             bigint        NOT NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`),
  KEY `idx_member_tenant` (`tenant_id`, `member_id`, `type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员提现账户管理表';

ALTER TABLE `cps_withdraw`
  ADD COLUMN `account_id` bigint NULL DEFAULT NULL COMMENT '关联会员提现账户ID' AFTER `withdraw_account_name`,
  ADD COLUMN `risk_level` tinyint NOT NULL DEFAULT 0 COMMENT '风控级别: 0正常自动 1待人工审核 2风控拦截' AFTER `review_note`,
  ADD COLUMN `client_ip` varchar(64) NULL DEFAULT NULL COMMENT '申请客户端IP' AFTER `idempotency_key`,
  ADD COLUMN `user_agent` varchar(512) NULL DEFAULT NULL COMMENT '客户端UserAgent' AFTER `client_ip`;

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.3', '2026-10-22 14:45:00', 'cps-update.sql', '新增会员提现账户管理表(cps_withdraw_account)与提现安全风控审计支持', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.3' AND `schema_update_time` = '2026-10-22 14:45:00'
);

-- ============================================================
-- 修改时间：2026-10-26 11:00:00
-- 目的：优化选品商品价格存储，仅价格变动时新增快照记录，不变动时顺延记录价格维持时间段（end_time）
-- 1. cps_goods_price_snapshot 增加 end_time (价格维持截止时间)
-- ============================================================

ALTER TABLE `cps_goods_price_snapshot`
  ADD COLUMN `end_time` datetime NULL DEFAULT NULL COMMENT '价格维持截止时间（未变动时的最近确认时间）' AFTER `snapshot_time`;

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.4', '2026-10-26 11:00:00', 'cps-update.sql', '优化选品商品价格存储，仅价格变动时新增快照记录，不变动时顺延记录价格维持时间段（end_time）', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.4' AND `schema_update_time` = '2026-10-26 11:00:00'
);



-- ============================================================
-- 修改时间：2026-10-26 11:53:00
-- 目的：初始化抖音联盟平台与官方 OpenAPI 供应商配置（支持精选联盟选品、抖客转链与订单同步）
-- 说明：插入 douyin 平台与 official 供应商基础配置，如已存在则跳过，可安全重复执行
-- ============================================================
INSERT INTO `cps_platform` (`platform_code`, `platform_name`, `platform_logo`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `platform_service_rate`, `sort`, `status`, `extra_config`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `active_vendor_code`, `supported_vendors`)
SELECT 'douyin', '抖音联盟', NULL, NULL, NULL, 'https://openapi-fxg.jinritemai.com', NULL, NULL, 0.0000, 80, 0, NULL, '请先在抖店开放平台创建应用并配置官方 AppKey、AppSecret，账号具备抖客/达人权限后再启用', 'system', '2026-10-26 11:53:00', 'system', '2026-10-26 11:53:00', b'0', 1, 'official', 'official'
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_platform`
  WHERE `tenant_id` = 1 AND `platform_code` = 'douyin' AND `deleted` = b'0'
);

UPDATE `cps_platform`
 SET `supported_vendors` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM COALESCE(`supported_vendors`, '')), ''), 'official'),
     `update_time` = '2026-10-26 11:53:00'
 WHERE `tenant_id` = 1
   AND `platform_code` = 'douyin'
   AND `deleted` = b'0'
   AND FIND_IN_SET('official', REPLACE(COALESCE(`supported_vendors`, ''), ' ', '')) = 0;

INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'official', '抖音联盟官方', 'official', 'douyin', '', '', 'https://openapi-fxg.jinritemai.com', NULL, NULL, '{"searchPath":"/buyin/kolMaterialsProductsSearch","convertPath":"/buyin/doukeCommandParseAndShare","orderPath":"/buyin/doukeOrderList"}', 100, 0, '抖音电商官方 OpenAPI 接入，支持精选联盟选品、抖客转链与订单同步', 'system', '2026-10-26 11:53:00', 'system', '2026-10-26 11:53:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'official' AND `platform_code` = 'douyin' AND `deleted` = b'0'
);

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.5', '2026-10-26 11:53:00', 'cps-update.sql', '初始化抖音联盟官方OpenAPI平台配置与供应商接入元数据', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.5' AND `schema_update_time` = '2026-10-26 11:53:00'
);

-- ============================================================
-- 修改时间：2026-10-26 12:00:00
-- 目的：初始化美团联盟官方平台配置与API Gateway供应商接入元数据（支持外卖/闪购/优选/到店卡券搜索、转链与订单同步）
-- 说明：插入 meituan 平台与 official 供应商基础配置，如已存在则跳过，可安全重复执行
-- ============================================================
INSERT INTO `cps_platform` (`platform_code`, `platform_name`, `platform_logo`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `platform_service_rate`, `sort`, `status`, `extra_config`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `active_vendor_code`, `supported_vendors`)
SELECT 'meituan', '美团联盟', NULL, NULL, NULL, 'https://openapi.meituan.com', NULL, NULL, 0.0000, 85, 0, NULL, '美团联盟官方API Gateway接入，请配置服务商appKey、appSecret与默认推广位SID后再启用', 'system', '2026-10-26 12:00:00', 'system', '2026-10-26 12:00:00', b'0', 1, 'official', 'official'
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_platform`
  WHERE `tenant_id` = 1 AND `platform_code` = 'meituan' AND `deleted` = b'0'
);

UPDATE `cps_platform`
 SET `supported_vendors` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM COALESCE(`supported_vendors`, '')), ''), 'official'),
     `update_time` = '2026-10-26 12:00:00'
 WHERE `tenant_id` = 1
   AND `platform_code` = 'meituan'
   AND `deleted` = b'0'
   AND FIND_IN_SET('official', REPLACE(COALESCE(`supported_vendors`, ''), ' ', '')) = 0;

INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'official', '美团联盟官方', 'official', 'meituan', '', '', 'https://openapi.meituan.com', NULL, NULL, '{"searchPath":"/cps_open/common/api/v1/query_coupon","convertPath":"/cps_open/common/api/v1/get_referral_link","orderPath":"/cps_open/common/api/v1/query_order","platform":1,"bizLine":1,"linkType":1}', 100, 0, '美团开放平台官方 API Gateway (Shepherd鉴权) 接入，支持外卖/闪购/优选/到店卡券搜索、转链与订单同步', 'system', '2026-10-26 12:00:00', 'system', '2026-10-26 12:00:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'official' AND `platform_code` = 'meituan' AND `deleted` = b'0'
);

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.6', '2026-10-26 12:00:00', 'cps-update.sql', '初始化美团联盟官方平台配置与API Gateway供应商接入元数据', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.6' AND `schema_update_time` = '2026-10-26 12:00:00'
);


-- ============================================================
-- 修改时间：2026-10-26 17:10:00
-- 目的：初始化穿山甲电商联盟（CSJ/Pangolin）抖音电商CPS供应商接入元数据
-- 说明：更新 douyin 平台的 supported_vendors 增加 csj，并插入 csj 供应商基础配置（基于 /order/search 与 MD5 签名机制）
-- ============================================================
UPDATE `cps_platform`
 SET `supported_vendors` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM COALESCE(`supported_vendors`, '')), ''), 'csj'),
     `update_time` = '2026-10-26 17:10:00'
 WHERE `tenant_id` = 1
   AND `platform_code` = 'douyin'
   AND `deleted` = b'0'
   AND FIND_IN_SET('csj', REPLACE(COALESCE(`supported_vendors`, ''), ' ', '')) = 0;

INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'csj', '穿山甲电商联盟', 'aggregator', 'douyin', '', '', 'https://ecom.pangolin-sdk-toutiao.com', NULL, NULL, '{"orderSearchPath":"/order/search","productSearchPath":"/product/search","productLinkPath":"/product/link","signType":"MD5"}', 90, 0, '穿山甲电商联盟（抖音电商CPS/CSJ平台）接入，基于order/search接口与MD5签名机制', 'system', '2026-10-26 17:10:00', 'system', '2026-10-26 17:10:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'csj' AND `platform_code` = 'douyin' AND `deleted` = b'0'
);

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.7', '2026-10-26 17:10:00', 'cps-update.sql', '初始化穿山甲电商联盟（CSJ）抖音电商CPS供应商接入元数据', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.7' AND `schema_update_time` = '2026-10-26 17:10:00'
);


-- ============================================================
-- 修改时间：2026-10-27 09:45:00
-- 目的：修复美团官方与穿山甲抖音官方活动主图失效URL，更新为有效活动主图
-- 说明：更新 cps_rebate_activity 中美团官方和穿山甲抖音官方活动的主图链接
-- ============================================================
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p1.meituan.net/marketingcpsmedia/9e4470f4ed627e3b9001a6155ae3bd1490318.jpg', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:33';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p0.meituan.net/marketingcpsmedia/efefc824b711b0df5c7b9d703daf87c2146349.jpg', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:4';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p1.meituan.net/marketingcpsmedia/82ee55b87a75745c73c6f2f472b0c543121899.png', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:2';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p0.meituan.net/marketingcpsmedia/d3ddbe27664ab7e6a0ea20b815c2f6a0142667.jpg', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:22';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p0.meituan.net/marketingcpsmedia/f34299e8f679c3cb7e9f8d652f3311f0113094.jpg', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:43';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p0.meituan.net/marketingcpsmedia/e800ff3136dfcba36c5a8a1f3689aabe241160.png', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:7';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://p1.meituan.net/marketingcpsmedia/755ed8475934cd8a843e57088752160e180396.jpg', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'official' AND `external_activity_id` = 'mt:act:51';

UPDATE `cps_rebate_activity` SET `main_pic` = 'https://img-haodanku-com.cdn.fudaiapp.com/haodanku/powerful_page/385mT1zdYYf_jgs7jdBDpYuxYXDh', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'csj' AND `external_activity_id` = 'csj:mat:1001';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://img-haodanku-com.cdn.fudaiapp.com/haodanku/powerful_page/7wqk9IFqJDm_PXo6IyTW4Zyvsnjq', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'csj' AND `external_activity_id` = 'csj:mat:1002';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://img-haodanku-com.cdn.fudaiapp.com/haodanku/powerful_page/MN2STQUjxz0_UmbxkgaLdQyFMpxK', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'csj' AND `external_activity_id` = 'csj:mat:1003';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://img-haodanku-com.cdn.fudaiapp.com/haodanku/powerful_page/uqSgCIuT1cZ_heUVw1FtbFvoDO4g', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'csj' AND `external_activity_id` = 'csj:mat:1004';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://img-haodanku-com.cdn.fudaiapp.com/haodanku/powerful_page/MItOh6F3bXy_szzXbmwZHacPU2iP', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'csj' AND `external_activity_id` = 'csj:mat:1005';
UPDATE `cps_rebate_activity` SET `main_pic` = 'https://img-haodanku-com.cdn.fudaiapp.com/haodanku/powerful_page/CNO5Xfwaxvd_8K5YO3FXOsotr3L5', `update_time` = '2026-10-27 09:45:00' WHERE `source_type` = 'csj' AND `external_activity_id` = 'csj:mat:1006';

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.8', '2026-10-27 09:45:00', 'cps-update.sql', '修复美团官方与穿山甲抖音官方活动主图失效URL', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.8' AND `schema_update_time` = '2026-10-27 09:45:00'
);


-- ============================================================
-- 修改时间：2026-10-27 10:00:00
-- 目的：清洗聚推客活动奖励中包含的 HTML 标签，更新为真实的佣金率描述
-- 说明：更新 cps_rebate_activity 中聚推客活动奖励字段，去除富文本样式残留
-- ============================================================
UPDATE `cps_rebate_activity`
SET `rebate_desc` = '以实际转链佣金为准',
    `update_time` = '2026-10-27 10:00:00'
WHERE `source_type` = 'jutuike'
  AND (`rebate_desc` LIKE '%<%' OR `rebate_desc` LIKE '%style=%');

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.9', '2026-10-27 10:00:00', 'cps-update.sql', '清洗聚推客活动奖励字段HTML标签并同步佣金率', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.9' AND `schema_update_time` = '2026-10-27 10:00:00'
);

-- ============================================================
-- 修改时间：2026-10-28 11:35:00
-- 目的：初始化淘宝闪购联盟（eleme）平台配置与官方 TOP OpenAPI 供应商接入元数据（支持官方常驻活动转链、单店推广与正向订单同步）
-- 说明：插入 eleme 平台与 official、haodanku 供应商基础配置，初始化官方常驻活动物料，可安全重复执行
-- ============================================================
INSERT INTO `cps_platform` (`platform_code`, `platform_name`, `platform_logo`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `platform_service_rate`, `sort`, `status`, `extra_config`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `active_vendor_code`, `supported_vendors`)
SELECT 'eleme', '淘宝闪购', 'https://img.alicdn.com/imgextra/i3/O1CN01fUvOcm1fM8VwB8P3C_!!6000000003986-2-tps-750-375.png', NULL, NULL, 'https://eco.taobao.com/router/rest', NULL, NULL, 0.0000, 90, 0, NULL, '阿里本地生活淘宝闪购联盟官方TOP OpenAPI接入，支持常驻官方活动、单店推广转链、导购选品与正向订单同步', 'system', '2026-10-28 11:35:00', 'system', '2026-10-28 11:35:00', b'0', 1, 'official', 'official,haodanku'
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_platform`
  WHERE `tenant_id` = 1 AND `platform_code` = 'eleme' AND `deleted` = b'0'
);

UPDATE `cps_platform`
 SET `supported_vendors` = CONCAT_WS(',', NULLIF(TRIM(BOTH ',' FROM COALESCE(`supported_vendors`, '')), ''), 'official'),
     `update_time` = '2026-10-28 11:35:00'
 WHERE `tenant_id` = 1
   AND `platform_code` = 'eleme'
   AND `deleted` = b'0'
   AND FIND_IN_SET('official', REPLACE(COALESCE(`supported_vendors`, ''), ' ', '')) = 0;

INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'official', '淘宝闪购联盟官方', 'official', 'eleme', '', '', 'https://eco.taobao.com/router/rest', NULL, NULL, '{"gatewayUrl":"https://eco.taobao.com/router/rest","officialActivityMethod":"alibaba.alsc.union.eleme.promotion.officialactivity.get","storePromotionMethod":"alibaba.alsc.union.eleme.promotion.storepromotion.get","storeQueryMethod":"alibaba.alsc.union.eleme.promotion.storepromotion.query","orderGetMethod":"alibaba.alsc.union.kbcpx.positive.order.get"}', 100, 0, '阿里本地生活淘宝闪购联盟官方 TOP OpenAPI 接入，支持MD5字典序签名、常驻活动推广、单店推广与正向订单明细同步', 'system', '2026-10-28 11:35:00', 'system', '2026-10-28 11:35:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'official' AND `platform_code` = 'eleme' AND `deleted` = b'0'
);

INSERT INTO `cps_api_vendor` (`vendor_code`, `vendor_name`, `vendor_type`, `platform_code`, `app_key`, `app_secret`, `api_base_url`, `auth_token`, `default_adzone_id`, `extra_config`, `priority`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'haodanku', '好单库淘宝闪购', 'aggregator', 'eleme', '', '', 'https://v3.api.haodanku.com', NULL, NULL, '{"convertPath":"/elm_activity_ratesurl","orderPath":"/get_elm_order"}', 90, 0, '好单库聚合供应商淘宝闪购接入，作为官方API的容灾备选', 'system', '2026-10-28 11:35:00', 'system', '2026-10-28 11:35:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'haodanku' AND `platform_code` = 'eleme' AND `deleted` = b'0'
);

-- 初始化淘宝闪购官方常驻推广活动
INSERT INTO `cps_rebate_activity` (`activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `promotion_activity_id`, `vendor_metadata`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT '淘宝闪购天天领红包', '外卖闪购', 'eleme', 'https://img.alicdn.com/imgextra/i3/O1CN01fUvOcm1fM8VwB8P3C_!!6000000003986-2-tps-750-375.png', '淘宝闪购天天领大额红包，全国流量适用', '天天领大额红包，享闪购CPS高额返佣', 'CPS', 0, 'official', 'eleme:official:10144', '10144', '{"activityId":"10144","trafficType":"全国流量"}', '全国流量', 'url', '', '淘宝闪购', 91, 1, '2026-01-01 00:00:00', '2028-12-31 23:59:59', '官方常驻在线活动', 'system', '2026-10-28 11:35:00', 'system', '2026-10-28 11:35:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_rebate_activity`
  WHERE `tenant_id` = 1 AND `source_type` = 'official' AND `external_activity_id` = 'eleme:official:10144' AND `deleted` = b'0'
);

INSERT INTO `cps_rebate_activity` (`activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `promotion_activity_id`, `vendor_metadata`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT '浏览店铺做任务', '任务领券', 'eleme', 'https://img.alicdn.com/imgextra/i2/O1CN01s2Zc641H0H5k3Uo9m_!!6000000000699-2-tps-750-375.png', '浏览店铺做任务领红包', '浏览品牌门店领任务红包', 'CPS', 0, 'official', 'eleme:official:10174', '10174', '{"activityId":"10174","trafficType":"全国流量"}', '全国流量', 'url', '', '做任务领红包', 92, 1, '2026-01-01 00:00:00', '2028-12-31 23:59:59', '官方常驻在线活动', 'system', '2026-10-28 11:35:00', 'system', '2026-10-28 11:35:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_rebate_activity`
  WHERE `tenant_id` = 1 AND `source_type` = 'official' AND `external_activity_id` = 'eleme:official:10174' AND `deleted` = b'0'
);

INSERT INTO `cps_rebate_activity` (`activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `promotion_activity_id`, `vendor_metadata`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT '零售专属会场', '超市零售', 'eleme', 'https://img.alicdn.com/imgextra/i4/O1CN01dE6gY81XvD2wYf6Z6_!!6000000002985-2-tps-750-375.png', '超市便利、生鲜水果零售专属会场', '超市便利、水果生鲜零售会场', 'CPS', 0, 'official', 'eleme:official:10183', '10183', '{"activityId":"10183","trafficType":"全国流量"}', '全国流量', 'url', '', '超市零售', 93, 1, '2026-01-01 00:00:00', '2028-12-31 23:59:59', '官方常驻在线活动', 'system', '2026-10-28 11:35:00', 'system', '2026-10-28 11:35:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_rebate_activity`
  WHERE `tenant_id` = 1 AND `source_type` = 'official' AND `external_activity_id` = 'eleme:official:10183' AND `deleted` = b'0'
);

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.10', '2026-10-28 11:35:00', 'cps-update.sql', '初始化淘宝闪购联盟官方OpenAPI平台配置与SID会员归因', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.10' AND `schema_update_time` = '2026-10-28 11:35:00'
);

-- ============================================================
-- 修改时间：2026-10-28 17:15:00
-- 目的：统一饿了么业务命名为淘宝闪购，更新平台、供应商、历史活动及转链记录名称
-- 说明：将平台配置中心及活动中饿了么相关词汇统一变更为淘宝闪购，并同步结构版本号
-- ============================================================
UPDATE `cps_platform`
SET `platform_name` = '淘宝闪购',
    `remark` = REPLACE(`remark`, '饿了么', '淘宝闪购'),
    `update_time` = '2026-10-28 17:15:00'
WHERE `platform_code` = 'eleme';

UPDATE `cps_api_vendor`
SET `vendor_name` = REPLACE(`vendor_name`, '饿了么', '淘宝闪购'),
    `remark` = REPLACE(`remark`, '饿了么', '淘宝闪购'),
    `update_time` = '2026-10-28 17:15:00'
WHERE `platform_code` = 'eleme';

UPDATE `cps_rebate_activity`
SET `activity_name` = REPLACE(`activity_name`, '饿了么', '淘宝闪购'),
    `search_keyword` = REPLACE(`search_keyword`, '饿了么', '淘宝闪购'),
    `short_desc` = REPLACE(`short_desc`, '饿了么', '淘宝闪购'),
    `rebate_desc` = REPLACE(`rebate_desc`, '饿了么', '淘宝闪购'),
    `remark` = REPLACE(`remark`, '饿了么', '淘宝闪购'),
    `update_time` = '2026-10-28 17:15:00'
WHERE `activity_name` LIKE '%饿了么%'
   OR `search_keyword` LIKE '%饿了么%'
   OR `short_desc` LIKE '%饿了么%'
   OR `rebate_desc` LIKE '%饿了么%'
   OR `remark` LIKE '%饿了么%'
   OR `platform_code` = 'eleme';

UPDATE `cps_transfer_record`
SET `item_title` = REPLACE(`item_title`, '饿了么', '淘宝闪购'),
    `update_time` = '2026-10-28 17:15:00'
WHERE `item_title` LIKE '%饿了么%';

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.11', '2026-10-28 17:15:00', 'cps-update.sql', '统一饿了么业务命名为淘宝闪购并更新平台与活动文案', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.11' AND `schema_update_time` = '2026-10-28 17:15:00'
);

-- ============================================================
-- 修改时间：2026-10-28 21:55:00
-- 目的：初始化唯品会开放平台（VOP）官方OpenAPI供应商配置结构（安全脱敏规范）
-- 说明：增加唯品会官方API供应商（official），密钥留空占位由运维环境直接配置或环境变量注入
-- ============================================================
UPDATE `cps_platform`
SET `supported_vendors` = 'official,haodanku',
    `active_vendor_code` = 'official',
    `api_base_url` = 'https://vop.vipapis.com/',
    `default_adzone_id` = '85815982c2a2ec438c94cea28166e505',
    `status` = 1,
    `remark` = '唯品会开放平台（VOP）官方SDK与API直连接入，支持商品检索、高佣转链与订单同步',
    `updater` = 'system',
    `update_time` = '2026-10-28 21:55:00'
WHERE `tenant_id` = 1 AND `platform_code` = 'vip' AND `deleted` = b'0';

INSERT INTO `cps_api_vendor` (
  `vendor_code`, `vendor_name`, `vendor_type`, `platform_code`,
  `app_key`, `app_secret`, `api_base_url`, `auth_token`,
  `default_adzone_id`, `extra_config`, `priority`, `status`,
  `remark`, `creator`, `create_time`, `updater`, `update_time`,
  `deleted`, `tenant_id`
)
SELECT
  'official', '唯品会联盟官方', 'official', 'vip',
  '', '', 'https://vop.vipapis.com/', NULL,
  '85815982c2a2ec438c94cea28166e505',
  '{"goodsService":"com.vip.adp.api.open.service.UnionGoodsV2Service","urlService":"com.vip.adp.api.open.service.UnionUrlV2Service","orderService":"com.vip.adp.api.open.service.UnionOrderV2Service","pidService":"com.vip.adp.api.open.service.UnionPidV2Service"}',
  100, 1,
  '唯品会开放平台官方Java SDK 2.0直连接入，支持HMAC-MD5签名、商品搜索、推广转链、PID推广位管理与订单同步',
  'system', '2026-10-28 21:55:00', 'system', '2026-10-28 21:55:00',
  b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_api_vendor`
  WHERE `tenant_id` = 1 AND `vendor_code` = 'official' AND `platform_code` = 'vip' AND `deleted` = b'0'
);

UPDATE `cps_adzone`
SET `is_default` = 0, `updater` = 'system', `update_time` = '2026-10-28 21:55:00'
WHERE `tenant_id` = 1 AND `platform_code` = 'vip';

INSERT INTO `cps_adzone` (`platform_code`, `adzone_id`, `adzone_name`, `adzone_type`, `is_default`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'vip', '85815982c2a2ec438c94cea28166e505', '新火支付小位', 'general', 1, 1, 'system', '2026-10-28 21:55:00', 'system', '2026-10-28 21:55:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_adzone` WHERE `tenant_id` = 1 AND `platform_code` = 'vip' AND `adzone_id` = '85815982c2a2ec438c94cea28166e505' AND `deleted` = b'0'
);

INSERT INTO `cps_adzone` (`platform_code`, `adzone_id`, `adzone_name`, `adzone_type`, `is_default`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'vip', '7712cd93f3c5536bf14f64984cb47d83', '新火app位', 'general', 0, 1, 'system', '2026-10-28 21:55:00', 'system', '2026-10-28 21:55:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_adzone` WHERE `tenant_id` = 1 AND `platform_code` = 'vip' AND `adzone_id` = '7712cd93f3c5536bf14f64984cb47d83' AND `deleted` = b'0'
);

INSERT INTO `cps_adzone` (`platform_code`, `adzone_id`, `adzone_name`, `adzone_type`, `is_default`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'vip', '31469c34447de21261b00e7a0152d5a2', '新火推广', 'general', 0, 1, 'system', '2026-10-28 21:55:00', 'system', '2026-10-28 21:55:00', b'0', 1
WHERE NOT EXISTS (
  SELECT 1 FROM `cps_adzone` WHERE `tenant_id` = 1 AND `platform_code` = 'vip' AND `adzone_id` = '31469c34447de21261b00e7a0152d5a2' AND `deleted` = b'0'
);

-- 记录数据库结构版本号
INSERT INTO `system_schema_version` (`module`, `version`, `schema_update_time`, `script_source`, `description`, `status`, `tenant_id`, `deleted`, `creator`, `updater`)
SELECT 'cps', 'v2.1.12', '2026-10-28 21:55:00', 'cps-update.sql', '初始化唯品会开放平台（VOP）官方OpenAPI供应商配置与推广位PID绑定', 'SUCCESS', 0, b'0', 'system', 'system'
WHERE NOT EXISTS (
  SELECT 1 FROM `system_schema_version` WHERE `module` = 'cps' AND `version` = 'v2.1.12' AND `schema_update_time` = '2026-10-28 21:55:00'
);
