<template>
  <div class="auth-scene">
    <section class="auth-card" aria-labelledby="login-welcome">
      <div class="auth-card__side">
        <div class="brand-mark" aria-hidden="true">🍃</div>
        <h2 id="login-welcome">欢迎回到社区团购</h2>
        <p>
          一站式团购管理平台，连接居民、团长、供应商，让每一次下单都透明可控。
        </p>
        <ul>
          <li>实时掌握社区库存与配送状态</li>
          <li>支持团长管理、供应商履约</li>
          <li>以社区为单位的精准推荐</li>
        </ul>
      </div>

      <div class="auth-card__form">
        <div class="form-header">
          <h1 id="login-form-title">登录账户</h1>
          <p>使用平台账号快速登录，继续管理和下单。</p>
        </div>
        <p id="login-privacy" class="sr-only">
          登录信息仅用于社区团购账号验证，不会用于其他用途。
        </p>
        <el-form :model="form" label-position="top" aria-labelledby="login-form-title" aria-describedby="login-privacy" @keyup.enter.native="onSubmit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <div class="form-actions">
            <el-checkbox v-model="remember">记住我</el-checkbox>
            <el-button text type="primary" @click="goForgot">忘记密码？</el-button>
          </div>
          <el-button type="primary" size="large" :loading="loading" @click="onSubmit" class="submit-btn">
            登录
          </el-button>
          <div class="form-footer">
            还没有账号？
            <el-button link type="primary" @click="router.push('/register')">去注册</el-button>
          </div>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const remember = ref(true)

const goForgot = () => {
  router.push('/forgot')
}

const onSubmit = async () => {
  if (!form.username || !form.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    const token = res?.token
    const userId = res?.userId
    const role = res?.role
    if (res?.code === 0 && token && userId) {
      user.setLogin({ token, userId, role, username: form.username, remember: remember.value })
      const roleHome = role === 'ADMIN'
        ? '/admin/dashboard'
        : role === 'LEADER'
          ? '/leader/dashboard'
          : role === 'SUPPLIER'
            ? '/supplier/dashboard'
            : '/shop'
      const redirect = typeof route.query?.redirect === 'string' ? route.query.redirect : ''
      router.replace(redirect || roleHome)
    } else if (res?.code !== 0) {
      ElMessage.error(res?.msg || '登录失败')
    } else {
      ElMessage.error('登录失败：缺少凭证')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录异常')
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

.form-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.submit-btn {
  width: 100%;
  margin-bottom: 12px;
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
