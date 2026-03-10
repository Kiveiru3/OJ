<template>
  <div class="discussion-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回</el-button>
            <div class="title-stack">
              <h2 class="pro-title-text">#{{ post.id }} {{ post.title || '未命名帖子' }}</h2>
              <span class="title-sub">讨论详情与评论互动</span>
            </div>
          </div>
          <div class="header-actions">
            <el-button v-if="post.problemId" type="primary" plain @click="goToProblem">
              题目 #{{ post.problemId }}
            </el-button>
            <el-button v-if="canDeletePost" type="danger" @click="handleDeletePost">删除</el-button>
            <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="overview-grid">
        <div class="overview-card">
          <div class="overview-label">浏览量</div>
          <div class="overview-value">{{ post.viewCount || 0 }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">评论数</div>
          <div class="overview-value">{{ commentPagination.total }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">作者</div>
          <div class="overview-value text-value">{{ authorName }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-label">发布时间</div>
          <div class="overview-value text-value">{{ formatDateTime(post.createTime) }}</div>
        </div>
      </div>

      <div class="meta-bar">
        <span class="meta-item">角色：{{ post.role || '-' }}</span>
        <span class="meta-item">帖子 ID：{{ post.id || '-' }}</span>
        <span v-if="post.problemId" class="meta-item meta-link" @click="goToProblem">
          关联题目：#{{ post.problemId }} {{ post.problemTitle || '' }}
        </span>
      </div>

      <section class="section-panel">
        <div class="section-title">正文内容</div>
        <div class="content-block">
          <pre>{{ post.content || '暂无内容' }}</pre>
        </div>
      </section>

      <section class="section-panel">
        <div class="section-head">
          <div class="section-title">评论区</div>
          <span class="section-extra">共 {{ commentPagination.total }} 条</span>
        </div>

        <el-form class="comment-form">
          <el-form-item>
            <el-input
              v-model="commentForm.content"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="写下你的评论..."
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="commentSubmitting" @click="submitComment">
              提交评论
            </el-button>
          </el-form-item>
        </el-form>

        <div v-loading="commentLoading" class="comment-list">
          <el-empty v-if="!commentLoading && !commentList.length" description="暂无评论，来发表第一条吧" />

          <div v-for="item in commentList" :key="item.id" class="comment-item">
            <div class="comment-top">
              <div class="comment-author">{{ item.nickname || item.username || `用户#${item.userId}` }}</div>
              <div class="comment-time">{{ formatDateTime(item.createTime) }}</div>
            </div>
            <div class="comment-content">{{ item.content }}</div>
            <div class="comment-actions">
              <el-button v-if="canDeleteComment(item)" type="danger" link @click="handleDeleteComment(item)">
                删除
              </el-button>
            </div>
          </div>
        </div>

        <div class="pagination-container pro-pagination">
          <el-pagination
            v-model:current-page="commentPagination.page"
            v-model:page-size="commentPagination.size"
            :total="commentPagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleCommentPageSizeChange"
            @current-change="loadComments"
          />
        </div>
      </section>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { discussionApi } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const post = ref({})
const commentLoading = ref(false)
const commentSubmitting = ref(false)
const commentList = ref([])

const postId = computed(() => Number(route.params.id))

const commentForm = reactive({
  content: ''
})

const commentPagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const authorName = computed(() => post.value.nickname || post.value.username || `用户#${post.value.userId || '-'}`)

const canDeletePost = computed(() => {
  return userStore.isAdmin || Boolean(post.value.editable)
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

const loadPost = async () => {
  loading.value = true
  try {
    const res = await discussionApi.getPostDetail(postId.value)
    post.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '加载帖子失败')
    router.push('/discussions')
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  commentLoading.value = true
  try {
    const res = await discussionApi.getCommentList(postId.value, {
      page: commentPagination.page,
      size: commentPagination.size
    })
    commentList.value = res.data.records || []
    commentPagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载评论失败')
  } finally {
    commentLoading.value = false
  }
}

const refreshAll = async () => {
  await Promise.all([loadPost(), loadComments()])
}

const submitComment = async () => {
  const content = commentForm.content.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentSubmitting.value = true
  try {
    await discussionApi.createComment(postId.value, { content })
    commentForm.content = ''
    commentPagination.page = 1
    await loadComments()
    ElMessage.success('评论发布成功')
  } catch (error) {
    ElMessage.error(error.message || '评论发布失败')
  } finally {
    commentSubmitting.value = false
  }
}

const handleDeletePost = async () => {
  try {
    await ElMessageBox.confirm('确认删除该帖子吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await discussionApi.deletePost(postId.value)
    ElMessage.success('删除成功')
    router.push('/discussions')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const canDeleteComment = (comment) => {
  return userStore.isAdmin || Boolean(comment.editable)
}

const handleDeleteComment = async (comment) => {
  try {
    await ElMessageBox.confirm('确认删除该评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await discussionApi.deleteComment(comment.id)
    ElMessage.success('评论已删除')
    if (commentList.value.length === 1 && commentPagination.page > 1) {
      commentPagination.page -= 1
    }
    await loadComments()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除评论失败')
    }
  }
}

const handleCommentPageSizeChange = () => {
  commentPagination.page = 1
  loadComments()
}

const goToProblem = () => {
  if (!post.value.problemId) return
  router.push(`/problem/${post.value.problemId}`)
}

const goBack = () => {
  router.push('/discussions')
}

onMounted(() => {
  refreshAll()
})

watch(
  () => route.params.id,
  () => {
    commentPagination.page = 1
    commentForm.content = ''
    refreshAll()
  }
)
</script>

<style scoped>
.discussion-detail-container {
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
  margin-bottom: 12px;
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

.meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid #d7e2f0;
  background: #f8fbff;
  color: #475569;
  font-size: 12px;
}

.meta-link {
  cursor: pointer;
  color: #155cc6;
}

.section-panel {
  border: 1px solid #e2e9f3;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.section-extra {
  color: #64748b;
  font-size: 13px;
}

.content-block {
  margin-top: 12px;
}

.content-block pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.78;
  font-family: inherit;
  color: #334155;
  font-size: 14px;
}

.comment-form {
  margin-bottom: 8px;
}

.comment-item {
  border: 1px solid #e3e9f4;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
  transition: box-shadow 0.2s ease, transform 0.18s ease;
}

.comment-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.08);
}

.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 600;
  color: #1e293b;
}

.comment-time {
  color: #94a3b8;
  font-size: 12px;
}

.comment-content {
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.65;
}

.comment-actions {
  margin-top: 8px;
  text-align: right;
}

.pagination-container {
  margin-top: 20px;
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
