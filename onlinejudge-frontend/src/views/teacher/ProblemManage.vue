<template>
  <div class="problem-manage-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><Edit /></el-icon>
            <h2 class="pro-title-text">题目管理</h2>
          </div>
          <el-button type="primary" @click="handleCreate" :icon="Plus" size="large">
            新建题目
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form pro-filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="题目标题 / 描述"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="searchForm.difficulty" clearable placeholder="全部" style="width: 140px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性">
          <el-select v-model="searchForm.scope" style="width: 160px">
            <el-option label="全部(含隐藏)" value="all" />
            <el-option label="仅公开" value="public" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="pro-table-shell">
        <el-table
          v-loading="loading"
          :data="problemList"
          stripe
          class="problem-table"
          :row-class-name="getRowClassName"
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="title" label="题目标题" min-width="220" />
          <el-table-column prop="difficulty" label="难度" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getDifficultyType(row.difficulty)" effect="dark">
                {{ getDifficultyText(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="acceptCount" label="通过数" width="100" align="center" />
          <el-table-column prop="submitCount" label="提交数" width="100" align="center" />
          <el-table-column label="操作" width="280" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)" :icon="Edit">编辑</el-button>
              <el-button size="small" type="warning" @click="openTestCaseDialog(row)" :icon="Tickets">
                测试用例
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)" :icon="Delete">
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无题目" :image-size="80" />
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
          @size-change="loadProblems"
          @current-change="loadProblems"
        />
      </div>
    </el-card>

    <el-card class="log-card card-shadow pro-main-card">
      <template #header>
        <div class="log-header">
          <span>最近操作日志</span>
          <div class="log-actions">
            <el-button
              type="primary"
              link
              :disabled="!operationLogs.length"
              @click="copyOperationLogs"
            >
              复制
            </el-button>
            <el-button
              type="primary"
              link
              :disabled="!operationLogs.length"
              @click="exportOperationLogs"
            >
              导出JSON
            </el-button>
            <el-button
              type="danger"
              link
              :disabled="!operationLogs.length"
              @click="clearOperationLogs"
            >
              清空
            </el-button>
          </div>
        </div>
      </template>
      <el-empty v-if="!operationLogs.length" description="暂无操作记录" />
      <el-timeline v-else class="log-timeline">
        <el-timeline-item
          v-for="log in operationLogs"
          :key="log.id"
          :timestamp="log.time"
          :type="getLogType(log.action)"
        >
          {{ formatLogText(log) }}
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="problem-form">
        <el-form-item label="题目标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入题目标题" />
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="form.difficulty" style="width: 200px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="6" placeholder="请输入题目描述" />
        </el-form-item>
        <el-form-item label="输入格式">
          <el-input v-model="form.inputFormat" type="textarea" :rows="3" placeholder="请输入输入格式" />
        </el-form-item>
        <el-form-item label="输出格式">
          <el-input v-model="form.outputFormat" type="textarea" :rows="3" placeholder="请输入输出格式" />
        </el-form-item>
        <el-form-item label="样例输入">
          <el-input v-model="form.sampleInput" type="textarea" :rows="3" placeholder="请输入样例输入" />
        </el-form-item>
        <el-form-item label="样例输出">
          <el-input v-model="form.sampleOutput" type="textarea" :rows="3" placeholder="请输入样例输出" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="时间限制(ms)">
              <el-input-number
                v-model="form.timeLimit"
                :min="1000"
                :max="10000"
                :step="1000"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内存限制(KB)">
              <el-input-number
                v-model="form.memoryLimit"
                :min="128"
                :max="512000"
                :step="128"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="testCaseDialogVisible"
      :title="`测试用例管理 - #${currentProblemId || '-'}`"
      width="900px"
      :close-on-click-modal="false"
      @closed="handleTestCaseDialogClosed"
    >
      <div class="test-case-toolbar">
        <el-button size="small" :icon="Refresh" @click="loadTestCases" :loading="testCaseLoading">
          刷新
        </el-button>
      </div>

      <el-table v-loading="testCaseLoading" :data="testCaseList" stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="input" label="输入" min-width="280" show-overflow-tooltip />
        <el-table-column prop="output" label="输出" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="editTestCase(row)" :icon="Edit">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTestCase(row)" :icon="Delete">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-divider />

      <el-form
        ref="testCaseFormRef"
        :model="testCaseForm"
        :rules="testCaseRules"
        label-width="80px"
        class="test-case-form"
      >
        <el-form-item label="输入" prop="input">
          <el-input v-model="testCaseForm.input" type="textarea" :rows="3" placeholder="请输入测试输入" />
        </el-form-item>
        <el-form-item label="输出" prop="output">
          <el-input v-model="testCaseForm.output" type="textarea" :rows="3" placeholder="请输入期望输出" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitTestCase" :loading="testCaseSubmitting">
            {{ testCaseForm.id ? '更新用例' : '新增用例' }}
          </el-button>
          <el-button @click="resetTestCaseForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { problemApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus, Delete, Tickets, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const formRef = ref(null)
const editingId = ref(null)
const problemList = ref([])
const highlightedProblemId = ref(null)
const highlightTimer = ref(null)
const operationLogs = ref([])
const searchForm = reactive({
  keyword: '',
  difficulty: '',
  scope: 'all'
})
const LOG_STORAGE_KEY = 'oj:teacher:problem-manage-logs'

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  title: '',
  difficulty: 'EASY',
  status: 1,
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  timeLimit: 2000,
  memoryLimit: 256000
})

