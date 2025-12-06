<template>
  <div class="page-wrap checkout-view">
    <section
      class="page-card checkout-hero"
      aria-labelledby="checkout-hero-title"
      aria-describedby="checkout-instructions"
    >
      <div>
        <span class="pill">订单结算</span>
        <h1 id="checkout-hero-title" class="checkout-title">确认收货信息并完成支付</h1>
        <p class="checkout-desc">
          根据购物车生成订单，请确认收货地址与团长信息后再提交，系统会自动完成支付以确保库存。
        </p>
      </div>
      <el-steps :active="activeStep" align-center finish-status="success">
        <el-step title="选择地址" description="绑定社区并确认团长" />
        <el-step title="确认商品" description="核对购物车与数量" />
        <el-step title="完成支付" description="在线支付并等待配送" />
      </el-steps>
      <p id="checkout-instructions" class="sr-only">
        先选择绑定社区的地址，再确认购物车内商品，最后提交订单完成支付。
      </p>
    </section>

    <div class="checkout-body">
      <section
        class="page-card address-card"
        aria-labelledby="checkout-address-title"
        role="region"
        aria-describedby="checkout-instructions"
      >
        <header class="page-section-title address-header">
          <div>
            <h2 id="checkout-address-title">收货地址</h2>
            <p class="muted">结算前需绑定社区，系统才能匹配团长和配送范围。</p>
          </div>
          <el-button type="primary" text @click="router.push('/u/addresses')">
            管理地址
          </el-button>
        </header>
        <el-skeleton v-if="loadingAddresses" animated :rows="3" />
        <el-radio-group v-else v-model="addressId" class="address-group">
          <el-radio
            v-for="address in addresses"
            :key="address.id"
            :label="address.id"
            border
            class="address-item"
          >
            <div class="address-meta">
              <div class="address-detail">{{ address.detail }}</div>
              <div class="address-sub">
                <span>社区 ID：{{ address.communityId || '未绑定' }}</span>
                <span>ID：{{ address.id }}</span>
              </div>
            </div>
            <el-tag v-if="addressId === address.id" type="success" effect="plain">
              当前使用
            </el-tag>
          </el-radio>
        </el-radio-group>
        <el-empty
          v-if="!loadingAddresses && !addresses.length"
          description="暂无可用收货地址"
        >
          <el-button type="primary" @click="router.push('/u/addresses')">
            去新增地址
          </el-button>
        </el-empty>
      </section>

      <section
        class="page-card order-card"
        aria-labelledby="checkout-form-title"
        role="region"
        aria-describedby="checkout-instructions"
      >
        <header class="page-section-title order-card-header">
          <div>
            <h2 id="checkout-form-title">订单信息</h2>
            <p class="muted">系统默认读取购物车清单，请核对商品与数量。</p>
          </div>
          <div class="order-card-actions">
            <el-button text @click="router.push('/u/cart')">返回购物车</el-button>
            <el-button text type="primary" @click="loadCart" :loading="loadingCart">
              刷新清单
            </el-button>
          </div>
        </header>

        <div class="order-card__body" v-loading="loadingCart">
          <template v-if="hasItems">
            <div class="order-items">
              <div
                v-for="item in cartItems"
                :key="item.productId"
                class="order-item"
              >
                <div class="order-thumb">{{ getInitials(item.productName) }}</div>
                <div class="order-info">
                  <div class="order-name">{{ item.productName || '未命名商品' }}</div>
                  <div class="order-meta">
                    <span>ID：{{ item.productId }}</span>
                    <span>数量：{{ item.quantity }}</span>
                  </div>
                </div>
                <div class="order-price">
                  ￥{{ formatCurrency((item.price || 0) * (item.quantity || 0)) }}
                </div>
              </div>
            </div>

            <el-form label-width="88px" :model="form" class="order-form">
              <el-form-item label="备注信息">
                <el-input
                  v-model="form.remark"
                  placeholder="可填写团长交付、配送时间等补充信息"
                  type="textarea"
                  rows="2"
                />
              </el-form-item>
            </el-form>

            <div class="order-summary">
              <div class="summary-row">
                <span>商品件数</span>
                <span>{{ totalQuantity }}</span>
              </div>
              <div class="summary-row">
                <span>预计总额</span>
                <span>￥{{ totalAmountDisplay }}</span>
              </div>
            </div>

            <div class="order-actions">
              <el-button
                type="primary"
                size="large"
                :loading="submitting"
                @click="submit"
              >
                提交订单并支付
              </el-button>
              <p class="order-tip">
                <el-icon><Notification /></el-icon>
                <span>系统会逐条提交购物车中的商品并自动扣款，请确保余额充足。</span>
              </p>
            </div>
          </template>
          <el-empty v-else description="购物车为空，无法结算">
            <el-button type="primary" @click="router.push('/shop')">
              返回选购
            </el-button>
          </el-empty>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Notification } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { listAddresses } from '@/api/users'
import { getCommunity } from '@/api/leader'
import { listCart, clearCart } from '@/api/cart'
import { createOrder, payOrder } from '@/api/orders'

const router = useRouter()
const user = useUserStore()

const addresses = ref([])
const addressId = ref(null)
const loadingAddresses = ref(false)
const loadingCart = ref(false)
const submitting = ref(false)

const cartItems = ref([])

