<template>
  <div class="page-wrap product-detail">
    <el-skeleton v-if="loading" animated :throttle="200" :rows="6" />
    <template v-else>
      <section class="page-card product-hero">
        <div class="product-gallery">
          <div class="hero-thumb">
            <span>{{ thumbnailText }}</span>
          </div>
          <div class="thumb-meta">
            <span class="pill" v-if="item?.categoryName">分类：{{ item.categoryName }}</span>
            <span class="thumb-stock">当前库存：{{ item?.stock ?? 0 }}</span>
            <span class="thumb-supplier">供应商 ID：{{ item?.supplierId || '暂无' }}</span>
          </div>
        </div>
        <div class="product-info">
          <h1 class="product-title">{{ item?.name }}</h1>
          <p class="product-description">
            {{ item?.description || '该商品由可信赖的社区供应商提供，团长实时跟踪到货状态，保证品质与时效。' }}
          </p>
          <div class="product-price">
            <span class="product-price__label">团购价</span>
            <span class="product-price__value">¥ {{ item?.price }}</span>
          </div>
          <div class="purchase-panel">
            <div class="quantity-control">
              <span>数量</span>
              <el-input-number v-model="qty" :min="1" :max="item?.stock || 1" />
            </div>
            <el-button type="primary" size="large" @click="add">
              加入购物车
            </el-button>
          </div>
          <div class="product-guarantee">
            <el-icon><Finished /></el-icon>
            <span>支持社区自提/团长配送，订单全程可追踪</span>
          </div>
        </div>
      </section>

      <section class="page-card">
        <div class="page-section-title">
          <span>商品详情</span>
        </div>
        <el-descriptions :column="2" border class="product-descriptions">
          <el-descriptions-item label="商品名称">{{ item?.name }}</el-descriptions-item>
          <el-descriptions-item label="商品价格">¥ {{ item?.price }}</el-descriptions-item>
          <el-descriptions-item label="当前库存">{{ item?.stock ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="供应商 ID">{{ item?.supplierId || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="上架状态">
            <el-tag type="success">可售</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="配送说明">
            团长收货成功后 2 小时内完成社区配送
          </el-descriptions-item>
        </el-descriptions>
      </section>

      <section class="page-card">
        <div class="page-section-title">
          <span>购买须知</span>
        </div>
        <ul class="product-notice">
          <li>下单后可在「我的订单」中查看配送进度并支持售后申请。</li>
          <li>如库存不足，团长会及时通知并提供替代推荐。</li>
          <li>建议提前确认收货地址，确保与团长负责的社区一致。</li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Finished } from '@element-plus/icons-vue'
import { getProduct } from '@/api/products'
import { addToCart } from '@/api/cart'
import { useUserStore } from '@/store/user'

const route = useRoute()
const user = useUserStore()

const item = ref(null)
const qty = ref(1)
const loading = ref(true)

const load = async () => {
  loading.value = true
  try {
    const res = await getProduct(route.params.id)
    if (res.code === 0 || res.success === true || res.status === 200) {
      const data = res.data || res
      item.value = data.item || data.product || data
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载异常')
  } finally {
    loading.value = false
  }
}

const add = async () => {
  if (!user.isLogin) {
    ElMessage.error('请先登录')
    return
  }
  try {
    const res = await addToCart(user.userId, Number(route.params.id), qty.value)
    if (res.code === 0 || res.success === true) {
      ElMessage.success('已加入购物车')
    } else {
      ElMessage.error(res.msg || '加入失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加入失败')
  }
}

const thumbnailText = computed(() => {
  if (!item.value?.name) return 'GO'
  return item.value.name.slice(0, 2).toUpperCase()
})

onMounted(load)
</script>

<style scoped>
.product-detail {
  gap: 28px;
}

.product-hero {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 32px;
}

.product-gallery {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-thumb {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(76, 110, 245, 0.2), rgba(255, 122, 122, 0.14));
  display: grid;
  place-items: center;
  font-size: 42px;
  font-weight: 700;
  color: var(--brand-primary);
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.14);
}

.thumb-meta {
  display: grid;
  gap: 10px;
  font-size: 13px;
  color: var(--text-2);
}

.thumb-stock,
.thumb-supplier {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(76, 110, 245, 0.08);
  color: var(--brand-primary);
  font-weight: 600;
}

.product-info {
  display: grid;
  gap: 18px;
}

.product-title {
  font-size: 30px;
  font-weight: 700;
  margin: 0;
  color: var(--text-1);
}

.product-description {
  margin: 0;
  font-size: 15px;
  color: var(--text-2);
  line-height: 1.7;
}

.product-price {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 8px;
}

.product-price__label {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 122, 122, 0.16);
  color: var(--brand-accent);
  font-size: 13px;
}

.product-price__value {
  font-size: 36px;
  font-weight: 700;
  color: var(--brand-accent);
}

.purchase-panel {
  display: flex;
  align-items: center;
  gap: 16px;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  border-radius: 16px;
  background: rgba(76, 110, 245, 0.06);
}

.product-guarantee {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 14px;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  font-size: 13px;
}

.product-descriptions :deep(.el-descriptions__cell) {
  padding: 16px;
}

.product-notice {
  margin: 0;
  padding-left: 20px;
  color: var(--text-2);
  display: grid;
  gap: 8px;
}

@media (max-width: 1024px) {
  .product-hero {
    grid-template-columns: 1fr;
  }
  .hero-thumb {
    aspect-ratio: 3 / 2;
  }
}

@media (max-width: 640px) {
  .purchase-panel {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
