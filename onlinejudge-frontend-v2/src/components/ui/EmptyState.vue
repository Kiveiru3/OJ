<template>
  <div class="rounded-xl border border-dashed border-line bg-white/60 p-10 text-center">
    <p class="text-sm text-soft">{{ message }}</p>
  </div>
</template>

<script setup>
defineProps({
  message: { type: String, default: '暂无数据' }
})
</script>
