<template>
  <div class="user-manage-container pro-page">
    <el-card class="main-card card-shadow pro-main-card">
      <template #header>
        <div class="card-header pro-card-header">
          <div class="header-left pro-title-group">
            <el-icon class="header-icon pro-title-icon"><User /></el-icon>
            <h2 class="pro-title-text">用户管理</h2>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form pro-filter-bar">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="用户名 / 昵称 / 邮箱"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.role" clearable placeholder="全部" style="width: 150px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="pro-table-shell">
        <el-table
          v-loading="loading"
          :data="userList"
          stripe
          class="user-table"
        >
          <el-table-column prop="id" label="ID" width="80" align="center" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="nickname" label="昵称" width="150" />
          <el-table-column prop="email" label="邮箱" min-width="220" />
          <el-table-column prop="role" label="角色" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getRoleType(row.role)" effect="dark">{{ getRoleText(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column prop="updateTime" label="更新时间" width="180" />
          <el-table-column label="操作" width="220" align="center" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="warning" @click="openResetPasswordDialog(row)">
                重置密码
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无用户数据" :image-size="80" />
          </template>
        </el-table>
      </div>

      <div class="pagination-container pro-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editDialogVisible"
      :title="`编辑用户 #${editForm.id || '-'}`"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEditUser">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetDialogVisible"
      :title="`重置密码 #${resetForm.id || '-'}`"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="120px">
        <el-form-item label="用户名">
          <el-input :model-value="resetForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            show-password
            maxlength="50"
            placeholder="6-50 位字符"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fillDefaultPassword">填充 123456</el-button>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitResetPassword">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { userApi } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'

const userStore = useUserStore()

const loading = ref(false)
const userList = ref([])

const searchForm = reactive({
  keyword: '',
  role: '',
  status: null
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const editDialogVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: null,
  username: '',
  role: 'STUDENT',
  status: 1
})

const resetDialogVisible = ref(false)
const resetSubmitting = ref(false)
const resetFormRef = ref(null)
const resetForm = reactive({
  id: null,
  username: '',
  newPassword: ''
})

const editRules = {
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 50, message: '长度应为 6-50 位', trigger: 'blur' }
  ]
}

const getRoleType = (role) => {
  const map = {
    ADMIN: 'danger',
    TEACHER: 'warning',
    STUDENT: 'success'
  }
  return map[role] || ''
}

const getRoleText = (role) => {
  const map = {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生'
  }
  return map[role] || role
}

const buildParams = () => {
  const params = {
    page: pagination.page,
    size: pagination.size
  }
  if (searchForm.keyword.trim()) {
    params.keyword = searchForm.keyword.trim()
  }
  if (searchForm.role) {
    params.role = searchForm.role
  }
  if (searchForm.status === 0 || searchForm.status === 1) {
    params.status = searchForm.status
  }
  return params
}

const loadUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getUserList(buildParams())
    userList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadUsers()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.role = ''
  searchForm.status = null
  pagination.page = 1
  loadUsers()
}

const handlePageSizeChange = () => {
  pagination.page = 1
  loadUsers()
}

const openEditDialog = (row) => {
  editForm.id = row.id
  editForm.username = row.username || ''
  editForm.role = row.role || 'STUDENT'
  editForm.status = Number(row.status ?? 1)
  editFormRef.value?.clearValidate()
  editDialogVisible.value = true
}

const submitEditUser = async () => {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      await userApi.adminUpdateUser(editForm.id, {
        role: editForm.role,
        status: Number(editForm.status)
      })
      editDialogVisible.value = false
      ElMessage.success('用户信息更新成功')
      await loadUsers()
      if (userStore.userInfo?.id === editForm.id) {
        await userStore.fetchUserInfo()
      }
    } catch (error) {
      ElMessage.error(error.message || '更新失败')
    } finally {
      editSubmitting.value = false
    }
  })
}

const openResetPasswordDialog = (row) => {
  resetForm.id = row.id
  resetForm.username = row.username || ''
  resetForm.newPassword = ''
  resetFormRef.value?.clearValidate()
  resetDialogVisible.value = true
}

const fillDefaultPassword = () => {
  resetForm.newPassword = '123456'
}

const submitResetPassword = async () => {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    resetSubmitting.value = true
    try {
      await userApi.adminResetPassword(resetForm.id, {
        newPassword: resetForm.newPassword
      })
      resetDialogVisible.value = false
      ElMessage.success('密码重置成功')
    } catch (error) {
      ElMessage.error(error.message || '密码重置失败')
    } finally {
      resetSubmitting.value = false
    }
  })
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.user-manage-container {
  max-width: 1400px;
  margin: 0 auto;
}

.main-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 20px;
  color: #1677ff;
}

.card-header h2 {
  margin: 0;
}

.search-form {
  margin-bottom: 14px;
}

.user-table {
  margin-top: 0;
}

.pagination-container {
  margin-top: 22px;
}
</style>
