-- Store per-testcase judging details for submission explainability.

CREATE TABLE IF NOT EXISTS `submission_case_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `submission_id` BIGINT NOT NULL,
  `case_no` INT NOT NULL,
  `is_sample` TINYINT NOT NULL DEFAULT 0,
  `status` VARCHAR(64) NOT NULL,
  `time_used` INT NULL,
  `memory_used` INT NULL,
  `input_preview` TEXT NULL,
  `expected_preview` TEXT NULL,
  `actual_preview` TEXT NULL,
  `error_message` TEXT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_submission_case` (`submission_id`, `case_no`),
  KEY `idx_scr_submission` (`submission_id`),
  KEY `idx_scr_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

