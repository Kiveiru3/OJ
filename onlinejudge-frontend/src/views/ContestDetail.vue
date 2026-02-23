<template>
  <div class="contest-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回</el-button>
            <h2>#{{ contest.id }} {{ contest.title }}</h2>
          </div>
          <div class="header-actions">
            <el-tag :type="getContestStatusType(contest.contestStatus)">
              {{ getContestStatusText(contest.contestStatus) }}
            </el-tag>
            <el-button
              v-if="!contest.joined && contest.contestStatus !== 'ENDED'"
              type="success"
              @click="joinContest"
            >
              报名
            </el-button>
            <el-button v-if="canManage" type="warning" @click="openEditDialog">编辑</el-button>
            <el-button v-if="canManage" type="primary" plain @click="exportRanking">
              导出排行榜
            </el-button>
            <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="meta-grid">
        <div class="meta-item">
          <div class="meta-label">开始时间</div>
          <div class="meta-value">{{ formatDateTime(contest.startTime) }}</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">结束时间</div>
          <div class="meta-value">{{ formatDateTime(contest.endTime) }}</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">题目数</div>
          <div class="meta-value">{{ contest.problemCount || 0 }}</div>
        </div>
        <div class="meta-item">
          <div class="meta-label">参赛人数</div>
          <div class="meta-value">{{ contest.participantCount || 0 }}</div>
        </div>
      </div>

      <div class="description-block">
        <div class="section-title">竞赛描述</div>
        <pre>{{ contest.description || '-' }}</pre>
      </div>

      <el-divider />

      <div class="section-title">题目列表</div>
      <div class="pro-table-shell">
        <el-table :data="contest.problems || []" stripe>
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="title" label="标题" min-width="220">
            <template #default="{ row }">
              <el-link type="primary" @click="goToProblem(row.id)">
                {{ row.title }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="140" />
          <template #empty>
            <el-empty description="暂无题目" :image-size="72" />
          </template>
        </el-table>
      </div>

      <el-divider />

      <div class="ranking-header">
        <div class="section-title">排行榜</div>
      </div>
      <div class="pro-table-shell">
        <el-table v-loading="rankingLoading" :data="rankingList" stripe>
          <el-table-column prop="rank" label="排名" width="90" align="center" />
          <el-table-column label="用户" min-width="220">
            <template #default="{ row }">
              {{ row.nickname || row.username || `用户#${row.userId}` }}
            </template>
          </el-table-column>
          <el-table-column prop="acceptedCount" label="通过题数" width="120" align="center" />
          <el-table-column prop="totalPenalty" label="罚时" width="120" align="center" />
          <el-table-column prop="totalSubmissions" label="提交数" width="130" align="center" />
          <el-table-column label="最后 AC 时间" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.lastAcceptedTime) }}
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无排行榜数据" :image-size="72" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="rankingPagination.page"
          v-model:page-size="rankingPagination.size"
          :total="rankingPagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleRankingPageSizeChange"
          @current-change="loadRanking"
        />
      </div>

      <template v-if="canManage">
        <el-divider />
        <div class="section-title">竞赛分析</div>

        <el-skeleton v-if="analyticsLoading" :rows="6" animated />

        <template v-else>
          <div class="meta-grid">
            <div class="meta-item">
              <div class="meta-label">活跃参赛人数</div>
              <div class="meta-value">
                {{ analytics.activeParticipantCount || 0 }} / {{ analytics.participantCount || 0 }}
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-label">总提交数</div>
              <div class="meta-value">{{ analytics.totalSubmissions || 0 }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">通过提交数</div>
              <div class="meta-value">{{ analytics.acceptedSubmissions || 0 }}</div>
            </div>
            <div class="meta-item">
              <div class="meta-label">通过率</div>
              <div class="meta-value">{{ analytics.acceptanceRate ?? 0 }}%</div>
            </div>
          </div>

          <div class="analytics-grid">
            <el-card class="analytics-card" shadow="never">
              <template #header>状态分布</template>
              <div class="pro-table-shell">
                <el-table :data="toDistributionRows(analytics.statusDistribution, analytics.totalSubmissions)" size="small">
                  <el-table-column prop="name" label="状态" min-width="180" />
                  <el-table-column prop="count" label="数量" width="90" align="center" />
                  <el-table-column prop="rate" label="占比" width="100" align="center" />
                </el-table>
              </div>
            </el-card>

            <el-card class="analytics-card" shadow="never">
              <template #header>语言分布</template>
              <div class="pro-table-shell">
                <el-table :data="toDistributionRows(analytics.languageDistribution, analytics.totalSubmissions)" size="small">
                  <el-table-column prop="name" label="语言" min-width="140" />
                  <el-table-column prop="count" label="数量" width="90" align="center" />
                  <el-table-column prop="rate" label="占比" width="100" align="center" />
                </el-table>
              </div>
            </el-card>
          </div>

          <div class="section-title analytics-subtitle">题目统计</div>
          <div class="pro-table-shell">
            <el-table :data="analytics.problemStats || []" stripe size="small">
              <el-table-column prop="problemId" label="题号" width="90" />
              <el-table-column prop="title" label="标题" min-width="220" />
              <el-table-column prop="difficulty" label="难度" width="110" align="center" />
              <el-table-column prop="totalSubmissions" label="提交数" width="120" align="center" />
              <el-table-column prop="acceptedSubmissions" label="通过数" width="110" align="center" />
              <el-table-column prop="acceptedUserCount" label="通过人数" width="130" align="center" />
              <el-table-column prop="passRate" label="通过率" width="100" align="center">
                <template #default="{ row }">{{ row.passRate ?? 0 }}%</template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无统计数据" :image-size="72" />
              </template>
            </el-table>
          </div>
        </template>
      </template>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="编辑竞赛"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="可见性" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目" prop="problemIds">
          <el-select
            v-model="form.problemIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%"
            placeholder="请选择题目"
          >
            <el-option
              v-for="item in problemOptions"
              :key="item.id"
              :label="`#${item.id} ${item.title}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { contestApi, problemApi } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const rankingLoading = ref(false)
const analyticsLoading = ref(false)
const contest = ref({})
const rankingList = ref([])
const analytics = ref({
  participantCount: 0,
  activeParticipantCount: 0,
  totalSubmissions: 0,
  acceptedSubmissions: 0,
  acceptanceRate: 0,
  statusDistribution: {},
  languageDistribution: {},
  problemStats: []
})

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const problemOptions = ref([])

const canManage = computed(() => userStore.isTeacher || userStore.isAdmin)

const rankingPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

const form = reactive({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  status: 1,
  problemIds: []
})

const validateTimeRange = (_, __, callback) => {
  if (!form.startTime || !form.endTime) {
    callback()
    return
  }
  if (new Date(form.endTime).getTime() <= new Date(form.startTime).getTime()) {
    callback(new Error('结束时间必须晚于开始时间'))
    return
  }
  callback()
}

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' },
    { validator: validateTimeRange, trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    { validator: validateTimeRange, trigger: 'change' }
  ],
  status: [{ required: true, message: '请选择可见性', trigger: 'change' }],
  problemIds: [{ required: true, type: 'array', min: 1, message: '至少选择一道题目', trigger: 'change' }]
}

