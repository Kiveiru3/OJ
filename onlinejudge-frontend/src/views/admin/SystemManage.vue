<template>
  <div class="system-manage-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><Setting /></el-icon>
            <h2 class="pro-title-text">系统配置</h2>
          </div>
          <el-button type="primary" @click="openCreateConfigDialog">新建配置</el-button>
        </div>
      </template>

      <el-skeleton v-if="configLoading && !configList.length" :rows="6" animated />
      <div v-else class="pro-table-shell">
        <el-table v-loading="configLoading" :data="configList" stripe class="pro-table">
          <el-table-column prop="configKey" label="键" min-width="220" />
          <el-table-column prop="configValue" label="值" min-width="320" show-overflow-tooltip />
          <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
          <el-table-column prop="updateUserId" label="更新人ID" width="110" align="center" />
          <el-table-column prop="updateTime" label="更新时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditConfigDialog(row)">编辑</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="暂无配置数据" :image-size="80" /></template>
        </el-table>
      </div>
    </el-card>

    <el-card class="main-card card-shadow pro-main-card monitor-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><DataAnalysis /></el-icon>
            <h2 class="pro-title-text">运行监控</h2>
          </div>
          <el-button @click="loadMonitor">刷新</el-button>
        </div>
      </template>

      <el-skeleton v-if="monitorLoading" :rows="5" animated />
      <template v-else>
        <div class="monitor-grid">
          <div class="monitor-item">
            <div class="monitor-label">用户（启用/总数）</div>
            <div class="monitor-value">{{ monitor.enabledUsers || 0 }} / {{ monitor.totalUsers || 0 }}</div>
            <div class="monitor-sub">今日新增：{{ monitor.newUsersToday || 0 }}</div>
          </div>
          <div class="monitor-item">
            <div class="monitor-label">提交总数</div>
            <div class="monitor-value">{{ monitor.totalSubmissions || 0 }}</div>
            <div class="monitor-sub">通过 {{ monitor.acceptedSubmissions || 0 }}（{{ monitor.acceptanceRate ?? 0 }}%）</div>
          </div>
          <div class="monitor-item">
            <div class="monitor-label">竞赛（进行中/总数）</div>
            <div class="monitor-value">{{ monitor.runningContests || 0 }} / {{ monitor.totalContests || 0 }}</div>
            <div class="monitor-sub">24小时操作日志：{{ monitor.operationLogs24h || 0 }}</div>
          </div>
          <div class="monitor-item">
            <div class="monitor-label">评测队列</div>
            <div class="monitor-value">{{ monitor.pendingSubmissions || 0 }}</div>
            <div class="monitor-sub">
              <el-tag :type="getQueueTagType(monitor.queueStatus)">{{ getQueueStatusText(monitor.queueStatus) }}</el-tag>
              <span class="queue-age">最长等待：{{ monitor.oldestPendingMinutes || 0 }} 分钟</span>
            </div>
          </div>
        </div>

        <el-divider />

        <div class="runtime-grid">
          <div class="runtime-line"><span>运行时长</span><span>{{ formatDuration(monitor.uptimeSeconds || 0) }}</span></div>
          <div class="runtime-line"><span>JVM 内存</span><span>{{ monitor.jvmUsedMemoryMb || 0 }}MB / {{ monitor.jvmMaxMemoryMb || 0 }}MB</span></div>
          <el-progress :percentage="memoryUsagePercent" :stroke-width="10" />
          <div class="runtime-line"><span>线程数 / CPU核心数</span><span>{{ monitor.threadCount || 0 }} / {{ monitor.availableProcessors || 0 }}</span></div>
          <div class="runtime-line runtime-time"><span>生成时间</span><span>{{ formatDateTime(monitor.generatedAt) }}</span></div>
        </div>
      </template>
    </el-card>

    <el-card class="main-card card-shadow pro-main-card log-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><Document /></el-icon>
            <h2 class="pro-title-text">审计日志</h2>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="logQuery" class="search-form pro-filter-bar">
        <el-form-item label="模块">
          <el-select v-model="logQuery.module" placeholder="全部模块" clearable style="width: 180px">
            <el-option v-for="item in logModuleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="动作">
          <el-select v-model="logQuery.action" placeholder="全部动作" clearable style="width: 180px">
            <el-option v-for="item in logActionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="logQuery.keyword" placeholder="操作者/详情" clearable style="width: 220px" @keyup.enter="handleLogSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogSearch">搜索</el-button>
          <el-button @click="resetLogSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-skeleton v-if="logLoading && !logList.length" :rows="7" animated />
      <div v-else class="pro-table-shell">
        <el-table v-loading="logLoading" :data="logList" stripe class="pro-table">
          <el-table-column prop="id" label="ID" width="90" align="center" />
          <el-table-column label="模块" width="140">
            <template #default="{ row }"><el-tag size="small" effect="plain">{{ getModuleText(row.module) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="动作" width="150">
            <template #default="{ row }"><el-tag size="small" type="info" effect="plain">{{ getActionText(row.action) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作者" width="180">
            <template #default="{ row }">{{ row.operatorUsername || `#${row.operatorId || '-'}` }}</template>
          </el-table-column>
          <el-table-column label="目标类型" width="130">
            <template #default="{ row }">{{ getTargetTypeText(row.targetType) }}</template>
          </el-table-column>
          <el-table-column prop="targetId" label="目标ID" width="100" />
          <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
          <el-table-column label="时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <template #empty><el-empty description="暂无审计日志" :image-size="80" /></template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="logPagination.page"
          v-model:page-size="logPagination.size"
          :total="logPagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleLogPageSizeChange"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-card class="main-card card-shadow pro-main-card judge-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><Document /></el-icon>
            <h2 class="pro-title-text">评测结果</h2>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="judgeQuery" class="search-form pro-filter-bar">
        <el-form-item label="用户ID">
          <el-input v-model="judgeQuery.userId" clearable placeholder="例如 1" style="width: 140px" @keyup.enter="handleJudgeSearch" />
        </el-form-item>
        <el-form-item label="题目ID">
          <el-input v-model="judgeQuery.problemId" clearable placeholder="例如 1001" style="width: 140px" @keyup.enter="handleJudgeSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="judgeQuery.status" clearable placeholder="全部状态" style="width: 170px">
            <el-option v-for="item in judgeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-input v-model="judgeQuery.language" clearable placeholder="如 java / cpp / python" style="width: 180px" @keyup.enter="handleJudgeSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleJudgeSearch">搜索</el-button>
          <el-button @click="resetJudgeSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <el-skeleton v-if="judgeLoading && !judgeList.length" :rows="6" animated />
      <div v-else class="pro-table-shell">
        <el-table v-loading="judgeLoading" :data="judgeList" stripe class="pro-table">
          <el-table-column prop="id" label="ID" width="90" align="center" />
          <el-table-column prop="submissionId" label="提交ID" width="100" align="center" />
          <el-table-column label="用户" min-width="170">
            <template #default="{ row }">{{ row.username || `用户#${row.userId || '-'}` }}</template>
          </el-table-column>
          <el-table-column label="题目" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.problemTitle || `题目#${row.problemId || '-'}` }}</template>
          </el-table-column>
          <el-table-column prop="language" label="语言" width="110" />
          <el-table-column label="状态" width="160">
            <template #default="{ row }"><el-tag :type="getJudgeStatusTagType(row.status)" effect="plain">{{ row.status || '-' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="耗时(ms)" width="95" align="center">
            <template #default="{ row }">{{ row.timeUsed ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="内存(KB)" width="100" align="center">
            <template #default="{ row }">{{ row.memoryUsed ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="评测时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.judgeTime) }}</template>
          </el-table-column>
          <el-table-column label="错误信息" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">{{ row.errorMessage || '-' }}</template>
          </el-table-column>
          <template #empty><el-empty description="暂无评测结果" :image-size="80" /></template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="judgePagination.page"
          v-model:page-size="judgePagination.size"
          :total="judgePagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleJudgePageSizeChange"
          @current-change="loadJudgeResults"
        />
      </div>
    </el-card>

    <el-dialog v-model="configDialogVisible" :title="isEditMode ? '编辑配置' : '新建配置'" width="640px" :close-on-click-modal="false">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-width="100px">
        <el-form-item label="键" prop="configKey">
          <el-input v-model="configForm.configKey" :disabled="isEditMode" maxlength="100" />
        </el-form-item>
        <el-form-item label="值" prop="configValue">
          <el-input v-model="configForm.configValue" type="textarea" :rows="4" maxlength="5000" show-word-limit />
          <div v-if="configKeyHint" class="config-hint">{{ configKeyHint }}</div>
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="configForm.description" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="configSubmitting" @click="submitConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { systemApi } from '@/api'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Document, Setting } from '@element-plus/icons-vue'

