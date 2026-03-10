/* eslint-disable no-console */
const axios = require('axios');

const DEFAULT_BASE_URL = 'http://localhost:8082';
const BASE_URL = (process.env.SMOKE_BASE_URL || DEFAULT_BASE_URL).replace(/\/+$/, '');
const USERNAME = process.env.SMOKE_USERNAME || 'admin2';
const PASSWORD = process.env.SMOKE_PASSWORD || 'admin123';
const TIMEOUT = Number(process.env.SMOKE_TIMEOUT_MS || 15000);
const RAW_API_PREFIX = process.env.SMOKE_API_PREFIX;
let apiPrefix = normalizePrefix(RAW_API_PREFIX);

const HELP_TEXT = `
OnlineJudge Smoke Test

Usage:
  npm run smoke:test
  npm run smoke:test -- --help

Environment variables:
  SMOKE_BASE_URL    Backend base URL (default: http://localhost:8082)
  SMOKE_API_PREFIX  API prefix: "", "/api" (default: auto probe)
  SMOKE_USERNAME    Login username (default: admin2)
  SMOKE_PASSWORD    Login password (default: admin123)
  SMOKE_TIMEOUT_MS  Request timeout in ms (default: 15000)
`;

if (process.argv.includes('--help') || process.argv.includes('-h')) {
  console.log(HELP_TEXT.trim());
  process.exit(0);
}

const client = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json'
  }
});

const stepResults = [];

function normalizePrefix(value) {
  const text = String(value == null ? '' : value).trim();
  if (!text) return '';
  if (text === '/') return '';
  return text.startsWith('/') ? text : `/${text}`;
}

function withPrefix(path) {
  if (!path.startsWith('/')) {
    throw new Error(`Path must start with "/": ${path}`);
  }
  return `${apiPrefix}${path}`;
}

function normalizeApiResponse(response) {
  const body = response && response.data;
  if (!body || typeof body !== 'object') {
    throw new Error('Invalid API response format');
  }
  if (Number(body.code) !== 200) {
    throw new Error(body.message || `API returned non-200 code: ${body.code}`);
  }
  return body.data;
}

function resolveRecords(pageData) {
  if (!pageData || typeof pageData !== 'object') return [];
  if (Array.isArray(pageData.records)) return pageData.records;
  return [];
}

async function runStep(name, fn) {
  const start = Date.now();
  try {
    const data = await fn();
    const duration = Date.now() - start;
    stepResults.push({ name, success: true, duration, error: '' });
    console.log(`OK   ${name} (${duration}ms)`);
    return data;
  } catch (error) {
    const duration = Date.now() - start;
    const message = error && error.message ? error.message : String(error);
    stepResults.push({ name, success: false, duration, error: message });
    console.error(`FAIL ${name} (${duration}ms): ${message}`);
    throw error;
  }
}

function printSummaryAndExit(code) {
  const total = stepResults.length;
  const passed = stepResults.filter((item) => item.success).length;
  const failed = total - passed;
  const totalCost = stepResults.reduce((sum, item) => sum + item.duration, 0);

  console.log('\nSmoke Summary');
  console.log(`- base url: ${BASE_URL}`);
  console.log(`- total steps: ${total}`);
  console.log(`- passed: ${passed}`);
  console.log(`- failed: ${failed}`);
  console.log(`- total cost: ${totalCost}ms`);

  if (failed > 0) {
    console.log('- failed steps:');
    stepResults
      .filter((item) => !item.success)
      .forEach((item) => console.log(`  * ${item.name}: ${item.error}`));
  }
  process.exit(code);
}

