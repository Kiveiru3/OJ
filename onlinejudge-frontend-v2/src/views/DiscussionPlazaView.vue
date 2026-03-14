<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">交流广场</h1>
      <p class="section-subtitle">
        支持帖子检索、发帖、题解查看与评论回复。
        <span v-if="activeProblemId > 0">当前题目筛选：#{{ activeProblemId }}</span>
      </p>
    </header>

    <AppCard>
      <div class="grid gap-3 md:grid-cols-[1fr_160px_120px_120px]">
        <input v-model.trim="keyword" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="搜索标题或内容" @keyup.enter="search" />
        <input v-model.trim="problemId" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="题目ID(可选)" @keyup.enter="search" />
        <AppButton @click="search">搜索</AppButton>
        <AppButton variant="secondary" @click="toggleCreate">{{ creating ? '收起' : '发帖' }}</AppButton>
      </div>
    </AppCard>

    <AppCard v-if="creating">
      <h2 class="text-lg font-semibold text-slate-800">发布帖子</h2>
      <div class="mt-3 space-y-3">
        <input v-model.trim="newPost.title" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="标题" />
        <input v-model.trim="newPost.problemId" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="关联题目ID（可空）" />
        <textarea v-model.trim="newPost.content" class="h-36 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="内容（支持 Markdown）" />
        <p v-if="postError" class="text-sm text-rose-600">{{ postError }}</p>
        <AppButton :disabled="posting" @click="submitPost">{{ posting ? '发布中...' : '发布' }}</AppButton>
      </div>
    </AppCard>

    <div class="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">帖子流</h2>
        <div class="mt-4 space-y-3">
          <article v-for="post in posts" :key="post.id" class="rounded-lg border p-4 transition" :class="selectedPostId === post.id ? 'border-slate-900 bg-slate-50' : 'border-line hover:border-slate-700'">
            <button class="w-full text-left" @click="selectPost(post.id)">
              <h3 class="text-[15px] font-semibold text-slate-800">{{ post.title }}</h3>
              <p class="mt-1 text-sm text-soft line-clamp-2">{{ post.contentPreview || post.content || '' }}</p>
              <div class="mt-2 flex flex-wrap items-center gap-3 text-xs text-soft">
                <span>{{ post.nickname || post.username || `用户#${post.userId}` }}</span>
                <span>•</span>
                <span>浏览 {{ post.viewCount || 0 }}</span>
                <span v-if="post.problemId">•</span>
                <span v-if="post.problemId">题目 #{{ post.problemId }}</span>
              </div>
            </button>
          </article>
        </div>
      </AppCard>

      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">帖子详情</h2>
        <div v-if="selectedPost" class="mt-3 space-y-3">
          <div class="text-base font-semibold text-slate-800">{{ selectedPost.title }}</div>
          <div class="text-xs text-soft">
            {{ selectedPost.nickname || selectedPost.username || '-' }} · {{ formatTime(selectedPost.createTime) }}
            <span v-if="selectedPost.problemId"> · 题目 #{{ selectedPost.problemId }}</span>
          </div>
          <ProblemRichContent :content="selectedPost.content || selectedPost.contentPreview || ''" />

          <div class="rounded-lg border border-line p-3">
            <div class="mb-2 flex items-center justify-between">
              <h3 class="text-sm font-semibold text-slate-800">回复</h3>
              <AppButton size="sm" variant="secondary" :disabled="commentLoading" @click="loadComments(selectedPost.id)">
                {{ commentLoading ? '刷新中...' : '刷新' }}
              </AppButton>
            </div>

            <div v-if="commentLoading" class="grid gap-2">
              <div v-for="n in 3" :key="`comment-skeleton-${n}`" class="skeleton h-14 rounded-lg" />
            </div>

            <div v-else-if="comments.length" class="space-y-2">
              <div v-for="comment in comments" :key="comment.id" class="rounded-md border border-line bg-slate-50 p-3">
                <div class="flex items-center justify-between text-xs text-soft">
                  <span>{{ comment.nickname || comment.username || `用户#${comment.userId}` }}</span>
                  <span>{{ formatTime(comment.createTime) }}</span>
                </div>
                <ProblemRichContent class="mt-2" :content="comment.content || ''" />
                <div class="mt-2 flex gap-2">
                  <AppButton size="sm" variant="ghost" @click="replyToComment(comment)">回复</AppButton>
                  <AppButton v-if="comment.editable || isAdmin" size="sm" variant="ghost" @click="removeComment(comment)">删除</AppButton>
                </div>
              </div>
            </div>
            <div v-else class="text-sm text-soft">暂无回复，欢迎抢沙发</div>

            <div class="mt-3 space-y-2">
              <textarea
                v-model.trim="commentDraft"
                class="h-24 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
                placeholder="写下你的回复（支持 Markdown）"
              />
              <p v-if="commentError" class="text-sm text-rose-600">{{ commentError }}</p>
              <AppButton size="sm" :disabled="commentPosting || !selectedPostId" @click="submitComment">
                {{ commentPosting ? '发布中...' : '发布回复' }}
              </AppButton>
            </div>
          </div>
        </div>
        <EmptyState v-else message="请选择左侧帖子查看详情" />
      </AppCard>
    </div>

    <div class="flex items-center justify-between">
      <p class="text-sm text-soft">共 {{ total }} 帖 · 当前第 {{ page }} 页</p>
      <div class="flex gap-2">
        <AppButton variant="secondary" size="sm" :disabled="page <= 1 || loading" @click="prevPage">上一页</AppButton>
        <AppButton size="sm" :disabled="loading || page * size >= total" @click="nextPage">下一页</AppButton>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { discussionApi } from '@/api'