const rules = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }]
}

const dialogTitle = computed(() => (editingId.value ? '编辑题目' : '新建题目'))

const getDifficultyType = (difficulty) => {
  const map = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return map[difficulty] || ''
}

const getDifficultyText = (difficulty) => {
  const map = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[difficulty] || difficulty
}

const getStatusType = (status) => (Number(status) === 1 ? 'success' : 'info')
const getStatusText = (status) => (Number(status) === 1 ? '公开' : '隐藏')

const loadProblems = async () => {
  loading.value = true
  try {
    const keyword = searchForm.keyword.trim()
    const includeHidden = searchForm.scope === 'all'
    const res = await problemApi.getProblemList({
      page: pagination.page,
      size: pagination.size,
      includeHidden,
      keyword: keyword || undefined,
      difficulty: searchForm.difficulty || undefined
    })
    problemList.value = res.data.records || res.data.list || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载题目列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadProblems()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.difficulty = ''
  searchForm.scope = 'all'
  pagination.page = 1
  loadProblems()
}

const handleCreate = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title || '',
    difficulty: row.difficulty || 'EASY',
    status: Number(row.status ?? 1),
    description: row.description || '',
    inputFormat: row.inputFormat || '',
    outputFormat: row.outputFormat || '',
    sampleInput: row.sampleInput || '',
    sampleOutput: row.sampleOutput || '',
    timeLimit: row.timeLimit || 2000,
    memoryLimit: row.memoryLimit || 256000
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这道题目吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await problemApi.deleteProblem(row.id)
    ElMessage.success('删除成功')
    appendOperationLog('DELETE', row.id, row.title)
    loadProblems()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      let changedId = null
      if (editingId.value) {
        await problemApi.updateProblem(editingId.value, form)
        changedId = editingId.value
        ElMessage.success('更新成功')
        appendOperationLog('UPDATE', changedId, form.title)
      } else {
        const createRes = await problemApi.createProblem(form)
        changedId = Number(createRes?.data) || null
        ElMessage.success('创建成功')
        appendOperationLog('CREATE', changedId, form.title)
      }
      dialogVisible.value = false
      markProblemChanged(changedId)
      loadProblems()
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  Object.assign(form, {
    title: '',
    difficulty: 'EASY',
    status: 1,
    description: '',
    inputFormat: '',
    outputFormat: '',
    sampleInput: '',
    sampleOutput: '',
    timeLimit: 2000,
    memoryLimit: 256000
  })
  formRef.value?.resetFields()
}

const testCaseDialogVisible = ref(false)
const testCaseLoading = ref(false)
const testCaseSubmitting = ref(false)
const currentProblemId = ref(null)
const testCaseList = ref([])
const testCaseFormRef = ref(null)
const testCaseForm = reactive({
  id: null,
  input: '',
  output: ''
})

const testCaseRules = {
  input: [{ required: true, message: '请输入测试输入', trigger: 'blur' }],
  output: [{ required: true, message: '请输入期望输出', trigger: 'blur' }]
}

const openTestCaseDialog = (row) => {
  currentProblemId.value = row.id
  testCaseDialogVisible.value = true
  resetTestCaseForm()
  loadTestCases()
}

const loadTestCases = async () => {
  if (!currentProblemId.value) return
  testCaseLoading.value = true
  try {
    const res = await problemApi.getProblemTestCases(currentProblemId.value)
    testCaseList.value = res.data || []
  } catch (error) {
    ElMessage.error(error.message || '加载测试用例失败')
  } finally {
    testCaseLoading.value = false
  }
}

const editTestCase = (row) => {
  testCaseForm.id = row.id
  testCaseForm.input = row.input || ''
  testCaseForm.output = row.output || ''
}

const resetTestCaseForm = () => {
  testCaseForm.id = null
  testCaseForm.input = ''
  testCaseForm.output = ''
  testCaseFormRef.value?.clearValidate()
}

