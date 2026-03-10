'use strict'

const TARGET_IDS = [
  'P1001', 'P1009', 'P1012', 'P1014', 'P1017',
  'P1022', 'P1024', 'P1025', 'P1028', 'P1029',
  'P1035', 'P1046', 'P1047', 'P1048', 'P1055',
  'P1059', 'P1075', 'P1089', 'P1307', 'P1980'
]

const DIGITS = '0123456789ABCDEF'

function createRng(seed) {
  let state = (Number(seed) >>> 0) || 1
  return function next() {
    state = (state * 1664525 + 1013904223) >>> 0
    return state / 4294967296
  }
}

function randInt(rng, min, max) {
  return min + Math.floor(rng() * (max - min + 1))
}

function choose(rng, arr) {
  return arr[randInt(rng, 0, arr.length - 1)]
}

function tokenizeNumbers(input) {
  return String(input || '').trim().split(/\s+/).filter(Boolean).map(Number)
}

function toFixedSafe(value, digits) {
  const v = Math.abs(value) < 1e-12 ? 0 : value
  return v.toFixed(digits)
}

function solveP1001(input) {
  const [a, b] = tokenizeNumbers(input)
  return String(a + b)
}

function buildP1001Case(rng) {
  const a = randInt(rng, -1000000000, 1000000000)
  const b = randInt(rng, -1000000000, 1000000000)
  return `${a} ${b}`
}

function solveP1009(input) {
  const n = Number(String(input || '').trim())
  let fac = 1n
  let sum = 0n
  for (let i = 1n; i <= BigInt(n); i += 1n) {
    fac *= i
    sum += fac
  }
  return sum.toString()
}

function buildP1009Case(rng) {
  return String(randInt(rng, 1, 50))
}

function solveP1012(input) {
  const lines = String(input || '').trim().split(/\n+/).map((x) => x.trim()).filter(Boolean)
  const n = Number(lines[0])
  const values = (lines[1] || '').split(/\s+/).filter(Boolean).slice(0, n)
  values.sort((a, b) => {
    const ab = a + b
    const ba = b + a
    if (ab === ba) return 0
    return ab > ba ? -1 : 1
  })
  return values.join('')
}

function buildP1012Case(rng) {
  const n = randInt(rng, 2, 12)
  const arr = []
  for (let i = 0; i < n; i += 1) {
    arr.push(String(randInt(rng, 1, 999999)))
  }
  return `${n}\n${arr.join(' ')}`
}

function solveP1014(input) {
  const n = Number(String(input || '').trim())
  let d = Math.floor((Math.sqrt(8 * n + 1) - 1) / 2)
  while ((d * (d + 1)) / 2 < n) d += 1
  const prev = ((d - 1) * d) / 2
  const offset = n - prev
  let num
  let den
  if (d % 2 === 0) {
    num = offset
    den = d - offset + 1
  } else {
    num = d - offset + 1
    den = offset
  }
  return `${num}/${den}`
}

function buildP1014Case(rng) {
  return String(randInt(rng, 1, 10000000))
}

function toNegativeBase(n, base) {
  if (n === 0) return '0'
  let x = n
  const out = []
  while (x !== 0) {
    let rem = x % base
    x = Math.trunc(x / base)
    if (rem < 0) {
      rem -= base
      x += 1
    }
    out.push(DIGITS[rem])
  }
  return out.reverse().join('')
}

function solveP1017(input) {
  const [n, base] = tokenizeNumbers(input)
  const encoded = toNegativeBase(n, base)
  return `${n}=${encoded}(base${base})`
}

function buildP1017Case(rng) {
  const n = randInt(rng, -32768, 32767)
  const base = -randInt(rng, 2, 16)
  return `${n} ${base}`
}

function parseLinearExpr(expr, variable) {
  let i = 0
  let coeff = 0
  let constant = 0
  let sign = 1
  const s = String(expr || '').replace(/\s+/g, '')
  while (i < s.length) {
    if (s[i] === '+') {
      sign = 1
      i += 1
      continue
    }
    if (s[i] === '-') {
      sign = -1
      i += 1
      continue
    }
    let num = ''
    while (i < s.length && s[i] >= '0' && s[i] <= '9') {
      num += s[i]
      i += 1
    }
    if (i < s.length && s[i] === variable) {
      const v = num === '' ? 1 : Number(num)
      coeff += sign * v
      i += 1
    } else {
      const v = num === '' ? 0 : Number(num)
      constant += sign * v
    }
    sign = 1
  }
  return { coeff, constant }
}

