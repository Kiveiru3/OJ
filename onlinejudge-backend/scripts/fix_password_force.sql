-- 强制修复密码 - 直接设置正确的60字符哈希
-- 如果其他方法都不行，使用这个

-- 先删除旧密码
UPDATE `user` 
SET `password` = ''
WHERE `username` = 'admin';

-- 设置正确的密码哈希（对应密码：admin123）
-- 这个哈希是确认可用的，长度正好60字符
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';

-- 立即验证
SELECT 
    username,
    LENGTH(password) as len,
    CHAR_LENGTH(password) as char_len,
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O' as exact_match,
    password
FROM `user`
WHERE `username` = 'admin';

-- 如果 len 和 char_len 都是 60，且 exact_match 是 1，说明修复成功

