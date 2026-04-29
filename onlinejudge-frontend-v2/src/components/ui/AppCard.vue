<template>
  <article :class="['rounded-xl border border-line bg-white shadow-card transition-colors duration-200 hover:border-slate-300', paddingClass]">
    <slot />
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  padding: { type: String, default: 'md' }
})

const paddingClass = computed(() => {
  if (props.padding === 'sm') return 'p-4'
  if (props.padding === 'lg') return 'p-7'
  return 'p-5'
})
</script>
