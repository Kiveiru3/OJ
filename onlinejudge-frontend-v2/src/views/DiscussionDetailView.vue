<template>
  <section class="space-y-6">
    <div class="flex items-center justify-between">
      <AppButton variant="secondary" size="sm" @click="goBack">返回讨论广场</AppButton>
      <AppButton v-if="post.problemId" size="sm" @click="goSolveProblem">去做这道题</AppButton>
    </div>

    <AppCard v-if="loading">
      <div class="grid gap-3">
        <div class="skeleton h-8 rounded-lg" />
        <div class="skeleton h-5 rounded-lg" />
        <div class="skeleton h-28 rounded-lg" />
      </div>
    </AppCard>

    <template v-else-if="post.id">
      <AppCard>
        <div class="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 class="text-xl font-semibold text-slate-800">{{ post.title }}</h1>
            <div class="mt-2 flex flex-wrap items-center gap-3 text-xs text-soft">
              <UserIdentity :user="post" avatar-size="xs" />
              <span>发布时间 {{ formatTime(post.createTime) }}</span>
              <span>浏览 {{ post.viewCount || 0 }}</span>
              <span>点赞 {{ post.likeCount || 0 }}</span>
              <span v-if="post.problemId">题目 #{{ post.problemId }}</span>
            </div>
          </div>

          <div class="flex flex-wrap gap-2">
            <button
              v-if="canLikePost"
              type="button"
              class="inline-flex items-center gap-2 rounded-full border px-3 py-2 text-sm font-medium transition"
              :class="post.liked ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-slate-200 bg-white text-slate-700 hover:border-slate-400'"
              :disabled="likeLoading"
              @click="togglePostLike"
            >
              <span>{{ post.liked ? '已点赞' : '点赞帖子' }}</span>
              <span>{{ post.likeCount || 0 }}</span>
            </button>
            <AppButton v-if="canSocialAction" size="sm" variant="secondary" :disabled="followLoading" @click="toggleFollow">
              {{ following ? '取消关注' : '关注作者' }}
            </AppButton>
            <RouterLink v-if="canSocialAction" :to="`/messages?peerUserId=${post.userId}`">
              <AppButton size="sm" variant="ghost">发私信</AppButton>
            </RouterLink>
          </div>
        </div>

        <div class="mt-5 rounded-xl border border-line bg-white p-4">
          <ProblemRichContent :content="post.content || ''" />
        </div>
      </AppCard>

      <AppCard>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold text-slate-800">回复区</h2>
          <AppButton size="sm" variant="secondary" :disabled="commentLoading" @click="loadComments">
            {{ commentLoading ? '刷新中...' : '刷新回复' }}
          </AppButton>
        </div>

        <div v-if="commentLoading" class="mt-3 grid gap-2">
          <div v-for="n in 4" :key="`comment-loading-${n}`" class="skeleton h-16 rounded-lg" />
        </div>

        <div v-else class="mt-3">
          <DiscussionCommentThread
            v-if="commentTree.length"
            :nodes="commentTree"
            :is-admin="isAdmin"
            @reply="replyToComment"
            @delete="removeComment"
          />
          <EmptyState v-else message="暂无回复，欢迎抢沙发" />
        </div>

        <div v-if="!userStore.isLoggedIn" class="mt-4 rounded-xl border border-dashed border-line bg-slate-50 p-4 text-sm text-soft">
          登录后可发布回复、参与楼中楼讨论。
          <div class="mt-2">
            <AppButton size="sm" @click="goLoginForComment">去登录</AppButton>
          </div>
        </div>

        <div v-else class="mt-4 rounded-xl border border-line bg-slate-50 p-4">
          <div class="flex items-center justify-between">
            <h3 class="text-sm font-semibold text-slate-800">写回复</h3>
            <button
              v-if="replyParentId"
              type="button"
              class="text-xs text-slate-500 underline-offset-2 hover:text-slate-700 hover:underline"
              @click="clearReplyTarget"
            >
              取消回复对象
            </button>
          </div>
          <p v-if="replyTargetName" class="mt-1 text-xs text-soft">当前回复：{{ replyTargetName }}</p>
          <textarea
            v-model.trim="commentDraft"
            class="mt-2 h-28 w-full rounded-lg border border-line bg-white px-3 py-2 text-sm outline-none focus:border-slate-800"
            placeholder="输入你的回复（支持 Markdown）"
          />
          <p v-if="commentError" class="mt-1 text-sm text-rose-600">{{ commentError }}</p>
          <div class="mt-2 flex justify-end">
            <AppButton size="sm" :disabled="commentPosting" @click="submitComment">
              {{ commentPosting ? '发布中...' : '发布回复' }}
            </AppButton>
          </div>
        </div>
      </AppCard>
    </template>

    <EmptyState v-else message="帖子不存在或已被删除" />
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { discussionApi, socialApi } from '@/api'
import DiscussionCommentThread from '@/components/discussion/DiscussionCommentThread.vue'
import ProblemRichContent from '@/components/problem/ProblemRichContent.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import UserIdentity from '@/components/ui/UserIdentity.vue'
import { useUiStore } from '@/stores/useUiStore'
import { useUserStore } from '@/stores/useUserStore'
import { requireLoginAction } from '@/utils/authAction'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const ui = useUiStore()

