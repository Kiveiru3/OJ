-- 快速重置密码脚本
-- 用于重置Deft216用户的密码

-- 选项1：重置为 "lirong216"（需要先运行PasswordGenerator生成正确的哈希）
-- 注意：下面的哈希值需要根据实际生成的BCrypt哈希替换

-- 选项2：使用临时简单密码（仅用于测试，不推荐生产环境）
-- 生成 "test123" 的BCrypt哈希（示例，需要实际生成）
-- UPDATE `user` 
-- SET `password` = '$2a$10$...'  -- 替换为实际生成的哈希
-- WHERE `username` = 'Deft216';

-- 选项3：直接使用开发工具重置（推荐）
-- 使用 /dev/token/Deft216 端点获取token，无需密码

-- 验证当前密码哈希
SELECT 
    username, 
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix,
    password
FROM `user`
WHERE `username` = 'Deft216';

-- 如果需要重置，运行以下步骤：
-- 1. 运行 PasswordGenerator.java 生成新密码的BCrypt哈希
-- 2. 使用生成的哈希更新数据库
-- 3. 使用新密码登录

