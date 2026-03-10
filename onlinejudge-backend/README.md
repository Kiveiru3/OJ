# OnlineJudge Backend Quick Start

## Requirements
- Java 17
- Maven 3.8+
- MySQL 8+
- Redis 6+
- Local toolchains for judging:
  - `javac` and `java`
  - `g++`
  - `python`

## Run
```powershell
mvn spring-boot:run
```

Service default address: `http://localhost:8082/api`

## Build / Test
```powershell
mvn -q -DskipTests compile
mvn -q test
```
Or use one-click script:
```powershell
.\scripts\run_backend_checks.ps1
```
Windows cmd:
```bat
scripts\run_backend_checks.bat
```
Project-level one-click acceptance (run from `D:\OJ-project`):
```powershell
.\scripts\run_acceptance_checks.ps1
```

## Smoke Test Script
```powershell
.\scripts\oj_smoke_test.ps1 -BaseUrl "http://localhost:8082/api" -Username "admin" -Password "admin123"
```

Optional submission check:
```powershell
.\scripts\oj_smoke_test.ps1 -SubmitProblemId 1001 -SubmitLanguage PYTHON
```

## Performance Smoke Test (k6)
```powershell
k6 run scripts/perf/k6_smoke.js
```
See `scripts/perf/README.md` for env variables.

## Core APIs Added
- Problem list supports hidden items for teacher/admin:
  - `GET /problem/list?includeHidden=true`
- Feature completion checklist (admin):
  - `GET /admin/system/feature-checklist`
- Runtime monitor (admin):
  - `GET /admin/system/monitor`
- Submission status polling: `GET /submission/{id}/status`
- Test case management (teacher/admin):
  - `GET /problem/{problemId}/test-cases`
  - `POST /problem/{problemId}/test-cases`
  - `PUT /problem/{problemId}/test-cases` (replace all non-sample cases)
  - `PUT /problem/{problemId}/test-cases/{testCaseId}`
  - `DELETE /problem/{problemId}/test-cases/{testCaseId}`

## Judge Command Configuration
Set in `src/main/resources/application.yml`:
```yaml
judge:
  java-compiler: javac
  java-runtime: java
  cpp-compiler: g++
  python-runtime: python
```

## Recommended DB Indexes
Run once on your `onlinejudge` database:
```sql
SOURCE scripts/add_core_indexes.sql;
```
