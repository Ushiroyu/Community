# community-frontend

Vue 3 + Vite + Pinia + Element Plus 构建的多角色前端，配合 `community-backend` 完成居民、团长、供应商与管理员的全链路体验。本文覆盖目录约定、核心页面、状态与 API 流程、环境变量以及可访问性规范。

---

## 目录速览
| 路径 | 说明 |
| --- | --- |
| `src/api/` | Axios 封装与各领域 API（`products.js`、`orders.js`、`admin.js` 等），统一在 `http.js` 中处理 Token、错误与回退逻辑。 |
| `src/router/` | 路由与角色守卫，基于 `meta.roles` 控制访问。 |
| `src/store/` | Pinia 仓库：`user.js` 管理登录态与 LocalStorage，同步路由守卫；`cart.js` 维护购物车内存状态。 |
| `src/views/` | 用户/团长/供应商/管理员四类子目录，逐页拆分业务场景。 |
| `src/components/` | 公共组件（`NavBar`、`SidebarMenu`、`AppFooter` 等）以及支持中心链接。 |
| `src/layouts/` | `PublicLayout`、`UserLayout`、`LeaderLayout`、`SupplierLayout`、`AdminLayout` 五套布局骨架。 |
| `src/styles/` | 全局样式、主题变量、`.sr-only` 辅助类、响应式工具。 |
| `dist/` | `npm run build` 后的产物，可直接由网关或静态服务器托管。 |

---

## 功能与页面
### 居民端（`src/views/user`）
- **Shop.vue**：商品广场，支持关键词、分类、分页与排序；展示库存、分类标签等信息。
- **ProductDetail.vue**：商品详情、下单入口、关联推荐。
- **Cart.vue / Checkout.vue**：基于 `/cart` 与 `/orders/create` API 的购物车 & 结算流程，附带库存/金额校验与地址选择。
- **MyOrders.vue**：与 `/orders/me` 对接的订单列表，支持支付、收货、查看物流。
- **Profile.vue / Addresses.vue**：个人信息与地址管理，地址默认值与社区联动来自后端 Redis。
- **Login.vue / Register.vue**：认证入口，成功后调用 `useUserStore.setLogin` 写入本地缓存。

### 团长端（`src/views/leader`）
- **LeaderDashboard.vue**：展示今日订单、履约进度、GMV 等指标，数据来自 `/orders/by-leader` 与 `/stats/leader-overview`。
- **CommunityOrders.vue**：团长视角的订单筛选、分页与操作入口。
- **LeaderStats.vue**：按时间范围查看经营统计，并可跳转到社区推荐模块。

### 供应商端（`src/views/supplier`）
- **SupplierDashboard.vue**：订单摘要、库存提醒、最新通知。
- **Products.vue / EditProduct.vue**：调用 `/suppliers/{id}/products` 管理自有商品，涵盖上/下架、库存、价格编辑。
- **SupplierOrders.vue / Shipments.vue**：结合 `/shipping/pending` 与 `/shipping/{orderId}/ship` 管理发货流程。
- **SupplierStats.vue**：对接 `/stats/supplier-overview`，以图表/卡片形式展示 GMV、订单阶段等指标。

### 管理后台（`src/views/admin`）
- **AdminDashboard.vue**：调用 `/admin/stats/overview`（若失败则退回 `/stats/overview`），展示 GMV、支付率、履约率等核心指标。
- **Users.vue / Permissions.vue**：用户列表与 RBAC 映射视图，便于快速调整角色/权限。
- **LeaderApprovals.vue / SupplierApprovals.vue / ProductApprovals.vue**：聚合审批工作台，调用 admin-service 的聚合接口完成状态流转。
- **OrdersAdmin.vue / Categories.vue**：全局订单查询、分类管理等日常运营功能。

### 公共体验
- **NavBar** 根据 Pinia 中的角色动态展示菜单，并提供“切换视角”入口。
- **SidebarMenu** 支持各角色的分组导航与高亮状态。
- **AppFooter** 通过环境变量配置支持中心链接（详见下文），并带有 “跳转主要内容” 的无障碍跳链。

---

## 技术栈与约定
- Vue 3 `<script setup>` + Vite 5，默认端口 5173。
- Element Plus 按需自动导入（`unplugin-auto-import`、`unplugin-vue-components`）。
- Axios 拦截器位于 `src/api/http.js`：
  - 请求阶段注入 `Authorization: Bearer <token>`；
  - 响应阶段统一校验 `code === 0`，失败时抛出错误；
  - 401/403 时清理本地缓存并跳转登录页（带 `redirect` 参数）。
- Vite 解析别名：`@` 指向 `src`，便于模块引用。
- 代码风格遵循组合式 API + `<style scoped>`，公共变量位于 `src/styles/theme.css`。

