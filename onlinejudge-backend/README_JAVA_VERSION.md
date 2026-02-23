# Java版本问题解决方案

## 错误信息
```
UnsupportedClassVersionError: class file version 61.0, this version only recognizes up to 55.0
```

## 原因
- 项目使用 **Java 17** 编译（class file version 61.0）
- 但运行时使用了 **Java 11**（class file version 55.0）

## 解决方案

### 方案1：在IDE中设置Java 17（推荐）

#### IntelliJ IDEA
1. 打开 `File` → `Project Structure` (Ctrl+Alt+Shift+S)
2. 在 `Project` 标签页：
   - 设置 `SDK` 为 **Java 17**
   - 设置 `Language level` 为 **17 - Sealed types, always-strict floating-point semantics**
3. 在 `Modules` 标签页：
   - 确保所有模块的 `Language level` 都是 **17**
4. 在 `Settings` → `Build, Execution, Deployment` → `Compiler` → `Java Compiler`：
   - 设置 `Project bytecode version` 为 **17**
   - 设置 `Per-module bytecode version` 为 **17**
5. 重启IDE

#### Eclipse
1. 右键项目 → `Properties`
2. `Java Build Path` → `Libraries` → 移除旧的JRE，添加 **Java 17**
3. `Java Compiler` → 设置 `Compiler compliance level` 为 **17**
4. 重启IDE

### 方案2：清理并重新编译

如果使用命令行：
```bash
# 清理编译文件
rm -rf target/

# 重新编译（确保使用Java 17）
mvn clean compile
```

### 方案3：检查环境变量

确保系统使用Java 17：
```bash
# Windows PowerShell
java -version  # 应该显示 17.x.x
javac -version  # 应该显示 17.x.x

# 检查JAVA_HOME
echo $env:JAVA_HOME  # 应该指向Java 17目录
```

### 方案4：如果必须使用Java 11

如果确实需要使用Java 11运行，需要修改 `pom.xml`：

```xml
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>
```

**注意**：Spring Boot 2.7.18 支持 Java 11，但建议使用 Java 17。

## 验证

运行以下命令验证：
```bash
java -version
# 应该显示：java version "17.0.x"

javac -version
# 应该显示：javac 17.0.x
```

## 常见问题

### Q: IDE显示Java 17，但还是报错？
A: 尝试：
1. `File` → `Invalidate Caches / Restart` → `Invalidate and Restart`
2. 删除 `.idea` 文件夹（会重新生成）
3. 重新导入项目

### Q: Maven编译成功，但运行失败？
A: 检查运行配置：
- `Run` → `Edit Configurations`
- 确保 `JRE` 设置为 **Java 17**

### Q: 多个Java版本如何切换？
A: 使用 `JAVA_HOME` 环境变量指向Java 17目录。

