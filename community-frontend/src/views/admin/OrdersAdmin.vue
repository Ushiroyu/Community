<template>
  <div class="page-wrap admin-orders">
    <section class="page-card orders-hero" aria-labelledby="admin-orders-title" aria-describedby="admin-orders-desc">
      <div>
        <span class="pill">订单管理</span>
        <h1 id="admin-orders-title" class="hero-title">全平台订单监控</h1>
        <p id="admin-orders-desc" class="hero-desc">
          支持按状态筛选、导出订单数据，掌握平台订单流转情况。
        </p>
        <p class="sr-only">
          当前筛选状态为 {{ status === 'ALL' ? '全部' : (statusMap[status]?.label || status) }}，列表共有 {{ total }} 笔订单。
        </p>
      </div>
      <div class="hero-actions" aria-label="过滤与导出">
        <span id="admin-orders-filter-tip" class="sr-only">切换状态选项将刷新订单列表。</span>
        <el-segmented v-model="status" :options="statusOptions" @change="onStatusChange" aria-describedby="admin-orders-filter-tip" />
        <el-button type="primary" @click="load" :loading="loading">刷新</el-button>
        <el-button @click="exportCsv" :disabled="!list.length">导出 CSV</el-button>
      </div>
    </section>

    <section class="table-card" role="region" aria-labelledby="admin-orders-table-title">
      <header class="table-card__header table-header">
        <h2 id="admin-orders-table-title" class="page-section-title">订单列表</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="admin-orders-table-title">
          <el-table-column prop="id" label="订单号" width="120" />
          <el-table-column prop="userId" label="用户ID" width="120" />
          <el-table-column prop="leaderId" label="团长ID" width="120" />
          <el-table-column prop="supplierId" label="供应商ID" width="120" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥ {{ format(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下单时间" />
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无订单数据" />
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
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import http from '@/api/http'

const status = ref('ALL')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待支付', value: 'CREATED' },
  { label: '已支付', value: 'PAID' },
  { label: '在途', value: 'SHIPPED' },
  { label: '已完成', value: 'DELIVERED' }
]

const statusMap = {
  CREATED: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '运输中', type: 'info' },
  DELIVERED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

const queryStatus = computed(() => (status.value === 'ALL' ? undefined : status.value))

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const load = async () => {
  loading.value = true
  try {
    const res = await http.get('/orders/all', {
      params: {
        status: queryStatus.value,
        page: page.value,
        size: size.value
      }
    })
    if (res.code === 0) {
      list.value = res.list || res.data?.list || []
      total.value = res.total ?? res.data?.total ?? list.value.length
    } else {
      ElMessage.error(res.msg || '加载订单失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onStatusChange = () => {
  page.value = 1
  load()
}

const exportCsv = () => {
  const rows = list.value || []
  const headers = ['订单号', '用户ID', '团长ID', '供应商ID', '金额', '状态', '下单时间']
  const lines = [headers.join(',')]
  for (const row of rows) {
    lines.push(
      [row.id, row.userId, row.leaderId, row.supplierId, row.amount, row.status, row.createdAt]
        .map((value) => `"${(value ?? '').toString().replaceAll('"', '""')}"`)
        .join(',')
    )
  }
  const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'orders.csv'
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(load)
</script>

<style scoped>
.admin-orders {
  gap: 24px;
}

.orders-hero {
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

.pagination-bar {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}
</style>
