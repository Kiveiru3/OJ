/* eslint-disable no-console */
const fs = require('fs')
const path = require('path')

const OUTPUT_PATH = path.resolve(__dirname, 'data', 'classic_problems_200.json')
const TEMPLATE_COUNT = 20
const VARIANTS_PER_TEMPLATE = 10
const VARIANT_LABELS = [
  '校园数据版',
  '工程调试版',
  '竞赛热身版',
  '实验记录版',
  '上线检查版',
  '评测样例版',
  '课程作业版',
  '算法训练版',
  '强化挑战版',
  '综合实战版'
]

function mulberry32(seed) {
  let t = seed >>> 0
  return function rng() {
    t += 0x6d2b79f5
    let x = t
    x = Math.imul(x ^ (x >>> 15), x | 1)
    x ^= x + Math.imul(x ^ (x >>> 7), x | 61)
    return ((x ^ (x >>> 14)) >>> 0) / 4294967296
  }
}

function randInt(rng, min, max) {
  return Math.floor(rng() * (max - min + 1)) + min
}

function randArray(rng, n, min, max) {
  const arr = []
  for (let i = 0; i < n; i += 1) {
    arr.push(randInt(rng, min, max))
  }
  return arr
}

function gcd(a, b) {
  let x = Math.abs(a)
  let y = Math.abs(b)
  while (y !== 0) {
    const t = x % y
    x = y
    y = t
  }
  return x
}

function lcm(a, b) {
  if (a === 0 || b === 0) return 0
  return Math.abs((a / gcd(a, b)) * b)
}

function fastPowMod(a, b, mod) {
  let base = BigInt(a) % BigInt(mod)
  let exp = BigInt(b)
  const m = BigInt(mod)
  let ans = 1n
  while (exp > 0n) {
    if (exp & 1n) ans = (ans * base) % m
    base = (base * base) % m
    exp >>= 1n
  }
  return ans.toString()
}

function fib(n) {
  let a = 0n
  let b = 1n
  for (let i = 0; i < n; i += 1) {
    const t = a + b
    a = b
    b = t
  }
  return a
}

function isPrime(n) {
  if (n < 2) return false
  if (n === 2) return true
  if (n % 2 === 0) return false
  const limit = Math.floor(Math.sqrt(n))
  for (let i = 3; i <= limit; i += 2) {
    if (n % i === 0) return false
  }
  return true
}

function countPrimes(n) {
  if (n < 2) return 0
  const prime = new Array(n + 1).fill(true)
  prime[0] = false
  prime[1] = false
  for (let i = 2; i * i <= n; i += 1) {
    if (!prime[i]) continue
    for (let j = i * i; j <= n; j += i) {
      prime[j] = false
    }
  }
  let cnt = 0
  for (let i = 2; i <= n; i += 1) {
    if (prime[i]) cnt += 1
  }
  return cnt
}

function countBits(n) {
  let x = BigInt(n)
  let ans = 0
  while (x > 0n) {
    ans += Number(x & 1n)
    x >>= 1n
  }
  return ans
}

function lowerBound(arr, x) {
  let l = 0
  let r = arr.length
  while (l < r) {
    const mid = (l + r) >> 1
    if (arr[mid] >= x) r = mid
    else l = mid + 1
  }
  return l === arr.length ? -1 : l + 1
}

function maxSubarray(arr) {
  let best = arr[0]
  let cur = arr[0]
  for (let i = 1; i < arr.length; i += 1) {
    cur = Math.max(arr[i], cur + arr[i])
    best = Math.max(best, cur)
  }
  return best
}

function mergedIntervalCount(intervals) {
  if (!intervals.length) return 0
  const a = [...intervals].sort((x, y) => (x[0] - y[0]) || (x[1] - y[1]))
  let count = 0
  let l = a[0][0]
  let r = a[0][1]
  for (let i = 1; i < a.length; i += 1) {
    const [nl, nr] = a[i]
    if (nl <= r) {
      r = Math.max(r, nr)
    } else {
      count += 1
      l = nl
      r = nr
    }
  }
  if (l <= r) count += 1
  return count
}

