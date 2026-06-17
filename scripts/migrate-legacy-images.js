#!/usr/bin/env node

const fs = require('fs')
const fsp = require('fs/promises')
const path = require('path')
const crypto = require('crypto')
const mysql = require('mysql2/promise')

const DEFAULT_LEGACY_IMAGE_HOSTS = [
  'qiniu.godlei.cn',
  'javaweb-godlei.oss-cn-beijing.aliyuncs.com'
]

const HELP_TEXT = `
GodLei Blog legacy image migration CLI

Usage:
  node ./scripts/migrate-legacy-images.js [--env-file ./scripts/migrate-legacy-images.env]
  node ./scripts/migrate-legacy-images.js --dry-run
  node ./scripts/migrate-legacy-images.js --apply
`

const MARKDOWN_IMAGE_REGEX = /!\[([^\]]*)]\((<)?((?:https?:\/\/|\/\/)[^)>\s]+)(>)?((?:\s+["'][^"']*["'])?)\)/gi
const HTML_IMAGE_REGEX = /<img\b([^>]*?)\bsrc=(['"])(.*?)\2([^>]*)>/gi

async function main() {
  const args = parseArgs(process.argv.slice(2))
  if (args.help) {
    console.log(HELP_TEXT.trim())
    return
  }

  if (args.envFile) {
    applyEnvFile(args.envFile)
  }

  const config = loadConfig(args)
  const report = createReport(config)

  let connection
  try {
    connection = await mysql.createConnection({
      host: config.db.host,
      port: config.db.port,
      user: config.db.user,
      password: config.db.password,
      database: config.db.name,
      charset: 'utf8mb4'
    })

    const state = await collectDiscoveryState(connection, config, report)

    if (config.dryRun) {
      await probeSources(state.assetsBySource, config, report)
    } else {
      printBackupReminder(config)
      await downloadSources(state.assetsBySource, config, report)
      await rewriteDatabase(connection, state, config, report)
      await validateDownloadedAssets(state.assetsBySource, config, report)
      const remainingState = await collectDiscoveryState(connection, config)
      report.remainingLegacyReferences = remainingState.references
        .map((reference) => sanitizeReportEntry(reference, config.keepSourceUrlReport))
    }
  } catch (error) {
    report.failed.push(sanitizeReportEntry({
      scope: 'runtime',
      error: error && error.message ? error.message : String(error || 'Unknown error')
    }, config.keepSourceUrlReport))
    throw error
  } finally {
    if (connection) {
      await connection.end()
    }
    await writeReport(config.reportPath, report)
    printSummary(report, config)
  }
}

function parseArgs(argv) {
  const args = {}
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index]
    if (value === '--help' || value === '-h') {
      args.help = true
      continue
    }
    if (value === '--env-file') {
      args.envFile = argv[index + 1]
      index += 1
      continue
    }
    if (value === '--report') {
      args.reportPath = argv[index + 1]
      index += 1
      continue
    }
    if (value === '--dry-run') {
      args.dryRun = true
      continue
    }
    if (value === '--apply') {
      args.dryRun = false
    }
  }
  return args
}

function applyEnvFile(filePath) {
  const absolutePath = path.resolve(process.cwd(), filePath)
  if (!fs.existsSync(absolutePath)) {
    throw new Error(`Env file not found: ${absolutePath}`)
  }

  const content = fs.readFileSync(absolutePath, 'utf8')
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const separatorIndex = line.indexOf('=')
    if (separatorIndex <= 0) continue
    const key = line.slice(0, separatorIndex).trim()
    const value = stripWrappingQuotes(line.slice(separatorIndex + 1).trim())
    if (!process.env[key]) {
      process.env[key] = value
    }
  }
}

function stripWrappingQuotes(value) {
  if (!value) return value
  if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
    return value.slice(1, -1)
  }
  return value
}

