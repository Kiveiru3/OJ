<template>
  <section class="space-y-6">
    <AppCard padding="lg" class="overflow-hidden border-slate-200 bg-[#f7f8fa] text-slate-900">
      <div class="grid gap-6 md:grid-cols-[1.2fr_0.8fr]">
        <div class="space-y-4">
          <AppBadge tone="neutral">在线评测教学平台</AppBadge>
          <h1 class="text-3xl font-bold leading-tight text-slate-900 md:text-4xl">题库练习、竞赛组织、自动评测一体化</h1>
          <div class="flex flex-wrap gap-3">
            <RouterLink to="/problems"><AppButton>浏览题库</AppButton></RouterLink>
            <RouterLink to="/contests"><AppButton variant="secondary">查看竞赛</AppButton></RouterLink>
            <RouterLink v-if="!userStore.isLoggedIn" to="/login"><AppButton variant="ghost">登录后开始做题</AppButton></RouterLink>
            <RouterLink v-else to="/studio"><AppButton variant="ghost">进入做题工作台</AppButton></RouterLink>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3 self-end">
          <div class="rounded-lg border border-line bg-white p-4">
            <div class="text-xs text-soft">题目总量</div>
            <div class="mt-1 text-2xl font-semibold text-slate-900">{{ dashboard.problemTotal }}</div>
          </div>
          <div class="rounded-lg border border-line bg-white p-4">
            <div class="text-xs text-soft">竞赛场次</div>
            <div class="mt-1 text-2xl font-semibold text-slate-900">{{ dashboard.contestTotal }}</div>
          </div>
          <div class="rounded-lg border border-line bg-white p-4">
            <div class="text-xs text-soft">讨论帖子</div>
            <div class="mt-1 text-2xl font-semibold text-slate-900">{{ dashboard.discussionTotal }}</div>
          </div>
          <div class="rounded-lg border border-line bg-white p-4">
            <div class="text-xs text-soft">我的提交</div>
            <div class="mt-1 text-2xl font-semibold text-slate-900">{{ userStore.isLoggedIn ? dashboard.submissionTotal : '-' }}</div>
          </div>
        </div>
      </div>
    </AppCard>

    <div class="grid gap-4 md:grid-cols-1">
      <AppCard>
        <div class="text-sm text-soft">当前身份</div>
        <div class="mt-2 text-2xl font-bold text-slate-900">{{ roleText }}</div>
      </AppCard>
    </div>

    <div class="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
      <AppCard>
        <div class="flex items-center justify-between">
          <h2 class="section-title">积分排名 Top 20</h2>
          <AppButton size="sm" variant="secondary" :disabled="rankingLoading" @click="loadPointRanking">刷新</AppButton>
        </div>
        <div v-if="rankingLoading" class="mt-3 grid gap-2">
          <div v-for="n in 8" :key="`ranking-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
        </div>
        <div v-else class="mt-3 overflow-x-auto">
          <table class="min-w-full text-left text-sm">
            <thead>
              <tr class="border-b border-line text-soft">
                <th class="px-2 py-2">排名</th>
                <th class="px-2 py-2">用户</th>
                <th class="px-2 py-2">已解题</th>
                <th class="px-2 py-2">积分</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in pointRanking" :key="item.userId" class="border-b border-line/70">
                <td class="px-2 py-2">{{ item.rank }}</td>
                <td class="px-2 py-2"><UserIdentity :user="item" avatar-size="xs" /></td>
                <td class="px-2 py-2">{{ item.solvedCount || 0 }}</td>
                <td class="px-2 py-2 font-semibold text-slate-900">{{ item.points || 0 }}</td>
              </tr>
              <tr v-if="!pointRanking.length">
                <td colspan="4" class="px-2 py-6 text-center text-soft">暂时还没有积分数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </AppCard>

      <AppCard>
        <h2 class="section-title">我的学习看板</h2>
        <p class="section-subtitle">登录后自动显示你的积分与最近提交状态。</p>

        <div v-if="!userStore.isLoggedIn" class="mt-4 rounded-lg border border-dashed border-line p-4 text-sm text-soft">
          你当前是访客，点击右上角登录后可查看个人积分、提交趋势与参赛记录。
        </div>

        <div v-else class="mt-4 space-y-3">
          <div class="grid gap-3 sm:grid-cols-2">
            <div class="rounded-lg border border-line p-4">
              <div class="text-xs text-soft">我的积分</div>
              <div class="mt-1 text-2xl font-semibold text-slate-900">{{ myPoint.points || 0 }}</div>
            </div>
            <div class="rounded-lg border border-line p-4">
              <div class="text-xs text-soft">积分排名</div>
              <div class="mt-1 text-2xl font-semibold text-slate-900">#{{ myPoint.rank || '-' }}</div>
            </div>
            <div class="rounded-lg border border-line p-4">
              <div class="text-xs text-soft">已解题目</div>
              <div class="mt-1 text-2xl font-semibold text-emerald-600">{{ myPoint.solvedCount || 0 }}</div>
            </div>
            <div class="rounded-lg border border-line p-4">
              <div class="text-xs text-soft">最近状态</div>
              <div class="mt-2">
                <AppBadge :tone="statusTone(personalBoard.latestStatus)">{{ statusText(personalBoard.latestStatus) }}</AppBadge>
              </div>
            </div>
          </div>

          <div class="rounded-lg border border-line p-4">
            <div class="mb-3 flex items-center justify-between">
              <h3 class="text-sm font-semibold text-slate-800">最近提交</h3>
              <RouterLink to="/studio" class="text-xs text-slate-600 underline">去做题</RouterLink>
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
        </div>
      </AppCard>
    </div>

    <AppCard>
      <div class="flex items-center justify-between gap-3">
        <div>
          <h2 class="section-title">热门讨论推荐</h2>
          <p class="section-subtitle">按点赞热度优先排序，适合快速进入高价值讨论。</p>
        </div>
        <RouterLink to="/discuss"><AppButton size="sm" variant="secondary">查看全部讨论</AppButton></RouterLink>
      </div>
      <div class="mt-4 grid gap-3 lg:grid-cols-3">
        <RouterLink
          v-for="item in hotDiscussions"
          :key="item.id"
          :to="`/discuss/${item.id}`"
          class="rounded-[18px] border border-line bg-white p-4 text-inherit no-underline transition hover:border-slate-300"
        >
          <div class="flex items-center justify-between gap-3">
            <AppBadge tone="neutral">讨论 #{{ item.id }}</AppBadge>
            <span class="rounded-full bg-rose-50 px-2.5 py-1 text-[11px] font-medium text-rose-700">点赞 {{ item.likeCount || 0 }}</span>
          </div>
          <div class="mt-3 line-clamp-2 text-base font-semibold text-slate-900">{{ item.title }}</div>
          <div class="mt-2 line-clamp-3 text-sm leading-6 text-soft">{{ item.contentPreview || '欢迎参与讨论' }}</div>
          <div class="mt-4 flex flex-wrap items-center gap-3 text-xs text-soft">
            <UserIdentity :user="item" avatar-size="xs" />
            <span>浏览 {{ item.viewCount || 0 }}</span>
          </div>
        </RouterLink>
        <div
          v-if="!hotDiscussions.length"
          class="rounded-[22px] border border-dashed border-line bg-white/80 p-6 text-sm text-soft lg:col-span-3"
        >
          暂无推荐讨论，发布或点赞帖子后会在这里展示。
        </div>
      </div>
    </AppCard>

    <AppCard v-if="isAdmin">
      <h2 class="section-title">系统健康检查（管理员可见）</h2>
      <p class="section-subtitle">用于快速确认关键接口是否正常返回。</p>
      <div class="mt-4 grid gap-3 md:grid-cols-2">
        <div v-for="item in checks" :key="item.name" class="rounded-lg border border-line p-4">
          <div class="flex items-center justify-between">
            <div class="text-sm font-semibold text-slate-800">{{ item.name }}</div>
            <AppBadge :tone="item.ok ? 'success' : 'danger'">{{ item.ok ? '正常' : '异常' }}</AppBadge>
          </div>
          <div class="mt-1 text-xs text-soft">{{ item.detail }}</div>
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
import UserIdentity from '@/components/ui/UserIdentity.vue'
import { contestApi, discussionApi, problemApi, submissionApi } from '@/api'
import { useUserStore } from '@/stores/useUserStore'

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
  latestStatus: '-'
})

