<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>
<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const locale = zhCn
</script>
<style>
html,body,#app{height:100%;margin:0;}
</style>
