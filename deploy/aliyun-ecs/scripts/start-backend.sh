#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$APP_ROOT/backend"
LOG_DIR="$APP_ROOT/logs"
PID_FILE="$BACKEND_DIR/app.pid"
ENV_FILE="$BACKEND_DIR/backend.env"

mkdir -p "$LOG_DIR"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  . "$ENV_FILE"
  set +a
fi

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
  echo "backend already running with pid $(cat "$PID_FILE")"
  exit 0
fi

JAVA_BIN="${JAVA_BIN:-java}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
SERVER_PORT="${SERVER_PORT:-8080}"
JAVA_OPTS_VALUE="${JAVA_OPTS:--Xms256m -Xmx512m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai}"
read -r -a JAVA_OPTS_ARRAY <<< "$JAVA_OPTS_VALUE"

nohup "$JAVA_BIN" "${JAVA_OPTS_ARRAY[@]}" \
  -Dspring.profiles.active="$SPRING_PROFILES_ACTIVE" \
  -jar "$BACKEND_DIR/app.jar" \
  --server.port="$SERVER_PORT" \
  > "$LOG_DIR/backend.out.log" 2> "$LOG_DIR/backend.err.log" < /dev/null &

echo $! > "$PID_FILE"
echo "backend started with pid $(cat "$PID_FILE")"
