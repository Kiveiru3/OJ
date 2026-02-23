-- 修复user表结构
USE onlinejudge;

-- 1. 检查当前表结构
DESCRIBE `user`;

-- 2. 修改password字段为VARCHAR(255)，确保足够长
ALTER TABLE `user` 
MODIFY COLUMN `password` VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码，需要60字符';

-- 3. 验证修改
DESCRIBE `user`;

-- 4. 检查密码长度
SELECT 
    username,
    LENGTH(password) as password_length,
    CASE 
        WHEN LENGTH(password) = 60 THEN '✓ Correct length'
        WHEN LENGTH(password) < 60 THEN '✗ Too short (truncated)'
        ELSE '? Unexpected length'
    END as length_status,
    LEFT(password, 7) as prefix
FROM `user`
WHERE username = 'Deft216';

