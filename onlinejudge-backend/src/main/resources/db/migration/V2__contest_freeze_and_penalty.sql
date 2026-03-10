-- Contest ranking freeze and configurable wrong-attempt penalty

SET @db_name = DATABASE();

SET @has_freeze_col = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'contest'
    AND COLUMN_NAME = 'scoreboard_freeze_time'
);
SET @sql = IF(
  @has_freeze_col = 0,
  'ALTER TABLE `contest` ADD COLUMN `scoreboard_freeze_time` DATETIME NULL AFTER `end_time`',
  'SELECT ''contest.scoreboard_freeze_time exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_penalty_col = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'contest'
    AND COLUMN_NAME = 'penalty_per_wrong'
);
SET @sql = IF(
  @has_penalty_col = 0,
  'ALTER TABLE `contest` ADD COLUMN `penalty_per_wrong` INT NOT NULL DEFAULT 20 AFTER `scoreboard_freeze_time`',
  'SELECT ''contest.penalty_per_wrong exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `contest`
SET `penalty_per_wrong` = 20
WHERE `penalty_per_wrong` IS NULL OR `penalty_per_wrong` < 0;

