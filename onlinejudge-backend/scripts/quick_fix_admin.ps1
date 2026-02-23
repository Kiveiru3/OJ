# 快速修复admin密码
# 直接使用确认可用的密码哈希更新数据库

Write-Host "=== 快速修复Admin密码 ===" -ForegroundColor Cyan

$sqlFile = "scripts\quick_fix_admin.sql"

# 创建SQL脚本
$sqlContent = @"
-- 快速修复admin密码为 admin123
-- 使用确认可用的BCrypt哈希

UPDATE \`user\` 
SET \`password\` = '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'
WHERE \`username\` = 'admin';

-- 验证
SELECT 
    username,
    LEFT(password, 20) as password_prefix,
    LENGTH(password) as password_length,
    role,
    status
FROM \`user\`
WHERE \`username\` = 'admin';
"@

$sqlContent | Out-File -FilePath $sqlFile -Encoding UTF8

Write-Host "SQL脚本已创建: $sqlFile" -ForegroundColor Green
Write-Host ""
Write-Host "请执行以下步骤:" -ForegroundColor Yellow
Write-Host "1. 连接到MySQL数据库" -ForegroundColor White
Write-Host "2. 选择数据库: USE onlinejudge;" -ForegroundColor White
Write-Host "3. 运行SQL脚本: SOURCE $sqlFile;" -ForegroundColor White
Write-Host "   或者直接复制SQL内容执行" -ForegroundColor Gray
Write-Host ""
Write-Host "4. 然后使用以下信息登录:" -ForegroundColor Yellow
Write-Host "   用户名: admin" -ForegroundColor White
Write-Host "   密码: admin123" -ForegroundColor White
Write-Host ""
Write-Host "如果仍然无法登录，使用开发端点获取token:" -ForegroundColor Cyan
Write-Host "GET http://localhost:8082/api/dev/token/admin" -ForegroundColor Gray

