<template>
  <div class="page-wrap leader-orders">
    <section class="page-card orders-hero" aria-labelledby="leader-orders-title" aria-describedby="leader-orders-desc">
      <div>
        <span class="pill">社区订单</span>
        <h1 id="leader-orders-title" class="hero-title">跟踪社区配送进度</h1>
        <p id="leader-orders-desc" class="hero-desc">
          可通过状态筛选查看订单详情，及时掌握在途与待签收订单，确保居民满意度。
        </p>
        <p class="sr-only">
          当前筛选状态为 {{ status === 'ALL' ? '全部' : (statusMap[status]?.label || status) }}，列表共 {{ total }} 笔订单，支持切换状态与分页查看。
        </p>
      </div>
      <div class="hero-actions" aria-label="筛选器">
        <span id="leader-status-tip" class="sr-only">选择订单状态后列表会自动刷新。</span>
        <el-segmented
          v-model="status"
          :options="statusOptions"
          @change="onStatusChange"
          aria-describedby="leader-status-tip"
        />
        <el-button text type="primary" @click="load">刷新</el-button>
      </div>
    </section>

    <section class="table-card" aria-labelledby="leader-orders-table-title" role="region">
      <header class="table-card__header table-header">
        <h2 id="leader-orders-table-title" class="page-section-title">订单列表</h2>
        <div class="table-tools">
          <el-select
            v-model="size"
            placeholder="每页数量"
            size="small"
            @change="onSizeChange"
            aria-label="选择每页显示数量"
          >
            <el-option :value="10" label="每页 10" />
            <el-option :value="20" label="每页 20" />
            <el-option :value="50" label="每页 50" />
          </el-select>
        </div>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="leader-orders-table-title">
          <el-table-column prop="id" label="订单号" width="120" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥ {{ format(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="statusMap[row.status]?.type || 'info'">
                {{ statusMap[row.status]?.label || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下单时间" min-width="180" />
          <el-table-column label="备注" min-width="160">
            <template #default="{ row }">
              {{ row.remark || '—' }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无订单数据" />
        <div class="pagination-bar" v-if="total">
          <el-pagination
            :total="total"
            v-model:current-page="page"
            :page-size="size"
            layout="prev, pager, next, jumper"
            @current-change="load"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { leaderOrders } from '@/api/orders'

const user = useUserStore()
const status = ref('ALL')
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

const statusMap = {
  CREATED: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '运输中', type: 'info' },
  DELIVERED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待付款', value: 'CREATED' },
  { label: '待发货', value: 'PAID' },
  { label: '运输中', value: 'SHIPPED' },
  { label: '已完成', value: 'DELIVERED' }
]

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const queryStatus = computed(() => (status.value === 'ALL' ? undefined : status.value))

const load = async () => {
  loading.value = true
  try {
    const res = await leaderOrders(user.userId, {
      status: queryStatus.value,
      page: page.value,
      size: size.value
    })
    if (res.code === 0) {
      const items = res.list || res.data?.list || []
      list.value = items.map((o) => ({
        ...o,
        createdAt: o.createdAt ? dayjs(o.createdAt).format('YYYY-MM-DD HH:mm') : ''
      }))
      total.value = res.total ?? res.data?.total ?? items.length
    } else {
      ElMessage.error(res.msg || '加载失败')
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

const onSizeChange = () => {
  page.value = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.leader-orders {
  gap: 24px;
}

.orders-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.hero-title {
  margin: 8px 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.hero-desc {
  margin: 0;
  color: var(--text-2);
}

.hero-actions {
  display: flex;
  align-items: center;
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

@media (max-width: 768px) {
  .orders-hero {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
