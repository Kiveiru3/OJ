/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')
const axios = require('axios')

const DEFAULT_INPUT = path.resolve(__dirname, 'data', 'classic_problems_200.json')

function parseArgs(argv) {
  const args = {
    baseUrl: 'http://localhost:8082/api',
    input: DEFAULT_INPUT,
    token: '',
    delay: 80,
    start: 1,
    limit: 0,
    dryRun: false
  }

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--base-url') args.baseUrl = String(argv[++i] || args.baseUrl).replace(/\/$/, '')
    else if (arg === '--input') args.input = path.resolve(process.cwd(), String(argv[++i] || args.input))
    else if (arg === '--token') args.token = String(argv[++i] || '')
    else if (arg === '--delay') args.delay = Number(argv[++i] || args.delay)
    else if (arg === '--start') args.start = Math.max(1, Number(argv[++i] || 1))
    else if (arg === '--limit') args.limit = Math.max(0, Number(argv[++i] || 0))
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/import_classic_problems.js --token <JWT_TOKEN> [options]')
  console.log('')
  console.log('Options:')
  console.log('  --base-url <url>   API base URL, default: http://localhost:8082/api')
  console.log('  --input <path>     Problem JSON file path')
  console.log('  --delay <ms>       Delay between each problem, default: 80')
  console.log('  --start <n>        Start index (1-based), default: 1')
  console.log('  --limit <n>        Max number of problems to import, 0 means all')
  console.log('  --dry-run          Validate data only, do not call API')
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchAllExistingTitles(client) {
  const titles = new Set()
  let page = 1
  const size = 200
  while (true) {
    const resp = await client.get('/problem/list', {
      params: {
        page,
        size,
        includeHidden: true
      }
    })
    const body = ensureResponseOk(resp, `Fetch problem list page ${page}`)
    const records = body.data?.records || body.data?.list || []
    if (!Array.isArray(records) || !records.length) {
      break
    }
    for (const item of records) {
      if (item && item.title) {
        titles.add(String(item.title).trim())
      }
    }
    if (records.length < size) {
      break
    }
    page += 1
  }
  return titles
}

function ensureResponseOk(resp, action) {
  const body = resp && resp.data
  if (!body || typeof body !== 'object') {
    throw new Error(`${action}: invalid response`)
  }
  if (body.code !== 200) {
    throw new Error(`${action}: ${body.message || 'request failed'}`)
  }
  return body
}

function extractProblemId(body) {
  const data = body.data
  if (typeof data === 'number') return data
  if (typeof data === 'string' && /^\d+$/.test(data)) return Number(data)
  if (data && typeof data === 'object' && data.id) return Number(data.id)
  return NaN
}

function validateProblem(problem, idx) {
  const required = ['title', 'difficulty', 'description', 'sampleInput', 'sampleOutput']
  for (const key of required) {
    if (!problem[key] && problem[key] !== 0) {
      throw new Error(`Problem #${idx}: missing field ${key}`)
    }
  }
  if (!Array.isArray(problem.testCases) || !problem.testCases.length) {
    throw new Error(`Problem #${idx}: testCases is empty`)
  }
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }
  if (!fs.existsSync(args.input)) {
    throw new Error(`Input file not found: ${args.input}`)
  }
  const raw = fs.readFileSync(args.input, 'utf8')
  const allProblems = JSON.parse(raw)
  if (!Array.isArray(allProblems) || !allProblems.length) {
    throw new Error('Input JSON must be a non-empty array')
  }

  const from = args.start - 1
  const until = args.limit > 0 ? from + args.limit : allProblems.length
  const problems = allProblems.slice(from, until)

  problems.forEach((p, i) => validateProblem(p, from + i + 1))

  console.log(`Loaded ${allProblems.length} problems, importing ${problems.length} from index ${args.start}`)
  if (args.dryRun) {
    console.log('Dry run mode, no API requests sent.')
    return
  }
  if (!args.token) {
    throw new Error('Missing --token')
  }

  const client = axios.create({
    baseURL: args.baseUrl,
    timeout: 15000,
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      Authorization: `Bearer ${args.token}`
    }
  })

  let success = 0
  let skipped = 0
  const failed = []
  const existingTitles = await fetchAllExistingTitles(client)
  console.log(`Existing titles in DB: ${existingTitles.size}`)

  for (let i = 0; i < problems.length; i += 1) {
    const p = problems[i]
    const seq = from + i + 1
    try {
      const title = String(p.title || '').trim()
      if (existingTitles.has(title)) {
        skipped += 1
        console.log(`[SKIP] #${seq} title already exists -> ${title}`)
        continue
      }

      const createPayload = {
        title,
        difficulty: p.difficulty,
        status: Number(p.status ?? 1),
        description: p.description,
        inputFormat: p.inputFormat || '',
        outputFormat: p.outputFormat || '',
        sampleInput: p.sampleInput || '',
        sampleOutput: p.sampleOutput || '',
        timeLimit: Number(p.timeLimit || 2000),
        memoryLimit: Number(p.memoryLimit || 256000)
      }

      const createResp = await client.post('/problem/create', createPayload)
      const createBody = ensureResponseOk(createResp, `Create problem #${seq}`)
      const problemId = extractProblemId(createBody)
      if (!Number.isFinite(problemId) || problemId <= 0) {
        throw new Error(`Create problem #${seq}: cannot parse problem id`)
      }

      for (const tc of p.testCases) {
        const caseResp = await client.post(`/problem/${problemId}/test-cases`, {
          input: tc.input ?? '',
          output: tc.output ?? ''
        })
        ensureResponseOk(caseResp, `Create test case for problem #${seq}`)
      }

      success += 1
      existingTitles.add(title)
      console.log(`[${success}/${problems.length}] Imported #${seq} -> problemId=${problemId} | ${p.title}`)
      if (args.delay > 0) {
        await sleep(args.delay)
      }
    } catch (error) {
      const msg = error.response?.data?.message || error.message || 'unknown error'
      failed.push({ seq, title: p.title, message: msg })
      console.error(`[FAIL] #${seq} ${p.title} -> ${msg}`)
    }
  }

  console.log('')
  console.log(`Done. Success: ${success}, Skipped: ${skipped}, Failed: ${failed.length}`)
  if (failed.length) {
    console.log('Failed list:')
    failed.forEach((f) => {
      console.log(`  #${f.seq} ${f.title}: ${f.message}`)
    })
    process.exitCode = 1
  }
}

main().catch((error) => {
  console.error(error.message || error)
  process.exit(1)
})
