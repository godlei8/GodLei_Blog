import { reactive } from 'vue'

const defaultPageContext = {
  pageType: 'page',
  route: '/',
  title: '',
  summary: '',
  contentExcerpt: '',
  momentId: null,
  currentMomentSummary: ''
}

const pageContextState = reactive({ ...defaultPageContext })

function normalizeText(value, maxLength = 600) {
  const normalized = typeof value === 'string' ? value.trim() : ''
  if (!normalized) return ''
  return normalized.slice(0, maxLength)
}

export function setPageContext(next = {}) {
  pageContextState.pageType = normalizeText(next.pageType || defaultPageContext.pageType, 64) || defaultPageContext.pageType
  pageContextState.route = normalizeText(next.route || defaultPageContext.route, 255) || defaultPageContext.route
  pageContextState.title = normalizeText(next.title, 160)
  pageContextState.summary = normalizeText(next.summary, 400)
  pageContextState.contentExcerpt = normalizeText(next.contentExcerpt, 2000)
  pageContextState.momentId = Number.isFinite(Number(next.momentId)) ? Number(next.momentId) : null
  pageContextState.currentMomentSummary = normalizeText(next.currentMomentSummary, 600)
}

export function resetPageContext() {
  setPageContext(defaultPageContext)
}

export { pageContextState }
