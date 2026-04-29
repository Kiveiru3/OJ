<template>
  <button :class="classes">
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: { type: String, default: 'primary' },
  size: { type: String, default: 'md' },
  block: { type: Boolean, default: false }
})

const variantMap = {
  primary: 'border border-slate-900 bg-slate-900 text-white hover:border-slate-800 hover:bg-slate-800',
  secondary: 'border border-line bg-white text-ink hover:border-slate-400 hover:bg-slate-50',
  ghost: 'bg-transparent text-ink hover:bg-slate-100'
}

const sizeMap = {
  sm: 'h-8 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
  lg: 'h-12 px-5 text-sm'
}

const classes = computed(() => [
  'inline-flex items-center justify-center rounded-lg font-medium transition-colors duration-200 active:scale-[0.98]',
  variantMap[props.variant] || variantMap.primary,
  sizeMap[props.size] || sizeMap.md,
  props.block ? 'w-full' : ''
])
</script>
