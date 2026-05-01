<template>
  <section class="space-y-6">
    <header>
      <div class="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 class="section-title">讨论广场</h1>
          <p v-if="activeProblemId" class="section-subtitle">
            当前按题目筛选：#{{ activeProblemId }}
          </p>
        </div>
        <div class="rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700">
          热门优先推荐
        </div>
      </div>
    </header>

    <AppCard>
      <div class="grid gap-3 md:grid-cols-[1fr_200px_140px_160px]">
        <input
          v-model.trim="keyword"
          class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="搜索帖子标题或内容"
          @keyup.enter="search"
        />
        <input
          v-model.trim="problemId"
          class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="题目 ID（可选）"
          @keyup.enter="search"
        />
        <AppButton @click="search">搜索</AppButton>
        <AppButton variant="secondary" @click="toggleCreate">{{ creating ? '取消发帖' : (userStore.isLoggedIn ? '发布帖子' : '登录后发帖') }}</AppButton>
      </div>
    </AppCard>

    <AppCard v-if="creating">
      <h2 class="text-lg font-semibold text-slate-800">发布新帖</h2>
      <div class="mt-3 space-y-3">
        <input
          v-model.trim="newPost.title"
          class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="帖子标题"
        />
        <input
          v-model.trim="newPost.problemId"
          class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="关联题目 ID（可选）"
        />
        <textarea
          v-model.trim="newPost.content"
          class="h-36 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="帖子内容（支持 Markdown）"
        />
        <p v-if="postError" class="text-sm text-rose-600">{{ postError }}</p>
        <AppButton :disabled="posting" @click="submitPost">{{ posting ? '发布中...' : '确认发布' }}</AppButton>
      </div>
    </AppCard>

    <div v-if="loading" class="grid gap-3">
      <div v-for="n in 6" :key="`post-skeleton-${n}`" class="skeleton h-24 rounded-xl" />
    </div>

    <div v-else-if="posts.length" class="grid gap-4 md:grid-cols-2">
      <article
        v-for="post in posts"
        :key="post.id"
        class="group cursor-pointer rounded-[18px] border border-line bg-white p-5 shadow-card transition-colors hover:border-slate-400"
        @click="openPost(post.id)"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="space-y-2">
            <div class="flex flex-wrap items-center gap-2">
              <span class="rounded-full bg-slate-900 px-2.5 py-1 text-[11px] font-semibold tracking-[0.12em] text-white">DISCUSS</span>
              <span v-if="post.likeCount" class="rounded-full bg-rose-50 px-2.5 py-1 text-[11px] font-medium text-rose-700">
                热度 {{ post.likeCount }}
              </span>
            </div>
            <h3 class="line-clamp-2 text-lg font-semibold text-slate-800">{{ post.title }}</h3>
          </div>
          <div class="flex flex-col items-end gap-2">
            <span v-if="post.problemId" class="shrink-0 rounded-full border border-slate-200 px-2 py-0.5 text-xs text-slate-600">
              #{{ post.problemId }}
            </span>
            <span v-if="showAuditBadge(post)" class="rounded-full px-2 py-0.5 text-xs" :class="auditStatusClass(post.auditStatus)">
              {{ auditStatusText(post.auditStatus) }}
            </span>
          </div>
        </div>
        <p class="mt-2 line-clamp-3 text-sm leading-6 text-soft">{{ post.contentPreview || post.content || '' }}</p>
        <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
          <div class="flex flex-wrap items-center gap-3 text-xs text-soft">
            <UserIdentity :user="post" avatar-size="xs" />
            <span>浏览 {{ post.viewCount || 0 }}</span>
            <span>点赞 {{ post.likeCount || 0 }}</span>
            <span>{{ formatTime(post.createTime) }}</span>
          </div>
          <button
            type="button"
            class="inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium transition"
            :class="post.liked ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:text-slate-800'"
            @click.stop="toggleLike(post)"
          >
            <span>{{ post.liked ? '已点赞' : '点赞' }}</span>
            <span>{{ post.likeCount || 0 }}</span>
          </button>
        </div>
      </article>
    </div>

    <EmptyState v-else message="暂无帖子，欢迎发布第一条讨论" />

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
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { discussionApi } from '@/api'
import { useUserStore } from '@/stores/useUserStore'
import UserIdentity from '@/components/ui/UserIdentity.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { requireLoginAction } from '@/utils/authAction'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const creating = ref(false)
const posting = ref(false)
const postError = ref('')

const posts = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(12)

const keyword = ref('')
const problemId = ref('')

const newPost = reactive({
  title: '',
  content: '',
  problemId: ''
})

const activeProblemId = computed(() => {
  const pid = Number(problemId.value)
  return Number.isFinite(pid) && pid > 0 ? pid : 0
})

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function auditStatusText(status) {
  const v = Number(status)
  if (v === 1) return '已通过'
  if (v === 2) return '已驳回'
  return '待审核'
}

function auditStatusClass(status) {
  const v = Number(status)
  if (v === 1) return 'bg-emerald-100 text-emerald-700'
  if (v === 2) return 'bg-rose-100 text-rose-700'
  return 'bg-amber-100 text-amber-700'
}

function showAuditBadge(post) {
  if (!post) return false
  if (Number(post.auditStatus) !== 1) return true
  return userStore.isAdmin
}

function getCurrentRedirectPath() {
  return route.fullPath || '/discuss'
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
  } finally {
    loading.value = false
  }
}

async function toggleCreate() {
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: '发布帖子'
  })
  if (!ok) {
    return
  }
  creating.value = !creating.value
}

async function submitPost() {
  postError.value = ''
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: '发布帖子'
  })
  if (!ok) {
    return
  }
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
    postError.value = e.message || '发帖失败'
  } finally {
    posting.value = false
  }
}

async function toggleLike(post) {
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: getCurrentRedirectPath(),
    actionText: post?.liked ? '取消点赞' : '点赞帖子'
  })
  if (!ok || !post?.id) {
    return
  }

  const liked = !!post.liked
  post.liked = !liked
  post.likeCount = Math.max(0, Number(post.likeCount || 0) + (liked ? -1 : 1))

  try {
    if (liked) {
      await discussionApi.unlikePost(post.id)
    } else {
      await discussionApi.likePost(post.id)
    }
    posts.value = [...posts.value].sort((a, b) => {
      const likeDiff = Number(b.likeCount || 0) - Number(a.likeCount || 0)
      if (likeDiff !== 0) return likeDiff
      return String(b.createTime || '').localeCompare(String(a.createTime || ''))
    })
  } catch (e) {
    post.liked = liked
    post.likeCount = Math.max(0, Number(post.likeCount || 0) + (liked ? 1 : -1))
  }
}

function openPost(postId) {
  router.push(`/discuss/${postId}`)
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
  if (page.value * size.value >= total.value) return
  page.value += 1
  load()
}

watch(
  () => route.query,
  (q) => {
    keyword.value = typeof q.keyword === 'string' ? q.keyword : ''
    problemId.value = typeof q.problemId === 'string' ? q.problemId : ''
    page.value = 1
    load()
  },
  { immediate: true }
)
</script>
