<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">代码工坊</h1>
      <p class="section-subtitle">
        {{ isContestMode ? '比赛模式：仅显示题面与评测状态。' : '在线做题、提交评测、查看题解讨论与历史提交。' }}
      </p>
    </header>

    <AppCard>
      <div class="grid gap-3 md:grid-cols-[1fr_160px_220px]">
        <select
          v-model.number="currentProblemId"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          @change="handleProblemChange"
        >
          <option :value="0">请选择题目</option>
          <option v-for="p in problemOptions" :key="p.id" :value="p.id">#{{ p.id }} {{ p.title }}</option>
        </select>

        <select
          v-model="form.language"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          @change="handleLanguageChange"
        >
          <option value="JAVA">Java</option>
          <option value="CPP">C++</option>
          <option value="PYTHON">Python</option>
        </select>

        <div class="flex gap-2">
          <AppButton variant="secondary" @click="formatCode">格式化代码</AppButton>
          <AppButton :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '提交代码' }}</AppButton>
        </div>
      </div>
    </AppCard>

    <div class="grid items-start gap-4 xl:grid-cols-[1.45fr_0.55fr]">
      <AppCard padding="lg">
        <h2 class="text-lg font-semibold text-slate-800">{{ currentProblemTitle }}</h2>
        <p class="mt-1 text-xs text-soft">
          时间限制 {{ problemDetail.timeLimit || 2000 }} ms · 内存限制 {{ problemDetail.memoryLimit || 256000 }} KB
        </p>

        <section v-if="tagList.length" class="tag-strip mt-4">
          <div class="tag-toggle mb-3">
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-lg border border-line bg-white px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-100"
              @click="tagsExpanded = !tagsExpanded"
            >
              <span>相关标签</span>
            </button>
          </div>
          <Transition name="tag-panel">
            <div v-if="tagsExpanded" class="tag-content mb-1 flex flex-wrap gap-2">
              <span
                v-for="tag in tagList"
                :key="`plain-${tag.key}`"
                class="inline-flex items-center rounded-lg border border-line bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-700"
              >
                {{ tag.label }}
              </span>
            </div>
          </Transition>
        </section>

        <div class="mt-4 space-y-3">
          <section class="rounded-xl border border-line bg-slate-50/60 p-4">
            <h3 class="text-sm font-semibold text-slate-800">题目描述</h3>
            <ProblemRichContent class="mt-2" :content="problemDetail.description || '请选择题目后查看题面'" />
          </section>

          <section v-if="hasInputFormat" class="rounded-xl border border-line bg-white p-4">
            <h3 class="text-sm font-semibold text-slate-800">输入格式</h3>
            <ProblemRichContent class="mt-2" :content="inputFormatText" />
          </section>

          <section v-if="hasOutputFormat" class="rounded-xl border border-line bg-white p-4">
            <h3 class="text-sm font-semibold text-slate-800">输出格式</h3>
            <ProblemRichContent class="mt-2" :content="outputFormatText" />
          </section>

          <section v-if="hasSample" class="rounded-xl border border-line bg-white p-4">
            <h3 class="text-sm font-semibold text-slate-800">样例</h3>
            <div class="mt-2 grid gap-3 md:grid-cols-2">
              <div class="rounded-lg border border-line bg-slate-900 p-3">
                <div class="mb-2 text-xs text-slate-300">输入</div>
                <pre class="max-h-52 overflow-auto whitespace-pre-wrap text-xs leading-6 text-slate-100">{{ sampleInputText }}</pre>
              </div>
              <div class="rounded-lg border border-line bg-slate-900 p-3">
                <div class="mb-2 text-xs text-slate-300">输出</div>
                <pre class="max-h-52 overflow-auto whitespace-pre-wrap text-xs leading-6 text-slate-100">{{ sampleOutputText }}</pre>
              </div>
            </div>
          </section>

          <section v-if="!isContestMode && hasHint" class="rounded-xl border border-amber-200 bg-amber-50 p-4">
            <h3 class="text-sm font-semibold text-amber-800">提示</h3>
            <ProblemRichContent class="mt-2" :content="hintText" />
          </section>
        </div>

        <CodeEditor
          v-model="form.code"
          v-model:autocomplete-enabled="autocompleteEnabled"
          class="mt-4"
          :language="form.language"
        />
      </AppCard>

      <div class="space-y-4">
        <AppCard>
          <h3 class="text-base font-semibold text-slate-800">评测状态</h3>
          <div v-if="latest.id" class="mt-3 space-y-2 text-sm">
            <div class="rounded-md border border-line px-3 py-2">提交 ID：{{ latest.id }}</div>
            <div class="rounded-md border border-line px-3 py-2">
              状态：<span class="font-semibold">{{ statusText(latest.status) }}</span>
            </div>
            <div class="rounded-md border border-line px-3 py-2">耗时：{{ latest.executeTime ?? '-' }} ms</div>
            <div class="rounded-md border border-line px-3 py-2">内存：{{ latest.executeMemory ?? '-' }} KB</div>
            <div v-if="latest.submitTime" class="rounded-md border border-line px-3 py-2 text-soft">
              提交时间：{{ formatDate(latest.submitTime) }}
            </div>
            <div v-if="latest.caseResults && latest.caseResults.length" class="rounded-md border border-line px-3 py-2">
              <div class="text-xs font-medium text-slate-700">测试点明细</div>
              <div class="mt-2 max-h-44 space-y-1 overflow-y-auto pr-1">
                <div
                  v-for="item in latest.caseResults"
                  :key="`case-${item.caseNo}`"
                  class="flex items-center justify-between rounded border border-line/80 px-2 py-1 text-xs"
                >
                  <div class="text-slate-700">
                    #{{ item.caseNo }} {{ Number(item.isSample) === 1 ? '样例' : '隐藏' }}
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-soft">{{ item.timeUsed ?? '-' }}ms</span>
                    <AppBadge :tone="statusTone(item.status)">{{ statusText(item.status) }}</AppBadge>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="latest.errorMessage" class="rounded-md border border-rose-200 bg-rose-50 px-3 py-2">
              <div class="text-xs font-medium text-rose-700">错误详情</div>
              <pre class="mt-1 whitespace-pre-wrap break-words font-mono text-xs leading-5 text-rose-700">{{ latest.errorMessage }}</pre>
            </div>
          </div>
          <EmptyState v-else message="提交后将显示实时判题状态" />
        </AppCard>

        <AppCard v-if="!isContestMode">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-semibold text-slate-800">题解讨论</h3>
            <AppButton size="sm" variant="secondary" :disabled="discussionLoading" @click="loadProblemDiscussions">
              {{ discussionLoading ? '刷新中...' : '刷新' }}
            </AppButton>
          </div>

          <div v-if="!currentProblemId" class="mt-3">
            <EmptyState message="先选择题目，再查看题解讨论" />
          </div>

          <div v-else-if="discussionLoading" class="mt-3 grid gap-2">
            <div v-for="n in 4" :key="`discussion-skeleton-${n}`" class="skeleton h-16 rounded-lg" />
          </div>

          <div v-else-if="problemDiscussions.length" class="mt-3 space-y-2">
            <article
              v-for="item in problemDiscussions"
              :key="item.id"
              class="cursor-pointer rounded-lg border border-line px-3 py-2 transition hover:border-slate-700"
              @click="openDiscussion(item.id)"
            >
              <h4 class="line-clamp-1 text-sm font-medium text-slate-800">{{ item.title }}</h4>
              <p class="mt-1 line-clamp-2 text-xs text-soft">{{ item.contentPreview || '' }}</p>
              <div class="mt-1 flex items-center gap-2 text-[11px] text-soft">
                <UserIdentity :user="item" avatar-size="xs" />
                <span>{{ formatDate(item.createTime) }}</span>
              </div>
            </article>
            <RouterLink :to="`/discuss?problemId=${currentProblemId}`" class="block">
              <AppButton size="sm" block variant="ghost">查看该题全部讨论</AppButton>
            </RouterLink>
          </div>

          <EmptyState v-else message="该题暂时没有讨论，欢迎发布第一篇题解" />
        </AppCard>

        <AppCard v-if="!isContestMode">
          <div class="flex items-center justify-between">
            <h3 class="text-base font-semibold text-slate-800">提交历史</h3>
            <AppButton size="sm" variant="secondary" :disabled="historyLoading" @click="loadRecentSubmissions">
              {{ historyLoading ? '刷新中...' : '刷新' }}
            </AppButton>
          </div>

          <div class="mt-3 grid gap-2">
            <select v-model="historyScope" class="rounded-lg border border-line px-3 py-2 text-xs outline-none focus:border-slate-800">
              <option value="CURRENT">当前题目</option>
              <option value="ALL">全部题目</option>
            </select>
            <div class="grid grid-cols-2 gap-2">
              <select v-model="historyStatus" class="rounded-lg border border-line px-3 py-2 text-xs outline-none focus:border-slate-800">
                <option value="">全部状态</option>
                <option value="ACCEPTED">通过</option>
                <option value="WRONG_ANSWER">答案错误</option>
                <option value="TIME_LIMIT_EXCEEDED">超出时间限制</option>
                <option value="MEMORY_LIMIT_EXCEEDED">超出内存限制</option>
                <option value="RUNTIME_ERROR">运行时错误</option>
                <option value="COMPILE_ERROR">编译错误</option>
                <option value="PENDING">等待评测</option>
                <option value="JUDGING">评测中</option>
              </select>
              <select v-model="historyLanguage" class="rounded-lg border border-line px-3 py-2 text-xs outline-none focus:border-slate-800">
                <option value="">全部语言</option>
                <option value="JAVA">JAVA</option>
                <option value="CPP">CPP</option>
                <option value="PYTHON">PYTHON</option>
              </select>
            </div>
          </div>

          <div v-if="historyLoading" class="mt-3 grid max-h-[420px] gap-2 overflow-y-auto pr-1">
            <div v-for="n in 6" :key="`history-skeleton-${n}`" class="skeleton h-14 rounded-lg" />
          </div>

          <div v-else-if="recentSubmissions.length" class="mt-3 max-h-[420px] space-y-2 overflow-y-auto pr-1 text-sm">
            <div
              v-for="item in recentSubmissions"
              :key="item.id"
              class="rounded-md border px-3 py-2"
              :class="selectedHistoryId === item.id ? 'border-slate-900 bg-slate-50' : 'border-line'"
            >
              <div class="flex items-center justify-between">
                <span class="font-medium text-slate-800">#{{ item.id }}</span>
                <div class="flex items-center gap-1">
                  <AppBadge :tone="statusTone(item.status)">{{ statusText(item.status) }}</AppBadge>
                  <AppBadge :tone="item.status === 'ACCEPTED' ? 'success' : 'warn'">
                    {{ item.status === 'ACCEPTED' ? '已通过' : '尝试过' }}
                  </AppBadge>
                </div>
              </div>
              <div class="mt-1 text-xs text-soft">题目 #{{ item.problemId }} · {{ item.language || '-' }}</div>
              <div class="mt-1 text-xs text-soft">{{ formatDate(item.submitTime) }}</div>
              <div class="mt-2 flex gap-2">
                <AppButton size="sm" variant="secondary" @click="restoreFromHistory(item)">回填代码</AppButton>
                <AppButton size="sm" variant="ghost" @click="selectHistory(item)">设为当前结果</AppButton>
              </div>
            </div>
          </div>
          <EmptyState v-else message="暂无提交记录" />
        </AppCard>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { discussionApi, problemApi, submissionApi } from '@/api'
