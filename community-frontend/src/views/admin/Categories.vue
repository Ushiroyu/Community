<template>
  <div class="page-wrap admin-categories">
    <section class="page-card category-form" aria-labelledby="admin-categories-form-title" aria-describedby="admin-categories-form-tip">
      <div class="page-section-title">
        <h1 id="admin-categories-form-title">新增分类</h1>
      </div>
      <p id="admin-categories-form-tip" class="sr-only">
        填写名称及可选的父分类 ID 后点击新增，系统会在列表中展示最新分类。
      </p>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="form-grid"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入分类名称"
            aria-label="分类名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="父分类 ID（可选）" prop="parentId">
          <el-input
            v-model="form.parentId"
            placeholder="仅数字，例如 1001"
            aria-label="父分类 ID"
            clearable
          />
        </el-form-item>
        <el-form-item class="form-actions">
          <el-button type="primary" @click="handleCreate" :loading="creating">
            新增
          </el-button>
          <el-button @click="resetForm" :disabled="creating">
            重置
          </el-button>
          <el-button @click="load" :loading="loading">
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-card" role="region" aria-labelledby="admin-categories-table-title">
      <header class="table-card__header table-header">
        <h2 id="admin-categories-table-title" class="page-section-title">分类列表</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="admin-categories-table-title">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column label="名称" min-width="220">
            <template #default="{ row }">
              <el-input v-model="row._name" size="small" placeholder="分类名称" />
            </template>
          </el-table-column>
          <el-table-column prop="parentId" label="父ID" width="100" />
          <el-table-column label="操作" width="220">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="rename(row)" :loading="row._saving">
                重命名
              </el-button>
              <el-button size="small" type="danger" @click="remove(row)" :loading="row._removing">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无分类数据" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listCategories, createCategory, updateCategory, deleteCategory } from '@/api/categories'

const list = ref([])
const loading = ref(false)
const creating = ref(false)
const formRef = ref()
const form = reactive({ name: '', parentId: '' })
const rules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { validator: (_rule, value, callback) => {
        if (!value) return callback()
        const trimmed = value.trim()
        if (!trimmed) return callback(new Error('请输入分类名称'))
        if (trimmed.length > 30) return callback(new Error('分类名称不超过 30 个字符'))
        return callback()
      }, trigger: 'blur'
    }
  ],
  parentId: [
    {
      validator: (_rule, value, callback) => {
        if (!value) return callback()
        const trimmed = value.trim()
        if (!trimmed) return callback()
        if (!/^\d+$/.test(trimmed)) return callback(new Error('父分类 ID 必须是数字'))
        return callback()
      },
      trigger: 'blur'
    }
  ]
}

const load = async () => {
  loading.value = true
  try {
    const res = await listCategories()
    if (res.code === 0) {
      const records = res.categories || res.data?.categories || []
      list.value = records.map((item) => ({ ...item, _name: item.name, _saving: false, _removing: false }))
    } else {
      ElMessage.error(res.msg || '加载分类失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.name = ''
  form.parentId = ''
  formRef.value?.clearValidate()
}

const handleCreate = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    const payload = {
      name: form.name.trim(),
      parentId: form.parentId ? Number(form.parentId.trim()) : null
    }
    if (payload.parentId !== null && Number.isNaN(payload.parentId)) {
      ElMessage.error('父分类 ID 必须是数字')
      return
    }
    creating.value = true
    try {
      const res = await createCategory(payload)
      if (res.code === 0) {
        ElMessage.success('分类已新增')
        resetForm()
        await load()
      } else {
        ElMessage.error(res.msg || '新增失败')
      }
    } catch (error) {
      ElMessage.error(error.message || '新增失败')
    } finally {
      creating.value = false
    }
  })
}

const rename = async (row) => {
  const nextName = (row._name || '').trim()
  if (!nextName) {
    ElMessage.error('请输入新名称')
    return
  }
  if (nextName === row.name) {
    ElMessage.info('名称未变化')
    return
  }
  row._saving = true
  try {
    const res = await updateCategory(row.id, { name: nextName })
    if (res.code === 0) {
      ElMessage.success('分类已更新')
      await load()
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    row._saving = false
  }
}

const remove = async (row) => {
  row._removing = true
  try {
    const res = await deleteCategory(row.id)
    if (res.code === 0) {
      ElMessage.success('分类已删除')
      await load()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  } finally {
    row._removing = false
  }
}

onMounted(load)
</script>

<style scoped>
.admin-categories {
  gap: 24px;
}

.category-form {
  display: grid;
  gap: 16px;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 380px;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 6px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
