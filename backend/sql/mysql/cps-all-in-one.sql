-- ----------------------------
-- CPS all-in-one SQL
-- Keep CPS business tables, seed data, menus and permissions here.
-- Do not add CPS business SQL to ruoyi-vue-pro.sql.
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
  `tag_text` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '标签文案',
  `jump_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none' COMMENT '跳转类型：search/url/none',
  `jump_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '跳转地址',
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
INSERT INTO `cps_rebate_activity` (`id`, `activity_name`, `activity_type`, `platform_code`, `main_pic`, `short_desc`, `rebate_desc`, `billing_type`, `promotion_count`, `source_type`, `external_activity_id`, `tag_text`, `jump_type`, `jump_url`, `search_keyword`, `sort`, `status`, `start_time`, `end_time`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, '88VIP开通奖励(优酷版)', '会员权益', 'taobao', '', '淘宝 88VIP 会员年卡活动', '最高7元/张', 'CPA', 1405, 'configured', 'taobao-88vip-youku', '热门', 'url', '', '88VIP', 1, 1, '2025-08-30 00:00:00', '2026-12-31 23:59:59', '', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0', 1);
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
  `ai_summary` varchar(1024) DEFAULT NULL COMMENT 'AI建议摘要',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',
  `start_time` datetime DEFAULT NULL COMMENT '上线时间',
  `end_time` datetime DEFAULT NULL COMMENT '下线时间',
  `refresh_status` varchar(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '刷新状态',
  `last_refresh_time` datetime DEFAULT NULL COMMENT '最后刷新时间',
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
  KEY `idx_cps_selection_theme_page` (`tenant_id`, `deleted`, `status`, `promotion_event`, `sort`) USING BTREE,
  KEY `idx_cps_selection_theme_platform` (`tenant_id`, `deleted`, `platform_codes`, `vendor_code`) USING BTREE,
  KEY `idx_cps_selection_theme_window` (`tenant_id`, `deleted`, `status`, `start_time`, `end_time`) USING BTREE
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
  KEY `idx_cps_selection_theme_item_score` (`tenant_id`, `deleted`, `theme_id`, `recommend_score`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'CPS选品主题商品快照表' ROW_FORMAT = DYNAMIC;
COMMIT;

-- ----------------------------
-- CPS menu records
-- ----------------------------
BEGIN;
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6200, 'CPS联盟', '', 1, 70, 0, '/cps', 'ep:shopping-cart', NULL, NULL, 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6201, '活动中心', 'cps:rebate-activity:query', 2, 10, 6200, 'activity/square', 'ep:present', 'cps/activity/square/index', 'CpsRebateActivitySquare', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6202, '活动查询', 'cps:rebate-activity:query', 3, 1, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6203, '活动创建', 'cps:rebate-activity:create', 3, 2, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6204, '活动更新', 'cps:rebate-activity:update', 3, 3, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6205, '活动删除', 'cps:rebate-activity:delete', 3, 4, 6201, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6206, '返利工具箱', 'cps:toolbox:query', 2, 20, 6200, 'toolbox', 'ep:tools', 'cps/toolbox/index', 'CpsRebateToolbox', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6207, '工具箱查询', 'cps:toolbox:query', 3, 1, 6206, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6208, '工具箱转链', 'cps:toolbox:link', 3, 2, 6206, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6209, '商品返利查询', 'cps:goods-rebate-query:query', 2, 30, 6200, 'goods/rebate-query', 'ep:search', 'cps/goods/rebate-query/index', 'CpsGoodsRebateQuery', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6210, '返利查询', 'cps:goods-rebate-query:query', 3, 1, 6209, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6211, '返利商品广场', 'cps:goods-square:query', 2, 40, 6200, 'goods/square', 'ep:goods', 'cps/goods/square/index', 'CpsGoodsSquare', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6212, '商品广场查询', 'cps:goods-square:query', 3, 1, 6211, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6213, '商品广场转链', 'cps:goods-square:link', 3, 2, 6211, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-23 00:00:00', '1', '2026-05-23 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6214, '选品库', 'cps:selection-theme:query', 2, 45, 6200, 'selection/theme', 'ep:collection', 'cps/selection/theme/index', 'CpsSelectionTheme', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6215, '选品库查询', 'cps:selection-theme:query', 3, 1, 6214, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6216, '选品库创建', 'cps:selection-theme:create', 3, 2, 6214, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6217, '选品库更新', 'cps:selection-theme:update', 3, 3, 6214, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6218, '选品库删除', 'cps:selection-theme:delete', 3, 4, 6214, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6219, '选品库发布', 'cps:selection-theme:publish', 3, 5, 6214, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6220, '选品库下线', 'cps:selection-theme:offline', 3, 6, 6214, '', '', '', '', 0, b'1', b'1', b'1', '1', '2026-05-24 00:00:00', '1', '2026-05-24 00:00:00', b'0');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
