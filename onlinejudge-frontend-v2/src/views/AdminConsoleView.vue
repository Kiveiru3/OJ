<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">管理控制台</h1>
      <p class="section-subtitle">管理员核心能力：用户、配置、日志、系统监控与判题观测。</p>
    </header>

    <AppCard>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="item in tabs"
          :key="item.key"
          class="rounded-lg px-3 py-2 text-sm transition"
          :class="item.key === activeTab ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'"
          @click="activeTab = item.key"
        >
          {{ item.label }}
        </button>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'users'" class="space-y-4">
      <div class="flex flex-wrap items-end gap-2">
        <input v-model.trim="userQuery.keyword" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="用户名/昵称/邮箱" />
        <select v-model="userQuery.role" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
          <option value="">全部角色</option>
          <option value="STUDENT">学生</option>
          <option value="TEACHER">教师</option>
          <option value="ADMIN">管理员</option>
        </select>
        <select v-model="userQuery.status" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">禁用</option>
        </select>
        <AppButton size="sm" :disabled="usersLoading" @click="fetchUsers">查询</AppButton>
      </div>

      <div v-if="usersLoading" class="grid gap-2">
        <div v-for="n in 8" :key="`user-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="border-b border-line text-left text-soft">
              <th class="px-3 py-2 font-medium">ID</th>
              <th class="px-3 py-2 font-medium">账号</th>
              <th class="px-3 py-2 font-medium">角色</th>
              <th class="px-3 py-2 font-medium">状态</th>
              <th class="px-3 py-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in userRows" :key="item.id" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2 text-slate-700">{{ item.id }}</td>
              <td class="px-3 py-2">
                <div class="font-medium text-slate-800">{{ item.username }}</div>
                <div class="text-xs text-soft">{{ item.nickname || '-' }} · {{ item.email || '-' }}</div>
              </td>
              <td class="px-3 py-2 text-slate-700">{{ roleLabel(item.role) }}</td>
              <td class="px-3 py-2">
                <span class="rounded-full px-2 py-1 text-xs" :class="item.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-3 py-2">
                <div class="flex flex-wrap gap-2">
                  <AppButton size="sm" variant="secondary" @click="openUserEdit(item)">编辑</AppButton>
                  <AppButton size="sm" variant="ghost" @click="quickResetPassword(item)">重置密码</AppButton>
                </div>
              </td>
            </tr>
            <tr v-if="!userRows.length">
              <td colspan="5" class="px-3 py-6 text-center text-sm text-soft">暂无用户数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-end gap-2 text-sm">
        <AppButton size="sm" variant="secondary" :disabled="userQuery.page <= 1 || usersLoading" @click="changeUserPage(userQuery.page - 1)">上一页</AppButton>
        <span class="text-soft">第 {{ userQuery.page }} 页 / 共 {{ userTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="userQuery.page >= userTotalPages || usersLoading" @click="changeUserPage(userQuery.page + 1)">下一页</AppButton>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'configs'" class="space-y-4">
      <div class="flex flex-wrap items-end gap-2">
        <input v-model.trim="configForm.configKey" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="配置键，如 site.name" />
        <input v-model.trim="configForm.configValue" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="配置值" />
        <input v-model.trim="configForm.description" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="描述（可选）" />
        <AppButton size="sm" :disabled="configsSaving" @click="saveConfig">{{ configsSaving ? '保存中...' : '保存配置' }}</AppButton>
        <AppButton size="sm" variant="secondary" :disabled="configsLoading" @click="fetchConfigs">刷新</AppButton>
      </div>

      <div v-if="configsLoading" class="grid gap-2">
        <div v-for="n in 6" :key="`config-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="border-b border-line text-left text-soft">
              <th class="px-3 py-2 font-medium">配置键</th>
              <th class="px-3 py-2 font-medium">配置值</th>
              <th class="px-3 py-2 font-medium">描述</th>
              <th class="px-3 py-2 font-medium">更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in configRows" :key="item.id || item.configKey" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2 text-slate-800">{{ item.configKey }}</td>
              <td class="px-3 py-2 text-slate-700">{{ item.configValue || '' }}</td>
              <td class="px-3 py-2 text-soft">{{ item.description || '-' }}</td>
              <td class="px-3 py-2 text-soft">{{ item.updateTime || '-' }}</td>
            </tr>
            <tr v-if="!configRows.length">
              <td colspan="4" class="px-3 py-6 text-center text-sm text-soft">暂无配置数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'logs'" class="space-y-4">
      <div class="flex flex-wrap items-end gap-2">
        <input v-model.trim="logQuery.keyword" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="关键字" />
        <input v-model.trim="logQuery.module" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="模块，如 USER_MANAGE" />
        <input v-model.trim="logQuery.action" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="动作，如 UPDATE_USER" />
        <AppButton size="sm" :disabled="logsLoading" @click="fetchLogs">查询</AppButton>
      </div>

      <div v-if="logsLoading" class="grid gap-2">
        <div v-for="n in 8" :key="`log-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="border-b border-line text-left text-soft">
              <th class="px-3 py-2 font-medium">时间</th>
              <th class="px-3 py-2 font-medium">操作人</th>
              <th class="px-3 py-2 font-medium">模块/动作</th>
              <th class="px-3 py-2 font-medium">目标</th>
              <th class="px-3 py-2 font-medium">详情</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in logRows" :key="item.id" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2 text-soft">{{ item.createTime || '-' }}</td>
              <td class="px-3 py-2 text-slate-700">{{ item.operatorUsername || '-' }}</td>
              <td class="px-3 py-2 text-slate-700">{{ item.module || '-' }} / {{ item.action || '-' }}</td>
              <td class="px-3 py-2 text-soft">{{ item.targetType || '-' }} #{{ item.targetId || '-' }}</td>
              <td class="px-3 py-2 text-soft">{{ item.detail || '-' }}</td>
            </tr>
            <tr v-if="!logRows.length">
              <td colspan="5" class="px-3 py-6 text-center text-sm text-soft">暂无日志数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-end gap-2 text-sm">
        <AppButton size="sm" variant="secondary" :disabled="logQuery.page <= 1 || logsLoading" @click="changeLogPage(logQuery.page - 1)">上一页</AppButton>
        <span class="text-soft">第 {{ logQuery.page }} 页 / 共 {{ logTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="logQuery.page >= logTotalPages || logsLoading" @click="changeLogPage(logQuery.page + 1)">下一页</AppButton>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'monitor'" class="space-y-4">
      <div class="flex justify-end">
        <AppButton size="sm" variant="secondary" :disabled="monitorLoading" @click="fetchMonitor">刷新监控</AppButton>
      </div>
      <div v-if="monitorLoading" class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="n in 8" :key="`monitor-skeleton-${n}`" class="skeleton h-20 rounded-lg" />
      </div>
      <div v-else class="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <div v-for="item in monitorCards" :key="item.label" class="rounded-lg border border-line bg-white p-4">
          <div class="text-xs text-soft">{{ item.label }}</div>
          <div class="mt-2 text-xl font-semibold text-slate-900">{{ item.value }}</div>
        </div>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'judge'" class="space-y-4">
      <div class="flex flex-wrap items-end gap-2">
        <input v-model.trim="judgeQuery.userId" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="用户 ID" />
        <input v-model.trim="judgeQuery.problemId" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="题目 ID" />
        <input v-model.trim="judgeQuery.status" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="状态，如 ACCEPTED" />
        <input v-model.trim="judgeQuery.language" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="语言，如 JAVA" />
        <AppButton size="sm" :disabled="judgeLoading" @click="fetchJudgeResults">查询</AppButton>
      </div>

      <div v-if="judgeLoading" class="grid gap-2">
        <div v-for="n in 8" :key="`judge-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="border-b border-line text-left text-soft">
              <th class="px-3 py-2 font-medium">提交 ID</th>
              <th class="px-3 py-2 font-medium">用户</th>
              <th class="px-3 py-2 font-medium">题目</th>
              <th class="px-3 py-2 font-medium">语言</th>
              <th class="px-3 py-2 font-medium">结果</th>
              <th class="px-3 py-2 font-medium">资源</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in judgeRows" :key="item.id" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2 text-slate-700">#{{ item.submissionId || '-' }}</td>
              <td class="px-3 py-2 text-soft">{{ item.username || '-' }} ({{ item.userId || '-' }})</td>
              <td class="px-3 py-2 text-soft">{{ item.problemTitle || '-' }} ({{ item.problemId || '-' }})</td>
              <td class="px-3 py-2 text-soft">{{ item.language || '-' }}</td>
              <td class="px-3 py-2 text-slate-700">{{ item.status || '-' }}</td>
              <td class="px-3 py-2 text-soft">{{ item.timeUsed ?? 0 }}ms / {{ item.memoryUsed ?? 0 }}KB</td>
            </tr>
            <tr v-if="!judgeRows.length">
              <td colspan="6" class="px-3 py-6 text-center text-sm text-soft">暂无判题结果</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-end gap-2 text-sm">
        <AppButton size="sm" variant="secondary" :disabled="judgeQuery.page <= 1 || judgeLoading" @click="changeJudgePage(judgeQuery.page - 1)">上一页</AppButton>
        <span class="text-soft">第 {{ judgeQuery.page }} 页 / 共 {{ judgeTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="judgeQuery.page >= judgeTotalPages || judgeLoading" @click="changeJudgePage(judgeQuery.page + 1)">下一页</AppButton>
      </div>
    </AppCard>

    <div v-if="editingUser" class="fixed inset-0 z-40 flex items-center justify-center bg-black/35 p-4">
      <div class="w-full max-w-md rounded-xl bg-white p-5 shadow-card">
        <div class="text-lg font-semibold text-slate-900">编辑用户 #{{ editingUser.id }}</div>
        <div class="mt-3 grid gap-3">
          <select v-model="editingForm.role" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
            <option value="STUDENT">学生</option>
            <option value="TEACHER">教师</option>
            <option value="ADMIN">管理员</option>
          </select>
          <select v-model.number="editingForm.status" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </div>
        <div class="mt-4 flex justify-end gap-2">
          <AppButton size="sm" variant="secondary" @click="editingUser = null">取消</AppButton>
          <AppButton size="sm" :disabled="editingSaving" @click="saveUserEdit">{{ editingSaving ? '保存中...' : '保存' }}</AppButton>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { adminApi, userApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'configs', label: '系统配置' },
  { key: 'logs', label: '操作日志' },
  { key: 'monitor', label: '系统监控' },
  { key: 'judge', label: '判题结果' }
]

const activeTab = ref('users')

const usersLoading = ref(false)
const userRows = ref([])
const userTotal = ref(0)
const userQuery = reactive({ page: 1, size: 10, keyword: '', role: '', status: '' })

const editingUser = ref(null)
const editingForm = reactive({ role: 'STUDENT', status: 1 })
const editingSaving = ref(false)

const configsLoading = ref(false)
const configsSaving = ref(false)
const configRows = ref([])
const configForm = reactive({ configKey: '', configValue: '', description: '' })

const logsLoading = ref(false)
const logRows = ref([])
const logTotal = ref(0)
const logQuery = reactive({ page: 1, size: 10, keyword: '', module: '', action: '' })

const monitorLoading = ref(false)
const monitorData = ref({})

const judgeLoading = ref(false)
const judgeRows = ref([])
const judgeTotal = ref(0)
const judgeQuery = reactive({ page: 1, size: 10, userId: '', problemId: '', status: '', language: '' })

const userTotalPages = computed(() => Math.max(1, Math.ceil(userTotal.value / userQuery.size)))
const logTotalPages = computed(() => Math.max(1, Math.ceil(logTotal.value / logQuery.size)))
const judgeTotalPages = computed(() => Math.max(1, Math.ceil(judgeTotal.value / judgeQuery.size)))

const monitorCards = computed(() => [
  { label: '用户总量', value: monitorData.value.totalUsers ?? 0 },
  { label: '启用用户', value: monitorData.value.enabledUsers ?? 0 },
  { label: '题目总量', value: monitorData.value.totalProblems ?? 0 },
  { label: '总提交量', value: monitorData.value.totalSubmissions ?? 0 },
  { label: '今日提交', value: monitorData.value.submissionsToday ?? 0 },
  { label: '通过率', value: formatRate(monitorData.value.acceptanceRate) },
  { label: '运行中竞赛', value: monitorData.value.runningContests ?? 0 },
  { label: '判题队列状态', value: monitorData.value.queueStatus || 'UNKNOWN' }
])

function roleLabel(role) {
  if (role === 'ADMIN') return '管理员'
  if (role === 'TEACHER') return '教师'
  if (role === 'STUDENT') return '学生'
  return role || '-'
}

function formatRate(rate) {
  if (rate === null || rate === undefined || Number.isNaN(Number(rate))) return '0.00%'
  return `${Number(rate).toFixed(2)}%`
}

function normalizePageData(data) {
  return {
    records: data?.records || [],
    total: Number(data?.total || 0)
  }
}

async function fetchUsers() {
  usersLoading.value = true
  try {
    const params = {
      page: userQuery.page,
      size: userQuery.size,
      keyword: userQuery.keyword || undefined,
      role: userQuery.role || undefined,
      status: userQuery.status === '' ? undefined : Number(userQuery.status)
    }
    const res = await userApi.getUserList(params)
    const pageData = normalizePageData(res?.data)
    userRows.value = pageData.records
    userTotal.value = pageData.total
  } finally {
    usersLoading.value = false
  }
}

function changeUserPage(page) {
  userQuery.page = page
  fetchUsers()
}

function openUserEdit(row) {
  editingUser.value = row
  editingForm.role = row.role || 'STUDENT'
  editingForm.status = Number(row.status ?? 1)
}

async function saveUserEdit() {
  if (!editingUser.value) return
  editingSaving.value = true
  try {
    await userApi.adminUpdateUser(editingUser.value.id, { role: editingForm.role, status: editingForm.status })
    editingUser.value = null
    await fetchUsers()
  } finally {
    editingSaving.value = false
  }
}

async function quickResetPassword(row) {
  const newPassword = window.prompt(`请输入用户 ${row.username} 的新密码（至少6位）`)
  if (!newPassword) return
  if (newPassword.length < 6) {
    window.alert('密码至少 6 位')
    return
  }
  await userApi.adminResetPassword(row.id, { newPassword })
  window.alert('重置成功')
}

async function fetchConfigs() {
  configsLoading.value = true
  try {
    const res = await adminApi.getConfigs()
    configRows.value = Array.isArray(res?.data) ? res.data : []
  } finally {
    configsLoading.value = false
  }
}

async function saveConfig() {
  if (!configForm.configKey.trim()) {
    window.alert('配置键不能为空')
    return
  }
  configsSaving.value = true
  try {
    await adminApi.upsertConfig({
      configKey: configForm.configKey.trim(),
      configValue: configForm.configValue || '',
      description: configForm.description || ''
    })
    configForm.configValue = ''
    configForm.description = ''
    await fetchConfigs()
  } finally {
    configsSaving.value = false
  }
}

async function fetchLogs() {
  logsLoading.value = true
  try {
    const params = {
      page: logQuery.page,
      size: logQuery.size,
      keyword: logQuery.keyword || undefined,
      module: logQuery.module || undefined,
      action: logQuery.action || undefined
    }
    const res = await adminApi.getLogs(params)
    const pageData = normalizePageData(res?.data)
    logRows.value = pageData.records
    logTotal.value = pageData.total
  } finally {
    logsLoading.value = false
  }
}

function changeLogPage(page) {
  logQuery.page = page
  fetchLogs()
}

async function fetchMonitor() {
  monitorLoading.value = true
  try {
    const res = await adminApi.getMonitor()
    monitorData.value = res?.data || {}
  } finally {
    monitorLoading.value = false
  }
}

async function fetchJudgeResults() {
  judgeLoading.value = true
  try {
    const params = {
      page: judgeQuery.page,
      size: judgeQuery.size,
      userId: judgeQuery.userId || undefined,
      problemId: judgeQuery.problemId || undefined,
      status: judgeQuery.status || undefined,
      language: judgeQuery.language || undefined
    }
    const res = await adminApi.getJudgeResults(params)
    const pageData = normalizePageData(res?.data)
    judgeRows.value = pageData.records
    judgeTotal.value = pageData.total
  } finally {
    judgeLoading.value = false
  }
}

function changeJudgePage(page) {
  judgeQuery.page = page
  fetchJudgeResults()
}

watch(activeTab, (tab) => {
  if (tab === 'users') fetchUsers()
  if (tab === 'configs') fetchConfigs()
  if (tab === 'logs') fetchLogs()
  if (tab === 'monitor') fetchMonitor()
  if (tab === 'judge') fetchJudgeResults()
})

onMounted(async () => {
  await fetchUsers()
})
</script>
