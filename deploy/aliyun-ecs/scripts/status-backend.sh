#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
PID_FILE="$APP_ROOT/backend/app.pid"

if [[ ! -f "$PID_FILE" ]]; then
  echo "backend is not running"
  exit 0
fi

PID="$(cat "$PID_FILE")"

if kill -0 "$PID" 2>/dev/null; then
  echo "backend is running with pid $PID"
else
  echo "backend pid file exists but process is not running"
fi