const configLoading = ref(false)
const configSubmitting = ref(false)
const configList = ref([])
const configDialogVisible = ref(false)
const configFormRef = ref(null)

const logLoading = ref(false)
const logList = ref([])
const logQuery = reactive({ module: '', action: '', keyword: '' })
const logPagination = reactive({ page: 1, size: 20, total: 0 })

const judgeLoading = ref(false)
const judgeList = ref([])
const judgeQuery = reactive({ userId: '', problemId: '', status: '', language: '' })
const judgePagination = reactive({ page: 1, size: 20, total: 0 })

const monitorLoading = ref(false)
const monitor = ref({
  generatedAt: null,
  totalUsers: 0,
  enabledUsers: 0,
  newUsersToday: 0,
  totalSubmissions: 0,
  acceptedSubmissions: 0,
  pendingSubmissions: 0,
  acceptanceRate: 0,
  totalContests: 0,
  runningContests: 0,
  operationLogs24h: 0,
  oldestPendingMinutes: 0,
  queueStatus: 'NORMAL',
  uptimeSeconds: 0,
  jvmUsedMemoryMb: 0,
  jvmMaxMemoryMb: 0,
  threadCount: 0,
  availableProcessors: 0
})

const isEditMode = ref(false)
const configForm = reactive({ configKey: '', configValue: '', description: '' })

