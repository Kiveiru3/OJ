<template>
  <section class="space-y-6">
    <AppCard v-if="loading">
      <div class="grid gap-3">
        <div class="skeleton h-10 rounded-lg" />
        <div class="skeleton h-6 rounded-lg" />
        <div class="skeleton h-28 rounded-lg" />
      </div>
    </AppCard>

    <template v-else-if="profile.userId">
      <AppCard class="overflow-hidden bg-[linear-gradient(120deg,#0f172a,#1e293b_45%,#334155)] text-white">
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div class="flex min-w-0 items-center gap-4">
            <UserAvatar :user="profile" size="xl" />
            <div class="min-w-0">
              <div class="text-xs uppercase tracking-[0.26em] text-slate-300">User Home</div>
              <h1 class="mt-2 truncate text-3xl font-semibold">{{ displayName }}</h1>
              <p class="mt-1 text-sm text-slate-200">@{{ profile.username || '-' }} · {{ roleText }}</p>
              <p class="mt-3 max-w-3xl text-sm text-slate-100">{{ profileBio }}</p>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <RouterLink v-if="isSelf" to="/profile">
              <AppButton size="sm" variant="secondary">个人设置</AppButton>
            </RouterLink>
            <RouterLink v-else :to="`/messages?peerUserId=${profile.userId}`">
              <AppButton size="sm" variant="secondary">发私信</AppButton>
            </RouterLink>
          </div>
        </div>
      </AppCard>

      <p v-if="errorMsg" class="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
        {{ errorMsg }}
      </p>

      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <AppCard>
          <div class="text-xs text-soft">总提交</div>
          <div class="mt-2 text-2xl font-bold text-slate-900">{{ profile.totalSubmissions ?? 0 }}</div>
        </AppCard>
        <AppCard>
          <div class="text-xs text-soft">总通过</div>
          <div class="mt-2 text-2xl font-bold text-emerald-600">{{ profile.acceptedSubmissions ?? 0 }}</div>
        </AppCard>
        <AppCard>
          <div class="text-xs text-soft">尝试题目</div>
          <div class="mt-2 text-2xl font-bold text-slate-900">{{ profile.attemptedProblems ?? 0 }}</div>
        </AppCard>
        <AppCard>
          <div class="text-xs text-soft">已通过题目</div>
          <div class="mt-2 text-2xl font-bold text-sky-600">{{ profile.solvedProblems ?? 0 }}</div>
        </AppCard>
        <AppCard>
          <div class="text-xs text-soft">通过率</div>
          <div class="mt-2 text-2xl font-bold text-slate-900">{{ formatRate(profile.acceptanceRate) }}</div>
        </AppCard>
      </div>

      <AppCard>
        <div class="flex flex-wrap items-center gap-2 border-b border-line pb-3">
          <button
            type="button"
            class="rounded-full px-3 py-1.5 text-sm transition"
            :class="activeTab === 'overview' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'"
            @click="activeTab = 'overview'"
          >
            主页
          </button>
          <button
            type="button"
            class="rounded-full px-3 py-1.5 text-sm transition"
            :class="activeTab === 'submissions' ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'"
            @click="activeTab = 'submissions'"
          >
            提交
          </button>
        </div>

        <div v-if="activeTab === 'overview'" class="mt-4 space-y-4">
          <section class="rounded-xl border border-line bg-white p-4">
            <div class="mb-3 flex items-center justify-between gap-2">
              <h2 class="text-sm font-semibold text-slate-800">做题热力图</h2>
              <span class="text-xs text-soft">近 26 周 · 提交 {{ heatmapTotalCount }} 次</span>
            </div>
            <div class="overflow-x-auto">
              <div class="inline-flex gap-1">
                <div v-for="(week, weekIndex) in heatmapWeeks" :key="`week-${weekIndex}`" class="grid grid-rows-7 gap-1">
                  <div
                    v-for="(cell, dayIndex) in week"
                    :key="`cell-${weekIndex}-${dayIndex}`"
                    class="h-3.5 w-3.5 rounded-sm border border-white/40"
                    :class="heatmapCellClass(cell)"
                    :title="`${cell.date}：提交 ${cell.count} 次`"
                  />
                </div>
              </div>
            </div>
            <div class="mt-3 flex items-center justify-end gap-2 text-xs text-soft">
              <span>少</span>
              <span v-for="level in [0, 1, 2, 3, 4]" :key="`legend-${level}`" class="h-3.5 w-3.5 rounded-sm border border-white/40" :class="legendCellClass(level)" />
              <span>多</span>
            </div>
          </section>

          <section class="rounded-xl border border-line bg-white p-4">
            <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
              <div>
                <h2 class="text-sm font-semibold text-slate-800">做题概览</h2>
                <p class="mt-1 text-xs text-soft">展示最近有提交记录的题目状态和尝试情况。</p>
              </div>
              <span class="text-xs text-soft">按最近提交排序</span>
            </div>
            <div v-if="problemProgress.length" class="max-h-[420px] space-y-2 overflow-y-auto pr-1 text-sm">
              <article
                v-for="item in problemProgress.slice(0, 10)"
                :key="`overview-progress-${item.problemId}`"
                class="rounded-lg border border-line px-3 py-2"
              >
                <div class="flex items-center justify-between gap-3">
                  <button type="button" class="truncate text-left font-medium text-slate-800 hover:text-sky-700" @click="openProblem(item.problemId)">
                    #{{ item.problemId }} {{ item.problemTitle || '' }}
                  </button>
                  <AppBadge :tone="item.passed ? 'success' : 'warn'">{{ item.passed ? '已通过' : '尝试过' }}</AppBadge>
                </div>
                <div class="mt-1 text-xs text-soft">尝试 {{ item.submitCount || 0 }} 次 · 通过 {{ item.acceptedCount || 0 }} 次</div>
              </article>
            </div>
            <EmptyState v-else message="暂无做题记录" />
          </section>
        </div>

        <div v-else class="mt-4 space-y-4">
          <section class="rounded-xl border border-line bg-white p-4">
            <h2 class="text-sm font-semibold text-slate-800">题目状态（尝试过 / 已通过）</h2>
            <div v-if="problemProgress.length" class="mt-3 overflow-x-auto">
              <table class="min-w-full text-sm">
                <thead>
                  <tr class="border-b border-line text-left text-soft">
                    <th class="px-3 py-2 font-medium">题目</th>
                    <th class="px-3 py-2 font-medium">状态</th>
                    <th class="px-3 py-2 font-medium">尝试次数</th>
                    <th class="px-3 py-2 font-medium">最近提交</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in problemProgress" :key="`progress-${item.problemId}`" class="border-b border-line/70 hover:bg-slate-50">
                    <td class="px-3 py-2">
                      <button type="button" class="text-left text-slate-700 hover:text-sky-700" @click="openProblem(item.problemId)">
                        #{{ item.problemId }} {{ item.problemTitle || '' }}
                      </button>
                    </td>
                    <td class="px-3 py-2">
                      <AppBadge :tone="item.passed ? 'success' : 'warn'">{{ item.passed ? '已通过' : '尝试过' }}</AppBadge>
                    </td>
                    <td class="px-3 py-2 text-slate-700">{{ item.submitCount || 0 }}</td>
                    <td class="px-3 py-2 text-slate-700">{{ formatDate(item.lastSubmitTime) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <EmptyState v-else message="暂无提交记录" />
          </section>

          <section class="rounded-xl border border-line bg-white p-4">
            <h2 class="text-sm font-semibold text-slate-800">最近提交</h2>
            <div v-if="recentSubmissions.length" class="mt-3 max-h-[420px] space-y-2 overflow-y-auto pr-1 text-sm">
              <article v-for="item in recentSubmissions" :key="`recent-${item.id}`" class="rounded-lg border border-line px-3 py-2">
                <div class="flex items-center justify-between gap-3">
                  <button type="button" class="truncate text-left font-medium text-slate-800 hover:text-sky-700" @click="openProblem(item.problemId)">
                    #{{ item.problemId }} {{ item.problemTitle || '' }}
                  </button>
                  <AppBadge :tone="statusTone(item.status)">{{ statusText(item.status) }}</AppBadge>
                </div>
                <div class="mt-1 flex flex-wrap items-center gap-3 text-xs text-soft">
                  <span>提交 ID #{{ item.id }}</span>
                  <span>{{ item.language || '-' }}</span>
                  <span>{{ formatDate(item.submitTime) }}</span>
                </div>
              </article>
            </div>
            <EmptyState v-else message="暂无最近提交" />
          </section>
        </div>
      </AppCard>
    </template>

    <EmptyState v-else message="用户不存在或无权限查看" />
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { userApi } from '@/api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useUserStore } from '@/stores/useUserStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const errorMsg = ref('')
const profile = ref({})
const activeTab = ref('overview')
const HEATMAP_WEEKS = 26
const HEATMAP_DAYS = HEATMAP_WEEKS * 7

const targetUserId = computed(() => Number(route.params.id || 0))
const isSelf = computed(() => Number(userStore.userInfo?.id || 0) === Number(profile.value?.userId || 0))
const displayName = computed(() => profile.value?.nickname || profile.value?.username || `用户#${profile.value?.userId || ''}`)
const profileBio = computed(() => profile.value?.roleProfile?.bio || '这个人很神秘，暂时还没有填写个人简介。')

const roleMap = {
  ADMIN: '管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}

const roleText = computed(() => roleMap[profile.value?.role] || profile.value?.role || '-')
const problemProgress = computed(() => profile.value?.problemProgress || [])
const recentSubmissions = computed(() => profile.value?.recentSubmissions || [])
const dailySubmissionActivity = computed(() => profile.value?.dailySubmissionActivity || [])

const activityCountMap = computed(() => {
  const map = new Map()
  for (const item of dailySubmissionActivity.value) {
    if (!item?.date) continue
    map.set(String(item.date), Number(item.count || 0))
  }
  return map
})

const heatmapWeeks = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const start = new Date(today)
  start.setDate(today.getDate() - HEATMAP_DAYS + 1)

  const weeks = []
  for (let week = 0; week < HEATMAP_WEEKS; week += 1) {
    const oneWeek = []
    for (let day = 0; day < 7; day += 1) {
      const current = new Date(start)
      current.setDate(start.getDate() + week * 7 + day)
      const dateKey = dateToKey(current)
      const count = activityCountMap.value.get(dateKey) || 0
      oneWeek.push({
        date: dateKey,
        count,
        level: heatmapLevel(count),
        isFuture: current.getTime() > today.getTime()
      })
    }
    weeks.push(oneWeek)
  }
  return weeks
})

const heatmapTotalCount = computed(() => {
  let total = 0
  for (const week of heatmapWeeks.value) {
    for (const cell of week) {
      if (!cell.isFuture) {
        total += Number(cell.count || 0)
      }
    }
  }
  return total
})

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

function formatRate(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '0.00%'
  return `${Number(value).toFixed(2)}%`
}

function dateToKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function heatmapLevel(count) {
  const value = Number(count || 0)
  if (value <= 0) return 0
  if (value <= 1) return 1
  if (value <= 3) return 2
  if (value <= 6) return 3
  return 4
}

function heatmapCellClass(cell) {
  if (cell.isFuture) return 'bg-slate-100'
  return legendCellClass(cell.level)
}

function legendCellClass(level) {
  const map = {
    0: 'bg-slate-100',
    1: 'bg-emerald-100',
    2: 'bg-emerald-300',
    3: 'bg-emerald-500',
    4: 'bg-emerald-700'
  }
  return map[level] || map[0]
}

function openProblem(problemId) {
  if (!problemId) return
  router.push({ path: '/studio', query: { problemId: String(problemId) } })
}

async function loadProfile() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await userApi.getPublicProfile(targetUserId.value)
    profile.value = res.data || {}
  } catch (e) {
    profile.value = {}
    errorMsg.value = e.message || '加载用户资料失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)

watch(targetUserId, () => {
  activeTab.value = 'overview'
  loadProfile()
})
</script>