function loadConfig(args) {
  const reportPath = args.reportPath || process.env.REPORT_PATH || defaultReportPath()
  const dryRun = typeof args.dryRun === 'boolean' ? args.dryRun : parseBoolean(process.env.DRY_RUN, true)
  const config = {
    dryRun,
    keepSourceUrlReport: parseBoolean(process.env.KEEP_SOURCE_URL_REPORT, true),
    reportPath: path.resolve(process.cwd(), reportPath),
    requestTimeoutMs: parseInteger(process.env.REQUEST_TIMEOUT_MS, 15000),
    db: {
      host: process.env.DB_HOST || '127.0.0.1',
      port: parseInteger(process.env.DB_PORT, 3306),
      name: process.env.DB_NAME || '',
      user: process.env.DB_USER || '',
      password: process.env.DB_PASSWORD || ''
    },
    uploadBaseDir: process.env.UPLOAD_BASE_DIR ? path.resolve(process.cwd(), process.env.UPLOAD_BASE_DIR) : '',
    uploadPublicPath: normalizePublicPath(process.env.UPLOAD_PUBLIC_PATH || '/uploads'),
    uploadPublicBaseUrl: trimTrailingSlash(process.env.UPLOAD_PUBLIC_BASE_URL || ''),
    legacyHosts: parseLegacyHosts(process.env.LEGACY_IMAGE_HOSTS || '')
  }

  if (!config.db.name) throw new Error('DB_NAME is required.')
  if (!config.db.user) throw new Error('DB_USER is required.')
  if (!config.dryRun && !config.uploadBaseDir) throw new Error('UPLOAD_BASE_DIR is required in apply mode.')

  return config
}

function defaultReportPath() {
  const now = new Date()
  const timestamp = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, '0'),
    String(now.getDate()).padStart(2, '0'),
    '-',
    String(now.getHours()).padStart(2, '0'),
    String(now.getMinutes()).padStart(2, '0'),
    String(now.getSeconds()).padStart(2, '0')
  ].join('')
  return path.join('scripts', 'reports', `legacy-image-migration-${timestamp}.json`)
}

function parseLegacyHosts(raw) {
  const values = new Set(DEFAULT_LEGACY_IMAGE_HOSTS.map((item) => item.toLowerCase()))
  for (const part of String(raw || '').split(',')) {
    const normalized = part.trim().toLowerCase()
    if (!normalized) continue
    try {
      const url = normalized.startsWith('http://') || normalized.startsWith('https://')
        ? new URL(normalized)
        : new URL(`https://${normalized}`)
      values.add(url.hostname.toLowerCase())
    } catch {
      values.add(normalized.replace(/^\.+/, ''))
    }
  }
  return Array.from(values)
}

function createReport(config) {
  return {
    startedAt: new Date().toISOString(),
    mode: config.dryRun ? 'dry-run' : 'apply',
    config: {
      dryRun: config.dryRun,
      uploadBaseDir: config.uploadBaseDir,
      uploadPublicPath: config.uploadPublicPath,
      uploadPublicBaseUrl: config.uploadPublicBaseUrl,
      legacyHosts: config.legacyHosts,
      requestTimeoutMs: config.requestTimeoutMs,
      reportPath: config.reportPath
    },
    discovered: {
      total: 0,
      uniqueSourceCount: 0,
      byTableField: {},
      byDomain: {},
      items: []
    },
    downloaded: [],
    rewritten: [],
    skipped: [],
    failed: [],
    unreachableSources: [],
    remainingLegacyReferences: []
  }
}

