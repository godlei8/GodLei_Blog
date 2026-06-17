# Legacy Image Migration

This document describes how to migrate historical on-site images from legacy hosts back to local disk storage and rewrite database references to `/api/uploads/...`.

## Scope

The CLI scans and migrates these locations:

- `blog_site_setting.setting_value` for:
  - `basic.profileAvatar`
  - `home.backgroundImage`
  - `about.animeImages[*]`
- `blog_post.cover`
- `blog_post.content` for Markdown image syntax and HTML `<img src>`
- `friend_link.avatar`
- `blog_moment_media.media_url`
- `blog_media_file.access_url`

It does not rewrite:

- third-party external images
- documentation sample images
- non-image resources

## Files

- CLI: [`scripts/migrate-legacy-images.js`](/D:/Ai/Blog/scripts/migrate-legacy-images.js)
- Config example: [`scripts/migrate-legacy-images.env.example`](/D:/Ai/Blog/scripts/migrate-legacy-images.env.example)
- Script package: [`scripts/package.json`](/D:/Ai/Blog/scripts/package.json)

## Install

Run once inside the `scripts` directory:

```powershell
cd D:\Ai\Blog\scripts
npm install
```

## Required Config

Set these values by environment variables or an env file loaded with `--env-file`:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `UPLOAD_BASE_DIR`
- `UPLOAD_PUBLIC_PATH`
- `UPLOAD_PUBLIC_BASE_URL`
- `LEGACY_IMAGE_HOSTS`
- `REQUEST_TIMEOUT_MS`
- `DRY_RUN`
- `REPORT_PATH`
- `KEEP_SOURCE_URL_REPORT`

## Dry Run

Recommended first pass:

```powershell
cd D:\Ai\Blog
node .\scripts\migrate-legacy-images.js --env-file .\scripts\migrate-legacy-images.env.example --dry-run
```

Expected output:

- discovered reference count
- unique source count
- unreachable source list
- JSON report path

## Apply

Before running `apply`, back up:

- `blog_post`
- `friend_link`
- `blog_moment_media`
- `blog_media_file`
- `blog_site_setting`
- upload target directory

Then run:

```powershell
cd D:\Ai\Blog
node .\scripts\migrate-legacy-images.js --env-file .\scripts\migrate-legacy-images.env.example --apply
```

## Validation Checklist

After apply:

- confirm the JSON report shows `remainingLegacyReferences` as `0`
- query the database to ensure old legacy hosts are gone
- check files exist under `<UPLOAD_BASE_DIR>/<bizType>/<yyyy>/<MM>/<dd>/`
- open random migrated pages and images
- verify `/api/uploads/...` returns `200`
- verify `/uploads/...` returns `200`

## Notes

- The CLI always persists relative runtime URLs like `/api/uploads/...`
- `UPLOAD_PUBLIC_BASE_URL` is only used for optional HTTP validation after download
- The tool updates existing `blog_media_file` rows when matched, and inserts missing ones for newly tracked migrated assets
- Keep legacy image hosting online for 7 days after a successful migration as the rollback window
