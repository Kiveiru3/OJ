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
  primary: 'bg-ink text-white hover:bg-black',
  secondary: 'bg-white text-ink border border-line hover:border-ink',
  ghost: 'bg-transparent text-ink hover:bg-black/5'
}

const sizeMap = {
  sm: 'h-8 px-3 text-xs',
  md: 'h-10 px-4 text-sm',
  lg: 'h-12 px-5 text-sm'
}

const classes = computed(() => [
  'inline-flex items-center justify-center rounded-lg font-medium transition-all duration-200 active:scale-[0.98]',
  variantMap[props.variant] || variantMap.primary,
  sizeMap[props.size] || sizeMap.md,
  props.block ? 'w-full' : ''
])
</script>
