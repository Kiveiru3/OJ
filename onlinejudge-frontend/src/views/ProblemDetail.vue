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
              <h2 class="problem-title">{{ problem.title || '未命名题目' }}</h2>
            </div>

            <div class="problem-meta">
              <el-tag :type="getDifficultyType(problem.difficulty)" effect="dark">
                {{ getDifficultyText(problem.difficulty) }}
              </el-tag>
              <el-button size="small" type="primary" plain @click="goToProblemDiscussions">
                相关讨论
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">通过数</div>
          <div class="overview-value">{{ problem.acceptCount || 0 }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">提交数</div>
          <div class="overview-value">{{ problem.submitCount || 0 }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">通过率</div>
          <div class="overview-value">{{ passRate }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">时间限制</div>
          <div class="overview-value text-value">{{ problem.timeLimit || 2000 }} 毫秒</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">内存限制</div>
          <div class="overview-value text-value">{{ problem.memoryLimit || 256000 }} KB</div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="problem-tabs">
        <el-tab-pane label="题目描述" name="description">
          <div class="problem-content">
            <section
              v-for="(section, index) in descriptionSections"
              :key="`${section.title}-${index}`"
              class="section-panel statement-panel"
            >
              <div class="section-head">
                <h3 class="section-title">
                  <span class="index-badge">{{ index + 1 }}</span>
                  {{ section.title }}
                </h3>
              </div>
              <div class="statement-text" v-html="section.html"></div>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="输入输出" name="io">
          <div class="problem-content io-grid">
            <section class="section-panel">
              <h3 class="section-title">
                <el-icon><Document /></el-icon>
                输入格式
              </h3>
              <div class="io-card">
                <div class="statement-text" v-html="renderRichText(problem.inputFormat || '-')"></div>
              </div>
            </section>
            <section class="section-panel">
              <h3 class="section-title">
                <el-icon><Document /></el-icon>
                输出格式
              </h3>
              <div class="io-card">
                <div class="statement-text" v-html="renderRichText(problem.outputFormat || '-')"></div>
              </div>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="示例" name="examples">
          <div class="problem-content">
            <section
              v-for="(example, index) in examples"
              :key="index"
              class="section-panel example-item"
            >
              <div class="section-head">
                <h4 class="section-title">
                  <el-icon><Star /></el-icon>
                  示例 {{ index + 1 }}
                </h4>
              </div>
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
            </section>
            <el-empty v-if="!examples.length" description="暂无示例" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="提交代码" name="submit">
          <div class="problem-content">
            <section class="section-panel submit-panel">
              <div class="submit-head">
                <h3 class="section-title">
                  <el-icon><Upload /></el-icon>
                  在线提交
                </h3>
                <span class="draft-tip">草稿自动保存（按题目 + 语言）</span>
              </div>

              <el-form :model="submitForm" label-position="top" class="submit-form">
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
                  <el-button class="language-action-btn" @click="formatCurrentCode">
                    格式化代码
                  </el-button>
                  <el-button class="language-action-btn" @click="clearCode">
                    清空
                  </el-button>
                </el-form-item>

                <el-form-item label="代码" class="code-form-item">
                  <div class="code-editor-wrapper">
                    <el-input
                      v-model="submitForm.code"
                      type="textarea"
                      :rows="24"
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
            </section>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { problemApi, submissionApi } from '@/api'
import { ElMessage } from 'element-plus'
import { Document, Star, Upload } from '@element-plus/icons-vue'

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

const passRate = computed(() => {
  const total = Number(problem.value.submitCount || 0)
  const accepted = Number(problem.value.acceptCount || 0)
  if (!total) return '0%'
  return `${Math.round((accepted * 1000) / total) / 10}%`
})

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

const normalizeImageUrl = (value) => {
  const raw = String(value || '').trim()
  if (!raw) return ''
  try {
    const url = new URL(raw)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      return ''
    }
    return url.href
  } catch (_) {
    return ''
  }
}

const formatMathExpr = (expr) => {
  let html = escapeHtml(String(expr || '').trim())
  if (!html) return ''

  html = html
    .replace(/\\leq?/g, '≤')
    .replace(/\\geq?/g, '≥')
    .replace(/\\neq/g, '≠')
    .replace(/\\times/g, '×')
    .replace(/\\cdot/g, '·')
    .replace(/\\to/g, '→')
    .replace(/\\notin/g, '∉')
    .replace(/\\in/g, '∈')
    .replace(/\\left/g, '')
    .replace(/\\right/g, '')
    .replace(/&lt;=/g, '≤')
    .replace(/&gt;=/g, '≥')

  html = html.replace(/\\frac\{([^{}]+)\}\{([^{}]+)\}/g, (_, numerator, denominator) => (
    `<span class="math-frac"><span class="math-num">${numerator}</span><span class="math-den">${denominator}</span></span>`
  ))

  html = html
    .replace(/([A-Za-z0-9)\]])_\{([^{}]+)\}/g, '$1<sub>$2</sub>')
    .replace(/([A-Za-z0-9)\]])_([A-Za-z0-9])/g, '$1<sub>$2</sub>')
    .replace(/([A-Za-z0-9)\]])\^\{([^{}]+)\}/g, '$1<sup>$2</sup>')
    .replace(/([A-Za-z0-9)\]])\^([A-Za-z0-9])/g, '$1<sup>$2</sup>')

  return html
}

