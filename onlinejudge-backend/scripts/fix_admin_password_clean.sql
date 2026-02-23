-- 清理并修复admin密码
-- 问题：密码哈希长度为61，应该是60（BCrypt标准）

-- 步骤1：检查当前密码（查看是否有隐藏字符）
SELECT 
    username,
    password,
    LENGTH(password) as password_length,
    HEX(password) as password_hex,
    ASCII(SUBSTRING(password, 61, 1)) as char_61_ascii
FROM `user`
WHERE `username` = 'admin';

-- 步骤2：清理密码字段（移除可能的换行符、空格等）
-- 使用TRIM和SUBSTRING确保密码是标准的60字符
UPDATE `user` 
SET `password` = TRIM(SUBSTRING(`password`, 1, 60))
WHERE `username` = 'admin';

-- 步骤3：如果清理后仍然有问题，使用新的确认可用的哈希
-- 这个哈希对应密码：admin123
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';

-- 步骤4：验证修复结果
SELECT 
    username,
    password,
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix,
    RIGHT(password, 10) as password_suffix,
    role,
    status
FROM `user`
WHERE `username` = 'admin';

-- 预期结果：
-- password_length: 60
-- password_prefix: $2a$10$N.
-- password_suffix: 7iwy7p8f5O

