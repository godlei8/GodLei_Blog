import { fetchSiteConfig } from '@/api'
import { normalizeMediaUrl } from '@/utils/image'

const REFERENCE_DEFAULT_AVATAR = 'https://qiniu.ayeez.cn/avatar.jpg'
const REFERENCE_DEFAULT_BACKGROUND = 'https://qiniu.ayeez.cn/bg.jpg'
const SITE_CONFIG_UPDATE_KEY = 'godlei-site-config-updated-at'
const SITE_CONFIG_CACHE_TTL = 15 * 1000

const defaultConfig = {
  basic: {
    siteName: 'GodLei Blog',
    profileAvatar: REFERENCE_DEFAULT_AVATAR
  },
  home: {
    backgroundImage: REFERENCE_DEFAULT_BACKGROUND,
    welcomePrefix: 'WELCOME TO',
    welcomeHighlight: 'GODLEI BLOG',
    noticeTitle: '公告',
    introLines: [
      '这里是 GodLei 的博客。',
      '很高兴与你相遇。',
      '这里会分享技术、生活和一些仍在折腾中的想法。'
    ],
    noticeLines: [
      '新站目前正在持续重构和完善中。',
      '如果你遇到图片缺失或排版异常，我会继续补齐。',
      '这里会记录开发、折腾和日常里值得留下的小事。'
    ],
    socialLinks: [
      { name: 'GitHub', icon: 'fab fa-github', url: 'https://github.com/godlei/' },
      { name: 'Bilibili', icon: 'fab fa-bilibili', url: 'https://space.bilibili.com/499974079' },
      { name: 'Email', icon: 'fas fa-envelope', url: 'mailto:godlei@gmail.com' }
    ]
  },
  about: {
    animeImages: []
  }
}

const clone = (value) => JSON.parse(JSON.stringify(value))

function unwrapSiteConfigPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return {}
  }

  if (payload.basic || payload.home || payload.about) {
    return payload
  }

  const nested = payload.data
  if (nested && typeof nested === 'object' && (nested.basic || nested.home || nested.about)) {
    return nested
  }

  return {}
}

const createLink = (item = {}) => ({
  name: item.name || '',
  icon: item.icon || '',
  url: item.url || ''
})

function normalizeText(value, fallback = '') {
  const normalized = typeof value === 'string' ? value.trim() : ''
  return normalized || fallback
}

function normalizeMediaValue(value, fallback = '') {
  return normalizeMediaUrl(normalizeText(value, fallback))
}

function normalizeStringList(list, fallback = []) {
  const source = Array.isArray(list) ? list : fallback
  return source.map((item) => String(item || '').trim()).filter(Boolean)
}

function normalizeMediaList(list, fallback = []) {
  return normalizeStringList(list, fallback).map((item) => normalizeMediaUrl(item))
}

function normalizeLinkList(list, fallback = []) {
  const source = Array.isArray(list) ? list : fallback
  return source.map((item) => createLink(item)).filter((item) => item.name || item.url)
}

function getSiteConfigVersionMarker() {
  if (typeof window === 'undefined' || !window.localStorage) return ''
  return window.localStorage.getItem(SITE_CONFIG_UPDATE_KEY) || ''
}

export function mergeSiteConfig(raw = {}) {
  const source = raw || {}

  return {
    basic: {
      siteName: normalizeText(source.basic?.siteName, defaultConfig.basic.siteName),
      profileAvatar: normalizeMediaValue(source.basic?.profileAvatar, defaultConfig.basic.profileAvatar)
    },
    home: {
      backgroundImage: normalizeMediaValue(source.home?.backgroundImage, defaultConfig.home.backgroundImage),
      welcomePrefix: normalizeText(source.home?.welcomePrefix, defaultConfig.home.welcomePrefix),
      welcomeHighlight: normalizeText(source.home?.welcomeHighlight, defaultConfig.home.welcomeHighlight),
      noticeTitle: normalizeText(source.home?.noticeTitle, defaultConfig.home.noticeTitle),
      introLines: normalizeStringList(source.home?.introLines, defaultConfig.home.introLines),
      noticeLines: normalizeStringList(source.home?.noticeLines, defaultConfig.home.noticeLines),
      socialLinks: normalizeLinkList(source.home?.socialLinks, defaultConfig.home.socialLinks)
    },
    about: {
      animeImages: normalizeMediaList(source.about?.animeImages, defaultConfig.about.animeImages)
    }
  }
}

let cachedConfig = null
let pendingConfigPromise = null
let cacheUpdatedAt = 0
let cachedVersionMarker = ''

export function getDefaultSiteConfig() {
  return clone(defaultConfig)
}

export async function loadSiteConfig(force = false) {
  const versionMarker = getSiteConfigVersionMarker()
  const cacheExpired = !cacheUpdatedAt || (Date.now() - cacheUpdatedAt) > SITE_CONFIG_CACHE_TTL
  const versionChanged = cachedVersionMarker !== versionMarker

  if (!force && cachedConfig && !cacheExpired && !versionChanged) {
    return clone(cachedConfig)
  }

  if (pendingConfigPromise) {
    return pendingConfigPromise.then((value) => clone(value))
  }

  pendingConfigPromise = fetchSiteConfig()
    .then((payload) => {
      cachedConfig = mergeSiteConfig(unwrapSiteConfigPayload(payload))
      cacheUpdatedAt = Date.now()
      cachedVersionMarker = getSiteConfigVersionMarker() || versionMarker
      return cachedConfig
    })
    .catch((error) => {
      console.error('加载站点配置失败，回退到默认配置', error)
      cachedConfig = mergeSiteConfig({})
      cacheUpdatedAt = Date.now()
      cachedVersionMarker = getSiteConfigVersionMarker() || versionMarker
      return cachedConfig
    })
    .finally(() => {
      pendingConfigPromise = null
    })

  const config = await pendingConfigPromise
  return clone(config)
}

export function resetSiteConfigCache() {
  cachedConfig = null
  pendingConfigPromise = null
  cacheUpdatedAt = 0
  cachedVersionMarker = ''
}
