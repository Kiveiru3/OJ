# 修复密码长度为61的问题

## 问题描述
执行UPDATE后，密码长度仍然是61，而不是标准的60字符。

## 诊断步骤

### 步骤1：诊断问题

执行以下SQL找出第61个字符是什么：

```sql
-- 查看密码详细信息
SELECT 
    username,
    LENGTH(password) as password_length,
    CHAR_LENGTH(password) as char_length,
    ASCII(SUBSTRING(password, 61, 1)) as char61_ascii,
    HEX(SUBSTRING(password, 61, 1)) as char61_hex,
    SUBSTRING(password, 61, 1) as char61_value,
    RIGHT(password, 5) as last_5_chars
FROM `user`
WHERE `username` = 'admin';
```

### 步骤2：检查表结构

```sql
SHOW CREATE TABLE `user`;
```

查看 `password` 字段的定义，可能是：
- `CHAR(255)` - 固定长度，会填充空格
- `VARCHAR(255)` - 可变长度，应该没问题

### 步骤3：检查是否有隐藏字符

```sql
SELECT 
    username,
    CASE WHEN LOCATE(CHAR(10), password) > 0 THEN CONCAT('Found LF at: ', LOCATE(CHAR(10), password)) ELSE 'No LF' END as has_lf,
    CASE WHEN LOCATE(CHAR(13), password) > 0 THEN CONCAT('Found CR at: ', LOCATE(CHAR(13), password)) ELSE 'No CR' END as has_cr,
    CASE WHEN LOCATE(CHAR(9), password) > 0 THEN CONCAT('Found TAB at: ', LOCATE(CHAR(9), password)) ELSE 'No TAB' END as has_tab
FROM `user`
WHERE `username` = 'admin';
```

## 修复方案

### 方案1：强制修复（推荐）

```sql
-- 先清空
UPDATE `user` SET `password` = '' WHERE `username` = 'admin';

-- 设置正确的60字符哈希
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';

-- 验证
SELECT 
    username,
    LENGTH(password) as len,
    CHAR_LENGTH(password) as char_len,
    password = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O' as exact_match
FROM `user`
WHERE `username` = 'admin';
```

### 方案2：如果字段是CHAR类型

如果 `password` 字段是 `CHAR(255)`，需要修改表结构：

```sql
-- 修改字段类型为VARCHAR
ALTER TABLE `user` 
MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- 然后重新设置密码
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';
```

### 方案3：彻底清理

```sql
-- 移除所有可能的隐藏字符
UPDATE `user` 
SET `password` = REPLACE(
    REPLACE(
        REPLACE(
            REPLACE(
                TRIM(SUBSTRING(`password`, 1, 60)),
                CHAR(10), ''
            ),
            CHAR(13), ''
        ),
        CHAR(9), ''
    ),
    ' ', ''
)
WHERE `username` = 'admin';

-- 如果还是61，直接设置
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';
```

## 验证修复

执行以下SQL确认修复成功：

```sql
SELECT 
    username,
    LENGTH(password) as len,
    CHAR_LENGTH(password) as char_len,
    LEFT(password, 10) as prefix,
    RIGHT(password, 10) as suffix
FROM `user`
WHERE `username` = 'admin';
```

**预期结果：**
- `len` = 60
- `char_len` = 60
- `prefix` = `$2a$10$N.`
- `suffix` = `7iwy7p8f5O`

## 如果仍然不行

1. **检查数据库连接**：确认你连接的是正确的数据库
2. **检查事务**：确保执行了 `COMMIT;`
3. **检查权限**：确保有UPDATE权限
4. **使用开发端点**：临时使用 `/dev/token/admin` 获取token

