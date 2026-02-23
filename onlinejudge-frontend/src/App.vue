<template>
  <div id="app">
    <el-container v-if="userStore.isLoggedIn">
      <el-header>
        <NavBar />
      </el-header>
      <div v-if="systemStore.siteAnnouncement" class="announcement-wrapper">
        <el-alert
          :title="systemStore.siteAnnouncement"
          type="info"
          :closable="false"
          show-icon
        />
      </div>
      <el-main>
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
    <router-view v-else v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useSystemStore } from '@/store/system'
import { useUserStore } from '@/store/user'
import NavBar from '@/components/NavBar.vue'

const userStore = useUserStore()
const systemStore = useSystemStore()

const loadSystemConfig = async () => {
  if (!userStore.isLoggedIn) {
    return
  }
  try {
    await systemStore.ensureLoaded()
  } catch (error) {
    // Keep default config when request fails.
  }
}

watch(
  () => userStore.isLoggedIn,
  async (loggedIn) => {
    if (!loggedIn) {
      systemStore.reset()
      return
    }
    await loadSystemConfig()
  }
)

onMounted(async () => {
  await loadSystemConfig()
})
</script>

<style>
#app {
  min-height: 100vh;
}

.el-header {
  padding: 0;
  height: 64px !important;
  background: transparent;
}

.el-main {
  padding: 20px 0 24px;
  background: transparent;
  min-height: calc(100vh - 64px);
}

.announcement-wrapper {
  max-width: var(--oj-content-max-width);
  margin: 0 auto;
  padding: 10px var(--oj-content-gutter) 0;
}

.announcement-wrapper :deep(.el-alert) {
  border-radius: 8px;
  border: 1px solid #cfe0f7;
  background: linear-gradient(145deg, #f6faff 0%, #eef6ff 100%);
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.06);
}

@media (max-width: 768px) {
  .el-main {
    padding: 14px 0 16px;
    min-height: calc(100vh - 64px);
  }

  .announcement-wrapper {
    padding-top: 8px;
  }
}
</style>
