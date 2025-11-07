<template>
  <div class="page-wrap admin-dashboard">
    <section class="page-card dashboard-hero" aria-labelledby="admin-dashboard-title" aria-describedby="admin-dashboard-desc">
      <div>
        <span class="pill">平台总览</span>
        <h1 id="admin-dashboard-title" class="hero-title">社区团购运营指标</h1>
        <p id="admin-dashboard-desc" class="hero-desc">
          按时间范围查看全平台订单、GMV 与履约情况，洞察业务增长动向。
        </p>
        <p class="sr-only">
          当前统计显示订单总数 {{ overview.total }}，完成率 {{ completionRate }}%，GMV {{ format(overview.gmv) }} 元。
        </p>
      </div>
      <div class="hero-actions" aria-label="筛选时间">
        <span id="admin-dashboard-filter-tip" class="sr-only">调整时间范围后指标会刷新。</span>
        <el-date-picker
          v-model="range"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :unlink-panels="true"
          @change="load"
          aria-label="选择运营指标时间范围"
          aria-describedby="admin-dashboard-filter-tip"
        />
        <el-button type="primary" @click="load" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </section>

    <section class="page-card" role="region" aria-labelledby="admin-keymetrics-title">
      <h2 id="admin-keymetrics-title" class="sr-only">关键指标</h2>
      <div class="stat-grid">
        <div class="stat-card">
          <span class="stat-card__title">订单总数</span>
          <span class="stat-card__value">{{ overview.total }}</span>
          <span class="stat-card__trend">含所有社区订单</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">已支付</span>
          <span class="stat-card__value">{{ overview.paid }}</span>
          <span class="stat-card__trend">支付转化率 {{ conversionRate }}%</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">已完成</span>
          <span class="stat-card__value">{{ overview.delivered }}</span>
          <span class="stat-card__trend">履约完成率 {{ completionRate }}%</span>
        </div>
        <div class="stat-card">
          <span class="stat-card__title">GMV</span>
          <span class="stat-card__value">¥ {{ format(overview.gmv) }}</span>
          <span class="stat-card__trend">统计周期内总额</span>
        </div>
      </div>
    </section>

    <section class="page-card overview-section" role="region" aria-labelledby="admin-overview-title">
      <h2 id="admin-overview-title" class="sr-only">运营补充数据</h2>
      <el-skeleton v-if="loading" animated :rows="4" />
      <template v-else>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="待发货">{{ overview.shipped }}</el-descriptions-item>
          <el-descriptions-item label="待支付">{{ pendingPayments }}</el-descriptions-item>
          <el-descriptions-item label="平均订单值">¥ {{ averageOrderValue }}</el-descriptions-item>
          <el-descriptions-item label="完成率">
            <el-progress :percentage="completionRate" status="success" :stroke-width="12" />
          </el-descriptions-item>
          <el-descriptions-item label="退款/取消订单">{{ cancelledOrders }}</el-descriptions-item>
          <el-descriptions-item label="平台活跃度">
            <el-tag type="success">良好</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { overviewStats } from '@/api/admin'

const loading = ref(false)
const overview = ref({ total: 0, paid: 0, shipped: 0, delivered: 0, gmv: 0 })
const range = ref([dayjs().subtract(6, 'day').startOf('day').toDate(), dayjs().endOf('day').toDate()])

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const conversionRate = computed(() => {
  if (!overview.value.total) return 0
  return Math.min(100, Math.round((overview.value.paid / overview.value.total) * 100))
})

const completionRate = computed(() => {
  if (!overview.value.total) return 0
  return Math.min(100, Math.round((overview.value.delivered / overview.value.total) * 100))
})

const averageOrderValue = computed(() => {
  if (!overview.value.total) return '0.00'
  return format(overview.value.gmv / overview.value.total)
})

const pendingPayments = computed(() => {
  return Math.max(overview.value.total - overview.value.paid, 0)
})

const cancelledOrders = computed(() => {
  return Math.max(overview.value.total - overview.value.delivered - overview.value.shipped, 0)
})

const load = async () => {
  loading.value = true
  try {
    const [from, to] = range.value || []
    const fmt = (d) => (d ? dayjs(d).toISOString() : undefined)
    const res = await overviewStats(fmt(from), fmt(to))
    if (res.code === 0) {
      overview.value = res.overview || res.data?.overview || overview.value
    } else {
      ElMessage.error(res.msg || '加载平台数据失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载异常')
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.admin-dashboard {
  gap: 24px;
}

.dashboard-hero {
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

.hero-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.overview-section :deep(.el-descriptions__cell) {
  padding: 16px;
}

@media (max-width: 768px) {
  .dashboard-hero {
    flex-direction: column;
  }
  .hero-actions {
    flex-wrap: wrap;
  }
}
</style>
