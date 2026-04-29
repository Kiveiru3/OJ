<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">题库中心</h1>
      <p class="section-subtitle">按卡片流展示题目，并支持搜索与难度筛选。</p>
    </header>

    <AppCard>
      <div class="grid gap-3 md:grid-cols-[1fr_180px_140px]">
        <input v-model.trim="query.keyword" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="搜索题目标题" @keyup.enter="search" />
        <select v-model="query.difficulty" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
          <option value="">全部难度</option>
          <option value="EASY">简单</option>
          <option value="MEDIUM">中等</option>
          <option value="HARD">困难</option>
        </select>
        <div class="flex gap-2">
          <AppButton block @click="search">搜索</AppButton>
          <AppButton variant="secondary" block @click="reset">重置</AppButton>
        </div>
      </div>
    </AppCard>

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <AppCard v-for="item in problems" :key="item.id" class="group">
        <div class="flex items-start justify-between gap-3">
          <div class="flex items-start gap-2">
            <span class="mt-1 inline-flex h-5 w-5 items-center justify-center rounded-full bg-emerald-100 text-xs font-bold text-emerald-700" v-if="isSolved(item)">✓</span>
            <span class="mt-1 inline-flex h-5 w-5 rounded-full border border-slate-300" v-else />
            <div>
              <h3 class="text-[15px] font-semibold text-slate-800">#{{ item.id }} {{ item.title }}</h3>
              <p class="mt-1 text-xs text-soft">提交 {{ item.submitCount || 0 }} · 通过 {{ item.acceptCount || 0 }}</p>
            </div>
          </div>
          <AppBadge :tone="difficultyTone(item.difficulty)">{{ difficultyText(item.difficulty) }}</AppBadge>
        </div>
        <div class="mt-4 flex gap-2">
          <AppButton size="sm" @click="startSolve(item.id)">去做题</AppButton>
          <AppButton size="sm" variant="secondary" @click="preview(item)">预览题面</AppButton>
        </div>
      </AppCard>
    </div>

    <EmptyState v-if="!loading && !problems.length" message="当前筛选条件下没有题目" />

    <div class="flex items-center justify-between">
      <p class="text-sm text-soft">共 {{ total }} 题 · 当前第 {{ page }} 页</p>
      <div class="flex gap-2">
        <AppButton variant="secondary" size="sm" :disabled="page <= 1 || loading" @click="prevPage">上一页</AppButton>
        <AppButton size="sm" :disabled="loading || page * size >= total" @click="nextPage">下一页</AppButton>
      </div>
    </div>

    <AppCard v-if="focused">
      <h2 class="text-lg font-semibold text-slate-800">题面预览：#{{ focused.id }} {{ focused.title }}</h2>
      <div v-if="previewLoading" class="mt-3 grid gap-2">
        <div v-for="n in 4" :key="`preview-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>
      <div v-else class="mt-3 space-y-3">
        <section class="rounded-xl border border-line bg-slate-50/60 p-4">
          <h3 class="text-sm font-semibold text-slate-800">题目描述</h3>
          <ProblemRichContent class="mt-2" :content="focused.description || '暂无描述'" />
        </section>
        <section v-if="focused.inputFormat" class="rounded-xl border border-line bg-white p-4">
          <h3 class="text-sm font-semibold text-slate-800">输入格式</h3>
          <ProblemRichContent class="mt-2" :content="focused.inputFormat" />
        </section>
        <section v-if="focused.outputFormat" class="rounded-xl border border-line bg-white p-4">
          <h3 class="text-sm font-semibold text-slate-800">输出格式</h3>
          <ProblemRichContent class="mt-2" :content="focused.outputFormat" />
        </section>
        <section v-if="focused.sampleInput || focused.sampleOutput" class="rounded-xl border border-line bg-white p-4">
          <h3 class="text-sm font-semibold text-slate-800">样例</h3>
          <div class="mt-2 grid gap-3 md:grid-cols-2">
            <div class="rounded-lg border border-line bg-slate-900 p-3">
              <div class="mb-2 text-xs text-slate-300">输入</div>
              <pre class="max-h-52 overflow-auto whitespace-pre-wrap text-xs leading-6 text-slate-100">{{ focused.sampleInput || '-' }}</pre>
            </div>
            <div class="rounded-lg border border-line bg-slate-900 p-3">
              <div class="mb-2 text-xs text-slate-300">输出</div>
              <pre class="max-h-52 overflow-auto whitespace-pre-wrap text-xs leading-6 text-slate-100">{{ focused.sampleOutput || '-' }}</pre>
            </div>
          </div>
        </section>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { problemApi } from '@/api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ProblemRichContent from '@/components/problem/ProblemRichContent.vue'
import { useUserStore } from '@/stores/useUserStore'
import { requireLoginAction } from '@/utils/authAction'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const problems = ref([])
const focused = ref(null)
const previewLoading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(12)

const query = reactive({
  keyword: '',
  difficulty: ''
})

function difficultyText(v) {
  return v === 'EASY' ? '简单' : v === 'MEDIUM' ? '中等' : v === 'HARD' ? '困难' : v || '未知'
}

function difficultyTone(v) {
  return v === 'EASY' ? 'success' : v === 'MEDIUM' ? 'warn' : v === 'HARD' ? 'danger' : 'neutral'
}

function isSolved(item) {
  return item?.solved === true || item?.isSolved === true || item?.userSolved === true || item?.userStatus === 'ACCEPTED'
}

async function startSolve(problemId) {
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: route.fullPath || '/problems',
    actionText: '开始做题'
  })
  if (!ok) return
  router.push({ path: '/studio', query: { problemId: String(problemId) } })
}

async function load() {
  loading.value = true
  try {
    const res = await problemApi.getProblemList({
      page: page.value,
      size: size.value,
      keyword: query.keyword || undefined,
      difficulty: query.difficulty || undefined
    })
    problems.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function preview(item) {
  focused.value = item
  previewLoading.value = true
  try {
    const res = await problemApi.getProblemDetail(item.id)
    focused.value = res.data || item
  } finally {
    previewLoading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  query.keyword = ''
  query.difficulty = ''
  page.value = 1
  load()
}

function prevPage() {
  if (page.value <= 1) return
  page.value -= 1
  load()
}

function nextPage() {
  page.value += 1
  load()
}

onMounted(load)
</script>
