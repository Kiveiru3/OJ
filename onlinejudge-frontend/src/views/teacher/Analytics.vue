<template>
  <div class="teacher-analytics-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><DataAnalysis /></el-icon>
            <h2 class="pro-title-text">教学数据分析</h2>
          </div>
          <div class="header-actions">
            <el-select v-model="days" style="width: 120px" @change="loadAnalytics">
              <el-option label="近 7 天" :value="7" />
              <el-option label="近 14 天" :value="14" />
              <el-option label="近 30 天" :value="30" />
            </el-select>
            <el-button type="primary" plain @click="exportOverviewCsv">导出 CSV</el-button>
            <el-button @click="loadAnalytics">刷新</el-button>
          </div>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="8" animated />

      <template v-else>
        <div class="metrics-grid">
          <div class="metric-item">
            <div class="metric-label">学生总数</div>
            <div class="metric-value">{{ analytics.totalStudents || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">教师总数</div>
            <div class="metric-value">{{ analytics.totalTeachers || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">题目总数</div>
            <div class="metric-value">{{ analytics.totalProblems || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">提交总数</div>
            <div class="metric-value">{{ analytics.totalSubmissions || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">通过提交数</div>
            <div class="metric-value">{{ analytics.acceptedSubmissions || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">通过率</div>
            <div class="metric-value">{{ analytics.acceptanceRate ?? 0 }}%</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">竞赛总数 / 进行中</div>
            <div class="metric-value">{{ analytics.totalContests || 0 }} / {{ analytics.activeContests || 0 }}</div>
          </div>
          <div class="metric-item">
            <div class="metric-label">帖子数 / 评论数</div>
            <div class="metric-value">{{ analytics.discussionPosts || 0 }} / {{ analytics.discussionComments || 0 }}</div>
          </div>
        </div>

        <el-divider />

        <div class="analytics-grid">
          <el-card class="analytics-card" shadow="never">
            <template #header>提交状态分布</template>
            <div class="pro-table-shell">
              <el-table :data="toDistributionRows(analytics.submissionStatusDistribution, analytics.totalSubmissions, 'status')" size="small">
                <el-table-column prop="name" label="状态" min-width="170" />
                <el-table-column prop="count" label="数量" width="90" align="center" />
                <el-table-column prop="rate" label="占比" width="100" align="center" />
                <template #empty>
                  <el-empty description="暂无状态分布数据" :image-size="70" />
                </template>
              </el-table>
            </div>
          </el-card>

          <el-card class="analytics-card" shadow="never">
            <template #header>语言分布</template>
            <div class="pro-table-shell">
              <el-table :data="toDistributionRows(analytics.languageDistribution, analytics.totalSubmissions, 'language')" size="small">
                <el-table-column prop="name" label="语言" min-width="140" />
                <el-table-column prop="count" label="数量" width="90" align="center" />
                <el-table-column prop="rate" label="占比" width="100" align="center" />
                <template #empty>
                  <el-empty description="暂无语言分布数据" :image-size="70" />
                </template>
              </el-table>
            </div>
          </el-card>
        </div>

        <el-divider />

        <div class="section-title">近 {{ days }} 天提交趋势</div>
        <div class="pro-table-shell">
          <el-table :data="analytics.dailySubmissionTrend || []" stripe>
            <el-table-column prop="date" label="日期" width="120" />
            <el-table-column prop="totalSubmissions" label="总提交数" width="150" align="center" />
            <el-table-column prop="acceptedSubmissions" label="通过提交数" width="170" align="center" />
            <el-table-column prop="acceptanceRate" label="通过率" width="140" align="center">
              <template #default="{ row }">{{ row.acceptanceRate ?? 0 }}%</template>
            </el-table-column>
            <el-table-column label="通过进度" min-width="240">
              <template #default="{ row }">
                <el-progress
                  :percentage="Math.min(100, Math.max(0, Number(row.acceptanceRate || 0)))"
                  :stroke-width="10"
                />
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无提交趋势数据" :image-size="70" />
            </template>
          </el-table>
        </div>
      </template>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'TeacherAnalytics'
}
</script>

<script setup>
import { onMounted, ref } from 'vue'
import { teacherApi } from '@/api'
import { ElMessage } from 'element-plus'
import { DataAnalysis } from '@element-plus/icons-vue'

const loading = ref(false)
const days = ref(7)
const analytics = ref({
  totalStudents: 0,
  totalTeachers: 0,
  totalProblems: 0,
  totalSubmissions: 0,
  acceptedSubmissions: 0,
  acceptanceRate: 0,
  totalContests: 0,
  activeContests: 0,
  discussionPosts: 0,
  discussionComments: 0,
  submissionStatusDistribution: {},
  languageDistribution: {},
  dailySubmissionTrend: []
})

const loadAnalytics = async () => {
  loading.value = true
  try {
    const res = await teacherApi.getOverviewAnalytics({ days: days.value })
    analytics.value = res.data || analytics.value
  } catch (error) {
    ElMessage.error(error.message || '加载教学分析数据失败')
  } finally {
    loading.value = false
  }
}

const getStatusText = (status) => {
  const map = {
    ACCEPTED: '通过',
    WRONG_ANSWER: '答案错误',
    TIME_LIMIT_EXCEEDED: '超出时间限制',
    MEMORY_LIMIT_EXCEEDED: '超出内存限制',
    RUNTIME_ERROR: '运行错误',
    COMPILE_ERROR: '编译错误',
    PENDING: '等待中',
    JUDGING: '评测中'
  }
  return map[status] || status
}

const getLanguageText = (language) => {
  const map = {
    JAVA: 'Java',
    CPP: 'C++',
    C: 'C',
    PYTHON: 'Python',
    JAVASCRIPT: 'JavaScript',
    GO: 'Go',
    RUST: 'Rust'
  }
  return map[language] || language
}

const toDistributionRows = (mapObj, total, scene) => {
  const source = mapObj || {}
  return Object.keys(source).map((key) => {
    const count = Number(source[key] || 0)
    const rate = total ? `${Math.round((count * 1000) / total) / 10}%` : '0%'
    let name = key
    if (scene === 'status') {
      name = getStatusText(key)
    } else if (scene === 'language') {
      name = getLanguageText(key)
    }
    return {
      name,
      count,
      rate
    }
  }).sort((a, b) => b.count - a.count)
}

const exportOverviewCsv = () => {
  teacherApi.exportOverviewCsv({ days: days.value })
    .then((res) => {
      const csv = res.data || ''
      const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `教学分析-${days.value}天.csv`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      ElMessage.success('CSV 导出成功')
    })
    .catch((error) => {
      ElMessage.error(error.message || '导出失败')
    })
}

onMounted(() => {
  loadAnalytics()
})
</script>

<style scoped>
.main-card {
  border-radius: 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.metric-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.analytics-card {
  border-radius: 8px;
}

.section-title {
  margin-bottom: 10px;
  font-size: 18px;
  font-weight: 600;
}

@media (max-width: 980px) {
  .metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .analytics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
