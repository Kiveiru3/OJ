/* eslint-disable no-console */
'use strict'

const fs = require('fs')
const path = require('path')
const { spawnSync } = require('child_process')

const DEFAULT_INPUT = path.resolve(__dirname, 'data', 'luogu_offline_problems_1000_hidden_20_dense.json')

function parseArgs(argv) {
  const args = {
    baseUrl: 'http://localhost:8082/api',
    input: DEFAULT_INPUT,
    token: '',
    batchSize: 20,
    start: 1,
    end: 0,
    delay: 80,
    dryRun: false,
    continueOnError: false
  }

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--base-url') args.baseUrl = String(argv[++i] || args.baseUrl).replace(/\/$/, '')
    else if (arg === '--input') args.input = path.resolve(process.cwd(), String(argv[++i] || args.input))
    else if (arg === '--token') args.token = String(argv[++i] || '')
    else if (arg === '--batch-size') args.batchSize = Math.max(1, Number(argv[++i] || args.batchSize))
    else if (arg === '--start') args.start = Math.max(1, Number(argv[++i] || args.start))
    else if (arg === '--end') args.end = Math.max(0, Number(argv[++i] || args.end))
    else if (arg === '--delay') args.delay = Math.max(0, Number(argv[++i] || args.delay))
    else if (arg === '--dry-run') args.dryRun = true
    else if (arg === '--continue-on-error') args.continueOnError = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/import_in_order_batches.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --input <path>            Problem JSON path, default: ${DEFAULT_INPUT}`)
  console.log('  --token <jwt>             JWT token (required unless --dry-run)')
  console.log('  --base-url <url>          API base URL, default: http://localhost:8082/api')
  console.log('  --batch-size <n>          Number of problems per batch, default: 20')
  console.log('  --start <n>               Start index (1-based), default: 1')
  console.log('  --end <n>                 End index (1-based), 0 means to end of file')
  console.log('  --delay <ms>              Delay between each problem in a batch, default: 80')
  console.log('  --continue-on-error       Continue next batch when a batch fails')
  console.log('  --dry-run                 Validate and print batches only')
}

function buildImportArgs(config, start, limit) {
  const args = [
    path.resolve(__dirname, 'import_classic_problems.js'),
    '--base-url', config.baseUrl,
    '--input', config.input,
    '--start', String(start),
    '--limit', String(limit),
    '--delay', String(config.delay)
  ]
  if (config.dryRun) {
    args.push('--dry-run')
  } else {
    args.push('--token', config.token)
  }
  return args
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

  if (!args.dryRun && !args.token) {
    throw new Error('Missing --token')
  }

  const problems = JSON.parse(fs.readFileSync(args.input, 'utf8'))
  if (!Array.isArray(problems) || !problems.length) {
    throw new Error('Input JSON must be a non-empty array')
  }

  const total = problems.length
  const start = Math.min(Math.max(1, args.start), total)
  const end = args.end > 0 ? Math.min(args.end, total) : total
  if (start > end) {
    throw new Error(`Invalid range: start=${start}, end=${end}`)
  }

  const totalToImport = end - start + 1
  const totalBatches = Math.ceil(totalToImport / args.batchSize)
  let doneBatches = 0
  let failedBatches = 0

  console.log(`Input total: ${total}`)
  console.log(`Import range: ${start}-${end}, batchSize=${args.batchSize}, batches=${totalBatches}`)

  for (let s = start; s <= end; s += args.batchSize) {
    const limit = Math.min(args.batchSize, end - s + 1)
    doneBatches += 1

    console.log('')
    console.log(`=== Batch ${doneBatches}/${totalBatches} | start=${s}, limit=${limit} ===`)

    const childArgs = buildImportArgs(args, s, limit)
    const result = spawnSync(process.execPath, childArgs, {
      cwd: path.resolve(__dirname, '..'),
      stdio: 'inherit'
    })

    if (result.status !== 0) {
      failedBatches += 1
      console.error(`Batch failed: start=${s}, limit=${limit}, exitCode=${result.status}`)
      if (!args.continueOnError) {
        break
      }
    }
  }

  console.log('')
  console.log(`Batch import finished. totalBatches=${doneBatches}, failedBatches=${failedBatches}`)
  if (failedBatches > 0) {
    process.exitCode = 1
  }
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}