async function collectDiscoveryState(connection, config, report = null) {
  const state = {
    siteConfigRow: null,
    postRows: [],
    friendLinkRows: [],
    momentMediaRows: [],
    mediaRows: [],
    references: [],
    assetsBySource: new Map()
  }

  const [siteRows] = await connection.execute(
    'SELECT setting_key AS settingKey, setting_value AS settingValue FROM blog_site_setting WHERE setting_key = ?',
    ['site_config']
  )
  if (siteRows.length) {
    let parsedConfig = null
    try {
      parsedConfig = parseJson(siteRows[0].settingValue, 'blog_site_setting.site_config')
    } catch (error) {
      if (report) {
        report.failed.push({
          table: 'blog_site_setting',
          rowId: siteRows[0].settingKey,
          error: error.message
        })
      }
    }
    state.siteConfigRow = {
      settingKey: siteRows[0].settingKey,
      settingValue: siteRows[0].settingValue,
      parsedConfig
    }
    scanSiteConfigRow(state, state.siteConfigRow, config, report)
  }

  const [posts] = await connection.execute('SELECT id, cover, content FROM blog_post')
  state.postRows = posts
  for (const row of posts) {
    addReferenceIfLegacy(state, config, report, {
      table: 'blog_post',
      field: 'cover',
      rowId: row.id,
      sourceUrl: row.cover,
      bizType: 'post-cover'
    })
    for (const url of extractContentImageUrls(row.content)) {
      addReferenceIfLegacy(state, config, report, {
        table: 'blog_post',
        field: 'content',
        rowId: row.id,
        sourceUrl: url,
        bizType: 'post-content'
      })
    }
  }

  const [friendLinks] = await connection.execute('SELECT id, avatar FROM friend_link')
  state.friendLinkRows = friendLinks
  for (const row of friendLinks) {
    addReferenceIfLegacy(state, config, report, {
      table: 'friend_link',
      field: 'avatar',
      rowId: row.id,
      sourceUrl: row.avatar,
      bizType: 'link-avatar'
    })
  }

  const [momentMediaRows] = await connection.execute(
    'SELECT id, moment_id AS momentId, media_url AS mediaUrl FROM blog_moment_media'
  )
  state.momentMediaRows = momentMediaRows
  for (const row of momentMediaRows) {
    addReferenceIfLegacy(state, config, report, {
      table: 'blog_moment_media',
      field: 'media_url',
      rowId: row.id,
      sourceUrl: row.mediaUrl,
      bizType: 'moment-media'
    })
  }

  const [mediaRows] = await connection.execute(
    'SELECT id, biz_type AS bizType, access_url AS accessUrl FROM blog_media_file'
  )
  state.mediaRows = mediaRows
  for (const row of mediaRows) {
    addReferenceIfLegacy(state, config, report, {
      table: 'blog_media_file',
      field: 'access_url',
      rowId: row.id,
      sourceUrl: row.accessUrl,
      bizType: row.bizType || 'common',
      existingMediaRow: {
        id: row.id,
        bizType: row.bizType || 'common'
      }
    })
  }

  if (report) {
    report.discovered.total = state.references.length
    report.discovered.uniqueSourceCount = state.assetsBySource.size
  }

  return state
}

function scanSiteConfigRow(state, row, config, report) {
  if (!row.parsedConfig) return

  addReferenceIfLegacy(state, config, report, {
    table: 'blog_site_setting',
    field: 'setting_value.basic.profileAvatar',
    rowId: row.settingKey,
    sourceUrl: row.parsedConfig.basic?.profileAvatar,
    bizType: 'site-avatar'
  })

  addReferenceIfLegacy(state, config, report, {
    table: 'blog_site_setting',
    field: 'setting_value.home.backgroundImage',
    rowId: row.settingKey,
    sourceUrl: row.parsedConfig.home?.backgroundImage,
    bizType: 'home-background'
  })

  const animeImages = Array.isArray(row.parsedConfig.about?.animeImages) ? row.parsedConfig.about.animeImages : []
  animeImages.forEach((item, index) => {
    addReferenceIfLegacy(state, config, report, {
      table: 'blog_site_setting',
      field: `setting_value.about.animeImages[${index}]`,
      rowId: row.settingKey,
      sourceUrl: item,
      bizType: 'anime-poster'
    })
  })
}

function addReferenceIfLegacy(state, config, report, reference) {
  const normalizedSourceUrl = normalizeLegacySourceUrl(reference.sourceUrl, config.legacyHosts)
  if (!normalizedSourceUrl) return

  const normalizedReference = {
    table: reference.table,
    field: reference.field,
    rowId: reference.rowId,
    sourceUrl: normalizedSourceUrl,
    bizType: normalizeBizType(reference.bizType || 'common')
  }

  if (reference.existingMediaRow) {
    normalizedReference.existingMediaRow = {
      id: reference.existingMediaRow.id,
      bizType: normalizeBizType(reference.existingMediaRow.bizType || 'common')
    }
  }

  state.references.push(normalizedReference)
  bumpCounter(report?.discovered?.byTableField, `${normalizedReference.table}.${normalizedReference.field}`)
  bumpCounter(report?.discovered?.byDomain, getHostname(normalizedSourceUrl))
  if (report) {
    report.discovered.items.push(sanitizeReportEntry(normalizedReference, config.keepSourceUrlReport))
  }

  let asset = state.assetsBySource.get(normalizedSourceUrl)
  if (!asset) {
    asset = {
      sourceUrl: normalizedSourceUrl,
      bizType: normalizedReference.bizType,
      references: [],
      existingMediaRows: [],
      local: null
    }
    state.assetsBySource.set(normalizedSourceUrl, asset)
  }

  if (!asset.bizType || asset.bizType === 'common') {
    asset.bizType = normalizedReference.bizType
  }
  asset.references.push(normalizedReference)
  if (normalizedReference.existingMediaRow) {
    asset.existingMediaRows.push(normalizedReference.existingMediaRow)
  }
}

