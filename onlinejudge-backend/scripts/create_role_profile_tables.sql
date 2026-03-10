USE onlinejudge;

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

-- Backfill existing users.
INSERT INTO `student_profile` (`user_id`)
SELECT u.`id`
FROM `user` u
WHERE u.`deleted` = 0 AND u.`role` = 'STUDENT'
ON DUPLICATE KEY UPDATE
`update_time` = CURRENT_TIMESTAMP;

INSERT INTO `teacher_profile` (`user_id`)
SELECT u.`id`
FROM `user` u
WHERE u.`deleted` = 0 AND u.`role` = 'TEACHER'
ON DUPLICATE KEY UPDATE
`update_time` = CURRENT_TIMESTAMP;

INSERT INTO `admin_profile` (`user_id`)
SELECT u.`id`
FROM `user` u
WHERE u.`deleted` = 0 AND u.`role` = 'ADMIN'
ON DUPLICATE KEY UPDATE
`update_time` = CURRENT_TIMESTAMP;

