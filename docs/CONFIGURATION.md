# GodLeiBlog 项目配置说明

本文档整理项目的本地开发配置和服务器部署配置，方便后续开发和版本更新。

## 一、环境概述

| 环境       | 说明                     | 配置文件           |
| ---------- | ------------------------ | ------------------ |
| 本地开发   | 本地启动所有服务进行开发 | `.env.development` |
| 服务器部署 | 部署到阿里云ECS服务器    | `.env.production`  |

---

## 二、前端配置 (GodLeiBlog-Frontend)

### 2.1 本地开发配置

创建文件：`GodLeiBlog-Frontend/.env.development`

```env
# ============================================
# 本地开发环境配置
# ============================================

# Twikoo评论系统 - 本地MongoDB+Twikoo
VITE_TWIKOO_URL=http://localhost:3000
VITE_TWIKOO_PROXY_TARGET=http://localhost:3000

# API接口地址 - 本地后端
VITE_API_BASE_URL=http://localhost:8080

# 站点配置
VITE_SITE_URL=http://localhost:5173
```

### 2.2 服务器部署配置

创建文件：`GodLeiBlog-Frontend/.env.production`

```env
# ============================================
# 生产环境配置 (服务器部署)
# ============================================

# Twikoo评论系统 - 服务器本地部署
VITE_TWIKOO_URL=http://8.137.187.70/twikoo/
VITE_TWIKOO_PROXY_TARGET=http://8.137.187.70/twikoo/

# API接口地址 - 同域代理
VITE_API_BASE_URL=/api

# 站点配置
VITE_SITE_URL=http://8.137.187.70
```

### 2.3 当前使用的配置

文件：`GodLeiBlog-Frontend/.env`

```env
# Twikoo评论系统配置
# 服务器部署配置
VITE_TWIKOO_URL=http://8.137.187.70/twikoo/
VITE_TWIKOO_PROXY_TARGET=http://8.137.187.70/twikoo/

# 本地开发配置（备用）
# VITE_TWIKOO_URL=http://localhost:3000
# VITE_TWIKOO_PROXY_TARGET=http://localhost:3000

# 旧Twikoo地址（已失效）
# VITE_TWIKOO_URL=https://twikoo.godlei.cn
```

---

## 三、管理端配置 (GodLeiBlog-AdminPanel)

### 3.1 本地开发配置

创建文件：`GodLeiBlog-AdminPanel/.env.development`

```env
# ============================================
# 管理端 - 本地开发环境配置
# ============================================

# Twikoo评论系统 - 本地服务
VITE_TWIKOO_URL=http://localhost:3000
VITE_TWIKOO_PROXY_TARGET=http://localhost:3000

# API接口地址 - 本地后端
VITE_API_BASE_URL=http://localhost:8080
```

### 3.2 服务器部署配置

创建文件：`GodLeiBlog-AdminPanel/.env.production`

```env
# ============================================
# 管理端 - 生产环境配置
# ============================================

# Twikoo评论系统 - 服务器本地部署
VITE_TWIKOO_URL=http://8.137.187.70/twikoo/
VITE_TWIKOO_PROXY_TARGET=http://8.137.187.70/twikoo/

# API接口地址
VITE_API_BASE_URL=/api
```

### 3.3 当前使用的配置

文件：`GodLeiBlog-AdminPanel/.env`

```env
# Twikoo评论系统配置
# 本地Twikoo服务
VITE_TWIKOO_URL=http://localhost:3000
VITE_TWIKOO_PROXY_TARGET=http://localhost:3000
```

---

## 四、后端配置 (GodLeiBlog-Backend)

### 4.1 主配置文件

文件：`blog-server/src/main/resources/application.yml`