function parseJson(text, label) {
  try {
    return JSON.parse(text)
  } catch (error) {
    throw new Error(`Failed to parse ${label}: ${error.message}`)
  }
}

function extractContentImageUrls(content) {
  const urls = new Set()
  const source = String(content || '')

  for (const match of source.matchAll(MARKDOWN_IMAGE_REGEX)) {
    urls.add(match[3])
  }
  for (const match of source.matchAll(HTML_IMAGE_REGEX)) {
    urls.add(match[3])
  }

  return Array.from(urls)
}

function normalizeLegacySourceUrl(raw, legacyHosts) {
  const source = String(raw || '').trim()
  if (!source) return ''

  let url
  try {
    if (source.startsWith('//')) {
      url = new URL(`https:${source}`)
    } else if (/^https?:\/\//i.test(source)) {
      url = new URL(source)
    } else {
      return ''
    }
  } catch {
    return ''
  }

  const hostname = url.hostname.toLowerCase()
  const isLegacy = legacyHosts.some((item) => hostname === item || hostname.endsWith(`.${item}`))
  if (!isLegacy) return ''

  url.hash = ''
  return url.toString()
}

function bumpCounter(target, key) {
  if (!target || !key) return
  target[key] = (target[key] || 0) + 1
}

async function probeSources(assetsBySource, config, report) {
  for (const asset of assetsBySource.values()) {
    try {
      await probeRemoteImage(asset.sourceUrl, config.requestTimeoutMs)
    } catch (error) {
      report.unreachableSources.push(sanitizeReportEntry({
        sourceUrl: asset.sourceUrl,
        error: error.message
      }, config.keepSourceUrlReport))
    }
  }
}

async function probeRemoteImage(sourceUrl, timeoutMs) {
  const response = await timedFetch(sourceUrl, timeoutMs)
  const contentType = getPrimaryContentType(response.headers.get('content-type'))
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  if (!contentType.startsWith('image/')) {
    throw new Error(`Unexpected content type: ${contentType || 'unknown'}`)
  }
  if (response.body && typeof response.body.cancel === 'function') {
    await response.body.cancel()
  }
}

async function downloadSources(assetsBySource, config, report) {
  for (const asset of assetsBySource.values()) {
    try {
      asset.local = await downloadRemoteImage(asset, config)
      report.downloaded.push(sanitizeReportEntry({
        sourceUrl: asset.sourceUrl,
        accessUrl: asset.local.accessUrl,
        relativePath: asset.local.relativePath,
        bizType: asset.local.bizType,
        fileSize: asset.local.fileSize,
        contentType: asset.local.contentType
      }, config.keepSourceUrlReport))
    } catch (error) {
      report.unreachableSources.push(sanitizeReportEntry({
        sourceUrl: asset.sourceUrl,
        error: error.message
      }, config.keepSourceUrlReport))
    }
  }
}

async function downloadRemoteImage(asset, config) {
  const response = await timedFetch(asset.sourceUrl, config.requestTimeoutMs)
  const contentType = getPrimaryContentType(response.headers.get('content-type'))
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  if (!contentType.startsWith('image/')) {
    throw new Error(`Unexpected content type: ${contentType || 'unknown'}`)
  }

  const arrayBuffer = await response.arrayBuffer()
  const buffer = Buffer.from(arrayBuffer)
  if (!buffer.length) {
    throw new Error('Downloaded file is empty')
  }

  const extension = resolveExtension(asset.sourceUrl, contentType)
  const originalName = resolveOriginalName(asset.sourceUrl, extension)
  const storedName = `${crypto.randomUUID().replace(/-/g, '')}${extension}`
  const bizType = resolveAssetBizType(asset)
  const relativePath = buildRelativePath(bizType, storedName)
  const absolutePath = resolveUploadTarget(config.uploadBaseDir, relativePath)

  await fsp.mkdir(path.dirname(absolutePath), { recursive: true })
  await fsp.writeFile(absolutePath, buffer)

  const accessUrl = buildRuntimeAccessUrl(config.uploadPublicPath, relativePath)
  return {
    bizType,
    originalName,
    storedName,
    extension,
    contentType,
    fileSize: buffer.length,
    relativePath,
    absolutePath,
    accessUrl,
    absoluteAccessUrl: config.uploadPublicBaseUrl ? `${config.uploadPublicBaseUrl}${accessUrl}` : ''
  }
}

