# 生成admin密码哈希的PowerShell脚本
# 需要先编译项目

Write-Host "=== 生成Admin密码哈希 ===" -ForegroundColor Cyan

# 检查项目是否已编译
if (-not (Test-Path "target\classes\com\academic\oj\util\PasswordGenerator.class")) {
    Write-Host "正在编译项目..." -ForegroundColor Yellow
    mvn compile -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "编译失败！" -ForegroundColor Red
        exit 1
    }
}

Write-Host "运行PasswordGenerator..." -ForegroundColor Green

# 运行Java程序生成密码哈希
$classpath = "target\classes"
$libs = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository\org\springframework\security\spring-security-crypto" -Recurse -Filter "*.jar" | Select-Object -First 1
if ($libs) {
    $classpath += ";$($libs.FullName)"
}

java -cp $classpath com.academic.oj.util.PasswordGenerator

Write-Host ""
Write-Host "=== 使用以下SQL更新admin密码 ===" -ForegroundColor Yellow
Write-Host "运行上面的Java程序后，复制生成的哈希值，然后执行:" -ForegroundColor White
Write-Host "UPDATE user SET password = '<生成的哈希>' WHERE username = 'admin';" -ForegroundColor Gray

