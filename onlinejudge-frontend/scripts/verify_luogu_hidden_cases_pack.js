/* eslint-disable no-console */
'use strict'

const fs = require('fs')
const path = require('path')
const { TARGET_IDS, RECIPES } = require('./lib/luogu_hidden_20_recipes')

const DEFAULT_INPUT = path.resolve(__dirname, 'data', 'luogu_offline_problems_1000_hidden_20.json')
const DEFAULT_LIMIT_PER_PROBLEM = 200

function parseArgs(argv) {
  const args = {
    input: DEFAULT_INPUT,
    limitPerProblem: DEFAULT_LIMIT_PER_PROBLEM,
    sampleOnly: false
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--input') args.input = path.resolve(process.cwd(), String(argv[++i] || args.input))
    else if (arg === '--limit-per-problem') args.limitPerProblem = Math.max(1, Number(argv[++i] || args.limitPerProblem))
    else if (arg === '--sample-only') args.sampleOnly = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/verify_luogu_hidden_cases_pack.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --input <path>               Input JSON. default: ${DEFAULT_INPUT}`)
  console.log(`  --limit-per-problem <n>      Max checked test cases per problem. default: ${DEFAULT_LIMIT_PER_PROBLEM}`)
  console.log('  --sample-only                Only check official sampleInput/sampleOutput.')
}

function normalize(text) {
  return String(text == null ? '' : text).trim()
}

function checkOneCase(recipe, input, expected) {
  const actual = normalize(recipe.solve(input))
  const exp = normalize(expected)
  return {
    ok: actual === exp,
    actual,
    expected: exp
  }
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }
  if (!fs.existsSync(args.input)) {
    throw new Error(`Input file not found: ${args.input}`)
  }

  const allProblems = JSON.parse(fs.readFileSync(args.input, 'utf8'))
  if (!Array.isArray(allProblems)) {
    throw new Error('Input must be a JSON array')
  }

  const byId = new Map(allProblems.map((p) => [String(p.externalId || ''), p]))
  const failures = []
  const summary = []

  for (const pid of TARGET_IDS) {
    const recipe = RECIPES[pid]
    const p = byId.get(pid)
    if (!recipe || !p) {
      failures.push({
        externalId: pid,
        phase: 'existence',
        message: 'missing problem or recipe'
      })
      continue
    }

    let checked = 0
    let passed = 0

    if (normalize(p.sampleInput) && normalize(p.sampleOutput)) {
      const sampleResult = checkOneCase(recipe, p.sampleInput, p.sampleOutput)
      checked += 1
      if (sampleResult.ok) {
        passed += 1
      } else {
        failures.push({
          externalId: pid,
          title: p.title,
          phase: 'sample',
          input: p.sampleInput,
          expected: sampleResult.expected,
          actual: sampleResult.actual
        })
      }
    }

    if (!args.sampleOnly && Array.isArray(p.testCases)) {
      const upper = Math.min(args.limitPerProblem, p.testCases.length)
      for (let i = 0; i < upper; i += 1) {
        const tc = p.testCases[i]
        if (!tc || !normalize(tc.input) || !normalize(tc.output)) continue
        const caseResult = checkOneCase(recipe, tc.input, tc.output)
        checked += 1
        if (caseResult.ok) {
          passed += 1
        } else {
          failures.push({
            externalId: pid,
            title: p.title,
            phase: 'test_case',
            caseIndex: i,
            input: tc.input,
            expected: caseResult.expected,
            actual: caseResult.actual
          })
          break
        }
      }
    }

    summary.push({
      externalId: pid,
      title: p.title,
      checked,
      passed
    })
  }

  console.log(`Checked ${summary.length} target problems.`)
  for (const row of summary) {
    console.log(`[${row.externalId}] checked=${row.checked}, passed=${row.passed} | ${row.title}`)
  }
  if (failures.length > 0) {
    console.error(`Verification failed: ${failures.length} issue(s).`)
    for (const f of failures.slice(0, 20)) {
      console.error(JSON.stringify(f, null, 2))
    }
    process.exit(1)
  }
  console.log('Verification passed.')
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}