function solveP1022(input) {
  const eq = String(input || '').replace(/\s+/g, '')
  const variableMatch = eq.match(/[a-zA-Z]/)
  const variable = variableMatch ? variableMatch[0] : 'x'
  const parts = eq.split('=')
  const left = parseLinearExpr(parts[0] || '', variable)
  const right = parseLinearExpr(parts[1] || '', variable)
  const a = left.coeff - right.coeff
  const b = right.constant - left.constant
  const x = b / a
  return `${variable}=${toFixedSafe(x, 3)}`
}

function formatVarTerm(c, variable) {
  if (c === 0) return ''
  if (c === 1) return variable
  if (c === -1) return `-${variable}`
  return `${c}${variable}`
}

function formatConstTerm(c) {
  if (c === 0) return ''
  return c > 0 ? `+${c}` : String(c)
}

function normalizeExpr(expr) {
  if (!expr) return '0'
  return expr[0] === '+' ? expr.slice(1) : expr
}

function buildP1022Case(rng) {
  const variable = choose(rng, ['a', 'x', 'y', 'm'])
  let c1 = 0
  let c2 = 0
  while (c1 === c2) {
    c1 = randInt(rng, -12, 12)
    c2 = randInt(rng, -12, 12)
  }
  const k1 = randInt(rng, -80, 80)
  const k2 = randInt(rng, -80, 80)
  const left = normalizeExpr(`${formatVarTerm(c1, variable)}${formatConstTerm(k1)}`)
  const right = normalizeExpr(`${formatVarTerm(c2, variable)}${formatConstTerm(k2)}`)
  return `${left}=${right}`
}

function solveP1024(input) {
  const [a, b, c, d] = tokenizeNumbers(input)
  const f = (x) => ((a * x + b) * x + c) * x + d
  const roots = []

  function pushRoot(x) {
    for (const r of roots) {
      if (Math.abs(r - x) < 1e-4) return
    }
    roots.push(x)
  }

  for (let i = -100; i <= 100; i += 1) {
    const y = f(i)
    if (Math.abs(y) < 1e-7) pushRoot(i)
  }

  for (let i = -100; i < 100; i += 1) {
    let l = i
    let r = i + 1
    let fl = f(l)
    let fr = f(r)
    if (Math.abs(fl) < 1e-7 || Math.abs(fr) < 1e-7 || fl * fr > 0) {
      continue
    }
    for (let iter = 0; iter < 80; iter += 1) {
      const m = (l + r) / 2
      const fm = f(m)
      if (fl * fm <= 0) {
        r = m
        fr = fm
      } else {
        l = m
        fl = fm
      }
    }
    pushRoot((l + r) / 2)
  }

  roots.sort((x, y) => x - y)
  return roots.slice(0, 3).map((x) => toFixedSafe(x, 2)).join(' ')
}

function buildP1024Case(rng) {
  let r1 = randInt(rng, -25, 25)
  let r2 = randInt(rng, -25, 25)
  let r3 = randInt(rng, -25, 25)
  while (r1 === r2 || r1 === r3 || r2 === r3) {
    r2 = randInt(rng, -25, 25)
    r3 = randInt(rng, -25, 25)
  }
  const roots = [r1, r2, r3].sort((a, b) => a - b)
  r1 = roots[0]
  r2 = roots[1]
  r3 = roots[2]
  const a = 1
  const b = -(r1 + r2 + r3)
  const c = r1 * r2 + r1 * r3 + r2 * r3
  const d = -r1 * r2 * r3
  return `${a} ${b} ${c} ${d}`
}