const submitTestCase = async () => {
  if (!currentProblemId.value || !testCaseFormRef.value) return
  await testCaseFormRef.value.validate(async (valid) => {
    if (!valid) return
    testCaseSubmitting.value = true
    try {
      const payload = {
        input: testCaseForm.input,
        output: testCaseForm.output
      }
      if (testCaseForm.id) {
        await problemApi.updateTestCase(currentProblemId.value, testCaseForm.id, payload)
        ElMessage.success('测试用例更新成功')
      } else {
        await problemApi.createTestCase(currentProblemId.value, payload)
        ElMessage.success('测试用例新增成功')
      }
      resetTestCaseForm()
      loadTestCases()
    } catch (error) {
      ElMessage.error(error.message || '保存测试用例失败')
    } finally {
      testCaseSubmitting.value = false
    }
  })
}

const deleteTestCase = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该测试用例吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await problemApi.deleteTestCase(currentProblemId.value, row.id)
    ElMessage.success('删除成功')
    loadTestCases()
    if (testCaseForm.id === row.id) {
      resetTestCaseForm()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除测试用例失败')
    }
  }
}

const handleTestCaseDialogClosed = () => {
  currentProblemId.value = null
  testCaseList.value = []
  resetTestCaseForm()
}

const getRowClassName = ({ row }) => {
  if (highlightedProblemId.value && row.id === highlightedProblemId.value) {
    return 'row-highlight'
  }
  return ''
}

const markProblemChanged = (problemId) => {
  highlightedProblemId.value = problemId
  if (highlightTimer.value) {
    clearTimeout(highlightTimer.value)
  }
  highlightTimer.value = setTimeout(() => {
    highlightedProblemId.value = null
    highlightTimer.value = null
  }, 4000)
}

const readOperationLogs = () => {
  const raw = localStorage.getItem(LOG_STORAGE_KEY)
  if (!raw) {
    operationLogs.value = []
    return
  }
  try {
    const parsed = JSON.parse(raw)
    operationLogs.value = Array.isArray(parsed) ? parsed : []
  } catch (error) {
    operationLogs.value = []
  }
}

const writeOperationLogs = () => {
  localStorage.setItem(LOG_STORAGE_KEY, JSON.stringify(operationLogs.value))
}

const appendOperationLog = (action, problemId, title) => {
  const entry = {
    id: `${Date.now()}-${Math.random().toString(16).slice(2, 8)}`,
    action,
    problemId: problemId || null,
    title: title || '',
    time: new Date().toLocaleString('zh-CN', { hour12: false })
  }
  operationLogs.value = [entry, ...operationLogs.value].slice(0, 20)
  writeOperationLogs()
}

const clearOperationLogs = () => {
  operationLogs.value = []
  writeOperationLogs()
}

const copyOperationLogs = async () => {
  if (!operationLogs.value.length) {
    ElMessage.warning('暂无可复制的日志')
    return
  }
  const text = operationLogs.value
    .map((log) => `${log.time} ${formatLogText(log)}`)
    .join('\n')
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('日志已复制')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const exportOperationLogs = () => {
  if (!operationLogs.value.length) {
    ElMessage.warning('暂无可导出的日志')
    return
  }
  const content = JSON.stringify(operationLogs.value, null, 2)
  const blob = new Blob([content], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  const timestamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')
  a.href = url
  a.download = `teacher-problem-logs-${timestamp}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}

const getLogType = (action) => {
  const map = {
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger'
  }
  return map[action] || 'info'
}

const getLogActionText = (action) => {
  const map = {
    CREATE: '创建',
    UPDATE: '编辑',
    DELETE: '删除'
  }
  return map[action] || action
}

const formatLogText = (log) => {
  const idText = log.problemId ? `#${log.problemId}` : '#-'
  const titleText = log.title ? ` ${log.title}` : ''
  return `【${getLogActionText(log.action)}】${idText}${titleText}`
}

onMounted(() => {
  readOperationLogs()
  loadProblems()
})

onBeforeUnmount(() => {
  if (highlightTimer.value) {
    clearTimeout(highlightTimer.value)
  }
})
</script>

<style scoped>
.problem-manage-container {
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
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 20px;
  color: #1677ff;
}

.card-header h2 {
  margin: 0;
}

.problem-table {
  margin-top: 0;
}

.search-form {
  margin-bottom: 14px;
}

.pagination-container {
  margin-top: 22px;
}

.problem-form {
  padding: 20px 0;
}

.test-case-toolbar {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}

.test-case-form {
  margin-top: 16px;
}

:deep(.row-highlight > td) {
  background: #ecfdf3 !important;
}

.log-card {
  margin-top: 16px;
  border-radius: 8px;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
  color: #303133;
}

.log-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-timeline {
  max-height: 320px;
  overflow: auto;
  padding-right: 8px;
}
</style>
