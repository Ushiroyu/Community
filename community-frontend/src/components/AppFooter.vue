<template>
  <footer class="app-footer" role="contentinfo">
    <div class="container footer-grid">
      <div class="footer-brand" role="presentation">
        <div class="footer-logo" aria-hidden="true">🍃</div>
        <div>
          <p class="footer-title">社区团购</p>
          <p class="footer-subtitle">让每个社区都拥有自己的生活服务网络</p>
        </div>
      </div>
      <nav class="footer-section" aria-labelledby="footer-quicklinks">
        <p id="footer-quicklinks" class="footer-heading">快速入口</p>
        <component
          v-for="link in quickLinks"
          :is="link.to ? 'router-link' : 'a'"
          :key="link.label"
          class="footer-link"
          v-bind="link.to ? { to: link.to } : { href: link.href, target: link.external ? '_blank' : undefined, rel: link.external ? 'noopener noreferrer' : undefined }"
        >
          {{ link.label }}
        </component>
      </nav>
      <nav class="footer-section" aria-labelledby="footer-support">
        <p id="footer-support" class="footer-heading">服务支持</p>
        <a
          v-for="item in supportLinks"
          :key="item.label"
          class="footer-link"
          :href="item.href"
          :target="item.external ? '_blank' : undefined"
          :rel="item.external ? 'noopener noreferrer' : undefined"
        >
          {{ item.label }}
        </a>
      </nav>
      <section class="footer-section" aria-labelledby="footer-contact">
        <p id="footer-contact" class="footer-heading">团队动态</p>
        <p class="footer-text">
          团购热销榜、新品尝鲜、社区活动等资讯持续更新，欢迎关注。
        </p>
        <div class="footer-pill">{{ serviceHours }}</div>
        <a class="footer-link contact-link" :href="contact.href">{{ contact.label }}</a>
      </section>
    </div>
    <div class="footer-meta">
      <div class="container footer-meta__inner">
        <span>© {{ currentYear }} 社区团购平台</span>
        <span>打造可信赖的社区供应链</span>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
import {
  quickLinks,
  supportLinks,
  contactLink as contact,
  serviceHours
} from '@/config/supportLinks'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  margin-top: 48px;
  padding: 48px 0 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9) 0%, rgba(236, 244, 255, 0.95) 100%);
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.footer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 32px;
  padding-bottom: 36px;
}

.footer-brand {
  display: flex;
  gap: 14px;
  align-items: center;
}

.footer-logo {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--brand-primary), var(--brand-accent));
  color: #fff;
  font-size: 20px;
  box-shadow: 0 18px 34px rgba(76, 110, 245, 0.36);
}

.footer-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-1);
  margin: 0;
}

.footer-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-2);
}

.footer-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.footer-heading {
  font-weight: 600;
  color: var(--text-1);
  margin: 0 0 6px;
}

.footer-link {
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.4;
  transition: color 0.2s ease;
}

.footer-link:hover,
.footer-link:focus-visible {
  color: var(--brand-primary);
}

.contact-link {
  margin-top: 12px;
}

.footer-text {
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.5;
  margin: 0;
}

.footer-pill {
  margin-top: 8px;
  display: inline-flex;
  padding: 6px 12px;
  background: rgba(76, 110, 245, 0.12);
  color: var(--brand-primary);
  border-radius: 999px;
  font-size: 12px;
}

.footer-meta {
  border-top: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.8);
  padding: 16px 0;
  color: var(--text-2);
  font-size: 13px;
}

.footer-meta__inner {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .app-footer {
    padding-top: 36px;
  }
  .footer-grid {
    gap: 24px;
  }
}
</style>