function solveP1025(input) {
  const [n, k] = tokenizeNumbers(input)
  const dp = Array.from({ length: k + 1 }, () => Array(n + 1).fill(0))
  dp[0][0] = 1
  for (let j = 1; j <= k; j += 1) {
    for (let i = 1; i <= n; i += 1) {
      const v1 = i - 1 >= 0 ? dp[j - 1][i - 1] : 0
      const v2 = i - j >= 0 ? dp[j][i - j] : 0
      dp[j][i] = v1 + v2
    }
  }
  return String(dp[k][n])
}

function buildP1025Case(rng) {
  const n = randInt(rng, 7, 200)
  const k = randInt(rng, 2, 6)
  return `${n} ${k}`
}

function solveP1028(input) {
  const n = Number(String(input || '').trim())
  if (n <= 1) return '1'
  const dp = Array(n + 1).fill(0)
  const prefix = Array(n + 1).fill(0)
  dp[1] = 1
  prefix[1] = 1
  for (let i = 2; i <= n; i += 1) {
    dp[i] = 1 + prefix[Math.floor(i / 2)]
    prefix[i] = prefix[i - 1] + dp[i]
  }
  return String(dp[n])
}

function buildP1028Case(rng) {
  return String(randInt(rng, 1, 1000))
}

function countDistinctPrimeFactors(n) {
  let x = n
  let count = 0
  for (let p = 2; p * p <= x; p += 1) {
    if (x % p !== 0) continue
    count += 1
    while (x % p === 0) x = Math.floor(x / p)
  }
  if (x > 1) count += 1
  return count
}

function solveP1029(input) {
  const [x0, y0] = tokenizeNumbers(input)
  if (y0 % x0 !== 0) return '0'
  const n = Math.floor(y0 / x0)
  const distinct = countDistinctPrimeFactors(n)
  return String(2 ** distinct)
}

function buildP1029Case(rng) {
  const x0 = randInt(rng, 1, 20000)
  const primes = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31]
  let ratio = 1
  const picked = randInt(rng, 1, 5)
  for (let i = 0; i < picked; i += 1) {
    const p = choose(rng, primes)
    const exp = randInt(rng, 1, 3)
    for (let e = 0; e < exp; e += 1) {
      if (ratio * p > 100000) break
      ratio *= p
    }
  }
  if (rng() < 0.15) {
    return `${x0} ${x0 * ratio + 1}`
  }
  return `${x0} ${x0 * ratio}`
}

function solveP1035(input) {
  const k = Number(String(input || '').trim())
  let sum = 0
  let n = 0
  while (sum <= k) {
    n += 1
    sum += 1 / n
  }
  return String(n)
}

function buildP1035Case(rng) {
  return String(randInt(rng, 1, 15))
}

function solveP1046(input) {
  const lines = String(input || '').trim().split(/\n+/).map((x) => x.trim()).filter(Boolean)
  const heights = (lines[0] || '').split(/\s+/).map(Number).slice(0, 10)
  const h = Number(lines[1] || 0)
  let count = 0
  for (const v of heights) {
    if (v <= h + 30) count += 1
  }
  return String(count)
}

function buildP1046Case(rng) {
  const heights = Array.from({ length: 10 }, () => randInt(rng, 100, 200))
  const h = randInt(rng, 100, 200)
  return `${heights.join(' ')}\n${h}`
}

function solveP1047(input) {
  const lines = String(input || '').trim().split(/\n+/).map((x) => x.trim()).filter(Boolean)
  const [l, m] = lines[0].split(/\s+/).map(Number)
  const alive = Array(l + 1).fill(true)
  for (let i = 1; i <= m; i += 1) {
    const [s, e] = lines[i].split(/\s+/).map(Number)
    const lo = Math.max(0, Math.min(s, e))
    const hi = Math.min(l, Math.max(s, e))
    for (let x = lo; x <= hi; x += 1) {
      alive[x] = false
    }
  }
  return String(alive.reduce((acc, v) => acc + (v ? 1 : 0), 0))
}

function buildP1047Case(rng) {
  const l = randInt(rng, 50, 10000)
  const m = randInt(rng, 1, 40)
  const lines = [`${l} ${m}`]
  for (let i = 0; i < m; i += 1) {
    const a = randInt(rng, 0, l)
    const b = randInt(rng, 0, l)
    lines.push(`${Math.min(a, b)} ${Math.max(a, b)}`)
  }
  return lines.join('\n')
}

