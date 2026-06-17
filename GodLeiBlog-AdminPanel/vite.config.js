import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'
  const uploadProxyTarget = env.VITE_UPLOAD_PROXY_TARGET || apiProxyTarget
  const twikooProxyTarget = env.VITE_TWIKOO_PROXY_TARGET || 'http://localhost:3000'

  const proxy = {
    '/api': {
      target: apiProxyTarget,
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    },
    '/uploads': {
      target: uploadProxyTarget,
      changeOrigin: true
    },
    '/twikoo-proxy': {
      target: twikooProxyTarget,
      changeOrigin: true,
      secure: false,
      rewrite: (path) => {
        const p = path.replace(/^\/twikoo-proxy/, '')
        return p || '/'
      }
    }
  }

  return {
    // 部署在 Nginx 子路径 /admin/ 时使用；本地 dev 不受影响
    base: '/admin/',
    plugins: [
      vue(),
      Components({
        dts: false,
        resolvers: [ElementPlusResolver({ importStyle: 'css' })]
      })
    ],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy
    },
    preview: {
      proxy
    }
  }
})
