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
          <RouterLink :to="`/studio?problemId=${item.id}`"><AppButton size="sm">去做题</AppButton></RouterLink>
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
      <div class="prose-readable mt-3 whitespace-pre-line">{{ focused.description || '暂无描述' }}</div>
    </AppCard>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { problemApi } from '@/api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const loading = ref(false)
const problems = ref([])
const focused = ref(null)
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

function preview(item) {
  focused.value = item
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
