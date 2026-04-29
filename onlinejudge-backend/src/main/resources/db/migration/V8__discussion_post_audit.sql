-- Discussion post moderation fields (compatible with old schemas)
SET @db_name = DATABASE();

SET @has_audit_status = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND COLUMN_NAME = 'audit_status'
);
SET @sql = IF(
  @has_audit_status = 0,
  'ALTER TABLE `discussion_post` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT ''0-pending,1-approved,2-rejected'' AFTER `view_count`',
  'SELECT ''discussion_post.audit_status already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_audit_user = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND COLUMN_NAME = 'audit_user_id'
);
SET @sql = IF(
  @has_audit_user = 0,
  'ALTER TABLE `discussion_post` ADD COLUMN `audit_user_id` BIGINT NULL AFTER `audit_status`',
  'SELECT ''discussion_post.audit_user_id already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_audit_remark = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND COLUMN_NAME = 'audit_remark'
);
SET @sql = IF(
  @has_audit_remark = 0,
  'ALTER TABLE `discussion_post` ADD COLUMN `audit_remark` VARCHAR(300) NULL AFTER `audit_user_id`',
  'SELECT ''discussion_post.audit_remark already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_audit_time = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND COLUMN_NAME = 'audit_time'
);
SET @sql = IF(
  @has_audit_time = 0,
  'ALTER TABLE `discussion_post` ADD COLUMN `audit_time` DATETIME NULL AFTER `audit_remark`',
  'SELECT ''discussion_post.audit_time already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_audit_idx = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND INDEX_NAME = 'idx_discussion_audit_status'
);
SET @sql = IF(
  @has_audit_idx = 0,
  'CREATE INDEX `idx_discussion_audit_status` ON `discussion_post` (`audit_status`)',
  'SELECT ''idx_discussion_audit_status already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

