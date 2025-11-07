<template>
  <div class="page-wrap leader-dashboard">
    <section class="page-card dashboard-hero" aria-labelledby="leader-dashboard-title" aria-describedby="leader-dashboard-desc">
      <div>
        <span class="pill">团长工作台</span>
        <h1 id="leader-dashboard-title" class="hero-title">掌握社区团购的每一个节点</h1>
        <p id="leader-dashboard-desc" class="hero-desc">
          快速查看订单进度、配送情况与经营指标，助力社区居民及时收到新鲜商品。
        </p>
        <p class="sr-only">
          当前摘要：今日订单 {{ metrics.todayOrders }} 笔，在途订单 {{ metrics.shipping }} 笔，近 7 日成交额 {{ metrics.amount }} 元。
        </p>
      </div>
      <el-button type="primary" @click="$router.push('/leader/orders')">
        查看全部订单
      </el-button>
    </section>

    <section class="page-card" role="region" aria-labelledby="leader-stats-title">
      <h2 id="leader-stats-title" class="sr-only">核心指标</h2>
      <div class="stat-grid">
        <div class="stat-card">
          <span class="stat-card__title">今日订单数</span>
          <span class="stat-card__value">{{ metrics.todayOrders }}</span>
          <span class="stat-card__trend">+{{ metrics.todayTrend }}% 较昨日</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">在途订单</span>
          <span class="stat-card__value">{{ metrics.shipping }}</span>
          <span class="stat-card__trend">需要尽快安排配送</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">已完成</span>
          <span class="stat-card__value">{{ metrics.delivered }}</span>
          <span class="stat-card__trend">累计完成订单</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">近7日成交额</span>
          <span class="stat-card__value">¥ {{ metrics.amount }}</span>
          <span class="stat-card__trend">含配送与自提订单</span>
        </div>
      </div>
    </section>

    <section class="page-card recent-orders" aria-labelledby="leader-recent-orders-title" role="region">
      <header class="page-section-title">
        <h2 id="leader-recent-orders-title">最新订单</h2>
        <el-button text type="primary" @click="$router.push('/leader/orders')">更多</el-button>
      </header>
      <el-table :data="recentOrders" v-loading="loading" border aria-describedby="leader-recent-orders-title">
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
        <el-table-column prop="createdAt" label="下单时间" />
      </el-table>
      <el-empty v-if="!loading && !recentOrders.length" description="暂时没有订单数据" />
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { leaderOrders } from '@/api/orders'

const user = useUserStore()
const loading = ref(false)
const recentOrders = ref([])
const metrics = reactive({
  todayOrders: 0,
  todayTrend: 0,
  shipping: 0,
  delivered: 0,
  amount: '0.00'
})

const statusMap = {
  CREATED: { label: '待支付', type: 'warning' },
  PAID: { label: '待发货', type: 'primary' },
  SHIPPED: { label: '运输中', type: 'info' },
  DELIVERED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await leaderOrders(user.userId, { page: 1, size: 20 })
    if (res.code === 0) {
      const items = res.list || res.data?.list || []
      recentOrders.value = items.slice(0, 6).map((o) => ({
        ...o,
        createdAt: o.createdAt ? dayjs(o.createdAt).format('YYYY-MM-DD HH:mm') : ''
      }))
      const today = dayjs().format('YYYY-MM-DD')
      const todayOrders = items.filter((o) =>
        dayjs(o.createdAt).format('YYYY-MM-DD') === today
      )
      metrics.todayOrders = todayOrders.length
      metrics.todayTrend = Math.max(Math.round(metrics.todayOrders / (items.length || 1) * 100), 0)
      metrics.shipping = items.filter((o) => o.status === 'SHIPPED').length
      metrics.delivered = items.filter((o) => o.status === 'DELIVERED').length
      metrics.amount = format(items.reduce((sum, item) => sum + (item.amount || 0), 0))
    } else {
      ElMessage.error(res.msg || '加载数据失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.leader-dashboard {
  gap: 24px;
}

.dashboard-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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

.recent-orders {
  display: grid;
  gap: 20px;
}
</style>
