<template>
  <div class="problem-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header">
          <div class="header-content">
            <div v-if="hasSubmissionFilterContext" class="context-bar">
              <span class="context-text">来自提交详情，可快速回看同题提交记录</span>
              <el-button size="small" type="primary" plain @click="goToFilteredSubmissions">
                返回提交记录（已筛选）
              </el-button>
            </div>
            <div class="problem-title-row">
              <span class="problem-code">题号 #{{ route.params.id }}</span>
              <h2 class="problem-title">{{ problem.title }}</h2>
            </div>
            <div class="problem-meta">
              <el-tag :type="getDifficultyType(problem.difficulty)" effect="dark" size="large">
                {{ getDifficultyText(problem.difficulty) }}
              </el-tag>
              <div class="meta-stats">
                <span class="stat-item">
                  <el-icon><Check /></el-icon>
                  通过数：{{ problem.acceptCount || 0 }}
                </span>
                <span class="stat-item">
                  <el-icon><Upload /></el-icon>
                  提交数：{{ problem.submitCount || 0 }}
                </span>
                <span class="stat-item">
                  时间限制：{{ problem.timeLimit || 2000 }}ms
                </span>
                <span class="stat-item">
                  内存限制：{{ problem.memoryLimit || 256000 }}KB
                </span>
                <el-button size="small" type="primary" plain @click="goToProblemDiscussions">
                  相关讨论
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="problem-tabs">
        <el-tab-pane label="题目描述" name="description">
          <div class="problem-content statement-layout">
            <div
              v-for="(section, index) in descriptionSections"
              :key="`${section.title}-${index}`"
              class="statement-section"
            >
              <h3 class="statement-title">{{ section.title }}</h3>
              <div class="statement-text" v-html="section.html"></div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="输入/输出" name="io">
          <div class="problem-content">
            <div class="io-section">
              <h3 class="section-title">
                <el-icon><Document /></el-icon>
                输入格式
              </h3>
              <div class="io-card">
                <div class="statement-text" v-html="renderRichText(problem.inputFormat || '-')"></div>
              </div>
            </div>
            <div class="io-section">
              <h3 class="section-title">
                <el-icon><Document /></el-icon>
                输出格式
              </h3>
              <div class="io-card">
                <div class="statement-text" v-html="renderRichText(problem.outputFormat || '-')"></div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="示例" name="examples">
          <div class="problem-content">
            <div
              v-for="(example, index) in examples"
              :key="index"
              class="example-item"
            >
              <h4 class="example-title">
                <el-icon><Star /></el-icon>
                示例 {{ index + 1 }}
              </h4>
              <div class="example-io">
                <div class="example-box">
                  <div class="example-label">输入</div>
                  <pre class="example-code">{{ example.input || '-' }}</pre>
                </div>
                <div class="example-box">
                  <div class="example-label">输出</div>
                  <pre class="example-code">{{ example.output || '-' }}</pre>
                </div>
              </div>
            </div>
            <el-empty v-if="!examples.length" description="暂无示例" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="提交代码" name="submit">
          <div class="submit-section">
            <el-form :model="submitForm" label-width="100px">
              <el-form-item label="语言">
                <el-select
                  v-model="submitForm.language"
                  style="width: 200px"
                  @change="handleLanguageChange"
                >
                  <el-option label="Java" value="JAVA" />
                  <el-option label="C++" value="CPP" />
                  <el-option label="Python" value="PYTHON" />
                </el-select>
                <el-button class="language-action-btn" @click="useTemplateCode">
                  使用模板
                </el-button>
                <el-button class="language-action-btn" @click="clearCode">
                  清空
                </el-button>
              </el-form-item>
              <el-form-item label="代码">
                <div class="code-editor-wrapper">
                  <el-input
                    v-model="submitForm.code"
                    type="textarea"
                    :rows="20"
                    placeholder="请在这里编写解答代码..."
                    class="code-editor"
                  />
                </div>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  @click="handleSubmit"
                  :loading="submitting"
                  size="large"
                  class="submit-button"
                >
                  <el-icon v-if="!submitting"><Upload /></el-icon>
                  {{ submitting ? '提交中...' : '提交代码' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { problemApi, submissionApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Check, Upload, Document, Star } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const problem = ref({})
const activeTab = ref('description')

const LANGUAGE_TEMPLATES = {
  JAVA: `public class Main {
  public static void main(String[] args) {

  }
}`,
  CPP: `#include <bits/stdc++.h>
using namespace std;

int main() {

  return 0;
}`,
  PYTHON: `def main():
    pass


if __name__ == "__main__":
    main()
`
}

const submitForm = reactive({
  language: 'JAVA',
  code: ''
})
const previousLanguage = ref('JAVA')
const currentProblemId = computed(() => String(route.params.id || ''))
const allowedSubmissionStatus = new Set([
  'ACCEPTED',
  'WRONG_ANSWER',
  'TIME_LIMIT_EXCEEDED',
  'MEMORY_LIMIT_EXCEEDED',
  'RUNTIME_ERROR',
  'COMPILE_ERROR',
  'PENDING',
  'JUDGING'
])
const allowedLanguage = new Set(['JAVA', 'CPP', 'PYTHON'])

const examples = computed(() => {
  if (Array.isArray(problem.value.examples)) {
    return problem.value.examples
  }

  if (problem.value.sampleInput || problem.value.sampleOutput) {
    return [
      {
        input: problem.value.sampleInput || '',
        output: problem.value.sampleOutput || ''
      }
    ]
  }

  return []
})

const escapeHtml = (value) => String(value || '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#39;')

const renderRichText = (text) => {
  const normalized = String(text || '').replace(/\r/g, '').trim()
  if (!normalized) {
    return '<p>-</p>'
  }

  const blocks = normalized.split(/\n{2,}/)
  const htmlBlocks = blocks.map((block) => {
    const lines = block.split('\n').map((line) => line.trim()).filter(Boolean)
    if (!lines.length) {
      return ''
    }

    if (lines.every((line) => /^[-*]\s+/.test(line))) {
      const items = lines
        .map((line) => `<li>${escapeHtml(line.replace(/^[-*]\s+/, ''))}</li>`)
        .join('')
      return `<ul>${items}</ul>`
    }

    if (lines.every((line) => /^\d+[.)]\s+/.test(line))) {
      const items = lines
        .map((line) => `<li>${escapeHtml(line.replace(/^\d+[.)]\s+/, ''))}</li>`)
        .join('')
      return `<ol>${items}</ol>`
    }

    return `<p>${lines.map((line) => escapeHtml(line)).join('<br>')}</p>`
  }).filter(Boolean)

  return htmlBlocks.join('')
}

const descriptionSections = computed(() => {
  const raw = String(problem.value.description || '').replace(/\r/g, '')
  if (!raw.trim()) {
    return [
      {
        title: '题目描述',
        html: '<p>-</p>'
      }
    ]
  }

  const lines = raw.split('\n')
  const sections = []
  let currentTitle = '题目描述'
  let currentLines = []

  const pushCurrent = (allowEmpty = false) => {
    const content = currentLines.join('\n').trim()
    if (!allowEmpty && !content) {
      return
    }
    sections.push({
      title: currentTitle,
      html: renderRichText(content || '-')
    })
  }

  for (const line of lines) {
    const match = line.trim().match(/^【(.+?)】$/)
    if (match) {
      pushCurrent(false)
      currentTitle = match[1]
      currentLines = []
    } else {
      currentLines.push(line)
    }
  }
  pushCurrent(true)

  return sections.length ? sections : [{ title: '题目描述', html: '<p>-</p>' }]
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

const hasSubmissionFilterContext = computed(() => {
  return route.query.fromSubmission === '1' && !!route.query.filterProblemId
})

const goToFilteredSubmissions = () => {
  const problemId = String(route.query.filterProblemId || route.params.id || '')
  const status = String(route.query.filterStatus || '')
  const language = String(route.query.filterLanguage || '')

  const query = {
    source: 'problem-detail',
    problemId
  }
  if (allowedSubmissionStatus.has(status)) {
    query.status = status
  }
  if (allowedLanguage.has(language)) {
    query.language = language
  }

  router.push({
    path: '/submissions',
    query
  })
}

const goToProblemDiscussions = () => {
  router.push({
    path: '/discussions',
    query: {
      problemId: String(route.params.id || '')
    }
  })
}

const getTemplateCode = (language) => LANGUAGE_TEMPLATES[language] || ''

const getDraftKey = (problemId, language) => `oj:draft:${problemId}:${language}`

const readDraftCode = (problemId, language) => {
  if (!problemId) return null
  return localStorage.getItem(getDraftKey(problemId, language))
}

const saveDraftCode = (problemId, language, code) => {
  if (!problemId) return
  localStorage.setItem(getDraftKey(problemId, language), code || '')
}

const restoreCodeFromDraft = () => {
  const draft = readDraftCode(currentProblemId.value, submitForm.language)
  submitForm.code = draft ?? getTemplateCode(submitForm.language)
}

const handleLanguageChange = (nextLanguage) => {
  saveDraftCode(currentProblemId.value, previousLanguage.value, submitForm.code)
  const nextDraft = readDraftCode(currentProblemId.value, nextLanguage)
  submitForm.code = nextDraft ?? getTemplateCode(nextLanguage)
  previousLanguage.value = nextLanguage
}

const useTemplateCode = () => {
  submitForm.code = getTemplateCode(submitForm.language)
}

const clearCode = () => {
  submitForm.code = ''
}

const loadProblem = async () => {
  loading.value = true
  try {
    const res = await problemApi.getProblemDetail(route.params.id)
    problem.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '加载题目失败')
    router.push('/problems')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!submitForm.code.trim()) {
    ElMessage.warning('请先输入代码')
    return
  }

  submitting.value = true
  try {
    const res = await submissionApi.submitCode({
      problemId: Number(route.params.id),
      code: submitForm.code,
      language: submitForm.language
    })
    ElMessage.success('提交成功')
    router.push(`/submission/${res.data.id}`)
  } catch (error) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadProblem()
  restoreCodeFromDraft()
})

watch(
  () => route.params.id,
  async (newId, oldId) => {
    if (oldId) {
      saveDraftCode(String(oldId), submitForm.language, submitForm.code)
    }
    activeTab.value = 'description'
    await loadProblem()
    restoreCodeFromDraft()
  }
)

watch(
  () => submitForm.code,
  (newCode) => {
    saveDraftCode(currentProblemId.value, submitForm.language, newCode)
  }
)

onBeforeUnmount(() => {
  saveDraftCode(currentProblemId.value, submitForm.language, submitForm.code)
})
</script>

<style scoped>
.problem-detail-container {
  max-width: 1400px;
  margin: 0 auto;
}

.main-card {
  border-radius: 8px;
}

.card-header {
  padding: 20px;
  background: linear-gradient(132deg, #f8fbff 0%, #eef5ff 55%, #f5fcff 100%);
  border-radius: 8px 8px 0 0;
}

.header-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.context-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  background: linear-gradient(145deg, #ecfbff 0%, #f0f8ff 100%);
  border: 1px solid #d4e9f9;
}

.context-text {
  font-size: 14px;
  color: #365173;
}

.problem-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.problem-code {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #0f5696;
  border: 1px solid #bfd6f0;
  background: #edf5ff;
  padding: 3px 10px;
  border-radius: 999px;
}

.problem-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.problem-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #334155;
  font-size: 13px;
  border: 1px solid #d8e4f2;
  background: #f8fbff;
  border-radius: 999px;
  padding: 4px 10px;
}

.problem-tabs {
  margin-top: 18px;
}

.problem-content {
  padding: 20px;
  line-height: 1.8;
  color: #303133;
  font-size: 15px;
}

.statement-layout {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.statement-section {
  border: 1px solid #dce8f6;
  border-radius: 8px;
  background: linear-gradient(160deg, #fcfdff 0%, #f5f9ff 100%);
  padding: 16px 18px;
}

.statement-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
  color: #0f356e;
  border-left: 3px solid #0b63f6;
  padding-left: 10px;
}

.statement-text {
  color: #303133;
  font-size: 15px;
  line-height: 1.9;
}

.statement-text :deep(p) {
  margin: 0 0 10px;
}

.statement-text :deep(p:last-child) {
  margin-bottom: 0;
}

.statement-text :deep(ul),
.statement-text :deep(ol) {
  margin: 0;
  padding-left: 22px;
}

.statement-text :deep(li) {
  margin: 6px 0;
}

.io-section {
  margin-bottom: 30px;
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

.io-card {
  background: linear-gradient(160deg, #f9fbff 0%, #f0f6ff 100%);
  border: 1px solid #d9e5f4;
  padding: 14px 16px;
  border-radius: 8px;
}

.example-item {
  margin-bottom: 40px;
  padding: 20px;
  background: linear-gradient(165deg, #fafcff 0%, #f2f7ff 100%);
  border-radius: 8px;
  border-left: 4px solid #0b63f6;
  border: 1px solid #dbe7f7;
}

.example-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.example-io {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.example-box {
  background: white;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #dde7f3;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
}

.example-label {
  font-weight: 600;
  margin-bottom: 10px;
  color: #606266;
}

.example-code {
  background: #f2f7fd;
  padding: 12px;
  border-radius: 8px;
  margin: 0;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
  overflow-x: auto;
  white-space: pre-wrap;
}

.submit-section {
  padding: 20px;
}

.language-action-btn {
  margin-left: 8px;
}

.code-editor-wrapper {
  border: 1px solid #cfdbeb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}

.code-editor :deep(textarea) {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  background: #2d2d2d;
  color: #f8f8f2;
  border: none;
}

.submit-button {
  width: 200px;
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #0b63f6 0%, #1d58bc 100%);
  border-color: #215bc1;
  transition: all 0.3s ease;
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(11, 99, 246, 0.32);
}

@media (max-width: 900px) {
  .context-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
