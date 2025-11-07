# 增强包（网关/注册中心 + 地址经纬�?+ 商品搜索�?
## 使用方法（最小步骤）

1) 执行 SQL�?    - `sql/patch_order_address_id.sql`（给 order 表补�?address_id�?    - `sql/product_fulltext_patch.sql`（给 product 表加 description + FULLTEXT�?2) 在各微服务的 pom.xml 增加�?   ```xml
   <dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-consul-discovery</artifactId>
   </dependency>
   ```
   并把 `discovery-yml/<service>-consul.yml` 内容合并到各�?`application.yml`�?3) 构建并运�?`gateway-service/`（端�?8080）�?4) 前端 `.env`：`VITE_API_BASE=http://localhost:8080/api`�?5) 地址经纬度：�?user-service 加入 `user-service-addons` 下代码；部署时设�?`AMAP_KEY` 环境变量�?
## 搜索接口

- `GET /api/products/search?q=关键�?categoryId=&page=1&size=20`

## 地理接口

- `GET /api/geo/forward?address=...&city=...` �?`{lat,lng}`
- `GET /api/geo/reverse?lat=..&lng=..` �?`{address}`
