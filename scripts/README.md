# 一键验收脚本

## 1. 入口

- PowerShell: `scripts/run_acceptance_checks.ps1`
- CMD: `scripts/run_acceptance_checks.bat`

## 2. 默认执行内容

1. 后端编译与测试（调用 `onlinejudge-backend/scripts/run_backend_checks.ps1`）
2. 前端 `npm run lint`
3. 前端 `npm run build`
4. 前端 `npm run smoke:test`

任一步骤失败会立即退出并返回非 0。

## 3. 运行方式

在仓库根目录 `D:\OJ-project` 下执行：

```powershell
.\scripts\run_acceptance_checks.ps1
```

或：

```bat
scripts\run_acceptance_checks.bat
```

## 4. 常用参数

```powershell
.\scripts\run_acceptance_checks.ps1 `
  -SmokeBaseUrl "http://localhost:8082" `
  -SmokeApiPrefix "/api" `
  -SmokeUsername "admin2" `
  -SmokePassword "admin123"
```

可选跳过项：

- `-SkipBackendChecks`
- `-SkipFrontendLint`
- `-SkipFrontendBuild`
- `-SkipFrontendSmoke`

## 5. 前置条件

1. MySQL/Redis 已启动
2. 后端服务已启动（脚本会在 smoke 前探测可达性）
3. 前后端依赖已安装（`mvn`、`npm`）
