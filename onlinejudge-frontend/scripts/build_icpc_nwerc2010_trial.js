/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const SOURCE_HTML = path.resolve(__dirname, 'data', 'kattis_nwerc2010_source.html')
const OUTPUT_JSON = path.resolve(__dirname, 'data', 'icpc_nwerc2010_trial_7_bilingual.json')
const OUTPUT_REPORT = path.resolve(__dirname, 'data', 'icpc_nwerc2010_trial_7_report.json')
const PAGE_CACHE_DIR = path.resolve(__dirname, 'data')
const TESTDATA_DIR = 'D:\\BaiduNetdiskDownload\\icpc\\nwerc2010\\testdata\\testdata'

const LETTER_BY_SLUG = {
  fairdivision: 'A',
  freegoodies: 'B',
  highscore: 'C',
  hilldriving: 'D',
  risk: 'F',
  sellingland: 'G',
  stockprices: 'H'
}

const CN_META = {
  fairdivision: {
    title: '公平分摊',
    desc: 'n 个人合买礼物，总价为 p。第 i 个人最多可支付 a_i（单位分）。在不超过各自上限的前提下，按“尽量公平”原则分摊费用：先最小化相对平均值 p/n 的最大偏差，再依次比较次大偏差；若仍有多解，上限更高者优先多付，再按输入顺序靠前者优先多付。'
  },
  freegoodies: {
    title: '免费礼物分配',
    desc: 'Petra 和 Jan 轮流挑选礼物，每件礼物对两人的价值不同。已知先手与每件礼物在两人视角下的价值，双方按各自策略选取，直到礼物选完。请输出两人最终获得的总价值（按各自估值计算）。'
  },
  highscore: {
    title: '最高分名字输入',
    desc: '你要用摇杆输入目标名字。初始字符串全为 A，光标在首位；上下可修改字符，左右可移动光标且支持环绕。求输入目标字符串所需的最少摇杆操作次数。'
  },
  hilldriving: {
    title: '丘陵驾驶',
    desc: '给定一条回家路线，每段有长度与坡度。车辆油耗模型由参数给定，并受最高车速与油量限制。可在不同路段选择速度，目标是最短总耗时；若油量不足以到达，输出 IMPOSSIBLE。'
  },
  risk: {
    title: 'Risk 兵力调度',
    desc: '在图上进行一回合兵力重分配：你的军队只能在己方控制且相邻连通的区域间移动，且每个区域至少保留 1 名士兵。请最大化回合结束后“边境区域（与敌方相邻）中最弱区域的兵力值”。'
  },
  sellingland: {
    title: '卖地',
    desc: '给定由 . 和 # 组成的网格，. 表示可售地块。你只能出售轴对齐矩形地块，收益与周长相关。需要在最优方案下统计各周长矩形出现次数，并按指定格式输出。'
  },
  stockprices: {
    title: '股票撮合',
    desc: '维护买单与卖单簿：当最高买价 >= 最低卖价时持续撮合成交。每条订单处理后，输出当前最低卖价（ask）、最高买价（bid）与最近成交价；不存在时输出 -。'
  }
}

function parseArgs(argv) {
  const args = {
    output: OUTPUT_JSON,
    report: OUTPUT_REPORT,
    sourceHtml: SOURCE_HTML,
    testdataDir: TESTDATA_DIR,
    refresh: false
  }
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i]
    if (arg === '--output') args.output = path.resolve(String(argv[++i] || args.output))
    else if (arg === '--report') args.report = path.resolve(String(argv[++i] || args.report))
    else if (arg === '--source') args.sourceHtml = path.resolve(String(argv[++i] || args.sourceHtml))
    else if (arg === '--testdata-dir') args.testdataDir = path.resolve(String(argv[++i] || args.testdataDir))
    else if (arg === '--refresh') args.refresh = true
    else if (arg === '--help' || arg === '-h') args.help = true
  }
  return args
}

function printHelp() {
  console.log('Usage:')
  console.log('  node scripts/build_icpc_nwerc2010_trial.js [options]')
  console.log('')
  console.log('Options:')
  console.log(`  --source <path>         Source page html, default: ${SOURCE_HTML}`)
  console.log(`  --testdata-dir <path>   Official testdata dir, default: ${TESTDATA_DIR}`)
  console.log(`  --output <path>         Output JSON, default: ${OUTPUT_JSON}`)
  console.log(`  --report <path>         Output report, default: ${OUTPUT_REPORT}`)
  console.log('  --refresh               Force refetch Kattis problem pages')
}

function ensureDirForFile(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
}

