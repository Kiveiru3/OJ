-- Discussion nested reply support + social follow/message tables.
-- Keep idempotent for compatibility with existing deployments.

SET @db_name = DATABASE();

-- discussion_comment.parent_comment_id
SET @has_parent_comment_id = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_comment'
    AND COLUMN_NAME = 'parent_comment_id'
);
SET @sql = IF(
  @has_parent_comment_id = 0,
  'ALTER TABLE `discussion_comment` ADD COLUMN `parent_comment_id` BIGINT NULL AFTER `user_id`',
  'SELECT ''discussion_comment.parent_comment_id exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_idx_comment_parent = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_comment'
    AND INDEX_NAME = 'idx_comment_parent'
);
SET @sql = IF(
  @has_idx_comment_parent = 0,
  'ALTER TABLE `discussion_comment` ADD KEY `idx_comment_parent` (`parent_comment_id`)',
  'SELECT ''idx_comment_parent exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `user_follow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `follower_id` BIGINT NOT NULL,
  `following_id` BIGINT NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`follower_id`, `following_id`),
  KEY `idx_follow_follower` (`follower_id`),
  KEY `idx_follow_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `private_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `from_user_id` BIGINT NOT NULL,
  `to_user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `read_flag` TINYINT NOT NULL DEFAULT 0,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pm_from_user` (`from_user_id`),
  KEY `idx_pm_to_user` (`to_user_id`),
  KEY `idx_pm_pair_time` (`from_user_id`, `to_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
