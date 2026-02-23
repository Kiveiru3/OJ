# Performance Test (k6)

## 1. Install k6

- Windows (choco): `choco install k6`
- Or download from: https://k6.io/docs/get-started/installation/

## 2. Start backend service

Ensure API is available, for example: `http://localhost:8082/api`

## 3. Run smoke load test

```bash
k6 run scripts/perf/k6_smoke.js
```

## 4. Run with custom env

```bash
k6 run `
  -e BASE_URL=http://localhost:8082/api `
  -e USERNAME=admin `
  -e PASSWORD=123456 `
  scripts/perf/k6_smoke.js
```

## 5. Current covered endpoints

- `POST /auth/login`
- `GET /problem/list`
- `GET /submission/list`
- `GET /contest/list`
- `GET /discussion/list`
- `GET /system/public-configs`

## 6. What to watch

- `http_req_failed` should remain low.
- `p(95)` latency should stay under threshold.
- If login fails, check test account status and password first.
