<template>
  <div class="page-wrap orders-view">
    <section class="page-card orders-hero" aria-labelledby="orders-hero-title">
      <div>
        <span class="pill">我的订单</span>
        <h1 id="orders-hero-title" class="orders-title">掌握每一笔社区团购进度</h1>
        <p class="orders-desc">
          查看订单状态、金额与配送节点，支持快速确认收货。遇到异常可联系团长或发起售后。
        </p>
      </div>
      <div class="orders-stats">
        <div class="stat-card">
          <span class="stat-card__title">总订单</span>
          <span class="stat-card__value">{{ list.length }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">已支付</span>
          <span class="stat-card__value">{{ paidCount }}</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">待签收</span>
          <span class="stat-card__value">{{ shippingCount }}</span>
        </div>
      </div>
      <p class="sr-only">
        当前共有 {{ list.length }} 笔订单，其中待发货 {{ list.length - shippingCount - paidCount }} 笔，运输中 {{ shippingCount }} 笔。
      </p>
    </section>

    <section class="table-card" aria-labelledby="orders-table-title" role="region">
      <header class="table-card__header orders-table-header">
        <h2 id="orders-table-title" class="page-section-title">订单列表</h2>
        <el-button type="primary" text @click="load" :loading="loading">刷新</el-button>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border>
          <el-table-column prop="id" label="订单号" width="120" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥ {{ format(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'" effect="light">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下单时间" min-width="180" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'SHIPPED'"
                size="small"
                type="primary"
                @click="confirm(row.id)"
                :loading="confirmingId === row.id"
              >
                确认收货
              </el-button>
              <el-button size="small" text @click="openDetail(row)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty
          v-if="!loading && !list.length"
          description="暂无订单，去挑选商品吧"
        />
      </div>
    </section>

    <el-drawer
      v-model="drawer.visible"
      title="订单详情"
      size="420px"
    >
      <el-descriptions v-if="drawer.data" :column="1" border>
        <el-descriptions-item label="订单号">
          {{ drawer.data.id }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[drawer.data.status]?.type || 'info'">
            {{ statusMap[drawer.data.status]?.label || drawer.data.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          ¥ {{ format(drawer.data.amount) }}
        </el-descriptions-item>
        <el-descriptions-item label="数量">
          {{ drawer.data.quantity || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="商品 ID">
          {{ drawer.data.productId || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="团长 ID">
          {{ drawer.data.leaderId || '--' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ drawer.data.createdAt }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ drawer.data.updatedAt || '--' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { myOrders, confirmOrder } from '@/api/orders'

const list = ref([])
const loading = ref(false)
const confirmingId = ref(null)
const drawer = reactive({ visible: false, data: null })

const statusMap = {
  CREATED: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '运输中', type: 'info' },
  DELIVERED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

const paidCount = computed(() =>
  list.value.filter((item) => item.status === 'PAID' || item.status === 'DELIVERED').length
)
const shippingCount = computed(() =>
  list.value.filter((item) => item.status === 'SHIPPED').length
)

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const load = async () => {
  loading.value = true
  try {
    const res = await myOrders()
    if (res.code === 0) {
      const items = res.list ?? res.data?.list ?? []
      list.value = items.map((o) => ({
        ...o,
        createdAt: o.createdAt ? dayjs(o.createdAt).format('YYYY-MM-DD HH:mm') : ''
      }))
    } else {
      ElMessage.error(res.msg || '加载订单失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const confirm = async (id) => {
  confirmingId.value = id
  try {
    const res = await confirmOrder(id)
    if (res.code === 0) {
      ElMessage.success('已确认收货')
      await load()
    } else {
      ElMessage.error(res.msg || '确认失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '确认失败')
  } finally {
    confirmingId.value = null
  }
}

const openDetail = (row) => {
  drawer.data = row
  drawer.visible = true
}

onMounted(load)
</script>

<style scoped>
.orders-view {
  gap: 24px;
}

.orders-hero {
  display: grid;
  gap: 20px;
}

.orders-title {
  margin: 8px 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.orders-desc {
  margin: 0;
  font-size: 15px;
  color: var(--text-2);
}

.orders-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
}

.orders-table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.el-descriptions :deep(.el-descriptions__cell) {
  padding: 14px;
}
</style>
