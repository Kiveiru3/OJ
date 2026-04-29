<template>
  <div class="space-y-3">
    <article
      v-for="node in nodes"
      :key="node.id"
      class="rounded-lg border border-line bg-slate-50 p-3"
      :style="{ marginLeft: `${Math.min(level, 4) * 16}px` }"
    >
      <div class="flex items-center justify-between text-xs text-soft">
        <UserIdentity :user="node" avatar-size="xs" />
        <span>{{ formatTime(node.createTime) }}</span>
      </div>

      <ProblemRichContent class="mt-2" :content="node.content || ''" />

      <div class="mt-2 flex gap-2">
        <AppButton size="sm" variant="ghost" @click="$emit('reply', node)">回复</AppButton>
        <AppButton v-if="node.editable || isAdmin" size="sm" variant="ghost" @click="$emit('delete', node)">删除</AppButton>
      </div>

      <DiscussionCommentThread
        v-if="node.children?.length"
        :nodes="node.children"
        :level="level + 1"
        :is-admin="isAdmin"
        @reply="$emit('reply', $event)"
        @delete="$emit('delete', $event)"
      />
    </article>
  </div>
</template>

<script setup>
import AppButton from '@/components/ui/AppButton.vue'
import ProblemRichContent from '@/components/problem/ProblemRichContent.vue'
import UserIdentity from '@/components/ui/UserIdentity.vue'

defineProps({
  nodes: {
    type: Array,
    default: () => []
  },
  level: {
    type: Number,
    default: 0
  },
  isAdmin: {
    type: Boolean,
    default: false
  }
})

defineEmits(['reply', 'delete'])

function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
</script>
