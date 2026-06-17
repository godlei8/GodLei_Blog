#!/bin/bash

# MongoDB 7.0 安装脚本（适用于 Alibaba Cloud Linux / RHEL 8）

set -e

echo "=== 开始安装 MongoDB 7.0 ==="

# 创建 MongoDB 仓库文件
cat > /etc/yum.repos.d/mongodb-org-7.0.repo << 'EOF'
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/redhat/8/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://pgp.mongodb.com/server-7.0.asc
EOF

# 安装 MongoDB
echo "安装 MongoDB..."
yum install -y mongodb-org

# 创建数据目录
mkdir -p /var/lib/mongo
mkdir -p /var/log/mongodb
chown -R mongod:mongod /var/lib/mongo
chown -R mongod:mongod /var/log/mongodb

# 启动并启用 MongoDB
systemctl daemon-reload
systemctl start mongod
systemctl enable mongod

echo "=== MongoDB 安装完成 ==="
echo "状态:"
systemctl status mongod --no-pager