function resolveAssetBizType(asset) {
  const existingBizType = asset.existingMediaRows.find((row) => row.bizType)?.bizType
  return normalizeBizType(existingBizType || asset.bizType || 'common')
}

function buildRelativePath(bizType, storedName) {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return [bizType, year, month, day, storedName].join('/')
}

function resolveUploadTarget(baseDir, relativePath) {
  const absoluteBaseDir = path.resolve(baseDir)
  const absoluteTarget = path.resolve(absoluteBaseDir, relativePath.split('/').join(path.sep))
  const safePrefix = `${absoluteBaseDir}${path.sep}`
  if (absoluteTarget !== absoluteBaseDir && !absoluteTarget.startsWith(safePrefix)) {
    throw new Error(`Resolved path escapes upload base dir: ${absoluteTarget}`)
  }
  return absoluteTarget
}

function buildRuntimeAccessUrl(publicPath, relativePath) {
  const normalizedPublicPath = normalizePublicPath(publicPath)
  const runtimePath = normalizedPublicPath.startsWith('/api/') ? normalizedPublicPath : `/api${normalizedPublicPath}`
  return `${runtimePath}/${relativePath.replace(/\\/g, '/')}`
}

function normalizePublicPath(raw) {
  let value = String(raw || '').trim()
  if (!value) value = '/uploads'
  if (!value.startsWith('/')) value = `/${value}`
  if (value.endsWith('/')) value = value.slice(0, -1)
  return value
}

function trimTrailingSlash(value) {
  let output = String(value || '').trim()
  while (output.endsWith('/')) {
    output = output.slice(0, -1)
  }
  return output
}

function getPrimaryContentType(header) {
  return String(header || '').split(';')[0].trim().toLowerCase()
}

function resolveExtension(sourceUrl, contentType) {
  try {
    const url = new URL(sourceUrl)
    const ext = path.posix.extname(url.pathname || '').toLowerCase()
    if (ext) return ext
  } catch {
    // ignore
  }

  switch (contentType) {
    case 'image/jpeg':
    case 'image/jpg':
      return '.jpg'
    case 'image/gif':
      return '.gif'
    case 'image/webp':
      return '.webp'
    case 'image/bmp':
      return '.bmp'
    case 'image/svg+xml':
      return '.svg'
    default:
      return '.png'
  }
}

function resolveOriginalName(sourceUrl, extension) {
  try {
    const url = new URL(sourceUrl)
    const basename = path.posix.basename(url.pathname || '')
    if (basename) {
      return basename.includes('.') ? basename : `${basename}${extension}`
    }
  } catch {
    // ignore
  }
  return `image${extension}`
}

async function rewriteDatabase(connection, state, config, report) {
  await rewriteSiteConfig(connection, state, config, report)
  await rewritePosts(connection, state, config, report)
  await rewriteFriendLinks(connection, state, config, report)
  await rewriteMomentMedia(connection, state, config, report)
  await rewriteMediaFiles(connection, state, config, report)
}