import UserIdentity from '@/components/ui/UserIdentity.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import CodeEditor from '@/components/problem/CodeEditor.vue'
import ProblemRichContent from '@/components/problem/ProblemRichContent.vue'

const route = useRoute()
const router = useRouter()

const problemOptions = ref([])
const problemDetail = ref({})
const currentProblemId = ref(0)

const form = reactive({
  language: 'JAVA',
  code: ''
})

const submitting = ref(false)
const latest = ref({})
const recentSubmissions = ref([])
const historyLoading = ref(false)
const selectedHistoryId = ref(0)
const historyScope = ref('CURRENT')
const historyStatus = ref('')
const historyLanguage = ref('')

const discussionLoading = ref(false)
const problemDiscussions = ref([])
const tagsExpanded = ref(false)

const AUTOCOMPLETE_STORAGE_KEY = 'ojv2:studio:autocomplete-enabled'
const autocompleteEnabled = ref(readAutocompletePreference())

let timer = null

const TEMPLATES = {
  JAVA: `import java.util.*;\n\npublic class Main {\n  public static void main(String[] args) {\n\n  }\n}`,
  CPP: `#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n\n  return 0;\n}`,
  PYTHON: `def main():\n    pass\n\nif __name__ == '__main__':\n    main()`
}

const DONE_STATUS = new Set([
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR'
])

