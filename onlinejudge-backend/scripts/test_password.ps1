# 测试密码生成和验证
# 用于验证BCrypt密码是否正确

Write-Host "Testing BCrypt Password Generation" -ForegroundColor Cyan
Write-Host ""

# 从日志中看到的密码哈希
$storedHash = '$2a$10$BXtR0M65r7HAh86HeYt/h.ODrg3q2XMCyB9fdchGawkEkvp6SgZzK'

Write-Host "Stored Hash: $storedHash" -ForegroundColor Yellow
Write-Host "Hash Length: $($storedHash.Length)" -ForegroundColor Yellow
Write-Host ""

# 提示用户输入密码进行测试
Write-Host "Please enter the password to test:" -ForegroundColor Green
$password = Read-Host

Write-Host ""
Write-Host "Testing password: $password" -ForegroundColor Cyan
Write-Host ""

# 注意：这个脚本只是显示信息，实际验证需要在Java代码中进行
Write-Host "To verify the password, check the application logs after login attempt." -ForegroundColor Yellow
Write-Host ""
Write-Host "Common passwords to try:" -ForegroundColor Cyan
Write-Host "  - lirong216" -ForegroundColor White
Write-Host "  - Deft216" -ForegroundColor White
Write-Host "  - password" -ForegroundColor White
Write-Host "  - 123456" -ForegroundColor White
Write-Host ""

