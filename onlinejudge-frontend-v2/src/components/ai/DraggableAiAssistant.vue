<template>
  <button
    type="button"
    class="ai-assistant"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    aria-label="打开 AI 助手"
    title="AI 助手"
    @pointerdown="startDrag"
    @click="openAiChat"
  >
    <span class="ai-assistant__halo" />
    <span class="ai-assistant__icon">
      <Bot :size="22" />
    </span>
    <span class="ai-assistant__text">
      <strong>AI</strong>
      <small>助手</small>
    </span>
    <GripVertical class="ai-assistant__grip" :size="15" />
  </button>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bot, GripVertical } from 'lucide-vue-next'

const props = defineProps({
  problemId: { type: [Number, String], default: 0 },
  problemTitle: { type: String, default: '' }
})

const router = useRouter()
const STORAGE_KEY = 'ojv2:ai-assistant-position'
const SIZE = { width: 118, height: 56 }
const EDGE_PADDING = 16

const position = reactive({ x: 0, y: 0 })
const dragState = reactive({
  active: false,
  moved: false,
  pointerId: 0,
  offsetX: 0,
  offsetY: 0
})
const suppressClick = ref(false)

function defaultPosition() {
  return {
    x: Math.max(EDGE_PADDING, window.innerWidth - SIZE.width - 32),
    y: Math.max(EDGE_PADDING, window.innerHeight - SIZE.height - 120)
  }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function clampPosition() {
  position.x = clamp(position.x, EDGE_PADDING, Math.max(EDGE_PADDING, window.innerWidth - SIZE.width - EDGE_PADDING))
  position.y = clamp(position.y, EDGE_PADDING, Math.max(EDGE_PADDING, window.innerHeight - SIZE.height - EDGE_PADDING))
}

function restorePosition() {
  const fallback = defaultPosition()
  position.x = fallback.x
  position.y = fallback.y
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const saved = JSON.parse(raw)
    if (Number.isFinite(saved?.x) && Number.isFinite(saved?.y)) {
      position.x = saved.x
      position.y = saved.y
      clampPosition()
    }
  } catch (_) {
    window.localStorage.removeItem(STORAGE_KEY)
  }
}

function savePosition() {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ x: position.x, y: position.y }))
}

function startDrag(event) {
  if (event.button !== 0) return
  dragState.active = true
  dragState.moved = false
  dragState.pointerId = event.pointerId
  dragState.offsetX = event.clientX - position.x
  dragState.offsetY = event.clientY - position.y
  event.currentTarget.setPointerCapture?.(event.pointerId)
  document.addEventListener('pointermove', handleDrag)
  document.addEventListener('pointerup', stopDrag)
}

function handleDrag(event) {
  if (!dragState.active || event.pointerId !== dragState.pointerId) return
  const nextX = event.clientX - dragState.offsetX
  const nextY = event.clientY - dragState.offsetY
  if (Math.abs(nextX - position.x) > 2 || Math.abs(nextY - position.y) > 2) {
    dragState.moved = true
  }
  position.x = nextX
  position.y = nextY
  clampPosition()
}

function stopDrag(event) {
  if (event.pointerId !== dragState.pointerId) return
  document.removeEventListener('pointermove', handleDrag)
  document.removeEventListener('pointerup', stopDrag)
  if (dragState.moved) {
    savePosition()
    suppressClick.value = true
    window.setTimeout(() => {
      suppressClick.value = false
    }, 0)
  }
  dragState.active = false
}

function openAiChat() {
  if (suppressClick.value) return
  const query = {}
  const id = Number(props.problemId || 0)
  if (id > 0) query.problemId = String(id)
  if (props.problemTitle) query.problemTitle = props.problemTitle
  router.push({ name: 'aiChat', query })
}

onMounted(() => {
  restorePosition()
  window.addEventListener('resize', clampPosition)
})

onBeforeUnmount(() => {
  document.removeEventListener('pointermove', handleDrag)
  document.removeEventListener('pointerup', stopDrag)
  window.removeEventListener('resize', clampPosition)
})
</script>

<style scoped>
.ai-assistant {
  position: fixed;
  z-index: 50;
  width: 118px;
  height: 56px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(15, 23, 42, 0.14);
  border-radius: 14px;
  padding: 8px 10px;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(14px);
  cursor: grab;
  user-select: none;
  touch-action: none;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.ai-assistant:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 23, 42, 0.34);
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.22);
}

.ai-assistant:active {
  cursor: grabbing;
  transform: scale(0.98);
}

.ai-assistant__halo {
  position: absolute;
  inset: -5px;
  z-index: -1;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.34), rgba(59, 130, 246, 0.26), rgba(245, 158, 11, 0.22));
  filter: blur(12px);
  opacity: 0.8;
}

.ai-assistant__icon {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #ecfeff;
  background: #0f172a;
}

.ai-assistant__text {
  display: grid;
  min-width: 0;
  text-align: left;
  line-height: 1.05;
}

.ai-assistant__text strong {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0;
}

.ai-assistant__text small {
  margin-top: 3px;
  color: #64748b;
  font-size: 11px;
}

.ai-assistant__grip {
  color: #94a3b8;
}

@media (max-width: 640px) {
  .ai-assistant {
    width: 58px;
    border-radius: 16px;
    padding: 10px;
  }

  .ai-assistant__text,
  .ai-assistant__grip {
    display: none;
  }
}
</style>
