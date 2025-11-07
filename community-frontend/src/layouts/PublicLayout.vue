<template>
  <div class="public-shell">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <NavBar />
    <main id="main-content" class="public-body" tabindex="-1">
      <router-view />
    </main>
    <AppFooter />
  </div>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<style scoped>
.public-shell {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.skip-link {
  position: absolute;
  top: -48px;
  left: 12px;
  padding: 10px 18px;
  background: var(--brand-primary);
  color: #fff;
  border-radius: 20px;
  z-index: 100;
  transition: top 0.2s ease;
}

.skip-link:focus {
  top: 16px;
}

.public-body {
  flex: 1;
  padding-top: 24px;
}
</style>