import ProblemRichContent from '@/components/problem/ProblemRichContent.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { useUserStore } from '@/stores/useUserStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const creating = ref(false)
const posting = ref(false)
const postError = ref('')

const posts = ref([])
const selectedPost = ref(null)
const selectedPostId = ref(0)
const total = ref(0)
const page = ref(1)
const size = ref(10)

const commentLoading = ref(false)
const commentPosting = ref(false)
const commentError = ref('')
const comments = ref([])
const commentDraft = ref('')

const keyword = ref('')
const problemId = ref('')

const newPost = reactive({
  title: '',
  content: '',
  problemId: ''
})

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const activeProblemId = computed(() => {
  const pid = Number(problemId.value)
  return Number.isFinite(pid) && pid > 0 ? pid : 0
})

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function syncRouteQuery() {
  const query = {}
  const pid = Number(problemId.value)
  if (keyword.value) query.keyword = keyword.value
  if (pid > 0) query.problemId = String(pid)
  router.replace({ path: '/discuss', query })
}

async function load() {
  loading.value = true
  try {
    const pid = Number(problemId.value)
    const res = await discussionApi.getPostList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      problemId: pid > 0 ? pid : undefined
    })
    posts.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)

    if (selectedPostId.value && !posts.value.some((p) => p.id === selectedPostId.value)) {
      selectedPost.value = null
      selectedPostId.value = 0
      comments.value = []
    }

    if (!selectedPostId.value && posts.value.length) {
      await selectPost(posts.value[0].id)
    }
  } finally {
    loading.value = false
  }
}

async function loadComments(postId) {
  commentLoading.value = true
  try {
    const res = await discussionApi.getCommentList(postId, { page: 1, size: 50 })
    comments.value = res.data?.records || []
  } finally {
    commentLoading.value = false
  }
}

async function selectPost(id) {
  const res = await discussionApi.getPostDetail(id)
  selectedPost.value = res.data || null
  selectedPostId.value = selectedPost.value?.id || 0
  commentDraft.value = ''
  commentError.value = ''
  if (selectedPostId.value) {
    await loadComments(selectedPostId.value)
  }
}

function toggleCreate() {
  creating.value = !creating.value
}

async function submitPost() {
  postError.value = ''
  if (!newPost.title || !newPost.content) {
    postError.value = '标题和内容不能为空'
    return
  }
  posting.value = true
  try {
    const pid = Number(newPost.problemId)
    await discussionApi.createPost({
      title: newPost.title,
      content: newPost.content,
      problemId: pid > 0 ? pid : null
    })
    newPost.title = ''
    newPost.content = ''
    newPost.problemId = ''
    creating.value = false
    await load()
  } catch (e) {
    postError.value = e.message || '发布失败'
  } finally {
    posting.value = false
  }
}

function replyToComment(comment) {
  const name = comment.nickname || comment.username || `用户#${comment.userId}`
  commentDraft.value = `@${name} ${commentDraft.value || ''}`.trim()
}

async function removeComment(comment) {
  if (!selectedPostId.value) return
  const ok = window.confirm('确定删除这条回复吗？')
  if (!ok) return
  await discussionApi.deleteComment(comment.id)
  await loadComments(selectedPostId.value)
}

async function submitComment() {
  commentError.value = ''
  if (!selectedPostId.value) {
    commentError.value = '请先选择帖子'
    return
  }
  if (!commentDraft.value) {
    commentError.value = '回复内容不能为空'
    return
  }
  commentPosting.value = true
  try {
    await discussionApi.createComment(selectedPostId.value, { content: commentDraft.value })
    commentDraft.value = ''
    await loadComments(selectedPostId.value)
  } catch (e) {
    commentError.value = e.message || '发布回复失败'
  } finally {
    commentPosting.value = false
  }
}

function search() {
  page.value = 1
  syncRouteQuery()
  load()
}

function prevPage() {
  if (page.value <= 1) return
  page.value -= 1
  load()
}

function nextPage() {
  page.value += 1
  load()
}

watch(
  () => route.query.problemId,
  (pid) => {
    const nextPid = pid ? String(pid) : ''
    if (problemId.value !== nextPid) {
      problemId.value = nextPid
      page.value = 1
      load()
    }
  }
)

onMounted(async () => {
  keyword.value = route.query.keyword ? String(route.query.keyword) : ''
  problemId.value = route.query.problemId ? String(route.query.problemId) : ''
  await load()
})
</script>
