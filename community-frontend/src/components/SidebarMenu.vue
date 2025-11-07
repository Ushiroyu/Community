<template>
  <div class="menu-shell">
    <div class="menu-header">
      <span class="menu-label">操作中心</span>
      <span class="menu-role">{{ roleLabel }}</span>
    </div>
    <el-menu
      :default-active="active"
      class="dashboard-menu"
      router
      unique-opened
      :collapse="collapsed"
    >
      <template v-for="item in menus" :key="item.id">
        <el-sub-menu v-if="item.children?.length" :index="item.id">
          <template #title>
            <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </template>
          <el-menu-item
            v-for="child in item.children"
            :key="child.id"
            :index="child.to"
          >
            <el-icon v-if="child.icon"><component :is="child.icon" /></el-icon>
            <span>{{ child.label }}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else :index="item.to">
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  Avatar,
  CollectionTag,
  DataAnalysis,
  Finished,
  Goods,
  Histogram,
  House,
  List,
  Postcard,
  ShoppingCartFull,
  Tickets,
  TrendCharts,
  UserFilled,
  Van
} from '@element-plus/icons-vue'

const route = useRoute()
const user = useUserStore()
const collapsed = ref(false)

const handleResize = () => {
  collapsed.value = window.innerWidth <= 960
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

const active = computed(() => route.path)
const role = computed(() => user.role)

const roleLabel = computed(() => {
  switch (role.value) {
    case 'ADMIN':
      return '系统管理员'
    case 'SUPPLIER':
      return '供应商'
    case 'LEADER':
      return '社区团长'
    case 'USER':
      return '社区居民'
    default:
      return '访客'
  }
})

const menus = computed(() => {
  switch (role.value) {
    case 'ADMIN':
      return [
        { id: 'admin-dashboard', label: '运营总览', to: '/admin/dashboard', icon: DataAnalysis },
        {
          id: 'admin-approvals',
          label: '审核中心',
          icon: Finished,
          children: [
            { id: 'admin-leader-approvals', label: '团长审核', to: '/admin/leader-approvals', icon: Avatar },
            { id: 'admin-supplier-approvals', label: '供应商审核', to: '/admin/supplier-approvals', icon: Goods },
            { id: 'admin-product-approvals', label: '商品审核', to: '/admin/product-approvals', icon: CollectionTag }
          ]
        },
        { id: 'admin-orders', label: '订单管理', to: '/admin/orders', icon: Tickets },
        { id: 'admin-users', label: '用户管理', to: '/admin/users', icon: UserFilled },
        { id: 'admin-permissions', label: '权限配置', to: '/admin/permissions', icon: Postcard },
        { id: 'admin-categories', label: '分类管理', to: '/admin/categories', icon: List }
      ]
    case 'SUPPLIER':
      return [
        { id: 'supplier-dashboard', label: '经营概览', to: '/supplier/dashboard', icon: DataAnalysis },
        { id: 'supplier-products', label: '商品管理', to: '/supplier/products', icon: Goods },
        { id: 'supplier-orders', label: '订单处理', to: '/supplier/orders', icon: Tickets },
        { id: 'supplier-shipments', label: '发货管理', to: '/supplier/shipments', icon: Van },
        { id: 'supplier-stats', label: '数据分析', to: '/supplier/stats', icon: TrendCharts }
      ]
    case 'LEADER':
      return [
        { id: 'leader-dashboard', label: '工作台', to: '/leader/dashboard', icon: House },
        { id: 'leader-orders', label: '社区订单', to: '/leader/orders', icon: Tickets },
        { id: 'leader-stats', label: '经营数据', to: '/leader/stats', icon: Histogram }
      ]
    case 'USER':
      return [
        { id: 'user-shop', label: '社区逛逛', to: '/shop', icon: ShoppingCartFull },
        { id: 'user-cart', label: '我的购物车', to: '/u/cart', icon: Goods },
        { id: 'user-orders', label: '我的订单', to: '/u/orders', icon: Tickets },
        {
          id: 'user-profile',
          label: '个人中心',
          icon: UserFilled,
          children: [
            { id: 'user-profile-basic', label: '资料设置', to: '/u/profile' },
            { id: 'user-profile-address', label: '收货地址', to: '/u/addresses' }
          ]
        }
      ]
    default:
      return [
        { id: 'guest-shop', label: '浏览商品', to: '/shop', icon: ShoppingCartFull }
      ]
  }
})
</script>

<style scoped>
.menu-shell {
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 18px;
  box-shadow: var(--el-box-shadow-light);
  backdrop-filter: blur(10px);
  min-height: calc(100vh - 120px);
}

.menu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.menu-label {
  font-weight: 700;
  color: var(--text-1);
}

.menu-role {
  font-size: 12px;
  color: var(--text-2);
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(91, 140, 255, 0.12);
}

.dashboard-menu {
  border-right: none;
  background: transparent;
}

.dashboard-menu :deep(.el-menu-item),
.dashboard-menu :deep(.el-sub-menu__title) {
  border-radius: 12px;
  margin-bottom: 6px;
  height: 44px;
  line-height: 44px;
  color: var(--text-2);
  transition: all 0.2s ease;
  font-weight: 500;
}

.dashboard-menu :deep(.is-active) {
  background: linear-gradient(135deg, rgba(91, 140, 255, 0.18), rgba(255, 122, 89, 0.14));
  color: var(--brand-primary);
}

.dashboard-menu :deep(.el-menu-item:hover),
.dashboard-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(91, 140, 255, 0.12);
  color: var(--brand-primary);
}

.dashboard-menu :deep(.el-menu-item .el-icon) {
  margin-right: 8px;
}

@media (max-width: 960px) {
  .menu-shell {
    min-height: auto;
    padding: 12px;
  }
}
</style>
