const LOCALHOST_NAMES = new Set(['localhost', '127.0.0.1', '::1'])
const DEV_PORT_FALLBACKS = {
  '5173': ['5174', '4173', '4174'],
  '5174': ['5173', '4173', '4174'],
  '4173': ['4174', '5173', '5174'],
  '4174': ['4173', '5173', '5174']
}

function isLocalHostname(hostname = '') {
  return LOCALHOST_NAMES.has(String(hostname || '').toLowerCase())
}

function normalizePath(path = '/') {
  const raw = String(path || '/').trim() || '/'
  return raw.startsWith('/') ? raw : `/${raw}`
}

function createLocalOrigin(protocol, hostname, port) {
  return `${protocol}//${hostname}${port ? `:${port}` : ''}`
}

export function resolveSitePreviewBaseUrl(explicitBase = '') {
  const configured = String(explicitBase || '').trim()
  if (configured) {
    if (typeof window === 'undefined') return configured
    try {
      return new URL(configured, window.location.origin).toString().replace(/\/$/, '')
    } catch {
      return configured.replace(/\/$/, '')
    }
  }

  if (typeof window === 'undefined') return ''

  try {
    const current = new URL(window.location.href)
    const currentOrigin = `${current.protocol}//${current.host}`

    if (!isLocalHostname(current.hostname)) {
      return currentOrigin
    }

    const fallbackPorts = DEV_PORT_FALLBACKS[current.port] || []
    if (fallbackPorts.length > 0) {
      return createLocalOrigin(current.protocol, current.hostname, fallbackPorts[0])
    }

    return currentOrigin
  } catch {
    return window.location.origin
  }
}

export function buildSitePreviewUrl(path = '/', explicitBase = '') {
  const base = resolveSitePreviewBaseUrl(explicitBase)
  if (!base) return ''

  try {
    return new URL(normalizePath(path), `${base}/`).toString()
  } catch {
    return `${base.replace(/\/$/, '')}${normalizePath(path)}`
  }
}

export function openSitePreviewInNewTab(path = '/', explicitBase = '') {
  if (typeof window === 'undefined') return ''
  const url = buildSitePreviewUrl(path, explicitBase)
  if (!url) return ''
  window.open(url, '_blank', 'noopener,noreferrer')
  return url
}