function bfsShortestPath(grid) {
  const n = grid.length
  const m = grid[0].length
  if (grid[0][0] === 1 || grid[n - 1][m - 1] === 1) return -1
  const dist = Array.from({ length: n }, () => new Array(m).fill(-1))
  const q = [[0, 0]]
  dist[0][0] = 0
  let head = 0
  const dirs = [[1, 0], [-1, 0], [0, 1], [0, -1]]
  while (head < q.length) {
    const [x, y] = q[head]
    head += 1
    for (const [dx, dy] of dirs) {
      const nx = x + dx
      const ny = y + dy
      if (nx < 0 || nx >= n || ny < 0 || ny >= m) continue
      if (grid[nx][ny] === 1 || dist[nx][ny] !== -1) continue
      dist[nx][ny] = dist[x][y] + 1
      q.push([nx, ny])
    }
  }
  return dist[n - 1][m - 1]
}

function difficultyByVariant(base, variant) {
  if (base === 'EASY') {
    if (variant <= 6) return 'EASY'
    if (variant <= 9) return 'MEDIUM'
    return 'HARD'
  }
  if (base === 'MEDIUM') {
    if (variant <= 3) return 'EASY'
    if (variant <= 8) return 'MEDIUM'
    return 'HARD'
  }
  if (variant <= 2) return 'MEDIUM'
  return 'HARD'
}

function stringifyArray(arr) {
  return arr.join(' ')
}

function buildDescription(baseDescription, payload) {
  const rangeText = payload.dataRange || '请根据输入格式理解数据规模，注意边界条件与极端情况。'
  const hintText = payload.hint || '建议先写出清晰的思路，再根据数据规模选择合适的算法与数据结构。'

  return [
    '【题目背景】',
    `${baseDescription}`,
    '',
    '【任务描述】',
    '请你根据给定输入，计算并输出正确结果。程序需要稳定处理常规与边界数据，保证结果准确。',
    '',
    '【数据范围】',
    rangeText,
    '',
    '【解题提示】',
    hintText
  ].join('\n')
}

function createProblem(templateName, variant, baseDifficulty, payload) {
  const cases = payload.cases
  if (!cases || !cases.length) {
    throw new Error(`Template ${templateName} variant ${variant} has no cases`)
  }
  const variantLabel = VARIANT_LABELS[(variant - 1) % VARIANT_LABELS.length]
  return {
    title: `${payload.title}（${variantLabel}）`,
    difficulty: difficultyByVariant(baseDifficulty, variant),
    status: 1,
    description: buildDescription(payload.description, payload),
    inputFormat: payload.inputFormat,
    outputFormat: payload.outputFormat,
    sampleInput: cases[0].input,
    sampleOutput: cases[0].output,
    timeLimit: payload.timeLimit || 2000,
    memoryLimit: payload.memoryLimit || 256000,
    testCases: cases.slice(1),
    source: `经典算法题型 - ${templateName}`
  }
}

function buildFib(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 8 + variant, 25 + variant * 2)
    cases.push({ input: `${n}`, output: fib(n).toString() })
  }
  return createProblem('动态规划', variant, 'EASY', {
    title: '斐波那契数列第 n 项',
    description: '给定正整数 n，求斐波那契数列第 n 项。定义 F1=1，F2=1，Fn=F(n-1)+F(n-2)。',
    inputFormat: '输入一个整数 n（1 <= n <= 60）。',
    outputFormat: '输出 F_n 的值。',
    dataRange: '1 <= n <= 60，结果在 64 位整数范围内。',
    hint: '可使用迭代动态规划，时间复杂度 O(n)，空间复杂度 O(1)。',
    timeLimit: 1000,
    cases
  })
}

function buildClimbStairs(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 5 + variant, 30 + variant)
    cases.push({ input: `${n}`, output: fib(n + 1).toString() })
  }
  return createProblem('动态规划', variant, 'EASY', {
    title: '爬楼梯方案数',
    description: '每次可以爬 1 或 2 阶台阶，求到达第 n 阶台阶的不同方案数。',
    inputFormat: '输入一个整数 n（1 <= n <= 45）。',
    outputFormat: '输出方案总数。',
    dataRange: '1 <= n <= 45。',
    hint: '这是经典递推问题，可转化为斐波那契型动态规划。',
    timeLimit: 1000,
    cases
  })
}

