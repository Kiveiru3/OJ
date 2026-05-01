<template>
  <section class="space-y-6">
    <header>
      <h1 class="section-title">私信中心</h1>
    </header>

    <AppCard padding="lg">
      <div class="grid gap-4 lg:grid-cols-[320px_1fr]">
        <section class="rounded-xl border border-line bg-white p-3">
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-slate-800">最近会话</h2>
            <AppButton size="sm" variant="secondary" :disabled="threadLoading" @click="loadThreads">
              {{ threadLoading ? '刷新中...' : '刷新' }}
            </AppButton>
          </div>

          <div v-if="threadLoading" class="grid gap-2">
            <div v-for="n in 7" :key="`thread-skeleton-${n}`" class="skeleton h-14 rounded-lg" />
          </div>

          <div v-else-if="threads.length" class="max-h-[620px] space-y-2 overflow-auto pr-1">
            <article
              v-for="item in threads"
              :key="item.peerUserId"
              class="cursor-pointer rounded-lg border px-3 py-2 transition"
              :class="activePeerId === item.peerUserId ? 'border-slate-800 bg-slate-50' : 'border-line hover:border-slate-700'"
              @click="selectThread(item)"
            >
              <div class="flex items-start gap-2">
                <UserAvatar :user="{ avatar: item.peerAvatar, nickname: item.peerNickname, username: item.peerUsername, userId: item.peerUserId }" size="sm" />
                <div class="min-w-0 flex-1">
                  <div class="flex items-center justify-between gap-2">
                    <div class="truncate text-sm font-medium text-slate-800">
                      {{ item.peerNickname || item.peerUsername || `用户#${item.peerUserId}` }}
                    </div>
                    <span v-if="item.unreadCount" class="rounded-full bg-rose-500 px-2 py-0.5 text-[11px] text-white">{{ item.unreadCount }}</span>
                  </div>
                  <p class="mt-1 line-clamp-2 text-xs text-soft">{{ item.lastMessage || '暂无消息' }}</p>
                </div>
              </div>
            </article>
          </div>

          <EmptyState v-else message="暂无私信会话" />
        </section>

        <section class="rounded-xl border border-line bg-white p-3">
          <template v-if="activePeerId">
            <div class="flex items-center justify-between border-b border-line pb-3">
              <div class="flex items-center gap-2">
                <UserAvatar :user="{ avatar: activePeer.avatar, nickname: activePeer.nickname, username: activePeer.username, userId: activePeer.id }" size="sm" />
                <div class="text-sm font-semibold text-slate-800">{{ activePeer.nickname || activePeer.username || `用户#${activePeer.id}` }}</div>
              </div>
              <AppButton size="sm" variant="ghost" :disabled="messageLoading" @click="loadMessages(activePeerId)">刷新消息</AppButton>
            </div>

            <div
              ref="messageContainerRef"
              class="mt-3 h-[500px] space-y-2 overflow-auto rounded-lg border border-line bg-slate-50 p-3"
            >
              <div v-if="messageLoading" class="grid gap-2">
                <div v-for="n in 6" :key="`msg-skeleton-${n}`" class="skeleton h-16 rounded-lg" />
              </div>

              <template v-else-if="messages.length">
                <article
                  v-for="item in messages"
                  :key="item.id"
                  class="flex items-end gap-2"
                  :class="item.mine ? 'justify-end' : 'justify-start'"
                >
                  <UserAvatar
                    v-if="!item.mine"
                    :user="{ avatar: item.fromAvatar, nickname: item.fromNickname, username: item.fromUsername, userId: item.fromUserId }"
                    size="xs"
                  />
                  <div class="max-w-[78%]">
                    <div class="mb-1 flex items-center gap-1 text-[11px] text-soft" :class="item.mine ? 'justify-end' : 'justify-start'">
                      <span>{{ item.mine ? '我' : item.fromNickname || item.fromUsername || `用户#${item.fromUserId}` }}</span>
                      <span>{{ formatDate(item.createTime) }}</span>
                    </div>
                    <div
                      class="rounded-2xl px-3 py-2 text-sm leading-6"
                      :class="item.mine ? 'bg-sky-500 text-white' : 'border border-line bg-white text-slate-700'"
                    >
                      {{ item.content }}
                    </div>
                  </div>
                  <UserAvatar
                    v-if="item.mine"
                    :user="currentUserForAvatar"
                    size="xs"
                  />
                </article>
              </template>

              <EmptyState v-else message="暂无消息，开始聊天吧" />
            </div>

            <div class="mt-3 rounded-xl border border-line bg-white p-3">
              <textarea
                v-model.trim="messageDraft"
                class="h-24 w-full resize-none rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
                placeholder="输入消息，按 Ctrl + Enter 快速发送"
                @keydown.ctrl.enter.prevent="sendMessage"
              />
              <p v-if="messageError" class="mt-1 text-sm text-rose-600">{{ messageError }}</p>
              <div class="mt-2 flex justify-end">
                <AppButton :disabled="messageSending" @click="sendMessage">
                  {{ messageSending ? '发送中...' : '发送消息' }}
                </AppButton>
              </div>
            </div>
          </template>

          <EmptyState v-else message="请先从左侧选择一个会话" />
        </section>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { socialApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { useUserStore } from '@/stores/useUserStore'

const threadLoading = ref(false)
const threads = ref([])
const route = useRoute()
const userStore = useUserStore()

const activePeerId = ref(0)
const activePeer = ref({ id: 0, avatar: '', nickname: '', username: '' })

const messageLoading = ref(false)
const messageSending = ref(false)
const messageDraft = ref('')
const messageError = ref('')
const messages = ref([])
const messageContainerRef = ref(null)
const currentUserForAvatar = ref({})

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadThreads() {
  threadLoading.value = true
  try {
    const res = await socialApi.getMessageThreads({ page: 1, size: 50 })
    threads.value = res.data?.records || []
    if (!activePeerId.value && threads.value.length) {
      await selectThread(threads.value[0])
    }
  } finally {
    threadLoading.value = false
  }
}

async function loadMessages(peerUserId) {
  if (!peerUserId) return
  messageLoading.value = true
  try {
    const res = await socialApi.getMessageList({ peerUserId, page: 1, size: 100 })
    const list = res.data?.records || []
    messages.value = [...list].reverse()
    await socialApi.markConversationRead(peerUserId).catch(() => null)
    await nextTick()
    scrollToBottom()
  } finally {
    messageLoading.value = false
  }
}

function scrollToBottom() {
  if (!messageContainerRef.value) return
  messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
}

async function selectThread(item) {
  activePeerId.value = item.peerUserId
  activePeer.value = {
    id: item.peerUserId,
    avatar: item.peerAvatar || '',
    nickname: item.peerNickname || '',
    username: item.peerUsername || ''
  }
  await loadMessages(item.peerUserId)
  await loadThreads()
}

async function sendMessage() {
  messageError.value = ''
  if (!activePeerId.value) return
  if (!messageDraft.value) {
    messageError.value = '消息内容不能为空'
    return
  }
  messageSending.value = true
  try {
    await socialApi.sendMessage({
      toUserId: activePeerId.value,
      content: messageDraft.value
    })
    messageDraft.value = ''
    await Promise.all([loadMessages(activePeerId.value), loadThreads()])
  } catch (e) {
    messageError.value = e.message || '发送失败'
  } finally {
    messageSending.value = false
  }
}

onMounted(async () => {
  await userStore.ensureUserInfo().catch(() => null)
  currentUserForAvatar.value = userStore.userInfo || {}
  await loadThreads()
  const initialPeerId = Number(route.query.peerUserId || 0)
  if (initialPeerId > 0) {
    const found = threads.value.find((item) => item.peerUserId === initialPeerId)
    if (found) {
      await selectThread(found)
    } else {
      activePeerId.value = initialPeerId
      activePeer.value = { id: initialPeerId, avatar: '', nickname: '', username: `用户#${initialPeerId}` }
      await loadMessages(initialPeerId)
    }
  }
})
</script>
