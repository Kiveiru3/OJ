<template>
  <section class="space-y-6">
    <AppCard padding="lg" class="overflow-hidden bg-gradient-to-br from-slate-900 to-slate-700 text-white">
      <div class="grid gap-6 md:grid-cols-[1.2fr_0.8fr]">
        <div class="space-y-4">
          <AppBadge tone="info">实时数据驱动</AppBadge>
          <h1 class="text-3xl font-bold leading-tight md:text-4xl">在线评测教学平台<br>一体化前端中枢</h1>
          <p class="max-w-2xl text-sm text-slate-200 md:text-base">当前页面数据由后端实时接口返回，已接入登录鉴权、题库、竞赛、讨论和提交链路。</p>
          <div class="flex flex-wrap gap-3">
            <RouterLink to="/problems"><AppButton>进入题库中心</AppButton></RouterLink>
            <RouterLink to="/studio"><AppButton variant="secondary">前往代码工坊</AppButton></RouterLink>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3 self-end">
          <div class="rounded-lg bg-white/10 p-4"><div class="text-xs text-slate-300">题库总量</div><div class="mt-1 text-2xl font-semibold">{{ dashboard.problemTotal }}</div></div>
          <div class="rounded-lg bg-white/10 p-4"><div class="text-xs text-slate-300">竞赛总场次</div><div class="mt-1 text-2xl font-semibold">{{ dashboard.contestTotal }}</div></div>
          <div class="rounded-lg bg-white/10 p-4"><div class="text-xs text-slate-300">讨论帖总量</div><div class="mt-1 text-2xl font-semibold">{{ dashboard.discussionTotal }}</div></div>
          <div class="rounded-lg bg-white/10 p-4"><div class="text-xs text-slate-300">我的提交</div><div class="mt-1 text-2xl font-semibold">{{ dashboard.submissionTotal }}</div></div>
        </div>
      </div>
    </AppCard>

    <div class="grid gap-4 md:grid-cols-3">
      <AppCard>
        <div class="text-sm text-soft">当前身份</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">{{ roleText }}</div>
        <div class="mt-1 text-sm text-soft">{{ userName }}</div>
      </AppCard>
      <AppCard>
        <div class="text-sm text-soft">系统公告</div>
        <div class="mt-2 text-lg font-semibold text-slate-900">{{ app.announcement }}</div>
      </AppCard>
      <AppCard>
        <div class="text-sm text-soft">登录状态</div>
        <div class="mt-2 text-2xl font-bold text-emerald-600">在线</div>
        <div class="mt-1 text-sm text-soft">Token 已加载</div>
      </AppCard>
    </div>

    <AppCard v-if="isAdmin">
      <h2 class="section-title">接口健康检查</h2>
      <p class="section-subtitle">管理员可见：用于快速定位关键接口异常</p>
      <div class="mt-4 grid gap-3 md:grid-cols-2">
        <div v-for="item in checks" :key="item.name" class="rounded-lg border border-line p-4">
          <div class="flex items-center justify-between">
            <div class="text-sm font-semibold text-slate-800">{{ item.name }}</div>
            <AppBadge :tone="item.ok ? 'success' : 'danger'">{{ item.ok ? 'OK' : 'FAIL' }}</AppBadge>
          </div>
          <div class="mt-1 text-xs text-soft">{{ item.detail }}</div>
        </div>
      </div>
    </AppCard>

    <AppCard v-else>
      <h2 class="section-title">个人进度看板</h2>
      <p class="section-subtitle">学生/教师可见：更聚焦学习与训练进度</p>

      <div class="mt-4 grid gap-3 md:grid-cols-4">
        <div class="rounded-lg border border-line p-4">
          <div class="text-xs text-soft">今日提交</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ personalBoard.todaySubmissionCount }}</div>
        </div>
        <div class="rounded-lg border border-line p-4">
          <div class="text-xs text-soft">近期通过</div>
          <div class="mt-1 text-2xl font-semibold text-emerald-600">{{ personalBoard.acceptedCount }}</div>
        </div>
        <div class="rounded-lg border border-line p-4">
          <div class="text-xs text-soft">即将开始竞赛</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ personalBoard.upcomingContestCount }}</div>
        </div>
        <div class="rounded-lg border border-line p-4">
          <div class="text-xs text-soft">最近状态</div>
          <div class="mt-2">
            <AppBadge :tone="personalBoard.latestStatusTone">{{ statusText(personalBoard.latestStatus) }}</AppBadge>
          </div>
        </div>
      </div>

      <div class="mt-4 grid gap-4 md:grid-cols-2">
        <div class="rounded-lg border border-line p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-800">最近提交</h3>
            <RouterLink to="/studio" class="text-xs text-slate-600 underline">去代码工坊</RouterLink>
          </div>
          <div v-if="recentSubmissionList.length" class="space-y-2">
            <div v-for="item in recentSubmissionList" :key="item.id" class="rounded-md bg-slate-50 p-3">
              <div class="flex items-center justify-between text-sm">
                <span class="font-medium text-slate-800">#{{ item.id }} · 题目 #{{ item.problemId }}</span>
                <AppBadge :tone="statusTone(item.status)">{{ statusText(item.status) }}</AppBadge>
              </div>
              <div class="mt-1 text-xs text-soft">{{ formatDateTime(item.submitTime) }}</div>
            </div>
          </div>
          <div v-else class="text-sm text-soft">暂无提交记录</div>
        </div>

        <div class="rounded-lg border border-line p-4">
          <div class="mb-3 flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-800">即将开始竞赛</h3>
            <RouterLink to="/contests" class="text-xs text-slate-600 underline">去赛事中枢</RouterLink>
          </div>
          <div v-if="upcomingContestList.length" class="space-y-2">
            <div v-for="item in upcomingContestList" :key="item.id" class="rounded-md bg-slate-50 p-3">
              <div class="text-sm font-medium text-slate-800">{{ item.title }}</div>
              <div class="mt-1 text-xs text-soft">开始时间：{{ formatDateTime(item.startTime) }}</div>
            </div>
          </div>
          <div v-else class="text-sm text-soft">暂无即将开始的竞赛</div>
        </div>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import { contestApi, discussionApi, problemApi, submissionApi } from '@/api'
