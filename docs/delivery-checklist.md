# 交付执行记录

> 依据「四步提交流程」整理，以下为已完成步骤，其余待收到明确要求后继续补充。

## 1) GitHub（填写具体仓库链接）
- **整体仓库**：<https://github.com/Ushiroyu/Community>
- **后端 `community-backend/`**：<https://github.com/Ushiroyu/Community/tree/main/community-backend>
- **前端 `community-frontend/`**：<https://github.com/Ushiroyu/Community/tree/main/community-frontend>

> 说明：按照「每个项目的具体仓库链接（不要放个人主页）」的指引，分别列出总仓库以及前后端子目录的可直接访问链接，便于审核方一键查看代码。

---

## 2) API 文档
- **静态规范**：`docs/api/openapi.yaml`（OpenAPI 3.0.3），覆盖登录、商品、购物车、下单、统计等关键接口，可直接导入 Swagger Editor / Redoc / Postman。
- **运行时预览**：启动网关后访问 <http://localhost:8080/swagger-ui/index.html>，即可在一个页面切换 user/product/order/leader/supplier/admin 六个微服务的 OpenAPI 文档。
- **Postman 集合**：根目录 `postman_collection.json`（同步自 `community-backend/docs/postman/community-backend.postman_collection.json`）以及环境 `community-backend/docs/postman/community-backend.local.postman_environment.json`，导入后填入 `auth_token` 即可调用受保护接口。
- **静态托管站点**：`docs/api/swagger.html` 使用 CDN Swagger UI 渲染 `openapi.yaml`，将 GitHub Pages 指向 `/docs` 后，可在 `https://<your-account>.github.io/Community/api/swagger.html` 对外展示。若首次配置 Pages，可参考 [GitHub Docs: Pages Quickstart](https://docs.github.com/en/pages/quickstart)。

> 说明：静态文件方便快速审阅结构；Swagger UI / Postman 集合提供“可运行”验证入口；GitHub Pages 版本则满足公开站点需求。

---

## 3) 演示（Demo）
1. **依赖就绪**：在仓库根目录执行 `docker-compose up -d`（MySQL/Redis/RabbitMQ/Adminer 一键拉起）。
2. **数据库初始化**：`mysql -u<user> -p<pass> < init.sql`（或分别运行 `community-backend/database/schema.sql` + `data.sql`）导入示例数据。
3. **启动后端**：进入 `community-backend`，执行 `./mvnw spring-boot:run -pl gateway-service -am`，自动将网关与依赖模块拉起。
4. **启动前端**：进入 `community-frontend`，执行 `npm install && npm run dev`，默认访问 `http://localhost:5173` 并代理到 `http://localhost:8080`。
5. **登录体验**：使用演示账号验证完整链路，例如管理员 `admin/password`、普通用户 `user001/password`、供应商 `supplier01/password`，即可在前端、Swagger UI、Postman 中走完注册/下单/审批等流程。

---

## 4) Postman 集合
- **集合文件**：仓库根目录 `postman_collection.json`（与 `community-backend/docs/postman/community-backend.postman_collection.json` 同步），涵盖注册/登录、商品检索、购物车/下单、团长/供应商/统计等主要链路。
- **环境文件**：`community-backend/docs/postman/community-backend.local.postman_environment.json`，导入后设置 `gateway_base_url`（本地或 Koyeb/Render 域名）与 `auth_token` 即可访问受保护接口。
- **使用步骤**：1) 在 Postman 导入集合 + 环境；2) 执行 “Users & Auth / Login” 获取 JWT，并写入环境变量 `auth_token`；3) 按业务流程调用其余请求即可完成 Demo，验证范围可控制在只读接口及少量 GET。

---

_如需公开在线 Demo 地址，可在 Step 3 的“健康检查”处补充。_