const recentSubmissionList = ref([])

const rankingLoading = ref(false)
const pointRanking = ref([])
const hotDiscussions = ref([])
const myPoint = reactive({
  rank: 0,
  solvedCount: 0,
  points: 0
})

const roleMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const roleText = computed(() => {
  if (!userStore.isLoggedIn) return '访客'
  return roleMap[userStore.userInfo?.role] || userStore.userInfo?.role || '未知'
})

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

async function loadPointRanking() {
  rankingLoading.value = true
  try {
    const res = await submissionApi.getPointRanking({ size: 20 })
    pointRanking.value = Array.isArray(res?.data) ? res.data : []
  } finally {
    rankingLoading.value = false
  }
}

async function loadPublicDashboard() {
  const reqs = [
    problemApi.getProblemList({ page: 1, size: 1 }),
    contestApi.getContestList({ page: 1, size: 1 }),
    discussionApi.getPostList({ page: 1, size: 3 })
  ]
  const names = ['题库接口 /problem/list', '竞赛接口 /contest/list', '讨论接口 /discussion/list']
  const results = await Promise.allSettled(reqs)

  results.forEach((r, idx) => {
    if (r.status === 'fulfilled') {
      const total = Number(r.value?.data?.total || 0)
      checks.value[idx] = { name: names[idx], ok: true, detail: `total=${total}` }
      if (idx === 0) dashboard.problemTotal = total
      if (idx === 1) dashboard.contestTotal = total
      if (idx === 2) {
        dashboard.discussionTotal = total
        hotDiscussions.value = Array.isArray(r.value?.data?.records) ? r.value.data.records.slice(0, 3) : []
      }
    } else {
      checks.value[idx] = { name: names[idx], ok: false, detail: r.reason?.message || '请求失败' }
    }
  })
}

