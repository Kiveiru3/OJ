<template>
  <div class="discussion-detail-container pro-page">
    <el-card v-loading="loading" class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button text @click="goBack">返回</el-button>
            <h2>#{{ post.id }} {{ post.title }}</h2>
          </div>
          <div class="header-actions">
            <el-button
              v-if="post.problemId"
              type="primary"
              plain
              @click="goToProblem"
            >
              题目 #{{ post.problemId }}
            </el-button>
            <el-button v-if="canDeletePost" type="danger" @click="handleDeletePost">
              删除
            </el-button>
            <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
          </div>
        </div>
      </template>

      <div class="meta-bar">
        <span class="meta-item">作者：{{ post.nickname || post.username || `用户#${post.userId}` }}</span>
        <span class="meta-item">角色：{{ post.role || '-' }}</span>
        <span class="meta-item">浏览量：{{ post.viewCount || 0 }}</span>
        <span class="meta-item">发布时间：{{ formatDateTime(post.createTime) }}</span>
      </div>

      <div v-if="post.problemId" class="problem-binding">
        关联题目：
        <el-link type="primary" @click="goToProblem">
          #{{ post.problemId }} {{ post.problemTitle || '' }}
        </el-link>
      </div>

      <el-divider />

      <div class="content-block">
        <pre>{{ post.content || '-' }}</pre>
      </div>

      <el-divider />

      <div class="comment-header">
        <h3>评论</h3>
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
          <el-button
            type="primary"
            :loading="commentSubmitting"
            @click="submitComment"
          >
            提交评论
          </el-button>
        </el-form-item>
      </el-form>

      <div v-loading="commentLoading" class="comment-list">
        <el-empty v-if="!commentLoading && !commentList.length" description="暂无评论" />

        <div
          v-for="item in commentList"
          :key="item.id"
          class="comment-item"
        >
          <div class="comment-top">
            <div class="comment-author">
              {{ item.nickname || item.username || `用户#${item.userId}` }}
            </div>
            <div class="comment-time">{{ formatDateTime(item.createTime) }}</div>
          </div>
          <div class="comment-content">{{ item.content }}</div>
          <div class="comment-actions">
            <el-button
              v-if="canDeleteComment(item)"
              type="danger"
              link
              @click="handleDeleteComment(item)"
            >
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left h2 {
  margin: 0;
  font-size: 22px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  color: #606266;
  font-size: 13px;
}

.meta-item {
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 6px 10px;
}

.problem-binding {
  margin-top: 12px;
  color: #606266;
}

.content-block pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  font-family: inherit;
  color: #303133;
}

.comment-header h3 {
  margin: 0;
  font-size: 18px;
}

.comment-form {
  margin-top: 12px;
  margin-bottom: 8px;
}

.comment-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  background: #fff;
}

.comment-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 600;
  color: #303133;
}

.comment-time {
  color: #909399;
  font-size: 12px;
}

.comment-content {
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  margin-top: 8px;
  text-align: right;
}

.pagination-container {
  margin-top: 22px;
}
</style>
