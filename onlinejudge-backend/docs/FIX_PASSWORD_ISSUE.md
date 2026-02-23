# 修复密码验证问题

## 问题描述

登录时出现错误：
```
Encoded password does not look like BCrypt
Invalid username or password
```

## 原因分析

BCrypt密码需要**60个字符**，如果数据库字段长度不够（比如VARCHAR(50)），密码会被截断，导致验证失败。

## 解决方案

### 方法1：修改数据库字段长度（推荐）

执行SQL脚本：
```sql
-- 修改密码字段长度为255
ALTER TABLE `user` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;

-- 重新设置admin密码（如果被截断）
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';
```

或者直接运行：
```bash
mysql -u root -proot onlinejudge < src/main/resources/db/fix_password_field.sql
```

### 方法2：检查当前密码长度

```sql
-- 查看admin用户的密码长度
SELECT 
    username, 
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix
FROM `user`
WHERE `username` = 'admin';
```

如果 `password_length` 小于 60，说明密码被截断了。

### 方法3：重新生成密码

如果密码被截断，需要重新生成BCrypt哈希：

1. 使用Java工具生成：
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String hash = encoder.encode("admin123");
System.out.println(hash);
```

2. 更新数据库：
```sql
UPDATE `user` 
SET `password` = '新生成的BCrypt哈希值'
WHERE `username` = 'admin';
```

## 验证修复

1. 检查字段长度：
```sql
SHOW COLUMNS FROM `user` LIKE 'password';
```
应该显示 `VARCHAR(255)` 或更长

2. 检查密码长度：
```sql
SELECT LENGTH(password) FROM `user` WHERE username = 'admin';
```
应该返回 60

3. 尝试登录：
- 用户名：`admin`
- 密码：`admin123`

## 预防措施

在创建数据库表时，确保密码字段足够长：

```sql
CREATE TABLE `user` (
    ...
    `password` VARCHAR(255) NOT NULL,  -- 至少255，推荐更长
    ...
);
```

## 常见问题

### Q: 为什么BCrypt密码是60个字符？
A: BCrypt格式：`$2a$10$...` (60字符)
- `$2a$` - 算法标识（4字符）
- `10` - 成本因子（2字符）
- `...` - 盐值和哈希（53字符）

### Q: 密码字段应该设置多长？
A: 推荐 `VARCHAR(255)` 或更长，为未来可能的算法升级留出空间。

### Q: 如何确认密码是否被截断？
A: 检查密码长度：
```sql
SELECT LENGTH(password) FROM user WHERE username = 'admin';
```
如果小于60，说明被截断了。

