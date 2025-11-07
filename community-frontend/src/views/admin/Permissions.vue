<template>
  <div class="page-wrap admin-permissions">
    <section class="page-card filters-card" aria-labelledby="admin-permissions-filter-title" aria-describedby="admin-permissions-filter-tip">
      <div class="page-section-title">
        <h1 id="admin-permissions-filter-title">角色筛选</h1>
      </div>
      <p id="admin-permissions-filter-tip" class="sr-only">
        输入用户名或选择角色后点击搜索可刷新权限列表。
      </p>
      <div class="filters-grid">
        <el-input v-model="keyword" placeholder="搜索用户名" clearable aria-label="搜索用户名" />
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

    <section class="table-card" role="region" aria-labelledby="admin-permissions-table-title">
      <header class="table-card__header table-header">
        <h2 id="admin-permissions-table-title" class="page-section-title">权限列表</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="admin-permissions-table-title">
          <el-table-column prop="id" label="用户ID" width="100" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="role" label="当前角色" width="140">
            <template #default="{ row }">
              <el-tag>{{ roleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="角色配置" width="220">
            <template #default="{ row }">
              <el-select v-model="row._role" placeholder="选择角色" size="small" style="width: 150px; margin-right: 6px">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="团长" value="LEADER" />
                <el-option label="供应商" value="SUPPLIER" />
                <el-option label="居民用户" value="USER" />
              </el-select>
              <el-button size="small" type="primary" @click="apply(row)" :loading="row._loading">
                保存
              </el-button>
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
    const res = await http.get('/users', {
      params: {
        page: page.value,
        size: size.value,
        keyword: keyword.value || undefined,
        role: role.value || undefined
      }
    })
    if (res.code === 0) {
      const records = res.list || res.data?.list || []
      list.value = records.map((item) => ({ ...item, _role: item.role, _loading: false }))
      total.value = res.total ?? res.data?.total ?? records.length
    } else {
      ElMessage.error(res.msg || '加载失败')
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

const apply = async (row) => {
  if (!row._role) {
    ElMessage.error('请选择角色')
    return
  }
  row._loading = true
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
    row._loading = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-permissions {
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

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