```yaml
# ============================================
# 后端主配置 (通用配置)
# ============================================

spring:
  application:
    name: blog-server
  datasource:
    url: jdbc:mysql://${hm.db.host}:3306/godleiblog
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: ${hm.db.password}
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB

mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  mapper-locations: classpath*:cn/godlei/blogserver/mapper/*.xml
  type-aliases-package: cn.godlei.blogpojo.entity

pagehelper:
  helper-dialect: mysql
  reasonable: true
  support-methods-arguments: true

server:
  port: 8080

# 存储配置
blog:
  storage:
    mode: ${BLOG_STORAGE_MODE:local}
    public-base-url: ${BLOG_STORAGE_PUBLIC_BASE_URL:}
    local:
      base-dir: ${BLOG_STORAGE_LOCAL_BASE_DIR:${user.dir}/uploads}
      public-path: ${BLOG_STORAGE_LOCAL_PUBLIC_PATH:/uploads}
```

### 4.2 开发环境配置

文件：`blog-server/src/main/resources/application-dev.yml`

```yaml
# ============================================
# 本地开发环境配置
# ============================================

hm:
  db:
    host: localhost
    password: your_local_mysql_password
```

### 4.3 服务器部署环境变量

文件：`deploy/aliyun-ecs/backend/backend.env`

```bash
# ============================================
# 服务器部署 - 后端环境变量
# ============================================

# 数据库配置
HM_DB_HOST=localhost
HM_DB_PASSWORD=your_server_mysql_password

# 存储配置
BLOG_STORAGE_MODE=local
BLOG_STORAGE_LOCAL_BASE_DIR=/opt/godleiblog/uploads
BLOG_STORAGE_LOCAL_PUBLIC_PATH=/uploads

# Java配置
JAVA_OPTS=-Xms512m -Xmx1024m
SERVER_PORT=8080
```

---

## 五、Twikoo 评论服务配置

### 5.1 本地开发配置

文件：`twikoo-local/server.js`

```javascript
const http = require('http')

// 本地开发环境变量
process.env.MONGODB_URI = 'mongodb://localhost:27017/twikoo'
process.env.TWIKOO_ADMIN_PASS = 'admin123'
process.env.SITE_NAME = 'GodLei Blog'
process.env.SITE_URL = 'http://localhost:5173'
process.env.LIMIT_PER_MINUTE = '0' // 禁用限流
process.env.LIMIT_PER_MINUTE_ALL = '0' // 禁用全站限流

const twikoo = require('twikoo-vercel')
const PORT = 3000
// ... 服务器代码
```

### 5.2 服务器部署配置

文件：`twikoo-server.js` (部署到 `/opt/twikoo/server.js`)

```javascript
const http = require('http')

// 服务器环境变量
process.env.MONGODB_URI = 'mongodb://localhost:27017/twikoo'
process.env.TWIKOO_ADMIN_PASS = 'admin123'
process.env.SITE_NAME = 'GodLei Blog'
process.env.SITE_URL = 'http://8.137.187.70'
process.env.LIMIT_PER_MINUTE = '0' // 禁用限流
process.env.LIMIT_PER_MINUTE_ALL = '0' // 禁用全站限流

const twikoo = require('twikoo-vercel')
const PORT = 3000
// ... 服务器代码
```

---

## 六、Nginx 配置

### 6.1 服务器 Nginx 配置

文件：`/etc/nginx/conf.d/godleiblog.conf`

```nginx
server {
    listen 80;
    server_name 8.137.187.70;

    # 前端静态文件
    location / {
        root /opt/godleiblog/frontend;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # Twikoo评论服务 - 支持 /twikoo 和 /twikoo/
    location = /twikoo {
        rewrite ^ /twikoo/ break;
        proxy_pass http://localhost:3000/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Content-Type $http_content_type;
        proxy_buffering off;
        client_max_body_size 10M;
    }

    location /twikoo/ {
        proxy_pass http://localhost:3000/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header Content-Type $http_content_type;
        proxy_buffering off;
        client_max_body_size 10M;
    }
}
```

---

## 七、开发工作流

### 7.1 本地开发启动步骤