---

## 状态、路由与 API 流
- **Pinia 仓库**：
  - `useUserStore`：持久化 token/userId/role，提供 `isAdmin`/`isLeader` 等 getter，`setLogin` 会同步 LocalStorage。
  - `useCartStore`：维护购物车行项、总数/总金额计算，供 `Cart.vue` 等组件直接消费。
- **路由守卫**（`src/router/index.js`）：
  - 每条受限路由在 `meta.roles` 中声明角色数组；
  - 全局 `beforeEach` 校验登录态与角色，不匹配则重定向至 `/shop` 或 `/login`。
- **API 层**：`src/api` 目录中每个模块仅封装业务相关路径，例如：
  - `products.js`：`pageProducts`、`listCategories`、`getProduct`；
  - `orders.js`：`createOrder`、`payOrder`、`leaderOrders` 等；
  - `admin.js`：审批接口与 `overviewStats`（内部具备“失败后直连 order-service”兜底）。
- **HTTP Fallback**：例如 `overviewStats` 在 admin-service 不可用时自动改调 `/stats/overview`，保证运营看板可用性。

---

## 环境变量与可配置项
| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `VITE_GATEWAY` | `http://localhost:8080` (`.env.development`) | 前端直连网关/后端的基础地址。线上建议指向经过反代的统一域名。 |
| `VITE_API_PREFIX` | `/api` | 在开发环境通过 Vite 代理拆分到各微服务，生产可留空由网关处理。 |
| `VITE_SUPPORT_FAQ_URL` | `https://support.community-groupbuy.com/faq` | AppFooter “常见问题” 链接。 |
| `VITE_SUPPORT_SHIPPING_URL` | `https://support.community-groupbuy.com/shipping` | “配送说明” 链接。 |
| `VITE_SUPPORT_REFUND_URL` | `https://support.community-groupbuy.com/refund` | “售后政策” 链接。 |
| `VITE_SUPPORT_CONTACT_URL` | `mailto:support@community-groupbuy.com` | “联系客服” 跳转（支持 mailto / https）。 |
| `VITE_SUPPORT_CONTACT_LABEL` | `联系客服邮箱` | 联系方式文案。 |
| `VITE_SUPPORT_SERVICE_HOURS` | `周一至周日 · 9:00 - 22:00` | 服务时间展示。 |

- `.env.development` 默认指向本地网关；`.env.production` 仅给出示例，可依据部署环境设置。
- 若需直接联通各微服务，可保留 Vite 代理配置（`vite.config.js` 中的 `/api/...` 条目），代理优先级高于 `VITE_GATEWAY`。

---

## 开发 & 构建
```bash
npm install          # 安装依赖
npm run dev          # 启动 Vite，端口 5173，内置代理 → 后端微服务
npm run build        # 构建产物至 dist/
npm run preview      # 以本地静态服务器预览 dist，默认 4173 端口
```
- Dev 模式下可同时启用网关或单体微服务，Vite 会优先命中 `/api/...` 代理规则。
- 将 `dist/` 挂载至 Nginx 或 Spring Cloud Gateway 的静态目录即可上线。

---

## 无障碍与设计约定
- 主布局提供“跳到主要内容” skip link，辅以 `aria-labelledby/aria-describedby` 标记主要区域（见 `Shop.vue`、`AdminDashboard.vue`）。
- 表格/面板使用 `role="region"`、`.sr-only` 文案传达数据摘要，便于读屏器提示当前指标。
- 表单控件配套说明文本与键盘操作提示，`AppFooter` 等组件遵守颜色对比与焦点可见性。
- 扩展页面时，可复用 `src/styles/global.css` 中的 `.sr-only`、`.page-card` 等结构，保持一致的语义与动效。

---

## 验证建议
1. **以居民身份登录** → 浏览 `Shop`、加入购物车、下单，并在 `MyOrders` 查看状态联动。
2. **切换到团长**（在后台设置角色后重新登录）→ 访问 `/leader/dashboard`，确认指标、订单筛选正确。
3. **切换到供应商** → 通过 `SupplierDashboard` → `Products` 编辑商品，检查接口 `/suppliers/{id}/products`、`/products/{id}/approve` 的响应。
4. **切换到管理员** → `AdminDashboard`、`LeaderApprovals`、`ProductApprovals` 全流程审批；若 admin-service 未启用，确认回退到 `/stats/overview` 仍能展示数据。
5. 验证 AppFooter 支持中心链接是否根据环境变量生效，并检查 401/403 场景是否触发自动登出与跳转。

通过以上检查可以确保与后端的用户/商品/订单/供应商/管理六大模块互通，并验证可访问性与配置项是否按预期工作。