async function main() {
  const loginData = await runStep('POST /auth/login', async () => {
    const autoProbe = RAW_API_PREFIX == null || String(RAW_API_PREFIX).trim() === '';
    const prefixes = autoProbe ? ['', '/api'] : [apiPrefix];
    let lastError = null;

    for (const prefix of prefixes) {
      try {
        apiPrefix = prefix;
        const res = await client.post(withPrefix('/auth/login'), {
          username: USERNAME,
          password: PASSWORD
        });
        return normalizeApiResponse(res);
      } catch (error) {
        lastError = error;
        const status = error && error.response ? error.response.status : null;
        if (autoProbe && status === 404) {
          continue;
        }
        throw error;
      }
    }
    throw lastError || new Error('Login probe failed');
  });

  const token = loginData && loginData.token;
  if (!token) {
    throw new Error('Token not found in login response');
  }
  client.defaults.headers.common.Authorization = `Bearer ${token}`;

  const userInfo = await runStep('GET /user/info', async () => {
    const res = await client.get(withPrefix('/user/info'));
    return normalizeApiResponse(res);
  });

  const role = String(userInfo && userInfo.role ? userInfo.role : '').toUpperCase();

  const problemPage = await runStep('GET /problem/list?page=1&size=10', async () => {
    const res = await client.get(withPrefix('/problem/list'), { params: { page: 1, size: 10 } });
    return normalizeApiResponse(res);
  });
  const problems = resolveRecords(problemPage);

  if (problems.length > 0 && problems[0] && problems[0].id != null) {
    await runStep(`GET /problem/${problems[0].id}`, async () => {
      const res = await client.get(withPrefix(`/problem/${problems[0].id}`));
      return normalizeApiResponse(res);
    });
  }

  const contestPage = await runStep('GET /contest/list?page=1&size=10', async () => {
    const res = await client.get(withPrefix('/contest/list'), { params: { page: 1, size: 10 } });
    return normalizeApiResponse(res);
  });
  const contests = resolveRecords(contestPage);

  if (contests.length > 0 && contests[0] && contests[0].id != null) {
    const contestId = contests[0].id;
    await runStep(`GET /contest/${contestId}`, async () => {
      const res = await client.get(withPrefix(`/contest/${contestId}`));
      return normalizeApiResponse(res);
    });
    await runStep(`GET /contest/${contestId}/ranking?page=1&size=20`, async () => {
      const res = await client.get(withPrefix(`/contest/${contestId}/ranking`), { params: { page: 1, size: 20 } });
      return normalizeApiResponse(res);
    });
    if (role === 'ADMIN' || role === 'TEACHER') {
      await runStep(`GET /contest/${contestId}/score-snapshot?page=1&size=20`, async () => {
        const res = await client.get(withPrefix(`/contest/${contestId}/score-snapshot`), { params: { page: 1, size: 20 } });
        return normalizeApiResponse(res);
      });
    }
  }

  const submissionPage = await runStep('GET /submission/list?page=1&size=10', async () => {
    const res = await client.get(withPrefix('/submission/list'), { params: { page: 1, size: 10 } });
    return normalizeApiResponse(res);
  });
  const submissions = resolveRecords(submissionPage);

  if ((role === 'ADMIN' || role === 'TEACHER') && submissions.length > 0 && submissions[0]?.id != null) {
    await runStep(`POST /submission/${submissions[0].id}/rejudge`, async () => {
      try {
        const res = await client.post(withPrefix(`/submission/${submissions[0].id}/rejudge`));
        return normalizeApiResponse(res);
      } catch (error) {
        const statusCode = error?.response?.status;
        if (statusCode === 404) {
          throw new Error('Rejudge endpoint missing (404). Restart backend with latest code.');
        }
        throw error;
      }
    });
  }

  const postPage = await runStep('GET /discussion/list?page=1&size=10', async () => {
    const res = await client.get(withPrefix('/discussion/list'), { params: { page: 1, size: 10 } });
    return normalizeApiResponse(res);
  });
  const posts = resolveRecords(postPage);
  if (posts.length > 0 && posts[0] && posts[0].id != null) {
    await runStep(`GET /discussion/${posts[0].id}`, async () => {
      const res = await client.get(withPrefix(`/discussion/${posts[0].id}`));
      return normalizeApiResponse(res);
    });
  }

  if (role === 'ADMIN') {
    await runStep('GET /admin/system/configs', async () => {
      const res = await client.get(withPrefix('/admin/system/configs'));
      return normalizeApiResponse(res);
    });
    await runStep('GET /admin/system/logs?page=1&size=20', async () => {
      const res = await client.get(withPrefix('/admin/system/logs'), { params: { page: 1, size: 20 } });
      return normalizeApiResponse(res);
    });
    await runStep('GET /admin/system/monitor', async () => {
      const res = await client.get(withPrefix('/admin/system/monitor'));
      return normalizeApiResponse(res);
    });
    await runStep('GET /admin/system/feature-checklist', async () => {
      const res = await client.get(withPrefix('/admin/system/feature-checklist'));
      return normalizeApiResponse(res);
    });
    await runStep('GET /admin/system/judge-results?page=1&size=20', async () => {
      const res = await client.get(withPrefix('/admin/system/judge-results'), { params: { page: 1, size: 20 } });
      return normalizeApiResponse(res);
    });
  }

  if ((role === 'ADMIN' || role === 'TEACHER') && problems.length > 0 && problems[0]?.title) {
    await runStep('POST /problem/batch-import (skip existing title)', async () => {
      const payload = {
        skipExistingTitle: true,
        problems: [
          {
            title: String(problems[0].title),
            description: 'Smoke test duplicate title probe',
            inputFormat: 'N/A',
            outputFormat: 'N/A',
            sampleInput: '',
            sampleOutput: '',
            difficulty: 'EASY',
            status: 1,
            testCases: []
          }
        ]
      };
      try {
        const res = await client.post(withPrefix('/problem/batch-import'), payload);
        return normalizeApiResponse(res);
      } catch (error) {
        const statusCode = error?.response?.status;
        if (statusCode === 404) {
          throw new Error('Batch import endpoint missing (404). Restart backend with latest code.');
        }
        throw error;
      }
    });
  }
}

main()
  .then(() => printSummaryAndExit(0))
  .catch(() => printSummaryAndExit(1));
