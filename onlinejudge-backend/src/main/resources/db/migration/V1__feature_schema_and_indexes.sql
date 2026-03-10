-- Feature schema migration (idempotent)
-- Covers contest/discussion/system/judge/profile tables and core hot-path indexes.

CREATE TABLE IF NOT EXISTS `discussion_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `title` VARCHAR(120) NOT NULL,
  `content` TEXT NOT NULL,
  `problem_id` BIGINT NULL,
  `view_count` INT NOT NULL DEFAULT 0,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_discussion_user` (`user_id`),
  KEY `idx_discussion_problem` (`problem_id`),
  KEY `idx_discussion_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `discussion_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_post` (`post_id`),
  KEY `idx_comment_user` (`user_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL,
  `config_value` TEXT NULL,
  `description` VARCHAR(255) NULL,
  `update_user_id` BIGINT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT NULL,
  `operator_username` VARCHAR(64) NULL,
  `module` VARCHAR(64) NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(64) NULL,
  `target_id` BIGINT NULL,
  `detail` TEXT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_aol_module_action` (`module`, `action`),
  KEY `idx_aol_operator` (`operator_id`),
  KEY `idx_aol_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
VALUES
('site.name', 'Online Judge', 'Website display name'),
('site.announcement', '', 'Homepage announcement'),
('contest.default_page_size', '20', 'Default contest ranking page size')
ON DUPLICATE KEY UPDATE
`description` = VALUES(`description`);

CREATE TABLE IF NOT EXISTS `contest` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
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

CREATE TABLE IF NOT EXISTS `judge_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `language` VARCHAR(32) NOT NULL,
  `status` VARCHAR(64) NOT NULL,
  `time_used` INT NULL,
  `memory_used` INT NULL,
  `error_message` TEXT NULL,
  `judge_time` DATETIME NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_judge_result_submission` (`submission_id`),
  KEY `idx_judge_result_user` (`user_id`),
  KEY `idx_judge_result_problem` (`problem_id`),
  KEY `idx_judge_result_status` (`status`),
  KEY `idx_judge_result_judge_time` (`judge_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `student_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `student_no` VARCHAR(64) NULL,
  `class_name` VARCHAR(128) NULL,
  `major` VARCHAR(128) NULL,
  `real_name` VARCHAR(64) NULL,
  `gender` VARCHAR(16) NULL,
  `bio` VARCHAR(512) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_profile_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `teacher_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `teacher_no` VARCHAR(64) NULL,
  `department` VARCHAR(128) NULL,
  `title` VARCHAR(128) NULL,
  `real_name` VARCHAR(64) NULL,
  `gender` VARCHAR(16) NULL,
  `bio` VARCHAR(512) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_profile_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `admin_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `admin_code` VARCHAR(64) NULL,
  `real_name` VARCHAR(64) NULL,
  `department` VARCHAR(128) NULL,
  `bio` VARCHAR(512) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_profile_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @db_name = DATABASE();

-- contest_participant.create_time compatibility
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
  'SELECT ''contest_participant.create_time exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- test_case input/output enlarge for large datasets
SET @has_test_case = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'test_case'
);
SET @sql = IF(
  @has_test_case = 1,
  'ALTER TABLE `test_case` MODIFY COLUMN `input` LONGTEXT NOT NULL, MODIFY COLUMN `output` LONGTEXT NOT NULL',
  'SELECT ''test_case missing, skip longtext migration'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- profile table backfill
SET @has_user = (
  SELECT COUNT(1)
  FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = @db_name
    AND TABLE_NAME = 'user'
);

SET @sql = IF(
  @has_user = 1,
  'INSERT INTO `student_profile` (`user_id`) SELECT u.`id` FROM `user` u WHERE u.`deleted` = 0 AND u.`role` = ''STUDENT'' ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP',
  'SELECT ''user missing, skip student profile backfill'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  @has_user = 1,
  'INSERT INTO `teacher_profile` (`user_id`) SELECT u.`id` FROM `user` u WHERE u.`deleted` = 0 AND u.`role` = ''TEACHER'' ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP',
  'SELECT ''user missing, skip teacher profile backfill'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  @has_user = 1,
  'INSERT INTO `admin_profile` (`user_id`) SELECT u.`id` FROM `user` u WHERE u.`deleted` = 0 AND u.`role` = ''ADMIN'' ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP',
  'SELECT ''user missing, skip admin profile backfill'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Core indexes
SET @exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db_name
    AND table_name = 'submission'
    AND index_name = 'idx_submission_user_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_submission_user_ctime ON submission(user_id, create_time)',
              'SELECT ''idx_submission_user_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db_name
    AND table_name = 'submission'
    AND index_name = 'idx_submission_user_status_lang_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_submission_user_status_lang_ctime ON submission(user_id, status, language, create_time)',
              'SELECT ''idx_submission_user_status_lang_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db_name
    AND table_name = 'submission'
    AND index_name = 'idx_submission_problem_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_submission_problem_ctime ON submission(problem_id, create_time)',
              'SELECT ''idx_submission_problem_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db_name
    AND table_name = 'problem'
    AND index_name = 'idx_problem_status_difficulty_ctime'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_problem_status_difficulty_ctime ON problem(status, difficulty, create_time)',
              'SELECT ''idx_problem_status_difficulty_ctime exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exists = (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db_name
    AND table_name = 'test_case'
    AND index_name = 'idx_test_case_problem'
);
SET @sql = IF(@exists = 0,
              'CREATE INDEX idx_test_case_problem ON test_case(problem_id)',
              'SELECT ''idx_test_case_problem exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
