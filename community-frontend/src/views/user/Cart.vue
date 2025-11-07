<template>
  <div class="page-wrap cart-view">
    <section class="page-card cart-hero" aria-labelledby="cart-hero-title" aria-describedby="cart-status">
      <div>
        <span class="pill">购物车</span>
        <h1 id="cart-hero-title" class="cart-title">待结算商品清单</h1>
        <p class="cart-desc">
          汇总所有加入购物车的商品，支持一键刷新库存与价格。请在结算前确认配送地址与团长范围。
        </p>
      </div>
      <div class="cart-hero-meta">
        <div class="meta-block">
          <span class="meta-value">{{ totalQuantity }}</span>
          <span class="meta-label">件商品</span>
        </div>
        <div class="meta-block">
          <span class="meta-value">¥ {{ totalAmount }}</span>
          <span class="meta-label">预计合计</span>
        </div>
        <div class="meta-block">
          <span class="meta-value">{{ items.length }}</span>
          <span class="meta-label">待处理条目</span>
        </div>
      </div>
      <p id="cart-status" class="sr-only" role="status">
        当前购物车包含 {{ items.length }} 种商品，共 {{ totalQuantity }} 件，预计金额 {{ totalAmount }} 元。
      </p>
    </section>

    <div class="cart-body">
      <section class="table-card" aria-labelledby="cart-details-title" role="region">
        <header class="table-card__header cart-table-header">
          <div>
            <h2 id="cart-details-title" class="page-section-title">购物车明细</h2>
            <p class="muted">可在下单前修改数量，随时掌握商品库存动态。</p>
          </div>
          <div class="cart-header-actions">
            <el-button text type="primary" @click="load" :loading="loading">
              刷新库存
            </el-button>
            <el-button
              text
              type="danger"
              @click="clearAll"
              :disabled="!hasItems"
            >
              清空购物车
            </el-button>
          </div>
        </header>

        <div class="table-card__body" id="cart-table-desc">
          <el-table
            v-if="hasItems"
            :data="items"
            v-loading="loading"
            border
            aria-label="购物车商品列表"
          >
            <el-table-column label="商品" min-width="240">
              <template #default="{ row }">
                <div class="product-cell">
                  <div class="product-thumb">{{ getInitials(row.productName) }}</div>
                  <div>
                    <div class="product-name">{{ row.productName }}</div>
                    <div class="product-id">ID：{{ row.productId }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="140" align="center">
              <template #default="{ row }">¥ {{ format(row.price) }}</template>
            </el-table-column>
            <el-table-column label="数量" width="120" align="center">
              <template #default="{ row }">{{ row.quantity }}</template>
            </el-table-column>
            <el-table-column label="小计" width="160" align="center">
              <template #default="{ row }">
                ¥ {{ format((row.price || 0) * row.quantity) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" align="center">
              <template #default="{ row }">
                <el-button
                  text
                  type="danger"
                  @click="remove(row.productId)"
                  :loading="removingId === row.productId"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty
            v-else
            description="购物车空空如也，去挑选心仪商品吧"
          >
            <el-button type="primary" @click="router.push('/shop')">
              去逛逛
            </el-button>
          </el-empty>
        </div>
      </section>

      <aside class="page-card cart-summary" aria-labelledby="cart-summary-title" aria-live="polite">
        <h2 id="cart-summary-title">结算信息</h2>
        <div class="summary-row">
          <span>商品数量</span>
          <span>{{ totalQuantity }}</span>
        </div>
        <div class="summary-row">
          <span>金额小计</span>
          <span>¥ {{ totalAmount }}</span>
        </div>
        <div class="summary-row summary-total">
          <span>应付金额</span>
          <span>¥ {{ totalAmount }}</span>
        </div>
        <el-button
          type="primary"
          size="large"
          :disabled="!hasItems"
          @click="goCheckout"
        >
          去结算
        </el-button>
        <p class="summary-tip">结算时可选择社区地址，支持团长配送/自提。</p>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { listCart, removeFromCart, clearCart } from '@/api/cart'

const router = useRouter()
const user = useUserStore()

const items = ref([])
const loading = ref(false)
const removingId = ref(null)

const hasItems = computed(() => items.value.length > 0)
const totalAmount = computed(() => format(items.value.reduce((sum, item) => {
  return sum + (item.price || 0) * (item.quantity || 0)
}, 0)))
const totalQuantity = computed(() =>
  items.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
)

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const getInitials = (name = '') => {
  if (!name) return 'GO'
  return name.slice(0, 2).toUpperCase()
}

const load = async () => {
  loading.value = true
  try {
    const response = await listCart(user.userId)
    if (response.code === 0 || response.success === true) {
      items.value = response.data?.items || response.items || []
    } else {
      ElMessage.error(response.msg || '加载购物车失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const remove = async (productId) => {
  removingId.value = productId
  try {
    await removeFromCart(user.userId, productId)
    ElMessage.success('已移除商品')
    await load()
  } catch (error) {
    ElMessage.error(error.message || '移除失败')
  } finally {
    removingId.value = null
  }
}

const clearAll = async () => {
  if (!hasItems.value) return
  try {
    await ElMessageBox.confirm('确认清空购物车？', '提示', { type: 'warning' })
    await clearCart(user.userId)
    ElMessage.success('购物车已清空')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '清空失败')
    }
  }
}

const goCheckout = () => {
  router.push('/u/checkout')
}

onMounted(load)
</script>

<style scoped>
.cart-view {
  gap: 24px;
}

.cart-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: center;
  gap: 24px;
}

.cart-title {
  margin: 8px 0 12px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.cart-desc {
  margin: 0;
  color: var(--text-2);
  font-size: 15px;
  line-height: 1.6;
}

.cart-hero-meta {
  display: flex;
  gap: 16px;
}

.meta-block {
  min-width: 120px;
  padding: 14px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 4px;
  text-align: center;
}

.meta-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--brand-primary);
}

.meta-label {
  font-size: 12px;
  color: var(--text-2);
}

.cart-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
  align-items: flex-start;
}

.cart-table-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.cart-header-actions {
  display: inline-flex;
  gap: 8px;
}

.product-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.product-thumb {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(76, 110, 245, 0.18), rgba(255, 122, 122, 0.12));
  display: grid;
  place-items: center;
  font-weight: 700;
  color: var(--brand-primary);
}

.product-name {
  font-weight: 600;
  color: var(--text-1);
}

.product-id {
  font-size: 12px;
  color: var(--text-2);
}

.cart-summary {
  position: sticky;
  top: 120px;
  display: grid;
  gap: 12px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--text-2);
}

.summary-total {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-1);
}

.summary-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-2);
}

@media (max-width: 1024px) {
  .cart-body {
    grid-template-columns: 1fr;
  }
  .cart-summary {
    position: static;
  }
  .cart-hero {
    grid-template-columns: 1fr;
  }
  .cart-hero-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .cart-hero-meta {
    flex-wrap: wrap;
  }
}
</style>
