<template>
  <div class="auth-scene">
    <section class="auth-card register-card" aria-labelledby="register-welcome">
      <div class="auth-card__side">
        <div class="brand-mark" aria-hidden="true">🌱</div>
        <h2 id="register-welcome">加入社区团购平台</h2>
        <p>轻松完成注册，即可体验智能团购、团长协作与供应链管理。</p>
        <ul>
          <li>精选商品每日更新</li>
          <li>团长实时响应配送</li>
          <li>供应商全链路追踪</li>
        </ul>
      </div>

      <div class="auth-card__form">
        <div class="form-header">
          <h1 id="register-form-title">创建新账号</h1>
          <p>填写基础信息后即可登录使用平台服务。</p>
        </div>
        <p id="register-privacy" class="sr-only">
          账号信息仅用于社区团购登录验证，请妥善保管密码。
        </p>
        <el-form :model="form" label-position="top" aria-labelledby="register-form-title" aria-describedby="register-privacy" @keyup.enter.native="onSubmit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="form.confirm" type="password" show-password placeholder="请再次确认密码" />
          </el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="onSubmit" class="submit-btn">
            注册
          </el-button>
          <div class="form-footer">
            已有账号？
            <el-button link type="primary" @click="router.push('/login')">返回登录</el-button>
          </div>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const form = reactive({ username: '', password: '', confirm: '' })
const loading = ref(false)

const onSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }
  if (form.password !== form.confirm) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await register({ username: form.username, password: form.password })
    if (res.code === 0 || res.success === true || res.status === 200) {
      ElMessage.success('注册成功，请登录')
      setTimeout(() => router.push('/login'), 500)
    } else {
      ElMessage.error(res.msg || '注册失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '注册异常')
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
  width: min(960px, 100%);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 40px 120px rgba(15, 23, 42, 0.22);
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.auth-card__side {
  padding: 48px;
  background: linear-gradient(180deg, rgba(76, 110, 245, 0.2), rgba(37, 99, 235, 0.1));
  display: grid;
  gap: 18px;
  color: var(--text-1);
}

.auth-card__side h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-1);
}

.auth-card__side p {
  margin: 0;
  color: var(--text-2);
}

.auth-card__side ul {
  margin: 0;
  padding-left: 20px;
  color: var(--text-2);
  display: grid;
  gap: 6px;
}

.brand-mark {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  font-size: 24px;
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-accent));
  color: #fff;
  box-shadow: 0 16px 40px rgba(76, 110, 245, 0.35);
}

.auth-card__form {
  padding: 48px;
  display: grid;
  gap: 24px;
}

.form-header h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}

.form-header p {
  margin: 6px 0 0;
  color: var(--text-2);
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

.form-footer {
  text-align: center;
  color: var(--text-2);
}

@media (max-width: 860px) {
  .auth-card {
    grid-template-columns: 1fr;
  }
  .auth-card__side {
    display: none;
  }
  .auth-card__form {
    padding: 32px;
  }
}
</style>
