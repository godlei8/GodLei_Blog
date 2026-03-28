import axios from 'axios'
import router from '@/router'

const baseURL = (import.meta?.env?.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

const request = axios.create({
  baseURL,
  timeout: 15000
})

function redirectToLogin(reason = 'expired') {
  localStorage.removeItem('token')
  if (router.currentRoute.value.path !== '/login') {
    router.replace({
      path: '/login',
      query: { reason }
    })
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers = config.headers || {}
      config.headers.token = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const payload = response.data || {}

    if (payload.code === 200) {
      return payload.data
    }

    if (payload.code === 401) {
      redirectToLogin('expired')
      return Promise.reject(new Error(payload.message || '登录已过期，请重新登录'))
    }

    return Promise.reject(new Error(payload.message || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '请求失败'

    if (status === 401) {
      redirectToLogin('expired')
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    return Promise.reject(new Error(message))
  }
)

export default request