const hasItems = computed(() => cartItems.value.length > 0)
const totalQuantity = computed(() =>
  cartItems.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
)
const totalAmount = computed(() =>
  cartItems.value.reduce(
    (sum, item) => sum + ((Number(item.price) || 0) * (item.quantity || 0)),
    0
  )
)
const totalAmountDisplay = computed(() => formatCurrency(totalAmount.value))

const activeStep = computed(() => {
  if (!addressId.value) return 0
  if (!hasItems.value) return 1
  return submitting.value ? 2 : 1
})

const form = reactive({
  remark: ''
})

const formatCurrency = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const getInitials = (value = '') => {
  if (!value) return 'GO'
  return value.slice(0, 2).toUpperCase()
}

const loadAddresses = async () => {
  loadingAddresses.value = true
  try {
    const res = await listAddresses(user.userId)
    if (res.code === 0 || res.success === true) {
      addresses.value =
        res.addresses || res.data?.addresses || res.data || []
      const def = res.defaultAddressId ?? res.data?.defaultAddressId
      if (def) {
        addressId.value = def
      } else if (addresses.value.length && !addressId.value) {
        addressId.value = addresses.value[0].id
      }
    } else {
      ElMessage.error(res.msg || '加载地址失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载地址失败')
  } finally {
    loadingAddresses.value = false
  }
}

const loadCart = async () => {
  loadingCart.value = true
  try {
    const res = await listCart(user.userId)
    if (res.code === 0 || res.success === true) {
      cartItems.value = res.data?.items || res.items || []
    } else {
      ElMessage.error(res.msg || '获取购物车失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '获取购物车失败')
  } finally {
    loadingCart.value = false
  }
}

const fetchLeaderId = async (communityId) => {
  if (!communityId) return null
  try {
    const res = await getCommunity(communityId)
    // 优先返回团长用户ID（用于订单按用户ID筛选）
    const pickLeaderUserId = (obj) =>
      obj?.community?.leaderUserId ??
      obj?.leaderUserId ??
      obj?.community?.leaderId ??
      obj?.leaderId
    return pickLeaderUserId(res) ?? pickLeaderUserId(res?.data)
  } catch {
    return null
  }
}

const submit = async () => {
  if (!addressId.value) {
    ElMessage.error('请先选择收货地址')
    return
  }
  if (!hasItems.value) {
    ElMessage.error('购物车中没有可以结算的商品')
    return
  }

  submitting.value = true
  try {
    const address = addresses.value.find((a) => a.id === addressId.value)
    const leaderId = await fetchLeaderId(address?.communityId) || null

    const orderIds = []
    for (const item of cartItems.value) {
      const createRes = await createOrder({
        userId: user.userId,
        leaderId,
        productId: item.productId,
        quantity: item.quantity,
        addressId: addressId.value,
        remark: form.remark
      })

      if (createRes.code !== 0) {
        throw new Error(createRes.msg || '下单失败')
      }

      const orderId = createRes.data?.orderId || createRes.orderId
      if (!orderId) {
        throw new Error('未获取到订单号')
      }
      orderIds.push(orderId)

      const payRes = await payOrder(orderId)
      if (payRes.code !== 0) {
        throw new Error(payRes.msg || '支付失败')
      }
    }

    await clearCart(user.userId)
    cartItems.value = []
    ElMessage.success(`已完成 ${orderIds.length} 笔订单支付，可前往订单列表查看详情`)
    router.push('/u/orders')
  } catch (error) {
    ElMessage.error(error.message || '结算失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadAddresses()
  loadCart()
})
</script>

<style scoped>
.checkout-view {
  gap: 24px;
}

.checkout-hero {
  display: grid;
  gap: 24px;
}

.checkout-title {
  margin: 8px 0 12px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.checkout-desc {
  margin: 0;
  color: var(--text-2);
  font-size: 15px;
  line-height: 1.6;
}

.checkout-body {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 24px;
  align-items: start;
}

.address-header,
.order-card-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.address-group {
  display: grid;
  gap: 12px;
}

.address-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-radius: 16px;
}

.address-item :deep(.el-radio__inner) {
  width: 16px;
  height: 16px;
}

.address-item :deep(.el-radio__label) {
  flex: 1;
  display: block;
}

.address-meta {
  display: grid;
  gap: 6px;
}

.address-detail {
  font-weight: 600;
  color: var(--text-1);
}

.address-sub {
  font-size: 12px;
  color: var(--text-2);
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.order-card__body {
  display: grid;
  gap: 16px;
}

.order-items {
  display: grid;
  gap: 12px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 16px;
}

.order-thumb {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(76, 110, 245, 0.18), rgba(255, 122, 122, 0.12));
  display: grid;
  place-items: center;
  font-weight: 700;
  color: var(--brand-primary);
}

.order-info {
  flex: 1;
  display: grid;
  gap: 4px;
}

.order-name {
  font-weight: 600;
  color: var(--text-1);
}

.order-meta {
  font-size: 12px;
  color: var(--text-2);
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.order-price {
  font-weight: 600;
  color: var(--text-1);
}

.order-form {
  max-width: 100%;
}

.order-summary {
  border-top: 1px solid var(--el-border-color-light);
  padding-top: 12px;
  display: grid;
  gap: 6px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--text-2);
}

.order-actions {
  display: grid;
  gap: 12px;
}

.order-tip {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(76, 110, 245, 0.12);
  color: var(--brand-primary);
  font-size: 12px;
}

@media (max-width: 960px) {
  .checkout-body {
    grid-template-columns: 1fr;
  }

  .order-card-header,
  .address-header {
    flex-direction: column;
  }
}
</style>
