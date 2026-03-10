<template>
  <div class="submission-list-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><List /></el-icon>
            <div class="title-stack">
              <h2 class="pro-title-text">提交记录</h2>
              <span class="title-sub">当前查询共 {{ pagination.total }} 条</span>
            </div>
          </div>
        </div>
      </template>

      <div v-if="hasFilterBanner" class="filter-banner">
        <div class="filter-banner-title">当前筛选来源：{{ filterSourceLabel }}</div>
        <div class="filter-banner-text">{{ filterSummaryText }}</div>
        <el-button size="small" type="primary" link @click="clearRouteFilters">
          清除来源筛选
        </el-button>
      </div>

      <el-form :inline="true" :model="searchForm" class="search-form pro-filter-bar">
        <el-form-item label="题目 ID">
          <el-input
            v-model="searchForm.problemId"
            placeholder="例如 1001"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="全部"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="语言">
          <el-select
            v-model="searchForm.language"
            placeholder="全部"
            clearable
            style="width: 140px"
          >
            <el-option label="Java" value="JAVA" />
            <el-option label="C++" value="CPP" />
            <el-option label="Python" value="PYTHON" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">当前页记录</div>
          <div class="overview-value">{{ pageStats.total }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">通过数</div>
          <div class="overview-value">{{ pageStats.accepted }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">评测中</div>
          <div class="overview-value">{{ pageStats.judging }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">平均耗时</div>
          <div class="overview-value">{{ pageStats.avgTime }}ms</div>
        </div>
      </div>

      <el-skeleton v-if="loading && !submissionList.length" :rows="7" animated />
      <div v-else class="pro-table-shell">
        <el-table
          v-loading="loading"
          :data="submissionList"
          stripe
          style="width: 100%"
          class="submission-table"
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />

          <el-table-column label="题目" min-width="250">
            <template #default="{ row }">
              <el-link
                type="primary"
                @click="goToProblem(row.problemId)"
                class="problem-link"
              >
                <el-icon style="margin-right: 5px;"><Document /></el-icon>
                {{ row.problemTitle }}
              </el-link>
            </template>
          </el-table-column>

          <el-table-column prop="language" label="语言" width="110" align="center">
            <template #default="{ row }">
              <el-tag type="info" effect="plain">{{ row.language }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="160" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="dark" size="large">
                <el-icon style="margin-right: 5px;">
                  <component :is="getStatusIcon(row.status)" />
                </el-icon>
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="executeTime" label="耗时" width="120" align="center">
            <template #default="{ row }">
              <span v-if="row.executeTime" class="stat-number">{{ row.executeTime }}ms</span>
              <span v-else class="stat-placeholder">-</span>
            </template>
          </el-table-column>

          <el-table-column prop="executeMemory" label="内存" width="120" align="center">
            <template #default="{ row }">
              <span v-if="row.executeMemory" class="stat-number">{{ row.executeMemory }}KB</span>
              <span v-else class="stat-placeholder">-</span>
            </template>
          </el-table-column>

          <el-table-column prop="submitTime" label="提交时间" width="180" align="center" />

          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" link @click="goToSubmission(row.id)" :icon="View">
                详情
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无提交记录" :image-size="80" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="loadSubmissions"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { submissionApi } from '@/api'
import { ElMessage } from 'element-plus'
import {
  List,
  Document,
  CircleCheck,
  CircleClose,
  Loading,
  WarningFilled,
  InfoFilled,
  View,
  Search,
  Refresh
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submissionList = ref([])
const pollingTimer = ref(null)
const filterSource = ref('')
const POLL_INTERVAL = 2500
const JUDGING_STATUS = new Set(['PENDING', 'JUDGING'])

const searchForm = reactive({
  problemId: '',
  status: '',
  language: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const statusOptions = [
  { label: '通过', value: 'ACCEPTED' },
  { label: '答案错误', value: 'WRONG_ANSWER' },
  { label: '超出时间限制', value: 'TIME_LIMIT_EXCEEDED' },
  { label: '超出内存限制', value: 'MEMORY_LIMIT_EXCEEDED' },
  { label: '运行错误', value: 'RUNTIME_ERROR' },
  { label: '编译错误', value: 'COMPILE_ERROR' },
  { label: '等待中', value: 'PENDING' },
  { label: '评测中', value: 'JUDGING' }
]

const hasFilterBanner = computed(() => !!filterSource.value)

const filterSourceLabel = computed(() => {
  if (filterSource.value === 'problem-detail') {
    return '题目详情页'
  }
  if (filterSource.value === 'submission-detail') {
    return '提交详情页'
  }
  return '外部链接'
})

const filterSummaryText = computed(() => {
  const parts = []
  if (searchForm.problemId) {
    parts.push(`题目ID: ${searchForm.problemId}`)
  }
  if (searchForm.status) {
    parts.push(`状态: ${getStatusText(searchForm.status)}`)
  }
  if (searchForm.language) {
    const languageMap = {
      JAVA: 'Java',
      CPP: 'C++',
      PYTHON: 'Python'
    }
    parts.push(`语言: ${languageMap[searchForm.language] || searchForm.language}`)
  }
  return parts.length ? parts.join(' | ') : '未附带筛选条件'
})

const pageStats = computed(() => {
  const list = submissionList.value || []
  if (!list.length) {
    return {
      total: 0,
      accepted: 0,
      judging: 0,
      avgTime: 0
    }
  }
  const accepted = list.filter((row) => row.status === 'ACCEPTED').length
  const judging = list.filter((row) => row.status === 'PENDING' || row.status === 'JUDGING').length
  const timedRows = list.filter((row) => Number(row.executeTime) > 0)
  const avgTime = timedRows.length
    ? Math.round(timedRows.reduce((sum, row) => sum + Number(row.executeTime || 0), 0) / timedRows.length)
    : 0
  return {
    total: list.length,
    accepted,
    judging,
    avgTime
  }
})

const getStatusType = (status) => {
  const map = {
    ACCEPTED: 'success',
    WRONG_ANSWER: 'danger',
    TIME_LIMIT_EXCEEDED: 'warning',
    MEMORY_LIMIT_EXCEEDED: 'warning',
    RUNTIME_ERROR: 'danger',
    COMPILE_ERROR: 'info'
  }
  return map[status] || ''
}

const getStatusIcon = (status) => {
  const map = {
    ACCEPTED: CircleCheck,
    WRONG_ANSWER: CircleClose,
    TIME_LIMIT_EXCEEDED: WarningFilled,
    MEMORY_LIMIT_EXCEEDED: WarningFilled,
    RUNTIME_ERROR: CircleClose,
    COMPILE_ERROR: InfoFilled,
    PENDING: Loading,
    JUDGING: Loading
  }
  return map[status] || InfoFilled
}

const getStatusText = (status) => {
  const map = {
    ACCEPTED: '通过',
    WRONG_ANSWER: '答案错误',
    TIME_LIMIT_EXCEEDED: '超出时间限制',
    MEMORY_LIMIT_EXCEEDED: '超出内存限制',
    RUNTIME_ERROR: '运行错误',
    COMPILE_ERROR: '编译错误',
    PENDING: '等待中',
    JUDGING: '评测中'
  }
  return map[status] || status
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
}

const pollSubmissionStatus = async () => {
  const pendingRows = submissionList.value.filter((row) => JUDGING_STATUS.has(row.status))
  if (!pendingRows.length) {
    stopPolling()
    return
  }

  const tasks = pendingRows.map((row) => submissionApi.getSubmissionStatus(row.id, { silent: true }))
  const results = await Promise.allSettled(tasks)
  results.forEach((result, index) => {
    if (result.status !== 'fulfilled') return
    const row = pendingRows[index]
    if (!row) return
    const statusData = result.value.data || {}
    row.status = statusData.status || row.status
    row.executeTime = statusData.executeTime
    row.executeMemory = statusData.executeMemory
  })
}

const syncPolling = () => {
  if (submissionList.value.some((row) => JUDGING_STATUS.has(row.status))) {
    if (!pollingTimer.value) {
      pollingTimer.value = setInterval(() => {
        pollSubmissionStatus()
      }, POLL_INTERVAL)
    }
  } else {
    stopPolling()
  }
}

const buildParams = () => {
  const params = {
    page: pagination.page,
    size: pagination.size
  }

  if (searchForm.problemId) {
    params.problemId = searchForm.problemId.trim()
  }

  if (searchForm.status) {
    params.status = searchForm.status
  }

  if (searchForm.language) {
    params.language = searchForm.language
  }

  return params
}

const loadSubmissions = async () => {
  loading.value = true
  try {
    const res = await submissionApi.getSubmissionList(buildParams())
    submissionList.value = res.data.records || res.data.list || []
    pagination.total = res.data.total || 0
    syncPolling()
  } catch (error) {
    ElMessage.error(error.message || '加载提交记录失败')
  } finally {
    loading.value = false
  }
}

const applyQueryFilters = (query) => {
  filterSource.value = String(query.source || '')
  if (!filterSource.value && query.fromSubmission === '1') {
    filterSource.value = 'submission-detail'
  }

  searchForm.problemId = query.problemId !== undefined ? String(query.problemId || '') : ''

  const status = String(query.status || '')
  if (!status || statusOptions.some((item) => item.value === status)) {
    searchForm.status = status
  } else {
    searchForm.status = ''
  }

  const language = String(query.language || '')
  if (!language || ['JAVA', 'CPP', 'PYTHON'].includes(language)) {
    searchForm.language = language
  } else {
    searchForm.language = ''
  }
}

const clearRouteFilters = () => {
  router.replace({ path: '/submissions' })
}

const handleSearch = () => {
  pagination.page = 1
  filterSource.value = ''
  stopPolling()
  loadSubmissions()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  stopPolling()
  loadSubmissions()
}

const resetSearch = () => {
  searchForm.problemId = ''
  searchForm.status = ''
  searchForm.language = ''
  filterSource.value = ''
  pagination.page = 1
  stopPolling()
  loadSubmissions()
}

const goToProblem = (id) => {
  router.push(`/problem/${id}`)
}

const goToSubmission = (id) => {
  router.push(`/submission/${id}`)
}

onMounted(() => {
  applyQueryFilters(route.query)
  loadSubmissions()
})

watch(
  () => route.query,
  (query) => {
    applyQueryFilters(query)
    pagination.page = 1
    stopPolling()
    loadSubmissions()
  }
)

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.main-card {
  border-radius: 8px;
}

.search-form {
  margin-bottom: 14px;
}

.filter-banner {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid #d2e2f7;
  background: linear-gradient(155deg, #f5f9ff 0%, #edf5ff 100%);
  border-radius: 8px;
}

.filter-banner-title {
  font-size: 13px;
  color: #1758b7;
  font-weight: 600;
  margin-bottom: 4px;
}

.filter-banner-text {
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}

.problem-link {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.overview-card {
  border: 1px solid #dce8f8;
  background: linear-gradient(160deg, #f9fbff 0%, #eef6ff 100%);
  border-radius: 8px;
  padding: 12px 14px;
  transition: transform 0.18s ease, box-shadow 0.2s ease;
}

.overview-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.1);
}

.overview-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 5px;
}

.overview-value {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.title-stack {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.title-sub {
  margin-top: 3px;
  font-size: 12px;
  color: #64748b;
}

.stat-number {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
}

.stat-placeholder {
  color: #c0c4cc;
}

.pagination-container {
  margin-top: 22px;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
