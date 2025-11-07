<template>
  <div class="page-wrap product-approvals">
    <section class="page-card approvals-hero" aria-labelledby="product-approvals-title" aria-describedby="product-approvals-desc">
      <div>
        <span class="pill">商品审核</span>
        <h1 id="product-approvals-title" class="hero-title">审核待上架商品</h1>
        <p id="product-approvals-desc" class="hero-desc">
          支持关键词搜索、批量通过/拒绝以及导出待审核商品，确保商品信息规范。
        </p>
        <p class="sr-only">
          当前待审核商品共 {{ list.length }} 个，可批量操作或单独审批。
        </p>
      </div>
      <div class="hero-actions" aria-label="筛选与批量操作">
        <el-input v-model="keyword" placeholder="输入关键词" clearable aria-label="输入商品搜索关键词" />
        <el-button type="primary" @click="load" :loading="loading">搜索</el-button>
        <el-button type="success" @click="batchApprove(true)" :disabled="!selection.length">
          批量通过
        </el-button>
        <el-button type="warning" @click="batchApprove(false)" :disabled="!selection.length">
          批量拒绝
        </el-button>
        <el-button @click="exportCsv" :disabled="!list.length">导出 CSV</el-button>
      </div>
    </section>

    <section class="table-card" role="region" aria-labelledby="product-approvals-table-title">
      <header class="table-card__header table-header">
        <h2 id="product-approvals-table-title" class="page-section-title">待审核商品</h2>
      </header>
      <div class="table-card__body">
        <el-table
          :data="list"
          v-loading="loading"
          row-key="id"
          border
          @selection-change="onSelChange"
          aria-describedby="product-approvals-table-title"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" min-width="200" />
          <el-table-column label="价格" width="120">
            <template #default="{ row }">¥ {{ format(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="approve(row, true)" :loading="row._loading">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="approve(row, false)" :loading="row._loading">
                拒绝
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无待审核商品" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageProducts } from '@/api/products'
import { approveProduct } from '@/api/admin'

const keyword = ref('')
const list = ref([])
const loading = ref(false)
const selection = ref([])

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const load = async () => {
  loading.value = true
  try {
    const res = await pageProducts({ page: 1, size: 50, keyword: keyword.value || undefined, approved: false })
    if (res.code === 0) {
      const records = res.list || res.data?.list || res.records || []
      list.value = records.map((item) => ({ ...item, _loading: false }))
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const approve = async (row, ok) => {
  row._loading = true
  try {
    const res = await approveProduct(row.id, ok)
    if (res.code === 0) {
      ElMessage.success(ok ? '已通过' : '已拒绝')
      await load()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    row._loading = false
  }
}

const onSelChange = (rows) => {
  selection.value = rows || []
}

const batchApprove = async (ok) => {
  if (!selection.value.length) return
  loading.value = true
  try {
    for (const row of selection.value) {
      await approveProduct(row.id, ok)
    }
    ElMessage.success(ok ? '批量通过完成' : '批量拒绝完成')
    await load()
  } catch (error) {
    ElMessage.error(error.message || '批量操作失败')
  } finally {
    loading.value = false
  }
}

const exportCsv = () => {
  const rows = list.value || []
  const headers = ['ID', '名称', '价格', '库存']
  const lines = [headers.join(',')]
  for (const row of rows) {
    lines.push(
      [row.id, row.name, row.price, row.stock]
        .map((value) => `"${(value ?? '').toString().replaceAll('"', '""')}"`)
        .join(',')
    )
  }
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'products_pending.csv'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.product-approvals {
  gap: 24px;
}

.approvals-hero {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
