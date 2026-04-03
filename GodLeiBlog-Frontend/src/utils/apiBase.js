const DEFAULT_API_BASE_URL = '/api'

function trimTrailingSlash(value = '') {
  return String(value || '').replace(/\/+$/, '')
}

function ensureLeadingSlash(value = '') {
  const normalized = String(value || '').trim()
  if (!normalized) return '/'
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

export function getApiBaseUrl() {
  const raw = String(import.meta?.env?.VITE_API_BASE_URL || DEFAULT_API_BASE_URL).trim()
  if (!raw || raw === '/') return DEFAULT_API_BASE_URL
  return trimTrailingSlash(raw)
}

export function resolveApiUrl(path = '/') {
  const normalizedPath = ensureLeadingSlash(path)
  const base = getApiBaseUrl()
  if (!base || base === '/') {
    return normalizedPath
  }
  return `${trimTrailingSlash(base)}${normalizedPath}`
}
