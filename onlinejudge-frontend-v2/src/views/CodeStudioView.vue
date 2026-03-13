<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">代码工坊</h1>
      <p class="section-subtitle">真实提交流程 + 提交历史回填，便于快速迭代调试。</p>
    </header>

    <AppCard>
      <div class="grid gap-3 md:grid-cols-[1fr_160px_220px]">
        <select v-model.number="currentProblemId" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" @change="handleProblemChange">
          <option :value="0">请选择题目</option>
          <option v-for="p in problemOptions" :key="p.id" :value="p.id">#{{ p.id }} {{ p.title }}</option>
        </select>

        <select v-model="form.language" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" @change="handleLanguageChange">
          <option value="JAVA">Java</option>
          <option value="CPP">C++</option>
          <option value="PYTHON">Python</option>
        </select>

        <div class="flex gap-2">
          <AppButton variant="secondary" @click="formatCode">格式化</AppButton>
          <AppButton :disabled="submitting" @click="submit">{{ submitting ? '提交中...' : '提交代码' }}</AppButton>
        </div>
      </div>
    </AppCard>

    <div class="grid gap-4 xl:grid-cols-[1.45fr_0.55fr]">
      <AppCard padding="lg">
        <h2 class="text-lg font-semibold text-slate-800">{{ currentProblemTitle }}</h2>
        <p class="mt-1 text-xs text-soft">时间限制 {{ problemDetail.timeLimit || 2000 }} ms · 内存限制 {{ problemDetail.memoryLimit || 256000 }} KB</p>
        <div class="mt-4 space-y-3">
          <section class="rounded-xl border border-line bg-slate-50/60 p-4">
            <h3 class="text-sm font-semibold text-slate-800">题目描述</h3>
            <ProblemRichContent class="mt-2" :content="problemDetail.description || '请选择题目后查看描述'" />
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

          <section v-if="hasHint" class="rounded-xl border border-amber-200 bg-amber-50 p-4">
            <h3 class="text-sm font-semibold text-amber-800">提示</h3>
            <ProblemRichContent class="mt-2" :content="hintText" />
          </section>
        </div>
        <textarea
          v-model="form.code"
          class="mt-4 h-[500px] w-full resize-y rounded-lg border border-line bg-slate-950 p-4 font-mono text-sm leading-6 text-slate-100 outline-none transition focus:border-sky-400"
          spellcheck="false"
        />
      </AppCard>

      <div class="space-y-4">
        <AppCard>
          <h3 class="text-base font-semibold text-slate-800">评测状态</h3>
          <div v-if="latest.id" class="mt-3 space-y-2 text-sm">
            <div class="rounded-md border border-line px-3 py-2">提交 ID：{{ latest.id }}</div>
            <div class="rounded-md border border-line px-3 py-2">状态：<span class="font-semibold">{{ latest.status || '-' }}</span></div>
            <div class="rounded-md border border-line px-3 py-2">耗时：{{ latest.executeTime ?? '-' }} ms</div>
            <div class="rounded-md border border-line px-3 py-2">内存：{{ latest.executeMemory ?? '-' }} KB</div>
            <div v-if="latest.submitTime" class="rounded-md border border-line px-3 py-2 text-soft">提交时间：{{ formatDate(latest.submitTime) }}</div>
            <div v-if="latest.errorMessage" class="rounded-md border border-line px-3 py-2 text-rose-600">{{ latest.errorMessage }}</div>
          </div>
          <EmptyState v-else message="提交后将显示实时判题状态" />
        </AppCard>

        <AppCard>
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
                <option value="ACCEPTED">ACCEPTED</option>
                <option value="WRONG_ANSWER">WRONG_ANSWER</option>
                <option value="TIME_LIMIT_EXCEEDED">TIME_LIMIT_EXCEEDED</option>
                <option value="MEMORY_LIMIT_EXCEEDED">MEMORY_LIMIT_EXCEEDED</option>
                <option value="RUNTIME_ERROR">RUNTIME_ERROR</option>
                <option value="COMPILE_ERROR">COMPILE_ERROR</option>
                <option value="PENDING">PENDING</option>
                <option value="JUDGING">JUDGING</option>
              </select>
              <select v-model="historyLanguage" class="rounded-lg border border-line px-3 py-2 text-xs outline-none focus:border-slate-800">
                <option value="">全部语言</option>
                <option value="JAVA">JAVA</option>
                <option value="CPP">CPP</option>
                <option value="PYTHON">PYTHON</option>
              </select>
            </div>
          </div>

          <div v-if="historyLoading" class="mt-3 grid gap-2">
            <div v-for="n in 6" :key="`history-skeleton-${n}`" class="skeleton h-14 rounded-lg" />
          </div>

          <div v-else-if="recentSubmissions.length" class="mt-3 space-y-2 text-sm">
            <div
              v-for="item in recentSubmissions"
              :key="item.id"
              class="rounded-md border px-3 py-2"
              :class="selectedHistoryId === item.id ? 'border-slate-900 bg-slate-50' : 'border-line'"
            >
              <div class="flex items-center justify-between">
                <span class="font-medium text-slate-800">#{{ item.id }}</span>
                <AppBadge :tone="statusTone(item.status)">{{ item.status || '-' }}</AppBadge>
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
import { useRoute, useRouter } from 'vue-router'
import { problemApi, submissionApi } from '@/api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
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

const currentProblemTitle = computed(() => {
  if (!currentProblemId.value) return '请选择题目'
  const found = problemOptions.value.find((p) => Number(p.id) === Number(currentProblemId.value))
  return found ? `#${found.id} ${found.title}` : `题目 #${currentProblemId.value}`
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

function normalizeText(value) {
  if (value === null || value === undefined) return ''
  return String(value)
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

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
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
  if (!currentProblemId.value) {
    form.code = TEMPLATES[form.language]
    return
  }
  const draft = loadDraft(currentProblemId.value, form.language)
  form.code = draft ?? TEMPLATES[form.language]
}

async function loadProblemOptions() {
  const res = await problemApi.getProblemList({ page: 1, size: 200 })
  problemOptions.value = res.data?.records || []
}

async function loadProblemDetail() {
  if (!currentProblemId.value) {
    problemDetail.value = {}
    return
  }
  const res = await problemApi.getProblemDetail(currentProblemId.value)
  problemDetail.value = res.data || {}
}

async function loadRecentSubmissions() {
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
  const query = currentProblemId.value ? { problemId: String(currentProblemId.value) } : {}
  router.replace({ path: '/studio', query })
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
    if (line.startsWith('}')) indent = Math.max(0, indent - 1)
    output.push(`${'  '.repeat(indent)}${line}`)
    if (line.endsWith('{')) indent += 1
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
    const query = currentProblemId.value ? { problemId: String(currentProblemId.value) } : {}
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
    saveDraft(currentProblemId.value, form.language, val)
  }
)

watch([historyScope, historyStatus, historyLanguage], () => {
  loadRecentSubmissions()
})

watch(
  () => route.query.problemId,
  async (pid) => {
    const n = Number(pid || 0)
    currentProblemId.value = Number.isFinite(n) ? n : 0
    await loadProblemDetail()
    applyTemplateIfNeeded()
    await loadRecentSubmissions()
  }
)

onMounted(async () => {
  await loadProblemOptions()
  currentProblemId.value = Number(route.query.problemId || 0)
  await Promise.all([loadProblemDetail(), loadRecentSubmissions()])
  applyTemplateIfNeeded()
})

onBeforeUnmount(() => {
  clearPollTimer()
})
</script>
