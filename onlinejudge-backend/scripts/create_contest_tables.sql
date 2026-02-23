USE onlinejudge;

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
