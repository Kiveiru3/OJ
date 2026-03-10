USE onlinejudge;

-- Add missing create_time for old contest_participant schema.
SET @db_name = DATABASE();
SET @has_col = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'contest_participant'
    AND COLUMN_NAME = 'create_time'
);

SET @sql = IF(
  @has_col = 0,
  'ALTER TABLE `contest_participant` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT ''contest_participant.create_time already exists'' AS msg'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

