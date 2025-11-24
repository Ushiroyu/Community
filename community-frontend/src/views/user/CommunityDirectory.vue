<template>
  <div class="page-wrap community-view">
    <section class="page-card" aria-labelledby="community-list-title">
      <header class="page-section-title">
        <h1 id="community-list-title">社区目录</h1>
        <p class="muted">查找社区编号与团长信息，方便填写收货地址。</p>
      </header>

      <el-skeleton v-if="loading" animated :rows="4" />
      <div v-else>
        <el-table :data="communities" border>
          <el-table-column prop="id" label="社区 ID" width="120" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
          <el-table-column prop="leaderUserId" label="团长用户ID" width="140">
            <template #default="{ row }">
              {{ row.leaderUserId || '暂无' }}
            </template>
          </el-table-column>
          <el-table-column prop="leaderId" label="团长记录ID" width="140">
            <template #default="{ row }">
              {{ row.leaderId || '暂无' }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!communities.length" description="暂无社区数据" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listCommunities } from '@/api/leader'

const communities = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await listCommunities()
    if (res.code === 0) {
      communities.value = res.communities || res.data?.communities || res.data || []
    } else {
      ElMessage.error(res.msg || '加载社区失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.community-view {
  gap: 24px;
}

.muted {
  margin: 0;
  color: var(--text-2);
}
</style>
