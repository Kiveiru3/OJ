SET @db_name = DATABASE();

SET @has_phone = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'phone'
);
SET @sql = IF(
  @has_phone = 0,
  'ALTER TABLE `user` ADD COLUMN `phone` VARCHAR(20) NULL AFTER `email`',
  'SELECT ''user.phone exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_phone_index = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db_name
    AND table_name = 'user'
    AND index_name = 'uk_user_phone'
);
SET @sql = IF(
  @has_phone_index = 0,
  'CREATE UNIQUE INDEX `uk_user_phone` ON `user` (`phone`)',
  'SELECT ''uk_user_phone exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
