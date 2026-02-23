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
          <el-button type="primary" @click="handleSearch" :icon="Search">
            搜索
          </el-button>
          <el-button @click="resetSearch" :icon="Refresh">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">当前页题目</div>
          <div class="overview-value">{{ pageStats.total }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">平均通过率</div>
          <div class="overview-value">{{ pageStats.avgRate }}%</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">高通过率题目</div>
          <div class="overview-value">{{ pageStats.highRateCount }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">困难题数量</div>
          <div class="overview-value">{{ pageStats.hardCount }}</div>
        </div>
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
              <el-link
                type="primary"
                @click="goToProblem(row.id)"
                class="problem-title-link"
              >
                <el-icon style="margin-right: 5px;"><Document /></el-icon>
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

          <el-table-column prop="passRate" label="通过率" width="140" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="normalizeRate(row.passRate, row.acceptCount, row.submitCount)"
                :color="getPassRateColor(normalizeRate(row.passRate, row.acceptCount, row.submitCount))"
                :stroke-width="8"
              />
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
          @size-change="handlePageSizeChange"
          @current-change="loadProblems"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { problemApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Document, Search, Refresh } from '@element-plus/icons-vue'

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

const pageStats = computed(() => {
  const list = problemList.value || []
  if (!list.length) {
    return {
      total: 0,
      avgRate: 0,
      highRateCount: 0,
      hardCount: 0
    }
  }

  const rates = list.map((item) => normalizeRate(item.passRate, item.acceptCount, item.submitCount))
  const avg = Math.round(rates.reduce((sum, x) => sum + x, 0) / rates.length)
  return {
    total: list.length,
    avgRate: avg,
    highRateCount: rates.filter((x) => x >= 70).length,
    hardCount: list.filter((item) => item.difficulty === 'HARD').length
  }
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

.problem-title-link {
  font-size: 15px;
  font-weight: 600;
  display: flex;
  align-items: center;
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

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
