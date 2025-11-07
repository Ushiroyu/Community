<template>
  <div class="page-wrap supplier-shipments">
    <section class="page-card shipments-hero" aria-labelledby="supplier-shipments-title" aria-describedby="supplier-shipments-desc">
      <div>
        <span class="pill">发货管理</span>
        <h1 id="supplier-shipments-title" class="hero-title">批量处理配送单据</h1>
        <p id="supplier-shipments-desc" class="hero-desc">
          输入供应商 ID 自动查询待发货订单，录入物流单号后即可完成发货确认。
        </p>
        <p class="sr-only">
          输入供应商编号后点击查询即可加载待发货订单，填写物流单号并点击发货更新状态。
        </p>
      </div>
      <div class="hero-actions" aria-label="供应商筛选">
        <el-input
          v-model="supplierId"
          placeholder="供应商 ID"
          clearable
          style="width: 220px"
          aria-label="输入供应商 ID"
        />
        <el-button type="primary" @click="load" :loading="loading">查询待发货</el-button>
      </div>
    </section>

    <section class="table-card" role="region" aria-labelledby="supplier-shipments-table-title">
      <header class="table-card__header table-header">
        <h2 id="supplier-shipments-table-title" class="page-section-title">待发货订单</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="supplier-shipments-table-title">
          <el-table-column prop="orderId" label="订单号" width="140" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">¥ {{ format(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="120" />
          <el-table-column label="物流单号" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row._trackingNo" placeholder="请输入物流单号" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button
                size="small"
                type="primary"
                @click="doShip(row)"
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
import { pendingShipments, shipOrder, me as meSupplier } from '@/api/supplier'
import { useUserStore } from '@/store/user'

const user = useUserStore()
const supplierId = ref('')
const list = ref([])
const loading = ref(false)

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

onMounted(async () => {
  try {
    if (user.isSupplier && !supplierId.value) {
      const res = await meSupplier()
      const sid = res?.supplier?.id ?? res?.data?.supplier?.id
      if (sid) supplierId.value = String(sid)
    }
  } catch (error) {
    console.warn('自动获取供应商ID失败', error)
  }
})

const load = async () => {
  if (!supplierId.value) {
    ElMessage.warning('请先输入供应商ID')
    return
  }
  loading.value = true
  try {
    const res = await pendingShipments(supplierId.value)
    if (res.code === 0) {
      const items = res.list ?? res.data?.list ?? res.orders ?? []
      list.value = items.map((item) => ({ ...item, _trackingNo: '', _shipLoading: false }))
    } else {
      ElMessage.error(res.msg || '加载待发货订单失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const doShip = async (row) => {
  if (!row._trackingNo) {
    ElMessage.warning('请填写物流单号')
    return
  }
  row._shipLoading = true
  try {
    const res = await shipOrder(row.orderId, row._trackingNo)
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
</script>

<style scoped>
.supplier-shipments {
  gap: 24px;
}

.shipments-hero {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  gap: 12px;
  flex-wrap: wrap;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
