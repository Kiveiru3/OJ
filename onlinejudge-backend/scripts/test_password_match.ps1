# 测试密码匹配
# 用于诊断密码问题

Write-Host "=== 测试Admin密码匹配 ===" -ForegroundColor Cyan

# 数据库中的密码哈希
$dbHash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O'

Write-Host "数据库哈希: $dbHash" -ForegroundColor Yellow
Write-Host "哈希长度: $($dbHash.Length)" -ForegroundColor Yellow
Write-Host ""

# 检查长度
if ($dbHash.Length -ne 60) {
    Write-Host "⚠ 警告: 哈希长度不是60！" -ForegroundColor Red
    Write-Host "实际长度: $($dbHash.Length)" -ForegroundColor Red
    Write-Host ""
    Write-Host "可能的原因:" -ForegroundColor Yellow
    Write-Host "1. 数据库字段中有额外的字符（换行符、空格等）" -ForegroundColor White
    Write-Host "2. 密码哈希被截断或添加了额外字符" -ForegroundColor White
    Write-Host ""
    Write-Host "解决方案:" -ForegroundColor Yellow
    Write-Host "运行 scripts/fix_admin_password_clean.sql 清理密码字段" -ForegroundColor White
} else {
    Write-Host "✓ 哈希长度正确 (60字符)" -ForegroundColor Green
}

Write-Host ""
Write-Host "请检查数据库中的实际密码值:" -ForegroundColor Cyan
Write-Host "SELECT password, LENGTH(password), HEX(password) FROM user WHERE username = 'admin';" -ForegroundColor Gray
Write-Host ""
Write-Host "如果长度不是60，运行以下SQL修复:" -ForegroundColor Yellow
Write-Host "UPDATE user SET password = TRIM(SUBSTRING(password, 1, 60)) WHERE username = 'admin';" -ForegroundColor Gray