function solveP1048(input) {
  const lines = String(input || '').trim().split(/\n+/).map((x) => x.trim()).filter(Boolean)
  const [t, m] = lines[0].split(/\s+/).map(Number)
  const dp = Array(t + 1).fill(0)
  for (let i = 1; i <= m; i += 1) {
    const [cost, value] = lines[i].split(/\s+/).map(Number)
    for (let j = t; j >= cost; j -= 1) {
      dp[j] = Math.max(dp[j], dp[j - cost] + value)
    }
  }
  return String(dp[t])
}

function buildP1048Case(rng) {
  const t = randInt(rng, 60, 1000)
  const m = randInt(rng, 5, 60)
  const lines = [`${t} ${m}`]
  for (let i = 0; i < m; i += 1) {
    const cost = randInt(rng, 1, Math.min(100, t))
    const value = randInt(rng, 1, 300)
    lines.push(`${cost} ${value}`)
  }
  return lines.join('\n')
}

function isbnCheckChar(digits9) {
  let sum = 0
  for (let i = 0; i < 9; i += 1) {
    sum += (i + 1) * Number(digits9[i])
  }
  const mod = sum % 11
  return mod === 10 ? 'X' : String(mod)
}

function solveP1055(input) {
  const s = String(input || '').trim()
  const clean = s.replace(/-/g, '')
  const digits9 = clean.slice(0, 9)
  const expected = isbnCheckChar(digits9)
  const actual = (clean[9] || '').toUpperCase()
  if (expected === actual) return 'Right'
  return `${s.slice(0, -1)}${expected}`
}

function buildP1055Case(rng) {
  const digits = Array.from({ length: 9 }, () => String(randInt(rng, 0, 9)))
  const correct = isbnCheckChar(digits.join(''))
  let last = correct
  if (rng() < 0.5) {
    const options = '0123456789X'.split('').filter((x) => x !== correct)
    last = choose(rng, options)
  }
  const s = `${digits[0]}-${digits.slice(1, 4).join('')}-${digits.slice(4, 9).join('')}-${last}`
  return s
}

function solveP1059(input) {
  const nums = tokenizeNumbers(input)
  const n = nums[0]
  const arr = nums.slice(1, 1 + n)
  const uniq = Array.from(new Set(arr)).sort((a, b) => a - b)
  return `${uniq.length}\n${uniq.join(' ')}`
}

function buildP1059Case(rng) {
  const n = randInt(rng, 5, 120)
  const arr = []
  for (let i = 0; i < n; i += 1) {
    arr.push(randInt(rng, 1, 1000))
  }
  return `${n}\n${arr.join(' ')}`
}

function solveP1075(input) {
  const n = Number(String(input || '').trim())
  for (let d = 2; d * d <= n; d += 1) {
    if (n % d === 0) {
      return String(Math.max(d, Math.floor(n / d)))
    }
  }
  return String(n)
}

function sieve(limit) {
  const isPrime = Array(limit + 1).fill(true)
  isPrime[0] = false
  isPrime[1] = false
  for (let i = 2; i * i <= limit; i += 1) {
    if (!isPrime[i]) continue
    for (let j = i * i; j <= limit; j += i) {
      isPrime[j] = false
    }
  }
  const out = []
  for (let i = 2; i <= limit; i += 1) {
    if (isPrime[i]) out.push(i)
  }
  return out
}

const SMALL_PRIMES = sieve(5000)

function buildP1075Case(rng) {
  const p = choose(rng, SMALL_PRIMES.slice(0, 300))
  const q = choose(rng, SMALL_PRIMES.slice(0, 500))
  const lo = Math.min(p, q)
  const hi = Math.max(p, q)
  return String(lo * hi)
}

function solveP1089(input) {
  const cost = String(input || '').trim().split(/\s+/).filter(Boolean).map(Number).slice(0, 12)
  let cash = 0
  let bank = 0
  for (let i = 0; i < 12; i += 1) {
    cash += 300
    cash -= cost[i]
    if (cash < 0) return `-${i + 1}`
    const save = Math.floor(cash / 100) * 100
    bank += save
    cash -= save
  }
  return String(cash + Math.floor(bank * 1.2))
}

