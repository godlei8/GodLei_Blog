import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const twikooProxyTarget = env.VITE_TWIKOO_PROXY_TARGET || env.VITE_TWIKOO_URL || 'https://twikoo.godlei.cn'

  // 本地访问 Twikoo：浏览器直连会 CORS，走同源代理转发。
  const twikooProxy = {
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

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      proxy: {
        // 兼容前台统一使用 /api 前缀：去掉 /api 并转发到后端真实路由
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        },
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },

        // 本地开发把前端请求转发到后端，避免 CORS
        '/post': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/logs': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        '/links/list': {
          target: 'http://localhost:8080',
          changeOrigin: true
        },
        ...twikooProxy
      }
    },
    preview: {
      proxy: {
        ...twikooProxy
      }
    }
  }
})
