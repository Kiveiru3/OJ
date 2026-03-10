<template>
  <div class="submission-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回</el-button>
            <div class="title-stack">
              <h2 class="pro-title-text">提交详情 #{{ submission.id }}</h2>
              <span class="title-sub">评测结果、资源占用与代码内容</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button :disabled="!submission.problemId" @click="goToProblemWithFilter">
              去题目页（保留筛选）
            </el-button>
                        <el-button
              v-if="canRejudge"
              type="warning"
              plain
              :disabled="!canClickRejudge"
              @click="handleRejudge"
            >
              Rejudge
            </el-button>
            <el-tag v-if="isJudging" type="warning" effect="dark">评测中，自动刷新</el-tag>
            <el-button :icon="Refresh" @click="loadSubmission" :loading="loading">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">评测状态</div>
          <div class="status-wrap">
            <el-tag :type="getStatusType(submission.status)" effect="dark" size="large">
              <el-icon style="margin-right: 5px;">
                <component :is="getStatusIcon(submission.status)" />
              </el-icon>
              {{ getStatusText(submission.status) }}
            </el-tag>
          </div>
        </div>
        <div class="overview-card">
          <div class="overview-label">执行时间</div>
          <div class="overview-value">{{ submission.executeTime ? `${submission.executeTime} ms` : '-' }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">执行内存</div>
          <div class="overview-value">{{ submission.executeMemory ? `${submission.executeMemory} KB` : '-' }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">提交语言</div>
          <div class="overview-value text-value">{{ submission.language || '-' }}</div>
        </div>
      </div>

      <section class="section-panel">
        <div class="section-title">判题进度</div>
        <el-steps
          :active="statusStep.active"
          align-center
          :process-status="statusStep.processStatus"
          :finish-status="statusStep.finishStatus"
          class="steps"
        >
          <el-step title="已提交" />
          <el-step title="评测中" />
          <el-step title="完成" />
        </el-steps>
        <div class="timeline-hint">{{ statusStep.hint }}</div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div class="section-title">提交信息</div>
          <el-link type="primary" @click="goToProblemWithFilter">
            {{ submission.problemTitle || '未绑定题目' }}
          </el-link>
        </div>
        <el-descriptions :column="2" border class="submission-info">
          <el-descriptions-item label="题目">
            <el-link type="primary" @click="goToProblemWithFilter">
              {{ submission.problemTitle }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(submission.status)" effect="dark">
              {{ getStatusText(submission.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="语言">
            <el-tag type="info" effect="plain">{{ submission.language }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行时间">
            {{ submission.executeTime ? `${submission.executeTime} ms` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="内存">
            {{ submission.executeMemory ? `${submission.executeMemory} KB` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ submission.submitTime || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div class="section-title">提交代码</div>
          <el-button size="small" @click="copyCode">复制代码</el-button>
        </div>
        <div class="code-wrapper">
          <pre class="code-block">{{ submission.code || '' }}</pre>
        </div>
      </section>

      <section v-if="submission.errorMessage" class="section-panel error-panel">
        <div class="section-head">
          <div class="section-title error-title">
            <el-icon><Warning /></el-icon>
            错误信息
          </div>
          <el-button size="small" type="danger" plain @click="copyErrorMessage">复制错误信息</el-button>
        </div>
        <div class="error-wrapper">
          <pre class="error-block">{{ submission.errorMessage }}</pre>
        </div>
      </section>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { submissionApi } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheck,
  CircleClose,
  InfoFilled,
  Loading,
  Refresh,
  Warning,
  WarningFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

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
const canRejudge = computed(() => userStore.isAdmin || userStore.isTeacher)
const canClickRejudge = computed(() => {
  return canRejudge.value && Boolean(submission.value.id) && !loading.value && !isJudging.value
})

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
    submission.value = res.data || {}
    syncPolling()
  } catch (error) {
    stopPolling()
    ElMessage.error('加载提交详情失败')
    router.push('/submissions')
  } finally {
    loading.value = false
  }
}

const handleRejudge = async () => {
  if (!canClickRejudge.value) return
  try {
    await ElMessageBox.confirm(
      'Rejudge will enqueue this submission again. Continue?',
      'Confirm Rejudge',
      {
        type: 'warning',
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel'
      }
    )
  } catch (error) {
    return
  }
  loading.value = true
  try {
    await submissionApi.rejudgeSubmission(route.params.id)
    ElMessage.success('Rejudge queued')
    await loadSubmission()
  } catch (error) {
    ElMessage.error(error?.message || 'Rejudge failed')
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

const goBack = () => {
  router.push('/submissions')
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

.status-wrap {
  margin-top: 2px;
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

.section-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.steps {
  margin-top: 10px;
}

.timeline-hint {
  margin-top: 10px;
  font-size: 13px;
  color: #64748b;
}

.submission-info {
  margin-top: 8px;
}

.code-wrapper,
.error-wrapper {
  border: 1px solid #d7e1ee;
  border-radius: 8px;
  overflow: hidden;
}

.code-block {
  margin: 0;
  padding: 20px;
  overflow-x: auto;
  background: #1f2937;
  color: #e5e7eb;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.65;
}

.error-panel {
  border-color: #f8d7da;
  background: linear-gradient(180deg, #fffefe 0%, #fff7f7 100%);
}

.error-title {
  color: #c53030;
}

.error-block {
  margin: 0;
  padding: 16px 18px;
  overflow-x: auto;
  background: #fef2f2;
  color: #b42318;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.65;
}

@media (max-width: 980px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
