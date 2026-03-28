import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const twikooProxyTarget = env.VITE_TWIKOO_PROXY_TARGET || env.VITE_TWIKOO_URL || 'https://twikoo.godlei.cn'

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
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/twikoo-proxy': {
          target: twikooProxyTarget,
          changeOrigin: true,
          // 某些历史目标证书与域名不完全匹配，开发环境放宽 TLS 校验。
          secure: false,
          rewrite: (path) => {
            const p = path.replace(/^\/twikoo-proxy/, '')
            return p || '/'
          }
        }
      }
    }
  }
})