async function rewriteSiteConfig(connection, state, config, report) {
  const row = state.siteConfigRow
  if (!row || !row.parsedConfig) return

  const nextConfig = JSON.parse(JSON.stringify(row.parsedConfig))
  const changedFields = []

  changedFields.push(...rewritePlainField(nextConfig.basic || (nextConfig.basic = {}), 'profileAvatar', state.assetsBySource, config, 'basic.profileAvatar'))
  changedFields.push(...rewritePlainField(nextConfig.home || (nextConfig.home = {}), 'backgroundImage', state.assetsBySource, config, 'home.backgroundImage'))

  const animeImages = Array.isArray(nextConfig.about?.animeImages) ? nextConfig.about.animeImages : []
  for (let index = 0; index < animeImages.length; index += 1) {
    const sourceUrl = normalizeLegacySourceUrl(animeImages[index], config.legacyHosts)
    const replacement = sourceUrl ? state.assetsBySource.get(sourceUrl)?.local?.accessUrl : ''
    if (replacement) {
      animeImages[index] = replacement
      changedFields.push(`about.animeImages[${index}]`)
    }
  }

  if (!changedFields.length) return

  try {
    await connection.execute(
      'UPDATE blog_site_setting SET setting_value = ? WHERE setting_key = ?',
      [JSON.stringify(nextConfig), row.settingKey]
    )
    report.rewritten.push(sanitizeReportEntry({
      table: 'blog_site_setting',
      rowId: row.settingKey,
      fields: changedFields
    }, config.keepSourceUrlReport))
  } catch (error) {
    report.failed.push({
      table: 'blog_site_setting',
      rowId: row.settingKey,
      error: error.message
    })
  }
}

function rewritePlainField(target, key, assetsBySource, config, reportField) {
  const sourceUrl = normalizeLegacySourceUrl(target?.[key], config.legacyHosts)
  const replacement = sourceUrl ? assetsBySource.get(sourceUrl)?.local?.accessUrl : ''
  if (!replacement) return []
  target[key] = replacement
  return [reportField || key]
}

async function rewritePosts(connection, state, config, report) {
  for (const row of state.postRows) {
    const updates = []
    const params = []
    const changedFields = []

    const coverUrl = normalizeLegacySourceUrl(row.cover, config.legacyHosts)
    const coverReplacement = coverUrl ? state.assetsBySource.get(coverUrl)?.local?.accessUrl : ''
    if (coverReplacement) {
      updates.push('cover = ?')
      params.push(coverReplacement)
      changedFields.push('cover')
    }

    const rewrittenContent = rewriteContentImages(row.content, state.assetsBySource, config)
    if (rewrittenContent.changed) {
      updates.push('content = ?')
      params.push(rewrittenContent.content)
      changedFields.push('content')
    }

    if (!updates.length) continue

    try {
      await connection.execute(
        `UPDATE blog_post SET ${updates.join(', ')} WHERE id = ?`,
        [...params, row.id]
      )
      report.rewritten.push(sanitizeReportEntry({
        table: 'blog_post',
        rowId: row.id,
        fields: changedFields
      }, config.keepSourceUrlReport))
    } catch (error) {
      report.failed.push({
        table: 'blog_post',
        rowId: row.id,
        error: error.message
      })
    }
  }
}

async function rewriteFriendLinks(connection, state, config, report) {
  for (const row of state.friendLinkRows) {
    const sourceUrl = normalizeLegacySourceUrl(row.avatar, config.legacyHosts)
    const replacement = sourceUrl ? state.assetsBySource.get(sourceUrl)?.local?.accessUrl : ''
    if (!replacement) continue

    try {
      await connection.execute(
        'UPDATE friend_link SET avatar = ? WHERE id = ?',
        [replacement, row.id]
      )
      report.rewritten.push(sanitizeReportEntry({
        table: 'friend_link',
        rowId: row.id,
        fields: ['avatar']
      }, config.keepSourceUrlReport))
    } catch (error) {
      report.failed.push({
        table: 'friend_link',
        rowId: row.id,
        error: error.message
      })
    }
  }
}

async function rewriteMomentMedia(connection, state, config, report) {
  for (const row of state.momentMediaRows) {
    const sourceUrl = normalizeLegacySourceUrl(row.mediaUrl, config.legacyHosts)
    const replacement = sourceUrl ? state.assetsBySource.get(sourceUrl)?.local?.accessUrl : ''
    if (!replacement) continue

    try {
      await connection.execute(
        'UPDATE blog_moment_media SET media_url = ? WHERE id = ?',
        [replacement, row.id]
      )
      report.rewritten.push(sanitizeReportEntry({
        table: 'blog_moment_media',
        rowId: row.id,
        fields: ['media_url']
      }, config.keepSourceUrlReport))
    } catch (error) {
      report.failed.push({
        table: 'blog_moment_media',
        rowId: row.id,
        error: error.message
      })
    }
  }
}

