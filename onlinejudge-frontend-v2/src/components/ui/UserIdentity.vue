<template>
  <button
    v-if="canNavigate"
    type="button"
    class="inline-flex min-w-0 items-center gap-2 text-left transition hover:text-sky-700"
    @click.stop="openUserHome"
  >
    <UserAvatar :user="user" :size="avatarSize" />
    <span class="truncate text-sm text-slate-800">{{ displayName }}</span>
  </button>
  <div v-else class="inline-flex min-w-0 items-center gap-2">
    <UserAvatar :user="user" :size="avatarSize" />
    <span class="truncate text-sm text-slate-800">{{ displayName }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { getDisplayName } from '@/utils/avatar'

const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  },
  avatarSize: {
    type: String,
    default: 'sm'
  },
  clickable: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()

const displayName = computed(() => getDisplayName(props.user))
const targetUserId = computed(() => Number(props.user?.userId || props.user?.id || 0))
const canNavigate = computed(() => props.clickable && targetUserId.value > 0)

function openUserHome() {
  if (!canNavigate.value) return
  router.push(`/users/${targetUserId.value}`)
}
</script>