const renderTextWithMath = (line) => {
  const source = String(line || '')
  if (!source.includes('$')) {
    return escapeHtml(source)
  }

  const pattern = /\$([^$]+)\$/g
  let cursor = 0
  let result = ''
  let match = pattern.exec(source)

  while (match) {
    result += escapeHtml(source.slice(cursor, match.index))
    result += `<span class="math-inline">${formatMathExpr(match[1])}</span>`
    cursor = pattern.lastIndex
    match = pattern.exec(source)
  }

  result += escapeHtml(source.slice(cursor))
  return result
}

const renderInlineRichText = (line) => {
  const source = String(line || '')
  if (!source.includes('![')) {
    return renderTextWithMath(source)
  }

  const imagePattern = /!\[([^\]]*)\]\(([^)]+)\)/g
  let cursor = 0
  let html = ''
  let match = imagePattern.exec(source)

  while (match) {
    const before = source.slice(cursor, match.index)
    html += renderTextWithMath(before)

    const alt = escapeHtml(match[1] || '题面图片')
    const src = normalizeImageUrl(match[2] || '')
    if (src) {
      html += `<span class="statement-image-inline"><img src="${escapeHtml(src)}" alt="${alt}" loading="lazy" referrerpolicy="no-referrer" /></span>`
    } else {
      html += escapeHtml(match[0])
    }

    cursor = imagePattern.lastIndex
    match = imagePattern.exec(source)
  }

  html += renderTextWithMath(source.slice(cursor))
  return html
}

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
        .map((line) => `<li>${renderInlineRichText(line.replace(/^[-*]\s+/, ''))}</li>`)
        .join('')
      return `<ul>${items}</ul>`
    }

    if (lines.every((line) => /^\d+[.)]\s+/.test(line))) {
      const items = lines
        .map((line) => `<li>${renderInlineRichText(line.replace(/^\d+[.)]\s+/, ''))}</li>`)
        .join('')
      return `<ol>${items}</ol>`
    }

    return `<p>${lines.map((line) => renderInlineRichText(line)).join('<br>')}</p>`
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
    const trimmed = line.trim()
    const match = trimmed.match(/^【(.+?)】$/) || trimmed.match(/^\[(.+?)\]$/)
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

const normalizeCodeText = (code) => String(code || '')
  .replace(/\r\n?/g, '\n')
  .replace(/\t/g, '  ')
  .split('\n')
  .map((line) => line.replace(/[ \t]+$/g, ''))
  .join('\n')

const formatBraceLanguageCode = (code) => {
  const normalized = normalizeCodeText(code)
  const lines = normalized.split('\n')
  const output = []
  let level = 0

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      output.push('')
      continue
    }

    const startsWithClose = /^}/.test(line)
    if (startsWithClose) {
      level = Math.max(0, level - 1)
    }

    output.push(`${'  '.repeat(level)}${line}`)

    const openCount = (line.match(/{/g) || []).length
    const closeCount = (line.match(/}/g) || []).length
    level = Math.max(0, level + openCount - closeCount)
  }

  return output.join('\n').replace(/\n{3,}/g, '\n\n').trimEnd()
}

const formatPythonCode = (code) => {
  const normalized = normalizeCodeText(code)
  const lines = normalized.split('\n')
  const output = []
  let level = 0

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line) {
      output.push('')
      continue
    }

    if (/^(elif|else|except|finally)\b/.test(line)) {
      level = Math.max(0, level - 1)
    }

    output.push(`${'  '.repeat(level)}${line}`)

    if (/:$/.test(line) && !line.startsWith('#')) {
      level += 1
    }
  }

  return output.join('\n').replace(/\n{3,}/g, '\n\n').trimEnd()
}

const formatCodeByLanguage = (language, code) => {
  if (language === 'PYTHON') {
    return formatPythonCode(code)
  }
  if (language === 'JAVA' || language === 'CPP') {
    return formatBraceLanguageCode(code)
  }
  return normalizeCodeText(code).trimEnd()
}

