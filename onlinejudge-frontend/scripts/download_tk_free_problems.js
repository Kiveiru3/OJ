/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const DEFAULT_OUT_DIR = 'D:\\BaiduNetdiskDownload\\tiku\\TK题库'
const DEFAULT_META = 'D:\\BaiduNetdiskDownload\\tiku\\tk_free_index.json'

function parseArgs(argv) {
  const args = {
    outDir: DEFAULT_OUT_DIR,
    metaFile: DEFAULT_META,
    limit: 300,
    maxPages: 20,
    force: false
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--out-dir') args.outDir = path.resolve(String(argv[++i] || args.outDir))
    else if (arg === '--meta') args.metaFile = path.resolve(String(argv[++i] || args.metaFile))
    else if (arg === '--limit') args.limit = Math.max(1, Number(argv[++i] || args.limit))
    else if (arg === '--max-pages') args.maxPages = Math.max(1, Number(argv[++i] || args.maxPages))
    else if (arg === '--force') args.force = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/download_tk_free_problems.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --out-dir <path>    Download directory, default: ${DEFAULT_OUT_DIR}`)
  console.log(`  --meta <path>       Metadata json path, default: ${DEFAULT_META}`)
  console.log('  --limit <n>         Max problem pages to download, default: 300')
  console.log('  --max-pages <n>     Max problemset pages to scan, default: 20')
  console.log('  --force             Redownload existing html files')
}

function fetchUrl(url) {
  return execFileSync('curl.exe', ['-k', '-L', '-s', url], {
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024
  })
}

function extractProblemIds(html) {
  const ids = [...String(html).matchAll(/problem\.php\?id=(\d+)/g)].map((m) => m[1])
  return [...new Set(ids)]
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true })
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }

  ensureDir(args.outDir)
  ensureDir(path.dirname(args.metaFile))

  const foundIds = []
  const idSeen = new Set()
  const pageStats = []

  for (let page = 1; page <= args.maxPages && foundIds.length < args.limit; page += 1) {
    const listUrl = `https://tk.hustoj.com/problemset.php?search=free&page=${page}`
    const html = fetchUrl(listUrl)
    const pageIds = extractProblemIds(html)
    let newCount = 0
    pageIds.forEach((id) => {
      if (!idSeen.has(id) && foundIds.length < args.limit) {
        idSeen.add(id)
        foundIds.push(id)
        newCount += 1
      }
    })
    pageStats.push({
      page,
      totalIds: pageIds.length,
      newIds: newCount
    })
    console.log(`scan page=${page}, ids=${pageIds.length}, new=${newCount}, total=${foundIds.length}`)
    if (pageIds.length === 0) break
  }

  let downloaded = 0
  let skipped = 0
  const files = []
  for (const id of foundIds) {
    const outFile = path.join(args.outDir, `${id}.html`)
    const relFile = path.relative(path.dirname(args.metaFile), outFile)
    if (!args.force && fs.existsSync(outFile) && fs.statSync(outFile).size > 0) {
      skipped += 1
      files.push({ id, file: relFile, status: 'skipped' })
      continue
    }
    const problemUrl = `https://tk.hustoj.com/problem.php?id=${id}`
    const html = fetchUrl(problemUrl)
    fs.writeFileSync(outFile, html, 'utf8')
    downloaded += 1
    files.push({ id, file: relFile, status: 'downloaded' })
    if ((downloaded + skipped) % 25 === 0) {
      console.log(`progress ${downloaded + skipped}/${foundIds.length}`)
    }
  }

  const meta = {
    generatedAt: new Date().toISOString(),
    source: 'https://tk.hustoj.com/problemset.php?search=free',
    args: {
      outDir: args.outDir,
      metaFile: args.metaFile,
      limit: args.limit,
      maxPages: args.maxPages,
      force: args.force
    },
    pageStats,
    totalSelected: foundIds.length,
    downloaded,
    skipped,
    files
  }

  fs.writeFileSync(args.metaFile, JSON.stringify(meta, null, 2), 'utf8')
  console.log(`done. selected=${foundIds.length}, downloaded=${downloaded}, skipped=${skipped}`)
  console.log(`meta: ${args.metaFile}`)
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}

