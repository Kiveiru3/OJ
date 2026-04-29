<template>
  <img
    :src="resolvedSrc"
    :alt="altText"
    :class="['rounded-full object-cover ring-1 ring-black/5', sizeClass]"
    loading="lazy"
    @error="onImgError"
  />
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getAvatarUrl, getDisplayName } from '@/utils/avatar'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  },
  size: {
    type: String,
    default: 'md'
  }
})

const sizeMap = {
  xs: 'h-5 w-5',
  sm: 'h-7 w-7',
  md: 'h-9 w-9',
  lg: 'h-12 w-12',
  xl: 'h-16 w-16'
}

const fallbackSrc = computed(() => getAvatarUrl(props.user))
const safeSrc = ref(fallbackSrc.value)

watch(
  () => [props.user?.avatar, props.user?.nickname, props.user?.username, props.user?.id, props.user?.userId],
  () => {
    safeSrc.value = fallbackSrc.value
  }
)

const resolvedSrc = computed(() => safeSrc.value)
const sizeClass = computed(() => sizeMap[props.size] || sizeMap.md)
const altText = computed(() => `${getDisplayName(props.user)} 的头像`)

function onImgError() {
  safeSrc.value = getAvatarUrl({ ...props.user, avatar: '' })
}
</script>
