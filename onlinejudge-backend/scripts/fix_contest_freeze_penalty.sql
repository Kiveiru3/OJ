USE onlinejudge;

-- Add missing contest.scoreboard_freeze_time and contest.penalty_per_wrong
-- for backward compatibility with older schemas.
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
  'SELECT ''contest.scoreboard_freeze_time already exists'' AS msg'
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
  'SELECT ''contest.penalty_per_wrong already exists'' AS msg'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `contest`
SET `penalty_per_wrong` = 20
WHERE `penalty_per_wrong` IS NULL OR `penalty_per_wrong` < 0;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
('contest.default_penalty_per_wrong', '20', 'Default wrong submission penalty(minutes) for new contests')
ON DUPLICATE KEY UPDATE
`description` = VALUES(`description`);

-- Verify
SHOW COLUMNS FROM `contest` LIKE 'scoreboard_freeze_time';
SHOW COLUMNS FROM `contest` LIKE 'penalty_per_wrong';
