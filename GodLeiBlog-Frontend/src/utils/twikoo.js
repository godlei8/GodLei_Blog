/**
 * Load Twikoo from a static script in /public so Vite does not bundle the
 * large UMD build into a warning-producing chunk.
 */

const DEFAULT_TWIKOO_URL = 'https://twikoo.godlei.cn'
const SAME_ORIGIN_TWIKOO_PROXY = '/twikoo-proxy/'
const TWIKOO_UNAVAILABLE_HINT = '当前 Twikoo 服务地址不可用，请检查 VITE_TWIKOO_URL 或 /twikoo-proxy 的反向代理目标。'
const TWIKOO_RATE_LIMIT_HINT = 'Twikoo 评论当前返回 Too Many Requests，请确认服务端已关闭限流或稍后再试。'

let _twikooApiCache = null
let _twikooLoadPromise = null

function pickTwikooApi(mod) {
  if (!mod || typeof mod !== 'object') return null
  const candidates = [mod, mod.default, mod.twikoo, mod.default?.default]
  for (const candidate of candidates) {
    if (candidate && typeof candidate.init === 'function') return candidate
  }
  return null
}

function getTwikooScriptSrc() {
  const base = import.meta.env.BASE_URL || '/'
  return `${base.endsWith('/') ? base : `${base}/`}twikoo.all.min.js`
}

function getTwikooApiFromWindow() {
  if (typeof window === 'undefined') return null
  return pickTwikooApi(window.twikoo) || pickTwikooApi({ default: window.twikoo, twikoo: window.twikoo })
}

function compactTwikooDetail(detail) {
  return String(detail || '')
    .replace(/\s+/g, ' ')
    .replace(/<[^>]+>/g, ' ')
    .trim()
    .slice(0, 200)
}

function formatTwikooTarget(target) {
  const raw = String(target || '').trim()
  if (!raw) return '未配置'
  try {
    const parsed = new URL(raw)
    return `${parsed.origin}${parsed.pathname}`.replace(/\/$/, '') || parsed.origin
  } catch {
    return raw
  }
}

function resolveTwikooTarget(target) {
  const raw = String(target || '').trim()
  if (!raw) return ''
  if (typeof window === 'undefined') return raw
  try {
    return new URL(raw, window.location.origin).toString()
  } catch {
    return raw
  }
}

function buildTwikooRequestErrorMessage(status, detail = '', target = '') {
  const compactDetail = compactTwikooDetail(detail)
  const isRateLimited =
    Number(status) === 429
    || /Too Many Requests/i.test(compactDetail)
  const isUnavailable =
    Number(status) === 405
    || /405 Not Allowed/i.test(compactDetail)
    || /站点已暂停|停止运行|nginx/i.test(compactDetail)

  if (isRateLimited) {
    return `${TWIKOO_RATE_LIMIT_HINT} 当前目标：${formatTwikooTarget(target)}`
  }

  if (isUnavailable) {
    return `${TWIKOO_UNAVAILABLE_HINT} 当前目标：${formatTwikooTarget(target)}`
  }

  return `Twikoo 请求失败：${status}${compactDetail ? ` ${compactDetail}` : ''}`
}

export function getTwikooUnavailableHint() {
  return TWIKOO_UNAVAILABLE_HINT
}

export function isTwikooServiceUnavailableError(input) {
  const message = typeof input === 'string' ? input : input?.message || ''
  return message.includes(TWIKOO_UNAVAILABLE_HINT) || message.includes('Twikoo 服务当前不可用')
}

export function normalizeTwikooError(error, target = '') {
  if (error instanceof Error && isTwikooServiceUnavailableError(error)) {
    return error
  }

  if (typeof error === 'number') {
    return new Error(buildTwikooRequestErrorMessage(error, '', target))
  }

  const status = Number(error?.status) || Number(error?.statusCode)
  if (status) {
    return new Error(buildTwikooRequestErrorMessage(status, error?.message || error?.detail || '', target))
  }

  const message = error instanceof Error
    ? error.message
    : (typeof error === 'string' ? error : String(error || ''))

  if (isTwikooServiceUnavailableError(message)) {
    return new Error(message)
  }

  if (/Failed to fetch|NetworkError|Load failed|fetch failed/i.test(message)) {
    return new Error(`Twikoo 服务当前不可用，请检查网络或评论服务地址。当前目标：${formatTwikooTarget(target)}`)
  }

  return new Error(message || `Twikoo 服务当前不可用。当前目标：${formatTwikooTarget(target)}`)
}

