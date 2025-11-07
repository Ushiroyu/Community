<template>
  <div class="page-wrap leader-approvals">
    <section class="page-card approvals-hero" aria-labelledby="leader-approvals-title" aria-describedby="leader-approvals-desc">
      <div>
        <span class="pill">团长审核</span>
        <h1 id="leader-approvals-title" class="hero-title">审核社区团长入驻</h1>
        <p id="leader-approvals-desc" class="hero-desc">
          查看待审批的团长申请，核对社区信息后快速通过，保障团购服务覆盖率。
        </p>
        <p class="sr-only">
          当前共有 {{ list.length }} 条待审核记录，可逐条通过申请。
        </p>
      </div>
      <el-button type="primary" @click="load" :loading="loading">
        刷新列表
      </el-button>
    </section>

    <section class="table-card" role="region" aria-labelledby="leader-approvals-table-title">
      <header class="table-card__header table-header">
        <h2 id="leader-approvals-table-title" class="page-section-title">待审核列表</h2>
      </header>
      <div class="table-card__body">
        <el-table :data="list" v-loading="loading" border aria-describedby="leader-approvals-table-title">
          <el-table-column prop="id" label="申请ID" width="120" />
          <el-table-column prop="userId" label="用户ID" width="120" />
          <el-table-column prop="communityId" label="社区ID" width="120" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag type="warning">{{ row.status || 'PENDING' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="approve(row)" :loading="row._loading">
                通过
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !list.length" description="暂无待审核申请" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pendingLeaders } from '@/api/leader'
import { approveLeader } from '@/api/admin'

const list = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await pendingLeaders()
    if (res.code === 0) {
      const items = res.pendingLeaders || res.data?.pendingLeaders || []
      list.value = items.map((item) => ({ ...item, _loading: false }))
    } else {
      ElMessage.error(res.msg || '加载失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const approve = async (row) => {
  row._loading = true
  try {
    const res = await approveLeader(row.id, row.userId)
    if (res.code === 0) {
      ElMessage.success('已通过审核')
      await load()
    } else {
      ElMessage.error(res.msg || '审核失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '审核失败')
  } finally {
    row._loading = false
  }
}

onMounted(load)
</script>

<style scoped>
.leader-approvals {
  gap: 24px;
}

.approvals-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  margin: 8px 0 10px;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-1);
}

.hero-desc {
  margin: 0;
  color: var(--text-2);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
