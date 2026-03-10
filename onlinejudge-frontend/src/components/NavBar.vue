<template>
  <div class="navbar-container">
    <div class="navbar-content">
      <div class="navbar-brand">
        <el-icon class="brand-icon"><Trophy /></el-icon>
        <div class="brand-stack">
          <span class="brand-text">{{ systemStore.siteName }}</span>
          <span class="brand-sub">算法训练平台</span>
        </div>
      </div>

      <el-menu
        mode="horizontal"
        :default-active="activeIndex"
        router
        class="navbar-menu"
        :ellipsis="false"
      >
        <el-menu-item index="/problems">
          <el-icon><Document /></el-icon>
          <span>题目</span>
        </el-menu-item>
        <el-menu-item index="/submissions">
          <el-icon><List /></el-icon>
          <span>提交</span>
        </el-menu-item>
        <el-menu-item index="/contests">
          <el-icon><Medal /></el-icon>
          <span>竞赛</span>
        </el-menu-item>
        <el-menu-item index="/discussions">
          <el-icon><ChatDotRound /></el-icon>
          <span>讨论</span>
        </el-menu-item>

        <el-sub-menu v-if="userStore.isAdmin" index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>管理端</span>
          </template>
          <el-menu-item index="/admin/problems">题目管理</el-menu-item>
          <el-menu-item index="/admin/users">用户管理</el-menu-item>
          <el-menu-item index="/admin/system">系统管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="userStore.isTeacher || userStore.isAdmin" index="teacher">
          <template #title>
            <el-icon><Edit /></el-icon>
            <span>教师端</span>
          </template>
          <el-menu-item index="/teacher/problems">题目管理</el-menu-item>
          <el-menu-item index="/teacher/analytics">教学分析</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div class="navbar-user">
        <el-dropdown trigger="click" @command="handleCommand">
          <button type="button" class="user-info" aria-label="用户菜单">
            <el-avatar :size="32" class="user-avatar" @click.stop="goProfile">
              <el-icon><User /></el-icon>
            </el-avatar>
            <span class="user-name" @click.stop="goProfile">
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}
            </span>
            <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSystemStore } from '@/store/system'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'
import {
  Trophy,
  Document,
  List,
  Medal,
  ChatDotRound,
  Setting,
  Edit,
  User,
  SwitchButton,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const systemStore = useSystemStore()

const activeIndex = computed(() => {
  if (route.path.startsWith('/problem/')) return '/problems'
  if (route.path.startsWith('/submission/')) return '/submissions'
  if (route.path.startsWith('/contest/')) return '/contests'
  if (route.path.startsWith('/discussion/')) return '/discussions'
  return route.path
})

const goProfile = () => {
  if (route.path !== '/profile') {
    router.push('/profile')
  }
}

const handleCommand = async (command) => {
  if (command === 'profile') {
    goProfile()
    return
  }

  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确认退出当前账号吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      router.push('/login')
    } catch (error) {
      // User canceled.
    }
  }
}
</script>

<style scoped>
.navbar-container {
  background: rgba(251, 253, 255, 0.92);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid #dbe6f3;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.navbar-content {
  max-width: var(--oj-content-max-width);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 62px;
  padding: 0 var(--oj-content-gutter);
  gap: 12px;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  color: #0f172a;
  flex-shrink: 0;
}

.brand-icon {
  font-size: 21px;
  color: #0b63f6;
  background: linear-gradient(145deg, #e9f1ff 0%, #dcfbff 100%);
  border: 1px solid #d3e2f7;
  box-shadow: 0 6px 16px rgba(11, 99, 246, 0.18);
  padding: 8px;
  border-radius: 8px;
}

.brand-stack {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.brand-text {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.2px;
  color: #0f172a;
}

.brand-sub {
  font-size: 11px;
  color: #64748b;
  letter-spacing: 0.9px;
  text-transform: uppercase;
}

.navbar-menu {
  flex: 1;
  background: transparent !important;
  border: none;
  margin-left: 24px;
  min-width: 0;
}

.navbar-menu :deep(.el-menu-item),
.navbar-menu :deep(.el-sub-menu__title) {
  color: #334155;
  border-bottom: none;
  transition: all 0.2s ease;
  border-radius: 8px;
  font-weight: 600;
  margin: 0 2px;
}

.navbar-menu :deep(.el-menu-item:hover),
.navbar-menu :deep(.el-sub-menu__title:hover) {
  background: #edf4ff;
  color: #113a8f;
}

.navbar-menu :deep(.el-menu-item.is-active) {
  color: #0b63f6;
  background: linear-gradient(145deg, #edf4ff 0%, #e8f9ff 100%);
  border: 1px solid #cddcf4;
}

.navbar-user {
  margin-left: 12px;
  flex-shrink: 0;
  position: relative;
  z-index: 20;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 999px;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid #d9e4f2;
  background: linear-gradient(145deg, #f8fbff 0%, #f1f6fd 100%);
  appearance: none;
  outline: none;
}

.user-info:hover {
  background: #eef5ff;
  border-color: #c6d6ec;
  box-shadow: 0 8px 14px rgba(15, 23, 42, 0.08);
}

.user-avatar {
  background: #e4efff;
  color: #0b63f6;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.dropdown-icon {
  font-size: 12px;
  color: #9ca3af;
}

.navbar-menu :deep(.el-sub-menu__title) {
  color: #334155;
}

.navbar-menu :deep(.el-menu--horizontal .el-sub-menu .el-sub-menu__title) {
  border-bottom: none;
}

.navbar-menu :deep(.el-menu--horizontal .el-sub-menu.is-active .el-sub-menu__title) {
  color: #0b63f6;
}

@media (max-width: 992px) {
  .navbar-content {
    gap: 8px;
  }

  .brand-text {
    font-size: 16px;
  }

  .brand-sub {
    display: none;
  }

  .navbar-menu {
    margin-left: 12px;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 768px) {
  .navbar-content {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .navbar-content::-webkit-scrollbar {
    display: none;
  }

  .navbar-menu {
    min-width: max-content;
  }
}
</style>

