import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    smoke: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 }
      ],
      gracefulRampDown: '10s'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1200']
  }
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082/api';
const USERNAME = __ENV.USERNAME || 'admin';
const PASSWORD = __ENV.PASSWORD || '123456';

function jsonHeaders(token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return { headers };
}

function login() {
  const payload = JSON.stringify({
    username: USERNAME,
    password: PASSWORD
  });

  const res = http.post(`${BASE_URL}/auth/login`, payload, jsonHeaders());
  check(res, {
    'login status is 200': (r) => r.status === 200,
    'login code is 200': (r) => {
      try {
        return r.json('code') === 200;
      } catch (_) {
        return false;
      }
    }
  });

  if (res.status !== 200) {
    return null;
  }
  try {
    return res.json('data.token');
  } catch (_) {
    return null;
  }
}

function getWithToken(path, token) {
  const res = http.get(`${BASE_URL}${path}`, jsonHeaders(token));
  check(res, {
    [`${path} status is 200`]: (r) => r.status === 200,
    [`${path} code is 200`]: (r) => {
      try {
        return r.json('code') === 200;
      } catch (_) {
        return false;
      }
    }
  });
  return res;
}

export default function () {
  const token = login();
  if (!token) {
    sleep(1);
    return;
  }

  getWithToken('/problem/list?page=1&size=10', token);
  getWithToken('/submission/list?page=1&size=10', token);
  getWithToken('/contest/list?page=1&size=10', token);
  getWithToken('/discussion/list?page=1&size=10', token);
  getWithToken('/system/public-configs', token);

  sleep(1);
}