function buildGcd(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const a = randInt(rng, 1, 50000 + variant * 8000)
    const b = randInt(rng, 1, 50000 + variant * 8000)
    cases.push({ input: `${a} ${b}`, output: `${gcd(a, b)}` })
  }
  return createProblem('数论', variant, 'EASY', {
    title: '最大公约数',
    description: '给定两个正整数 a、b，求它们的最大公约数（GCD）。',
    inputFormat: '输入一行，包含两个正整数 a 和 b。',
    outputFormat: '输出最大公约数。',
    dataRange: '1 <= a, b <= 10^9。',
    hint: '推荐使用欧几里得算法（辗转相除法）。',
    cases
  })
}

function buildLcm(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const a = randInt(rng, 1, 3000 + variant * 1500)
    const b = randInt(rng, 1, 3000 + variant * 1500)
    cases.push({ input: `${a} ${b}`, output: `${lcm(a, b)}` })
  }
  return createProblem('数论', variant, 'EASY', {
    title: '最小公倍数',
    description: '给定两个正整数 a、b，求它们的最小公倍数（LCM）。',
    inputFormat: '输入一行，包含两个正整数 a 和 b。',
    outputFormat: '输出最小公倍数。',
    dataRange: '1 <= a, b <= 10^9。',
    hint: '可利用关系 lcm(a,b)=a/gcd(a,b)*b，并注意溢出顺序。',
    cases
  })
}

function buildFastPow(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const a = randInt(rng, 2, 100000 + variant * 1000)
    const b = randInt(rng, 2, 100000 + variant * 1000)
    const m = randInt(rng, 10007, 1000000007)
    cases.push({ input: `${a} ${b} ${m}`, output: fastPowMod(a, b, m) })
  }
  return createProblem('数论', variant, 'MEDIUM', {
    title: '快速幂取模',
    description: '给定 a、b、m，求 a^b mod m。请使用快速幂思想。',
    inputFormat: '输入一行，三个整数 a b m。',
    outputFormat: '输出一个整数，表示 a^b mod m。',
    dataRange: '1 <= a, b <= 10^12，2 <= m <= 10^9+7。',
    hint: '使用二进制快速幂，将指数按二进制拆分。',
    timeLimit: 1500,
    cases
  })
}

function buildPrimeCheck(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 2, 100000 + variant * 100000)
    cases.push({ input: `${n}`, output: isPrime(n) ? 'YES' : 'NO' })
  }
  return createProblem('数论', variant, 'EASY', {
    title: '素数判定',
    description: '判断给定整数 n 是否为素数。是素数输出 YES，否则输出 NO。',
    inputFormat: '输入一个整数 n（2 <= n <= 10^9）。',
    outputFormat: '输出 YES 或 NO。',
    dataRange: '2 <= n <= 10^9。',
    hint: '试除到 sqrt(n) 即可，偶数可提前特判。',
    cases
  })
}

function buildPrimeCount(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 200 + variant * 30, 5000 + variant * 600)
    cases.push({ input: `${n}`, output: `${countPrimes(n)}` })
  }
  return createProblem('数论', variant, 'MEDIUM', {
    title: '统计不超过 n 的素数个数',
    description: '给定整数 n，求区间 [2, n] 内素数的数量。',
    inputFormat: '输入一个整数 n。',
    outputFormat: '输出素数个数。',
    dataRange: '2 <= n <= 10^6。',
    hint: '推荐使用埃氏筛法，时间复杂度约 O(n log log n)。',
    timeLimit: 1500,
    cases
  })
}

function buildBitCount(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 1, 1000000000 + variant * 100000000)
    cases.push({ input: `${n}`, output: `${countBits(n)}` })
  }
  return createProblem('位运算', variant, 'EASY', {
    title: '二进制中 1 的个数',
    description: '给定非负整数 n，输出其二进制表示中 1 的个数。',
    inputFormat: '输入一个整数 n。',
    outputFormat: '输出一个整数。',
    dataRange: '0 <= n <= 2^63-1。',
    hint: '可逐位统计，或使用 lowbit 思路优化。',
    cases
  })
}

