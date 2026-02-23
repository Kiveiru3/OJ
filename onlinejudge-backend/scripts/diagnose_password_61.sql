-- 详细诊断密码长度为61的问题
-- 找出第61个字符是什么

-- 1. 检查密码的详细信息
SELECT 
    username,
    password,
    LENGTH(password) as password_length,
    CHAR_LENGTH(password) as char_length,
    -- 查看每个字符的ASCII码
    ASCII(SUBSTRING(password, 1, 1)) as char1_ascii,
    ASCII(SUBSTRING(password, 60, 1)) as char60_ascii,
    ASCII(SUBSTRING(password, 61, 1)) as char61_ascii,
    -- 查看第61个字符的十六进制
    HEX(SUBSTRING(password, 61, 1)) as char61_hex,
    -- 查看最后几个字符
    RIGHT(password, 5) as last_5_chars,
    -- 查看是否有不可见字符
    SUBSTRING(password, 61, 1) as char61_value
FROM `user`
WHERE `username` = 'admin';

-- 2. 检查字段定义
SHOW CREATE TABLE `user`;

-- 3. 查看密码的完整十六进制表示（最后部分）
SELECT 
    username,
    HEX(RIGHT(password, 5)) as last_5_chars_hex
FROM `user`
WHERE `username` = 'admin';

-- 4. 尝试找出问题字符的位置
SELECT 
    username,
    -- 检查是否有换行符 (ASCII 10)
    CASE WHEN LOCATE(CHAR(10), password) > 0 THEN CONCAT('Found LF at position: ', LOCATE(CHAR(10), password)) ELSE 'No LF' END as has_lf,
    -- 检查是否有回车符 (ASCII 13)
    CASE WHEN LOCATE(CHAR(13), password) > 0 THEN CONCAT('Found CR at position: ', LOCATE(CHAR(13), password)) ELSE 'No CR' END as has_cr,
    -- 检查是否有制表符 (ASCII 9)
    CASE WHEN LOCATE(CHAR(9), password) > 0 THEN CONCAT('Found TAB at position: ', LOCATE(CHAR(9), password)) ELSE 'No TAB' END as has_tab,
    -- 检查是否有空格 (ASCII 32)
    CASE WHEN LOCATE(' ', password) > 0 THEN CONCAT('Found SPACE at position: ', LOCATE(' ', password)) ELSE 'No SPACE' END as has_space
FROM `user`
WHERE `username` = 'admin';

