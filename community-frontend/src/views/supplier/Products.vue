<template>
  <div class="page-wrap supplier-products">
    <section class="page-card products-hero" aria-labelledby="supplier-products-title" aria-describedby="supplier-products-desc">
      <div>
        <span class="pill">商品管理</span>
        <h1 id="supplier-products-title" class="hero-title">维护供应的商品信息</h1>
        <p id="supplier-products-desc" class="hero-desc">
          发布新品、调整库存与价格，关注审核状态，确保商品及时上架到社区团购平台。
        </p>
        <p class="sr-only">
          当前共有 {{ list.length }} 个商品，可在下方填写表单新增，并在列表中执行上下架操作。
        </p>
      </div>
      <el-button type="primary" @click="load" :loading="loading">刷新数据</el-button>
    </section>

    <section class="page-card product-form" role="region" aria-labelledby="supplier-product-form-title">
      <header class="page-section-title">
        <h2 id="supplier-product-form-title">发布新商品</h2>
      </header>
      <p id="supplier-product-form-tip" class="sr-only">
        必填项包括商品名称、价格和库存，填写完成后点击发布商品提交审核。
      </p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="form-grid"
        aria-describedby="supplier-product-form-tip"
      >
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="如：有机蔬菜礼盒" clearable />
        </el-form-item>
        <el-form-item label="分类 ID（可选）" prop="categoryId">
          <el-input v-model="form.categoryId" placeholder="仅限数字" clearable />
        </el-form-item>
        <el-form-item label="价格 (元)" prop="price">
          <el-input v-model="form.price" placeholder="0.00" clearable />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input v-model="form.stock" placeholder="库存数量" clearable />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            rows="3"
            maxlength="200"
            show-word-limit
            placeholder="简要介绍商品亮点"
          />
        </el-form-item>
        <el-form-item class="form-actions">
          <el-button type="primary" :loading="submitting" @click="handleCreate">
            发布商品
          </el-button>
          <el-button @click="resetForm" :disabled="submitting">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card" role="region" aria-labelledby="supplier-product-table-title">
      <header class="table-card__header table-header">
        <h2 id="supplier-product-table-title" class="page-section-title">商品列表</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="supplier-product-table-title">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="名称" min-width="200" />
          <el-table-column label="价格" width="120">
            <template #default="{ row }">¥ {{ format(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column label="上架状态" width="140">
            <template #default="{ row }">
              <el-tag :type="row.status ? 'success' : 'info'">
                {{ row.status ? '已上架' : '已下架' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审核状态" width="140">
            <template #default="{ row }">
              <el-tag :type="row.approved ? 'success' : 'warning'">
                {{ row.approved ? '审核通过' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="toggle(row)"
              >
                {{ row.status ? '下架' : '上架' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无商品，请先发布" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { listSupplierProducts, createSupplierProduct, setSupplierProductStatus } from '@/api/supplier'

const user = useUserStore()
const list = ref([])
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  name: '',
  categoryId: '',
  price: '',
  stock: '',
  description: ''
})

const rules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value && value.trim().length > 40) return callback(new Error('商品名称不超过 40 个字符'))
        return callback()
      },
      trigger: 'blur'
    }
  ],
  categoryId: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback()
        const trimmed = value.trim()
        if (!trimmed) return callback()
        if (!/^\d+$/.test(trimmed)) return callback(new Error('分类 ID 仅支持数字'))
        return callback()
      },
      trigger: 'blur'
    }
  ],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback(new Error('请输入价格'))
        const num = Number(value)
        if (Number.isNaN(num) || num < 0) return callback(new Error('价格需为非负数字'))
        return callback()
      },
      trigger: 'blur'
    }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback(new Error('请输入库存'))
        const num = Number(value)
        if (!Number.isInteger(num) || num < 0) return callback(new Error('库存需为非负整数'))
        return callback()
      },
      trigger: 'blur'
    }
  ],
  description: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback()
        if (value.trim().length > 200) return callback(new Error('描述不超过 200 个字符'))
        return callback()
      },
      trigger: 'blur'
    }
  ]
}

const format = (value) => {
  const num = Number(value) || 0
  return num.toFixed(2)
}

const resetForm = () => {
  form.name = ''
  form.categoryId = ''
  form.price = ''
  form.stock = ''
  form.description = ''
  formRef.value?.clearValidate()
}

const load = async () => {
  loading.value = true
  try {
    const res = await listSupplierProducts(user.userId, { page: 1, size: 100 })
    if (res.code === 0) {
      list.value = res.records || res.data?.records || res.list || []
    } else {
      ElMessage.error(res.msg || '商品列表加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        name: form.name.trim(),
        categoryId: form.categoryId ? Number(form.categoryId.trim()) : null,
        price: Number(form.price),
        stock: Number(form.stock),
        description: form.description?.trim() || ''
      }
      const res = await createSupplierProduct(user.userId, payload)
      if (res.code === 0) {
        ElMessage.success('商品已提交审核')
        resetForm()
        await load()
      } else {
        ElMessage.error(res.msg || '发布失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '发布异常')
    } finally {
      submitting.value = false
    }
  })
}

const toggle = async (row) => {
  try {
    const res = await setSupplierProductStatus(user.userId, row.id, !row.status)
    if (res.code === 0) {
      ElMessage.success('商品状态已更新')
      await load()
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  }
}

onMounted(load)
</script>

<style scoped>
.supplier-products {
  gap: 24px;
}

.products-hero {
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

.product-form {
  display: grid;
  gap: 16px;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 420px;
}

.form-actions {
  display: flex;
  gap: 12px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
