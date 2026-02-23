USE onlinejudge;

-- Reset admin password to admin123 with a verified 60-char BCrypt hash
-- Hash: $2a$10$FoAxdf9RStrvA10Njh7ErOOOhjIgJf/OVkcfS9ZjdMbXgr4N9Dl.O
-- Length: 60

ALTER TABLE `user`
MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

UPDATE `user`
SET `password` = CONVERT(0x24326124313024466F417864663952537472764131304E6A683745724F4F4F686A49674A662F4F566B636653395A6A644D62586772344E39446C2E4F USING ascii)
WHERE `username` = 'admin';

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
WHERE `username` = 'admin';