const CONFIG_HINT = {
  'site.name': '站点名称，1-80个字符',
  'site.announcement': '站点公告，最多2000个字符',
  'contest.default_page_size': '竞赛榜单默认分页，整数1-100',
  'contest.default_penalty_per_wrong': '竞赛错误罚时（分钟），整数0-120'
}

const configKeyHint = computed(() => CONFIG_HINT[String(configForm.configKey || '').trim()] || '')

const parseIntSafe = (value) => {
  const raw = String(value ?? '').trim()
  if (!/^-?\d+$/.test(raw)) return null
  return Number(raw)
}

const validateConfigValue = (_rule, value, callback) => {
  const key = String(configForm.configKey || '').trim()
  const raw = String(value ?? '')
  const trimmed = raw.trim()

  if (!key) return callback(new Error('请先填写配置键'))
  if (key === 'site.name') {
    if (!trimmed) return callback(new Error('site.name 不能为空'))
    if (trimmed.length > 80) return callback(new Error('site.name 长度不能超过80'))
  }
  if (key === 'site.announcement' && raw.length > 2000) return callback(new Error('site.announcement 长度不能超过2000'))
  if (key === 'contest.default_page_size') {
    const n = parseIntSafe(trimmed)
    if (n === null) return callback(new Error('contest.default_page_size 必须为整数'))
    if (n < 1 || n > 100) return callback(new Error('contest.default_page_size 必须在1-100之间'))
  }
  if (key === 'contest.default_penalty_per_wrong') {
    const n = parseIntSafe(trimmed)
    if (n === null) return callback(new Error('contest.default_penalty_per_wrong 必须为整数'))
    if (n < 0 || n > 120) return callback(new Error('contest.default_penalty_per_wrong 必须在0-120之间'))
  }
  callback()
}

