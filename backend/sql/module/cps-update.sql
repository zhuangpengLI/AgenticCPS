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

SET FOREIGN_KEY_CHECKS = 1;