const contestId = computed(() => Number(route.params.id))

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

const getContestStatusType = (status) => {
  if (status === 'RUNNING') return 'success'
  if (status === 'UPCOMING') return 'warning'
  if (status === 'ENDED') return 'info'
  return ''
}

const getContestStatusText = (status) => {
  if (status === 'RUNNING') return '进行中'
  if (status === 'UPCOMING') return '未开始'
  if (status === 'ENDED') return '已结束'
  return status || '-'
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await contestApi.getContestDetail(contestId.value)
    contest.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '加载竞赛详情失败')
    router.push('/contests')
  } finally {
    loading.value = false
  }
}

const loadRanking = async () => {
  rankingLoading.value = true
  try {
    const res = await contestApi.getContestRanking(contestId.value, {
      page: rankingPagination.page,
      size: rankingPagination.size
    })
    rankingList.value = res.data.records || []
    rankingPagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载排行榜失败')
  } finally {
    rankingLoading.value = false
  }
}

const toDistributionRows = (mapObj, total) => {
  const source = mapObj || {}
  return Object.keys(source).map((key) => {
    const count = Number(source[key] || 0)
    const rate = total ? `${Math.round((count * 1000) / total) / 10}%` : '0%'
    return {
      name: key,
      count,
      rate
    }
  }).sort((a, b) => b.count - a.count)
}

