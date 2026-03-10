# 前后端联调 Smoke 测试

用于答辩前快速验证核心接口是否可用，建议每次大改后都执行。

## 1. 脚本位置

- `scripts/smoke_test.js`：通用核心链路
- `scripts/smoke_contest_e2e.js`：竞赛端到端（建赛→报名→榜单→删除清理）

## 2. 覆盖范围

### 2.1 通用核心链路（`npm run smoke:test`）

1. 登录：`POST /auth/login`
2. 用户信息：`GET /user/info`
3. 题目列表与详情：`GET /problem/list`、`GET /problem/{id}`
4. 竞赛列表与详情与排名：`GET /contest/list`、`GET /contest/{id}`、`GET /contest/{id}/ranking`
5. 提交列表：`GET /submission/list`
6. 讨论列表与详情：`GET /discussion/list`、`GET /discussion/{id}`
7. 教师/管理员额外检查：`GET /contest/{id}/score-snapshot`
8. 管理员额外检查：`GET /admin/system/configs`、`GET /admin/system/logs`、`GET /admin/system/monitor`、`GET /admin/system/feature-checklist`、`GET /admin/system/judge-results`

### 2.2 竞赛端到端（`npm run smoke:contest`）

1. 登录并校验角色（要求教师或管理员）
2. 获取一个题目 ID 作为建赛题目
3. 创建公开竞赛：`POST /contest`
4. 查询竞赛详情：`GET /contest/{id}`
5. 报名：`POST /contest/{id}/join`
6. 查询实时榜：`GET /contest/{id}/ranking`
7. 查询快照榜：`GET /contest/{id}/score-snapshot`
8. 清理数据：`DELETE /contest/{id}`

## 3. 运行方式

在目录 `onlinejudge-frontend` 执行：

```bash
npm run smoke:test
npm run smoke:contest
```

## 4. 环境变量

```bash
SMOKE_BASE_URL=http://localhost:8082
SMOKE_API_PREFIX=/api
SMOKE_USERNAME=admin2
SMOKE_PASSWORD=admin123
SMOKE_TIMEOUT_MS=15000
```

PowerShell 示例：

```powershell
$env:SMOKE_BASE_URL="http://localhost:8082"
$env:SMOKE_API_PREFIX="/api"
$env:SMOKE_USERNAME="admin2"
$env:SMOKE_PASSWORD="admin123"
npm run smoke:test
npm run smoke:contest
```

## 5. 结果判定

- 全部步骤 `OK` 且最终 `failed: 0` 视为通过。
- 任一步骤失败，脚本返回非 0，可直接用于 CI 或发布前检查。