async function rewriteMediaFiles(connection, state, config, report) {
  const updatedSources = new Set()

  for (const row of state.mediaRows) {
    const sourceUrl = normalizeLegacySourceUrl(row.accessUrl, config.legacyHosts)
    const local = sourceUrl ? state.assetsBySource.get(sourceUrl)?.local : null
    if (!local) continue

    try {
      await connection.execute(
        `
          UPDATE blog_media_file
             SET biz_type = ?,
                 storage_type = 'local',
                 original_name = ?,
                 stored_name = ?,
                 extension = ?,
                 content_type = ?,
                 file_size = ?,
                 relative_path = ?,
                 access_url = ?
           WHERE id = ?
        `,
        [
          normalizeBizType(row.bizType || local.bizType),
          local.originalName,
          local.storedName,
          local.extension,
          local.contentType,
          local.fileSize,
          local.relativePath,
          local.accessUrl,
          row.id
        ]
      )
      updatedSources.add(sourceUrl)
      report.rewritten.push(sanitizeReportEntry({
        table: 'blog_media_file',
        rowId: row.id,
        fields: ['storage_type', 'relative_path', 'access_url'],
        sourceUrl
      }, config.keepSourceUrlReport))
    } catch (error) {
      report.failed.push(sanitizeReportEntry({
        table: 'blog_media_file',
        rowId: row.id,
        sourceUrl,
        error: error.message
      }, config.keepSourceUrlReport))
    }
  }

  for (const asset of state.assetsBySource.values()) {
    if (!asset.local) {
      report.skipped.push(sanitizeReportEntry({
        table: 'blog_media_file',
        sourceUrl: asset.sourceUrl,
        reason: 'No downloaded file available for insert'
      }, config.keepSourceUrlReport))
      continue
    }
    if (updatedSources.has(asset.sourceUrl) || asset.existingMediaRows.length) continue

    try {
      const [result] = await connection.execute(
        `
          INSERT INTO blog_media_file (
            biz_type,
            storage_type,
            original_name,
            stored_name,
            extension,
            content_type,
            file_size,
            relative_path,
            access_url
          ) VALUES (?, 'local', ?, ?, ?, ?, ?, ?, ?)
        `,
        [
          asset.local.bizType,
          asset.local.originalName,
          asset.local.storedName,
          asset.local.extension,
          asset.local.contentType,
          asset.local.fileSize,
          asset.local.relativePath,
          asset.local.accessUrl
        ]
      )
      report.rewritten.push(sanitizeReportEntry({
        table: 'blog_media_file',
        rowId: result.insertId,
        mode: 'insert',
        fields: ['relative_path', 'access_url'],
        sourceUrl: asset.sourceUrl
      }, config.keepSourceUrlReport))
    } catch (error) {
      report.failed.push(sanitizeReportEntry({
        table: 'blog_media_file',
        mode: 'insert',
        sourceUrl: asset.sourceUrl,
        error: error.message
      }, config.keepSourceUrlReport))
    }
  }
}

function rewriteContentImages(content, assetsBySource, config) {
  let changed = false
  let output = String(content || '')

  output = output.replace(MARKDOWN_IMAGE_REGEX, (match, altText, openBracket, url, closeBracket, titlePart) => {
    const normalized = normalizeLegacySourceUrl(url, config.legacyHosts)
    const replacement = normalized ? assetsBySource.get(normalized)?.local?.accessUrl : ''
    if (!replacement) return match
    changed = true
    return `![${altText}](${openBracket || ''}${replacement}${closeBracket || ''}${titlePart || ''})`
  })

  output = output.replace(HTML_IMAGE_REGEX, (match, before, quote, url, after) => {
    const normalized = normalizeLegacySourceUrl(url, config.legacyHosts)
    const replacement = normalized ? assetsBySource.get(normalized)?.local?.accessUrl : ''
    if (!replacement) return match
    changed = true
    return `<img${before}src=${quote}${replacement}${quote}${after}>`
  })

  return {
    changed,
    content: output
  }
}

