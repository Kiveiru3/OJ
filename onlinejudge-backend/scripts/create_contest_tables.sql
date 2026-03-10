USE onlinejudge;

CREATE TABLE IF NOT EXISTS `contest` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `scoreboard_freeze_time` DATETIME NULL,
  `penalty_per_wrong` INT NOT NULL DEFAULT 20,
  `creator_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-public, 0-hidden',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_contest_creator` (`creator_id`),
  KEY `idx_contest_time` (`start_time`, `end_time`),
  KEY `idx_contest_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `contest_problem` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contest_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contest_problem` (`contest_id`, `problem_id`),
  KEY `idx_cp_problem` (`problem_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `contest_participant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contest_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contest_user` (`contest_id`, `user_id`),
  KEY `idx_cp_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `contest_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contest_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `rank_no` INT NOT NULL,
  `accepted_count` INT NOT NULL DEFAULT 0,
  `total_penalty` INT NOT NULL DEFAULT 0,
  `total_submissions` INT NOT NULL DEFAULT 0,
  `last_accepted_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contest_score_user` (`contest_id`, `user_id`),
  KEY `idx_contest_score_rank` (`contest_id`, `rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backward compatibility: old table may miss create_time.
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

-- Backward compatibility: old contest table may miss freeze/penalty fields.
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