async function loadPrivateDashboard() {
  if (!userStore.isLoggedIn) {
    dashboard.submissionTotal = 0
    recentSubmissionList.value = []
    myPoint.rank = 0
    myPoint.solvedCount = 0
    myPoint.points = 0
    personalBoard.latestStatus = '-'
    return
  }

  const submissionReq = submissionApi.getSubmissionList({ page: 1, size: 8 })
  const pointReq = submissionApi.getMyPointSummary()
  const [submissionRes, pointRes] = await Promise.allSettled([submissionReq, pointReq])

  if (submissionRes.status === 'fulfilled') {
    const pageData = submissionRes.value?.data || {}
    dashboard.submissionTotal = Number(pageData.total || 0)
    recentSubmissionList.value = Array.isArray(pageData.records) ? pageData.records.slice(0, 6) : []
    personalBoard.latestStatus = recentSubmissionList.value[0]?.status || '-'
    checks.value[3] = { name: '提交接口 /submission/list', ok: true, detail: `total=${dashboard.submissionTotal}` }
  } else {
    checks.value[3] = { name: '提交接口 /submission/list', ok: false, detail: submissionRes.reason?.message || '请求失败' }
  }

  if (pointRes.status === 'fulfilled') {
    const data = pointRes.value?.data || {}
    myPoint.rank = Number(data.rank || 0)
    myPoint.solvedCount = Number(data.solvedCount || 0)
    myPoint.points = Number(data.points || 0)
  }
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.ensureUserInfo().catch(() => null)
  }
  await Promise.all([loadPublicDashboard(), loadPointRanking()])
  await loadPrivateDashboard()
})
</script>
