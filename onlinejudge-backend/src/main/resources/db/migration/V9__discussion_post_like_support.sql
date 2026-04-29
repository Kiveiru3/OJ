-- Add like support for discussion posts and keep ordering fields compatible.

SET @db_name = DATABASE();

SET @has_like_count = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND COLUMN_NAME = 'like_count'
);
SET @sql = IF(
  @has_like_count = 0,
  'ALTER TABLE `discussion_post` ADD COLUMN `like_count` INT NOT NULL DEFAULT 0 AFTER `view_count`',
  'SELECT ''discussion_post.like_count exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `discussion_post_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_discussion_post_like` (`post_id`, `user_id`),
  KEY `idx_discussion_post_like_user` (`user_id`),
  KEY `idx_discussion_post_like_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_idx_discussion_recommend = (
  SELECT COUNT(1)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'discussion_post'
    AND INDEX_NAME = 'idx_discussion_like_create'
);
SET @sql = IF(
  @has_idx_discussion_recommend = 0,
  'ALTER TABLE `discussion_post` ADD KEY `idx_discussion_like_create` (`like_count`, `create_time`)',
  'SELECT ''idx_discussion_like_create exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
