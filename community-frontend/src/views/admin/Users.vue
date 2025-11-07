<template>
  <div class="page-wrap admin-users">
    <section class="page-card filters-card" aria-labelledby="admin-users-filter-title" aria-describedby="admin-users-filter-tip">
      <div class="page-section-title">
        <h1 id="admin-users-filter-title">用户筛选</h1>
      </div>
      <p id="admin-users-filter-tip" class="sr-only">
        可以输入用户名或 ID，并按角色筛选，点击搜索刷新列表。
      </p>
      <div class="filters-grid">
        <el-input
          v-model="keyword"
          placeholder="搜索用户名或ID"
          clearable
          aria-label="搜索用户名或 ID"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="role" placeholder="角色（可选）" clearable>
          <el-option label="管理员" value="ADMIN" />
          <el-option label="团长" value="LEADER" />
          <el-option label="供应商" value="SUPPLIER" />
          <el-option label="居民用户" value="USER" />
        </el-select>
        <el-button type="primary" @click="onSearch" :loading="loading">
          搜索
        </el-button>
      </div>
    </section>

    <section class="table-card" role="region" aria-labelledby="admin-users-table-title">
      <header class="table-card__header table-header">
        <h2 id="admin-users-table-title" class="page-section-title">用户列表</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="admin-users-table-title">
          <el-table-column prop="id" label="用户ID" width="100" />
          <el-table-column label="用户名" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row._username" size="small" placeholder="输入昵称" />
            </template>
          </el-table-column>
          <el-table-column prop="role" label="当前角色" width="120">
            <template #default="{ row }">
              <el-tag>{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="角色设置" width="220">
            <template #default="{ row }">
              <el-select v-model="row._role" size="small" placeholder="选择角色" style="width: 140px; margin-right: 6px">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="团长" value="LEADER" />
                <el-option label="供应商" value="SUPPLIER" />
                <el-option label="居民用户" value="USER" />
              </el-select>
              <el-button size="small" type="primary" @click="saveRole(row)" :loading="row._roleLoading">
                保存
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="saveName(row)" :loading="row._nameLoading">
                保存名称
              </el-button>
              <el-popover placement="top" width="240" trigger="click">
                <template #reference>
                  <el-button size="small">重置密码</el-button>
                </template>
                <div class="reset-box">
                  <el-input v-model="row._pwd" placeholder="新密码" size="small" show-password />
                  <el-button size="small" type="primary" @click="applyReset(row)" :loading="row._resetLoading">
                    确定
                  </el-button>
                </div>
              </el-popover>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无数据" />
        <div class="pagination-bar" v-if="total">
          <el-pagination
            :total="total"
            v-model:current-page="page"
            :page-size="size"
            layout="prev, pager, next, jumper"
            @current-change="load"
            @size-change="load"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { listUsers, updateUserById, resetPassword } from '@/api/users'
import http from '@/api/http'

const keyword = ref('')
const role = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const roleLabel = (value) => {
  switch (value) {
    case 'ADMIN':
      return '管理员'
    case 'LEADER':
      return '团长'
    case 'SUPPLIER':
      return '供应商'
    default:
      return '居民用户'
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await listUsers({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      role: role.value || undefined
    })
    if (res.code === 0) {
      const records = res.list || res.data?.list || []
      list.value = records.map((u) => ({
        ...u,
        _username: u.username,
        _pwd: '',
        _role: u.role,
        _nameLoading: false,
        _roleLoading: false,
        _resetLoading: false
      }))
      total.value = res.total ?? res.data?.total ?? records.length
    } else {
      ElMessage.error(res.msg || '加载用户列表失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  page.value = 1
  load()
}

const saveName = async (row) => {
  if (!row._username) {
    ElMessage.error('用户名不能为空')
    return
  }
  row._nameLoading = true
  try {
    const res = await updateUserById(row.id, { nickname: row._username })
    if (res.code === 0) {
      ElMessage.success('已更新')
      row.username = row._username
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    row._nameLoading = false
  }
}

const saveRole = async (row) => {
  if (!row._role) {
    ElMessage.error('请选择角色')
    return
  }
  row._roleLoading = true
  try {
    const res = await http.put(`/users/${row.id}/role`, null, { params: { role: row._role } })
    if (res.code === 0) {
      ElMessage.success('角色已更新')
      row.role = row._role
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    row._roleLoading = false
  }
}

const applyReset = async (row) => {
  if (!row._pwd) {
    ElMessage.error('请输入新密码')
    return
  }
  row._resetLoading = true
  try {
    const res = await resetPassword(row.id, row._pwd)
    if (res.code === 0) {
      ElMessage.success('已重置密码')
      row._pwd = ''
    } else {
      ElMessage.error(res.msg || '重置失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '重置失败')
  } finally {
    row._resetLoading = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-users {
  gap: 24px;
}

.filters-card {
  display: grid;
  gap: 16px;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reset-box {
  display: flex;
  gap: 6px;
  align-items: center;
}

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
