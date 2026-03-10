/* eslint-disable no-console */
const axios = require('axios')

const DEFAULT_BASE_URL = 'http://localhost:8082'
const BASE_URL = (process.env.SMOKE_BASE_URL || DEFAULT_BASE_URL).replace(/\/+$/, '')
const USERNAME = process.env.SMOKE_USERNAME || 'admin2'
const PASSWORD = process.env.SMOKE_PASSWORD || 'admin123'
const TIMEOUT = Number(process.env.SMOKE_TIMEOUT_MS || 15000)
const RAW_API_PREFIX = process.env.SMOKE_API_PREFIX
let apiPrefix = normalizePrefix(RAW_API_PREFIX)

const HELP_TEXT = `
Contest E2E Smoke Test

Usage:
  npm run smoke:contest
  npm run smoke:contest -- --help

Environment variables:
  SMOKE_BASE_URL    Backend base URL (default: http://localhost:8082)
  SMOKE_API_PREFIX  API prefix: "", "/api" (default: auto probe)
  SMOKE_USERNAME    Login username (default: admin2)
  SMOKE_PASSWORD    Login password (default: admin123)
  SMOKE_TIMEOUT_MS  Request timeout in ms (default: 15000)
`

if (process.argv.includes('--help') || process.argv.includes('-h')) {
  console.log(HELP_TEXT.trim())
  process.exit(0)
}

const client = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'application/json'
  }
})

const stepResults = []

function normalizePrefix(value) {
  const text = String(value == null ? '' : value).trim()
  if (!text || text === '/') return ''
  return text.startsWith('/') ? text : `/${text}`
}

function withPrefix(path) {
  if (!path.startsWith('/')) {
    throw new Error(`Path must start with "/": ${path}`)
  }
  return `${apiPrefix}${path}`
}

function normalizeApiResponse(response) {
  const body = response && response.data
  if (!body || typeof body !== 'object') {
    throw new Error('Invalid API response format')
  }
  if (Number(body.code) !== 200) {
    throw new Error(body.message || `API returned non-200 code: ${body.code}`)
  }
  return body.data
}

function resolveRecords(pageData) {
  if (!pageData || typeof pageData !== 'object') return []
  return Array.isArray(pageData.records) ? pageData.records : []
}

function toLocalDateTime(input) {
  const d = new Date(input)
  const yyyy = d.getFullYear()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}`
}

async function runStep(name, fn) {
  const start = Date.now()
  try {
    const data = await fn()
    const duration = Date.now() - start
    stepResults.push({ name, success: true, duration, error: '' })
    console.log(`OK   ${name} (${duration}ms)`)
    return data
  } catch (error) {
    const duration = Date.now() - start
    const message = error && error.message ? error.message : String(error)
    stepResults.push({ name, success: false, duration, error: message })
    console.error(`FAIL ${name} (${duration}ms): ${message}`)
    throw error
  }
}

function printSummaryAndExit(code) {
  const total = stepResults.length
  const passed = stepResults.filter((item) => item.success).length
  const failed = total - passed
  const totalCost = stepResults.reduce((sum, item) => sum + item.duration, 0)

  console.log('\nContest E2E Smoke Summary')
  console.log(`- base url: ${BASE_URL}`)
  console.log(`- total steps: ${total}`)
  console.log(`- passed: ${passed}`)
  console.log(`- failed: ${failed}`)
  console.log(`- total cost: ${totalCost}ms`)

  if (failed > 0) {
    console.log('- failed steps:')
    stepResults
      .filter((item) => !item.success)
      .forEach((item) => console.log(`  * ${item.name}: ${item.error}`))
  }
  process.exit(code)
}

async function loginWithProbe() {
  const autoProbe = RAW_API_PREFIX == null || String(RAW_API_PREFIX).trim() === ''
  const prefixes = autoProbe ? ['', '/api'] : [apiPrefix]
  let lastError = null

  for (const prefix of prefixes) {
    try {
      apiPrefix = prefix
      const res = await client.post(withPrefix('/auth/login'), { username: USERNAME, password: PASSWORD })
      return normalizeApiResponse(res)
    } catch (error) {
      lastError = error
      const status = error && error.response ? error.response.status : null
      if (autoProbe && status === 404) continue
      throw error
    }
  }

  throw lastError || new Error('Login probe failed')
}

async function main() {
  let createdContestId = null
  try {
    const loginData = await runStep('POST /auth/login', loginWithProbe)
    const token = loginData && loginData.token
    if (!token) throw new Error('Token not found in login response')
    client.defaults.headers.common.Authorization = `Bearer ${token}`

    const userInfo = await runStep('GET /user/info', async () => {
      const res = await client.get(withPrefix('/user/info'))
      return normalizeApiResponse(res)
    })

    const role = String(userInfo && userInfo.role ? userInfo.role : '').toUpperCase()
    if (!['ADMIN', 'TEACHER'].includes(role)) {
      throw new Error(`Contest e2e requires TEACHER/ADMIN account, current role: ${role || 'UNKNOWN'}`)
    }

    const problemPage = await runStep('GET /problem/list?page=1&size=1', async () => {
      const res = await client.get(withPrefix('/problem/list'), { params: { page: 1, size: 1 } })
      return normalizeApiResponse(res)
    })
    const problems = resolveRecords(problemPage)
    if (!problems.length || problems[0].id == null) {
      throw new Error('No problem found, cannot create contest in smoke test')
    }
    const problemId = Number(problems[0].id)

    const now = Date.now()
    const payload = {
      title: `Smoke Contest ${now}`,
      description: 'Temporary contest generated by smoke script. Safe to delete.',
      startTime: toLocalDateTime(now - 5 * 60 * 1000),
      endTime: toLocalDateTime(now + 55 * 60 * 1000),
      scoreboardFreezeTime: toLocalDateTime(now + 30 * 60 * 1000),
      problemIds: [problemId],
      status: 1,
      penaltyPerWrong: 20
    }

    createdContestId = await runStep('POST /contest', async () => {
      const res = await client.post(withPrefix('/contest'), payload)
      return normalizeApiResponse(res)
    })

    await runStep(`GET /contest/${createdContestId}`, async () => {
      const res = await client.get(withPrefix(`/contest/${createdContestId}`))
      return normalizeApiResponse(res)
    })

    await runStep(`POST /contest/${createdContestId}/join`, async () => {
      const res = await client.post(withPrefix(`/contest/${createdContestId}/join`))
      return normalizeApiResponse(res)
    })

    await runStep(`GET /contest/${createdContestId}/ranking?page=1&size=20`, async () => {
      const res = await client.get(withPrefix(`/contest/${createdContestId}/ranking`), {
        params: { page: 1, size: 20 }
      })
      return normalizeApiResponse(res)
    })

    await runStep(`GET /contest/${createdContestId}/score-snapshot?page=1&size=20`, async () => {
      const res = await client.get(withPrefix(`/contest/${createdContestId}/score-snapshot`), {
        params: { page: 1, size: 20 }
      })
      return normalizeApiResponse(res)
    })
  } finally {
    if (createdContestId != null) {
      try {
        await runStep(`DELETE /contest/${createdContestId}`, async () => {
          const res = await client.delete(withPrefix(`/contest/${createdContestId}`))
          return normalizeApiResponse(res)
        })
      } catch (error) {
        console.error(`WARN cleanup failed for contest ${createdContestId}: ${error.message}`)
      }
    }
  }
}

main()
  .then(() => printSummaryAndExit(0))
  .catch(() => printSummaryAndExit(1))
