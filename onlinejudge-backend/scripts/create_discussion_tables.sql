USE onlinejudge;

CREATE TABLE IF NOT EXISTS `discussion_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `content` TEXT NOT NULL,
  `problem_id` BIGINT NULL,
  `view_count` INT NOT NULL DEFAULT 0,
  `audit_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-pending,1-approved,2-rejected',
  `audit_user_id` BIGINT NULL,
  `audit_remark` VARCHAR(300) NULL,
  `audit_time` DATETIME NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_discussion_user` (`user_id`),
  KEY `idx_discussion_problem` (`problem_id`),
  KEY `idx_discussion_audit_status` (`audit_status`),
  KEY `idx_discussion_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `discussion_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `parent_comment_id` BIGINT NULL,
  `content` TEXT NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_post` (`post_id`),
  KEY `idx_comment_user` (`user_id`),
  KEY `idx_comment_parent` (`parent_comment_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Compatibility for old schema without parent_comment_id
SET @db_name = DATABASE();
SET @has_parent_col = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_comment'
    AND COLUMN_NAME = 'parent_comment_id'
);
SET @sql = IF(
  @has_parent_col = 0,
  'ALTER TABLE `discussion_comment` ADD COLUMN `parent_comment_id` BIGINT NULL AFTER `user_id`',
  'SELECT ''discussion_comment.parent_comment_id already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Compatibility for old schema without post audit columns
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