function decodeEntities(text) {
  let s = String(text || '')
  const map = {
    '&nbsp;': ' ',
    '&lt;': '<',
    '&gt;': '>',
    '&amp;': '&',
    '&quot;': '"',
    '&#39;': "'"
  }
  Object.keys(map).forEach((k) => {
    s = s.split(k).join(map[k])
  })
  s = s.replace(/&#(\d+);/g, (_, n) => {
    const code = Number(n)
    return Number.isFinite(code) ? String.fromCodePoint(code) : ''
  })
  return s
}

function htmlToText(html) {
  let s = String(html || '')
  s = s.replace(/<\s*br\s*\/?>/gi, '\n')
  s = s.replace(/<\/\s*p\s*>/gi, '\n\n')
  s = s.replace(/<\/\s*li\s*>/gi, '\n')
  s = s.replace(/<\/\s*tr\s*>/gi, '\n')
  s = s.replace(/<\/\s*(h1|h2|h3)\s*>/gi, '\n\n')
  s = s.replace(/<pre[^>]*>/gi, '\n')
  s = s.replace(/<\/pre>/gi, '\n')
  s = s.replace(/<[^>]+>/g, '')
  s = decodeEntities(s)
  s = s.replace(/\r/g, '')
  s = s.replace(/[ \t]+\n/g, '\n')
  s = s.replace(/\n{3,}/g, '\n\n')
  return s.trim()
}

function between(haystack, left, right) {
  const i = haystack.indexOf(left)
  if (i < 0) return ''
  const start = i + left.length
  const j = haystack.indexOf(right, start)
  if (j < 0) return haystack.slice(start)
  return haystack.slice(start, j)
}

function normalizeWS(s) {
  return String(s || '').replace(/\r/g, '').trim()
}

function fetchProblemPage(slug, refresh = false) {
  const cacheFile = path.resolve(PAGE_CACHE_DIR, `kattis_${slug}.html`)
  if (!refresh && fs.existsSync(cacheFile) && fs.statSync(cacheFile).size > 0) {
    return fs.readFileSync(cacheFile, 'utf8')
  }
  const url = `https://open.kattis.com/problems/${slug}`
  const html = execFileSync('curl.exe', ['-L', '-s', url], {
    encoding: 'utf8',
    maxBuffer: 10 * 1024 * 1024
  })
  fs.writeFileSync(cacheFile, html, 'utf8')
  return html
}