function ensureTwikooScript() {
  const api = getTwikooApiFromWindow()
  if (api) return Promise.resolve(api)
  if (typeof window === 'undefined') {
    return Promise.reject(new Error('Twikoo can only be loaded in the browser.'))
  }
  if (_twikooLoadPromise) return _twikooLoadPromise

  const src = getTwikooScriptSrc()
  _twikooLoadPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector('script[data-twikoo-script="true"]')
    const script = existingScript || document.createElement('script')

    const cleanup = () => {
      script.removeEventListener('load', handleLoad)
      script.removeEventListener('error', handleError)
    }

    const handleLoad = () => {
      script.dataset.loaded = 'true'
      const loadedApi = getTwikooApiFromWindow()
      cleanup()
      if (loadedApi) {
        _twikooApiCache = loadedApi
        resolve(loadedApi)
        return
      }
      _twikooLoadPromise = null
      reject(new Error('Twikoo script loaded, but window.twikoo.init was not found.'))
    }

    const handleError = () => {
      cleanup()
      _twikooLoadPromise = null
      reject(new Error(`Failed to load Twikoo script: ${src}`))
    }

    script.addEventListener('load', handleLoad)
    script.addEventListener('error', handleError)

    if (!existingScript) {
      script.src = src
      script.async = true
      script.defer = true
      script.dataset.twikooScript = 'true'
      document.head.appendChild(script)
      return
    }

    if (existingScript.dataset.loaded === 'true') {
      Promise.resolve().then(handleLoad)
    }
  })

  return _twikooLoadPromise
}

/**
 * @returns {Promise<{ init: Function }>}
 */
export async function loadTwikoo() {
  if (_twikooApiCache) return _twikooApiCache
  const api = await ensureTwikooScript()
  _twikooApiCache = api
  return api
}

export function getTwikooEnvId() {
  const fromEnv = import.meta.env.VITE_TWIKOO_URL
  if (fromEnv) return resolveTwikooTarget(fromEnv)
  if (typeof window === 'undefined') return DEFAULT_TWIKOO_URL
  return resolveTwikooTarget(SAME_ORIGIN_TWIKOO_PROXY)
}

export function getTwikooAccessToken() {
  if (typeof window === 'undefined' || !window.localStorage) return ''
  return window.localStorage.getItem('twikoo-access-token') || ''
}

export async function requestTwikooEvent(event, payload = {}, envId = getTwikooEnvId()) {
  const target = typeof envId === 'string' ? envId.trim() : ''
  if (!target) {
    throw new Error(`Twikoo 服务未配置，请设置 VITE_TWIKOO_URL 或提供 /twikoo-proxy。当前目标：${formatTwikooTarget(target)}`)
  }

  const body = {
    event,
    accessToken: getTwikooAccessToken(),
    ...payload
  }

  let response
  try {
    response = await fetch(target, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(body)
    })
  } catch (error) {
    throw normalizeTwikooError(error, target)
  }

  if (!response.ok) {
    let detail = ''
    try {
      detail = await response.text()
    } catch {
      // ignore
    }
    throw new Error(buildTwikooRequestErrorMessage(response.status, detail, target))
  }

  return await response.json()
}

export async function getTwikooCommentsCount(options = {}) {
  const envId = getTwikooEnvId()
  const twikoo = await loadTwikoo()
  if (typeof twikoo.getCommentsCount !== 'function') {
    throw new Error('当前 Twikoo 版本不支持 getCommentsCount。')
  }
  try {
    return await twikoo.getCommentsCount({
      envId,
      includeReply: true,
      ...options
    })
  } catch (error) {
    throw normalizeTwikooError(error, envId)
  }
}

export async function getTwikooRecentComments(options = {}) {
  const envId = getTwikooEnvId()
  const twikoo = await loadTwikoo()
  if (typeof twikoo.getRecentComments !== 'function') {
    throw new Error('当前 Twikoo 版本不支持 getRecentComments。')
  }
  try {
    return await twikoo.getRecentComments({
      envId,
      includeReply: true,
      ...options
    })
  } catch (error) {
    throw normalizeTwikooError(error, envId)
  }
}
