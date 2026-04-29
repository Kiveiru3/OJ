<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">管理控制台</h1>
      <p class="section-subtitle">管理员核心能力：用户、系统配置、操作日志、系统监控与判题观测。</p>
    </header>

    <div class="grid gap-4 md:grid-cols-3">
      <AppCard class="bg-[linear-gradient(135deg,#0f172a,#1e293b)] text-white">
        <div class="text-xs uppercase tracking-[0.2em] text-slate-300">Console</div>
        <div class="mt-3 text-2xl font-semibold">统一管理工作台</div>
        <div class="mt-2 text-sm text-slate-300">围绕用户、配置、论坛、监控和判题建立一站式运维视图。</div>
      </AppCard>
      <AppCard>
        <div class="text-xs text-soft">当前配置项</div>
        <div class="mt-2 text-3xl font-semibold text-slate-900">{{ configRows.length }}</div>
        <div class="mt-2 text-sm text-soft">支持在线修改站点名称、公告和竞赛默认参数。</div>
      </AppCard>
      <AppCard>
        <div class="text-xs text-soft">待处理论坛帖子</div>
        <div class="mt-2 text-3xl font-semibold text-slate-900">{{ pendingForumCount }}</div>
        <div class="mt-2 text-sm text-soft">可直接查看热门帖热度、审核状态与发布时间。</div>
      </AppCard>
    </div>

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
        <input
          v-model.trim="userQuery.keyword"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="用户名/昵称/邮箱"
        />
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
                <UserIdentity :user="item" avatar-size="xs" />
                <div class="text-xs text-soft">{{ item.nickname || '-' }} / {{ item.email || '-' }}</div>
              </td>
              <td class="px-3 py-2 text-slate-700">{{ roleLabel(item.role) }}</td>
              <td class="px-3 py-2">
                <span class="rounded-full px-2 py-1 text-xs" :class="item.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'">
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-3 py-2">
                <div class="flex flex-wrap gap-2">
                  <AppButton size="sm" variant="secondary" @click="openUserEdit(item)">编辑账号</AppButton>
                  <AppButton size="sm" variant="secondary" @click="openProfileEditor(item)">角色档案</AppButton>
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
        <AppButton size="sm" variant="secondary" :disabled="userQuery.page <= 1 || usersLoading" @click="changeUserPage(userQuery.page - 1)">
          上一页
        </AppButton>
        <span class="text-soft">第 {{ userQuery.page }} 页 / 共 {{ userTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="userQuery.page >= userTotalPages || usersLoading" @click="changeUserPage(userQuery.page + 1)">
          下一页
        </AppButton>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'configs'" class="space-y-4 overflow-hidden">
      <div class="rounded-2xl border border-slate-200 bg-[radial-gradient(circle_at_top_left,rgba(59,130,246,0.08),transparent_40%),linear-gradient(180deg,#f8fbff,#f3f6fb)] p-4">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div class="text-sm font-semibold text-slate-900">系统配置中心</div>
            <div class="mt-1 text-xs text-soft">配置项展示为中文名称，同时保留原始键名，便于排查和交接。</div>
          </div>
          <div class="rounded-full bg-white px-3 py-1 text-xs text-slate-600 shadow-sm">
            共 {{ configRows.length }} 项
          </div>
        </div>
      </div>
      <div class="flex flex-wrap items-end gap-2">
        <input
          v-model.trim="configForm.configKey"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="配置标识，如 site.name"
        />
        <input
          v-model.trim="configForm.configValue"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="配置值"
        />
        <input
          v-model.trim="configForm.description"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="描述（可选）"
        />
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
              <th class="px-3 py-2 font-medium">配置项</th>
              <th class="px-3 py-2 font-medium">配置值</th>
              <th class="px-3 py-2 font-medium">描述</th>
              <th class="px-3 py-2 font-medium">更新时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in configRows" :key="item.id || item.configKey" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2">
                <div class="font-medium text-slate-800">{{ configKeyLabel(item.configKey) }}</div>
                <div class="mt-1 text-xs text-soft">{{ item.configKey }}</div>
              </td>
              <td class="px-3 py-2 text-slate-700">{{ item.configValue || '' }}</td>
              <td class="px-3 py-2 text-soft">{{ configDescriptionText(item) }}</td>
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
        <input
          v-model.trim="logQuery.keyword"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="关键词"
        />
        <input
          v-model.trim="logQuery.module"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="模块，如 USER_MANAGE"
        />
        <input
          v-model.trim="logQuery.action"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="动作，如 UPDATE_USER"
        />
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
        <AppButton size="sm" variant="secondary" :disabled="logQuery.page <= 1 || logsLoading" @click="changeLogPage(logQuery.page - 1)">
          上一页
        </AppButton>
        <span class="text-soft">第 {{ logQuery.page }} 页 / 共 {{ logTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="logQuery.page >= logTotalPages || logsLoading" @click="changeLogPage(logQuery.page + 1)">
          下一页
        </AppButton>
      </div>
    </AppCard>

    <AppCard v-if="activeTab === 'forum'" class="space-y-4">
      <div class="flex flex-wrap items-end gap-2">
        <input
          v-model.trim="forumQuery.keyword"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="按标题或内容关键词筛选"
        />
        <input
          v-model.trim="forumQuery.problemId"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="题目 ID（可选）"
        />
        <select v-model="forumQuery.auditStatus" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800">
          <option value="">全部审核状态</option>
          <option value="0">待审核</option>
          <option value="1">已通过</option>
          <option value="2">已驳回</option>
        </select>
        <AppButton size="sm" :disabled="forumLoading" @click="fetchForumPosts">查询</AppButton>
      </div>

      <div v-if="forumLoading" class="grid gap-2">
        <div v-for="n in 8" :key="`forum-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="min-w-full text-sm">
          <thead>
            <tr class="border-b border-line text-left text-soft">
              <th class="px-3 py-2 font-medium">帖子</th>
              <th class="px-3 py-2 font-medium">作者</th>
              <th class="px-3 py-2 font-medium">关联题目</th>
              <th class="px-3 py-2 font-medium">审核状态</th>
              <th class="px-3 py-2 font-medium">热度</th>
              <th class="px-3 py-2 font-medium">发布时间</th>
              <th class="px-3 py-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in forumRows" :key="item.id" class="border-b border-line/80 hover:bg-slate-50">
              <td class="px-3 py-2 text-slate-700">
                <div class="font-medium text-slate-800">#{{ item.id }} {{ item.title }}</div>
                <div class="text-xs text-soft line-clamp-2">{{ item.contentPreview || '-' }}</div>
              </td>
              <td class="px-3 py-2">
                <UserIdentity :user="item" avatar-size="xs" />
              </td>
              <td class="px-3 py-2 text-soft">{{ item.problemId ? `#${item.problemId}` : '-' }}</td>
              <td class="px-3 py-2">
                <span
                  class="rounded-full px-2 py-1 text-xs"
                  :class="auditStatusClass(item.auditStatus)"
                >
                  {{ auditStatusText(item.auditStatus) }}
                </span>
              </td>
              <td class="px-3 py-2 text-soft">
                <div>点赞 {{ item.likeCount ?? 0 }}</div>
                <div class="text-xs text-slate-400">浏览 {{ item.viewCount ?? 0 }}</div>
              </td>
              <td class="px-3 py-2 text-soft">{{ item.createTime || '-' }}</td>
              <td class="px-3 py-2">
                <div class="flex flex-wrap gap-2">
                  <AppButton size="sm" variant="secondary" @click="openDiscussionPost(item)">查看详情</AppButton>
                  <AppButton
                    size="sm"
                    variant="secondary"
                    :disabled="forumDeleting || Number(item.auditStatus) === 1"
                    @click="auditDiscussionPost(item, 1)"
                  >
                    通过
                  </AppButton>
                  <AppButton
                    size="sm"
                    variant="secondary"
                    :disabled="forumDeleting || Number(item.auditStatus) === 2"
                    @click="auditDiscussionPost(item, 2)"
                  >
                    驳回
                  </AppButton>
                  <AppButton size="sm" variant="ghost" :disabled="forumDeleting" @click="removeDiscussionPost(item)">删除帖子</AppButton>
                </div>
              </td>
            </tr>
            <tr v-if="!forumRows.length">
              <td colspan="7" class="px-3 py-6 text-center text-sm text-soft">暂无论坛帖子</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex items-center justify-end gap-2 text-sm">
        <AppButton size="sm" variant="secondary" :disabled="forumQuery.page <= 1 || forumLoading" @click="changeForumPage(forumQuery.page - 1)">
          上一页
        </AppButton>
        <span class="text-soft">第 {{ forumQuery.page }} 页 / 共 {{ forumTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="forumQuery.page >= forumTotalPages || forumLoading" @click="changeForumPage(forumQuery.page + 1)">
          下一页
        </AppButton>
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
        <input
          v-model.trim="judgeQuery.userId"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="用户 ID"
        />
        <input
          v-model.trim="judgeQuery.problemId"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="题目 ID"
        />
        <input
          v-model.trim="judgeQuery.status"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="状态，如 ACCEPTED"
        />
        <input
          v-model.trim="judgeQuery.language"
          class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
          placeholder="语言，如 JAVA"
        />
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
              <td class="px-3 py-2 text-soft">
                <UserIdentity :user="{ username: item.username, userId: item.userId }" avatar-size="xs" />
              </td>
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
        <AppButton size="sm" variant="secondary" :disabled="judgeQuery.page <= 1 || judgeLoading" @click="changeJudgePage(judgeQuery.page - 1)">
          上一页
        </AppButton>
        <span class="text-soft">第 {{ judgeQuery.page }} 页 / 共 {{ judgeTotalPages }} 页</span>
        <AppButton size="sm" variant="secondary" :disabled="judgeQuery.page >= judgeTotalPages || judgeLoading" @click="changeJudgePage(judgeQuery.page + 1)">
          下一页
        </AppButton>
      </div>
    </AppCard>

    <div v-if="editingUser" class="fixed inset-0 z-40 flex items-center justify-center bg-black/35 p-4">
      <div class="w-full max-w-md rounded-xl bg-white p-5 shadow-card">
        <div class="text-lg font-semibold text-slate-900">编辑账号 #{{ editingUser.id }}</div>
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

    <div v-if="profileUser" class="fixed inset-0 z-40 flex items-center justify-center bg-black/35 p-4">
      <div class="w-full max-w-2xl rounded-xl bg-white p-5 shadow-card">
        <div class="flex items-center justify-between">
          <div>
            <div class="text-lg font-semibold text-slate-900">角色档案编辑</div>
            <div class="text-xs text-soft">用户 #{{ profileUser.id }} / {{ profileUser.username }} / {{ roleLabel(profileUser.role) }}</div>
          </div>
          <AppButton size="sm" variant="ghost" @click="closeProfileEditor">关闭</AppButton>
        </div>

        <div v-if="profileLoading" class="mt-4 grid gap-2">
          <div v-for="n in 6" :key="`profile-skeleton-${n}`" class="skeleton h-10 rounded-lg" />
        </div>

        <div v-else class="mt-4 grid gap-3 md:grid-cols-2">
          <input v-if="isStudentProfile" v-model.trim="profileForm.studentNo" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="学号" />
          <input v-if="isStudentProfile" v-model.trim="profileForm.className" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="班级" />
          <input v-if="isStudentProfile" v-model.trim="profileForm.major" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="专业" />

          <input v-if="isTeacherProfile" v-model.trim="profileForm.teacherNo" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="工号" />
          <input v-if="isTeacherProfile" v-model.trim="profileForm.title" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="职称" />

          <input v-if="isAdminProfile" v-model.trim="profileForm.adminCode" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="管理编号" />

          <input v-model.trim="profileForm.realName" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="真实姓名" />
          <input v-model.trim="profileForm.gender" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="性别（可选）" />
          <input v-model.trim="profileForm.department" class="rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="院系/部门（可选）" />
          <textarea v-model.trim="profileForm.bio" class="md:col-span-2 h-24 rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800" placeholder="个人简介（可选）" />
        </div>

        <div class="mt-4 flex justify-end gap-2">
          <AppButton size="sm" variant="secondary" @click="closeProfileEditor">取消</AppButton>
          <AppButton size="sm" :disabled="profileSaving || profileLoading" @click="saveProfileEditor">{{ profileSaving ? '保存中...' : '保存档案' }}</AppButton>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { adminApi, discussionApi, userApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import UserIdentity from '@/components/ui/UserIdentity.vue'
import { useUiStore } from '@/stores/useUiStore'

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'configs', label: '系统配置' },
  { key: 'logs', label: '操作日志' },
  { key: 'forum', label: '论坛管理' },
  { key: 'monitor', label: '系统监控' },
  { key: 'judge', label: '判题结果' }
]

const activeTab = ref('users')
const ui = useUiStore()

const usersLoading = ref(false)
const userRows = ref([])
const userTotal = ref(0)
const userQuery = reactive({ page: 1, size: 10, keyword: '', role: '', status: '' })

const editingUser = ref(null)
const editingForm = reactive({ role: 'STUDENT', status: 1 })
const editingSaving = ref(false)

const profileUser = ref(null)
const profileLoading = ref(false)
const profileSaving = ref(false)
const profileForm = reactive({
  studentNo: '',
  className: '',
  major: '',
  teacherNo: '',
  title: '',
  adminCode: '',
  department: '',
  realName: '',
  gender: '',
  bio: ''
})

const configsLoading = ref(false)
const configsSaving = ref(false)
const configRows = ref([])
const configForm = reactive({ configKey: '', configValue: '', description: '' })

const logsLoading = ref(false)
const logRows = ref([])
const logTotal = ref(0)
const logQuery = reactive({ page: 1, size: 10, keyword: '', module: '', action: '' })

const forumLoading = ref(false)
const forumDeleting = ref(false)
const forumRows = ref([])
const forumTotal = ref(0)
const forumQuery = reactive({ page: 1, size: 10, keyword: '', problemId: '', auditStatus: '' })

const monitorLoading = ref(false)
const monitorData = ref({})

const judgeLoading = ref(false)
const judgeRows = ref([])
const judgeTotal = ref(0)
const judgeQuery = reactive({ page: 1, size: 10, userId: '', problemId: '', status: '', language: '' })

const userTotalPages = computed(() => Math.max(1, Math.ceil(userTotal.value / userQuery.size)))
const logTotalPages = computed(() => Math.max(1, Math.ceil(logTotal.value / logQuery.size)))
const forumTotalPages = computed(() => Math.max(1, Math.ceil(forumTotal.value / forumQuery.size)))
const judgeTotalPages = computed(() => Math.max(1, Math.ceil(judgeTotal.value / judgeQuery.size)))
const pendingForumCount = computed(() => forumRows.value.filter((item) => Number(item.auditStatus) === 0).length)

const configKeyLabelMap = {
  'site.name': '站点名称',
  'site.announcement': '首页公告',
  'contest.default_page_size': '竞赛榜单默认页大小'
}

const configDescriptionLabelMap = {
  'Website display name': '网站显示名称',
  'Homepage announcement': '首页公告内容',
  'Default contest ranking page size': '竞赛榜单默认分页大小'
}

const monitorCards = computed(() => [
  { label: '用户总量', value: monitorData.value.totalUsers ?? 0 },
  { label: '启用用户', value: monitorData.value.enabledUsers ?? 0 },
  { label: '近7天新增学生', value: monitorData.value.newStudents7d ?? 0 },
  { label: '近7天新增教师', value: monitorData.value.newTeachers7d ?? 0 },
  { label: '题目总量', value: monitorData.value.totalProblems ?? 0 },
  { label: '近7天新增题目', value: monitorData.value.newProblems7d ?? 0 },
  { label: '总提交量', value: monitorData.value.totalSubmissions ?? 0 },
  { label: '今日提交', value: monitorData.value.submissionsToday ?? 0 },
  { label: '通过率', value: formatRate(monitorData.value.acceptanceRate) },
  { label: '进行中竞赛', value: monitorData.value.runningContests ?? 0 },
  { label: '判题队列状态', value: monitorData.value.queueStatus || 'UNKNOWN' }
])

const isStudentProfile = computed(() => profileUser.value?.role === 'STUDENT')
const isTeacherProfile = computed(() => profileUser.value?.role === 'TEACHER')
const isAdminProfile = computed(() => profileUser.value?.role === 'ADMIN')

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

function configKeyLabel(key) {
  return configKeyLabelMap[key] || '自定义配置'
}

function configDescriptionText(item) {
  const raw = item?.description || ''
  if (configDescriptionLabelMap[raw]) {
    return configDescriptionLabelMap[raw]
  }
  return raw || '未填写说明'
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

function normalizePageData(data) {
  return {
    records: data?.records || [],
    total: Number(data?.total || 0)
  }
}

function resetProfileForm() {
  profileForm.studentNo = ''
  profileForm.className = ''
  profileForm.major = ''
  profileForm.teacherNo = ''
  profileForm.title = ''
  profileForm.adminCode = ''
  profileForm.department = ''
  profileForm.realName = ''
  profileForm.gender = ''
  profileForm.bio = ''
}

function applyProfileData(data = {}) {
  profileForm.studentNo = data.studentNo || ''
  profileForm.className = data.className || ''
  profileForm.major = data.major || ''
  profileForm.teacherNo = data.teacherNo || ''
  profileForm.title = data.title || ''
  profileForm.adminCode = data.adminCode || ''
  profileForm.department = data.department || ''
  profileForm.realName = data.realName || ''
  profileForm.gender = data.gender || ''
  profileForm.bio = data.bio || ''
}

function buildProfilePayload() {
  const payload = {
    department: profileForm.department || '',
    realName: profileForm.realName || '',
    gender: profileForm.gender || '',
    bio: profileForm.bio || ''
  }
  if (isStudentProfile.value) {
    payload.studentNo = profileForm.studentNo || ''
    payload.className = profileForm.className || ''
    payload.major = profileForm.major || ''
  }
  if (isTeacherProfile.value) {
    payload.teacherNo = profileForm.teacherNo || ''
    payload.title = profileForm.title || ''
  }
  if (isAdminProfile.value) {
    payload.adminCode = profileForm.adminCode || ''
  }
  return payload
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
  const newPassword = window.prompt(`请输入用户 ${row.username} 的新密码（至少 6 位）`)
  if (!newPassword) return
  if (newPassword.length < 6) {
    await ui.alert({ message: '密码至少 6 位' })
    return
  }
  await userApi.adminResetPassword(row.id, { newPassword })
  await ui.alert({ message: '重置成功' })
}

async function openProfileEditor(row) {
  profileUser.value = { id: row.id, username: row.username, role: row.role || 'STUDENT' }
  profileLoading.value = true
  resetProfileForm()
  try {
    const res = await userApi.adminGetRoleProfile(row.id)
    const data = res?.data || {}
    if (data.role) {
      profileUser.value.role = String(data.role).toUpperCase()
    }
    applyProfileData(data)
  } finally {
    profileLoading.value = false
  }
}

function closeProfileEditor() {
  profileUser.value = null
  profileLoading.value = false
  profileSaving.value = false
}

async function saveProfileEditor() {
  if (!profileUser.value) return
  profileSaving.value = true
  try {
    await userApi.adminUpdateRoleProfile(profileUser.value.id, buildProfilePayload())
    closeProfileEditor()
  } finally {
    profileSaving.value = false
  }
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
    await ui.alert({ message: '配置键不能为空' })
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

async function fetchForumPosts() {
  forumLoading.value = true
  try {
    const pid = Number(forumQuery.problemId)
    const hasAuditFilter = String(forumQuery.auditStatus).trim() !== ''
    const audit = hasAuditFilter ? Number(forumQuery.auditStatus) : NaN
    const params = {
      page: forumQuery.page,
      size: forumQuery.size,
      keyword: forumQuery.keyword || undefined,
      problemId: Number.isFinite(pid) && pid > 0 ? pid : undefined,
      auditStatus: Number.isFinite(audit) && audit >= 0 ? audit : undefined
    }
    const res = await discussionApi.getPostList(params)
    const pageData = normalizePageData(res?.data)
    forumRows.value = pageData.records
    forumTotal.value = pageData.total
  } finally {
    forumLoading.value = false
  }
}

function changeForumPage(page) {
  forumQuery.page = page
  fetchForumPosts()
}

function openDiscussionPost(row) {
  if (!row?.id) return
  window.open(`/discuss/${row.id}`, '_blank')
}

async function auditDiscussionPost(row, status) {
  if (!row?.id) return
  const actionText = status === 1 ? '通过' : '驳回'
  const ok = await ui.confirm({
    title: '帖子审核',
    message: `确认${actionText}帖子「${row.title}」吗？`,
    okText: actionText,
    cancelText: '取消'
  })
  if (!ok) return

  let auditRemark = ''
  if (status === 2) {
    auditRemark = window.prompt('请输入驳回原因（可选）') || ''
  }

  forumDeleting.value = true
  try {
    await discussionApi.auditPost(row.id, { auditStatus: status, auditRemark })
    await fetchForumPosts()
  } finally {
    forumDeleting.value = false
  }
}

async function removeDiscussionPost(row) {
  if (!row?.id) return
  const ok = await ui.confirm({
    title: '删除帖子',
    message: `确定删除帖子「${row.title}」吗？`,
    okText: '删除',
    cancelText: '取消'
  })
  if (!ok) return
  forumDeleting.value = true
  try {
    await discussionApi.deletePost(row.id)
    await fetchForumPosts()
  } finally {
    forumDeleting.value = false
  }
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
  if (tab === 'forum') fetchForumPosts()
  if (tab === 'monitor') fetchMonitor()
  if (tab === 'judge') fetchJudgeResults()
})

onMounted(async () => {
  await fetchUsers()
})
</script>
