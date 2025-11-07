const fallbackBase = 'https://support.community-groupbuy.com'

const getEnv = (key, fallback) => {
  const value = import.meta.env[key]
  return value && value.trim().length ? value : fallback
}

export const quickLinks = [
  { label: '商品广场', to: '/shop' },
  { label: '订单中心', to: '/u/orders' },
  { label: '团长工作台', to: '/leader/dashboard' },
  { label: '供应商看板', to: '/supplier/dashboard' }
]

export const supportLinks = [
  {
    label: '常见问题',
    href: getEnv('VITE_SUPPORT_FAQ_URL', `${fallbackBase}/faq`),
    external: true
  },
  {
    label: '配送说明',
    href: getEnv('VITE_SUPPORT_SHIPPING_URL', `${fallbackBase}/shipping`),
    external: true
  },
  {
    label: '售后政策',
    href: getEnv('VITE_SUPPORT_REFUND_URL', `${fallbackBase}/refund`),
    external: true
  }
]

export const contactLink = {
  label: getEnv('VITE_SUPPORT_CONTACT_LABEL', '联系客服邮箱'),
  href: getEnv('VITE_SUPPORT_CONTACT_URL', 'mailto:support@community-groupbuy.com')
}

export const serviceHours = getEnv('VITE_SUPPORT_SERVICE_HOURS', '周一至周日 · 9:00 - 22:00')
