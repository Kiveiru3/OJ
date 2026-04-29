<template>
  <Transition name="dialog-fade">
    <div v-if="ui.confirmVisible" class="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-900/45 p-4">
      <div class="w-full max-w-sm rounded-xl border border-line bg-white p-5 shadow-card">
        <div class="text-base font-semibold text-slate-900">{{ ui.confirmTitle }}</div>
        <div class="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-600">{{ ui.confirmMessage }}</div>
        <div class="mt-5 flex justify-end gap-2">
          <AppButton v-if="ui.confirmShowCancel" size="sm" variant="secondary" @click="cancel">{{ ui.confirmCancelText }}</AppButton>
          <AppButton size="sm" @click="confirm">{{ ui.confirmOkText }}</AppButton>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import AppButton from '@/components/ui/AppButton.vue'
import { useUiStore } from '@/stores/useUiStore'

const ui = useUiStore()

function cancel() {
  ui.resolveConfirm(false)
}

function confirm() {
  ui.resolveConfirm(true)
}
</script>

<style scoped>
.dialog-fade-enter-active,
.dialog-fade-leave-active {
  transition: opacity 0.2s ease;
}
.dialog-fade-enter-from,
.dialog-fade-leave-to {
  opacity: 0;
}
</style>