function buildToBinary(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 1, 1000000 + variant * 500000)
    cases.push({ input: `${n}`, output: n.toString(2) })
  }
  return createProblem('位运算', variant, 'EASY', {
    title: '十进制转二进制',
    description: '将十进制正整数 n 转换为二进制字符串。',
    inputFormat: '输入一个整数 n。',
    outputFormat: '输出 n 的二进制表示（不含前导 0）。',
    dataRange: '1 <= n <= 10^9。',
    hint: '可使用除 2 取余，或直接利用语言内置转换。',
    cases
  })
}

function buildReverseNumber(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 10, 1000000 + variant * 100000)
    const reversed = String(n).split('').reverse().join('').replace(/^0+/, '')
    cases.push({ input: `${n}`, output: reversed || '0' })
  }
  return createProblem('基础算法', variant, 'EASY', {
    title: '整数反转',
    description: '给定一个非负整数 n，输出其数字反转后的结果。',
    inputFormat: '输入一个整数 n。',
    outputFormat: '输出反转后的整数。',
    dataRange: '0 <= n <= 10^9。',
    hint: '字符串法最直观，也可用数学取模法实现。',
    cases
  })
}

function buildPalindromeString(variant, rng) {
  const chars = 'abcdefghijklmnopqrstuvwxyz'
  function randomString(length) {
    let s = ''
    for (let i = 0; i < length; i += 1) {
      s += chars[randInt(rng, 0, chars.length - 1)]
    }
    return s
  }
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const len = randInt(rng, 4, 10 + variant)
    const makePalindrome = (i + variant) % 2 === 0
    let s = randomString(len)
    if (makePalindrome) {
      const left = s.slice(0, Math.floor(len / 2))
      const mid = len % 2 === 1 ? s[Math.floor(len / 2)] : ''
      s = left + mid + left.split('').reverse().join('')
    }
    const ok = s === s.split('').reverse().join('')
    cases.push({ input: s, output: ok ? 'YES' : 'NO' })
  }
  return createProblem('字符串', variant, 'MEDIUM', {
    title: '回文串判定',
    description: '给定一个仅包含小写字母的字符串，判断是否为回文串。',
    inputFormat: '输入一行字符串 s。',
    outputFormat: '若是回文串输出 YES，否则输出 NO。',
    dataRange: '1 <= |s| <= 10^5。',
    hint: '双指针从两端向中间收缩即可。',
    cases
  })
}

function buildArraySum(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 5, 10 + variant * 2)
    const arr = randArray(rng, n, -200, 200)
    const sum = arr.reduce((acc, x) => acc + x, 0)
    cases.push({
      input: `${n}\n${stringifyArray(arr)}`,
      output: `${sum}`
    })
  }
  return createProblem('数组', variant, 'EASY', {
    title: '数组元素和',
    description: '给定长度为 n 的整数数组，求所有元素之和。',
    inputFormat: '第一行输入 n；第二行输入 n 个整数。',
    outputFormat: '输出数组元素和。',
    dataRange: '1 <= n <= 2*10^5，数组元素绝对值不超过 10^9。',
    hint: '线性扫描即可，注意总和可能需要 64 位整数。',
    cases
  })
}

function buildMaxMinDiff(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 6, 15 + variant)
    const arr = randArray(rng, n, -500, 500)
    const ans = Math.max(...arr) - Math.min(...arr)
    cases.push({
      input: `${n}\n${stringifyArray(arr)}`,
      output: `${ans}`
    })
  }
  return createProblem('数组', variant, 'EASY', {
    title: '数组极差',
    description: '给定 n 个整数，求最大值与最小值的差。',
    inputFormat: '第一行 n；第二行 n 个整数。',
    outputFormat: '输出 max - min。',
    dataRange: '1 <= n <= 2*10^5。',
    hint: '一次遍历同时维护最小值与最大值。',
    cases
  })
}

function buildRangeSum(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 8, 20 + variant)
    const arr = randArray(rng, n, -50, 120)
    let l = randInt(rng, 1, n)
    let r = randInt(rng, 1, n)
    if (l > r) {
      const t = l
      l = r
      r = t
    }
    const sum = arr.slice(l - 1, r).reduce((acc, x) => acc + x, 0)
    cases.push({
      input: `${n}\n${stringifyArray(arr)}\n${l} ${r}`,
      output: `${sum}`
    })
  }
  return createProblem('前缀和', variant, 'MEDIUM', {
    title: '单次区间求和',
    description: '给定数组和一个查询区间 [l, r]（1-based），输出区间元素和。',
    inputFormat: '第一行 n；第二行 n 个整数；第三行 l r。',
    outputFormat: '输出区间和。',
    dataRange: '1 <= n <= 2*10^5，1 <= l <= r <= n。',
    hint: '可直接遍历区间；若扩展到多次查询建议前缀和。',
    cases
  })
}

