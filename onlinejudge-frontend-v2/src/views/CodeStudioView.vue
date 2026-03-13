<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">代码工坊</h1>
      <p class="section-subtitle">已接入真实提交流程：提交 -> 轮询状态 -> 返回结果。</p>
    </header>

    <AppCard>
      <div class="grid gap-3 md:grid-cols-[1fr_160px_140px]">
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
          <AppButton @click="submit" :disabled="submitting">{{ submitting ? '提交中...' : '提交' }}</AppButton>
        </div>
      </div>
    </AppCard>

    <div class="grid gap-4 xl:grid-cols-[1.4fr_0.6fr]">
      <AppCard padding="lg">
        <h2 class="text-lg font-semibold text-slate-800">{{ currentProblemTitle }}</h2>
        <p class="mt-1 text-xs text-soft">时间限制 {{ problemDetail.timeLimit || 2000 }} ms · 内存限制 {{ problemDetail.memoryLimit || 256000 }} KB</p>
        <div class="prose-readable mt-3 max-h-32 overflow-auto whitespace-pre-line rounded-lg bg-slate-50 p-3">{{ problemDetail.description || '请选择题目后查看描述' }}</div>
        <textarea
          v-model="form.code"
          class="mt-4 h-[430px] w-full resize-y rounded-lg border border-line bg-slate-950 p-4 font-mono text-sm leading-6 text-slate-100 outline-none transition focus:border-sky-400"
          spellcheck="false"
        />
      </AppCard>

      <div class="space-y-4">
        <AppCard>
          <h3 class="text-base font-semibold text-slate-800">评测状态</h3>
          <div v-if="latest.id" class="mt-3 space-y-2 text-sm">
            <div class="rounded-md border border-line px-3 py-2">提交ID：{{ latest.id }}</div>
            <div class="rounded-md border border-line px-3 py-2">状态：<span class="font-semibold">{{ latest.status || '-' }}</span></div>
            <div class="rounded-md border border-line px-3 py-2">耗时：{{ latest.executeTime ?? '-' }} ms</div>
            <div class="rounded-md border border-line px-3 py-2">内存：{{ latest.executeMemory ?? '-' }} KB</div>
            <div class="rounded-md border border-line px-3 py-2 text-rose-600" v-if="latest.errorMessage">{{ latest.errorMessage }}</div>
          </div>
          <EmptyState v-else message="提交后将显示实时判题状态" />
        </AppCard>

        <AppCard>
          <h3 class="text-base font-semibold text-slate-800">最近提交</h3>
          <div class="mt-3 space-y-2 text-sm">
            <div v-for="item in recentSubmissions" :key="item.id" class="rounded-md border border-line px-3 py-2">
              <div class="flex items-center justify-between">
                <span>#{{ item.id }}</span>
                <AppBadge :tone="statusTone(item.status)">{{ item.status || '-' }}</AppBadge>
              </div>
              <div class="mt-1 text-xs text-soft">题目 #{{ item.problemId }} · {{ item.language }}</div>
            </div>
          </div>
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

function statusTone(status) {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'PENDING' || status === 'JUDGING') return 'warn'
  if (status === 'WRONG_ANSWER' || status === 'COMPILE_ERROR' || status === 'RUNTIME_ERROR') return 'danger'
  return 'neutral'
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
  const res = await submissionApi.getSubmissionList({ page: 1, size: 6 })
  recentSubmissions.value = res.data?.records || []
}

function handleLanguageChange() {
  applyTemplateIfNeeded()
}

function handleProblemChange() {
  router.replace({ path: '/studio', query: currentProblemId.value ? { problemId: String(currentProblemId.value) } : {} })
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

async function pollSubmission(id) {
  if (timer) clearInterval(timer)
  timer = setInterval(async () => {
    try {
      const res = await submissionApi.getSubmissionStatus(id, { silent: true })
      latest.value = res.data || {}
      if (DONE_STATUS.has(latest.value.status)) {
        clearInterval(timer)
        timer = null
        await loadRecentSubmissions()
      }
    } catch (_) {
      clearInterval(timer)
      timer = null
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

watch(
  () => route.query.problemId,
  async (pid) => {
    const n = Number(pid || 0)
    currentProblemId.value = Number.isFinite(n) ? n : 0
    await loadProblemDetail()
    applyTemplateIfNeeded()
  }
)

onMounted(async () => {
  await loadProblemOptions()
  currentProblemId.value = Number(route.query.problemId || 0)
  await Promise.all([loadProblemDetail(), loadRecentSubmissions()])
  applyTemplateIfNeeded()
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>
