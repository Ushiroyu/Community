<template>
  <div class="dashboard-shell">
    <a class="skip-link" href="#supplier-main">跳到主要内容</a>
    <NavBar />
    <div class="dashboard-body">
      <aside class="dashboard-sidebar" aria-label="侧边导航">
        <SidebarMenu />
      </aside>
      <main id="supplier-main" class="dashboard-main" tabindex="-1">
        <router-view />
      </main>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
import SidebarMenu from '@/components/SidebarMenu.vue'
import AppFooter from '@/components/AppFooter.vue'
</script>

<style scoped>
.dashboard-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.skip-link {
  position: absolute;
  top: -48px;
  left: 16px;
  padding: 10px 18px;
  background: var(--brand-primary);
  color: #fff;
  border-radius: 20px;
  z-index: 120;
  transition: top 0.2s ease;
}

.skip-link:focus {
  top: 16px;
}

.dashboard-body {
  flex: 1;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
  width: min(1280px, 95%);
  margin: 32px auto 0;
}

.dashboard-sidebar {
  position: sticky;
  top: 96px;
  align-self: flex-start;
}

.dashboard-main {
  padding-bottom: 72px;
}

@media (max-width: 1120px) {
  .dashboard-body {
    grid-template-columns: 1fr;
  }
  .dashboard-sidebar {
    position: static;
  }
}
</style>
