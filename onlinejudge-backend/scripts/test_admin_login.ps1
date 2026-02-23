# 测试admin登录
# 用于诊断密码问题

$baseUrl = "http://localhost:8082/api"
$username = "admin"
$password = "admin123"

Write-Host "=== 测试Admin登录 ===" -ForegroundColor Cyan
Write-Host "用户名: $username" -ForegroundColor Yellow
Write-Host "密码: $password" -ForegroundColor Yellow
Write-Host ""

# 测试登录
Write-Host "正在尝试登录..." -ForegroundColor Green
try {
    $loginBody = @{
        username = $username
        password = $password
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody `
        -ErrorAction Stop

    Write-Host "✓ 登录成功！" -ForegroundColor Green
    Write-Host ""
    Write-Host "响应内容:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    
    if ($response.code -eq 200 -and $response.data) {
        $token = $response.data.token
        if ($token) {
            Write-Host ""
            Write-Host "Token已复制到剪贴板" -ForegroundColor Green
            $token | Set-Clipboard
            Write-Host "Token: $token" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "✗ 登录失败！" -ForegroundColor Red
    Write-Host ""
    Write-Host "错误信息:" -ForegroundColor Red
    $_.Exception.Message
    
    if ($_.ErrorDetails.Message) {
        Write-Host ""
        Write-Host "详细错误:" -ForegroundColor Red
        $errorJson = $_.ErrorDetails.Message | ConvertFrom-Json
        $errorJson | ConvertTo-Json -Depth 10
    }
    
    Write-Host ""
    Write-Host "=== 解决方案 ===" -ForegroundColor Yellow
    Write-Host "1. 检查密码是否正确（应该是 admin123）" -ForegroundColor White
    Write-Host "2. 运行以下SQL更新密码:" -ForegroundColor White
    Write-Host "   UPDATE user SET password = '\$2a\$10\$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O' WHERE username = 'admin';" -ForegroundColor Gray
    Write-Host "3. 或者使用开发端点获取token（无需密码）:" -ForegroundColor White
    Write-Host "   GET $baseUrl/dev/token/admin" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== 使用开发端点（绕过密码验证） ===" -ForegroundColor Cyan
try {
    $devResponse = Invoke-RestMethod -Uri "$baseUrl/dev/token/$username" `
        -Method GET `
        -ErrorAction Stop
    
    Write-Host "✓ 开发端点成功！" -ForegroundColor Green
    $devResponse | ConvertTo-Json -Depth 10
    
    if ($devResponse.code -eq 200 -and $devResponse.data) {
        $token = $devResponse.data.token
        if ($token) {
            Write-Host ""
            Write-Host "Token已复制到剪贴板" -ForegroundColor Green
            $token | Set-Clipboard
            Write-Host "Token: $token" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "✗ 开发端点也失败" -ForegroundColor Red
    $_.Exception.Message
}

