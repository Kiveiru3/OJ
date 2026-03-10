/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const DEFAULT_INPUT = path.resolve(__dirname, 'data', 'tk_free_problems_200.json')
const DEFAULT_OUTPUT = path.resolve(__dirname, 'data', 'tk_free_problems_200_with_hidden_cases.json')
const DEFAULT_ZIP_DIR = 'D:\\BaiduNetdiskDownload\\tiku\\zipdata'
const DEFAULT_EXTRACT_DIR = 'D:\\BaiduNetdiskDownload\\tiku\\zipdata_extracted'

function parseArgs(argv) {
  const args = {
    input: DEFAULT_INPUT,
    output: DEFAULT_OUTPUT,
    zipDir: DEFAULT_ZIP_DIR,
    extractDir: DEFAULT_EXTRACT_DIR,
    maxCases: 30,
    forceExtract: false
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--input') args.input = path.resolve(String(argv[++i] || args.input))
    else if (arg === '--output') args.output = path.resolve(String(argv[++i] || args.output))
    else if (arg === '--zip-dir') args.zipDir = path.resolve(String(argv[++i] || args.zipDir))
    else if (arg === '--extract-dir') args.extractDir = path.resolve(String(argv[++i] || args.extractDir))
    else if (arg === '--max-cases') args.maxCases = Math.max(1, Number(argv[++i] || args.maxCases))
    else if (arg === '--force-extract') args.forceExtract = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/merge_tk_zip_testcases.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --input <path>        Problem JSON file, default: ${DEFAULT_INPUT}`)
  console.log(`  --output <path>       Output merged JSON, default: ${DEFAULT_OUTPUT}`)
  console.log(`  --zip-dir <path>      Zip directory, default: ${DEFAULT_ZIP_DIR}`)
  console.log(`  --extract-dir <path>  Zip extract directory, default: ${DEFAULT_EXTRACT_DIR}`)
  console.log('  --max-cases <n>       Max test cases per problem, default: 30')
  console.log('  --force-extract       Force re-extract all zip files')
}

function ensureDir(fileOrDir, isFilePath = false) {
  const dir = isFilePath ? path.dirname(fileOrDir) : fileOrDir
  fs.mkdirSync(dir, { recursive: true })
}

function walkFiles(dirPath, out) {
  if (!fs.existsSync(dirPath)) return
  const entries = fs.readdirSync(dirPath, { withFileTypes: true })
  for (const e of entries) {
    const full = path.join(dirPath, e.name)
    if (e.isDirectory()) walkFiles(full, out)
    else if (e.isFile()) out.push(full)
  }
}

function psEscape(p) {
  return String(p).replace(/'/g, "''")
}

function expandZip(zipFile, toDir) {
  const cmd = `$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force -Path '${psEscape(toDir)}' | Out-Null; Expand-Archive -LiteralPath '${psEscape(zipFile)}' -DestinationPath '${psEscape(toDir)}' -Force`
  execFileSync('powershell', ['-NoProfile', '-Command', cmd], { stdio: 'ignore' })
}

function decodeText(buffer) {
  const candidates = ['utf-8', 'gbk']
  let best = ''
  let bestScore = Number.POSITIVE_INFINITY
  for (const enc of candidates) {
    try {
      const text = new TextDecoder(enc).decode(buffer)
      const bad = (text.match(/[�]/g) || []).length
      const score = bad / Math.max(1, text.length)
      if (score < bestScore) {
        bestScore = score
        best = text
      }
    } catch (_) {
      // ignore
    }
  }
  return best.replace(/\r/g, '').trimEnd()
}

function basenameNoExt(p) {
  return path.basename(p).replace(/\.[^.]+$/, '')
}

function naturalCompare(a, b) {
  return String(a).localeCompare(String(b), 'en', { numeric: true, sensitivity: 'base' })
}

function collectCasesFromDir(problemDir, maxCases) {
  const files = []
  walkFiles(problemDir, files)
  const inMap = new Map()
  const outMap = new Map()

  for (const file of files) {
    const ext = path.extname(file).toLowerCase()
    const base = basenameNoExt(file).toLowerCase()
    if (ext === '.in' || ext === '.input' || ext === '.txt') {
      if (!inMap.has(base)) inMap.set(base, file)
    } else if (ext === '.out' || ext === '.ans' || ext === '.output') {
      if (!outMap.has(base)) outMap.set(base, file)
    }
  }

  const keys = [...new Set([...inMap.keys()].filter((k) => outMap.has(k)))]
  keys.sort(naturalCompare)
  const testCases = []
  for (const k of keys) {
    if (testCases.length >= maxCases) break
    const input = decodeText(fs.readFileSync(inMap.get(k)))
    const output = decodeText(fs.readFileSync(outMap.get(k)))
    testCases.push({ input, output })
  }
  return testCases
}

function getExternalId(problem) {
  if (problem && /^\d+$/.test(String(problem.externalId || ''))) return String(problem.externalId)
  const title = String(problem && problem.title ? problem.title : '')
  const m = title.match(/^(\d+)\s*[:：]/)
  return m ? m[1] : ''
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
  if (!fs.existsSync(args.zipDir)) {
    throw new Error(`Zip dir not found: ${args.zipDir}`)
  }

  ensureDir(args.extractDir)
  ensureDir(args.output, true)

  const zipFiles = fs.readdirSync(args.zipDir)
    .filter((n) => n.toLowerCase().endsWith('.zip'))
    .map((n) => path.join(args.zipDir, n))
  const idToCases = new Map()
  let extracted = 0

  for (const zipFile of zipFiles) {
    const id = path.basename(zipFile, '.zip')
    if (!/^\d+$/.test(id)) continue
    const toDir = path.join(args.extractDir, id)
    const needExtract = args.forceExtract || !fs.existsSync(toDir) || fs.readdirSync(toDir).length === 0
    if (needExtract) {
      try {
        expandZip(zipFile, toDir)
        extracted += 1
      } catch (_) {
        continue
      }
    }
    const cases = collectCasesFromDir(toDir, args.maxCases)
    if (cases.length > 0) {
      idToCases.set(id, cases)
    }
  }

  const problems = JSON.parse(fs.readFileSync(args.input, 'utf8'))
  if (!Array.isArray(problems)) {
    throw new Error('Input JSON must be an array.')
  }

  let merged = 0
  let noExternalId = 0
  for (const p of problems) {
    const id = getExternalId(p)
    if (!id) {
      noExternalId += 1
      continue
    }
    const cases = idToCases.get(id)
    if (!cases || !cases.length) continue
    p.testCases = cases
    merged += 1
  }

  fs.writeFileSync(args.output, JSON.stringify(problems, null, 2), 'utf8')
  console.log(`zip files: ${zipFiles.length}, extracted: ${extracted}, withCases: ${idToCases.size}`)
  console.log(`problems: ${problems.length}, merged: ${merged}, missingExternalId: ${noExternalId}`)
  console.log(`output: ${args.output}`)
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}

