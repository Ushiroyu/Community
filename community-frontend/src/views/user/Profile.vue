<template>
  <div class="page-wrap profile-view">
    <section class="page-card profile-card" aria-labelledby="profile-info-title" role="region">
      <header class="page-section-title">
        <h1 id="profile-info-title">基础信息</h1>
        <el-tag type="info" effect="plain">用户 ID：{{ user.userId }}</el-tag>
      </header>
      <p id="profile-info-desc" class="sr-only">
        包含账号身份信息与更新时间，仅平台内部可见。
      </p>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">
          {{ user.username || '未设置' }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ roleLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ user.phone || '暂无' }}
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">
          {{ user.createdAt || '暂无' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <el-form :model="form" label-width="100px" class="profile-form">
        <p id="profile-edit-tip" class="sr-only">
          修改昵称后将同步到导航栏与订单信息中。
        </p>
        <el-form-item label="昵称">
          <el-input
            v-model="form.nickname"
            placeholder="更新昵称后将同步账号显示名称"
            aria-describedby="profile-edit-tip"
          />
        </el-form-item>
        <el-form-item label="绑定手机号">
          <el-input
            v-model="form.phone"
            placeholder="用于找回密码，需与账号绑定一致"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">
            保存信息
          </el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="page-card leader-card" aria-labelledby="profile-role-title" role="region" aria-describedby="profile-role-desc">
      <header class="page-section-title">
        <h2 id="profile-role-title">角色拓展</h2>
        <el-tag type="success" effect="plain" v-if="user.isLeader">已是团长</el-tag>
      </header>
      <p class="muted">
        团长可管理社区成员订单、协助商品分发与售后处理，促进社区高效运作。
      </p>
      <p id="profile-role-desc" class="sr-only">
        申请成为团长后将进入平台审核，审核通过可管理所在社区订单。
      </p>
      <el-button
        type="success"
        @click="openLeaderDialog"
        :disabled="user.isLeader"
      >
        {{ user.isLeader ? '已成为团长' : '申请成为团长' }}
      </el-button>
    </section>

    <el-dialog
      v-model="leaderDialog.visible"
      title="团长申请"
      width="460px"
    >
      <el-form :model="leaderDialog.form" label-width="90px">
        <el-form-item label="社区名称">
          <el-input v-model="leaderDialog.form.communityName" />
        </el-form-item>
        <el-form-item label="社区地址">
          <el-input v-model="leaderDialog.form.communityAddress" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="leaderDialog.form.remark"
            type="textarea"
            rows="2"
            placeholder="说明社区规模、配送需求等信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="leaderDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="leaderDialog.loading"
          @click="submitLeader"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateUserById } from '@/api/users'
import { applyLeader as applyLeaderApi } from '@/api/leader'

const user = useUserStore()
const form = reactive({ nickname: user.username || '', phone: user.phone || '' })
const saving = ref(false)

const roleLabel = computed(() => {
  if (user.isAdmin) return '管理员'
  if (user.isSupplier) return '供应商'
  if (user.isLeader) return '社区团长'
  return '社区居民'
})

const leaderDialog = reactive({
  visible: false,
  loading: false,
  form: {
    communityName: '',
    communityAddress: '',
    remark: ''
  }
})

const save = async () => {
  if (!form.nickname) {
    ElMessage.error('请填写昵称')
    return
  }
  const phoneDigits = form.phone ? form.phone.replace(/\\D/g, '') : ''
  if (form.phone && (phoneDigits.length < 6 || phoneDigits.length > 20)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    const payload = { nickname: form.nickname, phone: phoneDigits || null }
    const res = await updateUserById(user.userId, payload)
    if (res.code === 0) {
      ElMessage.success('信息已更新')
      user.username = form.nickname
      user.phone = payload.phone
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    saving.value = false
  }
}

const openLeaderDialog = () => {
  leaderDialog.visible = true
}

const submitLeader = async () => {
  if (!leaderDialog.form.communityName || !leaderDialog.form.communityAddress) {
    ElMessage.error('请完善社区名称与地址')
    return
  }
  leaderDialog.loading = true
  try {
    const res = await applyLeaderApi(
      user.userId,
      leaderDialog.form.communityName,
      leaderDialog.form.communityAddress
    )
    if (res.code === 0) {
      ElMessage.success('申请已提交，请等待平台审核')
      leaderDialog.visible = false
    } else {
      ElMessage.error(res.msg || '申请失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '申请失败')
  } finally {
    leaderDialog.loading = false
  }
}
</script>

<style scoped>
.profile-view {
  gap: 24px;
}

.profile-card {
  display: grid;
  gap: 18px;
}

.profile-form {
  max-width: 420px;
}

.leader-card {
  display: grid;
  gap: 14px;
  align-items: start;
}

.el-descriptions :deep(.el-descriptions__cell) {
  padding: 16px;
}
</style>
