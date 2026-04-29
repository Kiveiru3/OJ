<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">赛事中枢</h1>
      <p class="section-subtitle">实时读取竞赛数据，支持报名、查看榜单与竞赛管理。</p>
    </header>

    <div class="grid gap-4 md:grid-cols-[1fr_120px_120px_140px]">
      <input
        v-model.trim="keyword"
        class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
        placeholder="搜索竞赛标题"
        @keyup.enter="search"
      />
      <AppButton @click="search">搜索</AppButton>
      <AppButton variant="secondary" @click="reset">重置</AppButton>
      <AppButton v-if="canManageContest" variant="secondary" @click="openCreate">新建竞赛</AppButton>
    </div>

    <div class="grid gap-4 lg:grid-cols-3">
      <AppCard v-for="event in contests" :key="event.id" class="group">
        <div class="flex items-center justify-between">
          <AppBadge :tone="statusTone(event.contestStatus)">{{ statusText(event.contestStatus) }}</AppBadge>
          <span class="text-xs text-soft">#{{ event.id }}</span>
        </div>
        <h3 class="mt-3 text-lg font-semibold text-slate-800">{{ event.title }}</h3>
        <p class="mt-1 text-sm text-soft">{{ event.description || '暂无描述' }}</p>
        <div class="mt-3 text-xs text-soft">{{ formatRange(event.startTime, event.endTime) }}</div>
        <div class="mt-3 rounded-lg bg-slate-100 p-3 text-sm">
          <div>题目数：{{ event.problemCount || 0 }}</div>
          <div>参赛：{{ event.participantCount || 0 }}</div>
          <div>罚时：{{ event.penaltyPerWrong ?? 20 }} 分钟</div>
          <div v-if="event.status === 0" class="mt-1 text-amber-700">状态：隐藏</div>
        </div>
        <div class="mt-4 flex gap-2">
          <AppButton size="sm" @click="showRanking(event.id)">查看榜单</AppButton>
          <AppButton size="sm" variant="secondary" :disabled="event.joined || joining" @click="join(event.id)">
            {{ event.joined ? '已报名' : (userStore.isLoggedIn ? '报名' : '登录后报名') }}
          </AppButton>
          <AppButton
            size="sm"
            variant="secondary"
            :disabled="enteringContest"
            @click="enterContest(event)"
          >
            进入比赛
          </AppButton>
        </div>
        <div v-if="canManageContest && canEditContest(event)" class="mt-2 flex gap-2">
          <AppButton size="sm" variant="ghost" @click="openEdit(event)">编辑</AppButton>
          <AppButton size="sm" variant="ghost" :disabled="managing" @click="removeContest(event)">删除</AppButton>
        </div>
      </AppCard>
    </div>

    <EmptyState v-if="!loading && !contests.length" message="暂无竞赛数据" />

    <div class="flex items-center justify-between">
      <p class="text-sm text-soft">共 {{ total }} 场 · 当前第 {{ page }} 页</p>
      <div class="flex gap-2">
        <AppButton variant="secondary" size="sm" :disabled="page <= 1 || loading" @click="prevPage">上一页</AppButton>
        <AppButton size="sm" :disabled="loading || page * size >= total" @click="nextPage">下一页</AppButton>
      </div>
    </div>

    <AppCard v-if="ranking.length">
      <h2 class="text-lg font-semibold text-slate-800">榜单预览（前 10）</h2>
      <div class="mt-3 overflow-x-auto">
        <table class="min-w-full text-left text-sm">
          <thead>
            <tr class="border-b border-line text-soft">
              <th class="px-2 py-2">排名</th>
              <th class="px-2 py-2">用户</th>
              <th class="px-2 py-2">解题</th>
              <th class="px-2 py-2">罚时</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in ranking" :key="item.userId" class="border-b border-line/60">
              <td class="px-2 py-2">{{ item.rank || '-' }}</td>
              <td class="px-2 py-2"><UserIdentity :user="item" avatar-size="xs" /></td>
              <td class="px-2 py-2">{{ item.acceptedCount ?? 0 }}</td>
              <td class="px-2 py-2">{{ item.totalPenalty ?? 0 }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>

    <div v-if="showContestPanel" class="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-3xl rounded-xl bg-white p-5 shadow-card">
        <div class="flex items-center justify-between">
          <div>
            <div class="text-lg font-semibold text-slate-900">{{ activeContest.title || '比赛题目' }}</div>
            <div class="mt-1 text-xs text-soft">{{ formatRange(activeContest.startTime, activeContest.endTime) }}</div>
          </div>
          <AppButton size="sm" variant="secondary" @click="closeContestPanel">关闭</AppButton>
        </div>

        <div class="mt-4 grid gap-2">
          <div
            v-for="p in activeContestProblems"
            :key="p.id"
            class="flex items-center justify-between rounded-lg border border-line px-3 py-2"
          >
            <div class="text-sm text-slate-700">#{{ p.id }} {{ p.title }}</div>
            <AppButton size="sm" @click="goSolve(p.id)">去做题</AppButton>
          </div>
        </div>

        <EmptyState v-if="!activeContestProblems.length" class="mt-4" message="该比赛暂无题目数据" />
      </div>
    </div>

    <div v-if="showEditor" class="fixed inset-0 z-40 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-2xl rounded-xl bg-white p-5 shadow-card">
        <div class="text-lg font-semibold text-slate-900">{{ editingContestId ? '编辑竞赛' : '新建竞赛' }}</div>
        <div class="mt-3 grid gap-3 md:grid-cols-2">
          <input v-model.trim="contestForm.title" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="竞赛标题" />
          <input v-model.trim="contestForm.description" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="竞赛描述（可选）" />

          <div>
            <div class="mb-1 text-xs text-soft">开始时间</div>
            <input v-model="contestForm.startTime" type="datetime-local" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" />
          </div>
          <div>
            <div class="mb-1 text-xs text-soft">结束时间</div>
            <input v-model="contestForm.endTime" type="datetime-local" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" />
          </div>

          <div>
            <div class="mb-1 text-xs text-soft">封榜时间（可选）</div>
            <input v-model="contestForm.scoreboardFreezeTime" type="datetime-local" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" />
          </div>
          <div>
            <div class="mb-1 text-xs text-soft">每次错误罚时（分钟）</div>
            <input v-model.number="contestForm.penaltyPerWrong" type="number" min="0" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" />
          </div>

          <div>
            <div class="mb-1 text-xs text-soft">可见性</div>
            <select v-model.number="contestForm.status" class="w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
              <option :value="1">公开</option>
              <option :value="0">隐藏</option>
            </select>
          </div>

          <div class="md:col-span-2">
            <div class="mb-1 text-xs text-soft">题目 ID 列表（用英文逗号分隔，例如 1001,1002,1003）</div>
            <textarea
              v-model.trim="contestForm.problemIdsText"
              class="h-24 w-full rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
              placeholder="1001,1002,1003"
            />
          </div>
        </div>

        <div class="mt-4 flex justify-end gap-2">
          <AppButton variant="secondary" size="sm" @click="closeEditor">取消</AppButton>
          <AppButton size="sm" :disabled="managing" @click="saveContest">{{ managing ? '保存中...' : '保存' }}</AppButton>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { contestApi } from '@/api'
import { useUiStore } from '@/stores/useUiStore'
import { useUserStore } from '@/stores/useUserStore'
import UserIdentity from '@/components/ui/UserIdentity.vue'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { requireLoginAction } from '@/utils/authAction'

const userStore = useUserStore()
const ui = useUiStore()
const router = useRouter()
const route = useRoute()

const loading = ref(false)
const joining = ref(false)
const managing = ref(false)
const contests = ref([])
const ranking = ref([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const size = ref(9)

const showEditor = ref(false)
const editingContestId = ref(0)
const enteringContest = ref(false)
const showContestPanel = ref(false)
const activeContest = ref({})
const activeContestProblems = ref([])
const contestForm = reactive({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  scoreboardFreezeTime: '',
  penaltyPerWrong: 20,
  status: 1,
  problemIdsText: ''
})

const canManageContest = computed(() => userStore.isTeacher || userStore.isAdmin)
const currentUserId = computed(() => Number(userStore.userInfo?.id || 0))

function statusText(v) {
  if (v === 'RUNNING') return '进行中'
  if (v === 'UPCOMING') return '未开始'
  if (v === 'ENDED') return '已结束'
  return v || '未知'
}

function statusTone(v) {
  if (v === 'RUNNING') return 'success'
  if (v === 'UPCOMING') return 'warn'
  if (v === 'ENDED') return 'neutral'
  return 'info'
}

function formatRange(start, end) {
  const s = start ? String(start).replace('T', ' ').slice(0, 19) : '-'
  const e = end ? String(end).replace('T', ' ').slice(0, 19) : '-'
  return `${s} ~ ${e}`
}

function canEditContest(event) {
  if (userStore.isAdmin) return true
  return Number(event.creatorId || 0) === currentUserId.value
}

function toDateTimeLocal(value) {
  if (!value) return ''
  const text = String(value).replace(' ', 'T')
  return text.slice(0, 16)
}

function toBackendDateTime(value) {
  if (!value) return null
  const text = String(value).trim().replace(' ', 'T')
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(text)) return `${text}:00`
  return text
}

function parseProblemIds(text) {
  if (!text) return []
  const tokens = String(text)
    .split(/[\s,，;；\n\r\t]+/)
    .map((it) => it.trim())
    .filter(Boolean)
  const ids = Array.from(new Set(tokens.map((it) => Number(it)).filter((it) => Number.isInteger(it) && it > 0)))
  return ids
}

function resetForm() {
  contestForm.title = ''
  contestForm.description = ''
  contestForm.startTime = ''
  contestForm.endTime = ''
  contestForm.scoreboardFreezeTime = ''
  contestForm.penaltyPerWrong = 20
  contestForm.status = 1
  contestForm.problemIdsText = ''
}

function openCreate() {
  editingContestId.value = 0
  resetForm()
  showEditor.value = true
}

async function openEdit(event) {
  editingContestId.value = event.id
  managing.value = true
  try {
    const res = await contestApi.getContestDetail(event.id)
    const data = res?.data || {}
    contestForm.title = data.title || ''
    contestForm.description = data.description || ''
    contestForm.startTime = toDateTimeLocal(data.startTime)
    contestForm.endTime = toDateTimeLocal(data.endTime)
    contestForm.scoreboardFreezeTime = toDateTimeLocal(data.scoreboardFreezeTime)
    contestForm.penaltyPerWrong = Number(data.penaltyPerWrong ?? 20)
    contestForm.status = Number(data.status ?? 1)
    contestForm.problemIdsText = Array.isArray(data.problems) ? data.problems.map((p) => p.id).join(',') : ''
    showEditor.value = true
  } finally {
    managing.value = false
  }
}

function closeEditor() {
  showEditor.value = false
}

function closeContestPanel() {
  showContestPanel.value = false
  activeContest.value = {}
  activeContestProblems.value = []
}

async function saveContest() {
  const problemIds = parseProblemIds(contestForm.problemIdsText)
  if (!contestForm.title.trim()) {
    await ui.alert({ message: '请输入竞赛标题' })
    return
  }
  if (!contestForm.startTime || !contestForm.endTime) {
    await ui.alert({ message: '请填写开始时间和结束时间' })
    return
  }
  if (new Date(contestForm.startTime).getTime() >= new Date(contestForm.endTime).getTime()) {
    await ui.alert({ message: '结束时间必须晚于开始时间' })
    return
  }
  if (!problemIds.length) {
    await ui.alert({ message: '请至少填写一个题目 ID' })
    return
  }

  const payload = {
    title: contestForm.title.trim(),
    description: contestForm.description || '',
    startTime: toBackendDateTime(contestForm.startTime),
    endTime: toBackendDateTime(contestForm.endTime),
    scoreboardFreezeTime: toBackendDateTime(contestForm.scoreboardFreezeTime),
    penaltyPerWrong: Number(contestForm.penaltyPerWrong ?? 20),
    status: Number(contestForm.status ?? 1),
    problemIds
  }

  managing.value = true
  try {
    if (editingContestId.value) {
      await contestApi.updateContest(editingContestId.value, payload)
    } else {
      await contestApi.createContest(payload)
    }
    closeEditor()
    await load()
  } finally {
    managing.value = false
  }
}

async function removeContest(event) {
  const ok = await ui.confirm({
    title: '删除竞赛',
    message: `确认删除竞赛「${event.title}」吗？`,
    okText: '删除',
    cancelText: '取消'
  })
  if (!ok) return
  managing.value = true
  try {
    await contestApi.deleteContest(event.id)
    await load()
  } finally {
    managing.value = false
  }
}

async function enterContest(event) {
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: route.fullPath || '/contests',
    actionText: '进入比赛'
  })
  if (!ok) {
    return
  }
  if (event.contestStatus === 'UPCOMING') {
    await ui.alert({ message: '比赛未开始，暂时不能进入做题。' })
    return
  }
  if (!event.joined && !canManageContest.value) {
    await ui.alert({ message: '请先报名再进入比赛。' })
    return
  }

  enteringContest.value = true
  try {
    const res = await contestApi.getContestDetail(event.id)
    const data = res?.data || {}
    activeContest.value = {
      id: data.id,
      title: data.title,
      startTime: data.startTime,
      endTime: data.endTime
    }
    activeContestProblems.value = Array.isArray(data.problems) ? data.problems : []
    showContestPanel.value = true
  } finally {
    enteringContest.value = false
  }
}

async function goSolve(problemId) {
  if (!problemId) return
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: route.fullPath || '/contests',
    actionText: '在比赛中做题'
  })
  if (!ok) return
  const contestId = Number(activeContest.value?.id || 0)
  closeContestPanel()
  router.push({
    path: '/studio',
    query: contestId > 0
      ? { problemId: String(problemId), contestId: String(contestId) }
      : { problemId: String(problemId) }
  })
}

async function load() {
  loading.value = true
  try {
    const res = await contestApi.getContestList({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined
    })
    contests.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

async function join(id) {
  const ok = await requireLoginAction({
    userStore,
    router,
    redirect: route.fullPath || '/contests',
    actionText: '报名竞赛'
  })
  if (!ok) {
    return
  }
  joining.value = true
  try {
    await contestApi.joinContest(id)
    await load()
  } finally {
    joining.value = false
  }
}

async function showRanking(id) {
  const res = await contestApi.getContestRanking(id, { page: 1, size: 10 })
  ranking.value = res.data?.records || []
}

function search() {
  page.value = 1
  load()
}

function reset() {
  keyword.value = ''
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

onMounted(async () => {
  await userStore.ensureUserInfo().catch(() => null)
  await load()
})
</script>
