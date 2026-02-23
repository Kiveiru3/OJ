-- 修复密码字段长度问题
-- BCrypt密码需要至少60个字符

-- 检查当前字段长度
SELECT 
    COLUMN_NAME, 
    CHARACTER_MAXIMUM_LENGTH,
    DATA_TYPE
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = 'onlinejudge' 
    AND TABLE_NAME = 'user' 
    AND COLUMN_NAME = 'password';

-- 修改密码字段长度为255（足够存储BCrypt密码）
ALTER TABLE `user` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- 如果admin用户的密码被截断，重新设置密码为 admin123
-- 注意：这个BCrypt哈希值对应密码 "admin123"
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';

-- 验证密码长度
SELECT 
    username, 
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix
FROM `user`
WHERE `username` = 'admin';

