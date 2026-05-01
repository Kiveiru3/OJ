<template>
  <section class="space-y-6">
    <header class="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
      <div>
        <h1 class="section-title">AI 助手</h1>
      </div>
      <RouterLink v-if="problemId" :to="{ path: '/studio', query: { problemId: String(problemId) } }">
        <AppButton variant="secondary">
          <ArrowLeft :size="16" />
          返回做题
        </AppButton>
      </RouterLink>
    </header>

    <AppCard padding="lg">
      <div class="grid gap-4 lg:grid-cols-[300px_1fr]">
        <aside class="rounded-xl border border-line bg-slate-50 p-4">
          <div class="flex items-center gap-3">
            <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-slate-900 text-white">
              <Bot :size="22" />
            </div>
            <div>
              <h2 class="text-sm font-semibold text-slate-900">在线判题助手</h2>
              <p class="mt-1 text-xs text-soft">提示优先，不替你直接交卷。</p>
            </div>
          </div>

          <div class="mt-4 rounded-lg border border-line bg-white p-3 text-sm">
            <div class="text-xs font-medium text-soft">当前场景</div>
            <div class="mt-1 font-semibold text-slate-800">{{ sceneTitle }}</div>
          </div>

          <div class="mt-4 grid gap-2">
            <button
              v-for="item in quickPrompts"
              :key="item"
              type="button"
              class="rounded-lg border border-line bg-white px-3 py-2 text-left text-xs text-slate-700 transition hover:border-slate-400 hover:bg-slate-100"
              @click="useQuickPrompt(item)"
            >
              {{ item }}
            </button>
          </div>
        </aside>

        <section class="rounded-xl border border-line bg-white p-3">
          <div ref="messageContainerRef" class="h-[560px] overflow-auto rounded-lg border border-line bg-slate-50 p-4">
            <div class="space-y-3">
              <article
                v-for="item in messages"
                :key="item.id"
                class="flex items-end gap-2"
                :class="item.role === 'user' ? 'justify-end' : 'justify-start'"
              >
                <div
                  v-if="item.role === 'assistant'"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-900 text-white"
                >
                  <Bot :size="16" />
                </div>

                <div class="max-w-[78%]">
                  <div
                    class="mb-1 flex items-center gap-1 text-[11px] text-soft"
                    :class="item.role === 'user' ? 'justify-end' : 'justify-start'"
                  >
                    <span>{{ item.role === 'user' ? '我' : 'AI 助手' }}</span>
                    <span>{{ item.time }}</span>
                  </div>
                  <div
                    class="whitespace-pre-wrap rounded-2xl px-3 py-2 text-sm leading-6"
                    :class="item.role === 'user' ? 'bg-sky-500 text-white' : 'border border-line bg-white text-slate-700'"
                  >
                    {{ item.content }}
                  </div>
                </div>

                <div
                  v-if="item.role === 'user'"
                  class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-line bg-white text-slate-700"
                >
                  <UserRound :size="16" />
                </div>
              </article>

              <article v-if="sending" class="flex items-end gap-2">
                <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-slate-900 text-white">
                  <Bot :size="16" />
                </div>
                <div class="rounded-2xl border border-line bg-white px-3 py-2 text-sm text-slate-600">
                  <Loader2 class="inline animate-spin" :size="15" />
                  正在思考...
                </div>
              </article>
            </div>
          </div>

          <div class="mt-3 rounded-xl border border-line bg-white p-3">
            <textarea
              v-model.trim="draft"
              class="h-24 w-full resize-none rounded-lg border border-line px-3 py-2 text-sm outline-none focus:border-slate-800"
              placeholder="输入问题，按 Ctrl + Enter 发送"
              @keydown.ctrl.enter.prevent="send"
            />
            <p v-if="errorMessage" class="mt-1 text-sm text-rose-600">{{ errorMessage }}</p>
            <div class="mt-2 flex items-center justify-between gap-3">
              <p class="text-xs text-soft">建议先问思路、边界条件或错误原因。</p>
              <AppButton :disabled="sending || !draft" @click="send">
                <Send :size="16" />
                {{ sending ? '发送中' : '发送' }}
              </AppButton>
            </div>
          </div>
        </section>
      </div>
    </AppCard>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeft, Bot, Loader2, Send, UserRound } from 'lucide-vue-next'
import { aiApi } from '@/api'
import AppButton from '@/components/ui/AppButton.vue'
import AppCard from '@/components/ui/AppCard.vue'

const route = useRoute()
const problemId = computed(() => Number(route.query.problemId || 0))
const problemTitle = computed(() => String(route.query.problemTitle || '').trim())
const sceneTitle = computed(() => {
  if (problemId.value) {
    return problemTitle.value || `题目 #${problemId.value}`
  }
  return '通用学习问答'
})

const messages = ref([])
const draft = ref('')
const sending = ref(false)
const errorMessage = ref('')
const messageContainerRef = ref(null)

const quickPrompts = [
  '帮我分析这道题的核心思路',
  '这题容易漏掉哪些边界条件？',
  '我的代码为什么可能会超时？',
  '给我一个提示，不要直接给完整答案'
]

function nowText() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function addMessage(role, content) {
  messages.value.push({
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    time: nowText()
  })
}

function scrollToBottom() {
  if (!messageContainerRef.value) return
  messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight
}

function buildScene() {
  if (!problemId.value) return 'OJ AI chat'
  return `OJ problem assistant. Current problem: ${sceneTitle.value}. Problem id: ${problemId.value}.`
}

function useQuickPrompt(text) {
  draft.value = text
}

async function send() {
  errorMessage.value = ''
  const message = draft.value.trim()
  if (!message || sending.value) return

  addMessage('user', message)
  draft.value = ''
  sending.value = true
  await nextTick()
  scrollToBottom()

  try {
    const res = await aiApi.chat({
      message,
      scene: buildScene()
    })
    addMessage('assistant', res.data?.content || 'AI 没有返回内容。')
  } catch (e) {
    errorMessage.value = e.message || 'AI 请求失败'
    addMessage('assistant', `请求失败：${errorMessage.value}`)
  } finally {
    sending.value = false
    await nextTick()
    scrollToBottom()
  }
}

onMounted(async () => {
  addMessage(
    'assistant',
    problemId.value
      ? `我已经进入 ${sceneTitle.value} 的辅助模式。你可以问我题意、思路、边界条件或调试方向。`
      : '你好，我是在线判题系统里的 AI 助手。你可以问我算法思路、代码调试或学习规划。'
  )
  await nextTick()
  scrollToBottom()
})
</script>