const configRules = {
  configKey: [{ required: true, message: '请输入配置键', trigger: 'blur' }],
  configValue: [{ validator: validateConfigValue, trigger: ['blur', 'change'] }]
}

const judgeStatusOptions = [
  { label: '等待中(PENDING)', value: 'PENDING' },
  { label: '评测中(JUDGING)', value: 'JUDGING' },
  { label: '通过(ACCEPTED)', value: 'ACCEPTED' },
  { label: '答案错误(WRONG_ANSWER)', value: 'WRONG_ANSWER' },
  { label: '编译错误(COMPILE_ERROR)', value: 'COMPILE_ERROR' },
  { label: '运行错误(RUNTIME_ERROR)', value: 'RUNTIME_ERROR' },
  { label: '超时(TIME_LIMIT_EXCEEDED)', value: 'TIME_LIMIT_EXCEEDED' },
  { label: '内存超限(MEMORY_LIMIT_EXCEEDED)', value: 'MEMORY_LIMIT_EXCEEDED' }
]

const logModuleOptions = [
  { label: '认证(AUTH)', value: 'AUTH' },
  { label: '用户管理(USER_MANAGE)', value: 'USER_MANAGE' },
  { label: '题目管理(PROBLEM_MANAGE)', value: 'PROBLEM_MANAGE' },
  { label: '提交(SUBMISSION)', value: 'SUBMISSION' },
  { label: '竞赛(CONTEST)', value: 'CONTEST' },
  { label: '讨论(DISCUSSION)', value: 'DISCUSSION' },
  { label: '教学分析(ANALYTICS)', value: 'ANALYTICS' },
  { label: '系统配置(SYSTEM_CONFIG)', value: 'SYSTEM_CONFIG' },
  { label: '审计日志(AUDIT)', value: 'AUDIT' },
  { label: '系统监控(SYSTEM_MONITOR)', value: 'SYSTEM_MONITOR' }
]

const logActionOptions = [
  { label: '创建(CREATE)', value: 'CREATE' },
  { label: '更新(UPDATE)', value: 'UPDATE' },
  { label: '删除(DELETE)', value: 'DELETE' },
  { label: '查询(QUERY)', value: 'QUERY' },
  { label: '列表(LIST)', value: 'LIST' },
  { label: '详情(DETAIL)', value: 'DETAIL' },
  { label: '登录(LOGIN)', value: 'LOGIN' },
  { label: '登出(LOGOUT)', value: 'LOGOUT' },
  { label: '提交(SUBMIT)', value: 'SUBMIT' },
  { label: '写入(UPSERT)', value: 'UPSERT' },
  { label: '查看(VIEW)', value: 'VIEW' }
]

const MODULE_TEXT_MAP = {
  AUTH: '认证',
  USER_MANAGE: '用户管理',
  PROBLEM_MANAGE: '题目管理',
  SUBMISSION: '提交',
  CONTEST: '竞赛',
  DISCUSSION: '讨论',
  ANALYTICS: '教学分析',
  SYSTEM_CONFIG: '系统配置',
  SYSTEM_MONITOR: '系统监控',
  AUDIT: '审计'
}

const ACTION_TEXT_MAP = {
  CREATE: '创建',
  UPDATE: '更新',
  DELETE: '删除',
  QUERY: '查询',
  LIST: '列表',
  DETAIL: '详情',
  LOGIN: '登录',
  LOGOUT: '登出',
  SUBMIT: '提交',
  UPSERT: '写入',
  VIEW: '查看'
}