import { useAppStore } from '@/stores/useAppStore'
import { useUserStore } from '@/stores/useUserStore'

const app = useAppStore()
const userStore = useUserStore()

const dashboard = reactive({
  problemTotal: 0,
  contestTotal: 0,
  discussionTotal: 0,
  submissionTotal: 0
})

const checks = ref([
  { name: '题库接口 /problem/list', ok: false, detail: '未检查' },
  { name: '竞赛接口 /contest/list', ok: false, detail: '未检查' },
  { name: '讨论接口 /discussion/list', ok: false, detail: '未检查' },
  { name: '提交接口 /submission/list', ok: false, detail: '未检查' }
])

const personalBoard = reactive({
  todaySubmissionCount: 0,
  acceptedCount: 0,
  upcomingContestCount: 0,
  latestStatus: '-',
  latestStatusTone: 'neutral'
})

const recentSubmissionList = ref([])
const upcomingContestList = ref([])

const roleMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const roleText = computed(() => roleMap[userStore.userInfo?.role] || userStore.userInfo?.role || '未知')
const userName = computed(() => userStore.userInfo?.nickname || userStore.userInfo?.username || '未命名用户')

function toDate(value) {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
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

function buildPersonalBoard(contestPage, submissionPage) {
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate())

  const submissionRecords = Array.isArray(submissionPage?.records) ? submissionPage.records : []
  recentSubmissionList.value = submissionRecords.slice(0, 6)
  personalBoard.acceptedCount = submissionRecords.filter((item) => item.status === 'ACCEPTED').length
  personalBoard.todaySubmissionCount = submissionRecords.filter((item) => {
    const dt = toDate(item.submitTime)
    return dt && dt >= startOfToday
  }).length
  personalBoard.latestStatus = submissionRecords[0]?.status || '-'
  personalBoard.latestStatusTone = statusTone(personalBoard.latestStatus)

  const contestRecords = Array.isArray(contestPage?.records) ? contestPage.records : []
  const upcoming = contestRecords
    .filter((item) => {
      const dt = toDate(item.startTime)
      return dt && dt > now
    })
    .sort((a, b) => toDate(a.startTime) - toDate(b.startTime))

  personalBoard.upcomingContestCount = upcoming.length
  upcomingContestList.value = upcoming.slice(0, 5)
}

async function loadDashboard() {
  const reqs = [
    problemApi.getProblemList({ page: 1, size: 1 }),
    contestApi.getContestList({ page: 1, size: 20 }),
    discussionApi.getPostList({ page: 1, size: 1 }),
    submissionApi.getSubmissionList({ page: 1, size: 20 })
  ]

  const names = ['题库接口 /problem/list', '竞赛接口 /contest/list', '讨论接口 /discussion/list', '提交接口 /submission/list']
  const results = await Promise.allSettled(reqs)

  let contestPage = null
  let submissionPage = null

  results.forEach((r, idx) => {
    if (r.status === 'fulfilled') {
      const pageData = r.value?.data || {}
      const total = Number(pageData?.total || 0)
      checks.value[idx] = { name: names[idx], ok: true, detail: `total=${total}` }
      if (idx === 0) dashboard.problemTotal = total
      if (idx === 1) {
        dashboard.contestTotal = total
        contestPage = pageData
      }
      if (idx === 2) dashboard.discussionTotal = total
      if (idx === 3) {
        dashboard.submissionTotal = total
        submissionPage = pageData
      }
    } else {
      checks.value[idx] = { name: names[idx], ok: false, detail: r.reason?.message || '请求失败' }
    }
  })

  buildPersonalBoard(contestPage, submissionPage)
}

onMounted(async () => {
  await userStore.ensureUserInfo().catch(() => null)
  await loadDashboard()
})
</script>
