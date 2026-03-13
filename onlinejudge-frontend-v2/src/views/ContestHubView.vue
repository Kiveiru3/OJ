<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">赛事中枢</h1>
      <p class="section-subtitle">实时读取竞赛数据，支持报名并查看简版榜单。</p>
    </header>

    <div class="grid gap-4 md:grid-cols-[1fr_160px_120px]">
      <input v-model.trim="keyword" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="搜索竞赛标题" @keyup.enter="search" />
      <AppButton @click="search">搜索</AppButton>
      <AppButton variant="secondary" @click="reset">重置</AppButton>
    </div>

    <div class="grid gap-4 lg:grid-cols-3">
      <AppCard v-for="event in contests" :key="event.id" class="group">
        <div class="flex items-center justify-between">
          <AppBadge :tone="statusTone(event.contestStatus)">{{ statusText(event.contestStatus) }}</AppBadge>
          <span class="text-xs text-soft">#{{ event.id }}</span>
        </div>
        <h3 class="mt-3 text-lg font-semibold text-slate-800">{{ event.title }}</h3>
        <p class="mt-1 text-sm text-soft">{{ event.description || '暂无描述' }}</p>
        <div class="mt-3 text-xs text-soft">{{ formatRange(event.startTime, event.endTime) }}</div>
        <div class="mt-3 rounded-lg bg-slate-100 p-3 text-sm">
          <div>题目数：{{ event.problemCount || 0 }}</div>
          <div>参赛：{{ event.participantCount || 0 }}</div>
          <div>罚时：{{ event.penaltyPerWrong ?? 20 }} 分钟</div>
        </div>
        <div class="mt-4 flex gap-2">
          <AppButton size="sm" @click="showRanking(event.id)">查看榜单</AppButton>
          <AppButton size="sm" variant="secondary" :disabled="event.joined || joining" @click="join(event.id)">
            {{ event.joined ? '已报名' : '报名' }}
          </AppButton>
        </div>
      </AppCard>
    </div>

    <EmptyState v-if="!loading && !contests.length" message="暂无竞赛数据" />

    <div class="flex items-center justify-between">
      <p class="text-sm text-soft">共 {{ total }} 场 · 当前第 {{ page }} 页</p>
      <div class="flex gap-2">
        <AppButton variant="secondary" size="sm" :disabled="page <= 1 || loading" @click="prevPage">上一页</AppButton>
        <AppButton size="sm" :disabled="loading || page * size >= total" @click="nextPage">下一页</AppButton>
      </div>
    </div>

    <AppCard v-if="ranking.length">
      <h2 class="text-lg font-semibold text-slate-800">榜单预览（前 10）</h2>
      <div class="mt-3 overflow-x-auto">
        <table class="min-w-full text-left text-sm">
          <thead>
            <tr class="border-b border-line text-soft">
              <th class="px-2 py-2">排名</th>
              <th class="px-2 py-2">用户</th>
              <th class="px-2 py-2">解题</th>
              <th class="px-2 py-2">罚时</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in ranking" :key="item.userId" class="border-b border-line/60">
              <td class="px-2 py-2">{{ item.rank || '-' }}</td>
              <td class="px-2 py-2">{{ item.nickname || item.username || `#${item.userId}` }}</td>
              <td class="px-2 py-2">{{ item.acceptedCount ?? 0 }}</td>
              <td class="px-2 py-2">{{ item.totalPenalty ?? 0 }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { contestApi } from '@/api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const loading = ref(false)
const joining = ref(false)
const contests = ref([])
const ranking = ref([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const size = ref(9)

function statusText(v) {
  if (v === 'RUNNING') return '进行中'
  if (v === 'UPCOMING') return '未开始'
  if (v === 'ENDED') return '已结束'
  return v || '未知'
}

function statusTone(v) {
  if (v === 'RUNNING') return 'success'
  if (v === 'UPCOMING') return 'warn'
  if (v === 'ENDED') return 'neutral'
  return 'info'
}

function formatRange(start, end) {
  const s = start ? String(start).replace('T', ' ').slice(0, 19) : '-'
  const e = end ? String(end).replace('T', ' ').slice(0, 19) : '-'
  return `${s} ~ ${e}`
}

async function load() {
  loading.value = true
  try {
    const res = await contestApi.getContestList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    contests.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function join(id) {
  joining.value = true
  try {
    await contestApi.joinContest(id)
    await load()
  } finally {
    joining.value = false
  }
}

async function showRanking(id) {
  const res = await contestApi.getContestRanking(id, { page: 1, size: 10 })
  ranking.value = res.data?.records || []
}

function search() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
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
