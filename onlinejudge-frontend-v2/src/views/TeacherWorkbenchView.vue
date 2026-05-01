<template>
  <section class="space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="section-title">教师工作台</h1>
        <p class="section-subtitle">查看教学数据概览，并发布、维护编程题目与测试数据。</p>
      </div>
      <div class="flex items-center gap-2">
        <select v-model.number="days" class="rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-slate-800">
          <option :value="7">最近 7 天</option>
          <option :value="14">最近 14 天</option>
          <option :value="30">最近 30 天</option>
        </select>
        <AppButton size="sm" variant="secondary" :disabled="loading" @click="loadData">刷新</AppButton>
        <AppButton size="sm" :disabled="loading || exporting" @click="exportCsv">{{ exporting ? '导出中...' : '导出数据' }}</AppButton>
      </div>
    </header>

    <p v-if="errorMsg" class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
      {{ errorMsg }}
    </p>

    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
      <AppCard v-for="item in statCards" :key="item.label">
        <div class="text-xs text-soft">{{ item.label }}</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">{{ item.value }}</div>
      </AppCard>
    </div>

    <div class="grid gap-4 lg:grid-cols-2">
      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">提交状态分布</h2>
        <div v-if="loading" class="mt-4 grid gap-2">
          <div v-for="n in 4" :key="`status-skeleton-${n}`" class="skeleton h-8 rounded-lg" />
        </div>
        <div v-else-if="statusBars.length" class="mt-4 space-y-3">
          <div v-for="item in statusBars" :key="item.key" class="space-y-1">
            <div class="flex items-center justify-between text-xs">
              <span class="text-slate-700">{{ statusText(item.key) }}</span>
              <span class="text-soft">{{ item.value }} ({{ item.percentText }})</span>
            </div>
            <div class="h-2.5 overflow-hidden rounded-full bg-slate-100">
              <div class="h-full rounded-full transition-all duration-300" :style="{ width: item.percentText, background: item.color }" />
            </div>
          </div>
        </div>
        <div v-else class="mt-4 text-sm text-soft">暂无提交状态数据</div>
      </AppCard>

      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">语言分布</h2>
        <div v-if="loading" class="mt-4 grid gap-2">
          <div v-for="n in 4" :key="`lang-skeleton-${n}`" class="skeleton h-8 rounded-lg" />
        </div>
        <div v-else-if="languageBars.length" class="mt-4 space-y-3">
          <div v-for="item in languageBars" :key="item.key" class="space-y-1">
            <div class="flex items-center justify-between text-xs">
              <span class="text-slate-700">{{ item.key }}</span>
              <span class="text-soft">{{ item.value }} ({{ item.percentText }})</span>
            </div>
            <div class="h-2.5 overflow-hidden rounded-full bg-slate-100">
              <div class="h-full rounded-full bg-slate-700 transition-all duration-300" :style="{ width: item.percentText }" />
            </div>
          </div>
        </div>
        <div v-else class="mt-4 text-sm text-soft">暂无语言分布数据</div>
      </AppCard>
    </div>

    <AppCard>
      <h2 class="text-lg font-semibold text-slate-800">每日提交趋势</h2>
      <div v-if="loading" class="mt-4 grid gap-2">
        <div v-for="n in 6" :key="`trend-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>
      <div v-else-if="trendList.length" class="mt-4 rounded-lg border border-line bg-slate-50 p-3">
        <div class="mb-3 flex items-center justify-end gap-4 text-xs text-soft">
          <span class="inline-flex items-center gap-2">
            <span class="h-2.5 w-2.5 rounded-full bg-sky-500" />
            总提交
          </span>
          <span class="inline-flex items-center gap-2">
            <span class="h-2.5 w-2.5 rounded-full bg-emerald-500" />
            通过
          </span>
        </div>
        <svg viewBox="0 0 760 260" class="h-56 w-full">
          <defs>
            <linearGradient id="teacher-trend-total" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stop-color="#38bdf8" stop-opacity="0.22" />
              <stop offset="100%" stop-color="#38bdf8" stop-opacity="0.04" />
            </linearGradient>
            <linearGradient id="teacher-trend-accepted" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stop-color="#10b981" stop-opacity="0.20" />
              <stop offset="100%" stop-color="#10b981" stop-opacity="0.03" />
            </linearGradient>
          </defs>

          <g>
            <line
              v-for="guide in chartGuides"
              :key="guide.y"
              x1="48"
              :y1="guide.y"
              x2="730"
              :y2="guide.y"
              stroke="#d7dee8"
              stroke-dasharray="4 6"
            />
          </g>

          <path :d="totalAreaPath" fill="url(#teacher-trend-total)" />
          <path :d="acceptedAreaPath" fill="url(#teacher-trend-accepted)" />
          <path :d="totalLinePath" fill="none" stroke="#0ea5e9" stroke-linecap="round" stroke-linejoin="round" stroke-width="4" />
          <path :d="acceptedLinePath" fill="none" stroke="#10b981" stroke-linecap="round" stroke-linejoin="round" stroke-width="4" />

          <g>
            <circle
              v-for="point in totalChartPoints"
              :key="`total-${point.index}`"
              :cx="point.x"
              :cy="point.y"
              r="4.5"
              fill="#0ea5e9"
              stroke="#fff"
              stroke-width="2.5"
            />
            <circle
              v-for="point in acceptedChartPoints"
              :key="`accepted-${point.index}`"
              :cx="point.x"
              :cy="point.y"
              r="4.5"
              fill="#10b981"
              stroke="#fff"
              stroke-width="2.5"
            />
          </g>

          <g class="fill-slate-400 text-[11px]">
            <text v-for="guide in chartGuides" :key="`guide-${guide.y}`" x="8" :y="guide.y + 4">
              {{ guide.label }}
            </text>
            <text
              v-for="point in totalChartPoints"
              :key="`date-${point.index}`"
              :x="point.x"
              y="244"
              text-anchor="middle"
            >
              {{ point.label }}
            </text>
          </g>
        </svg>
      </div>
      <div v-else class="mt-4 text-sm text-soft">暂无趋势数据</div>

      <div v-if="trendList.length" class="mt-4 overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="border-b border-line text-left text-soft">
              <th class="px-3 py-2 font-medium">日期</th>
              <th class="px-3 py-2 font-medium">总提交</th>
              <th class="px-3 py-2 font-medium">通过数</th>
              <th class="px-3 py-2 font-medium">通过率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in trendList" :key="item.date" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2 text-slate-700">{{ item.date || '-' }}</td>
              <td class="px-3 py-2 text-slate-700">{{ item.totalSubmissions ?? 0 }}</td>
              <td class="px-3 py-2 text-slate-700">{{ item.acceptedSubmissions ?? 0 }}</td>
              <td class="px-3 py-2 text-slate-700">{{ formatRate(item.acceptanceRate) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>

    <AppCard>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold text-slate-800">编程题发布与管理</h2>
          <p class="mt-1 text-xs text-soft">支持编辑题目描述、样例输入输出，并维护多组隐藏测试数据。</p>
        </div>
        <div class="flex items-center gap-2">
          <AppButton size="sm" variant="secondary" :disabled="manageLoading" @click="loadManageProblems">
            {{ manageLoading ? '加载中...' : '刷新题目列表' }}
          </AppButton>
          <AppButton size="sm" variant="ghost" @click="resetProblemForm">新建题目</AppButton>
        </div>
      </div>

      <p v-if="manageMessage" class="mt-3 rounded-lg border px-3 py-2 text-sm" :class="manageMessageTone === 'error' ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'">
        {{ manageMessage }}
      </p>

      <div class="mt-4 grid items-start gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <section class="space-y-4 rounded-xl border border-line bg-slate-50 p-4">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-semibold text-slate-800">
              {{ editingProblemId ? `编辑题目 #${editingProblemId}` : '发布新题目' }}
            </h3>
            <AppButton size="sm" variant="ghost" @click="resetProblemForm">清空</AppButton>
          </div>

          <div class="grid gap-3 md:grid-cols-2">
            <label class="space-y-1.5 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">题目标题</span>
              <input v-model.trim="problemForm.title" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="例如：A + B Problem" />
            </label>

            <label class="space-y-1.5">
              <span class="text-sm font-medium text-slate-700">难度</span>
              <select v-model="problemForm.difficulty" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
                <option value="EASY">简单</option>
                <option value="MEDIUM">中等</option>
                <option value="HARD">困难</option>
              </select>
            </label>

            <label class="space-y-1.5">
              <span class="text-sm font-medium text-slate-700">状态</span>
              <select v-model.number="problemForm.status" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
                <option :value="1">发布</option>
                <option :value="0">隐藏</option>
              </select>
            </label>

            <label class="space-y-1.5">
              <span class="text-sm font-medium text-slate-700">时间限制 (ms)</span>
              <input v-model.number="problemForm.timeLimit" type="number" min="1" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" />
            </label>

            <label class="space-y-1.5">
              <span class="text-sm font-medium text-slate-700">内存限制 (KB)</span>
              <input v-model.number="problemForm.memoryLimit" type="number" min="1" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" />
            </label>

            <label class="space-y-1.5 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">标签（逗号分隔）</span>
              <input v-model.trim="problemForm.tags" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="数组, 字符串, 模拟" />
            </label>

            <label class="space-y-1.5 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">题目描述</span>
              <textarea v-model.trim="problemForm.description" class="h-28 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入题目描述（支持 Markdown）" />
            </label>

            <label class="space-y-1.5 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">输入格式</span>
              <textarea v-model.trim="problemForm.inputFormat" class="h-20 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入输入格式" />
            </label>

            <label class="space-y-1.5 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">输出格式</span>
              <textarea v-model.trim="problemForm.outputFormat" class="h-20 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="请输入输出格式" />
            </label>

            <label class="space-y-1.5">
              <span class="text-sm font-medium text-slate-700">样例输入</span>
              <textarea v-model="problemForm.sampleInput" class="h-24 w-full rounded-lg border border-line px-3 py-2 text-sm font-mono outline-none focus:border-slate-800" placeholder="样例输入" />
            </label>

            <label class="space-y-1.5">
              <span class="text-sm font-medium text-slate-700">样例输出</span>
              <textarea v-model="problemForm.sampleOutput" class="h-24 w-full rounded-lg border border-line px-3 py-2 text-sm font-mono outline-none focus:border-slate-800" placeholder="样例输出" />
            </label>

            <label class="space-y-1.5 md:col-span-2">
              <span class="text-sm font-medium text-slate-700">提示（可选）</span>
              <textarea v-model.trim="problemForm.hint" class="h-20 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="可填写解题提示" />
            </label>
          </div>

          <div class="rounded-xl border border-line bg-white p-3">
            <div class="mb-2 flex items-center justify-between">
              <h4 class="text-sm font-semibold text-slate-800">隐藏测试数据（多组）</h4>
              <AppButton size="sm" variant="secondary" @click="addHiddenCase">新增一组</AppButton>
            </div>
            <div class="max-h-[360px] space-y-3 overflow-y-auto pr-1">
              <article v-for="(item, index) in hiddenCases" :key="`case-${index}`" class="rounded-lg border border-line bg-slate-50 p-3">
                <div class="mb-2 flex items-center justify-between text-xs text-soft">
                  <span>测试组 #{{ index + 1 }}</span>
                  <button
                    type="button"
                    class="text-rose-600 hover:underline"
                    :disabled="hiddenCases.length <= 1"
                    @click="removeHiddenCase(index)"
                  >
                    删除
                  </button>
                </div>
                <div class="grid gap-2 md:grid-cols-2">
                  <textarea v-model="item.input" class="h-24 w-full rounded-lg border border-line px-2 py-1.5 text-xs font-mono outline-none focus:border-slate-800" placeholder="输入数据" />
                  <textarea v-model="item.output" class="h-24 w-full rounded-lg border border-line px-2 py-1.5 text-xs font-mono outline-none focus:border-slate-800" placeholder="输出数据" />
                </div>
              </article>
            </div>
          </div>

          <div class="flex justify-end gap-2">
            <AppButton variant="secondary" :disabled="savingProblem" @click="resetProblemForm">重置</AppButton>
            <AppButton :disabled="savingProblem" @click="submitProblem">
              {{ savingProblem ? '保存中...' : (editingProblemId ? '更新题目' : '发布题目') }}
            </AppButton>
          </div>
        </section>

        <section class="rounded-xl border border-line bg-white p-4">
          <h3 class="text-base font-semibold text-slate-800">已发布题目</h3>
          <p class="mt-1 text-xs text-soft">教师显示自己创建的题目，管理员显示全部题目。</p>

          <div v-if="manageLoading" class="mt-3 grid gap-2">
            <div v-for="n in 6" :key="`problem-skeleton-${n}`" class="skeleton h-14 rounded-lg" />
          </div>

          <div v-else-if="manageableProblems.length" class="mt-3 max-h-[860px] space-y-2 overflow-y-auto pr-1">
            <article v-for="item in manageableProblems" :key="item.id" class="rounded-lg border border-line px-3 py-2">
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <div class="truncate text-sm font-semibold text-slate-800">#{{ item.id }} {{ item.title }}</div>
                  <div class="mt-1 text-xs text-soft">
                    {{ difficultyText(item.difficulty) }} · 提交 {{ item.submitCount || 0 }} · 通过 {{ item.acceptCount || 0 }}
                  </div>
                </div>
                <AppBadge :tone="Number(item.status) === 1 ? 'success' : 'warn'">
                  {{ Number(item.status) === 1 ? '已发布' : '已隐藏' }}
                </AppBadge>
              </div>
              <div class="mt-2 flex gap-2">
                <AppButton size="sm" variant="secondary" @click="editProblem(item)">编辑</AppButton>
                <AppButton size="sm" variant="ghost" @click="openProblem(item.id)">去做题</AppButton>
                <AppButton size="sm" variant="ghost" @click="removeProblem(item)">删除</AppButton>
              </div>
            </article>
          </div>

          <EmptyState v-else message="暂无可管理题目，先发布第一道题吧" />
        </section>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { problemApi, teacherApi, testCaseApi } from '@/api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { useUiStore } from '@/stores/useUiStore'
import { useUserStore } from '@/stores/useUserStore'
import { useRouter } from 'vue-router'

const STATUS_COLORS = {
  ACCEPTED: '#10b981',
  WRONG_ANSWER: '#f43f5e',
  COMPILE_ERROR: '#f59e0b',
  RUNTIME_ERROR: '#ef4444',
  TIME_LIMIT_EXCEEDED: '#8b5cf6',
  MEMORY_LIMIT_EXCEEDED: '#a855f7',
  PENDING: '#64748b',
  JUDGING: '#0ea5e9'
}

const DEFAULT_PROBLEM_FORM = {
  title: '',
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  hint: '',
  timeLimit: 1000,
  memoryLimit: 262144,
  difficulty: 'MEDIUM',
  tags: '',
  status: 1
}

const router = useRouter()
const ui = useUiStore()
const userStore = useUserStore()

const loading = ref(false)
const exporting = ref(false)
const errorMsg = ref('')
const days = ref(7)
const overview = ref({})

const manageLoading = ref(false)
const savingProblem = ref(false)
const editingProblemId = ref(0)
const hiddenCases = ref([{ input: '', output: '' }])
const problemList = ref([])
const manageMessage = ref('')
const manageMessageTone = ref('success')

const problemForm = reactive({ ...DEFAULT_PROBLEM_FORM })

const statCards = computed(() => [
  { label: '学生总数', value: overview.value.totalStudents ?? 0 },
  { label: '教师总数', value: overview.value.totalTeachers ?? 0 },
  { label: '题库总量', value: overview.value.totalProblems ?? 0 },
  { label: '总提交量', value: overview.value.totalSubmissions ?? 0 },
  { label: '通过率', value: formatRate(overview.value.acceptanceRate) }
])

const statusBars = computed(() => {
  const list = mapToList(overview.value.submissionStatusDistribution)
  return withPercent(list, (item) => STATUS_COLORS[item.key] || '#334155')
})

const languageBars = computed(() => {
  const list = mapToList(overview.value.languageDistribution)
  return withPercent(list, () => '#334155')
})

const trendList = computed(() => overview.value.dailySubmissionTrend || [])
const currentUserId = computed(() => Number(userStore.userInfo?.id || 0))

const manageableProblems = computed(() => {
  if (userStore.isAdmin) return problemList.value
  const ownerId = currentUserId.value
  return problemList.value.filter((item) => Number(item.creatorId || 0) === ownerId)
})

const chartGuides = computed(() => {
  const maxValue = Math.max(
    ...trendList.value.map((item) => Number(item.totalSubmissions || 0)),
    ...trendList.value.map((item) => Number(item.acceptedSubmissions || 0)),
    1
  )
  const steps = 4
  return Array.from({ length: steps + 1 }, (_, idx) => {
    const value = Math.round((maxValue / steps) * (steps - idx))
    const y = 28 + (204 / steps) * idx
    return { y, label: value }
  })
})

const totalChartPoints = computed(() => buildChartPoints(trendList.value, 'totalSubmissions'))
const acceptedChartPoints = computed(() => buildChartPoints(trendList.value, 'acceptedSubmissions'))
const totalLinePath = computed(() => linePath(totalChartPoints.value))
const acceptedLinePath = computed(() => linePath(acceptedChartPoints.value))
const totalAreaPath = computed(() => areaPath(totalChartPoints.value))
const acceptedAreaPath = computed(() => areaPath(acceptedChartPoints.value))

function mapToList(map) {
  if (!map || typeof map !== 'object') return []
  return Object.entries(map)
    .map(([key, value]) => ({ key, value: Number(value || 0) }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value)
}

function withPercent(list, colorFactory) {
  const total = list.reduce((sum, item) => sum + item.value, 0)
  if (!total) return []
  return list.map((item) => ({
    ...item,
    color: colorFactory(item),
    percentText: `${((item.value / total) * 100).toFixed(1)}%`
  }))
}

function buildChartPoints(list, key) {
  if (!Array.isArray(list) || !list.length) return []
  const left = 60
  const width = 650
  const height = 184
  const top = 24
  const maxValue = Math.max(
    ...list.map((item) => Number(item.totalSubmissions || 0)),
    ...list.map((item) => Number(item.acceptedSubmissions || 0)),
    1
  )
  const step = list.length === 1 ? 0 : width / (list.length - 1)

  return list.map((item, index) => {
    const value = Number(item[key] || 0)
    const x = left + index * step
    const y = top + height - (value / maxValue) * height
    return {
      index,
      x,
      y,
      value,
      label: String(item.date || '').slice(5)
    }
  })
}

function linePath(points) {
  if (!points.length) return ''
  return points.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`).join(' ')
}

function areaPath(points) {
  if (!points.length) return ''
  const baseline = 208
  const first = points[0]
  const last = points[points.length - 1]
  return `${linePath(points)} L ${last.x} ${baseline} L ${first.x} ${baseline} Z`
}

function statusText(status) {
  const map = {
    ACCEPTED: '通过',
    WRONG_ANSWER: '答案错误',
    COMPILE_ERROR: '编译错误',
    RUNTIME_ERROR: '运行错误',
    TIME_LIMIT_EXCEEDED: '超时',
    MEMORY_LIMIT_EXCEEDED: '超内存',
    PENDING: '等待中',
    JUDGING: '评测中'
  }
  return map[status] || status
}

function difficultyText(difficulty) {
  if (difficulty === 'EASY') return '简单'
  if (difficulty === 'HARD') return '困难'
  return '中等'
}

function formatRate(rate) {
  if (rate === null || rate === undefined || Number.isNaN(Number(rate))) return '0.00%'
  return `${Number(rate).toFixed(2)}%`
}

function openProblem(problemId) {
  if (!problemId) return
  router.push({ path: '/studio', query: { problemId: String(problemId) } })
}

function resetProblemForm() {
  Object.assign(problemForm, DEFAULT_PROBLEM_FORM)
  hiddenCases.value = [{ input: '', output: '' }]
  editingProblemId.value = 0
  manageMessage.value = ''
}

function addHiddenCase() {
  hiddenCases.value.push({ input: '', output: '' })
}

function removeHiddenCase(index) {
  if (hiddenCases.value.length <= 1) return
  hiddenCases.value.splice(index, 1)
}

function normalizeTestCases() {
  const list = []
  for (const item of hiddenCases.value) {
    const input = String(item?.input || '').trim()
    const output = String(item?.output || '').trim()
    if (!input && !output) continue
    if (!input || !output) {
      throw new Error('测试数据每一组都需要同时填写输入和输出')
    }
    list.push({ input, output })
  }
  return list
}

function buildProblemPayload() {
  const title = String(problemForm.title || '').trim()
  const description = String(problemForm.description || '').trim()
  if (!title || !description) {
    throw new Error('题目标题和题目描述不能为空')
  }
  if (!Number(problemForm.timeLimit) || Number(problemForm.timeLimit) <= 0) {
    throw new Error('时间限制必须大于 0')
  }
  if (!Number(problemForm.memoryLimit) || Number(problemForm.memoryLimit) <= 0) {
    throw new Error('内存限制必须大于 0')
  }

  return {
    title,
    description,
    inputFormat: String(problemForm.inputFormat || '').trim(),
    outputFormat: String(problemForm.outputFormat || '').trim(),
    sampleInput: String(problemForm.sampleInput || ''),
    sampleOutput: String(problemForm.sampleOutput || ''),
    hint: String(problemForm.hint || '').trim(),
    timeLimit: Number(problemForm.timeLimit),
    memoryLimit: Number(problemForm.memoryLimit),
    difficulty: String(problemForm.difficulty || 'MEDIUM'),
    tags: String(problemForm.tags || '').trim(),
    status: Number(problemForm.status) === 0 ? 0 : 1
  }
}

async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await teacherApi.getOverview({ days: days.value })
    overview.value = res?.data || {}
  } catch (e) {
    errorMsg.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function exportCsv() {
  exporting.value = true
  try {
    const res = await teacherApi.exportOverview({ days: days.value })
    const csv = String(res?.data || '')
    const withBom = `\uFEFF${csv}`
    const blob = new Blob([withBom], { type: 'text/csv;charset=utf-8;' })
    const fileName = `教师数据概览-${days.value}天.csv`
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

async function loadManageProblems() {
  manageLoading.value = true
  try {
    const res = await problemApi.getProblemList({ page: 1, size: 100, includeHidden: true })
    problemList.value = res.data?.records || []
  } catch (e) {
    manageMessageTone.value = 'error'
    manageMessage.value = e.message || '加载题目列表失败'
  } finally {
    manageLoading.value = false
  }
}

async function editProblem(item) {
  const targetId = Number(item?.id || 0)
  if (!targetId) return
  manageMessage.value = ''
  savingProblem.value = true
  try {
    const [detailRes, caseRes] = await Promise.all([
      problemApi.getProblemDetail(targetId),
      testCaseApi.getProblemTestCases(targetId)
    ])
    const detail = detailRes.data || {}
    Object.assign(problemForm, {
      title: detail.title || '',
      description: detail.description || '',
      inputFormat: detail.inputFormat || '',
      outputFormat: detail.outputFormat || '',
      sampleInput: detail.sampleInput || '',
      sampleOutput: detail.sampleOutput || '',
      hint: detail.hint || '',
      timeLimit: Number(detail.timeLimit || 1000),
      memoryLimit: Number(detail.memoryLimit || 262144),
      difficulty: detail.difficulty || 'MEDIUM',
      tags: detail.tags || '',
      status: Number(detail.status) === 0 ? 0 : 1
    })
    const loadedCases = (caseRes.data || []).map((tc) => ({
      input: tc.input || '',
      output: tc.output || ''
    }))
    hiddenCases.value = loadedCases.length ? loadedCases : [{ input: '', output: '' }]
    editingProblemId.value = targetId
    manageMessageTone.value = 'success'
    manageMessage.value = `已加载题目 #${targetId}，你可以继续编辑并更新。`
  } catch (e) {
    manageMessageTone.value = 'error'
    manageMessage.value = e.message || '加载题目详情失败'
  } finally {
    savingProblem.value = false
  }
}

async function submitProblem() {
  manageMessage.value = ''
  savingProblem.value = true
  try {
    const payload = buildProblemPayload()
    const testCases = normalizeTestCases()

    let problemId = editingProblemId.value
    if (problemId) {
      await problemApi.updateProblem(problemId, payload)
    } else {
      const created = await problemApi.createProblem(payload)
      problemId = Number(created.data || 0)
      editingProblemId.value = problemId
    }

    await testCaseApi.replaceProblemTestCases(problemId, testCases)
    await loadManageProblems()
    manageMessageTone.value = 'success'
    manageMessage.value = editingProblemId.value
      ? `题目 #${problemId} 已保存，测试数据已更新。`
      : '题目发布成功。'
  } catch (e) {
    manageMessageTone.value = 'error'
    manageMessage.value = e.message || '保存题目失败'
  } finally {
    savingProblem.value = false
  }
}

async function removeProblem(item) {
  const targetId = Number(item?.id || 0)
  if (!targetId) return
  const ok = await ui.confirm({
    title: '删除题目',
    message: `确定删除题目 #${targetId} 吗？该操作不可撤销。`,
    okText: '删除',
    cancelText: '取消'
  })
  if (!ok) return

  savingProblem.value = true
  manageMessage.value = ''
  try {
    await problemApi.deleteProblem(targetId)
    if (editingProblemId.value === targetId) {
      resetProblemForm()
    }
    await loadManageProblems()
    manageMessageTone.value = 'success'
    manageMessage.value = `题目 #${targetId} 已删除。`
  } catch (e) {
    manageMessageTone.value = 'error'
    manageMessage.value = e.message || '删除题目失败'
  } finally {
    savingProblem.value = false
  }
}

onMounted(async () => {
  await userStore.ensureUserInfo().catch(() => null)
  await Promise.all([loadData(), loadManageProblems()])
})
</script>
