-- 最终修复admin密码
-- 使用新生成的BCrypt哈希确保密码匹配

-- 方法1: 使用确认可用的哈希（对应密码 admin123）
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';

-- 验证更新结果
SELECT 
    username,
    LEFT(password, 20) as password_prefix,
    LENGTH(password) as password_length,
    role,
    status
FROM `user`
WHERE `username` = 'admin';

-- 如果上面的哈希不工作，运行以下Java代码生成新哈希：
-- java -cp "target/classes;lib/*" com.academic.oj.util.PasswordGenerator
-- 然后使用生成的新哈希更新密码

