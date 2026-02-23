<template>
  <div class="submission-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header">
          <h2 class="submission-title">
            <el-icon><Document /></el-icon>
            提交详情 #{{ submission.id }}
          </h2>
          <div class="header-actions">
            <el-button
              :disabled="!submission.problemId"
              @click="goToProblemWithFilter"
            >
              去题目页（保留筛选）
            </el-button>
            <el-tag v-if="isJudging" type="warning" effect="dark">
              评测中，自动刷新
            </el-tag>
            <el-button :icon="Refresh" @click="loadSubmission" :loading="loading">
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-item">
          <div class="overview-label">评测状态</div>
          <el-tag :type="getStatusType(submission.status)" effect="dark" size="large">
            <el-icon style="margin-right: 5px;">
              <component :is="getStatusIcon(submission.status)" />
            </el-icon>
            {{ getStatusText(submission.status) }}
          </el-tag>
        </div>
        <div class="overview-item">
          <div class="overview-label">执行时间</div>
          <div class="overview-value">{{ submission.executeTime ? `${submission.executeTime} ms` : '-' }}</div>
        </div>
        <div class="overview-item">
          <div class="overview-label">执行内存</div>
          <div class="overview-value">{{ submission.executeMemory ? `${submission.executeMemory} KB` : '-' }}</div>
        </div>
      </div>

      <div class="timeline-card">
        <div class="timeline-title">判题进度</div>
        <el-steps
          :active="statusStep.active"
          align-center
          :process-status="statusStep.processStatus"
          :finish-status="statusStep.finishStatus"
        >
          <el-step title="已提交" />
          <el-step title="评测中" />
          <el-step title="完成" />
        </el-steps>
        <div class="timeline-hint">{{ statusStep.hint }}</div>
      </div>

      <el-descriptions :column="2" border class="submission-info">
        <el-descriptions-item label="题目">
          <el-link type="primary" @click="goToProblemWithFilter">
            {{ submission.problemTitle }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(submission.status)" effect="dark" size="large">
            <el-icon style="margin-right: 5px;">
              <component :is="getStatusIcon(submission.status)" />
            </el-icon>
            {{ getStatusText(submission.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="语言">
          <el-tag type="info" effect="plain">{{ submission.language }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行时间">
          <span class="info-value">
            {{ submission.executeTime ? submission.executeTime + 'ms' : '-' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="内存">
          <span class="info-value">
            {{ submission.executeMemory ? submission.executeMemory + 'KB' : '-' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          <span class="info-value">{{ submission.submitTime }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <div class="code-section">
        <h3 class="section-title">
          <el-icon><Edit /></el-icon>
          提交代码
        </h3>
        <div class="section-actions">
          <el-button size="small" @click="copyCode">复制代码</el-button>
        </div>
        <div class="code-wrapper">
          <pre class="code-block">{{ submission.code }}</pre>
        </div>
      </div>

      <div v-if="submission.errorMessage" class="error-section">
        <el-divider />
        <h3 class="section-title error-title">
          <el-icon><Warning /></el-icon>
          错误信息
        </h3>
        <div class="section-actions">
          <el-button size="small" type="danger" plain @click="copyErrorMessage">
            复制错误信息
          </el-button>
        </div>
        <div class="error-wrapper">
          <pre class="error-block">{{ submission.errorMessage }}</pre>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { submissionApi } from '@/api'
import { ElMessage } from 'element-plus'
import {
  Document,
  CircleCheck,
  CircleClose,
  Loading,
  WarningFilled,
  InfoFilled,
  Edit,
  Warning,
  Refresh
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submission = ref({})
const pollingTimer = ref(null)

const JUDGING_STATUS = new Set(['PENDING', 'JUDGING'])
const FINAL_STATUS = new Set([
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR'
])
const POLL_INTERVAL = 2000

const isJudging = computed(() => JUDGING_STATUS.has(submission.value.status))

const statusStep = computed(() => {
  const status = submission.value.status
  if (status === 'PENDING') {
    return {
      active: 0,
      processStatus: 'process',
      finishStatus: 'success',
      hint: '代码已提交，等待进入判题队列。'
    }
  }
  if (status === 'JUDGING') {
    return {
      active: 1,
      processStatus: 'process',
      finishStatus: 'success',
      hint: '系统正在编译并运行测试点。'
    }
  }
  if (status === 'ACCEPTED') {
    return {
      active: 2,
      processStatus: 'success',
      finishStatus: 'success',
      hint: '恭喜，所有测试点均已通过。'
    }
  }
  if (FINAL_STATUS.has(status)) {
    return {
      active: 2,
      processStatus: 'error',
      finishStatus: 'success',
      hint: '评测结束，可查看错误信息定位问题。'
    }
  }
  return {
    active: 0,
    processStatus: 'wait',
    finishStatus: 'success',
    hint: '等待评测状态更新。'
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
    TIME_LIMIT_EXCEEDED: '超时',
    MEMORY_LIMIT_EXCEEDED: '内存超限',
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

const mergeStatus = (statusData) => {
  if (!statusData) return
  submission.value = {
    ...submission.value,
    status: statusData.status,
    executeTime: statusData.executeTime,
    executeMemory: statusData.executeMemory,
    errorMessage: statusData.errorMessage,
    submitTime: statusData.submitTime || submission.value.submitTime
  }
}

const pollSubmissionStatus = async () => {
  try {
    const res = await submissionApi.getSubmissionStatus(route.params.id, { silent: true })
    mergeStatus(res.data)
    if (FINAL_STATUS.has(res.data?.status)) {
      stopPolling()
    }
  } catch (error) {
    stopPolling()
  }
}

const startPolling = () => {
  if (pollingTimer.value) return
  pollingTimer.value = setInterval(() => {
    pollSubmissionStatus()
  }, POLL_INTERVAL)
}

const syncPolling = () => {
  if (JUDGING_STATUS.has(submission.value.status)) {
    startPolling()
  } else {
    stopPolling()
  }
}

const loadSubmission = async () => {
  loading.value = true
  try {
    const res = await submissionApi.getSubmissionDetail(route.params.id)
    submission.value = res.data
    syncPolling()
  } catch (error) {
    stopPolling()
    ElMessage.error('加载提交详情失败')
    router.push('/submissions')
  } finally {
    loading.value = false
  }
}

const goToProblemWithFilter = () => {
  if (!submission.value.problemId) return
  router.push({
    path: `/problem/${submission.value.problemId}`,
    query: {
      fromSubmission: '1',
      filterProblemId: String(submission.value.problemId),
      filterStatus: submission.value.status || '',
      filterLanguage: submission.value.language || ''
    }
  })
}

const copyText = async (text, successMessage) => {
  if (!text) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const copyCode = async () => {
  await copyText(submission.value.code, '代码已复制')
}

const copyErrorMessage = async () => {
  await copyText(submission.value.errorMessage, '错误信息已复制')
}

onMounted(() => {
  loadSubmission()
})

watch(
  () => route.params.id,
  () => {
    stopPolling()
    loadSubmission()
  }
)

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.submission-detail-container {
  max-width: 1400px;
  margin: 0 auto;
}

.main-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  background: linear-gradient(135deg, #f9fafc 0%, #eef2f6 100%);
  border-radius: 8px 8px 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.submission-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.submission-info {
  margin-top: 20px;
}

.timeline-card {
  margin-bottom: 20px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.timeline-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  font-weight: 600;
}

.timeline-hint {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.overview-item {
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.overview-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.overview-value {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.info-value {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.code-section,
.error-section {
  margin-top: 30px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-actions {
  margin-bottom: 10px;
}

.error-title {
  color: #f56c6c;
}

.code-wrapper,
.error-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.code-block {
  background: #2d2d2d;
  color: #f8f8f2;
  padding: 20px;
  margin: 0;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
}

.error-block {
  background: #fef0f0;
  color: #f56c6c;
  padding: 20px;
  margin: 0;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