```bash
# 1. 启动 MySQL (本地)
mysql -u root -p

# 2. 启动 MongoDB (本地)
mongod --dbpath D:\mongodb\data

# 3. 启动 Twikoo 服务 (本地)
cd twikoo-local
node server.js

# 4. 启动后端服务
cd GodLeiBlog-Backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 5. 启动前端开发服务器
cd GodLeiBlog-Frontend
npm run dev

# 6. 启动管理端开发服务器
cd GodLeiBlog-AdminPanel
npm run dev
```

### 7.2 服务器部署步骤

```bash
# 1. 部署后端
scp backend/app.jar root@8.137.187.70:/opt/godleiblog/backend/
ssh root@8.137.187.70 "systemctl restart godleiblog-backend"

# 2. 部署前端
scp -r frontend/dist/* root@8.137.187.70:/opt/godleiblog/frontend/

# 3. 部署管理端
scp -r admin/dist/* root@8.137.187.70:/opt/godleiblog/frontend/admin/

# 4. 重启 Twikoo (如有更新)
ssh root@8.137.187.70 "systemctl restart twikoo"

# 5. 重载 Nginx (如有配置更新)
ssh root@8.137.187.70 "nginx -t && systemctl reload nginx"
```

---

## 八、服务端口一览

| 服务    | 本地开发 | 服务器部署       | 说明             |
| ------- | -------- | ---------------- | ---------------- |
| MySQL   | 3306     | 3306             | 数据库           |
| MongoDB | 27017    | 27017            | Twikoo数据库     |
| Twikoo  | 3000     | 3000             | 评论服务         |
| 后端API | 8080     | 8080             | Spring Boot      |
| 前端    | 5173     | 80 (Nginx)       | Vite dev / Nginx |
| 管理端  | 5174     | 80/admin (Nginx) | Vite dev / Nginx |

---

## 九、重要文件位置

### 本地开发

```
D:\Ai\Blog\
├── GodLeiBlog-Frontend\.env          # 前端配置
├── GodLeiBlog-AdminPanel\.env        # 管理端配置
├── GodLeiBlog-Backend\blog-server\src\main\resources\
│   ├── application.yml                # 后端主配置
│   └── application-dev.yml            # 开发环境配置
├── twikoo-local\server.js             # 本地Twikoo服务
└── D:\mongodb\                        # MongoDB数据目录
```

### 服务器部署

```
/opt/
├── godleiblog/
│   ├── frontend/                      # 前端静态文件
│   ├── backend/
│   │   ├── app.jar                    # 后端JAR包
│   │   └── backend.env                # 后端环境变量
│   └── uploads/                       # 上传文件目录
├── twikoo/
│   ├── server.js                      # Twikoo服务
│   └── node_modules/                  # 依赖
└── /
    └── etc/nginx/conf.d/godleiblog.conf  # Nginx配置
```

---

## 十、常见问题

### Q1: 本地开发时 Twikoo 连接失败

- 检查 MongoDB 是否启动
- 检查 Twikoo 服务是否启动 (端口3000)
- 检查 `.env` 中的 `VITE_TWIKOO_URL` 是否为 `http://localhost:3000`

### Q2: 服务器部署后评论功能不工作

- 检查 MongoDB 是否运行: `systemctl status mongod`
- 检查 Twikoo 服务是否运行: `systemctl status twikoo`
- 检查 Nginx 配置是否正确代理 `/twikoo/` 路径
- 检查防火墙是否开放80端口

### Q3: 后端数据库连接失败

- 检查 MySQL 是否运行
- 检查 `application.yml` 或 `backend.env` 中的数据库密码
- 检查数据库 `godleiblog` 是否存在

---

## 十一、版本更新检查清单

每次版本更新时，请检查以下配置：

- [ ] 前端 `.env` 中的 API 地址是否正确
- [ ] 前端 `.env` 中的 Twikoo 地址是否正确
- [ ] 后端数据库连接配置是否正确
- [ ] 后端存储路径配置是否正确
- [ ] Twikoo 服务环境变量是否正确
- [ ] Nginx 配置是否需要更新
- [ ] 所有服务的 systemd 服务是否正常

---

_文档版本: 1.0_
_最后更新: 2026-04-01_