function buildP1089Case(rng) {
  const arr = Array.from({ length: 12 }, () => randInt(rng, 0, 350))
  return arr.join('\n')
}

function solveP1307(input) {
  const n = Number(String(input || '').trim())
  const sign = n < 0 ? -1 : 1
  let s = String(Math.abs(n)).split('').reverse().join('')
  s = s.replace(/^0+/, '')
  if (!s) s = '0'
  if (sign < 0 && s !== '0') return `-${s}`
  return s
}

function buildP1307Case(rng) {
  const n = randInt(rng, -2000000000, 2000000000)
  return String(n)
}

function countDigitInRange(n, d) {
  if (n <= 0) return 0
  let count = 0
  for (let factor = 1; factor <= n; factor *= 10) {
    const lower = n % factor
    const current = Math.floor(n / factor) % 10
    const higher = Math.floor(n / (factor * 10))
    if (d !== 0) {
      if (current < d) count += higher * factor
      else if (current === d) count += higher * factor + lower + 1
      else count += (higher + 1) * factor
    } else {
      if (higher === 0) continue
      if (current === 0) count += (higher - 1) * factor + lower + 1
      else count += higher * factor
    }
  }
  return count
}

function solveP1980(input) {
  const [n, x] = tokenizeNumbers(input)
  return String(countDigitInRange(n, x))
}

function buildP1980Case(rng) {
  const n = randInt(rng, 1, 20000000)
  const x = randInt(rng, 0, 9)
  return `${n} ${x}`
}

const RECIPES = {
  P1001: { name: 'A+B', solve: solveP1001, buildCase: buildP1001Case, hiddenCount: 80 },
  P1009: { name: 'Factorial Sum', solve: solveP1009, buildCase: buildP1009Case, hiddenCount: 45 },
  P1012: { name: 'Largest Number', solve: solveP1012, buildCase: buildP1012Case, hiddenCount: 70 },
  P1014: { name: 'Cantor Table', solve: solveP1014, buildCase: buildP1014Case, hiddenCount: 70 },
  P1017: { name: 'Negative Base', solve: solveP1017, buildCase: buildP1017Case, hiddenCount: 70 },
  P1022: { name: 'Linear Equation', solve: solveP1022, buildCase: buildP1022Case, hiddenCount: 70 },
  P1024: { name: 'Cubic Roots', solve: solveP1024, buildCase: buildP1024Case, hiddenCount: 60 },
  P1025: { name: 'Partition Count', solve: solveP1025, buildCase: buildP1025Case, hiddenCount: 60 },
  P1028: { name: 'Number Count DP', solve: solveP1028, buildCase: buildP1028Case, hiddenCount: 60 },
  P1029: { name: 'GCD/LCM Pair Count', solve: solveP1029, buildCase: buildP1029Case, hiddenCount: 60 },
  P1035: { name: 'Harmonic Sum', solve: solveP1035, buildCase: buildP1035Case, hiddenCount: 13 },
  P1046: { name: 'Apple Reach', solve: solveP1046, buildCase: buildP1046Case, hiddenCount: 60 },
  P1047: { name: 'Road Trees', solve: solveP1047, buildCase: buildP1047Case, hiddenCount: 60 },
  P1048: { name: 'Herb Knapsack', solve: solveP1048, buildCase: buildP1048Case, hiddenCount: 60 },
  P1055: { name: 'ISBN', solve: solveP1055, buildCase: buildP1055Case, hiddenCount: 60 },
  P1059: { name: 'Unique Sort', solve: solveP1059, buildCase: buildP1059Case, hiddenCount: 60 },
  P1075: { name: 'Prime Factor', solve: solveP1075, buildCase: buildP1075Case, hiddenCount: 60 },
  P1089: { name: 'Saving Plan', solve: solveP1089, buildCase: buildP1089Case, hiddenCount: 60 },
  P1307: { name: 'Reverse Integer', solve: solveP1307, buildCase: buildP1307Case, hiddenCount: 60 },
  P1980: { name: 'Digit Count', solve: solveP1980, buildCase: buildP1980Case, hiddenCount: 60 }
}

module.exports = {
  TARGET_IDS,
  RECIPES,
  createRng,
  randInt
}
