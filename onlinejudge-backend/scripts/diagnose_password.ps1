# 密码诊断工具
# 用于诊断密码验证问题

Write-Host "=== Password Diagnosis Tool ===" -ForegroundColor Cyan
Write-Host ""

# 从日志中提取的密码哈希
$storedHash = '$2a$10$BXtR0M65r7HAh86HeYt/h.ODrg3q2XMCyB9fdchGawkEkvp6SgZzK'

Write-Host "Stored Password Hash:" -ForegroundColor Yellow
Write-Host $storedHash -ForegroundColor White
Write-Host "Hash Length: $($storedHash.Length) characters" -ForegroundColor $(if ($storedHash.Length -eq 60) { "Green" } else { "Red" })
Write-Host ""

# 检查格式
if ($storedHash.StartsWith('$2a$') -or $storedHash.StartsWith('$2b$') -or $storedHash.StartsWith('$2y$')) {
    Write-Host "✓ BCrypt format: Correct" -ForegroundColor Green
} else {
    Write-Host "✗ BCrypt format: Incorrect" -ForegroundColor Red
}

Write-Host ""
Write-Host "Common passwords to try:" -ForegroundColor Cyan
Write-Host "  1. lirong216" -ForegroundColor White
Write-Host "  2. Deft216" -ForegroundColor White
Write-Host "  3. password" -ForegroundColor White
Write-Host "  4. 123456" -ForegroundColor White
Write-Host ""

Write-Host "=== Solution ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "If password doesn't match, you have two options:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Option 1: Use dev endpoint (no password needed)" -ForegroundColor Green
Write-Host "  GET http://localhost:8082/api/dev/token/Deft216" -ForegroundColor White
Write-Host ""
Write-Host "Option 2: Reset password in database" -ForegroundColor Green
Write-Host "  1. Run PasswordGenerator.java to generate new hash" -ForegroundColor White
Write-Host "  2. Update database with new hash" -ForegroundColor White
Write-Host "  3. Use new password to login" -ForegroundColor White
Write-Host ""

Write-Host "=== Check Database ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Run this SQL to check password field:" -ForegroundColor Yellow
Write-Host "  SELECT LENGTH(password), LEFT(password, 10) FROM user WHERE username = 'Deft216';" -ForegroundColor White
Write-Host ""

