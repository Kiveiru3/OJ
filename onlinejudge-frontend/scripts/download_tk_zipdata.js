/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const DEFAULT_INDEX = 'D:\\BaiduNetdiskDownload\\tiku\\tk_free_index.json'
const DEFAULT_OUT_DIR = 'D:\\BaiduNetdiskDownload\\tiku\\zipdata'
const DEFAULT_FAIL_DIR = 'D:\\BaiduNetdiskDownload\\tiku\\zipdata_failed'

function parseArgs(argv) {
  const args = {
    index: DEFAULT_INDEX,
    outDir: DEFAULT_OUT_DIR,
    failDir: DEFAULT_FAIL_DIR,
    cookie: process.env.TK_COOKIE || '',
    cookieFile: '',
    start: 1,
    limit: 0,
    delay: 150,
    force: false
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--index') args.index = path.resolve(String(argv[++i] || args.index))
    else if (arg === '--out-dir') args.outDir = path.resolve(String(argv[++i] || args.outDir))
    else if (arg === '--fail-dir') args.failDir = path.resolve(String(argv[++i] || args.failDir))
    else if (arg === '--cookie') args.cookie = String(argv[++i] || '')
    else if (arg === '--cookie-file') args.cookieFile = path.resolve(String(argv[++i] || ''))
    else if (arg === '--start') args.start = Math.max(1, Number(argv[++i] || 1))
    else if (arg === '--limit') args.limit = Math.max(0, Number(argv[++i] || 0))
    else if (arg === '--delay') args.delay = Math.max(0, Number(argv[++i] || 0))
    else if (arg === '--force') args.force = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/download_tk_zipdata.js --cookie "<Cookie>" [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --index <path>       tk index json, default: ${DEFAULT_INDEX}`)
  console.log(`  --out-dir <path>     zip output dir, default: ${DEFAULT_OUT_DIR}`)
  console.log(`  --fail-dir <path>    failed html dir, default: ${DEFAULT_FAIL_DIR}`)
  console.log('  --cookie <string>    Cookie header value from logged-in browser')
  console.log('  --cookie-file <path> Read cookie from file (first line)')
  console.log('  --start <n>          Start from nth id (1-based), default: 1')
  console.log('  --limit <n>          Max downloads, 0 = all')
  console.log('  --delay <ms>         Delay between requests, default: 150')
  console.log('  --force              Overwrite existing zip files')
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true })
}

function loadIdsFromIndex(indexFile) {
  if (!fs.existsSync(indexFile)) {
    throw new Error(`Index file not found: ${indexFile}`)
  }
  const json = JSON.parse(fs.readFileSync(indexFile, 'utf8'))
  const list = Array.isArray(json.files) ? json.files : []
  const ids = list.map((x) => String(x.id || '')).filter((x) => /^\d+$/.test(x))
  return [...new Set(ids)]
}

function loadCookie(args) {
  if (args.cookie && args.cookie.trim()) return args.cookie.trim()
  if (args.cookieFile) {
    if (!fs.existsSync(args.cookieFile)) {
      throw new Error(`Cookie file not found: ${args.cookieFile}`)
    }
    return String(fs.readFileSync(args.cookieFile, 'utf8')).split(/\r?\n/)[0].trim()
  }
  return ''
}

function looksLikeZip(buffer) {
  if (!buffer || buffer.length < 4) return false
  return (
    (buffer[0] === 0x50 && buffer[1] === 0x4B && buffer[2] === 0x03 && buffer[3] === 0x04) ||
    (buffer[0] === 0x50 && buffer[1] === 0x4B && buffer[2] === 0x05 && buffer[3] === 0x06) ||
    (buffer[0] === 0x50 && buffer[1] === 0x4B && buffer[2] === 0x07 && buffer[3] === 0x08)
  )
}

function fetchBinary(url, cookie) {
  const args = ['-k', '-L', '-sS', url]
  if (cookie) {
    args.push('-H', `Cookie: ${cookie}`)
  }
  return execFileSync('curl.exe', args, { maxBuffer: 40 * 1024 * 1024 })
}

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }
  const cookie = loadCookie(args)
  if (!cookie) {
    throw new Error('Missing cookie. Use --cookie or --cookie-file (or env TK_COOKIE).')
  }

  const ids = loadIdsFromIndex(args.index)
  const from = args.start - 1
  const to = args.limit > 0 ? from + args.limit : ids.length
  const selected = ids.slice(from, to)
  if (!selected.length) {
    throw new Error('No ids selected.')
  }

  ensureDir(args.outDir)
  ensureDir(args.failDir)

  let ok = 0
  let fail = 0
  let skip = 0

  for (let i = 0; i < selected.length; i += 1) {
    const id = selected[i]
    const outZip = path.join(args.outDir, `${id}.zip`)
    const failHtml = path.join(args.failDir, `${id}.html`)
    if (!args.force && fs.existsSync(outZip) && fs.statSync(outZip).size > 0) {
      skip += 1
      continue
    }
    const url = `https://tk.hustoj.com/pay.php?fmt=zipdata&pid=${id}`
    try {
      const data = fetchBinary(url, cookie)
      if (looksLikeZip(data)) {
        fs.writeFileSync(outZip, data)
        ok += 1
      } else {
        fs.writeFileSync(failHtml, data)
        fail += 1
      }
    } catch (_) {
      fail += 1
    }
    if ((i + 1) % 20 === 0 || i + 1 === selected.length) {
      console.log(`progress ${i + 1}/${selected.length} ok=${ok} fail=${fail} skip=${skip}`)
    }
    if (args.delay > 0) {
      await sleep(args.delay)
    }
  }

  console.log(`done. total=${selected.length}, ok=${ok}, fail=${fail}, skip=${skip}`)
  console.log(`zip dir: ${args.outDir}`)
  console.log(`failed html dir: ${args.failDir}`)
}

main().catch((error) => {
  console.error(error.message || error)
  process.exit(1)
})

