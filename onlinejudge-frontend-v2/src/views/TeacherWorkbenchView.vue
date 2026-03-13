<template>
  <section class="space-y-6">
    <header class="flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 class="section-title">教师工作台</h1>
        <p class="section-subtitle">查看教学数据概览，快速掌握提交趋势与题目活跃度。</p>
      </div>
      <div class="flex items-center gap-2">
        <select v-model.number="days" class="rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-slate-800">
          <option :value="7">最近 7 天</option>
          <option :value="14">最近 14 天</option>
          <option :value="30">最近 30 天</option>
        </select>
        <AppButton size="sm" variant="secondary" :disabled="loading" @click="loadData">刷新</AppButton>
        <AppButton size="sm" :disabled="loading || exporting" @click="exportCsv">{{ exporting ? '导出中...' : '导出 CSV' }}</AppButton>
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
        <div v-else-if="statusItems.length" class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="item in statusItems"
            :key="item.key"
            class="rounded-full border border-line bg-slate-50 px-3 py-1 text-xs text-slate-700"
          >
            {{ item.key }} · {{ item.value }}
          </span>
        </div>
        <div v-else class="mt-4 text-sm text-soft">暂无提交状态数据</div>
      </AppCard>

      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">语言分布</h2>
        <div v-if="loading" class="mt-4 grid gap-2">
          <div v-for="n in 4" :key="`lang-skeleton-${n}`" class="skeleton h-8 rounded-lg" />
        </div>
        <div v-else-if="languageItems.length" class="mt-4 flex flex-wrap gap-2">
          <span
            v-for="item in languageItems"
            :key="item.key"
            class="rounded-full border border-line bg-slate-50 px-3 py-1 text-xs text-slate-700"
          >
            {{ item.key }} · {{ item.value }}
          </span>
        </div>
        <div v-else class="mt-4 text-sm text-soft">暂无语言分布数据</div>
      </AppCard>
    </div>

    <AppCard>
      <h2 class="text-lg font-semibold text-slate-800">每日提交趋势</h2>
      <div v-if="loading" class="mt-4 grid gap-2">
        <div v-for="n in 6" :key="`trend-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>
      <div v-else-if="trendList.length" class="mt-4 overflow-x-auto">
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
      <div v-else class="mt-4 text-sm text-soft">暂无趋势数据</div>
    </AppCard>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { teacherApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'

const loading = ref(false)
const exporting = ref(false)
const errorMsg = ref('')
const days = ref(7)
const overview = ref({})

const statCards = computed(() => [
  { label: '学生总数', value: overview.value.totalStudents ?? 0 },
  { label: '教师总数', value: overview.value.totalTeachers ?? 0 },
  { label: '题库总量', value: overview.value.totalProblems ?? 0 },
  { label: '总提交量', value: overview.value.totalSubmissions ?? 0 },
  { label: '通过率', value: formatRate(overview.value.acceptanceRate) }
])

const statusItems = computed(() => mapToList(overview.value.submissionStatusDistribution))
const languageItems = computed(() => mapToList(overview.value.languageDistribution))
const trendList = computed(() => overview.value.dailySubmissionTrend || [])

function mapToList(map) {
  if (!map || typeof map !== 'object') return []
  return Object.entries(map)
    .map(([key, value]) => ({ key, value }))
    .sort((a, b) => Number(b.value || 0) - Number(a.value || 0))
}

function formatRate(rate) {
  if (rate === null || rate === undefined || Number.isNaN(Number(rate))) return '0.00%'
  return `${Number(rate).toFixed(2)}%`
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
    const csv = res?.data || ''
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const fileName = `teacher-overview-${days.value}d.csv`
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

onMounted(loadData)
</script>