function buildMaxSubarray(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 8, 20 + variant)
    const arr = randArray(rng, n, -40, 45)
    if (arr.every((x) => x <= 0)) {
      arr[randInt(rng, 0, n - 1)] = randInt(rng, 1, 40)
    }
    cases.push({
      input: `${n}\n${stringifyArray(arr)}`,
      output: `${maxSubarray(arr)}`
    })
  }
  return createProblem('动态规划', variant, 'MEDIUM', {
    title: '最大子段和',
    description: '给定一个整数数组，求连续子数组的最大和。',
    inputFormat: '第一行 n；第二行 n 个整数。',
    outputFormat: '输出最大子段和。',
    dataRange: '1 <= n <= 2*10^5。',
    hint: 'Kadane 算法可在线性时间内求解。',
    cases
  })
}

function buildLowerBound(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 8, 25 + variant)
    const arr = randArray(rng, n, 1, 150 + variant * 20).sort((a, b) => a - b)
    const x = randInt(rng, 1, 150 + variant * 20)
    cases.push({
      input: `${n} ${x}\n${stringifyArray(arr)}`,
      output: `${lowerBound(arr, x)}`
    })
  }
  return createProblem('二分查找', variant, 'MEDIUM', {
    title: '第一个大于等于 x 的位置',
    description: '给定有序数组，求第一个满足 a[i] >= x 的位置（1-based），不存在输出 -1。',
    inputFormat: '第一行 n x；第二行 n 个递增（可重复）整数。',
    outputFormat: '输出位置或 -1。',
    dataRange: '1 <= n <= 2*10^5。',
    hint: '标准 lower_bound 模板题。',
    cases
  })
}

function buildSortNumbers(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 8, 20 + variant)
    const arr = randArray(rng, n, -300, 300)
    const sorted = [...arr].sort((a, b) => a - b)
    cases.push({
      input: `${n}\n${stringifyArray(arr)}`,
      output: stringifyArray(sorted)
    })
  }
  return createProblem('排序', variant, 'MEDIUM', {
    title: '整数排序',
    description: '给定 n 个整数，请按从小到大排序后输出。',
    inputFormat: '第一行 n；第二行 n 个整数。',
    outputFormat: '输出一行，按升序排列后的数组。',
    dataRange: '1 <= n <= 2*10^5。',
    hint: '可使用快速排序、归并排序或语言内置排序。',
    cases
  })
}

function buildTwoSumSorted(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 8, 22 + variant)
    const arr = randArray(rng, n, 1, 120 + variant * 15).sort((a, b) => a - b)
    const hit = randInt(rng, 0, 1) === 1
    let target
    if (hit) {
      const p = randInt(rng, 0, n - 2)
      const q = randInt(rng, p + 1, n - 1)
      target = arr[p] + arr[q]
    } else {
      target = randInt(rng, 250 + variant * 10, 350 + variant * 20)
    }
    let l = 0
    let r = n - 1
    let ok = false
    while (l < r) {
      const s = arr[l] + arr[r]
      if (s === target) {
        ok = true
        break
      }
      if (s < target) l += 1
      else r -= 1
    }
    cases.push({
      input: `${n} ${target}\n${stringifyArray(arr)}`,
      output: ok ? 'YES' : 'NO'
    })
  }
  return createProblem('双指针', variant, 'MEDIUM', {
    title: '有序数组两数之和判定',
    description: '给定一个非降序数组和目标值 target，判断是否存在两个不同位置元素之和等于 target。',
    inputFormat: '第一行 n target；第二行 n 个非降序整数。',
    outputFormat: '存在输出 YES，否则输出 NO。',
    dataRange: '1 <= n <= 2*10^5。',
    hint: '双指针一头一尾向中间移动即可。',
    cases
  })
}

