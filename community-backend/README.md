# community-backend

Spring Boot + Spring Cloud 的社区团购后端，多模块涵盖用户、商品、订单、团长、供应商、管理后台与网关。下文汇总架构、模块要点、运行方式与技术难点，便于快速上手或二次开发。

---

## 模块与职责
| 模块 | 端口 | 关键职责 |
| --- | --- | --- |
| `common-service` | - | 公共配置与组件：`JwtAuthFilter`、`@RequiresPermissions` 切面、`ApiResponse`、`SimpleMetrics`、`TraceMdcFilter` 等。 |
| `user-service` | 8081 | 用户注册/登录、个人信息、地址簿（默认地址存 Redis）、RBAC（`/internal/rbac/has-perm`）、地理编码 (`/geo`)。 |
| `product-service` | 8082 | 分类、商品、全文搜索（MySQL FULLTEXT + 兜底模糊查）、库存管理（Redis + Lua 预占、同步/释放）、供应商自管商品。 |
| `order-service` | 8083 | 购物车（Redis 哈希）、下单、支付/确认、物流轨迹、统计、延迟关单、Outbox 事件、`OrderEventPublisher`。 |
| `leader-service` | 8084 | 团长申请、审批状态、社区档案、基于经纬度的附近社区推荐。 |
| `supplier-service` | 8085 | 供应商入驻、状态审批、自服务商品、对接订单服务发货。 |
| `admin-service` | 8086 | 管理审批（团长/供应商/商品）、聚合统计、事件消费日志、死信配置。 |
| `gateway-service` | 8080 | Spring Cloud Gateway 统一入口、CORS、TraceId 透传、路由规则。 |
| `discovery-yml/` | - | 可选 Consul 配置片段，若启用服务注册/发现将其并入各 `application.yml`。 |
| `docs/` | - | RabbitMQ 附加示例等辅助手稿。 |

---

## 外部依赖 & 环境变量
- **MySQL**：`community_schema_mysql.sql` 提供全量表结构与演示数据，适配 MyBatis-Plus。
- **Redis**：缓存默认地址、购物车、库存、轻量熔断器。
- **RabbitMQ**：延迟关单（TTL + DLX）、事件总线（`order.*` → admin-service）。
- **高德地图**：`AMAP_KEY` 环境变量供 `user-service` 进行正/逆地理编码。
- 常用环境变量（未设置则使用 `application.yml` 中的默认值）：

| 变量 | 作用 |
| --- | --- |
| `MYSQL_HOST/PORT/DB/USER/PASSWORD` | 各服务数据源 |
| `REDIS_HOST/PORT/DB` | Spring Data Redis |
| `RABBITMQ_HOST/PORT/USER/PASS` | RabbitMQ 连接 |
| `JWT_SECRET` | 自定义 JWT 密钥 |
| `DISCOVERY_ENABLED` | 控制 Spring Cloud Discovery（默认 false） |
| `PRODUCT_BASE_URL`、`ORDER_BASE_URL` 等 | 跨服务 RestClient 基址（见对应 `RestClientConfig`） |
| `AMAP_KEY` | 地理编码接口 |

---

## 构建与运行
1. **安装依赖**：JDK 21、Maven 3.9+、本地 MySQL/Redis/RabbitMQ（或使用仓库根目录的 `docker-compose.yml`）。
2. **导入数据库**：运行根目录 `community_schema_mysql.sql`，完成 DDL + 种子数据。
3. **构建**：在 `community-backend` 执行 `./mvnw clean package -DskipTests`。如需一次性启动全部模块，可在 IDE 中创建多模块 Run Configuration。
4. **运行单个服务**：`./mvnw spring-boot:run -pl product-service -am`。`-am` 会自动拉起对该模块有依赖的 `common-service`。
5. **调试入口**：每个服务暴露 `http://localhost:<port>/swagger-ui/index.html` 与 `http://localhost:<port>/v3/api-docs`；`common-service` 还提供 `/internal/metrics/basic` 的内存计数器快照。
6. **网关联调**：启动 `gateway-service`（8080）后，前端将 `VITE_GATEWAY` 指向 `http://localhost:8080`，所有 API 经网关转发并统一附带 `X-Trace-Id`。

## API 文档 & Postman
- **集中 Swagger UI**：网关已引入 `springdoc-openapi`，在本地启动 `gateway-service` 后访问 `http://localhost:8080/swagger-ui/index.html`，可在同一页面切换 `user-service`、`product-service`、`order-service`、`leader-service`、`supplier-service`、`admin-service` 的 OpenAPI 文档。
- **Postman 集合**：`docs/postman/community-backend.postman_collection.json` 覆盖核心链路（注册/登录、商品检索、购物车与下单、团长/供应商/统计接口）。配套环境变量文件位于 `docs/postman/community-backend.local.postman_environment.json`，导入后仅需填入 `auth_token` 即可连通受保护接口。

---

