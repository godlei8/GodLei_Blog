import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080'
  const uploadProxyTarget = env.VITE_UPLOAD_PROXY_TARGET || apiProxyTarget
  const twikooProxyTarget = env.VITE_TWIKOO_PROXY_TARGET || 'http://localhost:3000'

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

  const apiProxy = {
    '/api': {
      target: apiProxyTarget,
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }

  const uploadProxy = {
    '/uploads': {
      target: uploadProxyTarget,
      changeOrigin: true
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
        // 本地开发统一走 /api 与 /uploads，避免接口路径在不同环境下分叉
        ...apiProxy,
        ...uploadProxy,
        ...twikooProxy
      }
    },
    preview: {
      proxy: {
        ...apiProxy,
        ...uploadProxy,
        ...twikooProxy
      }
    }
  }
})
