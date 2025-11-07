<template>
  <div class="page-wrap supplier-approvals">
    <section class="page-card approvals-hero" aria-labelledby="supplier-approvals-title" aria-describedby="supplier-approvals-desc">
      <div>
        <span class="pill">供应商审核</span>
        <h1 id="supplier-approvals-title" class="hero-title">审核供应商入驻</h1>
        <p id="supplier-approvals-desc" class="hero-desc">
          核对供应商资质后即可通过申请，丰富社区团购商品供给。
        </p>
        <p class="sr-only">
          当前共有 {{ list.length }} 个供应商申请待审核。
        </p>
      </div>
      <el-button type="primary" @click="load" :loading="loading">
        刷新列表
      </el-button>
    </section>

    <section class="table-card" role="region" aria-labelledby="supplier-approvals-table-title">
      <header class="table-card__header table-header">
        <h2 id="supplier-approvals-table-title" class="page-section-title">待审核供应商</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="supplier-approvals-table-title">
          <el-table-column prop="id" label="申请ID" width="120" />
          <el-table-column prop="userId" label="用户ID" width="120" />
          <el-table-column prop="companyName" label="公司名称" />
          <el-table-column prop="status" label="状态" width="140">
            <template #default="{ row }">
              <el-tag type="warning">{{ row.status || 'PENDING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="approve(row)" :loading="row._loading">
                通过
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无待审核供应商" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pendingSuppliers } from '@/api/supplier'
import { approveSupplier } from '@/api/admin'

const list = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await pendingSuppliers()
    if (res.code === 0) {
      const items = res.pendingSuppliers || res.data?.pendingSuppliers || []
      list.value = items.map((item) => ({ ...item, _loading: false }))
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const approve = async (row) => {
  row._loading = true
  try {
    const res = await approveSupplier(row.id, row.userId)
    if (res.code === 0) {
      ElMessage.success('已通过审核')
      await load()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    row._loading = false
  }
}

onMounted(load)
</script>

<style scoped>
.supplier-approvals {
  gap: 24px;
}

.approvals-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  margin: 8px 0 10px;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-1);
}

.hero-desc {
  margin: 0;
  color: var(--text-2);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
