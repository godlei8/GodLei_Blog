#!/bin/bash

# Twikoo 服务安装脚本

set -e

TWIKOO_DIR="/opt/twikoo"

echo "=== 开始安装 Twikoo 服务 ==="

# 安装 Node.js 18.x（如果未安装）
if ! command -v node &> /dev/null; then
    echo "安装 Node.js 18.x..."
    curl -fsSL https://rpm.nodesource.com/setup_18.x | bash -
    yum install -y nodejs
fi

echo "Node.js 版本: $(node --version)"
echo "npm 版本: $(npm --version)"

# 创建 Twikoo 目录
mkdir -p $TWIKOO_DIR
cd $TWIKOO_DIR

# 初始化 package.json（如果不存在）
if [ ! -f package.json ]; then
    cat > package.json << 'EOF'
{
  "name": "twikoo-server",
  "version": "1.0.0",
  "description": "Twikoo comment server",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "twikoo-vercel": "latest"
  }
}
EOF
fi

# 安装依赖
echo "安装 Twikoo 依赖..."
npm install

# 复制 server.js（由部署脚本提供）
if [ ! -f server.js ]; then
    echo "错误: server.js 不存在，请确保已上传"
    exit 1
fi

# 创建 systemd 服务
cat > /etc/systemd/system/twikoo.service << EOF
[Unit]
Description=Twikoo Comment Service
After=network.target mongod.service

[Service]
Type=simple
User=root
WorkingDirectory=$TWIKOO_DIR
ExecStart=/usr/bin/node server.js
Restart=always
RestartSec=5
Environment=NODE_ENV=production
Environment=LIMIT_PER_MINUTE=0
Environment=LIMIT_PER_MINUTE_ALL=0

[Install]
WantedBy=multi-user.target
EOF

# 启动服务
systemctl daemon-reload
systemctl start twikoo
systemctl enable twikoo

echo "=== Twikoo 服务安装完成 ==="
echo "状态:"
systemctl status twikoo --no-pager

echo ""
echo "Twikoo 服务已运行在 http://localhost:3000"
echo "MongoDB: mongodb://localhost:27017/twikoo"
