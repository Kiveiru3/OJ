import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '60s', target: 20 },
    { duration: '30s', target: 0 }
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800']
  }
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const USERNAME = __ENV.USERNAME || 'admin2';
const PASSWORD = __ENV.PASSWORD || 'admin123';
const PROBLEM_ID = __ENV.PROBLEM_ID || '1';

function tryExtractToken(payload) {
  if (!payload || typeof payload !== 'object') {
    return '';
  }
  if (typeof payload.token === 'string') return payload.token;
  if (payload.data && typeof payload.data.token === 'string') return payload.data.token;
  if (payload.data && payload.data.accessToken) return payload.data.accessToken;
  return '';
}

export default function () {
  const loginRes = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ username: USERNAME, password: PASSWORD }),
    {
      headers: { 'Content-Type': 'application/json' }
    }
  );

  let loginJson = {};
  try {
    loginJson = loginRes.json();
  } catch (_) {
    loginJson = {};
  }

  const token = tryExtractToken(loginJson);

  check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'login result code is 200': () => Number(loginJson?.code) === 200,
    'token exists': () => token.length > 0
  });

  if (!token) {
    sleep(1);
    return;
  }

  const authHeaders = {
    Authorization: `Bearer ${token}`
  };

  const listRes = http.get(`${BASE_URL}/problem/list?page=1&size=20`, {
    headers: authHeaders
  });
  check(listRes, {
    'problem list status is 200': (r) => r.status === 200
  });

  const detailRes = http.get(`${BASE_URL}/problem/${PROBLEM_ID}`, {
    headers: authHeaders
  });
  check(detailRes, {
    'problem detail status is 200': (r) => r.status === 200
  });

  const contestRes = http.get(`${BASE_URL}/contest/list?page=1&size=20`, {
    headers: authHeaders
  });
  check(contestRes, {
    'contest list status is 200': (r) => r.status === 200
  });

  sleep(1);
}
