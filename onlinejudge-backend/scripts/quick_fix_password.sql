-- 快速修复密码问题
-- 用于重置Deft216用户的密码

USE onlinejudge;

-- 步骤1: 检查当前密码字段长度
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'onlinejudge' 
  AND TABLE_NAME = 'user' 
  AND COLUMN_NAME = 'password';

-- 步骤2: 如果字段长度小于255，修改它
ALTER TABLE `user` 
MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- 步骤3: 检查当前密码长度
SELECT 
    username,
    LENGTH(password) as password_length,
    LEFT(password, 10) as prefix,
    CASE 
        WHEN LENGTH(password) = 60 THEN '✓ Correct'
        WHEN LENGTH(password) < 60 THEN '✗ Truncated'
        ELSE '? Unexpected'
    END as status
FROM `user`
WHERE username = 'Deft216';

-- 步骤4: 如果需要重置密码，运行PasswordGenerator.java生成新的哈希
-- 然后执行：
-- UPDATE `user` 
-- SET `password` = '新生成的BCrypt哈希值'
-- WHERE `username` = 'Deft216';

-- 步骤5: 验证修复
SELECT 
    username,
    LENGTH(password) as password_length,
    LEFT(password, 7) as prefix
FROM `user`
WHERE username = 'Deft216';

