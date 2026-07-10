-- ============================================================
-- AI 模块 - 对话持久身份与角色会员可见性增量更新
-- 修改时间：2026-07-10 16:00:00
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `ai_chat_conversation`
  ADD COLUMN `owner_user_type` varchar(16) NULL DEFAULT NULL COMMENT '所有者用户类型（ADMIN/MEMBER）' AFTER `user_id`,
  ADD COLUMN `member_id` bigint NULL DEFAULT NULL COMMENT '会员编号' AFTER `owner_user_type`,
  ADD COLUMN `chat_mode` varchar(32) NULL DEFAULT NULL COMMENT '对话模式（STANDARD/SELF_MCP_TEST）' AFTER `member_id`,
  ADD COLUMN `mcp_client_name` varchar(128) NULL DEFAULT NULL COMMENT 'MCP Client名称' AFTER `chat_mode`,
  ADD COLUMN `allow_mutation` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许执行变更类工具' AFTER `mcp_client_name`,
  ADD COLUMN `identity_bound_time` datetime NULL DEFAULT NULL COMMENT '身份绑定时间' AFTER `allow_mutation`;

UPDATE `ai_chat_conversation`
SET `owner_user_type` = 'ADMIN'
WHERE `owner_user_type` IS NULL OR `owner_user_type` = '';

UPDATE `ai_chat_conversation`
SET `chat_mode` = 'STANDARD'
WHERE `chat_mode` IS NULL OR `chat_mode` = '';

ALTER TABLE `ai_chat_conversation`
  MODIFY COLUMN `owner_user_type` varchar(16) NOT NULL DEFAULT 'ADMIN' COMMENT '所有者用户类型（ADMIN/MEMBER）',
  MODIFY COLUMN `chat_mode` varchar(32) NOT NULL DEFAULT 'STANDARD' COMMENT '对话模式（STANDARD/SELF_MCP_TEST）',
  ADD KEY `idx_ai_chat_conversation_owner` (`tenant_id`, `owner_user_type`, `user_id`, `deleted`) USING BTREE;

ALTER TABLE `ai_chat_role`
  ADD COLUMN `member_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否允许会员使用' AFTER `public_status`;

SET FOREIGN_KEY_CHECKS = 1;
