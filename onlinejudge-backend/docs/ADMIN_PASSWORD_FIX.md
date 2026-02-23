# Admin密码问题修复指南

## 问题描述
管理员账号 `admin` 无法登录，提示"用户名或密码错误"。

## 快速解决方案

### 方案1：使用开发端点（推荐，无需密码）

直接获取token，无需密码验证：

```bash
GET http://localhost:8082/api/dev/token/admin
```

或者使用PowerShell：

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/dev/token/admin" -Method GET
```

### 方案2：重置密码

#### 步骤1：运行SQL脚本

执行以下SQL更新admin密码：

```sql
UPDATE `user` 
SET `password` = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE `username` = 'admin';
```

这个哈希对应密码：`admin123`

#### 步骤2：验证

```sql
SELECT 
    username,
    LEFT(password, 20) as password_prefix,
    LENGTH(password) as password_length,
    role,
    status
FROM `user`
WHERE `username` = 'admin';
```

应该看到：
- username: `admin`
- password_prefix: `$2a$10$N.zmdr9k7uOCQb376`
- password_length: `60`
- role: `ADMIN`
- status: `1`

#### 步骤3：登录

- 用户名：`admin`
- 密码：`admin123`

### 方案3：生成新密码哈希

如果上面的哈希不工作，可以生成新的：

#### 步骤1：运行PasswordGenerator

```bash
mvn compile
java -cp "target/classes;target/dependency/*" com.academic.oj.util.PasswordGenerator
```

#### 步骤2：复制生成的哈希

程序会输出类似这样的内容：

```
Password: admin123
BCrypt Hash: $2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

#### 步骤3：更新数据库

```sql
UPDATE `user` 
SET `password` = '<复制的哈希值>'
WHERE `username` = 'admin';
```

## 诊断步骤

### 1. 检查数据库中的密码哈希

```sql
SELECT 
    username,
    password,
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix
FROM `user`
WHERE `username` = 'admin';
```

**正常情况：**
- password_length 应该是 `60`
- password_prefix 应该是 `$2a$10$` 或 `$2b$10$` 或 `$2y$10$`

### 2. 测试密码匹配

运行 `PasswordGenerator.java` 的main方法，它会：
- 测试多个可能的密码
- 验证数据库中的哈希
- 如果都不匹配，生成新的哈希

### 3. 查看应用日志

启动应用后，尝试登录，查看控制台输出：

```
=== Password Verification Debug ===
Username: admin
Password hash length: 60
Password hash prefix: $2a$10$
Input password length: 8
Password match result: false/true
```

如果 `Password match result: false`，说明密码不匹配。

## 常见问题

### Q: 为什么密码哈希看起来正确但还是无法登录？

A: 可能的原因：
1. **密码输入错误**：确认输入的是 `admin123`（小写，无空格）
2. **哈希被截断**：检查 `password` 字段长度是否为 `60`
3. **字符编码问题**：确保数据库使用UTF-8编码
4. **BCrypt版本不匹配**：确保使用相同的BCrypt实现

### Q: 如何确认密码是否正确？

A: 使用 `PasswordGenerator.java` 验证：

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String dbHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O";
boolean matches = encoder.matches("admin123", dbHash);
System.out.println("Matches: " + matches);
```

### Q: 可以使用其他密码吗？

A: 可以，但需要：
1. 使用 `PasswordGenerator` 生成新密码的哈希
2. 更新数据库
3. 记住新密码

## 推荐做法

**开发环境：** 使用 `/dev/token/admin` 端点，无需密码验证。

**生产环境：** 
1. 使用强密码
2. 定期更换密码
3. 不要使用开发端点

## 相关文件

- `scripts/quick_fix_admin.sql` - 快速修复SQL脚本
- `scripts/test_admin_login.ps1` - 测试登录脚本
- `src/main/java/com/academic/oj/util/PasswordGenerator.java` - 密码生成工具
- `src/main/java/com/academic/oj/controller/DevController.java` - 开发端点