const TAG_ALGO_META = {
  math: {
    label: '数学',
    description: '通常考查数论、公式推导、取模运算、精度与边界处理。'
  },
  implementation: {
    label: '实现',
    description: '核心在按题意完整模拟，重视细节、边界与输入输出处理。'
  },
  'prefix-sum': {
    label: '前缀和',
    description: '通过预处理累计信息，将区间求和等查询从 O(n) 降到 O(1)。'
  },
  array: {
    label: '数组',
    description: '围绕顺序存储结构进行遍历、统计、双指针或下标技巧处理。'
  },
  string: {
    label: '字符串',
    description: '涉及字符处理、匹配、构造、哈希或字典序等文本相关算法。'
  },
  simulation: {
    label: '模拟',
    description: '按真实流程一步步执行，重点是流程完整性与状态维护。'
  },
  dp: {
    label: '动态规划',
    description: '通过状态定义与转移方程，把复杂问题拆成可复用子问题。'
  },
  sort: {
    label: '排序',
    description: '利用排序或自定义比较器组织数据，常和贪心或统计结合。'
  },
  graph: {
    label: '图论',
    description: '把问题建模为点和边，常用遍历、最短路、拓扑等图算法。'
  },
  bfs: {
    label: '广度优先搜索',
    description: '按层扩展搜索，适合无权最短路、最少步数或状态图问题。'
  },
  stack: {
    label: '栈',
    description: '利用后进先出特性处理括号匹配、单调性或表达式求值。'
  },
  'topological-sort': {
    label: '拓扑排序',
    description: '用于有向无环图的依赖排序，典型场景是任务调度先后关系。'
  },
  greedy: {
    label: '贪心',
    description: '每一步选择当前最优策略，关键在证明局部最优可导向全局最优。'
  },
  binarysearch: {
    label: '二分查找',
    description: '利用单调性折半搜索答案或位置，把复杂度降到对数级。'
  },
  'binary-search': {
    label: '二分查找',
    description: '利用单调性折半搜索答案或位置，把复杂度降到对数级。'
  }
}

