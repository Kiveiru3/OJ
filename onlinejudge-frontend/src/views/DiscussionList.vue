<template>
  <div class="discussion-list-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><ChatDotRound /></el-icon>
            <div class="title-stack">
              <h2 class="pro-title-text">讨论区</h2>
              <span class="title-sub">当前查询共 {{ pagination.total }} 个帖子</span>
            </div>
          </div>
          <el-button type="primary" :icon="Plus" @click="openCreateDialog">
            发布帖子
          </el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form pro-filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="标题或内容"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="题目 ID">
          <el-input
            v-model="searchForm.problemId"
            placeholder="例如 1001"
            clearable
            style="width: 140px"
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
          <div class="overview-label">当前页帖子</div>
          <div class="overview-value">{{ pageStats.total }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">关联题目帖</div>
          <div class="overview-value">{{ pageStats.withProblem }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">总浏览量</div>
          <div class="overview-value">{{ pageStats.viewCount }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">你可管理</div>
          <div class="overview-value">{{ pageStats.canManage }}</div>
        </div>
      </div>

      <el-skeleton v-if="loading && !postList.length" :rows="7" animated />
      <div v-else class="pro-table-shell">
        <el-table v-loading="loading" :data="postList" stripe class="discussion-table">
          <el-table-column prop="id" label="ID" width="90" align="center" />
          <el-table-column label="标题" min-width="280">
            <template #default="{ row }">
              <el-link type="primary" @click="goToDetail(row.id)">
                {{ row.title }}
              </el-link>
              <div class="preview-text">{{ row.contentPreview }}</div>
            </template>
          </el-table-column>
          <el-table-column label="关联题目" width="220">
            <template #default="{ row }">
              <el-link v-if="row.problemId" type="primary" @click="goToProblem(row.problemId)">
                #{{ row.problemId }} {{ row.problemTitle || '' }}
              </el-link>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="作者" width="150" align="center">
            <template #default="{ row }">
              {{ row.nickname || row.username || `用户#${row.userId}` }}
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览量" width="100" align="center" />
          <el-table-column label="发布时间" width="180" align="center">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="center" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="goToDetail(row.id)">详情</el-button>
              <el-button v-if="canDelete(row)" type="danger" link @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂时还没有讨论帖子" :image-size="80" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="loadPosts"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="发布讨论帖"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="关联题目">
          <el-select
            v-model="form.problemId"
            clearable
            filterable
            style="width: 100%"
            placeholder="可选：关联一道题目"
          >
            <el-option
              v-for="item in problemOptions"
              :key="item.id"
              :label="`#${item.id} ${item.title}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            maxlength="10000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPost">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { discussionApi, problemApi } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Plus, Search, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const postList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)
const problemOptions = ref([])

const searchForm = reactive({
  keyword: '',
  problemId: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const form = reactive({
  title: '',
  problemId: null,
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 1, max: 120, message: '长度应为 1-120 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 1, max: 10000, message: '长度应为 1-10000 个字符', trigger: 'blur' }
  ]
}

const pageStats = computed(() => {
  const list = postList.value || []
  if (!list.length) {
    return {
      total: 0,
      withProblem: 0,
      viewCount: 0,
      canManage: 0
    }
  }
  return {
    total: list.length,
    withProblem: list.filter((item) => Number(item.problemId) > 0).length,
    viewCount: list.reduce((sum, item) => sum + Number(item.viewCount || 0), 0),
    canManage: list.filter((item) => canDelete(item)).length
  }
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

const canDelete = (row) => userStore.isAdmin || Boolean(row.editable)

const buildParams = () => {
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (searchForm.keyword.trim()) {
    params.keyword = searchForm.keyword.trim()
  }
  const problemId = Number(searchForm.problemId)
  if (problemId > 0) {
    params.problemId = problemId
  }
  return params
}

const applyQueryFilters = (query) => {
  if (query.problemId !== undefined) {
    searchForm.problemId = String(query.problemId || '')
  }
  if (query.keyword !== undefined) {
    searchForm.keyword = String(query.keyword || '')
  }
}

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await discussionApi.getPostList(buildParams())
    postList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载帖子列表失败')
  } finally {
    loading.value = false
  }
}

const loadProblemOptions = async () => {
  try {
    const res = await problemApi.getProblemList({
      page: 1,
      size: 500
    })
    problemOptions.value = res.data.records || []
  } catch (error) {
    ElMessage.error(error.message || '加载题目列表失败')
  }
}

const resetForm = () => {
  form.title = ''
  form.problemId = null
  form.content = ''
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const submitPost = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await discussionApi.createPost({
        title: form.title.trim(),
        content: form.content.trim(),
        problemId: form.problemId || null
      })
      dialogVisible.value = false
      ElMessage.success('发布成功')
      router.push(`/discussion/${res.data}`)
    } catch (error) {
      ElMessage.error(error.message || '发布失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该帖子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await discussionApi.deletePost(row.id)
    ElMessage.success('删除成功')
    await loadPosts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const goToDetail = (id) => {
  router.push(`/discussion/${id}`)
}

const goToProblem = (problemId) => {
  router.push(`/problem/${problemId}`)
}

const handleSearch = () => {
  pagination.page = 1
  loadPosts()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.problemId = ''
  pagination.page = 1
  loadPosts()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  loadPosts()
}

onMounted(async () => {
  applyQueryFilters(route.query)
  await Promise.all([loadPosts(), loadProblemOptions()])
})

watch(
  () => route.query,
  (query) => {
    applyQueryFilters(query)
    pagination.page = 1
    loadPosts()
  }
)
</script>

<style scoped>
.main-card {
  border-radius: 8px;
}

.search-form {
  margin-bottom: 14px;
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

.preview-text {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
