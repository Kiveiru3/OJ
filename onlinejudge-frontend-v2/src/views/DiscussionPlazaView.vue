<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">交流广场</h1>
      <p class="section-subtitle">支持帖子检索、发布与详情预览。</p>
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
        <textarea v-model.trim="newPost.content" class="h-36 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="内容" />
        <p v-if="postError" class="text-sm text-rose-600">{{ postError }}</p>
        <AppButton :disabled="posting" @click="submitPost">{{ posting ? '发布中...' : '发布' }}</AppButton>
      </div>
    </AppCard>

    <div class="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
      <AppCard>
        <h2 class="text-lg font-semibold text-slate-800">帖子流</h2>
        <div class="mt-4 space-y-3">
          <article v-for="post in posts" :key="post.id" class="rounded-lg border border-line p-4 transition hover:border-slate-700">
            <button class="w-full text-left" @click="selectPost(post.id)">
              <h3 class="text-[15px] font-semibold text-slate-800">{{ post.title }}</h3>
              <p class="mt-1 text-sm text-soft line-clamp-2">{{ post.contentPreview || post.content || '' }}</p>
              <div class="mt-2 flex items-center gap-3 text-xs text-soft">
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
        <div v-if="selectedPost" class="mt-3 space-y-2">
          <div class="text-base font-semibold text-slate-800">{{ selectedPost.title }}</div>
          <div class="text-xs text-soft">{{ selectedPost.nickname || selectedPost.username || '-' }} · {{ formatTime(selectedPost.createTime) }}</div>
          <div class="prose-readable whitespace-pre-line">{{ selectedPost.content || selectedPost.contentPreview || '' }}</div>
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
import { onMounted, reactive, ref } from 'vue'
import { discussionApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const loading = ref(false)
const creating = ref(false)
const posting = ref(false)
const postError = ref('')

const posts = ref([])
const selectedPost = ref(null)
const total = ref(0)
const page = ref(1)
const size = ref(10)

const keyword = ref('')
const problemId = ref('')

const newPost = reactive({
  title: '',
  content: '',
  problemId: ''
})

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
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

async function selectPost(id) {
  const res = await discussionApi.getPostDetail(id)
  selectedPost.value = res.data || null
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

function search() {
  page.value = 1
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

onMounted(load)
</script>
