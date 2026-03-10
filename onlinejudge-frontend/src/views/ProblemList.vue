<template>
  <div class="problem-list-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><Document /></el-icon>
            <div class="title-stack">
              <h2 class="pro-title-text">题目列表</h2>
              <span class="title-sub">当前题库共 {{ pagination.total }} 道题</span>
            </div>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form pro-filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="题目标题关键词"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="难度">
          <el-select
            v-model="searchForm.difficulty"
            placeholder="全部"
            clearable
            style="width: 150px"
          >
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">搜索</el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="filter-note">
        <span class="filter-label">当前筛选：</span>
        <span class="filter-value">{{ filterSummary }}</span>
      </div>

      <el-skeleton v-if="loading && !problemList.length" :rows="7" animated />
      <div v-else class="pro-table-shell">
        <el-table
          v-loading="loading"
          :data="problemList"
          stripe
          style="width: 100%"
          class="problem-table"
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="title" label="题目标题" min-width="280">
            <template #default="{ row }">
              <el-link type="primary" @click="goToProblem(row.id)" class="problem-title-link">
                <el-icon v-if="isCurrentUserAccepted(row)" class="solved-icon"><CircleCheckFilled /></el-icon>
                <el-icon v-else style="margin-right: 5px;"><Document /></el-icon>
                {{ row.title }}
              </el-link>
            </template>
          </el-table-column>

          <el-table-column prop="difficulty" label="难度" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getDifficultyType(row.difficulty)" effect="dark" size="large">
                {{ getDifficultyText(row.difficulty) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="acceptCount" label="通过数" width="120" align="center">
            <template #default="{ row }">
              <span class="stat-number success">{{ row.acceptCount || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="submitCount" label="提交数" width="120" align="center">
            <template #default="{ row }">
              <span class="stat-number">{{ row.submitCount || 0 }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="passRate" label="通过率" width="150" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="normalizeRate(row.passRate, row.acceptCount, row.submitCount)"
                :color="getPassRateColor(normalizeRate(row.passRate, row.acceptCount, row.submitCount))"
                :stroke-width="8"
              />
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无题目，请调整筛选条件后重试" :image-size="80" />
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
          @current-change="loadProblems"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { problemApi } from '@/api'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Document, Refresh, Search } from '@element-plus/icons-vue'

const router = useRouter()

const loading = ref(false)
const problemList = ref([])

const searchForm = reactive({
  keyword: '',
  difficulty: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const filterSummary = computed(() => {
  const parts = []
  if (searchForm.keyword.trim()) {
    parts.push(`关键词：${searchForm.keyword.trim()}`)
  }
  if (searchForm.difficulty) {
    parts.push(`难度：${getDifficultyText(searchForm.difficulty)}`)
  }
  return parts.length ? parts.join(' ｜ ') : '全部题目'
})

const getDifficultyType = (difficulty) => {
  const map = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return map[difficulty] || ''
}

const getDifficultyText = (difficulty) => {
  const map = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return map[difficulty] || difficulty
}

const getPassRateColor = (rate) => {
  if (rate >= 70) return '#67c23a'
  if (rate >= 40) return '#e6a23c'
  return '#f56c6c'
}

const normalizeRate = (passRate, acceptCount, submitCount) => {
  if (typeof passRate === 'number') {
    return Math.max(0, Math.min(100, Math.round(passRate)))
  }
  if (!submitCount) return 0
  return Math.round(((acceptCount || 0) / submitCount) * 100)
}

const isCurrentUserAccepted = (row) => {
  if (!row) return false
  if (row.solved === true || row.isSolved === true || row.userSolved === true) return true
  if (row.userStatus === 'ACCEPTED' || row.myStatus === 'ACCEPTED' || row.status === 'ACCEPTED') return true
  if (row.solveStatus === 'SOLVED' || row.solveStatus === 'ACCEPTED') return true
  return false
}

const buildParams = () => {
  const params = {
    page: pagination.page,
    size: pagination.size
  }

  if (searchForm.difficulty) {
    params.difficulty = searchForm.difficulty
  }

  if (searchForm.keyword) {
    params.keyword = searchForm.keyword.trim()
  }

  return params
}

const loadProblems = async () => {
  loading.value = true
  try {
    const res = await problemApi.getProblemList(buildParams())
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

const handlePageSizeChange = () => {
  pagination.page = 1
  loadProblems()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.difficulty = ''
  pagination.page = 1
  loadProblems()
}

const goToProblem = (id) => {
  router.push(`/problem/${id}`)
}

onMounted(() => {
  loadProblems()
})
</script>

<style scoped>
.main-card {
  border-radius: 8px;
}

.search-form {
  margin-bottom: 10px;
}

.filter-note {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  margin-bottom: 12px;
  padding: 0 12px;
  border-radius: 8px;
  border: 1px solid #dce8f8;
  background: linear-gradient(160deg, #f8fbff 0%, #f1f7ff 100%);
}

.filter-label {
  color: #475569;
  font-size: 12px;
}

.filter-value {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.problem-title-link {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.solved-icon {
  margin-right: 6px;
  color: #16a34a;
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
  font-size: 16px;
  font-weight: 600;
  color: #606266;
}

.stat-number.success {
  color: #67c23a;
}

.pagination-container {
  margin-top: 22px;
}

</style>
