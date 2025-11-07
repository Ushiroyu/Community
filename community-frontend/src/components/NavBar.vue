<template>
  <header class="nav-root">
    <div class="container nav-container">
      <div class="nav-left">
        <router-link class="brand" to="/shop" aria-label="返回首页">
          <span class="brand-icon" aria-hidden="true">🍃</span>
          <span class="brand-text">社区团购</span>
        </router-link>
        <nav class="nav-links">
          <router-link
            v-for="link in visibleLinks"
            :key="link.to"
            :to="link.to"
            class="nav-link"
            :class="{ active: isActive(link.to) }"
            role="link"
          >
            <el-icon v-if="link.icon" class="nav-link__icon">
              <component :is="link.icon" />
            </el-icon>
            <span>{{ link.label }}</span>
          </router-link>
        </nav>
      </div>
      <div class="nav-right">
        <div class="nav-actions">
          <el-button
            v-if="user.isLogin && user.isUser"
            class="nav-action-btn"
            text
            @click="router.push('/u/cart')"
            aria-label="打开购物车"
          >
            <el-icon><ShoppingCart /></el-icon>
            <span class="nav-action-label">购物车</span>
          </el-button>
          <el-button
            v-if="user.isLogin && user.isLeader"
            class="nav-action-btn"
            text
            @click="router.push('/leader/dashboard')"
            aria-label="前往团长中心"
          >
            <el-icon><Location /></el-icon>
            <span class="nav-action-label">团长中心</span>
          </el-button>
          <el-button
            v-if="user.isLogin && user.isSupplier"
            class="nav-action-btn"
            text
            @click="router.push('/supplier/dashboard')"
            aria-label="前往供应商中心"
          >
            <el-icon><Goods /></el-icon>
            <span class="nav-action-label">商家中心</span>
          </el-button>
          <el-button
            v-if="user.isLogin && user.isAdmin"
            class="nav-action-btn"
            text
            @click="router.push('/admin/dashboard')"
            aria-label="前往管理后台"
          >
            <el-icon><DataAnalysis /></el-icon>
            <span class="nav-action-label">管理后台</span>
          </el-button>
        </div>

        <el-dropdown v-if="user.isLogin" trigger="click" aria-label="用户菜单">
          <span class="user-meta" role="button" tabindex="0">
            <el-avatar
              size="small"
              class="user-avatar"
              :src="user.avatar"
              >
              {{ initials }}
            </el-avatar>
            <div class="user-text">
              <span class="user-name">{{ displayName }}</span>
              <span class="user-role">{{ roleLabel }}</span>
            </div>
            <el-icon class="user-caret"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="user.isUser" @click="router.push('/u/profile')">
                个人设置
              </el-dropdown-item>
              <el-dropdown-item v-if="user.isUser" @click="router.push('/u/orders')">
                我的订单
              </el-dropdown-item>
              <el-dropdown-item v-if="user.isLeader" @click="router.push('/leader/dashboard')">
                团长工作台
              </el-dropdown-item>
              <el-dropdown-item v-if="user.isSupplier" @click="router.push('/supplier/dashboard')">
                供应商控制台
              </el-dropdown-item>
              <el-dropdown-item v-if="user.isAdmin" @click="router.push('/admin/dashboard')">
                管理后台
              </el-dropdown-item>
              <el-dropdown-item divided @click="onLogout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div v-else class="auth-btns">
          <el-button class="nav-login" text @click="router.push('/login')" aria-label="登录">
            登录
          </el-button>
          <el-button class="nav-register" type="primary" @click="router.push('/register')" aria-label="注册账号">
            立即注册
          </el-button>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  ArrowDown,
  DataAnalysis,
  Goods,
  Location,
  ShoppingCart,
  Tickets,
  UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const user = useUserStore()

const links = [
  { label: '首页', to: '/shop', icon: Goods },
  { label: '我的订单', to: '/u/orders', icon: Tickets, require: (u) => u.isLogin && u.isUser },
  { label: '个人中心', to: '/u/profile', icon: UserFilled, require: (u) => u.isLogin && u.isUser }
]

const visibleLinks = computed(() =>
  links.filter((item) => {
    if (!item.require) return true
    return item.require(user)
  })
)

const isActive = (target) => {
  if (target === '/shop') return route.path.startsWith('/shop')
  return route.path === target || route.path.startsWith(`${target}/`)
}

const displayName = computed(() => user.username || `用户 #${user.userId}`)
const roleLabel = computed(() => {
  if (user.isAdmin) return '管理员'
  if (user.isSupplier) return '供应商'
  if (user.isLeader) return '团长'
  if (user.isUser) return '社区居民'
  return '访客'
})

const initials = computed(() => {
  const name = displayName.value
  if (!name) return 'U'
  return name.slice(0, 1).toUpperCase()
})

const onLogout = () => {
  user.logout()
  router.push('/login')
}
</script>

<style scoped>
.nav-root {
  position: sticky;
  top: 0;
  z-index: 30;
  backdrop-filter: saturate(140%) blur(12px);
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.72));
  border-bottom: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.08);
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 70px;
  gap: 24px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.6px;
  color: var(--text-1);
}

.brand-icon {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--brand-primary) 0%, var(--brand-accent) 100%);
  border-radius: 14px;
  font-size: 18px;
  box-shadow: 0 6px 16px rgba(91, 140, 255, 0.28);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 14px;
  color: var(--text-2);
  transition: all 0.22s ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba(91, 140, 255, 0.18), rgba(255, 122, 89, 0.14));
  opacity: 0;
  transition: opacity 0.22s ease;
  z-index: -1;
}

.nav-link:hover {
  color: var(--text-1);
}

.nav-link:hover::after {
  opacity: 1;
}

.nav-link.active {
  color: var(--brand-primary);
  font-weight: 600;
}

.nav-link.active::after {
  opacity: 1;
}

.nav-link__icon {
  font-size: 16px;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-2);
  font-size: 13px;
  border-radius: 999px;
  transition: all 0.2s ease;
}

.nav-action-btn:hover {
  background: rgba(91, 140, 255, 0.12);
  color: var(--brand-primary);
}

.nav-action-label {
  white-space: nowrap;
}

.user-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.user-meta:hover {
  background: rgba(148, 163, 184, 0.16);
}

.user-avatar {
  background: linear-gradient(135deg, rgba(91, 140, 255, 0.22), rgba(255, 122, 89, 0.18));
  color: var(--brand-primary);
  font-weight: 600;
}

.user-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
}

.user-role {
  font-size: 11px;
  color: var(--text-2);
}

.user-caret {
  font-size: 14px;
  color: var(--text-2);
}

.auth-btns {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-login {
  color: var(--text-2);
}

.nav-register {
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(91, 140, 255, 0.25);
}

@media (max-width: 960px) {
  .nav-container {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    padding: 12px 0;
  }
  .nav-left {
    justify-content: space-between;
  }
  .nav-right {
    justify-content: space-between;
  }
  .nav-actions {
    flex-wrap: wrap;
    gap: 4px;
  }
  .nav-link {
    padding: 8px 12px;
  }
}
</style>
