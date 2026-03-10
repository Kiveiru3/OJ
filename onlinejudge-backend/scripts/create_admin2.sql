USE onlinejudge;

-- Create/repair admin2 account with a verified 60-char BCrypt hash
-- Username: admin2
-- Password: admin123
-- Hash: $2a$10$FoAxdf9RStrvA10Njh7ErOOOhjIgJf/OVkcfS9ZjdMbXgr4N9Dl.O

ALTER TABLE `user`
MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- Ensure clean target user by username
DELETE FROM `user` WHERE `username` = 'admin2';

INSERT INTO `user` (
  `username`,
  `password`,
  `email`,
  `nickname`,
  `role`,
  `status`,
  `deleted`,
  `create_time`,
  `update_time`
) VALUES (
  'admin2',
  CONVERT(0x24326124313024466F417864663952537472764131304E6A683745724F4F4F686A49674A662F4F566B636653395A6A644D62586772344E39446C2E4F USING ascii),
  CONCAT('admin2+', UNIX_TIMESTAMP(), '@example.com'),
  'Administrator2',
  'ADMIN',
  1,
  0,
  NOW(),
  NOW()
);

SELECT
  `id`,
  `username`,
  LENGTH(`password`) AS password_length,
  CHAR_LENGTH(`password`) AS password_char_length,
  RIGHT(`password`, 10) AS password_suffix,
  HEX(`password`) AS password_hex,
  `role`,
  `status`
FROM `user`
WHERE `username` = 'admin2';
