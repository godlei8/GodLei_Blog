const BROKEN_HOSTS = new Set(['qiniu.godlei.cn'])

export const avatarFallbackUrl = new URL('../assets/imgs/site/default-avatar.png', import.meta.url).href
export const coverFallbackUrl = new URL('../assets/imgs/site/default-home-bg.jpg', import.meta.url).href

function svgToDataUrl(svg) {
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg.trim())}`
}

const animePalettes = [
  ['#19080c', '#601624', '#d6ad5c'],
  ['#120508', '#7a1d2d', '#f2d39a'],
  ['#1d0a10', '#4a121d', '#c9963f'],
  ['#12060a', '#5c1020', '#e7c777']
]

export function createAnimePosterFallback(seed = 0) {
  const [base, mid, accent] = animePalettes[Math.abs(seed) % animePalettes.length]
  return svgToDataUrl(`
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 704">
      <defs>
        <linearGradient id="posterBg${seed}" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="${base}" />
          <stop offset="58%" stop-color="${mid}" />
          <stop offset="100%" stop-color="${accent}" />
        </linearGradient>
      </defs>
      <rect width="500" height="704" rx="28" fill="url(#posterBg${seed})" />
      <rect x="32" y="32" width="436" height="640" rx="22" fill="rgba(255,255,255,0.06)" />
      <circle cx="372" cy="150" r="78" fill="rgba(233,201,138,0.28)" />
      <path d="M80 552c50-88 116-136 166-136s116 48 166 136" fill="rgba(255,245,225,0.12)" />
      <path d="M84 620h332" stroke="${accent}" stroke-width="10" stroke-linecap="round" opacity="0.82" />
      <text x="86" y="142" font-family="Georgia, serif" font-size="56" fill="#f3debc">Poster ${seed + 1}</text>
    </svg>
  `)
}

function getHostname(url) {
  if (!url) return ''

  try {
    return new URL(url, 'https://local.asset').hostname.toLowerCase()
  } catch {
    return ''
  }
}

export function normalizeMediaUrl(url) {
  const value = typeof url === 'string' ? url.trim() : ''
  if (!value) return ''

  const normalized = value.replace(/\\/g, '/')
  if (normalized.startsWith('/api/uploads/')) {
    return normalized
  }
  if (normalized.startsWith('/uploads/')) {
    return `/api${normalized}`
  }
  if (normalized.startsWith('uploads/')) {
    return `/api/${normalized}`
  }

  return normalized
}

export function resolveImageUrl(url, fallback = coverFallbackUrl) {
  const value = normalizeMediaUrl(url)
  if (!value) return fallback
  if (BROKEN_HOSTS.has(getHostname(value))) return fallback
  return value
}

export function applyImageFallback(event, fallback = coverFallbackUrl) {
  const target = event?.target
  if (!target) return
  if (target.dataset.fallbackApplied === 'true') return
  target.dataset.fallbackApplied = 'true'
  target.src = fallback
}

export function bindImageFallback(img, fallback = coverFallbackUrl) {
  if (!img || img.dataset.fallbackBound === 'true') return
  img.dataset.fallbackBound = 'true'

  const handleError = () => {
    if (img.dataset.fallbackApplied === 'true') return
    img.dataset.fallbackApplied = 'true'
    img.src = fallback
  }

  img.addEventListener('error', handleError)

  if (img.complete && img.naturalWidth === 0) {
    handleError()
  }
}
