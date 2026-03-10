-- Compatibility backfill for legacy contest schema and defaults.
-- This migration is idempotent and safe on already-updated databases.

SET @db_name = DATABASE();

-- contest_participant.create_time
SET @has_participant_ctime = (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'contest_participant'
    AND COLUMN_NAME = 'create_time'
);
SET @sql = IF(
  @has_participant_ctime = 0,
  'ALTER TABLE `contest_participant` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT ''contest_participant.create_time exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- contest.scoreboard_freeze_time
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

-- contest.penalty_per_wrong
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

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
('contest.default_penalty_per_wrong', '20', 'Default wrong submission penalty(minutes) for new contests')
ON DUPLICATE KEY UPDATE
`description` = VALUES(`description`);
