# Community Groupbuy Frontend

本前端基于 Vite + Vue3 + Element Plus，提供社区团购的用户、团长、供应商与管理后台体验。  
以下内容聚焦新增的可定制项与无障碍约定。

## 支持中心链接配置

页脚的“服务支持”“联系客服”等跳转可通过环境变量覆盖默认链接，方便接入正式工单或知识库。  
在运行 `npm run dev` 或打包前，根据需要在 `.env` 或系统环境中设置：

| 变量名 | 描述 | 默认值 |
| --- | --- | --- |
| `VITE_SUPPORT_FAQ_URL` | 常见问题页面链接 | `https://support.community-groupbuy.com/faq` |
| `VITE_SUPPORT_SHIPPING_URL` | 配送说明链接 | `https://support.community-groupbuy.com/shipping` |
| `VITE_SUPPORT_REFUND_URL` | 售后政策链接 | `https://support.community-groupbuy.com/refund` |
| `VITE_SUPPORT_CONTACT_URL` | “联系客服”跳转链接，支持 `mailto:` | `mailto:support@community-groupbuy.com` |
| `VITE_SUPPORT_CONTACT_LABEL` | “联系客服”显示文案 | `联系客服邮箱` |
| `VITE_SUPPORT_SERVICE_HOURS` | 服务时间文案 | `周一至周日 · 9:00 - 22:00` |

若未配置，则使用上述默认值。修改后重新启动或打包即可生效。

## 无障碍约定

- 主布局提供 “跳到主要内容” 的 skip link，配合页面内的 `aria-labelledby`/`aria-describedby` 结构，引导键盘与读屏用户。
- 统计看板与表格区域均使用 `role="region"` 与隐藏的 `.sr-only` 文本增强语义，重要状态更新时可通过屏幕阅读器获知。
- 表单与筛选器包含额外的说明文本，确保不依赖视觉即可理解操作流程。

遵循上述约定扩展新页面时，可复用 `src/styles/global.css` 中定义的 `.sr-only` 类与现有结构。*** End Patch*** कैम
