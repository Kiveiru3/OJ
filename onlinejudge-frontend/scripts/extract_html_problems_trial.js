/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')

const DEFAULT_SOURCE_DIR = 'D:\\BaiduNetdiskDownload\\0972f-main\\OJ棰樺簱'
const DEFAULT_OUTPUT = path.resolve(__dirname, 'data', 'html_problems_trial_200.json')
const DEFAULT_REPORT = path.resolve(__dirname, 'data', 'html_problems_trial_report.json')

function parseArgs(argv) {
  const args = {
    sourceDir: DEFAULT_SOURCE_DIR,
    output: DEFAULT_OUTPUT,
    report: DEFAULT_REPORT,
    limit: 200,
    maxScan: 3000
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--source-dir') args.sourceDir = path.resolve(String(argv[++i] || args.sourceDir))
    else if (arg === '--output') args.output = path.resolve(String(argv[++i] || args.output))
    else if (arg === '--report') args.report = path.resolve(String(argv[++i] || args.report))
    else if (arg === '--limit') args.limit = Math.max(1, Number(argv[++i] || args.limit))
    else if (arg === '--max-scan') args.maxScan = Math.max(1, Number(argv[++i] || args.maxScan))
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/extract_html_problems_trial.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --source-dir <path>   Source root, default: ${DEFAULT_SOURCE_DIR}`)
  console.log(`  --output <path>       Output JSON, default: ${DEFAULT_OUTPUT}`)
  console.log(`  --report <path>       Parse report JSON, default: ${DEFAULT_REPORT}`)
  console.log('  --limit <n>           Number of extracted problems, default: 200')
  console.log('  --max-scan <n>        Max scanned html files, default: 3000')
}

function normalizeEncoding(enc) {
  const e = String(enc || '').trim().toLowerCase()
  if (!e) return 'utf-8'
  if (e === 'utf8' || e === 'utf-8') return 'utf-8'
  if (e === 'gbk' || e === 'gb2312' || e === 'gb18030') return 'gbk'
  return 'utf-8'
}

function detectEncoding(buffer) {
  const header = buffer.slice(0, 4096).toString('latin1')
  const m = header.match(/charset\s*=\s*["']?\s*([a-zA-Z0-9\-_]+)/i)
  if (m && m[1]) {
    return normalizeEncoding(m[1])
  }
  return 'utf-8'
}

function mojibakeScore(text) {
  const s = String(text || '')
  if (!s) return 999999
  const badPattern = /(閿焲鏉坾閸檤閻▅鐠噟閺峾閸弢閹祙閸抾缂亅锟斤拷)/g
  const badCount = (s.match(badPattern) || []).length
  return badCount / Math.max(1, s.length)
}

function readHtmlFile(filePath) {
  const buffer = fs.readFileSync(filePath)
  const declaredEncoding = detectEncoding(buffer)
  const candidates = declaredEncoding === 'gbk' ? ['gbk', 'utf-8'] : ['utf-8', 'gbk']
  let best = { html: '', encoding: declaredEncoding, score: 999999 }
  for (const enc of candidates) {
    try {
      const html = new TextDecoder(enc).decode(buffer)
      const score = mojibakeScore(html)
      if (score < best.score) {
        best = { html, encoding: enc, score }
      }
    } catch (_) {
      // ignore and try next encoding
    }
  }
  try {
    return { html: best.html, encoding: best.encoding }
  } catch (_) {
    return { html: buffer.toString('utf8'), encoding: 'utf-8-fallback' }
  }
}

function decodeEntities(text) {
  if (!text) return ''
  let s = String(text)
  const map = {
    '&nbsp;': ' ',
    '&lt;': '<',
    '&gt;': '>',
    '&amp;': '&',
    '&quot;': '"',
    '&#39;': "'",
    '&apos;': "'"
  }
  Object.keys(map).forEach((k) => {
    s = s.split(k).join(map[k])
  })
  s = s.replace(/&#(\d+);/g, (_, n) => {
    const code = Number(n)
    return Number.isFinite(code) ? String.fromCodePoint(code) : ''
  })
  s = s.replace(/&#x([0-9a-fA-F]+);/g, (_, n) => {
    const code = Number.parseInt(n, 16)
    return Number.isFinite(code) ? String.fromCodePoint(code) : ''
  })
  return s
}

function htmlToText(html) {
  if (!html) return ''
  let s = String(html)
  s = s.replace(/<\s*br\s*\/?>/gi, '\n')
  s = s.replace(/<\/\s*p\s*>/gi, '\n')
  s = s.replace(/<\/\s*div\s*>/gi, '\n')
  s = s.replace(/<\/\s*h[1-6]\s*>/gi, '\n')
  s = s.replace(/<[^>]+>/g, '')
  s = decodeEntities(s)
  s = s.replace(/\r/g, '')
  s = s.replace(/[ \t]+\n/g, '\n')
  s = s.replace(/\n{3,}/g, '\n\n')
  return s.trim()
}

function cleanSingleLine(text) {
  return htmlToText(text).replace(/\s+/g, ' ').trim()
}

function mapDifficultyFromTitle(title, timeLimit) {
  const t = String(title || '')
  if (/绠€鍗晐鍏ラ棬|A\+B|缁冧範I|鐑韩/i.test(t)) return 'EASY'
  if (/鍥伴毦|楂樼骇|hard/i.test(t)) return 'HARD'
  if (Number(timeLimit || 0) <= 1000) return 'MEDIUM'
  return 'MEDIUM'
}

function extractFirstNumber(text, fallback) {
  const m = String(text || '').match(/(\d+)/)
  return m ? Number(m[1]) : fallback
}

function parseTimeLimitFromMeta(meta, fallback) {
  const m = String(meta || '').match(/Time Limit:\s*([0-9]+(?:\.[0-9]+)?)(?:\s*\/\s*[0-9]+(?:\.[0-9]+)?)?\s*(MS|MSEC|SEC|S)?/i)
  if (!m) return fallback
  const value = Number(m[1])
  const unit = String(m[2] || '').toUpperCase()
  if (!Number.isFinite(value) || value <= 0) return fallback
  if (unit === 'SEC' || unit === 'S') return Math.round(value * 1000)
  return Math.round(value)
}

function parseMemoryLimitFromMeta(meta, fallback) {
  const m = String(meta || '').match(/Memory Limit:\s*([0-9]+(?:\.[0-9]+)?)(?:\s*\/\s*[0-9]+(?:\.[0-9]+)?)?\s*(KB|K|MB|M)?/i)
  if (!m) return fallback
  const value = Number(m[1])
  const unit = String(m[2] || '').toUpperCase()
  if (!Number.isFinite(value) || value <= 0) return fallback
  if (unit === 'MB' || unit === 'M') return Math.round(value * 1024)
  return Math.round(value)
}

function buildProblemItem(raw) {
  const title = cleanSingleLine(raw.title || '')
  const description = htmlToText(raw.description || '')
  const inputFormat = htmlToText(raw.inputFormat || '')
  const outputFormat = htmlToText(raw.outputFormat || '')
  const sampleInput = htmlToText(raw.sampleInput || '')
  const sampleOutput = htmlToText(raw.sampleOutput || '')
  const rawCases = Array.isArray(raw.testCases) ? raw.testCases : []
  const testCases = rawCases
    .map((x) => ({
      input: htmlToText(x && x.input ? x.input : ''),
      output: htmlToText(x && x.output ? x.output : '')
    }))
    .filter((x) => x.input && x.output)

  if (!title || !description) {
    return null
  }
  if ((!sampleInput || !sampleOutput) && !testCases.length) {
    return null
  }

  const item = {
    title,
    difficulty: raw.difficulty || mapDifficultyFromTitle(title, raw.timeLimit),
    status: 1,
    description,
    inputFormat: inputFormat || 'See problem statement.',
    outputFormat: outputFormat || 'See problem statement.',
    sampleInput: sampleInput || (testCases[0] ? testCases[0].input : ''),
    sampleOutput: sampleOutput || (testCases[0] ? testCases[0].output : ''),
    timeLimit: Number(raw.timeLimit || 2000),
    memoryLimit: Number(raw.memoryLimit || 256000),
    testCases: testCases.length ? testCases : [{ input: sampleInput, output: sampleOutput }],
    source: raw.source || 'HTML import'
  }
  if (raw.externalId) {
    item.externalId = String(raw.externalId)
  }
  return item
}
function parseLuogu(html, filePath) {
  const m = html.match(/window\._feInjection\s*=\s*JSON\.parse\(decodeURIComponent\("([\s\S]*?)"\)\)/)
  if (!m) return null

  let payload = null
  try {
    const decoded = decodeURIComponent(m[1])
    payload = JSON.parse(decoded)
  } catch (_) {
    return null
  }

  const p = payload && payload.currentData && payload.currentData.problem
  if (!p) return null

  let pairs = (Array.isArray(p.samples) ? p.samples : [])
    .map((s) => {
      if (Array.isArray(s)) {
        return {
          input: s[0] || '',
          output: s[1] || ''
        }
      }
      return {
        input: s && s.input ? s.input : '',
        output: s && s.output ? s.output : ''
      }
    })
    .filter((s) => String(s.input || '').trim() && String(s.output || '').trim())

  // Fallback: some offline pages do not keep structured samples in JSON.
  if (!pairs.length) {
    const sampleRe = /<h3>[^<]*(?:输入样例|Sample Input)[^<]*<\/h3>\s*<pre><code>([\s\S]*?)<\/code><\/pre>\s*<h3>[^<]*(?:输出样例|Sample Output)[^<]*<\/h3>\s*<pre><code>([\s\S]*?)<\/code><\/pre>/gi
    let mm = sampleRe.exec(html)
    while (mm) {
      const input = htmlToText(mm[1] || '')
      const output = htmlToText(mm[2] || '')
      if (input && output) {
        pairs.push({ input, output })
      }
      mm = sampleRe.exec(html)
    }
  }

  const parts = []
  if (p.background) parts.push(`【背景】\n${p.background}`)
  if (p.description) parts.push(`【题目描述】\n${p.description}`)
  if (p.hint) parts.push(`【提示】\n${p.hint}`)

  const first = pairs[0] || { input: '', output: '' }
  return buildProblemItem({
    title: p.title || p.pid || path.basename(filePath, '.html'),
    description: parts.join('\n\n'),
    inputFormat: p.inputFormat || '',
    outputFormat: p.outputFormat || '',
    sampleInput: first.input,
    sampleOutput: first.output,
    testCases: pairs,
    timeLimit: Number(p.limits && Array.isArray(p.limits.time) ? p.limits.time[0] : 1000),
    memoryLimit: Number(p.limits && Array.isArray(p.limits.memory) ? p.limits.memory[0] : 128000),
    externalId: p.pid || path.basename(filePath, '.html'),
    difficulty: (() => {
      const d = Number(p.difficulty || 0)
      if (d <= 2) return 'EASY'
      if (d <= 5) return 'MEDIUM'
      return 'HARD'
    })(),
    source: '洛谷(离线HTML)'
  })
}
function parseHdu(html) {
  const titleMatch = html.match(/<h1[^>]*>([\s\S]*?)<\/h1>/i)
  const title = titleMatch ? titleMatch[1] : ''
  const sectionRe = /<div class=panel_title[^>]*>([\s\S]*?)<\/div>\s*<div class=panel_content>([\s\S]*?)<\/div>/gi
  const sections = {}
  let m = sectionRe.exec(html)
  while (m) {
    const key = cleanSingleLine(m[1]).toLowerCase()
    sections[key] = m[2]
    m = sectionRe.exec(html)
  }

  const meta = htmlToText((html.match(/Time Limit:[\s\S]*?Accepted Submission/i) || [''])[0])
  const timeLimit = parseTimeLimitFromMeta(meta, 2000)
  const memoryLimit = parseMemoryLimitFromMeta(meta, 65536)

  return buildProblemItem({
    title,
    description: sections['problem description'] || '',
    inputFormat: sections.input || '',
    outputFormat: sections.output || '',
    sampleInput: sections['sample input'] || '',
    sampleOutput: sections['sample output'] || '',
    timeLimit,
    memoryLimit,
    source: 'HDU(离线HTML)'
  })
}

function parseHustojLike(html, sourceName, filePath) {
  const titleMatch = html.match(/<center><h2>([\s\S]*?)<\/h2>/i) || html.match(/<title>Problem[^-]*--([\s\S]*?)<\/title>/i)
  const title = titleMatch ? titleMatch[1] : ''

  const sections = {}
  const sectionRe = /<h2>\s*([^<]+?)\s*<\/h2>\s*(?:<div class=content>([\s\S]*?)<\/div>|<pre class=content><span class=sampledata>([\s\S]*?)<\/span><\/pre>)/gi
  let m = sectionRe.exec(html)
  while (m) {
    const key = cleanSingleLine(m[1]).toLowerCase()
    sections[key] = m[2] || m[3] || ''
    m = sectionRe.exec(html)
  }

  const meta = htmlToText((html.match(/Time Limit:[\s\S]*?(?:Memory Limit:)[\s\S]*?(?:<br>|Submit:)/i) || [''])[0])
  const timeLimit = parseTimeLimitFromMeta(meta, 1000)
  const memoryLimit = parseMemoryLimitFromMeta(meta, 65536)

  const desc = sections.description || sections['problem description'] || sections['题目描述'] || ''
  const input = sections.input || sections['输入'] || ''
  const output = sections.output || sections['输出'] || ''
  const sampleInput = sections['sample input'] || sections['样例输入'] || ''
  const sampleOutput = sections['sample output'] || sections['样例输出'] || ''

  return buildProblemItem({
    title,
    description: desc,
    inputFormat: input,
    outputFormat: output,
    sampleInput,
    sampleOutput,
    timeLimit,
    memoryLimit,
    externalId: (() => {
      const base = path.basename(filePath, '.html')
      return /^\d+$/.test(base) ? base : ''
    })(),
    source: sourceName + '(离线HTML)'
  })
}

function detectPlatform(filePath, root) {
  const rel = path.relative(root, filePath)
  const parts = rel.split(path.sep).filter(Boolean)
  if (parts.length <= 1) {
    return path.basename(root)
  }
  return parts[0]
}

function parseByPlatform(html, filePath, platform) {
  const normalized = String(platform || '').toLowerCase()

  // Prefer content-based detection so passing a single platform folder still works.
  if (/window\._feInjection\s*=\s*JSON\.parse\(decodeURIComponent\(/.test(html)) {
    const luogu = parseLuogu(html, filePath)
    if (luogu) return luogu
  }

  if (/panel_title[^>]*>\s*(Problem Description|Input|Output)/i.test(html)) {
    const hdu = parseHdu(html)
    if (hdu) return hdu
  }

  if (/<h2>\s*(Description|Input|Output|Sample Input|Sample Output|题目描述|输入|输出|样例输入|样例输出)\s*<\/h2>/i.test(html)) {
    const hustojLike = parseHustojLike(html, platform || 'HUSTOJ', filePath)
    if (hustojLike) return hustojLike
  }

  if (normalized.includes('luogu')) return parseLuogu(html, filePath)
  if (normalized.includes('hdu')) return parseHdu(html)
  if (normalized.includes('dnuioj') || normalized.includes('tk') || normalized.includes('hustoj')) {
    return parseHustojLike(html, platform || 'HUSTOJ', filePath)
  }

  return null
}
function walkHtmlFiles(dirPath, onFile) {
  const stack = [dirPath]
  while (stack.length > 0) {
    const current = stack.pop()
    let entries = []
    try {
      entries = fs.readdirSync(current, { withFileTypes: true })
    } catch (_) {
      continue
    }
    entries.sort((a, b) => a.name.localeCompare(b.name, 'zh-Hans-CN'))
    for (const entry of entries) {
      const fullPath = path.join(current, entry.name)
      if (entry.isDirectory()) {
        stack.push(fullPath)
        continue
      }
      if (!entry.isFile()) continue
      if (!entry.name.toLowerCase().endsWith('.html')) continue
      const shouldStop = onFile(fullPath)
      if (shouldStop) return
    }
  }
}

function listPlatformDirs(rootDir) {
  const priorities = ['杭州电子科技大学', '洛谷', 'DnuiOJ', 'TK题库', '聚石塔OJ']
  const entries = fs.readdirSync(rootDir, { withFileTypes: true })
  const dirs = entries.filter((x) => x.isDirectory()).map((x) => x.name)

  dirs.sort((a, b) => {
    const pa = priorities.indexOf(a)
    const pb = priorities.indexOf(b)
    if (pa === -1 && pb === -1) return a.localeCompare(b, 'zh-Hans-CN')
    if (pa === -1) return 1
    if (pb === -1) return -1
    return pa - pb
  })

  const result = dirs.map((name) => path.join(rootDir, name))
  const hasHtmlInRoot = entries.some((x) => x.isFile() && x.name.toLowerCase().endsWith('.html'))
  if (hasHtmlInRoot) {
    result.unshift(rootDir)
  }
  return result
}
function ensureDir(filePath) {
  const dir = path.dirname(filePath)
  fs.mkdirSync(dir, { recursive: true })
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }
  if (!fs.existsSync(args.sourceDir)) {
    throw new Error(`Source dir not found: ${args.sourceDir}`)
  }

  const problems = []
  const seenTitles = new Set()
  const report = {
    sourceDir: args.sourceDir,
    limit: args.limit,
    maxScan: args.maxScan,
    scanned: 0,
    accepted: 0,
    duplicate: 0,
    parseFailed: 0,
    invalid: 0,
    byPlatform: {},
    samples: []
  }

  const platformDirs = listPlatformDirs(args.sourceDir)
  for (const dirPath of platformDirs) {
    if (problems.length >= args.limit || report.scanned >= args.maxScan) break
    walkHtmlFiles(dirPath, (filePath) => {
      if (problems.length >= args.limit) return true
      if (report.scanned >= args.maxScan) return true

      report.scanned += 1
      const platform = detectPlatform(filePath, args.sourceDir)
      if (!report.byPlatform[platform]) {
        report.byPlatform[platform] = { scanned: 0, accepted: 0, parseFailed: 0, invalid: 0, duplicate: 0 }
      }
      report.byPlatform[platform].scanned += 1

      const { html, encoding } = readHtmlFile(filePath)
      let item = null
      try {
        item = parseByPlatform(html, filePath, platform)
      } catch (_) {
        item = null
      }

      if (!item) {
        report.parseFailed += 1
        report.byPlatform[platform].parseFailed += 1
        return false
      }

      if (!item.title || !item.sampleInput || !item.sampleOutput || !Array.isArray(item.testCases) || !item.testCases.length) {
        report.invalid += 1
        report.byPlatform[platform].invalid += 1
        return false
      }

      const titleKey = item.title.trim().toLowerCase()
      if (seenTitles.has(titleKey)) {
        report.duplicate += 1
        report.byPlatform[platform].duplicate += 1
        return false
      }

      seenTitles.add(titleKey)
      problems.push(item)
      report.accepted += 1
      report.byPlatform[platform].accepted += 1
      if (report.samples.length < 8) {
        report.samples.push({
          title: item.title,
          platform,
          encoding,
          filePath
        })
      }
      return false
    })
  }

  ensureDir(args.output)
  ensureDir(args.report)
  fs.writeFileSync(args.output, JSON.stringify(problems, null, 2), 'utf8')
  fs.writeFileSync(args.report, JSON.stringify(report, null, 2), 'utf8')

  console.log(`Extract done. scanned=${report.scanned}, accepted=${report.accepted}, duplicate=${report.duplicate}, parseFailed=${report.parseFailed}, invalid=${report.invalid}`)
  console.log(`Output: ${args.output}`)
  console.log(`Report: ${args.report}`)
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}