function parseSourceProblems(sourceHtml) {
  const html = fs.readFileSync(sourceHtml, 'utf8')
  const rows = [...html.matchAll(/<a href="\/problems\/([a-z0-9]+)"\s*>\s*([^<]+)\s*<\/a>[\s\S]*?difficulty_[a-z]+">([0-9.]+)<\/span>(Easy|Medium|Hard)/g)]
  const uniq = new Map()
  for (const m of rows) {
    const slug = m[1]
    if (!LETTER_BY_SLUG[slug]) continue
    if (!uniq.has(slug)) {
      uniq.set(slug, {
        slug,
        title: m[2].trim(),
        score: Number(m[3]),
        level: m[4]
      })
    }
  }
  return [...uniq.values()]
}

function parseProblemFields(problemHtml) {
  const title = normalizeWS(htmlToText((problemHtml.match(/<h1 class="book-page-heading">([\s\S]*?)<\/h1>/i) || [])[1]))
  const descHtml = between(problemHtml, '<div class="problembody">', '<h2>Input</h2>')
  const inputHtml = between(problemHtml, '<h2>Input</h2>', '<h2>Output</h2>')
  const outputHtml = between(problemHtml, '<h2>Output</h2>', '<table class="sample"')
  const sampleMatch = problemHtml.match(/<th>Sample Input[^<]*<\/th>[\s\S]*?<th>Sample Output[^<]*<\/th>[\s\S]*?<td>\s*<pre>\s*([\s\S]*?)<\/pre>[\s\S]*?<td>\s*<pre>\s*([\s\S]*?)<\/pre>/i)

  const cpu = normalizeWS(((problemHtml.match(/CPU Time limit<\/span><span[^>]*>([^<]+)<\/span>/i) || [])[1]) || '')
  const mem = normalizeWS(((problemHtml.match(/Memory limit<\/span><span[^>]*>([^<]+)<\/span>/i) || [])[1]) || '')

  let timeLimit = 2000
  let memoryLimit = 262144
  const cpuNum = Number((cpu.match(/([0-9]+(?:\.[0-9]+)?)/) || [])[1])
  if (Number.isFinite(cpuNum) && cpuNum > 0) {
    timeLimit = Math.max(1000, Math.round(cpuNum * 1000))
  }
  const memNum = Number((mem.match(/([0-9]+(?:\.[0-9]+)?)/) || [])[1])
  if (Number.isFinite(memNum) && memNum > 0) {
    memoryLimit = Math.round(memNum * 1024)
  }

  return {
    title,
    descriptionEn: normalizeWS(htmlToText(descHtml)),
    inputEn: normalizeWS(htmlToText(inputHtml)),
    outputEn: normalizeWS(htmlToText(outputHtml)),
    sampleInput: normalizeWS(htmlToText(sampleMatch ? sampleMatch[1] : '')),
    sampleOutput: normalizeWS(htmlToText(sampleMatch ? sampleMatch[2] : '')),
    timeLimit,
    memoryLimit
  }
}

function mapDifficulty(level, score) {
  if (level === 'Easy' || score <= 3) return 'EASY'
  if (level === 'Medium' || score <= 6) return 'MEDIUM'
  return 'HARD'
}

function readOfficialTestCase(letter, testdataDir) {
  const inFile = path.join(testdataDir, `${letter}.in`)
  const outFile = path.join(testdataDir, `${letter}.out`)
  if (!fs.existsSync(inFile) || !fs.existsSync(outFile)) {
    return null
  }
  return {
    input: normalizeWS(fs.readFileSync(inFile, 'utf8')),
    output: normalizeWS(fs.readFileSync(outFile, 'utf8'))
  }
}

function buildProblem(problemInfo, fields, officialCase) {
  const slug = problemInfo.slug
  const letter = LETTER_BY_SLUG[slug]
  const cn = CN_META[slug] || { title: problemInfo.title, desc: '请参考英文原题。' }
  const problemTitle = `${cn.title}（${problemInfo.title}）`

  const description = [
    '【中文题意】',
    cn.desc,
    '',
    '【English Statement】',
    fields.descriptionEn
  ].join('\n')

  const inputFormat = [
    '【中文说明】',
    '输入格式请结合英文原题理解；本题保留英文官方输入定义。',
    '',
    '【Input (EN)】',
    fields.inputEn
  ].join('\n')

  const outputFormat = [
    '【中文说明】',
    '输出格式请结合英文原题理解；本题保留英文官方输出定义。',
    '',
    '【Output (EN)】',
    fields.outputEn
  ].join('\n')

  const testCases = []
  if (fields.sampleInput && fields.sampleOutput) {
    testCases.push({ input: fields.sampleInput, output: fields.sampleOutput })
  }
  if (officialCase && officialCase.input && officialCase.output) {
    testCases.push(officialCase)
  }

  return {
    title: problemTitle,
    difficulty: mapDifficulty(problemInfo.level, problemInfo.score),
    status: 1,
    description,
    inputFormat,
    outputFormat,
    sampleInput: fields.sampleInput || (officialCase ? officialCase.input : ''),
    sampleOutput: fields.sampleOutput || (officialCase ? officialCase.output : ''),
    timeLimit: fields.timeLimit,
    memoryLimit: fields.memoryLimit,
    testCases,
    source: `ICPC NWERC 2010 / Kattis / Problem ${letter}`,
    externalId: `nwerc2010-${letter}-${slug}`,
    slug
  }
}

function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    printHelp()
    return
  }
  if (!fs.existsSync(args.sourceHtml)) {
    throw new Error(`Source html not found: ${args.sourceHtml}`)
  }
  if (!fs.existsSync(args.testdataDir)) {
    throw new Error(`Testdata dir not found: ${args.testdataDir}`)
  }

  const problemsMeta = parseSourceProblems(args.sourceHtml)
  if (!problemsMeta.length) {
    throw new Error('No problems parsed from source page.')
  }

  const result = []
  const report = {
    sourceHtml: args.sourceHtml,
    testdataDir: args.testdataDir,
    totalParsed: problemsMeta.length,
    built: 0,
    failed: []
  }

  for (const p of problemsMeta) {
    try {
      const html = fetchProblemPage(p.slug, args.refresh)
      const fields = parseProblemFields(html)
      const letter = LETTER_BY_SLUG[p.slug]
      const officialCase = readOfficialTestCase(letter, args.testdataDir)
      const item = buildProblem(p, fields, officialCase)
      if (!item.testCases.length) {
        throw new Error('no test cases available')
      }
      result.push(item)
      report.built += 1
    } catch (e) {
      report.failed.push({ slug: p.slug, message: e.message || String(e) })
    }
  }

  ensureDirForFile(args.output)
  ensureDirForFile(args.report)
  fs.writeFileSync(args.output, JSON.stringify(result, null, 2), 'utf8')
  fs.writeFileSync(args.report, JSON.stringify(report, null, 2), 'utf8')
  console.log(`built=${report.built}, failed=${report.failed.length}`)
  console.log(`output: ${args.output}`)
  console.log(`report: ${args.report}`)
}

try {
  main()
} catch (error) {
  console.error(error.message || error)
  process.exit(1)
}

