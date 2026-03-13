<template>
  <span :class="badgeClass"><slot /></span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  tone: { type: String, default: 'neutral' }
})

const toneMap = {
  neutral: 'bg-slate-100 text-slate-700',
  info: 'bg-sky-100 text-sky-700',
  success: 'bg-emerald-100 text-emerald-700',
  warn: 'bg-amber-100 text-amber-700',
  danger: 'bg-rose-100 text-rose-700'
}

const badgeClass = computed(() => [
  'inline-flex items-center rounded-md px-2.5 py-1 text-xs font-medium',
  toneMap[props.tone] || toneMap.neutral
])
</script>
