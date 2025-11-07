<template>
  <div class="page-wrap checkout-view">
    <section class="page-card checkout-hero" aria-labelledby="checkout-hero-title" aria-describedby="checkout-instructions">
      <div>
        <span class="pill">订单结算</span>
        <h1 id="checkout-hero-title" class="checkout-title">确认收货信息并完成支付</h1>
        <p class="checkout-desc">
          核对收货地址、商品数量后即可生成订单并自动完成支付。系统会为您匹配负责该社区的团长开展配送。
        </p>
      </div>
      <el-steps :active="activeStep" align-center finish-status="success">
        <el-step title="选择地址" description="绑定社区与团长" />
        <el-step title="确认商品" description="填写商品与数量" />
        <el-step title="支付完成" description="等待配送" />
      </el-steps>
      <p id="checkout-instructions" class="sr-only">
        先选择一个已保存的收货地址，确认商品编号和数量，再提交订单并完成支付。
      </p>
    </section>

    <div class="checkout-body">
      <section class="page-card address-card" aria-labelledby="checkout-address-title" role="region" aria-describedby="checkout-instructions">
        <header class="page-section-title">
          <h2 id="checkout-address-title">收货地址</h2>
          <el-button type="primary" text @click="router.push('/u/addresses')">
            管理地址
          </el-button>
        </header>
        <el-skeleton v-if="loadingAddresses" animated :rows="3" />
        <el-radio-group
          v-else
          v-model="addressId"
          class="address-group"
        >
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
                社区 ID：{{ address.communityId || '未关联' }}
              </div>
            </div>
            <el-tag v-if="addressId === address.id" type="success" effect="plain">
              使用中
            </el-tag>
          </el-radio>
        </el-radio-group>
        <el-empty
          v-if="!loadingAddresses && !addresses.length"
          description="您还没有可用的收货地址"
        >
          <el-button type="primary" @click="router.push('/u/addresses')">
            去添加地址
          </el-button>
        </el-empty>
      </section>

      <section class="page-card order-card" aria-labelledby="checkout-form-title" role="region" aria-describedby="checkout-instructions">
        <header class="page-section-title">
          <h2 id="checkout-form-title">填写订单</h2>
        </header>
        <el-form label-width="96px" :model="form" class="order-form">
          <el-form-item label="商品 ID">
            <el-input
              v-model.number="form.productId"
              placeholder="请输入商品 ID"
              clearable
            />
          </el-form-item>
          <el-form-item label="购买数量">
            <el-input-number
              v-model="form.quantity"
              :min="1"
              :max="999"
            />
          </el-form-item>
          <el-form-item label="备注信息">
            <el-input
              v-model="form.remark"
              placeholder="可填写配送需求或团长备注"
              type="textarea"
              rows="2"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              @click="submit"
            >
              提交订单并支付
            </el-button>
          </el-form-item>
        </el-form>
        <div class="order-tip">
          <el-icon><Notification /></el-icon>
          <span>若商品库存不足或团长暂时未接单，系统会自动退款。</span>
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
import { createOrder, payOrder } from '@/api/orders'

const router = useRouter()
const user = useUserStore()

const addresses = ref([])
const addressId = ref(null)
const loadingAddresses = ref(false)
const submitting = ref(false)
const activeStep = computed(() => {
  if (!addressId.value) return 0
  if (submitting.value) return 2
  return 1
})

const form = reactive({
  productId: null,
  quantity: 1,
  remark: ''
})

const loadAddresses = async () => {
  loadingAddresses.value = true
  try {
    const res = await listAddresses(user.userId)
    if (res.code === 0) {
      addresses.value =
        res.addresses || res.data?.addresses || res.data || []
      const def = res.defaultAddressId ?? res.data?.defaultAddressId
      if (def) addressId.value = def
    }
  } catch (error) {
    ElMessage.error(error.message || '地址加载失败')
  } finally {
    loadingAddresses.value = false
  }
}

const fetchLeaderId = async (communityId) => {
  if (!communityId) return null
  const res = await getCommunity(communityId)
  return res.data?.community?.leaderId || null
}

const submit = async () => {
  if (!addressId.value) {
    ElMessage.error('请选择收货地址')
    return
  }
  if (!form.productId || !form.quantity) {
    ElMessage.error('请填写商品 ID 和数量')
    return
  }

  submitting.value = true
  try {
    const address = addresses.value.find((a) => a.id === addressId.value)
    const leaderId = await fetchLeaderId(address?.communityId)
    if (!leaderId) {
      ElMessage.error('该地址暂未绑定团长，请更换地址')
      submitting.value = false
      return
    }

    const createRes = await createOrder({
      userId: user.userId,
      leaderId,
      productId: form.productId,
      quantity: form.quantity,
      addressId: addressId.value,
      remark: form.remark
    })

    if (createRes.code !== 0) {
      ElMessage.error(createRes.msg || '下单失败')
      submitting.value = false
      return
    }

    const orderId = createRes.data?.orderId
    const payRes = await payOrder(orderId)
    if (payRes.code === 0) {
      ElMessage.success('支付成功，前往订单中心查看进度')
      router.push('/u/orders')
    } else {
      ElMessage.error(payRes.msg || '支付失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '支付异常')
  } finally {
    submitting.value = false
  }
}

onMounted(loadAddresses)
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
}

.order-form {
  max-width: 420px;
}

.order-tip {
  margin-top: 16px;
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
}
</style>
