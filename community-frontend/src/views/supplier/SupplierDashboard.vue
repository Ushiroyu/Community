<template>
  <div class="page-wrap supplier-dashboard">
    <section class="page-card dashboard-hero" aria-labelledby="supplier-dashboard-title" aria-describedby="supplier-dashboard-desc">
      <div>
        <span class="pill">供应商看板</span>
        <h1 id="supplier-dashboard-title" class="hero-title">当前经营概览</h1>
        <p id="supplier-dashboard-desc" class="hero-desc">
          快速了解待发货订单、商品上架情况与审核进度，及时响应社区团购需求。
        </p>
        <p class="sr-only">
          待发货订单 {{ stats.pendingShipments }} 笔，上架商品 {{ stats.activeProducts }} 个，待审核商品 {{ stats.pendingProducts }} 个。
        </p>
      </div>
      <el-button type="primary" @click="$router.push('/supplier/orders')">
        前往订单发货
      </el-button>
    </section>

    <section class="page-card" role="region" aria-labelledby="supplier-stats-title">
      <h2 id="supplier-stats-title" class="sr-only">关键经营指标</h2>
      <div class="stat-grid">
        <div class="stat-card">
          <span class="stat-card__title">待发货订单</span>
          <span class="stat-card__value">{{ stats.pendingShipments }}</span>
          <span class="stat-card__trend">需要尽快处理</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">已上架商品</span>
          <span class="stat-card__value">{{ stats.activeProducts }}</span>
          <span class="stat-card__trend">含审核通过商品</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">待审核商品</span>
          <span class="stat-card__value">{{ stats.pendingProducts }}</span>
          <span class="stat-card__trend">请关注平台审核</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">近7日发货</span>
          <span class="stat-card__value">{{ stats.shippedLastWeek }}</span>
          <span class="stat-card__trend">含社区自提订单</span>
        </div>
      </div>
    </section>

    <section class="page-card latest-section" role="region" aria-labelledby="supplier-latest-orders-title">
      <header class="page-section-title">
        <h2 id="supplier-latest-orders-title">待发货订单</h2>
        <el-button text type="primary" @click="$router.push('/supplier/shipments')">查看全部</el-button>
      </header>
      <el-table :data="recentShipments" v-loading="loading" border aria-describedby="supplier-latest-orders-title">
        <el-table-column prop="orderId" label="订单号" width="140" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">¥ {{ format(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column prop="communityName" label="配送社区" />
      </el-table>
      <el-empty v-if="!loading && !recentShipments.length" description="暂无待发货订单" />
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { pendingShipments, listSupplierProducts } from '@/api/supplier'

const user = useUserStore()
const loading = ref(false)
const recentShipments = ref([])
const stats = reactive({
  pendingShipments: 0,
  activeProducts: 0,
  pendingProducts: 0,
  shippedLastWeek: 0
})

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const loadDashboard = async () => {
  if (!user.isSupplier) return
  loading.value = true
  try {
    const [shipRes, productRes] = await Promise.allSettled([
      pendingShipments(user.userId),
      listSupplierProducts(user.userId, { page: 1, size: 100 })
    ])

    if (shipRes.status === 'fulfilled' && shipRes.value.code === 0) {
      const items = shipRes.value.orders || shipRes.value.list || shipRes.value.data?.list || []
      stats.pendingShipments = items.length
      recentShipments.value = items.slice(0, 6)
    } else if (shipRes.status === 'fulfilled') {
      ElMessage.error(shipRes.value.msg || '待发货订单加载失败')
    }

    if (productRes.status === 'fulfilled' && productRes.value.code === 0) {
      const products = productRes.value.records || productRes.value.data?.records || productRes.value.list || []
      stats.activeProducts = products.filter((p) => p.status === true || p.status === 1).length
      stats.pendingProducts = products.filter((p) => !p.approved).length
    } else if (productRes.status === 'fulfilled') {
      ElMessage.error(productRes.value.msg || '商品数据加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '数据加载异常')
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.supplier-dashboard {
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

.latest-section {
  display: grid;
  gap: 20px;
}
</style>
