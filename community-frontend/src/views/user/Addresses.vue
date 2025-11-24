<template>
  <div class="page-wrap addresses-view">
    <section
      class="page-card addresses-hero"
      aria-labelledby="addresses-hero-title"
      aria-describedby="addresses-hero-desc"
    >
      <div>
        <span class="pill">收货地址</span>
        <h1 id="addresses-hero-title" class="addresses-title">为每个社区成员匹配配送地址</h1>
        <p id="addresses-hero-desc" class="addresses-desc">
          管理常用地址可以显著提升下单效率，系统会自动关联团长覆盖的社区范围，支持设置默认地址并快速调整。
        </p>
      </div>
      <div class="stat-card" aria-live="polite">
        <span class="stat-card__title">已保存地址</span>
        <span class="stat-card__value">{{ list.length }}</span>
      </div>
      <p id="addresses-default-tip" class="sr-only">
        默认地址会在结算时优先使用，可随时更换或删除。
      </p>
    </section>

    <section
      class="page-card address-list"
      aria-labelledby="addresses-list-title"
      role="region"
      aria-describedby="addresses-default-tip"
    >
      <header class="page-section-title">
        <h2 id="addresses-list-title">地址列表</h2>
      </header>
      <el-skeleton v-if="loading" animated :rows="4" />
      <div v-else>
        <div v-if="list.length" class="address-grid">
          <article
            v-for="item in list"
            :key="item.id"
            class="address-card"
            :class="{ 'is-default': item.id === defaultId }"
          >
            <div class="address-header">
              <div class="address-id">ID：{{ item.id }}</div>
              <el-tag v-if="item.id === defaultId" type="success" effect="plain">
                默认地址
              </el-tag>
            </div>
            <div class="address-detail">{{ item.detail }}</div>
            <div class="address-sub">
              社区 ID：<strong>{{ item.communityId || '未关联' }}</strong>
            </div>
            <div class="address-actions">
              <el-button
                text
                type="primary"
                @click="setDefault(item)"
                :disabled="item.id === defaultId"
              >
                设为默认
              </el-button>
              <el-button text type="danger" @click="remove(item)">
                删除
              </el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无地址，添加第一个地址方便配送吧" />
      </div>
    </section>

    <section class="page-card address-form" aria-labelledby="addresses-form-title" role="region">
      <header class="page-section-title">
        <h2 id="addresses-form-title">新增地址</h2>
      </header>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="form-content"
      >
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="form.detail"
            placeholder="如：XX小区X栋X单元XX室"
            clearable
          />
        </el-form-item>
        <el-form-item label="社区 ID（可选）" prop="communityId">
          <el-input
            v-model="form.communityId"
            placeholder="方便匹配团长，建议填写社区 ID（纯数字）"
            clearable
          />
        </el-form-item>
        <el-form-item class="form-actions">
          <el-button type="primary" :loading="submitting" @click="handleAdd">
            添加地址
          </el-button>
          <el-button @click="resetForm" :disabled="submitting">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { listAddresses, addAddress, setDefaultAddress, deleteAddress } from '@/api/users'

const user = useUserStore()
const list = ref([])
const defaultId = ref(null)
const loading = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  detail: '',
  communityId: ''
})

const rules = {
  detail: [
    { required: true, message: '请填写详细地址', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback(new Error('请填写详细地址'))
        const trimmed = value.trim()
        if (!trimmed) return callback(new Error('请填写详细地址'))
        if (trimmed.length > 80) return callback(new Error('地址长度不超过 80 个字符'))
        return callback()
      },
      trigger: 'blur'
    }
  ],
  communityId: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback()
        const trimmed = value.trim()
        if (!trimmed) return callback()
        if (!/^\d+$/.test(trimmed)) return callback(new Error('社区 ID 仅支持数字'))
        return callback()
      },
      trigger: 'blur'
    }
  ]
}

const load = async () => {
  loading.value = true
  try {
    const res = await listAddresses(user.userId)
    if (res.code === 0) {
      list.value = res.addresses || res.data?.addresses || res.data || []
      defaultId.value = res.defaultAddressId ?? res.data?.defaultAddressId ?? null
    } else {
      ElMessage.error(res.msg || '加载地址失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        detail: form.detail.trim(),
        communityId: form.communityId ? Number(form.communityId.trim()) : null
      }
      if (payload.communityId !== null && Number.isNaN(payload.communityId)) {
        ElMessage.error('社区 ID 仅支持数字')
        submitting.value = false
        return
      }
      const res = await addAddress(user.userId, payload)
      if (res.code === 0) {
        ElMessage.success('地址已添加')
        resetForm()
        await load()
      } else {
        ElMessage.error(res.msg || '新增失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '新增失败')
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  form.detail = ''
  form.communityId = ''
  formRef.value?.clearValidate()
}

const setDefault = async (address) => {
  try {
    await setDefaultAddress(user.userId, address.id)
    ElMessage.success('默认地址已更新')
    defaultId.value = address.id
  } catch (error) {
    ElMessage.error(error.message || '设置失败')
  }
}

const remove = async (address) => {
  try {
    await ElMessageBox.confirm('确定删除该地址吗？', '提示', { type: 'warning' })
    await deleteAddress(user.userId, address.id)
    ElMessage.success('地址已删除')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(load)
</script>

<style scoped>
.addresses-view {
  gap: 24px;
}

.addresses-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 18px;
  align-items: center;
}

.addresses-title {
  margin: 8px 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.addresses-desc {
  margin: 0;
  font-size: 15px;
  color: var(--text-2);
}

.stat-card {
  padding: 16px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
  display: grid;
  gap: 6px;
  text-align: center;
}

.stat-card__title {
  font-size: 13px;
  color: var(--text-2);
}

.stat-card__value {
  font-size: 24px;
  font-weight: 700;
  color: var(--brand-primary);
}

.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.address-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.08);
  display: grid;
  gap: 10px;
}

.address-card.is-default {
  border-color: rgba(76, 110, 245, 0.5);
  box-shadow: 0 20px 42px rgba(76, 110, 245, 0.18);
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-2);
}

.address-detail {
  font-weight: 600;
  color: var(--text-1);
  line-height: 1.5;
}

.address-sub {
  font-size: 13px;
  color: var(--text-2);
}

.address-actions {
  display: flex;
  gap: 8px;
}

.form-content {
  max-width: 420px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

@media (max-width: 768px) {
  .addresses-hero {
    grid-template-columns: 1fr;
  }
}
</style>
