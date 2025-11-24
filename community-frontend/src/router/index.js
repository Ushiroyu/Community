import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

// 公共
const PublicLayout = () => import('@/layouts/PublicLayout.vue')
const Login = () => import('@/views/user/Login.vue')
const Register = () => import('@/views/user/Register.vue')
const Forgot = () => import('@/views/user/ForgotPassword.vue')
const Communities = () => import('@/views/user/CommunityDirectory.vue')
const Shop = () => import('@/views/user/Shop.vue')
const ProductDetail = () => import('@/views/user/ProductDetail.vue')

// 用户
const UserLayout = () => import('@/layouts/UserLayout.vue')
const Cart = () => import('@/views/user/Cart.vue')
const Checkout = () => import('@/views/user/Checkout.vue')
const MyOrders = () => import('@/views/user/MyOrders.vue')
const Profile = () => import('@/views/user/Profile.vue')
const Addresses = () => import('@/views/user/Addresses.vue')

// 团长
const LeaderLayout = () => import('@/layouts/LeaderLayout.vue')
const LeaderDashboard = () => import('@/views/leader/LeaderDashboard.vue')
const CommunityOrders = () => import('@/views/leader/CommunityOrders.vue')
const LeaderStats = () => import('@/views/leader/LeaderStats.vue')

// 供应商
const SupplierLayout = () => import('@/layouts/SupplierLayout.vue')
const SupplierDashboard = () => import('@/views/supplier/SupplierDashboard.vue')
const SupplierOrders = () => import('@/views/supplier/SupplierOrders.vue')
const SupplierStats = () => import('@/views/supplier/SupplierStats.vue')

// 管理
const AdminLayout = () => import('@/layouts/AdminLayout.vue')
const AdminDashboard = () => import('@/views/admin/AdminDashboard.vue')
const Users = () => import('@/views/admin/Users.vue')
const LeaderApprovals = () => import('@/views/admin/LeaderApprovals.vue')
const SupplierApprovals = () => import('@/views/admin/SupplierApprovals.vue')
const ProductApprovals = () => import('@/views/admin/ProductApprovals.vue')
const OrdersAdmin = () => import('@/views/admin/OrdersAdmin.vue')
const Permissions = () => import('@/views/admin/Permissions.vue')
const CategoriesAdmin = () => import('@/views/admin/Categories.vue')

const routes = [
  { path: '/', redirect: '/shop' },

  {
    path: '/',
    component: PublicLayout,
    children: [
      { path: 'shop', component: Shop },
      { path: 'products/:id', component: ProductDetail },
      { path: 'login', component: Login },
      { path: 'register', component: Register },
      { path: 'forgot', component: Forgot },
      { path: 'communities', component: Communities }
    ]
  },

  {
    path: '/u',
    component: UserLayout,
    meta: { roles: ['USER'] },
    children: [
      { path: 'cart', component: Cart, meta: { roles: ['USER'] } },
      { path: 'checkout', component: Checkout, meta: { roles: ['USER'] } },
      { path: 'orders', component: MyOrders, meta: { roles: ['USER'] } },
      { path: 'profile', component: Profile, meta: { roles: ['USER'] } },
      { path: 'addresses', component: Addresses, meta: { roles: ['USER'] } }
    ]
  },
  { path: '/cart', redirect: '/u/cart' },
  { path: '/checkout', redirect: '/u/checkout' },
  { path: '/orders', redirect: '/u/orders' },
  { path: '/profile', redirect: '/u/profile' },
  { path: '/addresses', redirect: '/u/addresses' },

  {
    path: '/leader',
    component: LeaderLayout,
    meta: { roles: ['LEADER'] },
    children: [
      { path: 'dashboard', component: LeaderDashboard, meta: { roles: ['LEADER'] } },
      { path: 'orders', component: CommunityOrders, meta: { roles: ['LEADER'] } },
      { path: 'stats', component: LeaderStats, meta: { roles: ['LEADER'] } }
    ]
  },

  {
    path: '/supplier',
    component: SupplierLayout,
    meta: { roles: ['SUPPLIER'] },
    children: [
      { path: 'dashboard', component: SupplierDashboard, meta: { roles: ['SUPPLIER'] } },
      { path: 'products', component: () => import('@/views/supplier/Products.vue'), meta: { roles: ['SUPPLIER'] } },
      { path: 'orders', component: SupplierOrders, meta: { roles: ['SUPPLIER'] } },
      { path: 'shipments', component: () => import('@/views/supplier/Shipments.vue'), meta: { roles: ['SUPPLIER'] } },
      { path: 'stats', component: SupplierStats, meta: { roles: ['SUPPLIER'] } }
    ]
  },

  {
    path: '/admin',
    component: AdminLayout,
    meta: { roles: ['ADMIN'] },
    children: [
      { path: 'dashboard', component: AdminDashboard, meta: { roles: ['ADMIN'] } },
      { path: 'users', component: Users, meta: { roles: ['ADMIN'] } },
      { path: 'leader-approvals', component: LeaderApprovals, meta: { roles: ['ADMIN'] } },
      { path: 'supplier-approvals', component: SupplierApprovals, meta: { roles: ['ADMIN'] } },
      { path: 'product-approvals', component: ProductApprovals, meta: { roles: ['ADMIN'] } },
      { path: 'orders', component: OrdersAdmin, meta: { roles: ['ADMIN'] } },
      { path: 'permissions', component: Permissions, meta: { roles: ['ADMIN'] } },
      { path: 'categories', component: CategoriesAdmin, meta: { roles: ['ADMIN'] } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/shop' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 统一基于 meta.roles 的登录 + 角色守卫
router.beforeEach((to, from, next) => {
  const store = useUserStore()
  const roles = to.meta?.roles
  if (!roles) return next()

  if (!store.isLogin) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  // 用户区允许其他已登录角色（如 LEADER/SUPPLIER/ADMIN）也访问购物相关页面
  const isUserPage = roles.includes('USER')
  if (roles.includes(store.role) || (isUserPage && ['LEADER', 'SUPPLIER', 'ADMIN'].includes(store.role))) {
    return next()
  }

  // 角色不匹配，回到公开页
  return next('/shop')
})

export default router