## 核心业务流程
### 1. 用户与权限
- `UserController`（`/users`）完成注册、登录、资料更新、角色设置、密码重置（管理员）。
- 地址模块将默认地址放在 `user:default_address:{userId}`，避免额外字段。
- `JwtAuthFilter` 解析 Authorization，注入 `userId` / `role` 到 `SecurityContext`。
- 细粒度权限通过 `@RequiresPermissions`，切面访问 `user-service` 的 `/internal/rbac/has-perm`（基于 `user_role` / `role_permission` 表）。

### 2. 商品与库存
- `ProductController` 提供分页、审批、库存调整、Redis 预占/释放/同步接口。
- 库存由 `InventoryRedisServiceImpl` 维护：Lua 脚本保障 `reserve` 原子性，`/release` 回补库存，`/sync-stock` 以数据库为准重建缓存。
- 搜索优先使用 `ProductMapper#searchFulltext`（FULLTEXT），失败回退到 `LIKE` 模糊查询。
- 供应商透过 `SupplierProductController` 在 `/suppliers/{supplierId}/products` 下管理自己的商品，上架默认待审核。

### 3. 购物到收货
1. `CartController` 使用 Redis Hash (`cart:{userId}`) 存储购物车行项，并通过 `RestClient` 拉取产品信息。
2. `OrderController#create`：
   - 查询商品 → `POST /products/{id}/reserve` 预占库存；
   - 写入 `order` / `order_item`；
   - 投递 TTL=15 分钟的延迟消息到 `RabbitConfig.ORDER_TIMEOUT_QUEUE`。
3. 支付成功后调用 `/orders/{id}/pay`，状态进入 `PAID` 并触发 `OrderEventPublisher#publishOrderPaid`（Routing Key `order.paid`）。
4. Supplier Service 订阅「待发货」列表 `/orders/pending?supplierId=...`，并通过 `/orders/{id}/ship` 填写快递信息，同步 `ShipmentEvent`。
5. `OrderTimeoutListener` 监听超时队列，若订单仍为 `CREATED` 则取消并调用 `/products/{id}/release` 回退库存。
6. 用户可在 `/orders/{id}/confirm` 完成收货，或在 `/orders/{id}/remark` / `/invoice` 补充备注与发票。

### 4. 团长 & 社区
- `LeaderController` 负责团长申请（`/leaders/apply`）、审批状态、管理员审核。
- `CommunityController` 提供列表、`/nearby`（基于 Haversine 算法选出最近的社区）、`/summary/{leaderId}` 示例数据。

### 5. 供应商协作
- `SupplierController` 处理入驻、查看自身资料（JWT subject → userId）、状态切换。
- `ShippingController` 通过注入的 `orderClient`（见 `RestClientConfig`）与 Order Service 交互，透传 Authorization/X-Request-Id，避免重复认证。

### 6. 管理后台 & 事件流
- `AdminController` 聚合审批：调用 Leader/User/Supplier/Product 服务并保持 Authorization 透传。
- `/admin/stats/overview` 代理 `order-service` 的 `/stats/overview`，方便在同域下访问。
- `OrderEventPublisher` + `OutboxRelay`：将订单事件落表 `outbox_event`，定时任务发布到 `event.bus.ex`。Admin Service 通过 `EventListener` 幂等消费，写入 `event_consume_log` 并支持 DLQ。

### 7. 地理能力
- `user-service` 中的 `AMapGeocodingProvider` 使用 `AMAP_KEY` 调用高德 API。
- `LeaderService` 基于社区的经纬度（`community.lat/lng`）返回附近社区，用于地址联动与推荐。

---

## 调试 & 运维提示
- **Swagger/OpenAPI**：所有微服务已启用 springdoc；若在生产禁用可设置 `SPRINGDOC_API_DOCS_ENABLED=false`。
- **TraceId**：Gateway 写入 `X-Trace-Id`，`TraceMdcFilter` 将其塞入日志 MDC，方便跨服务排障。
- **熔断与防护**：`RedisCircuitBreaker` 可在热点接口周围快速集成，避免下游雪崩。
- **度量**：`SimpleMetrics.inc` 在事件发布/消费等路径已落点，可通过 `/internal/metrics/basic` 查看。
- **注册中心**：`discovery-yml` 内包含针对 Consul 的 profile，若接入可将文件内容 merge 进相应 `application.yml` 并开启 `DISCOVERY_ENABLED=true`。

---

## 常见验证步骤
1. 启动 `user-service` → `POST /users/register` 创建居民账户，`POST /users/login` 获取 JWT。
2. 运行 `product-service` → 使用 SQL 脚本导入商品后，`GET /products`、`GET /products/search` 测试检索。
3. 运行 `order-service` + `product-service` → `POST /cart` → `/orders/create`，检查 Redis `stock:prod:*` 变化与 RabbitMQ 延迟队列。
4. 启动 `supplier-service` → `GET /shipping/pending/{supplierId}` 查看待处理订单，`POST /shipping/{orderId}/ship` 更新状态。
5. 启动 `admin-service` → `GET /admin/stats/overview?from=...&to=...`，确认能聚合订单统计并记录消费日志。

如在运行过程中发现异常（如 `AMAP_KEY` 缺失、跨服务 401、库存不同步等），请根据上述流程逐项排查，并查看各服务日志中的 `traceId` 与 `SimpleMetrics` 计数。