const TARGET_TYPE_TEXT_MAP = {
  USER: '用户',
  PROBLEM: '题目',
  TEST_CASE: '测试用例',
  SUBMISSION: '提交',
  CONTEST: '竞赛',
  CONTEST_RANKING: '竞赛排名',
  DISCUSSION_POST: '讨论帖',
  DISCUSSION_COMMENT: '评论',
  SYSTEM_CONFIG: '系统配置',
  OPERATION_LOG: '操作日志',
  MONITOR: '系统监控',
  CONFIG: '配置'
}

const memoryUsagePercent = computed(() => {
  const max = Number(monitor.value.jvmMaxMemoryMb || 0)
  const used = Number(monitor.value.jvmUsedMemoryMb || 0)
  if (max <= 0) return 0
  return Math.min(100, Math.round((used * 1000) / max) / 10)
})

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

const formatDuration = (seconds) => {
  const total = Math.max(0, Number(seconds || 0))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = Math.floor(total % 60)
  return `${h}时 ${m}分 ${s}秒`
}

const getQueueTagType = (status) => (status === 'CRITICAL' ? 'danger' : status === 'WARNING' ? 'warning' : 'success')
const getQueueStatusText = (status) => (status === 'CRITICAL' ? '严重' : status === 'WARNING' ? '告警' : '正常')
const getJudgeStatusTagType = (status) => {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'PENDING' || status === 'JUDGING') return 'warning'
  if (status === 'WRONG_ANSWER' || status === 'COMPILE_ERROR' || status === 'RUNTIME_ERROR') return 'danger'
  return 'info'
}
const getModuleText = (module) => MODULE_TEXT_MAP[String(module || '').trim().toUpperCase()] || String(module || '-')
const getActionText = (action) => ACTION_TEXT_MAP[String(action || '').trim().toUpperCase()] || String(action || '-')
const getTargetTypeText = (targetType) => TARGET_TYPE_TEXT_MAP[String(targetType || '').trim().toUpperCase()] || String(targetType || '-')

const parseOptionalPositiveNumber = (value) => {
  const raw = String(value ?? '').trim()
  if (!raw) return undefined
  const n = Number(raw)
  return Number.isFinite(n) && n > 0 ? n : undefined
}

const buildLogParams = () => {
  const params = { page: logPagination.page, size: logPagination.size }
  if (logQuery.module.trim()) params.module = logQuery.module.trim()
  if (logQuery.action.trim()) params.action = logQuery.action.trim()
  if (logQuery.keyword.trim()) params.keyword = logQuery.keyword.trim()
  return params
}

const buildJudgeParams = () => {
  const params = { page: judgePagination.page, size: judgePagination.size }
  const userId = parseOptionalPositiveNumber(judgeQuery.userId)
  const problemId = parseOptionalPositiveNumber(judgeQuery.problemId)
  if (userId) params.userId = userId
  if (problemId) params.problemId = problemId
  if (judgeQuery.status.trim()) params.status = judgeQuery.status.trim()
  if (judgeQuery.language.trim()) params.language = judgeQuery.language.trim()
  return params
}

const loadConfigs = async () => {
  configLoading.value = true
  try {
    const res = await systemApi.getConfigs()
    configList.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载系统配置失败')
  } finally {
    configLoading.value = false
  }
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await systemApi.getLogs(buildLogParams())
    logList.value = res.data?.records || []
    logPagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载审计日志失败')
  } finally {
    logLoading.value = false
  }
}

const loadJudgeResults = async () => {
  judgeLoading.value = true
  try {
    const res = await systemApi.getJudgeResults(buildJudgeParams())
    judgeList.value = res.data?.records || []
    judgePagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载评测结果失败')
  } finally {
    judgeLoading.value = false
  }
}

const loadMonitor = async () => {
  monitorLoading.value = true
  try {
    const res = await systemApi.getMonitor()
    monitor.value = { ...monitor.value, ...(res.data || {}) }
  } catch (error) {
    ElMessage.error(error.message || '加载运行监控失败')
  } finally {
    monitorLoading.value = false
  }
}

