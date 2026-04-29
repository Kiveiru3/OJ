-- Add avatar field for user profile.
SET @db_name = DATABASE();

SET @has_avatar_col = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'avatar'
);

SET @sql = IF(
  @has_avatar_col = 0,
  'ALTER TABLE `user` ADD COLUMN `avatar` TEXT NULL AFTER `nickname`',
  'SELECT ''user.avatar exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
