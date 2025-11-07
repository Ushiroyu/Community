<template>
  <div class="page-wrap supplier-stats">
    <section class="page-card stats-hero" aria-labelledby="supplier-stats-hero-title" aria-describedby="supplier-stats-hero-desc">
      <div>
        <span class="pill">经营分析</span>
        <h1 id="supplier-stats-hero-title" class="hero-title">供应链履约表现</h1>
        <p id="supplier-stats-hero-desc" class="hero-desc">
          通过时间维度查看订单履约情况、审核进度与GMV趋势，及时调整供货节奏。
        </p>
        <p class="sr-only">
          当前统计显示订单总数 {{ overview.total }}，已发货 {{ overview.shipped }}，GMV 总额 {{ format(overview.gmv) }} 元。
        </p>
      </div>
      <div class="hero-filter" aria-label="筛选时间">
        <span id="supplier-stats-filter-tip" class="sr-only">调整时间范围后数据会刷新。</span>
        <el-date-picker
          v-model="range"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :unlink-panels="true"
          @change="load"
          aria-label="选择统计时间范围"
          aria-describedby="supplier-stats-filter-tip"
        />
        <el-button type="primary" @click="load" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </section>

    <section class="page-card overview-card" role="region" aria-labelledby="supplier-overview-title">
      <h2 id="supplier-overview-title" class="sr-only">订单履约概览</h2>
      <el-skeleton v-if="loading" animated :rows="3" />
      <template v-else>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="订单总数">{{ overview.total }}</el-descriptions-item>
          <el-descriptions-item label="已支付">{{ overview.paid }}</el-descriptions-item>
          <el-descriptions-item label="已发货">{{ overview.shipped }}</el-descriptions-item>
          <el-descriptions-item label="已签收">{{ overview.delivered }}</el-descriptions-item>
          <el-descriptions-item label="GMV">¥ {{ format(overview.gmv) }}</el-descriptions-item>
          <el-descriptions-item label="通过率">
            <el-progress
              :percentage="Math.min(Math.round((overview.approved / (overview.total || 1)) * 100), 100)"
              :stroke-width="12"
              status="success"
            />
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </section>

    <section class="page-card insight-grid">
      <article class="insight-card">
        <h3>发货准时率</h3>
        <p class="insight-value">{{ onTimeRate }}%</p>
        <p class="muted">按计划时间内发货的订单占比。</p>
      </article>
      <article class="insight-card">
        <h3>平均出货单</h3>
        <p class="insight-value">¥ {{ avgShipment }}</p>
        <p class="muted">衡量单笔订单金额规模。</p>
      </article>
      <article class="insight-card">
        <h3>待审核商品</h3>
        <p class="insight-value">{{ overview.pendingProducts || 0 }}</p>
        <p class="muted">需关注商品审核进度。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import http from '@/api/http'
import { useUserStore } from '@/store/user'

const user = useUserStore()
const loading = ref(false)
const overview = ref({ total: 0, paid: 0, shipped: 0, delivered: 0, gmv: 0, approved: 0, pendingProducts: 0 })
const range = ref([
  dayjs().subtract(6, 'day').startOf('day').toDate(),
  dayjs().endOf('day').toDate()
])

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const onTimeRate = computed(() => {
  if (!overview.value.shipped) return 0
  // 占位数据，实际应由后端返回准时发货数
  return Math.min(100, Math.round((overview.value.delivered / overview.value.shipped) * 100))
})

const avgShipment = computed(() => {
  if (!overview.value.shipped) return '0.00'
  return format(overview.value.gmv / overview.value.shipped)
})

const load = async () => {
  loading.value = true
  try {
    const [from, to] = range.value || []
    const params = {
      supplierId: user.userId,
      from: from ? dayjs(from).toISOString() : undefined,
      to: to ? dayjs(to).toISOString() : undefined
    }
    const res = await http.get('/stats/supplier-overview', { params })
    if (res.code === 0) {
      overview.value = res.overview || res.data?.overview || overview.value
    } else {
      ElMessage.error(res.msg || '统计数据加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '统计异常')
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.supplier-stats {
  gap: 24px;
}

.stats-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
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

.hero-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overview-card :deep(.el-descriptions__cell) {
  padding: 16px;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.insight-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 10px;
}

.insight-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.insight-value {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-primary);
}

@media (max-width: 768px) {
  .stats-hero {
    flex-direction: column;
  }
  .hero-filter {
    flex-wrap: wrap;
  }
}
</style>
