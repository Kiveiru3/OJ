<template>
  <div class="contest-list-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><Medal /></el-icon>
            <div class="title-stack">
              <h2 class="pro-title-text">竞赛列表</h2>
              <span class="title-sub">当前查询共 {{ pagination.total }} 场竞赛</span>
            </div>
          </div>
          <el-button v-if="canManage" type="primary" :icon="Plus" @click="openCreateDialog">
            新建竞赛
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form pro-filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="竞赛标题"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">当前页赛事</div>
          <div class="overview-value">{{ contestStats.total }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">进行中</div>
          <div class="overview-value">{{ contestStats.running }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">未开始</div>
          <div class="overview-value">{{ contestStats.upcoming }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">已报名</div>
          <div class="overview-value">{{ contestStats.joined }}</div>
        </div>
      </div>

      <el-skeleton v-if="loading && !contestList.length" :rows="7" animated />
      <div v-else class="pro-table-shell">
        <el-table v-loading="loading" :data="contestList" stripe class="contest-table">
          <el-table-column prop="id" label="ID" width="90" align="center" />
          <el-table-column prop="title" label="标题" min-width="220">
            <template #default="{ row }">
              <el-link type="primary" @click="goToDetail(row.id)">
                {{ row.title }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getContestStatusType(row.contestStatus)">
                {{ getContestStatusText(row.contestStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="比赛时间" min-width="320">
            <template #default="{ row }">
              {{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}
            </template>
          </el-table-column>
          <el-table-column label="封榜时间" min-width="170">
            <template #default="{ row }">
              {{ formatDateTime(row.scoreboardFreezeTime) }}
            </template>
          </el-table-column>
          <el-table-column label="错误罚时" width="110" align="center">
            <template #default="{ row }">
              {{ row.penaltyPerWrong ?? 20 }} 分
            </template>
          </el-table-column>
          <el-table-column prop="problemCount" label="题目数" width="110" align="center" />
          <el-table-column prop="participantCount" label="参赛人数" width="120" align="center" />
          <el-table-column label="是否报名" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.joined" type="success">已报名</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="goToDetail(row.id)">详情</el-button>
              <el-button
                v-if="!row.joined && row.contestStatus !== 'ENDED'"
                type="success"
                link
                @click="handleJoin(row)"
              >
                报名
              </el-button>
              <el-button v-if="canManage" type="warning" link @click="openEditDialog(row)">
                编辑
              </el-button>
              <el-button
                v-if="canManage && canDeleteContest(row)"
                type="danger"
                link
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无竞赛" :image-size="80" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="pageSizeOptions"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="loadContests"
        />
      </div>
    </el-card>

      <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑竞赛' : '新建竞赛'"
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
        <el-button type="primary" :loading="submitting" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { contestApi, problemApi } from '@/api'
import { useSystemStore } from '@/store/system'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Medal, Plus, Search, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()
const pageSizeOptions = [10, 20, 50]

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const contestList = ref([])
const problemOptions = ref([])
const formRef = ref(null)

const canManage = computed(() => userStore.isTeacher || userStore.isAdmin)

const searchForm = reactive({
  keyword: ''
})

const pagination = reactive({
  page: 1,
  size: normalizePageSize(systemStore.contestDefaultPageSize),
  total: 0
})

const form = reactive({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  scoreboardFreezeTime: '',
  penaltyPerWrong: Number(systemStore.contestDefaultPenaltyPerWrong || 20),
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

function normalizePageSize(size) {
  return pageSizeOptions.includes(size) ? size : 10
}

function getDefaultPenaltyPerWrong() {
  const value = Number(systemStore.contestDefaultPenaltyPerWrong)
  if (!Number.isFinite(value)) return 20
  if (value < 0) return 0
  if (value > 120) return 120
  return Math.floor(value)
}

const contestStats = computed(() => {
  const list = contestList.value || []
  if (!list.length) {
    return {
      total: 0,
      running: 0,
      upcoming: 0,
      joined: 0
    }
  }
  return {
    total: list.length,
    running: list.filter((row) => row.contestStatus === 'RUNNING').length,
    upcoming: list.filter((row) => row.contestStatus === 'UPCOMING').length,
    joined: list.filter((row) => Boolean(row.joined)).length
  }
})

const canDeleteContest = (row) => {
  if (!row) return false
  if (userStore.isAdmin) return true
  return Number(row.creatorId) > 0 && Number(row.creatorId) === Number(userStore.userInfo?.id)
}

const loadContests = async () => {
  loading.value = true
  try {
    const res = await contestApi.getContestList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchForm.keyword.trim() || undefined
    })
    contestList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载竞赛列表失败')
  } finally {
    loading.value = false
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

const resetForm = () => {
  editingId.value = null
  form.title = ''
  form.description = ''
  form.startTime = ''
  form.endTime = ''
  form.scoreboardFreezeTime = ''
  form.penaltyPerWrong = getDefaultPenaltyPerWrong()
  form.status = 1
  form.problemIds = []
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  try {
    const res = await contestApi.getContestDetail(row.id)
    const detail = res.data || {}
    editingId.value = row.id
    form.title = detail.title || ''
    form.description = detail.description || ''
    form.startTime = detail.startTime || ''
    form.endTime = detail.endTime || ''
    form.scoreboardFreezeTime = detail.scoreboardFreezeTime || ''
    form.penaltyPerWrong = Number.isFinite(Number(detail.penaltyPerWrong))
      ? Number(detail.penaltyPerWrong)
      : getDefaultPenaltyPerWrong()
    form.status = Number(detail.status ?? 1)
    form.problemIds = Array.isArray(detail.problems) ? detail.problems.map((item) => item.id) : []
    formRef.value?.clearValidate()
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '加载竞赛详情失败')
  }
}

const buildPayload = () => ({
  title: form.title.trim(),
  description: form.description?.trim() || '',
  startTime: form.startTime,
  endTime: form.endTime,
  scoreboardFreezeTime: form.scoreboardFreezeTime || null,
  penaltyPerWrong: Number(form.penaltyPerWrong),
  status: Number(form.status),
  problemIds: form.problemIds.map((id) => Number(id))
})

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = buildPayload()
      if (editingId.value) {
        await contestApi.updateContest(editingId.value, payload)
        ElMessage.success('竞赛更新成功')
      } else {
        await contestApi.createContest(payload)
        ElMessage.success('竞赛创建成功')
      }
      dialogVisible.value = false
      await loadContests()
    } catch (error) {
      ElMessage.error(error.message || '保存失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleJoin = async (row) => {
  try {
    await contestApi.joinContest(row.id)
    ElMessage.success('报名成功')
    await loadContests()
  } catch (error) {
    ElMessage.error(error.message || '报名失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除竞赛「${row.title}」吗？删除后不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消'
      }
    )
    await contestApi.deleteContest(row.id)
    ElMessage.success('竞赛已删除')
    await loadContests()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '删除失败')
  }
}

const goToDetail = (id) => {
  router.push(`/contest/${id}`)
}

const handleSearch = () => {
  pagination.page = 1
  loadContests()
}

const resetSearch = () => {
  searchForm.keyword = ''
  pagination.page = 1
  loadContests()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  loadContests()
}

onMounted(async () => {
  await systemStore.ensureLoaded().catch(() => null)
  pagination.size = normalizePageSize(systemStore.contestDefaultPageSize)
  await Promise.all([loadContests(), loadProblemOptions()])
})
</script>

<style scoped>
.main-card {
  border-radius: 8px;
}

.search-form {
  margin-bottom: 14px;
}

.contest-table {
  margin-top: 0;
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

.pagination-container {
  margin-top: 22px;
}

.field-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