function buildMergeIntervals(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 4, 10 + Math.floor(variant / 2))
    const intervals = []
    for (let j = 0; j < n; j += 1) {
      const l = randInt(rng, 0, 60 + variant * 6)
      const r = randInt(rng, l, l + randInt(rng, 2, 20))
      intervals.push([l, r])
    }
    const inputLines = [`${n}`]
    for (const [l, r] of intervals) {
      inputLines.push(`${l} ${r}`)
    }
    cases.push({
      input: inputLines.join('\n'),
      output: `${mergedIntervalCount(intervals)}`
    })
  }
  return createProblem('贪心', variant, 'HARD', {
    title: '区间合并后数量',
    description: '给定 n 个闭区间，合并所有有交集的区间后，输出剩余区间个数。',
    inputFormat: '第一行 n；接下来 n 行每行两个整数 l r。',
    outputFormat: '输出合并后区间数量。',
    dataRange: '1 <= n <= 2*10^5。',
    hint: '先按左端点排序，再线性扫描合并。',
    timeLimit: 2000,
    cases
  })
}

function buildGridShortestPath(variant, rng) {
  const cases = []
  for (let i = 0; i < 4; i += 1) {
    const n = randInt(rng, 4, 6 + Math.floor(variant / 2))
    const m = randInt(rng, 4, 6 + Math.floor(variant / 2))
    const grid = []
    for (let x = 0; x < n; x += 1) {
      const row = []
      for (let y = 0; y < m; y += 1) {
        row.push(randInt(rng, 0, 100) < 28 ? 1 : 0)
      }
      grid.push(row)
    }
    grid[0][0] = 0
    grid[n - 1][m - 1] = 0
    const inputLines = [`${n} ${m}`]
    for (const row of grid) {
      inputLines.push(stringifyArray(row))
    }
    cases.push({
      input: inputLines.join('\n'),
      output: `${bfsShortestPath(grid)}`
    })
  }
  return createProblem('图论', variant, 'HARD', {
    title: '网格最短路径',
    description: '给定 n*m 的 0/1 网格，0 表示可走，1 表示障碍。每次可上下左右移动一步，求从左上角到右下角的最短步数。',
    inputFormat: '第一行 n m；接下来 n 行每行 m 个 0/1 整数。',
    outputFormat: '输出最短步数；若不可达输出 -1。',
    dataRange: '1 <= n, m <= 200。',
    hint: '无权最短路可用 BFS，注意起点终点障碍特判。',
    timeLimit: 2500,
    memoryLimit: 512000,
    cases
  })
}

const builders = [
  buildFib,
  buildClimbStairs,
  buildGcd,
  buildLcm,
  buildFastPow,
  buildPrimeCheck,
  buildPrimeCount,
  buildBitCount,
  buildToBinary,
  buildReverseNumber,
  buildPalindromeString,
  buildArraySum,
  buildMaxMinDiff,
  buildRangeSum,
  buildMaxSubarray,
  buildLowerBound,
  buildSortNumbers,
  buildTwoSumSorted,
  buildMergeIntervals,
  buildGridShortestPath
]

function generateProblems() {
  if (builders.length !== TEMPLATE_COUNT) {
    throw new Error(`Expected ${TEMPLATE_COUNT} templates, got ${builders.length}`)
  }
  const problems = []
  for (let t = 0; t < builders.length; t += 1) {
    for (let v = 1; v <= VARIANTS_PER_TEMPLATE; v += 1) {
      const seed = (t + 1) * 100000 + v * 97
      const rng = mulberry32(seed)
      const problem = builders[t](v, rng)
      problems.push(problem)
    }
  }
  return problems
}

function main() {
  const problems = generateProblems()
  const dir = path.dirname(OUTPUT_PATH)
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }
  fs.writeFileSync(OUTPUT_PATH, JSON.stringify(problems, null, 2), 'utf8')

  const stats = problems.reduce((acc, p) => {
    acc[p.difficulty] = (acc[p.difficulty] || 0) + 1
    return acc
  }, {})

  console.log(`Generated: ${problems.length} problems`)
  console.log(`Difficulty: EASY=${stats.EASY || 0}, MEDIUM=${stats.MEDIUM || 0}, HARD=${stats.HARD || 0}`)
  console.log(`Output: ${OUTPUT_PATH}`)
}

if (require.main === module) {
  main()
}

module.exports = {
  generateProblems
}
