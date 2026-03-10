<template>
  <div class="contest-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回</el-button>
            <div class="title-stack">
              <h2 class="pro-title-text">#{{ contest.id || '-' }} {{ contest.title || '未命名竞赛' }}</h2>
              <span class="title-sub">竞赛详情、题目列表与排行榜</span>
            </div>
          </div>
          <div class="header-actions">
            <el-tag :type="getContestStatusType(contest.contestStatus)" class="status-tag">
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
            <el-button v-if="canManage && canDeleteContest" type="danger" @click="handleDeleteContest">
              删除
            </el-button>
            <el-button v-if="canManage" type="primary" plain @click="exportRanking">导出排行榜</el-button>
            <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">开始时间</div>
          <div class="overview-value text-value">{{ formatDateTime(contest.startTime) }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">结束时间</div>
          <div class="overview-value text-value">{{ formatDateTime(contest.endTime) }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">题目数</div>
          <div class="overview-value">{{ contest.problemCount || 0 }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">参赛人数</div>
          <div class="overview-value">{{ contest.participantCount || 0 }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">封榜时间</div>
          <div class="overview-value text-value">{{ formatDateTime(contest.scoreboardFreezeTime) }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">错误罚时</div>
          <div class="overview-value">{{ contest.penaltyPerWrong ?? 20 }} 分钟</div>
        </div>
      </div>

      <section class="section-panel">
        <div class="section-title">竞赛描述</div>
        <div class="description-block">
          <pre>{{ contest.description || '暂无竞赛描述' }}</pre>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div class="section-title">题目列表</div>
          <span class="section-extra">共 {{ (contest.problems || []).length }} 题</span>
        </div>
        <div class="pro-table-shell">
          <el-table :data="contest.problems || []" stripe>
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column prop="title" label="标题" min-width="220">
              <template #default="{ row }">
                <el-link type="primary" @click="goToProblem(row.id)">{{ row.title }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="difficulty" label="难度" width="120" />
            <template #empty>
              <el-empty description="暂无题目" :image-size="72" />
            </template>
          </el-table>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head ranking-head">
          <div class="section-title">排行榜</div>
          <div class="ranking-head-right">
            <el-radio-group v-if="canManage" v-model="rankingSource" size="small" @change="handleRankingSourceChange">
              <el-radio-button label="realtime">实时</el-radio-button>
              <el-radio-button label="snapshot">成绩快照</el-radio-button>
            </el-radio-group>
            <span class="section-extra">{{ rankingSourceText }}</span>
          </div>
        </div>
        <el-alert
          v-if="contest.rankingFrozen && !canManage"
          title="当前为封榜阶段，排行榜已冻结到封榜时刻"
          type="warning"
          :closable="false"
          class="freeze-alert"
        />
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
            <el-table-column prop="totalSubmissions" label="提交数" width="120" align="center" />
            <el-table-column label="最后 AC 时间" min-width="180">
              <template #default="{ row }">{{ formatDateTime(row.lastAcceptedTime) }}</template>
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
      </section>

      <template v-if="canManage">
        <section class="section-panel">
          <div class="section-title">竞赛分析</div>

          <el-skeleton v-if="analyticsLoading" :rows="6" animated />

          <template v-else>
            <div class="overview-grid analytics-overview">
              <div class="overview-card">
                <div class="overview-label">活跃参赛人数</div>
                <div class="overview-value text-value">
                  {{ analytics.activeParticipantCount || 0 }} / {{ analytics.participantCount || 0 }}
                </div>
              </div>
              <div class="overview-card">
                <div class="overview-label">总提交数</div>
                <div class="overview-value">{{ analytics.totalSubmissions || 0 }}</div>
              </div>
              <div class="overview-card">
                <div class="overview-label">通过提交数</div>
                <div class="overview-value">{{ analytics.acceptedSubmissions || 0 }}</div>
              </div>
              <div class="overview-card">
                <div class="overview-label">通过率</div>
                <div class="overview-value">{{ analytics.acceptanceRate ?? 0 }}%</div>
              </div>
            </div>

            <div class="analytics-grid">
              <el-card class="analytics-card" shadow="never">
                <template #header>状态分布</template>
                <div class="pro-table-shell">
                  <el-table :data="toDistributionRows(analytics.statusDistribution, analytics.totalSubmissions)" size="small">
                    <el-table-column prop="name" label="状态" min-width="160" />
                    <el-table-column prop="count" label="数量" width="90" align="center" />
                    <el-table-column prop="rate" label="占比" width="100" align="center" />
                  </el-table>
                </div>
              </el-card>

              <el-card class="analytics-card" shadow="never">
                <template #header>语言分布</template>
                <div class="pro-table-shell">
                  <el-table :data="toDistributionRows(analytics.languageDistribution, analytics.totalSubmissions)" size="small">
                    <el-table-column prop="name" label="语言" min-width="160" />
                    <el-table-column prop="count" label="数量" width="90" align="center" />
                    <el-table-column prop="rate" label="占比" width="100" align="center" />
                  </el-table>
                </div>
              </el-card>
            </div>

            <div class="section-title sub-section-title">题目统计</div>
            <div class="pro-table-shell">
              <el-table :data="analytics.problemStats || []" stripe size="small">
                <el-table-column prop="problemId" label="题号" width="90" />
                <el-table-column prop="title" label="标题" min-width="220" />
                <el-table-column prop="difficulty" label="难度" width="100" align="center" />
                <el-table-column prop="totalSubmissions" label="提交数" width="100" align="center" />
                <el-table-column prop="acceptedSubmissions" label="通过数" width="100" align="center" />
                <el-table-column prop="acceptedUserCount" label="通过人数" width="110" align="center" />
                <el-table-column label="通过率" width="90" align="center">
                  <template #default="{ row }">{{ row.passRate ?? 0 }}%</template>
                </el-table-column>
                <template #empty>
                  <el-empty description="暂无统计数据" :image-size="72" />
                </template>
              </el-table>
            </div>
          </template>
        </section>
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
        <el-form-item label="封榜时间" prop="scoreboardFreezeTime">
          <el-date-picker
            v-model="form.scoreboardFreezeTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            clearable
            placeholder="可选，不设置表示不封榜"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="错误罚时(分钟)" prop="penaltyPerWrong">
          <el-input-number v-model="form.penaltyPerWrong" :min="0" :max="120" style="width: 220px" />
          <span class="field-tip">每道题每次错误提交增加的罚时</span>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const rankingLoading = ref(false)
const analyticsLoading = ref(false)
const contest = ref({})
const rankingList = ref([])
const rankingSource = ref('realtime')

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
const canDeleteContest = computed(() => {
  if (!canManage.value) return false
  if (userStore.isAdmin) return true
  return Number(contest.value?.creatorId) > 0 && Number(contest.value?.creatorId) === Number(userStore.userInfo?.id)
})

const rankingSourceText = computed(() => (rankingSource.value === 'snapshot' ? '展示数据库成绩快照' : '展示实时排行榜'))

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
  scoreboardFreezeTime: '',
  penaltyPerWrong: 20,
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

const validateFreezeTime = (_, __, callback) => {
  if (!form.scoreboardFreezeTime) {
    callback()
    return
  }
  if (!form.startTime || !form.endTime) {
    callback(new Error('请先设置开始和结束时间'))
    return
  }
  const freezeTime = new Date(form.scoreboardFreezeTime).getTime()
  const startTime = new Date(form.startTime).getTime()
  const endTime = new Date(form.endTime).getTime()
  if (Number.isNaN(freezeTime) || freezeTime < startTime || freezeTime > endTime) {
    callback(new Error('封榜时间必须在开始和结束时间之间'))
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
  scoreboardFreezeTime: [{ validator: validateFreezeTime, trigger: 'change' }],
  penaltyPerWrong: [{ required: true, message: '请输入错误罚时', trigger: 'change' }],
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
    const params = {
      page: rankingPagination.page,
      size: rankingPagination.size
    }
    const res = rankingSource.value === 'snapshot' && canManage.value
      ? await contestApi.getContestScoreSnapshot(contestId.value, params)
      : await contestApi.getContestRanking(contestId.value, params)
    rankingList.value = res.data?.records || []
    rankingPagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载排行榜失败')
  } finally {
    rankingLoading.value = false
  }
}

const toDistributionRows = (mapObj, total) => {
  const source = mapObj || {}
  return Object.keys(source)
    .map((key) => {
      const count = Number(source[key] || 0)
      const rate = total ? `${Math.round((count * 1000) / total) / 10}%` : '0%'
      return { name: key, count, rate }
    })
    .sort((a, b) => b.count - a.count)
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

const handleDeleteContest = async () => {
  try {
    await ElMessageBox.confirm(
      `确认删除竞赛「${contest.value?.title || contestId.value}」吗？删除后不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await contestApi.deleteContest(contestId.value)
    ElMessage.success('竞赛已删除')
    router.push('/contests')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '删除失败')
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
    problemOptions.value = res.data?.records || []
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
  form.scoreboardFreezeTime = contest.value.scoreboardFreezeTime || ''
  form.penaltyPerWrong = Number.isFinite(Number(contest.value.penaltyPerWrong))
    ? Number(contest.value.penaltyPerWrong)
    : 20
  form.status = Number(contest.value.status ?? 1)
  form.problemIds = Array.isArray(contest.value.problems) ? contest.value.problems.map((item) => item.id) : []
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

const submitEdit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (_) {
    return
  }
  submitting.value = true
  try {
    await contestApi.updateContest(contestId.value, {
      title: form.title.trim(),
      description: form.description?.trim() || '',
      startTime: form.startTime,
      endTime: form.endTime,
      scoreboardFreezeTime: form.scoreboardFreezeTime || null,
      penaltyPerWrong: Number(form.penaltyPerWrong),
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
}

const exportRanking = async () => {
  try {
    const res = await contestApi.exportContestRanking(contestId.value)
    const csvText = res.data || ''
    const blob = new Blob([`\ufeff${csvText}`], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const safeTitle = String(contest.value.title || `竞赛-${contestId.value}`)
      .replace(/[\\/:*?"<>|]/g, '-')
      .replace(/\s+/g, '_')
      .slice(0, 60)
    a.href = url
    a.download = `${safeTitle || `竞赛-${contestId.value}`}-排行榜.csv`
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

const handleRankingSourceChange = async () => {
  rankingPagination.page = 1
  await loadRanking()
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
    if (!canManage.value && rankingSource.value === 'snapshot') {
      rankingSource.value = 'realtime'
    }
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

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.status-tag {
  font-weight: 700;
  min-height: 28px;
  padding-inline: 10px;
}

.title-stack {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.title-sub {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
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

.text-value {
  font-size: 14px;
  line-height: 1.35;
}

.section-panel {
  border: 1px solid #e2e9f3;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.ranking-head-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.section-extra {
  color: #64748b;
  font-size: 13px;
}

.freeze-alert {
  margin-bottom: 12px;
}

.description-block pre {
  margin: 8px 0 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.78;
  font-family: inherit;
  color: #334155;
  font-size: 14px;
}

.analytics-overview {
  margin-top: 10px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 10px 0 12px;
}

.analytics-card {
  border-radius: 8px;
}

.sub-section-title {
  margin-top: 4px;
  margin-bottom: 10px;
}

.pagination-container {
  margin-top: 20px;
}

.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 980px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ranking-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
