# 系统测试与性能测试（最小可执行方案）

本文档用于毕业设计“系统测试”章节，可直接复用执行步骤与结果截图。

## 1. 功能测试（后端）

### 1.1 自动化测试范围

已覆盖的核心控制器与服务测试位于 `src/test/java`，包含：

- 认证：`AuthControllerTest`、`AuthControllerWebMvcTest`
- 用户与管理：`UserControllerTest`、`AdminSystemControllerTest`
- 题目与提交：`ProblemControllerTest`、`SubmissionControllerTest`、`TestCaseControllerTest`
- 讨论区：`DiscussionControllerTest`、`DiscussionCommentControllerTest`
- 教学分析：`TeacherAnalyticsControllerTest`
- 竞赛新增能力：`ContestControllerTest`（成绩快照权限与查询）
- 评测服务：`SubmissionServiceImplTest`

### 1.2 执行命令

在目录 `onlinejudge-backend` 执行：

```bash
mvn -DfailIfNoTests=false test
```

若命令退出码为 `0`，可判定最小功能回归通过。

## 2. 性能测试（k6）

### 2.1 脚本位置

`scripts/perf/k6_smoke.js`

该脚本覆盖以下接口链路：

1. 登录：`POST /auth/login`
2. 题目列表：`GET /problem/list`
3. 题目详情：`GET /problem/{id}`
4. 竞赛列表：`GET /contest/list`

### 2.2 运行前置

1. 启动 MySQL 并导入建表 SQL
2. 启动后端服务（默认 `8082`）
3. 确认有可登录账户（如 `admin2/admin123`）
4. 本机安装 k6（`k6 version` 可用）

### 2.3 执行命令

在目录 `onlinejudge-backend` 执行：

```bash
k6 run `
  -e BASE_URL=http://localhost:8082 `
  -e USERNAME=admin2 `
  -e PASSWORD=admin123 `
  -e PROBLEM_ID=1 `
  scripts/perf/k6_smoke.js
```

### 2.4 默认压测配置

- 0~30s：升到 5 并发用户
- 30~90s：升到 20 并发用户
- 90~120s：降到 0

阈值：

- 失败率：`http_req_failed < 2%`
- 95 分位响应时间：`http_req_duration p(95) < 800ms`

## 3. 结果记录建议（论文可直接引用）

建议截图并记录以下内容：

- 自动化测试总通过数（`mvn test` 输出）
- k6 摘要中的：
  - `http_req_failed`
  - `http_req_duration` 的 `p(95)` 与 `avg`
  - 吞吐（requests/s）

并在论文中给出结论：

- 功能正确性：自动化测试全部通过
- 稳定性：在设定并发与阈值下，失败率与延迟满足目标
