/* eslint-disable no-console */
'use strict'

const fs = require('fs')
const path = require('path')
const { TARGET_IDS, RECIPES, createRng } = require('./lib/luogu_hidden_20_recipes')

const DEFAULT_INPUT = path.resolve(__dirname, 'data', 'luogu_offline_problems_1000.json')
const DEFAULT_OUTPUT = path.resolve(__dirname, 'data', 'luogu_offline_problems_1000_hidden_20.json')
const DEFAULT_REPORT = path.resolve(__dirname, 'data', 'luogu_hidden_20_report.json')

function parseArgs(argv) {
  const args = {
    input: DEFAULT_INPUT,
    output: DEFAULT_OUTPUT,
    report: DEFAULT_REPORT,
    seed: 20260225,
    hiddenPerProblem: 0,
    keepExistingCases: true
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--input') args.input = path.resolve(process.cwd(), String(argv[++i] || args.input))
    else if (arg === '--output') args.output = path.resolve(process.cwd(), String(argv[++i] || args.output))
    else if (arg === '--report') args.report = path.resolve(process.cwd(), String(argv[++i] || args.report))
    else if (arg === '--seed') args.seed = Number(argv[++i] || args.seed)
    else if (arg === '--hidden-per-problem') args.hiddenPerProblem = Math.max(0, Number(argv[++i] || 0))
    else if (arg === '--replace-cases') args.keepExistingCases = false
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/generate_luogu_hidden_cases_pack.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --input <path>                Input problem JSON. default: ${DEFAULT_INPUT}`)
  console.log(`  --output <path>               Output merged JSON. default: ${DEFAULT_OUTPUT}`)
  console.log(`  --report <path>               Generation report JSON. default: ${DEFAULT_REPORT}`)
  console.log('  --seed <number>               Random seed. default: 20260225')
  console.log('  --hidden-per-problem <n>      Force hidden case count for each target problem.')
  console.log('  --replace-cases               Replace existing testCases (default is append).')
}

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
}

function normalizeText(value) {
  return String(value == null ? '' : value).trim()
}

function isValidCase(tc) {
  if (!tc || typeof tc !== 'object') return false
  const input = normalizeText(tc.input)
  const output = normalizeText(tc.output)
  return input.length > 0 && output.length > 0
}

function mergeCases(existingCases, hiddenCases) {
  const seen = new Set()
  const out = []

  function pushCase(tc) {
    if (!isValidCase(tc)) return
    const key = `${normalizeText(tc.input)}\n---\n${normalizeText(tc.output)}`
    if (seen.has(key)) return
    seen.add(key)
    out.push({
      input: String(tc.input),
      output: String(tc.output)
    })
  }

  for (const tc of existingCases) pushCase(tc)
  for (const tc of hiddenCases) pushCase(tc)
  return out
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

  const rng = createRng(args.seed)
  const report = {
    input: args.input,
    output: args.output,
    seed: args.seed,
    keepExistingCases: args.keepExistingCases,
    totalProblems: allProblems.length,
    targetProblemCount: TARGET_IDS.length,
    generatedCaseCount: 0,
    mergedCaseCount: 0,
    missingTargets: [],
    detail: []
  }

  const byId = new Map(allProblems.map((p, idx) => [String(p.externalId || ''), { problem: p, index: idx }]))

  for (const pid of TARGET_IDS) {
    const recipe = RECIPES[pid]
    const found = byId.get(pid)
    if (!recipe || !found) {
      report.missingTargets.push(pid)
      continue
    }

    const p = found.problem
    const targetHidden = args.hiddenPerProblem > 0 ? args.hiddenPerProblem : recipe.hiddenCount
    const hiddenCases = []
    const seenInput = new Set()
    const attemptsLimit = Math.max(200, targetHidden * 80)

    const baseCases = Array.isArray(p.testCases) ? p.testCases.filter(isValidCase) : []
    for (const tc of baseCases) {
      seenInput.add(normalizeText(tc.input))
    }
    if (isValidCase({ input: p.sampleInput, output: p.sampleOutput })) {
      seenInput.add(normalizeText(p.sampleInput))
    }

    let attempts = 0
    while (hiddenCases.length < targetHidden && attempts < attemptsLimit) {
      attempts += 1
      const input = String(recipe.buildCase(rng))
      const key = normalizeText(input)
      if (!key || seenInput.has(key)) continue

      const output = String(recipe.solve(input))
      const recheck = String(recipe.solve(input))
      if (normalizeText(output) !== normalizeText(recheck)) {
        throw new Error(`Non-deterministic solver for ${pid}`)
      }
      hiddenCases.push({ input, output })
      seenInput.add(key)
    }

    if (hiddenCases.length < targetHidden) {
      throw new Error(`Failed to generate enough hidden cases for ${pid}: need ${targetHidden}, got ${hiddenCases.length}`)
    }

    report.generatedCaseCount += hiddenCases.length
    const existingOrSample = []
    if (args.keepExistingCases && baseCases.length) {
      existingOrSample.push(...baseCases)
    } else if (isValidCase({ input: p.sampleInput, output: p.sampleOutput })) {
      existingOrSample.push({ input: p.sampleInput, output: p.sampleOutput })
    }

    const merged = mergeCases(existingOrSample, hiddenCases)
    p.testCases = merged
    report.mergedCaseCount += merged.length
    report.detail.push({
      externalId: pid,
      title: p.title,
      recipe: recipe.name,
      existingBefore: baseCases.length,
      hiddenAdded: hiddenCases.length,
      finalCases: merged.length,
      generationAttempts: attempts
    })
  }

  ensureDir(args.output)
  ensureDir(args.report)
  fs.writeFileSync(args.output, JSON.stringify(allProblems, null, 2), 'utf8')
  fs.writeFileSync(args.report, JSON.stringify(report, null, 2), 'utf8')

  console.log(`Done. target=${TARGET_IDS.length}, missing=${report.missingTargets.length}, generatedHidden=${report.generatedCaseCount}`)
  console.log(`Output: ${args.output}`)
  console.log(`Report: ${args.report}`)
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}

