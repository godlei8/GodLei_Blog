#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

INSTALL_ROOT="${INSTALL_ROOT:-/opt/godleiblog}"
NGINX_CONF_DIR="${NGINX_CONF_DIR:-/etc/nginx/conf.d}"
SYSTEMD_DIR="${SYSTEMD_DIR:-/etc/systemd/system}"
SERVICE_NAME="${SERVICE_NAME:-godleiblog-backend}"

mkdir -p "$INSTALL_ROOT/backend"
mkdir -p "$INSTALL_ROOT/data/uploads"
mkdir -p "$INSTALL_ROOT/logs"
mkdir -p "$INSTALL_ROOT/sql"
mkdir -p "$INSTALL_ROOT/www/admin"
mkdir -p "$INSTALL_ROOT/scripts"

cp -f "$PACKAGE_ROOT/backend/app.jar" "$INSTALL_ROOT/backend/app.jar"
cp -f "$PACKAGE_ROOT/backend/backend.env.example" "$INSTALL_ROOT/backend/backend.env.example"

if [[ ! -f "$INSTALL_ROOT/backend/backend.env" ]]; then
  cp "$PACKAGE_ROOT/backend/backend.env.example" "$INSTALL_ROOT/backend/backend.env"
fi

cp -f "$PACKAGE_ROOT/sql/"* "$INSTALL_ROOT/sql/"
cp -f "$PACKAGE_ROOT/scripts/start-backend.sh" "$INSTALL_ROOT/scripts/start-backend.sh"
cp -f "$PACKAGE_ROOT/scripts/stop-backend.sh" "$INSTALL_ROOT/scripts/stop-backend.sh"
cp -f "$PACKAGE_ROOT/scripts/status-backend.sh" "$INSTALL_ROOT/scripts/status-backend.sh"
chmod +x "$INSTALL_ROOT/scripts/"*.sh

cp -rf "$PACKAGE_ROOT/www/"* "$INSTALL_ROOT/www/"

cp -f "$PACKAGE_ROOT/nginx/godleiblog.conf" "$NGINX_CONF_DIR/godleiblog.conf"
cp -f "$PACKAGE_ROOT/systemd/$SERVICE_NAME.service" "$SYSTEMD_DIR/$SERVICE_NAME.service"

systemctl daemon-reload

echo "Installed to $INSTALL_ROOT"
echo "Edit backend config: $INSTALL_ROOT/backend/backend.env"
echo "Check Nginx config: $NGINX_CONF_DIR/godleiblog.conf"
echo "Start backend: systemctl enable --now $SERVICE_NAME"
