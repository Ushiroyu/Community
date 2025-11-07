import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'node:path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_GATEWAY || env.VITE_PROXY_TARGET || 'http://localhost:8080'
  return {
    plugins: [
      vue(),
      AutoImport({
        imports: ['vue', 'vue-router'],
        resolvers: [ElementPlusResolver()],
        dts: 'src/auto-imports.d.ts'
      }),
      Components({
        resolvers: [ElementPlusResolver({ importStyle: 'css' })],
        dts: 'src/components.d.ts'
      })
    ],
    resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        // Dev 直连各微服务（网关未起也能用）
        '/api/users': { target: 'http://localhost:8081', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
        '/api/geo': { target: 'http://localhost:8081', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },

        '/api/products': { target: 'http://localhost:8082', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
        '/api/categories': { target: 'http://localhost:8082', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },

        '/api/orders': { target: 'http://localhost:8083', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
        '/api/cart': { target: 'http://localhost:8083', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
        '/api/stats': { target: 'http://localhost:8083', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },

        '/api/leaders': { target: 'http://localhost:8084', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
        '/api/communities': { target: 'http://localhost:8084', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },

        '/api/suppliers': { target: 'http://localhost:8085', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },
        '/api/shipping': { target: 'http://localhost:8085', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },

        // Admin stats in dev can bypass admin-service and hit order-service directly
        '/api/admin/stats': { target: 'http://localhost:8083', changeOrigin: true, rewrite: p => p.replace(/^\/api\/admin/, '') },
        '/api/admin': { target: 'http://localhost:8086', changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') },

        // 同时保留通过网关统一访问（启动网关后可用）
        '/api': { target, changeOrigin: true, rewrite: p => p.replace(/^\/api/, '') }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-icons': ['@element-plus/icons-vue'],
            'vendor-utils': ['dayjs']
          }
        }
      },
      chunkSizeWarningLimit: 1200
    }
  }
})
