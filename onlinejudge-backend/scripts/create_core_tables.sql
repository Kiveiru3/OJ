USE onlinejudge;

-- Core tables required by auth/problem/submission/judge flow.

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(128) NULL,
  `phone` VARCHAR(20) NULL,
  `nickname` VARCHAR(64) NULL,
  `avatar` TEXT NULL,
  `role` VARCHAR(16) NOT NULL DEFAULT 'STUDENT',
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  UNIQUE KEY `uk_user_phone` (`phone`),
  KEY `idx_user_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `problem` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NOT NULL,
  `description` LONGTEXT NULL,
  `input_format` TEXT NULL,
  `output_format` TEXT NULL,
  `sample_input` LONGTEXT NULL,
  `sample_output` LONGTEXT NULL,
  `hint` TEXT NULL,
  `time_limit` INT NOT NULL DEFAULT 1000,
  `memory_limit` INT NOT NULL DEFAULT 256000,
  `difficulty` VARCHAR(16) NOT NULL DEFAULT 'EASY',
  `tags` VARCHAR(255) NULL,
  `creator_id` BIGINT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `ac_count` INT NOT NULL DEFAULT 0,
  `submit_count` INT NOT NULL DEFAULT 0,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_problem_status_difficulty_ctime` (`status`, `difficulty`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `test_case` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `problem_id` BIGINT NOT NULL,
  `input` LONGTEXT NOT NULL,
  `output` LONGTEXT NOT NULL,
  `is_sample` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_test_case_problem` (`problem_id`),
  KEY `idx_test_case_problem_sample` (`problem_id`, `is_sample`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `problem_id` BIGINT NOT NULL,
  `language` VARCHAR(32) NOT NULL,
  `code` LONGTEXT NOT NULL,
  `status` VARCHAR(64) NOT NULL DEFAULT 'PENDING',
  `time_used` INT NULL,
  `memory_used` INT NULL,
  `error_message` LONGTEXT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_submission_user_ctime` (`user_id`, `create_time`),
  KEY `idx_submission_problem_ctime` (`problem_id`, `create_time`),
  KEY `idx_submission_user_status_lang_ctime` (`user_id`, `status`, `language`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Keep password column wide enough for BCrypt.
ALTER TABLE `user`
MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- Create/repair admin2 account.
-- username: admin2
-- password: admin123
-- bcrypt: $2a$10$FoAxdf9RStrvA10Njh7ErOOOhjIgJf/OVkcfS9ZjdMbXgr4N9Dl.O
UPDATE `user`
SET
  `password` = CONVERT(0x24326124313024466F417864663952537472764131304E6A683745724F4F4F686A49674A662F4F566B636653395A6A644D62586772344E39446C2E4F USING ascii),
  `role` = 'ADMIN',
  `status` = 1,
  `deleted` = 0,
  `update_time` = NOW()
WHERE `username` = 'admin2';

INSERT INTO `user` (
  `username`, `password`, `email`, `nickname`, `role`, `status`, `deleted`, `create_time`, `update_time`
)
SELECT
  'admin2',
  CONVERT(0x24326124313024466F417864663952537472764131304E6A683745724F4F4F686A49674A662F4F566B636653395A6A644D62586772344E39446C2E4F USING ascii),
  CONCAT('admin2+', UNIX_TIMESTAMP(), '@example.com'),
  'Administrator2',
  'ADMIN',
  1,
  0,
  NOW(),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `user` WHERE `username` = 'admin2'
);

-- Seed one smoke problem if absent.
INSERT INTO `problem` (
  `title`, `description`, `input_format`, `output_format`, `sample_input`, `sample_output`,
  `hint`, `time_limit`, `memory_limit`, `difficulty`, `tags`, `creator_id`, `status`, `ac_count`, `submit_count`, `deleted`
)
SELECT
  'A+B Problem',
  'Given two integers a and b, output their sum.',
  'One line contains two integers: a b.',
  'Output one integer: a+b.',
  '1 2',
  '3',
  '',
  1000,
  256000,
  'EASY',
  'math,implementation',
  NULL,
  1,
  0,
  0,
  0
WHERE NOT EXISTS (
  SELECT 1 FROM `problem` WHERE `title` = 'A+B Problem' AND `deleted` = 0
);

SET @ab_id = (
  SELECT `id` FROM `problem`
  WHERE `title` = 'A+B Problem' AND `deleted` = 0
  ORDER BY `id` ASC
  LIMIT 1
);

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @ab_id, '1 2', '3', 1
WHERE @ab_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `test_case`
    WHERE `problem_id` = @ab_id AND `input` = '1 2' AND `output` = '3'
  );

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @ab_id, '100 200', '300', 0
WHERE @ab_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `test_case`
    WHERE `problem_id` = @ab_id AND `input` = '100 200' AND `output` = '300'
  );

INSERT INTO `test_case` (`problem_id`, `input`, `output`, `is_sample`)
SELECT @ab_id, '-5 8', '3', 0
WHERE @ab_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `test_case`
    WHERE `problem_id` = @ab_id AND `input` = '-5 8' AND `output` = '3'
  );

-- Verify admin account hash length.
SELECT
  `id`,
  `username`,
  `role`,
  `status`,
  LENGTH(`password`) AS `password_length`,
  CHAR_LENGTH(`password`) AS `password_char_length`
FROM `user`
WHERE `username` = 'admin2';