const currentProblemTitle = computed(() => {
  if (!currentProblemId.value) return '请选择题目'
  const found = problemOptions.value.find((p) => Number(p.id) === Number(currentProblemId.value))
  return found ? `#${found.id} ${found.title}` : `题目 #${currentProblemId.value}`
})
const isContestMode = computed(() => {
  const contestId = Number(route.query.contestId || 0)
  return Number.isInteger(contestId) && contestId > 0
})

const inputFormatText = computed(() => normalizeText(problemDetail.value.inputFormat))
const outputFormatText = computed(() => normalizeText(problemDetail.value.outputFormat))
const hintText = computed(() => normalizeText(problemDetail.value.hint))
const sampleInputText = computed(() => {
  const first = problemDetail.value?.examples?.[0]?.input
  return normalizeText(problemDetail.value.sampleInput || first)
})
const sampleOutputText = computed(() => {
  const first = problemDetail.value?.examples?.[0]?.output
  return normalizeText(problemDetail.value.sampleOutput || first)
})
const hasInputFormat = computed(() => !!inputFormatText.value.trim())
const hasOutputFormat = computed(() => !!outputFormatText.value.trim())
const hasHint = computed(() => !!hintText.value.trim())
const hasSample = computed(() => !!sampleInputText.value.trim() || !!sampleOutputText.value.trim())
const tagList = computed(() => {
  const source = problemDetail.value?.tags
  const rawList = source
    ? (Array.isArray(source) ? source : String(source).split(/[,|/\u3001\uFF0C]/))
    : []
  const seen = new Set()
  const result = []

  for (const item of rawList) {
    const raw = normalizeText(item).trim()
    if (!raw) continue
    const key = normalizeTagKey(raw)
    if (seen.has(key)) continue
    seen.add(key)
    result.push({
      raw,
      key,
      label: tagDisplayName(raw)
    })
  }

  if (!result.length) {
    return buildFallbackTags(problemDetail.value).map((label) => ({
      raw: label,
      key: `fallback-${normalizeTagKey(label)}`,
      label
    }))
  }

  return result
})

function normalizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value)
}

function readAutocompletePreference() {
  if (typeof window === 'undefined') return true
  const raw = window.localStorage.getItem(AUTOCOMPLETE_STORAGE_KEY)
  if (raw === null) return true
  return raw !== 'false'
}

function normalizeTagKey(tag) {
  return normalizeText(tag).trim().toLowerCase().replace(/[_\s]+/g, '-')
}

function tagDisplayName(tag) {
  const key = normalizeTagKey(tag)
  return TAG_ALGO_META[key]?.label || normalizeText(tag).trim()
}

function buildFallbackTags(problem) {
  if (!problem?.id) return []

  const text = [
    normalizeText(problem.title),
    normalizeText(problem.description),
    normalizeText(problem.inputFormat),
    normalizeText(problem.outputFormat),
    normalizeText(problem.hint)
  ]
    .join(' ')
    .toLowerCase()

  const tags = []
  const addTag = (label) => {
    if (label && !tags.includes(label)) tags.push(label)
  }

  if (/字符串|string|字符|回文|子串/.test(text)) addTag('字符串')
  if (/栈|括号|stack/.test(text)) addTag('栈')
  if (/队列|queue/.test(text)) addTag('队列')
  if (/树|tree/.test(text)) addTag('树')
  if (/图|最短路|路径|连通|拓扑/.test(text)) addTag('图论')
  if (/bfs|广度/.test(text)) addTag('广度优先搜索')
  if (/dfs|深度/.test(text)) addTag('深度优先搜索')
  if (/排序|sort|排名|第k|top/.test(text)) addTag('排序')
  if (/前缀和|区间和|prefix/.test(text)) addTag('前缀和')
  if (/数组|array|子数组/.test(text)) addTag('数组')
  if (/动态规划|dp|方案数|状态转移/.test(text)) addTag('动态规划')
  if (/递归|recursion|斐波那契/.test(text)) addTag('递归')
  if (/二分|binary/.test(text)) addTag('二分查找')
  if (/贪心|greedy/.test(text)) addTag('贪心')
  if (/二进制|位运算|按位/.test(text)) addTag('位运算')
  if (/数学|质数|素数|公约数|公倍数|幂|取模|数列/.test(text)) addTag('数学')
  if (/模拟|实现|转换|统计|判定/.test(text)) addTag('实现')

  if (!tags.length) addTag(problem.difficulty === 'HARD' ? '综合' : '实现')

  return tags.slice(0, 4)
}

function clearPollTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function statusTone(status) {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'PENDING' || status === 'JUDGING') return 'warn'
  if (status === 'WRONG_ANSWER' || status === 'COMPILE_ERROR' || status === 'RUNTIME_ERROR') return 'danger'
  return 'neutral'
}

function statusText(status) {
  const map = {
    ACCEPTED: '通过',
    WRONG_ANSWER: '答案错误',
    TIME_LIMIT_EXCEEDED: '超出时间限制',
    MEMORY_LIMIT_EXCEEDED: '超出内存限制',
    RUNTIME_ERROR: '运行时错误',
    COMPILE_ERROR: '编译错误',
    PENDING: '等待评测',
    JUDGING: '评测中'
  }
  return map[status] || status || '-'
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function draftKey(problemId, language) {
  return `ojv2:draft:${problemId}:${language}`
}

function loadDraft(problemId, language) {
  if (!problemId) return null
  return localStorage.getItem(draftKey(problemId, language))
}

function saveDraft(problemId, language, code) {
  if (!problemId) return
  localStorage.setItem(draftKey(problemId, language), code || '')
}

function applyTemplateIfNeeded() {
  if (isContestMode.value || !currentProblemId.value) {
    form.code = TEMPLATES[form.language]
    return
  }
  const draft = loadDraft(currentProblemId.value, form.language)
  form.code = draft ?? TEMPLATES[form.language]
}

function buildStudioQuery(problemId) {
  const query = {}
  if (problemId) {
    query.problemId = String(problemId)
  }
  const contestId = Number(route.query.contestId || 0)
  if (contestId > 0) {
    query.contestId = String(contestId)
  }
  return query
}

async function loadSidebarData() {
  if (isContestMode.value) {
    problemDiscussions.value = []
    recentSubmissions.value = []
    return
  }
  await Promise.all([loadProblemDiscussions(), loadRecentSubmissions()])
}

async function loadProblemOptions() {
  const res = await problemApi.getProblemList({ page: 1, size: 300 })
  problemOptions.value = res.data?.records || []
}

async function loadProblemDetail() {
  if (!currentProblemId.value) {
    problemDetail.value = {}
    tagsExpanded.value = false
    return
  }
  const res = await problemApi.getProblemDetail(currentProblemId.value)
  problemDetail.value = res.data || {}
  tagsExpanded.value = false
}

async function loadProblemDiscussions() {
  if (isContestMode.value) {
    problemDiscussions.value = []
    return
  }
  if (!currentProblemId.value) {
    problemDiscussions.value = []
    return
  }
  discussionLoading.value = true
  try {
    const res = await discussionApi.getPostList({
      page: 1,
      size: 6,
      problemId: Number(currentProblemId.value)
    })
    problemDiscussions.value = res.data?.records || []
  } finally {
    discussionLoading.value = false
  }
}

async function loadRecentSubmissions() {
  if (isContestMode.value) {
    recentSubmissions.value = []
    historyLoading.value = false
    return
  }
  historyLoading.value = true
  try {
    const params = {
      page: 1,
      size: 12,
      status: historyStatus.value || undefined,
      language: historyLanguage.value || undefined
    }
    if (historyScope.value === 'CURRENT' && currentProblemId.value) {
      params.problemId = Number(currentProblemId.value)
    }
    const res = await submissionApi.getSubmissionList(params)
    recentSubmissions.value = res.data?.records || []
  } finally {
    historyLoading.value = false
  }
}

function handleLanguageChange() {
  applyTemplateIfNeeded()
}

function handleProblemChange() {
  const query = buildStudioQuery(currentProblemId.value)
  router.replace({ path: '/studio', query })
}

function openDiscussion(id) {
  router.push(`/discuss/${id}`)
}

function formatCode() {
  const lines = String(form.code || '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((line) => line.replace(/\t/g, '  ').replace(/[ \t]+$/g, ''))

  const output = []
  let indent = 0
  for (const raw of lines) {
    const line = raw.trim()
    if (!line) {
      output.push('')
      continue
    }
    if (line.startsWith('}') || line.startsWith(']')) indent = Math.max(0, indent - 1)
    output.push(`${'  '.repeat(indent)}${line}`)
    if (line.endsWith('{') || line.endsWith('[')) indent += 1
  }
  form.code = output.join('\n').replace(/\n{3,}/g, '\n\n').trimEnd()
}

function selectHistory(item) {
  selectedHistoryId.value = item.id
  latest.value = {
    id: item.id,
    status: item.status,
    executeTime: item.executeTime,
    executeMemory: item.executeMemory,
    errorMessage: item.errorMessage,
    submitTime: item.submitTime
  }
}

async function restoreFromHistory(item) {
  let target = item
  if (!target.code) {
    const res = await submissionApi.getSubmissionById(item.id)
    target = res.data || target
  }
  if (target.problemId && Number(target.problemId) !== Number(currentProblemId.value)) {
    currentProblemId.value = Number(target.problemId)
    await loadProblemDetail()
    if (!isContestMode.value) {
      await loadProblemDiscussions()
    }
    const query = buildStudioQuery(currentProblemId.value)
    router.replace({ path: '/studio', query })
  }
  if (target.language) {
    form.language = target.language
  }
  form.code = target.code || TEMPLATES[form.language]
  selectHistory(target)
}

async function pollSubmission(id) {
  clearPollTimer()
  timer = setInterval(async () => {
    try {
      const res = await submissionApi.getSubmissionStatus(id, { silent: true })
      latest.value = res.data || {}
      if (DONE_STATUS.has(latest.value.status)) {
        clearPollTimer()
        await loadRecentSubmissions()
      }
    } catch (_) {
      clearPollTimer()
    }
  }, 1500)
}

async function submit() {
  if (!currentProblemId.value || !form.code.trim()) return
  submitting.value = true
  try {
    const res = await submissionApi.submitCode({
      problemId: Number(currentProblemId.value),
      code: form.code,
      language: form.language
    })
    const id = res.data?.id
    latest.value = { id, status: 'PENDING' }
    if (id) {
      selectedHistoryId.value = id
      await pollSubmission(id)
    }
  } finally {
    submitting.value = false
  }
}

watch(
  () => form.code,
  (val) => {
    if (isContestMode.value) return
    saveDraft(currentProblemId.value, form.language, val)
  }
)

watch(autocompleteEnabled, (val) => {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(AUTOCOMPLETE_STORAGE_KEY, String(val))
})

watch([historyScope, historyStatus, historyLanguage], () => {
  if (isContestMode.value) return
  loadRecentSubmissions()
})

watch(
  () => [route.query.problemId, route.query.contestId],
  async ([pid]) => {
    const n = Number(pid || 0)
    currentProblemId.value = Number.isFinite(n) ? n : 0
    await loadProblemDetail()
    await loadSidebarData()
    applyTemplateIfNeeded()
  }
)

onMounted(async () => {
  await loadProblemOptions()
  currentProblemId.value = Number(route.query.problemId || 0)
  await loadProblemDetail()
  await loadSidebarData()
  applyTemplateIfNeeded()
})

onBeforeUnmount(() => {
  clearPollTimer()
})
</script>

<style scoped>
.tag-panel-enter-active,
.tag-panel-leave-active {
  transition: all 0.2s ease;
}

.tag-panel-enter-from,
.tag-panel-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
