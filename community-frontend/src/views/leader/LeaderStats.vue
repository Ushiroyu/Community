<template>
  <div class="page-wrap leader-stats">
    <section class="page-card stats-hero" aria-labelledby="leader-stats-hero-title" aria-describedby="leader-stats-hero-desc">
      <div>
        <span class="pill">数据分析</span>
        <h1 id="leader-stats-hero-title" class="hero-title">洞察社区订单趋势</h1>
        <p id="leader-stats-hero-desc" class="hero-desc">
          通过自定义时间范围查看订单转化情况，掌握发货、签收效率以及社区居民偏好。
        </p>
        <p class="sr-only">
          当前时间段内订单总数 {{ overview.total }}，已支付 {{ overview.paid }}，已送达 {{ overview.delivered }}。
        </p>
      </div>
      <div class="hero-filter" aria-label="筛选时间">
        <span id="leader-stats-filter-tip" class="sr-only">选择起止时间后会自动刷新统计数据。</span>
        <el-date-picker
          v-model="range"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :unlink-panels="true"
          @change="load"
          aria-label="选择统计时间范围"
          aria-describedby="leader-stats-filter-tip"
        />
        <el-button type="primary" @click="load" :loading="loading">
          刷新数据
        </el-button>
      </div>
    </section>

    <section class="page-card overview-card" role="region" aria-labelledby="leader-overview-title">
      <h2 id="leader-overview-title" class="sr-only">订单汇总数据</h2>
      <el-skeleton v-if="loading" animated :rows="4" />
      <template v-else>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="订单总数">{{ overview.total }}</el-descriptions-item>
          <el-descriptions-item label="已支付">{{ overview.paid }}</el-descriptions-item>
          <el-descriptions-item label="在途配送">{{ overview.shipped }}</el-descriptions-item>
          <el-descriptions-item label="已送达">{{ overview.delivered }}</el-descriptions-item>
          <el-descriptions-item label="GMV（总额）">¥ {{ format(overview.gmv) }}</el-descriptions-item>
          <el-descriptions-item label="完成率">
            <el-progress
              :percentage="Math.min(Math.round((overview.delivered / (overview.total || 1)) * 100), 100)"
              :stroke-width="12"
              status="success"
            />
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </section>

    <section class="page-card insight-grid">
      <article class="insight-card">
        <h3>支付转化率</h3>
        <p class="insight-value">
          {{ conversionRate }}%
        </p>
        <p class="muted">订单支付率反映居民参与热度。</p>
      </article>
      <article class="insight-card">
        <h3>平均订单金额</h3>
        <p class="insight-value">¥ {{ avgOrderAmount }}</p>
        <p class="muted">衡量社区消费能力与商品结构。</p>
      </article>
      <article class="insight-card">
        <h3>配送完成时效</h3>
        <p class="insight-value">{{ fulfillHours }} 小时</p>
        <p class="muted">从发货到签收的平均耗时。</p>
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
const overview = ref({ total: 0, paid: 0, shipped: 0, delivered: 0, gmv: 0 })
const range = ref([
  dayjs().subtract(6, 'day').startOf('day').toDate(),
  dayjs().endOf('day').toDate()
])

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const conversionRate = computed(() => {
  if (!overview.value.total) return 0
  return Math.round((overview.value.paid / overview.value.total) * 100)
})

const avgOrderAmount = computed(() => {
  if (!overview.value.total) return '0.00'
  return format(overview.value.gmv / overview.value.total)
})

const fulfillHours = computed(() => {
  if (!overview.value.shipped) return 0
  // 仅作为占位数据，实际应由后端返回
  return 6
})

const load = async () => {
  loading.value = true
  try {
    const [from, to] = range.value || []
    const params = {
      leaderId: user.userId,
      from: from ? dayjs(from).toISOString() : undefined,
      to: to ? dayjs(to).toISOString() : undefined
    }
    const res = await http.get('/stats/leader-overview', { params })
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
.leader-stats {
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
  gap: 12px;
  align-items: center;
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
  color: var(--text-1);
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
