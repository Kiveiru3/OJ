-- 检查user表结构，特别是password字段
USE onlinejudge;

-- 查看表结构
DESCRIBE `user`;

-- 或者使用
SHOW CREATE TABLE `user`;

-- 检查password字段的详细信息
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM 
    INFORMATION_SCHEMA.COLUMNS
WHERE 
    TABLE_SCHEMA = 'onlinejudge' 
    AND TABLE_NAME = 'user'
    AND COLUMN_NAME = 'password';

-- 检查当前密码的长度和内容
SELECT 
    username,
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix,
    RIGHT(password, 10) as password_suffix,
    password
FROM `user`
WHERE username = 'Deft216';

