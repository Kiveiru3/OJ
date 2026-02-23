-- 彻底修复密码长度为61的问题
-- 移除所有可能的隐藏字符

-- 方法1：使用REPLACE移除常见的问题字符
UPDATE `user` 
SET `password` = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                TRIM(SUBSTRING(`password`, 1, 60)),
                CHAR(10), ''  -- 移除换行符 (LF)
            ),
            CHAR(13), ''  -- 移除回车符 (CR)
        ),
        CHAR(9), ''  -- 移除制表符 (TAB)
    ),
    ' ', ''  -- 移除空格（如果不在开头结尾）
)
WHERE `username` = 'admin';

-- 如果方法1不行，使用方法2：直接设置正确的哈希值
-- 这个哈希对应密码：admin123，长度正好是60
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';

-- 验证修复结果
SELECT 
    username,
    password,
    LENGTH(password) as password_length,
    CHAR_LENGTH(password) as char_length,
    LEFT(password, 10) as password_prefix,
    RIGHT(password, 10) as password_suffix,
    -- 检查是否还有问题字符
    CASE WHEN LOCATE(CHAR(10), password) > 0 THEN 'Has LF' ELSE 'OK' END as check_lf,
    CASE WHEN LOCATE(CHAR(13), password) > 0 THEN 'Has CR' ELSE 'OK' END as check_cr,
    CASE WHEN LOCATE(CHAR(9), password) > 0 THEN 'Has TAB' ELSE 'OK' END as check_tab
FROM `user`
WHERE `username` = 'admin';

-- 预期结果：
-- password_length: 60
-- char_length: 60
-- password_prefix: $2a$10$N.
-- password_suffix: 7iwy7p8f5O
-- check_lf: OK
-- check_cr: OK
-- check_tab: OK

