# 构建修复说明（P02 build-fix）

- common-service 增加 AOP 与 Redis 依赖（解决 AspectJ/Redis 编译错误）。
- 修复 TraceIdFilter 中 lambda 捕获的 `traceId` 需要 final 的问题。
- order-service 启用 @EnableScheduling，确保 Outbox Relay 定时任务运行。

- 修复 `order-service` 启动类：`@EnableScheduling` 无参数；`scanBasePackages` 放在 `@SpringBootApplication` 上。
- 完整重写 `TraceIdFilter`，保证 `traceId` 为 `final` 且通过 `mutate()` 安全透传。 