const loadAnalytics = async () => {
  if (!canManage.value) {
    analytics.value = {
      participantCount: 0,
      activeParticipantCount: 0,
      totalSubmissions: 0,
      acceptedSubmissions: 0,
      acceptanceRate: 0,
      statusDistribution: {},
      languageDistribution: {},
      problemStats: []
    }
    return
  }
  analyticsLoading.value = true
  try {
    const res = await contestApi.getContestAnalytics(contestId.value)
    analytics.value = res.data || analytics.value
  } catch (error) {
    ElMessage.error(error.message || '加载竞赛分析失败')
  } finally {
    analyticsLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadDetail(), loadRanking(), loadAnalytics()])
}

const joinContest = async () => {
  try {
    await contestApi.joinContest(contestId.value)
    ElMessage.success('报名成功')
    await refreshAll()
  } catch (error) {
    ElMessage.error(error.message || '报名失败')
  }
}

const loadProblemOptions = async () => {
  if (!canManage.value) return
  try {
    const res = await problemApi.getProblemList({
      page: 1,
      size: 500,
      includeHidden: true
    })
    problemOptions.value = res.data.records || []
  } catch (error) {
    ElMessage.error(error.message || '加载题目选项失败')
  }
}

const openEditDialog = async () => {
  await loadProblemOptions()
  form.title = contest.value.title || ''
  form.description = contest.value.description || ''
  form.startTime = contest.value.startTime || ''
  form.endTime = contest.value.endTime || ''
  form.status = Number(contest.value.status ?? 1)
  form.problemIds = Array.isArray(contest.value.problems) ? contest.value.problems.map((item) => item.id) : []
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const submitEdit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await contestApi.updateContest(contestId.value, {
        title: form.title.trim(),
        description: form.description?.trim() || '',
        startTime: form.startTime,
        endTime: form.endTime,
        status: Number(form.status),
        problemIds: form.problemIds.map((id) => Number(id))
      })
      dialogVisible.value = false
      ElMessage.success('竞赛更新成功')
      await refreshAll()
    } catch (error) {
      ElMessage.error(error.message || '更新失败')
    } finally {
      submitting.value = false
    }
  })
}

const exportRanking = async () => {
  try {
    const res = await contestApi.exportContestRanking(contestId.value)
    const csvText = res.data || ''
    const blob = new Blob([`\ufeff${csvText}`], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const safeTitle = String(contest.value.title || `contest-${contestId.value}`)
      .replace(/[\\/:*?"<>|]/g, '-')
      .replace(/\s+/g, '_')
      .slice(0, 60)
    a.href = url
    a.download = `${safeTitle || `contest-${contestId.value}`}-ranking.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('排行榜导出成功')
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

const handleRankingPageSizeChange = () => {
  rankingPagination.page = 1
  loadRanking()
}

const goToProblem = (id) => {
  router.push(`/problem/${id}`)
}

const goBack = () => {
  router.push('/contests')
}

onMounted(async () => {
  await refreshAll()
})

watch(
  () => route.params.id,
  async () => {
    rankingPagination.page = 1
    await refreshAll()
  }
)
</script>

<style scoped>
.contest-detail-container {
  max-width: 1400px;
  margin: 0 auto;
}

.main-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left h2 {
  margin: 0;
  font-size: 22px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.meta-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.meta-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.meta-value {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 10px;
}

.description-block pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
  color: #606266;
}

.ranking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.analytics-card {
  border-radius: 8px;
}

.analytics-subtitle {
  margin-top: 8px;
}

.pagination-container {
  margin-top: 22px;
}

@media (max-width: 980px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }

  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