async function validateDownloadedAssets(assetsBySource, config, report) {
  for (const asset of assetsBySource.values()) {
    if (!asset.local) continue

    const exists = fs.existsSync(asset.local.absolutePath)
    if (!exists) {
      report.failed.push(sanitizeReportEntry({
        table: 'filesystem',
        sourceUrl: asset.sourceUrl,
        error: `Downloaded file missing: ${asset.local.absolutePath}`
      }, config.keepSourceUrlReport))
      continue
    }

    if (!asset.local.absoluteAccessUrl) {
      report.skipped.push({
        table: 'validation',
        reason: 'UPLOAD_PUBLIC_BASE_URL not configured, skipped HTTP validation',
        accessUrl: asset.local.accessUrl
      })
      continue
    }

    try {
      await probeRemoteImage(asset.local.absoluteAccessUrl, config.requestTimeoutMs)
    } catch (error) {
      report.failed.push(sanitizeReportEntry({
        table: 'validation',
        sourceUrl: asset.sourceUrl,
        accessUrl: asset.local.absoluteAccessUrl,
        error: error.message
      }, config.keepSourceUrlReport))
    }
  }
}

async function timedFetch(url, timeoutMs) {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(url, {
      method: 'GET',
      redirect: 'follow',
      signal: controller.signal,
      headers: {
        'user-agent': 'GodLeiBlog Legacy Image Migration/1.0'
      }
    })
  } finally {
    clearTimeout(timer)
  }
}

function getHostname(url) {
  try {
    return new URL(url).hostname.toLowerCase()
  } catch {
    return 'invalid-host'
  }
}

function normalizeBizType(raw) {
  const source = String(raw || '').trim().toLowerCase()
  if (!source) return 'common'
  const normalized = source
    .replace(/[^a-z0-9-]/g, '-')
    .replace(/-{2,}/g, '-')
    .replace(/^-|-$/g, '')
  return normalized || 'common'
}

function parseBoolean(value, fallback) {
  if (value === undefined || value === null || value === '') return fallback
  const normalized = String(value).trim().toLowerCase()
  if (['1', 'true', 'yes', 'on'].includes(normalized)) return true
  if (['0', 'false', 'no', 'off'].includes(normalized)) return false
  return fallback
}

function parseInteger(value, fallback) {
  const parsed = Number.parseInt(String(value || ''), 10)
  return Number.isFinite(parsed) ? parsed : fallback
}

async function writeReport(reportPath, report) {
  await fsp.mkdir(path.dirname(reportPath), { recursive: true })
  report.finishedAt = new Date().toISOString()
  await fsp.writeFile(reportPath, JSON.stringify(report, null, 2), 'utf8')
}

function sanitizeReportEntry(entry, keepSourceUrlReport) {
  const output = { ...entry }
  if (!keepSourceUrlReport) {
    delete output.sourceUrl
    delete output.sourceUrls
  }
  return output
}

function printBackupReminder(config) {
  console.log('[migrate-legacy-images] Apply mode enabled.')
  console.log('[migrate-legacy-images] Backup these tables before continuing:')
  console.log('  - blog_post')
  console.log('  - friend_link')
  console.log('  - blog_moment_media')
  console.log('  - blog_media_file')
  console.log('  - blog_site_setting')
  console.log(`[migrate-legacy-images] Backup upload directory: ${config.uploadBaseDir}`)
}

function printSummary(report, config) {
  console.log(`[migrate-legacy-images] Mode: ${report.mode}`)
  console.log(`[migrate-legacy-images] Discovered references: ${report.discovered.total}`)
  console.log(`[migrate-legacy-images] Unique sources: ${report.discovered.uniqueSourceCount}`)
  console.log(`[migrate-legacy-images] Downloaded: ${report.downloaded.length}`)
  console.log(`[migrate-legacy-images] Rewritten: ${report.rewritten.length}`)
  console.log(`[migrate-legacy-images] Unreachable: ${report.unreachableSources.length}`)
  console.log(`[migrate-legacy-images] Failed: ${report.failed.length}`)
  console.log(`[migrate-legacy-images] Remaining legacy refs: ${report.remainingLegacyReferences.length}`)
  console.log(`[migrate-legacy-images] Report: ${config.reportPath}`)
}

main().catch((error) => {
  console.error('[migrate-legacy-images] Failed:', error && error.stack ? error.stack : error)
  process.exitCode = 1
})
