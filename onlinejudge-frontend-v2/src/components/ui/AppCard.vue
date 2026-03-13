<template>
  <article :class="['rounded-xl border border-line bg-white shadow-card transition-all duration-300 hover:shadow-hover', paddingClass]">
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