const postId = computed(() => Number(route.params.id || 0))

const loading = ref(false)
const post = ref({})

const comments = ref([])
const commentLoading = ref(false)
const commentPosting = ref(false)
const commentDraft = ref('')
const commentError = ref('')
const replyParentId = ref(null)
const replyTargetName = ref('')

const followLoading = ref(false)
const following = ref(false)
const likeLoading = ref(false)

const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const canSocialAction = computed(() => {
  const currentUserId = Number(userStore.userInfo?.id || 0)
  const authorId = Number(post.value.userId || 0)
  return currentUserId > 0 && authorId > 0 && currentUserId !== authorId
})
const canLikePost = computed(() => {
  const currentUserId = Number(userStore.userInfo?.id || 0)
  const authorId = Number(post.value.userId || 0)
  return currentUserId > 0 && authorId > 0 && currentUserId !== authorId
})

const commentTree = computed(() => buildCommentTree(comments.value))

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function goBack() {
  router.push('/discuss')
}

function getCurrentRedirectPath() {
  return route.fullPath || '/discuss'
}

async function goSolveProblem() {
  if (!post.value.problemId) return
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: '做题'
  })
  if (!ok) return
  router.push({ path: '/studio', query: { problemId: String(post.value.problemId) } })
}

async function goLoginForComment() {
  await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: '回复帖子'
  })
}

function buildCommentTree(list) {
  if (!Array.isArray(list) || !list.length) return []
  const map = new Map()
  const roots = []
  list.forEach((item) => map.set(item.id, { ...item, children: [] }))
  list.forEach((item) => {
    const current = map.get(item.id)
    const parentId = item.parentCommentId
    if (parentId && map.has(parentId)) {
      map.get(parentId).children.push(current)
    } else {
      roots.push(current)
    }
  })
  return roots
}

async function loadPost() {
  if (!postId.value) return
  loading.value = true
  try {
    const res = await discussionApi.getPostDetail(postId.value)
    post.value = res.data || {}
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  if (!postId.value) return
  commentLoading.value = true
  try {
    const res = await discussionApi.getCommentList(postId.value, { page: 1, size: 200 })
    comments.value = res.data?.records || []
  } finally {
    commentLoading.value = false
  }
}

async function loadFollowStatus() {
  if (!canSocialAction.value) {
    following.value = false
    return
  }
  followLoading.value = true
  try {
    const res = await socialApi.getFollowStatus(post.value.userId)
    following.value = !!res.data
  } finally {
    followLoading.value = false
  }
}

async function toggleFollow() {
  if (!canSocialAction.value) return
  followLoading.value = true
  try {
    if (following.value) {
      await socialApi.unfollow(post.value.userId)
      following.value = false
    } else {
      await socialApi.follow(post.value.userId)
      following.value = true
    }
  } finally {
    followLoading.value = false
  }
}

async function togglePostLike() {
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: post.value?.liked ? '取消点赞' : '点赞帖子'
  })
  if (!ok || !post.value?.id || !canLikePost.value) {
    return
  }

  const liked = !!post.value.liked
  likeLoading.value = true
  post.value = {
    ...post.value,
    liked: !liked,
    likeCount: Math.max(0, Number(post.value.likeCount || 0) + (liked ? -1 : 1))
  }

  try {
    if (liked) {
      await discussionApi.unlikePost(post.value.id)
    } else {
      await discussionApi.likePost(post.value.id)
    }
  } catch (e) {
    post.value = {
      ...post.value,
      liked,
      likeCount: Math.max(0, Number(post.value.likeCount || 0) + (liked ? 1 : -1))
    }
  } finally {
    likeLoading.value = false
  }
}

function replyToComment(comment) {
  replyParentId.value = comment.id
  const name = comment.nickname || comment.username || `用户#${comment.userId}`
  replyTargetName.value = name
  commentDraft.value = `@${name} ${commentDraft.value || ''}`.trim()
}

function clearReplyTarget() {
  replyParentId.value = null
  replyTargetName.value = ''
}

async function submitComment() {
  commentError.value = ''
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: '发布回复'
  })
  if (!ok) {
    return
  }
  if (!commentDraft.value) {
    commentError.value = '回复内容不能为空'
    return
  }
  commentPosting.value = true
  try {
    await discussionApi.createComment(postId.value, {
      content: commentDraft.value,
      parentCommentId: replyParentId.value
    })
    commentDraft.value = ''
    clearReplyTarget()
    await loadComments()
  } catch (e) {
    commentError.value = e.message || '发布回复失败'
  } finally {
    commentPosting.value = false
  }
}

async function removeComment(comment) {
  const ok = await ui.confirm({
    title: '删除回复',
    message: '确定删除这条回复吗？删除后其子回复也会被删除。',
    okText: '删除',
    cancelText: '取消'
  })
  if (!ok) return
  await discussionApi.deleteComment(comment.id)
  await loadComments()
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.ensureUserInfo().catch(() => null)
  }
  await loadPost()
  await Promise.all([loadComments(), loadFollowStatus()])
})

watch(postId, async (value, oldValue) => {
  if (!value || value === oldValue) return
  clearReplyTarget()
  commentDraft.value = ''
  await loadPost()
  await Promise.all([loadComments(), loadFollowStatus()])
})
</script>
