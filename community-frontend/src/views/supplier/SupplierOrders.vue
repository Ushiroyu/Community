<template>
  <div class="page-wrap supplier-orders">
    <section class="page-card orders-hero" aria-labelledby="supplier-orders-title" aria-describedby="supplier-orders-desc">
      <div>
        <span class="pill">订单发货</span>
        <h1 id="supplier-orders-title" class="hero-title">处理社区待发货订单</h1>
        <p id="supplier-orders-desc" class="hero-desc">
          填写物流单号后即可确认发货，支持批量刷新数据，确保团长及时收到货品。
        </p>
        <p class="sr-only">
          列表展示待发货订单，填写物流单号后点击发货即可更新状态。
        </p>
      </div>
      <el-button type="primary" @click="load" :loading="loading">刷新列表</el-button>
    </section>

    <section class="table-card" aria-labelledby="supplier-orders-table-title" role="region">
      <header class="table-card__header table-header">
        <h2 id="supplier-orders-table-title" class="page-section-title">待发货订单</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="supplier-orders-table-title">
          <el-table-column prop="id" label="订单号" width="140" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥ {{ format(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag type="warning">待发货</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="物流单号" min-width="220">
            <template #default="{ row }">
              <el-input
                v-model="row._trackingNo"
                placeholder="请输入物流单号"
                clearable
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="ship(row)"
                :loading="row._shipLoading"
              >
                发货
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无待发货订单" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { pendingShipments, shipOrder } from '@/api/supplier'

const user = useUserStore()
const list = ref([])
const loading = ref(false)

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const load = async () => {
  loading.value = true
  try {
    const res = await pendingShipments(user.userId)
    if (res.code === 0) {
      const orders = res.orders || res.list || res.data?.list || []
      list.value = orders.map((order) => ({ ...order, _trackingNo: '', _shipLoading: false }))
    } else {
      ElMessage.error(res.msg || '加载待发货订单失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const ship = async (row) => {
  if (!row._trackingNo) {
    ElMessage.warning('请填写物流单号')
    return
  }
  row._shipLoading = true
  try {
    const res = await shipOrder(row.id || row.orderId, row._trackingNo)
    if (res.code === 0) {
      ElMessage.success('发货成功')
      await load()
    } else {
      ElMessage.error(res.msg || '发货失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '发货异常')
  } finally {
    row._shipLoading = false
  }
}

onMounted(load)
</script>

<style scoped>
.supplier-orders {
  gap: 24px;
}

.orders-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