const formatCurrentCode = () => {
  const before = submitForm.code || ''
  const after = formatCodeByLanguage(submitForm.language, before)
  submitForm.code = after
  if (after === before) {
    ElMessage.info('代码已是较规范格式')
    return
  }
  ElMessage.success('代码格式化完成')
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
  padding: 18px;
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
  gap: 10px;
  flex-wrap: wrap;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

.problem-tabs {
  margin-top: 6px;
}

.problem-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 600;
  height: 46px;
  line-height: 46px;
  padding: 0 18px;
}

.problem-tabs :deep(.el-tabs__item.is-active) {
  color: #1d4ed8;
}

.problem-content {
  padding: 14px 4px 4px;
}

.section-panel {
  border: 1px solid #e2e9f3;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border-radius: 8px;
  padding: 18px;
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
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.index-badge {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e8f1ff;
  color: #1d4ed8;
  font-size: 12px;
}

.statement-text {
  color: #1f2937;
  font-size: 17px;
  line-height: 2;
  letter-spacing: 0.1px;
}

.statement-text :deep(p) {
  margin: 0 0 14px;
}

.statement-text :deep(p:last-child) {
  margin-bottom: 0;
}

.statement-text :deep(ul),
.statement-text :deep(ol) {
  margin: 0;
  padding-left: 26px;
}

.statement-text :deep(li) {
  margin: 8px 0;
}

.statement-text :deep(.math-inline) {
  display: inline-flex;
  align-items: baseline;
  gap: 1px;
  padding: 0 4px;
  margin: 0 1px;
  border-radius: 4px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #0f172a;
  font-family: "Times New Roman", Georgia, serif;
  font-size: 1.02em;
}

.statement-text :deep(.math-inline sub),
.statement-text :deep(.math-inline sup) {
  font-size: 0.75em;
  line-height: 1;
}

.statement-text :deep(.math-frac) {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  line-height: 1.05;
  margin: 0 2px;
  vertical-align: middle;
}

.statement-text :deep(.math-frac .math-num) {
  padding: 0 2px 1px;
  border-bottom: 1px solid #334155;
}

.statement-text :deep(.math-frac .math-den) {
  padding: 1px 2px 0;
}

.statement-text :deep(.statement-image-inline) {
  display: inline-flex;
  margin: 6px 0;
  max-width: 100%;
  vertical-align: middle;
}

.statement-text :deep(.statement-image-inline img) {
  max-width: min(100%, 820px);
  height: auto;
  border-radius: 8px;
  border: 1px solid #dbe5f3;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.io-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.io-card {
  background: linear-gradient(160deg, #f9fbff 0%, #f0f6ff 100%);
  border: 1px solid #d9e5f4;
  padding: 16px 18px;
  border-radius: 8px;
}

.example-item {
  margin-bottom: 14px;
}

.example-io {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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
  font-size: 15px;
  margin-bottom: 10px;
  color: #606266;
}

.example-code {
  background: #f2f7fd;
  padding: 12px;
  border-radius: 8px;
  margin: 0;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 15px;
  line-height: 1.75;
  overflow-x: auto;
  white-space: pre-wrap;
}

.submit-panel {
  margin-bottom: 0;
}

.submit-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.draft-tip {
  font-size: 12px;
  color: #64748b;
}

.submit-form {
  width: 100%;
}

.submit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.submit-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #334155;
}

.submit-form :deep(.el-form-item__content) {
  width: 100%;
  margin-left: 0 !important;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.code-form-item :deep(.el-form-item__content) {
  display: block;
}

.language-action-btn {
  margin-left: 0;
}

.code-editor-wrapper {
  width: 100%;
  min-height: 560px;
  border: 1px solid #cfdbeb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.2);
}

.code-editor :deep(.el-textarea),
.code-editor :deep(.el-textarea__inner),
.code-editor :deep(textarea) {
  width: 100%;
  min-height: 560px;
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 15px;
  line-height: 1.7;
  background: #1f2937;
  color: #f8fafc;
  border: none;
}

.submit-button {
  width: 200px;
  height: 44px;
  font-size: 16px;
}

@media (max-width: 1024px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .problem-tabs :deep(.el-tabs__item) {
    font-size: 15px;
    padding: 0 12px;
  }

  .statement-text {
    font-size: 16px;
    line-height: 1.9;
  }

  .section-title {
    font-size: 17px;
  }

  .example-code {
    font-size: 14px;
  }

  .context-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .io-grid,
  .example-io {
    grid-template-columns: 1fr;
  }

  .submit-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .code-editor-wrapper {
    min-height: 440px;
  }

  .code-editor :deep(.el-textarea),
  .code-editor :deep(.el-textarea__inner),
  .code-editor :deep(textarea) {
    min-height: 440px;
    font-size: 14px;
  }
}
</style>

