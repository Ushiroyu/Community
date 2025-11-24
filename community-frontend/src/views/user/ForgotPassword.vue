<template>
  <div class="auth-scene">
    <section class="auth-card single-column" aria-labelledby="forgot-title">
      <div class="auth-card__form">
        <div class="form-header">
          <h1 id="forgot-title">重置密码</h1>
          <p>输入用户名与绑定手机号，设置一个新密码。</p>
        </div>
        <el-form label-position="top" :model="form">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="绑定手机号">
            <el-input v-model="form.phone" placeholder="请输入绑定的手机号" clearable />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认新密码">
            <el-input v-model="form.confirm" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
          <div class="form-actions">
            <el-button @click="router.push('/login')" text>返回登录</el-button>
            <el-button type="primary" :loading="loading" @click="onSubmit">提交重置</el-button>
          </div>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { resetPassword } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirm: ''
})

const onSubmit = async () => {
  if (!form.username || !form.phone || !form.newPassword || !form.confirm) {
    ElMessage.error('请完整填写信息')
    return
  }
  const phoneDigits = form.phone.replace(/\D/g, '')
  if (phoneDigits.length < 6 || phoneDigits.length > 20) {
    ElMessage.error('手机号格式不正确')
    return
  }
  if (form.newPassword !== form.confirm) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await resetPassword({
      username: form.username.trim(),
      phone: phoneDigits,
      newPassword: form.newPassword
    })
    if (res?.code === 0) {
      ElMessage.success(res.msg || '密码已重置，请使用新密码登录')
      router.push('/login')
    } else {
      ElMessage.error(res?.msg || '重置失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-scene {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: var(--app-background);
}

.auth-card {
  width: min(520px, 100%);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 36px 100px rgba(15, 23, 42, 0.2);
}

.auth-card__form {
  padding: 40px;
  display: grid;
  gap: 18px;
}

.form-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.form-header p {
  margin: 6px 0 0;
  color: var(--text-2);
}

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

@media (max-width: 640px) {
  .auth-card__form {
    padding: 28px;
  }
}
</style>
