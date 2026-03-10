USE onlinejudge;

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