const resetConfigForm = () => {
  configForm.configKey = ''
  configForm.configValue = ''
  configForm.description = ''
  configFormRef.value?.clearValidate()
}

const openCreateConfigDialog = () => {
  isEditMode.value = false
  resetConfigForm()
  configDialogVisible.value = true
}

const openEditConfigDialog = (row) => {
  isEditMode.value = true
  configForm.configKey = row.configKey || ''
  configForm.configValue = row.configValue || ''
  configForm.description = row.description || ''
  configFormRef.value?.clearValidate()
  configDialogVisible.value = true
}

const submitConfig = async () => {
  if (!configFormRef.value) return
  try {
    await configFormRef.value.validate()
  } catch (_) {
    return
  }

  const key = configForm.configKey.trim()
  const shouldTrimValue = ['site.name', 'contest.default_page_size', 'contest.default_penalty_per_wrong'].includes(key)
  const normalizedValue = shouldTrimValue ? String(configForm.configValue ?? '').trim() : String(configForm.configValue ?? '')

  configSubmitting.value = true
  try {
    await systemApi.upsertConfig({
      configKey: key,
      configValue: normalizedValue,
      description: configForm.description?.trim() || ''
    })
    configDialogVisible.value = false
    ElMessage.success('配置保存成功')
    await Promise.all([loadConfigs(), loadLogs()])
  } catch (error) {
    ElMessage.error(error.message || '配置保存失败')
  } finally {
    configSubmitting.value = false
  }
}

const handleLogSearch = () => {
  logPagination.page = 1
  loadLogs()
}
const resetLogSearch = () => {
  logQuery.module = ''
  logQuery.action = ''
  logQuery.keyword = ''
  logPagination.page = 1
  loadLogs()
}
const handleLogPageSizeChange = () => {
  logPagination.page = 1
  loadLogs()
}

const handleJudgeSearch = () => {
  judgePagination.page = 1
  loadJudgeResults()
}
const resetJudgeSearch = () => {
  judgeQuery.userId = ''
  judgeQuery.problemId = ''
  judgeQuery.status = ''
  judgeQuery.language = ''
  judgePagination.page = 1
  loadJudgeResults()
}
const handleJudgePageSizeChange = () => {
  judgePagination.page = 1
  loadJudgeResults()
}

onMounted(async () => {
  await Promise.all([loadConfigs(), loadMonitor(), loadLogs(), loadJudgeResults()])
})
</script>

<style scoped>
.system-manage-container {
  max-width: 1400px;
  margin: 0 auto;
}
.main-card {
  border-radius: 8px;
}
.monitor-card,
.log-card,
.judge-card {
  margin-top: 16px;
}
.monitor-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}
.monitor-item {
  border: 1px solid #e8edf3;
  border-radius: 8px;
  background: #fafcff;
  padding: 14px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}
.monitor-item:hover {
  border-color: #d7e6ff;
  box-shadow: 0 8px 18px rgba(22, 119, 255, 0.1);
  transform: translateY(-1px);
}
.monitor-label {
  font-size: 12px;
  color: #909399;
}
.monitor-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 600;
  color: #111827;
}
.monitor-sub {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;
}
.queue-age {
  color: #909399;
}
.runtime-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.runtime-line {
  display: flex;
  justify-content: space-between;
  color: #4b5563;
}
.runtime-time {
  color: #909399;
}
.config-hint {
  margin-top: 6px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}
.pro-table {
  border: 1px solid #edf1f5;
  border-radius: 8px;
}
.pro-table :deep(.el-table__row td.el-table__cell) {
  transition: background-color 0.2s ease;
}
.pro-table :deep(.el-table__row:hover td.el-table__cell) {
  background: #f7faff !important;
}
.pro-table :deep(.el-empty) {
  padding: 22px 0;
}
@media (max-width: 1080px) {
  .monitor-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
